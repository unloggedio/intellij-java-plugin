package com.insidious.plugin.factory.testcase.writer;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.expression.*;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.ParameterUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.insidious.plugin.util.ClassTypeUtils.createTypeFromTypeDeclaration;

public class PendingStatement {
    private static final Pattern anyRegexPicker = Pattern.compile("[( ,]any\\(([^)]+.class)\\)");
    private final ObjectRoutineScript objectRoutine;
    private final List<Expression> expressionList = new ArrayList<>();
    private final TestGenerationState testGenerationState;
    private Parameter lhsExpression;

    public PendingStatement(ObjectRoutineScript objectRoutine, TestGenerationState testGenerationState) {
        this.objectRoutine = objectRoutine;
        this.testGenerationState = testGenerationState;
    }

    public static PendingStatement in(ObjectRoutineScript objectRoutine, TestGenerationState testGenerationState) {
        return new PendingStatement(objectRoutine, testGenerationState);
    }

    /**
     * @param variableContainer list of parameters to be arranged
     * @return a string which is comma separated values to be passed to a method
     */
    public static String createMethodParametersStringWithNames(
            List<Parameter> variableContainer,
            TestGenerationState testGenerationState) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);
            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            makeParameterNameString(parameterStringBuilder, parameter, testGenerationState);
        }
        return parameterStringBuilder.toString();

    }

    //Handling order: name , [array, null , bool, , Primitive,]  all
    private static void makeParameterNameString(StringBuilder parameterStringBuilder, Parameter parameter,
                                                TestGenerationState testGenerationState) {
        String nameUsed = testGenerationState.getParameterNameFactory()
                .getNameForUse(parameter, null);
        if (nameUsed != null) {
            parameterStringBuilder.append(nameUsed);
            return;
        }

        if (handleValueBlockString(parameterStringBuilder, parameter, testGenerationState)) {
            return;
        }

        makeValueForOtherClasses(parameterStringBuilder, parameter);
    }

    private static boolean handleValueBlockString(
            StringBuilder parameterStringBuilder,
            Parameter parameter,
            TestGenerationState testGenerationState
    ) {

        String serializedValue = "";
        if (parameter.getProb() != null &&
                parameter.getProb().getSerializedValue().length > 0)
            serializedValue = new String(parameter.getProb().getSerializedValue());

        if (parameter.getType() != null && parameter.getType().endsWith("[]")) {
            // if the type of parameter is array like int[], long[] (i.e J[])
            String nameUsed = testGenerationState.getParameterNameFactory().getNameForUse(parameter, null);
            parameterStringBuilder.append(nameUsed == null ? "any()" : nameUsed);
            return true;
        }

        if (serializedValue.equals("null")) {
            // if the serialized value is null just append null
            parameterStringBuilder.append("null");
            return true;
        }

        if (parameter.isBooleanType()) {
            long value = parameter.getValue();
            parameterStringBuilder.append(value == 1L ? "true" : "false");
            return true;
        }

        if (parameter.isPrimitiveType()) {
            parameterStringBuilder.append(handlePrimitiveParameter(parameter, serializedValue));
            return true;
        }
        return false;
    }


    private static String handlePrimitiveParameter(Parameter parameter, String serializedValue) {
        StringBuilder valueBuilder = new StringBuilder();
        if (parameter.isBoxedPrimitiveType() && !serializedValue.isEmpty()) {
            serializedValue = ParameterUtils.addParameterTypeSuffix(serializedValue, parameter.getType());
            valueBuilder.append(serializedValue);
        } else {
            if (serializedValue.isEmpty()) {
                valueBuilder.append(ParameterUtils.makeParameterValueForPrimitiveType(parameter));
            } else {
                valueBuilder.append(serializedValue);
            }
        }

        return valueBuilder.toString();
    }


    private static String createMethodParametersString(List<Parameter> variableContainer,
                                                       TestGenerationState testGenerationState) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);
            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            makeParameterValueString(parameterStringBuilder, parameter, testGenerationState);
        }


        return parameterStringBuilder.toString();
    }

    // handling order: [ array(name), null, boolean, primitive,]  name,    all ,
    private static void makeParameterValueString(
            StringBuilder parameterStringBuilder,
            Parameter parameter,
            TestGenerationState testGenerationState
    ) {

        if (handleValueBlockString(parameterStringBuilder, parameter, testGenerationState)) {
            return;
        }

        String nameUsed = testGenerationState.getParameterNameFactory().getNameForUse(parameter, null);
        if (nameUsed != null) {
            if (nameUsed.contains("$")) {
                nameUsed =  nameUsed.replace('$', 'D');
            }
            parameterStringBuilder.append(nameUsed);
            return;
        }

        makeValueForOtherClasses(parameterStringBuilder, parameter);
    }

    private static void makeValueForOtherClasses(StringBuilder parameterStringBuilder, Parameter parameter) {
        Object parameterValue;
        parameterValue = parameter.getValue();
        String stringValue = parameter.getStringValue();
        if (stringValue == null) {
            if (!parameter.isPrimitiveType() && parameter.getValue() == 0) {
                parameterValue = "null";
            }
            parameterStringBuilder.append(parameterValue);
        } else {
            parameterStringBuilder.append(stringValue);
        }
    }


    private void writeCallStatement(
            MethodCallExpression methodCallExpression, StringBuilder statementBuilder,
            List<Object> statementParameters, int chainedCallNumber
    ) {
        String parameterString = createMethodParametersString(methodCallExpression.getArguments(),
                testGenerationState);
        ParameterNameFactory nameFactory = testGenerationState.getParameterNameFactory();
        final String methodName = methodCallExpression.getMethodName();
        Parameter methodCallSubject = methodCallExpression.getSubject();
        if (methodName.equals("<init>")) {
            assert chainedCallNumber == 0;
            statementBuilder.append("new $T(")
                    .append(parameterString)
                    .append(")");
            statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
        } else if (methodName.equals(JsonFramework.Gson.getFromJsonMethodName())
                && methodCallSubject.equals(JsonFramework.Gson.getInstance())) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            Parameter objectToDeserialize = variables.get(0);

            List<Parameter> templateMap = objectToDeserialize.getTemplateMap();
            if (objectToDeserialize.isContainer() && templateMap.size() > 0) {
                List<String> templateKeys = templateMap
                        .stream()
                        .map(Parameter::getName)
                        .sorted()
                        .collect(Collectors.toList());
                int count = templateKeys.size();


                StringBuilder templateString = new StringBuilder();
                for (int j = 0; j < count; j++) {
                    if (j > 0) {
                        templateString.append(", ");
                    }
                    templateString.append("$T");
                }
                //                1, 2, 3,      4, 5, 6
                statementBuilder
                        .append("$L.$L($S, new $T<$T<")
                        .append(templateString)
                        .append(">>(){}.getType())");
                statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));  // 1
                statementParameters.add(methodName); // 2

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue())); // 3

                statementParameters.add(JsonFramework.Gson.getTokenTypeClass()); // 4
                statementParameters.add(ClassName.bestGuess(objectToDeserialize.getType())); // 5

                for (String templateKey : templateKeys) {
                    Optional<Parameter> templateParameter = templateMap
                            .stream()
                            .filter(e -> e.getName().equals(templateKey))
                            .findFirst();
                    assert templateParameter.isPresent();
                    String templateParameterType = templateParameter.get().getType();
                    TypeName parameterClassName = createTypeFromTypeDeclaration(templateParameterType);
                    statementParameters.add(parameterClassName); // 6
                }


            } else {
                statementBuilder.append("$L.$L($S, $T.class)");
                statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));
                statementParameters.add(methodName);

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));
                statementParameters.add(ClassTypeUtils.createTypeFromNameString(objectToDeserialize.getType()));

            }

        }
        else if (methodName.equals(JsonFramework.Jackson.getFromJsonMethodName()) && methodCallSubject.equals(JsonFramework.Jackson.getInstance())) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            Parameter objectToDeserialize = variables.get(0);

            List<Parameter> templateMap = objectToDeserialize.getTemplateMap();
            if (objectToDeserialize.isContainer() && templateMap.size() > 0) {
                List<String> templateKeys = templateMap
                        .stream()
                        .map(Parameter::getName)
                        .sorted()
                        .collect(Collectors.toList());
                int templateParameterCount = templateKeys.size();


                StringBuilder templateString = new StringBuilder();
                for (int j = 0; j < templateParameterCount; j++) {
                    if (j > 0) {
                        templateString.append(", ");
                    }
                    templateString.append("$T");
                }
                //                1, 2, 3,      4, 5, 6
                statementBuilder
                        .append("$L.$L($S, new $T<$T<")
                        .append(templateString)
                        .append(">>(){})");
                statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));  // 1
                statementParameters.add(methodName); // 2

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue())); // 3

                statementParameters.add(JsonFramework.Jackson.getTokenTypeClass()); // 4
                statementParameters.add(ClassName.bestGuess(objectToDeserialize.getType())); // 5

                for (String templateKey : templateKeys) {
                    Optional<Parameter> templateParameter = templateMap
                            .stream()
                            .filter(e -> e.getName().equals(templateKey))
                            .findFirst();
                    assert templateParameter.isPresent();
                    String templateParameterType = templateParameter.get().getType();
                    TypeName parameterClassName = createTypeFromTypeDeclaration(templateParameterType);
                    statementParameters.add(parameterClassName); // 6
                }


            } else {
                statementBuilder.append("$L.$L($S, $T.class)");
                statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));
                statementParameters.add(methodName);

                statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));
                statementParameters.add(ClassTypeUtils.createTypeFromNameString(objectToDeserialize.getType()));

            }

        }
        else if (methodName.equals("ValueOf") && methodCallSubject == null) {

            List<? extends Parameter> variables = methodCallExpression.getArguments();
            Parameter objectToDeserialize = variables.get(0);
            List<Parameter> templateMap = objectToDeserialize.getTemplateMap();

            if (objectToDeserialize.isContainer() && templateMap.size() > 0) {
                statementBuilder.append("$L($S, new $T<");
                statementParameters.add(methodName); // 1
                statementParameters.add(new String(objectToDeserialize.getProb()
                        .getSerializedValue())); // 2

                // todo : this if can be used for all the jackson serialization eventually
                if (objectToDeserialize.isOptionalType())
                    statementParameters.add(JsonFramework.Jackson.getTokenTypeClass()); // 3
                else
                    statementParameters.add(JsonFramework.Gson.getTokenTypeClass()); // 3

                Parameter deepCopyParam = new Parameter(objectToDeserialize);
                ParameterUtils.createStatementStringForParameter(deepCopyParam, statementBuilder, statementParameters);

                // suspect that this was added for jackson mapper with optional value container,
                // but commenting out this change after testing with jackson
//                if (objectToDeserialize.isOptionalType())
//                    statementBuilder.append(">(){})");
//                else
                statementBuilder.append(">(){}.getType())");

            } else {
                statementBuilder.append("$L($S, $T.class)");
                statementParameters.add(methodName);

                statementParameters.add(new String(objectToDeserialize.getProb()
                        .getSerializedValue()));

                TypeName typeOfParam =
                        ClassTypeUtils.createTypeFromNameString(
                                ClassTypeUtils.getJavaClassName(objectToDeserialize.getType()));
//                statementParameters.add(ClassName.bestGuess(typeOfParam));
                statementParameters.add(typeOfParam);
            }
        }
        else if (methodName.equals("injectField") && methodCallExpression.isStaticCall() && methodCallSubject != null && methodCallSubject.getType().equals("io.unlogged.UnloggedTestUtils")) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();


            Parameter injectionTarget = variables.get(0);
            statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
            String targetNameForScript = nameFactory.getNameForUse(injectionTarget, null);
            if (targetNameForScript != null) {
                statementBuilder.append("$T.injectField($L, $S, $L)");
                statementParameters.add(targetNameForScript);
            } else if (injectionTarget.getType() != null) {
                statementBuilder.append("$T.injectField($T.class, $S, $L)");
                statementParameters.add(ClassName.bestGuess(injectionTarget.getType()));
            }

            Parameter secondArgument = variables.get(1);
            String secondArgumentNameForUse = nameFactory.getNameForUse(secondArgument, null);
            statementParameters.add(secondArgumentNameForUse);
            statementParameters.add(secondArgumentNameForUse);

        }
        else if (methodName.equals("thenThrow") && methodCallSubject == null) {


            List<? extends Parameter> variables = methodCallExpression.getArguments();

            statementBuilder.append(".$L($T.class)");
            statementParameters.add(methodName);
            statementParameters.add(ClassName.bestGuess(ClassTypeUtils.getJavaClassName(variables.get(0)
                    .getType())));

        }
        else if (methodName.equals("assertEquals")) {
            if (methodCallSubject != null) {
                parameterString = createMethodParametersStringWithNames(
                        methodCallExpression.getArguments(), testGenerationState);
                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
                    statementParameters.add(methodName);
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));
                    statementParameters.add(methodName);

                }
            } else {
                if (chainedCallNumber > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodName);
            }
        }
        else if (methodName.equals("when")) {
            // we need to disect the parameters inside the any() parameters and add them as proper class references
            // subject.methodName(any(com.package.AClass.class), any(ano.ther.package.BClass.class))

            Matcher matcher = anyRegexPicker.matcher(parameterString);
            List<Object> trailingParameters = new ArrayList<>();
            while (matcher.find()) {
                String matchedString = matcher.group();
                String className = matcher.group(1);
                if (className.contains("<")) {
                    className = className.substring(0, className.indexOf("<")) + className.substring(
                            className.lastIndexOf(">") + 1);
                }
                TypeName classNameType = ClassTypeUtils.createTypeFromNameString(className.split("\\.class")[0]);
                int matchedStartIndex = parameterString.indexOf(matchedString) + 1;
                parameterString = parameterString.substring(0, matchedStartIndex) + "any($T.class)" +
                        parameterString.substring(matchedStartIndex + matchedString.length() - 1);
//                parameterString = parameterString.replaceFirst(matchedString, "$T.class");
                trailingParameters.add(classNameType);
            }
            if (methodCallSubject != null) {

                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
                    statementParameters.add(methodName);
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));
                    statementParameters.add(methodName);

                }
            } else {
                if (chainedCallNumber > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodName);
            }
            statementParameters.addAll(trailingParameters);
        }
        else if (methodName.equals("mockStatic")) {
            parameterString = parameterString + ", $T.CALLS_REAL_METHODS";
            if (methodCallSubject != null) {

                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
                    statementParameters.add(methodName);

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
                    statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));
                    statementParameters.add(methodName);
                    statementParameters.add(methodName);
                    statementParameters.add(ClassName.bestGuess(
                            objectRoutine.getGenerationConfiguration()
                                    .getMockFramework()
                                    .getMockClassParameter()
                                    .getType()
                    ));
                }
            } else {
                if (chainedCallNumber > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodName);
            }
        }
        else if (methodName.equals("mock")) {
            statementBuilder.append("$T.$L(")
                    .append("$T.class")
                    .append(")");
            statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
            statementParameters.add(methodName);
            statementParameters.add(ClassName.bestGuess(parameterString));

        } else {
            if (methodCallSubject != null) {

                if (methodCallExpression.isStaticCall()) {
                    statementBuilder.append("$T.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(ClassName.bestGuess(methodCallSubject.getType()));
                    statementParameters.add(methodName);
                } else {
                    statementBuilder.append("$L.$L(")
                            .append(parameterString)
                            .append(")");
                    statementParameters.add(nameFactory.getNameForUse(methodCallSubject, null));
                    statementParameters.add(methodName);

                }
            } else {
                if (chainedCallNumber > 0) {
                    statementBuilder.append(".");
                }
                statementBuilder.append("$L(")
                        .append(parameterString)
                        .append(")");
                statementParameters.add(methodName);
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

        ParameterNameFactory nameFactory = testGenerationState.getParameterNameFactory();
        StringBuilder statementBuilder = new StringBuilder();
        List<Object> statementParameters = new ArrayList<>();
//        logger.warn("Complete statement: Lhs [" + lhsExpression + "] => ");
//        for (int i = /0; i < expressionList.size(); i++) {
//            Expression expression = expressionList.get(i);
//            logger.warn(" [" + i + "] Rhs [" + expression + "]");
//        }

        boolean isExceptionExcepted = lhsExpression != null && lhsExpression.getProbeInfo() != null &&
                lhsExpression.getProbeInfo()
                        .getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT;

        VariableContainer createdVariables = objectRoutine.getCreatedVariables();
        if (lhsExpression != null && lhsExpression.getType() != null && !lhsExpression.getType()
                .equals("V") && !isExceptionExcepted) {

            if (nameFactory.getNameForUse(lhsExpression, null) == null) {
                lhsExpression.setName(generateNameForParameter(lhsExpression));
            }

            TypeName lhsTypeName = ClassTypeUtils.createTypeFromNameString(
                    ClassTypeUtils.getJavaClassName(lhsExpression.getType()));

            if (!createdVariables.contains(nameFactory.getNameForUse(lhsExpression, null))) {

                createdVariables.add(lhsExpression);
                List<Parameter> templateMap = lhsExpression.getTemplateMap();
                if (lhsExpression.isContainer() && templateMap.size() > 0) {

                    // creating a deep copy of the lhsExpression type and templateMap only
                    // for handling generic type
                    Parameter deepCopyParam = new Parameter(lhsExpression);
                    ParameterUtils.createStatementStringForParameter(deepCopyParam, statementBuilder,
                            statementParameters);

                    statementBuilder.append(" ");
                } else {
                    // Add expr for Type and its statement Param ;
                    // eg: statementBuilder:[ $T ] var , statementParameter: [ String ]  => String var
                    statementBuilder.append("$T").append(" ");

                    statementParameters.add(lhsTypeName);
                }

            } else {
                // if param exists,
                Parameter existingParameter = createdVariables
                        .getParameterByName(nameFactory.getNameForUse(lhsExpression, null));

                // Checking if these variables point to the same
                // if not then updating with the latest object i.e. lhsExpression
                if (existingParameter != lhsExpression) {
                    createdVariables
                            .remove(existingParameter);
                    createdVariables
                            .add(lhsExpression);
                }
            }
            statementBuilder.append("$L");

            statementParameters.add(nameFactory.getNameForUse(lhsExpression, null));

            statementBuilder.append(" = ");
        }

        int i = 0;
        for (Expression expression : this.expressionList) {
            if (expression instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) expression;

                if (!methodCallExpression.isStaticCall()
                        && methodCallExpression.getSubject() != null
                        && methodCallExpression.getSubject().getValue() != 0) {
                    Parameter existingVariable = createdVariables
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
            } else if (expression instanceof NullExpression) {
                statementBuilder.append("null");
//                statementParameters.add(expression.toString());
            } else if (expression instanceof ClassValueExpression) {
                statementBuilder.append("$T.class");
                statementParameters.add(ClassName.bestGuess(expression.toString()));
            } else {
                statementBuilder.append(expression.toString());
            }
            i++;
        }

        if (isExceptionExcepted) {
            StringBuilder tryCatchEnclosure = new StringBuilder();
            String exceptionType = lhsExpression.getType();
            exceptionType = ClassTypeUtils.getJavaClassName(exceptionType);
            tryCatchEnclosure
                    .append("try {\n")
                    .append("    ")
                    .append("// this is going to throw exception <")
                    .append(exceptionType)
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

            // todo: use this MethodCallExpressionFactory.MockitoAssertFalse(param,objectRoutine.getGenerationConfiguration());
            //  to make the mce
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
            variableName = ClassTypeUtils.getJavaClassName(ClassTypeUtils.createVariableName(lhsExpression.getType()));

            if (variableName.endsWith("[]"))
                // we don't want [] in the name generated from type
                variableName = variableName.substring(0, variableName.indexOf("["));


            // for ignoring reserved words in java like boolean, int etc.
            if (lhsExpression.isPrimitiveType())
                variableName += "Var";

            if (!objectRoutine.getCreatedVariables()
                    .contains(variableName)) {
                return variableName;
            }
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
            TestCaseGenerationConfiguration generationConfiguration
    ) {
        if (lhsExpression == null) {
            return this;
        }

        String targetClassname = lhsExpression.getType();

        String lhsExprName = testGenerationState.getParameterNameFactory()
                .getNameForUse(lhsExpression, null);
        // todo: prakhar : can I remove "variableExistingParameter" cause the check is also happening in
        //  @Class MethodCallExpression
        Parameter variableExistingParameter = objectRoutine
                .getCreatedVariables()
                .getParameterByNameAndType(lhsExprName, lhsExpression);

        if (variableExistingParameter != null) {

            Object existingValue = variableExistingParameter.getValue();
            Object newValue = lhsExpression.getValue();

            if (variableExistingParameter.getProb().getSerializedValue() != null &&
                    variableExistingParameter.getProb().getSerializedValue().length > 0) {
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

        if ((targetClassname.startsWith("java.lang")
                && !targetClassname.equals("java.lang.Object"))
                || targetClassname.length() == 1) {
            // primitive variable types
            Parameter parameter = lhsExpression;
            long returnValue = parameter.getValue();

            String serializedValue = "";
            if (parameter.getProb() != null && parameter.getProb().getSerializedValue().length > 0)
                serializedValue = new String(parameter.getProb().getSerializedValue());

            if (serializedValue.equals("null")) {
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(serializedValue));
            } else if (parameter.isBooleanType()) {
                if (returnValue == 1L) {
                    this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression("true"));
                } else {
                    this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression("false"));
                }

            } else if (targetClassname.equals("java.lang.Class")) {
                if (serializedValue.contains("$")) {
                    serializedValue = serializedValue.substring(0, serializedValue.indexOf("$"));
                }
                this.expressionList.add(MethodCallExpressionFactory.ClassValueExpression(serializedValue));

            } else if (targetClassname.equals("java.lang.StringBuilder")) {
                Parameter parameterWithValue = new Parameter();
                parameterWithValue.setValue(new String(parameter.getProb().getSerializedValue()));
                parameterWithValue.setType("java.lang.String");
                MethodCallExpression mce = new MethodCallExpression("<init>", parameter,
                        Collections.singletonList(parameterWithValue), parameter, 0);
                this.expressionList.add(mce);

            } else if (!serializedValue.isEmpty()) {
                serializedValue = serializedValue.replaceAll("\\$", "\\$\\$");

                serializedValue = ParameterUtils.addParameterTypeSuffix(serializedValue, parameter.getType());
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(serializedValue));
            } else {
                serializedValue = ParameterUtils.makeParameterValueForPrimitiveType(parameter);
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(serializedValue));
            }

        } else {
            // non primitive variable types need to be reconstructed from the JSON values
            ResourceEmbedMode resourceEmbedMode = generationConfiguration.getResourceEmbedMode();
            if (resourceEmbedMode.equals(ResourceEmbedMode.IN_CODE)) {
                this.expressionList.add(MethodCallExpressionFactory.FromJson(lhsExpression, generationConfiguration));
            } else if (resourceEmbedMode.equals(ResourceEmbedMode.IN_FILE)) {
                // for enum type equate to EnumClass.ENUM and not ValueOf("ENUM0",Class.class)
                if (lhsExpression.getIsEnum()) {
                    this.expressionList.add(MethodCallExpressionFactory.createEnumExpression(lhsExpression));
                } else {
                    String nameForObject = testGenerationState.addObjectToResource(lhsExpression);
//                    lhsExpression.getProb().setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
                    Parameter buildWithJson = new Parameter(lhsExpression);
                    DataEventWithSessionId prob = new DataEventWithSessionId();
                    prob.setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
                    buildWithJson.setProbeAndProbeInfo(prob, new DataInfo());
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
