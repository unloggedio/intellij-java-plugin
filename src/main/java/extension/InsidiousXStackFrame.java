package extension;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.*;
import com.sun.jdi.ObjectReference;
import extension.connector.InsidiousStackFrameProxy;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluationContextImpl;
import org.jetbrains.annotations.NotNull;

public class InsidiousXStackFrame extends XStackFrame {

    private static final Logger logger = Logger.getInstance(XStackFrame.class);

    private final InsidiousDebugProcess insidiousDebugProcess;
    private final InsidiousXSuspendContext insidiousXSuspendContext;
    private final InsidiousStackFrameProxy insidiousStackFrameProxy;
    private final XSourcePosition xSourcePosition;
    private DebugProcessImpl myDebugProcess;

    public InsidiousXStackFrame(InsidiousDebugProcess insidiousDebugProcess,
                                InsidiousXSuspendContext insidiousXSuspendContext,
                                InsidiousStackFrameProxy insidiousStackFrameProxy, XSourcePosition xSourcePosition) {
        this.insidiousDebugProcess = insidiousDebugProcess;
        this.insidiousXSuspendContext = insidiousXSuspendContext;
        this.insidiousStackFrameProxy = insidiousStackFrameProxy;
        this.xSourcePosition = xSourcePosition;
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        super.computeChildren(node);
        @NotNull XValueChildrenList children = new XValueChildrenList();
        ObjectReference thisObject = null;
        try {
            thisObject = insidiousStackFrameProxy.thisObject();
        } catch (EvaluateException e) {
            node.setErrorMessage(e.getMessage());
        }

        EvaluationContextImpl evaluationContextImpl = new EvaluationContextImpl(this.insidiousXSuspendContext, this.insidiousStackFrameProxy, thisObject);

        buildVariables(evaluationContextImpl, children);
        node.addChildren(children, true);
    }

    private void buildVariables(EvaluationContext evaluationContextImpl, XValueChildrenList children) {
        children.add("variable-1", new XValue() {
            @Override
            public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
                node.setPresentation(null, "String", "Value 1", false);
            }
        });
    }


}
