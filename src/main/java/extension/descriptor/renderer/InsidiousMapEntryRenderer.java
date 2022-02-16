package extension.descriptor.renderer;


import com.intellij.debugger.engine.evaluation.CodeFragmentKind;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.InsidiousVirtualMachineProxy;
import extension.descriptor.InsidiousJavaValuePresentation;
import extension.descriptor.InsidiousValueDescriptor;
import extension.descriptor.InsidiousValueDescriptorImpl;
import extension.descriptor.InsidiousWatchItemDescriptor;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.expression.ExpressionEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class InsidiousMapEntryRenderer extends InsidiousCompoundReferenceRenderer {
    InsidiousMapEntryRenderer(InsidiousArrayRenderer arrayRenderer) {
        super("Map.Entry", new MapEntryLabelRenderer(),


                RendererManager.createEnumerationChildrenRenderer(new String[][]{{"key", "getKey()"}, {"value", "getValue()"}}));

        setClassName("java.util.Map$Entry");
    }

    private static class MapEntryLabelRenderer
            extends InsidiousReferenceRenderer
            implements InsidiousValueLabelRenderer {
        private static final Key<InsidiousValueDescriptorImpl> KEY_DESCRIPTOR = Key.create("KEY_DESCRIPTOR");

        private static final Key<InsidiousValueDescriptorImpl> VALUE_DESCRIPTOR = Key.create("VALUE_DESCRIPTOR");

        private final MyCachedEvaluator myKeyExpression = new MyCachedEvaluator();

        private final MyCachedEvaluator myValueExpression = new MyCachedEvaluator();


        private MapEntryLabelRenderer() {
            super("java.util.Map$Entry");
            this.myKeyExpression.setReferenceExpression(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, "this.getKey()", "", JavaFileType.INSTANCE));


            this.myValueExpression.setReferenceExpression(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, "this.getValue()", "", JavaFileType.INSTANCE));
        }


        public String calcLabel(InsidiousValueDescriptor descriptor,
                                EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
            String keyText = calcExpression(evaluationContext, descriptor, this.myKeyExpression, listener, KEY_DESCRIPTOR);


            String valueText = calcExpression(evaluationContext, descriptor, this.myValueExpression, listener, VALUE_DESCRIPTOR);


            return keyText + " -> " + valueText;
        }


        private String calcExpression(EvaluationContext evaluationContext, InsidiousValueDescriptor descriptor, MyCachedEvaluator evaluator, DescriptorLabelListener listener, Key<InsidiousValueDescriptorImpl> key) throws EvaluateException {
            Value eval = doEval(evaluationContext, descriptor.getValue(), evaluator);
            Project project = EvaluatorUtil.getProject(evaluationContext);
            if (eval != null) {


                InsidiousWatchItemDescriptor evalDescriptor = new InsidiousWatchItemDescriptor(project, evaluator.getReferenceExpression(), eval) {
                    public void updateRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) {
                        updateRepresentationNoNotify(process, labelListener);
                    }
                };
                evalDescriptor.updateRepresentation(evaluationContext
                        .getVirtualMachineProxy().getXDebugProcess(), listener);
                descriptor.putUserData(key, evalDescriptor);
                return evalDescriptor.getValueLabel();
            }
            descriptor.putUserData(key, null);
            return "null";
        }


        public String getUniqueId() {
            return "MapEntry renderer";
        }


        private Value doEval(EvaluationContext evaluationContext, Value originalValue, MyCachedEvaluator cachedEvaluator) throws EvaluateException {
            InsidiousVirtualMachineProxy virtualMachineProxy = evaluationContext.getVirtualMachineProxy();
            Project project = EvaluatorUtil.getProject(evaluationContext);
            if (originalValue == null) {
                return null;
            }

            try {
                ExpressionEvaluator evaluator = cachedEvaluator.getEvaluator(project, evaluationContext);
                if (!virtualMachineProxy.isAttached()) {
                    throw EvaluateExceptionUtil.PROCESS_EXITED;
                }

                EvaluationContext thisEvaluationContext = evaluationContext.createEvaluationContext(originalValue);
                return evaluator.evaluate(thisEvaluationContext);
            } catch (EvaluateException ex) {
                throw new EvaluateException(
                        DebuggerBundle.message("error.unable.to.evaluate.expression") + " " + ex

                                .getMessage(), ex);
            }
        }

        @NotNull
        public XValuePresentation getPresentation(InsidiousValueDescriptorImpl descriptor) {
            final boolean inCollection = descriptor instanceof com.intellij.debugger.ui.tree.ArrayElementDescriptor;
            return new InsidiousJavaValuePresentation(descriptor) {
                public void renderValue(@NotNull XValuePresentation.XValueTextRenderer renderer, @Nullable XValueNodeImpl node) {
                    renderDescriptor(MapEntryLabelRenderer.KEY_DESCRIPTOR, renderer, node);
                    renderer.renderComment(" -> ");
                    renderDescriptor(MapEntryLabelRenderer.VALUE_DESCRIPTOR, renderer, node);
                }


                private void renderDescriptor(Key<InsidiousValueDescriptorImpl> key, @NotNull XValuePresentation.XValueTextRenderer renderer, @Nullable XValueNodeImpl node) {
                    InsidiousValueDescriptorImpl valueDescriptor = this.myValueDescriptor.getUserData(key);
                    if (valueDescriptor != null) {
                        String type = valueDescriptor.getIdLabel();
                        if (inCollection && type != null) {
                            renderer.renderComment("{" + type + "} ");
                        }
                        (new InsidiousJavaValuePresentation(valueDescriptor)).renderValue(renderer, node);
                    } else {
                        renderer.renderValue("null");
                    }
                }


                @NotNull
                public String getSeparator() {
                    return inCollection ? "" : super.getSeparator();
                }


                public boolean isShowName() {
                    return !inCollection;
                }


                @Nullable
                public String getType() {
                    return inCollection ? null : super.getType();
                }
            };
        }

        private class MyCachedEvaluator extends InsidiousCachedEvaluator {
            private MyCachedEvaluator() {
            }

            protected String getClassName() {
                return MapEntryLabelRenderer.this.getClassName();
            }


            public ExpressionEvaluator getEvaluator(Project project, EvaluationContext evaluationContext) throws EvaluateException {
                return super.getEvaluator(project, evaluationContext);
            }
        }
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousMapEntryRenderer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */