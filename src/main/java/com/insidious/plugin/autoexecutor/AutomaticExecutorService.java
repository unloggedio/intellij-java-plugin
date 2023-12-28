package com.insidious.plugin.autoexecutor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.adapter.java.JavaParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.highlighter.MockMethodLineHighlighter;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.DiffUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.MethodUtils;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiExpressionEvaluator;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.resolve.graphInference.PsiPolyExpressionUtil;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.insidious.plugin.ui.assertions.MockValueMap.getChildrenOfTypeRecursive;

public class AutomaticExecutorService {

    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(50);
    private final InsidiousService insidiousService;
    private final AutoExecutionRecordQueue reportingQueue;
    private Thread consumerThread;
    private boolean executeInBackground = true;
    public static long classcount = 0;
    public static long methodcount = 0;
    public static long jsonExceptioncount = 0;
    public static long executingCount = 0;
    public static long responses = 0;
    public static long waitInterrupts = 0;
    public static long nullclasses = 0;
    public static long waiting = 0;
    public static long doneWaiting = 0;
    public boolean enableMocks = true;
    private static final Logger logger = LoggerUtil.getInstance(AutomaticExecutorService.class);

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

    public void executeAllMethodsForClass(ClassAdapter sourceClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        insidiousService.getReportingService().setReportingEnabled(true);
        MethodAdapter[] methods = sourceClass.getMethods();
        methodcount += methods.length;

//        ArrayList<DeclaredMock> declaredMocks = ApplicationManager.getApplication()
//                .runReadAction((Computable<ArrayList<DeclaredMock>>) () -> getDeclaredMocksForClass(sourceClass));
//        System.out.println("Declared mocks for class : " + sourceClass.getName());
//        System.out.println(declaredMocks.toString());

        for (MethodAdapter methodAdapter : methods) {
            if (methodAdapter.getName().equals("main")) {
                System.out.println("Possible main method : " + methodAdapter.getName());
                continue;
            }
//            if (true) {
//                return;
//            }
            List<String> argumentValues = new ArrayList<>();
            ParameterAdapter[] parameters = methodAdapter.getParameters();

            if (parameters.length > 0) {
                for (ParameterAdapter parameterAdapter : parameters) {
                    String value = ApplicationManager.getApplication().runReadAction(
                            (Computable<String>) () -> ClassUtils.createDummyValue(parameterAdapter.getType(),
                                    new ArrayList<>(4), insidiousService.getProject()));
                    argumentValues.add(value);
                }
            }

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
                            ApplicationManager.getApplication()
                                    .runReadAction((Computable<String>) () -> parameter.getType().getCanonicalText());
                    if ("java.lang.String".equals(cannonicalText) &&
                            !parameterValue.startsWith("\"")) {
                        try {
                            parameterValue = objectMapper.writeValueAsString(parameterValue);
                        } catch (JsonProcessingException e) {
                            // should never happen
                            jsonExceptioncount++;
                        }
                    }
                    methodArgumentValues.add(parameterValue);
                }
                ArrayList<DeclaredMock> declaredMocks = ApplicationManager.getApplication()
                        .runReadAction((Computable<ArrayList<DeclaredMock>>) () -> getDeclaredMocksForMethod(methodAdapter));
//                if (true) {
//                    return;
//                }
                AgentCommandRequest agentCommandRequest =
                        MethodUtils.createExecuteRequestWithParameters(methodAdapter, psiClass, methodArgumentValues,
                                false, declaredMocks);
                agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
//                System.out.println("Executing method " + methodAdapter.getName());
                executingCount++;
//                System.out.println("L0 : [Classcount,MethodCount,Executing,responses,waiting,doneWaiting] : [" + classcount +
//                        "," + methodcount + "," + executingCount + "," + responses + "," + waiting + "," + doneWaiting + "]");
//                logger.info("L0 : [Classcount,MethodCount,Executing,responses,waiting,doneWaiting] : [" + classcount +
//                        "," + methodcount + "," + executingCount + "," + responses + "," + waiting + "," + doneWaiting + "]");

                insidiousService.executeMethodInRunningProcessSync(agentCommandRequest,
                        (agentCommandRequest1, agentCommandResponse) -> {
                            if (ResponseType.EXCEPTION.equals(agentCommandResponse.getResponseType())) {
                                if (agentCommandResponse.getMessage() == null && agentCommandResponse.getResponseClassName() == null) {
                                    InsidiousNotification.notifyMessage(
                                            "Exception thrown when trying to invoke " + agentCommandRequest.getMethodName(),
                                            NotificationType.ERROR
                                    );
                                    nullclasses++;
                                    System.out.println("Got a null case : " + nullclasses);
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

                            boolean waiting_State = false;
                            if (reportingQueue.isFull()) {
                                try {
                                    waiting_State = true;
                                    waiting++;
                                    reportingQueue.waitIsNotFull();
                                } catch (InterruptedException e) {
//                                    System.out.println("Error while waiting to Produce messages.");
                                    waitInterrupts++;
                                }
                            }
                            responses++;
//                            System.out.println("L1 : [Classcount,MethodCount,Executing,responses,waiting,doneWaiting] : [" + classcount +
//                                    "," + methodcount + "," + executingCount + "," + responses + "," + waiting + "," + doneWaiting + "]");
                            logger.info("L1 : [Classcount,MethodCount,Executing,responses,waiting,doneWaiting] : [" + classcount +
                                    "," + methodcount + "," + executingCount + "," + responses + "," + waiting + "," + doneWaiting + "]");
                            reportingQueue.add(new AutoExecutorReportRecord(diffResult,
                                    insidiousService.getSessionInstance().getProcessedFileCount(),
                                    insidiousService.getSessionInstance().getTotalFileCount(), agentCommandRequest1.getDeclaredMocks()));
                        });
            });
        }
    }

    public void executeInBackground() {
        classcount = 0;
        methodcount = 0;
        responses = 0;
        executingCount = 0;
        waiting = 0;
        doneWaiting = 0;

        nullclasses = 0;
        jsonExceptioncount = 0;
        waitInterrupts = 0;
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
                Collection<VirtualFile> javaFiles = ApplicationManager.getApplication()
                        .runReadAction((Computable<Collection<VirtualFile>>) () -> FileTypeIndex.
                                getFiles(JavaFileType.INSTANCE,
                                        GlobalJavaSearchContext.projectScope(insidiousService.getProject())));
                for (VirtualFile virtualFile : javaFiles) {
                    PsiFile psiFile =
                            ApplicationManager.getApplication().runReadAction(
                                    (Computable<PsiFile>) () -> PsiManager.getInstance(insidiousService.getProject())
                                            .findFile(virtualFile));
                    if (psiFile instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                        PsiClass[] javaFileClasses =
                                ApplicationManager.getApplication()
                                        .runReadAction((Computable<PsiClass[]>) () -> psiJavaFile.getClasses());
                        for (PsiClass javaFileClass : javaFileClasses) {
                            String currentClassname =
                                    ApplicationManager.getApplication()
                                            .runReadAction((Computable<String>) () -> javaFileClass.getName());
                            classcount++;
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

    private ArrayList<DeclaredMock> getDeclaredMocksForClass(ClassAdapter classAdapter) {
//        System.out.println("Trying to find mocks for class : " + classAdapter.getName());
        ArrayList<DeclaredMock> declaredMocks = new ArrayList<>();
        PsiMethodCallExpression[] methodCallExpressions = getChildrenOfTypeRecursive(classAdapter.getSource(),
                PsiMethodCallExpression.class);
        if (methodCallExpressions == null || methodCallExpressions.length == 0) {
            return new ArrayList<>();
        }
        List<PsiMethodCallExpression> mockableCallExpressions = Arrays.stream(methodCallExpressions)
                .filter(MockMethodLineHighlighter::isNonStaticDependencyCall)
                .collect(Collectors.toList());

        for (PsiMethodCallExpression local : mockableCallExpressions) {
//            System.out.println("[AUTOMOCK] Local mockable method : " + localMockMethodName);
            PsiMethod methodFromExpression = local.resolveMethod();
            if (methodFromExpression == null) {
//                System.out.println("No method found to resolve");
            } else {
//                System.out.println("[FETCHED] Method from call : "
//                        + methodFromExpression.getName() + " from "
//                        + methodFromExpression.getContainingClass().getName());
                DeclaredMock newmock = createDummyMockForMethod(
                        new JavaMethodAdapter(methodFromExpression), local);
                declaredMocks.add(newmock);
            }
        }
        return declaredMocks;
    }

    private ArrayList<DeclaredMock> getDeclaredMocksForMethod(MethodAdapter methodAdapter) {
//        System.out.println("Trying to find methods to mock for : " + methodAdapter.getName());
//        if (methodAdapter.getName().equals("implPickupTest")) {
//            System.out.println("In debug method");
//        }
        ArrayList<DeclaredMock> declaredMocks = new ArrayList<>();
        PsiClass classPsi = JavaPsiFacade.getInstance(insidiousService.getProject())
                .findClass(methodAdapter.getContainingClass().getQualifiedName(),
                        GlobalSearchScope.projectScope(insidiousService.getProject()));
        PsiMethodCallExpression[] methodCallExpressions = getChildrenOfTypeRecursive(classPsi, PsiMethodCallExpression.class);
        if (methodCallExpressions == null || methodCallExpressions.length == 0) {
            return new ArrayList<>();
        }
        List<PsiMethodCallExpression> mockableCallExpressions = Arrays.stream(methodCallExpressions)
                .filter(MockMethodLineHighlighter::isNonStaticDependencyCall)
                .collect(Collectors.toList());

        for (PsiMethodCallExpression local : mockableCallExpressions) {
            String localMockMethodName = local.getMethodExpression().getReferenceName();
//            System.out.println("[AUTOMOCK] Local mockable method : " + localMockMethodName);
//            System.out.println("[FULL EXP] full expression : " + local.getText());

            if (methodContainsCall(methodAdapter.getText(), local.getText())) {
                //create a mock for this method
                System.out.println("Creating a mock for : " + local.getText());
                PsiMethod methodFromExpression = local.resolveMethod();
                if (methodFromExpression == null) {
//                System.out.println("No method found to resolve");
                } else {
//                System.out.println("[FETCHED] Method from call : "
//                        + methodFromExpression.getName() + " from "
//                        + methodFromExpression.getContainingClass().getName());
                    DeclaredMock newmock = createDummyMockForMethod(
                            new JavaMethodAdapter(methodFromExpression), local);
                    declaredMocks.add(newmock);
                }
            }
        }
        return declaredMocks;
    }

    private boolean methodContainsCall(String methodText, String methodCall) {
        return methodText.contains(methodCall);
    }

    private DeclaredMock createDummyMockForMethod(MethodAdapter methodAdapter,
                                                  PsiMethodCallExpression methodCallExpression) {
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(methodAdapter);

        PsiElement callerQualifier = methodCallExpression.getMethodExpression().getQualifier();
        String fieldName = callerQualifier.getText();
        PsiElement[] callerQualifierChildren = callerQualifier.getChildren();
        if (callerQualifierChildren.length > 1) {
            fieldName = callerQualifierChildren[callerQualifierChildren.length - 1].getText();
        }
        List<ParameterMatcher> parameterList = new ArrayList<>();
        JvmParameter[] jvmParameters = methodAdapter.getPsiMethod().getParameters();

        PsiType[] methodParameterTypes = methodCallExpression.getArgumentList().getExpressionTypes();
        for (int i = 0; i < methodParameterTypes.length; i++) {
            JavaParameterAdapter param = new JavaParameterAdapter(jvmParameters[i]);
            PsiType parameterType = methodParameterTypes[i];

            String parameterTypeName = parameterType.getCanonicalText();
            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                parameterTypeName = classReferenceType.rawType().getCanonicalText();
            }
            ParameterMatcher parameterMatcher = new ParameterMatcher(param.getName(),
                    ParameterMatcherType.ANY, parameterTypeName);
            parameterList.add(parameterMatcher);
        }

        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();
        String value = ApplicationManager.getApplication().runReadAction(
                (Computable<String>) () -> ClassUtils.createDummyValue(methodAdapter.getReturnType(),
                        new ArrayList<>(4), insidiousService.getProject()));
        String returnTypeName = "java.lang.Object";
        if (methodAdapter.getReturnType() != null) {
            returnTypeName = buildJvmClassName(methodAdapter.getReturnType());
        }
        thenParameterList.add(createDummyThenParameter(value, returnTypeName));
        DeclaredMock declaredMock = new DeclaredMock(
                "temp-mock" + methodAdapter.getName().hashCode(),
                methodUnderTest.getClassName(), methodAdapter.getContainingClass().getQualifiedName(),
                fieldName,
                methodUnderTest.getName(), parameterList, thenParameterList
        );
        return declaredMock;
    }

    @NotNull
    private ThenParameter createDummyThenParameter(String value, String returnTypeName) {
        ReturnValue returnValue = new ReturnValue(value, returnTypeName, ReturnValueType.REAL);
        return new ThenParameter(returnValue, MethodExitType.NORMAL);
    }

    //this fails
    private String buildJvmClassName(PsiType returnType) {
        if (!(returnType instanceof PsiClassReferenceType)) {
            return returnType.getCanonicalText();
        }
        PsiClassReferenceType classReferenceType = (PsiClassReferenceType) returnType;
        String classname = JvmClassUtil.getJvmClassName(classReferenceType.resolve());
        if (classname == null) {
            return "java.lang.Object";
        }
        StringBuilder jvmClassName =
                new StringBuilder(classname);
        int paramCount = classReferenceType.getParameterCount();
        if (paramCount > 0) {
            jvmClassName.append("<");
            for (PsiType parameter : classReferenceType.getParameters()) {
                jvmClassName.append(buildJvmClassName(parameter));
            }
            jvmClassName.append(">");
        }
        return jvmClassName.toString();
    }
}
