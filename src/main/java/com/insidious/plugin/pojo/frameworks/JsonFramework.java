package com.insidious.plugin.pojo.frameworks;

import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;

import static com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory.makeParameter;

public enum JsonFramework {
    Gson(makeParameter("gson", "com.google.gson.Gson", -99883),
            "fromJson", "toJson",
            ClassName.bestGuess("com.google.gson.reflect.TypeToken")
    ),
    Jackson(makeParameter("objectMapper", "com.fasterxml.jackson.databind.ObjectMapper", -99884),
            "readValue", "writeValueAsString",
            ClassName.bestGuess("com.fasterxml.jackson.core.type.TypeReference"));

    private final Parameter instance;
    private final String fromJsonMethodName;
    private final String toJsonMethodName;
    private final ClassName tokenTypeClass;

    JsonFramework(Parameter instance, String fromJsonMethodName, String toJsonMethodName, ClassName tokenTypeClass) {
        this.instance = instance;
        this.fromJsonMethodName = fromJsonMethodName;
        this.toJsonMethodName = toJsonMethodName;
        this.tokenTypeClass = tokenTypeClass;
    }

    public String getFromJsonMethodName() {
        return fromJsonMethodName;
    }

    public String getToJsonMethodName() {
        return toJsonMethodName;
    }

    public Parameter getInstance() {
        return instance;
    }

    public ClassName getTokenTypeClass() {
        return tokenTypeClass;
    }
}
