package extension.descriptor.renderer;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeExpression;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiExpression;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.InsidiousChildRenderer;
import extension.InsidiousDebuggerTreeNode;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.expression.ExpressionEvaluator;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InsidiousExpressionChildrenRenderer extends InsidiousReferenceRenderer implements InsidiousChildRenderer {
    @NonNls
    public static final String UNIQUE_ID = "ExpressionChildrenRenderer";
    private static final Key<Value> EXPRESSION_VALUE = new Key("EXPRESSION_VALUE");
    private static final Key<InsidiousNodeRenderer> LAST_CHILDREN_RENDERER = new Key("LAST_CHILDREN_RENDERER");


    private InsidiousCachedEvaluator myChildrenExpandable = createCachedEvaluator();
    private InsidiousCachedEvaluator myChildrenExpression = createCachedEvaluator();

    private InsidiousNodeRenderer myPredictedRenderer;

    @Nullable
    public static InsidiousNodeRenderer getLastChildrenRenderer(InsidiousValueDescriptor descriptor) {
        return (InsidiousNodeRenderer) descriptor.getUserData(LAST_CHILDREN_RENDERER);
    }

    public static void setPreferableChildrenRenderer(InsidiousValueDescriptor descriptor, InsidiousNodeRenderer renderer) {
        descriptor.putUserData(LAST_CHILDREN_RENDERER, renderer);
    }

    public static Value getLastChildrenValue(NodeDescriptor descriptor) {
        return (Value) descriptor.getUserData(EXPRESSION_VALUE);
    }

    private static InsidiousNodeRenderer getChildrenRenderer(Value childrenValue, InsidiousValueDescriptor parentDescriptor) {
        InsidiousNodeRenderer renderer = getLastChildrenRenderer(parentDescriptor);
        if (renderer == null || childrenValue == null ||

                !renderer.isApplicable(childrenValue.type())) {


            renderer = RendererManager.getInstance().getDefaultRenderer(
                    (childrenValue != null) ? childrenValue.type() : null);
            setPreferableChildrenRenderer(parentDescriptor, renderer);
        }
        return renderer;
    }

    public String getUniqueId() {
        return "ExpressionChildrenRenderer";
    }

    public InsidiousExpressionChildrenRenderer clone() {
        InsidiousExpressionChildrenRenderer clone = (InsidiousExpressionChildrenRenderer) super.clone();
        clone.myChildrenExpandable = createCachedEvaluator();
        clone.setChildrenExpandable(getChildrenExpandable());
        clone.myChildrenExpression = createCachedEvaluator();
        clone.setChildrenExpression(getChildrenExpression());
        return clone;
    }

    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
        InsidiousNodeManager nodeManager = builder.getNodeManager();

        try {
            InsidiousValueDescriptor parentDescriptor = builder.getParentDescriptor();

            Value childrenValue = evaluateChildren(evaluationContext
                    .createEvaluationContext(value), (NodeDescriptor) parentDescriptor);

            InsidiousNodeRenderer renderer = getChildrenRenderer(childrenValue, parentDescriptor);
            renderer.buildChildren(childrenValue, builder, evaluationContext);
        } catch (EvaluateException e) {
            List<InsidiousDebuggerTreeNode> errorChildren = new ArrayList<>();
            errorChildren.add(nodeManager
                    .createMessageNode(
                            DebuggerBundle.message("error.unable.to.evaluate.expression", new Object[0]) + " " + e

                                    .getMessage()));
            builder.setChildren(errorChildren);
        }
    }

    private Value evaluateChildren(EvaluationContext context, NodeDescriptor descriptor) throws EvaluateException {
        Project project = EvaluatorUtil.getProject(context);
        ExpressionEvaluator evaluator = this.myChildrenExpression.getEvaluator(project, context);
        Value value = evaluator.evaluate(context);
        descriptor.putUserData(EXPRESSION_VALUE, value);
        return value;
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        DefaultJDOMExternalizer.readExternal(this, element);


        TextWithImports childrenExpression = DebuggerUtils.getInstance().readTextWithImports(element, "CHILDREN_EXPRESSION");
        if (childrenExpression != null) {
            setChildrenExpression(childrenExpression);
        }


        TextWithImports childrenExpandable = DebuggerUtils.getInstance().readTextWithImports(element, "CHILDREN_EXPANDABLE");
        if (childrenExpandable != null) {
            this.myChildrenExpandable.setReferenceExpression(childrenExpandable);
        }
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        DefaultJDOMExternalizer.writeExternal(this, element);
        DebuggerUtils.getInstance()
                .writeTextWithImports(element, "CHILDREN_EXPANDABLE", getChildrenExpandable());
        DebuggerUtils.getInstance()
                .writeTextWithImports(element, "CHILDREN_EXPRESSION", getChildrenExpression());
    }

    public PsiExpression getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        Value expressionValue = getLastChildrenValue(node.getParent().getDescriptor());
        if (expressionValue == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("error.unable.to.evaluate.expression", new Object[0]));
        }


        InsidiousNodeRenderer childrenRenderer = getChildrenRenderer(expressionValue, (InsidiousValueDescriptor) node
                .getParent().getDescriptor());


        PsiExpression childrenPsiExpression = this.myChildrenExpression.getPsiExpression(node.getProject(), context);
        if (childrenPsiExpression == null) {
            return null;
        }
        return DebuggerTreeNodeExpression.substituteThis(childrenRenderer
                .getChildValueExpression(node, context), (PsiExpression) childrenPsiExpression
                .copy(), expressionValue);
    }

    public boolean isExpandable(Value value, EvaluationContext context, NodeDescriptor parentDescriptor) {
        EvaluationContext evaluationContext = context.createEvaluationContext(value);

        if (!StringUtil.isEmpty(this.myChildrenExpandable.getReferenceExpression().getText())) {
            try {
                Project project = EvaluatorUtil.getProject(evaluationContext);


                Value expanded = this.myChildrenExpandable.getEvaluator(project, evaluationContext).evaluate(evaluationContext);
                if (expanded instanceof BooleanValue) {
                    return ((BooleanValue) expanded).booleanValue();
                }
            } catch (EvaluateException evaluateException) {
            }
        }


        try {
            Value children = evaluateChildren(evaluationContext, parentDescriptor);

            InsidiousChildRenderer defaultChildrenRenderer = RendererManager.getInstance().getDefaultRenderer(value.type());
            return defaultChildrenRenderer.isExpandable(children, evaluationContext, parentDescriptor);
        } catch (EvaluateException e) {
            return true;
        }
    }

    public TextWithImports getChildrenExpression() {
        return this.myChildrenExpression.getReferenceExpression();
    }

    public void setChildrenExpression(TextWithImports expression) {
        this.myChildrenExpression.setReferenceExpression(expression);
    }

    public TextWithImports getChildrenExpandable() {
        return this.myChildrenExpandable.getReferenceExpression();
    }

    public void setChildrenExpandable(TextWithImports childrenExpandable) {
        this.myChildrenExpandable.setReferenceExpression(childrenExpandable);
    }


    public void setClassName(String name) {
        super.setClassName(name);
        this.myChildrenExpression.clear();
        this.myChildrenExpandable.clear();
    }

    public InsidiousNodeRenderer getPredictedRenderer() {
        return this.myPredictedRenderer;
    }

    public void setPredictedRenderer(InsidiousNodeRenderer predictedRenderer) {
        this.myPredictedRenderer = predictedRenderer;
    }
}


