package com.insidious.plugin;

import com.insidious.plugin.callbacks.GetProjectSessionErrorsCallback;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.videobugclient.VideobugClientInterface;
import com.insidious.plugin.videobugclient.VideobugLocalClient;
import com.insidious.plugin.videobugclient.pojo.ExceptionResponse;
import com.insidious.plugin.videobugclient.pojo.ExecutionSession;
import com.insidious.plugin.pojo.TracePoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
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
        client.getTracesByObjectValue("hello-string-8", new GetProjectSessionErrorsCallback() {
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
        assert result.getValue() == 8;
        client.getTracesByObjectValue("hello-string-128", new GetProjectSessionErrorsCallback() {
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
        assert result.getValue() == 128;
    }

}