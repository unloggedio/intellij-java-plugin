package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.adapter.java.JavaParameterAdapter;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
//import com.insidious.plugin.ui.highlighter.MockMethodLineHighlighter;
import com.insidious.plugin.util.ClassUtils;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.insidious.plugin.ui.assertions.MockValueMap.getChildrenOfTypeRecursive;

public class MockUtils {

    // Creates dummy mocks for an entire class
    @Deprecated
    public static ArrayList<DeclaredMock> getDeclaredMocksForClass(ClassAdapter classAdapter) {
        ArrayList<DeclaredMock> declaredMocks = new ArrayList<>();
        PsiMethodCallExpression[] methodCallExpressions = getChildrenOfTypeRecursive(classAdapter.getSource(),
                PsiMethodCallExpression.class);
        if (methodCallExpressions == null || methodCallExpressions.length == 0) {
            return new ArrayList<>();
        }
        List<PsiMethodCallExpression> mockableCallExpressions = Arrays.stream(methodCallExpressions).collect(Collectors.toList());
//                .filter(MockMethodLineHighlighter::isNonStaticDependencyCall)
//                .collect(Collectors.toList());

        for (PsiMethodCallExpression local : mockableCallExpressions) {
            PsiMethod methodFromExpression = local.resolveMethod();
            if (methodFromExpression != null) {
                DeclaredMock newmock = createDummyMock(
                        new JavaMethodAdapter(methodFromExpression), local);
                declaredMocks.add(newmock);
            }
        }
        return declaredMocks;
    }

    // Creates dummy mocks for only a single method
    @Deprecated
    public static ArrayList<DeclaredMock> getDeclaredMocksForMethod(MethodAdapter methodAdapter) {
        ArrayList<DeclaredMock> declaredMocks = new ArrayList<>();
        PsiMethodCallExpression[] methodCallExpressions = getChildrenOfTypeRecursive(methodAdapter.getPsiMethod(),
                PsiMethodCallExpression.class);
        if (methodCallExpressions == null || methodCallExpressions.length == 0) {
            return new ArrayList<>();
        }
        List<PsiMethodCallExpression> mockableCallExpressions = Arrays.stream(methodCallExpressions)
//                .filter(MockMethodLineHighlighter::isNonStaticDependencyCall)
                .collect(Collectors.toList());

        for (PsiMethodCallExpression local : mockableCallExpressions) {
            if (methodContainsCall(methodAdapter.getText(), local.getText())) {
                PsiMethod methodFromExpression = local.resolveMethod();
                if (methodFromExpression != null) {
                    try {
                        DeclaredMock newmock = createDummyMock(
                                methodAdapter, local);
                        if (newmock != null) {
                            declaredMocks.add(newmock);
                        }
                    } catch (Exception e) {
                        //don't add mocks in this case.
                        //caused when the number of arguments used in expression is different from what method expects (from PSI).
                        //expression vs psClass method prams usage
//                        logger.info("Failed to create a mock for method : " + methodAdapter.getName());
//                        logger.info("Expression  : " + local.getText());
                    }
                }
            }
        }
        return declaredMocks;
    }

    private static boolean methodContainsCall(String methodText, String methodCall) {
        try {
            return methodText.contains(methodCall);
        } catch (NullPointerException e) {
            //if you get a null pointer exception because body is null, return false
            return false;
        }
    }

    @NotNull
    private static ThenParameter createDummyThenParameter(String value, String returnTypeName) {
        ReturnValue returnValue = new ReturnValue(value, returnTypeName, ReturnValueType.REAL);
        return new ThenParameter(returnValue, MethodExitType.NORMAL);
    }

    private static String buildJvmClassName(PsiType returnType) {
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

    private static DeclaredMock createDummyMock(MethodAdapter methodBeingRun,
                                                PsiMethodCallExpression methodCallExpression) {
        //skip mocking any log calls
        if (methodCallExpression.getText().contains("log.")
                || methodCallExpression.getText().contains("logger.")) {
            //return null if the method is a logger method
            return null;
        }

        PsiMethod destinationMethod = methodCallExpression.resolveMethod();
        MethodUnderTest destinationMethodUnterTest =
                MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(destinationMethod));
        destinationMethodUnterTest = processClassnameForDestinationMethod(destinationMethodUnterTest,
                methodCallExpression);
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
//        DeclaredMock mock = new DeclaredMock(
//                "mock response " + expressionText,
//                destinationMethodUnterTest.getClassName(),
//                parentClass.getQualifiedName(),
//                fieldName,
//                destinationMethodUnterTest.getName(),
//                parameterList, thenParameterList
//        );
        DeclaredMock mock = new DeclaredMock();
        return mock;
    }

    @Nullable
    private static PsiType identifyReturnType(PsiExpression methodCallExpression) {
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

    private static MethodUnderTest processClassnameForDestinationMethod(MethodUnderTest methodUnderTest,
                                                                        PsiMethodCallExpression expression) {
        PsiExpression fieldExpression = expression.getMethodExpression().getQualifierExpression();
        PsiReferenceExpression qualifierExpression1 = (PsiReferenceExpression) fieldExpression;
        PsiField fieldPsiInstance = (PsiField) qualifierExpression1.resolve();

        PsiClass parentOfType = PsiTreeUtil.getParentOfType(expression, PsiClass.class);
        PsiType fieldTypeSubstitutor = TypeConversionUtil.getClassSubstitutor(fieldPsiInstance.getContainingClass(),
                parentOfType, PsiSubstitutor.EMPTY).substitute(fieldPsiInstance.getType());

        PsiMethod targetMethod = expression.resolveMethod();
        methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethod));
        if (fieldPsiInstance != null && fieldPsiInstance.getType() != null) {
            methodUnderTest.setClassName(fieldPsiInstance.getType().getCanonicalText());
        }

        if (fieldTypeSubstitutor != null) {
            String actualClass = fieldTypeSubstitutor.getCanonicalText();
            methodUnderTest.setClassName(actualClass);
        }
        return methodUnderTest;
    }
}
