package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.TestCandidateUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AtomicTestContainer {
    private static final Logger logger = LoggerUtil.getInstance(AtomicTestContainer.class);
    private final InsidiousService insidiousService;
    private final MethodExecutorComponent methodExecutorComponent;
    private JPanel mainPanel;
    private JPanel borderParent;
    private GutterState currentState;

    public AtomicTestContainer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        methodExecutorComponent = new MethodExecutorComponent(insidiousService);
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public synchronized void loadComponentForState(GutterState state) {
        logger.info("Loading Component for state : " + state);
        switch (state) {
            case PROCESS_NOT_RUNNING: {
                if (currentState != null && currentState.equals(state)) {
                    return;
                }
                borderParent.removeAll();
                AgentConfigComponent component = new AgentConfigComponent(insidiousService);
                borderParent.add(component.getComponent(), BorderLayout.CENTER);
                borderParent.revalidate();
                borderParent.repaint();
                mainPanel.revalidate();
                mainPanel.repaint();


                break;
            }
            case EXECUTE:
            case DATA_AVAILABLE:
                loadExecutionFlow();
                break;
            case PROCESS_RUNNING:
            default: {
                if (currentState != null && currentState.equals(state)) {
                    return;
                }
                borderParent.removeAll();
                GenericNavigationComponent component = new GenericNavigationComponent(state, insidiousService);
                JPanel component1 = component.getComponent();
                borderParent.add(component1, BorderLayout.CENTER);
                component1.validate();
                component1.repaint();
                borderParent.setVisible(false);
                borderParent.setVisible(true);
                borderParent.validate();
                borderParent.repaint();
                break;
            }
        }
        currentState = state;
    }

    public void loadExecutionFlow() {
        borderParent.removeAll();
        borderParent.add(methodExecutorComponent.getComponent(), BorderLayout.CENTER);
        borderParent.revalidate();
    }

    public void triggerMethodExecutorRefresh(MethodAdapter method) {
        if (GutterState.EXECUTE.equals(currentState) || GutterState.DATA_AVAILABLE.equals(currentState)) {
            methodExecutorComponent.refreshAndReloadCandidates(method, new ArrayList<>());
        } else {
            if (currentState.equals(GutterState.NO_AGENT) ||
                    currentState.equals(GutterState.PROCESS_NOT_RUNNING)) {
                loadComponentForState(currentState);
                return;
            }
            SessionInstance sessionInstance = insidiousService.getSessionInstance();
            if (sessionInstance == null) {
                loadComponentForState(currentState);
                return;
            }

            List<TestCandidateMetadata> methodTestCandidates =
                    ApplicationManager.getApplication().runReadAction((Computable<List<TestCandidateMetadata>>) () ->
                            insidiousService.getTestCandidateMetadata(method));

            if (methodTestCandidates.size() > 0) {
                loadExecutionFlow();
                methodExecutorComponent.refreshAndReloadCandidates(method, deDuplicateList(methodTestCandidates));
            } else {
                loadComponentForState(currentState);
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
        return currentState;
    }

    public void triggerCompileAndExecute() {
        methodExecutorComponent.compileAndExecuteAll();
    }
}
