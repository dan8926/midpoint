/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.impl.lex.json.Constants;
import com.evolveum.midpoint.prism.impl.xnode.*;

import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.MetadataAware;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes single or multiple documents (JSON/YAML).
 */
class DocumentWriter {

    @NotNull private final WritingContext<?> ctx;

    @NotNull private final JsonGenerator generator;

    private String currentNamespace;

    DocumentWriter(WritingContext<?> ctx) {
        this.ctx = ctx;
        this.generator = ctx.generator;
    }

    void writeListAsSeparateDocuments(@NotNull ListXNodeImpl root) throws IOException {
        boolean first = true;
        for (XNodeImpl item : root) {
            if (!first) {
                ctx.newDocument();
            } else {
                first = false;
            }
            write(item);
        }
    }

    public void write(XNodeImpl xnode) throws IOException {
        if (xnode instanceof RootXNodeImpl) {
            write(((RootXNodeImpl) xnode).toMapXNode(), false);
        } else {
            write(xnode, false);
        }
    }

    private void write(XNodeImpl xnode, boolean wrappingValue) throws IOException {
        if (xnode == null) {
            writeNull();
        } else if (xnode instanceof MapXNodeImpl) {
            writeMap((MapXNodeImpl) xnode);
        } else if (!wrappingValue && needsValueWrapping(xnode)) {
            writeWithValueWrapped(xnode);
        } else if (xnode instanceof ListXNodeImpl) {
            writeList((ListXNodeImpl) xnode);
        } else if (xnode instanceof PrimitiveXNodeImpl) {
            writePrimitive((PrimitiveXNodeImpl<?>) xnode);
        } else if (xnode instanceof SchemaXNodeImpl) {
            writeSchema((SchemaXNodeImpl) xnode);
        } else if (xnode instanceof IncompleteMarkerXNodeImpl) {
            writeIncomplete();
        } else {
            throw new UnsupportedOperationException("Cannot write " + xnode);
        }
    }

    private boolean needsValueWrapping(XNodeImpl xnode) {
        return xnode.getElementName() != null
                || getExplicitType(xnode) != null && !ctx.supportsInlineTypes()
                || xnode.hasMetadata();
    }

    private void writeNull() throws IOException {
        generator.writeNull();
    }

    private void writeWithValueWrapped(XNodeImpl xnode) throws IOException {
        assert !(xnode instanceof MapXNode);
        generator.writeStartObject();
        ctx.resetInlineTypeIfPossible();
        writeElementAndTypeIfNeeded(xnode);
        generator.writeFieldName(Constants.PROP_VALUE);
        write(xnode, true);
        writeMetadataIfNeeded(xnode);
        generator.writeEndObject();
    }

