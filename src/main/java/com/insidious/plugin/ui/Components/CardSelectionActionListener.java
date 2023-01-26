package com.insidious.plugin.ui.Components;

import java.util.Set;

public interface CardSelectionActionListener {
    void selectedOption(String selection, OnboardingScaffoldV3.DROP_TYPES type);
    void setSelectionsForDependencyAddition(Set<String> dependencies);
    void refreshModules();
    void refreshDependencies();
    void refreshSerializerSelection();
}
