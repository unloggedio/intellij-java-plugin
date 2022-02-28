package extension.descriptor;

import com.intellij.Patches;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.NodeDescriptorNameAdjuster;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.frame.XValueModifier;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.sun.jdi.*;
import extension.DebuggerBundle;
import extension.InsidiousDebuggerTreeNode;
import extension.descriptor.renderer.*;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import extension.util.DebuggerUtilsAsync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public abstract class InsidiousValueDescriptorImpl extends InsidiousNodeDescriptorImpl implements InsidiousValueDescriptor {
    protected static final Logger LOG = Logger.getInstance(InsidiousValueDescriptorImpl.class);

    protected final Project myProject;
    protected EvaluationContext myStoredEvaluationContext = null;
    protected boolean myIsNew = true;
    InsidiousNodeRenderer myRenderer = null;
    InsidiousNodeRenderer myAutoRenderer = null;
    private Value myValue;
    private volatile boolean myValueReady;
    private EvaluateException myValueException;
    private String myIdLabel;
    private String myValueText;
    private boolean myFullValue = false;
    @Nullable
    private Icon myValueIcon;
    private boolean myIsDirty = false;
    private boolean myIsLvalue = false;
    private boolean myIsExpandable;
    private boolean myShowIdLabel = true;

    protected InsidiousValueDescriptorImpl(Project project, Value value) {
        this.myProject = project;
        this.myValue = value;
        this.myValueReady = true;
    }

    protected InsidiousValueDescriptorImpl(Project project) {
        this.myProject = project;
    }

    @Nullable
    protected static Value invokeExceptionGetStackTrace(ObjectReference exceptionObj, EvaluationContext evaluationContext) throws EvaluateException {
        Method method = DebuggerUtils.findMethod(exceptionObj
                .referenceType(), "getStackTrace", "()[Ljava/lang/StackTraceElement;");


        if (method != null) {
            return EvaluatorUtil.invokeInstanceMethod(evaluationContext, exceptionObj, method,
                    Collections.emptyList(), 0);
        }
        return null;
    }

    @Nullable
    private static ObjectReference getTargetExceptionWithStackTraceFilled(@Nullable EvaluationContext evaluationContext, EvaluateException ex, boolean printToConsole) {
        ObjectReference exceptionObj = ex.getExceptionFromTargetVM();
        if (exceptionObj != null && evaluationContext != null) {
            try {
                Value value = invokeExceptionGetStackTrace(exceptionObj, evaluationContext);
            } catch (EvaluateException evaluateException) {
            }
        }

        return exceptionObj;
    }

    public static String getIdLabel(ObjectReference objRef) {
        return calcIdLabel(objRef, null, null);
    }

    @Nullable
    public static String calcIdLabel(InsidiousValueDescriptor descriptor, @NotNull DescriptorLabelListener labelListener) {
        Value value = descriptor.getValue();
        if (!(value instanceof ObjectReference)) {
            return null;
        }
        return calcIdLabel((ObjectReference) value, descriptor, labelListener);
    }

    @Nullable
    private static String calcIdLabel(ObjectReference objRef, @Nullable InsidiousValueDescriptor descriptor, @Nullable DescriptorLabelListener labelListener) {
        InsidiousClassRenderer classRenderer = RendererManager.getInstance().getClassRenderer();
        if (objRef instanceof com.sun.jdi.StringReference && !classRenderer.SHOW_STRINGS_TYPE) {
            return null;
        }
        StringBuilder buf = new StringBuilder();


        boolean showConcreteType = (!classRenderer.SHOW_DECLARED_TYPE
                || (!
                        (objRef instanceof com.sun.jdi.StringReference) &&
                        !(objRef instanceof com.sun.jdi.ClassObjectReference) &&
                        !isEnumConstant(objRef)
        ));
        if (showConcreteType || classRenderer.SHOW_OBJECT_ID) {

            if (showConcreteType) {
                buf.append(classRenderer.renderTypeName(objRef.type().name()));
            }
            if (classRenderer.SHOW_OBJECT_ID) {
                buf.append('@');
                if (ApplicationManager.getApplication().isUnitTestMode()) {
                    buf.append("uniqueID");
                } else {
                    buf.append(objRef.uniqueID());
                }
            }
        }


        if (objRef instanceof ArrayReference) {
            int idx = buf.indexOf("[");
            if (idx >= 0) {
                if (labelListener == null || descriptor == null) {
                    buf.insert(idx + 1, ((ArrayReference) objRef).length());
                } else {

                    CompletableFuture<String> asyncId = DebuggerUtilsAsync.length((ArrayReference) objRef).thenApply(length -> buf.insert(idx + 1, length).toString());
                    if (asyncId.isDone()) {
                        return asyncId.join();
                    }
                    asyncId.thenAccept(res -> {
                        descriptor.setIdLabel(res);


                        labelListener.labelChanged();
                    });
                }
            }
        }

        return buf.toString();
    }

    private static boolean isEnumConstant(ObjectReference objRef) {
        try {
            Type type = objRef.type();
            return (type instanceof ClassType && ((ClassType) type).isEnum());
        } catch (ObjectCollectedException objectCollectedException) {

            return false;
        }
    }

    private void assertValueReady() {
        if (!this.myValueReady) {
            LOG.error("Value is not yet calculated for " + getClass());
        }
    }

    public boolean isArray() {
        assertValueReady();
        return this.myValue instanceof ArrayReference;
    }

    public boolean isDirty() {
        assertValueReady();
        return this.myIsDirty;
    }

    public boolean isLvalue() {
        assertValueReady();
        return this.myIsLvalue;
    }

    protected void setLvalue(boolean value) {
        this.myIsLvalue = value;
    }

    public boolean isNull() {
        assertValueReady();
        return (this.myValue == null);
    }

    public boolean isString() {
        assertValueReady();
        return this.myValue instanceof com.sun.jdi.StringReference;
    }

    public boolean isPrimitive() {
        assertValueReady();
        return this.myValue instanceof com.sun.jdi.PrimitiveValue;
    }

    public boolean isEnumConstant() {
        assertValueReady();
        return (this.myValue instanceof ObjectReference && isEnumConstant((ObjectReference) this.myValue));
    }

    public boolean isValueValid() {
        return (this.myValueException == null);
    }

    public boolean isShowIdLabel() {
        return true;
//        return (this.myShowIdLabel && Registry.is("debugger.showTypes"));
    }

    public void setShowIdLabel(boolean showIdLabel) {
        this.myShowIdLabel = showIdLabel;
    }

    public boolean isValueReady() {
        return this.myValueReady;
    }

    public Value getValue() {
        if (Patches.IBM_JDK_DISABLE_COLLECTION_BUG) {
            EvaluationContext evalContext = this.myStoredEvaluationContext;
            if (evalContext != null && this.myValue instanceof ObjectReference && evalContext


                    .getVirtualMachineProxy()
                    .isCollected((ObjectReference) this.myValue)) {
                setContext(this.myStoredEvaluationContext);
            }
        }

        assertValueReady();
        return this.myValue;
    }

    public boolean isExpandable() {
        return this.myIsExpandable;
    }

    public final void setContext(EvaluationContext evaluationContext) {
        this.myStoredEvaluationContext = evaluationContext;

        try {
            Value value = calcValue(evaluationContext);

            if (!this.myIsNew) {
                try {
                    if (this.myValue instanceof DoubleValue &&
                            Double.isNaN(((DoubleValue) this.myValue).doubleValue())) {
                        this.myIsDirty = !(value instanceof DoubleValue);
                    } else if (this.myValue instanceof FloatValue &&
                            Float.isNaN(((FloatValue) this.myValue).floatValue())) {
                        this.myIsDirty = !(value instanceof FloatValue);
                    } else {
                        this.myIsDirty = !Objects.equals(value, this.myValue);
                    }
                } catch (ObjectCollectedException ignored) {
                    this.myIsDirty = true;
                }
            }
            this.myValue = value;
            this.myValueException = null;
        } catch (EvaluateException e) {
            this.myValueException = e;
            setFailed(e);
            this
                    .myValue = getTargetExceptionWithStackTraceFilled(evaluationContext, e, (


                    isPrintExceptionToConsole() ||
                            ApplicationManager.getApplication().isUnitTestMode()));
            this.myIsExpandable = false;
        } finally {
            this.myValueReady = true;
        }

        this.myIsNew = false;
    }

    protected boolean isPrintExceptionToConsole() {
        return true;
    }

    public void setAncestor(NodeDescriptor oldDescriptor) {
        super.setAncestor(oldDescriptor);
        this.myIsNew = false;
        if (!this.myValueReady) {
            InsidiousValueDescriptorImpl other = (InsidiousValueDescriptorImpl) oldDescriptor;
            if (other.myValueReady) {
                this.myValue = other.getValue();
                this.myValueReady = true;
            }
        }
    }

    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) {
        getRenderer()
                .thenAccept(renderer -> calcRepresentation(labelListener, renderer))
                .exceptionally(throwable -> {
                    String message;

                    throwable = DebuggerUtilsAsync.unwrap(throwable);

                    if (throwable instanceof java.util.concurrent.CancellationException) {
                        message = DebuggerBundle.message("error.context.has.changed");
                    } else {
                        message = DebuggerBundle.message("internal.debugger.error");
                        LOG.error(new Throwable(throwable));
                    }
                    setValueLabelFailed(new EvaluateException(message));
                    labelListener.labelChanged();
                    return null;
                });
        return "";
    }

    @NotNull
    private String calcRepresentation(DescriptorLabelListener labelListener, InsidiousNodeRenderer renderer) {
        CompletableFuture<Boolean> expandableFuture;
        EvaluateException valueException = this.myValueException;

        if (valueException == null || valueException.getExceptionFromTargetVM() != null) {
            expandableFuture = getChildrenRenderer().thenCompose(
                    r -> r.isExpandableAsync(getValue(), this.myStoredEvaluationContext, this)
            );
        } else {
            expandableFuture = CompletableFuture.completedFuture(Boolean.FALSE);
        }


        if (isShowIdLabel() && renderer instanceof InsidiousNodeRendererImpl) {
            setIdLabel(((InsidiousNodeRendererImpl) renderer).calcIdLabel(this, labelListener));
        }

        if (valueException == null) {
            try {
                setValueLabel(renderer.calcLabel(
                        this, this.myStoredEvaluationContext, labelListener));
            } catch (EvaluateException e) {
                setValueLabelFailed(e);
            }
        } else {
            setValueLabelFailed(valueException);
        }


        expandableFuture.whenComplete((res, ex) -> {
            if (ex == null) {
                this.myIsExpandable = res;
            } else {
                ex = DebuggerUtilsAsync.unwrap(ex);

                if (!(ex instanceof java.util.concurrent.CancellationException)) {
                    LOG.error(new Throwable(ex));
                }
            }

            labelListener.labelChanged();
        });
        return "";
    }

    public String getLabel() {
        return calcValueName() + getDeclaredTypeLabel() + " = " + getValueLabel();
    }

    public InsidiousValueDescriptorImpl getFullValueDescriptor() {
        InsidiousValueDescriptorImpl descriptor = new InsidiousValueDescriptorImpl(this.myProject, this.myValue) {

            public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
                return InsidiousValueDescriptorImpl.this.myValue;
            }


            public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
                return null;
            }


            public CompletableFuture<InsidiousNodeRenderer> getRenderer() {
                return InsidiousValueDescriptorImpl.this.getRenderer();
            }


            public <T> T getUserData(Key<T> key) {
                return InsidiousValueDescriptorImpl.this.getUserData(key);
            }
        };
        descriptor.myFullValue = true;
        return descriptor;
    }

    public String setValueLabelFailed(EvaluateException e) {
        String label = setFailed(e);
        setValueLabel(label);
        return label;
    }

    public Icon setValueIcon(Icon icon) {
        return this.myValueIcon = icon;
    }

    @Nullable
    public Icon getValueIcon() {
        return this.myValueIcon;
    }

    public String calcValueName() {
        String name = getName();
        NodeDescriptorNameAdjuster nameAdjuster = NodeDescriptorNameAdjuster.findFor(this);
        if (nameAdjuster != null) {
            return nameAdjuster.fixName(name, this);
        }
        return name;
    }

    @Nullable
    public String getDeclaredType() {
        return null;
    }

    public void displayAs(NodeDescriptor descriptor) {
        if (descriptor instanceof InsidiousValueDescriptorImpl) {
            InsidiousValueDescriptorImpl valueDescriptor = (InsidiousValueDescriptorImpl) descriptor;
            this.myRenderer = valueDescriptor.myRenderer;
        }
        super.displayAs(descriptor);
    }

    public Renderer getLastRenderer() {
        return (this.myRenderer != null) ? this.myRenderer : this.myAutoRenderer;
    }

    public Renderer getLastLabelRenderer() {
        InsidiousValueLabelRenderer insidiousValueLabelRenderer = null;
        Renderer lastRenderer = getLastRenderer();
        if (lastRenderer instanceof InsidiousCompoundReferenceRenderer) {
            insidiousValueLabelRenderer = ((InsidiousCompoundReferenceRenderer) lastRenderer).getLabelRenderer();
        }
        return insidiousValueLabelRenderer;
    }

    public CompletableFuture<InsidiousNodeRenderer> getChildrenRenderer() {
        return getRenderer();
    }

    public CompletableFuture<InsidiousNodeRenderer> getRenderer() {
        return DebuggerUtilsAsync.type(getValue()).thenCompose(type -> getRenderer(type));
    }

    public void setRenderer(InsidiousNodeRenderer renderer) {
        this.myRenderer = renderer;
        this.myAutoRenderer = null;
    }

    private CompletableFuture<InsidiousNodeRenderer> getRenderer(Type type) {
        if (this.myRenderer != null) {
            return CompletableFuture.completedFuture(this.myRenderer);
        }
        return CompletableFuture.completedFuture(
                RendererManager.getInstance().getRenderer(type));
    }

    @NotNull
    public CompletableFuture<PsiElement> getTreeEvaluation(InsidiousJavaValue value) throws EvaluateException {
        InsidiousJavaValue parent = value.getParent();
        if (parent != null) {
            InsidiousValueDescriptorImpl vDescriptor = parent.getDescriptor();
            return vDescriptor.getTreeEvaluation(parent).thenCompose(parentEvaluation -> !(parentEvaluation instanceof PsiExpression) ?
                    CompletableFuture.completedFuture(null) :
                    vDescriptor.getChildrenRenderer().thenApply(insidiousNodeRenderer -> {
                        return null;
                    }));
        }


        return ReadAction.compute(() -> CompletableFuture.completedFuture(getDescriptorEvaluation(this.myStoredEvaluationContext)));
    }

    public boolean canSetValue() {
        return (this.myValueReady && !this.myIsSynthetic && isLvalue());
    }

    public XValueModifier getModifier(InsidiousJavaValue value) {
        return null;
    }

    public String getIdLabel() {
        return this.myIdLabel;
    }

    public void setIdLabel(String idLabel) {
        this.myIdLabel = idLabel;
    }

    public String getValueLabel() {
        String label = getIdLabel();
        if (!StringUtil.isEmpty(label)) {
            return '{' + label + '}' + getValueText();
        }
        return getValueText();
    }

    public void setValueLabel(@NotNull String label) {
        this.myValueText = this.myFullValue ? label :
                DebuggerUtilsEx.truncateString(label);
    }

    @NotNull
    public String getValueText() {
        return StringUtil.notNullize(this.myValueText);
    }

    public void clear() {
        super.clear();
        setValueLabel("");
        this.myIsExpandable = false;
    }

    @Nullable
    public ValueMarkup getMarkup(XDebugProcess debugProcess) {
        Value value = getValue();
        if (value instanceof ObjectReference) {
            ObjectReference objRef = (ObjectReference) value;
            Map<ObjectReference, ValueMarkup> map = getMarkupMap(debugProcess);
            if (map != null) {
                return map.get(objRef);
            }
        }
        return null;
    }

    public void setMarkup(XDebugProcess debugProcess, @Nullable ValueMarkup markup) {
        Value value = getValue();
        if (value instanceof ObjectReference) {
            Map<ObjectReference, ValueMarkup> map = getMarkupMap(debugProcess);
            if (map != null) {
                ObjectReference objRef = (ObjectReference) value;
                if (markup != null) {
                    map.put(objRef, markup);
                } else {
                    map.remove(objRef);
                }
            }
        }
    }

    public boolean canMark() {
        if (!this.myValueReady) {
            return false;
        }
        return getValue() instanceof ObjectReference;
    }

    public Project getProject() {
        return this.myProject;
    }

    @NotNull
    public String getDeclaredTypeLabel() {
        InsidiousClassRenderer classRenderer = RendererManager.getInstance().getClassRenderer();
        if (classRenderer.SHOW_DECLARED_TYPE) {
            String declaredType = getDeclaredType();
            if (!StringUtil.isEmpty(declaredType)) {
                return ": " + classRenderer.renderTypeName(declaredType);
            }
        }
        return "";
    }

    public EvaluationContext getStoredEvaluationContext() {
        return this.myStoredEvaluationContext;
    }

    public abstract Value calcValue(EvaluationContext paramEvaluationContext) throws EvaluateException;

    public abstract PsiExpression getDescriptorEvaluation(EvaluationContext paramEvaluationContext) throws EvaluateException;

    private static class DebuggerTreeNodeMock implements InsidiousDebuggerTreeNode {
        private final InsidiousJavaValue value;

        DebuggerTreeNodeMock(InsidiousJavaValue value) {
            this.value = value;
        }


        public InsidiousDebuggerTreeNode getParent() {
            return new DebuggerTreeNodeMock(this.value.getParent());
        }


        public InsidiousValueDescriptorImpl getDescriptor() {
            return this.value.getDescriptor();
        }


        public Project getProject() {
            return this.value.getProject();
        }


        public void setRenderer(InsidiousNodeRenderer renderer) {
        }
    }
}

