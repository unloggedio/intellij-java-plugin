package com.insidious.plugin.client;

import org.jetbrains.annotations.NotNull;

public class VideobugTreeClassAggregateNode implements Comparable<VideobugTreeClassAggregateNode> {
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

    @Override
    public int compareTo(@NotNull VideobugTreeClassAggregateNode o) {
        return this.className.compareTo(o.getClassName());
    }
}
