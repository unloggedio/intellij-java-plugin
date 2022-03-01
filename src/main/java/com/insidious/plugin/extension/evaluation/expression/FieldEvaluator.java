package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.descriptor.InsidiousFieldDescriptorImpl;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class FieldEvaluator
        implements Evaluator {
    private final Evaluator myObjectEvaluator;
    private final TargetClassFilter myTargetClassFilter;
    private final String myFieldName;
    private Object myEvaluatedQualifier;
    private Field myEvaluatedField;

    public FieldEvaluator(Evaluator objectEvaluator, TargetClassFilter filter, @NonNls String fieldName) {
        this.myObjectEvaluator = objectEvaluator;
        this.myFieldName = fieldName;
        this.myTargetClassFilter = filter;
    }

    @NotNull
    public static TargetClassFilter createClassFilter(@Nullable PsiType psiType) {
        if (psiType == null || psiType instanceof com.intellij.psi.PsiArrayType) {
            return TargetClassFilter.ALL;
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if (psiClass != null) {
            return createClassFilter(psiClass);
        }
        return new FQNameClassFilter(psiType.getCanonicalText());
    }

    public static TargetClassFilter createClassFilter(PsiClass psiClass) {
        if (psiClass instanceof com.intellij.psi.PsiAnonymousClass) {
            return TargetClassFilter.ALL;
        }
        if (PsiUtil.isLocalClass(psiClass)) {
            return new LocalClassFilter(psiClass.getName());
        }
        String name = JVMNameUtil.getNonAnonymousClassName(psiClass);
        return (name != null) ? new FQNameClassFilter(name) : TargetClassFilter.ALL;
    }

    @Nullable
    private Field findField(@Nullable Type t) {
        if (t instanceof ClassType) {
            ClassType cls = (ClassType) t;
            if (this.myTargetClassFilter.acceptClass(cls)) {
                return cls.fieldByName(this.myFieldName);
            }
            for (InterfaceType interfaceType : cls.interfaces()) {
                Field field = findField(interfaceType);
                if (field != null) {
                    return field;
                }
            }
            return findField(cls.superclass());
        }
        if (t instanceof InterfaceType) {
            InterfaceType iface = (InterfaceType) t;
            if (this.myTargetClassFilter.acceptClass(iface)) {
                return iface.fieldByName(this.myFieldName);
            }
            for (InterfaceType interfaceType : iface.superinterfaces()) {
                Field field = findField(interfaceType);
                if (field != null) {
                    return field;
                }
            }
        }
        return null;
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        this.myEvaluatedField = null;
        this.myEvaluatedQualifier = null;
        Object object = this.myObjectEvaluator.evaluate(context);

        return evaluateField(object, context);
    }

    private Object evaluateField(Object object, EvaluationContext context) throws EvaluateException {
        if (object instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) object;
            Field field = findField(refType);
            if (field == null || !field.isStatic()) {
                field = refType.fieldByName(this.myFieldName);
            }
            if (field == null || !field.isStatic()) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.no.static.field", this.myFieldName));
            }
            this.myEvaluatedField = field;
            this.myEvaluatedQualifier = refType;
            return refType.getValue(field);
        }

        if (object instanceof ObjectReference) {
            ObjectReference objRef = (ObjectReference) object;
            ReferenceType refType = objRef.referenceType();
            if (!(refType instanceof ClassType) && !(refType instanceof com.sun.jdi.ArrayType)) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.class.or.array.expected", this.myFieldName));
            }


            if (objRef instanceof ArrayReference && "length".equals(this.myFieldName)) {
                VirtualMachine vm = context.getVirtualMachineProxy().getVirtualMachine();
                return vm.mirrorOf(((ArrayReference) objRef).length());
            }

            Field field = findField(refType);
            if (field == null) {
                field = refType.fieldByName(this.myFieldName);
            }

            if (field == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.no.instance.field", this.myFieldName));
            }
            this.myEvaluatedQualifier = field.isStatic() ? refType : objRef;
            this.myEvaluatedField = field;
            return field.isStatic() ? refType.getValue(field) : objRef.getValue(field);
        }

        if (object == null) {
            throw EvaluateExceptionUtil.createEvaluateException(new NullPointerException());
        }

        throw EvaluateExceptionUtil.createEvaluateException(
                DebuggerBundle.message("evaluation.error.evaluating.field", this.myFieldName));
    }

    public Modifier getModifier() {
        Modifier modifier = null;
        if (this.myEvaluatedField != null && (this.myEvaluatedQualifier instanceof ClassType || this.myEvaluatedQualifier instanceof ObjectReference)) {

            modifier = new Modifier() {
                public boolean canInspect() {
                    return FieldEvaluator.this.myEvaluatedQualifier instanceof ObjectReference;
                }


                public boolean canSetValue() {
                    return true;
                }


                public void setValue(Value value) throws ClassNotLoadedException, InvalidTypeException {
                    if (FieldEvaluator.this.myEvaluatedQualifier instanceof ReferenceType) {
                        ClassType classType = (ClassType) FieldEvaluator.this.myEvaluatedQualifier;
                        classType.setValue(FieldEvaluator.this.myEvaluatedField, value);
                    } else {
                        ObjectReference objRef = (ObjectReference) FieldEvaluator.this.myEvaluatedQualifier;
                        objRef.setValue(FieldEvaluator.this.myEvaluatedField, value);
                    }
                }


                public Type getExpectedType() throws ClassNotLoadedException {
                    return FieldEvaluator.this.myEvaluatedField.type();
                }


                public InsidiousNodeDescriptorImpl getInspectItem(Project project) {
                    if (FieldEvaluator.this.myEvaluatedQualifier instanceof ObjectReference)
                        return new InsidiousFieldDescriptorImpl(project,
                                (ObjectReference) FieldEvaluator.this.myEvaluatedQualifier,
                                FieldEvaluator.this.myEvaluatedField);
                    return null;
                }
            };
        }
        return modifier;
    }

    public String toString() {
        return "field " + this.myFieldName;
    }


    public interface TargetClassFilter {
        TargetClassFilter ALL = refType -> true;

        boolean acceptClass(ReferenceType param1ReferenceType);
    }

    private static final class FQNameClassFilter implements TargetClassFilter {
        private final String myQName;

        private FQNameClassFilter(String qName) {
            this.myQName = qName;
        }


        public boolean acceptClass(ReferenceType refType) {
            return refType.name().equals(this.myQName);
        }
    }

    private static final class LocalClassFilter implements TargetClassFilter {
        private final String myLocalClassShortName;

        private LocalClassFilter(String localClassShortName) {
            this.myLocalClassShortName = localClassShortName;
        }


        public boolean acceptClass(ReferenceType refType) {
            String name = refType.name();
            int index = name.lastIndexOf(this.myLocalClassShortName);
            if (index < 0) {
                return false;
            }
            for (int idx = index - 1; idx >= 0; idx--) {
                char ch = name.charAt(idx);
                if (ch == '$') {
                    return (idx < index - 1);
                }
                if (!Character.isDigit(ch)) {
                    return false;
                }
            }
            return false;
        }
    }
}


