package extension;

import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

public class InsidiousProcessHandler extends ProcessHandler {

    public InsidiousProcessHandler() {

    }

    @Override
    protected void destroyProcessImpl() {
    }

    @Override
    protected void detachProcessImpl() {
        new Exception().printStackTrace();

    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
//        new Exception().printStackTrace();
        return null;
    }

    @Override
    public void startNotify() {
        // show insidious toolbar here
        super.startNotify();
    }

    @Override
    public void destroyProcess() {
        super.destroyProcess();
    }
}
