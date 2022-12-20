package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.constants.PrimitiveDataType;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.Parameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestCaseWriter {

    @NotNull
    public static String createMethodParametersString(List<Parameter> variableContainer) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);
            String paramType = parameter.getType();

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (paramType != null &&
                    paramType.endsWith("[]")) {
                // if the type of parameter is array like int[], long[] (i.e J[])
                if (parameter.getNameForUse(null) != null)
                    parameterStringBuilder.append(parameter.getNameForUse(null));
                else
                    parameterStringBuilder.append("any()");

            } else if (parameter.getProb() != null
                    && parameter.getProb().getSerializedValue().length > 0
                    && (new String(parameter.getProb().getSerializedValue())).equals("null")) {

                // if the serialized value is null just append null
                parameterStringBuilder.append("null");
            } else if (parameter.isBooleanType()) {
                if (parameter.getValue() == 1) {
                    parameterStringBuilder.append("true");
                } else {
                    parameterStringBuilder.append("false");
                }
            } else if (parameter.isPrimitiveType()) {
                if (parameter.isBoxedPrimitiveType() &&
                        parameter.getProb() != null && parameter.getProb().getSerializedValue().length > 0) {

                    String serializedValue = new String(parameter.getProb()
                            .getSerializedValue());
                    serializedValue = addParameterSuffix(serializedValue, paramType);

                    parameterStringBuilder.append(serializedValue);
                } else {
                    parameterStringBuilder.append(makeParameterValueForPrimitiveType(parameter));
                }
            } else if (parameter.getNameForUse(null) != null) {
                parameterStringBuilder.append(parameter.getNameForUse(null));
            } else {
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

        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }


    @NotNull
    public static String
    createMethodParametersStringMock(List<Parameter> variableContainer) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            Object compareAgainst = null;
            String parameterType = parameter.getType();
            if (parameterType != null && parameterType.endsWith("[]")) {
                compareAgainst = "";
            } else if (parameter.getProb() != null
                    && parameter.getProb().getSerializedValue().length > 0
                    && (new String(parameter.getProb().getSerializedValue())).equals("null")) {

                // if the serialized value is null just append null
                compareAgainst = "null";
            } else if (parameter.isPrimitiveType()) {
                if (parameter.isBoxedPrimitiveType()) {
                    String serialisedValue = new String(parameter.getProb().getSerializedValue());
                    if (serialisedValue.length() > 0) {
                        compareAgainst = serialisedValue;
                    } else {
                        compareAgainst = parameter.getValue();
                    }

                    compareAgainst = addParameterSuffix(String.valueOf(compareAgainst), parameterType);
                } else {
                    compareAgainst = makeParameterValueForPrimitiveType(parameter);
                }

            } else if (parameter.getNameForUse(null) != null) {
                compareAgainst = parameter.getNameForUse(null);
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

            if (compareAgainst != null && parameterType != null && parameterType.equals("java.lang.String")) {
                parameterStringBuilder
                        .append("eq(")
                        .append(compareAgainst)
                        .append(")");
            } else if (parameterType != null && compareAgainst != null
                    && (parameter.isPrimitiveType()
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

                parameterStringBuilder.append("eq(").append(compareAgainst).append(")");
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

    private static String makeParameterValueForPrimitiveType(Parameter parameter) {
        StringBuilder valueBuilder = new StringBuilder();

        switch (parameter.getType()) {
            case PrimitiveDataType.LONG:
                valueBuilder.append(parameter.getValue());
                valueBuilder.append("L");
                break;
            case PrimitiveDataType.DOUBLE:
                valueBuilder.append(Double.longBitsToDouble(parameter.getValue()));
                valueBuilder.append("D");
                break;
            case PrimitiveDataType.FLOAT:
                valueBuilder.append(Float.intBitsToFloat((int) parameter.getValue()));
                valueBuilder.append("F");
                break;
            default:
                valueBuilder.append(parameter.getValue());
        }
        return valueBuilder.toString();
    }

    private static String addParameterSuffix(String valueString, String parameterType) {
        switch (parameterType) {
            case PrimitiveDataType.BOXED_LONG:
                valueString += "L";
                break;
            case PrimitiveDataType.BOXED_DOUBLE:
                valueString += "D";
                break;
            case PrimitiveDataType.BOXED_FLOAT:
                valueString += "F";
                break;
        }
        return valueString;
    }


    /**
     * @param variableContainer list of parameters to be arranged
     * @return a string which is comma separated values to be passed to a method
     */
    public static String createMethodParametersStringWithNames(List<Parameter> variableContainer) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (parameter.getType() != null && parameter.getType()
                    .endsWith("[]")) {
                // if the type of parameter is array like int[], long[] (i.e J[])
                if (parameter.getNameForUse(null) != null)
                    parameterStringBuilder.append(parameter.getNameForUse(null));
                else
                    parameterStringBuilder.append("any()");
            } else if (parameter.getNameForUse(null) != null) {
                parameterStringBuilder.append(parameter.getNameForUse(null));
            } else if (parameter.isBooleanType()) {
                if (parameter.getValue() == 1 || parameter.getValue() == 1L) {
                    parameterStringBuilder.append("true");
                } else {
                    parameterStringBuilder.append("false");
                }
            } else {
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
        }
        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;

    }
}
