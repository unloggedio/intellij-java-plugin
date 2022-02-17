package extension.smartstep;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Range;
import com.sun.jdi.ObjectReference;
import extension.ConstructorStepMethodFilter;
import extension.InsidiousXSuspendContext;
import extension.breakpoints.StepIntoBreakpoint;
import extension.connector.InsidiousStackFrameProxy;
import extension.connector.RequestHint;

public class ClassInstanceMethodFilter
        extends ConstructorStepMethodFilter {
    private final BreakpointStepMethodFilter myMethodFilter;

    public ClassInstanceMethodFilter(PsiMethod psiMethod, Range<Integer> lines) {
        super(psiMethod.getContainingClass(), lines);
        this.myMethodFilter = new AnonymousClassMethodFilter(psiMethod, getCallingExpressionLines());
    }

    public int onReached(InsidiousXSuspendContext context, RequestHint hint) {
        InsidiousStackFrameProxy proxy = context.getFrameProxy();
        if (proxy != null) {
            try {
                ObjectReference reference = proxy.thisObject();
                if (reference != null) {

                    StepIntoBreakpoint breakpoint = StepIntoBreakpoint.create(context
                            .getDebugProcess().getProject(), this.myMethodFilter);
                    if (breakpoint != null) {
                        breakpoint.addInstanceFilter(reference.uniqueID());
                        breakpoint.setInstanceFiltersEnabled(true);
                        context.getDebugProcess()
                                .getConnector()
                                .createSteppingBreakpoint(context, breakpoint, hint);
                        return -100;
                    }
                }
            } catch (EvaluateException evaluateException) {
            }
        }

        return 0;
    }
}


