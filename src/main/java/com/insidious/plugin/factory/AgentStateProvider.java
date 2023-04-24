package com.insidious.plugin.factory;

import com.insidious.plugin.agent.ConnectionStateListener;

public interface AgentStateProvider extends ConnectionStateListener {
    String getJavaAgentString();

    String getVideoBugAgentPath();

    String suggestAgentVersion();

    String fetchVersionFromLibName(String name, String lib);

    boolean doesAgentExist();

    boolean isAgentRunning();
}
