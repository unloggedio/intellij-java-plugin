package com.insidious.plugin.ui;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtomicTestContainer {
    private static final Logger logger = LoggerUtil.getInstance(AtomicTestContainer.class);
    private final InsidiousService insidiousService;
    private final MethodExecutorComponent methodExecutorComponent;
    private JPanel mainPanel;
    private JPanel borderParent;
    private GutterState currentState;
    private MethodAdapter lastSelection;
    private Map<String, Boolean> hasShownNewCandidateNotification = new HashMap<>();

    public AtomicTestContainer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        methodExecutorComponent = new MethodExecutorComponent(insidiousService);
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public synchronized void loadComponentForState(GutterState state) {

        switch (state) {
            case PROCESS_NOT_RUNNING: {
                if (currentState != null && currentState.equals(state)) {
                    return;
                }
                loadSDKOnboarding();
                break;
            }
            case EXECUTE:
            case DATA_AVAILABLE:
                loadExecutionFlow();
                break;
            default:
                loadExecutionFlow();
                methodExecutorComponent.refreshAndReloadCandidates(lastSelection, List.of());
        }
        currentState = state;
    }

    public void loadSDKOnboarding() {
        insidiousService.setAtomicWindowHeading("Get Started");
        borderParent.removeAll();
        UnloggedSDKOnboarding component = new UnloggedSDKOnboarding(insidiousService);
        JPanel component1 = component.getComponent();
        borderParent.add(component1, BorderLayout.CENTER);
        component1.validate();
        component1.repaint();
        borderParent.setVisible(false);
        borderParent.setVisible(true);
        borderParent.validate();
        borderParent.repaint();
    }

    public void loadExecutionFlow() {
        if (borderParent.getComponents().length > 0 &&
                borderParent.getComponent(0).equals(methodExecutorComponent.getComponent())) {
            return;
        }
        insidiousService.setAtomicWindowHeading("Replay");
        borderParent.removeAll();
        borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method) {
        long start = new Date().getTime();

        final MethodAdapter focussedMethod;
        if (method == null) {
            focussedMethod = methodExecutorComponent.getCurrentMethod();
        } else {
            focussedMethod = method;
        }
        if (focussedMethod == null) {
            insidiousService.getDirectInvokeTab().updateCandidateCount(0);
            return;
        }
        lastSelection = focussedMethod;
        if (currentState == null) {
            currentState = insidiousService.getGutterStateBasedOnAgentState();
        }

        if (currentState.equals(GutterState.PROCESS_NOT_RUNNING) || insidiousService.getSessionInstance() == null) {
            insidiousService.getDirectInvokeTab().updateCandidateCount(0);
            loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
            return;
        }


        if (GutterState.EXECUTE.equals(currentState) || GutterState.DATA_AVAILABLE.equals(currentState)) {
            insidiousService.getDirectInvokeTab().updateCandidateCount(0);
            methodExecutorComponent.refreshAndReloadCandidates(focussedMethod, List.of());
//            insidiousService.focusAtomicTestsWindow();
            return;
        }


        CandidateSearchQuery candidateSearchQuery =
                insidiousService.createSearchQueryForMethod(focussedMethod);

        List<StoredCandidate> methodTestCandidates =
                ApplicationManager.getApplication().runReadAction((Computable<List<StoredCandidate>>) () ->
                        insidiousService.getStoredCandidatesFor(candidateSearchQuery));

//        logger.warn("Candidates for [ " + candidateSearchQuery + "] => " + methodTestCandidates.size());

        loadExecutionFlow();
        insidiousService.getDirectInvokeTab().updateCandidateCount(methodTestCandidates.size());
        if (methodTestCandidates.size() > 0) {

            String className = candidateSearchQuery.getClassName();
            if (className.contains(".")) {
                className = className.substring(className.lastIndexOf(".") + 1);
            }
            String key = className + "." + candidateSearchQuery.getMethodName();
            if (hasShownNewCandidateNotification.size() == 0) {
                hasShownNewCandidateNotification.put(key, true);
                InsidiousNotification.notifyMessage(
                        "New replay candidate available for " + key + "(). \n You can now generate a JUnit test case " +
                                "after replaying it.",
                        NotificationType.INFORMATION
                );
            }

            methodTestCandidates.sort(StoredCandidate::compareTo);
            methodExecutorComponent.refreshAndReloadCandidates(focussedMethod, methodTestCandidates);
//            insidiousService.focusAtomicTestsWindow();
        } else {
            //runs for process_running
            insidiousService.focusDirectInvokeTab();
            loadComponentForState(currentState);
        }
        long end = new Date().getTime();

        logger.warn("load and refresh candidates in ATC for took " + (end - start) + " ms for [" + focussedMethod.getName() + "]");


    }

    public void clearBoardOnMethodExecutor() {
        methodExecutorComponent.clearBoard();
    }

    public void triggerCompileAndExecute() {
        methodExecutorComponent.compileAndExecuteAll();
    }

    public MethodAdapter getCurrentMethod() {
        return methodExecutorComponent.getCurrentMethod();
    }
}
