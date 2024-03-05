package com.insidious.plugin.pojo.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.ClassUtils;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;

import java.util.Objects;

public class MethodUnderTest {
    String name;
    String signature;
    String className;
    int methodHash;


    public MethodUnderTest(String name, String signature, int methodHash, String className) {
        this.name = name;
        this.signature = signature;
        this.className = className;
        this.methodHash = methodHash;
    }

    public MethodUnderTest() {
    }

    public static MethodUnderTest fromMethodAdapter(MethodAdapter methodElement) {
        final int methodHash = methodElement.getText().hashCode();
        final String methodName = methodElement.getName();
        final String methodJVMSignature = methodElement.getJVMSignature();
        final String classQualifiedName = methodElement.getContainingClass().getQualifiedName();
        return new MethodUnderTest(methodName, methodJVMSignature, methodHash, classQualifiedName);
    }

    public static MethodUnderTest fromTestCandidateMetadata(TestCandidateMetadata testCandidateMetadata) {
        return MethodUnderTest.fromMethodCallExpression(testCandidateMetadata.getMainMethod());
    }

    private static MethodUnderTest fromMethodCallExpression(MethodCallExpression methodCallExpression) {
        return new MethodUnderTest(
                methodCallExpression.getMethodName(), buildMethodSignature(methodCallExpression), 0,
                methodCallExpression.getSubject().getType());
    }

    private static String buildMethodSignature(MethodCallExpression mainMethod) {
        StringBuilder methodSignature = new StringBuilder("(");

        for (Parameter argument : mainMethod.getArguments()) {
            String type = argument.getType();
            methodSignature.append(ClassTypeUtils.getDescriptorName(type));
        }

        methodSignature.append(")");
        String type = mainMethod.getReturnValue().getType();
        methodSignature.append(ClassTypeUtils.getDescriptorName(type));

        return methodSignature.toString();
    }

    public static String buildMethodSignature(PsiMethodCallExpression mainMethod, PsiSubstitutor substitutor) {

        PsiMethod targetMethod = mainMethod.resolveMethod();
        return buildMethodSignature(targetMethod, substitutor);
    }

    public static String buildMethodSignature(PsiMethod targetMethod, PsiSubstitutor substitutor) {

        StringBuilder methodSignature = new StringBuilder("(");

        for (JvmParameter argument : targetMethod.getParameters()) {
            JvmType type1 = argument.getType();
            String descriptorName;
            descriptorName = getDescriptorName(substitutor, type1);
            methodSignature.append(descriptorName);
        }

        methodSignature.append(")");
        PsiType returnType = targetMethod.getReturnType();
        String returnTypeDescriptor = getDescriptorName(substitutor, returnType);
        methodSignature.append(returnTypeDescriptor);

        return methodSignature.toString();
    }

    private static String getDescriptorName(PsiSubstitutor substitutor, JvmType type1) {
        String descriptorName = null;
        if (type1 instanceof PsiWildcardType) {
            type1 = ClassTypeUtils.substituteClassRecursively((PsiWildcardType) type1, substitutor);
            String type = ((PsiWildcardType) type1).getCanonicalText();
            descriptorName = ClassTypeUtils.getDescriptorName(type);
        } else if (type1 instanceof PsiClassType) {
            type1 = ClassTypeUtils.substituteClassRecursively((PsiType) type1, substitutor);
            String type = ((PsiType) type1).getCanonicalText();
            descriptorName = ClassTypeUtils.getDescriptorName(type);
        } else if (type1 instanceof PsiPrimitiveType) {
            PsiPrimitiveType psiType = (PsiPrimitiveType) type1;
            descriptorName = psiType.getKind().getBinaryName();
        }
        return descriptorName;
    }


    public static MethodUnderTest fromPsiCallExpression(PsiMethodCallExpression methodCallExpression) {
        PsiExpression fieldExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        PsiType callOnType;
        if (fieldExpression instanceof PsiReferenceExpression) {
            PsiReferenceExpression qualifierExpression1 = (PsiReferenceExpression) fieldExpression;
            PsiElement resolve = qualifierExpression1.resolve();
            if (resolve instanceof PsiField) {
                PsiField fieldPsiInstance = (PsiField) resolve;
                callOnType = fieldPsiInstance.getType();
            } else if (resolve instanceof PsiLocalVariable) {
                PsiLocalVariable localVariable = (PsiLocalVariable) resolve;
                callOnType = localVariable.getType();
            } else if (resolve instanceof PsiClass) {
                PsiClass localVariable = (PsiClass) resolve;
                callOnType = JavaPsiFacade.getInstance(methodCallExpression.getProject())
                        .getElementFactory().createType(localVariable);
            } else {
                callOnType = qualifierExpression1.getType();
            }
        } else if (fieldExpression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression mce = (PsiMethodCallExpression) fieldExpression;
            callOnType = mce.getType();
        } else {
            // no qualifier , its a method on the same class
            callOnType = PsiTypesUtil.getClassType(PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class));
//            throw new RuntimeException("Unexpected expression: " + fieldExpression.toString());
        }

        PsiSubstitutor classSubstitutor = ClassUtils.getSubstitutorForCallExpression(methodCallExpression);
        PsiType fieldTypeSubstitutor = ClassTypeUtils.substituteClassRecursively(callOnType, classSubstitutor);


        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(
                new JavaMethodAdapter(methodCallExpression.resolveMethod()));


        methodUnderTest.setClassName(callOnType.getCanonicalText());
        String signatureVal = buildMethodSignature(methodCallExpression, classSubstitutor);
        methodUnderTest.setSignature(signatureVal);

        if (fieldTypeSubstitutor != null) {
            String actualClass = fieldTypeSubstitutor.getCanonicalText();
            methodUnderTest.setClassName(actualClass);
        }
        return methodUnderTest;
    }

    public static MethodUnderTest fromPsiCallExpression(PsiMethod method) {
        PsiType callOnType = PsiTypesUtil.getClassType(Objects.requireNonNull(method.getContainingClass()));

        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(method));


        methodUnderTest.setClassName(callOnType.getCanonicalText());
        String signatureVal = buildMethodSignature(method, new EmptySubstitutor());
        methodUnderTest.setSignature(signatureVal);

        return methodUnderTest;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getMethodHash() {
        return methodHash;
    }

    public void setMethodHash(int methodHash) {
        this.methodHash = methodHash;
    }

    @Override
    public String toString() {
        return "MethodUnderTest{" +
                className + "." + name + "(" + signature + ") " + methodHash +
                '}';
    }

    @JsonIgnore
    public String getMethodHashKey() {
        return className + "#" + name + "#" + signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodUnderTest that = (MethodUnderTest) o;
        return methodHash == that.methodHash
                && Objects.equals(name, that.name)
                && Objects.equals(signature, that.signature)
                && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, signature, className, methodHash);
    }
}
