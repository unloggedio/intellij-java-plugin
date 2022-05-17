package com.insidious.plugin.extension.evaluation.expression;


import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.JVMNameUtil;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.*;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.engine.evaluation.expression.UnsupportedExpressionException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class EvaluatorBuilderImpl implements EvaluatorBuilder {
    private static final EvaluatorBuilderImpl ourInstance = new EvaluatorBuilderImpl();

    private static final TokenSet SHIFT_OPS = TokenSet.create(JavaTokenType.LTLT, JavaTokenType.GTGT, JavaTokenType.GTGTGT);


    public static EvaluatorBuilder getInstance() {
        return ourInstance;
    }


    public static ExpressionEvaluator build(TextWithImports text, @Nullable PsiElement contextElement, @Nullable SourcePosition position, @NotNull Project project, EvaluationContext evaluationContext) throws EvaluateException {
        CodeFragmentFactory factory = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(text, contextElement);
        JavaCodeFragment javaCodeFragment = factory.createCodeFragment(text, contextElement, project);
        if (javaCodeFragment == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.invalid.expression", text.getText()));
        }
        DebuggerUtils.checkSyntax(javaCodeFragment);
        return ourInstance.build(javaCodeFragment, position, evaluationContext);
    }


    public ExpressionEvaluator build(PsiElement codeFragment, SourcePosition position, EvaluationContext evaluationContext) throws EvaluateException {
        return (new Builder(position, evaluationContext)).buildElement(codeFragment);
    }

    private static final class Builder extends JavaElementVisitor {
        private static final Logger logger = LoggerUtil.getInstance(EvaluatorBuilderImpl.class);
        private final Set<JavaCodeFragment> myVisitedFragments = new HashSet<>();
        @Nullable
        private final SourcePosition myPosition;
        @Nullable
        private final PsiClass myPositionPsiClass;
        private final EvaluationContext myEvaluationContext;
        private Evaluator myResult = null;
        private PsiClass myContextPsiClass;
        private CodeFragmentEvaluator myCurrentFragmentEvaluator;

        private Builder(@Nullable SourcePosition position, EvaluationContext evaluationContext) {
            this.myPosition = position;
            this.myPositionPsiClass = JVMNameUtil.getClassAt(this.myPosition);
            this.myEvaluationContext = evaluationContext;
        }

        private static Evaluator handleAssignmentBoxingAndPrimitiveTypeConversions(PsiType lType, PsiType rType, Evaluator rEvaluator, Project project, EvaluationContext evaluationContext) {
            PsiPrimitiveType psiPrimitiveType = PsiPrimitiveType.getUnboxedType(lType);

            if (psiPrimitiveType != null) {
                if (rType instanceof PsiPrimitiveType && !PsiType.NULL.equals(rType)) {
                    if (!rType.equals(psiPrimitiveType)) {
                        rEvaluator = createTypeCastEvaluator(rEvaluator, psiPrimitiveType, evaluationContext);
                    }

                    rEvaluator = new BoxingEvaluator(rEvaluator);
                }

            } else if (lType instanceof PsiPrimitiveType) {
                if (rType instanceof PsiClassType) {
                    rEvaluator = new UnBoxingEvaluator(rEvaluator);
                }
                PsiPrimitiveType unboxedRType = PsiPrimitiveType.getUnboxedType(rType);
                PsiType _rType = (unboxedRType != null) ? unboxedRType : rType;
                if (_rType instanceof PsiPrimitiveType && !PsiType.NULL.equals(_rType) &&
                        !lType.equals(_rType)) {
                    rEvaluator = createTypeCastEvaluator(rEvaluator, lType, evaluationContext);
                }
            } else if (lType instanceof PsiClassType && rType instanceof PsiPrimitiveType &&

                    !PsiType.NULL.equals(rType)) {


                PsiClassType rightBoxed = ((PsiPrimitiveType) rType).getBoxedType(
                        PsiManager.getInstance(project), ((PsiClassType) lType)
                                .getResolveScope());
                if (rightBoxed != null && TypeConversionUtil.isAssignable(lType, rightBoxed)) {
                    rEvaluator = new BoxingEvaluator(rEvaluator);
                }
            }

            return rEvaluator;
        }

        private static String getLabel(PsiElement element) {
            String label = null;
            if (element.getParent() instanceof PsiLabeledStatement) {
                label = ((PsiLabeledStatement) element.getParent()).getName();
            }
            return label;
        }

        private static Evaluator createBinaryEvaluator(Evaluator lResult, PsiType lType, Evaluator rResult, @NotNull PsiType rType, @NotNull IElementType operation, @NotNull PsiType expressionExpectedType, EvaluationContext evaluationContext) {
            if (isUnboxingInBinaryExpressionApplicable(lType, rType, operation)) {
                if (rType instanceof PsiClassType &&
                        UnBoxingEvaluator.isTypeUnboxable(rType.getCanonicalText())) {
                    rResult = new UnBoxingEvaluator(rResult);
                }
                if (lType instanceof PsiClassType &&
                        UnBoxingEvaluator.isTypeUnboxable(lType.getCanonicalText())) {
                    lResult = new UnBoxingEvaluator(lResult);
                }
            }
            if (isBinaryNumericPromotionApplicable(lType, rType, operation)) {
                PsiPrimitiveType psiPrimitiveType1 = null, psiPrimitiveType2 = null;
                PsiType _lType = lType;
                PsiPrimitiveType unboxedLType = PsiPrimitiveType.getUnboxedType(lType);
                if (unboxedLType != null) {
                    psiPrimitiveType1 = unboxedLType;
                }

                PsiType _rType = rType;
                PsiPrimitiveType unboxedRType = PsiPrimitiveType.getUnboxedType(rType);
                if (unboxedRType != null) {
                    psiPrimitiveType2 = unboxedRType;
                }


                if (PsiType.DOUBLE.equals(psiPrimitiveType1)) {
                    if (TypeConversionUtil.areTypesConvertible(psiPrimitiveType2, PsiType.DOUBLE)) {
                        rResult = createTypeCastEvaluator(rResult, PsiType.DOUBLE, evaluationContext);
                    }
                } else if (PsiType.DOUBLE.equals(psiPrimitiveType2)) {
                    if (TypeConversionUtil.areTypesConvertible(psiPrimitiveType1, PsiType.DOUBLE)) {
                        lResult = createTypeCastEvaluator(lResult, PsiType.DOUBLE, evaluationContext);
                    }
                } else if (PsiType.FLOAT.equals(psiPrimitiveType1)) {
                    if (TypeConversionUtil.areTypesConvertible(psiPrimitiveType2, PsiType.FLOAT)) {
                        rResult = createTypeCastEvaluator(rResult, PsiType.FLOAT, evaluationContext);
                    }
                } else if (PsiType.FLOAT.equals(psiPrimitiveType2)) {
                    if (TypeConversionUtil.areTypesConvertible(psiPrimitiveType1, PsiType.FLOAT)) {
                        lResult = createTypeCastEvaluator(lResult, PsiType.FLOAT, evaluationContext);
                    }
                } else if (PsiType.LONG.equals(psiPrimitiveType1)) {
                    if (TypeConversionUtil.areTypesConvertible(psiPrimitiveType2, PsiType.LONG)) {
                        rResult = createTypeCastEvaluator(rResult, PsiType.LONG, evaluationContext);
                    }
                } else if (PsiType.LONG.equals(psiPrimitiveType2)) {
                    if (TypeConversionUtil.areTypesConvertible(psiPrimitiveType1, PsiType.LONG)) {
                        lResult = createTypeCastEvaluator(lResult, PsiType.LONG, evaluationContext);
                    }
                } else {
                    if (!PsiType.INT.equals(psiPrimitiveType1) &&
                            TypeConversionUtil.areTypesConvertible(psiPrimitiveType1, PsiType.INT)) {
                        lResult = createTypeCastEvaluator(lResult, PsiType.INT, evaluationContext);
                    }
                    if (!PsiType.INT.equals(psiPrimitiveType2) &&
                            TypeConversionUtil.areTypesConvertible(psiPrimitiveType2, PsiType.INT)) {
                        rResult = createTypeCastEvaluator(rResult, PsiType.INT, evaluationContext);
                    }
                }

            } else if (EvaluatorBuilderImpl.SHIFT_OPS.contains(operation)) {
                lResult = handleUnaryNumericPromotion(lType, lResult, evaluationContext);
                rResult = handleUnaryNumericPromotion(rType, rResult, evaluationContext);
            }

            return DisableGC.create(new BinaryExpressionEvaluator(lResult, rResult, operation, expressionExpectedType


                    .getCanonicalText()));
        }

        private static boolean isBinaryNumericPromotionApplicable(PsiType lType, PsiType rType, IElementType opType) {
            if (lType == null || rType == null) {
                return false;
            }
            if (!TypeConversionUtil.isNumericType(lType) ||
                    !TypeConversionUtil.isNumericType(rType)) {
                return false;
            }
            if (opType == JavaTokenType.EQEQ || opType == JavaTokenType.NE) {
                if (PsiType.NULL.equals(lType) || PsiType.NULL.equals(rType)) {
                    return false;
                }
                if (lType instanceof PsiClassType && rType instanceof PsiClassType) {
                    return false;
                }
                if (lType instanceof PsiClassType) {
                    return (PsiPrimitiveType.getUnboxedType(lType) != null);
                }
                if (rType instanceof PsiClassType) {
                    return (PsiPrimitiveType.getUnboxedType(rType) != null);
                }
                return true;
            }

            return (opType == JavaTokenType.ASTERISK || opType == JavaTokenType.DIV || opType == JavaTokenType.PERC || opType == JavaTokenType.PLUS || opType == JavaTokenType.MINUS || opType == JavaTokenType.LT || opType == JavaTokenType.LE || opType == JavaTokenType.GT || opType == JavaTokenType.GE || opType == JavaTokenType.AND || opType == JavaTokenType.XOR || opType == JavaTokenType.OR);
        }

        private static boolean isUnboxingInBinaryExpressionApplicable(PsiType lType, PsiType rType, IElementType opCode) {
            if (PsiType.NULL.equals(lType) || PsiType.NULL.equals(rType)) {
                return false;
            }

            if (opCode == JavaTokenType.EQEQ || opCode == JavaTokenType.NE) {
                return ((lType instanceof PsiPrimitiveType && rType instanceof PsiClassType) || (lType instanceof PsiClassType && rType instanceof PsiPrimitiveType));
            }


            if (opCode == JavaTokenType.PLUS && ((
                    lType instanceof PsiClassType && lType
                            .equalsToText("java.lang.String")) || (rType instanceof PsiClassType && rType

                    .equalsToText("java.lang.String")))) {
                return false;
            }


            return (lType instanceof PsiClassType || rType instanceof PsiClassType);
        }

        @Nullable
        private static PsiType calcUnaryNumericPromotionType(PsiPrimitiveType type) {
            if (PsiType.BYTE.equals(type) || PsiType.SHORT
                    .equals(type) || PsiType.CHAR
                    .equals(type) || PsiType.INT
                    .equals(type)) {
                return PsiType.INT;
            }
            return null;
        }

        private static Evaluator createFallbackEvaluator(final Evaluator primary, final Evaluator fallback) {
            return new Evaluator() {
                private boolean myIsFallback;

                public Object evaluate(EvaluationContext context) throws EvaluateException {
                    try {
                        return primary.evaluate(context);
                    } catch (EvaluateException e) {
                        try {
                            Object res = fallback.evaluate(context);
                            this.myIsFallback = true;
                            return res;
                        } catch (EvaluateException e1) {
                            throw e;
                        }
                    }
                }


                public Modifier getModifier() {
                    return this.myIsFallback ? fallback.getModifier() : primary.getModifier();
                }
            };
        }

        private static void throwExpressionInvalid(PsiElement expression) {
            throwEvaluateException(
                    DebuggerBundle.message("evaluation.error.invalid.expression", expression.getText()));
        }

        private static void throwEvaluateException(String message) throws EvaluateRuntimeException {
            throw new EvaluateRuntimeException(
                    EvaluateExceptionUtil.createEvaluateException(message));
        }

        private static int calcDepth(PsiElement targetClass, PsiClass fromClass, boolean checkInheritance) {
            int iterationCount = 0;
            while (fromClass != null &&
                    !fromClass.equals(targetClass) && (!checkInheritance ||

                    !fromClass.isInheritor((PsiClass) targetClass, true))) {
                iterationCount++;
                fromClass = getOuterClass(fromClass);
            }
            return (fromClass != null) ? iterationCount : -1;
        }

        private static Evaluator handleUnaryNumericPromotion(PsiType operandExpressionType, Evaluator operandEvaluator, EvaluationContext evaluationContext) {
            PsiPrimitiveType unboxedType = PsiPrimitiveType.getUnboxedType(operandExpressionType);
            if (unboxedType != null && !PsiType.BOOLEAN.equals(unboxedType)) {
                operandEvaluator = new UnBoxingEvaluator(operandEvaluator);
            }


            PsiType _unboxedIndexType = (unboxedType != null) ? unboxedType : operandExpressionType;
            if (_unboxedIndexType instanceof PsiPrimitiveType) {

                PsiType promotionType = calcUnaryNumericPromotionType((PsiPrimitiveType) _unboxedIndexType);
                if (promotionType != null) {
                    operandEvaluator = createTypeCastEvaluator(operandEvaluator, promotionType, evaluationContext);
                }
            }

            return operandEvaluator;
        }

        private static TypeCastEvaluator createTypeCastEvaluator(Evaluator operandEvaluator, PsiType castType, EvaluationContext evaluationContext) {
            if (castType instanceof PsiPrimitiveType) {
                return new TypeCastEvaluator(operandEvaluator, castType.getCanonicalText());
            }
            return new TypeCastEvaluator(operandEvaluator, new TypeEvaluator(

                    JVMNameUtil.getJVMQualifiedName(castType)));
        }

        @Nullable
        private static PsiClass getOuterClass(PsiClass aClass) {
            return (aClass == null) ?
                    null :
                    PsiTreeUtil.getContextOfType(aClass, PsiClass.class, true);
        }

        public void visitCodeFragment(JavaCodeFragment codeFragment) {
            this.myVisitedFragments.add(codeFragment);
            ArrayList<Evaluator> evaluators = new ArrayList<>();

            CodeFragmentEvaluator oldFragmentEvaluator = setNewCodeFragmentEvaluator();

            try {
                PsiElement child = codeFragment.getFirstChild();
                for (; child != null;
                     child = child.getNextSibling()) {
                    child.accept(this);
                    if (this.myResult != null) {
                        evaluators.add(this.myResult);
                    }
                    this.myResult = null;
                }

                this.myCurrentFragmentEvaluator.setStatements(evaluators.toArray(new Evaluator[0]));
                this.myResult = this.myCurrentFragmentEvaluator;
            } finally {
                this.myCurrentFragmentEvaluator = oldFragmentEvaluator;
            }
        }

        public void visitErrorElement(@NotNull PsiErrorElement element) {
            throwExpressionInvalid(element);
        }

        public void visitAssignmentExpression(PsiAssignmentExpression expression) {
            PsiExpression rExpression = expression.getRExpression();
            if (rExpression == null) {
                throwExpressionInvalid(expression);
            }

            rExpression.accept(this);
            Evaluator rEvaluator = this.myResult;

            PsiExpression lExpression = expression.getLExpression();
            PsiType lType = lExpression.getType();
            if (lType == null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.unknown.expression.type", lExpression.getText()));
            }
            IElementType assignmentType = expression.getOperationTokenType();
            PsiType rType = rExpression.getType();
            if (!TypeConversionUtil.areTypesAssignmentCompatible(lType, rExpression) && rType != null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.incompatible.types", expression.getOperationSign().getText()));
            }
            lExpression.accept(this);
            Evaluator lEvaluator = this.myResult;


            rEvaluator = handleAssignmentBoxingAndPrimitiveTypeConversions(lType, rType, rEvaluator, expression
                    .getProject(), this.myEvaluationContext);

            if (assignmentType != JavaTokenType.EQ) {
                IElementType opType = TypeConversionUtil.convertEQtoOperation(assignmentType);

                PsiType typeForBinOp = TypeConversionUtil.calcTypeForBinaryExpression(lType, rType, opType, true);
                if (typeForBinOp == null || rType == null) {
                    throwEvaluateException(
                            DebuggerBundle.message("evaluation.error.unknown.expression.type", expression.getText()));
                }
                rEvaluator = createBinaryEvaluator(lEvaluator, lType, rEvaluator, rType, opType, typeForBinOp, this.myEvaluationContext);
            }


            this.myResult = new AssignmentEvaluator(lEvaluator, rEvaluator);
        }

        public void visitTryStatement(PsiTryStatement statement) {
            if (statement.getResourceList() != null) {
                throw new EvaluateRuntimeException(new UnsupportedExpressionException("Try with resources is not yet supported"));
            }


            Evaluator bodyEvaluator = accept(statement.getTryBlock());
            if (bodyEvaluator != null) {
                PsiCatchSection[] catchSections = statement.getCatchSections();
                List<CatchEvaluator> evaluators = new ArrayList<>();
                for (PsiCatchSection catchSection : catchSections) {
                    PsiParameter parameter = catchSection.getParameter();
                    PsiCodeBlock catchBlock = catchSection.getCatchBlock();
                    if (parameter != null && catchBlock != null) {
                        CodeFragmentEvaluator oldFragmentEvaluator = setNewCodeFragmentEvaluator();
                    }
                }


                this.myResult = new TryEvaluator(bodyEvaluator, evaluators, accept(statement.getFinallyBlock()));
            }
        }

        public void visitThrowStatement(PsiThrowStatement statement) {
            Evaluator accept = accept(statement.getException());
            if (accept != null) {
                this.myResult = new ThrowEvaluator(accept);
            }
        }

        public void visitReturnStatement(PsiReturnStatement statement) {
            this.myResult = new ReturnEvaluator(accept(statement.getReturnValue()));
        }

        public void visitYieldStatement(PsiYieldStatement statement) {
            this.myResult = new SwitchEvaluator.YieldEvaluator(accept(statement.getExpression()));
        }

        public void visitSynchronizedStatement(PsiSynchronizedStatement statement) {
            throw new EvaluateRuntimeException(new UnsupportedExpressionException("Synchronized is not yet supported"));
        }

        public void visitStatement(PsiStatement statement) {
            logger.error(
                    DebuggerBundle.message("evaluation.error.statement.not.supported", statement.getText()));
            throwEvaluateException(
                    DebuggerBundle.message("evaluation.error.statement.not.supported", statement.getText()));
        }

        private CodeFragmentEvaluator setNewCodeFragmentEvaluator() {
            CodeFragmentEvaluator old = this.myCurrentFragmentEvaluator;
            this.myCurrentFragmentEvaluator = new CodeFragmentEvaluator(this.myCurrentFragmentEvaluator);
            return old;
        }

        private Evaluator[] visitStatements(PsiStatement[] statements) {
            List<Evaluator> evaluators = new ArrayList<>();
            for (PsiStatement psiStatement : statements) {
                psiStatement.accept(this);
                if (this.myResult != null) {
                    evaluators.add(DisableGC.create(this.myResult));
                }
                this.myResult = null;
            }
            return evaluators.toArray(new Evaluator[0]);
        }

        public void visitCodeBlock(PsiCodeBlock block) {
            CodeFragmentEvaluator oldFragmentEvaluator = setNewCodeFragmentEvaluator();
            try {
                this.myResult = new BlockStatementEvaluator(visitStatements(block.getStatements()));
            } finally {
                this.myCurrentFragmentEvaluator = oldFragmentEvaluator;
            }
        }

        public void visitBlockStatement(PsiBlockStatement statement) {
            visitCodeBlock(statement.getCodeBlock());
        }

        public void visitLabeledStatement(PsiLabeledStatement labeledStatement) {
            PsiStatement statement = labeledStatement.getStatement();
            if (statement != null) {
                statement.accept(this);
            }
        }

        public void visitDoWhileStatement(PsiDoWhileStatement statement) {
            Evaluator bodyEvaluator = accept(statement.getBody());
            Evaluator conditionEvaluator = accept(statement.getCondition());
            if (conditionEvaluator != null) {
                this


                        .myResult = new DoWhileStatementEvaluator(new UnBoxingEvaluator(conditionEvaluator), bodyEvaluator, getLabel(statement));
            }
        }

        public void visitWhileStatement(PsiWhileStatement statement) {
            Evaluator bodyEvaluator = accept(statement.getBody());
            Evaluator conditionEvaluator = accept(statement.getCondition());
            if (conditionEvaluator != null) {
                this


                        .myResult = new WhileStatementEvaluator(new UnBoxingEvaluator(conditionEvaluator), bodyEvaluator, getLabel(statement));
            }
        }

        public void visitForStatement(PsiForStatement statement) {
            CodeFragmentEvaluator oldFragmentEvaluator = setNewCodeFragmentEvaluator();
            try {
                Evaluator initializerEvaluator = accept(statement.getInitialization());
                Evaluator conditionEvaluator = accept(statement.getCondition());
                if (conditionEvaluator != null) {
                    conditionEvaluator = new UnBoxingEvaluator(conditionEvaluator);
                }
                Evaluator updateEvaluator = accept(statement.getUpdate());
                Evaluator bodyEvaluator = accept(statement.getBody());
                if (bodyEvaluator != null) {
                    this


                            .myResult = new ForStatementEvaluator(initializerEvaluator, conditionEvaluator, updateEvaluator, bodyEvaluator, getLabel(statement));
                }
            } finally {
                this.myCurrentFragmentEvaluator = oldFragmentEvaluator;
            }
        }

        public void visitForeachStatement(PsiForeachStatement statement) {
            CodeFragmentEvaluator oldFragmentEvaluator = setNewCodeFragmentEvaluator();
            try {
                PsiParameter parameter = statement.getIterationParameter();
                String iterationParameterName = parameter.getName();
                this.myCurrentFragmentEvaluator.setInitialValue(iterationParameterName, null);
                SyntheticVariableEvaluator iterationParameterEvaluator = new SyntheticVariableEvaluator(this.myCurrentFragmentEvaluator, iterationParameterName, null);


                Evaluator iteratedValueEvaluator = accept(statement.getIteratedValue());
                Evaluator bodyEvaluator = accept(statement.getBody());
                if (bodyEvaluator != null) {
                    this


                            .myResult = new ForeachStatementEvaluator(iterationParameterEvaluator, iteratedValueEvaluator, bodyEvaluator, getLabel(statement));
                }
            } finally {
                this.myCurrentFragmentEvaluator = oldFragmentEvaluator;
            }
        }

        @Nullable
        private Evaluator accept(@Nullable PsiElement element) {
            if (element == null || element instanceof PsiEmptyStatement) {
                return null;
            }
            element.accept(this);
            return this.myResult;
        }

        public void visitIfStatement(PsiIfStatement statement) {
            PsiStatement thenBranch = statement.getThenBranch();
            if (thenBranch == null)
                return;
            thenBranch.accept(this);
            Evaluator thenEvaluator = this.myResult;

            PsiStatement elseBranch = statement.getElseBranch();
            Evaluator elseEvaluator = null;
            if (elseBranch != null) {
                elseBranch.accept(this);
                elseEvaluator = this.myResult;
            }

            PsiExpression condition = statement.getCondition();
            if (condition == null)
                return;
            condition.accept(this);

            this.myResult = new IfStatementEvaluator(new UnBoxingEvaluator(this.myResult), thenEvaluator, elseEvaluator);
        }

        private void visitSwitchBlock(PsiSwitchBlock statement) {
            PsiCodeBlock body = statement.getBody();
            if (body != null) {
                Evaluator expressionEvaluator = accept(statement.getExpression());
                if (expressionEvaluator != null) {
                    this


                            .myResult = new SwitchEvaluator(expressionEvaluator, visitStatements(body.getStatements()), getLabel(statement));
                }
            }
        }

        public void visitSwitchStatement(PsiSwitchStatement statement) {
            visitSwitchBlock(statement);
        }

        public void visitSwitchExpression(PsiSwitchExpression expression) {
            visitSwitchBlock(expression);
        }

        private void visitSwitchLabelStatementBase(PsiSwitchLabelStatementBase statement) {
            SmartList<Evaluator> smartList = new SmartList();
            PsiExpressionList caseValues = statement.getCaseValues();
            if (caseValues != null) {
                for (PsiExpression expression : caseValues.getExpressions()) {
                    Evaluator evaluator = accept(expression);
                    if (evaluator != null) {
                        smartList.add(evaluator);
                    }
                }
            }
            if (statement instanceof PsiSwitchLabeledRuleStatement) {
                this


                        .myResult = new SwitchEvaluator.SwitchCaseRuleEvaluator(smartList, statement.isDefaultCase(), accept(((PsiSwitchLabeledRuleStatement) statement).getBody()));
            } else {
                this

                        .myResult = new SwitchEvaluator.SwitchCaseEvaluator(smartList, statement.isDefaultCase());
            }
        }

        public void visitSwitchLabelStatement(PsiSwitchLabelStatement statement) {
            visitSwitchLabelStatementBase(statement);
        }

        public void visitSwitchLabeledRuleStatement(PsiSwitchLabeledRuleStatement statement) {
            visitSwitchLabelStatementBase(statement);
        }

        public void visitBreakStatement(PsiBreakStatement statement) {
            PsiIdentifier labelIdentifier = statement.getLabelIdentifier();
            this.myResult = BreakContinueStatementEvaluator.createBreakEvaluator(
                    (labelIdentifier != null) ? labelIdentifier.getText() : null);
        }

        public void visitContinueStatement(PsiContinueStatement statement) {
            PsiIdentifier labelIdentifier = statement.getLabelIdentifier();
            this
                    .myResult = BreakContinueStatementEvaluator.createContinueEvaluator(
                    (labelIdentifier != null) ? labelIdentifier.getText() : null);
        }

        public void visitExpressionListStatement(PsiExpressionListStatement statement) {
            this

                    .myResult = new ExpressionListEvaluator(ContainerUtil.mapNotNull(statement
                    .getExpressionList().getExpressions(), this::accept));
        }

        public void visitEmptyStatement(PsiEmptyStatement statement) {
        }

        public void visitExpressionStatement(PsiExpressionStatement statement) {
            statement.getExpression().accept(this);
        }

        public void visitExpression(PsiExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitExpression " + expression);
            }
        }

        private int calcIterationCount(PsiJavaCodeReferenceElement qualifier) {
            if (qualifier != null) {
                return calcIterationCount(qualifier.resolve(), qualifier.getText(), false);
            }
            return 0;
        }

        private int calcIterationCount(PsiElement targetClass, String name, boolean checkInheritance) {
            PsiClass fromClass = getPositionClass();
            if (targetClass == null || fromClass == null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.invalid.expression", name));
            }
            try {
                int iterationCount = calcDepth(targetClass, fromClass, checkInheritance);
                if (iterationCount < -1 &&
                        !fromClass.equals(this.myContextPsiClass)) {
                    iterationCount = calcDepth(targetClass, this.myContextPsiClass, checkInheritance);
                }
                return Math.max(0, iterationCount);
            } catch (Exception e) {
                throw new EvaluateRuntimeException(
                        EvaluateExceptionUtil.createEvaluateException(e));
            }
        }


        @Nullable
        private PsiClass getPositionClass() {
            return (this.myPositionPsiClass != null) ? this.myPositionPsiClass : this.myContextPsiClass;
        }


        private ExpressionEvaluator buildElement(PsiElement element) throws EvaluateException {
            logger.info("assert element is valid - {}", element.isValid());

            setNewCodeFragmentEvaluator();
            this.myContextPsiClass = PsiTreeUtil.getContextOfType(element, PsiClass.class, false);
            try {
                element.accept(this);
            } catch (EvaluateRuntimeException e) {
                throw e.getCause();
            }
            if (this.myResult == null)
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.invalid.expression", element.toString()));
            return new ExpressionEvaluatorImpl(this.myResult);
        }
    }
}

