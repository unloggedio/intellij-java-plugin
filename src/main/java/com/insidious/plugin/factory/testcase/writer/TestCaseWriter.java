package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ParameterUtils;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

public class TestCaseWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestCaseWriter.class);


    public static String
    createMethodParametersStringMock(List<Parameter> variableContainer, TestGenerationState testGenerationState) {
//        logger.warn("Create method parameters argument mock => " + variableContainer);
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
            } else if (parameterType != null && parameterType.equals("java.lang.Class")) {
                compareAgainst = new String(parameter.getProb().getSerializedValue());
            } else if (parameter.getProb() != null
                    && parameter.getProb().getSerializedValue().length > 0
                    && (new String(parameter.getProb().getSerializedValue())).equals("null")
            ) {

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

                    compareAgainst = ParameterUtils.addParameterTypeSuffix(String.valueOf(compareAgainst),
                            parameterType);
                } else {
                    compareAgainst = ParameterUtils.makeParameterValueForPrimitiveType(parameter);
                }

            } else if (testGenerationState.getParameterNameFactory().getNameForUse(parameter, null) != null) {
                compareAgainst = testGenerationState.getParameterNameFactory().getNameForUse(parameter, null);
            } else {
                compareAgainst = parameter.getValue();
                if (parameter.isStringType()) {
                    if (parameter.getProb().getSerializedValue() != null &&
                            parameter.getProb().getSerializedValue().length > 0) {
                        compareAgainst = new String(parameter.getProb().getSerializedValue());
                    } else if (parameter.getValue() == 0L) {
                        compareAgainst = "null";
                    }
                }
            }

//            logger.warn("Argument [" + parameter + "] will be compared as => " + compareAgainst);

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


        return parameterStringBuilder.toString();
    }


}
