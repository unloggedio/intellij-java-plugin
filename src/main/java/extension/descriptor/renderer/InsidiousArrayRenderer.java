package extension.descriptor.renderer;

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.actions.ArrayAction;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.impl.DebuggerUtilsAsync;
import com.intellij.debugger.memory.utils.ErrorsValueGroup;
import com.intellij.debugger.settings.NodeRendererSettings;
import com.intellij.debugger.settings.ViewsGeneralSettings;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.InsidiousDebuggerTreeNode;
import extension.descriptor.InsidiousArrayElementDescriptorImpl;
import extension.descriptor.InsidiousJavaValue;
import extension.descriptor.InsidiousNodeDescriptorFactory;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.expression.ExpressionEvaluator;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InsidiousArrayRenderer extends InsidiousNodeRendererImpl {
    @NonNls
    public static final String UNIQUE_ID = "ArrayRenderer";
    private static final Logger LOG = Logger.getInstance(InsidiousArrayRenderer.class);
    public int START_INDEX = 0;
    public int END_INDEX = Integer.MAX_VALUE;
    public int ENTRIES_LIMIT = 100;

    private boolean myForced = false;

    public InsidiousArrayRenderer() {
        super("unnamed", true);
    }


    public String getUniqueId() {
        return "ArrayRenderer";
    }

    @NonNls
    public String getName() {
        return "Array";
    }


    public void setName(String text) {
        LOG.assertTrue(false);
    }


    public InsidiousArrayRenderer clone() {
        return (InsidiousArrayRenderer) super.clone();
    }


    public String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
        return InsidiousClassRenderer.calcLabel(descriptor, evaluationContext);
    }

    public void setForced(boolean forced) {
        this.myForced = forced;
    }


    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
        ArrayReference array = (ArrayReference) value;
        DebuggerUtilsAsync.length(array)
                .thenAccept(arrayLength -> {
                    if (arrayLength.intValue() > 0) {
                        if (!this.myForced) {
                            builder.initChildrenArrayRenderer(this, arrayLength.intValue());
                        }
                        if (this.ENTRIES_LIMIT <= 0) {
                            this.ENTRIES_LIMIT = 1;
                        }
                        AtomicInteger added = new AtomicInteger();
                        AtomicBoolean hiddenNulls = new AtomicBoolean();
                        addChunk(array, this.START_INDEX, Math.min(arrayLength.intValue() - 1, this.END_INDEX), arrayLength.intValue(), builder, evaluationContext, added, hiddenNulls);
                    }
                });
    }


    private CompletableFuture<Void> addChunk(ArrayReference array, int start, int end, int length, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext, AtomicInteger added, AtomicBoolean hiddenNulls) {
        int chunkLength = Math.min(100, end - start + 1);
        return DebuggerUtilsAsync.getValues(array, start, chunkLength)
                .thenCompose(values -> {
                    int idx;
                    for (idx = start; idx < start + values.size(); idx++) {
                        Value val = values.get(idx - start);
                        if ((ViewsGeneralSettings.getInstance()).HIDE_NULL_ARRAY_ELEMENTS && val == null) {
                            hiddenNulls.set(true);
                        } else {
                            InsidiousArrayElementDescriptorImpl descriptor = (InsidiousArrayElementDescriptorImpl) builder.getDescriptorManager().getArrayItemDescriptor(builder.getParentDescriptor(), array, idx);
                            descriptor.setValue(val);
                            InsidiousDebuggerTreeNode arrayItemNode = ((InsidiousNodeManagerImpl) builder.getNodeManager()).createNode(descriptor, evaluationContext);
                            builder.addChildren(Collections.singletonList(arrayItemNode), false);
                            if (added.incrementAndGet() >= this.ENTRIES_LIMIT) {
                                break;
                            }
                        }
                    }
                    if (idx < end && added.get() < this.ENTRIES_LIMIT) {
                        return addChunk(array, idx, end, length, builder, evaluationContext, added, hiddenNulls);
                    }
                    finish(builder, length, added.get(), hiddenNulls.get(), end, idx);
                    return CompletableFuture.completedFuture(null);
                });
    }


    private void finish(InsidiousChildrenBuilder builder, int arrayLength, int added, boolean hiddenNulls, int end, int idx) {
        builder.addChildren(Collections.emptyList(), true);

        if (added == 0) {
            if (this.START_INDEX == 0 && arrayLength - 1 <= this.END_INDEX) {
                builder.setMessage(
                        DebuggerBundle.message("message.node.all.elements.null"), null, SimpleTextAttributes.REGULAR_ATTRIBUTES, null);

            } else {

                builder.setMessage(
                        DebuggerBundle.message("message.node.all.array.elements.null", Integer.valueOf(this.START_INDEX), Integer.valueOf(this.END_INDEX)), null, SimpleTextAttributes.REGULAR_ATTRIBUTES, null);
            }

        } else {

            if (hiddenNulls) {
                builder.setMessage(
                        DebuggerBundle.message("message.node.elements.null.hidden"), null, SimpleTextAttributes.REGULAR_ATTRIBUTES, null);
            }


            if (!this.myForced && idx < end)
                builder.tooManyChildren(end - idx);
        }
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        DefaultJDOMExternalizer.writeExternal(this, element);
    }

    public PsiExpression getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) {
        LOG.assertTrue(node
                .getDescriptor() instanceof InsidiousArrayElementDescriptorImpl, node
                .getDescriptor().getClass().getName());

        InsidiousArrayElementDescriptorImpl descriptor = (InsidiousArrayElementDescriptorImpl) node.getDescriptor();

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(node.getProject());

        try {
            LanguageLevel languageLevel = LanguageLevelProjectExtension.getInstance(node.getProject()).getLanguageLevel();
            return elementFactory.createExpressionFromText("this[" + descriptor
                    .getIndex() + "]", elementFactory
                    .getArrayClass(languageLevel));
        } catch (IncorrectOperationException e) {
            LOG.error(e);
            return null;
        }
    }

    public CompletableFuture<Boolean> isExpandableAsync(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        if (!(value instanceof ArrayReference)) {
            return CompletableFuture.completedFuture(Boolean.valueOf(false));
        }
        return DebuggerUtilsAsync.length((ArrayReference) value).thenApply(l -> Boolean.valueOf((l.intValue() > 0)));
    }

    public boolean isApplicable(Type type) {
        return type instanceof com.sun.jdi.ArrayType;
    }

    private static class ArrayValuesCache {
        private final ArrayReference myArray;
        private List<Value> myCachedValues = Collections.emptyList();
        private int myCachedStartIndex;

        private ArrayValuesCache(ArrayReference array) {
            this.myArray = array;
        }

        Value getValue(int index) {
            if (index < this.myCachedStartIndex || index >= this.myCachedStartIndex + this.myCachedValues.size()) {
                this.myCachedStartIndex = index;
                this
                        .myCachedValues = this.myArray.getValues(index,

                        Math.min(100, this.myArray

                                .length() - index));
            }
            return this.myCachedValues.get(index - this.myCachedStartIndex);
        }
    }

    public static class Filtered
            extends InsidiousArrayRenderer {
        public static final XDebuggerTreeNodeHyperlink FILTER_HYPERLINK = new XDebuggerTreeNodeHyperlink(" clear") {
            public void onClick(MouseEvent e) {
                XDebuggerTree tree = (XDebuggerTree) e.getSource();
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    TreeNode parent = ((TreeNode) path.getLastPathComponent()).getParent();
                    if (parent instanceof XValueNodeImpl) {
                        XValueNodeImpl valueNode = (XValueNodeImpl) parent;
                        ArrayAction.setArrayRenderer(
                                NodeRendererSettings.getInstance().getArrayRenderer(), valueNode,

                                DebuggerManagerEx.getInstanceEx(tree.getProject())
                                        .getContext());
                    }
                }
                e.consume();
            }
        };
        private final XExpression myExpression;

        public Filtered(XExpression expression) {
            this.myExpression = expression;
        }

        public XExpression getExpression() {
            return this.myExpression;
        }

        public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
            InsidiousNodeManagerImpl nodeManager = (InsidiousNodeManagerImpl) builder.getNodeManager();
            InsidiousNodeDescriptorFactory descriptorFactory = builder.getDescriptorManager();

            builder.setMessage(
                    DebuggerBundle.message("message.node.filtered") + " " + this.myExpression

                            .getExpression(), AllIcons.General.Filter, SimpleTextAttributes.REGULAR_ATTRIBUTES, FILTER_HYPERLINK);


            if (this.ENTRIES_LIMIT <= 0) {
                this.ENTRIES_LIMIT = 1;
            }

            ArrayReference array = (ArrayReference) value;
            int arrayLength = array.length();
            if (arrayLength > 0) {
                builder.initChildrenArrayRenderer(this, arrayLength);


                try {
                    ExpressionEvaluator expressionEvaluator = EvaluatorUtil.getExpressionEvaluator(evaluationContext,
                            TextWithImportsImpl.fromXExpression(this.myExpression));
                    int added = 0;
                    if (arrayLength - 1 >= this.START_INDEX) {

                        ErrorsValueGroup errorsGroup = null;
                        InsidiousArrayRenderer.ArrayValuesCache arrayValuesCache = new InsidiousArrayRenderer.ArrayValuesCache(array);

                        for (int idx = this.START_INDEX; idx < arrayLength; idx++) {


                            InsidiousArrayElementDescriptorImpl descriptor =
                                    (InsidiousArrayElementDescriptorImpl)
                                            descriptorFactory.getArrayItemDescriptor(
                                                    builder.getParentDescriptor(), array, idx);
                            Value val = arrayValuesCache.getValue(idx);
                            descriptor.setValue(val);

                            try {
                                if (EvaluatorUtil.evaluateBoolean(expressionEvaluator, evaluationContext)) {

                                    InsidiousDebuggerTreeNode arrayItemNode = nodeManager.createNode(descriptor, evaluationContext);
                                    builder.addChildren(
                                            Collections.singletonList(arrayItemNode), false);
                                    added++;

                                }

                            } catch (EvaluateException e) {
                                if (errorsGroup == null) {
                                    errorsGroup = new ErrorsValueGroup();
                                    builder.addChildren(
                                            XValueChildrenList.bottomGroup(errorsGroup), false);
                                }

                                InsidiousJavaValue childValue = InsidiousJavaValue.create(null, descriptor, evaluationContext, nodeManager, false);


                                errorsGroup.addErrorValue(e.getMessage(), childValue);
                            }
                        }
                    }

                    builder.addChildren(Collections.emptyList(), true);
                } catch (ObjectCollectedException | EvaluateException e) {
                    builder.setErrorMessage(
                            DebuggerBundle.message("evaluation.error.array.collected"));
                }
            }
        }
    }
}

