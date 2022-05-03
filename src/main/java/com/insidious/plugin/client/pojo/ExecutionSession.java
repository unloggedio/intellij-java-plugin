package com.insidious.plugin.client.pojo;


import com.insidious.common.weaver.TypeInfo;

import java.util.Date;
import java.util.List;

public class ExecutionSession {


    private long lastUpdateAt;
    private String projectId;
    private Date createdAt;
    private String name;
    private String sessionId;
    private String hostname;

    private List<TypeInfo> typeInfoList;

    public List<TypeInfo> getTypeInfoList() {
        return typeInfoList;
    }

    public void setTypeInfoList(List<TypeInfo> typeInfoList) {
        this.typeInfoList = typeInfoList;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public long getLastUpdateAt() {
        return lastUpdateAt;
    }

    public void setLastUpdateAt(long lastUpdateAt) {
        this.lastUpdateAt = lastUpdateAt;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
//
//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
}
