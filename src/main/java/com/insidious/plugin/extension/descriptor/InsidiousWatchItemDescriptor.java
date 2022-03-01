package com.insidious.plugin.extension.descriptor;
 
 import com.insidious.plugin.util.LoggerUtil;
 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.intellij.debugger.engine.evaluation.TextWithImports;
 import org.slf4j.Logger;
 import com.intellij.openapi.project.Project;
 import com.intellij.psi.PsiCodeFragment;
 import com.sun.jdi.Value;
 import com.insidious.plugin.extension.evaluation.EvaluationContext;
 import com.insidious.plugin.extension.evaluation.EvaluatorUtil;

public class InsidiousWatchItemDescriptor extends InsidiousEvaluationDescriptor {
   private static final Logger logger = LoggerUtil.getInstance(InsidiousWatchItemDescriptor.class);
   
   public InsidiousWatchItemDescriptor(Project project, TextWithImports text, Value value) {
     super(text, project, value);
   }
   
   public InsidiousWatchItemDescriptor(Project project, TextWithImports text) {
     super(text, project);
   }
 
   
   public EvaluateException getEvaluateException() {
     EvaluateException exception = super.getEvaluateException();
     if (exception != null) {
       logger.debug("Watch point evaluation exception", (Throwable)exception);
       if ("java.lang.UnsupportedOperationException".equals(exception.getMessage())) {
         exception = new EvaluateException("Insidious Bridge does not support change in JVM state!");
       }
     } 
     
     return exception;
   }
 
   
   protected EvaluationContext getEvaluationContext(EvaluationContext evaluationContext) {
     return evaluationContext;
   }
 
 
   
   protected PsiCodeFragment getEvaluationCode(EvaluationContext context) throws EvaluateException {
     return createCodeFragment(EvaluatorUtil.getContextElement(context));
   }
 }
