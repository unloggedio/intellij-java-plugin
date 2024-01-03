package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

public class AutoExecutionConsumer implements Runnable {

    private boolean consuming = false;
    private InsidiousService insidiousService;
    private AutoExecutionRecordQueue queue;
    public static long addcounts = 0;
    private static final Logger logger = LoggerUtil.getInstance(AutoExecutionConsumer.class);

    public AutoExecutionConsumer(InsidiousService insidiousService, AutoExecutionRecordQueue queue) {
        this.insidiousService = insidiousService;
        this.queue = queue;
    }

    @Override
    public void run() {
        consuming = true;
        consume();
    }

    public void stop() {
        consuming = false;
        queue.notifyIsNotEmpty();
    }

    private void consume() {
        while (consuming) {
            if (queue.isEmpty()) {
                try {
                    queue.waitIsNotEmpty();
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (!consuming) {
                break;
            }
            addcounts++;
            logger.info("[Autex] Consumer adding record : " + addcounts);
            AutoExecutorReportRecord record = queue.poll();
            insidiousService.addExecutionRecord(record);
        }
    }
}
