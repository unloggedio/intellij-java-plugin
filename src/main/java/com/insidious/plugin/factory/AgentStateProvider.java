package com.insidious.plugin.factory;

public interface AgentStateProvider
//        extends Runnable
{

    String fetchVersionFromLibName(String name, String lib);

    boolean isAgentRunning();
}
