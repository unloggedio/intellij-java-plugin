package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.mocking.MockDefinitionListPanel;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiMethod;
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


        methodCallExpression.getProject().getService(InsidiousService.class)
                .showMockCreator(
                        new JavaMethodAdapter((PsiMethod) methodCallExpression.getMethodExpression().resolve()),
                        methodCallExpression
                );

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
