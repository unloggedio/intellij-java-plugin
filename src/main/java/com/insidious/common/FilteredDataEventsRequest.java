package com.insidious.common;


import java.util.List;

public class FilteredDataEventsRequest {

    private String sessionId;

    private Long threadId;
    private Long nanotime;

    private List<Long> valueId;

    private Integer pageSize = 10;
    private Integer pageNumber = 0;

    private List<DebugPoint> debugPoints;
    private String sortOrder;
    private Integer probeId;


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

    public void setProbeId(int probeId) {
        this.probeId = probeId;
    }

    public int getProbeId() {
        return probeId;
    }

}
