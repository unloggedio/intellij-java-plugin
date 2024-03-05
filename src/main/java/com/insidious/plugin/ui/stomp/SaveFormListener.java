package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SaveFormListener implements CandidateLifeListener {
    private static final DateTimeFormatter simpleDateFormat = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final Logger logger = LoggerUtil.getInstance(SaveFormListener.class);
    private final AtomicRecordService atomicRecordService;
    private final InsidiousService insidiousService;

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

        StoredCandidate existingMatchingStoredCandidate = atomicRecordService.getStoredCandidateFor(
                loadedTestCandidate);
        if (existingMatchingStoredCandidate != null) {
            existingMatchingStoredCandidate.setName(
                    methodUnderTest.getName() + " saved on " + simpleDateFormat.format(new Date().toInstant()));
            existingMatchingStoredCandidate.setMockIds(storedCandidate.getMockIds());
            existingMatchingStoredCandidate.setTestAssertions(storedCandidate.getTestAssertions());
            existingMatchingStoredCandidate.setLineNumbers(storedCandidate.getLineNumbers());
            existingMatchingStoredCandidate.setMetadata(storedCandidate.getMetadata());
            existingMatchingStoredCandidate.setException(storedCandidate.isException());
            existingMatchingStoredCandidate.setMethodArguments(storedCandidate.getMethodArguments());
            existingMatchingStoredCandidate.setReturnValue(storedCandidate.getReturnValue());
            existingMatchingStoredCandidate.setProbSerializedValue(storedCandidate.getProbSerializedValue());
            atomicRecordService.saveCandidate(methodUnderTest, existingMatchingStoredCandidate);
        } else {
            storedCandidate.setCandidateId(UUID.randomUUID().toString());
            storedCandidate.setName("saved on " + simpleDateFormat.format(new Date().toInstant()));
            atomicRecordService.saveCandidate(methodUnderTest, storedCandidate);
        }

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
    public String onSaved(DeclaredMock declaredMock) {
        return insidiousService.saveMockDefinition(declaredMock);
    }

}
