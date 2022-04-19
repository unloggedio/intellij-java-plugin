package com.insidious.plugin.client.pojo;

public interface SessionUpdatedCallback {
    void error(String message);

    void success(String sessionId);
}
