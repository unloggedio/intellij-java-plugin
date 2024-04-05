package com.insidious.plugin.extension.model;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import net.openhft.chronicle.map.ChronicleMap;

import java.util.*;

public class ReplayData {
    private static final Logger logger = LoggerUtil.getInstance(ReplayData.class);
    private final FilteredDataEventsRequest filteredDataEventsRequest;
    List<DataEventWithSessionId> dataEvents;
    Map<Integer, ClassInfo> classInfoMap;
    Map<Integer, DataInfo> probeInfoMap;
    Map<Long, StringInfo> stringInfoMap;
    Map<Long, ObjectInfo> objectInfoMap;
    Map<Integer, TypeInfo> typeInfoMap;
    Map<Integer, MethodInfo> methodInfoMap;
    private VideobugClientInterface client;

    public ReplayData(
            VideobugClientInterface client,
            FilteredDataEventsRequest filteredDataEventsRequest,
            List<DataEventWithSessionId> dataList,
            Map<Integer, ClassInfo> classInfo,
            Map<Integer, DataInfo> dataInfo,
            Map<Long, StringInfo> stringInfo,
            Map<Long, ObjectInfo> objectInfoMap,
            Map<Integer, TypeInfo> typeInfoMap,
            Map<Integer, MethodInfo> methodInfoMap
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
//        return client.fetchObjectHistoryByObjectId(
//                filteredDataEventsRequestClone
//        );
        return null;
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
        filterRequest.setNanotime(event.getEventId());
        filterRequest.setPageInfo(paginationOlder);
//        return client.fetchObjectHistoryByObjectId(filterRequest);
        return null;
    }

    public ReplayData fetchEventsPost(DataEventWithSessionId event, int size) throws SessionNotSelectedException {
        assert client.getCurrentSession() != null;
        PageInfo pageNext = new PageInfo(0, size, PageInfo.Order.ASC);
        pageNext.setBufferSize(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getEventId());
        filterRequest.setPageInfo(pageNext);
//        return client.fetchObjectHistoryByObjectId(filterRequest);
        return null;
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
        DataInfo probeInfo = probeInfoMap.get(firstEvent.getProbeId());
        ClassInfo firstClass = classInfoMap.get(Long.valueOf(probeInfo.getClassId()));
        TypeInfo typeInfo = getTypeInfoByName(firstClass.getClassName());
        List<String> typeLadder = buildHierarchyFromType(typeInfo);


        if (scanRequest.hasValueListeners()) {

            Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
            while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
                DataEventWithSessionId event = dataEvents.get(callReturnIndex);
                probeInfo = probeInfoMap.get(event.getProbeId());
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
                    probeInfo = probeInfoMap.get(event.getProbeId());
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
                    probeInfo = probeInfoMap.get(event.getProbeId());
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
        DataInfo probeInfo = probeInfoMap.get(firstEvent.getProbeId());
        ClassInfo firstClass = classInfoMap.get(Long.valueOf(probeInfo.getClassId()));
        TypeInfo typeInfo = getTypeInfoByName(firstClass.getClassName());
        List<String> typeLadder = buildHierarchyFromType(typeInfo);


        Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
        while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
            DataEventWithSessionId event = dataEvents.get(callReturnIndex);
            probeInfo = probeInfoMap.get(event.getProbeId());
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
//        return client.fetchObjectHistoryByObjectId(
//                filteredDataEventsRequestClone
//        );
        return null;

    }

    public ReplayData getPage(int newPageNumber) throws SessionNotSelectedException {
        FilteredDataEventsRequest filteredDataEventsRequestClone =
                FilteredDataEventsRequest.copyOf(filteredDataEventsRequest);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        PageInfo.Order pageOrder = pageInfo.isAsc() ? PageInfo.Order.ASC : PageInfo.Order.DESC;

        pageInfo = new PageInfo(newPageNumber, pageInfo.getSize(), pageOrder);

        filteredDataEventsRequestClone.setPageInfo(pageInfo);
//        return client.fetchObjectHistoryByObjectId(
//                filteredDataEventsRequestClone
//        );
        return null;
    }

    public int getNextProbeIndex(Integer probeIndex) {
        if (filteredDataEventsRequest.getPageInfo().isAsc()) {
            return probeIndex + 1;
        } else {
            return probeIndex - 1;
        }
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
            DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getProbeId()));
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
