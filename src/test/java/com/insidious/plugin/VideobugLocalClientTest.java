package com.insidious.plugin;

import com.insidious.plugin.callbacks.GetProjectSessionErrorsCallback;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.pojo.TracePoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class VideobugLocalClientTest {


    @Test
    public void testLocalClient1() throws IOException, InterruptedException {

        BlockingQueue<ExecutionSession> blockingQueue = new ArrayBlockingQueue<>(1);

        VideobugClientInterface client = new VideobugLocalClient("src/test/resources/test-sessions/");

        client.getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                assert false;
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                assert executionSessionList.size() > 0;
                blockingQueue.offer(executionSessionList.get(0));
            }
        });

        ExecutionSession session = blockingQueue.take();
        client.setSession(session);


        BlockingQueue<TracePoint> tracePointsQueue = new ArrayBlockingQueue<>(1);
        client.getTracesByObjectValue("trace3:2:1:1:2:trace3", new GetProjectSessionErrorsCallback() {
            @Override
            public void error(ExceptionResponse errorResponse) {
                assert false;
            }

            @Override
            public void success(List<TracePoint> tracePoints) {
                assert tracePoints.size() > 0;
                tracePointsQueue.offer(tracePoints.get(0));
            }
        });
        TracePoint result = tracePointsQueue.take();
        assert result.getValue() == 581313178;
        assert result.getRecordedAt() == 1651944876379L;
        assert result.getLinenum() == 171;
        assert result.getThreadId() == 3;
        assert result.getDataId() == 2402;
        assert Objects.equals(result.getExecutionSessionId(), "selogger-1");
        assert Objects.equals(result.getClassname(), "org/zerhusen/service/GCDService");
        assert Objects.equals(result.getExceptionClass(), "java.lang.String");
        client.getTracesByObjectValue("what a message", new GetProjectSessionErrorsCallback() {
            @Override
            public void error(ExceptionResponse errorResponse) {
                assert false;
            }

            @Override
            public void success(List<TracePoint> tracePoints) {
                assert tracePoints.size() > 0;
                tracePointsQueue.offer(tracePoints.get(0));
            }
        });
        result = tracePointsQueue.take();
        assert result.getValue() == 125092821;
    }

}