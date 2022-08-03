package com.insidious.plugin.factory;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCandidateMetadata {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);
    private final List<Parameter> parameters = new LinkedList<>();
    private String fullyQualifiedClassname;
    private String unqualifiedClassname;
    private String packageName;
    private String testMethodName;
    private Parameter testSubject;
    private Parameter returnParameter;
    private long callTimeNanoSecond;
    private String methodName;

    private static String getTestSubjectName(int eventIndex,
                                             MethodInfo methodInfo,
                                             ReplayData objectReplayData) {
        int callStack = 0;
        int direction = -1;
        ClassInfo classInfo = objectReplayData.getClassInfoMap().get(String.valueOf(methodInfo.getClassId()));
        String[] classNameParts = classInfo.getClassName().split("/");
        String potentialClassVariableInstanceName =
                lowerInstanceName(classNameParts[classNameParts.length - 1]);
        for (int i = eventIndex + direction; i < objectReplayData.getDataEvents().size() && i > -1; i += direction) {
            DataEventWithSessionId event = objectReplayData.getDataEvents().get(i);
            DataInfo probeInfo = objectReplayData.getProbeInfoMap().get(String.valueOf(event.getDataId()));
            switch (probeInfo.getEventType()) {
                case CALL:
                    callStack += 1;
                    break;
                case CALL_RETURN:
                    callStack -= 1;
                    break;
                case NEW_OBJECT_CREATED:
                    // pagination required
                    if (callStack == -1 &&
                            (i + direction > -1 &&
                                    i + direction < objectReplayData.getDataEvents().size())) {
                        DataInfo nextEventProbe = objectReplayData.getProbeInfoMap().get(
                                String.valueOf(objectReplayData.getDataEvents().get(i + direction).getDataId())
                        );
                        return nextEventProbe.getAttribute("Name", potentialClassVariableInstanceName);
                    }
                    break;
            }
        }
        return null;
    }

    public static TestCandidateMetadata create(
            List<String> typeHierarchy,
            MethodInfo methodInfo,
            long entryProbeDataId,
            ReplayData replayData
    ) throws APICallException {
        logger.warn("create test case metadata for types [" + typeHierarchy + "] -> entry probe " +
                "nano time: " + entryProbeDataId);
        TestCandidateMetadata metadata = new TestCandidateMetadata();

//        final String className = methodInfo.getClassName();
        String targetMethodName = methodInfo.getMethodName();

        if (targetMethodName.startsWith("lambda$")) {
            // this function is a transformation of the original user function by jvm and the
            // name looks like "lambda$ORIGINAL_NAME$<int>"
            targetMethodName = targetMethodName.split("\\$")[1];
        }

        metadata.setFullyQualifiedClassname(typeHierarchy.get(0));

        String[] classNameParts = typeHierarchy.get(0).split("\\.");
        String unqualifiedClassName = classNameParts[classNameParts.length - 1];
        metadata.setUnqualifiedClassname(unqualifiedClassName);
        String packageName = StringUtil.join(classNameParts, ".");
        int lastDotIndex = packageName.lastIndexOf(".");
        if (lastDotIndex > -1) {
            packageName = packageName.substring(0, lastDotIndex);
        } else {
            packageName = "";
        }
        metadata.setPackageName(packageName);


        Map<String, DataInfo> probeInfoMap = replayData.getProbeInfoMap();

        metadata.setMethodName(targetMethodName);


        String testMethodName = "test" + upperInstanceName(targetMethodName);
        metadata.setTestMethodName(testMethodName);

        // https://gist.github.com/VijayKrishna/6160036
        List<String> methodDescription = splitMethodDesc(methodInfo.getMethodDesc());

        methodDescription.remove(methodDescription.size() - 1);


        List<DataEventWithSessionId> events = replayData.getDataEvents();


        int entryProbeIndex = 0;
        // going up matching the first event for our method entry probe
        // up is back in time
        while (entryProbeIndex < events.size()) {
            DataEventWithSessionId eventInfo = events.get(entryProbeIndex);
            if (eventInfo.getNanoTime() == entryProbeDataId) break;
            entryProbeIndex++;
        }

//        metadata.setEntryProbeIndex(entryProbeIndex);
//        metadata.setEntryProbe(events.get(entryProbeIndex));


        int callReturnIndex = -1;
        ReplayData replayDataPage = replayData;
        DataInfo entryProbeInfo = probeInfoMap.get(String.valueOf(events.get(entryProbeIndex).getDataId()));
        logger.warn("located entry probe at: " + entryProbeIndex + " -- " + entryProbeInfo);
        int currentEntryProbeIndex = entryProbeIndex;
        while (true) {
            if (entryProbeInfo.getEventType() == EventType.CALL) {

                if (methodInfo.getMethodName().equals("<init>")) {
                    callReturnIndex = searchCallReturnIndex(replayDataPage,
                            currentEntryProbeIndex,
                            List.of(EventType.NEW_OBJECT_CREATED));
                } else {
                    callReturnIndex = searchCallReturnIndex(replayDataPage,
                            currentEntryProbeIndex, List.of(EventType.CALL_RETURN));
                }

            } else if (entryProbeInfo.getEventType() == EventType.METHOD_ENTRY) {
//                callReturnIndex = searchCallReturnIndex(replayDataPage,
//                        currentEntryProbeIndex, List.of(
//                                EventType.METHOD_OBJECT_INITIALIZED
//                        ));
//

                if (methodInfo.getMethodName().equals("<init>")) {
                    logger.info("entry probe is of type method entry <init>");
                    callReturnIndex = searchCallReturnIndex(replayDataPage,
                            currentEntryProbeIndex,
                            List.of(EventType.METHOD_OBJECT_INITIALIZED));
                } else {
                    logger.info("entry probe is of type method entry " + methodInfo.getMethodName());
                    callReturnIndex = searchCallReturnIndex(replayDataPage,
                            currentEntryProbeIndex, List.of(EventType.METHOD_NORMAL_EXIT));
                }

            }

            if (callReturnIndex != -1) {
                break;
            }
            logger.warn("return probe not found, fetching next page: " +
                    replayDataPage.getFilteredDataEventsRequest().getPageInfo());
            replayDataPage = replayDataPage.getNextPage();
            if (replayDataPage.getDataEvents().size() == 0) {
                break;
            }
            currentEntryProbeIndex = replayDataPage.getDataEvents().size();
        }


//        metadata.setExitProbeIndex(callReturnIndex);
        logger.info("entry probe matched at event: " + entryProbeIndex +
                ", return found " + "at: " + callReturnIndex);

        if (callReturnIndex == -1) {
            logger.warn("call_return probe not found in the slice: " + entryProbeDataId +
                    " when generating test for method " + targetMethodName + " " +
                    " in class " + typeHierarchy.get(0) + ". Maybe need a bigger " +
                    "slice");
            return metadata;
        }
        long callTime = events.get(callReturnIndex).getRecordedAt() - events.get(entryProbeIndex).getRecordedAt();
        metadata.setCallTimeNanoSecond(callTime);

        List<Parameter> methodParameters =
                searchCallParameters(replayDataPage, entryProbeIndex, methodDescription.size());

        metadata.addAllParameter(methodParameters);


        Parameter returnParameter = createObject(callReturnIndex, replayData, 0);

//        if (returnParameter)

        metadata.setReturnParameter(returnParameter);


        String testSubjectInstanceName = lowerInstanceName(unqualifiedClassName);

        int paramsToSkip = methodParameters.size();
        boolean subjectNameFound = false;


        // identify the variable name on which this method is called
        if (methodInfo.getMethodName().equals("<init>")) {
            metadata.setTestSubject(returnParameter);
            subjectNameFound = true;
        } else {

            ReplayData moreHistory = replayData.fetchEventsPre(events.get(entryProbeIndex), 10);
            events.addAll(entryProbeIndex, moreHistory.getDataEvents());

            int callStack = 0;
            for (int i = entryProbeIndex; i < events.size(); i += 1) {
                DataEventWithSessionId event = events.get(i);
                DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                ClassInfo currentClassInfo = replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
                MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));
                EventType eventType = probeInfo.getEventType();

                logger.warn("[SearchSubject] #" + i + ", T=" + event.getNanoTime() +
                        ", P=" + event.getDataId() + ":" + event.getValue() +
                        " [Stack:" + callStack + "]" +
                        " " + String.format("%25s", probeInfo.getEventType())
                        + " in " + String.format("%25s", currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                        + ":" + probeInfo.getLine()
                        + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                        + "  -> " + probeInfo.getAttributes()
                );


                switch (eventType) {
                    case METHOD_NORMAL_EXIT:
                        callStack++;
                        break;
                    case METHOD_ENTRY:
                        callStack--;
                        break;
                    case LOCAL_LOAD:
                    case GET_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                        if (callStack != -1) {
                            continue;
                        }
                        String valueTypeNameWithSlash = probeInfo.getAttribute("Type", "");
                        String valueTypeNameWithDots = "";
                        if (valueTypeNameWithSlash.startsWith("L")) {
                            valueTypeNameWithDots = valueTypeNameWithSlash.substring(1)
                                    .split(";")[0].replace("/", ".");
                        } else {
                            valueTypeNameWithDots = valueTypeNameWithSlash;
                        }
                        if (paramsToSkip == 0) {
                            String variableName = probeInfo.getAttribute("Name", null);

                            if (variableName == null) {
                                variableName = probeInfo.getAttribute("FieldName",
                                        testSubjectInstanceName);
                            }
                            Parameter subjectInstanceParameter;
                            if (eventType == EventType.LOCAL_LOAD) {
                                subjectInstanceParameter = createObject(i, replayData, 0);
                            } else {
                                subjectInstanceParameter = createObject(i - 1, replayData, 0);
                            }

                            metadata.setTestSubject(subjectInstanceParameter);
                            subjectNameFound = true;
                            break;
                        }
                        paramsToSkip = paramsToSkip - 1;

                }

                if (subjectNameFound) {
                    break;
                }

            }
        }


        if (metadata.getReturnParameter().getName() == null) {

            String potentialReturnValueName;
            if (targetMethodName.startsWith("get") || targetMethodName.startsWith("set")) {
                if (targetMethodName.length() > 3) {
                    potentialReturnValueName = lowerInstanceName(targetMethodName.substring(3)) + "Value";
                } else {
                    potentialReturnValueName = "value";
                }
            } else {
                if (targetMethodName.equals("<init>")) {
                    potentialReturnValueName = createVariableName(metadata.getUnqualifiedClassname());
                } else {
                    potentialReturnValueName = targetMethodName + "Result";
                }
            }

            metadata.getReturnParameter().setName(potentialReturnValueName);
        }


        return metadata;
    }

    private static Object getValueByObjectId(DataEventWithSessionId event, ReplayData replayData) {
        long probeValue = event.getValue();
        String probeValueString = String.valueOf(probeValue);
        ObjectInfo objectInfo = replayData.getObjectInfo().get(probeValueString);

        if (objectInfo == null) {
            return probeValueString;
        }
        Map<String, StringInfo> stringInfoMap = replayData.getStringInfoMap();

        TypeInfo probeType = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
        if (stringInfoMap.containsKey(probeValueString)) {
            String strContent = stringInfoMap.get(probeValueString).getContent();
            return "\"" + StringUtil.escapeQuotes(strContent) + "\"";
        } else {
            return probeValueString;
        }
    }

    private static List<Parameter> searchCallParameters
            (
                    ReplayData replayData,
                    int entryProbeIndex,
                    int paramCount
            ) {
        logger.info("searchCallParameters starting from [" + entryProbeIndex + "] -> " + paramCount + " params to be found");
        DataInfo entryProbeInfo = replayData.getProbeInfoMap().get(
                String.valueOf(replayData.getDataEvents().get(entryProbeIndex).getDataId())
        );
        List<Parameter> methodParameterProbes = new LinkedList<>();
        int direction = -1;
        int callReturnIndex = entryProbeIndex + direction;

        boolean lookingForParams = true;
        int callStack = 0;
        // going down from where we found the method entry probe
        // to match the first call_return probe
        List<DataEventWithSessionId> events = replayData.getDataEvents();
        Map<String, DataInfo> probeInfoMap = replayData.getProbeInfoMap();
        int paramIndex = paramCount;
        if (entryProbeInfo.getEventType() == EventType.CALL) {
            while (callReturnIndex > -1) {
                DataEventWithSessionId event = events.get(callReturnIndex);
                DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                EventType eventType = probeInfo.getEventType();
                ClassInfo currentClassInfo = replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
                MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));
                logger.warn("[SearchCallParameters] #" + callReturnIndex + ", T=" + event.getNanoTime() +
                        ", P=" + event.getDataId() +
                        " [Stack:" + callStack + "]" +
                        " " + String.format("%25s", probeInfo.getEventType())
                        + " in " + String.format("%25s", currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                        + ":" + probeInfo.getLine()
                        + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                        + "  -> " + probeInfo.getAttributes()
                );

                if (eventType == EventType.CALL) {
                    callStack += 1;
                }
                if (callStack > 0 && eventType == EventType.CALL_RETURN) {
                    callStack -= 1;
                    callReturnIndex += direction;
                    continue;
                }
                if (callStack > 0) {
                    callReturnIndex += direction;
                    continue;
                }

                if (lookingForParams && eventType == EventType.CALL_PARAM) {
                    Parameter parameter = createObject(callReturnIndex, replayData, paramIndex - 1);
                    paramIndex -= 1;
                    methodParameterProbes.add(parameter);
                } else {
                    lookingForParams = false;
                }

                if (
                        probeInfo.getEventType() == EventType.CALL_RETURN
                ) {
                    break;
                }

                callReturnIndex += direction;

            }

        } else if (entryProbeInfo.getEventType() == EventType.METHOD_ENTRY) {
            while (callReturnIndex > -1) {
                DataEventWithSessionId event = events.get(callReturnIndex);
                DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                EventType eventType = probeInfo.getEventType();
                ClassInfo currentClassInfo = replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
                MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));

                logger.warn("[SearchCallParameters] #" + callReturnIndex + ", T=" + event.getNanoTime() +
                        ", P=" + event.getDataId() +
                        " [Stack:" + callStack + "]" +
                        " " + String.format("%25s", probeInfo.getEventType())
                        + " in " + String.format("%25s", currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                        + ":" + probeInfo.getLine()
                        + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                        + "  -> " + probeInfo.getAttributes()
                );


                if (eventType == EventType.CALL) {
                    callStack += 1;
                }
                if (callStack > 0 && eventType == EventType.CALL_RETURN) {
                    callStack -= 1;
                    callReturnIndex += direction;
                    continue;
                }
                if (callStack > 0) {
                    callReturnIndex += direction;
                    continue;
                }

                if (lookingForParams && eventType == EventType.METHOD_PARAM) {
                    Parameter parameter = createObject(callReturnIndex, replayData, 0);
                    methodParameterProbes.add(parameter);
                } else {
                    lookingForParams = false;
                }

                if (
                        probeInfo.getEventType() == EventType.METHOD_NORMAL_EXIT ||
                                probeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT
                ) {
                    logger.info("Stop looking for search call parameters: " + probeInfo.getEventType());
                    break;
                }

                callReturnIndex += direction;

            }

        }

        logger.info("Found [" + methodParameterProbes.size() + "] parameters");
        for (int i = 0; i < methodParameterProbes.size(); i++) {
            Parameter methodParameterProbe = methodParameterProbes.get(i);
            logger.warn("param #" + i + ": " + methodParameterProbe);
        }

        return methodParameterProbes;
    }

    private static int searchCallReturnIndex
            (
                    ReplayData replayData,
                    int entryProbeIndex,
                    List<EventType> eventTypeMatch
            ) {
        logger.warn("looking for call return index, with entry probe index at: " + entryProbeIndex + " -> " + eventTypeMatch);
        logger.warn("replay data has: " + replayData.getDataEvents().size() + " events.");
        int direction = -1;
        int callReturnIndex = entryProbeIndex + direction;

        int callStack = 0;
        // going down from where we found the method entry probe
        // to match the first call_return probe
        List<DataEventWithSessionId> events = replayData.getDataEvents();
        Map<String, DataInfo> probeInfoMap = replayData.getProbeInfoMap();
        while (callReturnIndex > -1) {
            DataEventWithSessionId event = events.get(callReturnIndex);
            DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
            EventType eventType = probeInfo.getEventType();
            ClassInfo currentClassInfo = replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
            MethodInfo methodInfo = replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));

            logger.warn("[SearchCallReturn] #" + callReturnIndex + ", T=" + event.getNanoTime() +
                    ", P=" + event.getDataId() +
                    " [Stack:" + callStack + "]" +
                    " " + String.format("%25s", probeInfo.getEventType())
                    + " in " + String.format("%25s",
                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                    + ":" + probeInfo.getLine()
                    + " in " + String.format("%20s", methodInfo.getMethodName())
                    + "  -> " + probeInfo.getAttributes());


            if (eventType == EventType.METHOD_ENTRY) {
                callStack += 1;
            }
            if (callStack > 0 && eventType == EventType.METHOD_NORMAL_EXIT) {
                callStack -= 1;
                callReturnIndex += direction;
                continue;
            }
            if (callStack > 0) {
                callReturnIndex += direction;
                continue;
            }

            if (eventTypeMatch.contains(probeInfo.getEventType())) {
                break;
            }

            callReturnIndex += direction;

        }
        return callReturnIndex;
    }

    private static Parameter createObject
            (
                    final int eventIndex,
                    ReplayData replayData,
                    int paramIndex) {

        logger.info("Create object from index [" + eventIndex + "] - ParamIndex" + paramIndex);
        Parameter parameter = new Parameter();
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);

        parameter.setProb(event);
        parameter.setIndex(eventIndex);


        String eventProbeIdString = String.valueOf(event.getDataId());
        String eventValueString = String.valueOf(event.getValue());
        DataInfo probeInfo = replayData.getProbeInfoMap().get(eventProbeIdString);
        ObjectInfo objectInfo = replayData.getObjectInfo().get(eventValueString);
        parameter.setProbeInfo(probeInfo);
        if (objectInfo != null) {
            TypeInfo typeInfo = replayData.getTypeInfo().get(
                    String.valueOf(objectInfo.getTypeId())
            );
            if (typeInfo == null) {
                logger.warn("type info is null: " + objectInfo.getObjectId() + ": -> " + objectInfo.getTypeId());
            } else {
                parameter.setType("L" + typeInfo.getTypeNameFromClass().replaceAll("\\.", "/") + ";");
            }

        }
