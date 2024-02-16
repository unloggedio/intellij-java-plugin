package com.insidious.plugin.actions;

import com.insidious.plugin.autoexecutor.AutoExecutorRunOptions;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class EnableAutoExecutor extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AutoExecutorRunOptions options = new AutoExecutorRunOptions(true);
//        e.getProject().getService(InsidiousService.class).autoExecutorEnableTrigger();
    }
}
