package com.insidious.plugin.ui;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.adapter.kotlin.KotlinMethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.Date;

public class InsidiousCaretListener implements EditorMouseListener {
    final static private Logger logger = LoggerUtil.getInstance(InsidiousCaretListener.class);
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

        if (DumbService.getInstance(project).isDumb()) {
            return;
        }

        try {
            InsidiousService insidiousService = project.getService(InsidiousService.class);

            Editor editor = event.getEditor();
            int offset = editor.getCaretModel().getOffset();
            Document document = editor.getDocument();
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (virtualFile instanceof LightVirtualFile) {
                return;
            }

            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (file == null) {
                return;
            }
            PsiMethod method = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiMethod.class, false);
            if (method != null) {
                // Single Window flow update on non gutter method click
                MethodAdapter methodAdapter = new JavaMethodAdapter(method);
//                long start = new Date().getTime();
//                GutterState state = insidiousService.getGutterStateFor(methodAdapter);
//                long end = new Date().getTime();
//                logger.warn("get gutter state took: " + (end - start) + " ms");
//                insidiousService.loadSingleWindowForState(state);
                insidiousService.methodFocussedHandler(methodAdapter);
                return;
            }
            KtNamedFunction kotlinMethod = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtNamedFunction.class,
                    false);
            if (kotlinMethod != null) {
                insidiousService.methodFocussedHandler(new KotlinMethodAdapter(kotlinMethod));
            }
        } catch (Exception ex) {
            //other exception
            logger.error("Exception in caret listener (" + ex.getMessage() + "): ", ex);
//            System.out.println("Exception : " + ex);
//            ex.printStackTrace();
        }
//        PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiClass.class, false);


    }

}
