package com.insidious.plugin.factory.testcase.parameter;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.ClassTypeUtils;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ParameterFactory {

    private final static Logger logger = LoggerUtil.getInstance(ParameterFactory.class);


    /**
     * createParameter needs to find
     * - name of the valueId
     * - type of the value
     */
    public
    static Parameter createReturnValueParameter(
            // the value id to be build for it located at this index in the dataEvents
            final int eventIndex,
            ReplayData replayData,
            String expectedParameterType
    ) {

//        logger.warn("Create object from index [" + eventIndex + "] - ParamIndex" + paramIndex);
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);


        DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
        final EventType eventType = probeInfo.getEventType();

        Parameter parameter = createParameterInternal(event, eventIndex, replayData, expectedParameterType);
        if (parameter.getType().equals("V")) {
            return parameter;
        }
        List<String> typeHierarchy = replayData.buildHierarchyFromTypeName(parameter.getType());

        if (typeHierarchy.size() == 0) {
            logger.warn("[1] failed to build type hierarchy for object [" + event + "]");
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
        } else if (probeInfo.getEventType() == EventType.METHOD_NORMAL_EXIT) {
            direction = -1; // go forward from current event
            callStackSearchLevel = 0; // we want something in the current method only
        }

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
                    replayData.getClassInfo(historyEventProbe.getClassId());
            MethodInfo methodInfoLocal = replayData.getMethodInfo(historyEventProbe.getMethodId());


            switch (historyEventProbe.getEventType()) {
                case CALL:
                    // this value has no name in this direction, maybe we can use the name of the
                    // argument it is passed as

                    // but if this is a call to a third party sdk, then we dont know the
                    // argument name

//                    if (callStack == 0 && direction == -1) {
//                        return parameter;
//                    }
                    break;
                case METHOD_NORMAL_EXIT:
                    callStack += direction;
                    break;

                case METHOD_ENTRY:
                    callStack -= direction;
                    break;

                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case LOCAL_LOAD:
                case LOCAL_STORE:
                case GET_INSTANCE_FIELD_RESULT:
                case GET_INSTANCE_FIELD:
                case PUT_INSTANCE_FIELD_VALUE:
                case PUT_INSTANCE_FIELD:
                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }
                    String fieldType = historyEventProbe.getAttribute("Type", "V");


                    String simpleClassName = ClassTypeUtils.getDottedClassName(fieldType);
                    if (typeHierarchy.contains(simpleClassName)) {

                        LoggerUtil.logEvent("SearchObjectName3", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);

                        String variableName = ClassTypeUtils.getVariableNameFromProbe(historyEventProbe, null);
                        parameter.setName(variableName);
                        parameter.setType(simpleClassName);
                        return parameter;
                    } else {
                        logger.info("");
                    }
                    break;
            }
        }


        return parameter;
    }


    /**
     * createParameter needs to find
     * - name of the valueId
     * - type of the value
     */

    private static Parameter createParameterInternal(
            DataEventWithSessionId event,
            int eventIndex,
            ReplayData replayData,
            String expectedParameterType
    ) {
        if (expectedParameterType != null) {
            expectedParameterType = ClassTypeUtils.getDottedClassName(expectedParameterType);
        }


        String eventProbeIdString = String.valueOf(event.getDataId());
        String eventValueString = String.valueOf(event.getValue());
        DataInfo probeInfo = replayData.getProbeInfoMap().get(eventProbeIdString);
        ObjectInfo objectInfo = replayData.getObjectInfoMap().get(eventValueString);
//        Set<String> typeHierarchy = new HashSet<>();


        Parameter parameter = new Parameter();
        parameter.setProb(event);
        parameter.setIndex(eventIndex);

        parameter.setProbeInfo(probeInfo);

        if (objectInfo != null) {
            TypeInfo receiverParameterTypeInfo = replayData.getTypeInfo(objectInfo.getTypeId());
            List<String> typeHierarchyFromReceiverTypeList = replayData.buildHierarchyFromType(
                    receiverParameterTypeInfo);
            assert typeHierarchyFromReceiverTypeList.size() != 0;
            parameter.setType(typeHierarchyFromReceiverTypeList.get(0));
//            typeHierarchy.addAll(typeHierarchyFromReceiverTypeList);

        }

        String finalIdentifiedType = getTypeForValueAtProbeIndex(event, eventIndex, replayData, probeInfo);

        parameter.setType(finalIdentifiedType);

        if (expectedParameterType != null) {
            if (finalIdentifiedType == null) {
                parameter.setType(ClassTypeUtils.getDottedClassName(expectedParameterType));
            } else if (!expectedParameterType.equals(finalIdentifiedType)) {
                logger.warn("final type does not matched expected type: " + expectedParameterType + " - " + finalIdentifiedType);
            }
        }

//        if (typeHierarchy.size() == 0) {
//            typeHierarchy = new HashSet<>(replayData.buildHierarchyFromTypeName(finalIdentifiedType));
//        }
//
//
//        if (typeHierarchy.size() == 0) {
//            logger.warn("[2] failed to build type hierarchy for object [" + event + "]");
//        }


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
        String paramName = ClassTypeUtils.getVariableNameFromProbe(probeInfo, null);
        if (paramName != null) {
            parameter.setName(paramName);
            return parameter;
        }

        if (parameter.getType() == null) {
            logger.warn("cannot identify parameter type: " + parameter);
        }

        return parameter;

    }

    private static String getTypeForValueAtProbeIndex(
            DataEventWithSessionId event,
            int eventIndex,
            ReplayData replayData,
            DataInfo probeInfo) {
        ScanRequest scanRequest = new ScanRequest(
                new ScanResult(eventIndex, 0), ScanRequest.ANY_STACK, DirectionType.FORWARDS);

        String typeFromProbe = probeInfo.getAttribute("Type", null);
        if (typeFromProbe != null) {
            return ClassTypeUtils.getDottedClassName(typeFromProbe);
        }

        AtomicReference<String> identifiedType = new AtomicReference<>();
        scanRequest.addListener(event.getValue(), index -> {
            DataEventWithSessionId matchedEvent = replayData.getDataEvents().get(index);
            DataInfo matchedEventProbe = replayData.getProbeInfo(matchedEvent.getDataId());
            switch (matchedEventProbe.getEventType()) {
                case LOCAL_LOAD:
                case LOCAL_STORE:
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case GET_INSTANCE_FIELD_RESULT:

                    String typeIdentified = probeInfo.getAttribute("Type", null);
                    if (typeIdentified != null) {
                        identifiedType.set(typeIdentified);
                        break;
                    }
                    scanRequest.matchUntil(EventType.LABEL);
                    break;
                default:

                    break;
            }
        });

//        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
//        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);
        replayData.eventScan(scanRequest);
        String finalIdentifiedType = identifiedType.get();
        return finalIdentifiedType;
    }


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
            // preferably dotted type name, like com.package.name, instead of Lcom/package/name;;
            String expectedParameterType
    ) {

//        logger.warn("Create object from index [" + eventIndex + "] - ParamIndex" + paramIndex);
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);


        DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
        Parameter parameter = createParameterInternal(event, eventIndex, replayData, expectedParameterType);


        if (parameter.getType().equals("V")) {
            return parameter;
        }

        EventType eventType = probeInfo.getEventType();
        List<String> typeHierarchy = replayData.buildHierarchyFromTypeName(parameter.getType());
        if (parameter.getName() != null) {
            return parameter;
        }

        int callStackSearchLevel = 0;

        int direction = 1;
        if (
                eventType == EventType.CALL_RETURN ||
                        eventType == EventType.NEW_OBJECT_CREATED ||
                        eventType == EventType.METHOD_OBJECT_INITIALIZED
        ) {
            direction = -1; // go forward from current event
            callStackSearchLevel = -1; // we want something in the caller method
        } else if (
                eventType == EventType.METHOD_NORMAL_EXIT
        ) {
            direction = -1; // go forward from current event
            callStackSearchLevel = 0; // we want something in the current method only
        } else if (
                eventType == EventType.METHOD_PARAM
        ) {
            direction = 1; // go backward from current event
            callStackSearchLevel = -1; // we want something in the caller method
        }


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
                    replayData.getClassInfo(historyEventProbe.getClassId());
            MethodInfo methodInfoLocal = replayData.getMethodInfo(historyEventProbe.getMethodId());


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
                    if (eventType == EventType.METHOD_OBJECT_INITIALIZED
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
                        LoggerUtil.logEvent("SearchObjectName", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);
                        parameter.setName(newVariableInstanceName);
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
                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }
                    String fieldType = historyEventProbe.getAttribute("Type", "V");


                    if (!fieldType.startsWith("L") || typeHierarchy.contains(fieldType)) {

                        LoggerUtil.logEvent("SearchObjectName3", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);

                        String variableName = ClassTypeUtils.getVariableNameFromProbe(probeInfo, null);
                        parameter.setName(variableName);
                        parameter.setType(fieldType);
                        return parameter;
                    }
                    break;
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
    static Parameter createMethodArgumentParameter(
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
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);


        DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
        Parameter parameter = createParameterInternal(event, eventIndex, replayData, expectedParameterType);


        if (parameter.getType() == null || parameter.getType().equals("V")) {
            return parameter;
        }

        List<String> typeHierarchy = replayData.buildHierarchyFromTypeName(parameter.getType());

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
            DataInfo historyEventProbe = replayData.getProbeInfo(historyEvent.getDataId());
            ClassInfo currentClassInfo =
                    replayData.getClassInfo(historyEventProbe.getClassId());
            MethodInfo methodInfoLocal = replayData.getMethodInfo(historyEventProbe.getMethodId());


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
                    String typeNameRaw = ClassTypeUtils.getDescriptorName(typeName);
                    String newVariableInstanceName = ClassTypeUtils.createVariableName(typeNameRaw);


                    if (parameter.getType().contains(typeName)) {
//                        logger.warn("Found name: " + newVariableInstanceName);
                        LoggerUtil.logEvent("SearchObjectName", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);
                        parameter.setName(newVariableInstanceName);
//                        return parameter;
                    }
                    break;
                case LOCAL_LOAD:
                case LOCAL_STORE:
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case GET_INSTANCE_FIELD:
                case GET_INSTANCE_FIELD_RESULT:
