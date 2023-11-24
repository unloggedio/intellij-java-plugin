package com.insidious.plugin.agent;

import com.intellij.util.messages.Topic;

public interface AgentConnectionStateNotifier {
    Topic<AgentConnectionStateNotifier> TOPIC_AGENT_CONNECTION_STATE =
            Topic.create("AGENT_CONNECTION_STATE", AgentConnectionStateNotifier.class);


    void onConnectedToAgentServer(ServerMetadata serverMetadata);

    void onDisconnectedFromAgentServer();
}
