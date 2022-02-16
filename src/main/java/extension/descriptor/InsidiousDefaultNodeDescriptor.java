package extension.descriptor;

import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;

public final class InsidiousDefaultNodeDescriptor extends InsidiousNodeDescriptorImpl {
    private static final Logger LOG = Logger.getInstance(InsidiousDefaultNodeDescriptor.class);

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
        LOG.assertTrue(false);
        return null;
    }
}

