package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.components.OnOffButton;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionPanel {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionPanel.class);
    private final InsidiousService insidiousService;
    private final MethodUnderTest methodUnderTest;
    private final PsiMethodCallExpression methodCallExpression;
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

    public MockDefinitionPanel(PsiMethodCallExpression methodCallExpression) {
        this.methodCallExpression = methodCallExpression;

        insidiousService = methodCallExpression.getProject().getService(InsidiousService.class);

        PsiMethod targetMethod = methodCallExpression.resolveMethod();
        methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethod));

        mockSwitchPanel.add(new OnOffButton(), BorderLayout.EAST);

        List<DeclaredMock> declaredMockList = insidiousService.getDeclaredMocks(methodUnderTest);

        int savedCandidateCount = declaredMockList.size();

        if (savedCandidateCount == 0) {
            ApplicationManager.getApplication().invokeLater(() -> {
                showMockEditor(null);
            });
        } else {
            JPanel itemListPanel = new JPanel();

            itemListPanel.setBorder(BorderFactory.createEmptyBorder());
            itemListPanel.setLayout(new GridLayout(savedCandidateCount, 1));
            itemListPanel.setAlignmentY(0);
            for (int i = 0; i < savedCandidateCount; i++) {
                SavedMockItemPanel savedMockItem = new SavedMockItemPanel(declaredMockList.get(i));
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
            savedItemScrollPanel.setViewportView(itemListPanel);

        }

        addNewMockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMockEditor(null);
            }
        });


    }

    private void showMockEditor(DeclaredMock declaredMock) {

        MockDefinitionEditor mockDefinitionEditor;
        if (declaredMock == null) {
            mockDefinitionEditor = new MockDefinitionEditor(methodUnderTest,
                    methodCallExpression.getMethodExpression().getQualifiedName());
        } else {
            mockDefinitionEditor = new MockDefinitionEditor(methodUnderTest, declaredMock);
        }

        JComponent gutterMethodComponent = mockDefinitionEditor.getComponent();

        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(gutterMethodComponent, null);

        gutterMethodComponentPopup
                .setProject(methodCallExpression.getProject())
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .setResizable(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelKeyEnabled(true)
//                .setCancelButton(gutterMethodPanel.getCloseButton())
                .setBelongsToGlobalPopupStack(false)
                .setTitle("Define mocks for " + methodCallExpression.getText())
                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
                .createPopup()
                .showUnderneathOf(addNewMockButton);


    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
