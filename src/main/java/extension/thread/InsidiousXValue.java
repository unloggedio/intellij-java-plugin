package extension.thread;

import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import org.jetbrains.annotations.NotNull;

public class InsidiousXValue extends XValue {
    private final InsidiousLocalVariable variable;

    public InsidiousXValue(InsidiousLocalVariable variable) {
        this.variable = variable;
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        node.setPresentation(null, variable.typeName(), String.valueOf(variable.getValue()), false);
    }

}
