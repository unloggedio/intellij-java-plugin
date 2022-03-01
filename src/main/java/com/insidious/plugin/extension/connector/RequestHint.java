package com.insidious.plugin.extension.connector;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.BreakpointStepMethodFilter;
import com.intellij.debugger.engine.ContextUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.util.Range;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.VMDisconnectedException;
import com.insidious.plugin.extension.BasicStepMethodFilter;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.smartstep.MethodFilter;
import org.jetbrains.annotations.Nullable;


public class RequestHint {
    public static final int STOP = 0;
    public static final int RESUME = -100;
    private static final Logger logger = LoggerUtil.getInstance(RequestHint.class);

    private final int mySize;

    private final int myDepth;
    @Nullable
    private final MethodFilter myMethodFilter;
    private final SourcePosition myPosition;
    private int myFrameCount;
    private boolean mySteppedOut = false;
    private int myFilterMatchedCount = 0;


    private boolean myTargetMethodMatched = false;


    private boolean myIgnoreFilters = false;


    private boolean myResetIgnoreFilters = false;


    private boolean myRestoreBreakpoints = false;


    public RequestHint(InsidiousXSuspendContext InsidiousXSuspendContext, int depth) {
        this(InsidiousXSuspendContext, -2, depth, null);
    }


    public RequestHint(InsidiousXSuspendContext insidiousXSuspendContext, int stepSize, int depth, @Nullable MethodFilter methodFilter) {
        this.mySize = stepSize;
        this.myDepth = depth;
        this.myMethodFilter = methodFilter;

        try {
            this.myFrameCount = insidiousXSuspendContext.getThreadReferenceProxy().frameCount();
        } catch (EvaluateException e) {
            this.myFrameCount = 0;
        }
        this.myPosition = insidiousXSuspendContext.getSourcePosition();
    }

    public static boolean isProxyMethod(Method method) {
        return (method.isBridge() || DebuggerUtilsEx.isProxyClass(method.declaringType()));
    }

    public boolean isResetIgnoreFilters() {
        return this.myResetIgnoreFilters;
    }

    public void setResetIgnoreFilters(boolean resetIgnoreFilters) {
        this.myResetIgnoreFilters = resetIgnoreFilters;
    }

    public boolean isRestoreBreakpoints() {
        return this.myRestoreBreakpoints;
    }

    public void setRestoreBreakpoints(boolean restoreBreakpoints) {
        this.myRestoreBreakpoints = restoreBreakpoints;
    }

    public boolean isIgnoreFilters() {
        return this.myIgnoreFilters;
    }

    public void setIgnoreFilters(boolean ignoreFilters) {
        this.myIgnoreFilters = ignoreFilters;
    }

    public int getSize() {
        return this.mySize;
    }

    public int getDepth() {
        return this.myDepth;
    }

    @Nullable
    public MethodFilter getMethodFilter() {
        return this.myMethodFilter;
    }

    public boolean wasStepTargetMethodMatched() {
        return (this.myMethodFilter instanceof BreakpointStepMethodFilter || this.myTargetMethodMatched);
    }

    protected boolean isTheSameFrame(InsidiousThreadReferenceProxy thread) {
        if (this.mySteppedOut) return false;
        if (thread != null) {
            try {
                int currentDepth = thread.frameCount();
                if (currentDepth < this.myFrameCount) this.mySteppedOut = true;
                return (currentDepth == this.myFrameCount);
            } catch (EvaluateException ignored) {
                logger.debug("ignored error: ", ignored);
            }
        }
        return false;
    }

    private boolean isOnTheSameLine(SourcePosition locationPosition) {
        if (this.myMethodFilter == null) {
            return (this.myPosition.getLine() == locationPosition.getLine());
        }
        Range<Integer> exprLines = this.myMethodFilter.getCallingExpressionLines();
        return (exprLines != null && exprLines.isWithin(Integer.valueOf(locationPosition.getLine())));
    }

    protected boolean isSteppedOut() {
        return this.mySteppedOut;
    }

    public Integer checkCurrentPosition(InsidiousXSuspendContext suspendContext, Location location) {
        if ((this.myDepth == 2 || this.myDepth == 1) && this.myPosition != null) {


            SourcePosition locationPosition = ReadAction.compute(() -> suspendContext.getDebugProcess().getPositionManager().getSourcePosition(location));


            if (locationPosition != null) {
                return ReadAction.compute(() ->

                        (this.myPosition.getFile().equals(locationPosition.getFile()) && isTheSameFrame(suspendContext.getThreadReferenceProxy()) && !this.mySteppedOut) ? Integer.valueOf(isOnTheSameLine(locationPosition) ? this.myDepth : 0) : null);
            }
        }


        return null;
    }

    public int getNextStepDepth(InsidiousXSuspendContext context) {

        try {
            Location location = context.getFrameProxy().location();


            if (this.myMethodFilter != null && location != null && !(this.myMethodFilter instanceof BreakpointStepMethodFilter) &&


                    !isTheSameFrame(context.getThreadReferenceProxy())) {
                if (isProxyMethod(location.method())) {
                    return 1;
                }


                boolean proxyMatch = (this.myMethodFilter instanceof BasicStepMethodFilter && ((BasicStepMethodFilter) this.myMethodFilter).proxyCheck(location, context, this));
                if (proxyMatch || this.myMethodFilter
                        .locationMatches(context
                                .getInsidiousVirtualMachineProxy(), location, context

                                .getFrameProxy())) {
                    if (this.myMethodFilter.getSkipCount() <= this.myFilterMatchedCount++) {
                        this.myTargetMethodMatched = true;

                        return 0;
                    }
                }
            }

            Integer resultDepth = checkCurrentPosition(context, location);
            if (resultDepth != null) {
                return resultDepth.intValue();
            }


            DebuggerSettings settings = DebuggerSettings.getInstance();

            if ((this.myMethodFilter != null || (settings.SKIP_SYNTHETIC_METHODS && !this.myIgnoreFilters)) && location != null &&

                    DebuggerUtils.isSynthetic(location.method())) {
                return this.myDepth;
            }

            if (!this.myIgnoreFilters) {
                if (settings.SKIP_GETTERS) {


                    boolean isGetter = ReadAction.compute(() -> {
                        PsiElement contextElement = ContextUtil.getContextElement(context.getSourcePosition());
                        return Boolean.valueOf((contextElement != null && DebuggerUtils.isInsideSimpleGetter(contextElement)));
                    }).booleanValue();

                    if (isGetter) {
                        return 3;
                    }
                }

                if (location != null) {
                    if (settings.SKIP_CONSTRUCTORS) {
                        Method method = location.method();
                        if (method != null && method.isConstructor()) {
                            return 3;
                        }
                    }

                    if (settings.SKIP_CLASSLOADERS &&
                            DebuggerUtilsEx.isAssignableFrom("java.lang.ClassLoader", location
                                    .declaringType())) {
                        return 3;
                    }
                }
            }

            if (this.myMethodFilter != null) {
                isTheSameFrame(context.getThreadReferenceProxy());
                if (!this.mySteppedOut) {
                    return 3;
                }
            }
        } catch (VMDisconnectedException vMDisconnectedException) {
        } catch (EvaluateException e) {
            logger.error("failed to evaluate", e);
        }

        return 0;
    }
}


