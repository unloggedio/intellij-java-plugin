package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.breakpoints.*;
import org.slf4j.Logger;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.*;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InsidiousBreakpointHandler extends XBreakpointHandler {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousBreakpointHandler.class);

    protected InsidiousJavaDebugProcess myProcess;


    public InsidiousBreakpointHandler(@NotNull Class<? extends XBreakpointType<?, ?>> breakpointTypeClass, InsidiousJavaDebugProcess process) {
        super(breakpointTypeClass);
        this.myProcess = process;
    }


    @Override
    public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
        Breakpoint javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
        if (javaBreakpoint != null) {

            SourcePosition sourcePosition = DebuggerUtilsEx.toSourcePosition(breakpoint
                    .getSourcePosition(), this.myProcess.getProject());
            BreakpointManager.addBreakpoint(javaBreakpoint);
            registerJavaBreakpoint(javaBreakpoint, sourcePosition);
        }
    }


    public void registerJavaBreakpoint(@NotNull Breakpoint<?> breakpoint, SourcePosition sourcePosition) {
        if (sourcePosition != null) {

            List<ReferenceType> classes = this.myProcess.getPositionManager().getAllClasses(sourcePosition);
            if (classes.size() > 0) {
                sendBreakpoint(breakpoint, classes.get(0));

            } else {


                List<ClassPrepareRequest> prepareRequests = this.myProcess.getPositionManager().createPrepareRequests(breakpoint, sourcePosition);
                for (ClassPrepareRequest prepareRequest : prepareRequests) {
                    if (prepareRequest != null) {
                        prepareRequest.enable();
                    }
                }
            }
        }
    }


    public void sendBreakpoint(Breakpoint<?> breakpoint, ReferenceType referenceType) {
    }

    public void removeBreakpoint(@NotNull XBreakpoint breakpoint, List<? extends EventRequest> allBreakpoints) {
        Breakpoint<?> javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
        if (javaBreakpoint != null) {
            List<EventRequest> deleteList = new ArrayList<>();
            for (EventRequest request : allBreakpoints) {
                if (javaBreakpoint.equals(request.getProperty(InsidiousJDIConnector.REQUESTOR))) {
                    deleteList.add(request);
                }
            }
            for (EventRequest request : deleteList) {
                this.myProcess.getConnector().deleteEventRequest(request);
            }
        }
    }


    @Override
    public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {
        List<BreakpointRequest> breakpoints = this.myProcess.getConnector().getAllBreakpoints();
        removeBreakpoint(breakpoint, breakpoints);
    }

    public static class InsidiousJavaLineBreakpointHandler extends InsidiousBreakpointHandler {
        public InsidiousJavaLineBreakpointHandler(InsidiousJavaDebugProcess process) {
            super(JavaLineBreakpointType.class, process);
        }


        @Override
        public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
            super.registerBreakpoint(breakpoint);
            this.myProcess.addPerformanceAction(this.myProcess
                    .getSession().getSuspendContext(), "line_breakpoint_set");
        }


        @Override
        public void sendBreakpoint(Breakpoint breakpoint, ReferenceType referenceType) {
            LineBreakpoint lineBreakpoint = (LineBreakpoint) breakpoint;

            SourcePosition position = lineBreakpoint.getSourcePosition();

            List<Location> locations = this.myProcess.getPositionManager().locationsOfLine(referenceType, position);
            for (Location loc : locations) {
                this.myProcess
                        .getConnector()
                        .createLocationBreakpoint(loc, 2, breakpoint);
            }
        }


        @Override
        public void unregisterBreakpoint(@NotNull XBreakpoint breakpoint, boolean temporary) {
            super.unregisterBreakpoint(breakpoint, temporary);
        }
    }

    public static class InsidiousJavaExceptionBreakpointHandler extends InsidiousBreakpointHandler {
        public InsidiousJavaExceptionBreakpointHandler(InsidiousJavaDebugProcess process) {
            super(JavaExceptionBreakpointType.class, process);
        }


        @Override
        public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
            super.registerBreakpoint(breakpoint);
            this.myProcess.addPerformanceAction(this.myProcess
                    .getSession().getSuspendContext(), "exception_breakpoint_set");
        }


        @Override
        public void sendBreakpoint(Breakpoint breakpoint, ReferenceType referenceType) {
            boolean notifyCaught = true;
            boolean notifyUncaught = true;
            if (breakpoint instanceof ExceptionBreakpoint) {
                ExceptionBreakpoint ebp = (ExceptionBreakpoint) breakpoint;
                notifyCaught = ebp.getXBreakpoint().getProperties().NOTIFY_CAUGHT;
                notifyUncaught = ebp.getXBreakpoint().getProperties().NOTIFY_UNCAUGHT;
            }
            this.myProcess
                    .getConnector()
                    .createExceptionBreakpoint(referenceType, notifyCaught, notifyUncaught, 2, breakpoint);
        }


        @Override
        public void unregisterBreakpoint(XBreakpoint breakpoint, boolean temporary) {
            List<ExceptionRequest> exceptionBreakpoints = this.myProcess.getConnector().getAllExceptionBreakpoints();
            removeBreakpoint(breakpoint, exceptionBreakpoints);
        }
    }

    public static class InsidiousJavaMethodBreakpointHandler extends InsidiousBreakpointHandler {
        public InsidiousJavaMethodBreakpointHandler(InsidiousJavaDebugProcess process) {
            super(JavaMethodBreakpointType.class, process);
        }


        @Override
        public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
            super.registerBreakpoint(breakpoint);
            this.myProcess.addPerformanceAction(this.myProcess
                    .getSession().getSuspendContext(), "method_breakpoint_set");
        }


        @Override
        public void sendBreakpoint(Breakpoint breakpoint, ReferenceType referenceType) {
            if (breakpoint instanceof MethodBreakpoint) {
                MethodBreakpoint bp = (MethodBreakpoint) breakpoint;
                this.myProcess.getConnector().createMethodBreakpoint(referenceType, bp);
            }
        }


        @Override
        public void unregisterBreakpoint(XBreakpoint breakpoint, boolean temporary) {
            List<EventRequest> methodRequests = this.myProcess.getConnector().getAllMethodRequests();
            removeBreakpoint(breakpoint, methodRequests);
        }
    }

    public static class InsidiousJavaWildcardBreakpointHandler extends InsidiousBreakpointHandler {
        public InsidiousJavaWildcardBreakpointHandler(InsidiousJavaDebugProcess process) {
            super(JavaWildcardMethodBreakpointType.class, process);
        }


        @Override
        public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
            super.registerBreakpoint(breakpoint);
            this.myProcess.addPerformanceAction(this.myProcess
                    .getSession().getSuspendContext(), "method_breakpoint_set");
        }


        @Override
        public void sendBreakpoint(Breakpoint breakpoint, ReferenceType referenceType) {
            MethodBreakpoint bp = (MethodBreakpoint) breakpoint;
            this.myProcess.getConnector().createMethodBreakpoint(referenceType, bp);
        }


        @Override
        public void unregisterBreakpoint(XBreakpoint breakpoint, boolean temporary) {
            List<EventRequest> methodRequests = this.myProcess.getConnector().getAllMethodRequests();
            removeBreakpoint(breakpoint, methodRequests);
        }
    }

    public static class InsidiousJavaFieldBreakpointHandler extends InsidiousBreakpointHandler {
        public InsidiousJavaFieldBreakpointHandler(InsidiousJavaDebugProcess process) {
            super(JavaFieldBreakpointType.class, process);
        }


        @Override
        public void registerBreakpoint(@NotNull XBreakpoint breakpoint) {
            super.registerBreakpoint(breakpoint);
            this.myProcess.addPerformanceAction(this.myProcess
                    .getSession().getSuspendContext(), "watchpoint_set");
        }


        @Override
        public void sendBreakpoint(Breakpoint breakpoint, ReferenceType referenceType) {
            if (breakpoint instanceof FieldBreakpoint) {
                FieldBreakpoint fbp = (FieldBreakpoint) breakpoint;
                try {
                    this.myProcess
                            .getConnector()
                            .createFieldWatchpoint(referenceType, fbp.getFieldName(), fbp);
                } catch (Exception e) {
                    logger.error("failed to evaluate", e);
                }
            }
        }


        @Override
        public void unregisterBreakpoint(XBreakpoint breakpoint, boolean temporary) {
            List<ModificationWatchpointRequest> watchpoints = this.myProcess.getConnector().getAllFieldWatchpoints();
            removeBreakpoint(breakpoint, watchpoints);
        }
    }
}


