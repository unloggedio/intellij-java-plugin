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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousReferenceRenderer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */