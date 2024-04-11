package com.insidious.plugin.factory.testcase.parameter;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import net.openhft.chronicle.map.ChronicleMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ChronicleVariableContainer {
    private static final Logger logger = LoggerUtil.getInstance(ChronicleVariableContainer.class);
    //    final ChronicleMap<Long, Parameter> parameterMap;
    final Map<Long, Parameter> parameterMap;

    public ChronicleVariableContainer(ChronicleMap<Long, Parameter> parameterMap) {
        this.parameterMap = new HashMap<>();
    }


    public void add(Parameter parameter) {
        long value = parameter.getValue();

        Parameter byValue = parameterMap.get(value);
        DataEventWithSessionId parameterProb = parameter.getProb();
        if (byValue == null) {
            try {
                if (parameterProb != null && parameterProb.getValue() != 0) {
                    parameterMap.put(parameterProb.getValue(), parameter);
                } else {
                    parameterMap.put(parameter.getValue(), parameter);
                }

            } catch (IllegalArgumentException iae) {
                // index is full
                logger.warn("parameter index is full", iae);
            } finally {
                if (parameterMap.size() > 45_000) {
                    parameterMap.clear();
                }
            }
            return;
        }
        if (byValue == parameter) {
            // literally the same value
            return;
        }
        if (parameterProb != null) {
            try {
                if (parameterProb.getValue() != 0) {
                    parameterMap.put(parameterProb.getValue(), parameter);
                } else {
                    parameterMap.put(parameter.getValue(), parameter);
                }
            } catch (IllegalArgumentException iae) {
                // index is full
                logger.warn("index is full", iae);
            } finally {
                if (parameterMap.size() > 45_000) {
                    parameterMap.clear();
                }
            }

        }
    }


    public int count() {
        return parameterMap.values()
                .size();
    }

    public Collection<Parameter> all() {
        return parameterMap.values();
    }

    public Parameter getParameterByValue(long eventValue) {
        if (eventValue == 0) {
            return new Parameter(eventValue);
        }
//        if (parameterMap.isClosed()) {
//            return new Parameter(eventValue);
//        }
        Parameter parameter = this.parameterMap.get(eventValue);
        if (parameter == null) {
            parameter = new Parameter(eventValue);
            return parameter;
        }
        return parameter;
    }

    public Parameter getParameterByValueUsing(long eventValue, Parameter parameter) {
        if (eventValue == 0) {
            return new Parameter(eventValue);
        }
        parameter = this.parameterMap.get(eventValue);
        if (parameter == null) {
            parameter = new Parameter(eventValue);
            return parameter;
        }
        return parameter;
    }


    public void close() {
//        parameterMap.close();
    }
}
