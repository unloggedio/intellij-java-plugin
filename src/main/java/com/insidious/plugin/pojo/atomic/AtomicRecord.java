package com.insidious.plugin.pojo.atomic;

import java.util.List;

public class AtomicRecord {

    private String classname;
    private String method;
    private List<StoredCandidate> storedCandidateList;

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<StoredCandidate> getStoredCandidateList() {
        return storedCandidateList;
    }

    public void setStoredCandidateList(List<StoredCandidate> storedCandidateList) {
        this.storedCandidateList = storedCandidateList;
    }

    @Override
    public String toString() {
        return "AtomicRecord{" +
                "classname='" + classname + '\'' +
                ", method='" + method + '\'' +
                ", storedCandidateList=" + storedCandidateList +
                '}';
    }
}
