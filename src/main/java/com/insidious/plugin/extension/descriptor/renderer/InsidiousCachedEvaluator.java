 package com.insidious.plugin.extension.descriptor.renderer;

 import com.insidious.plugin.util.LoggerUtil;
 import com.intellij.debugger.engine.DebuggerUtils;
 import com.intellij.debugger.engine.evaluation.CodeFragmentFactory;
 import com.intellij.debugger.engine.evaluation.EvaluateException;
 import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
 import com.intellij.debugger.engine.evaluation.TextWithImports;
 import com.intellij.debugger.engine.evaluation.expression.UnsupportedExpressionException;
 import com.intellij.debugger.impl.DebuggerUtilsEx;
 import com.intellij.debugger.impl.DebuggerUtilsImpl;
 import com.intellij.openapi.application.ApplicationManager;
 import com.intellij.openapi.application.WriteAction;
 import org.slf4j.Logger;
 import com.intellij.openapi.project.Project;
 import com.intellij.openapi.util.Pair;
 import com.intellij.psi.*;
 import com.intellij.reference.SoftReference;
 import com.insidious.plugin.extension.DebuggerBundle;
 import com.insidious.plugin.extension.evaluation.EvaluationContext;
 import com.insidious.plugin.extension.evaluation.expression.EvaluatorBuilderImpl;
 import com.insidious.plugin.extension.evaluation.expression.ExpressionEvaluator;
 import org.jetbrains.annotations.Nullable;

 public abstract class InsidiousCachedEvaluator {
   protected static final Logger LOG = LoggerUtil.getInstance(InsidiousCachedEvaluator.class);
   
   private static class Cache {
     protected ExpressionEvaluator myEvaluator;
     protected EvaluateException myException;
     protected PsiExpression myPsiChildrenExpression;
     
     private Cache() {} }
   SoftReference<Cache> myCache = new SoftReference(null);
   
   private TextWithImports myReferenceExpression;
 
   
   public TextWithImports getReferenceExpression() {
     return (this.myReferenceExpression != null) ? 
       this.myReferenceExpression : 
       DebuggerUtils.getInstance().createExpressionWithImports("");
   }
   
   public void setReferenceExpression(TextWithImports referenceExpression) {
     this.myReferenceExpression = referenceExpression;
     clear();
   }
   
   public void clear() {
     this.myCache.clear();
   }
 
   
   protected Cache initEvaluatorAndChildrenExpression(Project project, EvaluationContext evaluationContext) {
     Cache cache = new Cache();
     try {
       String className = getClassName();
       
       Pair<PsiElement, PsiType> psiClassAndType = DebuggerUtilsImpl.getPsiClassAndType(className, project);
       PsiElement context = (PsiElement)psiClassAndType.first;
       if (context == null) {
         throw EvaluateExceptionUtil.createEvaluateException(
             DebuggerBundle.message("evaluation.error.cannot.find.source", new Object[] { className }));
       }
       
       CodeFragmentFactory factory = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(this.myReferenceExpression, context);
 
       
       JavaCodeFragment codeFragment = factory.createCodeFragment(this.myReferenceExpression, 
           overrideContext(context), project);
       codeFragment.setThisType((PsiType)psiClassAndType.second);
       DebuggerUtils.checkSyntax((PsiCodeFragment)codeFragment);
       cache
 
         
         .myPsiChildrenExpression = (codeFragment instanceof PsiExpressionCodeFragment) ? ((PsiExpressionCodeFragment)codeFragment).getExpression() : null;
       
       try {
         cache
           
           .myEvaluator = EvaluatorBuilderImpl.getInstance().build((PsiElement)codeFragment, null, evaluationContext);
       } catch (UnsupportedExpressionException ex) {
         throw ex;
       } 
     } catch (EvaluateException e) {
       cache.myException = e;
     } 
     
     this.myCache = new SoftReference(cache);
     return cache;
   }
   
   protected PsiElement overrideContext(PsiElement context) {
     return context;
   }
 
   
   protected ExpressionEvaluator getEvaluator(Project project, EvaluationContext evaluationContext) throws EvaluateException {
     Cache cache = (Cache)this.myCache.get();
     if (cache == null) {
       ApplicationManager.getApplication()
         .invokeLater(() -> PsiDocumentManager.getInstance(project).commitAllDocuments());
 
 
 
       
       cache = (Cache)WriteAction.compute(() -> initEvaluatorAndChildrenExpression(project, evaluationContext));
     } 
     
     if (cache.myException != null) {
       throw cache.myException;
     }
     
     return cache.myEvaluator;
   }
 
   
   @Nullable
   protected PsiExpression getPsiExpression(Project project, EvaluationContext evaluationContext) {
     Cache cache = (Cache)this.myCache.get();
     if (cache == null) {
       cache = initEvaluatorAndChildrenExpression(project, evaluationContext);
     }
     
     return cache.myPsiChildrenExpression;
   }
   
   protected abstract String getClassName();
 }

