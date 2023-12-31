package com.insidious.plugin.ui;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;

import java.util.regex.Pattern;

public class InsidiousCaretListener implements EditorMouseListener {
    final static private Logger logger = LoggerUtil.getInstance(InsidiousCaretListener.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");

    public InsidiousCaretListener() {
    }


    @Override
    public void mouseReleased(EditorMouseEvent event) {
//        Project project = event.getEditor().getProject();
//        if (project == null) {
//            // non project based mouse event
//            return;
//        }
//        EditorMouseListener.super.mousePressed(event);
//        if (event.getArea() == null || !event.getArea().equals(EditorMouseEventArea.EDITING_AREA)) {
//            return;
//        }
//
//        if (DumbService.getInstance(project).isDumb()) {
//            return;
//        }
//
//        try {
//            InsidiousService insidiousService = project.getService(InsidiousService.class);
//
//            Editor editor = event.getEditor();
//            int offset = editor.getCaretModel().getOffset();
//            Document document = editor.getDocument();
//            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
//            if (virtualFile instanceof LightVirtualFile) {
//                return;
//            }
//
//            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
//            if (file == null) {
//                return;
//            }
//
//            Matcher fileMatcher = testFileNamePattern.matcher(file.getName());
//            if (fileMatcher.matches()) {
//                return;
//            }
//
//            PsiMethod method = PsiTreeUtil.findElementOfClassAtOffset(file, offset, PsiMethod.class, false);
//            if (method != null) {
//                MethodAdapter methodAdapter = new JavaMethodAdapter(method);
//                insidiousService.methodFocussedHandler(methodAdapter);
//                return;
//            }
//            KtNamedFunction kotlinMethod = PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtNamedFunction.class,
//                    false);
//            if (kotlinMethod != null) {
//                insidiousService.methodFocussedHandler(new KotlinMethodAdapter(kotlinMethod));
//            }
//        } catch (Exception ex) {
//            logger.error("Exception in caret listener (" + ex.getMessage() + "): ", ex);
//        }

    }

}
