package com.insidious.plugin.UITests.wrapper;

public class GitProjectInfo {
    private String projectName;
    private String gitUrl;
    private String gitBranch;
    private String buildFile;
    private LocalProjectInfo.BuildSystem buildSystem;
    private int startupWaitDuration;
    private boolean switchBranchOnOpen;
    private String jdkVersion;
    private String testBasePath;
    private LocalProjectInfo localProjectInfo;
    private GitLoginOptions loginOptions;
    private int lineCount;
    private WindowsScripts windowsScripts;

    public GitProjectInfo(String projectName, String gitUrl, String gitBranch, String buildFile, LocalProjectInfo.BuildSystem buildSystem, int startupWaitDuration, boolean switchBranchOnOpen, String jdkVersion, String testBasePath, int lineCount) {
        this.projectName = projectName;
        this.gitUrl = gitUrl;
        this.gitBranch = gitBranch;
        this.buildFile = buildFile;
        this.buildSystem = buildSystem;
        this.startupWaitDuration = startupWaitDuration;
        this.switchBranchOnOpen = switchBranchOnOpen;
        this.jdkVersion = jdkVersion;
        this.testBasePath = testBasePath;
        this.lineCount = lineCount;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    public LocalProjectInfo.BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public void setBuildSystem(LocalProjectInfo.BuildSystem buildSystem) {
        this.buildSystem = buildSystem;
    }

    public int getStartupWaitDuration() {
        return startupWaitDuration;
    }

    public void setStartupWaitDuration(int startupWaitDuration) {
        this.startupWaitDuration = startupWaitDuration;
    }

    public boolean isSwitchBranchOnOpen() {
        return switchBranchOnOpen;
    }

    public void setSwitchBranchOnOpen(boolean switchBranchOnOpen) {
        this.switchBranchOnOpen = switchBranchOnOpen;
    }

    public String getJdkVersion() {
        return jdkVersion;
    }

    public void setJdkVersion(String jdkVersion) {
        this.jdkVersion = jdkVersion;
    }

    public String getTestBasePath() {
        return testBasePath;
    }

    public void setTestBasePath(String testBasePath) {
        this.testBasePath = testBasePath;
    }

    public LocalProjectInfo getLocalProjectInfo() {
        return localProjectInfo;
    }

    public void setLocalProjectInfo(LocalProjectInfo localProjectInfo) {
        this.localProjectInfo = localProjectInfo;
    }

    public GitLoginOptions getLoginOptions() {
        return loginOptions;
    }

    public void setLoginOptions(GitLoginOptions loginOptions) {
        this.loginOptions = loginOptions;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    public int getLineCount() {
        return this.lineCount;
    }

    public WindowsScripts getWindowsScripts() {
        return windowsScripts;
    }

    public void setWindowsScripts(WindowsScripts windowsScripts) {
        this.windowsScripts = windowsScripts;
    }
}
