package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.intellij.psi.PsiClass;

public interface ClassChosenListener {
    void classSelected(ClassUnderTest classUnderTest);
}
