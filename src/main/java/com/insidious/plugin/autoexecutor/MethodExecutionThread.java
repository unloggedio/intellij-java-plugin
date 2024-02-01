package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.util.DiffUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

public class MethodExecutionThread implements Runnable {

    private ExecutionUnit parentUnit;
    private AgentCommandRequest agentCommandRequest;
    private MethodAdapter methodAdapter;
    private ClassUnderTest classUnderTest;

    public MethodExecutionThread(ExecutionUnit parentUnit, AgentCommandRequest agentCommandRequest,
                                 MethodAdapter methodAdapter, ClassUnderTest classUnderTest) {
        this.parentUnit = parentUnit;
        this.agentCommandRequest = agentCommandRequest;
        this.methodAdapter = methodAdapter;
        this.classUnderTest = classUnderTest;
    }

    @Override
    public void run() {
        ClassAdapter sourceClass = methodAdapter.getContainingClass();
        parentUnit.getAutomaticExecutorService().getInsidiousService().executeMethodInRunningProcessSync(agentCommandRequest,
                (agentCommandRequest1, agentCommandResponse) -> {
                    if (ResponseType.EXCEPTION.equals(agentCommandResponse.getResponseType())) {
                        if (agentCommandResponse.getMessage() == null && agentCommandResponse.getResponseClassName() == null) {
                            InsidiousNotification.notifyMessage(
                                    "Exception thrown when trying to invoke " + agentCommandRequest.getMethodName(),
                                    NotificationType.ERROR
                            );
                            return;
                        }
                    }

                    parentUnit.responses++;

                    ResponseType responseType1 = agentCommandResponse.getResponseType();
                    DiffResultType diffResultType = responseType1.equals(
                            ResponseType.NORMAL) ? DiffResultType.NO_ORIGINAL : DiffResultType.ACTUAL_EXCEPTION;
                    DifferenceResult diffResult = new DifferenceResult(null,
                            diffResultType, null,
                            DiffUtils.getFlatMapFor(agentCommandResponse.getMethodReturnValue()));
                    diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE);
                    diffResult.setResponse(agentCommandResponse);
                    diffResult.setCommand(agentCommandRequest);

                    if (parentUnit.getReportingQueue().isFull()) {
                        try {
                            parentUnit.getReportingQueue().waitIsNotFull();
                        } catch (InterruptedException e) {
                        }
                    }
                    if (sourceClass.isInterface()) {
                        // report using actual class rather than the executed impl
                        // without this impl classes will have 2 sets of executions in the report
                        // while the original interface here will have no entries
                        diffResult.getCommand().setClassName(sourceClass.getQualifiedName());
                    }
                    AutoExecutorReportRecord record = new AutoExecutorReportRecord(diffResult,
                            0,
                            0,
                            agentCommandRequest1.getDeclaredMocks());
                    record.setSource(parentUnit.getConfiguration().getExecutorId());
                    String cannonText = ApplicationManager.getApplication()
                            .runReadAction((Computable<String>) () -> methodAdapter.getReturnType().getCanonicalText());
                    record.setMethodReturnTypeCannonicalText(cannonText);
                    record.setImplementationSource(classUnderTest.getQualifiedClassName());
                    parentUnit.getReportingQueue().add(record);
                });
    }
}