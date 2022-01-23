package factory;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import ui.Credentials;
import ui.HorBugTable;

import java.io.IOException;

public class DebuggerFactory implements ToolWindowFactory {
    Project currentProject;
    Callback callback;
    OkHttpClient client;
    String projectname;
    Credentials credentials;
    ContentFactory contentFactory;
    HorBugTable bugsTable;
    Content credentialContent, bugsContent;
    ToolWindow toolWindow;


    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        this.currentProject = project;
        this.toolWindow = toolWindow;

        PropertiesComponent.getInstance().setValue(Constants.TOKEN, "");

        contentFactory = ContentFactory.SERVICE.getInstance();

        credentials = new Credentials(this.currentProject);
        credentialContent = contentFactory.createContent(credentials.getContent(), "Credentials", false);
        toolWindow.getContentManager().addContent(credentialContent);

        bugsTable = new HorBugTable(toolWindow);

        bugsContent = contentFactory.createContent(bugsTable.getContent(), "BugsTable", false);
        toolWindow.getContentManager().addContent(bugsContent);

        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN, "");

        if (token == "") {
            bugsTable.hideAll();
            bugsTable.setVariable("");
            bugsTable.setVarValue("Set your credentials first!");
        }
        else {
            try {
                bugsTable.setTableValues();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

}

