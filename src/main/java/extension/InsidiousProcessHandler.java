package extension;

import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class InsidiousProcessHandler extends ProcessHandler {
    @Override
    protected void destroyProcessImpl() {
        new Exception().printStackTrace();
    }

    @Override
    protected void detachProcessImpl() {
        new Exception().printStackTrace();

    }

    @Override
    public boolean detachIsDefault() {
        new Exception().printStackTrace();
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        new Exception().printStackTrace();
        return null;
    }
}
