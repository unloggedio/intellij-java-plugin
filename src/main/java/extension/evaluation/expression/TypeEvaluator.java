 package extension.evaluation.expression;

 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
 import com.intellij.reference.SoftReference;
 import com.sun.jdi.ClassLoaderReference;
 import com.sun.jdi.ReferenceType;
 import extension.DebuggerBundle;
 import extension.evaluation.EvaluationContext;
 import extension.evaluation.EvaluatorUtil;
 import extension.evaluation.JVMName;
 import org.jetbrains.annotations.NotNull;

 import java.lang.ref.WeakReference;


 public class TypeEvaluator
   implements Evaluator
 {
   private final JVMName myTypeName;
   private WeakReference<ReferenceType> myLastResult;
   private WeakReference<ClassLoaderReference> myLastClassLoader;
   
   public TypeEvaluator(@NotNull JVMName typeName) {
     this.myTypeName = typeName;
   }
 
 
   
   @NotNull
   public ReferenceType evaluate(EvaluationContext context) throws EvaluateException {
     ClassLoaderReference classLoader = context.getStackFrameProxy().getClassLoader();
     ReferenceType lastRes = (ReferenceType)SoftReference.dereference(this.myLastResult);
     if (lastRes != null && classLoader == SoftReference.dereference(this.myLastClassLoader))
     {
       if (classLoader != null || lastRes
         .virtualMachine()
         .equals(context.getVirtualMachineProxy().getVirtualMachine())) {
         return lastRes;
       } 
     }
     String typeName = this.myTypeName.getName(context.getVirtualMachineProxy());
     ReferenceType type = EvaluatorUtil.findClass(context, typeName, classLoader);
     if (type == null) {
       throw EvaluateExceptionUtil.createEvaluateException(
           DebuggerBundle.message("error.class.not.loaded", new Object[] { typeName }));
     }
     this.myLastClassLoader = new WeakReference<>(classLoader);
     this.myLastResult = new WeakReference<>(type);
     return type;
   }
 
   
   public String toString() {
     return "Type " + this.myTypeName;
   }
 }

