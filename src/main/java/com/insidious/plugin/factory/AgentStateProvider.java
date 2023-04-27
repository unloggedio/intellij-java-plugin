package com.insidious.plugin.factory;

public interface AgentStateProvider {
    String getJavaAgentString();

    String getVideoBugAgentPath();

    String suggestAgentVersion();

    String fetchVersionFromLibName(String name, String lib);

    boolean doesAgentExist();

    boolean isAgentRunning();
}
