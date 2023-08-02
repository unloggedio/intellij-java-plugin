package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class InsidiousUtils {
    public static FileEditor
    focusProbeLocationInEditor(
            int lineNumber,
            String className,
            InsidiousService service
    ) {
        if (className.contains("$")) {
            className = className.substring(0, className.indexOf('$'));
        }
        if (className.contains(".")) {
            className = className.replaceAll("\\.", "/");
        }
        String fileName = className + ".java";
        String fileLocation = "src/main/java/" + fileName;


        @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                .refreshAndFindFileByUrl(Path.of(service.getProject().getBasePath(), fileLocation).toUri().toString());

        if (newFile == null) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            @NotNull PsiFile @NotNull [] searchResult = FilenameIndex.getFilesByName(
                    service.getProject(), fileName, GlobalSearchScope.projectScope(service.getProject()));
            if (searchResult.length == 0) {
                return null;
            }
            newFile = searchResult[0].getVirtualFile();
        }

        FileEditor[] fileEditor = FileEditorManager.getInstance(service.getProject()).openFile(newFile,
                true, true);


        FileEditor fileEditor1 = fileEditor[0];
        Editor editor =
                DataManager.getInstance()
                        .getDataContext(fileEditor1.getComponent())
                        .getData(CommonDataKeys.EDITOR);


        @Nullable Document newDocument = FileDocumentManager.getInstance().getDocument(newFile);
        if (lineNumber > 0) {
            int lineOffsetStart = newDocument.getLineStartOffset(lineNumber - 1);
            editor.getCaretModel().getCurrentCaret().moveToOffset(lineOffsetStart);
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
        return fileEditor1;
    }
}
