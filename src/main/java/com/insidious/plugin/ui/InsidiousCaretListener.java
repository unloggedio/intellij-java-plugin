package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class InsidiousCaretListener implements EditorMouseListener {
    private final Project project;

    public InsidiousCaretListener(Project project) {
        this.project = project;
    }


    @Override
    public void mouseReleased(@NotNull EditorMouseEvent event) {
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
        PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiClass.class, false);


        insidiousService.showTestCreatorInterface(psiClass, method);
    }

}
