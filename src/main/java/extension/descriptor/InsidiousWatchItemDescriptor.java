package extension.descriptor;
 
 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.intellij.debugger.engine.evaluation.TextWithImports;
 import com.intellij.openapi.diagnostic.Logger;
 import com.intellij.openapi.project.Project;
 import com.intellij.psi.PsiCodeFragment;
 import com.sun.jdi.Value;
 import extension.evaluation.EvaluationContext;
 import extension.evaluation.EvaluatorUtil;

public class InsidiousWatchItemDescriptor extends InsidiousEvaluationDescriptor {
   private static final Logger logger = Logger.getInstance(InsidiousWatchItemDescriptor.class);
   
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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousWatchItemDescriptor.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */