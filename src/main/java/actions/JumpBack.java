package actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.XDebuggerActionBase;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import factory.InsidiousService;
import org.jetbrains.annotations.NotNull;

public class JumpBack extends XDebuggerActionBase implements DumbAware {
    DebuggerActionHandler actionHandler = new DebuggerActionHandler() {
        @Override
        public void perform(@NotNull Project project, AnActionEvent event) {
            project.getService(InsidiousService.class).getDebugProcess().stepBack(null, DebuggerUIUtil.getSession(event).getSuspendContext());
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
