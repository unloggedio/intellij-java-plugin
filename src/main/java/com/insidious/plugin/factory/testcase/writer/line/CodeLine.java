package com.insidious.plugin.factory.testcase.writer.line;

import com.squareup.javapoet.MethodSpec;

public interface CodeLine {
    String getLine();

    void writeTo(MethodSpec.Builder methodBuilder, Object[] arguments);
}
