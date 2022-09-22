package com.insidious.plugin.factory.testcase.mock;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;

import java.util.List;

public class MockFactory {

    public static TestCandidateMetadata createParameterMock(Parameter callSubject) {
        if (callSubject.getType().startsWith("java.")) {
            // we want to create the objects from java.lang.* namespace using their real values, so
            // in the test case it looks something like
            // Integer varName = value;
            TestCandidateMetadata testCaseMetadata = buildTestCandidateForBaseClass(callSubject);
            return testCaseMetadata;
        } else {
            // need to move this out to a configurable list of classes which need not
            // be mocked and ideally we already know a way to construct them
            if (callSubject.getType().equals("com.fasterxml.jackson.databind.ObjectMapper")) {
                return createUsingNoArgsConstructor(callSubject);
            } else {
                TestCandidateMetadata testCandidateMetadata = buildMockCandidateForBaseClass(callSubject);
                return testCandidateMetadata;
            }
        }
    }

    private static TestCandidateMetadata buildTestCandidateForBaseClass(Parameter parameter) {

        String javaClassName;
        if (parameter.getType().length() > 1) {
            String[] nameParts = parameter.getType().split(";")[0].split("/");
            javaClassName = nameParts[nameParts.length - 1];
        } else {
            javaClassName = parameter.getProbeInfo().getValueDesc().name();
        }

        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();

        testCandidateMetadata.setTestSubject(null);
//        Parameter returnStringParam = new Parameter();


//        testCandidateMetadata.setFullyQualifiedClassname("java.lang." + javaClassName);
//        testCandidateMetadata.setPackageName("java.lang");
//        testCandidateMetadata.setTestMethodName("<init>");
//        testCandidateMetadata.setUnqualifiedClassname(javaClassName);

        testCandidateMetadata.setMainMethod(
                MethodCallExpressionFactory.PlainValueExpression(String.valueOf(parameter.getValue()))
        );
        return testCandidateMetadata;
    }


    private static TestCandidateMetadata buildMockCandidateForBaseClass(
            Parameter parameter
    ) {

        String parameterTypeName = parameter.getType();
        boolean isArray = false;
        if (parameterTypeName.startsWith("[")) {
            isArray = true;
            parameterTypeName = parameterTypeName.substring(1);
        }

        parameterTypeName = ClassTypeUtils.getDottedClassName(parameterTypeName);
        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();


        ClassName targetClassname = ClassName.bestGuess(parameterTypeName);
        testCandidateMetadata.setTestSubject(null);

//        testCandidateMetadata.setFullyQualifiedClassname(targetClassname.canonicalName());
//        testCandidateMetadata.setPackageName(targetClassname.packageName());
//        testCandidateMetadata.setIsArray(isArray);
//        testCandidateMetadata.setTestMethodName("<init>");


        MethodCallExpression mainMethod = MethodCallExpressionFactory.MockClass(
                ClassName.bestGuess(targetClassname.canonicalName())
        );
        mainMethod.setReturnValue(parameter);
        testCandidateMetadata.setMainMethod(mainMethod);


//        testCandidateMetadata.setUnqualifiedClassname(targetClassname.simpleName());

        return testCandidateMetadata;
    }

    private static TestCandidateMetadata createUsingNoArgsConstructor(Parameter dependentParameter) {



//        String parameterTypeName = dependentParameter.getType();

//        boolean isArray = false;
//        if (parameterTypeName.startsWith("[")) {
//            isArray = true;
//            parameterTypeName = parameterTypeName.substring(1);
//        }

//        parameterTypeName = ClassTypeUtils.getDottedClassName(parameterTypeName);
        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();


//        ClassName targetClassname = ClassName.bestGuess(parameterTypeName);
        testCandidateMetadata.setTestSubject(null);
//        Parameter returnStringParam = new Parameter();

//        testCandidateMetadata.setFullyQualifiedClassname(targetClassname.canonicalName());
//        testCandidateMetadata.setPackageName(targetClassname.packageName());
//        testCandidateMetadata.setIsArray(isArray);
//        testCandidateMetadata.setTestMethodName("<init>");


        testCandidateMetadata.setMainMethod(
                new MethodCallExpression("<init>", dependentParameter, new VariableContainer(),
                        dependentParameter));

//        testCandidateMetadata.setUnqualifiedClassname(targetClassname.simpleName());

        return testCandidateMetadata;
    }


}
