package com.insidious.plugin.factory;

public interface AgentStateProvider {
    String getJavaAgentString();

    String fetchVersionFromLibName(String name, String lib);

    boolean doesAgentExist();

    boolean isAgentRunning();

    void triggerAgentDownload();
}
