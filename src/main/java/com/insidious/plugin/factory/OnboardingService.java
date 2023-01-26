package com.insidious.plugin.factory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public interface OnboardingService {

    Map<String, String> fetchMissingDependencies();

    void postProcessDependencies(Map<String, String> missing, HashSet<String> selectedDependencies);

    Map<String, String> getDependencyStatus();

    List<String> fetchModules();

    String fetchBasePackage();

    String fetchBasePackageForModule(String modulename);

    boolean canGoToDocumention();

    void copyDependenciesToClipboard(Map<String, String> missing);

    void downloadAgentForVersion(String version);

    Map<String, String> getMissingDependencies_v3();

    }
