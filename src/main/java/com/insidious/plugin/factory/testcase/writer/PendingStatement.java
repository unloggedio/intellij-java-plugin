package com.insidious.plugin.factory.testcase.writer;

import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.constants.PrimitiveDataType;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PendingStatement {
    public static final ClassName TYPE_TOKEN_CLASS = ClassName.bestGuess("com.google.gson.reflect.TypeToken");
    public static final ClassName LIST_CLASS = ClassName.bestGuess("java.util.List");
    private static final Pattern anyRegexPicker = Pattern.compile("any\\(([^)]+.class)\\)");
    private final ObjectRoutineScript objectRoutine;
    private final List<Expression> expressionList = new LinkedList<>();
    private Parameter lhsExpression;

    public PendingStatement(ObjectRoutineScript objectRoutine) {
        this.objectRoutine = objectRoutine;
    }

    private void writeCallStatement(
            MethodCallExpression methodCallExpression, StringBuilder statementBuilder,
            List<Object> statementParameters, int i
    ) {
        String parameterString = TestCaseWriter.createMethodParametersString(methodCallExpression.getArguments());
        if (methodCallExpression.getMethodName()
                .equals("<init>")) {
            assert i == 0;
            statementBuilder.append("new $T(")
                    .append(parameterString)
                    .append(")");
            statementParameters.add(ClassName.bestGuess(methodCallExpression.getSubject()
                    .getType()));
        } else if (methodCallExpression.getMethodName()
                .equals("fromJson")
                && methodCallExpression.getSubject()
                .equals(MethodCallExpressionFactory.GsonClass)) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            Parameter objectToDeserialize = variables.get(0);

            Map<String, Parameter> templateMap = objectToDeserialize.getTemplateMap();
            if (objectToDeserialize.isContainer() && templateMap.size() > 0) {
                List<String> templateKeys = new LinkedList<>(templateMap.keySet());
                Collections.sort(templateKeys);
                int count = templateKeys.size();


                StringBuilder templateString = new StringBuilder();
                for (int j = 0; j < count; j++) {
                    if (j > 0) {
                        templateString.append(", ");
                    }
                    templateString.append("$T");
                }
                //                        1, 2, 3,      4, 5, 6
                statementBuilder.append("$L.$L($S, new $T<$T<" + templateString + ">>(){}.getType())");
                statementParameters.add(methodCallExpression.getSubject()
                        .getNameForUse(null));  // 1
                statementParameters.add(methodCallExpression.getMethodName()); // 2

                statementParameters.add(new String(objectToDeserialize.getProb()
                        .getSerializedValue())); // 3

                statementParameters.add(TYPE_TOKEN_CLASS); // 4
                statementParameters.add(ClassName.bestGuess(objectToDeserialize.getType())); // 5

                for (String templateKey : templateKeys) {
                    Parameter templateParameter = templateMap
                            .get(templateKey);
                    String templateParameterType = templateParameter.getType();
                    ClassName parameterClassName = ClassName.bestGuess(templateParameterType);
                    statementParameters.add(parameterClassName); // 6
                }


            } else {
                statementBuilder.append("$L.$L($S, $T.class)");
                statementParameters.add(methodCallExpression.getSubject()
                        .getNameForUse(null));
                statementParameters.add(methodCallExpression.getMethodName());

                statementParameters.add(new String(objectToDeserialize.getProb()
                        .getSerializedValue()));
                statementParameters.add(
                        ClassName.bestGuess(ClassTypeUtils.getJavaClassName(objectToDeserialize.getType())));

            }

        } else if (methodCallExpression.getMethodName()
                .equals("ValueOf") && methodCallExpression.getSubject() == null) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            Parameter objectToDeserialize = variables.get(0);

            Map<String, Parameter> templateMap = objectToDeserialize.getTemplateMap();
            if (objectToDeserialize.isContainer() && templateMap.size() > 0) {

                List<String> templateKeys = new LinkedList<>(templateMap.keySet());
                Collections.sort(templateKeys);
                int count = templateKeys.size();


                StringBuilder templateString = new StringBuilder();
                for (int j = 0; j < count; j++) {
                    if (j > 0) {
                        templateString.append(", ");
                    }
                    templateString.append("$T");
                }
                //                        1, 2,      3, 4,    5
                statementBuilder.append("$L($S, new $T<$T<" + templateString + ">>(){}.getType())");
                statementParameters.add(methodCallExpression.getMethodName()); // 1

                statementParameters.add(new String(objectToDeserialize.getProb()
                        .getSerializedValue())); // 2

                statementParameters.add(TYPE_TOKEN_CLASS); // 3
                statementParameters.add(ClassName.bestGuess(objectToDeserialize.getType())); // 4

                for (String templateKey : templateKeys) {
                    Parameter templateParameter = templateMap
                            .get(templateKey);
                    String templateParameterType = templateParameter.getType();
                    ClassName parameterClassName = ClassName.bestGuess(templateParameterType);
                    statementParameters.add(parameterClassName); // 5
                }


            } else {
                statementBuilder.append("$L($S, $T.class)");
                statementParameters.add(methodCallExpression.getMethodName());

                statementParameters.add(new String(objectToDeserialize.getProb()
                        .getSerializedValue()));
                statementParameters.add(
                        ClassName.bestGuess(ClassTypeUtils.getJavaClassName(objectToDeserialize.getType())));

            }

        } else if (methodCallExpression.getMethodName()
                .equals("injectField")
                && methodCallExpression.isStaticCall()
                && methodCallExpression.getSubject() != null
                && methodCallExpression.getSubject()
                .getType()
                .equals("io.unlogged.UnloggedTestUtils")
        ) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();


            Parameter injectionTarget = variables.get(0);
            statementParameters.add(ClassName.bestGuess(methodCallExpression.getSubject()
                    .getType()));
            String targetNameForScript = injectionTarget.getName();
            if (targetNameForScript != null) {
                statementBuilder.append("$T.injectField($L, $S, $L)");
                statementParameters.add(targetNameForScript);
            } else if (injectionTarget.getType() != null) {
                statementBuilder.append("$T.injectField($T.class, $S, $L)");
                statementParameters.add(ClassName.bestGuess(injectionTarget.getType()));
            }

            Parameter secondArgument = variables.get(1);
            String secondArgumentNameForUse = secondArgument.getNameForUse(null);
            statementParameters.add(secondArgumentNameForUse);
            statementParameters.add(secondArgumentNameForUse);

        } else if (methodCallExpression.getMethodName()
                .equals("thenThrow")
                && methodCallExpression.getSubject() == null) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            statementBuilder.append(".$L($T.class)");
            statementParameters.add(methodCallExpression.getMethodName());
            statementParameters.add(ClassName.bestGuess(ClassTypeUtils.getJavaClassName(variables.get(0)
                    .getType())));

        } else if (methodCallExpression.getMethodName()
                .equals("assertEquals")) {
            Parameter callExpressionSubject = methodCallExpression.getSubject();
            if (callExpressionSubject != null) {
                parameterString = TestCaseWriter.createMethodParametersStringWithNames(
                        methodCallExpression.getArguments());
                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(callExpressionSubject.getType()));
                    statementParameters.add(methodCallExpression.getMethodName());
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(callExpressionSubject.getName());
                    statementParameters.add(methodCallExpression.getMethodName());

                }
            } else {
                if (i > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodCallExpression.getMethodName());
            }
        } else if (methodCallExpression.getMethodName()
                .equals("when")) {
            // we need to disect the parameters inside the any() parameters and add them as proper class references
            // subject.methodName(any(com.package.AClass.class), any(ano.ther.package.BClass.class))

            Matcher matcher = anyRegexPicker.matcher(parameterString);
            List<Object> trailingParameters = new LinkedList<>();
            while (matcher.find()) {
                String matchedString = matcher.group();
                String className = matcher.group(1);
                ClassName classNameType = ClassName.bestGuess(className.split("\\.class")[0]);
                int matchedStartIndex = parameterString.indexOf(matchedString);
                parameterString = parameterString.substring(0, matchedStartIndex) + "any($T.class)" +
                        parameterString.substring(matchedStartIndex + matchedString.length());
//                parameterString = parameterString.replaceFirst(matchedString, "$T.class");
                trailingParameters.add(classNameType);
            }
            Parameter callExpressionSubject = methodCallExpression.getSubject();
            if (callExpressionSubject != null) {

                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(callExpressionSubject.getType()));
                    statementParameters.add(methodCallExpression.getMethodName());
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(callExpressionSubject.getName());
                    statementParameters.add(methodCallExpression.getMethodName());

                }
            } else {
                if (i > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodCallExpression.getMethodName());
            }
            statementParameters.addAll(trailingParameters);
        } else if (methodCallExpression.getMethodName()
                .equals("mockStatic")) {
            Parameter callExpressionSubject = methodCallExpression.getSubject();
            parameterString = parameterString + ", $T.CALLS_REAL_METHODS";
            if (callExpressionSubject != null) {

                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(callExpressionSubject.getType()));
                    statementParameters.add(methodCallExpression.getMethodName());

                    // TODO: need to pick from test generation config and not
                    statementParameters.add(ClassName.bestGuess(
                            objectRoutine.getGenerationConfiguration()
                                    .getMockFramework()
                                    .getMockClassParameter()
                                    .getType()
                    ));
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(callExpressionSubject.getName());
                    statementParameters.add(methodCallExpression.getMethodName());
                    statementParameters.add(methodCallExpression.getMethodName());
                    statementParameters.add(ClassName.bestGuess(
                            objectRoutine.getGenerationConfiguration()
                                    .getMockFramework()
                                    .getMockClassParameter()
                                    .getType()
                    ));
                }
            } else {
                if (i > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodCallExpression.getMethodName());
            }
        } else {
            Parameter callExpressionSubject = methodCallExpression.getSubject();
            if (callExpressionSubject != null) {

                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(callExpressionSubject.getType()));
                    statementParameters.add(methodCallExpression.getMethodName());
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(callExpressionSubject.getName());
                    statementParameters.add(methodCallExpression.getMethodName());

                }
            } else {
                if (i > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
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
        List<Object> statementParameters = new ArrayList<>();

        boolean isExceptionExcepted = lhsExpression != null && lhsExpression.getProbeInfo() != null &&
                lhsExpression.getProbeInfo()
                        .getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT;

        if (lhsExpression != null && lhsExpression.getType() != null && !lhsExpression.getType()
                .equals("V") && !isExceptionExcepted) {

            if (lhsExpression.getNameForUse(null) == null) {
                lhsExpression.setName(generateNameForParameter(lhsExpression));
            }

            @Nullable TypeName lhsTypeName = ClassTypeUtils.createTypeFromName(
                    ClassTypeUtils.getJavaClassName(lhsExpression.getType()));

            if (!objectRoutine.getCreatedVariables()
                    .contains(lhsExpression.getNameForUse(null))) {

                objectRoutine.getCreatedVariables()
                        .add(lhsExpression);
                Map<String, Parameter> templateMap = lhsExpression.getTemplateMap();
                if (lhsExpression.isContainer() && templateMap.size() > 0) {

                    StringBuilder templateParams = new StringBuilder();
                    LinkedList<String> templateKeys = new LinkedList<>(templateMap.keySet());
                    Collections.sort(templateKeys);
                    for (int i = 0; i < templateKeys.size(); i++) {
                        if (i > 0) {
                            templateParams.append(", ");
                        }
//                        String templateKey = templateKeys.get(i);
                        templateParams.append("$T");
                    }


                    statementBuilder.append("$T<" + templateParams.toString() + ">")
                            .append(" ");
                    statementParameters.add(lhsTypeName);
                    for (String templateKey : templateKeys) {
                        Parameter templateParameter = templateMap.get(templateKey);
                        String templateType = templateParameter.getType();
                        statementParameters.add(ClassName.bestGuess(templateType));
                    }

                } else {
                    // Add expr for Type and its statement Param ;
                    // eg: statementBuilder:[ $T ] var , statementParameter: [ String ]  => String var
                    statementBuilder.append("$T")
                            .append(" ");
                    statementParameters.add(lhsTypeName);
                }

            } else {
                // if param exists,
                Parameter existingParameter = objectRoutine
                        .getCreatedVariables()
                        .getParameterByName(lhsExpression.getNameForUse(null));

                // Checking if these variables point to the same
                // if not then updating with the latest object i.e. lhsExpression
                if (existingParameter != lhsExpression) {
                    objectRoutine.getCreatedVariables()
                            .remove(existingParameter);
                    objectRoutine.getCreatedVariables()
                            .add(lhsExpression);
                }
            }
            statementBuilder.append("$L");

            statementParameters.add(lhsExpression.getNameForUse(null));

            statementBuilder.append(" = ");
        }

        int i = 0;
        for (Expression expression : this.expressionList) {
            if (expression instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) expression;

                if (!methodCallExpression.isStaticCall()
                        && methodCallExpression.getSubject() != null && methodCallExpression.getSubject()
                        .getValue() != 0) {
                    Parameter existingVariable = objectRoutine.getCreatedVariables()
                            .getParametersById(methodCallExpression.getSubject()
                                    .getValue());
                    if (existingVariable != null) {
                        methodCallExpression.setSubject(existingVariable);
                    }
                }
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
                    .append("try {\n")
                    .append("    ")
                    .append("// this is going to throw exception <")
                    .append(lhsExpression.getType())
                    .append(">\n")
                    .append("    ")
                    .append(statementBuilder)
                    .append(";\n");

            // todo: improve this mce code

            // preparing to write mce for Assertions.assertFalse(true)
            Parameter param = new Parameter();
            param.setType("org.junit.jupiter.api.Assertions");

            List<Parameter> paramL = new ArrayList<>(0);
            Parameter p = new Parameter();
            p.setType("java.lang.Boolean");
            p.setValue(1L);
            paramL.add(p);

            MethodCallExpression mce = new MethodCallExpression("assertFalse", param, paramL, null, 0);
            mce.setStaticCall(true);

            // this line writes the Assertions.assertFalse(true)
            writeCallStatement(mce, tryCatchEnclosure, statementParameters, 2);

            // checks if Exception Type Matches
            tryCatchEnclosure.append(";\n} catch (Exception e) {\n")
                    .append("Assertions.assertEquals($T.class, e.getClass());\n")
                    .append("}\n");

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
            if (!objectRoutine.getCreatedVariables()
                    .contains(variableName + i)) {
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

        String lhsExprName = lhsExpression.getNameForUse(null);
        // todo: prakhar : can I remove "variableExistingParameter" cause the check is also happening in
        //  @Class MethodCallExpression
        Parameter variableExistingParameter = objectRoutine
                .getCreatedVariables()
                .getParameterByNameAndType(lhsExprName, lhsExpression);

        if (variableExistingParameter != null) {

            Object existingValue = variableExistingParameter.getValue();
            Object newValue = lhsExpression.getValue();

            if (variableExistingParameter.getProb()
                    .getSerializedValue() != null
                    && variableExistingParameter.getProb()
                    .getSerializedValue().length > 0) {
                existingValue = new String(variableExistingParameter.getProb()
                        .getSerializedValue());
                newValue = new String(lhsExpression.getProb()
                        .getSerializedValue());
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
            if (targetClassname.equals("Z") || targetClassname.equals("java.lang.Boolean")) {
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

            } else if (parameter.getProb()
                    .getSerializedValue().length > 0) {
                String stringValue = new String(parameter.getProb()
                        .getSerializedValue());
                stringValue = stringValue.replaceAll("\\$", "\\$\\$");

                if (parameter.getType().equals(PrimitiveDataType.BOXED_LONG) || parameter.getType().equals(PrimitiveDataType.LONG)) {
                    stringValue = stringValue + "L";
                }
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(stringValue));
            } else {
                String stringValue = String.valueOf(parameter.getValue());
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(stringValue));
            }

        } else {
            // non primitive variable types need to be reconstructed from the JSON values
            if (generationConfiguration.getResourceEmbedMode()
                    .equals(ResourceEmbedMode.IN_CODE)) {
                this.expressionList.add(MethodCallExpressionFactory.FromJson(lhsExpression));
            } else if (generationConfiguration.getResourceEmbedMode()
                    .equals(ResourceEmbedMode.IN_FILE)) {
                // for enum type equate to EnumClass.ENUM and not ValueOf("ENUM0",Class.class)
                if (lhsExpression.getIsEnum()) {
                    this.expressionList.add(MethodCallExpressionFactory.createEnumExpression(lhsExpression));
                } else {
                    String nameForObject = testGenerationState.addObjectToResource(lhsExpression);
//                    lhsExpression.getProb().setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
                    Parameter buildWithJson = Parameter.cloneParameter(lhsExpression);
                    DataEventWithSessionId prob = new DataEventWithSessionId();
                    prob.setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
                    buildWithJson.setProb(prob);
                    buildWithJson.clearNames();
                    buildWithJson.setName(nameForObject);
                    this.expressionList.add(MethodCallExpressionFactory.FromJsonFetchedFromFile(buildWithJson));
                }

            } else {
                throw new RuntimeException("this never happened");
            }

        }

        return this;
    }

}
