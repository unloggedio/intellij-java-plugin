package com.insidious.plugin.factory.callbacks;

import com.insidious.plugin.callbacks.GetProjectSessionTracePointsCallback;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import org.json.JSONObject;

import java.util.List;

public class GetErrorsCallback implements GetProjectSessionTracePointsCallback {
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


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().cancel();
        }


        JSONObject eventProperties = new JSONObject();
        eventProperties.put("error", errorResponse.getError());
        eventProperties.put("message", errorResponse.getMessage());
        eventProperties.put("path", errorResponse.getPath());
        eventProperties.put("trace", errorResponse.getTrace());
        UsageInsightTracker.getInstance().RecordEvent("ErroredGetTracesByType", eventProperties);


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


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().cancel();
        }



        if (tracePoints.size() == 0) {

            JSONObject eventProperties = new JSONObject();
            UsageInsightTracker.getInstance().RecordEvent("NoResultGetTracesByType", eventProperties);

            InsidiousNotification.notifyMessage(
                    "No Exception data events matched in the last session [" + sessionId + "]",
                    NotificationType.INFORMATION);

        } else {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("count", tracePoints.size());
            UsageInsightTracker.getInstance().RecordEvent("YesResultGetTracesByType", eventProperties);

            insidiousService.getHorBugTable().setTracePoints(tracePoints);
        }

    }
}
