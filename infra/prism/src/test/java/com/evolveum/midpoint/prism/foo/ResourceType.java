//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.02.07 at 10:53:52 AM CET
//


package com.evolveum.midpoint.prism.foo;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;


/**
 * <p>Java class for ResourceType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ResourceType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd}ObjectType">
 *       &lt;sequence>
 *         &lt;element name="schema" type="{http://prism.evolveum.com/xml/ns/public/types-3}SchemaDefinitionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResourceType", propOrder = {
    "schema"
})
public class ResourceType extends ObjectType implements Serializable {
    private final static long serialVersionUID = 201202081233L;

    public final static ItemName F_SCHEMA = new ItemName(NS_FOO, "schema");

    protected SchemaDefinitionType schema;

    /**
     * Gets the value of the schema property.
     *
     * @return
     *     possible object is
     *     {@link SchemaDefinitionType }
     *
     */
    public SchemaDefinitionType getSchema() {
        return schema;
    }

    /**
     * Sets the value of the schema property.
     *
     * @param value
     *     allowed object is
     *     {@link SchemaDefinitionType }
     *
     */
    public void setSchema(SchemaDefinitionType value) {
        this.schema = value;
    }

}
