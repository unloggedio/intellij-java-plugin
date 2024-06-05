package com.insidious.plugin.UITests.wrapper;

public class LocalProjectInfo {
    private String projectName;
    private String projectPath;
    private String buildFile;
    private BuildSystem buildSystem;
    private String startScriptName;
    private String revertScriptName;
    private String removeScriptName;
    private String mainClassName;
    private int startupWaitDuration;

    public LocalProjectInfo(String projectName, String projectPath, String buildFile, BuildSystem buildSystem, String mainClassName) {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.buildFile = buildFile;
        this.buildSystem = buildSystem;
        this.mainClassName = mainClassName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public void setBuildSystem(BuildSystem buildSystem) {
        this.buildSystem = buildSystem;
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

    public enum BuildSystem {MAVEN, GRADLE}
}
