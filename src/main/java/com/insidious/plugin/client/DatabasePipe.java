package com.insidious.plugin.client;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
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
    private final List<MethodCallExpression> methodCallToSave = new LinkedList<>();
    private final List<MethodCallExpression> methodCallToUpdate = new LinkedList<>();
    private final List<TestCandidateMetadata> testCandidateMetadataList = new LinkedList<>();
    private boolean stop = false;
    private DaoService daoService;

    public DatabasePipe(LinkedTransferQueue<Parameter> parameterQueue, DaoService daoService) {
        this.parameterQueue = parameterQueue;
        this.daoService = daoService;
    }

    @Override
    public void run() {
        while (!stop) {
            Parameter param;
            try {
                param = parameterQueue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.warn("database pipe interrupted - " + e.getMessage());
                throw new RuntimeException(e);
            }

            if (dataInfoList.size() > 0) {
                Collection<DataInfo> saving = new LinkedList<>();
                dataInfoList.removeAll(saving);
                daoService.createOrUpdateProbeInfo(saving);
            }

            if (eventsToSave.size() > 0) {
                Collection<DataEventWithSessionId> saving = new LinkedList<>();
                eventsToSave.removeAll(saving);
                daoService.createOrUpdateDataEvent(saving);
            }
            if (methodCallToSave.size() > 0) {
                Collection<MethodCallExpression> saving = new LinkedList<>();
                methodCallToSave.removeAll(saving);
                daoService.createOrUpdateCall(saving);
            }
            if (methodCallToUpdate.size() > 0) {
                Collection<MethodCallExpression> saving = new LinkedList<>();
                methodCallToUpdate.removeAll(saving);
                daoService.updateCalls(saving);
            }
            if (testCandidateMetadataList.size() > 0) {
                Collection<TestCandidateMetadata> saving = new LinkedList<>();
                testCandidateMetadataList.removeAll(saving);
                daoService.createOrUpdateTestCandidate(saving);
            }

            if (param == null) {
                continue;
            }
            List<Parameter> batch = new LinkedList<>();
            parameterQueue.drainTo(batch);
            batch.add(param);
            logger.warn("Saving " + batch.size() + " parameters");
            daoService.createOrUpdateParameter(batch);
        }
        isSaving.offer(true);
    }

    public void close() throws InterruptedException {
        stop = true;
        isSaving.take();
        logger.warn("saving after close");
        List<Parameter> batch = new LinkedList<>();
        parameterQueue.drainTo(batch);
        daoService.createOrUpdateParameter(batch);


        if (dataInfoList.size() > 0) {
            Collection<DataInfo> saving = new LinkedList<>();
            dataInfoList.removeAll(saving);
            daoService.createOrUpdateProbeInfo(saving);
        }

        if (eventsToSave.size() > 0) {
            Collection<DataEventWithSessionId> saving = new LinkedList<>();
            eventsToSave.removeAll(saving);
            daoService.createOrUpdateDataEvent(saving);
        }
        if (methodCallToSave.size() > 0) {
            Collection<MethodCallExpression> saving = new LinkedList<>();
            methodCallToSave.removeAll(saving);
            daoService.createOrUpdateCall(saving);
        }
        if (methodCallToUpdate.size() > 0) {
            Collection<MethodCallExpression> saving = new LinkedList<>();
            methodCallToUpdate.removeAll(saving);
            daoService.updateCalls(saving);
        }
        if (testCandidateMetadataList.size() > 0) {
            Collection<TestCandidateMetadata> saving = new LinkedList<>();
            testCandidateMetadataList.removeAll(saving);
            daoService.createOrUpdateTestCandidate(saving);
        }

    }

    public void addParameters(List<Parameter> all) {
        parameterQueue.addAll(all);
    }

    public void addDataEvents(List<DataEventWithSessionId> events) {
        eventsToSave.addAll(events);
    }

    public void addProbeInfo(List<DataInfo> probesToSave) {
        dataInfoList.addAll(probesToSave);
    }

    public void addMethodCallsToSave(Set<MethodCallExpression> callsToSave) {
        methodCallToSave.addAll(callsToSave);
    }

    public void addMethodCallsToUpdate(Set<MethodCallExpression> callsToUpdate) {
        methodCallToUpdate.addAll(callsToUpdate);
    }

    public void addTestCandidates(List<TestCandidateMetadata> candidatesToSave) {
        testCandidateMetadataList.addAll(candidatesToSave);
    }
}