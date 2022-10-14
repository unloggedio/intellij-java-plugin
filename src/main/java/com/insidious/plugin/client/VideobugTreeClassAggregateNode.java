package com.insidious.plugin.client;

public class VideobugTreeClassAggregateNode {
    final String className;
    final String packageName;
    final Integer count;

    public VideobugTreeClassAggregateNode(String className, Integer count) {
        this.className = className;
        this.packageName = className.substring(0, className.lastIndexOf("."));
        this.count = count;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public String toString() {
        return className.substring(className.lastIndexOf(".") + 1) + " - [" + count + " items]";
    }
}
