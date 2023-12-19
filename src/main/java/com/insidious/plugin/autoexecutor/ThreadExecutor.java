package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.intellij.psi.PsiClass;

public class ThreadExecutor implements Runnable {
    private PsiClass javaFileClass;
    private AutomaticExecutorService automaticExecutorService;

    public ThreadExecutor(PsiClass javaFileClass, AutomaticExecutorService automaticExecutorService) {
        this.javaFileClass = javaFileClass;
        this.automaticExecutorService = automaticExecutorService;
    }

    @Override
    public void run() {
        automaticExecutorService.executeAllMethodsForClass(new JavaClassAdapter(javaFileClass));
    }
}
