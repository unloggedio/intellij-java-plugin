package com.insidious.plugin.pojo.atomic;

import com.insidious.plugin.agent.AgentCommandResponse;

public interface ExpectationInterface {
    boolean isExpected(AgentCommandResponse<?> response);
}
