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
import com.insidious.plugin.agent.AgentCommandResponse;
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
import com.intellij.psi.impl.source.tree.java.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    public boolean enableMocks = false;
    private static final Logger logger = LoggerUtil.getInstance(AutomaticExecutorService.class);

    public AutomaticExecutorService(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        reportingQueue = new AutoExecutionRecordQueue();
    }

    public void setEnableMocks(boolean status) {
        this.enableMocks = status;
        InsidiousNotification.notifyMessage("AutoExecutor mock enabled : " + enableMocks
                , NotificationType.INFORMATION);
    }

    public void executeAllJavaMethodsInProject(AutoExecutorRunOptions options) {

        setEnableMocks(options.isUseMocks());
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

//        if (!sourceClass.getQualifiedName().contains("UserController")) {
//            return;
//        } else {
//            System.out.println("Executing UserController");
//        }

        for (MethodAdapter methodAdapter : methods) {
            if (methodAdapter.getName().equals("main")) {
                continue;
            }
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
                ArrayList<DeclaredMock> declaredMocks = new ArrayList<>();
                if (enableMocks) {
                    declaredMocks = ApplicationManager.getApplication()
                            .runReadAction((Computable<ArrayList<DeclaredMock>>) () -> getDeclaredMocksForMethod(methodAdapter));
                }
                AgentCommandRequest agentCommandRequest =
                        MethodUtils.createExecuteRequestWithParameters(methodAdapter, psiClass, methodArgumentValues,
                                false, declaredMocks);
                agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
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
                                    waitInterrupts++;
                                }
                            }
                            responses++;
