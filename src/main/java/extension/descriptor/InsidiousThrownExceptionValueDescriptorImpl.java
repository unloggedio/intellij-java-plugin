package extension.descriptor;
 
 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.intellij.openapi.project.Project;
 import com.intellij.psi.PsiElement;
 import com.intellij.psi.PsiExpression;
 import com.sun.jdi.ObjectReference;
 import com.sun.jdi.Value;
 import extension.descriptor.renderer.RendererManager;
 import extension.evaluation.EvaluationContext;
 import org.jetbrains.annotations.NotNull;
 
 public class InsidiousThrownExceptionValueDescriptorImpl extends InsidiousValueDescriptorImpl {
   public InsidiousThrownExceptionValueDescriptorImpl(Project project, @NotNull ObjectReference exceptionObj) {
     super(project, exceptionObj);
 
     
     setRenderer(RendererManager.getInstance().getDefaultRenderer(exceptionObj.type()));
   }
 
   
   public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
     return getValue();
   }
 
   
   public String getName() {
     return "Exception";
   }
 
 
   
   public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
     throw new EvaluateException("Evaluation not supported for thrown exception object");
   }
 
   
   public boolean canSetValue() {
     return false;
   }
 }


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousThrownExceptionValueDescriptorImpl.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */