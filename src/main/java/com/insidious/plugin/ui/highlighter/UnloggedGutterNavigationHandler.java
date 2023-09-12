package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.ToggleActionButton;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.OnOffButton;
import com.intellij.ui.components.SelectionAwareListCellRenderer;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UnloggedGutterNavigationHandler implements GutterIconNavigationHandler<PsiIdentifier> {

    private static final Logger logger = LoggerUtil.getInstance(UnloggedGutterNavigationHandler.class);
    private final GutterState state;

    public UnloggedGutterNavigationHandler(GutterState state) {
        this.state = state;
    }

    @Override
    public void navigate(MouseEvent mouseEvent, PsiIdentifier identifier) {
        PsiMethod method = (PsiMethod) identifier.getParent();
        PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(method.getContainingFile(),
                method.getTextOffset(), PsiClass.class, false);
        InsidiousService insidiousService = psiClass.getProject().getService(InsidiousService.class);
        insidiousService.openToolWindow();
        JavaMethodAdapter methodAdapter = new JavaMethodAdapter(method);
        insidiousService.methodFocussedHandler(methodAdapter);

        UsageInsightTracker.getInstance().RecordEvent("ICON_CLICK_" + this.state, null);


//        if (!this.state.equals(GutterState.DIFF) &&
//                !this.state.equals(GutterState.NO_DIFF)) {
//            insidiousService.updateScaffoldForState(this.state, methodAdapter);
//        }

//        insidiousService.loadSingleWindowForState(state);
        if (this.state == GutterState.EXECUTE) {
            insidiousService.compileAndExecuteWithAgentForMethod(methodAdapter);
        } else {
            insidiousService.methodFocussedHandler(methodAdapter);
//            insidiousService.focusAtomicTestsWindow();
        }
    }
}
