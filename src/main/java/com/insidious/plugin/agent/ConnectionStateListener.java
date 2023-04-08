package com.insidious.plugin.agent;

public interface ConnectionStateListener {
    void onConnectedToAgentServer();

    void onDisconnectedFromAgentServer();
}
