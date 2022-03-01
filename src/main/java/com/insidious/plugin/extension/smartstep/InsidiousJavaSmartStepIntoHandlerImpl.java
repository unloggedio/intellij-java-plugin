package com.insidious.plugin.extension.smartstep;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.actions.LambdaSmartStepTarget;
import com.intellij.debugger.actions.MethodSmartStepTarget;
import com.intellij.debugger.actions.SmartStepTarget;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.DocumentUtil;
import com.intellij.util.Range;
import com.intellij.util.ThreeState;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousJavaSmartStepIntoHandler;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluationContextImpl;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import com.insidious.plugin.extension.evaluation.expression.EvaluatorBuilderImpl;
import com.insidious.plugin.extension.evaluation.expression.ExpressionEvaluator;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.util.DebuggerUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.jetbrains.org.objectweb.asm.Label;

import java.util.*;
import java.util.stream.IntStream;

public class InsidiousJavaSmartStepIntoHandlerImpl extends InsidiousJavaSmartStepIntoHandler {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousJavaSmartStepIntoHandler.class);

    private static void removeMatchingMethod(List<MethodSmartStepTarget> targets, String owner, String name, String desc, int ordinal, InsidiousVirtualMachineProxy virtualMachineProxy) {
        Iterator<MethodSmartStepTarget> iterator = targets.iterator();
        while (iterator.hasNext()) {
            MethodSmartStepTarget target = iterator.next();
            if (DebuggerUtil.methodMatches(target
                    .getMethod(), owner
                    .replace("/", "."), name, desc, virtualMachineProxy) && target


                    .getOrdinal() == ordinal) {
                iterator.remove();
                break;
            }
        }
    }

    private static void visitLinesInstructions(final Location location, boolean full, final Set<Integer> lines, final MethodInsnVisitor visitor) {
//        final TObjectIntHashMap<String> myCounter = new TObjectIntHashMap();
//
//        MethodBytecodeUtil.visit(location
//                        .method(),
//                full ? Long.MAX_VALUE : location.codeIndex(), new MethodVisitor(589824) {
//                    boolean myLineMatch = false;
//
//
//                    public void visitJumpInsn(int opcode, Label label) {
//                        if (this.myLineMatch) {
//                            visitor.visitJumpInsn(opcode, label);
//                        }
//                    }
//
//
//                    public void visitCode() {
//                        visitor.visitCode();
//                    }
//
//
//                    public void visitLineNumber(int line, Label start) {
//                        this.myLineMatch = lines.contains(Integer.valueOf(line - 1));
//                    }
//
//
//                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//                        if (this.myLineMatch) {
//                            String key = owner + "." + name + desc;
//                            final int currentCount = myCounter.get(key);
//                            myCounter.put(key, currentCount + 1);
//                            if (name.startsWith("access$")) {
//
//                                ReferenceType cls = (ReferenceType) ContainerUtil.getFirstItem(location
//                                        .virtualMachine().classesByName(owner));
//                                if (cls != null) {
//                                    Method method = DebuggerUtils.findMethod(cls, name, desc);
//                                    if (method != null) {
//                                        MethodBytecodeUtil.visit(method, new MethodVisitor(589824) {
//
//
//                                            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//                                                if ("java/lang/AbstractMethodError"
//                                                        .equals(owner)) {
//                                                    return;
//                                                }
//                                                visitor.visitMethodInsn(opcode, owner, name, desc, itf, currentCount);
//                                            }
//                                        } false)
//
//                                    }
//
//                                }
//
//
//                            } else {
//
//
//                                visitor.visitMethodInsn(opcode, owner, name, desc, itf, currentCount);
//                            }
//                        }
//                    }
//                } true)
    }

    private static StreamEx<MethodSmartStepTarget> immediateMethodCalls(List<SmartStepTarget> targets) {
        return StreamEx.of(targets)
                .select(MethodSmartStepTarget.class)
                .filter(target -> !target.needsBreakpointRequest());
    }

    private static StreamEx<MethodSmartStepTarget> existingMethodCalls(List<SmartStepTarget> targets, PsiMethod psiMethod) {
        return immediateMethodCalls(targets).filter(t -> t.getMethod().equals(psiMethod));
    }

    public boolean isAvailable(SourcePosition position) {
        PsiFile file = position.getFile();
        return file.getLanguage().isKindOf(JavaLanguage.INSTANCE);
    }

    @NotNull
    private Promise<List<SmartStepTarget>> findSmartStepTargetsAsync(SourcePosition position, InsidiousJavaDebugProcess debugProcess, boolean smart) {
        AsyncPromise<List<SmartStepTarget>> res = new AsyncPromise();
        res.setResult(
                ReadAction.compute(() -> {
                    InsidiousXSuspendContext suspendContext = (InsidiousXSuspendContext) debugProcess.getSession().getSuspendContext();

                    InsidiousStackFrameProxy stackFrameProxy = null;

                    if (suspendContext != null) {
                        stackFrameProxy = suspendContext.getFrameProxy();
                    }

                    return findStepTargets(position, debugProcess, suspendContext, stackFrameProxy, smart);
                }));

        return res;
    }

    @NotNull
    public Promise<List<SmartStepTarget>> findSmartStepTargetsAsync(SourcePosition position, InsidiousJavaDebugProcess debugProcess) {
        return findSmartStepTargetsAsync(position, debugProcess, true);
    }

    @NotNull
    public Promise<List<SmartStepTarget>> findStepIntoTargets(SourcePosition position, InsidiousJavaDebugProcess debugProcess) {
        if ((DebuggerSettings.getInstance()).ALWAYS_SMART_STEP_INTO) {
            return findSmartStepTargetsAsync(position, debugProcess, false);
        }
        return Promises.rejectedPromise();
    }

    @NotNull
    public List<SmartStepTarget> findSmartStepTargets(SourcePosition position) {
        throw new IllegalStateException("Should not be used");
    }

    protected List<SmartStepTarget> findStepTargets(final SourcePosition position, InsidiousJavaDebugProcess debugProcess,
                                                    final XSuspendContext suspendContext,
                                                    final InsidiousStackFrameProxy stackFrameProxy, boolean smart) {
        SourcePosition targetPosition = position;
        if (debugProcess.getLastDirectionType() == DirectionType.BACKWARDS) {
            boolean isStartOfLine = true;
            if (stackFrameProxy != null) {

                try {
//                    isStartOfLine = LocationUtil.isLocationStartOfLine(stackFrameProxy.location());
                } catch (Exception ex) {
                    logger.debug("failed", ex);
                    return Collections.emptyList();
                }
            }
            if (isStartOfLine) {

                int i = Math.max(0, position.getLine() - 1);
                targetPosition = SourcePosition.createFromLine(position.getFile(), i);
            }
        }

        int line = targetPosition.getLine();
        if (line < 0) {
            return Collections.emptyList();
        }

        PsiFile file = targetPosition.getFile();
        VirtualFile vFile = file.getVirtualFile();
        if (vFile == null) {
            return Collections.emptyList();
        }

        Document doc = FileDocumentManager.getInstance().getDocument(vFile);
        if (doc == null) return Collections.emptyList();
        if (line >= doc.getLineCount()) {
            return Collections.emptyList();
        }
        TextRange curLineRange = DocumentUtil.getLineTextRange(doc, line);
        PsiElement element = targetPosition.getElementAt();
        PsiElement body = DebuggerUtilsEx.getBody(DebuggerUtilsEx.getContainingMethod(element));

        final TextRange lineRange = (body != null) ? curLineRange.intersection(body.getTextRange()) : curLineRange;

        if (lineRange == null || lineRange.isEmpty()) {
            return Collections.emptyList();
        }

        if (element != null && !(element instanceof com.intellij.psi.PsiCompiledElement)) {
            while (true) {
                PsiElement parent = element.getParent();
                if (parent == null || parent.getTextOffset() < lineRange.getStartOffset()) {
                    break;
                }
                element = parent;
            }

            final List<SmartStepTarget> targets = new ArrayList<>();

            final Ref<TextRange> textRange = new Ref(lineRange);

            JavaRecursiveElementVisitor javaRecursiveElementVisitor = new JavaRecursiveElementVisitor() {
                final Deque<PsiMethod> myContextStack = new LinkedList<>();
                final Deque<String> myParamNameStack = new LinkedList<>();
                private int myNextLambdaExpressionOrdinal = 0;
                private boolean myInsideLambda = false;

                @Nullable
                private String getCurrentParamName() {
                    return this.myParamNameStack.peekFirst();
                }


                public void visitAnonymousClass(PsiAnonymousClass aClass) {
                    PsiExpressionList argumentList = aClass.getArgumentList();
                    if (argumentList != null) {
                        argumentList.accept(this);
                    }
                    for (PsiMethod psiMethod : aClass.getMethods()) {
                        targets.add(0, new MethodSmartStepTarget(psiMethod,


                                getCurrentParamName(), psiMethod
                                .getBody(), true, null));
                    }
                }


                public void visitLambdaExpression(PsiLambdaExpression expression) {
                    boolean inLambda = this.myInsideLambda;
                    this.myInsideLambda = true;
                    super.visitLambdaExpression(expression);
                    this.myInsideLambda = inLambda;
                    targets.add(0, new LambdaSmartStepTarget(expression,


                            getCurrentParamName(), expression
                            .getBody(), this.myNextLambdaExpressionOrdinal++, null, !this.myInsideLambda));
                }


                public void visitMethodReferenceExpression(PsiMethodReferenceExpression expression) {
                    PsiElement element = expression.resolve();
                    if (element instanceof PsiMethod) {
                        PsiElement navMethod = element.getNavigationElement();
                        if (navMethod instanceof PsiMethod) {
                            targets.add(0, new MethodSmartStepTarget((PsiMethod) navMethod, null, expression, true, null));
                        }
                    }
                }


                public void visitField(PsiField field) {
                    if (checkTextRange(field, false)) {
                        super.visitField(field);
                    }
                }


                public void visitMethod(PsiMethod method) {
                    if (checkTextRange(method, false)) {
                        super.visitMethod(method);
                    }
                }


                public void visitStatement(PsiStatement statement) {
                    if (checkTextRange(statement, true)) {
                        super.visitStatement(statement);
                    }
                }


                public void visitIfStatement(PsiIfStatement statement) {
                    visitConditional(statement
                            .getCondition(), statement
                            .getThenBranch(), statement
                            .getElseBranch());
                }


                public void visitConditionalExpression(PsiConditionalExpression expression) {
                    visitConditional(expression
                            .getCondition(), expression
                            .getThenExpression(), expression
                            .getElseExpression());
                }


                private void visitConditional(@Nullable PsiElement condition, @Nullable PsiElement thenBranch, @Nullable PsiElement elseBranch) {
                    if (condition != null && checkTextRange(condition, true)) {
                        condition.accept(this);
                    }
                    ThreeState conditionRes = evaluateCondition(condition);
                    if (conditionRes != ThreeState.NO && thenBranch != null &&

                            checkTextRange(thenBranch, true)) {
                        thenBranch.accept(this);
                    }
                    if (conditionRes != ThreeState.YES && elseBranch != null &&

                            checkTextRange(elseBranch, true)) {
                        elseBranch.accept(this);
                    }
                }

                private ThreeState evaluateCondition(@Nullable PsiElement condition) {
                    if (condition != null && !DebuggerUtils.hasSideEffects(condition)) {

                        try {
                            ObjectReference thisObjectReference = stackFrameProxy.getStackFrame().thisObject();
                            EvaluationContextImpl evaluationContextImpl = new EvaluationContextImpl(suspendContext, stackFrameProxy, thisObjectReference);


                            ExpressionEvaluator evaluator = EvaluatorBuilderImpl.getInstance().build(condition, position, evaluationContextImpl);
                            return ThreeState.fromBoolean(
                                    EvaluatorUtil.evaluateBoolean(evaluator, evaluationContextImpl));
                        } catch (EvaluateException e) {
                            logger.info("failed", e);
                        }
                    }
                    return ThreeState.UNSURE;
                }


                public void visitExpression(PsiExpression expression) {
                    checkTextRange(expression, true);
                    super.visitExpression(expression);
                }

                boolean checkTextRange(@NotNull PsiElement expression, boolean expand) {
                    TextRange range = expression.getTextRange();
                    if (lineRange.intersects(range)) {
                        if (expand) {
                            textRange.set(textRange.get().union(range));
                        }
                        return true;
                    }
                    return false;
                }


                public void visitExpressionList(PsiExpressionList expressionList) {
                    visitArguments(expressionList, this.myContextStack.peekFirst());
                }

                void visitArguments(PsiExpressionList expressionList, PsiMethod psiMethod) {
                    if (psiMethod != null) {
                        String methodName = psiMethod.getName();
                        PsiExpression[] expressions = expressionList.getExpressions();

                        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
                        for (int idx = 0; idx < expressions.length; idx++) {


                            String paramName = (idx < parameters.length && !parameters[idx].isVarArgs()) ? parameters[idx].getName() : ("arg" + (idx + 1));
                            this.myParamNameStack.push(methodName + ": " + paramName + ".");
                            PsiExpression argExpression = expressions[idx];

                        }


                    } else {

                        super.visitExpressionList(expressionList);
                    }
                }


                public void visitCallExpression(PsiCallExpression expression) {
                    int pos = -1;
                    if (this.myContextStack
                            .isEmpty()) {
                        pos = targets.size();
                    }
                    PsiMethod psiMethod = expression.resolveMethod();
                    boolean isMethodCall = expression instanceof PsiMethodCallExpression;
                    if (isMethodCall) {


                        PsiExpression qualifier = ((PsiMethodCallExpression) expression).getMethodExpression().getQualifierExpression();
                        if (qualifier != null) {
                            qualifier.accept(this);
                        }
                        visitArguments(expression.getArgumentList(), psiMethod);
                    }
                    if (psiMethod != null) {
                        this.myContextStack.push(psiMethod);


                        MethodSmartStepTarget target = new MethodSmartStepTarget(psiMethod, null, isMethodCall ? ((PsiMethodCallExpression) expression).getMethodExpression().getReferenceNameElement() : ((expression instanceof PsiNewExpression) ? ((PsiNewExpression) expression).getClassOrAnonymousClassReference() : expression), (this.myInsideLambda || (expression instanceof PsiNewExpression && ((PsiNewExpression) expression).getAnonymousClass() != null)), null);


                        target.setOrdinal(
                                (int) InsidiousJavaSmartStepIntoHandlerImpl.existingMethodCalls(targets, psiMethod).count());
                        if (pos != -1) {
                            targets.add(pos, target);
                        } else {
                            targets.add(target);
                        }
                    }
                    try {
                        if (isMethodCall) {
                            checkTextRange(expression, true);
                        } else {
                            super.visitCallExpression(expression);
                        }
                    } finally {
                        if (psiMethod != null) {
                            this.myContextStack.pop();
                        }
                    }
                }
            };

            element.accept(javaRecursiveElementVisitor);
            PsiElement sibling = element.getNextSibling();
            for (; sibling != null;
                 sibling = sibling.getNextSibling()) {
                if (!lineRange.intersects(sibling.getTextRange())) {
                    break;
                }
                sibling.accept(javaRecursiveElementVisitor);
            }


            Range<Integer> sourceLines = new Range(Integer.valueOf(doc.getLineNumber(textRange.get().getStartOffset())), Integer.valueOf(doc.getLineNumber(textRange.get().getEndOffset())));
            targets.forEach(t -> t.setCallingExpressionLines(sourceLines));

            Set<Integer> lines = new HashSet<>();
            Objects.requireNonNull(lines);
            IntStream.rangeClosed(sourceLines.getFrom().intValue(), sourceLines.getTo().intValue()).forEach(lines::add);

            LineNumbersMapping mapping = vFile.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
            if (mapping != null) {


                lines = ((StreamEx) StreamEx.of(lines).map(l -> Integer.valueOf(mapping.sourceToBytecode(l.intValue() + 1) - 1)).filter(l -> (l.intValue() >= 0))).toSet();
            }

            if (!targets.isEmpty()) {
                if (stackFrameProxy != null) {
                    VirtualMachineProxy virtualMachine = stackFrameProxy.getVirtualMachine();
                    if (!virtualMachine.canGetBytecodes()) {
                        return smart ? targets : Collections.emptyList();
                    }


                    try {
                        List<MethodSmartStepTarget> methodTargets = immediateMethodCalls(targets).toList();
                        visitLinesInstructions(stackFrameProxy
                                .location(), true, lines, (opcode, owner, name, desc, itf, ordinal) -> removeMatchingMethod(methodTargets, owner, name, desc, ordinal, debugProcess.getConnector()));


                        if (!methodTargets.isEmpty()) {
                            logger.debug("Sanity check failed for: " + methodTargets);
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        logger.info("failed", e);
                        return smart ? targets : Collections.emptyList();
                    }

                    try {
                        ArrayList<SmartStepTarget> all = new ArrayList<>(targets);

                        JumpsAndInsnVisitor visitor = new JumpsAndInsnVisitor(targets, debugProcess);


                        visitLinesInstructions(stackFrameProxy.location(), false, lines, visitor);


                        if (debugProcess.getLastDirectionType() == DirectionType.BACKWARDS) {
                            targets.clear();
                            targets.addAll(visitor.getExecutedTargets());
                        }

                        if (!smart && !targets.isEmpty()) {
                            ArrayList<SmartStepTarget> copy = new ArrayList<>(targets);

                            visitor.setJumpsToIgnore(visitor.getJumpCounter() + 1);
                            visitLinesInstructions(stackFrameProxy
                                    .location(), true, lines, visitor);


                            if (!targets.isEmpty() &&
                                    !immediateMethodCalls(targets).findAny().isPresent()) {
                                targets.clear();
                                targets.addAll(copy);
                            }
                        }


                        ArrayList<SmartStepTarget> removed = new ArrayList<>(all);
                        removed.removeAll(targets);
                        for (SmartStepTarget m : removed) {
                            MethodSmartStepTarget target = (MethodSmartStepTarget) m;
                            existingMethodCalls(all, target.getMethod())
                                    .forEach(t -> {
                                        int ordinal = t.getOrdinal();

                                        if (ordinal > target.getOrdinal()) {
                                            t.setOrdinal(ordinal - 1);
                                        }
                                    });
                        }
                    } catch (Exception e) {
                        logger.info("failed", e);
                        return Collections.emptyList();
                    }
                }

                return targets;
            }
        }
        return Collections.emptyList();
    }


    private interface MethodInsnVisitor {
        void visitMethodInsn(int param1Int1, String param1String1, String param1String2, String param1String3, boolean param1Boolean, int param1Int2);


        default void visitJumpInsn(int opcode, Label label) {
        }


        default void visitCode() {
        }
    }

    private static class JumpsAndInsnVisitor
            implements MethodInsnVisitor {
        private final List<SmartStepTarget> myTargets;
        private final InsidiousJavaDebugProcess myDebugProcess;
        List<SmartStepTarget> executedTargets;
        private int myJumpCounter;
        private int myJumpsToIgnore;

        JumpsAndInsnVisitor(List<SmartStepTarget> targets, InsidiousJavaDebugProcess debugProcess) {
            this.myTargets = targets;
            this.myDebugProcess = debugProcess;
            this.executedTargets = new ArrayList<>();
        }


        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf, int ordinal) {
            if (this.myJumpCounter >= this.myJumpsToIgnore) {
                Iterator<SmartStepTarget> iterator = this.myTargets.iterator();
                while (iterator.hasNext()) {
                    SmartStepTarget e = iterator.next();
                    if (e instanceof MethodSmartStepTarget &&
                            DebuggerUtil.methodMatches(((MethodSmartStepTarget) e)
                                    .getMethod(), owner
                                    .replace("/", "."), name, desc, this.myDebugProcess


                                    .getConnector()) && ((MethodSmartStepTarget) e)
                            .getOrdinal() == ordinal) {
                        this.executedTargets.add(e);
                        iterator.remove();
                        break;
                    }
                }
            }
        }


        public void visitCode() {
            this.myJumpCounter = 0;
        }


        public void visitJumpInsn(int opcode, Label label) {
            this.myJumpCounter++;
        }

        int getJumpCounter() {
            return this.myJumpCounter;
        }

        void setJumpsToIgnore(int jumpsToIgnore) {
            this.myJumpsToIgnore = jumpsToIgnore;
        }

        public List<SmartStepTarget> getExecutedTargets() {
            return this.executedTargets;
        }
    }
}


