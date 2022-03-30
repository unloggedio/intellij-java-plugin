package com.insidious.plugin.extension;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.OutputStream;

public class InsidiousProcessHandler extends ProcessHandler {

    private final static Logger logger = LoggerUtil.getInstance(InsidiousProcessHandler.class);
    private final String runProfileName;
    private InsidiousJavaDebugProcess insidiousJavaDebugProcess;

    public InsidiousProcessHandler(String runProfileName) {
        this.runProfileName = runProfileName;
    }

    @Override
    protected void destroyProcessImpl() {
        getInsidiousJavaDebugProcess().getProject().getService(InsidiousService.class).setDebugProcess(null);
        notifyProcessTerminated(0);
    }

    @Override
    protected void detachProcessImpl() {
        logger.info("end of debug session - {}", new Exception().getStackTrace()[0]);
        if (getInsidiousJavaDebugProcess() != null) {
            getInsidiousJavaDebugProcess().getProject().getService(InsidiousService.class).setDebugProcess(null);
            getInsidiousJavaDebugProcess().closeProcess(true);
        }
        notifyProcessDetached();

    }


    private InsidiousJavaDebugProcess getInsidiousJavaDebugProcess() {
        if (this.insidiousJavaDebugProcess == null) {
            this.insidiousJavaDebugProcess = InsidiousJavaDebugProcess.getInstance(this.runProfileName);
        }
        return this.insidiousJavaDebugProcess;
    }


    @Override
    public boolean detachIsDefault() {
        return true;
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
        notifyProcessTerminated(0);
        insidiousJavaDebugProcess.getProject().getService(InsidiousService.class).setDebugProcess(null);
    }
}
