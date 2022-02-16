package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;


public final class DisableGC implements Evaluator {
    private final Evaluator myDelegate;

    private DisableGC(@NotNull Evaluator delegate) {
        this.myDelegate = delegate;
    }

    public static Evaluator create(@NotNull Evaluator delegate) {
        if (!(delegate instanceof DisableGC)) {
            return new DisableGC(delegate);
        }
        return delegate;
    }

    public static Evaluator unwrap(Evaluator evaluator) {
        return (evaluator instanceof DisableGC) ? ((DisableGC) evaluator).myDelegate : evaluator;
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object result = this.myDelegate.evaluate(context);
        return result;
    }

    @Deprecated
    public Evaluator getDelegate() {
        return this.myDelegate;
    }

    public Modifier getModifier() {
        return this.myDelegate.getModifier();
    }

    public String toString() {
        return "NoGC -> " + this.myDelegate;
    }
}
