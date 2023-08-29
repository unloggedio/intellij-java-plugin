package com.insidious.plugin.actions;

import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;


public class ShowRawView extends AnAction {
    @Override
    public void actionPerformed( AnActionEvent e) {
        e.getProject().getService(InsidiousService.class).attachRawView();
    }
}
