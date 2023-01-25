package com.insidious.plugin.extension.breakpoints;

import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.smartstep.BreakpointStepMethodFilter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.ui.breakpoints.RunToCursorBreakpoint;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class StepIntoBreakpoint
        extends RunToCursorBreakpoint {
    private static final Logger logger = LoggerUtil.getInstance(StepIntoBreakpoint.class);

    @NotNull
    private final BreakpointStepMethodFilter myFilter;
    @Nullable
    private RequestHint myHint;

    protected StepIntoBreakpoint(@NotNull Project project, @NotNull SourcePosition pos, @NotNull BreakpointStepMethodFilter filter) {
        super(project, pos, false);
        this.myFilter = filter;
    }


    @Nullable
    public static StepIntoBreakpoint create(@NotNull Project project, @NotNull BreakpointStepMethodFilter filter) {
        SourcePosition pos = filter.getBreakpointPosition();
        if (pos != null) {
            StepIntoBreakpoint breakpoint = new StepIntoBreakpoint(project, pos, filter);
            breakpoint.init();
            return breakpoint;
        }
        return null;
    }

    public void setRequestHint(RequestHint hint) {
        this.myHint = hint;
    }
}


