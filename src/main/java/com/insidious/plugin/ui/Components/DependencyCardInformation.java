package com.insidious.plugin.ui.Components;

import java.util.List;

public class DependencyCardInformation {

    private String heading;
    private String description;
    private List<String> dependencies;

    public DependencyCardInformation(String heading, String description, List<String> dependencies) {
        this.heading = heading;
        this.description = description;
        this.dependencies = dependencies;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }
}
