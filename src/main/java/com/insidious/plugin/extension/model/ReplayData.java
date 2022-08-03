package com.insidious.plugin.extension.model;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;

import java.util.List;
import java.util.Map;

public class ReplayData {
    private VideobugClientInterface client;
    private final FilteredDataEventsRequest filteredDataEventsRequest;
    List<DataEventWithSessionId> dataEvents;
    Map<String, ClassInfo> classInfoMap;
    Map<String, DataInfo> probeInfoMap;
    Map<String, StringInfo> stringInfoMap;
    Map<String, ObjectInfo> objectInfo;
    Map<String, TypeInfo> typeInfo;
    Map<String, MethodInfo> methodInfoMap;

    public ReplayData(
            VideobugClientInterface client,
            FilteredDataEventsRequest filteredDataEventsRequest,
            List<DataEventWithSessionId> dataList,
            Map<String, ClassInfo> classInfo,
            Map<String, DataInfo> dataInfo,
            Map<String, StringInfo> stringInfo,
            Map<String, ObjectInfo> objectInfo,
            Map<String, TypeInfo> typeInfo,
            Map<String, MethodInfo> methodInfoMap
    ) {
        this.client = client;
        this.filteredDataEventsRequest = filteredDataEventsRequest;
        dataEvents = dataList;
        classInfoMap = classInfo;
        probeInfoMap = dataInfo;
        stringInfoMap = stringInfo;
        this.objectInfo = objectInfo;
        this.typeInfo = typeInfo;
        this.methodInfoMap = methodInfoMap;
    }

    public Map<String, MethodInfo> getMethodInfoMap() {
        return methodInfoMap;
    }

    public Map<String, ObjectInfo> getObjectInfo() {
        return objectInfo;
    }

    public Map<String, TypeInfo> getTypeInfo() {
        return typeInfo;
    }

    public List<DataEventWithSessionId> getDataEvents() {
        return dataEvents;
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

    public void setDataEvents(List<DataEventWithSessionId> objectEventsReverse) {
        this.dataEvents = objectEventsReverse;
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
    public ReplayData fetchEventsPost(DataEventWithSessionId event,int size) {
        PageInfo paginationOlder = new PageInfo(0, size, PageInfo.Order.ASC);
        paginationOlder.setBufferSize(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getNanoTime());
        filterRequest.setPageInfo(paginationOlder);
        return client.fetchObjectHistoryByObjectId(filterRequest);
    }
}
