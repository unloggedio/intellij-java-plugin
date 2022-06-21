package com.insidious.plugin.factory.callbacks;

import com.insidious.plugin.callbacks.GetProjectSessionErrorsCallback;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

public class GetErrorsCallback implements GetProjectSessionErrorsCallback {
    private final static Logger logger = LoggerUtil.getInstance(GetErrorsCallback.class);
    private final InsidiousService insidiousService;
    private final String sessionId;

    public GetErrorsCallback(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.sessionId = insidiousService.getClient().getCurrentSession().getSessionId();
    }

    @Override
    public void error(ExceptionResponse errorResponse) {
        logger.error("failed to get trace points from server - {}", errorResponse.getMessage());

        String message = errorResponse.getMessage();
        if (message == null) {
            message = "No results matched";
        }
        InsidiousNotification.notifyMessage("Failed to get trace points from server: "
                + message, NotificationType.ERROR);
    }

    @Override
    public void success(List<TracePoint> tracePoints) {
        logger.info("got [" + tracePoints.size() + "] trace points from server");
        if (tracePoints.size() == 0) {
            ApplicationManager.getApplication()
                    .runWriteAction(
                            () -> InsidiousNotification.notifyMessage(
                                    "No Exception data events matched in the last session [" + sessionId + "]",
                                    NotificationType.INFORMATION));

        } else {
            insidiousService.getHorBugTable().setTracePoints(tracePoints);
        }
    }
}
