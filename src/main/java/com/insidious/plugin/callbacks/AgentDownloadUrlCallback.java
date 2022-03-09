package com.insidious.plugin.callbacks;

public interface AgentDownloadUrlCallback {
    void error(String error);

    void success(String url);
}