//                            System.out.println("L1 : [Classcount,MethodCount,Executing,responses,waiting,doneWaiting] : [" + classcount +
//                                    "," + methodcount + "," + executingCount + "," + responses + "," + waiting + "," + doneWaiting + "]");
                            logger.info("L1 : [Classcount,MethodCount,Executing,responses,waiting,doneWaiting] : [" + classcount +
                                    "," + methodcount + "," + executingCount + "," + responses + "," + waiting + "," + doneWaiting + "]");
                            if (sourceClass.isInterface()) {
                                // report using actual class rather than the executed impl
                                // without this impl classes will have 2 sets of executions in the report
                                // while the original interface here will have no entries
                                diffResult.getCommand().setClassName(sourceClass.getQualifiedName());
                            }
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

                System.out.println("[AutoExecutor] Starting execution of all methods.");
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
            PsiMethod methodFromExpression = local.resolveMethod();
            if (methodFromExpression != null) {
                DeclaredMock newmock = createDummyMockForMethod(
                        new JavaMethodAdapter(methodFromExpression), local);
                declaredMocks.add(newmock);
            }
        }
        return declaredMocks;
    }

    private ArrayList<DeclaredMock> getDeclaredMocksForMethod(MethodAdapter methodAdapter) {
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
            if (methodContainsCall(methodAdapter.getText(), local.getText())) {
                PsiMethod methodFromExpression = local.resolveMethod();
                if (methodFromExpression != null) {
                    DeclaredMock newmock = createDummyMockV2(
                            methodAdapter, local);
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
            PsiClass returnTypeClass = PsiTypesUtil.getPsiClass(methodAdapter.getReturnType());
            if (returnTypeClass != null) {
                returnTypeName = returnTypeClass.getQualifiedName();
            } else {
                returnTypeName = buildJvmClassName(methodAdapter.getReturnType());
            }
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

    private DeclaredMock createDummyMockV2(MethodAdapter methodBeingRun,
                                           PsiMethodCallExpression methodCallExpression) {

        PsiMethod destinationMethod = methodCallExpression.resolveMethod();
        MethodUnderTest destinationMethodUnterTest =
                MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(destinationMethod));
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(methodBeingRun);
        PsiType returnType = identifyReturnType(methodCallExpression);
        String returnDummyValue;
        String methodReturnTypeName;

        if (returnType != null) {
            returnDummyValue = ClassUtils.createDummyValue(returnType, new ArrayList<>(),
                    destinationMethod.getProject());
            methodReturnTypeName = buildJvmClassName(returnType);
        } else {
            methodReturnTypeName = "java.lang.Object";
            returnDummyValue = "{}";
        }
        PsiClass parentClass = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);

        if (parentClass == null) {
            InsidiousNotification.notifyMessage("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]", NotificationType.ERROR);
            throw new RuntimeException("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]");
        }
        String expressionText = methodCallExpression.getMethodExpression().getText();
        PsiType[] methodParameterTypes = methodCallExpression.getArgumentList().getExpressionTypes();
        JvmParameter[] jvmParameters = destinationMethod.getParameters();
        List<ParameterMatcher> parameterList = new ArrayList<>();
        for (int i = 0; i < methodParameterTypes.length; i++) {
            JavaParameterAdapter param = new JavaParameterAdapter(jvmParameters[i]);
            PsiType parameterType = methodParameterTypes[i];

            String parameterTypeName = parameterType.getCanonicalText();
            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                parameterTypeName = classReferenceType.rawType().getCanonicalText();
            }
            ParameterMatcher parameterMatcher = new ParameterMatcher(param.getName(),
                    ParameterMatcherType.ANY_OF_TYPE, parameterTypeName);
            parameterList.add(parameterMatcher);
        }

        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();
        thenParameterList.add(createDummyThenParameter(returnDummyValue, methodReturnTypeName));
        PsiElement callerQualifier = methodCallExpression.getMethodExpression().getQualifier();
        String fieldName = callerQualifier.getText();
        PsiElement[] callerQualifierChildren = callerQualifier.getChildren();
        if (callerQualifierChildren.length > 1) {
            fieldName = callerQualifierChildren[callerQualifierChildren.length - 1].getText();
        }
        DeclaredMock mock = new DeclaredMock(
                "mock response " + expressionText,
                destinationMethodUnterTest.getClassName(), //part that messes up mocking
                parentClass.getQualifiedName(),
                fieldName,
                destinationMethodUnterTest.getName(),
                parameterList, thenParameterList
        );
        return mock;
    }

    @Nullable
    private PsiType identifyReturnType(PsiExpression methodCallExpression) {
        PsiType returnType = null;

        if (methodCallExpression.getParent() instanceof PsiConditionalExpressionImpl) {
            return identifyReturnType((PsiConditionalExpressionImpl) methodCallExpression.getParent());
        } else if (methodCallExpression.getParent() instanceof PsiLocalVariableImpl
                && methodCallExpression.getParent().getParent() instanceof PsiDeclarationStatementImpl) {
            // this is an assignment and we can probably get a better return type from the variable type which
            // this is being assigned to
            returnType = ((PsiLocalVariableImpl) methodCallExpression.getParent()).getType();
        } else if (methodCallExpression.getParent() instanceof PsiAssignmentExpressionImpl
                && methodCallExpression.getParent().getParent() instanceof PsiExpressionStatement) {
            // this is an assignment and we can probably get a better return type from the variable type which
            // this is being assigned to
            returnType = ((PsiAssignmentExpressionImpl) methodCallExpression.getParent()).getType();
        } else if (methodCallExpression.getParent() instanceof PsiExpressionListImpl
                && methodCallExpression.getParent().getParent() instanceof PsiMethodCallExpressionImpl) {
            // the return value is being passed to another method as a parameter
            PsiExpressionListImpl expressionList = (PsiExpressionListImpl) methodCallExpression.getParent();
            PsiType[] expressionTypes = expressionList.getExpressionTypes();
            PsiExpression[] allExpressions = expressionList.getExpressions();
            // identify the return value is which index
            int i = 0;
            for (PsiExpression expression : allExpressions) {
                if (expression == methodCallExpression) {
                    break;
                }
                i++;
            }

            if (i < expressionTypes.length) {
                returnType = expressionTypes[i];
            }

        } else if (methodCallExpression.getParent() instanceof PsiReturnStatementImpl) {
            // value is being returned, so we can use the return type of the method which contains this call
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(
                    methodCallExpression, PsiMethod.class);
            if (parentMethod != null && parentMethod.getReturnType() != null) {
                returnType = parentMethod.getReturnType();
            }
        } else if (methodCallExpression instanceof PsiMethodCallExpression) {
            returnType = ((PsiMethodCallExpression) methodCallExpression).resolveMethod().getReturnType();
        }
        return returnType;
    }
}
