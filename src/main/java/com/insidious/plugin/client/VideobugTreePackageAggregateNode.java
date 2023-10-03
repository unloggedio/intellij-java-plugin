package com.insidious.plugin.client;



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
    public int compareTo( VideobugTreePackageAggregateNode o) {
        return this.getPackageName().compareTo(o.getPackageName());
    }

    @Override
    public String toString() {
        return packageName + " - [" + count + " items]";
    }
}
