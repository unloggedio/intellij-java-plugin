package com.insidious.plugin.exceptionhandler;

import com.insidious.plugin.factory.UsageInsightTracker;
import com.intellij.diagnostic.AbstractMessage;
import com.intellij.diagnostic.IdeaReportingEvent;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.util.Consumer;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class InsidiousExceptionReportSubmitter extends ErrorReportSubmitter {
    @Override
    public @NlsActions.ActionText @NotNull String getReportActionText() {
        return "Report to Unlogged";
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events,
                          @Nullable String additionalInfo,
                          @NotNull Component parentComponent,
                          @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        List<Throwable> exceptionList = new ArrayList<>();

        DataManager mgr = DataManager.getInstance();
        DataContext context = mgr.getDataContext(parentComponent);
        Project project = CommonDataKeys.PROJECT.getData(context);

        new Task.Backgroundable(project, "Sending error report") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                for (IdeaLoggingEvent ideaLoggingEvent : events) {
                    if (ideaLoggingEvent instanceof IdeaReportingEvent
                            && ideaLoggingEvent.getData() instanceof AbstractMessage) {
                        Throwable throwableException = ((AbstractMessage) ideaLoggingEvent.getData()).getThrowable();
                        exceptionList.add(throwableException);
                    }
                }

                exceptionList.forEach(throwable -> {
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("project_name", project.getName());
                    eventProperties.put("message", throwable.getMessage());
                    eventProperties.put("comment", additionalInfo);
                    eventProperties.put("stacktrace", ExceptionUtil.getThrowableText(throwable));
                    UsageInsightTracker.getInstance().RecordEvent("PLUGIN_EXCEPTION_REPORTED", eventProperties);
                });

                ApplicationManager.getApplication().invokeLater(() -> {
                    SubmittedReportInfo status =
                            new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
                    consumer.consume(status);
                });
            }
        }.queue();
        return true;
    }
}
