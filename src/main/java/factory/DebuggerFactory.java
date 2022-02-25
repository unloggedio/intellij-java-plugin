package factory;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import ui.CredentialsToolbar;
import ui.HorBugTable;
import ui.LogicBugs;

public class DebuggerFactory implements ToolWindowFactory, DumbAware {
    Project currentProject;
    CredentialsToolbar credentialsToolbar;
    ContentFactory contentFactory;
    HorBugTable bugsTable;
    LogicBugs logicBugs;
    Content credentialContent, bugsContent, logicbugContent;
    ToolWindow toolWindow;


    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.currentProject = project;
        ProjectService projectService = project.getService(ProjectService.class);
        if (projectService.isLoggedIn()) {
            projectService.startDebugSession();
        }
        projectService.initiateUI(toolWindow);


    }

}

