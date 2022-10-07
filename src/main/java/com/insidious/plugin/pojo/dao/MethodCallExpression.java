package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "method_call")
public class MethodCallExpression {

    @DatabaseField(id = true)
    private long id;

    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private Long[] arguments = new Long[0];
    @DatabaseField
    private String methodName;
    @DatabaseField
    private boolean isStaticCall;
    @DatabaseField(foreign = true)
    private Parameter subject;

    @DatabaseField
    private long entryTime;
    @DatabaseField(foreign = true)
    private ProbeInfo entryProbeInfo;
    @DatabaseField(foreign = true)
    private Parameter returnValue;
    @DatabaseField(foreign = true)
    private DataEventWithSessionId entryProbe;
    @DatabaseField
    private int callStack;
    @DatabaseField
    private int methodAccess;


    public MethodCallExpression() {
    }

    public MethodCallExpression(
            String methodName,
            Parameter subject,
            Long[] arguments,
            Parameter returnValue
    ) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = arguments;
        this.returnValue = returnValue;
    }

    public static MethodCallExpression FromMCE(com.insidious.plugin.pojo.MethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return null;
        }

        MethodCallExpression methodCallExpression1 = new MethodCallExpression(
                methodCallExpression.getMethodName(),
                Parameter.fromParameter(methodCallExpression.getSubject()),
                methodCallExpression.getArguments().all().stream()
                        .map(e1 -> (long) e1.getValue())
                        .toArray(Long[]::new),
                Parameter.fromParameter(methodCallExpression.getReturnValue())
        );
        methodCallExpression1.setEntryProbe(methodCallExpression.getEntryProbe());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setEntryTime(methodCallExpression.getEntryTime());
        methodCallExpression1.setEntryProbeInfo(methodCallExpression.getEntryProbeInfo());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setId(methodCallExpression.getId());
        return methodCallExpression1;
    }

    public static com.insidious.plugin.pojo.MethodCallExpression ToMCE(MethodCallExpression methodCallExpression) {
        com.insidious.plugin.pojo.MethodCallExpression methodCallExpression1 = new com.insidious.plugin.pojo.MethodCallExpression(
                methodCallExpression.getMethodName(), null, new VariableContainer(), null, 0
        );
        methodCallExpression1.setEntryProbe(methodCallExpression.getEntryProbe());
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setEntryTime(methodCallExpression.getEntryTime());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setId(methodCallExpression.getId());

        return methodCallExpression1;
    }

    public static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

    public long getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(long entryTime) {
        this.entryTime = entryTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ProbeInfo getEntryProbeInfo() {
        return entryProbeInfo;
    }

    public void setEntryProbeInfo(DataInfo entryProbeInfo) {
        this.entryProbeInfo = ProbeInfo.FromProbeInfo(entryProbeInfo);
    }

    public void setEntryProbeInfo(ProbeInfo entryProbeInfo) {
        this.entryProbeInfo = entryProbeInfo;
    }

    public Parameter getSubject() {
        return subject;
    }

    public void setSubject(Parameter testSubject) {
        this.subject = testSubject;
    }

    public Long[] getArguments() {
        return arguments;
    }

    public Parameter getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Parameter returnValue) {
        this.returnValue = returnValue;
    }

    public String getMethodName() {
        return methodName;
    }

    public Parameter getException() {
        if (returnValue.getException()) {
            return returnValue;
        }
        return null;
    }

    @Override
    public String toString() {

        String owner = "<n/a>";
        if (subject != null && subject.getProbeInfo() != null) {
            owner = subject.getProbeInfo().getAttribute("Owner", null);
        }
        String name = "";
        if (subject != null) {
            name = subject.getName() + ".";
        }
        return "[" + owner + "] => " + ((returnValue == null || returnValue.getName() == null || returnValue.getException()) ?
                "" : (returnValue.getName() + " [" + returnValue.getValue() + "]" + "  == ")) +
                "[" + name + methodName + "(" + arguments + ")" + "]" +
                (returnValue != null && returnValue.getException() ? " throws " + returnValue.getType() : "");
    }

    public void setIsStatic(boolean aStatic) {
        this.isStaticCall = aStatic;
    }

    public boolean isStaticCall() {
        return isStaticCall;
    }

    public void setStaticCall(boolean staticCall) {
        isStaticCall = staticCall;
    }

    public DataEventWithSessionId getEntryProbe() {
        return entryProbe;
    }

    public void setEntryProbe(DataEventWithSessionId entryProbe) {
        this.entryTime = entryProbe.getNanoTime();
        this.entryProbe = entryProbe;
    }

    public int getCallStack() {
        return callStack;
    }

    public void setCallStack(int callStack) {
        this.callStack = callStack;
    }

    public int getMethodAccess() {
        return methodAccess;
    }

    public void setMethodAccess(int methodAccess) {
        this.methodAccess = methodAccess;
    }
}
