package com.insidious.plugin.util;

import com.insidious.plugin.constants.PrimitiveDataType;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ParameterUtils {
    public static String makeParameterValueForPrimitiveType(Parameter parameter) {
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

    public static String addParameterTypeSuffix(String valueString, String parameterType) {
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

    public static void normaliseParameterType(Parameter parameter) {
        if (!(parameter.getType().contains("<") && parameter.getType().contains(">")) && parameter.getTemplateMap().size() == 0) {
            return;
        }

        if (parameter.getType().contains("<") && parameter.getType().contains(">")) {
            // Map<Integer,List<List<String>>>
            int startOfTemplate = StringUtils.indexOf(parameter.getType(), "<");
            int endOfTemplate = StringUtils.lastIndexOf(parameter.getType(), ">");

            String paramType = parameter.getType().substring(0, startOfTemplate);

            String templateString = parameter.getType().substring(startOfTemplate + 1, endOfTemplate);
            String[] x = templateString.split(",");

            for (int i = 0; i < x.length; i++) {
                Parameter newParam = new Parameter();
                newParam.setType(x[i]);
                normaliseParameterType(newParam);
                parameter.getTemplateMap().add(newParam);
            }
            parameter.setTypeForced(paramType);
        } else {
            List<Parameter> templateList = parameter.getTemplateMap();
            for (int i = 0; i < templateList.size(); i++) {
                normaliseParameterType(templateList.get(i));
            }
        }

    }

    public static void denormalizeParameterType(Parameter parameter) {
        if (parameter.getTemplateMap().size() == 0) {
            return;
        }

        StringBuilder typeBuilder = new StringBuilder();

        typeBuilder.append(parameter.getType());
        typeBuilder.append("<");
        List<Parameter> templateList = parameter.getTemplateMap();

        for (int i = 0; i < templateList.size(); i++) {
            if (i > 0) {
                typeBuilder.append(", ");
            }

            denormalizeParameterType(templateList.get(i));
            typeBuilder.append(templateList.get(i).getType());
        }
        typeBuilder.append(">");

        parameter.setTypeForced(typeBuilder.toString());
    }

    public static void createStatementStringForParameter(Parameter parameter, StringBuilder statementBuilder, List<Object> statementParameters) {
        normaliseParameterType(parameter);

        statementBuilder.append("$T");
        statementParameters.add(ClassName.bestGuess(parameter.getType()));

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

    public static Parameter deepCloneType(Parameter parameter) {
        Parameter deepCloneParam = null;
        deepCloneParam = new Parameter();

        deepCloneParam.setValue(parameter.getValue());
        deepCloneParam.setType(parameter.getType());

        List<Parameter> deepCopyTemplateMap = new ArrayList<>();
        for (Parameter p : parameter.getTemplateMap()) {
            deepCopyTemplateMap.add(ParameterUtils.deepCloneType(p));
        }
        deepCloneParam.setTemplateMap(deepCopyTemplateMap);

        List<String> deepCopyNames = new ArrayList<>();
        for (String name : parameter.getNamesList()) {
            deepCopyNames.add(name);
        }
        deepCloneParam.setNamesList(deepCopyNames);

        return deepCloneParam;
    }
}