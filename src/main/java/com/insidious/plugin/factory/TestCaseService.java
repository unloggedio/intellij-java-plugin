package com.insidious.plugin.factory;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class TestCaseService {
    private final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final Project project;
    private final VideobugClientInterface client;

    public TestCaseService(Project project, VideobugLocalClient client) {
        this.project = project;
        this.client = client;
    }

    /**
     * try to generate a list of testable methods
     * this one tries to query probe ids based on events METHOD_ENTRY/METHOD_*
     * and then expects the user to do their own figuring out how to do the
     * rest of the flow
     *
     * @param tracePointsCallback callback which receives the results
     * @throws APICallException session related exception
     * @throws IOException      everything is on files
     */
    public void listTestCandidates(ClientCallBack<TracePoint> tracePointsCallback) throws APICallException, IOException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            tracePointsCallback.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);

        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
//                EventType.METHOD_EXCEPTIONAL_EXIT,
//                EventType.METHOD_OBJECT_INITIALIZED,
//                EventType.METHOD_PARAM,
//                EventType.METHOD_THROW,
//                EventType.METHOD_NORMAL_EXIT,
                        EventType.METHOD_ENTRY
                )
        );
        this.client.queryTracePointsByProbe(searchQuery,
                session.getSessionId(), tracePointsCallback);
    }

    /**
     * this is the third attempt to generate test candidates, by using the
     * complete class weave information of a session loaded instead of trying
     * to rebuild the pieces one by one. here we are going to have
     * classInfoList, methodInfoList and also probeInfoList to do *our*
     * processing and then return actually usable results
     *
     * @param clientCallBack callback to stream the results to
     */
    public

    void

    getTestCandidates(

            ClientCallBack<TestCandidate> clientCallBack

    )


            throws APICallException, IOException, InterruptedException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            clientCallBack.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);
        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
//                EventType.METHOD_EXCEPTIONAL_EXIT,
//                EventType.METHOD_OBJECT_INITIALIZED,
//                EventType.METHOD_PARAM,
//                EventType.METHOD_THROW,
//                EventType.METHOD_NORMAL_EXIT,
                        EventType.METHOD_ENTRY
                )
        );

        BlockingQueue<String> tracePointLock = new ArrayBlockingQueue<>(1);

        List<TracePoint> tracePointList = new LinkedList<>();
        this.client.queryTracePointsByProbe(searchQuery,
                session.getSessionId(), new ClientCallBack<TracePoint>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(Collection<TracePoint> tracePoints) {
                        tracePointList.addAll(tracePoints);

                    }

                    @Override
                    public void completed() {
                        tracePointLock.offer("new ExecutionSession()");
                    }
                });

        tracePointLock.take();

        if (tracePointList.size() == 0) {
            logger.warn("no trace point found for method_entry event");
            clientCallBack.completed();
            return;
        }

        ClassWeaveInfo classWeaveInfo = this.client.getSessionClassWeave(session.getSessionId());

        for (TracePoint tracePoint : tracePointList) {

            DataInfo probeInfo = classWeaveInfo.getProbeById(tracePoint.getDataId());
            MethodInfo methodInfo = classWeaveInfo.getMethodInfoById(probeInfo.getMethodId());
            ClassInfo classInfo = classWeaveInfo.getClassInfoById(methodInfo.getClassId());

            FilteredDataEventsRequest filterDataEventsRequest = tracePoint.toFilterDataEventRequest();

            ReplayData probeReplayData = client.fetchDataEvents(filterDataEventsRequest);

            TestCandidate testCandidate = new TestCandidate(methodInfo, classInfo, session, probeInfo,
                    probeReplayData);
            clientCallBack.success(List.of(testCandidate));

            logger.warn("generate test case for ["
                    +  classInfo.getClassName() + ":" + methodInfo.getMethodName()
                    + "] using " + probeReplayData.getDataEvents().size() +
                    " events");
        }

        clientCallBack.completed();


    }

    /**
     * If the name doesnt give enough meaning, this will iterate thru all the classes, their
     * methods, and query for each single method_entry probe entry one by one.
     * it is very slow
     * @throws APICallException it tries to use client
     * @throws IOException // if something on the disk fails
     * @throws InterruptedException // it waits
     */
    public void listTestCandidatesByEnumeratingAllProbes() throws APICallException, IOException, InterruptedException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            return;
        }


        ExecutionSession session = sessions.get(0);
        ClassWeaveInfo classWeaveInfo = this.client.getSessionClassWeave(session.getSessionId());

        for (ClassInfo classInfo : classWeaveInfo.getClassInfoList()) {

            List<MethodInfo> methods =
                    classWeaveInfo.getMethodInfoByClassId(classInfo.getClassId());

            for (MethodInfo method : methods) {


                // test can be generated for public methods
                if (Modifier.isPublic(method.getAccess())) {

                    String methodDescriptor = method.getMethodDesc();
                    List<DataInfo> methodProbes = classWeaveInfo.getProbesByMethodId(method.getMethodId());

                    if (methodProbes.size() == 0) {
                        logger.warn("method [" +
                                classInfo.getClassName() + ":" + method.getMethodName()
                                + "] has no probes");
                        continue;
                    }

                    Optional<DataInfo> methodEntryProbeOption =
                            methodProbes.stream()
                                    .filter(p -> p.getEventType() == EventType.METHOD_ENTRY)
                                    .findFirst();

                    Set<DataInfo> methodParamProbes = methodProbes.stream()
                            .filter(p -> p.getEventType() == EventType.METHOD_PARAM)
                            .collect(Collectors.toSet());

                    assert methodEntryProbeOption.isPresent();

                    DataInfo methodEntryProbe = methodEntryProbeOption.get();

                    BlockingQueue<ExecutionSession> blockingQueue = new ArrayBlockingQueue<>(1);

                    List<TracePoint> tracePoints = new LinkedList<>();
                    client.queryTracePointsByProbe(
                            SearchQuery.ByProbe(List.of(methodEntryProbe.getClassId())),
                            session.getSessionId(), new ClientCallBack<TracePoint>() {
                                @Override
                                public void error(ExceptionResponse errorResponse) {
                                    assert false;
                                }

                                @Override
                                public void success(Collection<TracePoint> tracePointList) {
                                    tracePoints.addAll(tracePointList);
                                }

                                @Override
                                public void completed() {
                                    blockingQueue.offer(new ExecutionSession());
                                }
                            });

                    blockingQueue.take();

                    if (tracePoints.size() == 0) {
                        logger.warn("no trace point found for probe["
                                + methodEntryProbe.getDataId() + " => [" + classInfo.getClassName() + "] => "
                                + method.getMethodName());
                        continue;
                    }

                    TracePoint tracePoint = tracePoints.get(0);

                    FilteredDataEventsRequest filterDataEventsRequest = tracePoint.toFilterDataEventRequest();
                    filterDataEventsRequest.setProbeId(methodEntryProbe.getDataId());
                    ReplayData probeReplayData = client.fetchDataEvents(filterDataEventsRequest);

                    logger.info("generate test case using " + probeReplayData.getDataEvents().size() + " events");


                }


            }


        }

    }


    /**
     * another attempt at generated test candidates but this one tries to get
     * a list of all methods in the session and then expects the user to do
     * their own filtering/sorting. probably not going to be used calling the
     * results as test candidates is misleading
     *
     * @param tracePointsCallback callback to stream results to
     * @throws APICallException from session related operations
     * @throws IOException      every thing is on the files
     */
    public void listTestCandidatesByMethods(
            ClientCallBack<TestCandidate> tracePointsCallback) throws APICallException, IOException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            tracePointsCallback.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);

