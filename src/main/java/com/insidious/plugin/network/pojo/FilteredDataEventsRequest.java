package com.insidious.plugin.network.pojo;


import com.insidious.plugin.pojo.TracePoint;

import java.util.Collections;
import java.util.List;

public class FilteredDataEventsRequest {
    private String sessionId;

    private Long threadId;

    private List<Long> valueId;

    private Integer pageSize = 10;
    private Integer pageNumber = 0;

    private List<DebugPoint> debugPoints;
    private String sortOrder;
    private long nanotime;
    private long probeId;

    public static FilteredDataEventsRequest fromTracePoint(TracePoint tracePoint) {
        FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId(tracePoint.getExecutionSessionId());
        filteredDataEventsRequest.setProbeId(tracePoint.getDataId());
        filteredDataEventsRequest.setThreadId(tracePoint.getThreadId());
        filteredDataEventsRequest.setNanotime(tracePoint.getNanoTime());
        filteredDataEventsRequest.setValueId(Collections.singletonList(tracePoint.getValue()));
        filteredDataEventsRequest.setPageSize(200);
        filteredDataEventsRequest.setPageNumber(0);
        filteredDataEventsRequest.setDebugPoints(Collections.emptyList());
        filteredDataEventsRequest.setSortOrder("DESC");
        return filteredDataEventsRequest;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public List<Long> getValueId() {
        return valueId;
    }

    public void setValueId(List<Long> valueId) {
        this.valueId = valueId;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public List<DebugPoint> getDebugPoints() {
        return debugPoints;
    }

    public void setDebugPoints(List<DebugPoint> debugPoints) {
        this.debugPoints = debugPoints;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setNanotime(long nanotime) {
        this.nanotime = nanotime;
    }

    public long getNanotime() {
        return nanotime;
    }

    public void setProbeId(long probeId) {
        this.probeId = probeId;
    }

    public long getProbeId() {
        return probeId;
    }
}
