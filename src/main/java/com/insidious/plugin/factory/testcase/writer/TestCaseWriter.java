package com.insidious.plugin.factory.testcase.writer;

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

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (parameter.getType() != null && parameter.getType()
                    .endsWith("[]")) {
                parameterStringBuilder.append("any()");
            } else if (parameter.isBooleanType()) {
                if (parameter.getValue() == 1) {
                    parameterStringBuilder.append("true");
                } else {
                    parameterStringBuilder.append("false");
                }
            } else if (parameter.isPrimitiveType()) {
                if (parameter.getProb() != null &&
                        parameter.getType()
                                .startsWith("java.lang") &&
                        parameter.getProb()
                                .getSerializedValue().length > 0) {
                    String serializedValue = new String(parameter.getProb()
                            .getSerializedValue());
                    parameterStringBuilder.append(serializedValue);
                } else {
                    parameterStringBuilder.append(parameter.getValue());
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

            Object compareAgainst = "";
            String parameterType = parameter.getType();
            if (parameterType != null && parameterType.endsWith("[]")) {
                compareAgainst = "";
            } else if (parameter.isPrimitiveType()) {
                compareAgainst = parameter.getValue();
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
                parameterStringBuilder.append("eq(" + compareAgainst + ")");
            } else if (parameterType != null && compareAgainst != null
                    && (parameterType.length() == 1 || parameterType.startsWith("java.lang.")
                    && !parameterType.contains(".Object"))
            ) {
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

                parameterStringBuilder.append("eq(" + compareAgainst + ")");
            } else {
                parameterStringBuilder.append("any(" +
                        ClassTypeUtils.getJavaClassName(parameterType)
                        + ".class)");
            }


        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
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
