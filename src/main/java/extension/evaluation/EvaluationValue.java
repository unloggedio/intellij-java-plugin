package extension.evaluation;

import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation;
import extension.descriptor.renderer.InsidiousClassRenderer;
import extension.descriptor.renderer.RendererManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EvaluationValue extends XNamedValue {
    private final String myType;
    private final String myValue;

    public EvaluationValue(String name, String type, String value) {
        super(name);
        this.myType = type;
        this.myValue = value;
    }


    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        final InsidiousClassRenderer classRenderer = RendererManager.getInstance().getClassRenderer();

//        final String type = Registry.is("debugger.showTypes") ? classRenderer.renderTypeName(this.myType) : null;
        final String type = classRenderer.renderTypeName(this.myType);
        Icon icon = AllIcons.Debugger.Value;
        if (this.myType != null && this.myType.startsWith("java.lang.String@")) {
            node.setPresentation(icon, new XStringValuePresentation(this.myValue) {

                @Nullable
                public String getType() {
                    return classRenderer.SHOW_STRINGS_TYPE ? type : null;
                }
            }, false);
            return;
        }
        node.setPresentation(icon, type, this.myValue, false);
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\EvaluationValue.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */