package extension;

import com.intellij.execution.Executor;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.UIBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class InsidiousExecutor extends Executor {

    private final @NotNull Icon icon = IconLoader.getIcon("/icons/videobug.svg", InsidiousRunConfigTypeInterface.class);

    @Override
    public @NotNull String getToolWindowId() {
        return ToolWindowId.DEBUG;
    }

    @Override
    public @NotNull Icon getToolWindowIcon() {
        return icon;
    }

    @Override
    public @NotNull Icon getIcon() {
        return icon;
    }

    @Override
    public Icon getDisabledIcon() {
        return null;
    }

    @Override
    public @NlsActions.ActionDescription String getDescription() {
        return "Time travel debugger";
    }

    @Override
    public @NotNull @NlsActions.ActionText String getActionName() {
        return "InsidiousTimeTravel";
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "InsidiousTimeTravel";
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getStartActionText() {
        return "Start time travel";
    }

    @Override
    public @NonNls String getContextActionId() {
        return "InsidiousTimeTravel";
    }

    @Override
    public @NonNls String getHelpId() {
        return null;
    }


}
