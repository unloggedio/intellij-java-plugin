package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
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
//    private final OnOffButton fieldMockSwitch;
    private final PsiMethod targetMethod;
    private JPanel mockDefinitionTitlePanel;
    private JLabel mockedMethodText;
    private JButton addNewMockButton;
    private JPanel savedMocksListParent;
    private JPanel savedMocksTitlePanel;
    private JPanel newMockButtonPanel;
    private JPanel titleEastPanel;
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

        savedItemScrollPanel.setViewportView(itemListPanel);
        itemListPanel.setBorder(BorderFactory.createEmptyBorder());
        itemListPanel.setAlignmentY(0);

        PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        insidiousService = methodCallExpression.getProject().getService(InsidiousService.class);
        String parentClassName = parentOfType.getQualifiedName();
        PsiExpression fieldExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        String fieldName = fieldExpression.getText();
        targetMethod = methodCallExpression.resolveMethod();
        assert targetMethod != null;

        addNewMockButton.setIcon(AllIcons.General.Add);

        methodUnderTest = MethodUnderTest.fromPsiCallExpression(methodCallExpression);


//        boolean fieldMockIsActive = insidiousService.isMockActive(parentClassName, fieldName);
//        boolean fieldMockIsActive = insidiousService.isPermanentMocks();
//        fieldMockSwitch = new OnOffButton();
//        fieldMockSwitch.setSelected(fieldMockIsActive);

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


//        mockFieldSwitchPanel.add(fieldMockSwitch, BorderLayout.EAST);
//        if (!insidiousService.isAgentConnected()) {
//            fieldMockSwitch.setEnabled(false);
//            fieldMockSwitch.setToolTipText("Start your application with unlogged-sdk to enable permanent mocking");
//        } else {
//            fieldMockSwitch.addActionListener(e -> {
//                boolean isActive = fieldMockSwitch.isSelected();
//                logger.warn("Field active changed: " + isActive);
//                if (isActive) {
//                    // inject only those mock definitions which are marked as enabled
//                    List<DeclaredMock> declaredMocksOf = insidiousService
//                            .getDeclaredMocksOf(methodUnderTest)
//                            .stream()
//                            .filter(insidiousService::isMockEnabled)
//                            .collect(Collectors.toList());
//
//                    insidiousService.injectMocksInRunningProcess(declaredMocksOf);
//                    insidiousService.enableMock(declaredMocksOf);
//                } else {
//                    // try to remove all mocks irrespective of they are enabled or not
//                    List<DeclaredMock> declaredMocksOf = insidiousService.getDeclaredMocksOf(methodUnderTest);
//                    insidiousService.removeMocksInRunningProcess(declaredMocksOf);
//                    insidiousService.disableMock(declaredMocksOf);
//                }
//            });
//        }

        targetMethod.getParameterList();
        int argumentCount = targetMethod.getParameterList().getParametersCount();
        String argumentCountText = "<small>" + (argumentCount == 1 ? "1 Argument" : (argumentCount + " Arguments")) +
                "</small>";
        String text = "<html>" + methodCallExpression.getMethodExpression().getText()
                + "( " + argumentCountText + " )" + "</html>";
        mockedMethodText.setText(
                text
        );

        addNewMockButton.addActionListener(e -> {
            insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
        });
        loadDefinitions(true);


    }

    private void loadDefinitions(boolean showAddNewIfEmpty) {
        declaredMockList = insidiousService.getDeclaredMocksOf(methodUnderTest);

        int savedCandidateCount = declaredMockList.size();

        String mockCountLabelText;
        if (savedCandidateCount == 1) {
            mockCountLabelText = savedCandidateCount + " declared mock";
        } else {
            mockCountLabelText = savedCandidateCount + " declared mocks";
        }
        mockCountLabel.setText("<html><small>" + mockCountLabelText + "</html></small>");
        if (savedCandidateCount == 0 && showAddNewIfEmpty) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
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

//    public void showMockEditor(DeclaredMock declaredMock) {
//        JBPopup editorPopup = null;
//
//        MockDefinitionEditor mockDefinitionEditor;
//        if (declaredMock == null) {
//            mockDefinitionEditor = ApplicationManager.getApplication().runReadAction(
//                    (Computable<MockDefinitionEditor>) () -> new MockDefinitionEditor(methodUnderTest,
//                            methodCallExpression, methodCallExpression.getProject(), this,
//                            component -> {
//
//                            }));
//        } else {
//            mockDefinitionEditor = ApplicationManager.getApplication().runReadAction(
//                    (Computable<MockDefinitionEditor>) () -> new MockDefinitionEditor(methodUnderTest,
//                            new DeclaredMock(declaredMock), methodCallExpression.getProject(), this));
//        }
//
//        JComponent gutterMethodComponent = mockDefinitionEditor.getComponent();
//
//        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(gutterMethodComponent, null);
//
//        editorPopup = gutterMethodComponentPopup
//                .setProject(methodCallExpression.getProject())
//                .setShowBorder(true)
//                .setShowShadow(true)
//                .setFocusable(true)
//                .setRequestFocus(true)
//                .setResizable(true)
//                .setCancelOnClickOutside(true)
//                .setCancelOnOtherWindowOpen(true)
//                .setCancelKeyEnabled(true)
//                .setBelongsToGlobalPopupStack(false)
//                .setTitle("Mock Editor")
//                .addListener(new JBPopupListener() {
//                    @Override
//                    public void onClosed(LightweightWindowEvent event) {
//                        JBPopupListener.super.onClosed(event);
//                        ApplicationManager.getApplication().invokeLater(() -> {
//                            loadDefinitions(false);
//                        });
//                    }
//                })
//                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
//                .createPopup();
//        JBPopup finalEditorPopup = editorPopup;
//        ApplicationManager.getApplication().invokeLater(() -> {
//            finalEditorPopup.showUnderneathOf(addNewMockButton);
//        });
//
//    }

    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void onUpdateRequest(DeclaredMock declaredMock) {
        insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
    }

    @Override
    public void onDeleteRequest(DeclaredMock declaredMock) {
        insidiousService.deleteMockDefinition(declaredMock);
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
    public void onSaveDeclaredMock(DeclaredMock declaredMock) {
        insidiousService.saveMockDefinition(declaredMock);
        insidiousService.enableMock(declaredMock);
//        insidiousService.enableFieldMock(parentClassName, fieldName);
//        fieldMockSwitch.setSelected(true);
    }
}
