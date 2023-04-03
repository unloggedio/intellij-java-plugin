package com.insidious.plugin.ui.Components;

public class DifferenceInstance {

    public enum DIFFERENCE_TYPE {DIFFERENCE, LEFT_ONLY, RIGHT_ONLY}
    private String key;
    private Object leftValue;
    private Object rightValue;
    private DIFFERENCE_TYPE differenceType;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(Object leftValue) {
        this.leftValue = leftValue;
    }

    public Object getRightValue() {
        return rightValue;
    }

    public void setRightValue(Object rightValue) {
        this.rightValue = rightValue;
    }

    public DIFFERENCE_TYPE getDifferenceType() {
        return differenceType;
    }

    public void setDifferenceType(DIFFERENCE_TYPE differenceType) {
        this.differenceType = differenceType;
    }

    public DifferenceInstance(String key, Object leftValue, Object rightValue, DIFFERENCE_TYPE differenceType) {
        this.key = key;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.differenceType = differenceType;
    }
}
