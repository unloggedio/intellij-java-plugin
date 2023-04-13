package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.Components.AgentExceptionResponseComponent;
import com.insidious.plugin.ui.Components.AgentResponseComponent;
import com.insidious.plugin.ui.Components.CompareControlComponent;

public interface MethodExecutionListener {

    void executeCandidate(TestCandidateMetadata metadata, CompareControlComponent controlComponent);

    void displayResponse(AgentResponseComponent responseComponent);

    void displayExceptionResponse(AgentExceptionResponseComponent comp);
}
