package com.insidious.plugin.actions;

import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;


public class ToggleReporting extends AnAction {
    @Override
    public void actionPerformed( AnActionEvent e) {
        e.getProject().getService(InsidiousService.class).toggleReportGeneration();
    }
}
