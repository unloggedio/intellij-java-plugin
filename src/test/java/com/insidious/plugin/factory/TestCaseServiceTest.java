package com.insidious.plugin.factory;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.DaoService;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.testcase.TestCaseRequest;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.util.MethodSpecUtil;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.*;
import com.intellij.openapi.project.Project;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
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
    void testPrintObjectHistory() throws SessionNotSelectedException, SQLException {

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

        Mockito.when(session.getCreatedAt()).thenThrow(Exception.class);
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

    private void printObjectHistory(Long objectId) throws SessionNotSelectedException, SQLException {

        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");

//        TestCaseService testCaseService = new TestCaseService(getDaoService("jdbc:sqlite:execution.db"), client);

//        List<TestCandidate> testCandidateList = new LinkedList<>();
//        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);


//        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

//        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);


        client.setSessionInstance(session);
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

            ObjectInfo valueObjectInfo = replayData.getObjectInfoMap().get(String.valueOf(dataEvent.getValue()));
            if (valueObjectInfo != null) {
                TypeInfo typeObjectInfo = replayData.getTypeInfoMap().get(String.valueOf(valueObjectInfo.getTypeId()));
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
    void testPrintObjectsByType() throws InterruptedException, SessionNotSelectedException, SQLException {

        List<String> targetClasses = List.of("com.appsmith.server.services.UserDataServiceImpl");


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("USERPROFILE") + "/.videobug/sessions");
//        VideobugLocalClient client = new VideobugLocalClient("D:\\workspace\\code\\appsmith\\videobug");

//        TestCaseService testCaseService = new TestCaseService(getDaoService("jdbc:sqlite:execution.db"), client);

//        List<TestCandidate> testCandidateList = new LinkedList<>();
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

    void testMono() {




    }

    @Test
    void testGenerateByObjects() throws InterruptedException, APICallException, IOException, SessionNotSelectedException, SQLException {


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("HOME") + "/.videobug/sessions");
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);

        client.getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                throw new RuntimeException(message);
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                client.setSessionInstance(executionSessionList.get(0));
                waiter.offer("done");
            }
        });

        waiter.take();
        TestCaseService testCaseService = new TestCaseService(client);



//        List<String> targetClasses = List.of("com.appsmith.server.authentication.handlers.ce.AuthenticationSuccessHandlerCE");
//        List<String> targetClasses = List.of("com.appsmith.server.solutions.ce.UserChangedHandlerCEImpl");
//        List<String> targetClasses = List.of("com.appsmith.server.services.UserDataServiceImpl");
//        List<String> targetClasses = List.of("com.appsmith.server.services.ce.FeatureFlagServiceCEImpl");
//        List<String> targetClasses = List.of("org.zerhusen.service.GCDService");
//        List<String> targetClasses = List.of("com.appsmith.server.services.SessionUserService");
//        List<String> targetClasses = List.of("jenkins.org.apache.commons.validator.routines.RegexValidator");
//        List<String> targetClasses = List.of("org.zerhusen.security.UserModelDetailsService");


