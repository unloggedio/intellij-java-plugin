package extension.descriptor;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodePresentationConfigurator;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public class InsidiousFieldReferringObject implements InsidiousReferringObject {
    @NotNull
    private final ObjectReference myReference;
    @NotNull
    private final Field myField;

    public InsidiousFieldReferringObject(@NotNull ObjectReference reference, @NotNull Field field) {
        this.myReference = reference;
        this.myField = field;
    }


    @NotNull
    public InsidiousValueDescriptorImpl createValueDescription(@NotNull Project project, @NotNull Value referee) {
        return new InsidiousFieldDescriptorImpl(project, this.myReference, this.myField) {
            public Value calcValue(EvaluationContext evaluationContext) {
                return InsidiousFieldReferringObject.this.myReference;
            }
        };
    }


    @Nullable
    public String getNodeName(int order) {
        return null;
    }


    @NotNull
    public Function<XValueNode, XValueNode> getNodeCustomizer() {
        return node -> new XValueNodePresentationConfigurator.ConfigurableXValueNodeImpl() {
            public void setFullValueEvaluator(@NotNull XFullValueEvaluator fullValueEvaluator) {
            }


            public void applyPresentation(@Nullable Icon icon, @NotNull final XValuePresentation valuePresenter, boolean hasChildren) {
                node.setPresentation(icon, new XValuePresentation() {
                    @NotNull
                    public String getSeparator() {
                        return " in ";
                    }

                    @Nullable
                    public String getType() {
                        return valuePresenter.getType();
                    }

                    public void renderValue(@NotNull XValuePresentation.XValueTextRenderer renderer) {
                        valuePresenter.renderValue(renderer);
                    }
                }, hasChildren);
            }
        }
                ;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousFieldReferringObject.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */