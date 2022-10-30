package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestCandidateTreeModel implements TreeModel {
    private final TestCandidateMetadata selectedCandidate;
    private final SessionInstance sessionInstance;
    private final RootNode rootNode;
    private final List<TestCandidateMetadata> candidates;
    private final TestCaseGenerationConfiguration testGenerationConfiguration;
    private List<TreeModelListener> treeModelListeners = new LinkedList<>();
    private Map<Long, List<MethodCallExpression>> candidateCallMap = new HashMap<>();
    private List<TestCandidateMetadata> candidateList = new LinkedList<>();

    public TestCandidateTreeModel(
            TestCandidateMetadata selectedCandidate,
            TestCaseGenerationConfiguration testGenerationConfiguration,
            SessionInstance sessionInstance
    ) {
        this.selectedCandidate = selectedCandidate;
        this.sessionInstance = sessionInstance;
        this.testGenerationConfiguration = testGenerationConfiguration;
        String testSubjectType = selectedCandidate.getTestSubject().getType();
        MethodCallExpression mainMethod = (MethodCallExpression) selectedCandidate.getMainMethod();
        this.rootNode = createRootNode(testSubjectType, mainMethod);

        candidates = sessionInstance.getTestCandidatesUntil(selectedCandidate.getTestSubject().getValue(),
                selectedCandidate.getCallTimeNanoSecond(), mainMethod.getId(), false);

        for (TestCandidateMetadata candidate : candidates) {
            TestCandidateMetadata candidateWithCalls = getTestCandidateById(candidate.getEntryProbeIndex());
            candidateList.add(candidateWithCalls);
            // add all candidates to default selected, or just first and last ?
            testGenerationConfiguration.getTestCandidateMetadataList().add(candidateWithCalls);
            // add all calls of all test candidate to be mocked
            testGenerationConfiguration.getCallExpressionList().addAll(candidateWithCalls.getCallsList());
        }

    }

    @NotNull
    private static RootNode createRootNode(String testSubjectType, MethodCallExpression mainMethod) {
        return new RootNode(testSubjectType.substring(testSubjectType.lastIndexOf('.') + 1) + "." +
                mainMethod.getMethodName());
    }

    private TestCandidateMetadata getTestCandidateById(Long value) {
        return sessionInstance.getTestCandidateById(value);
    }

    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == rootNode) {
            return candidates.get(index);
        }


        if (parent instanceof TestCandidateMetadata) {
            TestCandidateMetadata candidateNode = (TestCandidateMetadata) parent;
            return candidateNode.getCallsList().get(index);
        }

        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof RootNode) {
            return candidates.size();
        }
        if (parent instanceof TestCandidateMetadata) {
            TestCandidateMetadata testCandidateMetadata = (TestCandidateMetadata) parent;
            return testCandidateMetadata.getCallsList().size();
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node instanceof MethodCallExpression) {
            return true;
        }
        return false;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof RootNode) {
            return candidates.indexOf(child);
        }
        if (parent instanceof TestCandidateMetadata) {
            return ((TestCandidateMetadata) parent).getCallsList().indexOf(child);
        }
        return 0;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        this.treeModelListeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        this.treeModelListeners.remove(l);
    }

    static class RootNode {
        String label;

        public RootNode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
