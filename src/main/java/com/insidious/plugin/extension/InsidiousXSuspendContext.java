package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Location;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.EventSet;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.connector.InsidiousThreadReferenceProxyImpl;
import com.insidious.plugin.extension.thread.InsidiousThreadReference;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.insidious.plugin.extension.thread.InsidiousXExecutionStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InsidiousXSuspendContext
        extends XSuspendContext {
    private static final Logger LOG = LoggerUtil.getInstance(InsidiousXSuspendContext.class);
    InsidiousJavaDebugProcess debugProcess;
    InsidiousXExecutionStack activeExecutionStack;
    List<InsidiousXExecutionStack> allExecutionStacks;
    private InsidiousThreadReferenceProxy thread;
    private final EventSet myEventSet;
    private final int suspendPolicy;
    private boolean isInternalEvent = false;
    private final boolean isBookmarkEvent;

    public InsidiousXSuspendContext(InsidiousJavaDebugProcess debugProcess, ThreadReference thread, int suspendPolicy) {
        this(debugProcess, thread, suspendPolicy, null, false);
    }


    public InsidiousXSuspendContext(InsidiousJavaDebugProcess debugProcess, ThreadReference thread, int suspendPolicy, boolean isBookmarkEvent) {
        this(debugProcess, thread, suspendPolicy, null, isBookmarkEvent);
    }


    public InsidiousXSuspendContext(InsidiousJavaDebugProcess debugProcess, InsidiousThreadReference thread, int suspendPolicy, EventSet event) {
        this(debugProcess, thread, suspendPolicy, event, false);
    }


    public InsidiousXSuspendContext(InsidiousJavaDebugProcess debugProcess, ThreadReference thread, int suspendPolicy, EventSet event, boolean isBookmarkEvent) {
        this.debugProcess = debugProcess;


        InsidiousThreadReference newThread = debugProcess.getConnector().getThreadReferenceWithUniqueId((int) thread.uniqueID());
        if (newThread == null) {
            newThread = (InsidiousThreadReference) thread;
        }
        this.thread = (InsidiousThreadReferenceProxy) new InsidiousThreadReferenceProxyImpl(debugProcess.getConnector(), newThread);
        this.suspendPolicy = suspendPolicy;
        this.myEventSet = event;
        this.isBookmarkEvent = isBookmarkEvent;
    }

    @Nullable
    public InsidiousXExecutionStack getActiveExecutionStack() {
        if (this.activeExecutionStack == null) {
            LOG.info("Lazy loading InsidiousXExecutionStack");
            this.activeExecutionStack = new InsidiousXExecutionStack(this, this.thread, this.debugProcess, true);
        }

        return this.activeExecutionStack;
    }

    public InsidiousThreadReferenceProxy getThreadReferenceProxy() {
        return this.thread;
    }

    public void setThread(InsidiousThreadReferenceProxy thread) {
        this.thread = thread;
    }

    public int getSuspendPolicy() {
        return this.suspendPolicy;
    }

    @NotNull
    public XExecutionStack[] getExecutionStacks() {
        if (this.allExecutionStacks == null) {
            this.allExecutionStacks = new ArrayList<>();
            for (InsidiousThreadReferenceProxy currThread : this.thread.getVirtualMachine().allThreads()) {
                if (this.thread != null) {
                    if (currThread == this.thread) {
                        this.allExecutionStacks.add(getActiveExecutionStack());
                        continue;
                    }
                    this.allExecutionStacks.add(new InsidiousXExecutionStack(this, currThread, this.debugProcess, false));
                }
            }
        }

        return this.allExecutionStacks.toArray((XExecutionStack[]) new InsidiousXExecutionStack[0]);
    }

    public InsidiousJavaDebugProcess getDebugProcess() {
        return this.debugProcess;
    }

    public InsidiousStackFrameProxy getFrameProxy() {
        return getActiveExecutionStack().getTopStackFrameProxy();
    }

    public InsidiousVirtualMachineProxy getInsidiousVirtualMachineProxy() {
        return this.debugProcess.getConnector();
    }

    public SourcePosition getSourcePosition() {
        SourcePosition myPosition = null;
        try {
            Location location = getFrameProxy().location();

            myPosition = ReadAction.compute(() -> getInsidiousVirtualMachineProxy().getPositionManager().getSourcePosition(location));


        } catch (NoDataException | com.intellij.debugger.engine.evaluation.EvaluateException noDataException) {
        }

        return myPosition;
    }

    public EventSet getEventSet() {
        return this.myEventSet;
    }

    public boolean isInternalEvent() {
        return this.isInternalEvent;
    }

    public void setIsInternalEvent(boolean isInternalEvent) {
        this.isInternalEvent = isInternalEvent;
    }

    public boolean isBookmarkEvent() {
        return this.isBookmarkEvent;
    }
}

