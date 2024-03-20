package com.insidious.plugin.pojo.dao;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.StringUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "method_call")
public class MethodCallExpression implements MethodCallExpressionInterface {

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
    @DatabaseField
    private long subject_id;

    @DatabaseField
    private int entryProbeInfo_id;
    @DatabaseField
    private long returnValue_id;
    @DatabaseField
    private long entryProbe_id;
    @DatabaseField
    private int callStack;
    @DatabaseField
    private long callTimeNano;
    @DatabaseField(index = true)
    private int methodAccess;
    @DatabaseField
    private String argumentProbes;
    @DatabaseField
    private long returnDataEvent;
    @DatabaseField(index = true)
    private long parentId;
    @DatabaseField(index = true)
    private int methodDefinitionId;
    @DatabaseField(index = true)
    private int threadId;
    @DatabaseField
    private long returnNanoTime;
    @DatabaseField
    private long enterNanoTime;

    public MethodCallExpression() {
    }

    public MethodCallExpression(
            String methodName,
            long subject,
            List<Long> arguments,
            long returnValue_id,
            int callStack
    ) {
        this.methodName = methodName;
        this.subject_id = subject;
        this.arguments = StringUtils.join(arguments, ",");
        this.returnValue_id = returnValue_id;
        this.callStack = callStack;
    }

    public static MethodCallExpression FromMCE(IncompleteMethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return null;
        }

