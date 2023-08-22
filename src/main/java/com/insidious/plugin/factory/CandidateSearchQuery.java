package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.List;

public class CandidateSearchQuery {
    private final boolean loadCalls;
    MethodUnderTest methodUnderTest;
    private List<String> interfaceNames;
    private String argumentsDescriptor;
    private CandidateFilterType candidateFilterType;

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

    public CandidateSearchQuery(
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

    public static CandidateSearchQuery fromMethod(MethodAdapter focussedMethod,
                                                  List<String> interfaceQualifiedNamesWithSameMethod,
                                                  String argumentsDescriptor) {

        return new CandidateSearchQuery(
                MethodUnderTest.fromMethodAdapter(focussedMethod),
                argumentsDescriptor,
                interfaceQualifiedNamesWithSameMethod
        );
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

    public static CandidateSearchQuery cloneWithNewClassName(CandidateSearchQuery candidateSearchQuery, String interfaceName) {
        MethodUnderTest existingMethodUnderTest = candidateSearchQuery.methodUnderTest;
        MethodUnderTest newMethodUnderTest = new MethodUnderTest(existingMethodUnderTest.getName(),
                existingMethodUnderTest.getSignature(),
                existingMethodUnderTest.getMethodHash(), interfaceName);
        return new CandidateSearchQuery(
                newMethodUnderTest,
                candidateSearchQuery.argumentsDescriptor,
                candidateSearchQuery.interfaceNames,
                candidateSearchQuery.candidateFilterType,
                candidateSearchQuery.loadCalls
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
}
