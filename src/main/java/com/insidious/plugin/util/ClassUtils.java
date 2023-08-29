package com.insidious.plugin.util;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.ui.methodscope.ClassChosenListener;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;


import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.psi.PsiModifier.ABSTRACT;

public class ClassUtils {

//    private static final ObjectMapper objectMapper = new ObjectMapper();

//    static {
//        objectMapper.registerModule(new JavaTimeModule());
//    }

    public static String createDummyValue(
            PsiType parameterType,
            List<String> creationStack,
            Project project
    ) {
        String parameterTypeCanonicalText = parameterType.getCanonicalText();
        if (creationStack.contains(parameterTypeCanonicalText)) {
            return "null";
        }

        try {
            creationStack.add(parameterTypeCanonicalText);
            StringBuilder dummyValue = new StringBuilder();

            if (parameterType instanceof PsiArrayType || parameterType instanceof PsiEllipsisType) {
                PsiArrayType arrayType = (PsiArrayType) parameterType;
                dummyValue.append("[");
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

            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                if (
                        classReferenceType.rawType().getCanonicalText().equals("java.util.List") ||
                                classReferenceType.rawType().getCanonicalText().equals("java.util.ArrayList") ||
                                classReferenceType.rawType().getCanonicalText().equals("java.util.LinkedList") ||
                                classReferenceType.rawType().getCanonicalText().equals("java.util.Set")
                ) {
                    dummyValue.append("[");
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
                    dummyValue.append("]");
                    return dummyValue.toString();
                }

                if (classReferenceType.rawType().getCanonicalText().equals("java.util.Map") ||
                        // either from apache-collections or from spring
                        classReferenceType.rawType().getName().endsWith("MultiValueMap") ||
                        classReferenceType.rawType().getCanonicalText().equals("java.util.Map.Entry")
                ) {
                    dummyValue.append("{");
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
                    dummyValue.append(": ");
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[1], creationStack, project));
                    dummyValue.append("}");
                    return dummyValue.toString();
                }

                if (classReferenceType.rawType().getCanonicalText().equals("java.util.UUID")) {
                    dummyValue.append("\"");
                    dummyValue.append(UUID.randomUUID());
                    dummyValue.append("\"");
                    return dummyValue.toString();
                }

                PsiClass resolvedClass = JavaPsiFacade
                        .getInstance(project)
                        .findClass(classReferenceType.getCanonicalText(), GlobalSearchScope.allScope(project));

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

                PsiField[] parameterObjectFieldList = resolvedClass.getAllFields();

                dummyValue.append("{");
                if (creationStack.size() < 3) {
                    boolean firstField = true;
                    for (PsiField psiField : parameterObjectFieldList) {
                        if (psiField.getName().equals("serialVersionUID")) {
                            continue;
                        }
                        if (psiField.hasModifier(JvmModifier.STATIC)) {
                            continue;
                        }
                        if (!firstField) {
                            dummyValue.append(", ");
                        }

                        dummyValue.append("\"");
                        dummyValue.append(psiField.getName());
                        dummyValue.append("\"");
                        dummyValue.append(": ");
                        dummyValue.append(createDummyValue(psiField.getType(), creationStack, project));
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

    public static void chooseClassImplementation(ClassAdapter psiClass, ClassChosenListener classChosenListener) {


        JavaPsiFacade.getInstance(psiClass.getProject());
        ImplementationSearcher implementationSearcher = new ImplementationSearcher();
        PsiElement[] implementations = implementationSearcher.searchImplementations(
                psiClass.getSource(), null, true, false
        );
        if (implementations == null || implementations.length == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                    NotificationType.ERROR);
            return;
        }
        if (implementations.length == 1) {
            PsiClass singleImplementation = (PsiClass) implementations[0];
            if (singleImplementation.isInterface() || singleImplementation.hasModifierProperty(ABSTRACT)) {
                InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                        NotificationType.ERROR);
                return;
            }
            classChosenListener.classSelected(singleImplementation);
            return;
        }

        List<PsiClass> implementationOptions = Arrays.stream(implementations)
                .map(e -> (PsiClass) e)
                .filter(e -> !e.isInterface())
                .filter(e -> !e.hasModifierProperty(ABSTRACT))
                .collect(Collectors.toList());

        if (implementationOptions.size() == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                    NotificationType.ERROR);
            return;
        }
        if (implementationOptions.size() == 1) {
            classChosenListener.classSelected(implementationOptions.get(0));
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
                                classChosenListener.classSelected((PsiClass) e);
                            });
                })
                .createPopup();
        implementationChooserPopup.showInFocusCenter();

    }

    public static String getSimpleName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }
}
