package org.motechproject.tasks.service;

import org.motechproject.tasks.domain.ActionEvent;
import org.motechproject.tasks.domain.ActionParameter;
import org.motechproject.tasks.domain.MethodCallManner;
import org.motechproject.tasks.ex.TaskHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Utility class used by {@link TaskTriggerHandler} to construct a list of parameter
 * types of the method in the correct order.
 *
 * @see TaskTriggerHandler
 */
class MethodHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandler.class);

    private Class[] classes = new Class[0];
    private Object[] objects = new Object[0];

    public MethodHandler(ActionEvent action, Map<String, Object> parameters) throws TaskHandlerException {
        if (action != null) {
            SortedSet<ActionParameter> actionParameters = action.getActionParameters();

            if (null == action.getServiceMethodCallManner()) {
                LOGGER.debug("Method call manner for method {}::{} not specified. Using default (named parameters)",
                        action.getServiceInterface(), action.getServiceMethod());
                initForNamedParametersCall(parameters, actionParameters);
            } else if (MethodCallManner.NAMED_PARAMETERS == action.getServiceMethodCallManner()) {
                initForNamedParametersCall(parameters, actionParameters);
            } else if (MethodCallManner.MAP == action.getServiceMethodCallManner()) {
                initForMapCall(parameters);
            }
        }
    }

    private void initForNamedParametersCall(Map<String, Object> parameters, SortedSet<ActionParameter> actionParameters) {
        if (!actionParameters.isEmpty()) {
            classes = new Class[parameters.size()];
            objects = new Object[parameters.size()];

            for (ActionParameter actionParameter : actionParameters) {
                Object obj = parameters.get(actionParameter.getKey());
                Integer idx = actionParameter.getOrder();

                objects[idx] = obj;

                if (obj instanceof Map) {
                    classes[idx] = Map.class;
                } else if (obj instanceof List) {
                    classes[idx] = List.class;
                } else if (obj != null) {
                    classes[idx] = obj.getClass();
                } else {
                    classes[idx] = Object.class;
                }
            }
        } else {
            classes = new Class[0];
            objects = new Object[0];
        }
    }

    private void initForMapCall(Map<String, Object> parameters) {
        classes = new Class[] { Map.class };
        objects = new Object[] { parameters };
    }

    public Class[] getClasses() {
        return Arrays.copyOf(classes, classes.length);
    }

    public Object[] getObjects() {
        return Arrays.copyOf(objects, objects.length);
    }
}
