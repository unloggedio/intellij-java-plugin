package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.sun.jdi.ObjectReference;
import extension.evaluation.EvaluationContext;

public class CatchEvaluator
        implements Evaluator {
    private final String myExceptionType;
    private final String myParamName;
    private final CodeFragmentEvaluator myEvaluator;

    public CatchEvaluator(String exceptionType, String paramName, CodeFragmentEvaluator evaluator) {
        this.myExceptionType = exceptionType;
        this.myParamName = paramName;
        this.myEvaluator = evaluator;
    }


    public Object evaluate(ObjectReference exception, EvaluationContext context) throws EvaluateException {
        this.myEvaluator.setValue(this.myParamName, exception);
        return this.myEvaluator.evaluate(context);
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        throw new IllegalStateException("Use evaluate(ObjectReference exception, EvaluationContextImpl context)");
    }


    public String getExceptionType() {
        return this.myExceptionType;
    }
}

