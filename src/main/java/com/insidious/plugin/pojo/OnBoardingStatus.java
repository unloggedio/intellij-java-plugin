package com.insidious.plugin.pojo;
public class OnBoardingStatus {
    String currentModule;
    Boolean addOpens;
    ProjectTypeInfo.RUN_TYPES runType = ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION;
    String jdkVersion;
    public String getCurrentModule() {
        return currentModule;
    }

    public void setCurrentModule(String currentModule) {
        this.currentModule = currentModule;
//        System.out.println("SET CURRENT MODULE to " + currentModule);
    }

    public ProjectTypeInfo.RUN_TYPES getRunType() {
        return runType;
    }

    public void setRunType(ProjectTypeInfo.RUN_TYPES runType) {
//        System.out.println("SET RUN TYPE to " + runType.toString());
        this.runType = runType;
    }

    public String getJdkVersion() {
        return jdkVersion;
    }

    public void setJdkVersion(String jdkVersion) {
        this.jdkVersion = jdkVersion;
    }

    @Override
    public String toString() {
        return "OnBoardingStatus{" +
                "currentModule='" + currentModule + '\'' +
                ", addOpens=" + addOpens +
                ", runType=" + runType +
                ", jdkVersion='" + jdkVersion + '\'' +
                '}';
    }
}