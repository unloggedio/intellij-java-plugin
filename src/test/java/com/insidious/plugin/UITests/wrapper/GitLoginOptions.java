package com.insidious.plugin.UITests.wrapper;

public class GitLoginOptions {
    String personalAccessToken;

    public GitLoginOptions(String personalAccessToken) {
        this.personalAccessToken = personalAccessToken;
    }

    public String getPersonalAccessToken() {
        return personalAccessToken;
    }

    public void setPersonalAccessToken(String personalAccessToken) {
        this.personalAccessToken = personalAccessToken;
    }
}
