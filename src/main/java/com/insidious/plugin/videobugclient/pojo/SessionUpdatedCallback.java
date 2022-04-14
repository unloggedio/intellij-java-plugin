package com.insidious.plugin.videobugclient.pojo;

public interface SessionUpdatedCallback {
    void error(String message);

    void success(String sessionId);
}
