package factory;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import ui.Credentials;
import ui.HorBugTable;

public class DebuggerFactory implements ToolWindowFactory, DumbAware {
    Project currentProject;
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

        contentFactory = ContentFactory.SERVICE.getInstance();

        ProjectService projectService = currentProject.getService(ProjectService.class);

        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN, "");

        if (token.equals("")) {
            credentials = new Credentials(this.currentProject, this.toolWindow);
            credentialContent = contentFactory.createContent(credentials.getContent(), "Credentials", false);
            toolWindow.getContentManager().addContent(credentialContent);
        } else {
            bugsTable = new HorBugTable(currentProject, this.toolWindow);
            bugsContent = contentFactory.createContent(bugsTable.getContent(), "Exceptions", false);
            toolWindow.getContentManager().addContent(bugsContent);
            projectService.setHorBugTable(bugsTable);
            try {
                bugsTable.setTableValues();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //filters = new Filters(project);

    //debugpointContent = contentFactory.createContent(filters.getContent(), "Debug Points", false);

    //toolWindow.getContentManager().addContent(debugpointContent);

}

