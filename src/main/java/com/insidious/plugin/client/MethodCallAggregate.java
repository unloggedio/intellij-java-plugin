package com.insidious.plugin.client;

public class MethodCallAggregate {
    String methodName;
    Integer count;
    Float minimum;
    Float maximum;
    Float average;
    Float median;
    Float stdDev;
    private Float mode;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Float getMinimum() {
        return minimum;
    }

    public void setMinimum(Float minimum) {
        this.minimum = minimum;
    }

    public Float getMaximum() {
        return maximum;
    }

    public void setMaximum(Float maximum) {
        this.maximum = maximum;
    }

    public Float getAverage() {
        return average;
    }

    public void setAverage(Float average) {
        this.average = average;
    }

    public Float getMedian() {
        return median;
    }

    public void setMedian(Float median) {
        this.median = median;
    }

    public Float getStdDev() {
        return stdDev;
    }

    public void setStdDev(Float stdDev) {
        this.stdDev = stdDev;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setMode(Float mode) {
        this.mode = mode;
    }

    public Float getMode() {
        return mode;
    }
}
