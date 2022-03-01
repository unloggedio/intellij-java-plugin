package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.application.ApplicationManager;
import org.slf4j.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.ThreadReference;
import com.insidious.plugin.extension.util.TimelineManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;


public final class InsidiousDebugProcessStarterListener
        implements InsidiousDebugProcessListener {
    private static final Logger LOGGER = LoggerUtil.getInstance(InsidiousDebugProcessStarterListener.class);

    private final InsidiousJavaDebugProcess debugProcess;

    private final InsidiousApplicationState state;


    public InsidiousDebugProcessStarterListener(InsidiousJavaDebugProcess debugProcess, InsidiousApplicationState state) {
        this.debugProcess = debugProcess;
        this.state = state;
    }


    public void paused(@NotNull XSuspendContext suspendContext) {
        LOGGER.info("Debug process has been paused.");
//        TimelineManager.getTimeline(this.debugProcess.getSession().getSessionName()).refreshBookmarks(suspendContext);
        if (this.state.getCommandSender() != null && suspendContext instanceof InsidiousXSuspendContext) {
            InsidiousXSuspendContext InsidiousXSuspendContext = (InsidiousXSuspendContext) suspendContext;
            this.state.getCommandSender()
                    .setLastThreadId(


                            (int) InsidiousXSuspendContext.getThreadReferenceProxy().getThreadReference().uniqueID());
        }
        ApplicationManager.getApplication().invokeLater(this::repaintTimelineToolWindow);
        this.debugProcess.stopPerformanceTiming();
        this.debugProcess.cancelJumpToAssignmentBreakpoint();
    }


    private void repaintTimelineToolWindow() {
//        TimelineToolWindow timelineToolWindow = TimelineToolWindowManager.getTimelineToolWindow(this.debugProcess.getSession().getSessionName());
//        doRepaintTimelineToolWindow(timelineToolWindow);
    }

//    private void doRepaintTimelineToolWindow(TimelineToolWindow timelineToolWindow) {
//        TimelinePanel timelinePanel = timelineToolWindow.getContent();
//        repaint((Component) timelinePanel);
//        repaint((Component) timelinePanel.getPositionPanel());
//    }

    private void repaint(Component component) {
        component.repaint();
        component.revalidate();
    }


    public void resumed(XSuspendContext suspendContext) {
    }


    public void threadStarted(@NotNull XDebugProcess proc, ThreadReference thread) {
        LOGGER.info(String.format("Thread (%s) started", thread.name()));

        ThreadReference initialThread = this.state.getInitialThread();
        if (initialThread == null || thread.uniqueID() < initialThread.uniqueID()) {
            this.state.setInitialThread(thread);
        }
    }


    public void threadStopped(@NotNull XDebugProcess proc, ThreadReference thread) {
        LOGGER.info(String.format("Thread (%s) stopped", thread.name()));
    }


    public void processAttached(@NotNull XDebugProcess process) {
        LOGGER.info("Process has been attached.");
        updateInsidiousCommandSender(this.state, this.debugProcess);
        if (!this.state.isErrored()) {
            ApplicationManager.getApplication().invokeLater(() -> initialiseTimeline(process));
        }
    }


    private void updateInsidiousCommandSender(InsidiousApplicationState state, InsidiousJavaDebugProcess debugProcess) {
        state.getCommandSender().setDebugProcess(debugProcess);
        LOGGER.debug("Updated commandSender with debugProcess");
    }

    private void initialiseTimeline(@NotNull XDebugProcess process) {
        synchronized (this.state) {
            CommandSender commandSender = this.state.getCommandSender();
            if (!this.state.isErrored() && commandSender != null) {
                initialiseTimeline(process, commandSender);
            }
        }
    }


    private void initialiseTimeline(@NotNull XDebugProcess process, CommandSender commandSender) {
        XDebugSession session = process.getSession();
        String sessionName = session.getSessionName();
        TimelineManager.createTimeline(sessionName, commandSender);
//        TimelineToolWindowManager.createTimelineToolWindow(sessionName,
//                InsidiousBookmarksToolWindowFactory.createTimelineToolWindow(session.getProject()));
        LOGGER.info("Timeline has been initialised.");
    }


    public void processDetached(@NotNull XDebugProcess process, boolean closedByUser) {
        LOGGER.info("Process has been detached.");
        synchronized (this.state) {
            CommandSender commandSender = this.state.getCommandSender();
            if (commandSender != null) {
                commandSender.stopThreads();
                this.state.setCommandSender(null);
            }
            processTimelineToolWindow(process);
        }
    }

    private void processTimelineToolWindow(@NotNull XDebugProcess process) {
//        TimelineToolWindow timelineToolWindow = TimelineToolWindowManager.getTimelineToolWindow(process
//                .getSession().getSessionName());
//        if (timelineToolWindow != null) {
//            processTimelineToolWindow(timelineToolWindow, process);
//        }
    }


//    private void processTimelineToolWindow(TimelineToolWindow timelineToolWindow, XDebugProcess process) {
//        ApplicationManager.getApplication()
//                .invokeLater(() -> {
//                    if (timelineToolWindow.isTimelineToolWindowOpen() && timelineToolWindow.isAllDebugSessionsStopped())
//                        timelineToolWindow.getForm().getTimelineToolWindow().hide(null);
//                    String sessionName = process.getSession().getSessionName();
//                    TimelineManager.removeTimeline(sessionName);
//                    TimelineToolWindowManager.removeTimelineToolWindow(sessionName);
//                });
//    }
}


