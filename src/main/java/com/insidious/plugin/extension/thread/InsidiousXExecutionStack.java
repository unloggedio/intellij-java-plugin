package com.insidious.plugin.extension.thread;

import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class InsidiousXExecutionStack extends XExecutionStack {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousXExecutionStack.class);

    private final InsidiousThreadReferenceProxy myThreadProxy;

    private final InsidiousJavaDebugProcess myDebugProcess;
    private final InsidiousXSuspendContext mySuspendContext;
    private XStackFrame topFrame;
    private InsidiousStackFrameProxy myTopStackFrameProxy;
    private boolean computedFrames = false;

    public InsidiousXExecutionStack(@NotNull InsidiousXSuspendContext suspendContext, @NotNull InsidiousThreadReferenceProxy thread, @NotNull InsidiousJavaDebugProcess debugProcess, boolean current) {
        super(
                calcRepresentation(thread.getThreadReference()),
                calcIcon(thread.getThreadReference(), current));
        this.mySuspendContext = suspendContext;
        this.myThreadProxy = thread;
        this.myDebugProcess = debugProcess;
        try {
            if (this.myThreadProxy.frameCount() > 0) {
                this.myTopStackFrameProxy = myThreadProxy.frame(0);
                this.topFrame = createXStackFrame(this.myTopStackFrameProxy.getStackFrame(), 0);
            }
        } catch (Exception e) {
            logger.info("failed to evaluate", e);
        }
    }

    private static String calcRepresentation(ThreadReference thread) {
        String name = "";
        try {
            name = thread.name();
            ThreadGroupReference gr = thread.threadGroup();
            String grname = (gr != null) ? gr.name() : null;
            String threadStatusText = DebuggerUtilsEx.getThreadStatusText(thread.status());

            if (grname != null && !"SYSTEM".equalsIgnoreCase(grname)) {


                name = "\"" + name + "\"@" + thread.uniqueID() + " in group \"" + grname + "\": " + threadStatusText;

            } else {


                name = "\"" + name + "\"@" + thread.uniqueID() + ": " + threadStatusText;
            }
        } catch (Exception exception) {
        }

        return name;
    }

    private static Icon calcIcon(ThreadReference thread, boolean current) {
        try {
            if (current)
                return thread.isSuspended() ?
                        AllIcons.Debugger.ThreadCurrent :
                        AllIcons.Debugger.ThreadRunning;
            if (thread.frameCount() > 0 && thread.isAtBreakpoint())
                return AllIcons.Debugger.ThreadAtBreakpoint;
            if (thread.isSuspended()) {
                return AllIcons.Debugger.ThreadSuspended;
            }
            return AllIcons.Debugger.ThreadRunning;
        } catch (Exception ex) {
            return AllIcons.Debugger.Db_obsolete;
        }
    }

    private XStackFrame createXStackFrame(StackFrame stackFrame, int index) throws NoDataException, EvaluateException, AbsentInformationException {
//        XSourcePosition xSourcePosition = ReadAction.compute(() -> {
//            SourcePosition position = this.myDebugProcess.getPositionManager().getSourcePosition(stackFrame.location());
//
//            SourcePosition.createFromLine()
//
//            return DebuggerUtilsEx.toXSourcePosition(position);
//        });

        Location location = stackFrame.location();
        String[] sourcePathParts = location.sourcePath().split("/");

        Collection<VirtualFile> file = ApplicationManager.getApplication().runReadAction(new Computable<Collection<VirtualFile>>() {
            @Override
            public Collection<VirtualFile> compute() {
                return FilenameIndex.getVirtualFilesByName(myDebugProcess.getProject(),
                        sourcePathParts[sourcePathParts.length - 1], GlobalSearchScope.projectScope(myDebugProcess.getProject()));
            }
        });

//        @NotNull Collection<VirtualFile> file = FilenameIndex.getVirtualFilesByName(myDebugProcess.getProject(),
//                sourcePathParts[sourcePathParts.length - 1], GlobalSearchScope.projectScope(myDebugProcess.getProject()));

        XSourcePosition xSourcePosition = null;
        if (file.isEmpty()) {
            InsidiousNotification.notifyMessage(
                    "Failed to map stack back to a source file [" + sourcePathParts[sourcePathParts.length - 1] + "]. File not found. If you think this is a bug, please report at support@bug.video", NotificationType.ERROR
            );
        } else {
            VirtualFile vf = file.stream().findFirst().get();
            xSourcePosition = XDebuggerUtil.getInstance().createPosition(vf, location.lineNumber());
        }


        return new InsidiousXStackFrame(
                this.myDebugProcess, this.mySuspendContext, this.myThreadProxy.frame(index), xSourcePosition
        );
    }

    @Nullable
    public XStackFrame getTopFrame() {
        return this.topFrame;
    }

    public void computeStackFrames(int firstFrameIndex, XExecutionStack.XStackFrameContainer container) {
        if (container.isObsolete())
            return;
        try {
            ThreadReference thread = this.myThreadProxy.getThreadReference();
            int status = thread.status();
            if (status == 0) {
                container.errorOccurred(DebuggerBundle.message("frame.panel.thread.finished"));
            } else if (!thread.isCollected() && thread.isSuspended()) {
                if (status != -1 && status != 5) {

                    try {
                        if (!this.computedFrames) {
                            Iterator<StackFrame> iterator = thread.frames().iterator();
                            int counter = -1;
                            while (iterator.hasNext()) {
                                counter++;
                                StackFrame stackFrame = iterator.next();
                                if (counter >= firstFrameIndex) {
                                    XStackFrame frame = createXStackFrame(stackFrame, counter);
                                    container.addStackFrames(
                                            Collections.singletonList(frame), false);
                                }
                            }
                            this.computedFrames = true;
                        }
                        container.addStackFrames(Collections.emptyList(), true);
                    } catch (IncompatibleThreadStateException | NoDataException e) {
                        container.errorOccurred(e.getMessage());
                    }
                }
            } else {
                container.errorOccurred("Frames not available for unsuspended thread");
            }
        } catch (Exception ex) {
            container.errorOccurred(ex.toString());
        }
    }

    public InsidiousStackFrameProxy getTopStackFrameProxy() {
        return this.myTopStackFrameProxy;
    }
}

