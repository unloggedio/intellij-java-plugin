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
    private final VideobugClientInterface client;
    private final FilteredDataEventsRequest filteredDataEventsRequest;
    List<DataEventWithSessionId> dataEvents;
    Map<String, ClassInfo> classInfoMap;
    Map<String, DataInfo> dataInfoMap;
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
        dataInfoMap = dataInfo;
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


    public Map<String, ClassInfo> getClassInfoMap() {
        return classInfoMap;
    }

    public Map<String, DataInfo> getDataInfoMap() {
        return dataInfoMap;
    }

    public Map<String, StringInfo> getStringInfoMap() {
        return stringInfoMap;
    }

    public void setDataEvents(List<DataEventWithSessionId> objectEventsReverse) {
        this.dataEvents = objectEventsReverse;
    }
}
