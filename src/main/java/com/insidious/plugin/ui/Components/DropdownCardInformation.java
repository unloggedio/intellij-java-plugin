package com.insidious.plugin.ui.Components;

import java.util.List;

public class DropdownCardInformation {

    String heading;
    List<String> options;
    String description;
    OnboardingScaffold_v3.DROP_TYPES type;
    Integer defaultSelected = null;
    boolean showRefresh = false;
    public DropdownCardInformation(String heading, List<String> options, String description) {
        this.heading = heading;
        this.options = options;
        this.description = description;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OnboardingScaffold_v3.DROP_TYPES getType() {
        return type;
    }

    public void setType(OnboardingScaffold_v3.DROP_TYPES type) {
        this.type = type;
    }

    public Integer getDefaultSelected() {
        return defaultSelected;
    }

    public void setDefaultSelected(Integer defaultSelected) {
        this.defaultSelected = defaultSelected;
    }

    public boolean isShowRefresh() {
        return showRefresh;
    }

    public void setShowRefresh(boolean showRefresh) {
        this.showRefresh = showRefresh;
    }
}
