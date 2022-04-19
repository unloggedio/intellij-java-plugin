package com.insidious.plugin.client.exception;

public class ClassInfoNotFoundException extends Throwable {
    private final int classId;

    public ClassInfoNotFoundException(int classId) {
        super();
        this.classId = classId;
    }
}
