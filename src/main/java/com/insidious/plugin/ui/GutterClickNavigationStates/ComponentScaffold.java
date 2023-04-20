package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;

import javax.swing.*;
import java.awt.*;

public class ComponentScaffold {
    private JPanel mainPanel;
    private JPanel borderParent;
    private InsidiousService insidiousService;
    private MethodExecutorComponent methodExecutorComponent;

    private InsidiousService.GUTTER_STATE currentState;

    public ComponentScaffold(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
    }

    public JComponent getComponent() {
        return this.mainPanel;
    }

    public void loadComponentForState(InsidiousService.GUTTER_STATE state) {
        System.out.println("Loading Component for state : " + state);
        this.currentState = state;
        if (state.equals(InsidiousService.GUTTER_STATE.PROCESS_NOT_RUNNING)) {
            this.borderParent.removeAll();
            AgentConfigComponent component = new AgentConfigComponent(this.insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        } else if (state.equals(InsidiousService.GUTTER_STATE.EXECUTE)) {
            loadExecutionFlow();
        } else {
            this.borderParent.removeAll();
            GenericNavigationComponent component = new GenericNavigationComponent(state);
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
        if (InsidiousService.GUTTER_STATE.EXECUTE.equals(this.currentState)) {
            if (methodExecutorComponent != null) {
                methodExecutorComponent.refreshAndReloadCandidates(method);
            }
        }
    }

    public JComponent getContent() {
        return this.mainPanel;
    }

    public void refresh() {
        if (InsidiousService.GUTTER_STATE.EXECUTE.equals(this.currentState)) {
            if (methodExecutorComponent != null) {
                methodExecutorComponent.refresh();
            }
        }
    }
}
