package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.mocking.MockDefinitionListPanel;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.*;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MethodMockGutterNavigationHandler implements GutterIconNavigationHandler<PsiIdentifier> {

    private static final Logger logger = LoggerUtil.getInstance(MethodMockGutterNavigationHandler.class);

    public static <T extends PsiElement> T[] getChildrenOfTypeRecursive(PsiElement element, Class<T> aClass) {
        if (element == null) return null;
        List<T> result = getChildrenOfTypeAsListRecursive(element, aClass);
        return result.isEmpty() ? null : ArrayUtil.toObjectArray(result, aClass);
    }

    public static <T extends PsiElement> List<T> getChildrenOfTypeAsListRecursive(PsiElement element, Class<? extends T> aClass) {
        List<T> result = new ArrayList<>();
        if (element != null) {
            for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (aClass.isInstance(child)) {
                    result.add(aClass.cast(child));
                }
                result.addAll(getChildrenOfTypeAsListRecursive(child, aClass));
            }
        }
        return result;
    }

    @Override
    public void navigate(MouseEvent mouseEvent, PsiIdentifier identifier) {


        InsidiousService insidiousService = identifier.getProject().getService(InsidiousService.class);
        final PsiStatement statement = PsiTreeUtil.getParentOfType(identifier, PsiStatement.class, true,
                PsiMethod.class);

        PsiMethodCallExpression[] methodCallExpressions = getChildrenOfTypeRecursive(
                statement, PsiMethodCallExpression.class);
        if (methodCallExpressions == null) {
            return;
        }

//        List<PsiElement> more = Arrays.stream(getChildrenOfTypeRecursive(
//                        statement, PsiMethodReferenceExpressionImpl.class)).map(e -> e.getReference().resolve())
//                .collect(Collectors.toList());


        RelativePoint relativePoint = new RelativePoint(mouseEvent);
        Point point = relativePoint.getPoint();
        point.setLocation(point.getX(), point.getY() + 40);


        List<PsiMethodCallExpression> mockableCallExpressions = Arrays.stream(methodCallExpressions)
                .filter(MockMethodLineHighlighter::isNonStaticDependencyCall)
                .collect(Collectors.toList());

        if (mockableCallExpressions.size() > 1) {
            JPanel gutterMethodPanel = new JPanel();
            gutterMethodPanel.setLayout(new GridLayout(0, 1));
            gutterMethodPanel.setMinimumSize(new Dimension(400, 300));

            for (PsiMethodCallExpression methodCallExpression : mockableCallExpressions) {
                PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
                String methodCallText = methodExpression.getText();
                JPanel methodItemPanel = new JPanel();
                methodItemPanel.setLayout(new BorderLayout());

                methodItemPanel.add(new JLabel(methodCallText), BorderLayout.CENTER);
                JLabel iconLabel = new JLabel(UIUtils.CHECK_GREEN_SMALL);
                Border border = iconLabel.getBorder();
                CompoundBorder borderWithMargin;
                borderWithMargin = BorderFactory.createCompoundBorder(border,
                        BorderFactory.createEmptyBorder(0, 5, 0, 5));
                iconLabel.setBorder(borderWithMargin);
                methodItemPanel.add(iconLabel, BorderLayout.EAST);

                Border currentBorder = methodItemPanel.getBorder();
                borderWithMargin = BorderFactory.createCompoundBorder(currentBorder,
                        BorderFactory.createEmptyBorder(5, 10, 5, 5));
                methodItemPanel.setBorder(borderWithMargin);
                methodItemPanel.addMouseListener(new MockItemClickListener(methodItemPanel, methodCallExpression));

                gutterMethodPanel.add(methodItemPanel);

            }


            ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(gutterMethodPanel, null);

            gutterMethodComponentPopup
                    .setProject(identifier.getProject())
                    .setShowBorder(true)
                    .setShowShadow(true)
                    .setFocusable(true)
                    .setRequestFocus(true)
                    .setCancelOnClickOutside(true)
                    .setCancelOnOtherWindowOpen(true)
                    .setCancelKeyEnabled(true)
                    .setBelongsToGlobalPopupStack(false)
                    .setTitle("Mock Method Calls")
                    .setTitleIcon(new ActiveIcon(UIUtils.GHOST_MOCK))
                    .createPopup()
                    .show(new RelativePoint(mouseEvent));

        } else if (mockableCallExpressions.size() == 1) {

            // there is only a single mockable call on this line

            PsiMethodCallExpression methodCallExpression = mockableCallExpressions.get(0);
            MockDefinitionListPanel gutterMethodPanel = new MockDefinitionListPanel(methodCallExpression);

            JComponent gutterMethodComponent = gutterMethodPanel.getComponent();

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
                    .setBelongsToGlobalPopupStack(false)
                    .setTitle("Unlogged Mocks")
                    .setTitleIcon(new ActiveIcon(UIUtils.GHOST_MOCK))
                    .createPopup();
            componentPopUp.show(new RelativePoint(mouseEvent));
            gutterMethodPanel.setPopupHandle(componentPopUp);


        }

    }

}
