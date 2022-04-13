package com.insidious.plugin.extension.smartstep;

import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.breakpoints.StepIntoBreakpoint;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.jdi.MethodBytecodeUtil;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.ui.breakpoints.SteppingBreakpoint;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.util.containers.ContainerUtil;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LambdaAsyncMethodFilter extends BasicStepMethodFilter {
    private final int myParamNo;

    public LambdaAsyncMethodFilter(@NotNull PsiMethod callerMethod, int paramNo, LambdaMethodFilter methodFilter) {
        super(callerMethod, methodFilter.getCallingExpressionLines());
        this.myParamNo = paramNo;
        this.myMethodFilter = methodFilter;
    }


    private final LambdaMethodFilter myMethodFilter;


    public boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location, @Nullable InsidiousStackFrameProxy frameProxy) throws EvaluateException {
        if (super.locationMatches(virtualMachineProxy, location, frameProxy) && frameProxy != null) {

            Value lambdaReference = getLambdaReference(frameProxy);
            if (lambdaReference instanceof ObjectReference) {


                Objects.requireNonNull(virtualMachineProxy);
                Method lambdaMethod = MethodBytecodeUtil.getLambdaMethod(((ObjectReference) lambdaReference).referenceType(), virtualMachineProxy::classesByName);


                Location newLocation = (lambdaMethod != null) ? ContainerUtil.getFirstItem(DebuggerUtilsEx.allLineLocations(lambdaMethod)) : null;
                return (newLocation != null && this.myMethodFilter
                        .locationMatches(virtualMachineProxy, newLocation));
            }
        }
        return false;
    }


    public int onReached(InsidiousXSuspendContext context, RequestHint hint) {
        try {
            InsidiousStackFrameProxy proxy = context.getFrameProxy();
            if (proxy != null) {
                Value lambdaReference = getLambdaReference(proxy);
                if (lambdaReference instanceof ObjectReference) {
                    SourcePosition pos = this.myMethodFilter.getBreakpointPosition();
                    if (pos != null) {
                        Project project = context.getDebugProcess().getProject();
                        long lambdaId = ((ObjectReference) lambdaReference).uniqueID();
                        StepIntoBreakpoint breakpoint = new LambdaInstanceBreakpoint(project, lambdaId, pos, this.myMethodFilter);


                        context.getDebugProcess()
                                .getConnector()
                                .createSteppingBreakpoint(context, breakpoint, hint);
                        return -100;
                    }
                }
            }
        } catch (EvaluateException evaluateException) {
        }

        return 0;
    }

    @Nullable
    private Value getLambdaReference(InsidiousStackFrameProxy proxy) throws EvaluateException {
        return ContainerUtil.getOrElse(proxy.getArgumentValues(), this.myParamNo, null);
    }


    private static class LambdaInstanceBreakpoint
            extends StepIntoBreakpoint {
        private final long myLambdaId;


        LambdaInstanceBreakpoint(@NotNull Project project, long lambdaId, @NotNull SourcePosition pos, @NotNull BreakpointStepMethodFilter filter) {
            super(project, pos, filter);
            this.myLambdaId = lambdaId;
        }


        public boolean evaluateCondition(EvaluationContextImpl context, LocatableEvent event) throws EvaluateException {
            if (!super.evaluateCondition(context, event)) {
                return false;
            }

            if (!DebuggerUtilsEx.isLambda(event.location().method())) {
                return false;
            }


            ObjectReference lambdaReference = null;
            StackFrameProxyImpl parentFrame = context.getSuspendContext().getThread().frame(1);
            if (parentFrame != null) {
                try {
                    lambdaReference = parentFrame.thisObject();
                } catch (EvaluateException evaluateException) {
                }
            }

            return (lambdaReference != null && lambdaReference.uniqueID() == this.myLambdaId);
        }
    }
}


