package com.insidious.plugin.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectTypeInfo {

    private String javaVersion;
    private boolean isMaven=false;
    //feature flag to enable/disable searching from dependency tree
    private boolean detectDependencies=true;
    private List<Map<String,String>> serializers = new ArrayList<>();
    private String jacksonDatabindVersion = null;

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

    public void setDetectDependencies(boolean detectDependencies) {
        this.detectDependencies = detectDependencies;
    }

    public String getJacksonDatabindVersion() {
        return jacksonDatabindVersion;
    }

    public void setJacksonDatabindVersion(String jacksonDatabindVersion) {
        this.jacksonDatabindVersion = jacksonDatabindVersion;
    }
}
