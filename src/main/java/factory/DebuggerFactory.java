package factory;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import ui.Credentials;
import ui.HorBugTable;

import java.awt.*;

public class DebuggerFactory implements ToolWindowFactory, DumbAware {
    Project currentProject;
    Credentials credentials;
    ContentFactory contentFactory;
    HorBugTable bugsTable;
    Content credentialContent, bugsContent;
    ToolWindow toolWindow;
    FileEditorManager editorManager;
    Editor editor;
    TextAttributes textattributes;
    Color backgroundColor = new Color(240, 57, 45, 80);


    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        PropertiesComponent.getInstance().setValue(Constants.TOKEN, "");
        PropertiesComponent.getInstance().setValue(Constants.PROJECT_ID, "");
        PropertiesComponent.getInstance().setValue(Constants.BASE_URL, "");

        this.currentProject = project;
        this.toolWindow = toolWindow;

        textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
        editorManager = FileEditorManager.getInstance(project);
        editor = editorManager.getSelectedTextEditor();

        contentFactory = ContentFactory.SERVICE.getInstance();

        credentials = new Credentials(this.currentProject, this.toolWindow);
        credentialContent = contentFactory.createContent(credentials.getContent(), "Credentials", false);
        toolWindow.getContentManager().addContent(credentialContent);

        bugsTable = new HorBugTable(currentProject, this.toolWindow);

        bugsContent = contentFactory.createContent(bugsTable.getContent(), "Crashes", false);

        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN, "");

        if (token.equals("")) {
//            toolWindow.getContentManager().removeContent(bugsContent, true);
        } else {
            toolWindow.getContentManager().addContent(bugsContent);
            try {
                bugsTable.setTableValues();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        ProjectService projectService = project.getService(ProjectService.class);
        projectService.setHorBugTable(bugsTable);

    }


}

