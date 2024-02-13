package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SaveFormListener implements CandidateLifeListener {
    private static final DateTimeFormatter simpleDateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final Logger logger = LoggerUtil.getInstance(SaveFormListener.class);
    private AtomicRecordService atomicRecordService;
    private InsidiousService insidiousService;
    private TestCandidateSaveForm saveFormReference;

    public SaveFormListener(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
    }

    @Override
    public void executeCandidate(List<StoredCandidate> metadata, ClassUnderTest classUnderTest, ReplayAllExecutionContext context, AgentCommandResponseListener<TestCandidateMetadata, String> stringAgentCommandResponseListener) {

    }

    @Override
    public void displayResponse(Component responseComponent, boolean isExceptionFlow) {

    }

    @Override
    public int onSaved(StoredCandidate storedCandidate) {

        int mockCount = 0;
        MethodUnderTest methodUnderTest = storedCandidate.getMethod();

        TestCandidateMetadata loadedTestCandidate = insidiousService.getTestCandidateById(
                storedCandidate.getEntryProbeIndex(), true);

        StoredCandidate existingMatchingStoredCandidate = atomicRecordService
                .getStoredCandidateFor(methodUnderTest, loadedTestCandidate);
        if (existingMatchingStoredCandidate.getCandidateId() == null) {
            existingMatchingStoredCandidate.setCandidateId(UUID.randomUUID().toString());
            existingMatchingStoredCandidate.setName("saved on " + simpleDateFormat.format(new Date().toInstant()));
        }
        atomicRecordService.saveCandidate(methodUnderTest, existingMatchingStoredCandidate);

        PsiMethod targetMethod = ClassTypeUtils.getPsiMethod(loadedTestCandidate.getMainMethod(),
                insidiousService.getProject());

        List<PsiMethodCallExpression> allCallExpressions = getAllCallExpressions(targetMethod);


        Map<String, List<PsiMethodCallExpression>> expressionsBySignatureMap = allCallExpressions.stream()
                .collect(Collectors.groupingBy(e1 -> {
                    PsiMethod method = e1.resolveMethod();
                    return MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(method))
                            .getMethodHashKey();
                }));


        Map<String, DeclaredMock> mocks = new HashMap<>();

        List<MethodCallExpression> callsList = loadedTestCandidate.getCallsList();
        List<MethodCallExpression> callListCopy = new ArrayList<>(callsList);
        while (callListCopy.size() > 0) {
            MethodCallExpression methodCallExpression = callListCopy.remove(0);

            PsiMethod psiMethod = ClassTypeUtils.getPsiMethod(methodCallExpression,
                    insidiousService.getProject());
            if (psiMethod == null) {
                logger.warn(
                        "Failed to resolve method: " + methodCallExpression + ", call will not be mocked");
                continue;
            }
            MethodUnderTest mockMethodTarget = MethodUnderTest.fromMethodAdapter(
                    new JavaMethodAdapter(psiMethod));

            List<PsiMethodCallExpression> expressionsBySignature = expressionsBySignatureMap.get(
                    mockMethodTarget.getMethodHashKey());

            if (expressionsBySignature == null) {
                // this call is not on a field. it is probably a call to a method in the same class
                // not mocking this
                logger.warn("Skipping call for mocking: " + mockMethodTarget);
                continue;
            }

            PsiMethodCallExpression methodCallExpression1 = expressionsBySignature.get(0);
//                    methodCallExpression1.getMethodExpression()

            PsiReferenceExpression methodExpression = methodCallExpression1.getMethodExpression();
            PsiExpression qualifierExpression1 = methodExpression.getQualifierExpression();
            if (qualifierExpression1 == null) {
                // call to another method in the same class :)
                // should never happen
                continue;
            }

            if (!(qualifierExpression1 instanceof PsiReferenceExpression)) {
                // what is this ? TODO: add support for chain mocking
                continue;
            }
            PsiReferenceExpression qualifierExpression = (PsiReferenceExpression) qualifierExpression1;
            PsiElement qualifierField = qualifierExpression.resolve();
            if (!(qualifierField instanceof PsiField)) {
                // call is not on a field
                continue;
            }
            DeclaredMock declaredMock = ClassUtils.createDefaultMock(methodCallExpression1);

            DeclaredMock existingMock = mocks.get(mockMethodTarget.getMethodHashKey());
            if (existingMock == null) {
                mocks.put(mockMethodTarget.getMethodHashKey(), declaredMock);
            } else {
                existingMock.getThenParameter().addAll(declaredMock.getThenParameter());
            }


        }

        Collection<DeclaredMock> values = mocks.values();

        for (DeclaredMock value : values) {
            atomicRecordService.saveMockDefinition(value);
            mockCount++;
        }

        return mockCount;
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

    private List<PsiMethodCallExpression> getAllCallExpressions(PsiMethod targetMethod) {
//        @Nullable PsiClass containingClass = targetMethod.getContainingClass();
        List<PsiMethodCallExpression> psiMethodCallExpressions = new ArrayList<>(PsiTreeUtil.findChildrenOfType(
                targetMethod, PsiMethodCallExpression.class));

        List<PsiMethodCallExpression> collectedCalls = new ArrayList<>();

        for (PsiMethodCallExpression psiMethodCallExpression : psiMethodCallExpressions) {
            PsiExpression qualifierExpression = psiMethodCallExpression.getMethodExpression()
                    .getQualifierExpression();
            if (qualifierExpression == null) {
                // this call needs to be scanned
                PsiMethod subTargetMethod = (PsiMethod) psiMethodCallExpression.getMethodExpression().resolve();
                if (subTargetMethod.hasModifier(JvmModifier.STATIC)) {
                    // static methods to be added as it is for now
                    // but lets scan them also for down stream calls from their fields
                    // for possible support of injection in future
                    collectedCalls.add(psiMethodCallExpression);
                }
                List<PsiMethodCallExpression> subCalls = getAllCallExpressions(subTargetMethod);
                collectedCalls.addAll(subCalls);
            } else {
                collectedCalls.add(psiMethodCallExpression);
            }
        }

        return collectedCalls;
    }

}
