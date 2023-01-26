package com.insidious.plugin.actions;

import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

public class JumpForward extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ApplicationManager.getApplication().getService(InsidiousService.class).getDebugProcess().startStepOver(null);

    }


}
