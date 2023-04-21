package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;

import javax.swing.*;
import java.awt.*;

public class AtomicTestContainer {
    private JPanel mainPanel;
    private JPanel borderParent;
    private InsidiousService insidiousService;
    private MethodExecutorComponent methodExecutorComponent;

    private GutterState currentState;

    public AtomicTestContainer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
    }

    public JComponent getComponent() {
        return this.mainPanel;
    }

    public void loadComponentForState(GutterState state) {
        System.out.println("Loading Component for state : " + state);
        this.currentState = state;
        if (state.equals(GutterState.PROCESS_NOT_RUNNING)) {
            this.borderParent.removeAll();
            AgentConfigComponent component = new AgentConfigComponent(this.insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        } else if (state.equals(GutterState.EXECUTE) || state.equals(GutterState.DATA_AVAILABLE)) {
            loadExecutionFlow();
        } else {
            this.borderParent.removeAll();
            GenericNavigationComponent component = new GenericNavigationComponent(state, insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        }
    }

    public void loadExecutionFlow() {
        this.borderParent.removeAll();
        methodExecutorComponent = new MethodExecutorComponent(insidiousService);
        this.borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        this.borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method) {
        if (GutterState.EXECUTE.equals(this.currentState) ||
                GutterState.DATA_AVAILABLE.equals(this.currentState)) {
            if (methodExecutorComponent != null) {
                methodExecutorComponent.refreshAndReloadCandidates(method);
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
