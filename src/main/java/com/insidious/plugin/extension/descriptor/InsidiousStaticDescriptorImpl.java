package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.StaticDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.TypeComponent;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousClassRenderer;
import com.insidious.plugin.extension.descriptor.renderer.RendererManager;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;

public class InsidiousStaticDescriptorImpl
        extends InsidiousNodeDescriptorImpl implements StaticDescriptor {
    private final ReferenceType myType;
    private final boolean myHasStaticFields;

    public InsidiousStaticDescriptorImpl(ReferenceType refType) {
        this.myType = refType;
        this.myHasStaticFields = this.myType.allFields().stream().anyMatch(TypeComponent::isStatic);
    }


    public ReferenceType getType() {
        return this.myType;
    }


    public String getName() {
        return "static";
    }


    public boolean isExpandable() {
        return this.myHasStaticFields;
    }


    public void setContext(EvaluationContext context) {
    }


    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener descriptorLabelListener) throws EvaluateException {
        InsidiousClassRenderer classRenderer = RendererManager.getInstance().getClassRenderer();
        return getName() + " = " + classRenderer.renderTypeName(this.myType.name());
    }
}