//        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
////                EventType.METHOD_EXCEPTIONAL_EXIT,
////                EventType.METHOD_OBJECT_INITIALIZED,
////                EventType.METHOD_PARAM,
////                EventType.METHOD_THROW,
////                EventType.METHOD_NORMAL_EXIT,
//                        EventType.METHOD_ENTRY
//                )
//        );

        this.client.getMethods(session.getSessionId(), tracePointsCallback);
    }

    @NotNull
    private TracePoint dummyTracePoint() {
        return new TracePoint(
                1,
                2,
                3, 4, 5,
                "file.java",
                "ClassName",
                "ExceptionClassName",
                1234,
                1235);
    }

    public TestCaseScript generateTestCase(TestCandidate testCandidate) throws IOException {

        ByteArrayOutputStream classOut = new ByteArrayOutputStream();
        TraceClassVisitor traceClassVisitor =
                new TraceClassVisitor(new PrintWriter(classOut));

        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage("video.bug.generated").setName("UnloggedTest" + testCandidate.getClassInfo().getClassName());

        javaClass.addInterface(Serializable.class);
        javaClass.addField()
                .setName("serialVersionUID")
                .setType("long")
                .setLiteralInitializer("1L")
                .setPrivate()
                .setStatic(true)
                .setFinal(true);

        javaClass.addProperty(Integer.class, "id").setMutable(false);
        javaClass.addProperty(String.class, "firstName");
        javaClass.addProperty("String", "lastName");

        javaClass.addMethod()
                .setConstructor(true)
                .setPublic()
                .setBody("this.id = id;")
                .addParameter(Integer.class, "id");


        return new TestCaseScript(classOut.toString());

//        BufferedOutputStream boss = new BufferedOutputStream(outputStream);
//        DataOutputStream doss = new DataOutputStream(boss);
//        doss.write("package com.package.test\n".getBytes());
//        doss.write("\t@Test".getBytes());
//        doss.write("\t\tpublic void testMethod1() {\n".getBytes());
//        doss.write("\t}\n".getBytes());


    }
}
