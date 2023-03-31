package com.insidious.plugin.ui.Highlighter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.UIUtils;
import com.insidious.plugin.ui.gutter.MethodExecutorComponent;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

public class UnloggedGutterNavigationHandler implements GutterIconNavigationHandler<PsiIdentifier> {

    @Override
    public void navigate(MouseEvent e, PsiIdentifier identifier) {
//        if (identifier.getParent() instanceof PsiMethod) {
        PsiMethod method = (PsiMethod) identifier.getParent();
            PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(method.getContainingFile(),
                    method.getTextOffset(), PsiClass.class, false);
            InsidiousService insidiousService = psiClass.getProject().getService(InsidiousService.class);
            insidiousService.openTestCaseDesigner(psiClass.getProject());
            insidiousService.methodFocussedHandler(method);
            UsageInsightTracker.getInstance().RecordEvent("TestIconClick", null);
//        }

        @NotNull List<LineMarkerInfo<?>> lineMarkerInfoList = new LinkedList<>();
        lineMarkerInfoList.add(new LineHighlighter().getLineMarkerInfo(identifier));


//        MethodExecutorComponent gutterMethodPanel = new MethodExecutorComponent((PsiMethod) identifier.getParent());
//        JComponent gutterMethodComponent = gutterMethodPanel.getContent();
//        @NotNull ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(gutterMethodComponent, null);
//
//        gutterMethodComponentPopup
//                .setProject(identifier.getProject())
//                .setShowBorder(true)
//                .setShowShadow(true)
//                .setFocusable(true)
//                .setRequestFocus(true)
//                .setCancelOnClickOutside(true)
//                .setBelongsToGlobalPopupStack(false)
//                .setTitle("Execute " + method.getName())
//                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
//                .createPopup()
//                .show(new RelativePoint(e));

//        IPopupChooserBuilder<LineMarkerInfo<?>> builder = JBPopupFactory.getInstance()
//                .createPopupChooserBuilder(lineMarkerInfoList);
//        builder.setRenderer(new SelectionAwareListCellRenderer<>(dom -> {
//            Icon icon = null;
//            GutterIconRenderer renderer = dom.createGutterRenderer();
//            if (renderer != null) {
//                Icon originalIcon = renderer.getIcon();
//                icon = IconUtil.scale(originalIcon, null, JBUIScale.scale(16.0f) / originalIcon.getIconWidth());
//            }
//            PsiElement element = dom.getElement();
//            String elementPresentation;
//            if (element == null) {
//                elementPresentation = IdeBundle.message("node.structureview.invalid");
//            } else if (dom instanceof MergeableLineMarkerInfo) {
//                elementPresentation = ((MergeableLineMarkerInfo<?>) dom).getElementPresentation(element);
//            } else {
//                elementPresentation = element.getText();
//            }
//            String text = StringUtil.first(elementPresentation, 100, true).replace('\n', ' ');
//
//            JBLabel label = new JBLabel(text, icon, SwingConstants.LEFT);
//            label.setBorder(JBUI.Borders.empty(2));
//            return label;
//        }));
//        builder.setItemChosenCallback(value -> {
//            //noinspection unchecked
//            GutterIconNavigationHandler<PsiElement> handler = (GutterIconNavigationHandler<PsiElement>) value.getNavigationHandler();
//            if (handler != null) {
//                handler.navigate(e, value.getElement());
//            }
//        }).createPopup().show(new RelativePoint(e));
    }
}
