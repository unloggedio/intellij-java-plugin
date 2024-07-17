package com.insidious.plugin.UITests.wrapper;

public enum HelperScriptType {

    START_SCRIPT("start_project.sh"),
    REVERT_ALL_CHANGES_SCRIPT("remove_local_sessions.sh"),
    REMOVE_LOCAL_SESSIONS_SCRIPT("git_rollback.sh");

    private String value;

    HelperScriptType(String value) {
        this.value = value;
    }

    public String getScriptName() {
        return this.value;
    }
}
