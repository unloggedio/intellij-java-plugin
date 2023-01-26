package com.insidious.plugin.extension;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

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
        try {
            InsidiousJavaDebugProcess debugProcess = getInsidiousJavaDebugProcess();
            if (debugProcess == null) {
                return;
            }
            Project project = debugProcess.getProject();
            if (!project.isOpen()) {
                return;
            }
            InsidiousService service = ApplicationManager.getApplication().getService(InsidiousService.class);
            service.setDebugSession(null);
            service.setDebugProcess(null);
        } catch (Exception e) {
            logger.error("failed to destroy process", e);
        }
        notifyProcessTerminated(0);
    }

    @Override
    protected void detachProcessImpl() {
        logger.info("end of debug session - " + new Exception().getStackTrace()[0]);
        if (getInsidiousJavaDebugProcess() != null) {
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
    }
}
