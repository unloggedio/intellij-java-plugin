package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import java.awt.event.MouseEvent;

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

        UsageInsightTracker.getInstance().RecordEvent("ICON_CLICK_" + this.state, null);


//        if (!this.state.equals(GutterState.DIFF) &&
//                !this.state.equals(GutterState.NO_DIFF)) {
//            insidiousService.updateScaffoldForState(this.state, methodAdapter);
//        }

//        insidiousService.loadSingleWindowForState(state);
//        if (this.state == GutterState.EXECUTE) {
//            insidiousService.compileAndExecuteWithAgentForMethod(methodAdapter);
//        } else {
        insidiousService.methodFocussedHandler(methodAdapter);
        insidiousService.showRouterForMethod(methodAdapter);
//            insidiousService.focusAtomicTestsWindow();
//        }
    }
}
