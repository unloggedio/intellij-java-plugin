package com.insidious.plugin.callbacks;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public interface TestCandidateLifeListener {
    void executeCandidate(
            List<TestCandidateBareBone> metadata,
            ClassUnderTest classUnderTest,
            ExecutionRequestSourceType source,
            AgentCommandResponseListener<TestCandidateBareBone, String> responseListener
    );

    void displayResponse(Component responseComponent, boolean isExceptionFlow);

    void onSaved(TestCandidateBareBone storedCandidate);

    void onSelected(TestCandidateBareBone storedCandidate);

    void unSelected(TestCandidateBareBone storedCandidate);

    void onDeleteRequest(TestCandidateBareBone storedCandidate);

    void onDeleted(TestCandidateBareBone storedCandidate);

    void onUpdated(TestCandidateBareBone storedCandidate);

    void onUpdateRequest(TestCandidateBareBone storedCandidate);

    void onGenerateJunitTestCaseRequest(List<TestCandidateBareBone> storedCandidate);

    void onCandidateSelected(TestCandidateBareBone testCandidateMetadata, MouseEvent e);

    void onCancel();

}
