package com.insidious.plugin;

public class VideobugLocalClientTest {


//    @Test
//    public void testLocalClient1() throws IOException, InterruptedException, SQLException {
//
//        BlockingQueue<ExecutionSession> blockingQueue = new ArrayBlockingQueue<>(1);
//
//        VideobugClientInterface client = new VideobugLocalClient("src/test/resources/test-sessions/");
//
//        client.getProjectSessions(new GetProjectSessionsCallback() {
//            @Override
//            public void error(String message) {
//                assert false;
//            }
//
//            @Override
//            public void success(List<ExecutionSession> executionSessionList) {
//                assert executionSessionList.size() > 0;
//                blockingQueue.offer(executionSessionList.get(0));
//            }
//        });
//
//        ExecutionSession session = blockingQueue.take();
//        client.setSessionInstance(new SessionInstance(session));
//
//
//        BlockingQueue<TracePoint> tracePointsQueue = new ArrayBlockingQueue<>(1);
//        client.queryTracePointsByValue(SearchQuery.ByValue("trace3:2:1:1:2:trace3"), "selogger-1",
//                new ClientCallBack<TracePoint>() {
//                    @Override
//                    public void error(ExceptionResponse errorResponse) {
//                        assert false;
//                    }
//
//                    @Override
//                    public void success(Collection<TracePoint> tracePoints) {
//                        assert tracePoints.size() > 0;
//                    }
//
//                    @Override
//                    public void completed() {
//                        tracePointsQueue.offer(new TracePoint());
//                    }
//                });
//        TracePoint result = tracePointsQueue.take();
//        assert result.getMatchedValueId() == 581313178;
//        assert result.getRecordedAt() == 1651944876379L;
//        assert result.getLineNumber() == 171;
//        assert result.getThreadId() == 3;
//        assert result.getDataId() == 2402;
//        assert Objects.equals(result.getExecutionSession().getSessionId(), "selogger-1");
//        assert Objects.equals(result.getClassname(), "org/zerhusen/service/GCDService");
//        assert Objects.equals(result.getExceptionClass(), "java.lang.String");
//
//        client.queryTracePointsByValue(SearchQuery.ByValue("what a message"),
//                "selogger-1", new ClientCallBack<TracePoint>() {
//                    @Override
//                    public void error(ExceptionResponse errorResponse) {
//                        assert false;
//                    }
//
//                    @Override
//                    public void success(Collection<TracePoint> tracePoints) {
//                        assert tracePoints.size() > 0;
//                    }
//
//                    @Override
//                    public void completed() {
//                        tracePointsQueue.offer(new TracePoint());
//                    }
//                });
//        result = tracePointsQueue.take();
//        assert result.getMatchedValueId() == 125092821;
//    }

}