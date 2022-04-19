package com.insidious.plugin.callbacks;

public interface AgentJarDownloadCompleteCallback {
    void error(String message);

    void success(String url, String path);
}
