package com.insidious.plugin.util;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.java.JavaParameterAdapter;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.methodscope.ClassChosenListener;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.psi.PsiKeyword.ABSTRACT;

public class ClassUtils {

    private static final Logger logger = LoggerUtil.getInstance(ClassUtils.class);
    private static final Map<String, Boolean> classNotFound = new HashMap<>();

    public static DeclaredMock createDefaultMock(PsiMethodCallExpression methodCallExpression) {


        PsiMethod destinationMethod = methodCallExpression.resolveMethod();

        MethodUnderTest mut = MethodUnderTest.fromPsiCallExpression(methodCallExpression);

        PsiType returnType = identifyReturnType(methodCallExpression);
        String returnDummyValue;
        String methodReturnTypeName;
        if (returnType != null) {
            returnDummyValue = ClassUtils.createDummyValue(returnType, new ArrayList<>(),
                    destinationMethod.getProject());
            methodReturnTypeName = buildJvmClassName(returnType);
        } else {
            methodReturnTypeName = "java.lang.Object";
            returnDummyValue = "{}";
        }

        PsiClass parentClass = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        if (parentClass == null) {
            InsidiousNotification.notifyMessage("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]", NotificationType.ERROR);
            throw new RuntimeException("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]");
        }
        String expressionText = methodCallExpression.getMethodExpression().getText();

        PsiType[] methodParameterTypes = methodCallExpression.getArgumentList().getExpressionTypes();
        JvmParameter[] jvmParameters = destinationMethod.getParameters();
        List<ParameterMatcher> parameterList = new ArrayList<>();

        PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        PsiClass containingClass = destinationMethod.getContainingClass();
        PsiSubstitutor classSubstitutor = null;
        if (containingClass != null && parentOfType != null) {
            classSubstitutor = TypeConversionUtil
                    .getClassSubstitutor(containingClass, parentOfType, PsiSubstitutor.EMPTY);
        }

        for (int i = 0; i < methodParameterTypes.length; i++) {
            JavaParameterAdapter param = new JavaParameterAdapter(jvmParameters[i]);
            PsiType parameterType = methodParameterTypes[i];

            if (classSubstitutor != null) {
                parameterType = classSubstitutor.substitute(parameterType);
            }

            String parameterTypeName = parameterType.getCanonicalText();
            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                parameterTypeName = classReferenceType.rawType().getCanonicalText();
            }
            ParameterMatcher parameterMatcher = new ParameterMatcher(param.getName(),
                    ParameterMatcherType.ANY_OF_TYPE, parameterTypeName);
            parameterList.add(parameterMatcher);
        }


        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();
        thenParameterList.add(createDummyThenParameter(returnDummyValue, methodReturnTypeName));
        PsiElement callerQualifier = methodCallExpression.getMethodExpression().getQualifier();
        String fieldName = callerQualifier.getText();
        PsiElement[] callerQualifierChildren = callerQualifier.getChildren();
        if (callerQualifierChildren.length > 1) {
            fieldName = callerQualifierChildren[callerQualifierChildren.length - 1].getText();
        }
        return new DeclaredMock(
                "mock response " + expressionText, mut.getClassName(), parentClass.getQualifiedName(),
                fieldName, mut.getName(), mut.getMethodHashKey(), parameterList, thenParameterList
        );
    }

    public static ThenParameter createDummyThenParameter(String returnDummyValue1, String methodReturnTypeName1) {
        ReturnValue returnValue = new ReturnValue(returnDummyValue1, methodReturnTypeName1, ReturnValueType.REAL);
        return new ThenParameter(returnValue, MethodExitType.NORMAL);
    }

    public static String createDummyValue(
            PsiType parameterType,
            List<String> creationStack,
            Project project
    ) {
        if (parameterType == null) {
            return "null";
        }
        String parameterTypeCanonicalText =
                ApplicationManager.getApplication()
                        .runReadAction((Computable<String>) parameterType::getCanonicalText);
        if (creationStack.contains(parameterTypeCanonicalText)) {
            return "null";
        }

        try {
            creationStack.add(parameterTypeCanonicalText);
            StringBuilder dummyValue = new StringBuilder();

            if (parameterType instanceof PsiArrayType) {
                PsiArrayType arrayType = (PsiArrayType) parameterType;
                dummyValue.append("[");
//                PsiType psiType =
//                        ApplicationManager.getApplication()
//                                .runReadAction((Computable<PsiType>) arrayType::getComponentType);
                dummyValue.append(createDummyValue(arrayType.getComponentType(), creationStack, project));
                dummyValue.append("]");
                return dummyValue.toString();
            }

            if (parameterTypeCanonicalText.equals("java.lang.String")) {
                return "\"string\"";
            }
            if (parameterTypeCanonicalText.equals("java.lang.Boolean")) {
                return "\"true\"";
            }
            if (parameterTypeCanonicalText.startsWith("java.lang.")) {
                return "\"0\"";
            }

            if (parameterTypeCanonicalText.equals("java.util.Random")) {
                return "{}";
            }
            if (parameterTypeCanonicalText.equals("java.util.Date")) {
                return String.valueOf(new Date().getTime());
            }
            if (parameterTypeCanonicalText.equals("java.sql.Timestamp")) {
                return String.valueOf(new Date().getTime());
            }
            if (parameterTypeCanonicalText.equals("java.time.Instant")) {
//                Date date = new Date();
                return String.valueOf(new Date().getTime() / 1000);
            }
            if (parameterTypeCanonicalText.equals("org.joda.time.Instant")) {
                return String.valueOf(new Date().getTime());
            }
            if (parameterTypeCanonicalText.equals("org.joda.time.DateTime")) {
                return String.valueOf(new Date().getTime());
            }
            if (parameterTypeCanonicalText.equals("org.springframework.security.core.GrantedAuthority")) {
                // SimpleGrantedAuthority
                return "\"USER\"";
            }

            if (parameterType instanceof PsiClassType) {
                PsiClassType classReferenceType = (PsiClassType) parameterType;
                PsiClassType psiClassRawType =
                        ApplicationManager.getApplication()
                                .runReadAction((Computable<PsiClassType>) classReferenceType::rawType);

                String rawTypeCanonicalText =
                        ApplicationManager.getApplication()
                                .runReadAction((Computable<String>) psiClassRawType::getCanonicalText);
                if (
                        rawTypeCanonicalText.equals("java.util.List") ||
                                rawTypeCanonicalText.equals("java.util.ArrayList") ||
                                rawTypeCanonicalText.equals("java.util.LinkedList") ||
                                rawTypeCanonicalText.equals("java.util.TreeSet") ||
                                rawTypeCanonicalText.equals("java.util.SortedSet") ||
                                rawTypeCanonicalText.equals("java.util.Set")
                ) {
                    dummyValue.append("[");
                    PsiType type =
                            ApplicationManager.getApplication().runReadAction((Computable<PsiType>) () ->
                                    classReferenceType.getParameters().length > 0 ?
                                            classReferenceType.getParameters()[0] : PsiType.getTypeByName("java.lang" +
                                            ".Object", project, GlobalSearchScope.allScope(project)));
                    dummyValue.append(createDummyValue(type, creationStack, project));
                    dummyValue.append("]");
                    return dummyValue.toString();
                }
                if (
                        rawTypeCanonicalText.equals("net.minidev.json.JSONObject") ||
                                rawTypeCanonicalText.equals("com.google.gson.JsonObject") ||
                                rawTypeCanonicalText.equals("com.fasterxml.jackson.databind.JsonNode")
                ) {
                    return "{}";
                }

                if (rawTypeCanonicalText.equals("java.util.Map") ||
                        // either from apache-collections or from spring
                        psiClassRawType.getName().endsWith("MultiValueMap") ||
                        rawTypeCanonicalText.equals("java.util.Map.Entry")
                ) {
                    if (classReferenceType.getParameters().length == 2) {
                        dummyValue.append("{");
                        // key for a map is always string in json
                        // objectMapper cannot probably reconstruct this back
                        //
                        if (classReferenceType.getParameters()[0].toString().equals("PsiType:Integer")) {
                            dummyValue.append("\"0\"");
                        } else {
                            dummyValue.append("\"keyFromClass" + classReferenceType.getName() + "\"");
                        }
                        dummyValue.append(": ");
                        dummyValue.append(
                                createDummyValue(classReferenceType.getParameters()[1], creationStack, project));
                        dummyValue.append("}");
                        return dummyValue.toString();
                    }
                }

                if (rawTypeCanonicalText.equals("java.util.UUID")) {
                    dummyValue.append("\"");
                    dummyValue.append(UUID.randomUUID());
                    dummyValue.append("\"");
                    return dummyValue.toString();
                }

                if (rawTypeCanonicalText.equals("reactor.core.publisher.Flux")) {
                    dummyValue.append("[");
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
                    dummyValue.append("]");
                    return dummyValue.toString();
                }

                if (rawTypeCanonicalText.equals("reactor.core.publisher.Mono")) {
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
                    return dummyValue.toString();
                }

                if (rawTypeCanonicalText.equals("java.util.Optional")) {
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
                    return dummyValue.toString();
                }

                PsiClass resolvedClass =
                        ApplicationManager.getApplication().runReadAction((Computable<PsiClass>) () ->
                                JavaPsiFacade.getInstance(project)
                                        .findClass(classReferenceType.getCanonicalText(),
                                                GlobalSearchScope.allScope(project)));

                if (resolvedClass == null) {
                    // class not resolved
                    // lets hope it's just an object
                    return "{}";
                }

                if (resolvedClass.isEnum()) {
                    PsiField[] enumValues = resolvedClass.getAllFields();
                    if (enumValues.length == 0) {
                        return "";
                    }
                    return "\"" + enumValues[0].getName() + "\"";
                }

                PsiField[] parameterObjectFieldList =
                        ApplicationManager.getApplication()
                                .runReadAction((Computable<PsiField[]>) () -> resolvedClass.getAllFields());

                StringBuilder dummyInternalObjValue = new StringBuilder();
                if (creationStack.size() < 10) {
                    boolean firstField = true;
                    for (PsiField psiField : parameterObjectFieldList) {
                        String name =
                                ApplicationManager.getApplication()
                                        .runReadAction((Computable<String>) () -> psiField.getName());
                        if (name.equals("serialVersionUID")) {
                            continue;
                        }
                        boolean hasModifier =
                                ApplicationManager.getApplication().runReadAction(
                                        (Computable<Boolean>) () -> psiField.hasModifier(JvmModifier.STATIC));
                        if (hasModifier) {
                            continue;
                        }
                        if (!firstField) {
                            dummyInternalObjValue.append(", ");
                        }

                        dummyInternalObjValue.append("\"");
                        String fieldName =
                                ApplicationManager.getApplication()
                                        .runReadAction((Computable<String>) () -> psiField.getName());
                        dummyInternalObjValue.append(fieldName);
                        dummyInternalObjValue.append("\"");
                        dummyInternalObjValue.append(": ");
                        PsiType type =
                                ApplicationManager.getApplication()
                                        .runReadAction((Computable<PsiType>) () -> psiField.getType());
                        dummyInternalObjValue.append(createDummyValue(type, creationStack, project));
                        firstField = false;
                    }
                }

                // create dummyValue based on dummyInternalObjValue
                if (dummyInternalObjValue.length() == 0) {
                    dummyValue.append("null");
                } else {
                    dummyValue.append("{");
                    dummyValue.append(dummyInternalObjValue);
                    dummyValue.append("}");
                }

            } else if (parameterType instanceof PsiPrimitiveType) {
                PsiPrimitiveType primitiveType = (PsiPrimitiveType) parameterType;
                if ("boolean".equals(primitiveType.getName())) {
                    return "true";
                }
                return "\"0\"";
            }
            return dummyValue.toString();

        } finally {
            creationStack.remove(parameterTypeCanonicalText);
        }

    }

    public static void chooseClassImplementation(ClassAdapter psiClass, boolean showUI, ClassChosenListener classChosenListener) {
        ImplementationSearcher implementationSearcher = new ImplementationSearcher();
        PsiElement element =
                ApplicationManager.getApplication().runReadAction((Computable<PsiElement>) () -> psiClass.getSource());
//        PsiElement[] implementations = ApplicationManager.getApplication().runReadAction((Computable<PsiElement[]>) () -> implementationSearcher.searchImplementations(
//                psiElement, null, true, false));

        PsiElement[] implementations = implementationSearcher.searchImplementations(
                element, null, true, false
        );
        if (implementations == null || implementations.length == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                    NotificationType.ERROR);
            return;
        }
        if (implementations.length == 1) {
            PsiClass singleImplementation = (PsiClass) implementations[0];
            boolean isInterface =
                    ApplicationManager.getApplication()
                            .runReadAction((Computable<Boolean>) () -> singleImplementation.isInterface());
            boolean hasModifiedProperty =
                    ApplicationManager.getApplication().runReadAction(
                            (Computable<Boolean>) () -> singleImplementation.hasModifierProperty(ABSTRACT));
            if (isInterface || hasModifiedProperty) {
                InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                        NotificationType.ERROR);
                return;
            }
            ClassUnderTest classUnderTest =
                    ApplicationManager.getApplication().runReadAction(
                            (Computable<ClassUnderTest>) () -> new ClassUnderTest(
                                    JvmClassUtil.getJvmClassName(singleImplementation)));
            classChosenListener.classSelected(classUnderTest);
            return;
        }

