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
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.DiffUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.MethodUtils;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutionUnit implements Runnable {

    private AutomaticExecutorService automaticExecutorService;
    private Thread consumerThread;
    private ExecutionUnitConfiguration configuration;
    private AutoExecutionRecordQueue reportingQueue;
    public long classcount = 0;
    public long responses = 0;
    private static final Logger logger = LoggerUtil.getInstance(ExecutionUnit.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private int timeoutWaitDuration = 30;

    public ExecutionUnit(AutomaticExecutorService executorService,
                         ExecutionUnitConfiguration configuration) {
        this.automaticExecutorService = executorService;
        this.configuration = configuration;
        reportingQueue = new AutoExecutionRecordQueue();
    }

    @Override
    public void run() {
        executeInBackground();
    }

    public void stopConsumer() {
        consumerThread.interrupt();
    }

    public void executeInBackground() {
        Task.Backgroundable executeAll = new Task.Backgroundable(automaticExecutorService.getInsidiousService().getProject(),
                "Unlogged - Autex - " + configuration.getExecutorId(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (consumerThread != null) {
                    consumerThread.interrupt();
                }
                reportingQueue.clear();
                AutoExecutionConsumer consumer = new AutoExecutionConsumer(automaticExecutorService.getInsidiousService(), reportingQueue);
                consumer.setSource(configuration.getExecutorId());
                consumerThread = new Thread(consumer);
                consumerThread.start();

                for (VirtualFile virtualFile : configuration.getPayload()) {
                    if (isATestFile(virtualFile)) {
                        continue;
                    }
                    PsiFile psiFile =
                            ApplicationManager.getApplication().runReadAction(
                                    (Computable<PsiFile>) () -> PsiManager.getInstance(automaticExecutorService.getInsidiousService().getProject())
                                            .findFile(virtualFile));
                    if (psiFile instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                        String packageName = ApplicationManager.getApplication()
                                .runReadAction((Computable<String>) () -> psiJavaFile.getPackageName());
                        if (!packageName.toLowerCase().contains(configuration.getIncludePackage().toLowerCase())) {
                            continue;
                        }
                        PsiClass[] javaFileClasses =
                                ApplicationManager.getApplication()
                                        .runReadAction((Computable<PsiClass[]>) () -> psiJavaFile.getClasses());
                        for (PsiClass javaFileClass : javaFileClasses) {
                            ClassAdapter javaClassAdapter = new JavaClassAdapter(javaFileClass);
//                            AutomaticExecutorService.incrementClassCount();
                            checkProgressIndicator("[Executor Unit " + configuration.getExecutorId() + "] Executing methods in class : " + javaClassAdapter.getName() + " " +
                                    "| Executions : " + responses, null);
                            executeAllMethodsForClass(javaClassAdapter);
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

    public void executeAllMethodsForClass(ClassAdapter sourceClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        MethodAdapter[] methods = sourceClass.getMethods();

        if (sourceClass.getQualifiedName().contains("com.appsmith.server.dtos.UserSessionDTO")) {
            //skip this class
            return;
        }

        for (MethodAdapter methodAdapter : methods) {
            if (methodAdapter.getName().equals("main")) {
                continue;
            }
            if (methodAdapter.isConstructor()) {
                continue;
            }

            checkProgressIndicator("Executing methods in class : " + sourceClass.getName() + " " +
                    "| Executions : " + responses, methodAdapter.getName() + "()");
            List<String> argumentValues = new ArrayList<>();
            ParameterAdapter[] parameters = methodAdapter.getParameters();

            if (parameters.length > 0) {
                for (ParameterAdapter parameterAdapter : parameters) {
                    String value = ApplicationManager.getApplication().runReadAction(
                            (Computable<String>) () -> ClassUtils.createDummyValue(parameterAdapter.getType(),
                                    new ArrayList<>(4),
                                    automaticExecutorService.getInsidiousService().getProject()));
                    argumentValues.add(value);
                }
            }
            try {
                List<PsiClass> listOfImplementations = ClassUtils.getListOfImplementationOptions(methodAdapter.getContainingClass(), false);
                if (listOfImplementations == null) {
                    logger.info("Found nothing to execute with for method : " + methodAdapter.getName());
                    continue;
                }
                for (PsiClass implementationOption : listOfImplementations) {
                    ClassUnderTest classUnderTest =
                            ApplicationManager.getApplication().runReadAction(
                                    (Computable<ClassUnderTest>) () -> new ClassUnderTest(
                                            JvmClassUtil.getJvmClassName(implementationOption)));
                    List<String> methodArgumentValues = new ArrayList<>();
                    ParameterAdapter[] params = methodAdapter.getParameters();
                    for (int i = 0; i < argumentValues.size(); i++) {
                        ParameterAdapter parameter = params[i];
                        String parameterValue = argumentValues.get(i);
                        String cannonicalText =
                                ApplicationManager.getApplication()
                                        .runReadAction((Computable<String>) () -> parameter.getType().getCanonicalText());
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
                    ArrayList<DeclaredMock> declaredMocks = new ArrayList<>();
                    if (configuration.isUseMocks()) {
                        declaredMocks = ApplicationManager.getApplication()
                                .runReadAction((Computable<ArrayList<DeclaredMock>>) () -> MockUtils.getDeclaredMocksForMethod(methodAdapter));
                    }

                    AgentCommandRequest agentCommandRequest =
                            MethodUtils.createExecuteRequestWithParameters(methodAdapter, classUnderTest, methodArgumentValues,
                                    false, declaredMocks);
                    agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    Future future = executorService.submit(new MethodExecutionThread(
                            this, agentCommandRequest, methodAdapter, classUnderTest));
                    try {
                        future.get(timeoutWaitDuration, TimeUnit.SECONDS);
                    } catch (TimeoutException timeoutException) {
                        future.cancel(true);
                        InsidiousNotification.notifyMessage("Request timed out when trying to execute method : " +
                                "" + methodAdapter.getName() + " from class : " + sourceClass.getName(), NotificationType.ERROR);
                        AgentCommandResponse<String> response = new AgentCommandResponse<>();
                        response.setResponseType(ResponseType.EXCEPTION);
                        response.setTimestamp(new Date().getTime());
                        DiffResultType diffResultType = DiffResultType.ACTUAL_EXCEPTION;
                        response.setMethodReturnValue("Method Execution Timed out");
                        DifferenceResult diffResult = new DifferenceResult(null,
                                diffResultType, null,
                                DiffUtils.getFlatMapFor(response.getMethodReturnValue()));
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE);
                        diffResult.setResponse(response);
                        diffResult.setCommand(agentCommandRequest);

                        AutoExecutorReportRecord record = new AutoExecutorReportRecord(diffResult, 0, 0,
                                new ArrayList<>(agentCommandRequest.getDeclaredMocks()));
                        record.setSource(configuration.getExecutorId());
                        String cannonText = ApplicationManager.getApplication()
                                .runReadAction((Computable<String>) () -> methodAdapter.getReturnType().getCanonicalText());
                        record.setMethodReturnTypeCannonicalText(cannonText);
                        record.setImplementationSource(classUnderTest.getQualifiedClassName());
                        reportingQueue.add(record);
                    } catch (Exception e) {
                        System.out.println("Exception trying to execute method ");
                    }
                    AutomaticExecutorService.incrementResponses();
                }
            } catch (ClassCastException classCastException) {
                logger.info("Got a PsiLambdaExpressionImpl cast exception");
                logger.error(classCastException.getMessage(), classCastException);
                //skip this method
                //java.lang.ClassCastException: class com.intellij.psi.impl.source.tree.java.PsiLambdaExpressionImpl cannot be cast to class com.intellij.psi.PsiClass (com.intellij.psi.impl.source.tree.java.PsiLambdaExpressionImpl and com.intellij.psi.PsiClass are in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @3dd0ba8b)
                return;
            } catch (Exception e) {
                logger.info("Exception trying to Invoke method  : " + methodAdapter.getName());
                logger.error(e.getMessage(), e);
            }
        }
    }

    private boolean isATestFile(VirtualFile virtualFile) {
        String testPath = "src/test";
        String path = virtualFile.getPath();
        if (path.contains(testPath)) {
            //is in the test directory, mark true
            return true;
        }
        Matcher matcher = testFileNamePattern.matcher(virtualFile.getName());
        if (matcher.matches()) {
            //is an unlogged testfile, mark true
            return true;
        }
        return false;
    }

    public AutomaticExecutorService getAutomaticExecutorService() {
        return automaticExecutorService;
    }

    public AutoExecutionRecordQueue getReportingQueue() {
        return reportingQueue;
    }

    public ExecutionUnitConfiguration getConfiguration() {
        return configuration;
    }
}
