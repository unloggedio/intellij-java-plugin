package com.insidious.plugin.util;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.methodscope.ClassChosenListener;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

public class ClassUtils {

    public static String createDummyValue(
            PsiType parameterType,
            List<String> creationStack,
            Project project
    ) {
        if (parameterType == null) {
            return "null";
        }
        String parameterTypeCanonicalText =
                ApplicationManager.getApplication().runReadAction((Computable<String>) () -> parameterType.getCanonicalText());
        if (creationStack.contains(parameterTypeCanonicalText)) {
            return "null";
        }

        try {
            creationStack.add(parameterTypeCanonicalText);
            StringBuilder dummyValue = new StringBuilder();

            if (parameterType instanceof PsiArrayType || parameterType instanceof PsiEllipsisType) {
                PsiArrayType arrayType = (PsiArrayType) parameterType;
                dummyValue.append("[");
                PsiType psiType =
                        ApplicationManager.getApplication().runReadAction((Computable<PsiType>) () -> arrayType.getComponentType());
                dummyValue.append(createDummyValue(arrayType.getComponentType(), creationStack, project));
                dummyValue.append("]");
                return dummyValue.toString();
            }

            if (parameterTypeCanonicalText.equals("java.lang.String")) {
                return "\"string\"";
            }
            if (parameterTypeCanonicalText.startsWith("java.lang.")) {
                return "0";
            }

            if (parameterTypeCanonicalText.equals("java.util.Random")) {
                return "{}";
            }
            if (parameterTypeCanonicalText.equals("java.util.Date")) {
                return String.valueOf(new Date().getTime());
            }
            if (parameterTypeCanonicalText.equals("java.time.Instant")) {
//                Date date = new Date();
                return String.valueOf(new Date().getTime() / 1000);

            }

            if (parameterType instanceof PsiClassType) {
                PsiClassType classReferenceType = (PsiClassType) parameterType;
                PsiClassType psiClassRawType =
                        ApplicationManager.getApplication().runReadAction((Computable<PsiClassType>) () -> classReferenceType.rawType());

                String rawTypeCanonicalText =
                        ApplicationManager.getApplication().runReadAction((Computable<String>) () -> psiClassRawType.getCanonicalText());
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
                        dummyValue.append(
                                createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
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
                                        .findClass(classReferenceType.getCanonicalText(), GlobalSearchScope.allScope(project)));

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
                        ApplicationManager.getApplication().runReadAction((Computable<PsiField[]>) () -> resolvedClass.getAllFields());

                dummyValue.append("{");
                if (creationStack.size() < 3) {
                    boolean firstField = true;
                    for (PsiField psiField : parameterObjectFieldList) {
                        String name =
                                ApplicationManager.getApplication().runReadAction((Computable<String>) () -> psiField.getName());
                        if (name.equals("serialVersionUID")) {
                            continue;
                        }
                        boolean hasModifier =
                                ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> psiField.hasModifier(JvmModifier.STATIC));
                        if (hasModifier) {
                            continue;
                        }
                        if (!firstField) {
                            dummyValue.append(", ");
                        }

                        dummyValue.append("\"");
                        String fieldName =
                                ApplicationManager.getApplication().runReadAction((Computable<String>) () -> psiField.getName());
                        dummyValue.append(fieldName);
                        dummyValue.append("\"");
                        dummyValue.append(": ");
                        PsiType type =
                                ApplicationManager.getApplication().runReadAction((Computable<PsiType>) () -> psiField.getType());
                        dummyValue.append(createDummyValue(type, creationStack, project));
                        firstField = false;
                    }
                }
                dummyValue.append("}");

            } else if (parameterType instanceof PsiPrimitiveType) {
                PsiPrimitiveType primitiveType = (PsiPrimitiveType) parameterType;
                if ("boolean".equals(primitiveType.getName())) {
                    return "true";
                }
                return "0";
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
                    ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> singleImplementation.isInterface());
            boolean hasModifiedProperty =
                    ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> singleImplementation.hasModifierProperty(ABSTRACT));
            if (isInterface || hasModifiedProperty) {
                InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                        NotificationType.ERROR);
                return;
            }
            ClassUnderTest classUnderTest =
                    ApplicationManager.getApplication().runReadAction((Computable<ClassUnderTest>) () -> new ClassUnderTest(JvmClassUtil.getJvmClassName(singleImplementation)));
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
                .filter(e -> !ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> e.isInterface()))
                .filter(e -> !ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> e.hasModifierProperty(ABSTRACT)))
                .collect(Collectors.toList());

        if (implementationOptions.size() == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                    NotificationType.ERROR);
            return;
        }
        if (implementationOptions.size() == 1) {
            ClassUnderTest classUnderTest =
                    ApplicationManager.getApplication().runReadAction((Computable<ClassUnderTest>) () -> new ClassUnderTest(JvmClassUtil.getJvmClassName(implementationOptions.get(0))));
            classChosenListener.classSelected(classUnderTest);
            return;
        }
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
        if (showUI) {
            implementationChooserPopup.showInFocusCenter();
        }

    }

    public static String getSimpleName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }
}
