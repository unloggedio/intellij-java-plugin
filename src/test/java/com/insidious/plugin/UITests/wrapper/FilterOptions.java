package com.insidious.plugin.UITests.wrapper;

import java.util.List;

public class FilterOptions {
    private List<String> includedClasses;
    private List<String> excludedClasses;
    private List<String> includedMethods;
    private List<String> excludedMethods;
    private boolean clearFilters;

    public FilterOptions(List<String> includedClasses, List<String> excludedClasses, List<String> includedMethods, List<String> excludedMethods, boolean clearFilters) {
        this.includedClasses = includedClasses;
        this.excludedClasses = excludedClasses;
        this.includedMethods = includedMethods;
        this.excludedMethods = excludedMethods;
        this.clearFilters = clearFilters;
    }

    public List<String> getIncludedClasses() {
        return includedClasses;
    }

    public List<String> getExcludedClasses() {
        return excludedClasses;
    }

    public List<String> getIncludedMethods() {
        return includedMethods;
    }

    public List<String> getExcludedMethods() {
        return excludedMethods;
    }

    public boolean isClearFilters() {
        return clearFilters;
    }
}
