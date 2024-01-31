package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.ui.components.OnOffButton;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionListPanel implements DeclaredMockLifecycleListener, OnSaveListener {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionListPanel.class);
    private final InsidiousService insidiousService;
    private final MethodUnderTest methodUnderTest;
    private final PsiMethodCallExpression methodCallExpression;
    private final JPanel itemListPanel = new JPanel();
    private final String fieldName;
    private final String parentClassName;
    private final OnOffButton fieldMockSwitch;
    private JPanel mockDefinitionTitlePanel;
    private JLabel mockedMethodText;
    private JLabel mockEnableSwitchLabel;
    private JPanel mockSwitchContainer;
    private JPanel mockFieldSwitchPanel;
    private JButton addNewMockButton;
    private JPanel savedMocksListParent;
    private JPanel savedMocksTitlePanel;
    private JPanel newMockButtonPanel;
    private JPanel titleEastPanel;
    private JPanel titleWestPanel;
    private JPanel mainPanel;
    private JPanel titlePanelParent;
    private JScrollPane savedItemScrollPanel;
    private JPanel northPanel;
    private JLabel mockCountLabel;
    private JLabel permanentMockHelpLabel;
    private JBPopup componentPopUp;
    private List<DeclaredMock> declaredMockList;

    public MockDefinitionListPanel(PsiMethodCallExpression methodCallExpression) {
        this.methodCallExpression = methodCallExpression;

        PsiExpression fieldExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        this.fieldName = fieldExpression.getText();
        PsiReferenceExpression qualifierExpression1 = (PsiReferenceExpression) fieldExpression;
        PsiField fieldPsiInstance = (PsiField) qualifierExpression1.resolve();

        savedItemScrollPanel.setViewportView(itemListPanel);
        itemListPanel.setBorder(BorderFactory.createEmptyBorder());
        itemListPanel.setAlignmentY(0);

        PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        PsiSubstitutor classSubstitutor = TypeConversionUtil.getClassSubstitutor(fieldPsiInstance.getContainingClass(),
                parentOfType, PsiSubstitutor.EMPTY);
        PsiType fieldTypeSubstitutor = ClassTypeUtils.substituteClassRecursively(fieldPsiInstance.getType(),
                classSubstitutor);

        parentClassName = parentOfType.getQualifiedName();

        insidiousService = methodCallExpression.getProject().getService(InsidiousService.class);

        PsiMethod targetMethod = methodCallExpression.resolveMethod();
        methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethod));
        if (fieldPsiInstance != null && fieldPsiInstance.getType() != null) {
            methodUnderTest.setClassName(fieldPsiInstance.getType().getCanonicalText());
        }

        if (fieldTypeSubstitutor != null) {
            String actualClass = fieldTypeSubstitutor.getCanonicalText();
            methodUnderTest.setClassName(actualClass);
        }

        boolean fieldMockIsActive = insidiousService.isFieldMockActive(parentClassName, fieldName);
