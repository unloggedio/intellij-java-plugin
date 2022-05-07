package com.insidious.plugin.actions;

import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.XDebuggerActionBase;
import org.jetbrains.annotations.NotNull;

public class JumpBack extends XDebuggerActionBase implements DumbAware {
    DebuggerActionHandler actionHandler = new DebuggerActionHandler() {
        @Override
        public void perform(@NotNull Project project, AnActionEvent event) {
            InsidiousService service = project.getService(InsidiousService.class);
            InsidiousJavaDebugProcess debugProcess = service.getDebugProcess();
            debugProcess.stepBack(null,
                            debugProcess.getSession().getSuspendContext());
        }

        @Override
        public boolean isEnabled(@NotNull Project project, AnActionEvent event) {
            return true;
        }
    };

    @Override
    protected @NotNull DebuggerActionHandler getHandler(@NotNull DebuggerSupport debuggerSupport) {
        return actionHandler;
    }

}
