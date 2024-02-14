package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.library.DeclaredMockItemPanel;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

public class SaveFormListener implements CandidateLifeListener {
    private static final DateTimeFormatter simpleDateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final Logger logger = LoggerUtil.getInstance(SaveFormListener.class);
    private AtomicRecordService atomicRecordService;
    private InsidiousService insidiousService;

    public SaveFormListener(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        atomicRecordService = insidiousService.getProject().getService(AtomicRecordService.class);
    }

    @Override
    public void executeCandidate(List<StoredCandidate> metadata, ClassUnderTest classUnderTest, ReplayAllExecutionContext context, AgentCommandResponseListener<TestCandidateMetadata, String> stringAgentCommandResponseListener) {

    }

    @Override
    public void displayResponse(Component responseComponent, boolean isExceptionFlow) {

    }

    @Override
    public void onSaved(StoredCandidate storedCandidate) {

        MethodUnderTest methodUnderTest = storedCandidate.getMethod();

        TestCandidateMetadata loadedTestCandidate = insidiousService.getTestCandidateById(
                storedCandidate.getEntryProbeIndex(), false);

        StoredCandidate existingMatchingStoredCandidate = atomicRecordService
                .getStoredCandidateFor(loadedTestCandidate);
        if (existingMatchingStoredCandidate.getCandidateId() == null) {
            existingMatchingStoredCandidate.setCandidateId(UUID.randomUUID().toString());
            existingMatchingStoredCandidate.setName("saved on " + simpleDateFormat.format(new Date().toInstant()));
        }
        atomicRecordService.saveCandidate(methodUnderTest, existingMatchingStoredCandidate);

    }

    @Override
    public void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse) {

    }

    @Override
    public void onDeleteRequest(StoredCandidate storedCandidate) {

    }

    @Override
    public void onDeleted(StoredCandidate storedCandidate) {

    }

    @Override
    public void onUpdated(StoredCandidate storedCandidate) {

    }

    @Override
    public void onUpdateRequest(StoredCandidate storedCandidate) {

    }

    @Override
    public void onGenerateJunitTestCaseRequest(StoredCandidate storedCandidate) {

    }

    @Override
    public void onCandidateSelected(StoredCandidate testCandidateMetadata) {

    }

    @Override
    public boolean canGenerateUnitCase(StoredCandidate candidate) {
        return false;
    }

    @Override
    public void onCancel() {
//        insidiousService.hideCandidateSaveForm(saveFormReference);
    }

    @Override
    public Project getProject() {
        return insidiousService.getProject();
    }

    @Override
    public void onSaved(DeclaredMockItemPanel value) {

    }

}
