package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AtomicTestContainer {
    private JPanel mainPanel;
    private JPanel borderParent;
    private InsidiousService insidiousService;
    private MethodExecutorComponent methodExecutorComponent;
    private GutterState currentState;
    private MethodAdapter lastSelectedMethod;

    public AtomicTestContainer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        methodExecutorComponent = new MethodExecutorComponent(insidiousService);
    }

    public JComponent getComponent() {
        return this.mainPanel;
    }

    public void loadComponentForState(GutterState state, MethodAdapter method) {
        if (method != null) {
            lastSelectedMethod = method;
        }
        System.out.println("Loading Component for state : " + state);
        this.currentState = state;
        if (state.equals(GutterState.PROCESS_NOT_RUNNING)) {
            this.borderParent.removeAll();
            AgentConfigComponent component = new AgentConfigComponent(this.insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        } else if (state.equals(GutterState.EXECUTE) || state.equals(GutterState.DATA_AVAILABLE)) {
            loadExecutionFlow();
        } else if (state.equals(GutterState.PROCESS_RUNNING)) {
            insidiousService.focusDirectInvokeTab();
            this.borderParent.removeAll();
            GenericNavigationComponent component = new GenericNavigationComponent(state, insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        } else {
            this.borderParent.removeAll();
            GenericNavigationComponent component = new GenericNavigationComponent(state, insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        }
    }

    public void loadExecutionFlow() {
        this.borderParent.removeAll();
        this.borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        this.borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method) {
        if (GutterState.EXECUTE.equals(this.currentState) ||
                GutterState.DATA_AVAILABLE.equals(this.currentState)) {
            if (methodExecutorComponent != null) {
                methodExecutorComponent.refreshAndReloadCandidates(method);
            }
        } else {
            String classQualifiedName = method.getContainingClass().getQualifiedName();
            String methodName = method.getName();
            List<TestCandidateMetadata> methodTestCandidates = this.insidiousService
                    .getSessionInstance()
                    .getTestCandidatesForAllMethod(classQualifiedName, methodName, false);
            if (methodTestCandidates.size() > 0) {
                loadExecutionFlow();
                methodExecutorComponent.refreshAndReloadCandidates(method);
            } else {
                loadComponentForState(this.currentState, method);
            }
        }


    }

    public void refresh() {
        if (GutterState.EXECUTE.equals(this.currentState)
                || GutterState.DATA_AVAILABLE.equals(this.currentState)) {
            if (methodExecutorComponent != null) {
                methodExecutorComponent.refresh();
            }
        }
    }

    public GutterState getCurrentState() {
        return this.currentState;
    }

    public void triggerCompileAndExecute() {
        methodExecutorComponent.compileAndExecuteAll();
    }
}
