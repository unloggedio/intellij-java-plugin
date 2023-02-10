package com.insidious.plugin.factory.testcase.util;

import com.insidious.common.weaver.DataInfo;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassTypeUtils {

    public static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1)
                .toUpperCase() + methodName.substring(1);
    }

    public static String lowerInstanceName(String methodName) {
        return methodName.substring(0, 1)
                .toLowerCase() + methodName.substring(1);
    }

    /**
     * parses a method descriptor string and return in a list form where each item is the type
     *
     * @param desc method descriptor string
     * @return a list of strings, last item in the list is the return parameter, and 0 to n-1 items are method arguments
     */
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
        Pattern pattern = Pattern.compile(
                "\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
        Matcher matcher = pattern.matcher(x0);
        List<String> listMatches = new LinkedList<>();
        while (matcher.find()) {
            listMatches.add(matcher.group());
        }
        listMatches.add(desc.substring(endIndex + 1));
        return listMatches;
    }


    public static String createVariableName(String typeNameRaw) {
        if (typeNameRaw == null) {
            return null;
        }
        String lastPart = ClassTypeUtils.getDottedClassName(typeNameRaw);
        lastPart = lastPart.substring(lastPart.lastIndexOf(".") + 1);
        if (lastPart.length() < 2) {
            return lastPart.toLowerCase();
        }
        if (lastPart.contains("$")) {
            lastPart = lastPart.replace('$', 'S');
        }
        lastPart = lastPart.substring(0, 1)
                .toLowerCase() + lastPart.substring(1);
        if (lastPart.equals("int")
                || lastPart.equals("long")
                || lastPart.equals("byte")
                || lastPart.equals("float")
                || lastPart.equals("class")
                || lastPart.equals("enum")
                || lastPart.equals("double")
                || lastPart.equals("short")) {
            return lastPart + "Value";
        } else {
            return lastPart;
        }
    }


    @NotNull
    public static String getDescriptorName(String className) {
        if (className.contains("$")) {
            className = className.substring(0, className.indexOf('$'));
        }
        return "L" + className.replace('.', '/') + ";";
    }

    public static String getDottedClassName(String className) {
        if (className == null) {
            return null;
        }
        if (className.contains(".")) {
            return className;
        }

        if (className.endsWith(";")) {
            className = className.substring(0, className.length() - 1);
        }

        while (className.startsWith("[")) {
            className = className.substring(1) + "[]";
        }
        if (className.startsWith("L")) {
            className = className.substring(1);
        }

        String dottedName = className.replace('/', '.');
        if (dottedName.contains("$$")) {
            dottedName = dottedName.substring(0, dottedName.indexOf("$$"));
        }
        return dottedName;
    }

    public static String getJavaClassName(String className) {
        if (className == null) {
            return null;
        }
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        if (className.contains(".") && !className.contains("/")) {
            className = className.replace('$', '.');
        } else {
            className = className.replace('$', '.');

            if (className.endsWith(";")) {
                className = className.substring(0, className.length() - 1);
            }

            while (className.startsWith("[")) {
                className = className.substring(1) + "[]";
            }
            if (className.startsWith("L")) {
                className = className.substring(1);
            }
            className = className.replace('/', '.');
        }

        if (className.matches(".+\\.[0-9]+$")) {

            // if the class name is like a `ClassName.1`
            className = className.substring(0, className.lastIndexOf("."));
        }

        return className;
    }


    /**
     * Find the ClassName or TypeName or ArrayType Name From provided string
     * Checks for primitive array type also
     *
     * @param typeName name of the class for which a variable name is to be created
     * @return String
     */
    public static TypeName createTypeFromNameString(String typeName) {
        TypeName returnValueSquareClass = null;
        if (typeName.startsWith("L") || typeName.startsWith("[")) {
            returnValueSquareClass = constructClassName(typeName);
            return returnValueSquareClass;
        }

        if (typeName.contains(".")) {
            if (typeName.contains("$")) {
                typeName = typeName.substring(0, typeName.indexOf("$"));
            }
            // for class array should be an ArrayTypeName instead of ClassName
            // though the string value is same but the
            // behaviour of the class imported is different
            // hence below if is required
            if (typeName.endsWith("[]")) {
                return ArrayTypeName.of(createTypeFromNameString(typeName.substring(0, typeName.length() - 2)));
            }
            returnValueSquareClass = ClassName.bestGuess(typeName);
            return returnValueSquareClass;
        }

        TypeName returnParamType = getClassFromDescriptor(typeName);
        if (returnParamType != null && typeName.endsWith("[]")) {
            return ArrayTypeName.of(returnParamType);
        }

        return returnParamType;
    }

    private static TypeName constructClassName(String methodReturnValueType) {
        char firstChar = methodReturnValueType.charAt(0);
        switch (firstChar) {
            case 'V':
                return ClassName.get(void.class);
            case 'Z':
                return ClassName.get(boolean.class);
            case 'B':
                return ClassName.get(byte.class);
            case 'C':
                return ClassName.get(char.class);
            case 'S':
                return ClassName.get(short.class);
            case 'I':
                return ClassName.get(int.class);
            case 'J':
                return ClassName.get(long.class);
            case 'F':
                return ClassName.get(float.class);
            case 'D':
                return ClassName.get(double.class);
            case 'L':
                String returnValueClass = methodReturnValueType.substring(1)
                        .split(";")[0];
                return ClassName.bestGuess(returnValueClass.replace("/", "."));
            case '[':
//                String returnValueClass1 = methodReturnValueType.substring(1);
                return ArrayTypeName.of(constructClassName(methodReturnValueType.substring(1)));

            default:
                assert false;

        }
//        assert false;
        return null;
    }

    private static TypeName getClassFromDescriptor(String descriptor) {
        char firstChar = descriptor.charAt(0);
        switch (firstChar) {
            case 'V':
                return TypeName.VOID;
            case 'Z':
                return TypeName.BOOLEAN;
            case 'B':
                return TypeName.BYTE;
            case 'C':
                return TypeName.CHAR;
            case 'S':
                return TypeName.SHORT;
            case 'I':
                return TypeName.INT;
            case 'J':
                return TypeName.LONG;
            case 'F':
                return TypeName.FLOAT;
            case 'D':
                return TypeName.DOUBLE;
        }
        return null;

    }


    public static String createVariableNameFromMethodName(String targetMethodName, String className) {

        String potentialReturnValueName = targetMethodName + "Result";

        if (targetMethodName.startsWith("get") || targetMethodName.startsWith("set")) {
            if (targetMethodName.length() > 3) {
                potentialReturnValueName = lowerInstanceName(targetMethodName.substring(3));
            } else {
                potentialReturnValueName = null;
            }
        } else {
            if (targetMethodName.equals("<init>")) {
                potentialReturnValueName = createVariableName(className);
            } else {
                potentialReturnValueName = targetMethodName + "Result";
            }
        }
        return potentialReturnValueName;

    }

}
