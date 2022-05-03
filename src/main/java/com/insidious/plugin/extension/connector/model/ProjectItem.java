package com.insidious.plugin.extension.connector.model;

import com.insidious.plugin.client.pojo.ExecutionSession;

import java.util.List;

public class ProjectItem {
    String name;
    String id;
    String createdAt;
    List<ExecutionSession> sessionList;

    public List<ExecutionSession> getSessionList() {
        return sessionList;
    }

    public void setSessionList(List<ExecutionSession> sessionList) {
        this.sessionList = sessionList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
