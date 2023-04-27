package com.insidious.plugin.ui;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.adapter.kotlin.KotlinMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedFunction;

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
                insidiousService.methodFocussedHandler(new JavaMethodAdapter(method));
                return;
            }
            KtNamedFunction kotlinMethod = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtNamedFunction.class,
                    false);
            if (kotlinMethod != null) {
                insidiousService.methodFocussedHandler(new KotlinMethodAdapter(kotlinMethod));
            }
        } catch (AlreadyDisposedException e) {
            //case where insidious service may not be ready/null.
            logger.error("Exception in caret listener (AlreadyDisposed): ", e);
//            e.printStackTrace();
//            InsidiousNotification.notifyMessage("Please try again when Unlogged is ready",
//                    NotificationType.ERROR);
        } catch (Exception ex) {
            //other exception
            logger.error("Exception in caret listener (" + ex.getMessage() + "): ", ex);
//            System.out.println("Exception : " + ex);
//            ex.printStackTrace();
        }
//        PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiClass.class, false);


    }

}
