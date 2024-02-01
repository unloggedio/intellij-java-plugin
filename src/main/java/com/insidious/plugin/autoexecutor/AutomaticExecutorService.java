package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;

import java.util.*;

public class AutomaticExecutorService {

    private final InsidiousService insidiousService;
    public static long classcount = 0;
    public static long methodcount = 0;
    public static long jsonExceptioncount = 0;
    public static long executingCount = 0;
    public static long responses = 0;
    public static long waitInterrupts = 0;
    public static long nullclasses = 0;
    public static long waiting = 0;
    public static long doneWaiting = 0;
    public static long writes = 0;
    public boolean enableMocks = false;
    private static final Logger logger = LoggerUtil.getInstance(AutomaticExecutorService.class);
    /**
     * @Param executorCount controls the number of producer consumer pairs generated and used.
     * Each ExecutionUnit (A producer and consumer pair) will write to one file.
     * Each ExecutionUnit will write to a different file based on their ID.
     */
    private final int executorCount = 1;
    private List<ExecutionUnit> executors;

    public AutomaticExecutorService(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
    }

    public void setEnableMocks(boolean status) {
        this.enableMocks = status;
        InsidiousNotification.notifyMessage("AutoExecutor mock enabled : " + enableMocks
                , NotificationType.INFORMATION);
    }

    public void executeAllJavaMethodsInProject(AutoExecutorRunOptions options) {
        setEnableMocks(options.isUseMocks());
        insidiousService.getReportingService().setReportingEnabled(true);
        parallelExecution();
    }

    public void parallelExecution() {
        classcount = 0;
        methodcount = 0;
        responses = 0;
        executingCount = 0;
        waiting = 0;
        doneWaiting = 0;

        nullclasses = 0;
        jsonExceptioncount = 0;
        waitInterrupts = 0;
        writes = 0;

        if (!insidiousService.getAgentStateProvider().isAgentRunning()) {
            InsidiousNotification.notifyMessage("Can't start AutoExecutor when the project is not running with unlogged",
                    NotificationType.ERROR);
            return;
        }

        String includedPackageName = insidiousService.getAgentStateProvider().getIncludedPackageName();
        if (includedPackageName.equals("*")) {
            //handle this situation by calculating base package yourself, or enter it somehow
            includedPackageName = "";
        }
        List<VirtualFile> javaFiles = new ArrayList<>(ApplicationManager.getApplication()
                .runReadAction((Computable<Collection<VirtualFile>>) () -> FileTypeIndex.
                        getFiles(JavaFileType.INSTANCE,
                                GlobalJavaSearchContext.projectScope(insidiousService.getProject()))));
        logger.info("[P-Autex] Total java file count : " + javaFiles.size());
        if (executors != null) {
            executors.forEach(ExecutionUnit::stopConsumer);
        }
        executors = new ArrayList<>(executorCount);
        int startIndex = 0;
        int step = javaFiles.size() / executorCount;
        boolean lastFill = false;
        for (int i = 0; i < executorCount; i++) {
            if (i == executorCount - 1) {
                lastFill = true;
            }
            List<VirtualFile> batch;
            if (!lastFill) {
                batch = javaFiles.subList(startIndex, startIndex + step);
                startIndex = startIndex + step;
            } else {
                batch = javaFiles.subList(startIndex, javaFiles.size());
            }
            ExecutionUnitConfiguration executionUnitConfiguration =
                    new ExecutionUnitConfiguration("Executor_" + i, batch, includedPackageName,
                            8000, this.enableMocks);
            ExecutionUnit executionUnit = new ExecutionUnit(this, executionUnitConfiguration);
            executors.add(executionUnit);
            executionUnit.run();
        }
    }

    public InsidiousService getInsidiousService() {
        return insidiousService;
    }

    public static synchronized void incrementClassCount() {
        classcount++;
        System.out.println("Class Count : " + classcount);
    }

    public static synchronized void incrementResponses() {
        responses++;
        logger.info("Response Count : " + responses);
    }

    public static synchronized void incrementWrites() {
        writes++;
        logger.info("Writes Count : " + writes);
    }
}
