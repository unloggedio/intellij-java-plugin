package com.insidious.plugin.pojo.atomic;

import java.util.List;
import java.util.Map;

public class AtomicRecord {

    private String classname;
    private Map<String,List<StoredCandidate>> storedCandidateMap;

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public Map<String,List<StoredCandidate>> getStoredCandidateMap() {
        return storedCandidateMap;
    }

    public void setStoredCandidateMap(Map<String,List<StoredCandidate>> storedCandidateList) {
        this.storedCandidateMap = storedCandidateList;
    }

    @Override
    public String toString() {
        return "AtomicRecord{" +
                "classname='" + classname + '\'' +
                ", storedCandidateList=" + storedCandidateMap +
                '}';
    }
}
