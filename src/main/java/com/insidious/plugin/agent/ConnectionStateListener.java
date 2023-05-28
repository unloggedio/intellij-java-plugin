package com.insidious.plugin.agent;

public interface ConnectionStateListener {
    void onConnectedToAgentServer(ServerMetadata serverMetadata);

    void onDisconnectedFromAgentServer();
}
