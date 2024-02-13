package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.insidious.plugin.ui.library.StoredCandidateItemPanel;
import com.insidious.plugin.ui.methodscope.OnCloseListener;
import com.insidious.plugin.util.AtomicAssertionUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import org.json.JSONArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class TestCandidateSaveForm {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateSaveForm.class);
    private final List<TestCandidateMetadata> candidateMetadataList;
    private final SaveFormListener saveFormListener;
    private final List<StoredCandidate> candidateList;
    private final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final Map<StoredCandidate, StoredCandidateItemPanel> candidatePanelMap = new HashMap<>();
    private JPanel mainPanel;
    private JLabel assertionCountLabel;
    private JLabel linesCountLabel;
    private JLabel selectedReplayCountLabel;
    private JLabel mockCallCountLabel;
    private JSeparator replaySummaryLine;
    private JLabel replayExpandIcon;
    private JPanel replayLineContainer;
    private JPanel hiddenCandidateListContainer;
    private JPanel candidateListContainer;
    private JButton confirmButton;
    private JButton cancelButton;
    private JCheckBox checkBox1;
    private JCheckBox checkBox2;
    private JRadioButton integrationRadioButton;
    private JRadioButton unitRadioButton;

    public TestCandidateSaveForm(List<TestCandidateMetadata> candidateMetadataList,
                                 SaveFormListener saveFormListener, OnCloseListener<TestCandidateSaveForm> onCloseListener) {
        this.candidateMetadataList = candidateMetadataList;
        this.saveFormListener = saveFormListener;
        selectedReplayCountLabel.setText(candidateMetadataList.size() + " replay selected");

        long downstreamCallCount = candidateMetadataList.stream().mapToLong(e -> e.getCallsList().size()).sum();
        mockCallCountLabel.setText(downstreamCallCount + " downstream call mocks");


        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (StoredCandidate storedCandidate : candidateList) {
                    saveFormListener.onSaved(storedCandidate);
                }
                onCloseListener.onClose(TestCandidateSaveForm.this);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCloseListener.onClose(TestCandidateSaveForm.this);
            }
        });

        replaySummaryLine.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayExpandIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectedReplayCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayLineContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ItemLifeCycleListener<StoredCandidate> listener = new ItemLifeCycleListener<StoredCandidate>() {
            @Override
            public void onSelect(StoredCandidate item) {

            }

            @Override
            public void onUnSelect(StoredCandidate item) {

            }

            @Override
            public void onDelete(StoredCandidate item) {

            }

            @Override
            public void onEdit(StoredCandidate item) {

            }
        };

        JPanel itemContainer = new JPanel();
        itemContainer.setLayout(new GridLayout(0, 1));
        JBScrollPane itemScroller = new JBScrollPane(itemContainer);
        itemScroller.setMaximumSize(new Dimension(500, 400));
        candidateListContainer.add(itemScroller, BorderLayout.CENTER);


        MouseAdapter showCandidatesAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hiddenCandidateListContainer.isVisible()) {
                    replayExpandIcon.setIcon(UIUtils.COTRACT_UP_DOWN_ICON);
                    hiddenCandidateListContainer.setVisible(false);
                } else {
                    replayExpandIcon.setIcon(UIUtils.COTRACT_UP_DOWN_ICON);
                    hiddenCandidateListContainer.setVisible(true);
                }
            }
        };
        replaySummaryLine.addMouseListener(showCandidatesAdapter);
        replayExpandIcon.addMouseListener(showCandidatesAdapter);
        selectedReplayCountLabel.addMouseListener(showCandidatesAdapter);
        replayLineContainer.addMouseListener(showCandidatesAdapter);


        candidateList = candidateMetadataList.stream()
                .map(candidateMetadata -> {

                    JsonNode returnValue;
                    MethodCallExpression mainMethod = candidateMetadata.getMainMethod();
                    Parameter returnValue1 = mainMethod.getReturnValue();
                    String stringValue = returnValue1.getStringValue();
                    String nonNullReturnValue = stringValue == null ? "null" : stringValue;
                    try {
                        returnValue = objectMapper.readTree(nonNullReturnValue);
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse response value as a json object: " + e.getMessage());
                        returnValue =
                                objectMapper.getNodeFactory().textNode(nonNullReturnValue);
                    }
                    StoredCandidate storedCandidate = new StoredCandidate(candidateMetadata);


                    AtomicAssertion assertion = createAssertions(returnValue);
                    storedCandidate.setTestAssertions(assertion);
                    return storedCandidate;
                })
                .collect(Collectors.toList());

        long assertionCount = candidateList.stream()
                .map(e -> AtomicAssertionUtils.countAssertions(e.getTestAssertions()))
                .collect(Collectors.summarizingInt(e -> e)).getCount();

        assertionCountLabel.setText(assertionCount + " assertions");

        Set<String> lineCoverageMap = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (StoredCandidate storedCandidate : candidateList) {
            List<Integer> lines = storedCandidate.getLineNumbers();
            String hashKey = storedCandidate.getMethod().getMethodHashKey() + "#" + lines;

            classNames.add(storedCandidate.getMethod().getClassName());
            lineCoverageMap.add(hashKey);
        }

        int classCount = classNames.size();
        linesCountLabel.setText(lineCoverageMap.size() + " unique lines covered in " + classCount + " class"
                + (classCount == 1 ? "" : "es"));
        for (StoredCandidate storedCandidate : candidateList) {
            StoredCandidateItemPanel storedCandidateItemPanel = new StoredCandidateItemPanel(storedCandidate, listener,
                    saveFormListener.getProject());
            candidatePanelMap.put(storedCandidate,  storedCandidateItemPanel);
            itemContainer.add(storedCandidateItemPanel.getComponent(), new GridConstraints());
        }


    }

    private AtomicAssertion createAssertions(JsonNode value, String key) {
        if (value.isArray()) {

            AtomicAssertion parentAssertion = new AtomicAssertion(Expression.SELF, AssertionType.ALLOF, key, null);


            int arrayObjectLength = value.size();
            for (int i = 0; i < arrayObjectLength; i++) {
                JsonNode arrayObject = value.get(i);
                AtomicAssertion subAssertions = createAssertions(arrayObject,
                        (Objects.equals(key, "/") ? "/" + i : key + "/" + i));
                parentAssertion.getSubAssertions().add(subAssertions);
            }
            return parentAssertion;

        } else if (value.isObject()) {
            Iterator<String> fields = value.fieldNames();
            AtomicAssertion parentAssertion = new AtomicAssertion(Expression.SELF, AssertionType.ALLOF, key, null);
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode fieldValue = value.get(field);
                AtomicAssertion subAssertions = createAssertions(fieldValue,
                        (Objects.equals(key, "/") ? "/" + field : key + "/" + field));
                parentAssertion.getSubAssertions().add(subAssertions);
            }

            return parentAssertion;
        } else {
            return new AtomicAssertion(Expression.SELF, AssertionType.EQUAL, key,
                    value.toString());
        }
    }

    private AtomicAssertion createAssertions(JsonNode returnValue) {
        return createAssertions(returnValue, "/");
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
