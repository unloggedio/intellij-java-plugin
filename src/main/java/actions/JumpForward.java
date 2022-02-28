package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import factory.InsidiousService;
import org.jetbrains.annotations.NotNull;

public class JumpForward extends AnAction {


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        e.getProject().getService(InsidiousService.class).getDebugProcess().startStepOver(null);

    }


}
