package extension.descriptor.renderer;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiElement;
import com.sun.jdi.Value;
import extension.InsidiousChildRenderer;
import extension.InsidiousDebuggerTreeNode;
import extension.descriptor.*;
import extension.evaluation.EvaluationContext;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class InsidiousEnumerationChildrenRenderer
        extends InsidiousReferenceRenderer implements InsidiousChildRenderer {
    @NonNls
    public static final String UNIQUE_ID = "EnumerationChildrenRenderer";
    @NonNls
    public static final String APPEND_DEFAULT_NAME = "AppendDefault";
    @NonNls
    public static final String CHILDREN_EXPRESSION = "ChildrenExpression";
    @NonNls
    public static final String CHILD_NAME = "Name";
    @NonNls
    public static final String CHILD_ONDEMAND = "OnDemand";
    private boolean myAppendDefaultChildren;
    private List<ChildInfo> myChildren;
    public InsidiousEnumerationChildrenRenderer() {
        this(new ArrayList<>());
    }

    public InsidiousEnumerationChildrenRenderer(List<ChildInfo> children) {
        this.myChildren = children;
    }

    @Nullable
    public static InsidiousEnumerationChildrenRenderer getCurrent(InsidiousValueDescriptorImpl valueDescriptor) {
        Renderer renderer = valueDescriptor.getLastRenderer();
        if (renderer instanceof InsidiousCompoundReferenceRenderer) {

            InsidiousChildRenderer childrenRenderer = ((InsidiousCompoundReferenceRenderer) renderer).getChildrenRenderer();
            if (childrenRenderer instanceof InsidiousEnumerationChildrenRenderer) {
                return (InsidiousEnumerationChildrenRenderer) childrenRenderer;
            }
        }
        return null;
    }

    public boolean isAppendDefaultChildren() {
        return this.myAppendDefaultChildren;
    }

    public void setAppendDefaultChildren(boolean appendDefaultChildren) {
        this.myAppendDefaultChildren = appendDefaultChildren;
    }

    public String getUniqueId() {
        return "EnumerationChildrenRenderer";
    }

    public InsidiousEnumerationChildrenRenderer clone() {
        return (InsidiousEnumerationChildrenRenderer) super.clone();
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        this.myChildren.clear();

        this
                .myAppendDefaultChildren = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, "AppendDefault"));

        List<Element> children = element.getChildren("ChildrenExpression");
        for (Element item : children) {
            String name = item.getAttributeValue("Name");

            TextWithImports text = DebuggerUtils.getInstance().readTextWithImports(item.getChildren().get(0));
            boolean onDemand = Boolean.parseBoolean(item.getAttributeValue("OnDemand"));

            this.myChildren.add(new ChildInfo(name, text, onDemand));
        }
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        if (this.myAppendDefaultChildren) {
            JDOMExternalizerUtil.writeField(element, "AppendDefault", "true");
        }

        for (ChildInfo childInfo : this.myChildren) {
            Element child = new Element("ChildrenExpression");
            child.setAttribute("Name", childInfo.myName);
            if (childInfo.myOnDemand) {
                child.setAttribute("OnDemand", "true");
            }
            child.addContent(
                    DebuggerUtils.getInstance().writeTextWithImports(childInfo.myExpression));

            element.addContent(child);
        }
    }

    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
        InsidiousNodeManager nodeManager = builder.getNodeManager();
        InsidiousNodeDescriptorFactory descriptorFactory = builder.getDescriptorManager();

        List<InsidiousDebuggerTreeNode> children = new ArrayList<>();
        int idx = 0;
        for (ChildInfo childInfo : this.myChildren) {


            InsidiousUserExpressionData data = new InsidiousUserExpressionData((InsidiousValueDescriptorImpl) builder.getParentDescriptor(), getClassName(), childInfo.myName, childInfo.myExpression);


            data.setEnumerationIndex(idx++);

            InsidiousUserExpressionDescriptor descriptor = descriptorFactory.
                    getUserExpressionDescriptor(builder.getParentDescriptor(), data);
            if (childInfo.myOnDemand) {
                descriptor.putUserData(InsidiousOnDemandRenderer.ON_DEMAND_CALCULATED, Boolean.FALSE);
            }
            children.add(nodeManager.createNode(descriptor, evaluationContext));
        }
        builder.addChildren(children, !this.myAppendDefaultChildren);

        if (this.myAppendDefaultChildren) {
            RendererManager.getInstance()
                    .getDefaultRenderer(value.type())
                    .buildChildren(value, builder, evaluationContext);
        }
    }

    public PsiElement getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        return ((InsidiousValueDescriptor) node.getDescriptor()).getDescriptorEvaluation(context);
    }

    public CompletableFuture<Boolean> isExpandableAsync(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        if (this.myChildren.size() > 0) {
            return CompletableFuture.completedFuture(Boolean.valueOf(true));
        }
        if (this.myAppendDefaultChildren) {
            return RendererManager.getInstance()
                    .getDefaultRenderer(value.type())
                    .isExpandableAsync(value, evaluationContext, parentDescriptor);
        }
        return CompletableFuture.completedFuture(Boolean.valueOf(false));
    }

    public List<ChildInfo> getChildren() {
        return this.myChildren;
    }

    public void setChildren(List<ChildInfo> children) {
        this.myChildren = children;
    }

    public static class ChildInfo implements Cloneable {
        public String myName;
        public TextWithImports myExpression;
        public boolean myOnDemand;

        public ChildInfo(String name, TextWithImports expression, boolean onDemand) {
            this.myName = name;
            this.myExpression = expression;
            this.myOnDemand = onDemand;
        }
    }
}


