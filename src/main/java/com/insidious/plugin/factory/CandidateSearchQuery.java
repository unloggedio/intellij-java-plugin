package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.List;

public class CandidateSearchQuery {
    private final boolean loadCalls;
    private List<String> interfaceNames;
    private String className;
    private String methodName;
    private String methodSignature;
    private String argumentsDescriptor;
    private CandidateFilterType candidateFilterType;
    public CandidateSearchQuery(
            String className,
            String methodName,
            String methodSignature,
            String argumentsDescriptor,
            List<String> interfaceNames,
            CandidateFilterType candidateFilterType,
            boolean loadCalls
    ) {
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.argumentsDescriptor = argumentsDescriptor;
        this.interfaceNames = interfaceNames;
        this.candidateFilterType = candidateFilterType;
        this.loadCalls = loadCalls;
    }
    public CandidateSearchQuery(
            String className,
            String methodName,
            String methodSignature,
            String argumentsDescriptor,
            List<String> interfaceNames
    ) {
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.argumentsDescriptor = argumentsDescriptor;
        this.interfaceNames = interfaceNames;
        this.candidateFilterType = CandidateFilterType.ALL;
        this.loadCalls = false;
    }

    public static CandidateSearchQuery fromMethod(MethodAdapter focussedMethod,
                                                  List<String> interfaceQualifiedNamesWithSameMethod,
                                                  String argumentsDescriptor) {

        return new CandidateSearchQuery(
                focussedMethod.getContainingClass().getQualifiedName(),
                focussedMethod.getName(),
                focussedMethod.getJVMSignature(),
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
                focussedMethod.getContainingClass().getQualifiedName(),
                focussedMethod.getName(),
                focussedMethod.getJVMSignature(),
                methodArgsDescriptor,
                interfacesWithSameSignature,
                candidateFilterType,
                loadCalls
        );
    }

    public boolean isLoadCalls() {
        return loadCalls;
    }

    public static CandidateSearchQuery cloneWithNewClassName(CandidateSearchQuery candidateSearchQuery, String interfaceName) {
        return new CandidateSearchQuery(
                interfaceName,
                candidateSearchQuery.methodName,
                candidateSearchQuery.methodSignature,
                candidateSearchQuery.argumentsDescriptor,
                candidateSearchQuery.interfaceNames,
                candidateSearchQuery.candidateFilterType,
                candidateSearchQuery.loadCalls
        );

    }

    public List<String> getInterfaceNames() {
        return interfaceNames;
    }

    public String getArgumentsDescriptor() {
        return argumentsDescriptor;
    }

    public String getClassName() {
        return className;
    }


    public String getMethodName() {
        return methodName;
    }


    public String getMethodSignature() {
        return methodSignature;
    }


    public CandidateFilterType getCandidateFilterType() {
        return candidateFilterType;
    }
}
