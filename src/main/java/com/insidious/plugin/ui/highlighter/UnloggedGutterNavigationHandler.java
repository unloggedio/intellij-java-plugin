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
        insidiousService.methodFocussedHandler(new JavaMethodAdapter(method));
        UsageInsightTracker.getInstance().RecordEvent("TestIconClick", null);


        @NotNull List<LineMarkerInfo<?>> lineMarkerInfoList = new LinkedList<>();
        lineMarkerInfoList.add(new LineHighlighter().getLineMarkerInfo(identifier));

        if (!this.state.equals(GutterState.DIFF) &&
                !this.state.equals(GutterState.NO_DIFF)) {
            insidiousService.updateScaffoldForState(this.state);
        }
//        boolean execute = false;
//        if (this.state.equals(GutterState.EXECUTE)) {
//            execute = true;
//        }
        insidiousService.executeWithAgentForMethod(method);


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
