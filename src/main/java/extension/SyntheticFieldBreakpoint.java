package extension;

import com.intellij.debugger.ui.breakpoints.SyntheticLineBreakpoint;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import org.jetbrains.annotations.NotNull;

public class SyntheticFieldBreakpoint
        extends SyntheticLineBreakpoint {
    private final ObjectReference myObjectReference;
    private final Field myField;
    private final String myClassName;
    private final String myFieldName;

    public SyntheticFieldBreakpoint(@NotNull Project project, ObjectReference objectReference, Field field) {
        super(project);
        this.myObjectReference = objectReference;
        this.myField = field;
        this.myClassName = field.declaringType().name();
        this.myFieldName = field.name();
    }

    public ObjectReference getObjectReference() {
        return this.myObjectReference;
    }

    public Field getField() {
        return this.myField;
    }


    public String toString() {
        return "SyntheticFieldBreakpoint{myClassName='" + this.myClassName + '\'' + ", myFieldName='" + this.myFieldName + '\'' + '}';
    }
}


