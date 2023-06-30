package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AtomicTestContainer {
    private static final Logger logger = LoggerUtil.getInstance(AtomicTestContainer.class);
    private final InsidiousService insidiousService;
    private final MethodExecutorComponent methodExecutorComponent;
    private JPanel mainPanel;
    private JPanel borderParent;
    private GutterState currentState;
    private MethodAdapter lastSelection;

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
                methodExecutorComponent.setMethod(lastSelection);
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
        if (borderParent.getComponent(0).equals(methodExecutorComponent.getComponent())) {
            return;
        }
        insidiousService.setAtomicWindowHeading("Atomic Tests");
        borderParent.removeAll();
        borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method) {
        final MethodAdapter focussedMethod;
        if (method == null) {
            focussedMethod = methodExecutorComponent.getCurrentMethod();
        } else {
            focussedMethod = method;
        }
        if (focussedMethod == null) {
            return;
        }
        lastSelection = focussedMethod;

        if (GutterState.EXECUTE.equals(currentState) || GutterState.DATA_AVAILABLE.equals(currentState)) {
            methodExecutorComponent.refreshAndReloadCandidates(focussedMethod, List.of());
            return;
        }

        if (currentState.equals(GutterState.PROCESS_NOT_RUNNING) ||
                insidiousService.getSessionInstance() == null) {
            loadComponentForState(currentState);
            return;
        }

        List<StoredCandidate> methodTestCandidates =
                ApplicationManager.getApplication().runReadAction((Computable<List<StoredCandidate>>) () ->
                        insidiousService.getStoredCandidatesFor(focussedMethod));
        if (methodTestCandidates.size() > 0) {
            loadExecutionFlow();
            methodExecutorComponent.refreshAndReloadCandidates(focussedMethod, methodTestCandidates);
        } else {
            //runs for process_running
            loadComponentForState(currentState);
        }

    }

    public void triggerCompileAndExecute() {
        methodExecutorComponent.compileAndExecuteAll();
    }

    public MethodAdapter getCurrentMethod() {
        return methodExecutorComponent.getCurrentMethod();
    }
}
