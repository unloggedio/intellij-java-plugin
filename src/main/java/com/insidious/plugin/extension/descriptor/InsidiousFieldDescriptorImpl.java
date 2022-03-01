package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.settings.NodeRendererSettings;
import com.intellij.debugger.settings.ViewsGeneralSettings;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeExpression;
import com.intellij.debugger.ui.tree.FieldDescriptor;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.frame.XValueModifier;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousFieldDescriptorImpl extends InsidiousValueDescriptorImpl implements FieldDescriptor {
    public static final String OUTER_LOCAL_VAR_FIELD_PREFIX = "val$";
    private final Field myField;
    private final ObjectReference myObject;
    private final boolean myIsStatic;
    private Boolean myIsPrimitive = null;
    private Ref<Value> myPresetValue;

    public InsidiousFieldDescriptorImpl(Project project, ObjectReference objRef, @NotNull Field field) {
        super(project);
        this.myObject = objRef;
        this.myField = field;
        this.myIsStatic = field.isStatic();
        setLvalue(!field.isFinal());
    }


    public Field getField() {
        return this.myField;
    }


    public ObjectReference getObject() {
        return this.myObject;
    }


    public void setAncestor(NodeDescriptor oldDescriptor) {
        super.setAncestor(oldDescriptor);
        Boolean isPrimitive = ((InsidiousFieldDescriptorImpl) oldDescriptor).myIsPrimitive;
        if (isPrimitive != null) {
            this.myIsPrimitive = isPrimitive;
        }
    }


    public boolean isPrimitive() {
        if (this.myIsPrimitive == null) {
            Value value = getValue();
            if (value != null) {
                this.myIsPrimitive = Boolean.valueOf(super.isPrimitive());
            } else {
                this.myIsPrimitive = Boolean.valueOf(DebuggerUtils.isPrimitiveType(this.myField.typeName()));
            }
        }
        return this.myIsPrimitive.booleanValue();
    }

    public void setValue(Value value) {
        this.myPresetValue = Ref.create(value);
    }


    public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        try {
            Value fieldValue;
            if (this.myPresetValue != null) {
                fieldValue = this.myPresetValue.get();
            } else if (this.myObject != null) {
                fieldValue = this.myObject.getValue(this.myField);
            } else {
                fieldValue = this.myField.declaringType().getValue(this.myField);
            }

            if (this.myObject != null &&
                    populateExceptionStackTraceIfNeeded(fieldValue, evaluationContext)) {
                fieldValue = this.myObject.getValue(this.myField);
            }

            return fieldValue;
        } catch (InternalException e) {
            if (evaluationContext
                    .getVirtualMachineProxy()
                    .canBeModified()) {
                LOG.debug("failed", e);
            } else {
                LOG.warn("failed", e);
            }
            throw new EvaluateException(DebuggerBundle.message("internal.debugger.error"));
        } catch (ObjectCollectedException ignored) {
            throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
        }
    }


    private boolean populateExceptionStackTraceIfNeeded(Value value, EvaluationContext evaluationContext) {
        if ("stackTrace".equals(getName()) &&
                (ViewsGeneralSettings.getInstance()).POPULATE_THROWABLE_STACKTRACE && value instanceof ArrayReference && ((ArrayReference) value)

                .length() == 0 &&
                DebuggerUtils.instanceOf(this.myObject
                        .type(), "java.lang.Throwable")) {
            try {
                invokeExceptionGetStackTrace(this.myObject, evaluationContext);
                return true;
            } catch (Throwable e) {
                LOG.info("failed", e);
            }
        }
        return false;
    }

    public boolean isStatic() {
        return this.myIsStatic;
    }


    public String getName() {
        return this.myField.name();
    }


    public String calcValueName() {
        String res = super.calcValueName();
        if (Boolean.TRUE.equals(getUserData(SHOW_DECLARING_TYPE))) {
            return NodeRendererSettings.getInstance()
                    .getClassRenderer()
                    .renderTypeName(this.myField.declaringType().name()) + "." + res;
        }


        return res;
    }

    public boolean isOuterLocalVariableValue() {
        try {
            return (DebuggerUtils.isSynthetic(this.myField) && this.myField
                    .name().startsWith("val$"));
        } catch (UnsupportedOperationException ignored) {
            return false;
        }
    }


    @Nullable
    public String getDeclaredType() {
        return this.myField.typeName();
    }


    public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
        String fieldName;
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(this.myProject);

        if (isStatic()) {
            String typeName = this.myField.declaringType().name().replace('$', '.');

            typeName = DebuggerTreeNodeExpression.normalize(typeName,
                    EvaluatorUtil.getContextElement(context), this.myProject);
            fieldName = typeName + "." + getName();

        } else {


            fieldName = isOuterLocalVariableValue() ? StringUtil.trimStart(getName(), "val$") : ("this." + getName());
        }
        try {
            return elementFactory.createExpressionFromText(fieldName, null);
        } catch (IncorrectOperationException e) {
            throw new EvaluateException(
                    DebuggerBundle.message("error.invalid.field.name", getName()), e);
        }
    }


    public XValueModifier getModifier(InsidiousJavaValue value) {
        return new InsidiousJavaValueModifier(value) {
            protected void setValueImpl(@NotNull XExpression expression, @NotNull XValueModifier.XModificationCallback callback) {
                final Field field = InsidiousFieldDescriptorImpl.this.getField();
                InsidiousFieldDescriptorImpl.FieldValueSetter setter = null;

                if (!field.isStatic()) {
                    ObjectReference object = InsidiousFieldDescriptorImpl.this.getObject();
                    if (object != null) {
                        setter = (v -> object.setValue(field, v));
                    }
                } else {
                    ReferenceType refType = field.declaringType();
                    if (refType instanceof ClassType) {
                        ClassType classType = (ClassType) refType;
                        setter = (v -> classType.setValue(field, v));
                    }
                }

                if (setter != null) {
                    final InsidiousFieldDescriptorImpl.FieldValueSetter finalSetter = setter;
                    set(expression, callback, InsidiousFieldDescriptorImpl.this


                            .getProject(), new InsidiousJavaValueModifier.SetValueRunnable() {


                        public void setValue(EvaluationContext evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {
                            finalSetter.setValue(newValue);
                            InsidiousJavaValueModifier.update(evaluationContext);
                        }


                        public ClassLoaderReference getClassLoader(EvaluationContext evaluationContext) {
                            return field.declaringType().classLoader();
                        }


                        @NotNull
                        public Type getLType() throws ClassNotLoadedException {
                            return field.type();
                        }
                    });
                }
            }
        };
    }

    private interface FieldValueSetter {
        void setValue(Value param1Value) throws InvalidTypeException, ClassNotLoadedException;
    }
}

