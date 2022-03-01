package com.insidious.plugin.extension.descriptor;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import org.slf4j.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;

public final class InsidiousDefaultNodeDescriptor extends InsidiousNodeDescriptorImpl {
    private static final Logger LOG = LoggerUtil.getInstance(InsidiousDefaultNodeDescriptor.class);

    public boolean equals(Object obj) {
        return obj instanceof InsidiousDefaultNodeDescriptor;
    }

    public int hashCode() {
        return 0;
    }


    public boolean isExpandable() {
        return true;
    }


    public void setContext(EvaluationContext context) {
    }


    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) {
        LOG.info("Assert false");
        return null;
    }
}

