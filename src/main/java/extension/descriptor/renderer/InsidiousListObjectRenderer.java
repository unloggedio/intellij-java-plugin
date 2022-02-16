package extension.descriptor.renderer;


import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.impl.watch.ArrayElementDescriptorImpl;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import extension.InsidiousDebuggerTreeNode;
import extension.descriptor.InsidiousArrayElementDescriptorImpl;
import extension.evaluation.EvaluationContext;


public class InsidiousListObjectRenderer
        extends InsidiousCompoundReferenceRenderer {
    InsidiousListObjectRenderer(InsidiousArrayRenderer arrayRenderer) {
        super("List",

                RendererManager.createLabelRenderer(" size = ", "size()", null),
                RendererManager.createExpressionArrayChildrenRenderer("toArray()", "!isEmpty()", arrayRenderer));

        setClassName("java.util.List");
    }


    public PsiElement getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        LOG.assertTrue(node.getDescriptor() instanceof InsidiousArrayElementDescriptorImpl);
        try {
            return getChildValueExpression("this.get(" + ((ArrayElementDescriptorImpl) node

                    .getDescriptor()).getIndex() + ")", node, context);


        } catch (IncorrectOperationException e) {

            return super.getChildValueExpression(node, context);
        }
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousListObjectRenderer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */