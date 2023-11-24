package com.insidious.plugin.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;

public class InsidiousToolWindowManagerListener implements ToolWindowManagerListener {


    private final InsidiousService insidiousService;
    private final Project project;
    private boolean isToolWindowVisible = false;

    public InsidiousToolWindowManagerListener(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        this.project = project;
    }

    @Override
    public synchronized void stateChanged() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        boolean newToolWindowVisibleState = toolWindow != null && toolWindow.isVisible();
        if (isToolWindowVisible && !newToolWindowVisibleState) {
            isToolWindowVisible = false;
            insidiousService.highlightLines(insidiousService.getCurrentHighlightRequest());
        } else if (!isToolWindowVisible && newToolWindowVisibleState) {
            isToolWindowVisible = true;
            insidiousService.highlightLines(insidiousService.getCurrentHighlightRequest());
        }
    }


}
