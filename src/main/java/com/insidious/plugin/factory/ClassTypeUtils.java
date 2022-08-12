package com.insidious.plugin.factory;

import com.insidious.common.weaver.DataInfo;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassTypeUtils {

    public static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    public static String lowerInstanceName(String methodName) {
        return methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
    }

    public static List<String> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException();
        }
        String x0;
        if (beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        } else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }
        Pattern pattern = Pattern.compile("\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
        Matcher matcher = pattern.matcher(x0);
        List<String> listMatches = new LinkedList<>();
        while (matcher.find()) {
            listMatches.add(matcher.group());
        }
        listMatches.add(desc.substring(endIndex + 1));
        return listMatches;
    }


    public static String createVariableName(String typeNameRaw) {
        String[] typeNameParts = typeNameRaw.split("/");
        String lastPart = typeNameParts[typeNameParts.length - 1];
        lastPart = lastPart.substring(0, 1).toLowerCase() + lastPart.substring(1,
                lastPart.length() - 1);
        return lastPart;
    }


    @NotNull
    public static String getBasicClassName(String className) {
        return "L" + className
                .replaceAll("\\.", "/") + ";";
    }

    @NotNull
    public static String getDottedClassName(String className) {
        if (!className.startsWith("L")) {
            return className;
        }

        String dottedName = className.substring(1,
                className.length() - 1).replace('/', '.');
        if (dottedName.contains("$")){
            dottedName = dottedName.substring(0, dottedName.indexOf("$"));
        }
        return dottedName;
    }


    public static String getDescriptorName(String className) {
        return "L" + className.split("\\$")[0] + ";";
    }

    public static String getVariableNameFromProbe(DataInfo historyEventProbe, String defaultValue) {
        return historyEventProbe.getAttribute(
                "Name", historyEventProbe.getAttribute(
                        "FieldName", defaultValue));
    }
}
