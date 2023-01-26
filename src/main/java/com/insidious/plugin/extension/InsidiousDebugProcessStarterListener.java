package com.insidious.plugin.extension;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.ThreadReference;
import com.insidious.plugin.extension.util.TimelineManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;


public final class InsidiousDebugProcessStarterListener
        implements InsidiousDebugProcessListener {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousDebugProcessStarterListener.class);

    private final InsidiousJavaDebugProcess debugProcess;

    private final InsidiousApplicationState state;
    private RemoteConnection myConnection;


    public InsidiousDebugProcessStarterListener(
            InsidiousJavaDebugProcess debugProcess,
            InsidiousApplicationState state,
            RemoteConnection myConnection) {
        this.debugProcess = debugProcess;
        this.state = state;
        this.myConnection = myConnection;
    }


    public void paused(@NotNull XSuspendContext suspendContext) {
        logger.info("Debug process has been paused.");
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
        logger.info(String.format("resumed Thread (%s) started", suspendContext.toString()));

    }


    public void threadStarted(@NotNull XDebugProcess proc, ThreadReference thread) {
        logger.info(String.format("Thread (%s) started", thread.name()));

        ThreadReference initialThread = this.state.getInitialThread();
        if (initialThread == null || thread.uniqueID() < initialThread.uniqueID()) {
            this.state.setInitialThread(thread);
        }
    }


    public void threadStopped(@NotNull XDebugProcess proc, ThreadReference thread) {
        logger.info(String.format("Thread (%s) stopped", thread.name()));
    }


    public void processAttached(@NotNull XDebugProcess process) {
        logger.info("Process has been attached.");
        updateInsidiousCommandSender(this.state, this.debugProcess);

        ProcessHandler processHandler = process.getProcessHandler();

        String message = DebuggerBundle.message("status.connected", myConnection + "\n");
        processHandler.notifyTextAvailable(message, ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\nFind trace points to start debugging in the VideoBug tool window", ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\nAfter fetching a session from trace point, you can step back and forward using the <- and -> arrow buttons above", ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\nYou can also assign it a keyboard shortcut, just like F8", ProcessOutputTypes.SYSTEM);


        processHandler.notifyTextAvailable("\n\n", ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("        _     _              ___             \n" +
                " /\\   /(_) __| | ___  ___   / __\\_   _  __ _ \n" +
                " \\ \\ / | |/ _` |/ _ \\/ _ \\ /__\\/| | | |/ _` |\n" +
                "  \\ V /| | (_| |  __| (_) / \\/  | |_| | (_| |\n" +
                "   \\_/ |_|\\__,_|\\___|\\___/\\_____/\\__,_|\\__, |\n" +
                "                                       |___/ ", ProcessOutputTypes.SYSTEM);

        InsidiousService insidiousService = ApplicationManager.getApplication().getService(InsidiousService.class);

        processHandler.notifyTextAvailable("                                        \n" +
                "                 .°...°°°°..            \n" +
                "                  ....     .°*°.        \n" +
                "      .     .*O##@@@@@@##Oo°   *o°      \n" +
                "     O*   o@@@@@@@@@@@@@@@@@@O.  *o     \n" +
                "    #°  °#oo#@#Oo**°°°*oO#@#o*#O  °#    \n" +
                "   oo  .@#*                  °O@o  oo   \n" +
                "   #.  o@#@o   OO°     o#o   @@@#  .#   \n" +
                "   oo  .@#@.   ..       .    O@@o  oo   \n" +
                "    #°  °@@                  o@o  °#    \n" +
                "     o*   *                  ..  *o     \n" +
                "      °o*                      *o°      \n" +
                "        .°*°.              .°*°.        \n" +
                "            ..°°°°°°°°°°°°..            \n" +
                "                                        ", ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\n The agent jar is available at: " + insidiousService.getVideoBugAgentPath(), ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\n Set the following as vm option parameter", ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\n" + insidiousService.getJavaAgentString(), ProcessOutputTypes.SYSTEM);
        processHandler.notifyTextAvailable("\n", ProcessOutputTypes.SYSTEM);


        if (!this.state.isErrored()) {
            ApplicationManager.getApplication().invokeLater(() -> initialiseTimeline(process));
        }
    }


    private void updateInsidiousCommandSender(InsidiousApplicationState state, InsidiousJavaDebugProcess debugProcess) {
        state.getCommandSender().setDebugProcess(debugProcess);
        logger.debug("Updated commandSender with debugProcess");
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
        logger.info("Timeline has been initialised.");
    }


    public void processDetached(@NotNull XDebugProcess process, boolean closedByUser) {
        logger.info("Process has been detached.");

        ProcessHandler processHandler = process.getProcessHandler();

        String message = DebuggerBundle.message("status.disconnected", myConnection + "\n");
        processHandler.notifyTextAvailable(message, ProcessOutputTypes.SYSTEM);

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


