package com.insidious.plugin.client;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.HashMap;
import java.util.Map;

public class ClassMethodAggregates {
    private static final Logger logger = LoggerUtil.getInstance(ClassMethodAggregates.class);
    Map<String, MethodCallAggregate> methodCallAggregateMap = new HashMap<>();

    public void addMethodAggregate(MethodCallAggregate methodClassAggregate) {
        if (methodCallAggregateMap.containsKey(methodClassAggregate.getMethodName())) {
            logger.error("overridden method not handled: " + methodClassAggregate);
            return;
        }
        methodCallAggregateMap.put(methodClassAggregate.getMethodName(), methodClassAggregate);
    }

    public MethodCallAggregate getMethodAggregate(String name) {
        return methodCallAggregateMap.get(name);
    }
}
