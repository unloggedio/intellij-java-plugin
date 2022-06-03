package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.thread.InsidiousLocalVariableProxy;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.insidious.plugin.extension.connector.DecompiledLocalVariable;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.connector.LocalVariablesUtil;
import com.insidious.plugin.extension.descriptor.InsidiousWatchItemDescriptor;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


class LocalVariableEvaluator
        implements Evaluator {
    private static final Logger logger = LoggerUtil.getInstance(LocalVariableEvaluator.class);

    private final String myLocalVariableName;
    private final boolean myCanScanFrames;
    private EvaluationContext myContext;
    private InsidiousLocalVariableProxy myEvaluatedVariable;
    private DecompiledLocalVariable myEvaluatedDecompiledVariable;

    LocalVariableEvaluator(String localVariableName, boolean canScanFrames) {
        this.myLocalVariableName = localVariableName;
        this.myCanScanFrames = canScanFrames;
    }

    @Nullable
    private static PsiVariable resolveVariable(StackFrameProxy frame, String name, InsidiousVirtualMachineProxy virtualMachineProxy) {
        XDebugProcess process = virtualMachineProxy.getXDebugProcess();
        Project project = process.getSession().getProject();

        try {
            SourcePosition position = virtualMachineProxy.getPositionManager().getSourcePosition(frame.location());
            PsiElement place = position.getElementAt();
            if (place == null) {
                return null;
            }
            return ReadAction.compute(() -> JavaPsiFacade.getInstance(project).getResolveHelper().resolveReferencedVariable(name, place));


        } catch (NoDataException | EvaluateException e) {
            return null;
        }
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        InsidiousStackFrameProxy frameProxy = context.getStackFrameProxy();
        if (frameProxy == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.no.stackframe"));
        }

        try {
            InsidiousThreadReferenceProxy threadProxy = null;
            int lastFrameIndex = -1;
            PsiVariable variable = null;

            boolean topFrame = true;


            while (true) {
                try {
                    InsidiousLocalVariableProxy local = frameProxy.visibleVariableByName(this.myLocalVariableName);
                    if (local != null) {
                        if (!topFrame) {
                            if (variable
                                    .equals(
                                            resolveVariable(frameProxy, this.myLocalVariableName, context


                                                    .getVirtualMachineProxy()))) {
                                this.myEvaluatedVariable = local;
                                this.myContext = context;
                                return frameProxy.getValue(local);
                            }
                        } else {
                            this.myEvaluatedVariable = local;
                            this.myContext = context;
                            return frameProxy.getValue(local);
                        }

                    }
                } catch (EvaluateException e) {
                    if (!(e.getCause() instanceof com.sun.jdi.AbsentInformationException)) {
                        throw e;
                    }


                    try {
                        InsidiousStackFrameProxy lambdaFrameProxy = frameProxy;

                        SourcePosition position = ReadAction.compute(() -> context.getVirtualMachineProxy().getPositionManager().getSourcePosition(lambdaFrameProxy.location()));


                        Map<DecompiledLocalVariable, Value> vars = LocalVariablesUtil.fetchValues(frameProxy, position, true);
                        for (Map.Entry<DecompiledLocalVariable, Value> entry : vars.entrySet()) {
                            DecompiledLocalVariable var = entry.getKey();
                            if (var.getMatchedNames().contains(this.myLocalVariableName) || var
                                    .getDefaultName().equals(this.myLocalVariableName)) {
                                this.myEvaluatedDecompiledVariable = var;
                                this.myContext = context;
                                return entry.getValue();
                            }
                        }
                    } catch (Exception e1) {
                        logger.info("failed", e1);
                    }
                }

                if (this.myCanScanFrames) {
                    if (topFrame) {

                        variable = resolveVariable(frameProxy, this.myLocalVariableName, context


                                .getVirtualMachineProxy());
                        if (variable == null)
                            break;
                    }
                    if (threadProxy == null) {
                        threadProxy = frameProxy.threadProxy();
                        lastFrameIndex = threadProxy.frameCount() - 1;
                    }
                    int currentFrameIndex = frameProxy.getFrameIndex();
                    if (currentFrameIndex < lastFrameIndex) {
                        frameProxy = threadProxy.frame(currentFrameIndex + 1);
                        if (frameProxy != null) {
                            topFrame = false;

                            continue;
                        }
                    }
                }
                break;
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.local.variable.missing", this.myLocalVariableName));
        } catch (EvaluateException e) {
            this.myEvaluatedVariable = null;
            this.myContext = null;
            throw e;
        }
    }

    public Modifier getModifier() {
        Modifier modifier = null;
        if ((this.myEvaluatedVariable != null || this.myEvaluatedDecompiledVariable != null) && this.myContext != null) {
            modifier = new Modifier() {
                public boolean canInspect() {
                    return true;
                }


                public boolean canSetValue() {
                    return true;
                }


                public void setValue(Value value) throws ClassNotLoadedException, InvalidTypeException {
                    InsidiousStackFrameProxy frameProxy = LocalVariableEvaluator.this.myContext.getStackFrameProxy();
                    try {
                        assert frameProxy != null;
                        if (LocalVariableEvaluator.this.myEvaluatedVariable != null) {
                            frameProxy.setValue(LocalVariableEvaluator.this.myEvaluatedVariable, value);
                        } else {
                            LocalVariablesUtil.setValue(frameProxy
                                    .getStackFrame(), LocalVariableEvaluator.this
                                    .myEvaluatedDecompiledVariable.getSlot(), value);
                        }

                    } catch (EvaluateException e) {
                        logger.error("failed", e);
                    }
                }


                public Type getExpectedType() throws ClassNotLoadedException {
                    try {
                        InsidiousStackFrameProxy frameProxy = LocalVariableEvaluator.this.myContext.getStackFrameProxy();
                        return frameProxy.getType(LocalVariableEvaluator.this.myEvaluatedVariable);
                    } catch (EvaluateException e) {
                        logger.error("failed", e);
                        return null;
                    }
                }


                public InsidiousNodeDescriptorImpl getInspectItem(Project project) {
                    InsidiousStackFrameProxy frameProxy = LocalVariableEvaluator.this.myContext.getStackFrameProxy();
                    try {
                        String varName = frameProxy.getVariableName(LocalVariableEvaluator.this.myEvaluatedVariable);
                        Value varValue = frameProxy.getValue(LocalVariableEvaluator.this.myEvaluatedVariable);

                        TextWithImports text = TextWithImportsImpl.fromXExpression(
                                XExpressionImpl.fromText(varName));
                        return new InsidiousWatchItemDescriptor(project, text, varValue);
                    } catch (EvaluateException e) {
                        logger.debug("failed", e);
                        return null;
                    }
                }
            };
        }
        return modifier;
    }

    public String toString() {
        return this.myLocalVariableName;
    }
}


