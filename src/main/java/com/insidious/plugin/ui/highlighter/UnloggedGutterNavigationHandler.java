package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

public class UnloggedGutterNavigationHandler implements GutterIconNavigationHandler<PsiIdentifier> {

    private static final Logger logger = LoggerUtil.getInstance(UnloggedGutterNavigationHandler.class);
    private final GutterState state;
    public UnloggedGutterNavigationHandler(GutterState state) {
        this.state = state;
    }

    @Override
    public void navigate(MouseEvent e, PsiIdentifier identifier) {
        PsiMethod method = (PsiMethod) identifier.getParent();
        PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(method.getContainingFile(),
                method.getTextOffset(), PsiClass.class, false);
        InsidiousService insidiousService = psiClass.getProject().getService(InsidiousService.class);
        insidiousService.openTestCaseDesigner(psiClass.getProject());
        JavaMethodAdapter methodAdapter = new JavaMethodAdapter(method);
        insidiousService.methodFocussedHandler(methodAdapter);
        recordAnalyticEvent();

        @NotNull List<LineMarkerInfo<?>> lineMarkerInfoList = new LinkedList<>();
        lineMarkerInfoList.add(new LineHighlighter().getLineMarkerInfo(identifier));

//        if (!this.state.equals(GutterState.DIFF) &&
//                !this.state.equals(GutterState.NO_DIFF)) {
//            insidiousService.updateScaffoldForState(this.state, methodAdapter);
//        }

        if (this.state == GutterState.EXECUTE) {
            insidiousService.compileAndExecuteWithAgentForMethod(methodAdapter);
        } else {
            insidiousService.focusAtomicTestsWindow();
        }

    }

    private void recordAnalyticEvent()
    {
        String event = "GutterIconClicked";
        switch (this.state)
        {
            case NO_AGENT:
                event="IconClickNoAgent";
                break;
            case PROCESS_NOT_RUNNING:
                event="IconClickProcessNotRunning";
                break;
            case PROCESS_RUNNING:
                event="IconClickProcessRunning";
                break;
            case DATA_AVAILABLE:
                event="IconClickDataAvailable";
                break;
            case EXECUTE:
                event="IconClickReload";
                break;
            case DIFF:
                event="IconClickDiff";
                break;
            case NO_DIFF:
                event="IconClickNoDiff";
                break;
        }
        UsageInsightTracker.getInstance().RecordEvent(event, null);
    }
}
