package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;

import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

public interface MethodExecutionListener {

    void executeCandidate(
            TestCandidateMetadata metadata,
            List<String> methodArgumentValues,
            AgentCommandResponseListener<String> stringAgentCommandResponseListener
    );

    void displayResponse(Supplier<Component> responseComponent);
}
