package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


public class BlockStatementEvaluator
        implements Evaluator {
    protected Evaluator[] myStatements;

    public BlockStatementEvaluator(Evaluator[] statements) {
        this.myStatements = statements;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object result = context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid();
        for (Evaluator statement : this.myStatements) {
            result = statement.evaluate(context);
        }
        return result;
    }


    public Modifier getModifier() {
        return (this.myStatements.length > 0) ? this.myStatements[this.myStatements.length - 1].getModifier() : null;
    }
}

