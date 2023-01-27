package com.insidious.plugin.pojo;

import java.util.*;

public class ProjectTypeInfo {

    //feature flag to enable/disable searching from dependency tree
    private final boolean detectDependencies = true;
    private final boolean downloadAgent = true;
    private final String defaultAgentType = "jackson-2.13"; //or 'gson' if you want to use gson.
    private final boolean useOnboarding_V3 = true;
    private String javaVersion;
    private boolean isMaven = false;
    private List<Map<String, String>> serializers = new ArrayList<>();
    private String jacksonDatabindVersion = null;
    private Set<String> dependencies_addedManually = new HashSet<>();
    private boolean usesGson = false;

    public boolean getUsesGson() {
        return usesGson;
    }

    public void setUsesGson(boolean usesGson) {
        this.usesGson = usesGson;
    }

    public String getDefaultAgentType() {
        return defaultAgentType;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public boolean isMaven() {
        return isMaven;
    }

    public void setMaven(boolean maven) {
        isMaven = maven;
    }

    public List<Map<String, String>> getSerializers() {
        return serializers;
    }

    public void setSerializers(List<Map<String, String>> serializers) {
        this.serializers = serializers;
    }

    public boolean isDetectDependencies() {
        return detectDependencies;
    }

    public boolean isUseOnboarding_V3() {
        return useOnboarding_V3;
    }

    public String getJacksonDatabindVersion() {
        return jacksonDatabindVersion;
    }

    public void setJacksonDatabindVersion(String jacksonDatabindVersion) {
        this.jacksonDatabindVersion = jacksonDatabindVersion;
    }

    public List<String> getDependenciesToWatch() {
        List<String> dependenciesToWatch = new ArrayList<>();
        dependenciesToWatch.add("jackson-datatype-hibernate5");
        dependenciesToWatch.add("jackson-datatype-joda");
        dependenciesToWatch.add("jackson-datatype-jdk8");
        dependenciesToWatch.add("jackson-databind");
        dependenciesToWatch.add("jackson-core");
        dependenciesToWatch.add("gson");
        dependenciesToWatch.add("jackson-datatype-jsr310");
        return dependenciesToWatch;
    }

    public Set<String> getDependencies_addedManually() {
        return dependencies_addedManually;
    }

    public void setDependencies_addedManually(Set<String> dependencies_addedManually) {
        this.dependencies_addedManually = dependencies_addedManually;
    }

    public boolean isDownloadAgent() {
        return downloadAgent;
    }

    public RUN_TYPES[] getAllRunTypes() {
        return RUN_TYPES.values();
    }

    public enum RUN_TYPES {INTELLIJ_APPLICATION, MAVEN_CLI, GRADLE_CLI, JAVA_JAR_CLI}
}
