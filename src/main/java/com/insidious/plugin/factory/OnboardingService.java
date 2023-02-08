package com.insidious.plugin.factory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OnboardingService {

//    Map<String, String> fetchMissingDependencies();

    void postProcessDependencies(Map<String, String> missing, Set<String> selectedDependencies);

    List<String> fetchModules();

    String fetchBasePackage();

    String fetchBasePackageForModule(String modulename);

    void downloadAgentForVersion(String version);

    Map<String, String> getMissingDependencies_v3();


    void setSelectedModule(String part);

    String fetchPackagePathForModule(String modulename);
}
