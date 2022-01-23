package actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.impl.file.PsiPackageBase;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import ui.HorBugTable;

import java.awt.*;
import java.io.File;
import java.util.UUID;

public class JumpForward extends AnAction {
    int lineNum;
    Project project;
    Color backgroundColor = new Color(240, 57, 45, 80);
    FileEditorManager editorManager;
    Editor editor;
    TextAttributes textattributes;
    String basePath;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
        editorManager = FileEditorManager.getInstance(project);
        editor = editorManager.getSelectedTextEditor();
        //PsiPackage pack = JavaPsiFacade.getInstance(project).findPackage("");
        basePath = project.getBasePath();

        System.out.print( ModuleManager.getInstance(project).getModules()[0].getName());
        //testDropdown();
        //highlight();
        //jumpForwardToLine();
        //getThreadIds();
    }

    private void jumpForwardToLine() {
        String fileName = PropertiesComponent.getInstance().getValue("lineNum");
        if (fileName == null) {
            System.out.print("No Line Number Found\n");
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
        editor.getMarkupModel().addLineHighlighter(lineNum++, HighlighterLayer.CARET_ROW, textattributes);

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("VideoBug");

        HorBugTable horBugTable = new HorBugTable(project, toolWindow);
        horBugTable.setVariable("Changing Content:");
        horBugTable.setVarValue(UUID.randomUUID().toString());

//        DebuggerWindow myToolWindow = new DebuggerWindow(toolWindow);
//        myToolWindow.setVariableType("Changing Content:");
//        myToolWindow.setVariableValue(UUID.randomUUID().toString());
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(horBugTable.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void getThreadIds() {
        //GET call
    }


}
