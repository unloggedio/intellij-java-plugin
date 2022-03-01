package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.ContextUtil;
import com.intellij.debugger.engine.evaluation.*;
import com.intellij.debugger.engine.evaluation.expression.UnsupportedExpressionException;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.engine.requests.LocatableEventRequestor;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.WildcardMethodBreakpoint;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.ObjectUtils;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.StepRequest;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluationContextImpl;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import com.insidious.plugin.extension.evaluation.expression.EvaluatorBuilder;
import com.insidious.plugin.extension.evaluation.expression.EvaluatorBuilderImpl;
import com.insidious.plugin.extension.evaluation.expression.ExpressionEvaluator;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;

import java.util.Arrays;
import java.util.function.Function;

public class InsidiousDebuggerEventThread implements Runnable {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousDebuggerEventThread.class);
    private static final Key<Long> HIT_COUNTER = Key.create("HIT_COUNTER");
    private final InsidiousJavaDebugProcess debugProcess;
    private final MethodReturnValueWatcher myReturnValueWatcher;


    private final Thread eventReadingThread;


    private boolean myIsStopped;
    private EvaluationContext evaluationContext;


    InsidiousDebuggerEventThread(InsidiousJavaDebugProcess debugProcess) {
        this.myIsStopped = false;
        this.debugProcess = debugProcess;
//        EventRequestManager eventRequestManager = debugProcess.getConnector().getEventRequestManager();
        this.myReturnValueWatcher = new MethodReturnValueWatcher(null);
//        this.myReturnValueWatcher = new MethodReturnValueWatcher(eventRequestManager);
        this.eventReadingThread = new Thread(this, "InsidiousDebuggerEventThread");
        this.eventReadingThread.setDaemon(true);
    }

    protected static boolean typeMatchesClassFilters(@Nullable String typeName, ClassFilter[] includeFilters, ClassFilter[] exludeFilters) {
        if (typeName == null) {
            return true;
        }
        boolean matches = false, hasEnabled = false;
        for (ClassFilter classFilter : includeFilters) {
            if (classFilter.isEnabled()) {
                hasEnabled = true;
                if (classFilter.matches(typeName)) {
                    matches = true;
                    break;
                }
            }
        }
        if (hasEnabled && !matches) {
            return false;
        }
        return Arrays.stream(exludeFilters)
                .noneMatch(classFilter -> (classFilter.isEnabled() && classFilter.matches(typeName)));
    }

    private static boolean hasObjectID(JavaBreakpointProperties properties, long id) {
        return Arrays.stream(properties.getInstanceFilters())
                .anyMatch(instanceFilter -> (instanceFilter.getId() == id));
    }

    protected static String calculateEventClass(StackFrameProxy frameProxy, LocatableEvent event) throws EvaluateException {
        String className = null;

        ObjectReference thisObject = frameProxy.getStackFrame().thisObject();
        if (thisObject != null) {
            className = thisObject.referenceType().name();
        } else {
            className = frameProxy.location().declaringType().name();
        }
        return className;
    }

    private static ExpressionEvaluator createExpressionEvaluator(Project project, PsiElement contextPsiElement, SourcePosition contextSourcePosition, TextWithImports text, Function<? super PsiElement, ? extends PsiCodeFragment> fragmentFactory, EvaluationContext evaluationContext) throws EvaluateException {
        try {
            return EvaluatorBuilderImpl.build(text, contextPsiElement, contextSourcePosition, project, evaluationContext);
        } catch (UnsupportedExpressionException ex) {
            throw ex;
        }
    }

    public synchronized void startListening() {
        this.myIsStopped = false;
        this.eventReadingThread.start();
    }

    public synchronized void stopListening() {
        this.myIsStopped = true;
    }

    private synchronized boolean isStopped() {
        return this.myIsStopped;
    }

    public void run() {
        try {
            EventQueue eventQueue = this.debugProcess.getConnector().getVirtualMachine().eventQueue();
            while (!isStopped()) {
                try {
                    EventSet eventSet = eventQueue.remove();

                    for (Event event : eventSet) {
                        logger.debug("EVENT : " + event);
                        try {
//                            if (event instanceof VMStartEvent) {
//
//
//                                InsidiousXSuspendContext suspendContext = new InsidiousXSuspendContext(this.debugProcess, ((VMStartEvent) event).thread(), 2, eventSet);
//
//
//                                processVMStartEvent(suspendContext, (VMStartEvent) event);
//                                continue;
//                            }
//                            if (event instanceof com.sun.jdi.event.VMDeathEvent || event instanceof com.sun.jdi.event.VMDisconnectEvent) {
//
//                                close(false);
//                                continue;
//                            }
//                            if (event instanceof ClassPrepareEvent) {
//
//
//                                InsidiousXSuspendContext suspendContext = new InsidiousXSuspendContext(this.debugProcess, ((ClassPrepareEvent) event).thread(), eventSet.suspendPolicy(), eventSet);
//
//                                processClassPrepareEvent(suspendContext, (ClassPrepareEvent) event);
//                                continue;
//                            }
//                            if (event instanceof LocatableEvent) {
//
//
//                                InsidiousXSuspendContext suspendContext = new InsidiousXSuspendContext(this.debugProcess, ((LocatableEvent) event).thread(), eventSet.suspendPolicy(), eventSet);
//
//                                processLocatableEvent(suspendContext, (LocatableEvent) event);
//                                continue;
//                            }
//                            if (event instanceof ThreadStartEvent) {
//
//
//                                InsidiousXSuspendContext suspendContext = new InsidiousXSuspendContext(this.debugProcess, ((ThreadStartEvent) event).thread(), eventSet.suspendPolicy(), eventSet);
//
//                                this.debugProcess.threadStarted(suspendContext);
//                                continue;
//                            }
//                            if (event instanceof ThreadDeathEvent) {
//                                this.debugProcess.threadStopped(((ThreadDeathEvent) event).thread());
//                                continue;
//                            }
//                            if (event instanceof com.sun.jdi.event.ClassUnloadEvent) {
//                                this.debugProcess.resume(null);
//                            }
                        } catch (VMDisconnectedException e) {
                            logger.debug("failed", e);
                        } catch (InternalException e) {
                            logger.info("failed", e);
                        } catch (Throwable e) {
                            logger.error("failed", e);
                        }

                    }
                } catch (InternalException e) {
                    logger.debug("failed", e);
                } catch (InterruptedException | com.intellij.openapi.progress.ProcessCanceledException | VMDisconnectedException e) {


                    throw e;
                } catch (Throwable e) {
                    logger.debug("failed", e);
                }
            }
        } catch (InterruptedException | VMDisconnectedException e) {
            close(false);
        } finally {
            Thread.interrupted();
        }
    }

    private void processVMStartEvent(InsidiousXSuspendContext suspendContext, VMStartEvent event) {
//        InsidiousThreadReferenceProxyImpl InsidiousThreadReferenceProxyImpl = new InsidiousThreadReferenceProxyImpl(this.debugProcess.getConnector(), event.thread());
//        suspendContext.setThread(InsidiousThreadReferenceProxyImpl);
//        this.debugProcess.resumeAutomatic(suspendContext);
//        logger.debug("processedVMStartEvent()");
    }

    private void processClassPrepareEvent(InsidiousXSuspendContext suspendContext, ClassPrepareEvent event) {
        ReferenceType refType = event.referenceType();
        if (refType instanceof com.sun.jdi.ClassType || refType instanceof com.sun.jdi.InterfaceType) {

            ClassPrepareRequestor requestor = (ClassPrepareRequestor) event.request().getProperty(InsidiousJDIConnector.REQUESTOR);
            if (requestor instanceof Breakpoint) {
                XBreakpoint<?> breakpoint = ((Breakpoint) requestor).getXBreakpoint();
                for (InsidiousBreakpointHandler handler : this.debugProcess.getBreakpointHandlers()) {
                    if (handler.getBreakpointTypeClass().equals(breakpoint.getType().getClass())) {
                        handler.sendBreakpoint((Breakpoint) requestor, refType);
                        break;
                    }
                }
            }
        }
        if (suspendContext.getSuspendPolicy() != 0) {
            this.debugProcess.resumeAutomatic(suspendContext);
        }
    }

    private void processLocatableEvent(InsidiousXSuspendContext suspendContext, LocatableEvent event) {
        if (this.myReturnValueWatcher != null && this.myReturnValueWatcher.isTrackingEnabled() &&
                this.myReturnValueWatcher.processEvent(event)) {
            return;
        }


        this.debugProcess.cancelRunToCursorBreakpoint();
        this.debugProcess.cancelJumpToAssignmentBreakpoint();


        LocatableEventRequestor requestor = (LocatableEventRequestor) event.request().getProperty(InsidiousJDIConnector.REQUESTOR);
        if (requestor instanceof Breakpoint && ((Breakpoint) requestor)
                .getXBreakpoint() != null) {
            Breakpoint<?> breakpoint = (Breakpoint) requestor;
            boolean sendResume = false;
            try {
                if (breakpoint instanceof com.intellij.debugger.ui.breakpoints.MethodBreakpoint) {

                    PsiMethod method = BreakpointUtil.getPsiMethod(this.debugProcess
                            .getProject(), breakpoint.getXBreakpoint());
                    String methodName = BreakpointUtil.getMethodName(method);

                    String signature = BreakpointUtil.getSignature(method, this.debugProcess.getConnector());
                    Method eventMethod = event.location().method();


                    sendResume = (eventMethod == null || !eventMethod.name().equals(methodName) || !eventMethod.signature().equals(signature));
                } else if (breakpoint instanceof WildcardMethodBreakpoint) {
                    Method eventMethod = event.location().method();

                    sendResume = !BreakpointUtil.matchesWildcardMethod(eventMethod, ((WildcardMethodBreakpoint) breakpoint)

                            .getMethodName());
                }

                if (!sendResume &&
                        !evaluateBreakpointCondition(suspendContext, breakpoint, event)) {
                    sendResume = true;
                }
            } catch (EvaluateException ex) {
                sendResume = true;
            }
            if (sendResume) {
                this.debugProcess.resumeAutomatic(suspendContext);
            } else {
                logger.debug("breakpoint reached starting notify");
                notifyPaused(suspendContext);
                this.debugProcess
                        .getSession()
                        .breakpointReached(breakpoint.getXBreakpoint(), null, suspendContext);
                logger.debug("notified breakpointReached!");
                setBreakpointHitTelemetryAction(breakpoint);
            }
        } else if (event instanceof StepEvent) {
            processStepEvent(suspendContext, (StepEvent) event);
        } else if (event instanceof WatchpointEvent && requestor instanceof SyntheticFieldBreakpoint) {

            processSyntheticFieldBreakpointHit((SyntheticFieldBreakpoint) requestor, (WatchpointEvent) event, suspendContext);
        } else {

            pause(suspendContext);
        }
    }

    private void pause(InsidiousXSuspendContext suspendContext) {
        logger.debug("notify paused");
        notifyPaused(suspendContext);

        suspendContext.setIsInternalEvent(true);
        this.debugProcess.getSession().positionReached(suspendContext);
        suspendContext.setIsInternalEvent(false);
        logger.debug("notified positionReached");
    }

    private void processSyntheticFieldBreakpointHit(SyntheticFieldBreakpoint syntheticFieldBreakpoint, WatchpointEvent event, InsidiousXSuspendContext suspendContext) {
        ObjectReference obj = event.object();
        try {
            if (obj == null) {
                obj = suspendContext.getFrameProxy().thisObject();
            }
        } catch (EvaluateException ex) {
            logger.debug("Couldn't get this object", ex);
        }

        if (syntheticFieldBreakpoint.getObjectReference() == null || syntheticFieldBreakpoint
                .getField().isStatic() || syntheticFieldBreakpoint
                .getObjectReference().equals(obj)) {

            pause(suspendContext);
        } else {
            this.debugProcess.resumeAutomatic(suspendContext);
        }
    }

    private void processStepEvent(InsidiousXSuspendContext suspendContext, StepEvent event) {
        this.debugProcess.getConnector().deleteEventRequest(event.request());

        RequestHint hint = (RequestHint) event.request().getProperty(InsidiousJDIConnector.REQUEST_HINT);
        boolean notifyPaused = true;

        if (hint != null) {

            int nextStepDepth = hint.getNextStepDepth(suspendContext);
            if (nextStepDepth == -100) {
                this.debugProcess.resumeAutomatic(suspendContext);
                notifyPaused = false;
            } else if (nextStepDepth != 0) {
                this.debugProcess
                        .getConnector()
                        .doStep(suspendContext, -2, nextStepDepth, hint);
                notifyPaused = false;
            }
        }

        if (notifyPaused) {
            notifyPaused(suspendContext);
            this.debugProcess.getSession().positionReached(suspendContext);
            setSingleStepHitTelemetryAction((StepRequest) event.request(), hint);
        }
    }

    public void notifyPaused(XSuspendContext suspendContext) {
        stopWatchingMethodReturn();
        this.debugProcess.notifyPaused(suspendContext);
    }

    private void close(boolean closedByUser) {
        stopListening();
        this.debugProcess.closeProcess(closedByUser);
    }

    public boolean evaluateBreakpointCondition(InsidiousXSuspendContext suspendContext, Breakpoint breakpoint, LocatableEvent event) throws EvaluateException {
        InsidiousStackFrameProxy stackFrameProxy = suspendContext.getActiveExecutionStack().getTopStackFrameProxy();
        XBreakpoint<?> xBreakpoint = breakpoint.getXBreakpoint();

        JavaBreakpointProperties<?> properties = (JavaBreakpointProperties) xBreakpoint.getProperties();
        if (properties.isCALLER_FILTERS_ENABLED() && stackFrameProxy != null) {
            InsidiousThreadReferenceProxy InsidiousThreadReferenceProxy = stackFrameProxy.threadProxy();

            StackFrameProxy parentFrame = (InsidiousThreadReferenceProxy.frameCount() > 1) ? InsidiousThreadReferenceProxy.frame(1) : null;


            String key = (parentFrame != null) ? DebuggerUtilsEx.methodKey(parentFrame.location().method()) : null;
            if (!typeMatchesClassFilters(key, properties
                    .getCallerFilters(), properties.getCallerExclusionFilters())) {
                return false;
            }
        }

        if (breakpoint.isInstanceFiltersEnabled()) {
            Value value = stackFrameProxy.thisObject();
            if (value != null) {
                ObjectReference reference = (ObjectReference) value;
                if (!hasObjectID(properties, reference.uniqueID())) {
                    return false;
                }
            }
        }

        if (breakpoint.isClassFiltersEnabled() &&
                !typeMatchesClassFilters(
                        calculateEventClass(stackFrameProxy, event), properties
                                .getClassFilters(), properties
                                .getClassExclusionFilters())) {
            return false;
        }

        if (breakpoint.isConditionEnabled()) {

            TextWithImports condition = TextWithImportsImpl.fromXExpression(xBreakpoint.getConditionExpression());
            if (condition.isEmpty())
                return true;
            if (stackFrameProxy == null) {
                return false;
            }

            try {
                ObjectReference thisObject = stackFrameProxy.thisObject();
                EvaluationContextImpl evaluationContextImpl = new EvaluationContextImpl(suspendContext, stackFrameProxy, thisObject);


                ExpressionEvaluator evaluator = DebuggerInvocationUtil.commitAndRunReadAction(this.debugProcess
                        .getProject(), () -> {
                    SourcePosition position = DebuggerUtilsEx.toSourcePosition(xBreakpoint.getSourcePosition(), this.debugProcess.getProject());


                    PsiElement contextElement = ContextUtil.getContextElement(position);


                    PsiElement contextPsiElement = (contextElement != null) ? contextElement : breakpoint.getEvaluationElement();


                    CodeFragmentFactory codeFragmentFactory = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(condition, contextPsiElement);


                    JavaCodeFragment code = codeFragmentFactory.createCodeFragment(condition, contextPsiElement, this.debugProcess.getProject());


                    EvaluatorBuilder evaluatorBuilder = EvaluatorBuilderImpl.getInstance();


                    return evaluatorBuilder.build(code, position, evaluationContext);
                });


                if (!EvaluatorUtil.evaluateBoolean(evaluator, evaluationContextImpl)) {
                    return false;
                }
            } catch (EvaluateException ex) {
                if (ex.getCause() instanceof VMDisconnectedException) {
                    return false;
                }
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("error.failed.evaluating.breakpoint.condition", condition, ex.getMessage()));
            }
        }
        if (breakpoint.isCountFilterEnabled() && breakpoint.isConditionEnabled()) {

            long hitCount = ((Long) ObjectUtils.notNull(event.request().getProperty(HIT_COUNTER), Long.valueOf(0L))).longValue() + 1L;
            event.request().putProperty(HIT_COUNTER, Long.valueOf(hitCount));
            return (hitCount % breakpoint.getCountFilter() == 0L);
        }
        return true;
    }

    public MethodReturnValueWatcher getReturnValueWatcher() {
        return this.myReturnValueWatcher;
    }

    public void startWatchingMethodReturn(ThreadReference thread) {
        if (this.myReturnValueWatcher != null) {
            this.myReturnValueWatcher.enable(thread);
        }
    }

    void stopWatchingMethodReturn() {
        if (this.myReturnValueWatcher != null) {
            this.myReturnValueWatcher.disable();
        }
    }

    private void setBreakpointHitTelemetryAction(Breakpoint<?> breakpoint) {
        if (breakpoint instanceof com.intellij.debugger.ui.breakpoints.LineBreakpoint) {
            this.debugProcess.addPerformanceAction("line_breakpoint_hit");
        } else if (breakpoint instanceof com.intellij.debugger.ui.breakpoints.MethodBreakpoint || breakpoint instanceof WildcardMethodBreakpoint) {

            this.debugProcess.addPerformanceAction("method_breakpoint_hit");
        } else if (breakpoint instanceof com.intellij.debugger.ui.breakpoints.ExceptionBreakpoint) {
            this.debugProcess.addPerformanceAction("exception_breakpoint_hit");
        } else if (breakpoint instanceof com.intellij.debugger.ui.breakpoints.FieldBreakpoint) {
            this.debugProcess.addPerformanceAction("watchpoint_hit");
        }
    }

    private void setSingleStepHitTelemetryAction(StepRequest request, RequestHint hint) {
        if (this.debugProcess.getLastDirectionType() == DirectionType.BACKWARDS) {
            if (request.depth() == 1) {
                if (hint != null && hint.getMethodFilter() != null) {
                    this.debugProcess.addPerformanceAction("reverse_smartstep_into_hit");
                } else {
                    this.debugProcess.addPerformanceAction("reverse_step_into_hit");
                }
            } else if (request.depth() == 3) {
                this.debugProcess.addPerformanceAction("reverse_step_out_hit");
            } else if (request.depth() == 2) {
                this.debugProcess.addPerformanceAction("reverse_step_over_hit");
            }

        } else if (request.depth() == 1) {
            if (hint != null && hint.getMethodFilter() != null) {
                this.debugProcess.addPerformanceAction("smart_step_into_hit");
            } else {
                this.debugProcess.addPerformanceAction("step_into_hit");
            }
        } else if (request.depth() == 3) {
            this.debugProcess.addPerformanceAction("step_out_hit");
        } else if (request.depth() == 2) {
            this.debugProcess.addPerformanceAction("step_over_hit");
        }
    }
}


