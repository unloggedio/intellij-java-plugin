package com.insidious.plugin.util;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.ui.methodscope.ClassChosenListener;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClassUtils {

    public static void chooseClassImplementation(ClassAdapter psiClass, ClassChosenListener classChosenListener) {

        if (!psiClass.isInterface()) {
            classChosenListener.classSelected((PsiClass) psiClass.getSource());
            return;
        }

        if (psiClass.isInterface()) {
            JavaPsiFacade.getInstance(psiClass.getProject());
            ImplementationSearcher implementationSearcher = new ImplementationSearcher();
            PsiElement[] implementations = implementationSearcher.searchImplementations(
                    psiClass.getSource(), null, false, false
            );
            if (implementations == null || implementations.length == 0) {
                InsidiousNotification.notifyMessage("No implementations found for " + psiClass.getName(),
                        NotificationType.ERROR);
            } else if (implementations.length == 1) {
                PsiClass singleImplementation = (PsiClass) implementations[0];
                classChosenListener.classSelected(singleImplementation);
            } else {
                @NotNull List<String> implementationOptions = Arrays.stream(implementations)
                        .map(e -> ((PsiClass) e).getQualifiedName())
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
        }

    }

}
