package com.insidious.plugin.ui;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.extension.model.ReplayData;
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
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class InsidiousUtils {
    public static void
    focusProbeLocationInEditor(
            DataInfo probeInfo,
            String className,
            InsidiousService service
    ) {
        if (className.contains("$")) {
            className = className.substring(0, className.indexOf('$'));
        }
        String fileLocation = "src/main/java/" + className + ".java";


        @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                .refreshAndFindFileByUrl(
                        Path.of(service.getProject().getBasePath(), fileLocation).toUri().toString());

        if (newFile == null) {
            return;
        }

        FileEditor[] fileEditor = FileEditorManager.getInstance(service.getProject()).openFile(newFile,
                true, true);


        Editor editor =
                DataManager.getInstance()
                        .getDataContext(fileEditor[0].getComponent())
                        .getData(CommonDataKeys.EDITOR);


        @Nullable Document newDocument = FileDocumentManager.getInstance().getDocument(newFile);
        if (probeInfo.getLine() > 0) {
            int lineOffsetStart = newDocument.getLineStartOffset(probeInfo.getLine() - 1);
            editor.getCaretModel().getCurrentCaret().moveToOffset(lineOffsetStart);
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        }
    }
}