//        List<PsiClass> implementationOptions = Arrays.stream(implementations)
//                .map(e -> (PsiClass) e)
//                .filter(e -> !e.isInterface())
//                .filter(e -> !e.hasModifierProperty(ABSTRACT))
//                .collect(Collectors.toList());

        List<PsiClass> implementationOptions = Arrays.stream(implementations)
                .map(e -> (PsiClass) e)
                .filter(e -> !ApplicationManager.getApplication()
                        .runReadAction((Computable<Boolean>) () -> e.isInterface()))
                .filter(e -> !ApplicationManager.getApplication()
                        .runReadAction((Computable<Boolean>) () -> e.hasModifierProperty(ABSTRACT)))
                .collect(Collectors.toList());

        if (implementationOptions.size() == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                    NotificationType.ERROR);
            return;
        }
        if (implementationOptions.size() == 1) {
            ClassUnderTest classUnderTest =
                    ApplicationManager.getApplication().runReadAction(
                            (Computable<ClassUnderTest>) () -> new ClassUnderTest(
                                    JvmClassUtil.getJvmClassName(implementationOptions.get(0))));
            classChosenListener.classSelected(classUnderTest);
            return;
        }
        if (showUI) {
            JBPopup implementationChooserPopup = JBPopupFactory
                    .getInstance()
                    .createPopupChooserBuilder(implementationOptions.stream()
                            .map(PsiClass::getQualifiedName)
                            .sorted()
                            .collect(Collectors.toList()))
                    .setTitle("Run using implementation for " + psiClass.getName())
                    .setItemChosenCallback(psiElementName -> {
                        Arrays.stream(implementations)
                                .filter(e -> Objects.equals(((PsiClass) e).getQualifiedName(), psiElementName))
                                .findFirst().ifPresent(e -> {
                                    classChosenListener.classSelected(
                                            new ClassUnderTest(JvmClassUtil.getJvmClassName((PsiClass) e)));
                                });
                    })
                    .createPopup();
            implementationChooserPopup.showInFocusCenter();
        } else {
            ClassUnderTest classUnderTest =
                    ApplicationManager.getApplication().runReadAction(
                            (Computable<ClassUnderTest>) () -> new ClassUnderTest(
                                    JvmClassUtil.getJvmClassName(implementationOptions.get(0))));
            classChosenListener.classSelected(classUnderTest);
        }

    }

    public static String getSimpleName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }

    public static void resolveTemplatesInCall(MethodCallExpression methodCallExpression, Project project) {
        Parameter callSubject = methodCallExpression.getSubject();
        String subjectType = callSubject.getType();
        if (subjectType.length() == 1) {
            return;
        }
        if (classNotFound.containsKey(subjectType)) {
            return;
        }

        PsiClass classPsiInstance = null;

        try {
            classPsiInstance = JavaPsiFacade.getInstance(project)
                    .findClass(ClassTypeUtils.getJavaClassName(subjectType), GlobalSearchScope.allScope(project));
        } catch (IndexNotReadyException e) {
//            e.printStackTrace();
            InsidiousNotification.notifyMessage("Test Generation can start only after indexing is complete!",
                    NotificationType.ERROR);
        }

        if (classPsiInstance == null) {
            // if a class by this name was not found, then either we have a different project loaded
            // or the source code has been modified and the class have been renamed or deleted or moved
            // cant do much here
            classNotFound.put(subjectType, true);
            logger.warn("Class not found in source code for resolving generic template: " + subjectType);
            return;
        }

        String methodName = methodCallExpression.getMethodName();
        if (methodName.startsWith("lambda$")) {
            methodName = methodName.split("\\$")[1];
        }

        JvmMethod[] methodPsiInstanceList =
                classPsiInstance.findMethodsByName(methodName, true);
        if (methodPsiInstanceList.length == 0) {
            logger.warn(
                    "[2] did not find a matching method in source code: " + subjectType + "." + methodName);
            return;
        }

        List<Parameter> methodArguments = methodCallExpression.getArguments();
        int expectedArgumentCount = methodArguments
                .size();
        for (JvmMethod jvmMethod : methodPsiInstanceList) {

            int parameterCount = jvmMethod.getParameters().length;
            if (expectedArgumentCount != parameterCount) {
                // this is not the method which we are looking for
                // either this has been updated in the source code and so we wont find a matching method
                // or this is an overridden method
                continue;
            }

            JvmParameter[] parameters = jvmMethod.getParameters();
            // to resolve generics
            for (int i = 0; i < parameters.length; i++) {
                JvmParameter parameterFromSourceCode = parameters[i];
                Parameter parameterFromProbe = methodArguments.get(i);
                JvmType typeFromSourceCode = parameterFromSourceCode.getType();
                if (typeFromSourceCode instanceof PsiPrimitiveType) {
                    PsiPrimitiveType primitiveType = (PsiPrimitiveType) typeFromSourceCode;
                } else if (typeFromSourceCode instanceof PsiClassReferenceType) {
                    PsiClassReferenceType classReferenceType = (PsiClassReferenceType) typeFromSourceCode;
                    if (parameterFromProbe.getType() == null) {
                        parameterFromProbe.setType(classReferenceType.getReference()
                                .getQualifiedName());

                    } else if (!classReferenceType.getReference()
                            .getQualifiedName()
                            .startsWith(parameterFromProbe.getType())) {
                        logger.warn(
                                "Call expected argument [" + i + "] [" + parameterFromProbe.getType() + "] did not " +
                                        "match return type in  source: [" + classReferenceType.getCanonicalText()
                                        + "] for call: " + methodCallExpression);

                        break;
                    }

                    if (classReferenceType.hasParameters()) {
                        parameterFromProbe.setContainer(true);
                        List<Parameter> templateMap = parameterFromProbe.getTemplateMap();
                        extractTemplateMap(classReferenceType, templateMap);
                    }
                }
            }

            Parameter returnParameter = methodCallExpression.getReturnValue();

            JvmType returnParameterType = jvmMethod.getReturnType();
            if (returnParameterType instanceof PsiPrimitiveType) {
                PsiPrimitiveType primitiveType = (PsiPrimitiveType) returnParameterType;
            } else if (returnParameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType returnTypeClassReference = (PsiClassReferenceType) returnParameterType;
                if (returnParameter != null &&
                        returnParameter.getType() != null) {
                    if (!returnTypeClassReference.getReference()
                            .getQualifiedName()
                            .equals(returnParameter.getType())) {
                        // type name mismatch
                        logger.warn(
                                "Call expected return [" + returnParameter.getType() + "] did not match return type in " +
                                        "source: [" + returnTypeClassReference.getCanonicalText()
                                        + "] for call: " + methodCallExpression);
                        continue;
                    } else {
                        // type matched, we can go ahead to identify template parameters
                    }
                }

                if (returnTypeClassReference.hasParameters()) {
                    PsiType[] typeTemplateParameters = returnTypeClassReference.getParameters();
                    returnParameter.setContainer(true);
                    List<Parameter> templateMap = returnParameter.getTemplateMap();
                    char templateChar = 'D';
                    boolean hasGenericTemplate = false;
                    for (PsiType typeTemplateParameter : typeTemplateParameters) {
                        templateChar++;
                        Parameter value = new Parameter();
                        String canonicalText = typeTemplateParameter.getCanonicalText();
                        // <? super ClassName>
                        if (canonicalText.contains(" super ")) {
                            canonicalText = canonicalText.substring(
                                    canonicalText.indexOf(" super ") + " super ".length());
                        }

                        // <? extends ClassName>
                        if (canonicalText.contains(" extends ")) {
                            canonicalText = "?";
                            // todo: for ? extends ClassName, identify what typename can we use in the generated test
                            //  case (its not ClassName)
//                            canonicalText = canonicalText.substring(
//                                    canonicalText.indexOf(" extends ") + " extends ".length());
                        }

                        if (canonicalText.length() == 1) {
                            hasGenericTemplate = true;
                            break;
                        }
                        value.setType(canonicalText);
                        String templateKey = String.valueOf(templateChar);
                        value.setName(templateKey);
                        Optional<Parameter> exitingTemplateOptional = templateMap.stream()
                                .filter(e -> e.getName()
                                        .equals(templateKey))
                                .findFirst();
                        if (exitingTemplateOptional.isPresent()) {
                            Parameter existingValue = exitingTemplateOptional.get();
                            if (existingValue.getType()
                                    .length() < 2) {
                                templateMap.remove(exitingTemplateOptional.get());
                                templateMap.add(value);
                            }
                        } else {
                            templateMap.add(value);
                        }
                    }
                    if (hasGenericTemplate) {
                        templateMap.clear();
                    }
                }
            }
        }
    }

    public static void
    extractTemplateMap(PsiClassType classReferenceType, List<Parameter> templateMap) {
        char templateChar = 'D';
        boolean hasGenericTemplate = false;
        PsiType[] typeTemplateParameters = classReferenceType.getParameters();
        for (PsiType typeTemplateParameter : typeTemplateParameters) {
            templateChar++;
            Parameter value = new Parameter();
            String canonicalText = typeTemplateParameter.getCanonicalText();
            // <? super ClassName>
            if (canonicalText.contains(" super ")) {
                canonicalText = canonicalText.substring(
                        canonicalText.indexOf(" super ") + " super ".length());
            }

            // <? extends ClassName>
            if (canonicalText.contains(" extends ")) {
                canonicalText = canonicalText.substring(
                        canonicalText.indexOf(" extends ") + " extends ".length());
            }
            if (canonicalText.length() == 1) {
                hasGenericTemplate = true;
                break;
            }
//            logger.warn("Setting template type for parameter[" + templateChar + "]: "
//                    + canonicalText + " for parameter: [" + classReferenceType);
            value.setType(canonicalText);
            String templateKey = String.valueOf(templateChar);
            value.setName(templateKey);
            Optional<Parameter> existingTemplateOptional = templateMap.stream()
                    .filter(e -> e.getName()
                            .equals(templateKey))
                    .findFirst();
            if (existingTemplateOptional.isPresent()) {
                Parameter existingValue = existingTemplateOptional.get();
                if (existingValue.getType()
                        .length() < 2) {
                    templateMap.remove(existingTemplateOptional.get());
                    templateMap.add(value);
                }
            } else {
                templateMap.add(value);
            }
        }
        if (hasGenericTemplate) {
            templateMap.clear();
        }
    }

    public static PsiType identifyReturnType(PsiExpression methodCallExpression) {
        PsiType returnType = null;

        if (methodCallExpression.getParent() instanceof PsiConditionalExpressionImpl) {
            return identifyReturnType((PsiConditionalExpressionImpl) methodCallExpression.getParent());
        } else if (methodCallExpression.getParent() instanceof PsiLocalVariableImpl
                && methodCallExpression.getParent().getParent() instanceof PsiDeclarationStatementImpl) {
            // this is an assignment and we can probably get a better return type from the variable type which
            // this is being assigned to
            returnType = ((PsiLocalVariableImpl) methodCallExpression.getParent()).getType();
        } else if (methodCallExpression.getParent() instanceof PsiAssignmentExpressionImpl
                && methodCallExpression.getParent().getParent() instanceof PsiExpressionStatement) {
            // this is an assignment and we can probably get a better return type from the variable type which
            // this is being assigned to
            returnType = ((PsiAssignmentExpressionImpl) methodCallExpression.getParent()).getType();
        } else if (methodCallExpression.getParent() instanceof PsiExpressionListImpl
                && methodCallExpression.getParent().getParent() instanceof PsiMethodCallExpressionImpl) {
            // the return value is being passed to another method as a parameter
            PsiExpressionListImpl expressionList = (PsiExpressionListImpl) methodCallExpression.getParent();
            PsiType[] expressionTypes = expressionList.getExpressionTypes();
            PsiExpression[] allExpressions = expressionList.getExpressions();
            // identify the return value is which index
            int i = 0;
            for (PsiExpression expression : allExpressions) {
                if (expression == methodCallExpression) {
                    break;
                }
                i++;
            }

            if (i < expressionTypes.length) {
                returnType = expressionTypes[i];
            }

            PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
            PsiMethodCallExpression parentCall = PsiTreeUtil.getParentOfType(expressionList,
                    PsiMethodCallExpression.class);
            if (parentCall != null && parentCall.resolveMethod() != null) {
                PsiMethod psiMethod = parentCall.resolveMethod();
                PsiClass containingClass = psiMethod.getContainingClass();
                if (containingClass != null && parentOfType != null) {
                    PsiSubstitutor classSubstitutor = TypeConversionUtil
                            .getClassSubstitutor(containingClass,
                                    parentOfType, PsiSubstitutor.EMPTY);
                    returnType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
                }
            }
        } else if (methodCallExpression.getParent() instanceof PsiReturnStatementImpl) {
            // value is being returned, so we can use the return type of the method which contains this call
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(methodCallExpression, PsiMethod.class);
            if (parentMethod != null && parentMethod.getReturnType() != null) {
                returnType = parentMethod.getReturnType();
                PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
                PsiClass containingClass = parentMethod.getContainingClass();
                if (containingClass != null && parentOfType != null) {
                    PsiSubstitutor classSubstitutor = TypeConversionUtil
                            .getClassSubstitutor(containingClass, parentOfType, PsiSubstitutor.EMPTY);
                    returnType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
                }
            }
        } else if (methodCallExpression instanceof PsiMethodCallExpression) {
            PsiMethod psiMethod = ((PsiMethodCallExpression) methodCallExpression).resolveMethod();

            PsiField fieldImpl = (PsiField) ((PsiReferenceExpression)
                    ((PsiMethodCallExpression) methodCallExpression)
                            .getMethodExpression().getQualifierExpression()).resolve();

            PsiClass fieldClass = ((PsiClassReferenceType) fieldImpl.getType()).resolve();

            if (psiMethod != null) {

                returnType = psiMethod.getReturnType();
                PsiClass containingClass = psiMethod.getContainingClass();

                PsiSubstitutor classSubstitutor = TypeConversionUtil.getClassSubstitutor(containingClass,
                        fieldClass, PsiSubstitutor.EMPTY);
                returnType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
            }
        }

        return returnType;
    }

    private static String buildJvmClassName(PsiType returnType) {
        if (returnType == null) {
            return "java.lang.Object";
        }

        if (!(returnType instanceof PsiClassReferenceType)) {
            return returnType.getCanonicalText();
        }
        PsiClassReferenceType classReferenceType = (PsiClassReferenceType) returnType;
        if (classReferenceType.resolve() == null) {
            return "java.lang.Object";
        }

        String jvmClassName1 = JvmClassUtil.getJvmClassName(classReferenceType.resolve());
        if (jvmClassName1 == null) {
            jvmClassName1 = "java.lang.Object";
        }
        StringBuilder jvmClassName =
                new StringBuilder(jvmClassName1);

        int paramCount = classReferenceType.getParameterCount();
        if (paramCount > 0) {
            jvmClassName.append("<");

            PsiType[] parameterArray = classReferenceType.getParameters();
            for (int i = 0; i <= paramCount - 1; i++) {
                jvmClassName.append(buildJvmClassName(parameterArray[i]));
                if (i != paramCount - 1) {
                    jvmClassName.append(",");
                }
            }
            jvmClassName.append(">");
        }

        return jvmClassName.toString();
    }

}
