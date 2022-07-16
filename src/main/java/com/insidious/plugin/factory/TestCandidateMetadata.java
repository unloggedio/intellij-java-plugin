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
    private String fullyQualifiedClassname;
    private String unqualifiedClassname;
    private String packageName;
    private String testMethodName;
    private Parameter testSubject;
    private Parameter returnParameter;
    private int entryProbeIndex;
    private int exitProbeIndex;
    private DataEventWithSessionId entryProbe;
    private final List<Parameter> parameters = new LinkedList<>();

    public String getMethodName() {
        return methodName;
    }

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
            DataInfo probeInfo = objectReplayData.getDataInfoMap().get(String.valueOf(event.getDataId()));
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
                        DataInfo nextEventProbe = objectReplayData.getDataInfoMap().get(
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
            MethodInfo methodInfo,
            long entryProbeDataId,
            ReplayData replayData
    ) throws APICallException {
        TestCandidateMetadata metadata = new TestCandidateMetadata();

        final String className = methodInfo.getClassName();
        String targetMethodName = methodInfo.getMethodName();

        metadata.setFullyQualifiedClassname(className);

        String[] classNameParts = className.split("/");
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


        Map<String, TypeInfo> typeInfo = replayData.getTypeInfo();
        Map<String, ObjectInfo> objectInfoMap = replayData.getObjectInfo();
        Map<String, DataInfo> probeInfoMap = replayData.getDataInfoMap();
        Map<String, StringInfo> stringInfoMap = replayData.getStringInfoMap();

        metadata.setMethodName(targetMethodName);


        String testMethodName = "test" + upperInstanceName(targetMethodName);
        metadata.setTestMethodName(testMethodName);

        // https://gist.github.com/VijayKrishna/6160036
        List<String> methodDescription = splitMethodDesc(methodInfo.getMethodDesc());
        String methodReturnValueType = methodDescription.get(methodDescription.size() - 1);

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

        metadata.setEntryProbeIndex(entryProbeIndex);
        metadata.setEntryProbe(events.get(entryProbeIndex));


        int callReturnIndex = -1;
        ReplayData replayDataPage = replayData;
        DataInfo entryProbeInfo = probeInfoMap.get(String.valueOf(events.get(entryProbeIndex).getDataId()));
        while (true) {
            if (entryProbeInfo.getEventType() == EventType.CALL) {

                if (methodInfo.getMethodName().equals("<init>")) {
                    callReturnIndex = searchCallReturnIndex(replayDataPage, entryProbeIndex,
                            List.of(EventType.NEW_OBJECT_CREATED));
                } else {
                    callReturnIndex = searchCallReturnIndex(replayDataPage, entryProbeIndex, List.of(EventType.CALL_RETURN));
                }

            } else if (entryProbeInfo.getEventType() == EventType.METHOD_ENTRY) {
                callReturnIndex = searchCallReturnIndex(replayDataPage, entryProbeIndex,
                        List.of(
                                EventType.METHOD_OBJECT_INITIALIZED
                        ));

            }

            if (callReturnIndex != -1) {
                break;
            }
            replayDataPage = replayDataPage.getNextPage();
            if (replayDataPage.getDataEvents().size() == 0) {
                break;
            }
        }


        ReplayData callReturnReplayData;
        callReturnReplayData = replayData;

        metadata.setExitProbeIndex(callReturnIndex);
        logger.info("entry probe matched at event: " + entryProbeIndex +
                ", return found " + "at: " + callReturnIndex);

        if (callReturnIndex == -1) {
            logger.debug("call_return probe not found in the slice: " + entryProbeDataId +
                    " when generating test for method " + targetMethodName + " " +
                    " in class " + className + ". Maybe need a bigger " +
                    "slice");
            return metadata;
        }


        List<Parameter> methodParameters = searchCallParameters(replayDataPage, entryProbeIndex);

        metadata.addAllParameter(methodParameters);


        Parameter returnParameter = createObject(callReturnIndex, replayData);

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
            for (int i = entryProbeIndex; i < events.size(); i += 1) {
                DataEventWithSessionId event = events.get(i);
                DataInfo eventProbeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                EventType eventType = eventProbeInfo.getEventType();
                switch (eventType) {
                    case LOCAL_LOAD:
                    case GET_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                        if (eventProbeInfo.getAttribute("Type", "").contains(className)) {
                            String variableName = eventProbeInfo.getAttribute("Name", null);

                            if (variableName == null) {
                                variableName = eventProbeInfo.getAttribute("FieldName",
                                        testSubjectInstanceName);
                            }
                            Parameter subjectInstanceParameter = createObject(i - 1, replayData);

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
                    int entryProbeIndex
            ) {
        DataInfo entryProbeInfo = replayData.getDataInfoMap().get(
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
        Map<String, DataInfo> probeInfoMap = replayData.getDataInfoMap();

        if (entryProbeInfo.getEventType() == EventType.CALL) {
            while (callReturnIndex > -1) {
                DataEventWithSessionId event = events.get(callReturnIndex);
                DataInfo eventProbeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                EventType eventType = eventProbeInfo.getEventType();

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
                    Parameter parameter = createObject(callReturnIndex, replayData);
                    methodParameterProbes.add(parameter);
                } else {
                    lookingForParams = false;
                }

                if (
                        eventProbeInfo.getEventType() == EventType.CALL_RETURN
                ) {
                    break;
                }

                callReturnIndex += direction;

            }

        } else if (entryProbeInfo.getEventType() ==  EventType.METHOD_ENTRY) {
            while (callReturnIndex > -1) {
                DataEventWithSessionId event = events.get(callReturnIndex);
                DataInfo eventProbeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                EventType eventType = eventProbeInfo.getEventType();

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
                    Parameter parameter = createObject(callReturnIndex, replayData);
                    methodParameterProbes.add(parameter);
                } else {
                    lookingForParams = false;
                }

                if (
                        eventProbeInfo.getEventType() == EventType.METHOD_NORMAL_EXIT ||
                                eventProbeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT
                ) {
                    break;
                }

                callReturnIndex += direction;

            }

        }

        return methodParameterProbes;
    }


    private static int searchCallReturnIndex
            (
                    ReplayData replayData,
                    int entryProbeIndex,
                    List<EventType> eventTypeMatch
            ) {
        int direction = -1;
        int callReturnIndex = entryProbeIndex + direction;

        int callStack = 0;
        // going down from where we found the method entry probe
        // to match the first call_return probe
        List<DataEventWithSessionId> events = replayData.getDataEvents();
        Map<String, DataInfo> probeInfoMap = replayData.getDataInfoMap();
        while (callReturnIndex > -1) {
            DataEventWithSessionId event = events.get(callReturnIndex);
            DataInfo eventProbeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
            EventType eventType = eventProbeInfo.getEventType();

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

            if (eventTypeMatch.contains(eventProbeInfo.getEventType())) {
                break;
            }

            callReturnIndex += direction;

        }
        return callReturnIndex;
    }

    private static Parameter createObject
            (
                    int eventIndex,
                    ReplayData replayData
            ) {

        Parameter parameter = new Parameter();
        DataEventWithSessionId event = replayData.getDataEvents().get(eventIndex);

        parameter.setProb(event);
        parameter.setIndex(eventIndex);


        String eventProbeIdString = String.valueOf(event.getDataId());
        String eventValueString = String.valueOf(event.getValue());
        DataInfo probeInfo = replayData.getDataInfoMap().get(eventProbeIdString);
        ObjectInfo objectInfo = replayData.getObjectInfo().get(eventValueString);
        parameter.setProbeInfo(probeInfo);
        if (objectInfo != null) {
            TypeInfo typeInfo = replayData.getTypeInfo().get(
                    String.valueOf(objectInfo.getTypeId())
            );
            parameter.setType("L" + typeInfo.getTypeNameFromClass().replaceAll("\\.", "/") + ";");

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

        int direction;
        if (probeInfo.getEventType() == EventType.CALL_RETURN) {
            direction = -1; // go forward from current event
        } else {
            direction = 1; // go backward from current event
        }

        for (int i = eventIndex + direction; i < replayData.getDataEvents().size(); i += direction) {
            DataEventWithSessionId historyEvent = replayData.getDataEvents().get(i);
            DataInfo historyEventProbe = replayData.getDataInfoMap().get(
                    String.valueOf(historyEvent.getDataId())
            );
            switch (historyEventProbe.getEventType()) {
                case NEW_OBJECT_CREATED:
                    ObjectInfo oInfo = replayData.getObjectInfo().get(String.valueOf(historyEvent.getValue()));
                    if (oInfo == null) {
                        logger.warn("object info is null, gotta check");
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
                    String variableType = historyEventProbe.getAttribute("Type", "V");
                    if (variableType.equals(parameter.getType())) {
                        String variableName = historyEventProbe.getAttribute("Name", null);
                        parameter.setName(variableName);
                        return parameter;
                    }
                    break;
                case GET_STATIC_FIELD:
                case PUT_STATIC_FIELD:
                case GET_INSTANCE_FIELD:
                case PUT_INSTANCE_FIELD:
                    String fieldType = historyEventProbe.getAttribute("Type", "V");
                    if (fieldType.equals(parameter.getType())) {
                        String variableName = historyEventProbe.getAttribute("FieldName", null);
                        parameter.setName(variableName);
                        return parameter;
                    }
                    break;
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

    private void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public void setFullyQualifiedClassname(String fullyQualifiedClassname) {
        this.fullyQualifiedClassname = fullyQualifiedClassname;
    }

    public String getFullyQualifiedClassname() {
        return fullyQualifiedClassname;
    }

    public void setUnqualifiedClassname(String unqualifiedClassname) {
        this.unqualifiedClassname = unqualifiedClassname;
    }

    public String getUnqualifiedClassname() {
        return unqualifiedClassname;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public Parameter getTestSubject() {
        return testSubject;
    }

    public void setTestSubject(Parameter testSubject) {
        this.testSubject = testSubject;
    }

    public void setEntryProbeIndex(int entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public int getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setExitProbeIndex(int callReturnProbeIndex) {
        this.exitProbeIndex = callReturnProbeIndex;
    }

    public int getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setEntryProbe(DataEventWithSessionId entryProbe) {
        this.entryProbe = entryProbe;
    }

    public DataEventWithSessionId getEntryProbe() {
        return entryProbe;
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

    public void setReturnParameter(Parameter returnParameter) {
        this.returnParameter = returnParameter;
    }

    public Parameter getReturnParameter() {
        return returnParameter;
    }
}