//        List<String> targetClasses = List.of("com.repyute.service.pocket.PocketService");
//        List<String> targetClasses = List.of("com.repyute.helper.pocket.PocketHelper");
//        List<String> targetClasses = List.of("com.ayu.cabeza.service.CustomerProfileService");
//        List<String> targetClasses = List.of("com.ayu.cabeza.communication.whatsapp.api.WhatsappAPIController");
//        List<String> targetClasses = List.of("com.repyute.service.paybooks.PaybooksService");
//        List<String> targetClasses = List.of("com.repyute.helper.paybooks.PaybooksHelper");
//        List<String> targetClasses = List.of("com.ayu.cabeza.service.CustomerProfileService");
//        List<String> targetClasses = List.of("com.ayu.cabeza.service.AyuCityService");
//        List<String> targetClasses = List.of("com.ayu.cabeza.service.AyuCatalogueService");
//        List<String> targetClasses = List.of("com.ayu.cabeza.service.HospitalProfileService");
        List<String> targetClasses = List.of("com.ayu.cabeza.service.DoctorProfileService");


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

        TestCaseRequest testCaseRequest = new TestCaseRequest(
                allObjects,
                List.of(
                        "com.fasterxml",
                        "com.google"
                ),
                Set.of()
        );
        TestSuite testSuite = testCaseService.generateTestCase(testCaseRequest);

        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
            System.out.println(testCaseScript);
        }


    }


    @Test public void testScanAndGenerateAll() throws Exception {

        File dbFile = new File("execution.db");
        dbFile.delete();


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("HOME") + "/.videobug/sessions");
        TestCaseService testCaseService = new TestCaseService(client);

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);
        client.setSessionInstance(session);


        FilteredDataEventsRequest request = new FilteredDataEventsRequest();
        for (int i = 0; i < 2; i++) {
            request.setThreadId((long) i);
            client.getSessionInstance().scanDataAndBuildReplay();
        }

    }

    @NotNull
    private static DaoService getDaoService(String url) throws SQLException {
        ConnectionSource connectionSource = new JdbcConnectionSource(url);
        DaoService daoService = new DaoService(connectionSource);
        return daoService;
    }

    @Test
    public void writeTestFromDB() throws SQLException {

        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");


        File dbFile = new File("execution.db");
        boolean dbFileExists = dbFile.exists();
//        dbFile.delete();
        // this uses h2 but you can change it to match your database
        String databaseUrl = "jdbc:sqlite:execution.db";
        // create a connection source to our database
        DaoService daoService = getDaoService(databaseUrl);


        VideobugLocalClient client = new VideobugLocalClient(System.getenv("HOME") + "/.videobug/sessions");

        TestCaseService testCaseService = new TestCaseService(client);




        List<Parameter> parameterList =  daoService.getParametersByType("com.ayu.cabeza.service.DoctorProfileService");

        long valueId = (long) parameterList.get(0).getValue();
        Parameter targetParameter = parameterList.get(0);
        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidates =
                daoService.getTestCandidateForSubjectId(valueId);

        testCandidates.sort(Comparator.comparing(TestCandidateMetadata::getEntryProbeIndex));

        ClassName targetClassName = ClassName.bestGuess(targetParameter.getType());
        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer(targetParameter);
        objectRoutineContainer.newRoutine("test" + targetClassName.simpleName());
        for (TestCandidateMetadata testCandidateMetadata : testCandidates) {

            MethodCallExpression methodInfo = (MethodCallExpression) testCandidateMetadata.getMainMethod();
            if (methodInfo.getReturnValue() == null || methodInfo.getReturnValue().getProb() == null) {
                continue;
            }
            if (methodInfo.getMethodName().equals("<init>")) {
                objectRoutineContainer.getConstructor().setTestCandidateList(testCandidateMetadata);
            } else {
                objectRoutineContainer.addMetadata(testCandidateMetadata);
            }

        }


//        VariableContainer variableContainer =  testCaseService.postProcessObjectRoutine(objectRoutineContainer);

        testCaseService.createFieldMocks(objectRoutineContainer);

        // part 3
//        testCaseService.createDependentRoutines(testCaseRequest, variableContainer, objectRoutineContainer);


        ObjectRoutineScriptContainer testCaseScript = objectRoutineContainer.toRoutineScript();


//        if (simpleClassName.contains("$")) {
//            simpleClassName = simpleClassName.split("\\$")[0];
//        }

        String generatedTestClassName =
                "Test" + testCaseScript.getName() + "V";
        TypeSpec.Builder typeSpecBuilder = TypeSpec
                .classBuilder(generatedTestClassName)
                .addModifiers(
                        javax.lang.model.element.Modifier.PUBLIC,
                        javax.lang.model.element.Modifier.FINAL);

        for (Parameter field : testCaseScript.getFields()) {
            if (field == null) {
                continue;
            }
            typeSpecBuilder.addField(field.toFieldSpec().build());
        }


        for (ObjectRoutineScript objectRoutine : testCaseScript.getObjectRoutines()) {
            if (objectRoutine.getName().equals("<init>")) {
                continue;
            }
            MethodSpec methodSpec = objectRoutine.toMethodSpec().build();
            typeSpecBuilder.addMethod(methodSpec);
        }

        typeSpecBuilder.addMethod(MethodSpecUtil.createInjectFieldMethod());

        if (objectRoutineContainer.getVariablesOfType("okhttp3.").size() > 0) {
            typeSpecBuilder.addMethod(MethodSpecUtil.createOkHttpMockCreator());
        }


        ClassName gsonClass = ClassName.get("com.google.gson", "Gson");


        typeSpecBuilder
                .addField(FieldSpec
                        .builder(gsonClass,
                                "gson", javax.lang.model.element.Modifier.PRIVATE)
                        .initializer("new $T()", gsonClass)
                        .build());


        TypeSpec helloWorld = typeSpecBuilder.build();

        JavaFile javaFile = JavaFile.builder(objectRoutineContainer.getPackageName(), helloWorld)
                .addStaticImport(ClassName.bestGuess("org.mockito.ArgumentMatchers"), "*")
                .build();


        TestCaseUnit testCaseUnit = new TestCaseUnit(
                javaFile.toString(), objectRoutineContainer.getPackageName(), generatedTestClassName);

        System.out.println(testCaseUnit);

        StringSelection selection = new StringSelection(testCaseUnit.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);

    }


}
