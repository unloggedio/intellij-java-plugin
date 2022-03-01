package com.insidious.plugin.extension.evaluation.expression;


import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightUtil;
import com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.ContextUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.*;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.engine.evaluation.expression.UnsupportedExpressionException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jvm.JvmModifier;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.extractMethodObject.ExtractLightMethodObjectHandler;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.JVMName;
import com.insidious.plugin.extension.evaluation.JVMNameUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


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

    private static void processBoxingConversions(PsiParameter[] declaredParams, PsiExpression[] actualArgumentExpressions, PsiSubstitutor methodResolveSubstitutor, Evaluator[] argumentEvaluators) {
        if (declaredParams.length > 0) {

            int paramCount = Math.max(declaredParams.length, actualArgumentExpressions.length);
            PsiType varargType = null;
            for (int idx = 0; idx < paramCount &&
                    idx < actualArgumentExpressions.length; idx++) {
                PsiType declaredParamType;


                if (idx < declaredParams.length) {

                    declaredParamType = methodResolveSubstitutor.substitute(declaredParams[idx].getType());
                    if (declaredParamType instanceof PsiEllipsisType) {

                        declaredParamType = varargType = ((PsiEllipsisType) declaredParamType).getComponentType();
                    }
                } else if (varargType != null) {
                    declaredParamType = varargType;
                } else {
                    break;
                }
                PsiType actualArgType = actualArgumentExpressions[idx].getType();
                if (TypeConversionUtil.boxingConversionApplicable(declaredParamType, actualArgType) || (declaredParamType != null && actualArgType == null)) {

                    Evaluator argEval = argumentEvaluators[idx];
                    argumentEvaluators[idx] =
                            (declaredParamType instanceof PsiPrimitiveType) ?
                                    new UnBoxingEvaluator(argEval) :
                                    new BoxingEvaluator(argEval);
                }
            }
        }
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

        public void visitPolyadicExpression(PsiPolyadicExpression wideExpression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitPolyadicExpression " + wideExpression);
            }
            PsiExpression[] operands = wideExpression.getOperands();
            operands[0].accept(this);
            Evaluator result = this.myResult;
            PsiType lType = operands[0].getType();
            for (int i = 1; i < operands.length; i++) {
                PsiExpression expression = operands[i];
                if (expression == null) {
                    throwExpressionInvalid(wideExpression);
                }
                expression.accept(this);
                Evaluator rResult = this.myResult;
                IElementType opType = wideExpression.getOperationTokenType();
                PsiType rType = expression.getType();
                if (rType == null) {
                    throwEvaluateException(
                            DebuggerBundle.message("evaluation.error.unknown.expression.type", expression.getText()));
                }
                PsiType typeForBinOp = TypeConversionUtil.calcTypeForBinaryExpression(lType, rType, opType, true);
                if (typeForBinOp == null)
                    throwEvaluateException(
                            DebuggerBundle.message("evaluation.error.unknown.expression.type", wideExpression.getText()));
                this
                        .myResult = createBinaryEvaluator(result, lType, rResult, rType, opType, typeForBinOp, this.myEvaluationContext);


                lType = typeForBinOp;
                result = this.myResult;
            }
        }

        public void visitDeclarationStatement(PsiDeclarationStatement statement) {
            List<Evaluator> evaluators = new ArrayList<>();

            PsiElement[] declaredElements = statement.getDeclaredElements();
            for (PsiElement declaredElement : declaredElements) {
                if (declaredElement instanceof PsiLocalVariable) {
                    if (this.myCurrentFragmentEvaluator != null) {
                        PsiLocalVariable localVariable = (PsiLocalVariable) declaredElement;

                        PsiType lType = localVariable.getType();


                        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(localVariable.getProject());

                        try {
                            PsiExpression initialValue = elementFactory.createExpressionFromText(
                                    PsiTypesUtil.getDefaultValueOfType(lType), null);

                            Object value = JavaConstantExpressionEvaluator.computeConstantExpression(initialValue, true);

                            this.myCurrentFragmentEvaluator.setInitialValue(localVariable
                                    .getName(), value);
                        } catch (IncorrectOperationException e) {
                            logger.error("failed", e);
                        }

                        PsiExpression initializer = localVariable.getInitializer();
                        if (initializer != null) {
                            try {
                                if (!TypeConversionUtil.areTypesAssignmentCompatible(lType, initializer)) {
                                    throwEvaluateException(
                                            DebuggerBundle.message("evaluation.error.incompatible.variable.initializer.type", localVariable.getName()));
                                }
                                PsiType rType = initializer.getType();
                                initializer.accept(this);
                                Evaluator rEvaluator = this.myResult;


                                PsiExpression localVarReference = elementFactory.createExpressionFromText(localVariable
                                        .getName(), initializer);

                                localVarReference.accept(this);
                                Evaluator lEvaluator = this.myResult;

                                rEvaluator = handleAssignmentBoxingAndPrimitiveTypeConversions(localVarReference
                                        .getType(), rType, rEvaluator, statement


                                        .getProject(), this.myEvaluationContext);


                                Evaluator assignment = new AssignmentEvaluator(lEvaluator, rEvaluator);

                                evaluators.add(assignment);
                            } catch (IncorrectOperationException e) {
                                logger.error("failed", e);
                            }
                        }
                    } else {
                        throw new EvaluateRuntimeException(new EvaluateException(

                                DebuggerBundle.message("evaluation.error.local.variable.declarations.not.supported"), null));
                    }

                } else {

                    throw new EvaluateRuntimeException(new EvaluateException(

                            DebuggerBundle.message("evaluation.error.unsupported.declaration", declaredElement.getText()), null));
                }
            }

            if (!evaluators.isEmpty()) {
                CodeFragmentEvaluator codeFragmentEvaluator = new CodeFragmentEvaluator(this.myCurrentFragmentEvaluator);

                codeFragmentEvaluator.setStatements(evaluators.toArray(new Evaluator[0]));
                this.myResult = codeFragmentEvaluator;
            } else {
                this.myResult = null;
            }
        }

        public void visitConditionalExpression(PsiConditionalExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitConditionalExpression " + expression);
            }
            PsiExpression thenExpression = expression.getThenExpression();
            PsiExpression elseExpression = expression.getElseExpression();
            if (thenExpression == null || elseExpression == null) {
                throwExpressionInvalid(expression);
            }
            PsiExpression condition = expression.getCondition();
            condition.accept(this);
            if (this.myResult == null) {
                throwExpressionInvalid(condition);
            }
            Evaluator conditionEvaluator = new UnBoxingEvaluator(this.myResult);
            thenExpression.accept(this);
            if (this.myResult == null) {
                throwExpressionInvalid(thenExpression);
            }
            Evaluator thenEvaluator = this.myResult;
            elseExpression.accept(this);
            if (this.myResult == null) {
                throwExpressionInvalid(elseExpression);
            }
            Evaluator elseEvaluator = this.myResult;
            this.myResult = new ConditionalExpressionEvaluator(conditionEvaluator, thenEvaluator, elseEvaluator);
        }

        public void visitReferenceExpression(PsiReferenceExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitReferenceExpression " + expression);
            }
            PsiExpression qualifier = expression.getQualifierExpression();
            JavaResolveResult resolveResult = expression.advancedResolve(true);
            PsiElement element = resolveResult.getElement();

            if (element instanceof PsiLocalVariable || element instanceof PsiParameter) {

                Value labeledValue = element.getUserData(CodeFragmentFactoryContextWrapper.LABEL_VARIABLE_VALUE_KEY);

                if (labeledValue != null) {
                    this.myResult = new IdentityEvaluator(labeledValue);

                    return;
                }
                PsiFile containingFile = element.getContainingFile();
                if (containingFile instanceof PsiCodeFragment && this.myCurrentFragmentEvaluator != null && this.myVisitedFragments

                        .contains(containingFile)) {


                    JVMName jvmName = JVMNameUtil.getJVMQualifiedName(CompilingEvaluatorTypesUtil

                            .getVariableType((PsiVariable) element));
                    this


                            .myResult = new SyntheticVariableEvaluator(this.myCurrentFragmentEvaluator, ((PsiVariable) element).getName(), jvmName);

                    return;
                }

                PsiVariable psiVar = (PsiVariable) element;
                String localName = psiVar.getName();
                PsiClass variableClass = getContainingClass(psiVar);
                PsiClass positionClass = getPositionClass();
                if (Objects.equals(positionClass, variableClass)) {
                    PsiElement method = DebuggerUtilsEx.getContainingMethod(expression);


                    boolean canScanFrames = (method instanceof PsiLambdaExpression || ContextUtil.isJspImplicit(element));
                    this.myResult = new LocalVariableEvaluator(localName, canScanFrames);


                    return;
                }


                int iterationCount = calcIterationCount(variableClass, "Base class not found for " + psiVar

                        .getName(), false) - 1;


                if (iterationCount > -1) {
                    PsiExpression initializer = psiVar.getInitializer();
                    if (initializer != null) {


                        Object value = JavaPsiFacade.getInstance(psiVar.getProject()).getConstantEvaluationHelper().computeConstantExpression(initializer);
                        if (value != null) {

                            PsiType type = resolveResult.getSubstitutor().substitute(psiVar.getType());
                            this.myResult = new LiteralEvaluator(value, type.getCanonicalText());
                            return;
                        }
                    }
                    Evaluator objectEvaluator = new ThisEvaluator(iterationCount);
                    this
                            .myResult = createFallbackEvaluator(new FieldEvaluator(objectEvaluator,


                            FieldEvaluator.createClassFilter(positionClass), "val$" + localName), new LocalVariableEvaluator(localName, true));

                    return;
                }

                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.local.variable.missing.from.class.closure", localName));

            } else if (element instanceof PsiField) {
                Evaluator objectEvaluator;
                PsiField psiField = (PsiField) element;
                PsiClass fieldClass = psiField.getContainingClass();
                if (fieldClass == null) {
                    throwEvaluateException(
                            DebuggerBundle.message("evaluation.error.cannot.resolve.field.class", psiField.getName()));
                    return;
                }
                if (psiField.hasModifierProperty("static")) {

                    JVMName className = JVMNameUtil.getContextClassJVMQualifiedName(
                            SourcePosition.createFromElement(psiField));
                    if (className == null) {
                        className = JVMNameUtil.getJVMQualifiedName(fieldClass);
                    }
                    objectEvaluator = new TypeEvaluator(className);
                } else if (qualifier != null) {
                    qualifier.accept(this);
                    objectEvaluator = this.myResult;
                } else {
                    int iterations = calcIterationCount(fieldClass, fieldClass.getName(), true);
                    if (iterations < 0)
                        throwEvaluateException(
                                DebuggerBundle.message("evaluation.error.cannot.sources.for.field.class", psiField.getName()));
                    objectEvaluator = new ThisEvaluator(iterations);
                }
                this


                        .myResult = new FieldEvaluator(objectEvaluator, FieldEvaluator.createClassFilter(fieldClass), psiField.getName());
            } else {
                String name;
                PsiElement nameElement = expression.getReferenceNameElement();

                if (nameElement instanceof PsiIdentifier) {
                    name = nameElement.getText();
                } else {

                    String elementDisplayString = (nameElement != null) ? nameElement.getText() : "(null)";
                    throwEvaluateException(
                            DebuggerBundle.message("evaluation.error.identifier.expected", elementDisplayString));

                    return;
                }

                if (qualifier != null) {


                    PsiElement qualifierTarget = (qualifier instanceof PsiReferenceExpression) ? ((PsiReferenceExpression) qualifier).resolve() : null;
                    if (qualifierTarget instanceof PsiClass) {

                        PsiClass psiClass = (PsiClass) qualifierTarget;
                        JVMName typeName = JVMNameUtil.getJVMQualifiedName(psiClass);
                        this


                                .myResult = new FieldEvaluator(new TypeEvaluator(typeName), FieldEvaluator.createClassFilter(psiClass), name);
                    } else {

                        qualifier.accept(this);
                        if (this.myResult == null) {
                            throwEvaluateException(
                                    DebuggerBundle.message("evaluation.error.cannot.evaluate.qualifier", qualifier.getText()));
                        }
                        this


                                .myResult = new FieldEvaluator(this.myResult, FieldEvaluator.createClassFilter(qualifier.getType()), name);
                    }
                } else {

                    this
                            .myResult = createFallbackEvaluator(new LocalVariableEvaluator(name, false), new FieldEvaluator(new ThisEvaluator(), FieldEvaluator.TargetClassFilter.ALL, name));
                }
            }
        }

        public void visitSuperExpression(PsiSuperExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitSuperExpression " + expression);
            }
            this.myResult = new SuperEvaluator(calcIterationCount(expression.getQualifier()));
        }

        public void visitThisExpression(PsiThisExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitThisExpression " + expression);
            }
            this.myResult = new ThisEvaluator(calcIterationCount(expression.getQualifier()));
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

        public void visitInstanceOfExpression(PsiInstanceOfExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitInstanceOfExpression " + expression);
            }
            PsiTypeElement checkType = expression.getCheckType();
            if (checkType == null) {
                throwExpressionInvalid(expression);
            }
            PsiType type = checkType.getType();
            expression.getOperand().accept(this);


            Evaluator operandEvaluator = this.myResult;
            this


                    .myResult = new InstanceofEvaluator(operandEvaluator, new TypeEvaluator(JVMNameUtil.getJVMQualifiedName(type)));
        }

        public void visitParenthesizedExpression(PsiParenthesizedExpression expression) {
            if (logger.isDebugEnabled()) {
                logger.debug("visitParenthesizedExpression " + expression);
            }
            PsiExpression expr = expression.getExpression();
            if (expr != null) {
                expr.accept(this);
            }
        }

        public void visitPostfixExpression(PsiPostfixExpression expression) {
            if (expression.getType() == null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.unknown.expression.type", expression.getText()));
            }
            PsiExpression operandExpression = expression.getOperand();
            operandExpression.accept(this);

            Evaluator operandEvaluator = this.myResult;

            IElementType operation = expression.getOperationTokenType();
            PsiType operandType = operandExpression.getType();

            PsiPrimitiveType psiPrimitiveType = PsiPrimitiveType.getUnboxedType(operandType);


            Evaluator incrementImpl = createBinaryEvaluator(operandEvaluator, operandType, new LiteralEvaluator(


                            Integer.valueOf(1), "int"), PsiType.INT,

                    (operation == JavaTokenType.PLUSPLUS) ?
                            JavaTokenType.PLUS :
                            JavaTokenType.MINUS,
                    (psiPrimitiveType != null) ? psiPrimitiveType : operandType, this.myEvaluationContext);

            if (psiPrimitiveType != null) {
                incrementImpl = new BoxingEvaluator(incrementImpl);
            }
            this.myResult = new PostfixOperationEvaluator(operandEvaluator, incrementImpl);
        }

        public void visitPrefixExpression(PsiPrefixExpression expression) {
            PsiType expressionType = expression.getType();
            if (expressionType == null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.unknown.expression.type", expression.getText()));
            }
            PsiExpression operandExpression = expression.getOperand();
            if (operandExpression == null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.unknown.expression.operand", expression.getText()));
            }
            operandExpression.accept(this);
            Evaluator operandEvaluator = this.myResult;


            PsiType operandType = operandExpression.getType();

            PsiPrimitiveType psiPrimitiveType = PsiPrimitiveType.getUnboxedType(operandType);

            IElementType operation = expression.getOperationTokenType();

            if (operation == JavaTokenType.PLUSPLUS || operation == JavaTokenType.MINUSMINUS) {

                try {
                    Evaluator rightEval = createBinaryEvaluator(operandEvaluator, operandType, new LiteralEvaluator(


                                    Integer.valueOf(1), "int"), PsiType.INT,

                            (operation == JavaTokenType.PLUSPLUS) ?
                                    JavaTokenType.PLUS :
                                    JavaTokenType.MINUS,
                            (psiPrimitiveType != null) ? psiPrimitiveType : operandType, this.myEvaluationContext);

                    this.myResult = new AssignmentEvaluator(operandEvaluator, (psiPrimitiveType != null) ? new BoxingEvaluator(rightEval) : rightEval);
                } catch (IncorrectOperationException e) {
                    logger.error("failed", e);
                }
            } else {
                if (JavaTokenType.PLUS.equals(operation) || JavaTokenType.MINUS
                        .equals(operation) || JavaTokenType.TILDE
                        .equals(operation)) {

                    operandEvaluator = handleUnaryNumericPromotion(operandType, operandEvaluator, this.myEvaluationContext);

                } else if (psiPrimitiveType != null) {
                    operandEvaluator = new UnBoxingEvaluator(operandEvaluator);
                }

                this


                        .myResult = new UnaryExpressionEvaluator(operation, expressionType.getCanonicalText(), operandEvaluator, expression.getOperationSign().getText());
            }
        }

        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            Evaluator objectEvaluator;
            if (logger.isDebugEnabled()) {
                logger.debug("visitMethodCallExpression " + expression);
            }
            PsiExpressionList argumentList = expression.getArgumentList();
            PsiExpression[] argExpressions = argumentList.getExpressions();
            Evaluator[] argumentEvaluators = new Evaluator[argExpressions.length];

            for (int idx = 0; idx < argExpressions.length; idx++) {
                PsiExpression psiExpression = argExpressions[idx];
                psiExpression.accept(this);
                if (this.myResult == null) {
                    throwExpressionInvalid(psiExpression);
                }
                argumentEvaluators[idx] = DisableGC.create(this.myResult);
            }
            PsiReferenceExpression methodExpr = expression.getMethodExpression();

            JavaResolveResult resolveResult = methodExpr.advancedResolve(false);

            PsiMethod psiMethod = CompilingEvaluatorTypesUtil.getReferencedMethod(resolveResult);

            PsiExpression qualifier = methodExpr.getQualifierExpression();

            JVMName contextClass = null;

            if (psiMethod != null) {
                PsiClass methodPsiClass = psiMethod.getContainingClass();
                contextClass = JVMNameUtil.getJVMQualifiedName(methodPsiClass);
                if (psiMethod.hasModifierProperty("static")) {
                    objectEvaluator = new TypeEvaluator(contextClass);
                } else if (qualifier != null) {
                    qualifier.accept(this);
                    objectEvaluator = this.myResult;
                } else {
                    int iterationCount = 0;
                    PsiElement currentFileResolveScope = resolveResult.getCurrentFileResolveScope();
                    if (currentFileResolveScope instanceof PsiClass) {
                        iterationCount = calcIterationCount(currentFileResolveScope, ((PsiClass) currentFileResolveScope)

                                .getName(), false);
                    }

                    objectEvaluator = new ThisEvaluator(iterationCount);
                }

            } else if (qualifier != null) {
                PsiType type = qualifier.getType();

                if (type != null) {
                    contextClass = JVMNameUtil.getJVMQualifiedName(type);
                }

                if (qualifier instanceof PsiReferenceExpression && ((PsiReferenceExpression) qualifier)
                        .resolve() instanceof PsiClass) {


                    if (contextClass == null) {
                        contextClass = JVMNameUtil.getJVMRawText(((PsiReferenceExpression) qualifier)

                                .getQualifiedName());
                    }
                    objectEvaluator = new TypeEvaluator(contextClass);
                } else {
                    qualifier.accept(this);
                    objectEvaluator = this.myResult;
                }
            } else {
                objectEvaluator = new ThisEvaluator();
                PsiClass positionClass = getPositionClass();
                if (positionClass != null) {
                    contextClass = JVMNameUtil.getJVMQualifiedName(positionClass);
                }
            }


            if (objectEvaluator == null) {
                throwExpressionInvalid(expression);
            }

            if (psiMethod != null && !psiMethod.isConstructor() &&
                    psiMethod.getReturnType() == null) {
                throwEvaluateException(
                        DebuggerBundle.message("evaluation.error.unknown.method.return.type", psiMethod.getText()));
            }

            boolean defaultInterfaceMethod = false;
            boolean mustBeVararg = false;

            if (psiMethod != null) {
                EvaluatorBuilderImpl.processBoxingConversions(psiMethod
                        .getParameterList().getParameters(), argExpressions, resolveResult

                        .getSubstitutor(), argumentEvaluators);

                defaultInterfaceMethod = psiMethod.hasModifierProperty("default");
                mustBeVararg = psiMethod.isVarArgs();
            }

            this


                    .myResult = new MethodEvaluator(objectEvaluator, contextClass, methodExpr.getReferenceName(), (psiMethod != null) ? JVMNameUtil.getJVMSignature(psiMethod) : null, argumentEvaluators, defaultInterfaceMethod, mustBeVararg);
        }

        public void visitLiteralExpression(PsiLiteralExpression expression) {
            HighlightInfo parsingError = HighlightUtil.checkLiteralExpressionParsingError(expression,
                    PsiUtil.getLanguageLevel(expression), null);
            if (parsingError != null) {
                throwEvaluateException(parsingError.getDescription());

                return;
            }
            PsiType type = expression.getType();
            if (type == null) {
                throwEvaluateException(expression + ": null type");

                return;
            }
            this

                    .myResult = new LiteralEvaluator(expression.getValue(), type.getCanonicalText());
        }

        public void visitArrayAccessExpression(PsiArrayAccessExpression expression) {
            PsiExpression indexExpression = expression.getIndexExpression();
            if (indexExpression == null) {
                throwExpressionInvalid(expression);
            }
            indexExpression.accept(this);

            Evaluator indexEvaluator = handleUnaryNumericPromotion(indexExpression
                    .getType(), this.myResult, this.myEvaluationContext);

            expression.getArrayExpression().accept(this);
            Evaluator arrayEvaluator = this.myResult;
            this.myResult = new ArrayAccessEvaluator(arrayEvaluator, indexEvaluator);
        }

        public void visitTypeCastExpression(PsiTypeCastExpression expression) {
            PsiExpression operandExpr = expression.getOperand();
            if (operandExpr == null) {
                throwExpressionInvalid(expression);
            }
            operandExpr.accept(this);
            Evaluator operandEvaluator = this.myResult;
            PsiTypeElement castTypeElem = expression.getCastType();
            if (castTypeElem == null) {
                throwExpressionInvalid(expression);
            }
            PsiType castType = castTypeElem.getType();
            PsiType operandType = operandExpr.getType();


            if (operandType != null &&
                    !TypeConversionUtil.areTypesConvertible(operandType, castType) &&
                    PsiUtil.resolveClassInType(operandType) != null) {
                throw new EvaluateRuntimeException(new EvaluateException(

                        DebuggerBundle.message("inconvertible.type.cast", JavaHighlightUtil.formatType(operandType),
                                JavaHighlightUtil.formatType(castType))));
            }


            boolean shouldPerformBoxingConversion = (operandType != null && TypeConversionUtil.boxingConversionApplicable(castType, operandType));
            boolean castingToPrimitive = castType instanceof PsiPrimitiveType;
            if (shouldPerformBoxingConversion && castingToPrimitive) {
                operandEvaluator = new UnBoxingEvaluator(operandEvaluator);
            }

            boolean performCastToWrapperClass = (shouldPerformBoxingConversion && !castingToPrimitive);


            if (!(PsiUtil.resolveClassInClassTypeOnly(castType) instanceof com.intellij.psi.PsiTypeParameter)) {
                if (performCastToWrapperClass) {
                    castType = ObjectUtils.notNull(
                            PsiPrimitiveType.getUnboxedType(castType), operandType);
                }

                this.myResult = createTypeCastEvaluator(operandEvaluator, castType, this.myEvaluationContext);
            }

            if (performCastToWrapperClass) {
                this.myResult = new BoxingEvaluator(this.myResult);
            }
        }

        public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression expression) {
            PsiType type = expression.getOperand().getType();

            if (type instanceof PsiPrimitiveType) {

                JVMName typeName = JVMNameUtil.getJVMRawText(((PsiPrimitiveType) type).getBoxedTypeName());
                this.myResult = new FieldEvaluator(new TypeEvaluator(typeName), FieldEvaluator.TargetClassFilter.ALL, "TYPE");

            } else {


                this

                        .myResult = new ClassObjectEvaluator(new TypeEvaluator(JVMNameUtil.getJVMQualifiedName(type)));
            }
        }

        public void visitLambdaExpression(PsiLambdaExpression expression) {
            throw new EvaluateRuntimeException(new UnsupportedExpressionException(

                    DebuggerBundle.message("evaluation.error.lambda.evaluation.not.supported")));
        }

        public void visitMethodReferenceExpression(PsiMethodReferenceExpression expression) {
            PsiElement qualifier = expression.getQualifier();
            PsiType interfaceType = expression.getFunctionalInterfaceType();
            if (!Registry.is("debugger.compiling.evaluator.method.refs") && interfaceType != null && qualifier != null) {


                String code = null;
                try {
                    PsiElement resolved = expression.resolve();
                    if (resolved instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) resolved;
                        PsiClass containingClass = method.getContainingClass();
                        if (containingClass != null) {
                            String find;
                            boolean bind = false;
                            if (method.isConstructor()) {


                                find = "findConstructor(" + containingClass.getQualifiedName() + ".class, mt)";
                            } else if (qualifier instanceof PsiSuperExpression) {


                                find = "in(" + containingClass.getQualifiedName() + ".class).findSpecial(" + containingClass.getQualifiedName() + ".class, \"" + method.getName() + "\", mt, " + containingClass.getQualifiedName() + ".class)";

                                bind = true;

                            } else {

                                find = containingClass.getQualifiedName() + ".class, \"" + method.getName() + "\", mt)";

                                if (method.hasModifier(JvmModifier.STATIC)) {
                                    find = "findStatic(" + find;
                                } else {
                                    find = "findVirtual(" + find;
                                    if (qualifier instanceof PsiReference) {
                                        PsiElement resolve = ((PsiReference) qualifier).resolve();
                                        if (!(resolve instanceof PsiClass)) {
                                            bind = true;
                                        }
                                    } else {
                                        bind = true;
                                    }
                                }
                            }

                            String bidStr = bind ? ("mh = mh.bindTo(" + qualifier.getText() + ");\n") : "";


                            code = "MethodType mt = MethodType.fromMethodDescriptorString(\"" + JVMNameUtil.getJVMSignature(method) + "\", null);\nMethodHandle mh = MethodHandles.lookup()." + find + ";\n" + bidStr + "MethodHandleProxies.asInterfaceInstance(" + interfaceType.getCanonicalText() + ".class, mh);";
                        }

                    } else if (PsiUtil.isArrayClass(resolved)) {


                        code = "MethodType mt = MethodType.methodType(Object.class, Class.class, int.class);\nMethodHandle mh = MethodHandles.publicLookup().findStatic(Array.class, \"newInstance\", mt);\nmh = mh.bindTo(" + StringUtil.substringBeforeLast(qualifier.getText(), "[]") + ".class)\nMethodHandleProxies.asInterfaceInstance(" + interfaceType.getCanonicalText() + ".class, mh);";
                    }

                    if (code != null) {
                        this
                                .myResult = buildFromJavaCode(code, "java.lang.invoke.MethodHandle,java.lang.invoke.MethodHandleProxies,java.lang.invoke.MethodHandles,java.lang.invoke.MethodType,java.lang.reflect.Array", expression);


                        return;
                    }
                } catch (Exception e) {
                    logger.error("failed", e);
                }
            }
            throw new EvaluateRuntimeException(new UnsupportedExpressionException(

                    DebuggerBundle.message("evaluation.error.method.reference.evaluation.not.supported")));
        }

        private Evaluator buildFromJavaCode(String code, String imports, @NotNull PsiElement context) {
            TextWithImportsImpl text = new TextWithImportsImpl(CodeFragmentKind.CODE_BLOCK, code, imports, JavaFileType.INSTANCE);


            JavaCodeFragment codeFragment = DefaultCodeFragmentFactory.getInstance().createCodeFragment(text, context, context.getProject());
            return accept(codeFragment);
        }

        public void visitNewExpression(PsiNewExpression expression) {
            PsiType expressionPsiType = expression.getType();
            if (expressionPsiType instanceof PsiArrayType) {
                Evaluator dimensionEvaluator = null;
                PsiExpression[] dimensions = expression.getArrayDimensions();
                if (dimensions.length == 1) {
                    PsiExpression dimensionExpression = dimensions[0];
                    dimensionExpression.accept(this);
                    if (this.myResult != null) {

                        dimensionEvaluator = handleUnaryNumericPromotion(dimensionExpression
                                .getType(), this.myResult, this.myEvaluationContext);
                    } else {

                        throwEvaluateException(
                                DebuggerBundle.message("evaluation.error.invalid.array.dimension.expression", dimensionExpression.getText()));
                    }
                } else if (dimensions.length > 1) {
                    throwEvaluateException(
                            DebuggerBundle.message("evaluation.error.multi.dimensional.arrays.creation.not.supported"));
                }


                Evaluator initializerEvaluator = null;
                PsiArrayInitializerExpression arrayInitializer = expression.getArrayInitializer();
                if (arrayInitializer != null) {
                    if (dimensionEvaluator != null) {
                        throwExpressionInvalid(expression);
                    }
                    arrayInitializer.accept(this);
                    if (this.myResult != null) {

                        initializerEvaluator = handleUnaryNumericPromotion(arrayInitializer
                                .getType(), this.myResult, this.myEvaluationContext);
                    } else {
                        throwExpressionInvalid(arrayInitializer);
                    }
                }


                if (dimensionEvaluator == null && initializerEvaluator == null) {
                    throwExpressionInvalid(expression);
                }
                this


                        .myResult = new NewArrayInstanceEvaluator(new TypeEvaluator(JVMNameUtil.getJVMQualifiedName(expressionPsiType)), dimensionEvaluator, initializerEvaluator);

            } else if (expressionPsiType instanceof PsiClassType) {

                PsiClass aClass = CompilingEvaluatorTypesUtil.getClass((PsiClassType) expressionPsiType);
                if (aClass instanceof com.intellij.psi.PsiAnonymousClass) {
                    throw new EvaluateRuntimeException(new UnsupportedExpressionException(

                            DebuggerBundle.message("evaluation.error.anonymous.class.evaluation.not.supported")));
                }

                PsiExpressionList argumentList = expression.getArgumentList();
                if (argumentList == null) {
                    throwExpressionInvalid(expression);
                }
                PsiExpression[] argExpressions = argumentList.getExpressions();

                JavaResolveResult constructorResolveResult = expression.resolveMethodGenerics();

                PsiMethod constructor = CompilingEvaluatorTypesUtil.getReferencedConstructor((PsiMethod) constructorResolveResult
                        .getElement());
                if (constructor == null && argExpressions.length > 0) {
                    throw new EvaluateRuntimeException(new EvaluateException(

                            DebuggerBundle.message("evaluation.error.cannot.resolve.constructor", expression.getText()), null));
                }
                Evaluator[] argumentEvaluators = new Evaluator[argExpressions.length];

                for (int idx = 0; idx < argExpressions.length; idx++) {
                    PsiExpression argExpression = argExpressions[idx];
                    argExpression.accept(this);
                    if (this.myResult != null) {
                        argumentEvaluators[idx] = DisableGC.create(this.myResult);
                    } else {
                        throwExpressionInvalid(argExpression);
                    }
                }

                if (constructor != null) {
                    EvaluatorBuilderImpl.processBoxingConversions(constructor
                            .getParameterList().getParameters(), argExpressions, constructorResolveResult

                            .getSubstitutor(), argumentEvaluators);
                }


                if (aClass != null) {
                    PsiClass containingClass = aClass.getContainingClass();
                    if (containingClass != null &&
                            !aClass.hasModifierProperty("static")) {
                        PsiExpression qualifier = expression.getQualifier();
                        if (qualifier != null) {
                            qualifier.accept(this);
                            if (this.myResult != null) {
                                argumentEvaluators = (Evaluator[]) ArrayUtil.prepend(this.myResult, (Object[]) argumentEvaluators);
                            }
                        } else {

                            argumentEvaluators = (Evaluator[]) ArrayUtil.prepend(new ThisEvaluator(

                                    calcIterationCount(containingClass, "this", false)), (Object[]) argumentEvaluators);
                        }
                    }
                }


                JVMName signature = JVMNameUtil.getJVMConstructorSignature(constructor, aClass);

                PsiType instanceType = CompilingEvaluatorTypesUtil.getClassType((PsiClassType) expressionPsiType);
                this

                        .myResult = new NewClassInstanceEvaluator(new TypeEvaluator(JVMNameUtil.getJVMQualifiedName(instanceType)), signature, argumentEvaluators);


            } else if (expressionPsiType != null) {
                throwEvaluateException("Unsupported expression type: " + expressionPsiType

                        .getPresentableText());
            } else {
                throwEvaluateException("Unknown type for expression: " + expression.getText());
            }
        }

        public void visitArrayInitializerExpression(PsiArrayInitializerExpression expression) {
            PsiExpression[] initializers = expression.getInitializers();
            Evaluator[] evaluators = new Evaluator[initializers.length];
            PsiType type = expression.getType();


            boolean primitive = (type instanceof PsiArrayType && ((PsiArrayType) type).getComponentType() instanceof PsiPrimitiveType);
            for (int idx = 0; idx < initializers.length; idx++) {
                PsiExpression initializer = initializers[idx];
                initializer.accept(this);
                if (this.myResult != null) {


                    Evaluator coerced = primitive ? handleUnaryNumericPromotion(initializer.getType(), this.myResult, this.myEvaluationContext) : new BoxingEvaluator(this.myResult);
                    evaluators[idx] = DisableGC.create(coerced);
                } else {
                    throwExpressionInvalid(initializer);
                }
            }
            this.myResult = new ArrayInitializerEvaluator(evaluators);


            if (type != null && !(expression.getParent() instanceof PsiNewExpression)) {
                this

                        .myResult = new NewArrayInstanceEvaluator(new TypeEvaluator(JVMNameUtil.getJVMQualifiedName(type)), null, this.myResult);
            }
        }

        public void visitAssertStatement(PsiAssertStatement statement) {
            PsiExpression condition = statement.getAssertCondition();
            if (condition == null) {
                throwEvaluateException("Assert condition expected in: " + statement.getText());
            }
            PsiExpression description = statement.getAssertDescription();
            String descriptionText = (description != null) ? description.getText() : "";
            this.myResult = new AssertStatementEvaluator(buildFromJavaCode("if (!(" +
                    condition.getText() +
                    ")) { throw new java.lang.AssertionError(" +
                    descriptionText +
                    ");}", "", statement));
        }

        @Nullable
        private PsiClass getContainingClass(@NotNull PsiVariable variable) {
            PsiClass element = PsiTreeUtil.getParentOfType(variable.getParent(), PsiClass.class, false);
            return (element == null) ? this.myContextPsiClass : element;
        }

        @Nullable
        private PsiClass getPositionClass() {
            return (this.myPositionPsiClass != null) ? this.myPositionPsiClass : this.myContextPsiClass;
        }


        protected ExpressionEvaluator buildElement(PsiElement element) throws EvaluateException {
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

    private static class CompilingEvaluatorTypesUtil {
        @NotNull
        private static PsiType getVariableType(@NotNull PsiVariable variable) {
            PsiType type = variable.getType();
            PsiClass psiClass = PsiTypesUtil.getPsiClass(type);
            if (psiClass != null) {

                PsiType typeToUse = psiClass.getUserData(ExtractLightMethodObjectHandler.REFERENCED_TYPE);
                if (typeToUse != null) {
                    type = typeToUse;
                }
            }

            return type;
        }

        @Nullable
        private static PsiMethod getReferencedMethod(@NotNull JavaResolveResult resolveResult) {
            PsiMethod psiMethod = (PsiMethod) resolveResult.getElement();


            PsiMethod methodToUseInstead = (psiMethod == null) ? null : psiMethod.getUserData(ExtractLightMethodObjectHandler.REFERENCE_METHOD);

            if (methodToUseInstead != null) {
                psiMethod = methodToUseInstead;
            }

            return psiMethod;
        }

        @Nullable
        private static PsiClass getClass(@NotNull PsiClassType classType) {
            PsiClass aClass = classType.resolve();


            PsiType type = (aClass == null) ? null : aClass.getUserData(ExtractLightMethodObjectHandler.REFERENCED_TYPE);
            if (type != null) {
                return PsiTypesUtil.getPsiClass(type);
            }

            return aClass;
        }

        @Nullable
        @Contract("null -> null")
        private static PsiMethod getReferencedConstructor(@Nullable PsiMethod originalConstructor) {
            if (originalConstructor == null) return null;

            PsiMethod methodToUseInstead = originalConstructor.getUserData(ExtractLightMethodObjectHandler.REFERENCE_METHOD);

            return (methodToUseInstead == null) ? originalConstructor : methodToUseInstead;
        }

        @NotNull
        private static PsiType getClassType(@NotNull PsiClassType expressionPsiType) {
            PsiClass aClass = expressionPsiType.resolve();


            PsiType type = (aClass == null) ? null : aClass.getUserData(ExtractLightMethodObjectHandler.REFERENCED_TYPE);
            return (type != null) ? type : expressionPsiType;
        }
    }
}

