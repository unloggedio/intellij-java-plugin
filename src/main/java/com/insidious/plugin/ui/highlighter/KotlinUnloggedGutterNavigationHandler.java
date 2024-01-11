package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.kotlin.KotlinMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.awt.event.MouseEvent;

public class KotlinUnloggedGutterNavigationHandler implements GutterIconNavigationHandler<LeafPsiElement> {

    @Override
    public void navigate(MouseEvent e, LeafPsiElement identifier) {
        return;
        KtNamedFunction method = (KtNamedFunction) identifier.getParent();
        KtClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(method.getContainingFile(),
                method.getTextOffset(), KtClass.class, false);
        InsidiousService insidiousService = psiClass.getProject().getService(InsidiousService.class);
        insidiousService.openToolWindow();
        insidiousService.methodFocussedHandler(new KotlinMethodAdapter(method));
        UsageInsightTracker.getInstance().RecordEvent("TestIconClick", null);


//         List<LineMarkerInfo<?>> lineMarkerInfoList = new LinkedList<>();
//        lineMarkerInfoList.add(new LineHighlighter().getLineMarkerInfo(identifier));

//        insidiousService.executeWithAgentForMethod(method);


//        MethodExecutorComponent gutterMethodPanel = new MethodExecutorComponent((PsiMethod) identifier.getParent());
//        JComponent gutterMethodComponent = gutterMethodPanel.getContent();
//         ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
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
