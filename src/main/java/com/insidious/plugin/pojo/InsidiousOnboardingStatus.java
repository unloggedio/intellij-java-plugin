package com.insidious.plugin.pojo;

import com.insidious.plugin.ui.Components.OnboardingScaffoldV3;

public class InsidiousOnboardingStatus {

    private boolean completed=false;
    private OnBoardingStatus status;

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public OnBoardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnBoardingStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "InsidiousOnboardingStatus{" +
                "completed=" + completed +
                ", status=" + status +
                '}';
    }
}
