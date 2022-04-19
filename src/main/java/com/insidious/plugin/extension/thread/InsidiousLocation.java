package com.insidious.plugin.extension.thread;

import com.intellij.debugger.engine.DebuggerUtils;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

public class InsidiousLocation implements Location {

    private String methodName;
    private ReferenceType declaringType;
    private Method method;
    private long codeIndex;
    private String sourceName;
    private final String sourcePath;
    private final int lineNumber;

    public InsidiousLocation(ReferenceType declaringType, String methodName, long codeIndex,
                             String sourceName, String sourcePath,
                             int lineNumber) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.codeIndex = codeIndex;
        this.sourceName = sourceName;
        this.sourcePath = sourcePath;
        this.lineNumber = lineNumber;
        this.method = DebuggerUtils.findMethod(declaringType, methodName, null);
    }

    public InsidiousLocation(String sourcePath, int lineNumber) {
        this.sourcePath = sourcePath;
        this.lineNumber = lineNumber;
    }

    @Override
    public ReferenceType declaringType() {
        return declaringType;
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public long codeIndex() {
        return -2;
    }

    @Override
    public String sourceName() throws AbsentInformationException {
        return sourceName;
    }

    @Override
    public String sourceName(String s) throws AbsentInformationException {
        return sourceName;
    }

    @Override
    public String sourcePath() throws AbsentInformationException {
        return sourcePath;
    }

    @Override
    public String sourcePath(String s) throws AbsentInformationException {
        return sourcePath;
    }

    @Override
    public int lineNumber() {
        return lineNumber;
    }

    @Override
    public int lineNumber(String s) {
        return lineNumber;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return declaringType.virtualMachine();
    }

    @Override
    public int compareTo(@NotNull Location location) {
        return 0;
    }
}
