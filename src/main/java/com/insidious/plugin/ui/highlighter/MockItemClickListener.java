package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.ui.mocking.MockDefinitionListPanel;
import com.insidious.plugin.ui.mocking.OnSaveListener;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MockItemClickListener extends MouseAdapter {

    private final JPanel methodItemPanel;
    private final PsiMethodCallExpression methodCallExpression;
    private final Color originalBackgroundColor;
    private final Logger logger = LoggerUtil.getInstance(MockItemClickListener.class);

    public MockItemClickListener(JPanel methodItemPanel, PsiMethodCallExpression methodCallExpression) {
        this.methodItemPanel = methodItemPanel;
        this.methodCallExpression = methodCallExpression;
        originalBackgroundColor = methodItemPanel.getBackground();
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        logger.warn("Clicked item: " + methodItemPanel);
        MockDefinitionListPanel gutterMethodPanel = new MockDefinitionListPanel(methodCallExpression);

        JComponent gutterMethodComponent = gutterMethodPanel.getComponent();
        Dimension max = gutterMethodComponent.getMaximumSize();
        gutterMethodComponent.setMaximumSize(new Dimension((int) max.getWidth(), 650));

        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(gutterMethodComponent, null);

        JBPopup componentPopUp = gutterMethodComponentPopup
                .setProject(methodCallExpression.getProject())
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setCancelButton(new IconButton("Close", AllIcons.Actions.CloseDarkGrey))
                .setCancelKeyEnabled(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setResizable(true)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setBelongsToGlobalPopupStack(true)
                .setTitle("Unlogged Mocks")
                .setTitleIcon(new ActiveIcon(UIUtils.GHOST_MOCK))
                .createPopup();
        componentPopUp.showCenteredInCurrentWindow(methodCallExpression.getProject());
        gutterMethodPanel.setPopupHandle(componentPopUp);
        ApplicationManager.getApplication().invokeLater(() -> {
            Dimension size = componentPopUp.getSize();
            componentPopUp.setSize(new Dimension((int) size.getWidth(), (int) Math.min(650, size.getHeight())));
        });

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        methodItemPanel.setBackground(new JBColor(
                new Color(113, 119, 236),
                new Color(113, 119, 236)
        ));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        methodItemPanel.setBackground(originalBackgroundColor);
    }
}
