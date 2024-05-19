package com.insidious.plugin.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.upload.SourceModel;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbService;
import io.netty.util.concurrent.DefaultThreadFactory;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SessionLoader implements Runnable, GetProjectSessionsCallback, Disposable {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    private final ExecutorService ourPool;
    private final List<GetProjectSessionsCallback> listeners = new ArrayList<>();
    private VideobugClientInterface client;
    private List<ExecutionSession> lastResult = new ArrayList<>();

    public SessionLoader() {
        ourPool = Executors.newFixedThreadPool(1, new DefaultThreadFactory("UnloggedAppThreadPool"));
        ourPool.submit(this);
    }

    @Override
    public void error(String message) {
        logger.warn("Failed to get sessions: " + message);
    }

    @Override
    public void success(List<ExecutionSession> executionSessionList) {
        lastResult = executionSessionList;
        for (GetProjectSessionsCallback listener : listeners) {
            listener.success(executionSessionList);
        }

    }


    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
                if (client == null) {
                    continue;
                }
                logger.debug("Check for new sessions");
                client.getProjectSessions(this);
            } catch (InterruptedException ie) {
                logger.warn("Session loader interrupted: " + ie.getMessage());
                break;
            }
        }
    }

    private String getPackageName() {

        String packageName = "";

        List<VirtualFile> javaFiles = new ArrayList<>(ApplicationManager.getApplication()
                .runReadAction((Computable<Collection<VirtualFile>>) () -> FileTypeIndex.
                        getFiles(JavaFileType.INSTANCE, GlobalJavaSearchContext.projectScope(project))));

        for (VirtualFile virtualFile: javaFiles) {
            PsiFile psiFile = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () ->
                    PsiManager.getInstance(project).findFile(virtualFile));

            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                PsiClass[] javaFileClasses = ApplicationManager.getApplication()
                        .runReadAction((Computable<PsiClass[]>) () -> psiJavaFile.getClasses());
                for (PsiClass javaFileClass : javaFileClasses) {
                    if (isMainClass(javaFileClass)) {
                        String localPackage = ApplicationManager.getApplication()
                                .runReadAction((Computable<String>) () -> psiJavaFile.getPackageName());
                        packageName = localPackage;
                        break;
                    }
                }
            }
        }

        return packageName;
    }

    private boolean isMainClass(PsiClass psiClass) {
        if (psiClass == null || psiClass.getName() == null) {
            return false;
        }

        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            PsiMethod[] methods = psiClass.findMethodsByName("main", false);
            for (PsiMethod method : methods) {
                if (method.hasModifierProperty(PsiModifier.STATIC) && method.hasModifierProperty(PsiModifier.PUBLIC)) {
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    if (parameters.length == 1 && parameters[0].getType() instanceof PsiArrayType) {
                        PsiType componentType = ((PsiArrayType) parameters[0].getType()).getComponentType();
                        if (PsiType.getJavaLangString(method.getManager(), method.getResolveScope()).equals(componentType)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    public void addSessionCallbackListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.add(getProjectSessionsCallback);
        if (lastResult != null) {
            getProjectSessionsCallback.success(lastResult);
        }
    }

    public void setClient(VideobugClientInterface client) {
        this.client = client;
    }

    public void removeListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.remove(getProjectSessionsCallback);
    }

    @Override
    public void dispose() {
        ourPool.shutdown();
    }
}
