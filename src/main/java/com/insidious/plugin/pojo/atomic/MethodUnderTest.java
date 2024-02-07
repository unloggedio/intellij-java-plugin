package com.insidious.plugin.pojo.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.ClassTypeUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;

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
        return new MethodUnderTest(
                testCandidateMetadata.getMainMethod().getMethodName(), buildMethodSignature(testCandidateMetadata), 0,
                testCandidateMetadata.getFullyQualifiedClassname());
    }

    private static String buildMethodSignature(TestCandidateMetadata testCandidateMetadata) {
        StringBuilder methodSignature = new StringBuilder("(");

        for (Parameter argument : testCandidateMetadata.getMainMethod().getArguments()) {
            String type = argument.getType();
            methodSignature.append(ClassTypeUtils.getDescriptorName(type));
        }

        methodSignature.append(")");
        String type = testCandidateMetadata.getMainMethod().getReturnValue().getType();
        methodSignature.append(ClassTypeUtils.getDescriptorName(type));

        return methodSignature.toString();
    }

    public static MethodUnderTest fromCandidateSearchQuery(CandidateSearchQuery candidateSearchQuery) {
        return new MethodUnderTest(candidateSearchQuery.getMethodName(),
                candidateSearchQuery.getMethodSignature(), 0, candidateSearchQuery.getClassName());
    }

    public static MethodUnderTest fromPsiCallExpression(PsiMethodCallExpression methodCallExpression) {
        PsiExpression fieldExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        PsiReferenceExpression qualifierExpression1 = (PsiReferenceExpression) fieldExpression;
        PsiField fieldPsiInstance = (PsiField) qualifierExpression1.resolve();


        PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        PsiSubstitutor classSubstitutor = TypeConversionUtil.getClassSubstitutor(fieldPsiInstance.getContainingClass(),
                parentOfType, PsiSubstitutor.EMPTY);
        PsiType fieldTypeSubstitutor = ClassTypeUtils.substituteClassRecursively(fieldPsiInstance.getType(),
                classSubstitutor);


        PsiMethod targetMethod = methodCallExpression.resolveMethod();
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethod));
        if (fieldPsiInstance != null && fieldPsiInstance.getType() != null) {
            methodUnderTest.setClassName(fieldPsiInstance.getType().getCanonicalText());
        }

        if (fieldTypeSubstitutor != null) {
            String actualClass = fieldTypeSubstitutor.getCanonicalText();
            methodUnderTest.setClassName(actualClass);
        }
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
