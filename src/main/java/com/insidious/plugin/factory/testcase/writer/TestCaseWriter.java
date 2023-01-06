package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ParameterUtils;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestCaseWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestCaseWriter.class);

    @NotNull
    public static String createMethodParametersString(List<Parameter> variableContainer, TestGenerationState testGenerationState) {
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


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }

    // handling order: [ array(name), null, boolean, primitive,]  name,    all ,
    public static void makeParameterValueString(StringBuilder parameterStringBuilder, Parameter parameter,
                                                TestGenerationState testGenerationState) {

        if (handleValueBlockString(parameterStringBuilder, parameter, testGenerationState)) {
            return;
        }

        String nameUsed = testGenerationState.getParameterNameFactory()
                .getNameForUse(parameter, null);
        if (nameUsed != null) {
            parameterStringBuilder.append(nameUsed);
            return;
        }

        makeValueForOtherClasses(parameterStringBuilder, parameter);
    }

    public static String handlePrimitiveParameter(Parameter parameter, String serializedValue) {
        StringBuilder valueBuilder = new StringBuilder();
        if (parameter.isBoxedPrimitiveType() && !serializedValue.isEmpty()) {
            serializedValue = ParameterUtils.addParameterTypeSuffix(serializedValue, parameter.getType());
            valueBuilder.append(serializedValue);
        } else {
            valueBuilder.append(ParameterUtils.makeParameterValueForPrimitiveType(parameter));
        }

        return valueBuilder.toString();
    }

    private static boolean handleValueBlockString(
            StringBuilder parameterStringBuilder,
            Parameter parameter,
            TestGenerationState testGenerationState
    ) {

        String serializedValue = "";
        if (parameter.getProb() != null
                && parameter.getProb()
                .getSerializedValue().length > 0)
            serializedValue = new String(parameter.getProb()
                    .getSerializedValue());

        if (parameter.getType() != null && parameter.getType()
                .endsWith("[]")) {
            // if the type of parameter is array like int[], long[] (i.e J[])
            String nameUsed = testGenerationState.getParameterNameFactory()
                    .getNameForUse(parameter, null);
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


    @NotNull
    public static String
    createMethodParametersStringMock(List<Parameter> variableContainer, TestGenerationState testGenerationState) {
        logger.warn("Create method parameters argument mock => " + variableContainer);
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            Object compareAgainst;
            String parameterType = parameter.getType();
            if (parameterType != null && parameterType.endsWith("[]")) {
                compareAgainst = "";
            } else if (parameter.getProb() != null
                    && parameter.getProb()
                    .getSerializedValue().length > 0
                    && (new String(parameter.getProb()
                    .getSerializedValue())).equals("null")) {

                // if the serialized value is null just append null
                compareAgainst = "null";
            } else if (parameter.isPrimitiveType()) {
                if (parameter.isBoxedPrimitiveType()) {
                    String serialisedValue = new String(parameter.getProb()
                            .getSerializedValue());
                    if (serialisedValue.length() > 0) {
                        compareAgainst = serialisedValue;
                    } else {
                        compareAgainst = parameter.getValue();
                    }

                    compareAgainst = ParameterUtils.addParameterTypeSuffix(String.valueOf(compareAgainst),
                            parameterType);
                } else {
                    compareAgainst = ParameterUtils.makeParameterValueForPrimitiveType(parameter);
                }

            } else if (testGenerationState.getParameterNameFactory()
                    .getNameForUse(parameter, null) != null) {
                compareAgainst = testGenerationState.getParameterNameFactory()
                        .getNameForUse(parameter, null);
            } else {
                compareAgainst = parameter.getValue();
                if (parameter.isStringType()) {
                    if (parameter.getProb()
                            .getSerializedValue() != null &&
                            parameter.getProb()
                                    .getSerializedValue().length > 0) {
                        compareAgainst = new String(parameter.getProb()
                                .getSerializedValue());
                    } else if (parameter.getValue() == 0L) {
                        compareAgainst = "null";
                    }
                }
            }

            logger.warn("Argument [" + parameter + "] will be compared as => " + compareAgainst);

            if (compareAgainst != null && parameterType != null && parameterType.equals("java.lang.String")) {
                parameterStringBuilder
                        .append("eq(")
                        .append(compareAgainst)
                        .append(")");
            } else if (parameterType != null && compareAgainst != null
                    && (parameterType.length() == 1 || parameterType.startsWith("java.lang.")
                    && !parameterType.contains(".Object"))) {
                if (parameter.isBooleanType()) {
                    if (compareAgainst.equals("0")
                            || compareAgainst.equals(0L) // compare specifically to 0L, since comparing
                            // with 0 (int) sometimes (always) turn out to be false
                            || compareAgainst.equals(0)
                    ) {
                        compareAgainst = "false";
                    } else {
                        compareAgainst = "true";
                    }
                }
                parameterStringBuilder.append("eq(")
                        .append(compareAgainst)
                        .append(")");
            } else {
                if (parameter.getValue() == 0) {
                    parameterStringBuilder.append("any()");
                } else {
                    parameterStringBuilder.append("any(")
                            .append(ClassTypeUtils.getJavaClassName(parameterType))
                            .append(".class)");
                }
            }
        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
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
        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;

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
}
