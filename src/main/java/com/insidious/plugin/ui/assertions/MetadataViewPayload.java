package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.pojo.atomic.TestType;

public class MetadataViewPayload {
    private String name;
    private String description;
    private TestType type;
    private StoredCandidateMetadata storedCandidateMetadata;

    public MetadataViewPayload(String name, String description, TestType type, StoredCandidateMetadata storedCandidateMetadata) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.storedCandidateMetadata = storedCandidateMetadata;
    }

    public TestType getType() {
        return type;
    }

    public void setType(TestType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StoredCandidateMetadata getStoredCandidateMetadata() {
        return storedCandidateMetadata;
    }

    public void setStoredCandidateMetadata(StoredCandidateMetadata storedCandidateMetadata) {
        this.storedCandidateMetadata = storedCandidateMetadata;
    }

    @Override
    public String toString() {
        return "MetadataViewPayload{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", storedCandidateMetadata=" + storedCandidateMetadata +
                '}';
    }
}
