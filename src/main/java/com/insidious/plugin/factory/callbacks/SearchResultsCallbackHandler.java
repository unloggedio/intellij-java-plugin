package com.insidious.plugin.factory.callbacks;

import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchResultsCallbackHandler implements ClientCallBack<TracePoint> {

    private final AtomicInteger doneCount;
    private static final Logger logger = LoggerUtil.getInstance(SearchResultsCallbackHandler.class);
    private final List<TracePoint> results = new LinkedList<>();
    private final SearchQuery searchQuery;

    public SearchResultsCallbackHandler(SearchQuery searchQuery) {
        this.searchQuery = searchQuery;
        this.doneCount = new AtomicInteger(0);
    }

    @Override
    public void error(ExceptionResponse errorResponse) {
        logger.error("failed to get trace points from server - {}", errorResponse.getMessage());
        doneCount.addAndGet(1);

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("error", errorResponse.getError());
        eventProperties.put("message", errorResponse.getMessage());
        eventProperties.put("path", errorResponse.getPath());
        eventProperties.put("trace", errorResponse.getSearchQuery().getQuery());
        UsageInsightTracker.getInstance().RecordEvent(
                "ErroredGetTraces" + errorResponse.getSearchQuery().getQueryType(), eventProperties);


        String message = errorResponse.getMessage();
        if (message == null) {
            message = "No results matched (Something went wrong)";
        }
        InsidiousNotification.notifyMessage("Failed to query: "
                + message, NotificationType.ERROR);
    }

    @Override
    public void success(Collection<TracePoint> tracePoints) {
        logger.info("got [" + tracePoints.size() + "] trace points from server");
        results.addAll(tracePoints);


        if (tracePoints.size() != 0) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("count", tracePoints.size());
            UsageInsightTracker.getInstance().RecordEvent("YesResultGetTraces" + searchQuery.getQueryType(), eventProperties);
        }
    }

    @Override
    public void completed() {
        doneCount.addAndGet(1);
    }

    public List<TracePoint> getResults() {
        return results;
    }

    public int getDoneCount() {
        return doneCount.get();
    }
}
