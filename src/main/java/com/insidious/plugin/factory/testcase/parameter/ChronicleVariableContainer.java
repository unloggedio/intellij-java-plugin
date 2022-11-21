package com.insidious.plugin.factory.testcase.parameter;

import com.insidious.plugin.pojo.Parameter;
import net.openhft.chronicle.map.ChronicleMap;

import java.util.Collection;

public class ChronicleVariableContainer {
    final ChronicleMap<Long, Parameter> parameterMap;

    public ChronicleVariableContainer(ChronicleMap<Long, Parameter> parameterMap) {
        this.parameterMap = parameterMap;
    }


    public void add(Parameter parameter) {
        long value = parameter.getValue();
        Parameter byValue = parameterMap.get(value);
        if (byValue == null) {
            if (parameter.getProb() != null) {
                parameterMap.put(parameter.getProb().getValue(), parameter);
            } else {
                parameterMap.put(parameter.getValue(), parameter);
            }
            return;
        }
        if (byValue == parameter) {
            // literally the same value
            return;
        }
        if (parameter.getProb() != null) {

//            if (byValue.getName() != null && byValue.getName().equals(parameter.getName())) {
//                byte[] newSerializedValue = parameter.getProb().getSerializedValue();
//                if (newSerializedValue == null || newSerializedValue.length == 0) {
//                    return;
//                }
//                byte[] existingSerializedValue = byValue.getProb().getSerializedValue();
//                if (existingSerializedValue == null || existingSerializedValue.length == 0) {
//                    byValue.setProb(parameter.getProb());
//                } else if (byValue.getProb().getNanoTime() < parameter.getProb().getNanoTime()) {
//                    byValue.setProb(parameter.getProb());
//                }
//            } else {
            parameterMap.put(parameter.getProb()
                    .getValue(), parameter);
//            }

        }
    }

    public Parameter getParametersById(long value) {
        return this.parameterMap.get(value);
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
        Parameter parameter = this.parameterMap.get(eventValue);
        if (parameter == null) {
            parameter = new Parameter(eventValue);
            return parameter;
        }
        return parameter;
    }


}
