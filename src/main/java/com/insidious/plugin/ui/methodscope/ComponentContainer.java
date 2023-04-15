package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

import javax.swing.*;
import java.util.List;

public class ComponentContainer {

    private CompareControlComponent source;
    private AgentResponseComponent normalResponse = null;
    private AgentExceptionResponseComponent exceptionResponse = null;

    public ComponentContainer(CompareControlComponent source) {
        this.source = source;
    }

    public CompareControlComponent getSource() {
        return source;
    }

    public void setSource(CompareControlComponent source) {
        this.source = source;
    }

    public AgentResponseComponent getNormalResponse() {
        return normalResponse;
    }

    public void setNormalResponse(AgentResponseComponent normalResponse) {
        this.normalResponse = normalResponse;
        this.source.setResponseComponent(normalResponse);
    }

    public AgentExceptionResponseComponent getExceptionResponse() {
        return exceptionResponse;
    }

    public void setExceptionResponse(AgentExceptionResponseComponent exceptionResponse) {
        this.exceptionResponse = exceptionResponse;
    }

    public TestCandidateMetadata getCandidateMetadata() {

        return source.getCandidateMetadata();
    }

    public List<String> getMethodArgumentValues() {

        return source.getMethodArgumentValues();
    }

    public Integer getHash() {
        return source.getHash();
    }

    public JPanel getComponent() {

        if (this.normalResponse != null) {
            return this.normalResponse.getComponent();
        } else {
            return this.exceptionResponse.getComponent();
        }
    }

    public void setAndDisplayExceptionFlow(AgentExceptionResponseComponent exceptionResponseComponent) {
        setExceptionResponse(exceptionResponseComponent);
        this.source.DisplayExceptionResponse(exceptionResponseComponent);
    }
}
