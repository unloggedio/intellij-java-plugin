package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.pojo.EventMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.insidious.plugin.pojo.MethodCallExpression.in;

public class CandidateMetadataFactory {
    public static ObjectRoutineScript toObjectScript(
            TestCandidateMetadata testCandidateMetadata,
            VariableContainer createdVariables
    ) {
        ObjectRoutineScript objectRoutineScript = new ObjectRoutineScript(createdVariables);


        if (testCandidateMetadata.getMainMethod() instanceof MethodCallExpression) {


            MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();


            Map<String, MethodCallExpression> mockedCalls = new HashMap<>();
            Collection<MethodCallExpression> callToMock = testCandidateMetadata.getCallsList();

            if (callToMock.size() > 0) {

                objectRoutineScript.addComment("");
                for (MethodCallExpression methodCallExpression : callToMock) {
                    if (mainMethod.getException() != null && mockedCalls.containsKey(mainMethod.getMethodName())) {
                        continue;
                    }
                    methodCallExpression.writeCommentTo(objectRoutineScript);
                    methodCallExpression.writeMockTo(objectRoutineScript);
                    mockedCalls.put(mainMethod.getMethodName(), methodCallExpression);
                }
                objectRoutineScript.addComment("");
                objectRoutineScript.addComment("");
            }


            List<MethodCallExpression> staticCallsList = testCandidateMetadata.getStaticCalls();

            if (staticCallsList != null && staticCallsList.size() > 0) {

                Map<String, Boolean> doneMap = new HashMap<>();

                for (MethodCallExpression methodCallExpression : staticCallsList) {

                    Parameter subject = methodCallExpression.getSubject();

                    StringBuilder callBuilder = new StringBuilder();
                    callBuilder.append(subject.getName())
                            .append(".")
                            .append(methodCallExpression.getMethodName());

                    for (Parameter parameter : methodCallExpression.getArguments().all()) {
                        callBuilder.append(parameter.getValue());
                    }


                    String mockedFieldsKey = callBuilder.toString();

                    if (!doneMap.containsKey(mockedFieldsKey)) {
                        if (!objectRoutineScript.getCreatedVariables().contains(subject.getName())) {
                            subject.setContainer(true);
                            Parameter childParameter = new Parameter();
                            childParameter.setType(subject.getType());
                            subject.setType("org.mockito.MockedStatic");
                            subject.getTemplateMap().put("E", childParameter);
                            in(objectRoutineScript)
                                    .assignVariable(subject)
                                    .writeExpression(MethodCallExpressionFactory
                                            .MockStaticClass(ClassName.bestGuess(childParameter.getType())))
                                    .endStatement();
                        }

                        doneMap.put(mockedFieldsKey, true);

                    }


                    objectRoutineScript.addComment("Add mock for static method call: " + methodCallExpression);
                    if (!methodCallExpression.getReturnValue().getType().equals("V")) {
                        methodCallExpression.writeMockTo(objectRoutineScript);
                    }


                }


            }

            if (mainMethod.getMethodName().equals("<init>")) {
                objectRoutineScript.getCreatedVariables().add(mainMethod.getReturnValue());
            }

            mainMethod.writeTo(objectRoutineScript);


        } else {
            testCandidateMetadata.getMainMethod().writeTo(objectRoutineScript);
        }
        objectRoutineScript.addComment("");
        return objectRoutineScript;

    }