//                case PUT_INSTANCE_FIELD:
//                case PUT_INSTANCE_FIELD_VALUE:
                case OBJECT_CONSTANT_LOAD:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }
                    if (paramIndex > 0) {
                        paramIndex -= 1;
                        continue;
                    }
                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }
                    String fieldType = historyEventProbe.getAttribute("Type", "V");
                    if (fieldType.startsWith("[")) {
                        fieldType = fieldType.substring(1);
                    }


                    if (!fieldType.startsWith("L") ||
                            typeHierarchy.contains(ClassTypeUtils.getDottedClassName(fieldType))) {
                        LoggerUtil.logEvent("SearchObjectName3", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);
                        String variableName = ClassTypeUtils.getVariableNameFromProbe(historyEventProbe, null);
                        if (variableName != null) {
                            parameter.setName(variableName);
                            parameter.setType(ClassTypeUtils.getDottedClassName(fieldType));
                            return parameter;
                        } else {
                            break;
                        }
                    }
                    break;
            }
            if (parameter.getName() != null) {
                break;
            }
        }

        // we are potentially already invoking another call on this object for
        // which we wonted to get a name for., the return value has no name, and
        // is being used to invoke another function directly, so we can stop the
        // search for a name

        // note 2: if we were looking for a value which was a METHOD_PARAM then
        // we also have an option to look ahead for the parameters name where it
        // would have been potentially used


        ScanRequest scanRequest = new ScanRequest(new ScanResult(eventIndex, 0), 0,
                DirectionType.FORWARDS);
        replayData.eventScan(scanRequest);

        logger.warn("switching direction of search for a method param name");
        direction = -1;
        callStackSearchLevel = 0;
        callStack = 0;
        paramIndex = 0;


        for (int i = eventIndex + direction; i < dataEventCount && i > -1; i += direction) {
            DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
            DataInfo historyEventProbe = replayData.getProbeInfo(historyEvent.getDataId());
            ClassInfo currentClassInfo = replayData.getClassInfo(historyEventProbe.getClassId());
            MethodInfo methodInfoLocal = replayData.getMethodInfo(historyEventProbe.getMethodId());


            switch (historyEventProbe.getEventType()) {
                case CALL:
                    // this value has no name in this direction, maybe we can use the name of the
                    // argument it is passed as

                    // but if this is a call to a third party sdk, then we dont know the
                    // argument name

                    if (callStack < 0) {
                        return parameter;
                    }

                    break;
                case METHOD_NORMAL_EXIT:
                    callStack += direction;
                    break;

                case METHOD_ENTRY:
                    if (probeInfo.getEventType() == EventType.METHOD_OBJECT_INITIALIZED
                            && callStack == callStackSearchLevel) {

                        // the scenario where a newly constructed objects name was not found
                        // because it was created by a third party package where we do not have
                        // probes
                        return parameter;
                    }
                    callStack -= direction;
                    break;

                case LOCAL_LOAD:
                case LOCAL_STORE:
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case GET_INSTANCE_FIELD:
                case PUT_INSTANCE_FIELD:
                case OBJECT_CONSTANT_LOAD:
                    if (callStack != callStackSearchLevel) {
                        continue;
                    }

                    String fieldType = historyEventProbe.getAttribute("Type", "V");

                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }

                    if (!fieldType.startsWith("L") || typeHierarchy.contains(ClassTypeUtils.getDottedClassName(fieldType))) {
                        LoggerUtil.logEvent("SearchObjectName6", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);

                        String variableName = ClassTypeUtils.getVariableNameFromProbe(historyEventProbe,
                                parameter.getName());
                        parameter.setName(variableName);
                        parameter.setType(ClassTypeUtils.getDottedClassName(fieldType));
                        return parameter;
                    }
                    break;
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
    static Parameter createParameterByCallArgument(
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
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);
        Parameter parameter = createParameterInternal(event, eventIndex, replayData, expectedParameterType);

        if (parameter.getType() == null) {
            logger.warn("failed to identify type of parameter: " + parameter);
            return parameter;
        }

        if (parameter.getType().equals("V")) {
            return parameter;
        }

        List<String> typeHierarchy = replayData.buildHierarchyFromTypeName(parameter.getType());


        int callStackSearchLevel = 0;
        int direction = 1;


        int callStack = 0;
        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        int dataEventCount = dataEvents.size();
        for (int i = eventIndex + direction; i < dataEventCount && i > -1; i += direction) {

            DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
            DataInfo historyEventProbe = replayData.getProbeInfo(historyEvent.getDataId());
            ClassInfo currentClassInfo =
                    replayData.getClassInfo(historyEventProbe.getClassId());
            MethodInfo methodInfoLocal = replayData.getMethodInfo(historyEventProbe.getMethodId());


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
                    String typeNameRaw = ClassTypeUtils.getDescriptorName(typeName);
                    String newVariableInstanceName = ClassTypeUtils.createVariableName(typeNameRaw);


                    if (parameter.getType().contains(typeName)) {
//                        logger.warn("Found name: " + newVariableInstanceName);
                        LoggerUtil.logEvent("SearchObjectName", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);
                        parameter.setName(newVariableInstanceName);
                        return parameter;
                    }
                    break;
                case LOCAL_LOAD:
                case LOCAL_STORE:
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
                    if (historyEvent.getValue() != event.getValue()) {
                        continue;
                    }
                    String fieldType = historyEventProbe.getAttribute("Type", "V");


                    if (!fieldType.startsWith("L") || typeHierarchy.contains(ClassTypeUtils.getDottedClassName(fieldType))) {
                        LoggerUtil.logEvent("SearchObjectName3", callStack, i,
                                historyEvent, historyEventProbe, currentClassInfo, methodInfoLocal);
                        String variableName = ClassTypeUtils.getVariableNameFromProbe(historyEventProbe, null);
                        parameter.setName(variableName);
                        parameter.setType(ClassTypeUtils.getDottedClassName(fieldType));
                        return parameter;
                    }
                    break;
            }
        }


        return parameter;
    }


    public static Parameter createStringByName(String s) {
        Parameter parameter = new Parameter();
        parameter.setName(s);
        parameter.setType("java.lang.String");
        return parameter;
    }

    public static Parameter createStringByType(String s) {
        Parameter parameter = new Parameter();
        parameter.setType(s);
        return parameter;
    }
}