//
//        TypeInfo valueTypeInfo;
//        if (objectInfo != null) {
//            valueTypeInfo = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
//        }

        Object probeValue = getValueByObjectId(parameter.getProb(), replayData);
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

                        probeInfo.getEventType() == EventType.NEW_OBJECT_CREATED
        ) {
            direction = -1; // go forward from current event
            callStackSearchLevel = 0; // we want something in the current method only
        } else if (
                probeInfo.getEventType() == EventType.METHOD_NORMAL_EXIT
        ) {
            direction = -1; // go forward from current event
            callStackSearchLevel = -1; // we want something in the caller method
        } else if (
                probeInfo.getEventType() == EventType.METHOD_PARAM
        ) {
            direction = 1; // go backward from current event
            callStackSearchLevel = -1; // we want something in the caller method
        }

        Long parameterValueId = 0L;
        try {
            parameterValueId = Long.valueOf((String) parameter.getValue());
        }catch (NumberFormatException nfe) {

        }

        int callStack = 0;
        for (int i = eventIndex + direction; i < replayData.getDataEvents().size()
                && i > -1; i += direction) {
            DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
            DataInfo historyEventProbe = replayData.getProbeInfoMap().get(
                    String.valueOf(historyEvent.getDataId())
            );
            ClassInfo currentClassInfo =
                    replayData.getClassInfoMap().get(String.valueOf(historyEventProbe.getClassId()));
            MethodInfo methodInfoLocal = replayData.getMethodInfoMap().get(String.valueOf(historyEventProbe.getMethodId()));


            logger.warn("[SearchObjectName] #" + i + ", T=" + historyEvent.getNanoTime() +
                    ", P=" + historyEvent.getDataId() +
                    " [Stack:" + callStack + "]" +
                    " " + String.format("%25s", historyEventProbe.getEventType())
                    + " in " + String.format("%25s",
                    currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                    + ":" + historyEventProbe.getLine()
                    + " in " + String.format("%20s", methodInfoLocal.getMethodName())
                    + "  -> " + historyEventProbe.getAttributes());


            switch (historyEventProbe.getEventType()) {
                case CALL:
                    // this value has no name in this direction, maybe we can use the name of the
                    // argument it is passed as

                    // but if this is a call to a third party sdk, then we dont know the
                    // argument name

                    if (callStack == 0) {

                        // we are potentially already invoking another call on this object for
                        // which we wonted to get a name for., the return value has no name, and
                        // is being used to invoke another function directly, so we can stop the
                        // search for a name
                        return parameter;

                    }

                    if (callStack == callStackSearchLevel) {

                        // this happens when we were looking back for a parameter name, but find
                        // ourself inside another call, need to check this out again

                        return parameter;
                    }
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
                    ObjectInfo oInfo = replayData.getObjectInfo().get(String.valueOf(historyEvent.getValue()));
                    if (oInfo == null) {
                        logger.warn("object info is null [" + historyEvent.getValue() + "], gotta " +
                                "check");
                        break;
                    }
                    TypeInfo oTypeInfo = replayData.getTypeInfo().get(String.valueOf(oInfo.getTypeId()));
                    String typeName = oTypeInfo.getTypeNameFromClass();
                    String typeNameRaw = typeName.replaceAll("\\.", "/");
                    String newVariableInstanceName = createVariableName(typeNameRaw);
                    if (parameter.getType().contains(typeNameRaw)) {
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
                    // Ljava/lang/Integer (implicite conversion by jvm). removing the if should
                    // be fine because we are also tracing the parameters by index (which was not
                    // there when the type check was initially added)
                    if (!variableType.startsWith("L") || variableType.equals(parameter.getType())) {
                        String variableName = historyEventProbe.getAttribute("Name", null);
                        parameter.setName(variableName);
                        return parameter;
                    }
//                    break;
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
//                    if (fieldType.equals(parameter.getType())) {
                    String variableName1 = historyEventProbe.getAttribute("FieldName", null);
                    parameter.setName(variableName1);
                    return parameter;
//                    }
//                    break;
            }
        }


        return parameter;
    }

    private static String createVariableName(String typeNameRaw) {
        String[] typeNameParts = typeNameRaw.split("/");
        String lastPart = typeNameParts[typeNameParts.length - 1];
        lastPart = lastPart.substring(0, 1).toLowerCase() + lastPart.substring(1);
        return lastPart;
    }

    private static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    private static String lowerInstanceName(String methodName) {
        return methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
    }

    public static List<String> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException();
        }
        String x0;
        if (beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        } else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }
        Pattern pattern = Pattern.compile("\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
        Matcher matcher = pattern.matcher(x0);
        List<String> listMatches = new LinkedList<>();
        while (matcher.find()) {
            listMatches.add(matcher.group());
        }
        listMatches.add(desc.substring(endIndex + 1));
        return listMatches;
    }

    public String getMethodName() {
        return methodName;
    }

    private void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFullyQualifiedClassname() {
        return fullyQualifiedClassname;
    }

    public void setFullyQualifiedClassname(String fullyQualifiedClassname) {
        this.fullyQualifiedClassname = fullyQualifiedClassname;
    }

    public String getUnqualifiedClassname() {
        return unqualifiedClassname;
    }

    public void setUnqualifiedClassname(String unqualifiedClassname) {
        this.unqualifiedClassname = unqualifiedClassname;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    public Parameter getTestSubject() {
        return testSubject;
    }

    public void setTestSubject(Parameter testSubject) {
        this.testSubject = testSubject;
    }


    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public void addAllParameter(List<Parameter> parameter) {
        this.parameters.addAll(parameter);
    }

    public List<Parameter> getParameterValues() {
        return this.parameters;
    }

    public Parameter getReturnParameter() {
        return returnParameter;
    }

    public void setReturnParameter(Parameter returnParameter) {
        this.returnParameter = returnParameter;
    }

    public long getCallTimeNanoSecond() {
        return callTimeNanoSecond;
    }

    public void setCallTimeNanoSecond(long callTimeNanoSecond) {
        this.callTimeNanoSecond = callTimeNanoSecond;
    }

    @Override
    public String toString() {
        return "TestCandidateMetadata{" +
                "parameters=" + parameters +
                ", fullyQualifiedClassname='" + fullyQualifiedClassname + '\'' +
                ", testMethodName='" + testMethodName + '\'' +
                ", testSubject=" + testSubject +
                ", returnParameter=" + returnParameter +
                ", callTimeNanoSecond=" + callTimeNanoSecond +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