    private void writeMap(MapXNodeImpl map) throws IOException {
        writeInlineTypeIfNeeded(map);
        generator.writeStartObject();
        ctx.resetInlineTypeIfPossible();
        String oldDefaultNamespace = currentNamespace;
        writeNsDeclarationIfNeeded(map);
        writeElementAndTypeIfNeeded(map);
        writeMetadataIfNeeded(map);
        for (Map.Entry<QName, XNodeImpl> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                generator.writeFieldName(createKeyUri(entry));
                write(entry.getValue(), false);
            }
        }
        generator.writeEndObject();
        currentNamespace = oldDefaultNamespace;
    }

    private void writeList(ListXNodeImpl list) throws IOException {
        writeInlineTypeIfNeeded(list);
        generator.writeStartArray();
        ctx.resetInlineTypeIfPossible();
        for (XNodeImpl item : list) {
            write(item, false);
        }
        generator.writeEndArray();
    }

    private <T> void writePrimitive(PrimitiveXNodeImpl<T> primitive) throws IOException {
        writeInlineTypeIfNeeded(primitive);
        if (primitive.isParsed()) {
            generator.writeObject(primitive.getValue());
        } else {
            generator.writeObject(primitive.getStringValue());
        }
    }

    private void writeSchema(SchemaXNodeImpl node) throws IOException {
        writeInlineTypeIfNeeded(node);
        Element schemaElement = node.getSchemaElement();
        DOMUtil.fixNamespaceDeclarations(schemaElement); // TODO reconsider if it's OK to modify schema DOM element in this way
        generator.writeObject(schemaElement);
    }

    private void writeIncomplete() throws IOException {
        generator.writeStartObject();
        generator.writeFieldName(Constants.PROP_INCOMPLETE);
        generator.writeBoolean(true);
        generator.writeEndObject();
    }

    private void writeElementAndTypeIfNeeded(XNodeImpl xnode) throws IOException {
        QName elementName = xnode.getElementName();
        if (elementName != null) {
            generator.writeObjectField(Constants.PROP_ELEMENT, createElementNameUri(elementName));
        }
        QName typeName = getExplicitType(xnode);
        if (typeName != null) {
            if (!ctx.supportsInlineTypes()) {
                generator.writeObjectField(Constants.PROP_TYPE, typeName);
            }
        }
    }

    private void writeMetadataIfNeeded(XNodeImpl xnode) throws IOException {
        if (xnode instanceof MetadataAware) {
            List<MapXNode> metadataNodes = ((MetadataAware) xnode).getMetadataNodes();
            if (!metadataNodes.isEmpty()) {
                generator.writeFieldName(Constants.PROP_METADATA);
                if (metadataNodes.size() == 1) {
                    writeMap((MapXNodeImpl) metadataNodes.get(0));
                } else {
                    generator.writeStartArray();
                    for (MapXNode metadataNode : metadataNodes) {
                        writeMap((MapXNodeImpl) metadataNode);
                    }
                    generator.writeEndArray();
                }
            }
        }
    }

    private void writeNsDeclarationIfNeeded(MapXNodeImpl map) throws IOException {
        SerializationOptions opts = ctx.prismSerializationContext.getOptions();
        if (!SerializationOptions.isUseNsProperty(opts) || map.isEmpty()) {
            return;
        }
        String namespace = determineNewCurrentNamespace(map);
        if (namespace != null && !StringUtils.equals(namespace, currentNamespace)) {
            currentNamespace = namespace;
            generator.writeFieldName(Constants.PROP_NAMESPACE);
            generator.writeString(namespace);
        }
    }

    private String determineNewCurrentNamespace(MapXNodeImpl map) {
        Map<String,Integer> counts = new HashMap<>();
        for (QName childName : map.keySet()) {
            String childNs = childName.getNamespaceURI();
            if (StringUtils.isEmpty(childNs)) {
                continue;
            }
            if (childNs.equals(currentNamespace)) {
                return currentNamespace;                    // found existing => continue with it
            }
            increaseCounter(counts, childNs);
        }
        if (map.getElementName() != null && QNameUtil.hasNamespace(map.getElementName())) {
            increaseCounter(counts, map.getElementName().getNamespaceURI());
        }
        // otherwise, take the URI that occurs the most in the map
        Map.Entry<String,Integer> max = null;
        for (Map.Entry<String,Integer> count : counts.entrySet()) {
            if (max == null || count.getValue() > max.getValue()) {
                max = count;
            }
        }
        return max != null ? max.getKey() : null;
    }

    private void increaseCounter(Map<String, Integer> counts, String childNs) {
        Integer c = counts.get(childNs);
        counts.put(childNs, c != null ? c+1 : 1);
    }

    private String createKeyUri(Map.Entry<QName, XNodeImpl> entry) {
        QName key = entry.getKey();
        if (namespaceMatch(currentNamespace, key.getNamespaceURI())) {
            return key.getLocalPart();
        } else if (StringUtils.isNotEmpty(currentNamespace) && !isAttribute(entry.getValue())) {
            return QNameUtil.qNameToUri(key, true);        // items with no namespace should be written as such (starting with '#')
        } else {
            return QNameUtil.qNameToUri(key, false);    // items with no namespace can be written in plain
        }
    }

    private String createElementNameUri(QName elementName) {
        if (namespaceMatch(currentNamespace, elementName.getNamespaceURI())) {
            return elementName.getLocalPart();
        } else {
            return QNameUtil.qNameToUri(elementName, StringUtils.isNotEmpty(currentNamespace));
        }
    }

    private boolean isAttribute(XNodeImpl node) {
        return node instanceof PrimitiveXNodeImpl && ((PrimitiveXNodeImpl) node).isAttribute();
    }

    private boolean namespaceMatch(String currentNamespace, String itemNamespace) {
        if (StringUtils.isEmpty(currentNamespace)) {
            return StringUtils.isEmpty(itemNamespace);
        } else {
            return currentNamespace.equals(itemNamespace);
        }
    }

    private void writeInlineTypeIfNeeded(XNodeImpl node) throws IOException {
        QName explicitType = getExplicitType(node);
        if (ctx.supportsInlineTypes() && explicitType != null) {
            ctx.writeInlineType(explicitType);
        }
    }

    private QName getExplicitType(XNodeImpl xnode) {
        return xnode.isExplicitTypeDeclaration() ? xnode.getTypeQName() : null;
    }
}
