package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DefaultMethodArgumentValueCache {
    Map<String, AgentCommandRequest> cacheMap = new HashMap<>();

    @NotNull
    private static String getKey(AgentCommandRequest agentCommandRequest) {
        return agentCommandRequest.getClassName() + "#" + agentCommandRequest.getMethodName()
                + "#" + agentCommandRequest.getMethodSignature();
    }

    public void addArgumentSet(AgentCommandRequest agentCommandRequest) {
        cacheMap.put(getKey(agentCommandRequest), agentCommandRequest);
    }

    public AgentCommandRequest getArgumentSets(AgentCommandRequest agentCommandRequest) {
        String key = getKey(agentCommandRequest);
        return cacheMap.get(key);
    }
}
