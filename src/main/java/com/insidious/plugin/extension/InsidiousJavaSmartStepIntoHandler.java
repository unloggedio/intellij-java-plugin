package com.insidious.plugin.extension;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.actions.LambdaSmartStepTarget;
import com.intellij.debugger.actions.MethodSmartStepTarget;
import com.intellij.debugger.actions.SmartStepTarget;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.*;
import com.insidious.plugin.extension.smartstep.AnonymousClassMethodFilter;
import com.insidious.plugin.extension.smartstep.LambdaAsyncMethodFilter;
import com.insidious.plugin.extension.smartstep.LambdaMethodFilter;
import com.insidious.plugin.extension.smartstep.MethodFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.List;

public abstract class InsidiousJavaSmartStepIntoHandler {
    @NotNull
    public List<SmartStepTarget> findSmartStepTargets(SourcePosition position) {
        throw new AbstractMethodError();
    }


    @NotNull
    public Promise<List<SmartStepTarget>> findSmartStepTargetsAsync(SourcePosition position, InsidiousJavaDebugProcess debugProcess) {
        return Promises.resolvedPromise(findSmartStepTargets(position));
    }


    @NotNull
    public Promise<List<SmartStepTarget>> findStepIntoTargets(SourcePosition position, InsidiousJavaDebugProcess debugProcess) {
        return Promises.rejectedPromise();
    }


    public abstract boolean isAvailable(SourcePosition paramSourcePosition);


    @Nullable
    protected MethodFilter createMethodFilter(SmartStepTarget stepTarget) {
        if (stepTarget instanceof MethodSmartStepTarget) {
            MethodSmartStepTarget methodSmartStepTarget = (MethodSmartStepTarget) stepTarget;
            PsiMethod method = methodSmartStepTarget.getMethod();
            if (stepTarget.needsBreakpointRequest()) {
                return
                        (Registry.is("debugger.async.smart.step.into") && method.getContainingClass() instanceof com.intellij.psi.PsiAnonymousClass) ?
                                new ClassInstanceMethodFilter(method, stepTarget
                                        .getCallingExpressionLines()) :
                                new AnonymousClassMethodFilter(method, stepTarget
                                        .getCallingExpressionLines());
            }
            return new BasicStepMethodFilter(method, methodSmartStepTarget

                    .getOrdinal(), stepTarget
                    .getCallingExpressionLines());
        }

        if (stepTarget instanceof LambdaSmartStepTarget) {
            LambdaSmartStepTarget lambdaTarget = (LambdaSmartStepTarget) stepTarget;


            LambdaMethodFilter lambdaMethodFilter = new LambdaMethodFilter(lambdaTarget.getLambda(), lambdaTarget.getOrdinal(), stepTarget.getCallingExpressionLines());

            if (Registry.is("debugger.async.smart.step.into") && lambdaTarget.isAsync()) {
                PsiLambdaExpression lambda = ((LambdaSmartStepTarget) stepTarget).getLambda();
                PsiElement expressionList = lambda.getParent();
                if (expressionList instanceof PsiExpressionList) {
                    PsiElement method = expressionList.getParent();
                    if (method instanceof PsiMethodCallExpression) {
                        return new LambdaAsyncMethodFilter(((PsiMethodCallExpression) method)
                                .resolveMethod(),
                                LambdaUtil.getLambdaIdx((PsiExpressionList) expressionList, lambda), lambdaMethodFilter);
                    }
                }
            }


            return lambdaMethodFilter;
        }
        return null;
    }
}


