package com.insidious.plugin.factory;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TestCaseServiceTest {


    private final static Logger logger = Logger.getLogger(TestCaseServiceTest.class.getName());

//    @Test
//    void testGenerationUsingClassWeave() throws APICallException, IOException, InterruptedException {
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//
//        VideobugLocalClient client = new VideobugLocalClient(
//                System.getenv("USERPROFILE") + "/.videobug/sessions");
//
//        TestCaseService testCaseService = new TestCaseService(project, client);
//
//        List<TestCandidate> testCandidateList = new LinkedList<>();
//        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);
//        testCaseService.getTestCandidates(
//                new ClientCallBack<>() {
//                    @Override
//                    public void error(ExceptionResponse errorResponse) {
//                        assert false;
//                    }
//
//                    @Override
//                    public void success(Collection<TestCandidate> testCandidates) {
//
//                        if (testCandidates.size() == 0) {
//                            return;
//                        }
//                        testCandidateList.addAll(testCandidates);
//
//                    }
//
//                    public void completed() {
//                        waiter.offer("ok");
//                    }
//                }
//        );
//        waiter.take();
//
//        TestSuite testSuite = null;
//        testSuite = testCaseService.generateTestCase(testCandidateList);
//        System.out.println(testSuite);
//
//    }


    @Test
    void testPrintObjectHistory() {

        Long objectId = Long.valueOf(909497978);
//        List<String> targetClasses = List.of("com.appsmith.server.services.ce.UserDataServiceCEImpl");

        printObjectHistory(objectId);

    }

    @Test
    void testPrintEventsByProbeIds() {

        List<Long> probeIds = List.of(909497978L);


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");


        ExecutionSession session = client.fetchProjectSessions().getItems().get(0);

//        client.fetchDataEvents()

    }

    @Test public void testByListCandatidates2() throws APICallException, IOException, InterruptedException {

        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");


        ExecutionSession session = client.fetchProjectSessions().getItems().get(0);
        TestCaseService testCaseService = new TestCaseService(project, client);


        List<String> targetClasses = List.of("com/appsmith/server/services/ce" +
                "/UserDataServiceCEImpl");

        testCaseService.listTestCandidatesByEnumeratingAllProbes(targetClasses);


    }

    private void printObjectHistory(Long objectId) {

        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");

        TestCaseService testCaseService = new TestCaseService(project, client);

        List<TestCandidate> testCandidateList = new LinkedList<>();
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);


//        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

//        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);


        client.setSession(session);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();

        filterRequest.setObjectId(objectId);
        PageInfo pageInfo = new PageInfo(0, 500, PageInfo.Order.ASC);
        pageInfo.setBufferSize(0);

        filterRequest.setPageInfo(pageInfo);

        ReplayData replayData = client.fetchObjectHistoryByObjectId(filterRequest);

        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        for (int i = 0; i < dataEvents.size(); i++) {
            DataEventWithSessionId dataEvent = dataEvents.get(i);

            DataInfo probeInfo = replayData.getProbeInfoMap().get(String.valueOf(dataEvent.getDataId()));

            ClassInfo classInfo = replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
            String logLine = "#" + i + ": [" + dataEvent.getNanoTime() + "]["
                    + probeInfo.getEventType() + "] ["
                    + classInfo.getClassName().replaceAll("/", ".") + ":" + probeInfo.getLine() + "]" +
                    "";

            ObjectInfo valueObjectInfo = replayData.getObjectInfo().get(String.valueOf(dataEvent.getValue()));
            if (valueObjectInfo != null) {
                TypeInfo typeObjectInfo = replayData.getTypeInfo().get(String.valueOf(valueObjectInfo.getTypeId()));
                logLine = logLine + " [" + typeObjectInfo.getTypeNameFromClass() + "]";
                StringInfo stringValue = replayData.getStringInfoMap().get(String.valueOf(dataEvent.getValue()));
                if (stringValue != null) {
                    logLine = logLine + " [" + stringValue.getContent() + "]";
                }
            }


            System.out.println(logLine);

        }

    }


    @Test
    void testPrintObjectsByType() throws InterruptedException {

        Long objectId;
        List<String> targetClasses = List.of("com.appsmith.server.services.UserDataServiceImpl");


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
//        VideobugLocalClient client = new VideobugLocalClient("D:\\workspace\\code\\appsmith\\videobug");

        TestCaseService testCaseService = new TestCaseService(project, client);

        List<TestCandidate> testCandidateList = new LinkedList<>();
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);


        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);


        client.getObjectsByType(
                searchQuery, session.getSessionId(), new ClientCallBack<ObjectWithTypeInfo>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(Collection<ObjectWithTypeInfo> tracePoints) {
                        allObjects.addAll(tracePoints);
                    }

                    @Override
                    public void completed() {
                        waiter.offer("done");
                    }
                }
        );
        waiter.take();

        Map<Long, List<ObjectWithTypeInfo>> objectsGroupedByType = allObjects.stream().collect(Collectors.groupingBy(e -> e.getObjectInfo().getTypeId()));

        System.out.println("Found [" + objectsGroupedByType.size() + "] type starting with [" + searchQuery.getQuery() + "]");
        for (Long typeId : objectsGroupedByType.keySet()) {
            List<ObjectWithTypeInfo> objects = objectsGroupedByType.get(typeId);
            System.out.println(objects.size() + " objects of type [" + typeId + "] -> "
                    + objects.get(0).getTypeInfo().getTypeNameFromClass());
        }


        for (ObjectWithTypeInfo allObject : allObjects) {
            System.out.println("Object [" + allObject.getObjectInfo().getObjectId()
                    + "] -> " + " [TypeId:" + allObject.getObjectInfo().getTypeId() + "]"
                    + allObject.getTypeInfo().getTypeNameFromClass());
            System.out.println("");
            System.out.println("");
            printObjectHistory(allObject.getObjectInfo().getObjectId());
            System.out.println("");
            System.out.println("");
            System.out.println("######################################");
            System.out.println("######################################");
        }


    }


    @Test
    public void printClassProbes() {


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
//        VideobugLocalClient client = new VideobugLocalClient("D:\\workspace\\code\\appsmith\\videobug");


        ExecutionSession session = client.fetchProjectSessions().getItems().get(0);


        List<String> targetClasses = List.of("com.appsmith.server.services.ce.UserDataServiceCEImpl");

        ClassWeaveInfo sessionWeaveInfo = client.getSessionClassWeave(session.getSessionId());

        for (String targetClass : targetClasses) {

            ClassInfo classInfo = sessionWeaveInfo.getClassInfoByName(targetClass.replaceAll("\\" +
                    ".", "/"));


            List<DataInfo> classProbes = sessionWeaveInfo.getProbesByClassId(classInfo.getClassId());

            System.out.println("## " + classInfo.getClassName());

            for (int i = 0; i < classProbes.size(); i++) {
                DataInfo classProbe = classProbes.get(i);

                System.out.println("#" + i + ": " + classProbe.getDataId() + " [" + classProbe.getEventType() + "] on line ["
                        + classProbe.getLine() + "] -> " + classProbe.getAttributes());

            }


        }

    }

    @Test
    void testGenerateByObjects() throws InterruptedException, APICallException, IOException {


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
//        VideobugLocalClient client = new VideobugLocalClient("D:\\workspace\\code\\appsmith\\videobug");

        TestCaseService testCaseService = new TestCaseService(project, client);

        List<TestCandidate> testCandidateList = new LinkedList<>();
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);


        List<String> targetClasses = List.of("com.appsmith.server.services.UserDataServiceImpl");
//        List<String> targetClasses = List.of("org.zerhusen.service.GCDService");
//        List<String> targetClasses = List.of("com.appsmith.server.services.SessionUserService");

        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);

        client.getObjectsByType(
                searchQuery, session.getSessionId(), new ClientCallBack<ObjectWithTypeInfo>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(Collection<ObjectWithTypeInfo> tracePoints) {
                        allObjects.addAll(tracePoints);
                    }

                    @Override
                    public void completed() {
                        waiter.offer("done");
                    }
                }
        );
        waiter.take();

        TestSuite testSuite = testCaseService.generateTestCase(allObjects);

        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
            System.out.println(testCaseScript);
        }


    }
}
