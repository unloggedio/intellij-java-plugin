package com.insidious.plugin.factory;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import org.json.JSONObject;

public class DebuggerFactory implements ToolWindowFactory, DumbAware {
    private static final Logger logger = LoggerUtil.getInstance(DebuggerFactory.class);
    Project currentProject;

    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent( Project project,  ToolWindow toolWindow) {
        try {
            logger.info("Start unlogged tool window");
            this.currentProject = project;
            InsidiousService insidiousService = project.getService(InsidiousService.class);
            insidiousService.init(project, toolWindow);
        } catch (Exception e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("error", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("ToolWindowInitFail", eventProperties);
            logger.info("exception in create tool window", e);
        }
    }

}

