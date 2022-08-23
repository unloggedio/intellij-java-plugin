package com.insidious.plugin.factory;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.pojo.EventTypeMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCandidateMetadata {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);
    private final List<Parameter> parameters = new LinkedList<>();
    private final List<MethodCallExpression> methodCallExpressions = new LinkedList<>();
    private String fullyQualifiedClassname;
    private String unqualifiedClassname;
    private String packageName;
    private String testMethodName;
    private Parameter testSubject;
    private Parameter returnParameter;
    private long callTimeNanoSecond;
    private String methodName;
    private List<MethodCallExpression> callsList;
    private boolean isArray;

    public static TestCandidateMetadata create(
            List<String> typeHierarchy,
            MethodInfo methodInfo,
            long entryProbeDataId,
            ReplayData replayData
    ) throws APICallException {
        logger.warn("[" + methodInfo.getMethodName() + "] " +
                "create test case metadata for types [" + entryProbeDataId + "] -> entry probe " +
                " types  " + typeHierarchy);
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


//        Map<String, DataInfo> probeInfoMap = replayData.getProbeInfoMap();

        metadata.setMethodName(targetMethodName);


        String testMethodName = "test" + ClassTypeUtils.upperInstanceName(targetMethodName);
        metadata.setTestMethodName(testMethodName);

        // https://gist.github.com/VijayKrishna/6160036
        List<String> methodParameterDescriptions =
                ClassTypeUtils.splitMethodDesc(methodInfo.getMethodDesc());

        String returnParameterDescription = methodParameterDescriptions.remove(methodParameterDescriptions.size() - 1);
        if (methodInfo.getMethodName().equals("<init>")) {
            returnParameterDescription = ClassTypeUtils.getDescriptorName(typeHierarchy.get(0));
        }


        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();


        int entryProbeIndex = 0;
        // going up matching the first event for our method entry probe
        // up is back in time
        while (entryProbeIndex < dataEvents.size()) {
            DataEventWithSessionId eventInfo = dataEvents.get(entryProbeIndex);
            if (eventInfo.getNanoTime() == entryProbeDataId) break;
            entryProbeIndex++;
        }


        DataEventWithSessionId entryProbe = dataEvents.get(entryProbeIndex);
        DataInfo entryProbeInfo = replayData.getProbeInfo(entryProbe.getDataId());
        logger.info("located entry probe at: " + entryProbeIndex + " -- " + entryProbeInfo);

        int currentEntryProbeIndex = entryProbeIndex;
        ScanResult callReturnScanResult = new ScanResult(currentEntryProbeIndex, 0);
        while (true) {

            if (methodInfo.getMethodName().equals("<init>")) {
                logger.info("entry probe is of type method entry <init>");
                callReturnScanResult = searchMethodExitIndex(replayData,
                        callReturnScanResult,
                        List.of(EventType.METHOD_OBJECT_INITIALIZED));
            } else {
                logger.info("entry probe is of type method entry " + methodInfo.getMethodName());
                callReturnScanResult = searchMethodExitIndex(replayData,
                        callReturnScanResult, List.of(EventType.METHOD_NORMAL_EXIT));
            }


            if (callReturnScanResult.getIndex() != -1) {
                break;
            }
            logger.warn("return probe not found, fetching next page: " +
                    replayData.getFilteredDataEventsRequest().getPageInfo());

       DataEventWithSessionId lastEvent = replayData.getDataEvents().get(0);
            if (Objects.equals(replayData.getFilteredDataEventsRequest().getSortOrder(), "ASC")) {
                lastEvent = replayData.getDataEvents().get(replayData.getDataEvents().size() - 1);
            }
            ReplayData nextPage = replayData.fetchEventsPost(lastEvent, 1000);
            if (nextPage.getDataEvents().size() < 2) {
                break;
            }
            callReturnScanResult = new ScanResult(nextPage.getDataEvents().size() - 1,
                    callReturnScanResult.getCallStack());

            replayData.mergeReplayData(nextPage);
        }

        // we are going to do this again, since the index might have shifted from the added pages
        // in the search for return index
        entryProbeIndex = 0;
        // going up matching the first event for our method entry probe
        // up is back in time
        while (entryProbeIndex < dataEvents.size()) {
            DataEventWithSessionId eventInfo = dataEvents.get(entryProbeIndex);
            if (eventInfo.getNanoTime() == entryProbeDataId) break;
            entryProbeIndex++;
        }



//        metadata.setExitProbeIndex(callReturnIndex);
        logger.info("entry probe matched at event: " + entryProbeIndex +
                ", return found " + "at: " + callReturnScanResult.getIndex());

        if (callReturnScanResult.getIndex() == -1) {
            logger.warn("call_return probe not found in the slice: " + entryProbeDataId +
                    " when generating test for method " + targetMethodName + " " +
                    " in class " + typeHierarchy.get(0) + ". Maybe need a bigger " +
                    "slice");
            return metadata;
        }
        long callTime = dataEvents.get(callReturnScanResult.getIndex()).getRecordedAt() - dataEvents.get(entryProbeIndex).getRecordedAt();
        metadata.setCallTimeNanoSecond(callTime);

        List<Parameter> methodParameters =
                searchMethodParameters(replayData, entryProbeIndex, methodParameterDescriptions);

        VariableContainer variableContainer = new VariableContainer();
        for (Parameter methodParameter : methodParameters) {
            variableContainer.add(methodParameter);
        }


        List<MethodCallExpression> callsList =
                searchMethodCallExpressions(replayData, entryProbeIndex, typeHierarchy, variableContainer);

        metadata.setCallList(callsList);

        metadata.addAllParameter(methodParameters);


//        logger.warn("create return parameter from event at index: " + callReturnIndex);
        Parameter returnParameter = ParameterFactory.createReturnValueParameter(callReturnScanResult.getIndex(),
                replayData, returnParameterDescription);
        if (returnParameter.getName() == null || returnParameter.getName().length() == 1) {
            returnParameter.setName(ClassTypeUtils.createVariableName(methodInfo.getMethodName()));
        }

//        if (returnParameter)

        metadata.setReturnParameter(returnParameter);


        String testSubjectInstanceName = ClassTypeUtils.lowerInstanceName(unqualifiedClassName);

//        int paramsToSkip = methodParameters.size();
        int paramsToSkip = 0;
        boolean subjectNameFound = false;


        // identify the variable name on which this method is called
        if (methodInfo.getMethodName().equals("<init>")) {
            metadata.setTestSubject(returnParameter);
            subjectNameFound = true;
        } else {

            int callStack = 0;
            for (int i = entryProbeIndex; i < dataEvents.size(); i += 1) {
                DataEventWithSessionId event = dataEvents.get(i);
                DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
                ClassInfo currentClassInfo = replayData.getClassInfo(probeInfo.getClassId());
                MethodInfo methodInfoLocal = replayData.getMethodInfo(probeInfo.getMethodId());
                EventType eventType = probeInfo.getEventType();


                switch (eventType) {
                    case CALL_PARAM:
                        if (callStack == -1) {
                            paramsToSkip++;
                        }
                        break;
                    case METHOD_NORMAL_EXIT:
                        callStack++;
                        break;
                    case METHOD_ENTRY:
                        callStack--;
                        break;
                    case LOCAL_LOAD:
                    case GET_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                    case NEW_OBJECT_CREATED:
                        if (callStack != -1) {
                            break;
                        }
                        String rawType = probeInfo.getAttribute("Type", "");
                        String valueTypeNameWithSlash = rawType;
                        String valueTypeNameWithDots = "";
                        if (valueTypeNameWithSlash.startsWith("L")) {
                            valueTypeNameWithDots = valueTypeNameWithSlash.substring(1)
                                    .split(";")[0].replace("/", ".");
                        } else {
                            valueTypeNameWithDots = valueTypeNameWithSlash;
                        }
                        if (!typeHierarchy.contains(ClassTypeUtils.getDottedClassName(rawType))) {
                            break;
                        }

                        if (paramsToSkip == 0) {
                            String variableName = ClassTypeUtils.getVariableNameFromProbe(probeInfo,
                                    testSubjectInstanceName);

                            Parameter subjectInstanceParameter;
                            if (eventType == EventType.LOCAL_LOAD) {
                                subjectInstanceParameter = ParameterFactory.createSubjectParameter(i,
                                        replayData, 0);
                            } else {
                                subjectInstanceParameter = ParameterFactory.createSubjectParameter(
                                        i - 1, replayData, 0);
                            }

                            metadata.setTestSubject(subjectInstanceParameter);
                            LoggerUtil.logEvent("SearchSubject", callStack, i,
                                    event, probeInfo, currentClassInfo, methodInfoLocal);


                            subjectNameFound = true;
                            break;
                        }
                        paramsToSkip = paramsToSkip - 1;

                }

                if (subjectNameFound || callStack < -1) {
                    break;
                }

            }
        }


        if (metadata.getReturnParameter().getName() == null
                && !methodInfo.getMethodName().equals("<init>")) {

            String potentialReturnValueName;
            if (targetMethodName.startsWith("get") || targetMethodName.startsWith("set")) {
                if (targetMethodName.length() > 3) {
                    potentialReturnValueName = ClassTypeUtils.lowerInstanceName(targetMethodName.substring(3)) + "Value";
                } else {
                    potentialReturnValueName = "value";
                }
            } else {
                if (targetMethodName.equals("<init>")) {
                    potentialReturnValueName = ClassTypeUtils.createVariableName(metadata.getUnqualifiedClassname());
                } else {
                    potentialReturnValueName = targetMethodName + "Result";
                }
            }

            metadata.getReturnParameter().setName(potentialReturnValueName);
        }


        return metadata;
    }

    private static List<MethodCallExpression>
    searchMethodCallExpressions(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> typeHierarchy,
            /*
            variableContainer keeping a track of local variables and method arguments here so
            that we dont record/mock calls on these objects
             */
            VariableContainer variableContainer
    ) {


        ScanRequest scanRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0),
                ScanRequest.CURRENT_CLASS,
                DirectionType.FORWARDS);

        scanRequest.addListener(EventType.LOCAL_LOAD, new EventTypeMatchListener() {
            @Override
            public void eventMatched(Integer index) {
                Parameter potentialParameter = ParameterFactory.createParameter(
                        index, replayData, 0, null
                );
                variableContainer.add(potentialParameter);
            }
        });

        MethodCallExtractor methodCallExtractor = new MethodCallExtractor(
                replayData, variableContainer, typeHierarchy
        );
        scanRequest.addListener(EventType.CALL, methodCallExtractor);
        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        replayData.eventScan(scanRequest);

        return methodCallExtractor.getCallList();
    }

    private static List<Parameter> searchMethodParameters(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> callParameterDescriptions
    ) {
        logger.info("searchCallParameters starting from [" + entryProbeIndex + "] -> " +
                callParameterDescriptions.size() + " params to be found");

        List<Parameter> methodParameterProbes = new LinkedList<>();
        ScanRequest searchRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0), 0,
                DirectionType.FORWARDS);
        searchRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        searchRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);
        searchRequest.matchUntil(EventType.LABEL);
        searchRequest.matchUntil(EventType.LINE_NUMBER);
        searchRequest.addListener(EventType.METHOD_PARAM, new EventTypeMatchListener() {
            private int paramIndex = 0;

            @Override
            public void eventMatched(Integer callReturnIndex) {
                DataEventWithSessionId event = replayData.getDataEvents().get(callReturnIndex);
                DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
                ClassInfo currentClassInfo = replayData.getClassInfo(probeInfo.getClassId());
                MethodInfo methodInfoLocal =
                        replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));

                LoggerUtil.logEvent("SearchCallParameters", 0, paramIndex,
                        event, probeInfo, currentClassInfo, methodInfoLocal);

                Parameter parameter = ParameterFactory.createMethodArgumentParameter(
                        callReturnIndex, replayData, callParameterDescriptions.size() - paramIndex - 1,
                        callParameterDescriptions.get(paramIndex));
                methodParameterProbes.add(parameter);
                paramIndex++;
            }
        });

        replayData.eventScan(searchRequest);
        logger.info("Found [" + methodParameterProbes.size() + "] parameters");

        return methodParameterProbes;
    }

    private static ScanResult searchMethodExitIndex(
            ReplayData replayData,
            ScanResult entryProbeIndex,
            List<EventType> eventTypeMatch
    ) {
        logger.info("looking for call return index, with entry probe index at: " + entryProbeIndex + " -> " + eventTypeMatch);
        logger.info("replay data has: " + replayData.getDataEvents().size() + " events.");


        ScanRequest searchRequest = new ScanRequest(entryProbeIndex, 0,
                DirectionType.FORWARDS);
        for (EventType typeMatch : eventTypeMatch) {
            searchRequest.matchUntil(typeMatch);
        }

        return replayData.eventScan(searchRequest);
    }

    private void setCallList(List<MethodCallExpression> callsList) {
        this.callsList = callsList;
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

    public List<MethodCallExpression> getCallsList() {
        return callsList;
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

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }
}
