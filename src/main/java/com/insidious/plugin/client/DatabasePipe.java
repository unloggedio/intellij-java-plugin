package com.insidious.plugin.client;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.DatabaseVariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

class DatabasePipe implements Runnable {

    private static final Logger logger = LoggerUtil.getInstance(DatabasePipe.class);
    private final LinkedTransferQueue<Parameter> parameterQueue;
    private final ArrayBlockingQueue<Boolean> isSaving = new ArrayBlockingQueue<>(1);
    private final List<DataEventWithSessionId> eventsToSave = new LinkedList<>();
    private final List<DataInfo> dataInfoList = new LinkedList<>();
    private boolean stop = false;
    private DaoService daoService;
    private boolean isRunning;

    public DatabasePipe(LinkedTransferQueue<Parameter> parameterQueue, DaoService daoService) {
        this.parameterQueue = parameterQueue;
        this.daoService = daoService;
    }

    @Override
    public void run() {
//        isRunning = true;
//        try {
//            while (!stop) {
//                Parameter param;
//                try {
//                    param = parameterQueue.poll(1, TimeUnit.SECONDS);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    logger.warn("database pipe interrupted - " + e.getMessage());
//                    throw new RuntimeException(e);
//                }
//
//                if (dataInfoList.size() > 0) {
//                    Collection<DataInfo> saving = new LinkedList<>();
//                    dataInfoList.removeAll(saving);
//                    daoService.createOrUpdateProbeInfo(saving);
//                }
//
//                if (eventsToSave.size() > 0) {
//                    Collection<DataEventWithSessionId> saving = new LinkedList<>();
//                    eventsToSave.removeAll(saving);
//                    daoService.createOrUpdateDataEvent(saving);
//                }
//
//                if (param == null) {
//                    continue;
//                }
//                List<Parameter> batch = new LinkedList<>();
//                parameterQueue.drainTo(batch);
//                batch.add(param);
//                logger.warn("Saving " + batch.size() + " parameters");
//                daoService.createOrUpdateParameter(batch);
//            }
//
//        } finally {
//            isRunning = false;
//        }
//        isSaving.offer(true);
    }

    public void close() throws InterruptedException {
//        if (isRunning) {
//            stop = true;
//            isSaving.take();
//        }
//        logger.warn("saving after close");
//        List<Parameter> batch = new LinkedList<>();
//        parameterQueue.drainTo(batch);
//        daoService.createOrUpdateParameter(batch);
//        if (dataInfoList.size() > 0) {
//            Collection<DataInfo> saving = new LinkedList<>();
//            dataInfoList.removeAll(saving);
//            daoService.createOrUpdateProbeInfo(saving);
//        }
//
//        if (eventsToSave.size() > 0) {
//            Collection<DataEventWithSessionId> saving = new LinkedList<>();
//            eventsToSave.removeAll(saving);
//            daoService.createOrUpdateDataEvent(saving);
//        }

    }

    public void addParameters(Collection<Parameter> all) {
        parameterQueue.addAll(all);
    }

    public void addDataEvents(List<DataEventWithSessionId> events) {
        eventsToSave.addAll(events);
    }

    public void addProbeInfo(List<DataInfo> probesToSave) {
        dataInfoList.addAll(probesToSave);
    }

}