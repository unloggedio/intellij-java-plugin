package com.insidious.plugin.network.pojo;

public interface SessionUpdatedCallback {
    void error(String message);
    void success(String sessionId);
}
