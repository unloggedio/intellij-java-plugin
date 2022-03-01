package com.insidious.plugin.extension.thread;

import com.intellij.xdebugger.frame.*;
import com.insidious.plugin.extension.thread.types.InsidiousObjectReference;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class InsidiousXValue extends XValue {
    private final InsidiousValue variable;

    public InsidiousXValue(InsidiousValue variable) {
        this.variable = variable;
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        Object actualValue = variable.getActualValue();

        if (actualValue instanceof InsidiousObjectReference) {
            InsidiousObjectReference valueObjectReference = (InsidiousObjectReference) actualValue;
            node.setPresentation(null, variable.type().name(), "{" + valueObjectReference.type().name() + "}", true);
        } else {
            node.setPresentation(null, variable.type().name(), String.valueOf(actualValue), false);
        }

    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        InsidiousObjectReference valueObjectReference = (InsidiousObjectReference) variable.getActualValue();

        Map<String, InsidiousLocalVariable> childValues = valueObjectReference.getValues();

        @NotNull XValueChildrenList children = new XValueChildrenList();
        for (Map.Entry<String, InsidiousLocalVariable> entry : childValues.entrySet()) {
            children.add(entry.getKey(), new InsidiousXValue(entry.getValue().getValue()));
        }
        node.addChildren(children, true);


    }
}
