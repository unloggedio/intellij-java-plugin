package com.insidious.plugin.autoexecutor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.DiffUtils;
import com.insidious.plugin.util.MethodUtils;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;

public class AutomaticExecutorService {

    private InsidiousService insidiousService;
    private AutoExecutionRecordQueue reportingQueue;
    private Thread consumerThread;
    private boolean executeInBackground = true;

    public AutomaticExecutorService(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        reportingQueue = new AutoExecutionRecordQueue();
    }

    public void executeAllJavaMethodsInProject() {

        insidiousService.getReportingService().setReportingEnabled(true);
        if (executeInBackground) {
            executeInBackground();
        }
    }

    private void executeAllMethodsForClass(ClassAdapter sourceClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        insidiousService.getReportingService().setReportingEnabled(true);
        MethodAdapter[] methods = sourceClass.getMethods();
        for (MethodAdapter methodAdapter : methods) {
            List<String> argumentValues = new ArrayList<>();
            ParameterAdapter[] parameters = methodAdapter.getParameters();

            if (parameters.length > 0) {
                for (ParameterAdapter parameterAdapter : parameters) {
                    String value = ClassUtils.createDummyValue(parameterAdapter.getType(), new ArrayList<>(4),
                            insidiousService.getProject());
                    argumentValues.add(value);
                }
            }

            System.out.println("Executing method " + methodAdapter.getName());
            ClassUtils.chooseClassImplementation(methodAdapter.getContainingClass(), false, psiClass -> {
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("className", psiClass.getQualifiedClassName());
                eventProperties.put("methodName", methodAdapter.getName());

                UsageInsightTracker.getInstance().RecordEvent("ALL_INVOKE_CLASS", eventProperties);
                List<String> methodArgumentValues = new ArrayList<>();
                ParameterAdapter[] params = methodAdapter.getParameters();
                for (int i = 0; i < argumentValues.size(); i++) {
                    ParameterAdapter parameter = params[i];
                    String parameterValue = argumentValues.get(i);
                    String cannonicalText =
                            ApplicationManager.getApplication().runReadAction((Computable<String>) () -> parameter.getType().getCanonicalText());
                    if ("java.lang.String".equals(cannonicalText) &&
                            !parameterValue.startsWith("\"")) {
                        try {
                            parameterValue = objectMapper.writeValueAsString(parameterValue);
                        } catch (JsonProcessingException e) {
                            // should never happen
                        }
                    }
                    methodArgumentValues.add(parameterValue);
                }

                AgentCommandRequest agentCommandRequest =
                        MethodUtils.createExecuteRequestWithParameters(methodAdapter, psiClass, methodArgumentValues,
                                false, new ArrayList<>());
                agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);

                insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                        (agentCommandRequest1, agentCommandResponse) -> {
                            if (ResponseType.EXCEPTION.equals(agentCommandResponse.getResponseType())) {
                                if (agentCommandResponse.getMessage() == null && agentCommandResponse.getResponseClassName() == null) {
                                    InsidiousNotification.notifyMessage(
                                            "Exception thrown when trying to invoke " + agentCommandRequest.getMethodName(),
                                            NotificationType.ERROR
                                    );
                                    return;
                                }
                            }
                            ResponseType responseType1 = agentCommandResponse.getResponseType();
                            DiffResultType diffResultType = responseType1.equals(
                                    ResponseType.NORMAL) ? DiffResultType.NO_ORIGINAL : DiffResultType.ACTUAL_EXCEPTION;
                            DifferenceResult diffResult = new DifferenceResult(null,
                                    diffResultType, null,
                                    DiffUtils.getFlatMapFor(agentCommandResponse.getMethodReturnValue()));
                            diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE);
                            diffResult.setResponse(agentCommandResponse);
                            diffResult.setCommand(agentCommandRequest);

                            if (reportingQueue.isFull()) {
                                try {
                                    reportingQueue.waitIsNotFull();
                                } catch (InterruptedException e) {
//                                    System.out.println("Error while waiting to Produce messages.");
                                }
                            }
                            reportingQueue.add(new AutoExecutorReportRecord(diffResult,
                                    insidiousService.getSessionInstance().getProcessedFileCount(),
                                    insidiousService.getSessionInstance().getTotalFileCount()));
                        });
            });
        }
    }

    public void executeInBackground() {
        Task.Backgroundable executeAll = new Task.Backgroundable(insidiousService.getProject(), "Unlogged", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (consumerThread != null) {
                    consumerThread.interrupt();
                }
                reportingQueue.clear();
                AutoExecutionConsumer consumer = new AutoExecutionConsumer(insidiousService, reportingQueue);
                consumerThread = new Thread(consumer);
                consumerThread.start();

                System.out.println("Starting execution of all ");
                Collection<VirtualFile> javaFiles = ApplicationManager.getApplication().runReadAction((Computable<Collection<VirtualFile>>) () -> FileBasedIndex.getInstance()
                        .getContainingFiles(
                                FileTypeIndex.NAME,
                                JavaFileType.INSTANCE,
                                GlobalJavaSearchContext.projectScope(insidiousService.getProject())));
                for (VirtualFile virtualFile : javaFiles) {
                    PsiFile psiFile =
                            ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> PsiManager.getInstance(insidiousService.getProject()).findFile(virtualFile));
                    if (psiFile instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                        PsiClass[] javaFileClasses =
                                ApplicationManager.getApplication().runReadAction((Computable<PsiClass[]>) () -> psiJavaFile.getClasses());
                        for (PsiClass javaFileClass : javaFileClasses) {
                            String currentClassname =
                                    ApplicationManager.getApplication().runReadAction((Computable<String>) () -> javaFileClass.getName());
                            checkProgressIndicator("Executing methods in class", currentClassname);
                            executeAllMethodsForClass(new JavaClassAdapter(javaFileClass));
                        }
                    }
                }
                reportingQueue.notifyIsNotFull();
            }
        };
        ProgressManager.getInstance().run(executeAll);
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText(text1);
            }
        }
    }
}
