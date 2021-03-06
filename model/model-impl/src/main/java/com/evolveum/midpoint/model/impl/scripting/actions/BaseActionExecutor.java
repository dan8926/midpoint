/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.scripting.*;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Superclass for all action executors.
 */
public abstract class BaseActionExecutor implements ActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(BaseActionExecutor.class);

    @Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;
    @Autowired protected PrismContext prismContext;
    @Autowired protected OperationsHelper operationsHelper;
    @Autowired protected ExpressionHelper expressionHelper;
    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected ModelService modelService;
    @Autowired protected SecurityEnforcer securityEnforcer;
    @Autowired protected SecurityContextManager securityContextManager;
    @Autowired protected TaskService taskService;
    @Autowired @Qualifier("cacheRepositoryService") protected RepositoryService cacheRepositoryService;
    @Autowired protected ScriptingActionExecutorRegistry actionExecutorRegistry;
    @Autowired protected MidpointFunctions midpointFunctions;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected MatchingRuleRegistry matchingRuleRegistry;
    @Autowired protected SchemaHelper schemaHelper;

    private String optionsSuffix(ModelExecuteOptions options) {
        return options.notEmpty() ? " " + options : "";
    }

    String drySuffix(boolean dry) {
        return dry ? " (dry run)" : "";
    }

    String optionsSuffix(ModelExecuteOptions options, boolean dry) {
        return optionsSuffix(options) + drySuffix(dry);
    }

    protected String exceptionSuffix(Throwable t) {
        return t != null ? " (error: " + t.getClass().getSimpleName() + ": " + t.getMessage() + ")" : "";
    }

    Throwable processActionException(Throwable e, String actionName, PrismValue value, ExecutionContext context) throws ScriptExecutionException {
        if (context.isContinueOnAnyError()) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute action '{}' on {}: {}", e,
                    actionName, value, e.getMessage());
            return e;
        } else {
            throw new ScriptExecutionException("Couldn't execute action '" + actionName + "' on " + value + ": " + e.getMessage(), e);
        }
    }

    void checkRootAuthorization(ExecutionContext context, OperationResult globalResult, String actionName)
            throws ScriptExecutionException {
        if (context.isPrivileged()) {
            return;
        }
        try {
            securityEnforcer.authorize(AuthorizationConstants.AUTZ_ALL_URL, null, AuthorizationParameters.EMPTY, null, context.getTask(), globalResult);
        } catch (SecurityViolationException | SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException e) {
            throw new ScriptExecutionException("You are not authorized to execute '" + actionName + "' action.");
        }
    }

    @FunctionalInterface
    public interface ItemProcessor {
        void process(PrismValue value, PipelineItem item, OperationResult result) throws ScriptExecutionException, CommonException;
    }

    @FunctionalInterface
    public interface ConsoleFailureMessageWriter {
        void write(PrismValue value, @NotNull Throwable exception);
    }

    void iterateOverItems(PipelineData input, ExecutionContext context, OperationResult globalResult,
            ItemProcessor itemProcessor, ConsoleFailureMessageWriter writer)
            throws ScriptExecutionException {

        for (PipelineItem item : input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);

            context.checkTaskStop();
            long started;
            if (value instanceof PrismObjectValue) {
                started = operationsHelper.recordStart(context, asObjectType(value));
            } else {
                started = 0;
            }
            try {
                itemProcessor.process(value, item, result);
                if (value instanceof PrismObjectValue) {
                    operationsHelper.recordEnd(context, asObjectType(value), started, null);
                }
            } catch (Throwable ex) {
                if (value instanceof PrismObjectValue) {
                    operationsHelper.recordEnd(context, asObjectType(value), started, ex);
                }
                Throwable exception = processActionException(ex, getActionName(), value, context);
                writer.write(value, exception);
            }
            operationsHelper.trimAndCloneResult(result, item.getResult());
        }
    }

    private ObjectType asObjectType(PrismValue value) {
        return (ObjectType) ((PrismObjectValue) value).asObjectable();
    }

    String getDescription(PrismValue value) {
        if (value instanceof PrismObjectValue<?>) {
            return asObjectType(value).asPrismObject().toString();
        } else {
            return value.toHumanReadableString();
        }
    }

    abstract String getActionName();
}
