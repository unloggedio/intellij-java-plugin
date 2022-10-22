package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;

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
    private List<TreeModelListener> treeModelListeners = new LinkedList<>();
    private Map<Long, TestCandidateMetadata> candidatesWithCalls = new HashMap<>();

    public TestCandidateTreeModel(TestCandidateMetadata selectedCandidate, SessionInstance sessionInstance) {
        this.selectedCandidate = selectedCandidate;
        this.sessionInstance = sessionInstance;
        String testSubjectType = selectedCandidate.getTestSubject().getType();
        MethodCallExpression mainMethod = (MethodCallExpression) selectedCandidate.getMainMethod();
        this.rootNode = new RootNode(testSubjectType.substring(testSubjectType.lastIndexOf('.') + 1) + "." +
                mainMethod.getMethodName());

        candidates = sessionInstance.getTestCandidatesUntil(selectedCandidate.getTestSubject().getValue(),
                selectedCandidate.getCallTimeNanoSecond(), mainMethod.getId(), false);
    }

    private TestCandidateMetadata getTestCandidateById(Long value) {
        if (candidatesWithCalls.containsKey(value)) {
            return candidatesWithCalls.get(value);
        }
        TestCandidateMetadata testCandidateWithCalls = sessionInstance
                .getTestCandidateById(value);
        candidatesWithCalls.put(value, testCandidateWithCalls);
        return testCandidateWithCalls;
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
            return (getTestCandidateById(candidateNode.getEntryProbeIndex())).getCallsList().get(index);
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
            return getTestCandidateById(testCandidateMetadata.getEntryProbeIndex()).getCallsList().size();
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
    }
}