//        boolean fieldMockIsActive = insidiousService.isPermanentMocks();
        fieldMockSwitch = new OnOffButton();
        fieldMockSwitch.setSelected(fieldMockIsActive);

        permanentMockHelpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);


                insidiousService.getProject()
                        .getMessageBus().syncPublisher(Notifications.TOPIC)
                        .notify(new Notification(InsidiousNotification.DISPLAY_ID, "Permanent mocks",
                                "Activate Persistent Mocking to simulate call responses when triggered by external " +
                                        "methods.\n All the enabled mocks will work for all code executions",
                                NotificationType.INFORMATION));


            }
        });


        mockFieldSwitchPanel.add(fieldMockSwitch, BorderLayout.EAST);
        if (!insidiousService.isAgentConnected()) {
            fieldMockSwitch.setEnabled(false);
            fieldMockSwitch.setToolTipText("Start your application with unlogged-sdk to enable permanent mocking");
        } else {
            fieldMockSwitch.addActionListener(e -> {
                boolean isActive = fieldMockSwitch.isSelected();
                logger.warn("Field active changed: " + isActive);
                if (isActive) {
                    // inject only those mock definitions which are marked as enabled
                    List<DeclaredMock> declaredMocksOf = insidiousService
                            .getDeclaredMocksOf(methodUnderTest)
                            .stream()
                            .filter(insidiousService::isMockEnabled)
                            .collect(Collectors.toList());

                    insidiousService.injectMocksInRunningProcess(declaredMocksOf);
                    insidiousService.enableFieldMock(parentClassName, fieldName);
                } else {
                    // try to remove all mocks irrespective of they are enabled or not
                    List<DeclaredMock> declaredMocksOf = insidiousService.getDeclaredMocksOf(methodUnderTest);
                    insidiousService.removeMocksInRunningProcess(declaredMocksOf);
                    insidiousService.disableFieldMock(parentClassName, fieldName);
                }
            });
        }

        int argumentCount = targetMethod.getParameterList().getParametersCount();
        mockedMethodText.setText(
                methodCallExpression.getMethodExpression().getText()
                        + "( " + (
                        argumentCount == 1 ? "1 Argument" : (argumentCount + " Arguments")
                ) + " )"
        );

        addNewMockButton.addActionListener(e -> showMockEditor(null));
        loadDefinitions(true);


    }

    private void loadDefinitions(boolean showAddNewIfEmpty) {
        declaredMockList = insidiousService.getDeclaredMocksOf(methodUnderTest);

        int savedCandidateCount = declaredMockList.size();

        if (savedCandidateCount == 1) {
            mockCountLabel.setText(savedCandidateCount + " declared mock");
        } else {
            mockCountLabel.setText(savedCandidateCount + " declared mocks");
        }
        if (savedCandidateCount == 0 && showAddNewIfEmpty) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                showMockEditor(null);
            });
        } else {

            itemListPanel.removeAll();
            itemListPanel.setLayout(new GridLayout(savedCandidateCount, 1));
            for (int i = 0; i < savedCandidateCount; i++) {
                DeclaredMock declaredMock = declaredMockList.get(i);
                SavedMockItemPanel savedMockItem = new SavedMockItemPanel(declaredMock, this,
                        insidiousService.isMockEnabled(declaredMock));
                GridConstraints constraints = new GridConstraints(
                        i, 0, 1, 1, ANCHOR_NORTH,
                        GridConstraints.FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK,
                        SIZEPOLICY_FIXED,
                        new Dimension(-1, 75),
                        new Dimension(-1, 75),
                        new Dimension(-1, 75)
                );
                Component component = savedMockItem.getComponent();
                itemListPanel.add(component, constraints);
            }

            savedItemScrollPanel.setBorder(BorderFactory.createEmptyBorder());
            int containerHeight = Math.min(300, savedCandidateCount * 75);

            savedItemScrollPanel.getViewport().setSize(new Dimension(-1, containerHeight));
            savedItemScrollPanel.getViewport().setPreferredSize(new Dimension(-1, containerHeight));
            savedItemScrollPanel.setPreferredSize(new Dimension(-1, containerHeight));
            savedItemScrollPanel.setSize(new Dimension(-1, containerHeight));


            itemListPanel.revalidate();
            itemListPanel.repaint();
            savedItemScrollPanel.revalidate();
            savedItemScrollPanel.repaint();
            mainPanel.repaint();
            mainPanel.revalidate();
            if (componentPopUp != null) {
                Dimension currentSize = componentPopUp.getSize();
                if (currentSize != null) {
                    componentPopUp.setSize(new Dimension((int) currentSize.getWidth(), containerHeight + 140));
                }
            }
        }
    }

    public void showMockEditor(DeclaredMock declaredMock) {
        JBPopup editorPopup = null;

        MockDefinitionEditor mockDefinitionEditor;
        if (declaredMock == null) {
            mockDefinitionEditor = ApplicationManager.getApplication().runReadAction(
                    (Computable<MockDefinitionEditor>) () -> new MockDefinitionEditor(methodUnderTest,
                            methodCallExpression, methodCallExpression.getProject(), this));
        } else {
            mockDefinitionEditor = ApplicationManager.getApplication().runReadAction(
                    (Computable<MockDefinitionEditor>) () -> new MockDefinitionEditor(methodUnderTest,
                            new DeclaredMock(declaredMock), methodCallExpression.getProject(), this));
        }

        JComponent gutterMethodComponent = mockDefinitionEditor.getComponent();

        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(gutterMethodComponent, null);

        editorPopup = gutterMethodComponentPopup
                .setProject(methodCallExpression.getProject())
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .setResizable(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelKeyEnabled(true)
                .setBelongsToGlobalPopupStack(false)
                .setTitle("Mock Editor")
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(LightweightWindowEvent event) {
                        JBPopupListener.super.onClosed(event);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            loadDefinitions(false);
                        });
                    }
                })
                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
                .createPopup();
        JBPopup finalEditorPopup = editorPopup;
        ApplicationManager.getApplication().invokeLater(() -> {
            finalEditorPopup.showUnderneathOf(addNewMockButton);
        });

        mockDefinitionEditor.setPopupHandle(editorPopup);


    }

    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void onUpdateRequest(DeclaredMock declaredMock) {
        showMockEditor(declaredMock);
    }

    @Override
    public void onDeleteRequest(DeclaredMock declaredMock) {
        insidiousService.deleteMockDefinition(methodUnderTest, declaredMock);
        ApplicationManager.getApplication().invokeLater(() -> {
            loadDefinitions(false);
        });
    }

    @Override
    public void onEnable(DeclaredMock declaredMock) {
        insidiousService.enableMock(declaredMock);
//        if (!fieldMockSwitch.isSelected()) {
//            fieldMockSwitch.setSelected(true);
//        }
    }

    @Override
    public void onDisable(DeclaredMock declaredMock) {
        insidiousService.disableMock(declaredMock);
    }

    public void setPopupHandle(JBPopup componentPopUp) {
        this.componentPopUp = componentPopUp;
    }

    @Override
    public void onSaveDeclaredMock(DeclaredMock declaredMock, MethodUnderTest methodUnderTest) {
        insidiousService.saveMockDefinition(declaredMock, this.methodUnderTest);
        insidiousService.enableMock(declaredMock);
//        insidiousService.enableFieldMock(parentClassName, fieldName);
//        fieldMockSwitch.setSelected(true);
    }
}
