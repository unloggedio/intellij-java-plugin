package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Range;
import com.sun.jdi.*;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.evaluation.JVMName;
import com.insidious.plugin.extension.evaluation.JVMNameUtil;
import com.insidious.plugin.extension.smartstep.NamedMethodFilter;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.insidious.plugin.extension.util.DebuggerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasicStepMethodFilter implements NamedMethodFilter {
    private static final Logger logger = LoggerUtil.getInstance(BasicStepMethodFilter.class);
    private static final String PROXY_CALL_SIGNATURE_POSTFIX = "Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;";
    @NotNull
    protected final JVMName myDeclaringClassName;
    @Nullable
    protected final JVMName myTargetMethodSignature;
    @NotNull
    private final String myTargetMethodName;
    private final Range<Integer> myCallingExpressionLines;
    private final int myOrdinal;
    private final boolean myCheckCaller;

    public BasicStepMethodFilter(@NotNull PsiMethod psiMethod, Range<Integer> callingExpressionLines) {
        this(psiMethod, 0, callingExpressionLines);
    }


    public BasicStepMethodFilter(@NotNull PsiMethod psiMethod, int ordinal, Range<Integer> callingExpressionLines) {
        this(
                JVMNameUtil.getJVMQualifiedName(psiMethod.getContainingClass()),
                JVMNameUtil.getJVMMethodName(psiMethod),
                JVMNameUtil.getJVMSignature(psiMethod), ordinal, callingExpressionLines,
                checkCaller(psiMethod));
    }


    protected BasicStepMethodFilter(@NotNull JVMName declaringClassName, @NotNull String targetMethodName, @Nullable JVMName targetMethodSignature, int ordinal, Range<Integer> callingExpressionLines, boolean checkCaller) {
        this.myDeclaringClassName = declaringClassName;
        this.myTargetMethodName = targetMethodName;
        this.myTargetMethodSignature = targetMethodSignature;
        this.myCallingExpressionLines = callingExpressionLines;
        this.myOrdinal = ordinal;
        this.myCheckCaller = checkCaller;
    }

    private static boolean checkCaller(PsiMethod method) {
        PsiClass aClass = method.getContainingClass();
        return (aClass != null && aClass.hasAnnotation("java.lang.FunctionalInterface"));
    }

    private static boolean signatureMatches(Method method, String expectedSignature) {
//        if (expectedSignature.equals(method.signature())) {
//            return true;
//        }
//
//        for (Method candidate : method.declaringType().methodsByName(method.name())) {
//            if (candidate != method && candidate
//                    .isBridge() && expectedSignature
//                    .equals(candidate.signature())) {
//                return true;
//            }
//        }
//        return false;
        return false;
    }

    @NotNull
    public String getMethodName() {
        return this.myTargetMethodName;
    }

    public boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location) throws EvaluateException {
        return locationMatches(virtualMachineProxy, location, null, false);
    }

    public boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location, @Nullable InsidiousStackFrameProxy frameProxy) throws EvaluateException {
        return locationMatches(virtualMachineProxy, location, frameProxy, false);
    }

    private boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location, @Nullable InsidiousStackFrameProxy stackFrame, boolean caller) throws EvaluateException {
        Method method = location.method();
        String name = method.name();
        if (!this.myTargetMethodName.equals(name)) {
            if (isLambdaCall(virtualMachineProxy, name, location)) {
                return true;
            }
            if (!caller && this.myCheckCaller) {
                int index = stackFrame.getFrameIndex();
                InsidiousStackFrameProxy callerFrame = stackFrame.threadProxy().frame(index + 1);
                if (callerFrame != null) {
                    return locationMatches(virtualMachineProxy, callerFrame
                            .location(), callerFrame, true);
                }
            }
            return false;
        }
        if (this.myTargetMethodSignature != null &&
                !signatureMatches(method, this.myTargetMethodSignature
                        .getName(virtualMachineProxy))) {
            return false;
        }
        if (!caller && RequestHint.isProxyMethod(method)) {
            return false;
        }
        String declaringClassNameName = this.myDeclaringClassName.getName(virtualMachineProxy);

        boolean res = DebuggerUtilsEx.isAssignableFrom(declaringClassNameName, location.declaringType());
        if (!res && !method.isStatic() && stackFrame != null) {
            ObjectReference thisObject = stackFrame.thisObject();
            if (thisObject != null) {
                res = DebuggerUtilsEx.isAssignableFrom(declaringClassNameName, thisObject
                        .referenceType());
            }
        }
        return res;
    }

    private boolean isLambdaCall(InsidiousVirtualMachineProxy virtualMachineProxy, String name, Location location) {
        if (DebuggerUtilsEx.isLambdaName(name)) {
            SourcePosition position = null;
            try {
                position = virtualMachineProxy.getPositionManager().getSourcePosition(location);
            } catch (NoDataException noDataException) {
            }

            SourcePosition finalPosition = position;
            return ReadAction.compute(() -> {
                PsiElement psiMethod = DebuggerUtilsEx.getContainingMethod(finalPosition);


                if (psiMethod instanceof PsiLambdaExpression) {
                    PsiType type = ((PsiLambdaExpression) psiMethod).getFunctionalInterfaceType();


                    PsiMethod interfaceMethod = LambdaUtil.getFunctionalInterfaceMethod(type);


                    if (type != null && interfaceMethod != null && this.myTargetMethodName.equals(interfaceMethod.getName())) {
                        try {
                            return InheritanceUtil.isInheritor(type,
                                    this.myDeclaringClassName.getName(virtualMachineProxy).replace('$', '.'));
                        } catch (EvaluateException e) {
                            logger.error("failed to evaluate", e);
                        }
                    }
                }
                return Boolean.FALSE;
            });
        }
        return false;
    }

    public boolean proxyCheck(Location location, InsidiousXSuspendContext context, RequestHint hint) {
        InsidiousJavaDebugProcess debugProcess = context.getDebugProcess();
        InsidiousVirtualMachineProxy virtualMachineProxy = context.getInsidiousVirtualMachineProxy();
        InsidiousStackFrameProxy stackFrameProxy = context.getFrameProxy();
        if (isProxyCall(virtualMachineProxy, location.method(), stackFrameProxy)) {
            if (!DebuggerUtil.isPositionFiltered(location)) {
                return true;
            }


//            try {
//                StepIntoMethodBreakpoint breakpoint = new StepIntoMethodBreakpoint(this.myDeclaringClassName.getName(virtualMachineProxy), this.myTargetMethodName, (this.myTargetMethodSignature != null) ? this.myTargetMethodSignature.getName(virtualMachineProxy) : null, debugProcess.getProject());
//                debugProcess.getConnector().createSteppingBreakpoint(context, breakpoint, hint);
//            } catch (EvaluateException e) {
//                logger.error("failed to evaluate", e);
//            }
        }
        return false;
    }

    private boolean isProxyCall(InsidiousVirtualMachineProxy virtualMachineProxy, Method method, @Nullable InsidiousStackFrameProxy stackFrame) {
        try {
            String signature = method.signature();
            if (stackFrame != null && signature != null && signature

                    .endsWith("Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;")) {
                String methodName = method.name();
                boolean match = false;

                if ("invoke".equals(methodName)) {
                    ReferenceType type = method.declaringType();
                    if (type instanceof ClassType && ((ClassType) type)

                            .interfaces().stream()
                            .map(ReferenceType::name)
                            .anyMatch("java.lang.reflect.InvocationHandler"::equals)) {

                        match = true;
                    }
                }
                if (DebuggerUtilsEx.isLambdaName(methodName)) {
                    match = true;
                } else {
                    ObjectReference thisObject = stackFrame.thisObject();
                    if (thisObject != null &&
                            StringUtil.containsIgnoreCase(thisObject
                                    .referenceType().name(), "CGLIB")) {
                        match = true;
                    }
                }
                if (match) {
                    List<Value> argumentValues = stackFrame.getArgumentValues();
                    int size = argumentValues.size();
                    if (size >= 3) {
                        Value proxyValue = argumentValues.get(size - 3);
                        if (proxyValue != null) {
                            Type proxyType = proxyValue.type();
                            if (proxyType instanceof ReferenceType &&
                                    DebuggerUtilsEx.isAssignableFrom(this.myDeclaringClassName
                                            .getName(virtualMachineProxy), proxyType)) {

                                Value methodValue = argumentValues.get(size - 2);
                                if (methodValue instanceof ObjectReference) {


                                    ReferenceType methodType = ((ObjectReference) methodValue).referenceType();
                                    return this.myTargetMethodName.equals(((StringReference) ((ObjectReference) methodValue)


                                            .getValue(methodType
                                                    .fieldByName("name")))

                                            .value());
                                }
                            }
                        }
                    }
                }
            }
        } catch (EvaluateException e) {
            logger.info("failed to evaluate", e);
        }
        return false;
    }

    @Nullable
    public Range<Integer> getCallingExpressionLines() {
        return this.myCallingExpressionLines;
    }


    public int getSkipCount() {
        return this.myOrdinal;
    }
}


