package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.AgentExceptionResponseComponent;
import com.insidious.plugin.ui.methodscope.AgentResponseComponent;
import com.insidious.plugin.ui.methodscope.CompareControlComponent;

public interface MethodExecutionListener {

    void executeCandidate(TestCandidateMetadata metadata, CompareControlComponent controlComponent);

    void displayResponse(AgentResponseComponent responseComponent);

    void displayExceptionResponse(AgentExceptionResponseComponent comp);
}
