package com.insidious.plugin.extension.connector;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.MultiMap;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class LocalVariablesUtil {
    private static final Logger logger = LoggerUtil.getInstance(LocalVariablesUtil.class);

    private static final boolean ourInitializationOk;
    private static final boolean ourInitializationOkSet;
    private static Class<?> ourSlotInfoClass;
    private static Constructor<?> slotInfoConstructor;
    private static java.lang.reflect.Method ourEnqueueMethod;
    private static java.lang.reflect.Method ourWaitForReplyMethod;
    private static Class<?> ourSlotInfoClassSet;
    private static Constructor<?> slotInfoConstructorSet;
    private static java.lang.reflect.Method ourEnqueueMethodSet;
    private static java.lang.reflect.Method ourWaitForReplyMethodSet;

    static {
        boolean success = false;
        try {
            String GetValuesClassName = "com.sun.tools.jdi.JDWP$StackFrame$GetValues";
            ourSlotInfoClass = Class.forName(GetValuesClassName + "$SlotInfo");
            slotInfoConstructor = ourSlotInfoClass.getDeclaredConstructor(int.class, byte.class);
            slotInfoConstructor.setAccessible(true);

            Class<?> ourGetValuesClass = Class.forName(GetValuesClassName);
            ourEnqueueMethod = getDeclaredMethodByName(ourGetValuesClass, "enqueueCommand");
            ourWaitForReplyMethod = getDeclaredMethodByName(ourGetValuesClass, "waitForReply");

            success = true;
        } catch (Throwable e) {
            logger.info("failed", e);
        }
        ourInitializationOk = success;


        success = false;
        try {
            String setValuesClassName = "com.sun.tools.jdi.JDWP$StackFrame$SetValues";
            ourSlotInfoClassSet = Class.forName(setValuesClassName + "$SlotInfo");
            slotInfoConstructorSet = ourSlotInfoClassSet.getDeclaredConstructors()[0];
            slotInfoConstructorSet.setAccessible(true);

            Class<?> ourGetValuesClassSet = Class.forName(setValuesClassName);
            ourEnqueueMethodSet = getDeclaredMethodByName(ourGetValuesClassSet, "enqueueCommand");

            ourWaitForReplyMethodSet = getDeclaredMethodByName(ourGetValuesClassSet, "waitForReply");

            success = true;
        } catch (Throwable e) {
            logger.info("failed", e);
        }
        ourInitializationOkSet = success;
    }


    public static Map<DecompiledLocalVariable, Value> fetchValues(@NotNull StackFrameProxy frameProxy, SourcePosition position, boolean full) throws Exception {
        Map<DecompiledLocalVariable, Value> map = new LinkedHashMap<>();


        Location location = frameProxy.location();
        Method method = location.method();
        int firstLocalVariableSlot = getFirstLocalsSlot(method);


        MultiMap<Integer, String> namesMap = full ? calcNames(position, firstLocalVariableSlot) : MultiMap.empty();


        int slot = getFirstArgsSlot(method);
        List<String> typeNames = method.argumentTypeNames();
        List<Value> argValues = frameProxy.getStackFrame().getArgumentValues();
        for (int i = 0; i < argValues.size(); i++) {
            map.put(new DecompiledLocalVariable(slot, true, null, namesMap
                    .get(Integer.valueOf(slot))), argValues
                    .get(i));
            slot += getTypeSlotSize(typeNames.get(i));
        }

        if (!full || !ourInitializationOk) {
            return map;
        }


        List<DecompiledLocalVariable> vars = collectVariablesFromBytecode(frameProxy.getVirtualMachine(), location, namesMap);
        StackFrame frame = frameProxy.getStackFrame();
        int size = vars.size();
        while (size > 0) {
            try {
                return fetchSlotValues(map, vars.subList(0, size), frame);
            } catch (Exception e) {
                logger.debug("failed", e);

                size--;
            }
        }
        return map;
    }


    private static Map<DecompiledLocalVariable, Value> fetchSlotValues(Map<DecompiledLocalVariable, Value> map, List<? extends DecompiledLocalVariable> vars, StackFrame frame) throws Exception {
        Object ps;
        Long frameId = ReflectionUtil.getField(frame.getClass(), frame, long.class, "id");
        VirtualMachine vm = frame.virtualMachine();
        java.lang.reflect.Method stateMethod = vm.getClass().getDeclaredMethod("state");
        stateMethod.setAccessible(true);

        Object slotInfoArray = createSlotInfoArray(vars);


        Object vmState = stateMethod.invoke(vm);
        synchronized (vmState) {
            ps = ourEnqueueMethod.invoke(null, vm, frame.thread(), frameId, slotInfoArray);
        }

        Object reply = ourWaitForReplyMethod.invoke(null, vm, ps);

        Value[] values = ReflectionUtil.getField(reply.getClass(), reply, Value[].class, "values");
        if (vars.size() != values.length) {
            throw new InternalException("Wrong number of values returned from target VM");
        }
        int idx = 0;
        for (DecompiledLocalVariable var : vars) {
            map.put(var, values[idx++]);
        }
        return map;
    }

    public static boolean canSetValues() {
        return ourInitializationOkSet;
    }

    public static void setValue(StackFrame frame, int slot, Value value) throws EvaluateException {
        new Exception().printStackTrace();
//        try {
//            Object ps;
//            Long frameId = ReflectionUtil.getField(frame.getClass(), frame, long.class, "id");
//            VirtualMachine vm = frame.virtualMachine();
//            java.lang.reflect.Method stateMethod = vm.getClass().getDeclaredMethod("state");
//            stateMethod.setAccessible(true);
//
//            Object slotInfoArray = createSlotInfoArraySet(slot, value);
//
//
//            Object vmState = stateMethod.invoke(vm);
//            synchronized (vmState) {
//                ps = ourEnqueueMethodSet.invoke(null, new Object[]{vm, frame.thread(), frameId, slotInfoArray});
//            }
//
//            ourWaitForReplyMethodSet.invoke(null, new Object[]{vm, ps});
//        } catch (Exception e) {
//            throw new EvaluateException("Unable to set value", e);
//        }
    }


    private static Object createSlotInfoArraySet(int slot, Value value) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Object arrayInstance = Array.newInstance(ourSlotInfoClassSet, 1);
        Array.set(arrayInstance, 0, slotInfoConstructorSet.newInstance(Integer.valueOf(slot), value));
        return arrayInstance;
    }


    private static Object createSlotInfoArray(Collection<? extends DecompiledLocalVariable> vars) throws Exception {
        Object arrayInstance = Array.newInstance(ourSlotInfoClass, vars.size());

        int idx = 0;
        for (DecompiledLocalVariable var : vars) {

            Object info = slotInfoConstructor.newInstance(Integer.valueOf(var.getSlot()), Byte.valueOf((byte) var.getSignature().charAt(0)));
            Array.set(arrayInstance, idx++, info);
        }

        return arrayInstance;
    }


    private static java.lang.reflect.Method getDeclaredMethodByName(Class<?> aClass, String methodName) throws NoSuchMethodException {
//        for (Method method : aClass.getDeclaredMethods()) {
//            if (methodName.equals(method.getName())) {
//                method.setAccessible(true);
//                return method;
//            }
//        }
        throw new NoSuchMethodException(aClass.getName() + "." + methodName);
    }


    @NotNull
    private static List<DecompiledLocalVariable> collectVariablesFromBytecode(VirtualMachineProxy vm, Location location, final MultiMap<Integer, String> namesMap) {
//        if (!vm.canGetBytecodes()) {
        return Collections.emptyList();
//        }

//        try {
//            LOG.assertTrue((location != null));
//            Method method = location.method();
//            Location methodLocation = method.location();
//            if (methodLocation == null || methodLocation.codeIndex() < 0L) {
//
//                return Collections.emptyList();
//            }
//
//            long codeIndex = location.codeIndex();
//            if (codeIndex > 0L) {
//                byte[] bytecodes = method.bytecodes();
//                if (bytecodes != null && bytecodes.length > 0) {
//                    final int firstLocalVariableSlot = getFirstLocalsSlot(method);
//                    final HashMap<Integer, DecompiledLocalVariable> usedVars = new HashMap<>();
//                    MethodBytecodeUtil.visit(method, codeIndex, new MethodVisitor(589824) {
//
//
//                        public void visitVarInsn(int opcode, int slot) {
//                            if (slot >= firstLocalVariableSlot) {
//                                DecompiledLocalVariable variable = usedVars.get(Integer.valueOf(slot));
//
//
//                                String typeSignature = MethodBytecodeUtil.getVarInstructionType(opcode).getDescriptor();
//                                if (variable == null ||
//                                        !typeSignature.equals(variable.getSignature())) {
//
//
//                                    variable = new DecompiledLocalVariable(slot, false, typeSignature, namesMap.get(Integer.valueOf(slot)));
//                                    usedVars.put(Integer.valueOf(slot), variable);
//                                }
//                            }
//                        }
//                    } false)
//
//                    if (usedVars.isEmpty()) {
//                        if (Collections.emptyList() == null) $$$reportNull$$$0(3);
//                        return Collections.emptyList();
//                    }
//
//                    List<DecompiledLocalVariable> vars = new ArrayList<>(usedVars.values());
//                    vars.sort(Comparator.comparingInt(DecompiledLocalVariable::getSlot));
//                    if (vars == null) $$$reportNull$$$0(4);
//                    return vars;
//                }
//            }
//        } catch (UnsupportedOperationException unsupportedOperationException) {
//        } catch (VMDisconnectedException e) {
//            throw e;
//        } catch (Exception e) {
//            LOG.debug(e);
//        }

//        return Collections.emptyList();
    }


    @NotNull
    private static MultiMap<Integer, String> calcNames(@NotNull SourcePosition position, int firstLocalsSlot) {
        if (position != null) {
            return ReadAction.compute(() -> {
                PsiElement element = position.getElementAt();
                PsiElement method = DebuggerUtilsEx.getContainingMethod(element);
                if (method != null) {
                    MultiMap<Integer, String> res = new MultiMap();
                    int slot = Math.max(0, firstLocalsSlot - getParametersStackSize(method));
                    for (PsiParameter parameter : DebuggerUtilsEx.getParameters(method)) {
                        res.putValue(Integer.valueOf(slot), parameter.getName());
                        slot += getTypeSlotSize(parameter.getType());
                    }
                    PsiElement body = DebuggerUtilsEx.getBody(method);
                    if (body != null) try {
                        body.accept(new LocalVariableNameFinder(firstLocalsSlot, res, element));
                    } catch (Exception e) {
                        logger.info("failed", e);
                    }


                    return res;
                }

                return MultiMap.empty();
            });
        }
        return MultiMap.empty();
    }

    private static int getParametersStackSize(PsiElement method) {
        return Arrays.stream(DebuggerUtilsEx.getParameters(method))
                .mapToInt(parameter -> getTypeSlotSize(parameter.getType()))
                .sum();
    }

    private static int getTypeSlotSize(PsiType varType) {
        if (PsiType.DOUBLE.equals(varType) || PsiType.LONG.equals(varType)) {
            return 2;
        }
        return 1;
    }

    private static int getFirstArgsSlot(Method method) {
        return method.isStatic() ? 0 : 1;
    }

    private static int getFirstLocalsSlot(Method method) {
        return getFirstArgsSlot(method) + method
                .argumentTypeNames().stream()
                .mapToInt(LocalVariablesUtil::getTypeSlotSize)
                .sum();
    }

    private static int getTypeSlotSize(String name) {
        if ("double".equals(name) || "long".equals(name)) {
            return 2;
        }
        return 1;
    }

    private static class LocalVariableNameFinder
            extends JavaRecursiveElementVisitor {
        private final MultiMap<Integer, String> myNames;
        private final PsiElement myElement;
        private final Deque<Integer> myIndexStack = new LinkedList<>();
        private int myCurrentSlotIndex;
        private boolean myReached = false;

        LocalVariableNameFinder(int startSlot, MultiMap<Integer, String> names, PsiElement element) {
            this.myNames = names;
            this.myCurrentSlotIndex = startSlot;
            this.myElement = element;
        }

        private boolean shouldVisit(PsiElement scope) {
            return (!this.myReached && PsiTreeUtil.isContextAncestor(scope, this.myElement, false));
        }


        public void visitElement(@NotNull PsiElement element) {
            if (element == this.myElement) {
                this.myReached = true;
            } else {
                super.visitElement(element);
            }
        }


        public void visitLocalVariable(PsiLocalVariable variable) {
            super.visitLocalVariable(variable);
            if (!this.myReached) {
                appendName(variable);
            }
        }


        public void visitSynchronizedStatement(PsiSynchronizedStatement statement) {
            if (shouldVisit(statement)) {
                this.myIndexStack.push(Integer.valueOf(this.myCurrentSlotIndex));
                try {
                    appendName("<monitor>", 1);
                    super.visitSynchronizedStatement(statement);
                } finally {
                    this.myCurrentSlotIndex = this.myIndexStack.pop().intValue();
                }
            }
        }

        private void appendName(@Nullable PsiVariable variable) {
            if (variable != null) {
                appendName(variable.getName(), LocalVariablesUtil.getTypeSlotSize(variable.getType()));
            }
        }

        private void appendName(String varName, int size) {
            this.myNames.putValue(Integer.valueOf(this.myCurrentSlotIndex), varName);
            this.myCurrentSlotIndex += size;
        }


        public void visitCodeBlock(PsiCodeBlock block) {
            if (shouldVisit(block)) {
                this.myIndexStack.push(Integer.valueOf(this.myCurrentSlotIndex));
                try {
                    super.visitCodeBlock(block);
                } finally {
                    this.myCurrentSlotIndex = this.myIndexStack.pop().intValue();
                }
            }
        }


        public void visitForStatement(PsiForStatement statement) {
            if (shouldVisit(statement)) {
                this.myIndexStack.push(Integer.valueOf(this.myCurrentSlotIndex));
                try {
                    super.visitForStatement(statement);
                } finally {
                    this.myCurrentSlotIndex = this.myIndexStack.pop().intValue();
                }
            }
        }


        public void visitForeachStatement(PsiForeachStatement statement) {
            if (shouldVisit(statement)) {
                this.myIndexStack.push(Integer.valueOf(this.myCurrentSlotIndex));
                try {
                    PsiExpression value = statement.getIteratedValue();
                    if (value != null && value.getType() instanceof com.intellij.psi.PsiArrayType) {
                        appendName("", 1);
                        appendName("<length>", 1);
                        appendName("<index>", 1);
                    } else {
                        appendName("<iterator>", 1);
                    }
                    appendName(statement.getIterationParameter());
                    super.visitForeachStatement(statement);
                } finally {
                    this.myCurrentSlotIndex = this.myIndexStack.pop().intValue();
                }
            }
        }


        public void visitCatchSection(PsiCatchSection section) {
            if (shouldVisit(section)) {
                this.myIndexStack.push(Integer.valueOf(this.myCurrentSlotIndex));
                try {
                    appendName(section.getParameter());
                    super.visitCatchSection(section);
                } finally {
                    this.myCurrentSlotIndex = this.myIndexStack.pop().intValue();
                }
            }
        }


        public void visitResourceList(PsiResourceList resourceList) {
            if (shouldVisit(resourceList)) {
                this.myIndexStack.push(Integer.valueOf(this.myCurrentSlotIndex));
                try {
                    super.visitResourceList(resourceList);
                } finally {
                    this.myCurrentSlotIndex = this.myIndexStack.pop().intValue();
                }
            }
        }


        public void visitClass(PsiClass aClass) {
        }
    }
}

