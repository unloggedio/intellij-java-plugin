package com.insidious.plugin.UITests.wrapper;

public class LocalProjectInfo {
    private String startScriptName;
    private String revertScriptName;
    private String removeScriptName;
    private String mainClassName;
    private int startupWaitDuration;
    private String projectPath;

    public LocalProjectInfo(String projectPath, String startScriptName, String revertScriptName, String removeScriptName, String mainClassName, int startupWaitDuration) {
        this.startScriptName = startScriptName;
        this.revertScriptName = revertScriptName;
        this.removeScriptName = removeScriptName;
        this.mainClassName = mainClassName;
        this.startupWaitDuration = startupWaitDuration;
        this.projectPath = projectPath;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public String getStartScriptName() {
        return startScriptName;
    }

    public void setStartScriptName(String startScriptName) {
        this.startScriptName = startScriptName;
    }

    public String getRevertScriptName() {
        return revertScriptName;
    }

    public void setRevertScriptName(String revertScriptName) {
        this.revertScriptName = revertScriptName;
    }

    public String getRemoveScriptName() {
        return removeScriptName;
    }

    public void setRemoveScriptName(String removeScriptName) {
        this.removeScriptName = removeScriptName;
    }

    public int getStartupWaitDuration() {
        return startupWaitDuration;
    }

    public void setStartupWaitDuration(int startupWaitDuration) {
        this.startupWaitDuration = startupWaitDuration;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public enum BuildSystem {MAVEN, GRADLE}
}
