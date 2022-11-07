package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.intellij.openapi.util.text.Strings;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "method_call")
public class MethodCallExpression {

    @DatabaseField(id = true)
    private long id;

    @DatabaseField
    private String arguments;
    @DatabaseField
    private String methodName;
    @DatabaseField
    private boolean isStaticCall;


    @DatabaseField
    private boolean usesFields;
    @DatabaseField(foreign = true, index = true)
    private Parameter subject;

    @DatabaseField(foreign = true)
    private ProbeInfo entryProbeInfo;
    @DatabaseField(foreign = true)
    private Parameter returnValue;
    @DatabaseField(foreign = true)
    private DataEventWithSessionId entryProbe;
    @DatabaseField
    private int callStack;
    @DatabaseField(index = true)
    private int methodAccess;
    @DatabaseField
    private String argumentProbes;
    @DatabaseField
    private long returnDataEvent;


    public MethodCallExpression() {
    }

    public MethodCallExpression(
            String methodName,
            Parameter subject,
            List<Long> arguments,
            Parameter returnValue
    ) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = Strings.join(arguments, ",");
        this.returnValue = returnValue;
    }

    public static MethodCallExpression FromMCE(com.insidious.plugin.pojo.MethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return null;
        }

        MethodCallExpression methodCallExpression1 = new MethodCallExpression(
                methodCallExpression.getMethodName(),
                Parameter.fromParameter(methodCallExpression.getSubject()),
                methodCallExpression.getArguments()
                        .stream()
                        .map(e1 -> (long) e1.getValue())
                        .collect(Collectors.toList()),
                Parameter.fromParameter(methodCallExpression.getReturnValue())
        );
        methodCallExpression1.setEntryProbe(methodCallExpression.getEntryProbe());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setEntryProbeInfo(methodCallExpression.getEntryProbeInfo());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setId(methodCallExpression.getId());
        methodCallExpression1.setUsesFields(methodCallExpression.getUsesFields());
        methodCallExpression1.setArgumentProbes(methodCallExpression.getArgumentProbes()
                .stream().map(DataEventWithSessionId::getNanoTime).collect(Collectors.toList()));
        if (methodCallExpression.getReturnDataEvent() != null) {
            methodCallExpression1.setReturnDataEvent(methodCallExpression.getReturnDataEvent().getNanoTime());
        }
        return methodCallExpression1;
    }

    public static com.insidious.plugin.pojo.MethodCallExpression ToMCE(MethodCallExpression methodCallExpression) {
        com.insidious.plugin.pojo.MethodCallExpression methodCallExpression1 = new com.insidious.plugin.pojo.MethodCallExpression(
                methodCallExpression.getMethodName(), null, new LinkedList<>(), null, 0
        );
        methodCallExpression1.setEntryProbe(methodCallExpression.getEntryProbe());
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setId(methodCallExpression.getId());
        methodCallExpression1.setUsesFields(methodCallExpression.getUsesFields());

        return methodCallExpression1;
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

    public List<Long> getArguments() {
        if (arguments == null || arguments.length() < 1) {
            return List.of();
        }
        List<Long> argumentList = new LinkedList<>();
        String[] args = arguments.split(",");
        for (String arg : args) {
            argumentList.add(Long.valueOf(arg));
        }
        return argumentList;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
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

        String owner = null;
        if (subject != null && subject.getProbeInfo() != null) {
            String methodCallOwner = subject.getProbeInfo().getAttribute("Owner", null);
            if (methodCallOwner != null) {
                owner = "[" + methodCallOwner + "]";
            }
        }
        String name = "";
        if (subject != null) {
            name = subject.getName() + ".";
        }
        return ((returnValue == null || returnValue.getName() == null || returnValue.getException()) ?
                "" : (returnValue.getName() + " [" + returnValue.getValue() + "]" + "  == ")) +
                "[" + name + methodName + "(" + (arguments == null ? "0" : arguments.length()) + " arguments)" + "]" +
                (returnValue != null && returnValue.getException() ? " throws " + returnValue.getType() : "") +
                (owner == null ? "" : owner);
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

    public List<Long> getArgumentProbes() {
        if (argumentProbes == null || argumentProbes.length() < 1) {
            return List.of();
        }
        List<Long> argsProbeList = new LinkedList<>();
        String[] probeIds = argumentProbes.split(",");
        for (String probeId : probeIds) {
            argsProbeList.add(Long.valueOf(probeId));
        }
        return argsProbeList;
    }

    public void setArgumentProbes(List<Long> argumentProbes) {
        this.argumentProbes = Strings.join(argumentProbes, ",");
    }

    public long getReturnDataEvent() {
        return returnDataEvent;
    }

    public void setReturnDataEvent(long returnDataEvent) {
        this.returnDataEvent = returnDataEvent;
    }
    public boolean getUsesFields() {
        return usesFields;
    }

    public void setUsesFields(boolean usesFields) {
        this.usesFields = usesFields;
    }

}
