package extension;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;

public class InsidiousRunConfigType extends SimpleConfigurationType implements InsidiousRunConfigTypeInterface {

    private static final String _ID = "InsidiousBridge";
    private static final String _NAME = "Insidious (Java)";

    protected InsidiousRunConfigType() {
        super(_ID, _NAME, null, NotNullLazyValue.createValue(() -> ICON));
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new InsidiousRunConfiguration(_NAME, project, this);
    }
}
