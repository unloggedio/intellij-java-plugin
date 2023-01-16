package com.insidious.plugin.factory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public interface OnboardingService {

    public Map<String,String> fetchMissingDependencies();

    void postProcessDependencies(Map<String, String> missing, HashSet<String> selectedDependencies);

    public Map<String, String> getDependencyStatus();

    public List<String> fetchModules();

    public String fetchBasePackage();

    public String fetchBasePackageForModule(String modulename);

    public boolean canGoToDocumention();
}
