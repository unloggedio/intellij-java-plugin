package com.insidious.plugin.extension.model;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public ReplayData getNextPage() throws APICallException {
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

    public ReplayData fetchEventsPre(DataEventWithSessionId event, int size) {
        PageInfo paginationOlder = new PageInfo(0, size, PageInfo.Order.DESC);
        paginationOlder.setBufferSize(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getNanoTime());
        filterRequest.setPageInfo(paginationOlder);
        return client.fetchObjectHistoryByObjectId(filterRequest);
    }

    public ReplayData fetchEventsPost(DataEventWithSessionId event, int size) {
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
    public int eventScan(ScanRequest scanRequest) {

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


        int entryProbeIndex = scanRequest.getStartIndex();
        int callReturnIndex = entryProbeIndex + direction;

        int callStack = 0;

        Integer searchRequestCallStack = scanRequest.getCallStack();


        Set<EventType> matchUntilEvent = scanRequest.getMatchUntilEvent();
        while (callReturnIndex > -1 && callReturnIndex < dataEvents.size()) {
            DataEventWithSessionId event = dataEvents.get(callReturnIndex);
            DataInfo probeInfo = probeInfoMap.get(String.valueOf(event.getDataId()));
            EventType eventType = probeInfo.getEventType();

            scanRequest.onEvent(callStack, eventType, callReturnIndex);
            if (callStack == searchRequestCallStack && matchUntilEvent.contains(eventType)) {
                break;
            }

            if (eventType == EventType.METHOD_ENTRY) {
                callStack += 1;
            } else if (callStack > 0 && (eventType == EventType.METHOD_NORMAL_EXIT ||
                    eventType == EventType.METHOD_EXCEPTIONAL_EXIT)) {
                callStack -= 1;
                callReturnIndex += direction;
                continue;
            }

            callReturnIndex += direction;

        }

        // when not found
        return callReturnIndex;

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


}
