package extension.descriptor.renderer;

import com.intellij.debugger.engine.DebuggerUtils;
import com.sun.jdi.Type;
import org.jetbrains.annotations.NotNull;

public abstract class InsidiousReferenceRenderer
        extends InsidiousTypeRenderer {
    protected InsidiousReferenceRenderer() {
    }

    protected InsidiousReferenceRenderer(@NotNull String className) {
        super(className);
    }


    public boolean isApplicable(Type type) {
        return (type instanceof com.sun.jdi.ReferenceType && DebuggerUtils.instanceOf(type, getClassName()));
    }
}


