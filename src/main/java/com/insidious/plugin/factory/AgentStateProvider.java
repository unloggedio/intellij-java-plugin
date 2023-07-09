package com.insidious.plugin.factory;

public interface AgentStateProvider
//        extends Runnable
{
    String getJavaAgentString();

    String fetchVersionFromLibName(String name, String lib);

    boolean doesAgentExist();

    boolean isAgentRunning();

    void triggerAgentDownload();
}
