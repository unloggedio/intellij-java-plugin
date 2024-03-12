package com.insidious.plugin.mocking;

import java.util.Objects;

public class ThenParameter {
    private ReturnValue returnParameter;
    private MethodExitType methodExitType;

    public ThenParameter(ReturnValue returnParameter, MethodExitType methodExitType) {
        this.returnParameter = returnParameter;
        this.methodExitType = methodExitType;
    }

    public ThenParameter(ThenParameter e) {
        this.returnParameter = new ReturnValue(e.returnParameter);
        this.methodExitType = e.methodExitType;

    }

    public ThenParameter() {
    }

    public ReturnValue getReturnParameter() {
        return returnParameter;
    }

    public void setReturnParameter(ReturnValue returnParameter) {
        this.returnParameter = returnParameter;
    }

    public MethodExitType getMethodExitType() {
        return methodExitType;
    }

    public void setMethodExitType(MethodExitType methodExitType) {
        this.methodExitType = methodExitType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThenParameter that = (ThenParameter) o;
        return returnParameter.equals(that.returnParameter) && methodExitType == that.methodExitType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnParameter, methodExitType);
    }
}
