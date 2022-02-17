package extension.breakpoints;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.ui.breakpoints.RunToCursorBreakpoint;
import com.intellij.debugger.ui.breakpoints.SteppingBreakpoint;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import extension.connector.RequestHint;
import extension.smartstep.BreakpointStepMethodFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class StepIntoBreakpoint
        extends RunToCursorBreakpoint
        implements SteppingBreakpoint {
    private static final Logger LOG = Logger.getInstance(StepIntoBreakpoint.class);

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


