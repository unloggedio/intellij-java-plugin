package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.Components.AgentResponseComponent;
import com.insidious.plugin.ui.Components.CompareControlComponent;

import java.util.List;

public interface MethodExecutionListener {

    public void ExecuteCandidate(TestCandidateMetadata metadata, CompareControlComponent controlComponent);

    void displayResponse(AgentResponseComponent responseComponent);
}
