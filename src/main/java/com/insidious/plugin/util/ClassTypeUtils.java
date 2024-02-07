package com.insidious.plugin.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassTypeUtils {

    private static final JavaParser javaParser = new JavaParser(new ParserConfiguration());

    public static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1)
                .toUpperCase() + methodName.substring(1);
    }

    public static PsiType substituteClassRecursively(PsiType typeToBeSubstituted, PsiSubstitutor classSubstitutor) {
        if (classSubstitutor == null || typeToBeSubstituted instanceof PsiPrimitiveType) {
            return typeToBeSubstituted;
        }
        PsiType fieldTypeSubstitutor = classSubstitutor.substitute(typeToBeSubstituted);
        if (fieldTypeSubstitutor.getCanonicalText().equals(typeToBeSubstituted.getCanonicalText())) {
            PsiClassType[] checkSuperTypes = ((PsiClassReferenceType) typeToBeSubstituted).resolve()
                    .getExtendsListTypes();
            for (PsiClassType checkSuperType : checkSuperTypes) {
                PsiType possibleType = classSubstitutor.substitute(checkSuperType);
                if (!possibleType.getCanonicalText().equals(typeToBeSubstituted.getCanonicalText())) {
                    fieldTypeSubstitutor = possibleType;
                    break;
                }
            }

        }
        return fieldTypeSubstitutor;
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
    public static List<String> splitMethodDescriptor(String desc) {
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


    public static String getDescriptorName(String className) {
        if (className == null) {
            return "V";
        }
        if (className.length() < 2) {
            return className;
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
                typeName = typeName.replace("$", ".");
            }
            // for class array should be an ArrayTypeName instead of ClassName
            // though the string value is same but the
            // behaviour of the class imported is different
            // hence below if is required
            if (typeName.endsWith("[]")) {
                return ArrayTypeName.of(createTypeFromNameString(typeName.substring(0, typeName.length() - 2)));
            }
            try {
                returnValueSquareClass = ClassName.bestGuess(typeName);
            } catch (Exception exception) {
                // java poet failed to create class name from string
                String packageName = typeName.substring(0, typeName.lastIndexOf("."));
                String simpleName = typeName.substring(typeName.lastIndexOf(".") + 1);
                return ClassName.get(packageName, simpleName);
            }
            return returnValueSquareClass;
        }

        TypeName returnParamType = getTypeNameFromDescriptor(typeName);
        while (returnParamType != null && typeName.contains("[]")) {
            typeName = typeName.substring(typeName.indexOf("[]") + 2);
            returnParamType = ArrayTypeName.of(returnParamType);
        }

        return returnParamType;
    }

    public static TypeName createTypeFromTypeDeclaration(String templateParameterType) {
        TypeName parameterClassName;
        if (templateParameterType.contains("<")) {
            ParseResult<ClassOrInterfaceType> parsedJavaTypeDeclaration =
                    javaParser.parseClassOrInterfaceType(
                            templateParameterType);

            ClassOrInterfaceType parseResult = parsedJavaTypeDeclaration.getResult().get();
            String rawClassName = parseResult.getNameWithScope();
            NodeList<Type> typeArguments = parseResult.getTypeArguments().get();
            TypeName[] typeArgumentsArray = new TypeName[typeArguments.size()];
            for (int j = 0; j < typeArguments.size(); j++) {
                Type typeArgument = typeArguments.get(j);
                typeArgumentsArray[j] = createTypeFromTypeDeclaration(
                        typeArgument.asClassOrInterfaceType().getNameWithScope());
            }


            parameterClassName =
                    ParameterizedTypeName.get(ClassName.bestGuess(rawClassName), typeArgumentsArray);
        } else {
            parameterClassName = ClassName.bestGuess(templateParameterType);
        }
        return parameterClassName;
    }


    private static TypeName constructClassName(String typeName) {
        char firstChar = typeName.charAt(0);
        switch (firstChar) {
            case 'V':
                return TypeName.get(void.class);
            case 'Z':
                return TypeName.get(boolean.class);
            case 'B':
                return TypeName.get(byte.class);
            case 'C':
                return TypeName.get(char.class);
            case 'S':
                return TypeName.get(short.class);
            case 'I':
                return TypeName.get(int.class);
            case 'J':
                return TypeName.get(long.class);
            case 'F':
                return TypeName.get(float.class);
            case 'D':
                return TypeName.get(double.class);
            case 'L':
                String returnValueClass = typeName.substring(1)
                        .split(";")[0];
                return ClassName.bestGuess(returnValueClass.replace("/", "."));
            case '[':
//                String returnValueClass1 = methodReturnValueType.substring(1);
                return ArrayTypeName.of(constructClassName(typeName.substring(1)));
            default:
                return ClassName.bestGuess(typeName);

        }
//        assert false;
//        return null;
    }

    private static TypeName getTypeNameFromDescriptor(String descriptor) {
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
        if (descriptor.startsWith("byte")) {
            return TypeName.BYTE;
        }
        if (descriptor.startsWith("boolean")) {
            return TypeName.BOOLEAN;
        }
        if (descriptor.startsWith("long")) {
            return TypeName.LONG;
        }
        if (descriptor.startsWith("float")) {
            return TypeName.FLOAT;
        }
        if (descriptor.startsWith("short")) {
            return TypeName.SHORT;
        }
        if (descriptor.startsWith("int")) {
            return TypeName.INT;
        }
        if (descriptor.startsWith("double")) {
            return TypeName.DOUBLE;
        }
        if (descriptor.startsWith("void")) {
            return TypeName.VOID;
        }
        if (descriptor.startsWith("char")) {
            return TypeName.CHAR;
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

    public static PsiMethod getPsiMethod(MethodCallExpression methodCallExpression, Project project) {
        MethodCallExpression mainMethod = methodCallExpression;
        PsiClass classPsiElement = JavaPsiFacade
                .getInstance(project)
                .findClass(mainMethod.getSubject().getType(),
                        GlobalSearchScope.allScope(project));

        String methodName = mainMethod.getMethodName();
        boolean isLambda = false;
        if (methodName.startsWith("lambda$")) {
            methodName = methodName.split("\\$")[1];
            isLambda = true;
        }
        JvmMethod[] methodsByName = classPsiElement.findMethodsByName(methodName, true);

        if (methodsByName.length == 1 && isLambda) {
            // should we verify parameters ?
            return (PsiMethod) methodsByName[0].getSourceElement();
        }

        for (JvmMethod jvmMethod : methodsByName) {

            List<Parameter> expectedArguments = mainMethod.getArguments();
            JvmParameter[] actualArguments = jvmMethod.getParameters();

            if (expectedArguments.size() == actualArguments.length) {

                boolean mismatch = false;
                for (int i = 0; i < expectedArguments.size(); i++) {
                    Parameter expectedArgument = expectedArguments.get(i);
                    JvmParameter actualArgument = actualArguments[i];
                    JvmType type = actualArgument.getType();
                    if (type instanceof PsiType) {
                        if (!((PsiType) type).getCanonicalText().contains(expectedArgument.getType())) {

                            PsiClass expectedClassPsi = JavaPsiFacade.getInstance(project)
                                    .findClass(expectedArgument.getType(), GlobalSearchScope.allScope(project));

                            if (expectedClassPsi != null) {
                                if (type instanceof PsiClassReferenceType) {
                                    boolean ok = InheritanceImplUtil.isInheritor(
                                            ((PsiClassReferenceType) type).resolve(),
                                            expectedClassPsi, true);
                                    if (ok) {
                                        return (PsiMethod) jvmMethod.getSourceElement();
                                    }
                                } else if (type instanceof PsiClass) {
                                    boolean ok = InheritanceImplUtil.isInheritor(((PsiClass) type),
                                            expectedClassPsi, true);
                                    if (ok) {
                                        return (PsiMethod) jvmMethod.getSourceElement();
                                    }
                                }

                            }


                            mismatch = true;
                            break;
                        }
                    }

                }
                if (mismatch) {
                    continue;
                }

                return (PsiMethod) jvmMethod.getSourceElement();


            }

        }
        return null;
    }
}
