package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.ArrayList;
import java.util.List;

public class CandidateSearchQuery {
    private final boolean loadCalls;
    private final List<String> interfaceNames;
    private final String argumentsDescriptor;
    private final CandidateFilterType candidateFilterType;
    MethodUnderTest methodUnderTest;

    public CandidateSearchQuery(
            MethodUnderTest methodUnderTest,
            String argumentsDescriptor,
            List<String> interfaceNames,
            CandidateFilterType candidateFilterType,
            boolean loadCalls
    ) {
        this.methodUnderTest = methodUnderTest;
        this.argumentsDescriptor = argumentsDescriptor;
        this.interfaceNames = interfaceNames;
        this.candidateFilterType = candidateFilterType;
        this.loadCalls = loadCalls;
    }

    private CandidateSearchQuery(
            MethodUnderTest methodUnderTest,
            String argumentsDescriptor,
            List<String> interfaceNames
    ) {
        this.methodUnderTest = methodUnderTest;
        this.argumentsDescriptor = argumentsDescriptor;
        this.interfaceNames = interfaceNames;
        this.candidateFilterType = CandidateFilterType.METHOD;
        this.loadCalls = false;
    }

    public CandidateSearchQuery() {

        loadCalls = false;
        interfaceNames = new ArrayList<>();
        argumentsDescriptor = "";
        candidateFilterType = CandidateFilterType.ALL;
    }

    public static CandidateSearchQuery fromMethod(
            MethodAdapter focussedMethod,
            List<String> interfacesWithSameSignature,
            String methodArgsDescriptor,
            CandidateFilterType candidateFilterType,
            boolean loadCalls
    ) {
        return new CandidateSearchQuery(
                MethodUnderTest.fromMethodAdapter(focussedMethod),
                methodArgsDescriptor,
                interfacesWithSameSignature,
                candidateFilterType,
                loadCalls
        );
    }


    public boolean isLoadCalls() {
        return loadCalls;
    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    public String getArgumentsDescriptor() {
        return argumentsDescriptor;
    }

    public String getClassName() {
        return methodUnderTest.getClassName();
    }


    public String getMethodName() {
        return methodUnderTest.getName();
    }


    public String getMethodSignature() {
        return methodUnderTest.getSignature();
    }


    public CandidateFilterType getCandidateFilterType() {
        return candidateFilterType;
    }

    @Override
    public String toString() {
        return "CandidateSearchQuery{" +
                "loadCalls=" + loadCalls +
                ", methodUnderTest=" + methodUnderTest +
                ", interfaceNames=" + interfaceNames +
                ", argumentsDescriptor='" + argumentsDescriptor + '\'' +
                ", candidateFilterType=" + candidateFilterType +
                '}';
    }
}
