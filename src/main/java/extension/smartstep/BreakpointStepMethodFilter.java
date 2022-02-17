package extension.smartstep;

import com.intellij.debugger.SourcePosition;
import org.jetbrains.annotations.Nullable;

public interface BreakpointStepMethodFilter extends MethodFilter {
    @Nullable
    SourcePosition getBreakpointPosition();

    int getLastStatementLine();
}


