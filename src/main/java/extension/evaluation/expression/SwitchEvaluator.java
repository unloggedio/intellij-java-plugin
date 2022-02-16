package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.BreakException;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;


public class SwitchEvaluator implements Evaluator {
    private final Evaluator myExpressionEvaluator;
    private final Evaluator[] myBodyEvaluators;
    private final String myLabelName;

    public SwitchEvaluator(Evaluator expressionEvaluator, Evaluator[] bodyEvaluators, @Nullable String labelName) {
        this.myExpressionEvaluator = expressionEvaluator;
        this.myBodyEvaluators = bodyEvaluators;
        this.myLabelName = labelName;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object switchValue = UnBoxingEvaluator.unbox(this.myExpressionEvaluator.evaluate(context), context);
        Object res = null;
        try {
            boolean caseFound = false;
            for (Evaluator evaluator : this.myBodyEvaluators) {
                if (caseFound) {
                    res = evaluator.evaluate(context);
                } else {
                    Evaluator e = DisableGC.unwrap(evaluator);
                    if (e instanceof SwitchCaseEvaluator) {
                        res = ((SwitchCaseEvaluator) e).match(switchValue, context);
                        if (Boolean.TRUE.equals(res)) {
                            caseFound = true;
                        } else if (res instanceof com.sun.jdi.Value) {
                            return res;
                        }
                    }
                }
            }
        } catch (YieldException e) {
            return e.getValue();
        } catch (BreakException e) {
            if (!Objects.equals(e.getLabelName(), this.myLabelName)) {
                throw e;
            }
        }
        return res;
    }

    static class SwitchCaseEvaluator implements Evaluator {
        final List<? extends Evaluator> myEvaluators;
        final boolean myDefaultCase;

        SwitchCaseEvaluator(List<? extends Evaluator> evaluators, boolean defaultCase) {
            this.myEvaluators = evaluators;
            this.myDefaultCase = defaultCase;
        }

        Object match(Object value, EvaluationContext context) throws EvaluateException {
            if (this.myDefaultCase) {
                return Boolean.valueOf(true);
            }
            for (Evaluator evaluator : this.myEvaluators) {
                if (value.equals(UnBoxingEvaluator.unbox(evaluator.evaluate(context), context))) {
                    return Boolean.valueOf(true);
                }
            }
            return Boolean.valueOf(false);
        }


        public Object evaluate(EvaluationContext context) throws EvaluateException {
            return null;
        }
    }


    static class SwitchCaseRuleEvaluator
            extends SwitchCaseEvaluator {
        final Evaluator myBodyEvaluator;

        SwitchCaseRuleEvaluator(List<? extends Evaluator> evaluators, boolean defaultCase, Evaluator bodyEvaluator) {
            super(evaluators, defaultCase);
            this.myBodyEvaluator = bodyEvaluator;
        }


        Object match(Object value, EvaluationContext context) throws EvaluateException {
            Object res = super.match(value, context);
            if (Boolean.TRUE.equals(res)) {
                return this.myBodyEvaluator.evaluate(context);
            }
            return res;
        }
    }

    static class YieldEvaluator implements Evaluator {
        @Nullable
        final Evaluator myValueEvaluator;

        YieldEvaluator(@Nullable Evaluator valueEvaluator) {
            this.myValueEvaluator = valueEvaluator;
        }


        public Object evaluate(EvaluationContext context) throws EvaluateException {
            Object value = (this.myValueEvaluator == null) ? context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid() : this.myValueEvaluator.evaluate(context);
            throw new SwitchEvaluator.YieldException(value);
        }
    }

    static class YieldException extends EvaluateException {
        final Object myValue;

        YieldException(Object value) {
            super("Yield");
            this.myValue = value;
        }

        public Object getValue() {
            return this.myValue;
        }
    }
}

