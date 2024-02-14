package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.library.DeclaredMockItemPanel;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.insidious.plugin.ui.library.StoredCandidateItemPanel;
import com.insidious.plugin.ui.methodscope.OnCloseListener;
import com.insidious.plugin.util.*;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TestCandidateSaveForm {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateSaveForm.class);
    private final List<TestCandidateMetadata> candidateMetadataList;
    private final SaveFormListener saveFormListener;
    private final List<StoredCandidate> candidateList;
    private final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final Map<StoredCandidate, StoredCandidateItemPanel> candidatePanelMap = new HashMap<>();
    private final Map<DeclaredMock, DeclaredMockItemPanel> declaredMockPanelMap = new HashMap<>();
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
    private JLabel candidateInfoIcon;
    private JLabel mockInfoIcon;

    public TestCandidateSaveForm(List<TestCandidateMetadata> sourceCandidates,
                                 SaveFormListener saveFormListener, OnCloseListener<TestCandidateSaveForm> onCloseListener) {
        InsidiousService service = saveFormListener.getProject().getService(InsidiousService.class);

//        ProgressManager instance = ProgressManager.getInstance();
//        ProgressIndicator progressIndicator = instance.getProgressIndicator();
//        progressIndicator.setText("Loading " + sourceCandidates.size() + " Replay");

        List<TestCandidateMetadata> list = new ArrayList<>();
        for (TestCandidateMetadata sourceCandidate : sourceCandidates) {
//            if (progressIndicator.isCanceled()) {
//                throw new RuntimeException("saving cancelled");
//            }
            TestCandidateMetadata testCandidateById = service.getTestCandidateById(sourceCandidate.getEntryProbeIndex(),
                    true);
            list.add(testCandidateById);
        }
        this.candidateMetadataList = list;


        candidateInfoIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(candidateInfoIcon, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                                0, 0, 0, false));
            }
        });
        candidateInfoIcon.setToolTipText(
                "" +
                        "<html>" +
                        "Run from CLI using" +
                        "<pre>mvn test</pre>" +
                        " or " +
                        "<pre>gradle test</pre>" +
                        "</html>"
        );

        mockInfoIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(candidateInfoIcon, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                                0, 0, 0, false));
            }
        });

        mockInfoIcon.setToolTipText(
                "" +
                        "Saved mocks can be used for real time mocking (Enable from Library) and managed inside Library"
        );

        this.saveFormListener = saveFormListener;
        selectedReplayCountLabel.setText(candidateMetadataList.size() + " replay selected");

        List<DeclaredMock> mocksList = ApplicationManager.getApplication().runReadAction(
                (Computable<List<DeclaredMock>>) () -> collectDownstreamMockCalls(candidateMetadataList));

        mockCallCountLabel.setText(mocksList.size() + " downstream call mocks");


        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    for (StoredCandidate storedCandidate : candidateList) {
                        if (!unitRadioButton.isSelected()) {
                            storedCandidate.setMockIds(new HashSet<>());
                        }
                        saveFormListener.onSaved(storedCandidate);
                    }

                    Collection<DeclaredMockItemPanel> values = declaredMockPanelMap.values();
                    for (DeclaredMockItemPanel value : values) {
                        saveFormListener.onSaved(value);
                    }


                    InsidiousNotification
                            .notifyMessage(
                                    "Saved " + candidateList.size() + " replay tests and "
                                            + values.size() + " mock definitions", NotificationType.INFORMATION,
                                    List.of(
                                            new AnAction(new Supplier<String>() {
                                                @Override
                                                public String get() {
                                                    return "Show Library";
                                                }
                                            }, UIUtils.LIBRARY_ICON) {
                                                @Override
                                                public void actionPerformed(@NotNull AnActionEvent e) {
                                                    saveFormListener.getProject()
                                                            .getService(InsidiousService.class)
                                                            .showLibrary();
                                                }
                                            }
                                    )
                            );

                    onCloseListener.onClose(TestCandidateSaveForm.this);
                });
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCloseListener.onClose(TestCandidateSaveForm.this);
            }
        });


        JPanel candidateItemContainer = new JPanel();
        candidateItemContainer.setLayout(new GridBagLayout());
        JBScrollPane itemScroller = new JBScrollPane(candidateItemContainer);
        itemScroller.setBorder(BorderFactory.createEmptyBorder());

        replayScrollParentPanel.add(itemScroller, BorderLayout.CENTER);

        candidateItemContainer.setAlignmentY(0);
        candidateItemContainer.setAlignmentX(0);


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
            candidateItemContainer.add(storedCandidateItemPanel.getComponent(), createGBCForLeftMainComponent(i));
        }
        candidateItemContainer.add(new JPanel(), createGBCForFakeComponent(candidateList.size()));


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
        mockItemScroller.setBorder(BorderFactory.createEmptyBorder());
        mockScrollParentPanel.add(mockItemScroller, BorderLayout.CENTER);

        mockItemContainer.setAlignmentY(0);
        mockItemContainer.setAlignmentX(0);

        for (int i = 0; i < mocksList.size(); i++) {
            DeclaredMock declaredMock = mocksList.get(i);
            DeclaredMockItemPanel declaredMockItemPanel = new DeclaredMockItemPanel(declaredMock,
                    itemLifeCycleListener, project);
            declaredMockItemPanel.setIsSelectable(false);
            declaredMockPanelMap.put(declaredMock, declaredMockItemPanel);
            mockItemContainer.add(declaredMockItemPanel.getComponent(), createGBCForLeftMainComponent(i));
        }


        mockItemContainer.add(new JPanel(), createGBCForFakeComponent(mocksList.size()));


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
        assertionItemScroller.setBorder(BorderFactory.createEmptyBorder());

        JLabel assertionDescriptionPanel = new JLabel("Can be customized from library after saving");
        assertionDescriptionPanel.setIcon(UIUtils.INFO_ICON);
        JPanel labelContainer = new JPanel();
        assertionDescriptionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        labelContainer.add(assertionDescriptionPanel, BorderLayout.CENTER);
        assertionsScrollParentPanel.add(assertionDescriptionPanel, BorderLayout.NORTH);
        assertionsScrollParentPanel.add(assertionItemScroller, BorderLayout.CENTER);

        assertionItemContainer.setAlignmentY(0);
        assertionItemContainer.setAlignmentX(0);


        Set<String> doneMap = new HashSet<>();
        int assertionPanelCount = 0;
        for (StoredCandidate storedCandidate : candidateList) {
            if (doneMap.contains(storedCandidate.getMethod().getMethodHashKey())) {
                continue;
            }
            doneMap.add(storedCandidate.getMethod().getMethodHashKey());

            AtomicAssertion atomicAssertion = storedCandidate.getTestAssertions();
            AtomicAssertionItemPanel atomicAssertionItemPanel = new AtomicAssertionItemPanel(
                    atomicAssertion, atomicAssertionLifeListener, project);

            atomicAssertionPanelMap.put(atomicAssertion, atomicAssertionItemPanel);
            assertionItemContainer.add(atomicAssertionItemPanel.getComponent(),
                    createGBCForLeftMainComponent(assertionPanelCount));
            int count = AtomicAssertionUtils.countAssertions(atomicAssertion);
            atomicAssertionItemPanel.setTitle(count + " assertions for " +
                    ClassTypeUtils.getSimpleClassName(storedCandidate.getReturnValueClassname()));
            assertionPanelCount++;
        }
        assertionItemContainer.add(new JPanel(), createGBCForFakeComponent(assertionPanelCount));


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

    private List<PsiMethodCallExpression> getAllCallExpressions(PsiMethod targetMethod) {
//        @Nullable PsiClass containingClass = targetMethod.getContainingClass();
        List<PsiMethodCallExpression> psiMethodCallExpressions = new ArrayList<>(PsiTreeUtil.findChildrenOfType(
                targetMethod, PsiMethodCallExpression.class));

        List<PsiMethodCallExpression> collectedCalls = new ArrayList<>();

        for (PsiMethodCallExpression psiMethodCallExpression : psiMethodCallExpressions) {
            PsiExpression qualifierExpression = psiMethodCallExpression.getMethodExpression()
                    .getQualifierExpression();
            if (qualifierExpression == null) {
                // this call needs to be scanned
                PsiMethod subTargetMethod = (PsiMethod) psiMethodCallExpression.getMethodExpression().resolve();
                if (subTargetMethod.hasModifier(JvmModifier.STATIC)) {
                    // static methods to be added as it is for now
                    // but lets scan them also for down stream calls from their fields
                    // for possible support of injection in future
                    collectedCalls.add(psiMethodCallExpression);
                }
                List<PsiMethodCallExpression> subCalls = getAllCallExpressions(subTargetMethod);
                collectedCalls.addAll(subCalls);
            } else {
                collectedCalls.add(psiMethodCallExpression);
            }
        }

        return collectedCalls;
    }

    private List<DeclaredMock> collectDownstreamMockCalls(List<TestCandidateMetadata> candidateMetadataList) {

        Map<String, DeclaredMock> mocks = new HashMap<>();
        for (TestCandidateMetadata testCandidateMetadata : candidateMetadataList) {
            PsiMethod targetMethod = ClassTypeUtils.getPsiMethod(testCandidateMetadata.getMainMethod(),
                    saveFormListener.getProject());

            List<PsiMethodCallExpression> allCallExpressions = getAllCallExpressions(targetMethod);


            Map<String, List<PsiMethodCallExpression>> expressionsBySignatureMap = allCallExpressions.stream()
                    .collect(Collectors.groupingBy(e1 -> {
                        PsiMethod method = e1.resolveMethod();
                        return MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(method))
                                .getMethodHashKey();
                    }));


            mocks = new HashMap<>();

            List<MethodCallExpression> callsList = testCandidateMetadata.getCallsList();
            List<MethodCallExpression> callListCopy = new ArrayList<>(callsList);
            while (callListCopy.size() > 0) {
                MethodCallExpression methodCallExpression = callListCopy.remove(0);

                PsiMethod psiMethod = ClassTypeUtils.getPsiMethod(methodCallExpression, saveFormListener.getProject());
                if (psiMethod == null) {
                    logger.warn(
                            "Failed to resolve method: " + methodCallExpression + ", call will not be mocked");
                    continue;
                }
                MethodUnderTest mockMethodTarget = MethodUnderTest.fromMethodAdapter(
                        new JavaMethodAdapter(psiMethod));

                List<PsiMethodCallExpression> expressionsBySignature = expressionsBySignatureMap.get(
                        mockMethodTarget.getMethodHashKey());

                if (expressionsBySignature == null) {
                    // this call is not on a field. it is probably a call to a method in the same class
                    // not mocking this
                    logger.warn("Skipping call for mocking: " + mockMethodTarget);
                    continue;
                }

                PsiMethodCallExpression methodCallExpression1 = expressionsBySignature.get(0);
//                    methodCallExpression1.getMethodExpression()

                PsiReferenceExpression methodExpression = methodCallExpression1.getMethodExpression();
                PsiExpression qualifierExpression1 = methodExpression.getQualifierExpression();
                if (qualifierExpression1 == null) {
                    // call to another method in the same class :)
                    // should never happen
                    continue;
                }

                if (!(qualifierExpression1 instanceof PsiReferenceExpression)) {
                    // what is this ? TODO: add support for chain mocking
                    continue;
                }
                PsiReferenceExpression qualifierExpression = (PsiReferenceExpression) qualifierExpression1;
                PsiElement qualifierField = qualifierExpression.resolve();
                if (!(qualifierField instanceof PsiField)) {
                    // call is not on a field
                    continue;
                }
                DeclaredMock declaredMock = ApplicationManager.getApplication().runReadAction(
                        (Computable<DeclaredMock>) () -> ClassUtils.createDefaultMock(methodCallExpression1));

                DeclaredMock existingMock = mocks.get(mockMethodTarget.getMethodHashKey());
                if (existingMock == null) {
                    mocks.put(mockMethodTarget.getMethodHashKey(), declaredMock);
                } else {
                    existingMock.getThenParameter().addAll(declaredMock.getThenParameter());
                }


            }


        }


        return new ArrayList<>(mocks.values());

    }

    private AtomicAssertion createAssertions(JsonNode value, String key) {
        if (value.isArray()) {

            AtomicAssertion parentAssertion = new AtomicAssertion(Expression.SELF, AssertionType.ALLOF, key, "true");


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
            AtomicAssertion parentAssertion = new AtomicAssertion(Expression.SELF, AssertionType.ALLOF, key, "true");
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode fieldValue = value.get(field);
                AtomicAssertion subAssertions = createAssertions(fieldValue,
                        (Objects.equals(key, "/") ? "/" + field : key + "/" + field));
                parentAssertion.getSubAssertions().add(subAssertions);
            }

            return parentAssertion;
        } else {
            return new AtomicAssertion(Expression.SELF, AssertionType.EQUAL, key, value.toString());
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
