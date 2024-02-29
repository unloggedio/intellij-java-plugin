package com.insidious.plugin.ui;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.adapter.kotlin.KotlinMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InsidiousCaretListener implements EditorMouseListener, CaretListener, DocumentListener {
    final static private Logger logger = LoggerUtil.getInstance(InsidiousCaretListener.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");

    public InsidiousCaretListener() {
    }


    @Override
    public void mouseReleased(EditorMouseEvent event) {
        Project project = event.getEditor().getProject();
        if (project == null) {
            // non project based mouse event
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

            Matcher fileMatcher = testFileNamePattern.matcher(file.getName());
            if (fileMatcher.matches()) {
                return;
            }

            PsiMethod method = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiMethod.class, false);
            if (method != null) {
                MethodAdapter methodAdapter = new JavaMethodAdapter(method);
                insidiousService.methodFocussedHandler(methodAdapter);
//                return;
            }
//            KtNamedFunction kotlinMethod = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtNamedFunction.class,
//                    false);
//            if (kotlinMethod != null) {
//                insidiousService.methodFocussedHandler(new KotlinMethodAdapter(kotlinMethod));
//            }
        } catch (Exception ex) {
            logger.error("Exception in caret listener (" + ex.getMessage() + "): ", ex);
        }

    }

    @Override
    public void caretPositionChanged(@NotNull CaretEvent event) {
        CaretListener.super.caretPositionChanged(event);

        Project project = event.getEditor().getProject();
        if (project == null) {
            // non project based mouse event
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

            Matcher fileMatcher = testFileNamePattern.matcher(file.getName());
            if (fileMatcher.matches()) {
                return;
            }

            PsiMethod method = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiMethod.class, false);
            if (method != null) {
                PsiClass containingClass = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiClass.class, false);
                MethodAdapter methodAdapter = new JavaMethodAdapter(method) {
                    @Override
                    public ClassAdapter getContainingClass() {
                        return ApplicationManager.getApplication().runReadAction(
                                (Computable<JavaClassAdapter>) () -> new JavaClassAdapter(containingClass));
                    }
                };

                insidiousService.methodFocussedHandler(methodAdapter);
//                return;
            }
//            KtNamedFunction kotlinMethod = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtNamedFunction.class,
//                    false);
//            if (kotlinMethod != null) {
//                insidiousService.methodFocussedHandler(new KotlinMethodAdapter(kotlinMethod));
//            }
        } catch (Exception ex) {
            logger.error("Exception in caret listener (" + ex.getMessage() + "): ", ex);
        }


    }

}