    public final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);

    public static TestCandidateMetadata create(
            List<String> typeHierarchy,
            MethodInfo methodInfo,
            long entryProbeDataId,
            ReplayData replayData
    ) throws APICallException, SessionNotSelectedException {
        final String methodName = methodInfo.getMethodName();
        logger.warn("[" + methodName + "] " +
                "create test case metadata for types [" + entryProbeDataId + "] -> entry probe " +
                " types  " + typeHierarchy);
        TestCandidateMetadata metadata = new TestCandidateMetadata();


//        final String className = methodInfo.getClassName();
        String targetMethodName = methodName;

        if (targetMethodName.startsWith("lambda$")) {
            // this function is a transformation of the original user function by jvm and the
            // name looks like "lambda$ORIGINAL_NAME$<int>"
            targetMethodName = targetMethodName.split("\\$")[1];
        }

//        metadata.setFullyQualifiedClassname(typeHierarchy.get(0));

        String[] classNameParts = typeHierarchy.get(0).split("\\.");
        String unqualifiedClassName = classNameParts[classNameParts.length - 1];
//        String packageName = StringUtil.join(classNameParts, ".");
//        int lastDotIndex = packageName.lastIndexOf(".");
//        if (lastDotIndex > -1) {
//            packageName = packageName.substring(0, lastDotIndex);
//        } else {
//            packageName = "";
//        }

        // https://gist.github.com/VijayKrishna/6160036
        List<String> methodParameterDescriptions =
                ClassTypeUtils.splitMethodDesc(methodInfo.getMethodDesc());

        String returnParameterDescription = methodParameterDescriptions.remove(methodParameterDescriptions.size() - 1);
        if (methodName.equals("<init>")) {
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

        metadata.setEntryProbeIndex(entryProbeIndex);


        DataEventWithSessionId entryProbe = dataEvents.get(entryProbeIndex);
        DataInfo entryProbeInfo = replayData.getProbeInfo(entryProbe.getDataId());
        logger.info("located entry probe at: " + entryProbeIndex + " -- " + entryProbeInfo);

        int currentEntryProbeIndex = entryProbeIndex;
        ScanResult callReturnScanResult = new ScanResult(currentEntryProbeIndex, 0, false);
        int pageSize = 3000;
        while (true) {

            if (methodName.equals("<init>")) {
                logger.info("entry probe is of type method entry <init>");
                callReturnScanResult = searchMethodExitIndex(
                        replayData,
                        callReturnScanResult,
                        List.of(EventType.METHOD_OBJECT_INITIALIZED)
                );
            } else {
                logger.info("entry probe is of type method entry " + methodName);
                callReturnScanResult =
                        searchMethodExitIndex(
                                replayData,
                                callReturnScanResult,
                                List.of(
                                        EventType.METHOD_NORMAL_EXIT,
                                        EventType.METHOD_EXCEPTIONAL_EXIT
                                )
                        );
            }


            if (callReturnScanResult.getIndex() != -1) {
                break;
            }
            logger.warn("return probe not found, fetching next page: " + pageSize);

            DataEventWithSessionId lastEvent = replayData.getDataEvents().get(0);
            if (Objects.equals(replayData.getFilteredDataEventsRequest().getSortOrder(), "ASC")) {
                lastEvent = replayData.getDataEvents().get(replayData.getDataEvents().size() - 1);
            }
            ReplayData nextPage = replayData.fetchEventsPost(lastEvent, pageSize);
            logger.warn("next page has " + nextPage.getDataEvents().size() + " events");
            pageSize = pageSize * 2;
            if (pageSize > 1000000) {
                pageSize = 1000000;
            }
            if (nextPage.getDataEvents().size() < 2) {
                break;
            }
            callReturnScanResult = new ScanResult(nextPage.getDataEvents().size() - 1,
                    callReturnScanResult.getCallStack(), false);

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

        metadata.setEntryProbeIndex(entryProbeIndex);
        metadata.setExitProbeIndex(callReturnScanResult.getIndex());


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

        VariableContainer methodArguments = searchMethodArguments(replayData, entryProbeIndex, methodParameterDescriptions);

        VariableContainer fieldsContainer = searchMethodFieldsAccessed(replayData, entryProbeIndex);

        VariableContainer variableContainer = new VariableContainer();
        for (Parameter methodParameter : methodArguments.all()) {
            variableContainer.add(methodParameter);
        }
        metadata.setVariables(variableContainer);


        metadata.addAllFields(fieldsContainer);


//        logger.warn("create return parameter from event at index: " + callReturnIndex);
        Parameter returnParameter = ParameterFactory.createMethodArgumentParameter(callReturnScanResult.getIndex(),
                replayData, 0, returnParameterDescription);
        if (returnParameter.getName() == null || returnParameter.getName().length() == 1) {
            if (methodName.equals("<init>")) {
                returnParameter.setName(ClassTypeUtils.createVariableName(methodName));
            }
        }


        String testSubjectInstanceName = ClassTypeUtils.lowerInstanceName(unqualifiedClassName);

//        int paramsToSkip = methodParameters.size();
        int paramsToSkip = 0;
        boolean subjectNameFound = false;


        // identify the variable name on which this method is called
        if (methodName.equals("<init>")) {
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
                        String rawType = ClassTypeUtils.getDottedClassName(
                                probeInfo.getAttribute("Type", "V"));

                        if (!typeHierarchy.contains(rawType)) {
                            paramsToSkip = paramsToSkip - 1;
                            break;
                        }

                        if (paramsToSkip == 0) {
                            String variableName = ClassTypeUtils.getVariableNameFromProbe(probeInfo,
                                    testSubjectInstanceName);

                            Parameter subjectInstanceParameter;
                            if (eventType == EventType.LOCAL_LOAD) {
                                subjectInstanceParameter = ParameterFactory.createParameter(i,
                                        replayData, 0, null);
                            } else {
                                subjectInstanceParameter = ParameterFactory.createParameter(
                                        i - 1, replayData, 0, null);
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

        MethodCallExpression mainMethodExpression = new MethodCallExpression(
                targetMethodName, metadata.getTestSubject(),
                methodArguments, returnParameter, 0);

        if (mainMethodExpression.getReturnValue().getName() == null
                && !methodName.equals("<init>")) {

            String potentialReturnValueName =
                    ClassTypeUtils.createVariableNameFromMethodName(methodName,
                            metadata.getFullyQualifiedClassname());

            mainMethodExpression.getReturnValue().setName(potentialReturnValueName);
        }


        metadata.setMainMethod(mainMethodExpression);

        return metadata;
    }

    public static MethodCallExpression buildObject(ReplayData replayData,
                                                   final Parameter targetParameter) {
        logger.warn("reconstruct object: " + targetParameter);

        switch (targetParameter.getType()) {
            case "okhttp3.Response":

                AtomicReference<Parameter> responseBodyProbe = new AtomicReference<>();
                AtomicReference<Parameter> responseBodyStringProbe = new AtomicReference<>();

                ScanRequest scanRequest = new ScanRequest(
                        new ScanResult(targetParameter.getIndex(), 0, false), 0, DirectionType.FORWARDS);

                scanRequest.addListener(EventType.CALL_RETURN, (index, matchedStack) -> {


                    DataEventWithSessionId callReturnEvent = replayData.getDataEvents().get(index);
                    int callEventIndex = replayData.getPreviousProbeIndex(index);
                    DataEventWithSessionId callEvent = replayData.getDataEvents().get(callEventIndex);
                    DataInfo callEventProbe = replayData.getProbeInfo(callEvent.getDataId());
                    if (callEventProbe.getEventType() != EventType.CALL) {
                        return;
                    }


                    DataInfo callReturnProbeInfo =
                            replayData.getProbeInfo(callReturnEvent.getDataId());

                    @NotNull String returnType = ClassTypeUtils.getDottedClassName(
                            callReturnProbeInfo.getAttribute("Type", "V")
                    );

                    String callOwner = ClassTypeUtils.getDottedClassName(callEventProbe.getAttribute("Owner", ""));
                    String methodName = callEventProbe.getAttribute("Name", null);
                    assert methodName != null;


                    switch (returnType) {
                        case "java.lang.String":
                            if (!methodName.equals("string")) {
                                return;
                            }
                            if (responseBodyProbe.get() == null) {
                                logger.warn("body probe is still missing so this cannot be the " +
                                        "string we are looking for");
                            }
                            Parameter responseBodyProbeInstance = responseBodyProbe.get();
                            if (responseBodyProbeInstance.getProb().getValue() != callEvent.getValue()) {
                                logger.warn("this is not the response body string from the object" +
                                        " we are building: " + callEvent + " -- " + callEventProbe);
                                return;
                            }
                            responseBodyStringProbe.set(ParameterFactory.createMethodArgumentParameter(
                                    index, replayData, 0, "java.lang.String"
                            ));
                            break;
                        case "okhttp3.ResponseBody":
                            if (!methodName.equals("body")) {
                                return;
                            }
                            if (callEvent.getValue() != targetParameter.getProb().getValue()) {
                                logger.warn("this is not the response body from the object we are" +
                                        " building: " + callEvent + " -- " + callEventProbe);
                                return;
                            }
                            if (responseBodyProbe.get() != null) {
                                return;
                            }
                            responseBodyProbe.set(ParameterFactory.createMethodArgumentParameter(
                                    index, replayData, 0, "okhttp3.ResponseBody"
                            ));
                            break;
                    }

                });

                scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
                scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

                replayData.ScanForEvents(scanRequest);

                if (responseBodyStringProbe.get() == null) {
                    logger.warn("response body string value not found for okhttp.Response: " + targetParameter);
                }

                if (responseBodyProbe.get() == null) {
                    logger.warn("response body object value not found for okhttp.Response: " + targetParameter);
                }

                VariableContainer variableContainer = VariableContainer.from(
                        List.of(responseBodyStringProbe.get())
                );

                return MethodCallExpressionFactory.MethodCallExpression(
                        "buildOkHttpResponseFromString", null, variableContainer,
                        targetParameter);
            case "java.util.List":


                ScanRequest identifyIteratorScanRequest = new ScanRequest(
                        new ScanResult(targetParameter.getIndex(), 0, false), ScanRequest.CURRENT_CLASS,
                        DirectionType.FORWARDS
                );

                AtomicInteger iteratorProbe = new AtomicInteger(-1);
                AtomicInteger nextValueProbe = new AtomicInteger(-1);
                AtomicReference<Parameter> containedParameterIterator = new AtomicReference<>();
                AtomicReference<Parameter> nextValueParameter = new AtomicReference<>();
                identifyIteratorScanRequest.addListener(targetParameter.getProb().getValue(),

                        new EventMatchListener() {
                            @Override
                            public void eventMatched(Integer index, int matchedStack) {
                                DataEventWithSessionId event = replayData.getDataEvents().get(index);
                                DataInfo probeInfo = replayData.getProbeInfo(
                                        event.getDataId()
                                );
                                if (probeInfo.getEventType() == EventType.CALL) {
                                    String methodName = probeInfo.getAttribute("Name", null);
                                    Parameter containedParameter;
                                    DataInfo callReturnProbeInfo;

                                    switch (methodName) {
                                        case "iterator":
                                            if (iteratorProbe.get() != -1) {
                                                return;
                                            }
                                            assert methodName != null;


                                            int nextProbeIndex = replayData.getNextProbeIndex(index);
                                            DataEventWithSessionId callReturnProbe = replayData
                                                    .getDataEvents()
                                                    .get(nextProbeIndex);
                                            callReturnProbeInfo = replayData.getProbeInfo(callReturnProbe.getDataId());
                                            assert callReturnProbeInfo.getEventType() == EventType.CALL_RETURN;
                                            nextValueProbe.set(index);

                                            Parameter returnParam = ParameterFactory.createMethodArgumentParameter(
                                                    nextProbeIndex, replayData, 0, null);
                                            containedParameterIterator.set(returnParam);

                                            identifyIteratorScanRequest.addListener(returnParam.getProb().getValue()
                                                    , this);
                                            iteratorProbe.set(index);

                                            break;
                                        case "next":
                                            if (iteratorProbe.get() == -1) {
                                                return;
                                            }
                                            if (event.getValue() == containedParameterIterator.get().getProb().getValue()) {
                                                int nextProbeIndex1 = replayData.getNextProbeIndex(index);
                                                callReturnProbe = replayData.getDataEvents()
                                                        .get(nextProbeIndex1);
                                                callReturnProbeInfo = replayData.getProbeInfo(callReturnProbe.getDataId());
                                                assert callReturnProbeInfo.getEventType() == EventType.CALL_RETURN;
                                                nextValueProbe.set(index);

                                                containedParameter = ParameterFactory.createMethodArgumentParameter(
                                                        nextProbeIndex1, replayData, 0, null
                                                );
                                                identifyIteratorScanRequest.addListener(callReturnProbe.getValue(), this);
                                                nextValueParameter.set(containedParameter);

                                            }
                                    }
                                } else if (probeInfo.getEventType() == EventType.LOCAL_STORE || probeInfo.getEventType() == EventType.LOCAL_LOAD) {
                                    if (nextValueParameter.get() == null) {
                                        return;
                                    }
                                    if (event.getValue() != nextValueParameter.get().getProb().getValue()) {
                                        return;
                                    }
                                    Parameter paramWithNameAndType = ParameterFactory.createMethodArgumentParameter(
                                            index, replayData, 0, null
                                    );
                                    if (paramWithNameAndType.getName().startsWith("\\(")) {
                                        return;
                                    }
                                    if (paramWithNameAndType.getType() != null) {
                                        Parameter existingValue = nextValueParameter.get();
                                        paramWithNameAndType.getProb().setSerializedValue(
                                                existingValue.getProb().getSerializedValue()
                                        );
                                        nextValueParameter.set(paramWithNameAndType);
                                        identifyIteratorScanRequest.abort();
                                    }
                                }
                            }
                        });

                replayData.ScanForValue(identifyIteratorScanRequest);
                Parameter nextValueParam = nextValueParameter.get();
                targetParameter.setTemplateParameter("E", nextValueParam);

                return null;
            default:
                break;
        }
        return null;
    }

    public static VariableContainer searchMethodFieldsAccessed(
            ReplayData replayData,
            int entryProbeIndex
    ) {
        VariableContainer variableContainer = new VariableContainer();


        ScanRequest scanRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0, false),
                ScanRequest.CURRENT_CLASS, DirectionType.FORWARDS);

        scanRequest.addListener(EventType.GET_INSTANCE_FIELD_RESULT, (index, matchedStack) -> {
            DataEventWithSessionId event = replayData.getDataEvents().get(index);
            DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());

            String fieldType = probeInfo.getAttribute("Type", "V");
            Parameter fieldParameter = ParameterFactory.createParameter(index, replayData, 0, fieldType);
            variableContainer.add(fieldParameter);
        });


        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        replayData.ScanForEvents(scanRequest);

        return variableContainer;

    }

    public static List<MethodCallExpression>
    searchMethodCallExpressions(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> typeHierarchy,
            /*
            variableContainer keeping a track of local variables and method arguments here so
            that we dont record/mock calls on these objects
             */
            VariableContainer variableContainer,
            List<String> noMockClassList
    ) {


        ScanRequest scanRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0, false),
                ScanRequest.CURRENT_CLASS, DirectionType.FORWARDS);

        scanRequest.addListener(EventType.LOCAL_LOAD, (index, matchedStack) -> {
            Parameter potentialParameter = ParameterFactory.createParameter(
                    index, replayData, 0, null
            );
            variableContainer.add(potentialParameter);
        });

        MethodCallExtractor methodCallExtractor = new MethodCallExtractor(
                replayData, variableContainer, typeHierarchy, noMockClassList
        );
        scanRequest.addListener(EventType.CALL, methodCallExtractor);
        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        replayData.ScanForEvents(scanRequest);

        return methodCallExtractor.getCallList();
    }

    public static VariableContainer searchMethodArguments(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> callParameterDescriptions
    ) {
        logger.info("searchCallParameters starting from [" + entryProbeIndex + "] -> " +
                callParameterDescriptions.size() + " params to be found");

        List<Parameter> methodParameterProbes = new LinkedList<>();
        ScanRequest searchRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0, false), 0,
                DirectionType.FORWARDS);
        searchRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        searchRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);
        searchRequest.matchUntil(EventType.LABEL);
        searchRequest.matchUntil(EventType.LINE_NUMBER);
        searchRequest.addListener(EventType.METHOD_PARAM, new EventMatchListener() {
            private int paramIndex = 0;

            @Override
            public void eventMatched(Integer callReturnIndex, int matchedStack) {
                DataEventWithSessionId event = replayData.getDataEvents().get(callReturnIndex);
                DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
                ClassInfo currentClassInfo = replayData.getClassInfo(probeInfo.getClassId());
                MethodInfo methodInfoLocal =
                        replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));

                LoggerUtil.logEvent("SearchCallParameters", 0, paramIndex,
                        event, probeInfo, currentClassInfo, methodInfoLocal);

                String expectedParameterType = callParameterDescriptions.get(paramIndex);
                Parameter parameter = ParameterFactory.createMethodArgumentParameter(
                        callReturnIndex, replayData, callParameterDescriptions.size() - paramIndex - 1,
                        expectedParameterType);
                methodParameterProbes.add(parameter);
                paramIndex++;
            }
        });

        replayData.ScanForEvents(searchRequest);
        logger.info("Found [" + methodParameterProbes.size() + "] parameters");

        return VariableContainer.from(methodParameterProbes);
    }

    public static ScanResult searchMethodExitIndex(
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

        return replayData.ScanForEvents(searchRequest);
    }

    public static List<MethodCallExpression>
    searchStaticMethodCallExpression(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> typeHierarchy,
            VariableContainer variableContainer,
            List<String> noMockClassList
    ) {
        ScanRequest scanRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0, false),
                ScanRequest.ANY_STACK,
                DirectionType.FORWARDS);

        scanRequest.addListener(EventType.LOCAL_LOAD, (index, matchedStack) -> {
            Parameter potentialParameter = ParameterFactory.createParameter(
                    index, replayData, 0, null
            );
            variableContainer.add(potentialParameter);
        });

        StaticMethodCallExtractor methodCallExtractor = new StaticMethodCallExtractor(
                replayData, variableContainer, typeHierarchy, noMockClassList
        );
        scanRequest.addListener(EventType.CALL, methodCallExtractor);
        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_OBJECT_INITIALIZED);

        replayData.ScanForEvents(scanRequest);

        return methodCallExtractor.getCallList();
    }
}
