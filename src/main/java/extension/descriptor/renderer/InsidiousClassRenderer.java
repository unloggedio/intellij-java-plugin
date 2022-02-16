package extension.descriptor.renderer;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.watch.MessageDescriptor;
import com.intellij.debugger.ui.tree.FieldDescriptor;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.settings.XDebuggerSettingsManager;
import com.jetbrains.jdi.StringReferenceImpl;
import com.sun.jdi.*;
import extension.DebuggerBundle;
import extension.InsidiousDebuggerTreeNode;
import extension.connector.InsidiousStackFrameProxy;
import extension.descriptor.InsidiousNodeDescriptorFactory;
import extension.descriptor.InsidiousValueDescriptor;
import extension.descriptor.InsidiousValueDescriptorImpl;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.util.DebuggerUtilsAsync;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class InsidiousClassRenderer extends InsidiousNodeRendererImpl {
    @NonNls
    public static final String UNIQUE_ID = "ClassRenderer";
    private static final Logger LOG = Logger.getInstance(InsidiousClassRenderer.class);
    public boolean SHOW_SYNTHETICS = true;

    public boolean SHOW_VAL_FIELDS_AS_LOCAL_VARIABLES = true;
    public boolean SHOW_STATIC = false;
    public boolean SHOW_STATIC_FINAL = false;
    public boolean SHOW_FQ_TYPE_NAMES = false;
    public boolean SHOW_DECLARED_TYPE = false;
    public boolean SHOW_OBJECT_ID = true;
    public boolean SHOW_STRINGS_TYPE = false;

    public InsidiousClassRenderer() {
        super("unnamed", true);
    }

    private static String calcLabelAsync(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) throws EvaluateException {
        CompletableFuture<String> future;
        Value value = descriptor.getValue();

        if (value instanceof StringReferenceImpl) {
            EvaluatorUtil.ensureNotInsideObjectConstructor((ObjectReference) value, evaluationContext);

            future = DebuggerUtilsAsync.getStringValue((StringReference) value);
        } else {
            future = CompletableFuture.completedFuture(calcLabel(descriptor, evaluationContext));
        }
        return calcLabelFromFuture(future, descriptor, labelListener);
    }

    private static String calcLabelFromFuture(CompletableFuture<String> future, InsidiousValueDescriptor descriptor, DescriptorLabelListener labelListener) {
        if (!future.isDone()) {
            future.whenComplete((s, throwable) -> {
                if (throwable != null) {
                    descriptor.setValueLabelFailed((EvaluateException) throwable);
                } else {
                    descriptor.setValueLabel(s);
                }

                labelListener.labelChanged();
            });
        }
        return future.getNow(XDebuggerBundle.message("xdebugger.building.tree.node.message"));
    }

    protected static String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext) throws EvaluateException {
        Value value = descriptor.getValue();
        if (value instanceof ObjectReference) {
            if (value instanceof StringReference) {
                EvaluatorUtil.ensureNotInsideObjectConstructor((ObjectReference) value, evaluationContext);

                return ((StringReference) value).value();
            }
            if (value instanceof ClassObjectReference) {
                ReferenceType referenceType = ((ClassObjectReference) value).reflectedType();
                return (referenceType != null) ? referenceType.name() : "{...}";
            }
            ObjectReference objRef = (ObjectReference) value;
            Type type = objRef.type();
            if (type instanceof ClassType && ((ClassType) type).isEnum()) {
                String name = getEnumConstantName(objRef, (ClassType) type);
                if (name != null) {
                    return name;
                }
                return type.name();
            }

            return "";
        }

        if (value == null) {
            return "null";
        }
        return DebuggerBundle.message("label.undefined");
    }

    @Nullable
    public static String getEnumConstantName(@NotNull ObjectReference objRef, ClassType classType) {
        while (true) {
            if (!classType.isPrepared()) {
                return null;
            }
            classType = classType.superclass();
            if (classType == null) {
                return null;
            }
            if ("java.lang.Enum".equals(classType.name())) {
                Field field = classType.fieldByName("name");
                if (field == null) {
                    return null;
                }
                Value value = objRef.getValue(field);
                if (!(value instanceof StringReference)) {
                    return null;
                }
                return ((StringReference) value).value();
            }
        }

    }

    @Nullable
    public final String renderTypeName(@Nullable String typeName) {
        if (this.SHOW_FQ_TYPE_NAMES || typeName == null) {
            return typeName;
        }
        String baseLambdaClassName = DebuggerUtilsEx.getLambdaBaseClassName(typeName);
        if (baseLambdaClassName != null) {
            return renderTypeName(baseLambdaClassName) + "$lambda";
        }

        int dotIndex = typeName.lastIndexOf('.');
        if (dotIndex > 0) {
            return typeName.substring(dotIndex + 1);
        }
        return typeName;
    }

    public String getUniqueId() {
        return "ClassRenderer";
    }

    public InsidiousClassRenderer clone() {
        return (InsidiousClassRenderer) super.clone();
    }

    public String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) throws EvaluateException {
        return calcLabelAsync(descriptor, evaluationContext, labelListener);
    }

    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
        InsidiousValueDescriptorImpl parentDescriptor = (InsidiousValueDescriptorImpl) builder.getParentDescriptor();
        InsidiousNodeManager nodeManager = builder.getNodeManager();
        InsidiousNodeDescriptorFactory nodeDescriptorFactory = builder.getDescriptorManager();

        List<InsidiousDebuggerTreeNode> children = new ArrayList<>();
        if (value instanceof ObjectReference) {
            ObjectReference objRef = (ObjectReference) value;
            ReferenceType refType = objRef.referenceType();

            List<Field> fields = refType.allFields();
            if (!fields.isEmpty()) {
                Set<String> names = new HashSet<>();
                for (Field field : fields) {
                    if (shouldDisplay(evaluationContext, objRef, field)) {

                        FieldDescriptor fieldDescriptor = createFieldDescriptor(parentDescriptor, nodeDescriptorFactory, objRef, field, evaluationContext);


                        String name = fieldDescriptor.getName();
                        if (names.contains(name)) {
                            fieldDescriptor.putUserData(FieldDescriptor.SHOW_DECLARING_TYPE, Boolean.TRUE);
                        } else {

                            names.add(name);
                        }
                        children.add(nodeManager.createNode(fieldDescriptor, evaluationContext));
                    }
                }

                if (children.isEmpty()) {
                    children.add(nodeManager
                            .createMessageNode(
                                    DebuggerBundle.message("message.node.class.no.fields.to.display")));
                } else if (XDebuggerSettingsManager.getInstance()
                        .getDataViewSettings()
                        .isSortValues()) {
                    children.sort(InsidiousNodeManagerImpl.getNodeComparator());
                }
            } else {
                children.add(nodeManager
                        .createMessageNode(MessageDescriptor.CLASS_HAS_NO_FIELDS
                                .getLabel()));
            }
        }
        builder.setChildren(children);
    }

    @NotNull
    protected FieldDescriptor createFieldDescriptor(InsidiousValueDescriptor parentDescriptor,
                                                    InsidiousNodeDescriptorFactory nodeDescriptorFactory,
                                                    ObjectReference objRef, Field field,
                                                    EvaluationContext evaluationContext) {
        return nodeDescriptorFactory.getFieldDescriptor(parentDescriptor, objRef, field);
    }

    protected boolean shouldDisplay(EvaluationContext context, @NotNull ObjectReference objInstance, @NotNull Field field) {
        boolean isSynthetic = DebuggerUtils.isSynthetic(field);
        if (!this.SHOW_SYNTHETICS && isSynthetic) {
            return false;
        }
        if (this.SHOW_VAL_FIELDS_AS_LOCAL_VARIABLES && isSynthetic) {
            try {
                InsidiousStackFrameProxy InsidiousStackFrameProxy = context.getStackFrameProxy();
                Location location = InsidiousStackFrameProxy.location();
                if (location != null && objInstance
                        .equals(context.computeThisObject()) &&
                        Comparing.equal(objInstance.referenceType(), location.declaringType()) &&
                        StringUtil.startsWith(field
                                .name(), "val$")) {
                    return false;
                }
            } catch (EvaluateException evaluateException) {
            }
        }

        if (!this.SHOW_STATIC && field.isStatic()) {
            return false;
        }

        return this.SHOW_STATIC_FINAL || !field.isStatic() || !field.isFinal();
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        DefaultJDOMExternalizer.writeExternal(this, element, new DifferenceFilter(this, new InsidiousClassRenderer()));
    }

    public PsiElement getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        FieldDescriptor fieldDescriptor = (FieldDescriptor) node.getDescriptor();

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(node.getProject());

        try {
            Project project = context.getVirtualMachineProxy().getXDebugProcess().getSession().getProject();
            return elementFactory.createExpressionFromText("this." + fieldDescriptor
                            .getField().name(),
                    DebuggerUtils.findClass(fieldDescriptor
                                    .getObject().referenceType().name(), project,

                            GlobalSearchScope.allScope(project)));
        } catch (IncorrectOperationException e) {
            throw new EvaluateException(
                    DebuggerBundle.message("error.invalid.field.name", fieldDescriptor.getField().name()), null);
        }
    }

    public CompletableFuture<Boolean> isExpandableAsync(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        if (value instanceof ArrayReference)
            return DebuggerUtilsAsync.length((ArrayReference) value)
                    .thenApply(r -> Boolean.valueOf((r.intValue() > 0)))
                    .exceptionally(throwable -> Boolean.valueOf(true));
        if (value instanceof ObjectReference) {
            return CompletableFuture.completedFuture(
                    Boolean.valueOf(true));
        }


        return CompletableFuture.completedFuture(Boolean.valueOf(false));
    }

    public boolean isApplicable(Type type) {
        return (type instanceof ReferenceType && !(type instanceof com.sun.jdi.ArrayType));
    }

    @NonNls
    public String getName() {
        return "Object";
    }

    public void setName(String text) {
        LOG.assertTrue(false);
    }
}

