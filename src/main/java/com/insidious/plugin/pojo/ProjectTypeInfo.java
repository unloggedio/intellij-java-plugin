package com.insidious.plugin.pojo;

import java.util.*;

public class ProjectTypeInfo {

    private String javaVersion;
    private boolean isMaven=false;
    //feature flag to enable/disable searching from dependency tree
    private final boolean detectDependencies=true;
    private List<Map<String,String>> serializers = new ArrayList<>();
    private String jacksonDatabindVersion = null;
    private final boolean downloadAgent = false;
    private Set<String> dependencies_addedManually = new HashSet<>();

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


    public String getJacksonDatabindVersion() {
        return jacksonDatabindVersion;
    }

    public void setJacksonDatabindVersion(String jacksonDatabindVersion) {
        this.jacksonDatabindVersion = jacksonDatabindVersion;
    }

    public List<String> getDependenciesToWatch()
    {
        List<String> dependenciesToWatch = new ArrayList<>();
        dependenciesToWatch.add("jackson-datatype-hibernate5");
        dependenciesToWatch.add("jackson-datatype-joda");
        dependenciesToWatch.add("jackson-datatype-jdk8");
        dependenciesToWatch.add("jackson-databind");
        return dependenciesToWatch;
    }

    public Set<String> getDependencies_addedManually() {
        return dependencies_addedManually;
    }

    public boolean isDownloadAgent() {
        return downloadAgent;
    }
}
