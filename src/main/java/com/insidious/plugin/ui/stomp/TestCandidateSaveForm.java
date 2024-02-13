package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.library.DeclaredMockItemPanel;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.insidious.plugin.ui.library.StoredCandidateItemPanel;
import com.insidious.plugin.ui.methodscope.OnCloseListener;
import com.insidious.plugin.util.AtomicAssertionUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

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
    private final Map<MethodCallExpression, DeclaredMockItemPanel> declaredMockPanelMap = new HashMap<>();
    private final Map<AtomicAssertion, AtomicAssertionItemPanel> atomicAssertionPanelMap = new HashMap<>();
    private JPanel mainPanel;
    private JLabel assertionCountLabel;
    private JLabel linesCountLabel;
    private JLabel selectedReplayCountLabel;
    private JLabel mockCallCountLabel;
    private JSeparator replaySummaryLine;
    private JLabel replayExpandIcon;
    private JPanel replayLabelContainer;
    private JPanel hiddenCandidateListContainer;
    private JButton confirmButton;
    private JButton cancelButton;
    private JRadioButton integrationRadioButton;
    private JRadioButton unitRadioButton;
    private JCheckBox checkBox1;
    private JPanel replayScrollParentPanel;
    private JSeparator mockSummaryExpandLine;
    private JLabel mockCallsExpandIcon;
    private JPanel mockScrollParentPanel;
    private JPanel hiddenMockListContainer;
    private JSeparator assertionLine;
    private JLabel assertionExpandIcon;
    private JSeparator linesCoveredLine;
    private JLabel linesCoveredExpandIcon;
    private JPanel assertionsScrollParentPanel;
    private JPanel hiddenAssertionsListContainer;

    public TestCandidateSaveForm(List<TestCandidateMetadata> candidateMetadataList,
                                 SaveFormListener saveFormListener, OnCloseListener<TestCandidateSaveForm> onCloseListener) {
        this.candidateMetadataList = candidateMetadataList;
        this.saveFormListener = saveFormListener;
        selectedReplayCountLabel.setText(candidateMetadataList.size() + " replay selected");

        List<MethodCallExpression> downstreamCallList = candidateMetadataList
                .stream()
                .flatMap(e -> e.getCallsList()
                        .stream()
                        .filter(e1 -> (e1.getSubject() != null && e1.getSubject().getValue() != 0))
                ).collect(Collectors.toList());
        mockCallCountLabel.setText(downstreamCallList.size() + " downstream call mocks");


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


        JPanel candiateItemContainer = new JPanel();
        candiateItemContainer.setLayout(new GridBagLayout());
        JBScrollPane itemScroller = new JBScrollPane(candiateItemContainer);
        replayScrollParentPanel.add(itemScroller, BorderLayout.CENTER);

        candiateItemContainer.setAlignmentY(0);
        candiateItemContainer.setAlignmentX(0);


        candidateList = candidateMetadataList.stream()
                .map(candidateMetadata -> {
                    StoredCandidate storedCandidate = new StoredCandidate(candidateMetadata);

                    JsonNode returnValue;
                    MethodCallExpression mainMethod = candidateMetadata.getMainMethod();
                    if (mainMethod.getReturnValue().getValue() == 0 || mainMethod.getReturnValue().getType() == null) {
                        return storedCandidate;
                    }
                    Parameter returnValue1 = mainMethod.getReturnValue();
                    if (returnValue1.getProb().getSerializedValue().length == 0) {
                        return storedCandidate;
                    }
                    String stringValue = new String(returnValue1.getProb().getSerializedValue());
                    if (stringValue.length() == 0) {
                        return storedCandidate;
                    }
                    try {
                        returnValue = objectMapper.readTree(stringValue);
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse response value as a json object: " + e.getMessage());
                        returnValue =
                                objectMapper.getNodeFactory().textNode(stringValue);
                    }


                    AtomicAssertion assertion;
                    if (!returnValue1.isException()) {
                        assertion = createAssertions(returnValue);
                    } else {
                        JsonNode message = returnValue.get("message");
                        String expectedValue = message == null ? "" : message.toString();
                        assertion = new AtomicAssertion(Expression.SELF, AssertionType.EQUAL, "/message",
                                expectedValue);
                    }
                    storedCandidate.setTestAssertions(assertion);
                    return storedCandidate;
                }).collect(Collectors.toList());


        List<AtomicAssertion> allAssertions = candidateList.stream()
                .flatMap(e -> AtomicAssertionUtils.flattenAssertionMap(e.getTestAssertions()).stream())
                .collect(Collectors.toList());

        long assertionCount = allAssertions.size();

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


        MouseAdapter showCandidatesAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hiddenCandidateListContainer.isVisible()) {
                    replayExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenCandidateListContainer.setVisible(false);
                } else {
                    replayExpandIcon.setIcon(UIUtils.COTRACT_UP_DOWN_ICON);
                    hiddenCandidateListContainer.setVisible(true);

                    mockCallsExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenMockListContainer.setVisible(false);

                    assertionExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenAssertionsListContainer.setVisible(false);

                }
            }
        };
        replaySummaryLine.addMouseListener(showCandidatesAdapter);
        replayExpandIcon.addMouseListener(showCandidatesAdapter);
        selectedReplayCountLabel.addMouseListener(showCandidatesAdapter);
        replayLabelContainer.addMouseListener(showCandidatesAdapter);
        replaySummaryLine.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayExpandIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectedReplayCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayLabelContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ItemLifeCycleListener<StoredCandidate> listener = new ItemLifeCycleListener<>() {
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


        Project project = saveFormListener.getProject();
        for (int i = 0; i < candidateList.size(); i++) {
            StoredCandidate storedCandidate = candidateList.get(i);
            StoredCandidateItemPanel storedCandidateItemPanel = new StoredCandidateItemPanel(storedCandidate, listener,
                    project);
            storedCandidateItemPanel.setIsSelectable(false);
            candidatePanelMap.put(storedCandidate, storedCandidateItemPanel);
            candiateItemContainer.add(storedCandidateItemPanel.getComponent(), createGBCForLeftMainComponent(i));
        }
        candiateItemContainer.add(new JPanel(), createGBCForFakeComponent(candidateList.size()));


        ItemLifeCycleListener<DeclaredMock> itemLifeCycleListener = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(DeclaredMock item) {

            }

            @Override
            public void onUnSelect(DeclaredMock item) {

            }

            @Override
            public void onDelete(DeclaredMock item) {

            }

            @Override
            public void onEdit(DeclaredMock item) {

            }
        };


        JPanel mockItemContainer = new JPanel();
        mockItemContainer.setLayout(new GridBagLayout());
        JBScrollPane mockItemScroller = new JBScrollPane(mockItemContainer);
        mockScrollParentPanel.add(mockItemScroller, BorderLayout.CENTER);

        mockItemContainer.setAlignmentY(0);
        mockItemContainer.setAlignmentX(0);


        int i = 0;
        while (i < downstreamCallList.size()) {
            MethodCallExpression methodCallExpression = downstreamCallList.get(i);
            DeclaredMock declaredMock = StompComponent.createDeclaredMockFromCallExpression(methodCallExpression,
                    project);
            DeclaredMockItemPanel declaredMockItemPanel = new DeclaredMockItemPanel(declaredMock,
                    itemLifeCycleListener, project);
            declaredMockItemPanel.setIsSelectable(false);
            declaredMockPanelMap.put(methodCallExpression, declaredMockItemPanel);
            mockItemContainer.add(declaredMockItemPanel.getComponent(), createGBCForLeftMainComponent(i));
            i++;
        }
        mockItemContainer.add(new JPanel(), createGBCForFakeComponent(downstreamCallList.size()));


        MouseAdapter showMocksAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hiddenMockListContainer.isVisible()) {
                    mockCallsExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenMockListContainer.setVisible(false);
                } else {
                    mockCallsExpandIcon.setIcon(UIUtils.COTRACT_UP_DOWN_ICON);
                    hiddenMockListContainer.setVisible(true);

                    replayExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenCandidateListContainer.setVisible(false);

                    assertionExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenAssertionsListContainer.setVisible(false);

                }
            }
        };
        mockSummaryExpandLine.addMouseListener(showMocksAdapter);
        mockCallsExpandIcon.addMouseListener(showMocksAdapter);
        mockCallCountLabel.addMouseListener(showMocksAdapter);
        mockSummaryExpandLine.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mockCallsExpandIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mockCallCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(AtomicAssertion item) {

            }

            @Override
            public void onUnSelect(AtomicAssertion item) {

            }

            @Override
            public void onDelete(AtomicAssertion item) {

            }

            @Override
            public void onEdit(AtomicAssertion item) {

            }
        };

        JPanel assertionItemContainer = new JPanel();
        assertionItemContainer.setLayout(new GridBagLayout());
        JBScrollPane assertionItemScroller = new JBScrollPane(assertionItemContainer);
        assertionsScrollParentPanel.add(assertionItemScroller, BorderLayout.CENTER);

        assertionItemContainer.setAlignmentY(0);
        assertionItemContainer.setAlignmentX(0);


        for (AtomicAssertion atomicAssertion : allAssertions) {
            AtomicAssertionItemPanel atomicAssertionItemPanel = new AtomicAssertionItemPanel(
                    atomicAssertion, atomicAssertionLifeListener, project);

            atomicAssertionPanelMap.put(atomicAssertion, atomicAssertionItemPanel);
            assertionItemContainer.add(atomicAssertionItemPanel.getComponent(), createGBCForLeftMainComponent(i));
        }
        assertionItemContainer.add(new JPanel(), createGBCForFakeComponent(allAssertions.size()));


        MouseAdapter showAssertionsAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (hiddenAssertionsListContainer.isVisible()) {
                    assertionExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenAssertionsListContainer.setVisible(false);
                } else {
                    assertionExpandIcon.setIcon(UIUtils.COTRACT_UP_DOWN_ICON);
                    hiddenAssertionsListContainer.setVisible(true);

                    replayExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenCandidateListContainer.setVisible(false);

                    mockCallsExpandIcon.setIcon(UIUtils.EXPAND_UP_DOWN_ICON);
                    hiddenMockListContainer.setVisible(false);

                }
            }
        };

        assertionExpandIcon.setIcon(UIUtils.COTRACT_UP_DOWN_ICON);
        hiddenAssertionsListContainer.setVisible(true);

        assertionLine.addMouseListener(showAssertionsAdapter);
        assertionExpandIcon.addMouseListener(showAssertionsAdapter);
        assertionCountLabel.addMouseListener(showAssertionsAdapter);
        assertionLine.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        assertionExpandIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        assertionCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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


    private GridBagConstraints createGBCForLeftMainComponent(int i) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = i;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }


    private GridBagConstraints createGBCForFakeComponent(int i) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = i;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(8);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }

}
