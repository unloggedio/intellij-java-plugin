package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ArrayUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

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


//        IPopupChooserBuilder<LineMarkerInfo<?>> builder = JBPopupFactory.getInstance()
//                .createPopupChooserBuilder(lineMarkerInfoList);

        RelativePoint relativePoint = new RelativePoint(mouseEvent);
        Point point = relativePoint.getPoint();
        point.setLocation(point.getX(), point.getY() + 40);

        JPanel gutterMethodPanel = new JPanel();
        gutterMethodPanel.setLayout(new GridLayout(0, 1));
        gutterMethodPanel.setMinimumSize(new Dimension(400, 300));

        for (PsiMethodCallExpression methodCallExpression : methodCallExpressions) {
            if (MockMethodLineHighlighter.isNonStaticDependencyCall(methodCallExpression)) {
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
        }


        JComponent gutterMethodComponent = gutterMethodPanel;
        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(gutterMethodComponent, null);

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
                .setTitle("Mock Methods")
                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
                .createPopup()
                .show(new RelativePoint(mouseEvent));

//        builder.setRenderer(new SelectionAwareListCellRenderer<>(dom -> {
//                    Icon icon = null;
//                    GutterIconRenderer renderer = dom.createGutterRenderer();
//                    if (renderer != null) {
//                        Icon originalIcon = renderer.getIcon();
//                        icon = IconUtil.scale(originalIcon, null, JBUIScale.scale(16.0f) / originalIcon.getIconWidth());
//                    }
//                    PsiElement element = dom.getElement();
//                    String elementPresentation;
//                    if (element == null) {
//                        elementPresentation = IdeBundle.message("node.structureview.invalid");
//                    } else if (dom instanceof MergeableLineMarkerInfo) {
//                        elementPresentation = ((MergeableLineMarkerInfo<?>) dom).getElementPresentation(element);
//                    } else {
//                        elementPresentation = element.getText();
//                    }
//                    String text = StringUtil.first(elementPresentation, 100, true).replace('\n', ' ');
//
//                    JBLabel label = new JBLabel(text, icon, SwingConstants.LEFT);
//                    label.setBorder(JBUI.Borders.empty(2));
//                    JPanel panel = new JPanel();
//                    panel.setLayout(new BorderLayout());
//                    panel.add(label, BorderLayout.CENTER);
//                    OnOffButton toggleActionButton = new OnOffButton();
//                    toggleActionButton.setOnText("Mocking enabled");
//                    toggleActionButton.setOffText("Mocking disabled");
//                    toggleActionButton.addActionListener(e -> logger.warn("Enable/Disable toggled"));
//                    panel.add(toggleActionButton, BorderLayout.EAST);
//                    Border existingBorder = panel.getBorder();
//                    existingBorder = BorderFactory.createCompoundBorder(existingBorder,
//                            BorderFactory.createEmptyBorder(5, 5, 5, 5));
//                    panel.setBorder(existingBorder);
//                    return panel;
//                })).setItemChosenCallback(value -> {
//                    logger.warn("gutter menu item chosen: " + value.getElement());
//                    //noinspection unchecked
//                    GutterIconNavigationHandler<PsiElement> handler = (GutterIconNavigationHandler<PsiElement>) value.getNavigationHandler();
////                    if (handler != null) {
////                        handler.navigate(mouseEvent, value.getElement());
////                    }
//
//                    MethodExecutorComponent gutterMethodPanel = new MethodExecutorComponent(insidiousService);
//                    gutterMethodPanel.refreshAndReloadCandidates(new JavaMethodAdapter(method), new ArrayList<>());
//                    gutterMethodPanel.refreshSearchAndLoad();
//                    JComponent gutterMethodComponent = gutterMethodPanel.getContent();
//                    ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
//                            .createComponentPopupBuilder(gutterMethodComponent, null);
//
//                    gutterMethodComponentPopup
//                            .setProject(identifier.getProject())
//                            .setShowBorder(true)
//                            .setShowShadow(true)
//                            .setFocusable(true)
//                            .setRequestFocus(true)
//                            .setCancelOnClickOutside(true)
//                            .setCancelOnOtherWindowOpen(true)
//                            .setCancelKeyEnabled(true)
//                            .setBelongsToGlobalPopupStack(false)
//                            .setTitle("Execute " + method.getName())
//                            .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
//                            .createPopup()
//                            .show(new RelativePoint(mouseEvent));
//
//                })
//                .setCancelOnClickOutside(true)
//                .setCancelKeyEnabled(true)
//                .createPopup()
//                .show(relativePoint);

    }

}
