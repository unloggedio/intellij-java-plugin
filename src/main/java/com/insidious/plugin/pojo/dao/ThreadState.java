package com.insidious.plugin.pojo.dao;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "thread_state")
public class ThreadState {

    @DatabaseField(id = true)
    private int threadId;
    @DatabaseField
    private String callStack;
    @DatabaseField
    private String valueStack;
    @DatabaseField
    private String nextNewObjectStack;

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public String getCallStack() {
        return callStack;
    }

    public void setCallStack(String callStack) {
        this.callStack = callStack;
    }

    public String getValueStack() {
        return valueStack;
    }

    public void setValueStack(String valueStack) {
        this.valueStack = valueStack;
    }

    public String getNextNewObjectStack() {
        return nextNewObjectStack;
    }

    public void setNextNewObjectStack(String nextNewObjectStack) {
        this.nextNewObjectStack = nextNewObjectStack;
    }
}
