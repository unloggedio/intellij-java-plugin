package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.insidious.plugin.util.TestCandidateUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AtomicTestContainer {
    private final InsidiousService insidiousService;
    private final MethodExecutorComponent methodExecutorComponent;
    private JPanel mainPanel;
    private JPanel borderParent;
    private GutterState currentState;
//    private MethodAdapter lastSelectedMethod;

    public AtomicTestContainer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        methodExecutorComponent = new MethodExecutorComponent(insidiousService);
    }

    public JComponent getComponent() {
        return this.mainPanel;
    }

    public void loadComponentForState(GutterState state) {
        System.out.println("Loading Component for state : " + state);
        switch (state) {
            case PROCESS_NOT_RUNNING: {
                if (this.currentState != null && this.currentState.equals(state)) {
                    return;
                }
                this.borderParent.removeAll();
                AgentConfigComponent component = new AgentConfigComponent(this.insidiousService);
                this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
                this.borderParent.revalidate();
                break;
            }
            case EXECUTE:
            case DATA_AVAILABLE:
                loadExecutionFlow();
                break;
            case PROCESS_RUNNING: {
                insidiousService.focusDirectInvokeTab();
                if (this.currentState != null && this.currentState.equals(state)) {
                    return;
                }
                this.borderParent.removeAll();
                GenericNavigationComponent component = new GenericNavigationComponent(state, insidiousService);
                this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
                this.borderParent.revalidate();
                break;
            }
            default: {
                if (this.currentState != null && this.currentState.equals(state)) {
                    return;
                }
                this.borderParent.removeAll();
                GenericNavigationComponent component = new GenericNavigationComponent(state, insidiousService);
                this.borderParent.add(component.getComponent(), BorderLayout.CENTER);
                this.borderParent.revalidate();
                break;
            }
        }
        this.currentState = state;
    }

    public void loadExecutionFlow() {
        this.borderParent.removeAll();
        this.borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        this.borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method) {
        if (GutterState.EXECUTE.equals(this.currentState) ||
                GutterState.DATA_AVAILABLE.equals(this.currentState)) {
            methodExecutorComponent.refreshAndReloadCandidates(method, new ArrayList<>());
        } else {
            if(this.currentState.equals(GutterState.NO_AGENT) ||
            this.currentState.equals(GutterState.PROCESS_NOT_RUNNING))
            {
                loadComponentForState(this.currentState);
            }
            SessionInstance sessionInstance = this.insidiousService.getSessionInstance();
            if (sessionInstance == null) {
                loadComponentForState(this.currentState);
                return;
            }

            List<TestCandidateMetadata> methodTestCandidates =
                    ApplicationManager.getApplication().runReadAction((Computable<List<TestCandidateMetadata>>) () ->
                            this.insidiousService.getTestCandidateMetadata(method));

            if (methodTestCandidates.size() > 0) {
                loadExecutionFlow();
                methodExecutorComponent.refreshAndReloadCandidates(method, deDuplicateList(methodTestCandidates));
            } else {
                loadComponentForState(this.currentState);
            }
        }


    }

    public List<TestCandidateMetadata> deDuplicateList(List<TestCandidateMetadata> list) {
        Map<Integer, TestCandidateMetadata> candidateHashMap = new TreeMap<>();
        for (TestCandidateMetadata metadata : list) {
            int candidateHash = TestCandidateUtils.getCandidateSimilarityHash(metadata);
            if (!candidateHashMap.containsKey(candidateHash)) {
                candidateHashMap.put(candidateHash, metadata);
            }
        }
        return new ArrayList<>(candidateHashMap.values());
    }


    public GutterState getCurrentState() {
        return this.currentState;
    }

    public void triggerCompileAndExecute() {
        methodExecutorComponent.compileAndExecuteAll();
    }
}
