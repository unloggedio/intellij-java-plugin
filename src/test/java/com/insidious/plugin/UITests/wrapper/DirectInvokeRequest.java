package com.insidious.plugin.UITests.wrapper;


import java.util.List;

public class DirectInvokeRequest {

    private String classname;
    private String methodIdentifier;
    private List<DirectInvokeTreeLine> inputs;
    private List<AssertionOptions> assertionOptionsList;
    private List<DirectInvokeTreeLine> expectedOutputs;
    private boolean openFile;

    public DirectInvokeRequest(String classname, String methodIdentifier, List<DirectInvokeTreeLine> inputs, List<DirectInvokeTreeLine> expectedOutputs, List<AssertionOptions> assertionOptionsList, boolean openFile) {
        this.classname = classname;
        this.methodIdentifier = methodIdentifier;
        this.inputs = inputs;
        this.assertionOptionsList = assertionOptionsList;
        this.openFile = openFile;
        this.expectedOutputs = expectedOutputs;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getMethodIdentifier() {
        return methodIdentifier;
    }

    public void setMethodIdentifier(String methodIdentifier) {
        this.methodIdentifier = methodIdentifier;
    }

    public List<DirectInvokeTreeLine> getInputs() {
        return inputs;
    }

    public void setInputs(List<DirectInvokeTreeLine> inputs) {
        this.inputs = inputs;
    }

    public List<AssertionOptions> getAssertionOptionsList() {
        return assertionOptionsList;
    }

    public void setAssertionOptionsList(List<AssertionOptions> assertionOptionsList) {
        this.assertionOptionsList = assertionOptionsList;
    }

    public boolean isOpenFile() {
        return openFile;
    }

    public void setOpenFile(boolean openFile) {
        this.openFile = openFile;
    }

    public List<DirectInvokeTreeLine> getExpectedOutputs() {
        return expectedOutputs;
    }

    public void setExpectedOutputs(List<DirectInvokeTreeLine> expectedOutputs) {
        this.expectedOutputs = expectedOutputs;
    }
}
