package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ParameterUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import java.util.List;

public class TestCaseWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestCaseWriter.class);

    private static String psiTypeToJvmType(String canonicalText, boolean isReturnParameter) {
        if (canonicalText.endsWith("[]")) {
            canonicalText = psiTypeToJvmType(canonicalText.substring(0, canonicalText.length() - 2), isReturnParameter);
            return "[" + canonicalText;
        }
        switch (canonicalText) {
            case "void":
                canonicalText = "V";
                break;
            case "boolean":
                canonicalText = "Z";
                break;
            case "byte":
                canonicalText = "B";
                break;
            case "char":
                canonicalText = "C";
                break;
            case "short":
                canonicalText = "S";
                break;
            case "int":
                canonicalText = "I";
                break;
            case "long":
                canonicalText = "J";
                break;
            case "float":
                canonicalText = "F";
                break;
            case "double":
                canonicalText = "D";
                break;
            case "java.util.Map":
                if (!isReturnParameter) {
                    canonicalText = "java.util.HashMap";
                }
                break;
            case "java.util.List":
                if (!isReturnParameter) {
                    canonicalText = "java.util.ArrayList";
                }
                break;
            case "java.util.Set":
                if (!isReturnParameter) {
                    canonicalText = "java.util.HashSet";
                }
                break;
            case "java.util.Collection":
                if (!isReturnParameter) {
                    canonicalText = "java.util.ArrayList";
                }
                break;
            default:
        }
        return canonicalText;
    }

    static public void setParameterTypeFromPsiType(Parameter parameter, PsiType psiType, boolean isReturnParameter) {
        if (psiType instanceof PsiClassReferenceType) {
            PsiClassReferenceType returnClassType = (PsiClassReferenceType) psiType;
            if (returnClassType.getCanonicalText().equals(returnClassType.getName())) {
                logger.warn("return class type canonical text[" + returnClassType.getCanonicalText()
                        + "] is same as its name [" + returnClassType.getName() + "]");
                // this is a generic template type <T>, and not a real class
                parameter.setTypeForced("java.lang.Object");
                return;
            }
            parameter.setTypeForced(psiTypeToJvmType(returnClassType.rawType().getCanonicalText(), isReturnParameter));
            if (returnClassType.hasParameters()) {
                SessionInstance.extractTemplateMap(returnClassType, parameter.getTemplateMap());
                parameter.setContainer(true);
            }
        } else {
            parameter.setTypeForced(psiTypeToJvmType(psiType.getCanonicalText(), isReturnParameter));
        }
    }

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
