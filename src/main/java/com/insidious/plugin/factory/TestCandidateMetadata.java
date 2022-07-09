package com.insidious.plugin.factory;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCandidateMetadata {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);
    private DataEventWithSessionId callReturnProbe;
    private String fullyQualifiedClassname;
    private String unqualifiedClassname;
    private String packageName;
    private String testMethodName;
    private String testSubjectInstanceName;
    private String returnValueType;
    private int entryProbeIndex;
    private int exitProbeIndex;
    private DataEventWithSessionId entryProbe;
    private List<DataEventWithSessionId> parameterProbes;
    private List<String> parameterValues;
    private String returnValue;
    private TypeInfo returnTypeInfo;
    private Object returnSubjectInstanceName;

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
                    if (callStack == -1) {
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
    ) {
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
        metadata.setReturnValueType(methodReturnValueType);
        methodDescription.remove(methodDescription.size() - 1);


        List<DataEventWithSessionId> events = replayData.getDataEvents();


        int entryProbeIndex = 0;
        List<DataEventWithSessionId> methodParameterProbes = new LinkedList<>();
        boolean lookingForParams = true;
        // going up matching the first event for our method entry probe
        // up is back in time
        while (entryProbeIndex < events.size()) {
            DataEventWithSessionId eventInfo = events.get(entryProbeIndex);
            if (eventInfo.getNanoTime() == entryProbeDataId) break;
            entryProbeIndex++;
        }

        metadata.setEntryProbeIndex(entryProbeIndex);
        metadata.setEntryProbe(events.get(entryProbeIndex));


        int direction = -1;
        int callReturnIndex = entryProbeIndex + direction;

        int callStack = 0;
        // going down from where we found the method entry probe
        // to match the first call_return probe
        while (callReturnIndex > -1) {
            DataEventWithSessionId event = events.get(callReturnIndex);
            DataInfo eventProbeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
            EventType eventType = eventProbeInfo.getEventType();

            if (eventType == EventType.CALL) {
                callStack += 1;
            }
            if (callStack > 0 && eventType == EventType.CALL_RETURN) {
                callStack -= 1;
            }
            if (callStack > 0) {
                callReturnIndex += direction;
                continue;
            }

            if (lookingForParams && eventType == EventType.CALL_PARAM) {
                methodParameterProbes.add(event);
            } else {
                lookingForParams = false;
            }

            if (eventProbeInfo.getEventType() == EventType.CALL_RETURN) {
                break;
            }

            callReturnIndex += direction;

        }

        metadata.setExitProbeIndex(callReturnIndex);
        logger.info("entry probe matched at event: " + entryProbeIndex +
                ", return found " + "at: " + callReturnIndex);


        String testSubjectInstanceName = lowerInstanceName(unqualifiedClassName);

        int paramsToSkip = methodParameterProbes.size();
        boolean subjectNameFound = false;


        if (methodInfo.getMethodName().equals("<init>")) {
            String subjectName = getTestSubjectName(entryProbeIndex, methodInfo, replayData);
            if (subjectName != null) {
                metadata.setTestSubjectInstanceName(subjectName);
                subjectNameFound = true;
            }
        } else {
            for (int i = entryProbeIndex; i < events.size(); i += 1) {
                DataEventWithSessionId event = events.get(i);
                DataInfo eventProbeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
                EventType eventType = eventProbeInfo.getEventType();
                switch (eventType) {
                    case LOCAL_LOAD:
                    case GET_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                        if (eventProbeInfo.getAttribute("Type", "").contains(className)
                                && paramsToSkip == 0) {
                            metadata.setTestSubjectInstanceName(
                                    eventProbeInfo.getAttribute("Name", testSubjectInstanceName));
                            subjectNameFound = true;
                        }
                        paramsToSkip = paramsToSkip - 1;

                }

            }
        }



        if (!subjectNameFound) {
            metadata.setTestSubjectInstanceName(null);
            return metadata;
        }



        int i = 0;
        List<String> methodParameterValues = new LinkedList<>();
        List<String> methodParameterTypes = new LinkedList<>();

        metadata.setParameterProbes(methodParameterProbes);
        for (DataEventWithSessionId parameterProbe : methodParameterProbes) {
            i++;
            long probeValue = parameterProbe.getValue();
            String probeValueString = String.valueOf(probeValue);
            ObjectInfo objectInfo = objectInfoMap.get(probeValueString);

            if (objectInfo == null) {
                methodParameterValues.add(probeValueString);
                continue;
            }

            TypeInfo probeType = typeInfo.get(String.valueOf(objectInfo.getTypeId()));
            if (stringInfoMap.containsKey(probeValueString)) {
                String quotedStringValue =
                        "\"" + StringUtil.escapeQuotes(stringInfoMap.get(probeValueString).getContent()) + "\"";
                methodParameterValues.add(quotedStringValue);
            } else {
                methodParameterValues.add(probeValueString);
            }
        }
        metadata.setParameterValues(methodParameterValues);


        if (callReturnIndex == -1) {
            logger.debug("call_return probe not found in the slice: " + entryProbeDataId +
                    " when generating test for method " + targetMethodName + " " +
                    " in class " + className + ". Maybe need a bigger " +
                    "slice");
            return metadata;
        }
        DataEventWithSessionId callReturnProbeValue = events.get(callReturnIndex);
        String returnProbeValue = String.valueOf(callReturnProbeValue.getValue());
        ObjectInfo objectInfo = objectInfoMap.get(returnProbeValue);


        metadata.setReturnValue(returnProbeValue);
        metadata.setCallReturnProbe(callReturnProbeValue);

        StringInfo possibleReturnStringValue = null;
        if (stringInfoMap.containsKey(returnProbeValue)) {
            possibleReturnStringValue = stringInfoMap.get(returnProbeValue);
            metadata.setReturnValue(possibleReturnStringValue.getContent());
        } else if (objectInfo != null) {
            TypeInfo objectTypeInfo = typeInfo.get(String.valueOf(objectInfo.getTypeId()));
            metadata.setReturnTypeInfo(objectTypeInfo);
        }

        String potentialReturnValueName;
        if (targetMethodName.startsWith("get") || targetMethodName.startsWith("set")) {
            if (targetMethodName.length() > 3) {
                potentialReturnValueName = lowerInstanceName(targetMethodName.substring(3)) + "Value";
            } else {
                potentialReturnValueName = "value";
            }
        } else {
            potentialReturnValueName = targetMethodName + "Result";
        }

        metadata.setReturnSubjectInstanceName(potentialReturnValueName);


        return metadata;
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

    public DataEventWithSessionId callReturnProbe() {
        return callReturnProbe;
    }

    public void setCallReturnProbe(DataEventWithSessionId callReturnProbe) {
        this.callReturnProbe = callReturnProbe;
    }

    public DataEventWithSessionId getCallReturnProbe() {
        return callReturnProbe;
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

    public void setTestSubjectInstanceName(String testSubjectInstanceName) {
        this.testSubjectInstanceName = testSubjectInstanceName;
    }

    public String getTestSubjectInstanceName() {
        return testSubjectInstanceName;
    }

    public void setReturnValueType(String returnValueType) {
        this.returnValueType = returnValueType;
    }

    public String getReturnValueType() {
        return returnValueType;
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

    public void setParameterProbes(List<DataEventWithSessionId> parameterProbes) {
        this.parameterProbes = parameterProbes;
    }

    public List<DataEventWithSessionId> getParameterProbes() {
        return parameterProbes;
    }

    public void setParameterValues(List<String> parameterValues) {
        this.parameterValues = parameterValues;
    }

    public List<String> getParameterValues() {
        return parameterValues;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnTypeInfo(TypeInfo returnTypeInfo) {
        this.returnTypeInfo = returnTypeInfo;
    }

    public TypeInfo getReturnTypeInfo() {
        return returnTypeInfo;
    }

    public Object getReturnSubjectInstanceName() {
        return returnSubjectInstanceName;
    }

    public void setReturnSubjectInstanceName(Object returnSubjectInstanceName) {
        this.returnSubjectInstanceName = returnSubjectInstanceName;
    }
}
