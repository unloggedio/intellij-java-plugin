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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousDefaultNodeDescriptor.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */