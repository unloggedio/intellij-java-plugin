package com.insidious.plugin.UITests.wrapper;

public class WindowsScripts {

    private String clearTests;
    private String startProcess;
    private String stopProcess;

    public WindowsScripts(String clearTests, String startProcess, String stopProcess) {
        this.clearTests = clearTests;
        this.startProcess = startProcess;
        this.stopProcess = stopProcess;
    }

    public String getClearTests() {
        return clearTests;
    }

    public void setClearTests(String clearTests) {
        this.clearTests = clearTests;
    }

    public String getStartProcess() {
        return startProcess;
    }

    public void setStartProcess(String startProcess) {
        this.startProcess = startProcess;
    }

    public String getStopProcess() {
        return stopProcess;
    }

    public void setStopProcess(String stopProcess) {
        this.stopProcess = stopProcess;
    }
}
