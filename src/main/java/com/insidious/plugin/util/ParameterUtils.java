package com.insidious.plugin.util;

import com.insidious.plugin.constants.PrimitiveDataType;
import com.insidious.plugin.pojo.Parameter;

import java.util.List;

import static com.insidious.plugin.factory.testcase.util.ClassTypeUtils.createTypeFromTypeDeclaration;

public class ParameterUtils {
    public static String makeParameterValueForPrimitiveType(Parameter parameter) {
        StringBuilder valueBuilder = new StringBuilder();

        switch (parameter.getType()) {
            case PrimitiveDataType.BOXED_LONG:
            case PrimitiveDataType.LONG:
                valueBuilder.append(parameter.getValue());
                valueBuilder.append("L");
                break;
            case PrimitiveDataType.BOXED_DOUBLE:
            case PrimitiveDataType.DOUBLE:
                valueBuilder.append(Double.longBitsToDouble(parameter.getValue()));
                valueBuilder.append("D");
                break;
            case PrimitiveDataType.BOXED_FLOAT:
            case PrimitiveDataType.FLOAT:
                valueBuilder.append(Float.intBitsToFloat((int) parameter.getValue()));
                valueBuilder.append("F");
                break;
            case PrimitiveDataType.SHORT:
            case PrimitiveDataType.BOXED_SHORT:
                valueBuilder.append("(short) ");
                valueBuilder.append(parameter.getValue());
                break;
            default:
                valueBuilder.append(parameter.getValue());
        }
        return valueBuilder.toString();
    }

    public static String addParameterTypeSuffix(String valueString, String parameterType) {
        if (parameterType == null) {
            return valueString;
        }
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


    public static void createStatementStringForParameter(Parameter parameter, StringBuilder statementBuilder, List<Object> statementParameters) {
//        normaliseParameterType(parameter);

        statementBuilder.append("$T");

        String parameterType = parameter.getType();
        statementParameters.add(createTypeFromTypeDeclaration(parameterType));

        if (parameter.getTemplateMap().size() == 0) {
            return;
        }

        List<Parameter> templateMap = parameter.getTemplateMap();

        statementBuilder.append("<");
        for (int i = 0; i < templateMap.size(); i++) {
            Parameter paramInTemplate = templateMap.get(i);
            if (i > 0) {
                statementBuilder.append(", ");
            }
            createStatementStringForParameter(paramInTemplate, statementBuilder, statementParameters);
        }
        statementBuilder.append(">");
    }
}