package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.intellij.psi.PsiMethod;

import javax.swing.*;
import java.awt.*;

public class ComponentScaffold {
    private JPanel mainPanel;
    private JPanel borderParent;
    private InsidiousService insidiousService;
    private MethodExecutorComponent methodExecutorComponent;

    private InsidiousService.GUTTER_STATE currentState;

    public JComponent getComponent()
    {
        return this.mainPanel;
    }

    public ComponentScaffold(InsidiousService insidiousService)
    {
        this.insidiousService = insidiousService;
    }

    public void loadComponentForState(InsidiousService.GUTTER_STATE state)
    {
        System.out.println("Loading Component for state : "+state);
        this.currentState = state;
        if(state.equals(InsidiousService.GUTTER_STATE.PROCESS_NOT_RUNNING))
        {
            this.borderParent.removeAll();
            AgentConfigComponent component = new AgentConfigComponent(this.insidiousService);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        }
        else if(state.equals(InsidiousService.GUTTER_STATE.EXECUTE))
        {
            loadExecutionFlow();
        }
        else {
            this.borderParent.removeAll();
            GenericNavigationComponent component = new GenericNavigationComponent(state);
            this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
            this.borderParent.revalidate();
        }
    }

    public void loadExecutionFlow()
    {
        this.borderParent.removeAll();
//        this.borderParent.revalidate();
        //if(methodExecutorComponent==null)
        //{
            methodExecutorComponent = new MethodExecutorComponent(insidiousService);
        //}
        this.borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        this.borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method)
    {
        if(this.currentState!=null && this.currentState.equals(InsidiousService.GUTTER_STATE.EXECUTE))
        {
            if(methodExecutorComponent!=null)
            {
                methodExecutorComponent.refreshAndReloadCandidates(method);
            }
        }
    }

    public JComponent getContent() {
        return this.mainPanel;
    }

    public void refresh()
    {
        if(this.currentState.equals(InsidiousService.GUTTER_STATE.EXECUTE))
        {
            if(methodExecutorComponent!=null)
            {
                methodExecutorComponent.refresh();
            }
        }
    }
}
