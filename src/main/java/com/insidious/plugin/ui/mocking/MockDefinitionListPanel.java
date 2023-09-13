package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.*;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.components.OnOffButton;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionListPanel implements UpdateDeleteListener<DeclaredMock> {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionListPanel.class);
    private final InsidiousService insidiousService;
    private final MethodUnderTest methodUnderTest;
    private final PsiMethodCallExpression methodCallExpression;
    private final JPanel itemListPanel = new JPanel();
    private JPanel mockDefinitionTitlePanel;
    private JLabel mockedMethodText;
    private JLabel mockEnableSwitchLabel;
    private JPanel mockSwitchContainer;
    private JPanel mockSwitchPanel;
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

    public MockDefinitionListPanel(PsiMethodCallExpression methodCallExpression) {
        this.methodCallExpression = methodCallExpression;

        savedItemScrollPanel.setViewportView(itemListPanel);
        itemListPanel.setBorder(BorderFactory.createEmptyBorder());
        itemListPanel.setAlignmentY(0);

        insidiousService = methodCallExpression.getProject().getService(InsidiousService.class);

        PsiMethod targetMethod = methodCallExpression.resolveMethod();
        methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethod));

        mockSwitchPanel.add(new OnOffButton(), BorderLayout.EAST);

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
        logger.warn("Load mock definitions for " + methodUnderTest);
        List<DeclaredMock> declaredMockList = insidiousService.getDeclaredMocks(methodUnderTest);

        int savedCandidateCount = declaredMockList.size();

        if (savedCandidateCount == 1) {
            mockCountLabel.setText(savedCandidateCount + " declared mock");
        } else {
            mockCountLabel.setText(savedCandidateCount + " declared mocks");
        }
        if (savedCandidateCount == 0 && showAddNewIfEmpty) {
            ApplicationManager.getApplication().invokeLater(() -> {
                showMockEditor(null);
            });
        } else {

            itemListPanel.removeAll();
            itemListPanel.setLayout(new GridLayout(savedCandidateCount, 1));
            for (int i = 0; i < savedCandidateCount; i++) {
                SavedMockItemPanel savedMockItem = new SavedMockItemPanel(declaredMockList.get(i), this);
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
        }
    }

    private void showMockEditor(DeclaredMock declaredMock) {
        JBPopup editorPopup = null;

        MockDefinitionEditor mockDefinitionEditor;
        if (declaredMock == null) {
            mockDefinitionEditor = new MockDefinitionEditor(methodUnderTest, methodCallExpression,
                    methodCallExpression.getProject());
        } else {
            mockDefinitionEditor = new MockDefinitionEditor(methodUnderTest, new DeclaredMock(declaredMock),
                    methodCallExpression.getProject());
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
                        loadDefinitions(false);
                        JBPopupListener.super.onClosed(event);
                    }
                })
                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
                .createPopup();
        editorPopup.showUnderneathOf(addNewMockButton);

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
        loadDefinitions(true);
    }
}
