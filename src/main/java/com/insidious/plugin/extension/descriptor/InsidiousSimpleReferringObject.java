package com.insidious.plugin.extension.descriptor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiExpression;
import com.intellij.xdebugger.frame.XValueNode;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class InsidiousSimpleReferringObject implements InsidiousReferringObject {
    @NotNull
    private final ObjectReference myReference;


    public InsidiousSimpleReferringObject(@NotNull ObjectReference reference) {
        this.myReference = reference;
    }

    @NotNull
    public InsidiousValueDescriptorImpl createValueDescription(@NotNull Project project, @NotNull Value referee) {
        return new InsidiousValueDescriptorImpl(project, this.myReference) {
            public Value calcValue(EvaluationContext evaluationContext) {
                return getValue();
            }


            public String getName() {
                return "Ref";
            }


            public PsiExpression getDescriptorEvaluation(EvaluationContext context) {
                return null;
            }
        };
    }

    @Nullable
    public String getNodeName(int order) {
        return "Referrer " + order;
    }


    @NotNull
    public Function<XValueNode, XValueNode> getNodeCustomizer() {
        return Function.identity();
    }
}

