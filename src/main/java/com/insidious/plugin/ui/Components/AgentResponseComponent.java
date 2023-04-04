package com.insidious.plugin.ui.Components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.JsonFramework;
import com.insidious.plugin.pojo.MockFramework;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AgentResponseComponent {
    private static final Logger logger = LoggerUtil.getInstance(AgentResponseComponent.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final boolean SHOW_TEST_CASE_CREATE_BUTTON = false;
    private final String oldResponse;
    private final String agentResponse;
    private final InsidiousService insidiousService;
    private final Map<String, String> parameters;
    private final TestCandidateMetadata testCandidateMetadata;
    private final AgentCommandResponse agentCommandResponse;
    private final boolean MOCK_MODE = false;
    //    TreeMap<String, String> differences = new TreeMap<>();
    //    String s1 = "{\"indicate\":[{\"name\":\"c\",\"age\":24},\"doing\",\"brain\"],\"thousand\":false,\"number\":\"machine\",\"wut\":\"ay\",\"get\":\"ay\",\"sut\":\"ay\",\"put\":\"ay\",\"fut\":\"ay\"}";
//    String s1 = "";
    String s1 = "1";
    //    String s2 = "{\"indicate\":[{\"name\":\"a\",\"age\":25},\"doing\",\"e\"],\"thousand\":false,\"number\":\"dababy\",\"e\":\"f\"}";
//    String s1 = "{\"indicate\":[{\"name\":\"a\",\"age\":25},\"doing\",\"e\"],\"thousand\":false,\"number\":\"daboi\"}";
    String s2 = "2";
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel centerPanel;
    private JPanel bottomControlPanel;
    private JButton viewFullButton;
    private JTable mainTable;
    private JLabel statusLabel;
    private JScrollPane scrollParent;
    private JPanel topP;
    private JButton closeButton;
    private JPanel tableParent;
    private JPanel inputsParent;
    private JTextArea inputArea;

    public AgentResponseComponent(
            TestCandidateMetadata testCandidateMetadata,
            AgentCommandResponse agentCommandResponse,
            InsidiousService insidiousService,
            Map<String, String> parameters
    ) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.agentCommandResponse = agentCommandResponse;
        this.oldResponse = new String(testCandidateMetadata.getMainMethod().getReturnDataEvent().getSerializedValue());
        this.agentResponse = String.valueOf(agentCommandResponse.getMethodReturnValue());
        this.insidiousService = insidiousService;
        this.parameters = parameters;

        if (parameters != null && parameters.size() > 0) {
            StringBuilder inputs = new StringBuilder();
            for (String keyParam : parameters.keySet()) {
                inputs.append("" + keyParam + " = " + parameters.get(keyParam) + "\n");
            }
            this.inputArea.setText(inputs.toString());
        }

        if (MOCK_MODE) {
            tryTestDiff();
        } else {
            computeDifferences();
        }
        viewFullButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (MOCK_MODE) {
                    GenerateCompareWindows(s1, s2);
                } else {
                    GenerateCompareWindows(oldResponse, agentResponse);
                }
            }
        });

        if (SHOW_TEST_CASE_CREATE_BUTTON) {
            JButton createTestCaseButton = new JButton("Create test case");
            bottomControlPanel.add(createTestCaseButton, new GridConstraints());
            createTestCaseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    logger.warn("Create test case: " + testCandidateMetadata);

                    TestCandidateMetadata loadedTestCandidate = insidiousService.getSessionInstance()
                            .getTestCandidateById(testCandidateMetadata.getEntryProbeIndex(), true);


                    String testMethodName =
                            "testMethod" + ClassTypeUtils.upperInstanceName(
                                    loadedTestCandidate.getMainMethod().getMethodName());
                    TestCaseGenerationConfiguration testCaseGenerationConfiguration = new TestCaseGenerationConfiguration(
                            TestFramework.JUnit5,
                            MockFramework.Mockito,
                            JsonFramework.JACKSON,
                            ResourceEmbedMode.IN_FILE
                    );


                    // mock all calls by default
                    testCaseGenerationConfiguration.getCallExpressionList()
                            .addAll(loadedTestCandidate.getCallsList());


                    testCaseGenerationConfiguration.setTestMethodName(testMethodName);


                    testCaseGenerationConfiguration.getTestCandidateMetadataList().clear();
                    testCaseGenerationConfiguration.getTestCandidateMetadataList().add(loadedTestCandidate);


                    try {
                        insidiousService.generateAndSaveTestCase(testCaseGenerationConfiguration);
                    } catch (Exception ex) {
                        InsidiousNotification.notifyMessage("Failed to generate test case: " + ex.getMessage(),
                                NotificationType.ERROR);
                    }
                }
            });
        }
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    //constructor to take string difference and parameter/mce from candidate
    public void computeDifferences() {
        calculateDifferences(this.oldResponse, this.agentResponse);
    }

    public void tryTestDiff() {

        calculateDifferences(s1, s2);
    }

    private void calculateDifferences(String s1, String s2) {
        ObjectMapper om = new ObjectMapper();
        try {
            Map<String, Object> m1;
            if (s1 == null || s1.isEmpty()) {
                m1 = new TreeMap<>();
            } else {
                m1 = (Map<String, Object>) (om.readValue(s1, Map.class));
            }
            Map<String, Object> m2 = (Map<String, Object>) (om.readValue(s2, Map.class));

            System.out.println("Differences : ");
            MapDifference<String, Object> res = Maps.difference(flatten(m1), flatten(m2));
            System.out.println(res);

            System.out.println("Left Entries");
            res.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, Object> leftOnly = res.entriesOnlyOnLeft();

            System.out.println("Right Entries");
            res.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, Object> rightOnly = res.entriesOnlyOnRight();

            System.out.println("Differing entries");
            res.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, MapDifference.ValueDifference<Object>> differences = res.entriesDiffering();
            logger.info("[COMP DIFF] " + differences);
            logger.info("[COMP DIFF LEN] " + differences.size());
            List<DifferenceInstance> differenceInstances = getDifferenceModel(leftOnly,
                    rightOnly, differences);
            if (differenceInstances.size() == 0) {
                //no differences
                this.statusLabel.setText("Both Responses are Equal.");
            } else if (s1 == null || s1.isEmpty()) {
                this.statusLabel.setText("No previous Candidate found, current response.");
                renderTableForResponse(rightOnly);
            } else {
                //merge left and right differneces
                //or iterate and create a new pojo that works with 1 table model
                this.statusLabel.setText("Differences Found.");
                renderTableWithDifferences(differenceInstances);
            }
        } catch (Exception e) {
            System.out.println("TestDiff Exception: " + e);
            e.printStackTrace();
            if (s1.equals(s2)) {
                this.statusLabel.setText("Both Responses are Equal.");
                return;
            }
            //happens for malformed jsons or primitives.
            DifferenceInstance instance = new DifferenceInstance("Return Value", s1, s2,
                    DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
            differenceInstances.add(instance);
            renderTableWithDifferences(differenceInstances);
        }
    }

    private void renderTableWithDifferences(List<DifferenceInstance> differenceInstances) {
        CompareTableModel newModel = new CompareTableModel(differenceInstances);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
    }

    private void renderTableForResponse(Map<String, Object> rightOnly) {
        ResponseMapTable newModel = new ResponseMapTable(rightOnly);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
    }

    public Map<String, Object> flatten(Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(this::flatten)
                .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {
        if (entry == null) {
            return Stream.empty();
        }

        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?, ?>) entry.getValue()).entrySet().stream()
                    .flatMap(e -> flatten(
                            new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
        }

        if (entry.getValue() instanceof List<?>) {
            List<?> list = (List<?>) entry.getValue();
            return IntStream.range(0, list.size())
                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
                    .flatMap(this::flatten);
        }

        return Stream.of(entry);
    }

    public void GenerateCompareWindows(String before, String after) {
        DocumentContent content1 = DiffContentFactory.getInstance().create(getPrettyJsonString(before));
        DocumentContent content2 = DiffContentFactory.getInstance().create(getPrettyJsonString(after));
        SimpleDiffRequest request = new SimpleDiffRequest("Comparing Before and After", content1, content2, "Before",
                "After");
        DiffManager.getInstance().showDiff(this.insidiousService.getProject(), request);
    }

    private String getPrettyJsonString(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        JsonElement je = gson.fromJson(input, JsonElement.class);
        return gson.toJson(je);
    }

    private List<DifferenceInstance> getDifferenceModel(Map<String, Object> left, Map<String, Object> right,
                                                        Map<String, MapDifference.ValueDifference<Object>> differences) {
        ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
        for (String key : differences.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, differences.get(key).leftValue(),
                    differences.get(key).rightValue(), DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            differenceInstances.add(instance);
        }
        for (String key : left.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, left.get(key),
                    "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
            differenceInstances.add(instance);
        }
        for (String key : right.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, "",
                    right.get(key), DifferenceInstance.DIFFERENCE_TYPE.RIGHT_ONLY);
            differenceInstances.add(instance);
        }
        return differenceInstances;
    }
}
