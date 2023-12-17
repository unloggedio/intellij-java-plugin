package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.factory.InsidiousService;

public class AutoExecutionConsumer implements Runnable {

    private boolean consuming = false;
    private InsidiousService insidiousService;
    private AutoExecutionRecordQueue queue;

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
//                    System.out.println("Consumer is waiting");
                    queue.waitIsNotEmpty();
                } catch (InterruptedException e) {
//                    System.out.println("Error while waiting to Consume record.");
                    break;
                }
            }
            if (!consuming) {
                break;
            }
            System.out.println("Consumer adding record.");
            AutoExecutorReportRecord record = queue.poll();
            insidiousService.addExecutionRecord(record);
        }
    }
}
