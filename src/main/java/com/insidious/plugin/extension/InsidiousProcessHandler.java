package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.OutputStream;

public class InsidiousProcessHandler extends ProcessHandler {

    private final static Logger logger = LoggerUtil.getInstance(InsidiousProcessHandler.class);

    public InsidiousProcessHandler() {

    }

    @Override
    protected void destroyProcessImpl() {
    }

    @Override
    protected void detachProcessImpl() {
        logger.info("end of debug session - ", new Exception());

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