        long returnParameterValue = methodCallExpression.getReturnValue_id();
        long subjectParameterValue = methodCallExpression.getSubject();
        MethodCallExpression methodCallExpression1 = new MethodCallExpression(
                methodCallExpression.getMethodName(),
                subjectParameterValue,
                methodCallExpression.getArguments(),
                returnParameterValue,
                methodCallExpression.getCallStack()
        );
        methodCallExpression1.setMethodDefinitionId(methodCallExpression.getMethodDefinitionId());
        methodCallExpression1.setEntryProbeId(methodCallExpression.getEntryProbe_id());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setEntryProbeInfoId(methodCallExpression.getEntryProbeInfo_id());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setThreadId(methodCallExpression.getThreadId());
        methodCallExpression1.setId(methodCallExpression.getId());
        methodCallExpression1.setParentId(methodCallExpression.getParentId());
        methodCallExpression1.setUsesFields(methodCallExpression.getUsesFields());
        methodCallExpression1.setArgumentProbes(methodCallExpression.getArgumentProbesString());
        methodCallExpression1.setReturnDataEvent(methodCallExpression.getReturnDataEvent());
        methodCallExpression1.setEnterNanoTime(methodCallExpression.getEnterNanoTime());
        methodCallExpression1.setReturnNanoTime(methodCallExpression.getReturnNanoTime());
        return methodCallExpression1;
    }

    public static IncompleteMethodCallExpression IncompleteFromMCE(MethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return null;
        }

        long returnParameterValue = methodCallExpression.getReturnValue_id();
        long subjectParameterValue = methodCallExpression.getSubject();
        IncompleteMethodCallExpression methodCallExpression1 = new IncompleteMethodCallExpression(
                methodCallExpression.getMethodName(),
                subjectParameterValue,
                methodCallExpression.getArgumentsString(),
                returnParameterValue
        );
        methodCallExpression1.setEntryProbeId(methodCallExpression.getEntryProbe_id());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setEntryProbeInfoId(methodCallExpression.getEntryProbeInfo_id());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setThreadId(methodCallExpression.getThreadId());
        methodCallExpression1.setMethodDefinitionId(methodCallExpression.getMethodDefinitionId());
        methodCallExpression1.setId(methodCallExpression.getId());
        methodCallExpression1.setParentId(methodCallExpression.getParentId());
        methodCallExpression1.setUsesFields(methodCallExpression.getUsesFields());
        methodCallExpression1.setArgumentProbes(methodCallExpression.getArgumentProbesString());
        methodCallExpression1.setReturnDataEvent(methodCallExpression.getReturnDataEvent());
        methodCallExpression1.setEnterNanoTime(methodCallExpression.getEnterNanoTime());
        methodCallExpression1.setReturnNanoTime(methodCallExpression.getReturnNanoTime());

        return methodCallExpression1;
    }

    public static com.insidious.plugin.pojo.MethodCallExpression ToMCEFromDao(MethodCallExpressionInterface methodCallExpression) {
        com.insidious.plugin.pojo.MethodCallExpression methodCallExpression1 = new com.insidious.plugin.pojo.MethodCallExpression(
                methodCallExpression.getMethodName(), null, new LinkedList<>(), null, 0
        );
        methodCallExpression1.setStaticCall(methodCallExpression.isStaticCall());
        methodCallExpression1.setCallStack(methodCallExpression.getCallStack());
        methodCallExpression1.setMethodAccess(methodCallExpression.getMethodAccess());
        methodCallExpression1.setThreadId(methodCallExpression.getThreadId());
        methodCallExpression1.setId(methodCallExpression.getId());
        methodCallExpression1.setParentId(methodCallExpression.getParentId());
        methodCallExpression1.setUsesFields(methodCallExpression.getUsesFields());
        methodCallExpression1.setMethodDefinitionId(methodCallExpression.getMethodDefinitionId());
        methodCallExpression1.setEnterNanoTime(methodCallExpression.getEnterNanoTime());
        methodCallExpression1.setReturnNanoTime(methodCallExpression.getReturnNanoTime());

        return methodCallExpression1;
    }

    public long getCallTimeNano() {
        return callTimeNano;
    }

    public void setCallTimeNano(long callTimeNano) {
        this.callTimeNano = callTimeNano;
    }

    public long getEnterNanoTime() {
        return enterNanoTime;
    }

    public void setEnterNanoTime(long enterNanoTime) {
        this.enterNanoTime = enterNanoTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getEntryProbeInfo_id() {
        return entryProbeInfo_id;
    }

    public void setEntryProbeInfoId(int probeId) {
        this.entryProbeInfo_id = probeId;
    }

    public long getSubject() {
        return subject_id;
    }

    public void setSubject(long subjectValue) {
        this.subject_id = subjectValue;
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

    public String getArgumentsString() {
        return arguments;
    }

    public long getReturnValue_id() {
        return returnValue_id;
    }

    public void setReturnValue_id(long returnParameterValue) {
        this.returnValue_id = returnParameterValue;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setIsStatic(boolean aStatic) {
        this.isStaticCall = aStatic;
    }

    public boolean isMethodPublic() {
        return (methodAccess & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC;
    }


    public boolean isStaticCall() {
        return isStaticCall;
    }

    public void setStaticCall(boolean staticCall) {
        isStaticCall = staticCall;
    }

    public long getEntryProbe_id() {
        return entryProbe_id;
    }

    public void setEntryProbeId(long eventId) {
        this.entryProbe_id = eventId;
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

    public void setArgumentProbes(String argumentProbes) {
        this.argumentProbes = argumentProbes;
    }

    public String getArgumentProbesString() {
        return argumentProbes;
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

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public int getMethodDefinitionId() {
        return methodDefinitionId;
    }

    public void setMethodDefinitionId(int methodDefinitionId) {
        this.methodDefinitionId = methodDefinitionId;
    }

    @Override
    public int getThreadId() {
        return threadId;
    }

    @Override
    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public void addArgument(long parameterValue) {
        if (arguments == null || arguments.length() == 0) {
            arguments = String.valueOf(parameterValue);
        } else {
            arguments = arguments + "," + parameterValue;
        }
    }

    public void addArgumentProbe(long eventId) {
        if (argumentProbes == null || argumentProbes.length() == 0) {
            argumentProbes = String.valueOf(eventId);
        } else {
            argumentProbes = argumentProbes + "," + eventId;
        }
    }

    @Override
    public String toString() {
        return "[" + id + "]" + methodName + "() entryId = " + entryProbe_id;
    }

    public long getReturnNanoTime() {
        return returnNanoTime;
    }

    public void setReturnNanoTime(long returnNanoTime) {
        this.returnNanoTime = returnNanoTime;
        this.callTimeNano = returnNanoTime - enterNanoTime;
    }
}
