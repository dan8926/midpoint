/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.Function;

/**
 * @param <E> evaluator bean (configuration) type
 * @author Radovan Semancik
 */
public abstract class AbstractExpressionEvaluator<V extends PrismValue, D extends ItemDefinition, E>
        implements ExpressionEvaluator<V> {

    /**
     * Qualified name of the evaluator.
     */
    @NotNull private final QName elementName;

    /**
     * Bean (i.e. configuration) for the evaluator.
     * In some cases it can be null - e.g. for implicit asIs evaluator.
     */
    protected final E expressionEvaluatorBean;

    /**
     * Definition of the output item.
     * Needed for some of the evaluators (the question is if it's really needed).
     */
    protected final D outputDefinition;

    protected final Protector protector;
    @NotNull protected final PrismContext prismContext;

    public AbstractExpressionEvaluator(@NotNull QName elementName, E expressionEvaluatorBean, D outputDefinition,
            Protector protector, @NotNull PrismContext prismContext) {
        this.elementName = elementName;
        this.expressionEvaluatorBean = expressionEvaluatorBean;
        this.outputDefinition = outputDefinition;
        this.prismContext = prismContext;
        this.protector = protector;
    }

    @Override
    public @NotNull QName getElementName() {
        return elementName;
    }

    /**
     * Check expression profile. Throws security exception if the execution is not allowed by the profile.
     * <p>
     * This implementation works only for simple evaluators that do not have any profile settings.
     * Complex evaluators should override this method.
     *
     * @throws SecurityViolationException expression execution is not allowed by the profile.
     */
    protected void checkEvaluatorProfile(ExpressionEvaluationContext context) throws SecurityViolationException {
        ExpressionUtil.checkEvaluatorProfileSimple(this, context);
    }

    public @NotNull PrismContext getPrismContext() {
        return prismContext;
    }

    public D getOutputDefinition() {
        return outputDefinition;
    }

    public Protector getProtector() {
        return protector;
    }

    /**
     * Converts intermediate expression result triple to the final output triple
     * according to expected Java type and additional convertor.
     *
     * TODO why it is used only for some evaluators?
     */
    public PrismValueDeltaSetTriple<V> finishOutputTriple(PrismValueDeltaSetTriple<V> resultTriple,
            Function<Object, Object> additionalConvertor, ItemPath residualPath) {

        if (resultTriple == null) {
            return null;
        }

        final Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
        if (resultTripleValueClass == null) {
            // triple is empty. type does not matter.
            return resultTriple;
        }
        Class<?> expectedJavaType = getClassForType(outputDefinition.getTypeName());
        if (resultTripleValueClass == expectedJavaType) {
            return resultTriple;
        }

        resultTriple.accept(visitable -> {
            if (visitable instanceof PrismPropertyValue<?>) {
                //noinspection unchecked
                PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>) visitable;
                Object realVal = pval.getValue();
                if (realVal != null) {
                    if (Structured.class.isAssignableFrom(resultTripleValueClass)) {
                        if (residualPath != null && !residualPath.isEmpty()) {
                            realVal = ((Structured) realVal).resolve(residualPath);
                        }
                    }
                    if (expectedJavaType != null) {
                        Object convertedVal = ExpressionUtil.convertValue(expectedJavaType, additionalConvertor, realVal, protector, prismContext);
                        pval.setValue(convertedVal);
                    }
                }
            }
        });
        return resultTriple;
    }

    // TODO this should be a standard method
    private Class<?> getClassForType(@NotNull QName typeName) {
        Class<?> aClass = XsdTypeMapper.toJavaType(typeName);
        if (aClass != null) {
            return aClass;
        } else {
            return prismContext.getSchemaRegistry().getCompileTimeClass(typeName);
        }
    }

    public TypedValue<?> findInSourcesAndVariables(ExpressionEvaluationContext context, String variableName) {
        for (Source<?, ?> source : context.getSources()) {
            if (variableName.equals(source.getName().getLocalPart())) {
                return new TypedValue<>(source, source.getDefinition());
            }
        }

        if (context.getVariables() != null) {
            return context.getVariables().get(variableName);
        } else {
            return null;
        }
    }

    public void applyValueMetadata(PrismValueDeltaSetTriple<V> triple, ExpressionEvaluationContext context,
            OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        if (triple != null && context.getValueMetadataComputer() != null) {
            for (V value : triple.getPlusSet()) {
                applyValueMetadata(value, context, result);
            }
            for (V value : triple.getZeroSet()) {
                applyValueMetadata(value, context, result);
            }
        }
    }

    private void applyValueMetadata(PrismValue value, ExpressionEvaluationContext context, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (value != null) {
            value.setValueMetadata(
                    context.getValueMetadataComputer()
                            .compute(Collections.singletonList(value), result));
        }
    }
}
