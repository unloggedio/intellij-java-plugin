package extension.evaluation.expression;
 
 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.intellij.debugger.engine.evaluation.expression.Modifier;
 import com.sun.jdi.ArrayReference;
 import com.sun.jdi.Value;
 import extension.evaluation.EvaluationContext;


public class ForeachStatementEvaluator
   extends ForStatementEvaluatorBase
 {
   private final Evaluator myIterationParameterEvaluator;
   private final Evaluator myIterableEvaluator;
   private Evaluator myConditionEvaluator;
   private Evaluator myNextEvaluator;
   private int myArrayLength = -1;
   private int myCurrentIndex = 0;
 
 
   
   private Modifier myModifier;
 
 
   
   public ForeachStatementEvaluator(Evaluator iterationParameterEvaluator, Evaluator iterableEvaluator, Evaluator bodyEvaluator, String labelName) {
     super(labelName, bodyEvaluator);
     this.myIterationParameterEvaluator = iterationParameterEvaluator;
     this.myIterableEvaluator = DisableGC.create(iterableEvaluator);
   }
 
   
   public Modifier getModifier() {
     return this.myModifier;
   }
 
 
   
   protected Object evaluateInitialization(EvaluationContext context, Object value) throws EvaluateException {
     final Object iterable = this.myIterableEvaluator.evaluate(context);
     if (!(iterable instanceof com.sun.jdi.ObjectReference)) {
       throw new EvaluateException("Unable to do foreach for" + iterable);
     }
     IdentityEvaluator iterableEvaluator = new IdentityEvaluator((Value)iterable);
     if (iterable instanceof ArrayReference) {
       this.myCurrentIndex = 0;
       this.myArrayLength = ((ArrayReference)iterable).length();
       this.myNextEvaluator = new AssignmentEvaluator(this.myIterationParameterEvaluator, new Evaluator()
           {
 
 
             
             public Object evaluate(EvaluationContext context) throws EvaluateException
             {
               return ((ArrayReference)iterable).getValue(ForeachStatementEvaluator.this.myCurrentIndex++);
             }
           });
     }
     else {
       
       Object iterator = (new MethodEvaluator(iterableEvaluator, null, "iterator", null, new Evaluator[0])).evaluate(context);
       IdentityEvaluator iteratorEvaluator = new IdentityEvaluator((Value)iterator);
       this.myConditionEvaluator = new MethodEvaluator(iteratorEvaluator, null, "hasNext", null, new Evaluator[0]);
       
       this.myNextEvaluator = new AssignmentEvaluator(this.myIterationParameterEvaluator, new MethodEvaluator(iteratorEvaluator, null, "next", null, new Evaluator[0]));
     } 
 
 
 
     
     return value;
   }
   
   private boolean isArray() {
     return (this.myArrayLength > -1);
   }
 
   
   protected Object evaluateCondition(EvaluationContext context) throws EvaluateException {
     if (isArray()) {
       return Boolean.valueOf((this.myCurrentIndex < this.myArrayLength));
     }
     Object res = this.myConditionEvaluator.evaluate(context);
     this.myModifier = this.myConditionEvaluator.getModifier();
     return res;
   }
 
 
   
   protected void evaluateBody(EvaluationContext context) throws EvaluateException {
     this.myNextEvaluator.evaluate(context);
     super.evaluateBody(context);
   }
 }


