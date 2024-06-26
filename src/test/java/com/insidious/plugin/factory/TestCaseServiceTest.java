package com.insidious.plugin.factory;

import com.google.gson.Gson;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.client.DaoService;
import com.insidious.plugin.client.ParameterProvider;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.ValueResourceContainer;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.intellij.openapi.project.Project;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TestCaseServiceTest {


    private final static Logger logger = Logger.getLogger(TestCaseServiceTest.class.getName());


    private static void copyTestCaseToClipboard(TestCaseUnit testCaseUnit) {
        System.out.println(testCaseUnit);

        StringSelection selection = new StringSelection(testCaseUnit.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        clipboard.setContents(selection, selection);
        System.out.println(selection);
    }

//    @Test public void testByListCandatidates2() throws APICallException, IOException, InterruptedException {
//
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//
//        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
//
//
//        ExecutionSession session = client.fetchProjectSessions().getItems().get(0);
//        TestCaseService testCaseService = new TestCaseService(project, client);
//
//
//        List<String> targetClasses = List.of("com/appsmith/server/services/ce" +
//                "/UserDataServiceCEImpl");
//
//        testCaseService.listTestCandidatesByEnumeratingAllProbes(targetClasses);
//
//
//    }

//    @Test
//    void testPrintObjectHistory() throws SessionNotSelectedException, SQLException, IOException {
//
//        Long objectId = Long.valueOf(909497978);
////        List<String> targetClasses = List.of("com.appsmith.server.services.ce.UserDataServiceCEImpl");
//
//        printObjectHistory(objectId);
//
//    }

//    @Test
//    void testPrintEventsByProbeIds() {
//
//        List<Long> probeIds = List.of(909497978L);
//
//
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//
//        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
//
//
//        ExecutionSession session = client.fetchProjectSessions().getItems().get(0);
//
////        client.fetchDataEvents()
//
//        Mockito.when(session.getCreatedAt()).thenThrow(Exception.class);
//    }

    private void printObjectHistory(Long objectId) throws SessionNotSelectedException, SQLException, IOException {

        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions",
                project, null);

//        TestCaseService testCaseService = new TestCaseService(getDaoService("jdbc:sqlite:execution.db"), client);

//        List<TestCandidate> testCandidateList = new LinkedList<>();
//        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);


//        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

//        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);


        client.setSessionInstance(new SessionInstance(session, new ServerMetadata(), project));
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();

        filterRequest.setObjectId(objectId);
        PageInfo pageInfo = new PageInfo(0, 500, PageInfo.Order.ASC);
        pageInfo.setBufferSize(0);

        filterRequest.setPageInfo(pageInfo);

        ReplayData replayData = client.fetchObjectHistoryByObjectId(filterRequest);

        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        for (int i = 0; i < dataEvents.size(); i++) {
            DataEventWithSessionId dataEvent = dataEvents.get(i);

            DataInfo probeInfo = replayData.getProbeInfo(dataEvent.getProbeId());

            ClassInfo classInfo = replayData.getClassInfo(probeInfo.getClassId());
            String logLine = "#" + i + ": [" + dataEvent.getEventId() + "]["
                    + probeInfo.getEventType() + "] ["
                    + classInfo.getClassName()
                    .replaceAll("/", ".") + ":" + probeInfo.getLine() + "]" +
                    "";

            ObjectInfo valueObjectInfo = replayData.getObjectInfoMap()
                    .get(dataEvent.getValue());
            if (valueObjectInfo != null) {
                TypeInfo typeObjectInfo = replayData.getTypeInfoMap()
                        .get(valueObjectInfo.getTypeId());
                logLine = logLine + " [" + typeObjectInfo.getTypeNameFromClass() + "]";
                StringInfo stringValue = replayData.getStringInfoMap()
                        .get(dataEvent.getValue());
                if (stringValue != null) {
                    logLine = logLine + " [" + stringValue.getContent() + "]";
                }
            }


            System.out.println(logLine);

        }

    }

//    @Test
//    void testPrintObjectsByType() throws InterruptedException, SessionNotSelectedException, SQLException, IOException {
//
//        List<String> targetClasses = List.of("com.appsmith.server.services.UserDataServiceImpl");
//
//
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//
//        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
////        VideobugLocalClient client = new VideobugLocalClient("D:\\workspace\\code\\appsmith\\videobug");
//
////        TestCaseService testCaseService = new TestCaseService(getDaoService("jdbc:sqlite:execution.db"), client);
//
////        List<TestCandidate> testCandidateList = new LinkedList<>();
//        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);
//
//
//        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);
//
//        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();
//
//        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
//        ExecutionSession session = sessions.getItems().get(0);
//
//
//        client.getObjectsByType(
//                searchQuery, session.getSessionId(), new ClientCallBack<ObjectWithTypeInfo>() {
//                    @Override
//                    public void error(ExceptionResponse errorResponse) {
//
//                    }
//
//                    @Override
//                    public void success(Collection<ObjectWithTypeInfo> tracePoints) {
//                        allObjects.addAll(tracePoints);
//                    }
//
//                    @Override
//                    public void completed() {
//                        waiter.offer("done");
//                    }
//                }
//        );
//        waiter.take();
//
//        Map<Long, List<ObjectWithTypeInfo>> objectsGroupedByType = allObjects.stream().collect(Collectors.groupingBy(e -> e.getObjectInfo().getTypeId()));
//
//        System.out.println("Found [" + objectsGroupedByType.size() + "] type starting with [" + searchQuery.getQuery() + "]");
//        for (Long typeId : objectsGroupedByType.keySet()) {
//            List<ObjectWithTypeInfo> objects = objectsGroupedByType.get(typeId);
//            System.out.println(objects.size() + " objects of type [" + typeId + "] -> "
//                    + objects.get(0).getTypeInfo().getTypeNameFromClass());
//        }
//
//
//        for (ObjectWithTypeInfo allObject : allObjects) {
//            System.out.println("Object [" + allObject.getObjectInfo().getObjectId()
//                    + "] -> " + " [TypeId:" + allObject.getObjectInfo().getTypeId() + "]"
//                    + allObject.getTypeInfo().getTypeNameFromClass());
//            System.out.println("");
//            System.out.println("");
//            printObjectHistory(allObject.getObjectInfo().getObjectId());
//            System.out.println("");
//            System.out.println("");
//            System.out.println("######################################");
//            System.out.println("######################################");
//        }
//
//
//    }

//    @Test
//    public void printClassProbes() {
//
//
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//
//        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
////        VideobugLocalClient client = new VideobugLocalClient("D:\\workspace\\code\\appsmith\\videobug");
//
//
//        ExecutionSession session = client.fetchProjectSessions().getItems().get(0);
//
//
//        List<String> targetClasses = List.of("com.appsmith.server.services.ce.UserDataServiceCEImpl");
//
//        ClassWeaveInfo sessionWeaveInfo = client.getSessionClassWeave(session.getSessionId());
//
//        for (String targetClass : targetClasses) {
//
//            ClassInfo classInfo = sessionWeaveInfo.getClassInfoByName(targetClass.replaceAll("\\" +
//                    ".", "/"));
//
//
//            List<DataInfo> classProbes = sessionWeaveInfo.getProbesByClassId(classInfo.getClassId());
//
//            System.out.println("## " + classInfo.getClassName());
//
//            for (int i = 0; i < classProbes.size(); i++) {
//                DataInfo classProbe = classProbes.get(i);
//
//                System.out.println("#" + i + ": " + classProbe.getDataId() + " [" + classProbe.getEventType() + "] on line ["
//                        + classProbe.getLine() + "] -> " + classProbe.getAttributes());
//
//            }
//
//
//        }
//
//    }

    void testMono() {


    }

//    @Test
//    void testGenerateByObjects() throws InterruptedException {
//
//
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//
//        VideobugLocalClient client = new VideobugLocalClient(System.getenv("HOME") + "/.videobug/sessions");
//        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);
//
//        client.getProjectSessions(new GetProjectSessionsCallback() {
//            @Override
//            public void error(String message) {
//                throw new RuntimeException(message);
//            }
//
//            @Override
//            public void success(List<ExecutionSession> executionSessionList) {
//                try {
//                    client.setSessionInstance(new SessionInstance(executionSessionList.get(0)));
//                } catch (SQLException | IOException e) {
//                    throw new RuntimeException(e);
//                }
//                waiter.offer("done");
//            }
//        });
//
//        waiter.take();
//        TestCaseService testCaseService = new TestCaseService(client);
//
//
////        List<String> targetClasses = List.of("com.appsmith.server.authentication.handlers.ce.AuthenticationSuccessHandlerCE");
////        List<String> targetClasses = List.of("com.appsmith.server.solutions.ce.UserChangedHandlerCEImpl");
////        List<String> targetClasses = List.of("com.appsmith.server.services.UserDataServiceImpl");
////        List<String> targetClasses = List.of("com.appsmith.server.services.ce.FeatureFlagServiceCEImpl");
////        List<String> targetClasses = List.of("org.zerhusen.service.GCDService");
////        List<String> targetClasses = List.of("com.appsmith.server.services.SessionUserService");
////        List<String> targetClasses = List.of("jenkins.org.apache.commons.validator.routines.RegexValidator");
////        List<String> targetClasses = List.of("org.zerhusen.security.UserModelDetailsService");
//
//
////        List<String> targetClasses = List.of("com.repyute.service.pocket.PocketService");
////        List<String> targetClasses = List.of("com.repyute.helper.pocket.PocketHelper");
////        List<String> targetClasses = List.of("com.ayu.cabeza.service.CustomerProfileService");
////        List<String> targetClasses = List.of("com.ayu.cabeza.communication.whatsapp.api.WhatsappAPIController");
////        List<String> targetClasses = List.of("com.repyute.service.paybooks.PaybooksService");
////        List<String> targetClasses = List.of("com.repyute.helper.paybooks.PaybooksHelper");
////        List<String> targetClasses = List.of("com.ayu.cabeza.service.CustomerProfileService");
////        List<String> targetClasses = List.of("com.ayu.cabeza.service.AyuCityService");
////        List<String> targetClasses = List.of("com.ayu.cabeza.service.AyuCatalogueService");
////        List<String> targetClasses = List.of("com.ayu.cabeza.service.HospitalProfileService");
//        List<String> targetClasses = List.of("com.ayu.cabeza.service.DoctorProfileService");
//
//
//        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);
//
//        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();
//
//        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
//        ExecutionSession session = sessions.getItems().get(0);
//
//        client.getObjectsByType(
//                searchQuery, session.getSessionId(), new ClientCallBack<>() {
//                    @Override
//                    public void error(ExceptionResponse errorResponse) {
//
//                    }
//
//                    @Override
//                    public void success(Collection<ObjectWithTypeInfo> tracePoints) {
//                        allObjects.addAll(tracePoints);
//                    }
//
//                    @Override
//                    public void completed() {
//                        waiter.offer("done");
//                    }
//                }
//        );
//        waiter.take();
//
//        TestCaseRequest testCaseRequest = new TestCaseRequest(
//                allObjects,
//                List.of(
//                        "com.fasterxml",
//                        "com.google"
//                ),
//                Set.of()
//        );
//        TestSuite testSuite = testCaseService.generateTestCase(testCaseRequest);
//
//        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
//            System.out.println(testCaseScript);
//        }
//
//
//    }

//    @Test
//    public void testGetTestCaseUnit() throws Exception {
//        Project project = Mockito.mock(Project.class);
//        Mockito.when(project.getBasePath()).thenReturn("./");
//        VideobugLocalClient client = new VideobugLocalClient(System.getenv("HOME") + "/.videobug/sessions", project,
//                null);
//
//        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
//        if (sessions.getItems().size() == 0) {
//            return;
//        }
//        ExecutionSession session = sessions.getItems().get(0);
//        SessionInstance sessionInstance = new SessionInstance(session, project);
//        client.setSessionInstance(sessionInstance);
//
//
//        TestCaseService testCaseService = new TestCaseService(sessionInstance);
//
////        List<TestCandidateMetadata> candidateList = testCaseService.getTestCandidatesForMethod(
////                "com.ayu.cabeza.service.PatientLeadService", "changePatientForCase", true);
//
////        List<TestCandidateMetadata> candidateList = testCaseService.getTestCandidatesForMethod(
////                "com.repyute.helper.snaphrm.SnapHrmHelper", "createLendingProfile", true);
//
//        List<TestCandidateMetadata> candidateList = testCaseService.getTestCandidatesForMethod(
//                "com.ayu.cabeza.service.LoyaltyCardService",
//                "generateCardForCustomer", true);
//
////        List<TestCandidateMetadata> candidateList = testCaseService.getTestCandidatesForMethod(
////                "com.repyute.service.paybooks.PaybooksService", "getLendingProfile", true);
//
//        if (candidateList.size() == 0) {
//            return;
//        }
//
//        TestCandidateMetadata testCandidateMetadata = candidateList.get(0);
//        testCandidateMetadata = sessionInstance.getTestCandidateById(testCandidateMetadata.getEntryProbeIndex(), false);
//
//        List<TestCandidateMetadata> list =
//                testCaseService.getTestCandidatesForMethod(
//                        testCandidateMetadata.getTestSubject()
//                                .getType(), "<init>", true);
//
//        list.add(testCandidateMetadata);
//        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
//                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.Gson, ResourceEmbedMode.IN_FILE
//        );
//        generationConfiguration.getTestCandidateMetadataList().addAll(list);
//        for (TestCandidateMetadata candidateMetadata : list) {
//            generationConfiguration.getCallExpressionList()
//                    .addAll(candidateMetadata.getCallsList());
//        }
//         TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
//
//        copyTestCaseToClipboard(testCaseUnit);
//        ValueResourceContainer valueResourceMap = testCaseUnit.getTestGenerationState()
//                .getValueResourceMap();
//        if (valueResourceMap.getValueResourceMap().size() > 0) {
//            System.out.println(new Gson().toJson(valueResourceMap.getValueResourceMap()));
//        }
//    }

    @Test
    public void testScanAndGenerateAll() throws Exception {

        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("HOME") + "/.videobug/sessions", project,
                null);

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        if (sessions.getItems().size() == 0) {
            return;
        }
        ExecutionSession session = sessions.getItems().get(0);
        client.setSessionInstance(new SessionInstance(session, new ServerMetadata(), project));
        SessionInstance sessionInstance = client.getSessionInstance();
        sessionInstance.unlockNextScan();
        sessionInstance.close();

    }

}
