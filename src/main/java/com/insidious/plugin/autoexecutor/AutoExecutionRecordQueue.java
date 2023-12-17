package com.insidious.plugin.autoexecutor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class AutoExecutionRecordQueue {
    private final LinkedBlockingQueue<AutoExecutorReportRecord> queue = new LinkedBlockingQueue<>();
    private final int maxSize = 500;
    private final Object IS_NOT_FULL = new Object();
    private final Object IS_NOT_EMPTY = new Object();

    public boolean isFull() {
        return queue.size() == maxSize ? true : false;
    }

    public boolean isEmpty() {
        return queue.size() == 0 ? true : false;
    }

    public void waitIsNotFull() throws InterruptedException {
        synchronized (IS_NOT_FULL) {
            IS_NOT_FULL.wait();
        }
    }

    public void notifyIsNotFull() {
        synchronized (IS_NOT_FULL) {
            IS_NOT_FULL.notify();
        }
    }

    public void waitIsNotEmpty() throws InterruptedException {
        synchronized (IS_NOT_EMPTY) {
            IS_NOT_EMPTY.wait();
        }
    }

    public void notifyIsNotEmpty() {
        synchronized (IS_NOT_EMPTY) {
            IS_NOT_EMPTY.notify();
        }
    }

    public void add(AutoExecutorReportRecord record) {
        queue.add(record);
        notifyIsNotEmpty();
    }

    public AutoExecutorReportRecord remove() {
        AutoExecutorReportRecord record = queue.poll();
        notifyIsNotFull();
        return record;
    }

    public AutoExecutorReportRecord poll() {
        return queue.poll();
    }

    public void clear() {
        queue.clear();
    }
}
