package com.insidious.plugin.videobugclient.pojo.exceptions;

public class APICallException extends Exception {

    public APICallException(String message, Throwable cause) {
        super(message, cause);
    }

    public APICallException(String message) {
        super(message);
    }

    public APICallException() {
    }
}
