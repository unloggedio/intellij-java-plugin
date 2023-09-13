package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.ui.mocking.MockDefinitionListPanel;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;

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

        MockDefinitionListPanel gutterMethodPanel =
                new MockDefinitionListPanel(methodCallExpression);

        JComponent gutterMethodComponent = gutterMethodPanel.getComponent();
        gutterMethodComponent.setPreferredSize(new Dimension(600, 400));

        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(gutterMethodComponent, null);

        gutterMethodComponentPopup
                .setProject(methodCallExpression.getProject())
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setMinSize(new Dimension(600, 400))
                .setRequestFocus(true)
                .setResizable(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelKeyEnabled(true)
//                .setCancelButton(gutterMethodPanel.getCloseButton())
                .setBelongsToGlobalPopupStack(false)
                .setTitle("Manage Mocks")
                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
                .createPopup()
                .show(new RelativePoint(mouseEvent));


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
