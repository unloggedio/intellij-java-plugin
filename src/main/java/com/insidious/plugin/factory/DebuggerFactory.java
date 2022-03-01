package com.insidious.plugin.factory;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import com.insidious.plugin.ui.CredentialsToolbar;
import com.insidious.plugin.ui.HorBugTable;
import com.insidious.plugin.ui.LogicBugs;
import org.slf4j.Logger;

public class DebuggerFactory implements ToolWindowFactory, DumbAware {
    private static final Logger logger = LoggerUtil.getInstance(DebuggerFactory.class);
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
        logger.info("Start insidious debugger");
        this.currentProject = project;
        InsidiousService insidiousService = project.getService(InsidiousService.class);
        if (insidiousService.isLoggedIn()) {
            insidiousService.startDebugSession();
        }
        insidiousService.setToolWindow(toolWindow);
    }

}

