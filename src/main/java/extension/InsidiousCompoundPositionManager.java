package extension;

import com.intellij.debugger.MultiRequestPositionManager;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InsidiousCompoundPositionManager implements MultiRequestPositionManager {
    @Override
    public @NotNull List<ClassPrepareRequest> createPrepareRequests(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) throws NoDataException {
        return null;
    }

    @Override
    public @Nullable SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
        return null;
    }

    @Override
    public @NotNull List<ReferenceType> getAllClasses(@NotNull SourcePosition classPosition) throws NoDataException {
        return null;
    }

    @Override
    public @NotNull List<Location> locationsOfLine(@NotNull ReferenceType type, @NotNull SourcePosition position) throws NoDataException {
        return null;
    }

    @Override
    public @Nullable ClassPrepareRequest createPrepareRequest(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position) throws NoDataException {
        return null;
    }
}
