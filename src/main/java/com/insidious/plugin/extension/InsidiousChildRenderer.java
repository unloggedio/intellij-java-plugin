package com.insidious.plugin.extension;


import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.psi.PsiElement;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousChildrenBuilder;
import com.insidious.plugin.extension.evaluation.EvaluationContext;

import java.util.concurrent.CompletableFuture;


public interface InsidiousChildRenderer
        extends Renderer {
    void buildChildren(Value paramValue, InsidiousChildrenBuilder paramInsidiousChildrenBuilder, EvaluationContext paramEvaluationContext);


    PsiElement getChildValueExpression(InsidiousDebuggerTreeNode paramInsidiousDebuggerTreeNode, EvaluationContext paramEvaluationContext) throws EvaluateException;


    @Deprecated
    default boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {

        throw new AbstractMethodError("isExpandableAsync is not implemented");

    }


    default CompletableFuture<Boolean> isExpandableAsync(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {

        return CompletableFuture.completedFuture(
                Boolean.valueOf(isExpandable(value, evaluationContext, parentDescriptor)));

    }

}
