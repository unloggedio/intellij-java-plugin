package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.ui.adapter.kotlin.KotlinMethodAdapter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedFunction;

public class InsidiousCaretListener implements EditorMouseListener {
    private final Project project;

    public InsidiousCaretListener(Project project) {
        this.project = project;
    }


    @Override
    public void mouseReleased(@NotNull EditorMouseEvent event) {
        if (project == null) {
            return;
        }
        EditorMouseListener.super.mousePressed(event);
        if (event.getArea() == null || !event.getArea().equals(EditorMouseEventArea.EDITING_AREA)) {
            return;
        }

        InsidiousService insidiousService = project.getService(InsidiousService.class);

        Editor editor = event.getEditor();
        int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null) {
            return;
        }
        PsiMethod method = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiMethod.class, false);
        if (method != null) {
            insidiousService.methodFocussedHandler(new JavaMethodAdapter(method));
            return;
        }
        KtNamedFunction kotlinMethod = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtNamedFunction.class,
                false);
        if (kotlinMethod != null) {
            insidiousService.methodFocussedHandler(new KotlinMethodAdapter(kotlinMethod));
            return;
        }
//        PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiClass.class, false);


    }

}
