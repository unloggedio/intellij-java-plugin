package com.insidious.plugin.ui.Components;

import java.util.List;
import java.util.Map;

public interface CardActionListener {

    void performActions(List<Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String>> actions);

    String getBasePackageForModule(String moduleName);

    void checkForSelogs();

    void loadLiveLiew();

    void refreshModules();

    void refreshDependencies();

    void refreshSerializers();

    boolean hasRunnableApplicationConfig();

    boolean runApplicationWithUnlogged();

    boolean isApplicationRunning();

    void triggerOnboardingRestart();

    String getCurrentBasePackage();

    void updateBasePackage(String text);
}
