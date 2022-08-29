package com.insidious.plugin.extension.model;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.ClassTypeUtils;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;

import java.util.*;

public class ReplayData {
    private static final Logger logger = LoggerUtil.getInstance(ReplayData.class);
    private final FilteredDataEventsRequest filteredDataEventsRequest;
    List<DataEventWithSessionId> dataEvents;
    Map<String, ClassInfo> classInfoMap;
    Map<String, DataInfo> probeInfoMap;
    Map<String, StringInfo> stringInfoMap;
    Map<String, ObjectInfo> objectInfoMap;
    Map<String, TypeInfo> typeInfoMap;
    Map<String, MethodInfo> methodInfoMap;
    private VideobugClientInterface client;

    public ReplayData(
            VideobugClientInterface client,
            FilteredDataEventsRequest filteredDataEventsRequest,
            List<DataEventWithSessionId> dataList,
            Map<String, ClassInfo> classInfo,
            Map<String, DataInfo> dataInfo,
            Map<String, StringInfo> stringInfo,
            Map<String, ObjectInfo> objectInfoMap,
            Map<String, TypeInfo> typeInfoMap,
            Map<String, MethodInfo> methodInfoMap
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

    public Map<String, MethodInfo> getMethodInfoMap() {
        return methodInfoMap;
    }

    public Map<String, ObjectInfo> getObjectInfoMap() {
        return objectInfoMap;
    }

    public Map<String, TypeInfo> getTypeInfoMap() {
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

    public Map<String, ClassInfo> getClassInfoMap() {
        return classInfoMap;
    }

    public Map<String, DataInfo> getProbeInfoMap() {
        return probeInfoMap;
    }

    public Map<String, StringInfo> getStringInfoMap() {
        return stringInfoMap;
    }

    public FilteredDataEventsRequest getFilteredDataEventsRequest() {
        return filteredDataEventsRequest;
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
        PageInfo paginationOlder = new PageInfo(0, size, PageInfo.Order.ASC);
        paginationOlder.setBufferSize(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getNanoTime());
        filterRequest.setPageInfo(paginationOlder);
        return client.fetchObjectHistoryByObjectId(filterRequest);
    }

    public TypeInfo getTypeInfoByName(String type) {
        return client.getTypeInfoByName(client.getCurrentSession().getSessionId(), type);
    }


    // A function to scan events starting from some particular index and in a particular
    // direction. the scanner should callback any event listners on matching events. the scan
    // stops when matchUntil events are hit, the index where the first matchUntil event was
    // matched will be returned
    public ScanResult eventScan(ScanRequest scanRequest) {

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
        DataInfo probeInfo = probeInfoMap.get(String.valueOf(firstEvent.getDataId()));
        ClassInfo firstClass = classInfoMap.get(String.valueOf(probeInfo.getClassId()));
        TypeInfo typeInfo = getTypeInfoByName(firstClass.getClassName());
        List<String> typeLadder = buildHierarchyFromType(typeInfo);


        Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
        while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
            DataEventWithSessionId event = dataEvents.get(callReturnIndex);
            probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
            EventType eventType = probeInfo.getEventType();
            ClassInfo classInfo = classInfoMap.get(String.valueOf(probeInfo.getClassId()));
            boolean stackMatch = callStack == 0;

            if (searchRequestCallStack == ScanRequest.CURRENT_CLASS) {
                stackMatch = firstClass.getClassId() == classInfo.getClassId();
                if (!stackMatch) {
                    if (typeLadder.contains(ClassTypeUtils.getDottedClassName(classInfo.getClassName()))) {
                        stackMatch = true;
                    }
                }

            }

            scanRequest.onEvent(stackMatch, eventType, callReturnIndex);
            if (callStack == 0 && matchUntilEvent.contains(eventType)) {
                return new ScanResult(callReturnIndex, callStack);
            }

            if (eventType == EventType.METHOD_ENTRY) {
                callStack += direction;
            } else if (
                    eventType == EventType.METHOD_NORMAL_EXIT ||
                            eventType == EventType.METHOD_EXCEPTIONAL_EXIT) {
                callStack -= direction;
                if (eventType == EventType.METHOD_EXCEPTIONAL_EXIT &&
                        matchUntilEvent.contains(EventType.METHOD_EXCEPTIONAL_EXIT) && callStack == 0) {
                    return new ScanResult(callReturnIndex, callStack);
                }
            }

            callReturnIndex += direction;

        }
        // when not found
        return new ScanResult(callReturnIndex, callStack);

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
        if (!expectedParameterType.startsWith("L")) {
            return List.of(expectedParameterType);
        }


        List<String> typeHierarchy = new LinkedList<>();

        TypeInfo typeInfo = null;

        final String expectedParameterTypeDotted =
                ClassTypeUtils.getDottedClassName(expectedParameterType);

        typeInfo = getTypeInfoByName(expectedParameterTypeDotted);
        typeHierarchy = buildHierarchyFromType(typeInfo);

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
                TypeInfo interfaceType = typeInfoMap.get(String.valueOf(anInterface));
                String interfaceName = interfaceType.getTypeNameFromClass();
                if (!typeHierarchy.contains(interfaceName)) {
                    typeHierarchy.add(interfaceName);
                }
            }

            typeInfoToAdd = typeInfoMap.get(String.valueOf(typeInfoToAdd.getSuperClass()));
        }
        return typeHierarchy;
    }

    public DataInfo getProbeInfo(int id) {
        return probeInfoMap.get(String.valueOf(id));
    }

    public TypeInfo getTypeInfo(long id) {
        return typeInfoMap.get(String.valueOf(id));
    }

    public StringInfo getStringInfo(int id) {
        return stringInfoMap.get(String.valueOf(id));
    }

    public ObjectInfo getObjectInfo(long id) {
        return objectInfoMap.get(String.valueOf(id));
    }

    public ClassInfo getClassInfo(int id) {
        return classInfoMap.get(String.valueOf(id));
    }

    public MethodInfo getMethodInfo(int methodId) {
        return methodInfoMap.get(String.valueOf(methodId));
    }

    public void mergeReplayData(ReplayData replayEventsAfter) {
        List<DataEventWithSessionId> afterEvents = replayEventsAfter.getDataEvents();


        if (this.filteredDataEventsRequest.getPageInfo().isDesc()) {


            if (replayEventsAfter.filteredDataEventsRequest.getPageInfo().isAsc()) {
                assert afterEvents.get(0).getNanoTime() == this.dataEvents.get(0).getNanoTime();
                afterEvents.remove(0);
                Collections.reverse(afterEvents);
                this.dataEvents.addAll(0, afterEvents);
            } else if (replayEventsAfter.filteredDataEventsRequest.getPageInfo().isDesc()) {
                assert afterEvents.get(0).getNanoTime() == this.dataEvents.get(0).getNanoTime();
                afterEvents.remove(0);
                Collections.reverse(afterEvents);
                this.dataEvents.addAll(0, afterEvents);

            }

        } else if (this.filteredDataEventsRequest.getPageInfo().isAsc()) {

            if (replayEventsAfter.filteredDataEventsRequest.getPageInfo().isAsc()) {
                assert afterEvents.get(0).getNanoTime() == this.dataEvents.get(0).getNanoTime();
                afterEvents.remove(0);
                Collections.reverse(afterEvents);
                this.dataEvents.addAll(0, afterEvents);
            } else if (replayEventsAfter.filteredDataEventsRequest.getPageInfo().isDesc()) {
                assert afterEvents.get(0).getNanoTime() == this.dataEvents.get(0).getNanoTime();
                afterEvents.remove(0);
                Collections.reverse(afterEvents);
                this.dataEvents.addAll(0, afterEvents);

            }

        }


        this.classInfoMap.putAll(replayEventsAfter.getClassInfoMap());
        this.probeInfoMap.putAll(replayEventsAfter.getProbeInfoMap());
        this.methodInfoMap.putAll(replayEventsAfter.getMethodInfoMap());
        this.stringInfoMap.putAll(replayEventsAfter.getStringInfoMap());
        this.objectInfoMap.putAll(replayEventsAfter.getObjectInfoMap());
        this.typeInfoMap.putAll(replayEventsAfter.getTypeInfoMap());
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
}
