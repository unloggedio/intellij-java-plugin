package com.insidious.plugin.client;

public class FailedToReadClassWeaveException extends Exception {
    public FailedToReadClassWeaveException(String s) {
        super(s);
    }

    public FailedToReadClassWeaveException(String s, Exception e) {
        super(s, e);
    }
}
