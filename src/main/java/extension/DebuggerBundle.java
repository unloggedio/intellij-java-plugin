package extension;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class DebuggerBundle extends AbstractBundle {
    public static final String PATH_TO_BUNDLE = "META-INF.debugger";
    private static final AbstractBundle ourInstance = new DebuggerBundle();

    private DebuggerBundle() {
        super("META-INF.debugger");
    }


    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = "META-INF.debugger") String key, @NotNull Object... params) {
        return ourInstance.getMessage(key, params);
    }
}