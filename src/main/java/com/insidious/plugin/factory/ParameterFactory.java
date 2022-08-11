package com.insidious.plugin.factory;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ParameterFactory {

    private final static Logger logger = LoggerUtil.getInstance(ParameterFactory.class);

    /**
     * createParameter needs to find
     * - name of the valueId
     * - type of the value
     */
    public
    static Parameter createParameter
    (
            // the value id to be build for it located at this index in the dataEvents
            final int eventIndex,
            ReplayData replayData,
            // if the parameter we are creating was a method argument, then we need extra
            // information to identify the position of the argument, else we will capture the
            // first variable of matching type and end up using the same value
            int paramIndex,
            String expectedParameterType
    ) {

//        logger.warn("Create object from index [" + eventIndex + "] - ParamIndex" + paramIndex);
        Parameter parameter = new Parameter();
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);

        parameter.setProb(event);
        parameter.setIndex(eventIndex);


        String eventProbeIdString = String.valueOf(event.getDataId());
        String eventValueString = String.valueOf(event.getValue());
        DataInfo probeInfo = replayData.getProbeInfoMap().get(eventProbeIdString);
        ObjectInfo objectInfo = replayData.getObjectInfoMap().get(eventValueString);
        parameter.setProbeInfo(probeInfo);
        Set<String> typeHierarchy = new HashSet<>();

        TypeInfo typeInfo = null;

        if (expectedParameterType != null) {
            if (expectedParameterType.startsWith("L")) {
                expectedParameterType = expectedParameterType.substring(1,
                        expectedParameterType.length() - 1).replace('/', '.');
                String finalExpectedParameterType = expectedParameterType;
                List<TypeInfo> matchedTypeInfo = replayData.getTypeInfoMap()
                        .values().stream()
                        .filter(e -> e.getTypeNameFromClass().equals(finalExpectedParameterType))
                        .collect(Collectors.toList());
                if (matchedTypeInfo.size() == 0) {
                    logger.warn("matched type from suggested nothing [" + finalExpectedParameterType + "]");
                } else {
                    typeInfo = matchedTypeInfo.get(0);
                }
            } else {
                parameter.setType(expectedParameterType);
                typeHierarchy.add(expectedParameterType);
            }
        }

        if (typeInfo == null) {
            if (objectInfo != null) {
//                logger.warn("type info is null: " + objectInfo.getObjectId() + ": -> " + objectInfo.getTypeId());
            }
        } else {
            parameter.setType(ClassTypeUtils.getBasicClassName(typeInfo.getTypeNameFromClass()));
            typeHierarchy = ClassTypeUtils.buildHierarchyFromType(replayData, typeInfo);
        }

        if (objectInfo != null) {
            TypeInfo receiverParameterTypeInfo = replayData.getTypeInfoMap().get(
                    String.valueOf(objectInfo.getTypeId())
            );
            Set<String> typeHierarchyFromReceiverType = ClassTypeUtils.buildHierarchyFromType(replayData,
                    receiverParameterTypeInfo);
            parameter.setType(ClassTypeUtils.getBasicClassName(receiverParameterTypeInfo.getTypeNameFromClass()));
            typeHierarchy.addAll(typeHierarchyFromReceiverType);

        }


        if (typeHierarchy.size() == 0) {
            logger.warn("failed to build type hierarchy for object [" + event + "]");
            return parameter;
        }


        Object probeValue = replayData.getValueByObjectId(parameter.getProb());
        parameter.setValue(probeValue);


        if (objectInfo == null) {
            String variableTypeName = probeInfo.getAttribute("Type", probeInfo.getValueDesc().getString());
            parameter.setType(variableTypeName);
            if (Objects.equals(variableTypeName, "V")) {
                parameter.setValue(null);
                return parameter;
            }
        }

        if (probeInfo.getEventType() == EventType.GET_INSTANCE_FIELD_RESULT) {
            String name = replayData.getProbeInfoMap()
                    .get(String.valueOf(replayData.getDataEvents().get(eventIndex + 1).getDataId()))
                    .getAttribute("FieldName", null);
            parameter.setName(name);
            return parameter;
        }

        if (probeInfo.getEventType() == EventType.GET_STATIC_FIELD) {
            String name = replayData.getProbeInfoMap()
                    .get(String.valueOf(replayData.getDataEvents().get(eventIndex + 1).getDataId()))
                    .getAttribute("FieldName", null);
            parameter.setName(name);
            return parameter;
        }

        if (probeInfo.getEventType() == EventType.LOCAL_LOAD) {
            String name = replayData.getProbeInfoMap()
                    .get(String.valueOf(replayData.getDataEvents().get(eventIndex + 1).getDataId()))
                    .getAttribute("Name", null);
            parameter.setName(name);
            return parameter;
        }

        int callStackSearchLevel = 0;

        int direction = 1;
        if (
                probeInfo.getEventType() == EventType.CALL_RETURN ||
                        probeInfo.getEventType() == EventType.NEW_OBJECT_CREATED ||
                        probeInfo.getEventType() == EventType.METHOD_OBJECT_INITIALIZED
        ) {
            direction = -1; // go forward from current event
            callStackSearchLevel = -1; // we want something in the caller method
        } else if (
                probeInfo.getEventType() == EventType.METHOD_NORMAL_EXIT
        ) {
            direction = -1; // go forward from current event
            callStackSearchLevel = 0; // we want something in the current method only
        } else if (
                probeInfo.getEventType() == EventType.METHOD_PARAM
        ) {
            direction = 1; // go backward from current event
            callStackSearchLevel = -1; // we want something in the caller method
        }
//
//        Long parameterValueId = 0L;
//        try {
//            parameterValueId = Long.valueOf((String) parameter.getValue());
//        } catch (NumberFormatException nfe) {
//
//        }

//        String subjectName = replayData.getNameForValue(parameterValueId);
//        parameter.setName(subjectName);

        int callStack = 0;
        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        int dataEventCount = dataEvents.size();
        for (int i = eventIndex + direction; i < dataEventCount
                && i > -1; i += direction) {
            DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
            DataInfo historyEventProbe = replayData.getProbeInfoMap().get(
                    String.valueOf(historyEvent.getDataId())
            );
            ClassInfo currentClassInfo =
                    replayData.getClassInfoMap().get(String.valueOf(historyEventProbe.getClassId()));
            MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(historyEventProbe.getMethodId()));


            switch (historyEventProbe.getEventType()) {
                case CALL:
                    // this value has no name in this direction, maybe we can use the name of the
                    // argument it is passed as

                    // but if this is a call to a third party sdk, then we dont know the
                    // argument name

                    if (callStack < 0 && direction == -1) {
                        return parameter;
                    }

                    // direction == 1 => going back
//                    if (callStack == callStackSearchLevel && direction == 1) {
//
//                        // this happens when we were looking back for a parameter name, but find
//                        // ourself inside another call, need to check this out again
//
//                        return parameter;
//                    }
                    break;
                case METHOD_NORMAL_EXIT:
                    callStack += direction;
                    break;

                case METHOD_ENTRY:
                    if (probeInfo.getEventType() == EventType.METHOD_OBJECT_INITIALIZED
                            && callStack == callStackSearchLevel) {

                        // the scenario where a newly construted objects name was not found
                        // because it was created by a third party package where we do not have
                        // probes
                        return parameter;
                    }
                    callStack -= direction;
                    break;

                case NEW_OBJECT_CREATED:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (paramIndex > 0) {
                        paramIndex -= 1;
                        continue;
                    }
                    ObjectInfo oInfo = replayData.getObjectInfoMap().get(String.valueOf(historyEvent.getValue()));
                    if (oInfo == null) {
                        logger.warn("object info is null [" + historyEvent.getValue() + "], gotta " +
                                "check");
                        break;
                    }
                    TypeInfo oTypeInfo = replayData.getTypeInfoMap().get(String.valueOf(oInfo.getTypeId()));
                    String typeName = oTypeInfo.getTypeNameFromClass();
                    String typeNameRaw = typeName.replaceAll("\\.", "/");
                    String newVariableInstanceName = ClassTypeUtils.createVariableName(typeNameRaw);


                    if (parameter.getType().contains(typeNameRaw)) {
//                        logger.warn("Found name: " + newVariableInstanceName);
                        logger.warn("[SearchObjectName1] #" + i + ", T=" + historyEvent.getNanoTime() +
                                ", P=" + historyEvent.getDataId() +
                                " [Stack:" + callStack + "]" +
                                " " + String.format("%25s", historyEventProbe.getEventType())
                                + " in " + String.format("%25s",
                                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                + ":" + historyEventProbe.getLine()
                                + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                + "  -> " + historyEventProbe.getAttributes());
                        parameter.setName(newVariableInstanceName);
                        return parameter;
                    }
                    break;
                case LOCAL_LOAD:
                case LOCAL_STORE:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }
                    String variableType = historyEventProbe.getAttribute("Type", "V");


                    // removing this if condition because this fails in the case of int vs
                    // Ljava/lang/Integer (implicit conversion by jvm). removing the if should
                    // be fine because we are also tracing the parameters by index (which was not
                    // there when the type check was initially added)
                    if (!variableType.startsWith("L")
                            || typeHierarchy.contains(variableType)) {
                        logger.warn("[SearchObjectName2] #" + i + ", T=" + historyEvent.getNanoTime() +
                                ", P=" + historyEvent.getDataId() +
                                " [Stack:" + callStack + "]" +
                                " " + String.format("%25s", historyEventProbe.getEventType())
                                + " in " + String.format("%25s",
                                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                + ":" + historyEventProbe.getLine()
                                + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                + "  -> " + historyEventProbe.getAttributes());
                        String variableName = historyEventProbe.getAttribute("Name", null);
                        parameter.setName(variableName);
                        parameter.setType(variableType);
                        return parameter;
                    }
                    break;
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case GET_INSTANCE_FIELD:
                case PUT_INSTANCE_FIELD:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (paramIndex > 0) {
                        paramIndex -= 1;
                        continue;
                    }
                    String fieldType = historyEventProbe.getAttribute("Type", "V");


                    if (!fieldType.startsWith("L") || typeHierarchy.contains(fieldType)) {
                        logger.warn("[SearchObjectName3] #" + i + ", T=" + historyEvent.getNanoTime() +
                                ", P=" + historyEvent.getDataId() +
                                " [Stack:" + callStack + "]" +
                                " " + String.format("%25s", historyEventProbe.getEventType())
                                + " in " + String.format("%25s",
                                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                + ":" + historyEventProbe.getLine()
                                + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                + "  -> " + historyEventProbe.getAttributes());
                        String variableName = historyEventProbe.getAttribute("FieldName", null);
                        parameter.setName(variableName);
                        parameter.setType(fieldType);
                        return parameter;
                    }
                    break;
            }
        }

        if (probeInfo.getEventType() == EventType.METHOD_PARAM) {
            // we are potentially already invoking another call on this object for
            // which we wonted to get a name for., the return value has no name, and
            // is being used to invoke another function directly, so we can stop the
            // search for a name

            // note 2: if we were looking for a value which was a METHOD_PARAM then
            // we also have an option to look ahead for the parameters name where it
            // would have been potentially used

            logger.warn("switching direction of search for a method param name");
            direction = -1;
            callStackSearchLevel = 0;
            callStack = 0;
            paramIndex = 0;


            for (int i = eventIndex + direction; i < dataEventCount
                    && i > -1; i += direction) {
                DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
                DataInfo historyEventProbe = replayData.getProbeInfoMap().get(
                        String.valueOf(historyEvent.getDataId())
                );
                ClassInfo currentClassInfo =
                        replayData.getClassInfoMap().get(String.valueOf(historyEventProbe.getClassId()));
                MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(historyEventProbe.getMethodId()));


                switch (historyEventProbe.getEventType()) {
                    case CALL:
                        // this value has no name in this direction, maybe we can use the name of the
                        // argument it is passed as

                        // but if this is a call to a third party sdk, then we dont know the
                        // argument name

                        if (callStack < 0) {
                            return parameter;
                        }

                        // direction == 1 => going back
//                    if (callStack == callStackSearchLevel && direction == 1) {
//
//                        // this happens when we were looking back for a parameter name, but find
//                        // ourself inside another call, need to check this out again
//
//                        return parameter;
//                    }
                        break;
                    case METHOD_NORMAL_EXIT:
                        callStack += direction;
                        break;

                    case METHOD_ENTRY:
                        if (probeInfo.getEventType() == EventType.METHOD_OBJECT_INITIALIZED
                                && callStack == callStackSearchLevel) {

                            // the scenario where a newly construted objects name was not found
                            // because it was created by a third party package where we do not have
                            // probes
                            return parameter;
                        }
                        callStack -= direction;
                        break;

                    case NEW_OBJECT_CREATED:
                        if (callStack != callStackSearchLevel) {
                            continue;
                        }
                        if (paramIndex > 0) {
                            paramIndex -= 1;
                            continue;
                        }
                        ObjectInfo oInfo = replayData.getObjectInfoMap().get(String.valueOf(historyEvent.getValue()));
                        if (oInfo == null) {
                            logger.warn("object info is null [" + historyEvent.getValue() + "], gotta " +
                                    "check");
                            break;
                        }
                        TypeInfo oTypeInfo = replayData.getTypeInfoMap().get(String.valueOf(oInfo.getTypeId()));
                        String typeName = oTypeInfo.getTypeNameFromClass();
                        String typeNameRaw = typeName.replaceAll("\\.", "/");
                        String newVariableInstanceName =
                                ClassTypeUtils.createVariableName(typeNameRaw);


                        if (parameter.getType().contains(typeNameRaw)) {
//                            logger.warn("Found name: " + newVariableInstanceName);
                            logger.warn("[SearchObjectName4] #" + i + ", T=" + historyEvent.getNanoTime() +
                                    ", P=" + historyEvent.getDataId() +
                                    " [Stack:" + callStack + "]" +
                                    " " + String.format("%25s", historyEventProbe.getEventType())
                                    + " in " + String.format("%25s",
                                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                    + ":" + historyEventProbe.getLine()
                                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                    + "  -> " + historyEventProbe.getAttributes());

                            parameter.setName(newVariableInstanceName);
                            return parameter;
                        }
                        break;
                    case LOCAL_LOAD:
                    case LOCAL_STORE:
                        if (callStack != callStackSearchLevel) {
                            continue;
                        }
                        if (paramIndex > 0) {
                            paramIndex -= 1;
                            continue;
                        }
                        String variableType = historyEventProbe.getAttribute("Type", "V");


                        // removing this if condition because this fails in the case of int vs
                        // Ljava/lang/Integer (implicit conversion by jvm). removing the if should
                        // be fine because we are also tracing the parameters by index (which was not
                        // there when the type check was initially added)
                        if (!variableType.startsWith("L") || typeHierarchy.contains(variableType)) {

                            logger.warn("[SearchObjectName5] #" + i + ", T=" + historyEvent.getNanoTime() +
                                    ", P=" + historyEvent.getDataId() +
                                    " [Stack:" + callStack + "]" +
                                    " " + String.format("%25s", historyEventProbe.getEventType())
                                    + " in " + String.format("%25s",
                                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                    + ":" + historyEventProbe.getLine()
                                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                    + "  -> " + historyEventProbe.getAttributes());

                            String variableName = historyEventProbe.getAttribute("Name", null);
                            parameter.setName(variableName);
                            parameter.setType(variableType);
                            return parameter;
                        }
                        break;
                    case GET_STATIC_FIELD:
                    case PUT_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                    case PUT_INSTANCE_FIELD:
                        if (callStack != callStackSearchLevel) {
                            continue;
                        }
                        if (paramIndex > 0) {
                            paramIndex -= 1;
                            continue;
                        }
                        String fieldType = historyEventProbe.getAttribute("Type", "V");


                        if (!fieldType.startsWith("L") || typeHierarchy.contains(fieldType)) {
                            logger.warn("[SearchObjectName6] #" + i + ", T=" + historyEvent.getNanoTime() +
                                    ", P=" + historyEvent.getDataId() +
                                    " [Stack:" + callStack + "]" +
                                    " " + String.format("%25s", historyEventProbe.getEventType())
                                    + " in " + String.format("%25s",
                                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                    + ":" + historyEventProbe.getLine()
                                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                    + "  -> " + historyEventProbe.getAttributes());

                            String variableName = historyEventProbe.getAttribute("FieldName", null);
                            parameter.setName(variableName);
                            parameter.setType(fieldType);
                            return parameter;
                        }
                        break;
                }
            }


        }


        return parameter;
    }


    /**
     * createParameter needs to find
     * - name of the valueId
     * - type of the value
     */
    public
    static Parameter createMethodArgumentParameter
    (
            // the value id to be build for it located at this index in the dataEvents
            final int eventIndex,
            ReplayData replayData,
            // if the parameter we are creating was a method argument, then we need extra
            // information to identify the position of the argument, else we will capture the
            // first variable of matching type and end up using the same value
            int paramIndex,
            String expectedParameterType
    ) {

//        logger.warn("Create object from index [" + eventIndex + "] - ParamIndex" + paramIndex);
        Parameter parameter = new Parameter();
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);

        parameter.setProb(event);
        parameter.setIndex(eventIndex);


        String eventProbeIdString = String.valueOf(event.getDataId());
        String eventValueString = String.valueOf(event.getValue());
        DataInfo probeInfo = replayData.getProbeInfoMap().get(eventProbeIdString);
        assert probeInfo.getEventType() == EventType.METHOD_PARAM;

        ObjectInfo objectInfo = replayData.getObjectInfoMap().get(eventValueString);
        parameter.setProbeInfo(probeInfo);
        Set<String> typeHierarchy = new HashSet<>();

        TypeInfo typeInfo = null;

        if (expectedParameterType != null) {
            if (expectedParameterType.startsWith("L")) {
                expectedParameterType = expectedParameterType.substring(1,
                        expectedParameterType.length() - 1).replace('/', '.');
                String finalExpectedParameterType = expectedParameterType;
                List<TypeInfo> matchedTypeInfo = replayData.getTypeInfoMap()
                        .values().stream()
                        .filter(e -> e.getTypeNameFromClass().equals(finalExpectedParameterType))
                        .collect(Collectors.toList());
                if (matchedTypeInfo.size() == 0) {
                    logger.warn("matched type from suggested nothing [" + finalExpectedParameterType + "]");
                } else {
                    typeInfo = matchedTypeInfo.get(0);
                }
            } else {
                parameter.setType(expectedParameterType);
                typeHierarchy.add(expectedParameterType);
            }
        }

        if (typeInfo == null) {
            if (objectInfo != null) {
//                logger.warn("type info is null: " + objectInfo.getObjectId() + ": -> " + objectInfo.getTypeId());
            }
        } else {
            parameter.setType(ClassTypeUtils.getBasicClassName(typeInfo.getTypeNameFromClass()));
            typeHierarchy = ClassTypeUtils.buildHierarchyFromType(replayData, typeInfo);
        }

        if (objectInfo != null) {
            TypeInfo receiverParameterTypeInfo = replayData.getTypeInfoMap().get(
                    String.valueOf(objectInfo.getTypeId())
            );
            Set<String> typeHierarchyFromReceiverType = ClassTypeUtils.buildHierarchyFromType(replayData,
                    receiverParameterTypeInfo);
            parameter.setType(ClassTypeUtils.getBasicClassName(receiverParameterTypeInfo.getTypeNameFromClass()));
            typeHierarchy.addAll(typeHierarchyFromReceiverType);

        }


        if (typeHierarchy.size() == 0) {
            logger.warn("failed to build type hierarchy for object [" + event + "]");
            return parameter;
        }


        Object probeValue = replayData.getValueByObjectId(parameter.getProb());
        parameter.setValue(probeValue);


        if (objectInfo == null) {
            String variableTypeName = probeInfo.getAttribute("Type", probeInfo.getValueDesc().getString());
            parameter.setType(variableTypeName);
            if (Objects.equals(variableTypeName, "V")) {
                parameter.setValue(null);
                return parameter;
            }
        }


        int callStackSearchLevel = 0;

        int direction = 1;

        direction = 1; // go backward from current event
        callStackSearchLevel = -1; // we want something in the caller method


        int callStack = 0;
        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        int dataEventCount = dataEvents.size();
        for (int i = eventIndex + direction; i < dataEventCount
                && i > -1; i += direction) {
            DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
            DataInfo historyEventProbe = replayData.getProbeInfoMap().get(
                    String.valueOf(historyEvent.getDataId())
            );
            ClassInfo currentClassInfo =
                    replayData.getClassInfoMap().get(String.valueOf(historyEventProbe.getClassId()));
            MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(historyEventProbe.getMethodId()));


            switch (historyEventProbe.getEventType()) {
                case CALL:
                    // this value has no name in this direction, maybe we can use the name of the
                    // argument it is passed as

                    // but if this is a call to a third party sdk, then we dont know the
                    // argument name

                    if (callStack < 0 && direction == -1) {
                        return parameter;
                    }


                    break;
                case METHOD_NORMAL_EXIT:
                    callStack += direction;
                    break;

                case METHOD_ENTRY:
                    callStack -= direction;
                    break;

                case NEW_OBJECT_CREATED:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (paramIndex > 0) {
                        paramIndex -= 1;
                        continue;
                    }
                    ObjectInfo oInfo = replayData.getObjectInfoMap().get(String.valueOf(historyEvent.getValue()));
                    if (oInfo == null) {
                        logger.warn("object info is null [" + historyEvent.getValue() + "], gotta " +
                                "check");
                        break;
                    }
                    TypeInfo oTypeInfo = replayData.getTypeInfoMap().get(String.valueOf(oInfo.getTypeId()));
                    String typeName = oTypeInfo.getTypeNameFromClass();
                    String typeNameRaw = typeName.replaceAll("\\.", "/");
                    String newVariableInstanceName = ClassTypeUtils.createVariableName(typeNameRaw);


                    if (parameter.getType().contains(typeNameRaw)) {
//                        logger.warn("Found name: " + newVariableInstanceName);
                        logger.warn("[SearchObjectName1] #" + i + ", T=" + historyEvent.getNanoTime() +
                                ", P=" + historyEvent.getDataId() +
                                " [Stack:" + callStack + "]" +
                                " " + String.format("%25s", historyEventProbe.getEventType())
                                + " in " + String.format("%25s",
                                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                + ":" + historyEventProbe.getLine()
                                + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                + "  -> " + historyEventProbe.getAttributes());
                        parameter.setName(newVariableInstanceName);
                        return parameter;
                    }
                    break;
                case LOCAL_LOAD:
                case LOCAL_STORE:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }
                    String variableType = historyEventProbe.getAttribute("Type", "V");


                    // removing this if condition because this fails in the case of int vs
                    // Ljava/lang/Integer (implicit conversion by jvm). removing the if should
                    // be fine because we are also tracing the parameters by index (which was not
                    // there when the type check was initially added)
                    if (!variableType.startsWith("L")
                            || typeHierarchy.contains(variableType)) {
                        logger.warn("[SearchObjectName2] #" + i + ", T=" + historyEvent.getNanoTime() +
                                ", P=" + historyEvent.getDataId() +
                                " [Stack:" + callStack + "]" +
                                " " + String.format("%25s", historyEventProbe.getEventType())
                                + " in " + String.format("%25s",
                                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                + ":" + historyEventProbe.getLine()
                                + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                + "  -> " + historyEventProbe.getAttributes());
                        String variableName = historyEventProbe.getAttribute("Name", null);
                        parameter.setName(variableName);
                        parameter.setType(variableType);
                        return parameter;
                    }
                    break;
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case GET_INSTANCE_FIELD:
                case PUT_INSTANCE_FIELD:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (paramIndex > 0) {
                        paramIndex -= 1;
                        continue;
                    }
                    String fieldType = historyEventProbe.getAttribute("Type", "V");


                    if (!fieldType.startsWith("L") || typeHierarchy.contains(fieldType)) {
                        logger.warn("[SearchObjectName3] #" + i + ", T=" + historyEvent.getNanoTime() +
                                ", P=" + historyEvent.getDataId() +
                                " [Stack:" + callStack + "]" +
                                " " + String.format("%25s", historyEventProbe.getEventType())
                                + " in " + String.format("%25s",
                                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                + ":" + historyEventProbe.getLine()
                                + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                + "  -> " + historyEventProbe.getAttributes());
                        String variableName = historyEventProbe.getAttribute("FieldName", null);
                        parameter.setName(variableName);
                        parameter.setType(fieldType);
                        return parameter;
                    }
                    break;
            }
        }

        if (probeInfo.getEventType() == EventType.METHOD_PARAM) {
            // we are potentially already invoking another call on this object for
            // which we wonted to get a name for., the return value has no name, and
            // is being used to invoke another function directly, so we can stop the
            // search for a name

            // note 2: if we were looking for a value which was a METHOD_PARAM then
            // we also have an option to look ahead for the parameters name where it
            // would have been potentially used

            logger.warn("switching direction of search for a method param name");
            direction = -1;
            callStackSearchLevel = 0;
            callStack = 0;
            paramIndex = 0;


            for (int i = eventIndex + direction; i < dataEventCount
                    && i > -1; i += direction) {
                DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
                DataInfo historyEventProbe = replayData.getProbeInfoMap().get(
                        String.valueOf(historyEvent.getDataId())
                );
                ClassInfo currentClassInfo =
                        replayData.getClassInfoMap().get(String.valueOf(historyEventProbe.getClassId()));
                MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(historyEventProbe.getMethodId()));


                switch (historyEventProbe.getEventType()) {
                    case CALL:
                        // this value has no name in this direction, maybe we can use the name of the
                        // argument it is passed as

                        // but if this is a call to a third party sdk, then we dont know the
                        // argument name

                        if (callStack < 0) {
                            return parameter;
                        }

                        // direction == 1 => going back
//                    if (callStack == callStackSearchLevel && direction == 1) {
//
//                        // this happens when we were looking back for a parameter name, but find
//                        // ourself inside another call, need to check this out again
//
//                        return parameter;
//                    }
                        break;
                    case METHOD_NORMAL_EXIT:
                        callStack += direction;
                        break;

                    case METHOD_ENTRY:
                        if (probeInfo.getEventType() == EventType.METHOD_OBJECT_INITIALIZED
                                && callStack == callStackSearchLevel) {

                            // the scenario where a newly construted objects name was not found
                            // because it was created by a third party package where we do not have
                            // probes
                            return parameter;
                        }
                        callStack -= direction;
                        break;

                    case NEW_OBJECT_CREATED:
                        if (callStack != callStackSearchLevel) {
                            continue;
                        }
                        if (paramIndex > 0) {
                            paramIndex -= 1;
                            continue;
                        }
                        ObjectInfo oInfo = replayData.getObjectInfoMap().get(String.valueOf(historyEvent.getValue()));
                        if (oInfo == null) {
                            logger.warn("object info is null [" + historyEvent.getValue() + "], gotta " +
                                    "check");
                            break;
                        }
                        TypeInfo oTypeInfo = replayData.getTypeInfoMap().get(String.valueOf(oInfo.getTypeId()));
                        String typeName = oTypeInfo.getTypeNameFromClass();
                        String typeNameRaw = typeName.replaceAll("\\.", "/");
                        String newVariableInstanceName =
                                ClassTypeUtils.createVariableName(typeNameRaw);


                        if (parameter.getType().contains(typeNameRaw)) {
//                            logger.warn("Found name: " + newVariableInstanceName);
                            logger.warn("[SearchObjectName4] #" + i + ", T=" + historyEvent.getNanoTime() +
                                    ", P=" + historyEvent.getDataId() +
                                    " [Stack:" + callStack + "]" +
                                    " " + String.format("%25s", historyEventProbe.getEventType())
                                    + " in " + String.format("%25s",
                                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                    + ":" + historyEventProbe.getLine()
                                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                    + "  -> " + historyEventProbe.getAttributes());

                            parameter.setName(newVariableInstanceName);
                            return parameter;
                        }
                        break;
                    case LOCAL_LOAD:
                    case LOCAL_STORE:
                        if (callStack != callStackSearchLevel) {
                            continue;
                        }
                        if (paramIndex > 0) {
                            paramIndex -= 1;
                            continue;
                        }
                        String variableType = historyEventProbe.getAttribute("Type", "V");


                        // removing this if condition because this fails in the case of int vs
                        // Ljava/lang/Integer (implicit conversion by jvm). removing the if should
                        // be fine because we are also tracing the parameters by index (which was not
                        // there when the type check was initially added)
                        if (!variableType.startsWith("L") || typeHierarchy.contains(variableType)) {

                            logger.warn("[SearchObjectName5] #" + i + ", T=" + historyEvent.getNanoTime() +
                                    ", P=" + historyEvent.getDataId() +
                                    " [Stack:" + callStack + "]" +
                                    " " + String.format("%25s", historyEventProbe.getEventType())
                                    + " in " + String.format("%25s",
                                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                    + ":" + historyEventProbe.getLine()
                                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                    + "  -> " + historyEventProbe.getAttributes());

                            String variableName = historyEventProbe.getAttribute("Name", null);
                            parameter.setName(variableName);
                            parameter.setType(variableType);
                            return parameter;
                        }
                        break;
                    case GET_STATIC_FIELD:
                    case PUT_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                    case PUT_INSTANCE_FIELD:
                        if (callStack != callStackSearchLevel) {
                            continue;
                        }
                        if (paramIndex > 0) {
                            paramIndex -= 1;
                            continue;
                        }
                        String fieldType = historyEventProbe.getAttribute("Type", "V");


                        if (!fieldType.startsWith("L") || typeHierarchy.contains(fieldType)) {
                            logger.warn("[SearchObjectName6] #" + i + ", T=" + historyEvent.getNanoTime() +
                                    ", P=" + historyEvent.getDataId() +
                                    " [Stack:" + callStack + "]" +
                                    " " + String.format("%25s", historyEventProbe.getEventType())
                                    + " in " + String.format("%25s",
                                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                                    + ":" + historyEventProbe.getLine()
                                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                                    + "  -> " + historyEventProbe.getAttributes());

                            String variableName = historyEventProbe.getAttribute("FieldName", null);
                            parameter.setName(variableName);
                            parameter.setType(fieldType);
                            return parameter;
                        }
                        break;
                }
            }


        }


        return parameter;
    }


}
