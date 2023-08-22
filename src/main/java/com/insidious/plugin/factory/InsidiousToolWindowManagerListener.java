package com.insidious.plugin.factory;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;

public class InsidiousToolWindowManagerListener implements ToolWindowManagerListener {


    private final InsidiousService insidiousService;
    private boolean isToolWindowVisible = false;

    public InsidiousToolWindowManagerListener(com.intellij.openapi.project.Project project) {
        insidiousService = project.getService(InsidiousService.class);
    }

    @Override
    public synchronized void stateChanged() {
        ToolWindow toolWindow = insidiousService.getToolWindow();
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
