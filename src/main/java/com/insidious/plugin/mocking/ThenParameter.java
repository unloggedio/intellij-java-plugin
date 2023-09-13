package com.insidious.plugin.mocking;

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
}
