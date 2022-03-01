 package com.insidious.plugin.extension.evaluation.expression;

 import com.intellij.debugger.engine.DebuggerUtils;
 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.sun.jdi.ObjectReference;
 import com.insidious.plugin.extension.evaluation.EvaluationContext;
 import org.jetbrains.annotations.NotNull;
 import org.jetbrains.annotations.Nullable;

 import java.util.List;


 public class TryEvaluator
   implements Evaluator
 {
   @NotNull
   private final Evaluator myBodyEvaluator;
   private final List<? extends CatchEvaluator> myCatchBlockEvaluators;
   @Nullable
   private final Evaluator myFinallyEvaluator;
   
   public TryEvaluator(@NotNull Evaluator bodyEvaluator, List<? extends CatchEvaluator> catchBlockEvaluators, @Nullable Evaluator finallyEvaluator) {
     this.myBodyEvaluator = bodyEvaluator;
     this.myCatchBlockEvaluators = catchBlockEvaluators;
     this.myFinallyEvaluator = finallyEvaluator;
   }
 
   
   public Object evaluate(EvaluationContext context) throws EvaluateException {
     Object result = context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid();
     try {
       result = this.myBodyEvaluator.evaluate(context);
     } catch (EvaluateException e) {
       boolean catched = false;
       ObjectReference vmException = e.getExceptionFromTargetVM();
       if (vmException != null) {
         for (CatchEvaluator evaluator : this.myCatchBlockEvaluators) {
           if (evaluator != null && 
             DebuggerUtils.instanceOf(vmException
               .type(), evaluator.getExceptionType())) {
             result = evaluator.evaluate(vmException, context);
             catched = true;
             break;
           } 
         } 
       }
       if (!catched) {
         throw e;
       }
     } finally {
       if (this.myFinallyEvaluator != null) {
         result = this.myFinallyEvaluator.evaluate(context);
       }
     } 
     return result;
   }
 }

