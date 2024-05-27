package com.insidious.plugin.constants;

public enum ExecutionSessionSourceMode {
    LOCAL,
    REMOTE;

    @Override
    public String toString() {
        switch (this) {
            case LOCAL:
                return "Local";
            case REMOTE:
                return "Remote";
        }
        return "n/a";
    }
}
