package com.insidious.plugin.factory;

import com.insidious.plugin.ui.ConfigurationWindow;
import com.insidious.plugin.ui.SearchByTypeWindow;
import com.insidious.plugin.ui.SearchByValueWindow;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class DebuggerFactory implements ToolWindowFactory, DumbAware {
    private static final Logger logger = LoggerUtil.getInstance(DebuggerFactory.class);
    Project currentProject;
    ConfigurationWindow credentialsToolbar;
    ContentFactory contentFactory;
    SearchByTypeWindow bugsTable;
    SearchByValueWindow logicBugs;
    Content credentialContent, bugsContent, logicbugContent;
    ToolWindow toolWindow;

    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {


            logger.info("Start insidious debugger");
            this.currentProject = project;
            InsidiousService insidiousService = project.getService(InsidiousService.class);
            if (insidiousService.isLoggedIn()) {
                insidiousService.startDebugSession();
            }
            insidiousService.setToolWindow(toolWindow);
        }catch (Exception e) {
            logger.info("exception in create tool window", e);
        }
    }

}

