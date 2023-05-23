package com.insidious.plugin.util;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.ui.methodscope.ClassChosenListener;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ClassUtils {

    public static String createDummyValue(
            PsiType parameterType,
            List<String> creationStack, Project project) {
        String creationKey = parameterType.getCanonicalText();
        if (creationStack.contains(creationKey)) {
            return "null";
        }

        try {
            creationStack.add(creationKey);
            StringBuilder dummyValue = new StringBuilder();
            if (parameterType.getCanonicalText().equals("java.lang.String")) {
                return "\"string\"";
            }
            if (parameterType.getCanonicalText().startsWith("java.lang.")) {
                return "0";
            }

            if (parameterType.getCanonicalText().equals("java.util.Random")) {
                return "";
            }
            if (parameterType.getCanonicalText().equals("java.util.Date")) {
//                try {
                return "\"" + new Date().toGMTString() + "\"";
//                } catch (JsonProcessingException e) {
//                     should never happen
//                }
            }
            if (parameterType instanceof PsiArrayType) {
                PsiArrayType arrayType = (PsiArrayType) parameterType;
                dummyValue.append("[");
                dummyValue.append(createDummyValue(arrayType.getComponentType(), creationStack, project));
                dummyValue.append("]");
                return dummyValue.toString();
            }

            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                if (classReferenceType.rawType().getCanonicalText().equals("java.util.List") ||
                        classReferenceType.rawType().getCanonicalText().equals("java.util.Set")
                ) {
                    dummyValue.append("[");
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0], creationStack, project));
                    dummyValue.append("]");
                    return dummyValue.toString();
                }

                if (classReferenceType.rawType().getCanonicalText().equals("java.util.Map") ||
                        // either from apache-collections or from spring
                        classReferenceType.rawType().getName().equals("MultiValueMap")) {
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
                        .findClass(classReferenceType.getCanonicalText(), parameterType.getResolveScope());

                if (resolvedClass == null) {
                    // class not resolved
                    return dummyValue.toString();
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
            creationStack.remove(creationKey);
        }

    }

    public static void chooseClassImplementation(ClassAdapter psiClass, ClassChosenListener classChosenListener) {

//        if (!psiClass.isInterface()) {
//            classChosenListener.classSelected((PsiClass) psiClass.getSource());
//            return;
//        }


//        if (psiClass.isInterface()) {
        JavaPsiFacade.getInstance(psiClass.getProject());
        ImplementationSearcher implementationSearcher = new ImplementationSearcher();
        PsiElement[] implementations = implementationSearcher.searchImplementations(
                psiClass.getSource(), null, true, false
        );
        if (implementations == null || implementations.length == 0) {
//            if (!psiClass.isInterface()) {
//                classChosenListener.classSelected((PsiClass) psiClass.getSource());
//            } else {
            InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                    NotificationType.ERROR);
            return;
//            }
        }
        if (implementations.length == 1) {
            PsiClass singleImplementation = (PsiClass) implementations[0];
            classChosenListener.classSelected(singleImplementation);
        } else {
            @NotNull List<String> implementationOptions = Arrays.stream(implementations)
                    .map(e -> (PsiClass)e)
                    .filter(e -> !e.isInterface())
                    .map(PsiClass::getQualifiedName)
                    .collect(Collectors.toList());
            @NotNull JBPopup implementationChooserPopup = JBPopupFactory
                    .getInstance()
                    .createPopupChooserBuilder(implementationOptions)
                    .setTitle("Run using implementation for " + psiClass.getName())
                    .setItemChosenCallback(psiElementName -> {
                        Optional<PsiElement> selectedClass = Arrays.stream(implementations)
                                .filter(e -> Objects.equals(((PsiClass) e).getQualifiedName(), psiElementName))
                                .findFirst();
                        classChosenListener.classSelected((PsiClass) selectedClass.get());
                    })
                    .createPopup();
            implementationChooserPopup.showInFocusCenter();
        }
//        }

    }

}
