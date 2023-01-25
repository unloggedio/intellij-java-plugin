package com.insidious.plugin.actions;

import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.XDebuggerActionBase;
import org.jetbrains.annotations.NotNull;

public class JumpBack extends XDebuggerActionBase implements DumbAware {
    DebuggerActionHandler actionHandler = new DebuggerActionHandler() {
        @Override
        public void perform(@NotNull Project project, AnActionEvent event) {
            InsidiousService service = ServiceManager.getService(InsidiousService.class);
            InsidiousJavaDebugProcess debugProcess = service.getDebugProcess();
            debugProcess.stepBack(null, debugProcess.getSession().getSuspendContext());
        }

        @Override
        public boolean isEnabled(@NotNull Project project, AnActionEvent event) {
            InsidiousJavaDebugProcess debugProcess = ServiceManager.getService(InsidiousService.class)
                    .getDebugProcess();
            if (debugProcess == null) {
                return false;
            }
            XSuspendContext suspendContext = debugProcess.getSession().getSuspendContext();
            return suspendContext != null;
        }
    };


    @Override
    protected @NotNull DebuggerActionHandler getHandler(@NotNull DebuggerSupport debuggerSupport) {
        return actionHandler;
    }
}
