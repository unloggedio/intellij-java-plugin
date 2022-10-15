package com.insidious.plugin.client;

import org.jetbrains.annotations.NotNull;

public class VideobugTreePackageAggregateNode implements Comparable<VideobugTreePackageAggregateNode> {
    final String packageName;
    final Integer count;

    public VideobugTreePackageAggregateNode(String packageName, Integer count) {
        this.packageName = packageName;
        this.count = count;
    }

    public String getPackageName() {
        return packageName;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public int compareTo(@NotNull VideobugTreePackageAggregateNode o) {
        return this.getPackageName().compareTo(o.getPackageName());
    }

    @Override
    public String toString() {
        return packageName + " - [" + count + " items]";
    }
}
