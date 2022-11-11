package com.insidious.plugin.extension.model;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.EventMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import net.openhft.chronicle.map.ChronicleMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplayData {
    private static final Logger logger = LoggerUtil.getInstance(ReplayData.class);
    private final FilteredDataEventsRequest filteredDataEventsRequest;
    List<DataEventWithSessionId> dataEvents;
    ChronicleMap<Integer, ClassInfo> classInfoMap;
    ChronicleMap<Integer, DataInfo> probeInfoMap;
    Map<Long, StringInfo> stringInfoMap;
    Map<Long, ObjectInfo> objectInfoMap;
    Map<Integer, TypeInfo> typeInfoMap;
    ChronicleMap<Integer, MethodInfo> methodInfoMap;
    private VideobugClientInterface client;

    public ReplayData(
            VideobugClientInterface client,
            FilteredDataEventsRequest filteredDataEventsRequest,
            List<DataEventWithSessionId> dataList,
            ChronicleMap<Integer, ClassInfo> classInfo,
            ChronicleMap<Integer, DataInfo> dataInfo,
            Map<Long, StringInfo> stringInfo,
            Map<Long, ObjectInfo> objectInfoMap,
            Map<Integer, TypeInfo> typeInfoMap,
            ChronicleMap<Integer, MethodInfo> methodInfoMap
    ) {
        this.client = client;
        this.filteredDataEventsRequest = filteredDataEventsRequest;
        dataEvents = dataList;
        classInfoMap = classInfo;
        probeInfoMap = dataInfo;
        stringInfoMap = stringInfo;
        this.objectInfoMap = objectInfoMap;
        this.typeInfoMap = typeInfoMap;
        this.methodInfoMap = methodInfoMap;
    }

    public Map<Long, ObjectInfo> getObjectInfoMap() {
        return objectInfoMap;
    }

    public Map<Integer, TypeInfo> getTypeInfoMap() {
        return typeInfoMap;
    }

    public List<DataEventWithSessionId> getDataEvents() {
        return dataEvents;
    }

    public void setDataEvents(List<DataEventWithSessionId> objectEventsReverse) {
        this.dataEvents = objectEventsReverse;
    }

    public ReplayData getNextPage() throws SessionNotSelectedException {
        FilteredDataEventsRequest filteredDataEventsRequestClone =
                FilteredDataEventsRequest.copyOf(filteredDataEventsRequest);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        PageInfo.Order pageOrder = pageInfo.isAsc() ? PageInfo.Order.ASC : PageInfo.Order.DESC;
        pageInfo = new PageInfo(pageInfo.getNumber() + 1, pageInfo.getSize(), pageOrder);

        filteredDataEventsRequestClone.setPageInfo(pageInfo);
        return client.fetchObjectHistoryByObjectId(
                filteredDataEventsRequestClone
        );
    }

    public void setClient(VideobugClientInterface client) {
        this.client = client;
    }

    public Map<Integer, ClassInfo> getClassInfoMap() {
        return classInfoMap;
    }

    public Map<Long, StringInfo> getStringInfoMap() {
        return stringInfoMap;
    }

    public ReplayData fetchEventsPre(DataEventWithSessionId event, int size) throws SessionNotSelectedException {
        PageInfo paginationOlder = new PageInfo(0, size, PageInfo.Order.DESC);
        paginationOlder.setBufferSize(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getNanoTime());
        filterRequest.setPageInfo(paginationOlder);
        return client.fetchObjectHistoryByObjectId(filterRequest);
    }

    public ReplayData fetchEventsPost(DataEventWithSessionId event, int size) throws SessionNotSelectedException {
        assert client.getCurrentSession() != null;
        PageInfo pageNext = new PageInfo(0, size, PageInfo.Order.ASC);
        pageNext.setBufferSize(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getNanoTime());
        filterRequest.setPageInfo(pageNext);
        return client.fetchObjectHistoryByObjectId(filterRequest);
    }

    public TypeInfo getTypeInfoByName(String type) {
        return client.getTypeInfoByName(client.getCurrentSession().getSessionId(), type);
    }


    // A function to scan events starting from some particular index and in a particular
    // direction. the scanner should call back all event listeners on matching events. the scan
    // stops when matchUntil events are hit, the index where the first matchUntil event was
    // matched will be returned
    public ScanResult ScanForEvents(ScanRequest scanRequest) {

        int direction = 1;

        // forwards is -1 for is because we assume the replay data is in descending order of
        // event id
        if (filteredDataEventsRequest.getPageInfo().isDesc()) {

            if (scanRequest.getDirection() == DirectionType.FORWARDS) {
                direction = -1;
            }
        } else {
            if (scanRequest.getDirection() == DirectionType.BACKWARDS) {
                direction = -1;
            }

        }


        ScanResult entryProbeIndex = scanRequest.getStartIndex();
        int callReturnIndex = entryProbeIndex.getIndex() + direction;

        int callStack = entryProbeIndex.getCallStack();

        Integer searchRequestCallStack = scanRequest.getCallStack();

        DataEventWithSessionId firstEvent = dataEvents.get(callReturnIndex);
        DataInfo probeInfo = probeInfoMap.get(firstEvent.getDataId());
        ClassInfo firstClass = classInfoMap.get(Long.valueOf(probeInfo.getClassId()));
        TypeInfo typeInfo = getTypeInfoByName(firstClass.getClassName());
        List<String> typeLadder = buildHierarchyFromType(typeInfo);


        if (scanRequest.hasValueListeners()) {

            Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
            while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
                DataEventWithSessionId event = dataEvents.get(callReturnIndex);
                probeInfo = probeInfoMap.get(event.getDataId());
                EventType eventType = probeInfo.getEventType();
                ClassInfo classInfo = classInfoMap.get(Long.valueOf(probeInfo.getClassId()));
                boolean stackMatch = callStack == 0;
                int callStackMatched = callStack;
                if (searchRequestCallStack == ScanRequest.CURRENT_CLASS) {
                    stackMatch = firstClass.getClassId() == classInfo.getClassId();
                    if (!stackMatch) {
                        if (typeLadder.contains(ClassTypeUtils.getDottedClassName(classInfo.getClassName()))) {
                            stackMatch = true;
                            callStackMatched = ScanRequest.CURRENT_CLASS;
                        }
                    }
                } else if (searchRequestCallStack == ScanRequest.ANY_STACK) {
                    stackMatch = true;
                    callStackMatched = ScanRequest.ANY_STACK;
                }

                scanRequest.onEvent(stackMatch, callStackMatched, eventType, callReturnIndex);

                if (callStack == 0 && matchUntilEvent.contains(eventType) || scanRequest.isAborted()) {
                    return new ScanResult(callReturnIndex, callStack, true);
                }

                if (eventType == EventType.METHOD_ENTRY) {
                    callStack += direction;
                } else if (
                        eventType == EventType.METHOD_NORMAL_EXIT ||
                                eventType == EventType.METHOD_EXCEPTIONAL_EXIT) {
                    callStack -= direction;
                    if (eventType == EventType.METHOD_EXCEPTIONAL_EXIT &&
                            matchUntilEvent.contains(EventType.METHOD_EXCEPTIONAL_EXIT) && callStack == 0) {
                        return new ScanResult(callReturnIndex, callStack, true);
                    }
                }

                callReturnIndex += direction;

            }


        } else {

            if (scanRequest.hasAllEventListener()) {


                Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
                while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
                    DataEventWithSessionId event = dataEvents.get(callReturnIndex);
                    probeInfo = probeInfoMap.get(event.getDataId());
                    EventType eventType = probeInfo.getEventType();

                    scanRequest.onAllEvent(callStack, callReturnIndex);

                    if (callStack == 0 && matchUntilEvent.contains(eventType) || scanRequest.isAborted()) {
                        return new ScanResult(callReturnIndex, callStack, true);
                    }

                    if (eventType == EventType.METHOD_ENTRY) {
                        callStack += direction;
                    } else if (
                            eventType == EventType.METHOD_NORMAL_EXIT ||
                                    eventType == EventType.METHOD_EXCEPTIONAL_EXIT) {
                        callStack -= direction;
                        if (eventType == EventType.METHOD_EXCEPTIONAL_EXIT &&
                                matchUntilEvent.contains(EventType.METHOD_EXCEPTIONAL_EXIT) && callStack == 0) {
                            return new ScanResult(callReturnIndex, callStack, true);
                        }
                    }

                    callReturnIndex += direction;

                }

            } else {


                Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
                while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
                    DataEventWithSessionId event = dataEvents.get(callReturnIndex);
                    probeInfo = probeInfoMap.get(event.getDataId());
                    EventType eventType = probeInfo.getEventType();

                    if (callStack == 0 && matchUntilEvent.contains(eventType) || scanRequest.isAborted()) {
                        return new ScanResult(callReturnIndex, callStack, true);
                    }

                    if (eventType == EventType.METHOD_ENTRY) {
                        callStack += direction;
                    } else if (
                            eventType == EventType.METHOD_NORMAL_EXIT ||
                                    eventType == EventType.METHOD_EXCEPTIONAL_EXIT) {
                        callStack -= direction;
                        if (eventType == EventType.METHOD_EXCEPTIONAL_EXIT &&
                                matchUntilEvent.contains(EventType.METHOD_EXCEPTIONAL_EXIT) && callStack == 0) {
                            return new ScanResult(callReturnIndex, callStack, true);
                        }
                    }

                    callReturnIndex += direction;

                }
            }


        }


        // when not found
        return new ScanResult(callReturnIndex, callStack, false);

    }


    // A function to scan events starting from some particular index and in a particular
    // direction. the scanner should callback any event listners on matching events. the scan
    // stops when matchUntil events are hit, the index where the first matchUntil event was
    // matched will be returned
    public ScanResult ScanForValue(ScanRequest scanRequest) {

        int direction = 1;

        // forwards is -1 for is because we assume the replay data is in descending order of
        // event id
        if (filteredDataEventsRequest.getPageInfo().isDesc()) {

            if (scanRequest.getDirection() == DirectionType.FORWARDS) {
                direction = -1;
            }
        } else {
            if (scanRequest.getDirection() == DirectionType.BACKWARDS) {
                direction = -1;
            }

        }


        ScanResult entryProbeIndex = scanRequest.getStartIndex();
        int callReturnIndex = entryProbeIndex.getIndex() + direction;

        int callStack = entryProbeIndex.getCallStack();

        Integer searchRequestCallStack = scanRequest.getCallStack();

        DataEventWithSessionId firstEvent = dataEvents.get(callReturnIndex);
        DataInfo probeInfo = probeInfoMap.get(firstEvent.getDataId());
        ClassInfo firstClass = classInfoMap.get(Long.valueOf(probeInfo.getClassId()));
        TypeInfo typeInfo = getTypeInfoByName(firstClass.getClassName());
        List<String> typeLadder = buildHierarchyFromType(typeInfo);


        Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
        while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
            DataEventWithSessionId event = dataEvents.get(callReturnIndex);
            probeInfo = probeInfoMap.get(event.getDataId());
            EventType eventType = probeInfo.getEventType();
            ClassInfo classInfo = classInfoMap.get(Long.valueOf(probeInfo.getClassId()));
            boolean stackMatch = callStack == 0;
            int callStackMatched = callStack;
            if (searchRequestCallStack == ScanRequest.CURRENT_CLASS) {
                stackMatch = firstClass.getClassId() == classInfo.getClassId();
                if (!stackMatch) {
                    if (typeLadder.contains(ClassTypeUtils.getDottedClassName(classInfo.getClassName()))) {
                        stackMatch = true;
                        callStackMatched = ScanRequest.CURRENT_CLASS;
                    }
                }
            } else if (searchRequestCallStack == ScanRequest.ANY_STACK) {
                stackMatch = true;
                callStackMatched = ScanRequest.ANY_STACK;
            }

            scanRequest.onValue(stackMatch, callStackMatched, event.getValue(), callReturnIndex);

            if (callStack == 0 && matchUntilEvent.contains(eventType) || scanRequest.isAborted()) {
                return new ScanResult(callReturnIndex, callStack, true);
            }

            if (eventType == EventType.METHOD_ENTRY) {
                callStack += direction;
            } else if (
                    eventType == EventType.METHOD_NORMAL_EXIT ||
                            eventType == EventType.METHOD_EXCEPTIONAL_EXIT) {
                callStack -= direction;
                if (eventType == EventType.METHOD_EXCEPTIONAL_EXIT &&
                        matchUntilEvent.contains(EventType.METHOD_EXCEPTIONAL_EXIT) && callStack == 0) {
                    return new ScanResult(callReturnIndex, callStack, true);
                }
            }

            callReturnIndex += direction;

        }
        // when not found
        return new ScanResult(callReturnIndex, callStack, false);

    }


    public Object getValueByObjectId(DataEventWithSessionId event) {
        long probeValue = event.getValue();
        String probeValueString = String.valueOf(probeValue);
        ObjectInfo objectInfo = objectInfoMap.get(probeValueString);

        if (objectInfo == null) {
            return probeValueString;
        }
        if (stringInfoMap.containsKey(probeValueString)) {
            String strContent = stringInfoMap.get(probeValueString).getContent();
            return "\"" + StringUtil.escapeQuotes(strContent) + "\"";
        } else {
            return probeValueString;
        }
    }


    public List<String> buildHierarchyFromTypeName(String expectedParameterType) {

        if (expectedParameterType == null) {
            return List.of();
        }

        if (expectedParameterType.startsWith("L")) {
            expectedParameterType = ClassTypeUtils.getDottedClassName(expectedParameterType);
        }

        TypeInfo typeInfo = getTypeInfoByName(expectedParameterType);
        List<String> typeHierarchy = buildHierarchyFromType(typeInfo);

        return typeHierarchy;
    }

    public List<String> buildHierarchyFromType(TypeInfo typeInfo) {
        List<String> typeHierarchy = new LinkedList<>();
        typeHierarchy.add(typeInfo.getTypeNameFromClass());
        TypeInfo typeInfoToAdd = typeInfo;
        while (typeInfoToAdd != null && typeInfoToAdd.getSuperClass() != -1) {
            String className = typeInfoToAdd.getTypeNameFromClass();

            if (!typeHierarchy.contains(className)) {
                typeHierarchy.add(className);
            }

            for (int anInterface : typeInfoToAdd.getInterfaces()) {
                TypeInfo interfaceType = client.getSessionInstance().getTypeInfo(anInterface);
//                TypeInfo interfaceType = typeInfoMap.get(String.valueOf(anInterface));
                String interfaceName = interfaceType.getTypeNameFromClass();
                if (!typeHierarchy.contains(interfaceName)) {
                    typeHierarchy.add(interfaceName);
                }
            }

            typeInfoToAdd = typeInfoMap.get(String.valueOf(typeInfoToAdd.getSuperClass()));
        }
        return typeHierarchy;
    }

    public DataInfo getProbeInfo(long id) {
        return probeInfoMap.get((int)id);
    }

    public TypeInfo getTypeInfo(long id) {
        return typeInfoMap.get(id);
    }

    public StringInfo getStringInfo(long id) {
        return stringInfoMap.get(id);
    }

    public ObjectInfo getObjectInfo(long id) {
        return objectInfoMap.get(id);
    }

    public ClassInfo getClassInfo(long id) {
        return classInfoMap.get((int)id);
    }

    public MethodInfo getMethodInfo(long methodId) {
        return methodInfoMap.get((int)methodId);
    }

    public ReplayData getPreviousPage() throws SessionNotSelectedException {
        FilteredDataEventsRequest filteredDataEventsRequestClone =
                FilteredDataEventsRequest.copyOf(filteredDataEventsRequest);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        PageInfo.Order pageOrder = pageInfo.isAsc() ? PageInfo.Order.ASC : PageInfo.Order.DESC;
        int newPageNumber = pageInfo.getNumber() - 1;
        if (newPageNumber < 0) {
            newPageNumber = 0;
        }
        pageInfo = new PageInfo(newPageNumber, pageInfo.getSize(), pageOrder);

        filteredDataEventsRequestClone.setPageInfo(pageInfo);
        return client.fetchObjectHistoryByObjectId(
                filteredDataEventsRequestClone
        );
    }

    public ReplayData getPage(int newPageNumber) throws SessionNotSelectedException {
        FilteredDataEventsRequest filteredDataEventsRequestClone =
                FilteredDataEventsRequest.copyOf(filteredDataEventsRequest);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        PageInfo.Order pageOrder = pageInfo.isAsc() ? PageInfo.Order.ASC : PageInfo.Order.DESC;

        pageInfo = new PageInfo(newPageNumber, pageInfo.getSize(), pageOrder);

        filteredDataEventsRequestClone.setPageInfo(pageInfo);
        return client.fetchObjectHistoryByObjectId(
                filteredDataEventsRequestClone
        );
    }

    public int getNextProbeIndex(Integer probeIndex) {
        if (filteredDataEventsRequest.getPageInfo().isAsc()) {
            return probeIndex + 1;
        } else {
            return probeIndex - 1;
        }
    }

    public int getPreviousProbeIndex(Integer probeIndex) {
        if (filteredDataEventsRequest.getPageInfo().isAsc()) {
            return probeIndex - 1;
        } else {
            return probeIndex + 1;
        }
    }

    public MethodCallExpression extractMethodCall(Integer index) {
        final DataEventWithSessionId callEvent = getDataEvents().get(index);
        DataInfo probeInfo = getProbeInfo(callEvent.getDataId());
        String methodName = probeInfo.getAttribute("Name", null);
        String methodDescription = probeInfo.getAttribute("Desc", null);
        List<String> methodDescList = ClassTypeUtils.splitMethodDesc(methodDescription);
        String returnType = methodDescList.get(methodDescList.size() - 1);


        String ownerClass = probeInfo.getAttribute("Owner", null);
        if (ownerClass == null) {
            return null;
        }

        List<String> subjectTypeHierarchy = buildHierarchyFromTypeName("L" + ownerClass + ";");


        logger.warn("[MethodCall] " + ownerClass + "." + methodName + " : " + methodDescription);

        List<Parameter> callArguments = new LinkedList<>();

        ScanRequest callSubjectScan = new ScanRequest(new ScanResult(index, 0, false), 0,
                DirectionType.BACKWARDS);
        AtomicInteger subjectMatchIndex = new AtomicInteger(0);
        List<Parameter> subjectParameterList = new LinkedList<>();
        EventMatchListener subjectMatchListener = new EventMatchListener() {
            @Override
            public void eventMatched(Integer index, int matchedStack) {

                DataEventWithSessionId matchedSubjectEvent = getDataEvents().get(index);
                DataInfo subjectEventProbe = getProbeInfo(matchedSubjectEvent.getDataId());

                if (matchedSubjectEvent.getValue() != callEvent.getValue()) {
                    return;
                }

                String parameterClassType = subjectEventProbe.getAttribute("Type", null);

                Parameter potentialSubjectParameter;

                potentialSubjectParameter =
                        ParameterFactory.createParameter(index, ReplayData.this, 0, parameterClassType);

                List<String> buildHierarchyFromTypeName =
                        buildHierarchyFromTypeName(
                                ClassTypeUtils.getDescriptorName(
                                        potentialSubjectParameter.getType()));

                if (!buildHierarchyFromTypeName.contains(subjectTypeHierarchy.get(0))) {
                    logger.warn("type hierarchy mismatch for subject: " + subjectTypeHierarchy +
                            " vs " + buildHierarchyFromTypeName  +  " for parameter: " + potentialSubjectParameter);
                }
                subjectParameterList.add(potentialSubjectParameter);
                callSubjectScan.abort();
            }
        };
        callSubjectScan.addListener(EventType.GET_INSTANCE_FIELD_RESULT, subjectMatchListener);
        callSubjectScan.addListener(EventType.GET_STATIC_FIELD, subjectMatchListener);
        callSubjectScan.addListener(EventType.LOCAL_LOAD, subjectMatchListener);
        callSubjectScan.addListener(EventType.CALL_RETURN, subjectMatchListener);

        callSubjectScan.matchUntil(EventType.METHOD_ENTRY);
        ScanForEvents(callSubjectScan);

        Parameter subjectParameter ;
        if (subjectParameterList.size() == 0) {
            logger.warn("failed to identify subject for the call: " + methodName + " at " + probeInfo);
            subjectParameter = new Parameter();
            subjectParameter.setType(subjectTypeHierarchy.get(0));
            subjectParameter.setProb(callEvent);
            subjectParameter.setProbeInfo(probeInfo);
            subjectParameter.setName(subjectParameter.getType().substring(
                    subjectParameter.getType().lastIndexOf('.')
            ));
        } else {
            subjectParameter = subjectParameterList.get(0);
        }


        ScanRequest callReturnScan = new ScanRequest(new ScanResult(index, 0, false), 0,
                DirectionType.FORWARDS);

        AtomicInteger lookingForParams = new AtomicInteger(1);
        callReturnScan.addListener(EventType.CALL_PARAM, new EventMatchListener() {
            @Override
            public void eventMatched(Integer index, int matchedStack) {
                if (lookingForParams.get() == 1) {
                    Parameter callArgumentParameter = ParameterFactory.createParameterByCallArgument(
                            index, ReplayData.this, callArguments.size(), null
                    );
                    callArguments.add(callArgumentParameter);
                }
            }
        });
        callReturnScan.addListener(EventType.CALL, (i, s) -> lookingForParams.set(0));
        callReturnScan.addListener(EventType.METHOD_ENTRY, (i, s) -> lookingForParams.set(0));


        callReturnScan.matchUntil(EventType.CALL_RETURN);
        callReturnScan.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        ScanResult callReturnScanResult = ScanForEvents(callReturnScan);
        if (callReturnScanResult.getIndex() == -1 ||
                callReturnScanResult.getIndex() == getDataEvents().size()) {
            logger.warn("call return not found for " + probeInfo);
            return null;
        }

        DataInfo exitProbeInfo = getProbeInfo(getDataEvents().get(callReturnScanResult.getIndex()).getDataId());
        Parameter callReturnParameter = null;
        Parameter exception = null;


        if (exitProbeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT) {

            DataEventWithSessionId extEvent = getDataEvents().get(callReturnScanResult.getIndex());
            ObjectInfo exitValueObjectInfo = getObjectInfo(extEvent.getValue());
            TypeInfo exceptionTypeInfo = getTypeInfo(exitValueObjectInfo.getTypeId());

            exception = new Parameter();
            exception.setType(ClassTypeUtils.getDottedClassName(exceptionTypeInfo.getTypeNameFromClass()));
            exception.setValue(extEvent.getValue());
            exception.setProb(extEvent);
            exception.setProbeInfo(exitProbeInfo);

        } else if (exitProbeInfo.getEventType() == EventType.CALL_RETURN) {
            callReturnParameter = ParameterFactory.createReturnValueParameter(
                    callReturnScanResult.getIndex(), this, returnType);

            if (callReturnParameter.getName() == null) {
                callReturnParameter.setName(
                        ClassTypeUtils.createVariableNameFromMethodName(methodName,
                                callReturnParameter.getType())
                );
            }

            if (callReturnParameter.getType() == null || callReturnParameter.getType().equals("V")) {
//                return null;
            }

        } else {
            logger.error("what kind of exit");
        }


        MethodCallExpression methodCallExpression = MethodCallExpressionFactory.MethodCallExpression(
                methodName, subjectParameter, VariableContainer.from(callArguments),
                callReturnParameter);
        return methodCallExpression;

    }

    public void ParseData() {


        int direction = 1;

        // forwards is -1 for is because we assume the replay data is in descending order of
        // event id
        if (filteredDataEventsRequest.getPageInfo().isAsc()) {
            direction = 1;
        } else {
            direction = -1;

        }


        int callReturnIndex = 0;
        int callStack = 0;


        while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
            DataEventWithSessionId event = dataEvents.get(callReturnIndex);
            DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
            EventType eventType = probeInfo.getEventType();
            ClassInfo classInfo = classInfoMap.get(String.valueOf(probeInfo.getClassId()));
            MethodInfo methodInfo = methodInfoMap.get(String.valueOf(probeInfo.getMethodId()));

            /// process event

            LoggerUtil.logEvent(
                    "SCAN", callStack, callReturnIndex, event, probeInfo, classInfo, methodInfo
            );

            // end process even

            if (eventType == EventType.METHOD_ENTRY) {
                callStack += direction;
            } else if (
                    eventType == EventType.METHOD_NORMAL_EXIT || eventType == EventType.METHOD_EXCEPTIONAL_EXIT) {
                callStack -= direction;
            }

            callReturnIndex += direction;

        }


    }
}
