package com.insidious.plugin.factory.testcase.writer;

import com.insidious.common.weaver.EventType;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.expression.StringExpression;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class PendingStatement {
    public static final ClassName TYPE_TOKEN_CLASS = ClassName.bestGuess("com.google.gson.reflect.TypeToken");
    public static final ClassName LIST_CLASS = ClassName.bestGuess("java.util.List");
    private final ObjectRoutineScript objectRoutine;
    private final List<Expression> expressionList = new LinkedList<>();
    private Parameter lhsExpression;
    private Parameter expectingException;

    public PendingStatement(ObjectRoutineScript objectRoutine) {
        this.objectRoutine = objectRoutine;
    }

    private static void writeCallStatement(
            MethodCallExpression methodCallExpression, StringBuilder statementBuilder,
            List<Object> statementParameters, int i
    ) {
        String parameterString = TestCaseWriter.createMethodParametersString(methodCallExpression.getArguments());
        if (methodCallExpression.getMethodName().equals("<init>")) {
            assert i == 0;
            statementBuilder.append("new $T(").append(parameterString).append(")");
            statementParameters.add(ClassName.bestGuess(methodCallExpression.getSubject().getType()));
        } else if (methodCallExpression.getMethodName().equals("fromJson")
                && methodCallExpression.getSubject().equals(MethodCallExpressionFactory.GsonClass)) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            Parameter objectToDeserialize = variables.get(0);

            if (objectToDeserialize.isContainer() && objectToDeserialize.getTemplateMap().get("E") != null) {

                statementBuilder.append("$L.$L($S, new $T<$T<$T>>(){}.getType())");
                statementParameters.add(methodCallExpression.getSubject().getName());
                statementParameters.add(methodCallExpression.getMethodName());

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));

                statementParameters.add(TYPE_TOKEN_CLASS);
                statementParameters.add(LIST_CLASS);

                Parameter templateParameter = objectToDeserialize.getTemplateMap().get("E");
                String templateParameterType = templateParameter.getType();
                ClassName parameterClassName = ClassName.bestGuess(templateParameterType);
                statementParameters.add(parameterClassName);


            } else {
                statementBuilder.append("$L.$L($S, $T.class)");
                statementParameters.add(methodCallExpression.getSubject().getName());
                statementParameters.add(methodCallExpression.getMethodName());

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));
                statementParameters.add(ClassName.bestGuess(ClassTypeUtils.getJavaClassName(objectToDeserialize.getType())));

            }

        } else if (methodCallExpression.getMethodName().equals("ValueOf") && methodCallExpression.getSubject() == null) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            Parameter objectToDeserialize = variables.get(0);

            if (objectToDeserialize.isContainer() && objectToDeserialize.getTemplateMap().get("E") != null) {

                statementBuilder.append("$L($S, new $T<$T<$T>>(){}.getType())");
                statementParameters.add(methodCallExpression.getMethodName());

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));

                statementParameters.add(TYPE_TOKEN_CLASS);
                statementParameters.add(LIST_CLASS);

                Parameter templateParameter = objectToDeserialize.getTemplateMap().get("E");
                String templateParameterType = templateParameter.getType();
                ClassName parameterClassName = ClassName.bestGuess(templateParameterType);
                statementParameters.add(parameterClassName);


            } else {
                statementBuilder.append("$L($S, $T.class)");
                statementParameters.add(methodCallExpression.getMethodName());

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));
                statementParameters.add(ClassName.bestGuess(ClassTypeUtils.getJavaClassName(objectToDeserialize.getType())));

            }

        } else if (methodCallExpression.getMethodName().equals("injectField")
                && methodCallExpression.getSubject() == null) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();


            Parameter injectionTarget = variables.get(0);
            if (injectionTarget.getName() != null) {
                statementBuilder.append("injectField($L, $S, $L)");
                statementParameters.add(injectionTarget.getName());
            } else if (injectionTarget.getType() != null) {
                statementBuilder.append("injectField($T.class, $S, $L)");
                statementParameters.add(ClassName.bestGuess(injectionTarget.getType()));
            }

            statementParameters.add(variables.get(1).getName());
            statementParameters.add(variables.get(1).getName());

        } else if (methodCallExpression.getMethodName().equals("thenThrow")
                && methodCallExpression.getSubject() == null) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            statementBuilder.append(".$L($T.class)");
            statementParameters.add(methodCallExpression.getMethodName());
            statementParameters.add(ClassName.bestGuess(ClassTypeUtils.getJavaClassName(variables.get(0).getType())));

        } else {
            Parameter callExpressionSubject = methodCallExpression.getSubject();
            if (callExpressionSubject != null) {

                if (Objects.equals(callExpressionSubject.getName(), "Mockito")
                        || Objects.equals(callExpressionSubject.getName(), "Assert")) {
                    statementBuilder.append("$T.$L(").append(parameterString).append(")");
                    statementParameters.add(ClassName.bestGuess(callExpressionSubject.getType()));
                    statementParameters.add(methodCallExpression.getMethodName());
                } else {
                    statementBuilder.append("$L.$L(").append(parameterString).append(")");
                    statementParameters.add(callExpressionSubject.getName());
                    statementParameters.add(methodCallExpression.getMethodName());

                }
            } else {
                if (i > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(").append(parameterString).append(")");
                statementParameters.add(methodCallExpression.getMethodName());
            }
        }
    }

    public PendingStatement writeExpression(Expression expression) {
        this.expressionList.add(expression);
        return this;
    }

    public PendingStatement assignVariable(Parameter lhsExpression) {
        this.lhsExpression = lhsExpression;
        return this;
    }

    public void endStatement() {

        StringBuilder statementBuilder = new StringBuilder();
        List<Object> statementParameters = new LinkedList<>();

        boolean isExceptionExcepted = false;

        if (lhsExpression != null && lhsExpression.getType() != null && !lhsExpression.getType().equals("V")) {

            if (lhsExpression.getName() == null) {
                lhsExpression.setName(generateNameForParameter(lhsExpression));
            }

            if (lhsExpression.getProbeInfo() != null &&
                    lhsExpression.getProbeInfo().getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT) {
                isExceptionExcepted = true;
            } else {


                @Nullable TypeName lhsTypeName = ClassTypeUtils.createTypeFromName(ClassTypeUtils.getJavaClassName(lhsExpression.getType()));
                if (!objectRoutine.getCreatedVariables().contains(lhsExpression.getName())) {
                    objectRoutine.getCreatedVariables().add(lhsExpression);
                    if (lhsExpression.isContainer() && lhsExpression.getTemplateMap().get("E") != null) {
                        statementBuilder.append("$T<$T>").append(" ");
                        statementParameters.add(lhsTypeName);
                        Parameter templateParameter = lhsExpression.getTemplateMap().get("E");
                        String templateType = templateParameter.getType();
                        statementParameters.add(ClassName.bestGuess(templateType));
                    } else {
                        statementBuilder.append("$T").append(" ");
                        statementParameters.add(lhsTypeName);
                    }

                }
                statementBuilder.append("$L");

                statementParameters.add(lhsExpression.getName());

                statementBuilder.append(" = ");
            }
        }

        int i = 0;
        for (Expression expression : this.expressionList) {
            if (expression instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) expression;


                writeCallStatement(methodCallExpression, statementBuilder, statementParameters, i);

            } else if (expression instanceof StringExpression) {
                statementBuilder.append("$S");
                statementParameters.add(expression.toString());
            } else {
                statementBuilder.append(expression.toString());
            }
            i++;
        }

        if (isExceptionExcepted) {
            StringBuilder tryCatchEnclosure = new StringBuilder();
            tryCatchEnclosure
                    .append("        try {\n")
                    .append("            ").append("// this is going to throw exception <>\n")
                    .append("            ").append(statementBuilder).append(";\n")
                    .append("        } catch ($T e) {\n")
                    .append("            \n")
                    .append("        }\n");
            statementParameters.add(ClassName.bestGuess(lhsExpression.getType()));
            statementBuilder = tryCatchEnclosure;
        }


        String statement = statementBuilder.toString();
        if (statement.length() < 1) {
            return;
        }
        objectRoutine.addStatement(statement, statementParameters);
    }

    private String generateNameForParameter(Parameter lhsExpression) {
        String variableName = "var";
        if (lhsExpression != null && lhsExpression.getType() != null) {
            variableName = ClassTypeUtils.createVariableName(lhsExpression.getType());
        }
        for (int i = 0; i < 100; i++) {
            if (!objectRoutine.getCreatedVariables().contains(variableName + i)) {
                return variableName + i;
            }
        }
        return "thisNeverHappened";
    }

    public PendingStatement fromRecordedValue(
            TestCaseGenerationConfiguration generationConfiguration,
            TestGenerationState testGenerationState
    ) {
        if (lhsExpression == null) {
            return this;
        }
        String targetClassname = lhsExpression.getType();

        Parameter variableExistingParameter = objectRoutine
                .getCreatedVariables()
                .getParameterByName(lhsExpression.getName());

        if (variableExistingParameter != null) {

            Object existingValue = variableExistingParameter.getValue();
            Object newValue = lhsExpression.getValue();

            if (variableExistingParameter.getProb().getSerializedValue() != null
                    && variableExistingParameter.getProb().getSerializedValue().length > 0) {
                existingValue = new String(variableExistingParameter.getProb().getSerializedValue());
                newValue = new String(lhsExpression.getProb().getSerializedValue());
            }

            if (Objects.equals(existingValue, newValue)) {
                this.lhsExpression = null;
                this.expressionList.clear();
                return this;
            }

        }

        if (targetClassname == null) {
            // targetClassname is null for methods that return void
            return this;
        }

        if (targetClassname.startsWith("java.lang") || targetClassname.length() == 1) {
            // primitive variable types
            Parameter parameter = lhsExpression;


            Object returnValue = parameter.getValue();
            if (targetClassname.equals("Z")) {
                if (returnValue instanceof String) {
                    if (Objects.equals(returnValue, "0")) {
                        returnValue = "false";
                    } else {
                        returnValue = "true";
                    }
                    this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression((String) returnValue));

                } else if (returnValue instanceof Long) {
                    if ((long) returnValue == 1) {
                        this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression("true"));
                    } else {
                        this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression("false"));
                    }

                }

            } else if (parameter.getProb().getSerializedValue().length > 0) {
                String stringValue = new String(parameter.getProb().getSerializedValue());
                stringValue = stringValue.replaceAll("\\$", "\\$\\$");

                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(stringValue));
            } else {
                String stringValue = String.valueOf(parameter.getValue());
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(stringValue));
            }

        } else {
            // non primitive variable types need to be reconstructed from the JSON values
            if (generationConfiguration.getResourceEmbedMode().equals(ResourceEmbedMode.IN_CODE)) {
                this.expressionList.add(MethodCallExpressionFactory.FromJson(lhsExpression));
            } else if (generationConfiguration.getResourceEmbedMode().equals(ResourceEmbedMode.IN_FILE)) {

                String nameForObject = testGenerationState.addObjectToResource(lhsExpression);
                lhsExpression.getProb().setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
                this.expressionList.add(MethodCallExpressionFactory.FromJsonFetchedFromFile(lhsExpression));

            } else {
                throw new RuntimeException("this never happened");
            }

        }


        return this;
    }
}
