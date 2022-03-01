package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluateRuntimeException;
import com.intellij.debugger.engine.evaluation.expression.SuperEvaluator;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import org.slf4j.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.rt.debugger.DefaultMethodInvoker;
import com.intellij.util.containers.ContainerUtil;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import com.insidious.plugin.extension.evaluation.JVMName;
import one.util.streamex.StreamEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodEvaluator implements Evaluator {
    private static final Logger logger = LoggerUtil.getInstance(MethodEvaluator.class);

    private final JVMName myClassName;

    private final JVMName myMethodSignature;

    private final String myMethodName;

    private final Evaluator[] myArgumentEvaluators;

    private final Evaluator myObjectEvaluator;
    private final boolean myCheckDefaultInterfaceMethod;
    private final boolean myMustBeVararg;

    public MethodEvaluator(Evaluator objectEvaluator, JVMName className, String methodName, JVMName signature, Evaluator[] argumentEvaluators) {
        this(objectEvaluator, className, methodName, signature, argumentEvaluators, false, false);
    }


    public MethodEvaluator(Evaluator objectEvaluator, JVMName className, String methodName, JVMName signature, Evaluator[] argumentEvaluators, boolean checkDefaultInterfaceMethod, boolean mustBeVararg) {
        this.myObjectEvaluator = DisableGC.create(objectEvaluator);
        this.myClassName = className;
        this.myMethodName = methodName;
        this.myMethodSignature = signature;
        this.myArgumentEvaluators = argumentEvaluators;
        this.myCheckDefaultInterfaceMethod = checkDefaultInterfaceMethod;
        this.myMustBeVararg = mustBeVararg;
    }

    private static boolean matchArgs(Method m, List<Value> args) {
        try {
            List<Type> argumentTypes = m.argumentTypes();
            for (int i = 0; i < argumentTypes.size(); i++) {
                Type expectedArgType = argumentTypes.get(i);
                Type argType = args.get(i).type();
                if (expectedArgType.equals(argType)) {
                    continue;
                }
                if (expectedArgType instanceof ReferenceType) {
                    if (argType == null)
                        continue;
                    if (!(argType instanceof com.sun.jdi.PrimitiveType)) {
                        if (argType instanceof ReferenceType &&
                                DebuggerUtilsImpl.instanceOf((ReferenceType) argType, (ReferenceType) expectedArgType)) {
                            continue;
                        }
                    }
                }
                return false;
            }
        } catch (ClassNotLoadedException ignored) {
            return false;
        }
        return true;
    }

    private static void argsConversions(Method jdiMethod, List<Value> args, EvaluationContext context) throws EvaluateException {
        if (!jdiMethod.isVarArgs()) {
            List<String> typeNames = jdiMethod.argumentTypeNames();
            int size = typeNames.size();
            if (size == args.size()) {
                for (int i = 0; i < size; i++) {
                    Value arg = args.get(i);

                    PsiPrimitiveType primitiveType = PsiJavaParserFacadeImpl.getPrimitiveType(typeNames.get(i));
                    if (primitiveType == null && arg.type() instanceof com.sun.jdi.PrimitiveType) {
                        args.set(i, (Value) BoxingEvaluator.box(arg, context));
                    } else if (primitiveType != null && !(arg.type() instanceof com.sun.jdi.PrimitiveType)) {
                        args.set(i, (Value) UnBoxingEvaluator.unbox(arg, context));
                    }
                }
            }
        }
    }

    private static boolean isInvokableType(Object type) {
        return (type instanceof ClassType || type instanceof InterfaceType);
    }

    private static Value invokeDefaultMethod(EvaluationContext evaluationContext, Value obj, String name) throws EvaluateException {
        ClassType invokerClass = (ClassType) EvaluatorUtil.findClass(evaluationContext, DefaultMethodInvoker.class

                .getName(), evaluationContext
                .getStackFrameProxy().getClassLoader());

        if (invokerClass != null) {
            Method method = DebuggerUtils.findMethod(invokerClass, "invoke", null);
            if (method != null)
                return EvaluatorUtil.invokeMethod(evaluationContext, invokerClass, method,


                        Arrays.asList(obj, evaluationContext.getVirtualMachineProxy()
                        .getVirtualMachine()
                        .mirrorOf(name)));
        }
        return null;
    }

    private static void wrapVarargParams(Method method, List<Value> args) throws ClassNotLoadedException, InvalidTypeException {
        int argCount = args.size();
        List<Type> paramTypes = method.argumentTypes();
        Type varargType = ContainerUtil.getLastItem(paramTypes);
        if (varargType instanceof ArrayType) {
            int paramCount = paramTypes.size();
            int arraySize = argCount - paramCount + 1;
            ArrayReference argArray = ((ArrayType) varargType).newInstance(arraySize);
            argArray.setValues(0, args, paramCount - 1, arraySize);
            if (paramCount <= argCount) {
                args.subList(paramCount - 1, argCount).clear();
            }
            args.add(argArray);
        }
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        if (!context.getVirtualMachineProxy().isAttached()) {
            return null;
        }


        boolean requiresSuperObject = DisableGC.unwrap(this.myObjectEvaluator) instanceof SuperEvaluator;
        Object object = this.myObjectEvaluator.evaluate(context);
        if (logger.isDebugEnabled()) {
            logger.debug("MethodEvaluator: object = " + object);
        }
        if (object == null) {
            throw EvaluateExceptionUtil.createEvaluateException(new NullPointerException());
        }
        if (!(object instanceof ObjectReference) && !isInvokableType(object)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.evaluating.method", this.myMethodName));
        }
        List<Value> args = new ArrayList<>(this.myArgumentEvaluators.length);
        for (Evaluator evaluator : this.myArgumentEvaluators) {
            args.add((Value) evaluator.evaluate(context));
        }
        try {
            ReferenceType referenceType = null;

            if (object instanceof ObjectReference) {


                referenceType = ((ObjectReference) object).referenceType();
            } else if (isInvokableType(object)) {
                referenceType = (ReferenceType) object;
            }

            if (referenceType == null) {
                throw new EvaluateRuntimeException(
                        EvaluateExceptionUtil.createEvaluateException(
                                DebuggerBundle.message("evaluation.error.cannot.evaluate.qualifier", this.myMethodName)));
            }


            String signature = (this.myMethodSignature != null) ? this.myMethodSignature.getName(context.getVirtualMachineProxy()) : null;

            if (requiresSuperObject && referenceType instanceof ClassType) {
                referenceType = ((ClassType) referenceType).superclass();
                String className = this.myClassName.getName(context.getVirtualMachineProxy());
                if (referenceType == null || (className != null &&
                        !className.equals(referenceType.name()))) {
                    referenceType = context.getVirtualMachineProxy().classesByName(className).get(0);
                }
            }

            Method jdiMethod = null;
            if (signature == null) {


                List<Method> matchingMethods = ((StreamEx) StreamEx.of(referenceType.methodsByName(this.myMethodName)).filter(m -> (m.argumentTypeNames().size() == args.size()))).toList();
                if (matchingMethods.size() == 1) {
                    jdiMethod = matchingMethods.get(0);
                } else if (matchingMethods.size() > 1) {


                    jdiMethod = matchingMethods.stream().filter(m -> matchArgs(m, args)).findFirst().orElse(null);
                }
            }
            if (jdiMethod == null) {
                jdiMethod = DebuggerUtils.findMethod(referenceType, this.myMethodName, signature);
            }
            if (jdiMethod == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.no.instance.method", this.myMethodName));
            }

            if (this.myMustBeVararg && !jdiMethod.isVarArgs()) {


                wrapVarargParams(jdiMethod, args);
            }
            if (signature == null) {
                argsConversions(jdiMethod, args, context);
            }


            if (isInvokableType(object)) {
                if (isInvokableType(referenceType) &&
                        jdiMethod.isStatic()) {
                    if (referenceType instanceof ClassType) {
                        return EvaluatorUtil.invokeMethod(context, (ClassType) referenceType, jdiMethod, args);
                    }

                    return EvaluatorUtil.invokeMethod(context, (InterfaceType) referenceType, jdiMethod, args);
                }


                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.no.static.method", DebuggerUtilsEx.methodName(referenceType
                                .name(), this.myMethodName, signature)));
            }

            ObjectReference objRef = (ObjectReference) object;

            if (requiresSuperObject) {
                return EvaluatorUtil.invokeInstanceMethod(context, objRef, jdiMethod, args, 2);
            }


            if (!SystemInfo.isJavaVersionAtLeast(8, 0, 45) && this.myCheckDefaultInterfaceMethod && jdiMethod

                    .declaringType() instanceof InterfaceType) {
                try {
                    return invokeDefaultMethod(context, objRef, this.myMethodName);
                } catch (EvaluateException e) {
                    logger.info("failed", e);
                }
            }
            return EvaluatorUtil.invokeInstanceMethod(context, objRef, jdiMethod, args);
        } catch (Exception e) {
            logger.debug("failed", e);
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }

    public String toString() {
        return "call " + this.myMethodName;
    }
}


