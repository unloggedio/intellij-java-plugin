package com.insidious.plugin.pojo.atomic;

import com.insidious.plugin.agent.AgentCommandResponse;

import java.util.List;

public class OneOfExpectation implements ExpectationInterface {

    List<Object> objects;

    public OneOfExpectation(List<Object> objects) {
        this.objects = objects;
    }

    @Override
    public boolean isExpected(AgentCommandResponse<?> response) {
        return objects.contains(response.getMethodReturnValue());
    }
}
