package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.debugger.ui.breakpoints.RunToCursorBreakpoint;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.application.ReadAction;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Alarm;
import com.intellij.util.EventDispatcher;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.sun.jdi.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.extension.connector.InsidiousThreadsDebuggerTree;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousNodeManagerImpl;
import com.insidious.plugin.extension.jwdp.RequestMessage;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.smartstep.MethodFilter;
import com.insidious.plugin.extension.thread.InsidiousThreadReference;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.network.pojo.ExecutionSession;
import com.insidious.plugin.network.pojo.exceptions.APICallException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InsidiousJavaDebugProcess extends XDebugProcess {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousJavaDebugProcess.class);
    private static final Map<Project, InsidiousJavaDebugProcess> instances = new HashMap<>();
    protected final EventDispatcher<InsidiousDebugProcessListener> myDebugProcessDispatcher = EventDispatcher.create(InsidiousDebugProcessListener.class);
    protected final EventDispatcher<JdwpRequestMessageListener> jdwpEventDispatcher = EventDispatcher.create(JdwpRequestMessageListener.class);
    private final Alarm myStatusUpdateAlarm = new Alarm();
    private final InsidiousJavaSmartStepIntoActionHandler mySmartStepIntoActionHandler;
    private final InsidiousJDIConnector connector;
    private final InsidiousNodeManagerImpl insidiousNodeManager;
    private final InsidiousCompoundPositionManager myPositionManager;
    private final InsidiousBreakpointHandler[] myBreakpointHandlers;
    private final RemoteConnection myConnection;
    private final JavaDebuggerEditorsProvider myEditorsProvider;
    public DirectionType lastDirectionType = DirectionType.FORWARDS;
    private ExecutionResult executionResult;
    private ConsoleView executionConsole;
    private Sdk alternativeJre;
    private GlobalSearchScope mySearchScope;
    private InsidiousDebuggerEventThread insidiousEventReaderthread;
    private RunToCursorBreakpoint myRunToCursorBreakpoint;
    private Location myRunToCursorLocation;
    private TelemetryTracker telemetryTracker;
    private SyntheticFieldBreakpoint myJumpToAssignmentBreakpoint;
    private XSuspendContext suspendedContext;


    protected InsidiousJavaDebugProcess(
            @NotNull XDebugSession session,
            @NotNull final RemoteConnection connection) throws APICallException, IOException {
        super(session);
        this.myEditorsProvider = new JavaDebuggerEditorsProvider();

        this.connector = new InsidiousJDIConnector(this,
                connection.getClient());
        session.getProject().getService(InsidiousService.class).setConnector(this.connector);

        InsidiousThreadsDebuggerTree tree = new InsidiousThreadsDebuggerTree(getSession().getProject(), this);
        this.insidiousNodeManager = new InsidiousNodeManagerImpl(getSession().getProject(), tree, this);
        this.myPositionManager = ReadAction.compute(() -> new InsidiousCompoundPositionManager(new InsidiousPositionManager(this)));

        InsidiousBreakpointHandler[] handlers = {
                new InsidiousBreakpointHandler.InsidiousJavaLineBreakpointHandler(this),
                new InsidiousBreakpointHandler.InsidiousJavaExceptionBreakpointHandler(this),
                new InsidiousBreakpointHandler.InsidiousJavaFieldBreakpointHandler(this),
                new InsidiousBreakpointHandler.InsidiousJavaMethodBreakpointHandler(this),
                new InsidiousBreakpointHandler.InsidiousJavaWildcardBreakpointHandler(this)
        };


        this.myBreakpointHandlers = handlers;
        this.myConnection = connection;
        instances.put(getProject(), this);

        this.myDebugProcessDispatcher.addListener(new InsidiousDebugProcessListener() {

            public void processDetached(@NotNull XDebugProcess process, boolean closedByUser) {
                ProcessHandler processHandler = process.getProcessHandler();

                String message = DebuggerBundle.message("status.disconnected", myConnection + "\n");
                processHandler.notifyTextAvailable(message, ProcessOutputTypes.SYSTEM);
            }


            public void processAttached(@NotNull XDebugProcess process) {
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


                processHandler.notifyTextAvailable("                                                                                                                                                                                                                                                   \n" +
                        "                                                                                                            .-==+++++++++++++++++++=:-.                                                                                                            \n" +
                        "                                                                                                      .++++++++++++++++++++++++++++++++++++++++++++=:-                                                                                             \n" +
                        "                                                                                                       :+++++++++++++++++++++++++++++++++++++++++++++++++++++=-                                                                                    \n" +
                        "                                                                                                                                               .:==+++++++++++++++++=.                                                                             \n" +
                        "                                                                                                                                                          -+++++++++++++++=.                                                                       \n" +
                        "                                                                                                                                                                 -=+++++++++++++.                                                                  \n" +
                        "                                                                                                                                                                       -=+++++++++++:                                                              \n" +
                        "                                                                                                                                                                            .+++++++++++=                                                          \n" +
                        "                                                                                                                ..-::==+++++++==::-..                                            :++++++++++-                                                      \n" +
                        "                                                                                                   -==++++++++++++++++++++++++++++++++++++++++++=:                                   =+++++++++=                                                   \n" +
                        "                                                                                           :+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++:                              .=++++++++=                                                \n" +
                        "                                                                                     :+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++:                            =++++++++=                                             \n" +
                        "                                                                                =+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++=                          +++++++++-                                          \n" +
                        "                                                                           .=+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++=.                       .=+++++++=                                        \n" +
                        "                                      =++=.                             =+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++=                       :+++++++=                                      \n" +
                        "                                   .+++++++=                        -+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++-                     -++++++++                                    \n" +
                        "                                  ++++++++                       -+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++-                    .++++++++                                  \n" +
                        "                                =+++++++                       =+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++=                    .+++++++=                                \n" +
                        "                              :+++++++-                     -+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++-                   -+++++++:                              \n" +
                        "                             =++++++=                     :+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++:                   =++++++=                             \n" +
                        "                           -+++++++.                    :+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++:                  .+++++++-                           \n" +
                        "                          =++++++=                    -+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++-                  =++++++:                          \n" +
                        "                         =++++++=                    +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++                  =++++++=                         \n" +
                        "                        +++++++-                   :++++++++++++++++++++++++++++++++++++++++++++++++++++++++=:-.                      .::=+++++++++++++++++++++++++++++++++++++++++++++++++++++++:                 -+++++++                        \n" +
                        "                       +++++++-                   =++++++++++=        .=++++++++++++++++++++++++++=-                                              :++++++++++++++++++++++++++=         =++++++++++=                 -+++++++                       \n" +
                        "                      =++++++-                   =+++++++++=              -++++++++++++++++=:                                                            =++++++++++++++++-              :+++++++++=                 :++++++=                      \n" +
                        "                     -++++++=                   =++++++++++:                  =+++++++=.                                                                      -=++++++:                  :++++++++++=                 =++++++-                     \n" +
                        "                     +++++++                   =+++++++++++++=                                                                                                                         =+++++++++++++=                 +++++++                     \n" +
                        "                    :++++++-                  -+++++++++++++++++=.                                                                                                                 .=+++++++++++++++++-                -++++++:                    \n" +
                        "                    ++++++=                   ++++++++++++++++++++++=                                                                                                           =++++++++++++++++++++++                 =++++++                    \n" +
                        "                   -++++++:                  :+++++++++++++++++++++++++.                                                                                                      +++++++++++++++++++++++++:                :++++++-                   \n" +
                        "                   :++++++.                  ++++++++++++++++++++++++++-                                                                                                      ++++++++++++++++++++++++++                .++++++:                   \n" +
                        "                   =++++++                   +++++++++++++++++++++++++=                     =++==   :=++=.                                     .=++++++++=                    .+++++++++++++++++++++++++                 ++++++=                   \n" +
                        "                   +++++++                  .+++++++++++++++++++++++++:                   +++++++++++++++++                                  =++++++++++++++=                  +++++++++++++++++++++++++                 +++++++                   \n" +
                        "                   +++++++                  .+++++++++++++++++++++++++                     +++++++++++++++.                                .++++++++++++++++++                 -++++++++++++++++++++++++                 +++++++                   \n" +
                        "                   =++++++                   +++++++++++++++++++++++++                     =++++++++++++++                                 .++++++++++++++++++                  ++++++++++++++++++++++++                 ++++++=                   \n" +
                        "                   :++++++.                  ++++++++++++++++++++++++:                    +++++++++++++++++                                 .+++++++++++++++=                   =+++++++++++++++++++++++                .++++++:                   \n" +
                        "                   -++++++:                  :+++++++++++++++++++++++.                     -+++++.  +++++:                                     =++++++++++-                     -++++++++++++++++++++++:                :++++++-                   \n" +
                        "                    ++++++=                   +++++++++++++++++++++++                                                                                                           .++++++++++++++++++++++                 +++++++                    \n" +
                        "                    :++++++-                  -++++++++++++++++++++++                                                                                                            =++++++++++++++++++++-                -++++++:                    \n" +
                        "                     =++++++                   =++++++++++++++++++++:                                                                                                            :+++++++++++++++++++=                 +++++++                     \n" +
                        "                     -++++++=                   =+++++++++++++++++++-                                                                                                            .++++++++++++++++++=                 =++++++-                     \n" +
                        "                      =++++++:                   =++++++++++++++++++                                                                                                              +++++++++++++++++=                 :++++++=                      \n" +
                        "                       +++++++-                   =+++++++++++++++++                                                                                                              =+++++++++++++++=                 -+++++++                       \n" +
                        "                        +++++++-                   :++++++++++++++++                                                                                                              :++++++++++++++:                 -+++++++                        \n" +
                        "                         =++++++=                    ++++++++++++++:                                                                                                              .+++++++++++++                  =++++++=                         \n" +
                        "                          :++++++=                    -++++++++++++-                                                                                                               =++++++++++-                  =++++++:                          \n" +
                        "                           -+++++++.                    :++++++++++.                                                                                                               =++++++++:                  -+++++++-                           \n" +
                        "                             =++++++=                     :++++++++                                                                                                                :++++++:                   =++++++=                             \n" +
                        "                              :+++++++-                     -+++++=                                                                                                                .++++-                   -+++++++-                              \n" +
                        "                                =+++++++.                      =++:                                                                                                                 ==                    .+++++++=                                \n" +
                        "                                  ++++++++.                      --                                                                                                                                     .++++++++                                  \n" +
                        "                                    ++++++++-                                                                                                                                                         -++++++++                                    \n" +
                        "                                      =+++++++:                                                                                                                                                     :+++++++=                                      \n" +
                        "                                        =++++++++.                                                                                                                                               .++++++++=                                        \n" +
                        "                                          -+++++++++                                                                                                                                           +++++++++-                                          \n" +
                        "                                             =++++++++=                                                                                                                                     =++++++++=                                             \n" +
                        "                                                =++++++++=.                                                                                                                             .=++++++++=                                                \n" +
                        "                                                   =+++++++++=                                                                                                                       =+++++++++=                                                   \n" +
                        "                                                      -++++++++++:                                                                                                               :++++++++++-                                                      \n" +
                        "                                                          =+++++++++++.                                                                                                     .+++++++++++=                                                          \n" +
                        "                                                              :+++++++++++=-                                                                                           -=+++++++++++:                                                              \n" +
                        "                                                                  .+++++++++++++=-                                                                               -=++++++++++++=.                                                                  \n" +
                        "                                                                       .=+++++++++++++++-                                                                 :+++++++++++++++=-                                                                       \n" +
                        "                                                                             .=+++++++++++++++++==:.                                           .:==+++++++++++++++++=-                                                                             \n" +
                        "                                                                                    -=+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++=.                                                                                    \n" +
                        "                                                                                             -:=+++++++++++++++++++++++++++++++++++++++++++++++++++=:.                                                                                             \n" +
                        "                                                                                                            .-:=+++++++++++++++++++=:-.                                                                                                            \n" +
                        "                                                                                                                                                                                                                                                   \n" +
                        "                                                                                                                                                                                                                                                   ", ProcessOutputTypes.SYSTEM);

            }
        });
        this.mySmartStepIntoActionHandler = new InsidiousJavaSmartStepIntoActionHandler(this);
        session.setPauseActionSupported(true);
    }

    public static InsidiousJavaDebugProcess create(@NotNull XDebugSession session,
                                                   @NotNull RemoteConnection connection)
            throws APICallException, IOException {
        logger.info("Creating InsidiousJavaDebugProcess with port " + connection.getClient().getEndpoint());

        InsidiousJavaDebugProcess debugProcess = new InsidiousJavaDebugProcess(session, connection);

//        session.getProject().getService(InsidiousService.class).setDebugSession(session);
//        session.getProject().getService(InsidiousService.class).setDebugProcess(debugProcess);
        return debugProcess;
    }

    public static InsidiousJavaDebugProcess getInstance(String runProfileName) {
        for (InsidiousJavaDebugProcess InsidiousJavaDebugProcess : instances.values()) {
            if (InsidiousJavaDebugProcess.getRunProfileName().equals(runProfileName)) {
                return InsidiousJavaDebugProcess;
            }
        }
        return null;
    }

    public static Optional<InsidiousJavaDebugProcess> getInstanceWithTelemetrySessionId(String sessionId) {
        return instances.values().stream()
                .filter(p -> (p.telemetryTracker != null && sessionId.equals(p.telemetryTracker.getSessionId())))
                .findFirst();
    }

    @Override
    public void sessionInitialized() {
        getProject().getService(InsidiousService.class).setDebugSession(getSession());
        getProject().getService(InsidiousService.class).setDebugProcess(this);
        getProject().getService(InsidiousService.class).focusExceptionWindow();
    }

    @NotNull
    public XDebuggerEditorsProvider getEditorsProvider() {
        return this.myEditorsProvider;
    }

    public void addDebugProcessListener(InsidiousDebugProcessListener listener) {
        this.myDebugProcessDispatcher.addListener(listener);
    }

    public void removeDebugProcessListener(InsidiousDebugProcessListener listener) {
        this.myDebugProcessDispatcher.removeListener(listener);
    }

    public void addJdwpEventListener(JdwpRequestMessageListener listener) {
        this.jdwpEventDispatcher.addListener(listener);
    }

    public void removeJdwpEventListener(JdwpRequestMessageListener listener) {
        this.jdwpEventDispatcher.removeListener(listener);
    }

    public void requestMessageReceived(RequestMessage requestMessage) {
        this.jdwpEventDispatcher.getMulticaster().requestMessageReceived(requestMessage);
    }

    public void attachVM(String timeout) throws IOException {
        try {
            logger.info(String.format("Attaching to VM on endpoint [%s]", this.myConnection.getClient().getEndpoint()));
            this.connector.attachVirtualMachine(this.myConnection
                            .getHostName(), this.myConnection.getClient().getEndpoint(),
                    this.myConnection.isUseSockets(), false, timeout);

            this.connector.createThreadStartRequest();

            this.insidiousEventReaderthread = new InsidiousDebuggerEventThread(this);
            this.myDebugProcessDispatcher.getMulticaster().processAttached(this);
            logger.info("Process Attached");
            beginTelemetrySession();
        } catch (Exception ex) {
            logger.error("Couldn't attach to target VM:", ex);
            if (!getState().isErrored()) {
                throw new IOException(ex);
            }
        }
    }

    public void startInsidiousEventReaderThread() {
        if (this.insidiousEventReaderthread != null) {
            this.insidiousEventReaderthread.startListening();
        }
    }

    public InsidiousJDIConnector getConnector() {
        return this.connector;
    }

    private VirtualMachine getVirtualMachine() {
        return this.connector.getVirtualMachine();
    }

    @Nullable
    protected ProcessHandler doGetProcessHandler() {
        if (this.executionResult != null) {
            return this.executionResult.getProcessHandler();
        }
        return super.doGetProcessHandler();
    }

    public void setExecutionResult(ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    @NotNull
    public ExecutionConsole createConsole() {
        if (this.executionConsole == null) {
            if (this.executionResult != null) {
                this.executionConsole = (ConsoleView) this.executionResult.getExecutionConsole();
            } else {
                this.executionConsole = (ConsoleView) super.createConsole();
            }
        }
        return this.executionConsole;
    }

    public InsidiousNodeManagerImpl getInsidiousNodeManager() {
        return this.insidiousNodeManager;
    }

    public Sdk getAlternativeJre() {
        return this.alternativeJre;
    }

    public void setAlternativeJre(Sdk alternativeJre) {
        this.alternativeJre = alternativeJre;
    }

    public GlobalSearchScope getSearchScope() {
        return this.mySearchScope;
    }

    public void setSearchScope(GlobalSearchScope mySearchScope) {
        this.mySearchScope = mySearchScope;
    }

    public InsidiousCompoundPositionManager getPositionManager() {
        return this.myPositionManager;
    }

    @NotNull
    public InsidiousBreakpointHandler[] getBreakpointHandlers() {
        return this.myBreakpointHandlers;
    }

    public void showStatusText(String text) {
        this.myStatusUpdateAlarm.cancelAllRequests();
        this.myStatusUpdateAlarm.addRequest(() -> {
            if (!getSession().getProject().isDisposed())
                StatusBarUtil.setStatusBarInfo(getSession().getProject(), text);
        }, 50);
    }

    public void stop() {
        closeProcess(true);
    }

    public void resumeAutomatic(@NotNull XSuspendContext context) {
        InsidiousXSuspendContext InsidiousContext = null;
        boolean isInternalEventOldValue = false;
        if (context instanceof InsidiousXSuspendContext) {
            InsidiousContext = (InsidiousXSuspendContext) context;
            isInternalEventOldValue = InsidiousContext.isInternalEvent();
            InsidiousContext.setIsInternalEvent(true);
        }
        resume(context);
        if (InsidiousContext != null && !isInternalEventOldValue) {
            ((InsidiousXSuspendContext) context).setIsInternalEvent(false);
        }
    }

    public void resume(@Nullable XSuspendContext context) {
        if (getLastDirectionType() == DirectionType.BACKWARDS) {
            addPerformanceAction(context, "reverse_resume");
        } else {
            addPerformanceAction(context, "resume");
        }
        this.connector.resume();
    }

    public void startPausing() {
        super.startPausing();
//        getVirtualMachine().suspend();
        notifySuspended();
    }

    public void notifySuspended() {
        InsidiousApplicationState state = getProcessHandler().getUserData(InsidiousApplicationState.KEY);
        CommandSender commandSender = state.getCommandSender();
        InsidiousXSuspendContext suspendContext = null;
        if (commandSender.getLastThreadId() > 0) {

            InsidiousThreadReference thread = getConnector().getThreadReferenceWithUniqueId(commandSender.getLastThreadId());
            suspendContext = new InsidiousXSuspendContext(this, thread, 2);
        } else if (state.getInitialThread() != null) {


            suspendContext = new InsidiousXSuspendContext(this, state.getInitialThread(), 2);
        } else {
            List<InsidiousThreadReferenceProxy> allThreads = getConnector().allThreads();
            if (allThreads.size() > 0) {
                InsidiousThreadReferenceProxy minThread = allThreads.get(0);
                for (int i = 1; i < allThreads.size(); i++) {
                    if (allThreads.get(i).getThreadReference().uniqueID() < minThread.getThreadReference().uniqueID()) {
                        minThread = allThreads.get(i);
                    }
                }


                suspendContext = new InsidiousXSuspendContext(this, minThread.getThreadReference(), 2);
            } else {
                logger.debug(
                        "Cannot pause now since the threads are not created yet.");
            }
        }

        if (suspendContext != null) {
            this.insidiousEventReaderthread.notifyPaused(suspendContext);

            ((XDebugSessionImpl) getSession()).positionReached(suspendContext, true);
        }
    }

    public void setRunToCursorLocation(Location myRunToCursorLocation) {
        this.myRunToCursorLocation = myRunToCursorLocation;
    }

    public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
        this.myRunToCursorBreakpoint = DebuggerManagerEx.getInstanceEx(getProject()).getBreakpointManager().addRunToCursorBreakpoint(position, true);
        showStatusText(DebuggerBundle.message("status.run.to.cursor"));
        if (this.myRunToCursorBreakpoint == null) {
            return;
        }


        getConnector().disableAllBreakpoints();


        InsidiousXSuspendContext suspendContext = (InsidiousXSuspendContext) context;
        this.myRunToCursorBreakpoint.setSuspendPolicy(
                (suspendContext.getSuspendPolicy() == 1) ?
                        "SuspendThread" :
                        "SuspendAll");

        InsidiousBreakpointHandler handler = getBreakpointHandlers(JavaLineBreakpointType.class);
        if (this.myRunToCursorLocation != null) {

            getConnector()
                    .createLocationBreakpoint(this.myRunToCursorLocation, suspendContext

                            .getSuspendPolicy(), this.myRunToCursorBreakpoint);

            this.myRunToCursorLocation = null;
        } else {
            handler.registerJavaBreakpoint(this.myRunToCursorBreakpoint, this.myRunToCursorBreakpoint
                    .getSourcePosition());
        }

        resumeAutomatic(context);
        addPerformanceAction(context, "run_to_cursor");
    }

    public void cancelRunToCursorBreakpoint() {
        RunToCursorBreakpoint runToCursorBreakpoint = this.myRunToCursorBreakpoint;
        if (runToCursorBreakpoint != null) {
            this.myRunToCursorBreakpoint = null;

            for (BreakpointRequest request : getConnector().getBreakpointsWithRequestor(runToCursorBreakpoint)) {
                getConnector().deleteEventRequest(request);
            }
            if (runToCursorBreakpoint.isRestoreBreakpoints()) {
                getConnector().enableAllBreakpoints();
            }
        }
    }

    public void createJumpToAssignmentBreakpoint(ObjectReference objectReference, Field field, XSuspendContext context) {
        this.myJumpToAssignmentBreakpoint = new SyntheticFieldBreakpoint(getProject(), objectReference, field);


        getConnector().disableAllBreakpoints();


        InsidiousXSuspendContext suspendContext = (InsidiousXSuspendContext) context;
        this.myJumpToAssignmentBreakpoint.setSuspendPolicy(
                (suspendContext.getSuspendPolicy() == 1) ?
                        "SuspendThread" :
                        "SuspendAll");
        getConnector()
                .createFieldWatchpoint(field
                        .declaringType(), field.name(), this.myJumpToAssignmentBreakpoint);
    }

    public void cancelJumpToAssignmentBreakpoint() {
        SyntheticFieldBreakpoint jumpToAssignmentBreakpoint = this.myJumpToAssignmentBreakpoint;
        if (jumpToAssignmentBreakpoint != null) {
            this.myJumpToAssignmentBreakpoint = null;

            for (ModificationWatchpointRequest request : getConnector()
                    .getModificationWatchpointsWithRequestor(jumpToAssignmentBreakpoint)) {
                getConnector().deleteEventRequest(request);
            }
            getConnector().enableAllBreakpoints();
            logger.debug("removed " + jumpToAssignmentBreakpoint);
        }
    }

    public InsidiousBreakpointHandler getBreakpointHandlers(Class<? extends XBreakpointType> breakpointTypeClass) {
        for (InsidiousBreakpointHandler handler : getBreakpointHandlers()) {
            if (handler.getBreakpointTypeClass().equals(breakpointTypeClass)) {
                return handler;
            }
        }
        return null;
    }

    public void notifyPaused(XSuspendContext suspendContext) {
        this.myDebugProcessDispatcher.getMulticaster().paused(suspendContext);
    }

    public void startStepInto(@Nullable XSuspendContext context) {
        this.suspendedContext = context;
        stepInto(null, context);
        if (getLastDirectionType() == DirectionType.BACKWARDS) {
            addPerformanceAction(context, "reverse_step_into");
        } else {
            addPerformanceAction(context, "step_into");
        }
    }

    public void startStepOut(@Nullable XSuspendContext context) {
        this.suspendedContext = context;
        this.insidiousEventReaderthread.startWatchingMethodReturn(((InsidiousXSuspendContext) context)
                .getThreadReferenceProxy().getThreadReference());
        this.connector.doStep((InsidiousXSuspendContext) context, -2, 3);

        if (getLastDirectionType() == DirectionType.BACKWARDS) {
            addPerformanceAction(context, "reverse_step_out");
        } else {
            addPerformanceAction(context, "step_out");
        }
    }

    public void startStepOver(@Nullable XSuspendContext context) {
        this.suspendedContext = context;
        this.insidiousEventReaderthread.startWatchingMethodReturn(((InsidiousXSuspendContext) context)
                .getThreadReferenceProxy().getThreadReference());
        this.connector.doStep((InsidiousXSuspendContext) context, 2, 2);

        if (getLastDirectionType() == DirectionType.BACKWARDS) {
            addPerformanceAction(context, "reverse_step_over");
        } else {
            addPerformanceAction(context, "step_over");
        }
    }

    public void closeProcess(boolean closedByUser) {
        try {
            if (this.insidiousEventReaderthread != null) {
                this.insidiousEventReaderthread.stopListening();
                this.insidiousEventReaderthread = null;
                this.myDebugProcessDispatcher.getMulticaster().processDetached(this, closedByUser);
                if (this.telemetryTracker != null) {
                    this.telemetryTracker.endSession();
                }
                getProject().getService(InsidiousService.class).setDebugProcess(null);
            }
        } finally {
            this.connector.dispose();
        }
    }

    public Project getProject() {
        return getSession().getProject();
    }

    public void threadStarted(@NotNull InsidiousXSuspendContext suspendContext) {
        InsidiousThreadReferenceProxy threadReferenceProxy = suspendContext.getThreadReferenceProxy();
        this.myDebugProcessDispatcher
                .getMulticaster()
                .threadStarted(this, threadReferenceProxy.getThreadReference());
        if (suspendContext.getSuspendPolicy() != 0) {
            resumeAutomatic(suspendContext);
        }
    }

    public void threadStopped(ThreadReference thread) {
        this.myDebugProcessDispatcher.getMulticaster().threadStopped(this, thread);
    }

    public MethodReturnValueWatcher getReturnValueWatcher() {
        return this.insidiousEventReaderthread.getReturnValueWatcher();
    }

    @Nullable
    public XSmartStepIntoHandler<?> getSmartStepIntoHandler() {
        return this.mySmartStepIntoActionHandler;
    }


    public void stepInto(@Nullable MethodFilter smartStepFilter, XSuspendContext suspendContext) {
        RequestHint hint = new RequestHint((InsidiousXSuspendContext) suspendContext, -2, 1, smartStepFilter);
        this.connector.doStep((InsidiousXSuspendContext) suspendContext, -2, 1, hint);
    }

    public void stepBack(@Nullable MethodFilter smartStepFilter, XSuspendContext suspendContext) {
        RequestHint hint = new RequestHint((InsidiousXSuspendContext) suspendContext, -2, 1, smartStepFilter);
        this.connector.doStep((InsidiousXSuspendContext) suspendContext, -2, 1, hint);
    }


    public boolean isAttached() {
        return getConnector().isAttached();
    }


    public void addPerformanceAction(XSuspendContext context, String action) {
        if (context instanceof InsidiousXSuspendContext &&
                !((InsidiousXSuspendContext) context).isInternalEvent()) {
            addPerformanceAction(action);
        }
    }


    public void addPerformanceAction(String action) {
        logger.debug("Adding performance action: " + action);
//        this.telemetryTracker.actionStart(action);
    }

    public void stopPerformanceTiming() {
        logger.debug("stop performance action: ");
//        this.telemetryTracker.actionComplete();
    }

    public String getSessionId() {
        logger.debug("get telemetry session id");
        return "insidious-session-1";
//        return this.telemetryTracker.getSessionId();
    }

    private void beginTelemetrySession() {
        logger.debug("begin telemetry session: ");
//        new Exception().printStackTrace();
    }

    public RemoteConnection getRemoteConnection() {
        return this.myConnection;
    }

    public InsidiousApplicationState getState() {
        return getProcessHandler().getUserData(InsidiousApplicationState.KEY);
    }

    public String getRunProfileName() {
        return getState().getRunProfileName();
    }

    public DirectionType getLastDirectionType() {
        return this.lastDirectionType;
    }

    public void setLastDirectionType(DirectionType lastDirectionType) {
        logger.info("Last direction set to " + lastDirectionType);
        this.lastDirectionType = lastDirectionType;
    }
}


