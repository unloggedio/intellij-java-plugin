package com.insidious.plugin.ui.Components.AtomicRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.Components.AtomicAssertionConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class SaveFlowTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private String storedCandidateSource = """
            {
                  "testAssertions" : {
                    "subAssertions" : [ {
                      "subAssertions" : [ ],
                      "expression" : "SELF",
                      "expectedValue" : "{\\"user_id\\":1,\\"username\\":\\"Something else\\",\\"password\\":\\"admin\\",\\"email\\":\\"admin@jsp_wfm\\"}",
                      "id" : "45c4e7a8-e5fe-4852-b4a1-fa7a7131d31d",
                      "assertionType" : "EQUAL",
                      "key" : "/"
                    } ],
                    "expression" : "SELF",
                    "expectedValue" : null,
                    "id" : "557ae481-fb76-4bdb-8e02-ad7877b616e2",
                    "assertionType" : "ALLOF",
                    "key" : null
                  },
                  "candidateId" : "8a31cc44-aa61-42ad-899d-8d79e9259742",
                  "name" : "admin",
                  "description" : "admin",
                  "methodArguments" : [ "\\"admin\\"" ],
                  "returnValue" : "{\\"user_id\\":1,\\"username\\":\\"Something else\\",\\"password\\":\\"admin\\",\\"email\\":\\"admin@jsp_wfm\\"}",
                  "returnValueClassname" : "com.jsp.jspwfm.Models.Entities.User",
                  "metadata" : {
                    "recordedBy" : "testerfresher",
                    "hostMachineName" : "testerfresher",
                    "timestamp" : 1690873470917,
                    "candidateStatus" : "PASSING"
                  },
                  "entryProbeIndex" : -236194851,
                  "probSerializedValue" : "eyJ1c2VyX2lkIjoxLCJ1c2VybmFtZSI6IlNvbWV0aGluZyBlbHNlIiwicGFzc3dvcmQiOiJhZG1pbiIsImVtYWlsIjoiYWRtaW5AanNwX3dmbSJ9",
                  "exception" : false,
                  "method" : {
                    "name" : "testFetchUser",
                    "signature" : "(Ljava/lang/String;)Lcom/jsp/jspwfm/Models/Entities/User;",
                    "className" : "com.jsp.jspwfm.Controllers.UserController",
                    "methodHash" : -48255829
                  }
                }
            """;
    private String responseSource = "{\"methodReturnValue\":\"{\\\"user_id\\\":1,\\\"username\\\":\\\"Something else\\\",\\\"password\\\":\\\"admin\\\",\\\"email\\\":\\\"admin@jsp_wfm\\\"}\",\"responseType\":\"NORMAL\",\"responseClassName\":\"com.jsp.jspwfm.Models.Entities.User\",\"message\":null,\"targetMethodName\":\"testFetchUser\",\"targetClassName\":\"com.jsp.jspwfm.Controllers.UserController\",\"targetMethodSignature\":\"(Ljava/lang/String;)Lcom/jsp/jspwfm/Models/Entities/User;\",\"timestamp\":1690874956616}";
    private CandidateLifeListener candidateLifeListener;
    private StoredCandidate candidate;
    private AgentCommandResponse<String> agentCommandResponse;

    @BeforeEach
    public void setup() throws Exception {
        candidateLifeListener = Mockito.mock(CandidateLifeListener.class);
        Mockito.when(candidateLifeListener.getSaveLocation()).thenReturn("testLoc");

        candidate = objectMapper.readValue(storedCandidateSource, StoredCandidate.class);
        agentCommandResponse = objectMapper.readValue(responseSource, AgentCommandResponse.class);
    }

    @Test
    public void testComponentManagement() {
        SaveForm saveForm = new SaveForm(candidate, agentCommandResponse, candidateLifeListener);

        AssertionBlock ruleEditor = saveForm.getRuleEditor();
        JTree candidateExplorerTree = saveForm.getCandidateExplorerTree();

        //should have just 1 rule on top level block
        Assertions.assertEquals(1, ruleEditor.getAssertionRules().size());

        //group count to be 0 at this stage
        Assertions.assertEquals(0, ruleEditor.getAssertionGroups().size());

        //fetches a list of paths, this can be used to simulate click to a certain treeNode
        List<TreePath> paths = getPaths(candidateExplorerTree.getPathForRow(0));

        int index = new Random().nextInt(paths.size());
        candidateExplorerTree.setSelectionPath(paths.get(index));

        //add new rule based on last selection
        ruleEditor.addNewRule();

        //should contain 2 rules
        Assertions.assertEquals(2, ruleEditor.getAssertionRules().size());

        //add new rule from top
        index = new Random().nextInt(paths.size());
        candidateExplorerTree.setSelectionPath(paths.get(index));

        ruleEditor.addNewGroup();
        //should have 1 group under top level
        Assertions.assertEquals(1, ruleEditor.getAssertionGroups().size());

        AssertionBlock l2Block = ruleEditor.getAssertionGroups().get(0);

        //sub block should have 1 rule
        Assertions.assertEquals(1, l2Block.getAssertionRules().size());

        //add a group to the last sub block
        l2Block.addNewGroup();

        //trigger delete from the newest sub block from rule, ensure the group gets deleted when it has 0 rules
        AssertionBlock l3Block = l2Block.getAssertionGroups().get(0);
        l3Block.getAssertionRules().get(0).deleteRule();

        Assertions.assertEquals(0, l2Block.getAssertionGroups().size());

        //delete the new group
        ruleEditor.getAssertionGroups().get(0).removeAssertionGroup();

        Assertions.assertEquals(0, ruleEditor.getAssertionGroups().size());

        //delete the last added rule
        ruleEditor.getAssertionRules().get(ruleEditor.getAssertionRules().size() - 1).deleteRule();
        Assertions.assertEquals(1, ruleEditor.getAssertionRules().size());
    }

    @Test
    public void testColorCodes() {
        SaveForm saveForm = new SaveForm(candidate, agentCommandResponse, candidateLifeListener);
        AssertionRule rootRule = saveForm.getRuleEditor().getAssertionRules().get(0);

        //expected : The background color to be green for the default rule.
        Assertions.assertEquals(AtomicAssertionConstants.PASSING_COLOR,
                fetchBackgroundColorForRule(rootRule));
        JComboBox<String> rootRuleOperations = fetchComboBoxFromRule(rootRule);
        //select not equals case
        rootRuleOperations.setSelectedItem("is not");

        //expected : The background color to be green for the default rule post operator change.
        Assertions.assertEquals(AtomicAssertionConstants.FAILING_COLOR,
                fetchBackgroundColorForRule(rootRule));
    }

    private Color fetchBackgroundColorForRule(AssertionRule rule) {
        return rule.getMainPanel().getComponents()[0].getBackground();
    }

    private JComboBox<String> fetchComboBoxFromRule(AssertionRule rule) {
        JPanel rootRuleMainPanel = rule.getMainPanel();
        JPanel topAligner = (JPanel) rootRuleMainPanel.getComponents()[0];
        Component[] ruleCompoenents = topAligner.getComponents();
        JComboBox<String> comboBox = null;
        for (int i = 0; i < ruleCompoenents.length; i++) {
            if (ruleCompoenents[i] instanceof JComboBox<?>) {
                comboBox = (JComboBox<String>) ruleCompoenents[i];
                break;
            }
        }
        return comboBox;
    }

    public List<TreePath> getPaths(TreePath parent) {
        List<TreePath> treePaths = new ArrayList<>();
        treePaths.add(parent);

        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                treePaths.addAll(getPaths(path));
            }
        }
        return treePaths;
    }
}
