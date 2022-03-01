package com.insidious.plugin.extension.evaluation;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.debugger.engine.evaluation.CodeFragmentFactory;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XStackFrame;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.evaluation.expression.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EvaluatorUtil {
    private static final Logger logger = LoggerUtil.getInstance(EvaluatorUtil.class);


    public static String getValueAsString(EvaluationContext evaluationContext, Value value) throws EvaluateException {
        try {
            if (value == null) {
                return "null";
            }
            if (value instanceof StringReference) {
                return ((StringReference) value).value();
            }
            if (isInteger(value)) {
                return String.valueOf(((PrimitiveValue) value).longValue());
            }
            if (value instanceof FloatValue) {
                return String.valueOf(((FloatValue) value).floatValue());
            }
            if (value instanceof DoubleValue) {
                return String.valueOf(((DoubleValue) value).doubleValue());
            }
            if (value instanceof BooleanValue) {
                return String.valueOf(((PrimitiveValue) value).booleanValue());
            }
            if (value instanceof com.sun.jdi.CharValue) {
                return String.valueOf(((PrimitiveValue) value).charValue());
            }
            if (value instanceof ObjectReference) {
                if (value instanceof ArrayReference) {
                    StringJoiner joiner = new StringJoiner(",", "[", "]");
                    for (Value element : ((ArrayReference) value).getValues()) {
                        joiner.add(getValueAsString(evaluationContext, element));
                    }
                    return joiner.toString();
                }

                ObjectReference objRef = (ObjectReference) value;
                Method toStringMethod = null;
                if (toStringMethod == null)
                    try {
                        ReferenceType refType = getObjectClassType(objRef.virtualMachine());
                        toStringMethod = findMethod(refType, "toString", "()Ljava/lang/String;");
                    } catch (Exception ignored) {
                        throw EvaluateExceptionUtil.createEvaluateException(
                                DebuggerBundle.message("evaluation.error.cannot.evaluate.tostring", objRef.referenceType().name()));
                    }
                if (toStringMethod == null)
                    throw EvaluateExceptionUtil.createEvaluateException(
                            DebuggerBundle.message("evaluation.error.cannot.evaluate.tostring", objRef.referenceType().name()));
                Method finalToStringMethod = toStringMethod;

                InsidiousXSuspendContext suspendContext = (InsidiousXSuspendContext) evaluationContext.getXSuspendContext();

                Value result = objRef.invokeMethod(suspendContext.getThreadReferenceProxy().getThreadReference(), finalToStringMethod,

                        Collections.emptyList(), 1);

                if (result == null) {
                    return "null";
                }
                return (result instanceof StringReference) ? (
                        (StringReference) result).value() :
                        result.toString();
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.unsupported.expression.type"));
        } catch (ObjectCollectedException ignored) {
            throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
        } catch (IncompatibleThreadStateException | InvalidTypeException | ClassNotLoadedException | InvocationException ex) {


            throw new EvaluateException(ex.getMessage(), ex);
        }
    }

    public static boolean isInteger(Value value) {
        return (value instanceof com.sun.jdi.ByteValue || value instanceof com.sun.jdi.ShortValue || value instanceof com.sun.jdi.LongValue || value instanceof com.sun.jdi.IntegerValue);
    }


    private static ReferenceType getObjectClassType(VirtualMachine virtualMachine) {
        return ContainerUtil.getFirstItem(virtualMachine
                .classesByName("java.lang.Object"));
    }


    public static Method findMethod(@NotNull ReferenceType refType, @NonNls String methodName, @Nullable @NonNls String methodSignature) {
        if (refType instanceof ArrayType) {


            Method method1 = findMethod(
                    getObjectClassType(refType.virtualMachine()), methodName, methodSignature);


            if (method1 != null) {
                return method1;
            }


            if ("clone".equals(methodName) && "()[Ljava/lang/Object;".equals(methodSignature)) {
                method1 = findMethod(getObjectClassType(refType.virtualMachine()), "clone", null);
                if (method1 != null) {
                    return method1;
                }
            }
        }

        Method method = null;

        if (refType instanceof ClassType) {
            method = concreteMethodByName((ClassType) refType, methodName, methodSignature);
        }
        if (method == null) {
            method = ContainerUtil.getFirstItem(
                    (methodSignature != null) ?
                            refType.methodsByName(methodName, methodSignature) :
                            refType.methodsByName(methodName));
        }
        return method;
    }


    private static Method concreteMethodByName(@NotNull ClassType type, @NotNull String name, @Nullable String signature) {

        Processor<Method> signatureChecker = (signature != null) ? (m -> m.signature().equals(signature)) : CommonProcessors.alwaysTrue();
        LinkedList<ReferenceType> types = new LinkedList<>();

        while (type != null) {
            for (Method candidate : type.methods()) {
                if (candidate.name().equals(name) && signatureChecker.process(candidate)) {
                    return !candidate.isAbstract() ? candidate : null;
                }
            }
            types.add(type);
            type = type.superclass();
        }

        Set<ReferenceType> checkedInterfaces = new HashSet<>();
        ReferenceType t;
        while ((t = types.poll()) != null) {
            if (t instanceof ClassType) {
                types.addAll(0, ((ClassType) t).interfaces());
                continue;
            }
            if (t instanceof InterfaceType && checkedInterfaces.add(t)) {
                for (Method candidate : t.methods()) {
                    if (candidate.name().equals(name) && signatureChecker
                            .process(candidate) &&
                            !candidate.isAbstract()) {
                        return candidate;
                    }
                }
                types.addAll(0, ((InterfaceType) t).superinterfaces());
            }
        }
        return null;
    }


    public static ReferenceType loadClass(EvaluationContext evaluationContext, String qName, ClassLoaderReference classLoader) throws InvocationException, ClassNotLoadedException, IncompatibleThreadStateException, InvalidTypeException, EvaluateException {
        qName = reformatArrayName(qName);
        ReferenceType refType = null;

        VirtualMachine virtualMachine = evaluationContext.getVirtualMachineProxy().getVirtualMachine();


        ClassType classClassType = (ClassType) ContainerUtil.getFirstItem(virtualMachine
                .classesByName("java.lang.Class"));
        if (classClassType != null) {
            Method forNameMethod;
            List<Value> args = new ArrayList<>();


            args.add(virtualMachine.mirrorOf(qName));
            if (classLoader != null) {


                forNameMethod = DebuggerUtils.findMethod(classClassType, "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");


                args.add(virtualMachine.mirrorOf(true));
                args.add(classLoader);

            } else {

                forNameMethod = DebuggerUtils.findMethod(classClassType, "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
            }


            ThreadReference thread = evaluationContext.getStackFrameProxy().threadProxy().getThreadReference();

            Value classReference = classClassType.invokeMethod(thread, forNameMethod, args, 1);

            if (classReference instanceof ClassObjectReference) {
                refType = ((ClassObjectReference) classReference).reflectedType();
            }
        }
        return refType;
    }

    private static String reformatArrayName(String className) {
        if (className.indexOf('[') == -1) return className;

        int dims = 0;
        while (className.endsWith("[]")) {
            className = className.substring(0, className.length() - 2);
            dims++;
        }

        StringBuilder buffer = new StringBuilder();
        StringUtil.repeatSymbol(buffer, '[', dims);
        String primitiveSignature = JVMNameUtil.getPrimitiveSignature(className);
        if (primitiveSignature != null) {
            buffer.append(primitiveSignature);
        } else {
            buffer.append('L');
            buffer.append(className);
            buffer.append(';');
        }
        return buffer.toString();
    }

    public static Value createValue(VirtualMachine vm, String expectedType, double value) {
        if (PsiType.DOUBLE.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.FLOAT.getName().equals(expectedType)) {
            return vm.mirrorOf((float) value);
        }
        if (PsiType.LONG.getName().equals(expectedType)) {
            return vm.mirrorOf((long) value);
        }
        if (PsiType.INT.getName().equals(expectedType)) {
            return vm.mirrorOf((int) value);
        }
        if (PsiType.SHORT.getName().equals(expectedType)) {
            return vm.mirrorOf((short) (int) value);
        }
        if (PsiType.BYTE.getName().equals(expectedType)) {
            return vm.mirrorOf((byte) (int) value);
        }
        if (PsiType.CHAR.getName().equals(expectedType)) {
            return vm.mirrorOf((char) (int) value);
        }
        return null;
    }

    public static Value createValue(VirtualMachine vm, String expectedType, long value) {
        if (PsiType.LONG.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.INT.getName().equals(expectedType)) {
            return vm.mirrorOf((int) value);
        }
        if (PsiType.SHORT.getName().equals(expectedType)) {
            return vm.mirrorOf((short) (int) value);
        }
        if (PsiType.BYTE.getName().equals(expectedType)) {
            return vm.mirrorOf((byte) (int) value);
        }
        if (PsiType.CHAR.getName().equals(expectedType)) {
            return vm.mirrorOf((char) (int) value);
        }
        if (PsiType.DOUBLE.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.FLOAT.getName().equals(expectedType)) {
            return vm.mirrorOf((float) value);
        }
        return null;
    }

    public static Value createValue(VirtualMachine vm, String expectedType, boolean value) {
        if (PsiType.BOOLEAN.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        return null;
    }

    public static Value createValue(VirtualMachine vm, String expectedType, char value) {
        if (PsiType.CHAR.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.LONG.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.INT.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.SHORT.getName().equals(expectedType)) {
            return vm.mirrorOf((short) value);
        }
        if (PsiType.BYTE.getName().equals(expectedType)) {
            return vm.mirrorOf((byte) value);
        }
        if (PsiType.DOUBLE.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        if (PsiType.FLOAT.getName().equals(expectedType)) {
            return vm.mirrorOf(value);
        }
        return null;
    }


    public static Value invokeMethod(EvaluationContext evaluationContext, ClassType classType, Method method, List<? extends Value> args) throws EvaluateException {
        ThreadReference thread = evaluationContext.getStackFrameProxy().threadProxy().getThreadReference();
        try {
            return classType.invokeMethod(thread, method, args, 1);
        } catch (InvalidTypeException | InvocationException | IncompatibleThreadStateException | ClassNotLoadedException e) {


            throw new EvaluateException(e.getMessage(), e);
        }
    }


    public static Value invokeMethod(EvaluationContext evaluationContext, InterfaceType classType, Method method, List<? extends Value> args) throws EvaluateException {
        ThreadReference thread = evaluationContext.getStackFrameProxy().threadProxy().getThreadReference();
        try {
            return classType.invokeMethod(thread, method, args, 1);
        } catch (InvalidTypeException | InvocationException | IncompatibleThreadStateException | ClassNotLoadedException e) {


            throw new EvaluateException(e.getMessage(), e);
        }
    }


    public static Value invokeInstanceMethod(EvaluationContext evaluationContext, ObjectReference instance, Method method, List<? extends Value> args) throws EvaluateException {
        return invokeInstanceMethod(evaluationContext, instance, method, args, 1);
    }


    public static Value invokeInstanceMethod(EvaluationContext evaluationContext, ObjectReference instance, Method method, List<? extends Value> args, int options) throws EvaluateException {
        ThreadReference thread = evaluationContext.getStackFrameProxy().threadProxy().getThreadReference();
        try {
            return instance.invokeMethod(thread, method, args, options);
        } catch (InvalidTypeException | InvocationException | IncompatibleThreadStateException | ClassNotLoadedException e) {


            throw new EvaluateException(e.getMessage(), e);
        }
    }


    public static ReferenceType findClass(@Nullable EvaluationContext evaluationContext, String className, ClassLoaderReference classLoader) throws EvaluateException {
        try {
            ReferenceType result = null;

            List<ReferenceType> types = ContainerUtil.filter(evaluationContext
                    .getVirtualMachineProxy().classesByName(className), ReferenceType::isPrepared);


            result = ContainerUtil.find(types, refType -> Objects.equals(classLoader, refType.classLoader()));


            if (result == null && classLoader != null) {
                result = ContainerUtil.find(types, refType -> isVisibleFromClassLoader(classLoader, refType));
            }

            if (result == null && evaluationContext != null) {
                EvaluationContext evalContext = evaluationContext;
                if (evalContext.isAutoLoadClasses()) {
                    return loadClass(evalContext, className, classLoader);
                }
            }
            return result;
        } catch (InvocationException | InvalidTypeException | IncompatibleThreadStateException | ClassNotLoadedException e) {


            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }


    private static boolean isVisibleFromClassLoader(@NotNull ClassLoaderReference fromLoader, ReferenceType refType) {
        return fromLoader.visibleClasses().contains(refType);
    }


    public static ArrayReference newArrayInstance(ArrayType arrayType, int dimension) throws EvaluateException {
        try {
            return arrayType.newInstance(dimension);
        } catch (Exception e) {
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }


    public static void ensureNotInsideObjectConstructor(@NotNull ObjectReference reference, EvaluationContext context) throws EvaluateException {
        if (context != null) {
            Location location = context.getStackFrameProxy().location();
            if (location != null && location
                    .method().isConstructor() && reference
                    .equals(context.computeThisObject())) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.object.is.being.initialized"));
            }
        }
    }


    public static SourcePosition getTopFrameSourcePosition(EvaluationContext evaluationContext) {
        XStackFrame topFrame = evaluationContext.getXSuspendContext().getActiveExecutionStack().getTopFrame();
        return getSourcePosition(topFrame, evaluationContext);
    }


    public static SourcePosition getSourcePosition(XStackFrame xstackFrame, EvaluationContext evaluationContext) {
        try {
            XSourcePosition xPosition = xstackFrame.getSourcePosition();

            SourcePosition position = DebuggerUtilsEx.toSourcePosition(xPosition, evaluationContext


                    .getVirtualMachineProxy()
                    .getXDebugProcess()
                    .getSession()
                    .getProject());
            return position;
        } catch (Exception ex) {
            return null;
        }
    }

    public static PsiElement getContextElement(EvaluationContext evaluationContext) {
        SourcePosition position = getTopFrameSourcePosition(evaluationContext);
        if (position != null) {
            return position.getElementAt();
        }
        return null;
    }

    public static Project getProject(EvaluationContext evaluationContext) {
        return evaluationContext
                .getVirtualMachineProxy()
                .getXDebugProcess()
                .getSession()
                .getProject();
    }


    public static ExpressionEvaluator getExpressionEvaluator(EvaluationContext evaluationContext, TextWithImports expression) throws EvaluateException {
        ExpressionEvaluator evaluator = (ExpressionEvaluator) ReadAction.compute(() -> {
            SourcePosition position = getTopFrameSourcePosition(evaluationContext);

            PsiElement context = getContextElement(evaluationContext);

            CodeFragmentFactory codeFragmentFactory = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(expression, context);

            JavaCodeFragment code = codeFragmentFactory.createCodeFragment(expression, context, getProject(evaluationContext));

            EvaluatorBuilder evaluatorBuilder = EvaluatorBuilderImpl.getInstance();

            return evaluatorBuilder.build(code, position, evaluationContext);
        });
        return evaluator;
    }


    public static boolean evaluateBoolean(ExpressionEvaluator evaluator, EvaluationContext context) throws EvaluateException {
        Object value = UnBoxingEvaluator.unbox(evaluator.evaluate(context), context);
        if (!(value instanceof BooleanValue)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.boolean.expected"));
        }
        return ((BooleanValue) value).booleanValue();
    }


    public static Value preprocessValue(EvaluationContext context, Value value, @NotNull Type varType) throws EvaluateException {
        if (value != null && "java.lang.String"
                .equals(varType.name()) && !(value instanceof StringReference)) {

            String v = getValueAsString(context, value);
            if (v != null) {
                value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOf(v);
            }
        }
        if (value instanceof DoubleValue) {
            double dValue = ((DoubleValue) value).doubleValue();
            if (varType instanceof com.sun.jdi.FloatType && 1.401298464324817E-45D <= dValue && dValue <= 3.4028234663852886E38D) {


                value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOf((float) dValue);
            }
        }
        if (value != null) {
            if (varType instanceof com.sun.jdi.PrimitiveType) {
                if (!(value instanceof PrimitiveValue)) {
                    value = (Value) UnBoxingEvaluator.unbox(value, context);
                }
            } else if (varType instanceof ReferenceType &&
                    value instanceof PrimitiveValue) {
                value = (Value) BoxingEvaluator.box(value, context);
            }
        }

        return value;
    }
}


