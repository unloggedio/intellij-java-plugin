package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.insidious.plugin.util.TestCandidateUtils;
import org.apache.commons.collections.ListUtils;

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
//        if (method != null) {
//            lastSelectedMethod = method;
//        }
        System.out.println("Loading Component for state : " + state);
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
            SessionInstance sessionInstance = this.insidiousService.getSessionInstance();
            if (sessionInstance == null) {
                loadComponentForState(this.currentState);
                return;
            }

            List<TestCandidateMetadata> methodTestCandidates = this.insidiousService.getTestCandidateMetadata(method);

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
