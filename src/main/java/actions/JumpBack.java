package actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;

public class JumpBack extends AnAction {
    int lineNum;
    Color backgroundColor = new Color(240, 57, 45, 80);
    Project project;
    FileEditorManager editorManager;
    Editor editor;
    TextAttributes textattributes;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        jumpBackToLine();
    }

    private void jumpBackToLine() {
        String basePath = project.getBasePath();
        String fileName = PropertiesComponent.getInstance().getValue("lineNum");
        if (fileName == null) {
            System.out.print("No Line Number Found");
            return;
        }

        VirtualFile file = LocalFileSystem
                .getInstance().findFileByIoFile(new File(basePath + fileName));

        FileEditorManager.getInstance(project).openFile(file, true);

        lineNum = PropertiesComponent.getInstance().getInt("lineNum", 0);

        if (lineNum ==0) {
            return;
        }
        highlight();
    }

    private void highlight() {
        editor.getMarkupModel().removeAllHighlighters();
        editor.getMarkupModel().addLineHighlighter(lineNum, HighlighterLayer.CARET_ROW, textattributes);
    }
}
