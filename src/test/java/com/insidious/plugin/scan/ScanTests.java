package com.insidious.plugin.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.UnloggedLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.ActiveSessionManager;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.scan.model.*;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.insidious.plugin.util.ClassTypeUtils;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ScanTests {

    public static String SESSIONS_PATH;
    public static int calls = 0;

    @BeforeAll
    public static void beforeAll() {
        SESSIONS_PATH = Thread.currentThread().getContextClassLoader()
                .getResource("test-sessions/").getPath();
    }

    @Test
    public void runScanTests() throws SQLException, IOException, InterruptedException {

        List<ScanTestResult> scanTestResults = new ArrayList<>();
        for (ScanTestModel scanTestModel : getTests()) {
            Map<MethodReference, AssertionResult> assertionResults = assertScannedValuesFromSession(scanTestModel.getAssertions(), scanTestModel.getSessionFolder());
            ScanTestResult scanTestResult = new ScanTestResult(scanTestModel.getSessionFolder(), assertionResults, scanTestModel);
            scanTestResults.add(scanTestResult);

            Thread.sleep(500);
        }

        Map<ScanTestModel, Boolean> sessionWiseStatus = new HashMap<>();

        AtomicBoolean overallPassing = new AtomicBoolean(true);
        scanTestResults.forEach(scanTestResult -> {
            AtomicBoolean sessionPassing = new AtomicBoolean(true);
            System.out.println("Results from session : " + scanTestResult.getSessionFolder() + "\n");
            if (scanTestResult.getAssertionResults().size() < scanTestResult.getScanTestModel().getAssertions().size()) {
                System.out.println("Some candidates were not found");
                System.out.println("Missing candidate count : " + (scanTestResult.getScanTestModel().getAssertions().size() - scanTestResult.getAssertionResults().size()));
                System.out.println("Missing Candidates -> \n");
                Set<MethodReference> originalAssertions = scanTestResult.getScanTestModel().getAssertions().keySet();
                Set<MethodReference> actualAssertions = scanTestResult.getAssertionResults().keySet();
                originalAssertions.removeAll(actualAssertions);
                originalAssertions.forEach((key) -> {
                    System.out.println("Classname : " + key.getContainingClass());
                    System.out.println("Methodname : " + key.getMethodName());
                    System.out.println("Expected value : " + scanTestResult.getScanTestModel().getAssertions().get(key));
                    System.out.println("Status : Missing\n");
                });
                overallPassing.set(false);
                sessionPassing.set(false);
            }
            System.out.println("\nCompleted Assertions -> \n");
            AtomicInteger count = new AtomicInteger(1);
            scanTestResult.getAssertionResults().forEach((key, value) -> {
                System.out.println("* Case - " + count.getAndIncrement());
                System.out.println("Classname : " + key.getContainingClass());
                System.out.println("Methodname : " + key.getMethodName());
                System.out.println("->");
                System.out.println("Expected Value : " + value.getExpectedValue());
                System.out.println("Recorded Value : " + value.getScannedValue());
                System.out.println("Expected Count : " + value.getExpectedCount());
                System.out.println("Recorded Count : " + value.getActualCount());
                System.out.println("Status : " + (value.isPassing() ? "Passing" : "Failing"));
                System.out.println("\n");
                if (!value.isPassing()) {
                    overallPassing.set(false);
                    sessionPassing.set(false);
                }
            });
            System.out.println("---------------------");
            sessionWiseStatus.put(scanTestResult.getScanTestModel(), sessionPassing.get());
        });
        System.out.println("\nStatus by session -> \n");
        sessionWiseStatus.forEach((key, value) -> {
            System.out.println("Session : " + key.getSessionFolder());
            System.out.println("Status : " + (value ? "Passing" : "Failing") + "\n");
        });
        Assertions.assertEquals(true, overallPassing.get());
    }

    public Map<MethodReference, AssertionResult> assertScannedValuesFromSession(Map<MethodReference, AssertionOptions> assetions, String sessionFolder) throws SQLException, IOException, InterruptedException {
        System.out.println("[Testing session] : " + sessionFolder);
        String sessionPath = SESSIONS_PATH + sessionFolder;
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getName()).thenReturn("test-project");
        new UnloggedLocalClient(sessionPath);
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setPath(sessionPath);
        executionSession.setSessionId("1");
        executionSession.setHostname("test-host");
        executionSession.setCreatedAt(new Date());
        executionSession.setLastUpdateAt(0);
        ServerMetadata serverMetadata = new ServerMetadata();
        serverMetadata.setAgentServerUrl("http://localhost:12100");
        serverMetadata.setAgentServerPort("12100");
        SessionInstance sessionInstance = new SessionInstance(executionSession, serverMetadata, project);
        sessionInstance.unlockNextScan();

        final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(1);
        threadPoolExecutor.submit(sessionInstance);

        int zipCount = new File(sessionPath).listFiles().length;
        while (sessionInstance.getProcessedFileCount() < zipCount) {
            continue;
        }

        CountDownLatch cdl = new CountDownLatch(1);
        Map<MethodReference, AssertionResult> assertionResults = new HashMap<>();
        AtomicBoolean waiting = new AtomicBoolean(false);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable timeoutCheck = new Runnable() {
            @Override
            public void run() {
                System.out.println("Scheduled call : " + calls++);
                if (waiting.get()) {
                    System.out.println("Done From timeout");
                    sessionInstance.close();
                    cleanUpFolder(new File(sessionPath));
                }
            }
        };
        AtomicReference<ScheduledFuture<?>> lastFuture = new AtomicReference<>(null);
        sessionInstance.getTestCandidates(testCandidateMetadata -> {
            System.out.println("In candidate list for : " + sessionFolder);
            waiting.set(false);
            assetions.forEach((key, value) -> {
                String classname = key.getContainingClass();
                String methodname = key.getMethodName();
                String expectedValue = value.getExpectedValue();
                Long count = value.getCount();

                if (assertionResults.containsKey(key)) {
                    return;
                }

                List<TestCandidateBareBone> candidateBareBoneList = testCandidateMetadata.stream().filter(bareBonesCandidate -> {
                    if (bareBonesCandidate.getMethodUnderTest().getClassName().equals(classname)
                            && bareBonesCandidate.getMethodUnderTest().getName().equals(methodname)) {
                        return true;
                    }
                    return false;
                }).toList();

                boolean passing = false;
                String scannedValueReference = null;
                for (TestCandidateBareBone bareBoneCandidate : candidateBareBoneList) {
                    TestCandidateMetadata testCandidateMetadataLoaded = sessionInstance.getTestCandidateById(bareBoneCandidate.getId(), true);
                    String scannedValue = getOutputText(testCandidateMetadataLoaded);
                    scannedValueReference = scannedValue;
                    if (scannedValue.equals(expectedValue)) {
                        passing = true;
                        break;
                    }
                }
                if (!candidateBareBoneList.isEmpty()) {
                    Long current = (long) candidateBareBoneList.size();
                    if (assertionResults.containsKey(key)) {
                        if (assertionResults.get(key).getActualCount() == null) {
                            assertionResults.get(key).setActualCount(0L);
                        }
                        current += assertionResults.get(key).getActualCount();
                    }
                    if (count != null) {
                        if (!current.equals(count)) {
                            passing = false;
                        }
                    }
                    assertionResults.put(key, new AssertionResult(value.getExpectedValue(), scannedValueReference, passing, count, current));
                }
            });
            cdl.countDown();
            if (assertionResults.size() == assetions.size()) {
                sessionInstance.close();
                cleanUpFolder(new File(sessionPath));
            }
            waiting.set(true);
            if (lastFuture.get() != null) {
                lastFuture.get().cancel(true);
            }
            lastFuture.set(executor.schedule(timeoutCheck, 30, TimeUnit.SECONDS));
        }, 0, new StompFilterModel(), new AtomicInteger(1));
        if (lastFuture.get() != null) {
            lastFuture.get().cancel(true);
        }
        cdl.await();
        return assertionResults;
    }

    private void cleanUpFolder(File folder) {
        File[] files = folder.listFiles();
        if (files == null || !folder.isDirectory()) {
            return;
        }
        for (File file : files) {
            if (!file.getName().endsWith(".zip")) {
                System.err.println("Delete file " + file.getAbsolutePath());
                cleanUpFolder(file);
                file.delete();
            }
        }
    }

    private String getOutputText(TestCandidateMetadata metadata) {
        MethodCallExpression mainMethod = metadata.getMainMethod();
        JsonNode valueForParameter = ClassTypeUtils.getValueForParameter(mainMethod.getReturnValue());
        if (valueForParameter.isNumber()
                && (mainMethod.getReturnValue().getType() == null ||
                (mainMethod.getReturnValue().getType().length() != 1 &&
                        !mainMethod.getReturnValue().getType().startsWith("java.lang")))
        ) {
        } else {
            return valueForParameter.toString();
        }
        return "Not serializable";
    }

    private List<ScanTestModel> getTests() {
        List<ScanTestModel> scanTests = new ArrayList<>();
        Map<MethodReference, AssertionOptions> assertions = new HashMap<>();

        assertions = new HashMap<>();
        assertions.put(new MethodReference("getFutureResult",
                "org.unlogged.demo.controller.FutureController"), new AssertionOptions("\"yolo\"", 4L));
        assertions.put(new MethodReference("getFutureResultOptional",
                "org.unlogged.demo.controller.FutureController"), new AssertionOptions("\"string\"", 3L));
        assertions.put(new MethodReference("getUrl",
                "org.unlogged.demo.controller.SiteUrl"), new AssertionOptions("\"https://localhost:8080\"", 2L));

        //SDK 0.6.100 - With process counter = 4, class counter = 3 and method counter = 2
        ScanTestModel freqLogging = new ScanTestModel("freq-logging-maven-demo", assertions);
        scanTests.add(freqLogging);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("getDefaultModel",
                        "org.unlogged.demo.controller.ModelMapperOpsController"),
                new AssertionOptions("{\"id\":1,\"username\":\"User1\",\"address\":{\"house\":\"#144\",\"street\":\"StreetX\",\"area\":\"AreaX\",\"city\":\"CityX\",\"state\":\"StateX\",\"country\":\"CountryX\",\"pincode\":\"PinX\"},\"contactInformation\":{\"emails\":[\"user1@gmail.com\",\"user1@yahoo.com\"],\"numbers\":[\"phone-number-1\",\"phone-number-2\"]}}", null));

        assertions.put(new MethodReference("getFromConverter",
                        "org.unlogged.demo.controller.ModelMapperOpsController"),
                new AssertionOptions("{\"username\":\"User1\",\"phoneNumber\":\"phone-number-1\",\"email\":\"user1@gmail.com\",\"address\":\"##144, StateX, AreaX, CityX, StateX, CountryX - Pin-code : PinX\"}", null));

        assertions.put(new MethodReference("getEmptyUserModelDto",
                        "org.unlogged.demo.controller.ModelMapperOpsController"),
                new AssertionOptions("{\"username\":null,\"phoneNumber\":null,\"email\":null,\"address\":null}", null));

        assertions.put(new MethodReference("getUserModelDtoWithProvider",
                        "org.unlogged.demo.controller.ModelMapperOpsController"),
                new AssertionOptions("{\"username\":null,\"phoneNumber\":null,\"email\":null,\"address\":\"##144, StateX, AreaX, CityX, StateX, CountryX - Pin-code : PinX\"}", null));

        assertions.put(new MethodReference("getUserModelMiniDto",
                        "org.unlogged.demo.controller.ModelMapperOpsController"),
                new AssertionOptions("{\"id\":1,\"username\":\"user1\"}", null));

        //SDK 0.6.3 - Model mapper candidates
        ScanTestModel modelMapperNonReactive = new ScanTestModel("modelmapper-non-reactive", assertions);
        scanTests.add(modelMapperNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("deleteById",
                        "org.unlogged.demo.controller.MongoOpsController"),
                new AssertionOptions("0", null));

        assertions.put(new MethodReference("updatePojo",
                        "org.unlogged.demo.controller.MongoOpsController"),
                new AssertionOptions("{\"id\":\"string\",\"name\":\"string\"}", null));

        assertions.put(new MethodReference("getById",
                        "org.unlogged.demo.controller.MongoOpsController"),
                new AssertionOptions("{\"id\":\"string\",\"name\":\"string\"}", null));

        assertions.put(new MethodReference("getall",
                        "org.unlogged.demo.controller.MongoOpsController"),
                new AssertionOptions("[{\"id\":\"aaa\",\"name\":\"Name AAA\"},{\"id\":\"string\",\"name\":\"string\"}]", null));

        assertions.put(new MethodReference("insertNew",
                        "org.unlogged.demo.controller.MongoOpsController"),
                new AssertionOptions("{\"id\":\"string\",\"name\":\"string\"}", null));

        //SDK 0.6.3 - Mongo Crud Non reactive
        ScanTestModel mongoCrudNonReactive = new ScanTestModel("mongo-crud-non-reactive", assertions);
        scanTests.add(mongoCrudNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("chain",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("\"default\"", null));

        assertions.put(new MethodReference("getNonEmptyOptionalUser",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("{\"name\":\"a\",\"email\":\"c\",\"number\":\"e\"}", null));

        assertions.put(new MethodReference("flatMapUsage",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("\"a\"", null));

        assertions.put(new MethodReference("countNameLength",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("5", null));

        assertions.put(new MethodReference("getDefaultUser",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("{\"user_id\":1,\"username\":\"User1\",\"password\":\"user\",\"email\":\"user@gmail.com\"}", null));

        assertions.put(new MethodReference("filterUserOptional",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("[true,false]", null));

        assertions.put(new MethodReference("getUserUsage",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("{\"user_id\":1,\"username\":\"User1\",\"password\":\"user\",\"email\":\"user@gmail.com\"}", null));

        assertions.put(new MethodReference("throwOnNull",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("{\"cause\":null,\"stackTrace\":[{\"classLoaderName\":null,\"moduleName\":\"java.base\",\"moduleVersion\":\"17.0.7\",\"methodName\":\"orElseThrow\",\"fileName\":\"Optional.java\",\"lineNumber\":403,\"nativeMethod\":false,\"className\":\"java.util.Optional\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"Unlogged$Probed$$throwOnNull\",\"fileName\":\"OptionalOpsController.java\",\"lineNumber\":74,\"nativeMethod\":false,\"className\":\"org.unlogged.demo.controller.OptionalOpsController\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"throwOnNull\",\"fileName\":\"OptionalOpsController.java\",\"lineNumber\":-1,\"nativeMethod\":false,\"className\":\"org.unlogged.demo.controller.OptionalOpsController\"},{\"classLoaderName\":null,\"moduleName\":\"java.base\",\"moduleVersion\":\"17.0.7\",\"methodName\":\"invoke0\",\"fileName\":\"NativeMethodAccessorImpl.java\",\"lineNumber\":-2,\"nativeMethod\":true,\"className\":\"jdk.internal.reflect.NativeMethodAccessorImpl\"},{\"classLoaderName\":null,\"moduleName\":\"java.base\",\"moduleVersion\":\"17.0.7\",\"methodName\":\"invoke\",\"fileName\":\"NativeMethodAccessorImpl.java\",\"lineNumber\":77,\"nativeMethod\":false,\"className\":\"jdk.internal.reflect.NativeMethodAccessorImpl\"},{\"classLoaderName\":null,\"moduleName\":\"java.base\",\"moduleVersion\":\"17.0.7\",\"methodName\":\"invoke\",\"fileName\":\"DelegatingMethodAccessorImpl.java\",\"lineNumber\":43,\"nativeMethod\":false,\"className\":\"jdk.internal.reflect.DelegatingMethodAccessorImpl\"},{\"classLoaderName\":null,\"moduleName\":\"java.base\",\"moduleVersion\":\"17.0.7\",\"methodName\":\"invoke\",\"fileName\":\"Method.java\",\"lineNumber\":568,\"nativeMethod\":false,\"className\":\"java.lang.reflect.Method\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"executeCommandRaw\",\"fileName\":\"AgentCommandExecutorImpl.java\",\"lineNumber\":407,\"nativeMethod\":false,\"className\":\"io.unlogged.AgentCommandExecutorImpl\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"executeCommand\",\"fileName\":\"AgentCommandExecutorImpl.java\",\"lineNumber\":477,\"nativeMethod\":false,\"className\":\"io.unlogged.AgentCommandExecutorImpl\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"serve\",\"fileName\":\"AgentCommandServer.java\",\"lineNumber\":74,\"nativeMethod\":false,\"className\":\"io.unlogged.command.AgentCommandServer\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"execute\",\"fileName\":\"NanoHTTPD.java\",\"lineNumber\":945,\"nativeMethod\":false,\"className\":\"fi.iki.elonen.NanoHTTPD$HTTPSession\"},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"run\",\"fileName\":\"NanoHTTPD.java\",\"lineNumber\":192,\"nativeMethod\":false,\"className\":\"fi.iki.elonen.NanoHTTPD$ClientHandler\"},{\"classLoaderName\":null,\"moduleName\":\"java.base\",\"moduleVersion\":\"17.0.7\",\"methodName\":\"run\",\"fileName\":\"Thread.java\",\"lineNumber\":833,\"nativeMethod\":false,\"className\":\"java.lang.Thread\"}],\"message\":null,\"suppressed\":[],\"localizedMessage\":null}", null));

        assertions.put(new MethodReference("orElseGet",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("{\"user_id\":1,\"username\":\"User1\",\"password\":\"user\",\"email\":\"user@gmail.com\"}", null));

        assertions.put(new MethodReference("orElseCase",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("\"Default\"", null));

        assertions.put(new MethodReference("orElseCase",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("\"Default\"", null));

        assertions.put(new MethodReference("ifPresent",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("\"Ace###\"", null));

        assertions.put(new MethodReference("getEmptyStatus",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("[false,true]", null));

        assertions.put(new MethodReference("getPresentStatus",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("[true,false]", null));

        assertions.put(new MethodReference("createNullable",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("null", null));

        assertions.put(new MethodReference("create1",
                        "org.unlogged.demo.controller.OptionalOpsController"),
                new AssertionOptions("\"default\"", null));

        //SDK 0.6.3 - Optional Usage Non reactive
        ScanTestModel optionalNonReactive = new ScanTestModel("optional-non-reactive", assertions);
        scanTests.add(optionalNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("createWithCode",
                        "org.unlogged.demo.controller.ResponseEntityOps"),
                new AssertionOptions("{\"headers\":{},\"body\":{\"user_id\":1,\"username\":\"u1\",\"password\":\"p1\",\"email\":\"e1\"},\"status\":\"CREATED\"}", null));

        assertions.put(new MethodReference("getUserOf",
                        "org.unlogged.demo.controller.ResponseEntityOps"),
                new AssertionOptions("{\"headers\":{},\"body\":{\"type\":\"about:blank\",\"title\":null,\"status\":200,\"detail\":null,\"instance\":null,\"properties\":null},\"status\":200}", null));

        assertions.put(new MethodReference("getOkUser",
                        "org.unlogged.demo.controller.ResponseEntityOps"),
                new AssertionOptions("{\"headers\":{},\"body\":{\"user_id\":1,\"username\":\"u1\",\"password\":\"p1\",\"email\":\"e1\"},\"status\":\"OK\"}", null));

        assertions.put(new MethodReference("getOkString",
                        "org.unlogged.demo.controller.ResponseEntityOps"),
                new AssertionOptions("{\"headers\":{},\"body\":\"ok\",\"status\":\"OK\"}", null));

        //SDK 0.6.3 - Response Entity Non reactive candidates
        ScanTestModel responseEntityNonReactive = new ScanTestModel("responseEntity-non-reactive", assertions);
        scanTests.add(responseEntityNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("groupBy",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("{\"a\":[{\"user_id\":9903,\"username\":\"arb\",\"password\":\"YK04\",\"email\":\"SHA@gmail.com\"}],\"c\":[{\"user_id\":401,\"username\":\"CLP\",\"password\":\"C401\",\"email\":\"CLP401@gmail.com\"}],\"d\":[{\"user_id\":403,\"username\":\"DED\",\"password\":\"D403\",\"email\":\"DED403@gmail.com\"}],\"t\":[{\"user_id\":402,\"username\":\"TRP\",\"password\":\"T402\",\"email\":\"TRP402@gmail.com\"}],\"u\":[{\"user_id\":1,\"username\":\"User1\",\"password\":\"User1pass\",\"email\":\"User1@gmail.com\"},{\"user_id\":2,\"username\":\"User2\",\"password\":\"User2pass\",\"email\":\"User2@gmail.com\"},{\"user_id\":3,\"username\":\"User3\",\"password\":\"User3pass\",\"email\":\"User3@gmail.com\"}],\"l\":[{\"user_id\":9901,\"username\":\"lck\",\"password\":\"S???\",\"email\":\"SL?@gmail.com\"}],\"m\":[{\"user_id\":9902,\"username\":\"msc\",\"password\":\"S117\",\"email\":\"S117@gmail.com\"}]}", null));

        assertions.put(new MethodReference("getUserList",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[{\"user_id\":11,\"username\":\"UserA\",\"password\":\"a\",\"email\":\"userA@gmail.com\"},{\"user_id\":23,\"username\":\"UserB\",\"password\":\"b\",\"email\":\"userB@gmail.com\"},{\"user_id\":14,\"username\":\"UserC\",\"password\":\"c\",\"email\":\"userC@gmail.com\"},{\"user_id\":59,\"username\":\"UserD\",\"password\":\"d\",\"email\":\"userD@gmail.com\"},{\"user_id\":64,\"username\":\"UserE\",\"password\":\"e\",\"email\":\"userE@gmail.com\"},{\"user_id\":80,\"username\":\"UserF\",\"password\":\"f\",\"email\":\"userF@gmail.com\"},{\"user_id\":24,\"username\":\"UserG\",\"password\":\"g\",\"email\":\"userG@gmail.com\"}]", null));

        assertions.put(new MethodReference("getSortedIdOrder",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("\"11 < 14 < 23 < 24 < 59 < 64 < 80\"", null));

        assertions.put(new MethodReference("getUserGroups",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[[{\"user_id\":1,\"username\":\"User1\",\"password\":\"User1pass\",\"email\":\"User1@gmail.com\"},{\"user_id\":2,\"username\":\"User2\",\"password\":\"User2pass\",\"email\":\"User2@gmail.com\"},{\"user_id\":3,\"username\":\"User3\",\"password\":\"User3pass\",\"email\":\"User3@gmail.com\"}],[{\"user_id\":401,\"username\":\"CLP\",\"password\":\"C401\",\"email\":\"CLP401@gmail.com\"},{\"user_id\":402,\"username\":\"TRP\",\"password\":\"T402\",\"email\":\"TRP402@gmail.com\"},{\"user_id\":403,\"username\":\"DED\",\"password\":\"D403\",\"email\":\"DED403@gmail.com\"}],[{\"user_id\":9901,\"username\":\"lck\",\"password\":\"S???\",\"email\":\"SL?@gmail.com\"},{\"user_id\":9902,\"username\":\"msc\",\"password\":\"S117\",\"email\":\"S117@gmail.com\"},{\"user_id\":9903,\"username\":\"arb\",\"password\":\"YK04\",\"email\":\"SHA@gmail.com\"}]]", null));

        assertions.put(new MethodReference("reduceUsage",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("30918", null));

        assertions.put(new MethodReference("matchCases",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("{\"all-false\":false,\"all-true\":true,\"any-false\":false,\"any-true\":true,\"none-false\":false,\"none-true\":true}", null));

        assertions.put(new MethodReference("distinctUsage",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[2,5,3,4]", null));

        assertions.put(new MethodReference("limitUsers",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[]", null));

        assertions.put(new MethodReference("countUsersInGroups",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("9", null));

        assertions.put(new MethodReference("peek_all",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[[{\"user_id\":1,\"username\":\"User1#\",\"password\":\"user1pass_1397\",\"email\":\"user1pass_1397\"},{\"user_id\":2,\"username\":\"User2#\",\"password\":\"user2pass_1397\",\"email\":\"user2pass_1397\"},{\"user_id\":3,\"username\":\"User3#\",\"password\":\"user3pass_1397\",\"email\":\"user3pass_1397\"}],[{\"user_id\":401,\"username\":\"CLP#\",\"password\":\"c401_1397\",\"email\":\"c401_1397\"},{\"user_id\":402,\"username\":\"TRP#\",\"password\":\"t402_1397\",\"email\":\"t402_1397\"},{\"user_id\":403,\"username\":\"DED#\",\"password\":\"d403_1397\",\"email\":\"d403_1397\"}],[{\"user_id\":9901,\"username\":\"lck#\",\"password\":\"s???_1397\",\"email\":\"s???_1397\"},{\"user_id\":9902,\"username\":\"msc#\",\"password\":\"s117_1397\",\"email\":\"s117_1397\"},{\"user_id\":9903,\"username\":\"arb#\",\"password\":\"yk04_1397\",\"email\":\"yk04_1397\"}]]", null));

        assertions.put(new MethodReference("flatmap_minId",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("{\"user_id\":1,\"username\":\"User1\",\"password\":\"User1pass\",\"email\":\"User1@gmail.com\"}", null));

        assertions.put(new MethodReference("flatmap_maxId",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("{\"user_id\":9903,\"username\":\"arb\",\"password\":\"YK04\",\"email\":\"SHA@gmail.com\"}", null));

        assertions.put(new MethodReference("toArrayCollection_Usernames",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[\"UserA\",\"UserB\",\"UserC\",\"UserD\",\"UserE\",\"UserF\",\"UserG\"]", null));

        assertions.put(new MethodReference("filterAndFindFirst",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("{\"user_id\":14,\"username\":\"UserC\",\"password\":\"c\",\"email\":\"userC@gmail.com\"}", null));

        assertions.put(new MethodReference("mapAndFilter",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[11,23,59]", null));

        assertions.put(new MethodReference("mapVector",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[11,23,14,59,64,80,24]", null));

        assertions.put(new MethodReference("mapSet",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[64,80,23,24,11,59,14]", null));

        assertions.put(new MethodReference("mapAndCollect",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[11,23,14,59,64,80,24]", null));

        assertions.put(new MethodReference("forEachRunParallel",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[{\"user_id\":11,\"username\":\"UserA#\",\"password\":\"a_1459\",\"email\":\"userA-@gmail.com\"},{\"user_id\":23,\"username\":\"UserB#\",\"password\":\"b_1459\",\"email\":\"userB-@gmail.com\"},{\"user_id\":14,\"username\":\"UserC#\",\"password\":\"C_1397\",\"email\":\"userC+@gmail.com\"},{\"user_id\":59,\"username\":\"UserD#\",\"password\":\"d_1459\",\"email\":\"userD-@gmail.com\"},{\"user_id\":64,\"username\":\"UserE#\",\"password\":\"E_1397\",\"email\":\"userE+@gmail.com\"},{\"user_id\":80,\"username\":\"UserF#\",\"password\":\"F_1397\",\"email\":\"userF+@gmail.com\"},{\"user_id\":24,\"username\":\"UserG#\",\"password\":\"G_1397\",\"email\":\"userG+@gmail.com\"}]", null));

        assertions.put(new MethodReference("forEachRun",
                        "org.unlogged.demo.controller.StreamOpsController"),
                new AssertionOptions("[{\"user_id\":11,\"username\":\"UserA#\",\"password\":\"a_1459\",\"email\":\"userA-@gmail.com\"},{\"user_id\":23,\"username\":\"UserB#\",\"password\":\"b_1459\",\"email\":\"userB-@gmail.com\"},{\"user_id\":14,\"username\":\"UserC#\",\"password\":\"C_1397\",\"email\":\"userC+@gmail.com\"},{\"user_id\":59,\"username\":\"UserD#\",\"password\":\"d_1459\",\"email\":\"userD-@gmail.com\"},{\"user_id\":64,\"username\":\"UserE#\",\"password\":\"E_1397\",\"email\":\"userE+@gmail.com\"},{\"user_id\":80,\"username\":\"UserF#\",\"password\":\"F_1397\",\"email\":\"userF+@gmail.com\"},{\"user_id\":24,\"username\":\"UserG#\",\"password\":\"G_1397\",\"email\":\"userG+@gmail.com\"}]", null));

        //SDK 0.6.3 - Streams Non reactive candidates
        ScanTestModel streamNonReactive = new ScanTestModel("stream-non-reactive", assertions);
        scanTests.add(streamNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("getCustomers",
                        "org.unlogged.demo.controller.VarOpsController"),
                new AssertionOptions("{\"headers\":{},\"body\":[{\"customerid\":1,\"customername\":\"c1\",\"dateofbirth\":\"dob1\",\"email\":\"email1\",\"contactnumber\":\"001\",\"address\":\"address\",\"referralcodes\":[\"1\",\"2\"],\"createdDate\":\"Jun 27, 2024 18:19:05 pm\",\"updatedDate\":\"Jun 27, 2024 18:19:05 pm\"},{\"customerid\":2,\"customername\":\"c2\",\"dateofbirth\":\"dob2\",\"email\":\"email2\",\"contactnumber\":\"002\",\"address\":\"address\",\"referralcodes\":[\"1\",\"2\"],\"createdDate\":\"Jun 27, 2024 18:19:05 pm\",\"updatedDate\":\"Jun 27, 2024 18:19:05 pm\"}],\"status\":\"OK\"}", null));

        assertions.put(new MethodReference("getAsResponseEntity",
                        "org.unlogged.demo.controller.VarOpsController"),
                new AssertionOptions("{\"headers\":{},\"body\":{\"Even\":[{\"user_id\":14,\"username\":\"UserC\",\"password\":\"c\",\"email\":\"userC@gmail.com\"},{\"user_id\":64,\"username\":\"UserE\",\"password\":\"e\",\"email\":\"userE@gmail.com\"},{\"user_id\":80,\"username\":\"UserF\",\"password\":\"f\",\"email\":\"userF@gmail.com\"},{\"user_id\":24,\"username\":\"UserG\",\"password\":\"g\",\"email\":\"userG@gmail.com\"}],\"Odd\":[{\"user_id\":11,\"username\":\"UserA\",\"password\":\"a\",\"email\":\"userA@gmail.com\"},{\"user_id\":23,\"username\":\"UserB\",\"password\":\"b\",\"email\":\"userB@gmail.com\"},{\"user_id\":59,\"username\":\"UserD\",\"password\":\"d\",\"email\":\"userD@gmail.com\"}]},\"status\":\"OK\"}", null));

        assertions.put(new MethodReference("varListAndMap",
                        "org.unlogged.demo.controller.VarOpsController"),
                new AssertionOptions("{\"Even\":[{\"user_id\":14,\"username\":\"UserC\",\"password\":\"c\",\"email\":\"userC@gmail.com\"},{\"user_id\":64,\"username\":\"UserE\",\"password\":\"e\",\"email\":\"userE@gmail.com\"},{\"user_id\":80,\"username\":\"UserF\",\"password\":\"f\",\"email\":\"userF@gmail.com\"},{\"user_id\":24,\"username\":\"UserG\",\"password\":\"g\",\"email\":\"userG@gmail.com\"}],\"Odd\":[{\"user_id\":11,\"username\":\"UserA\",\"password\":\"a\",\"email\":\"userA@gmail.com\"},{\"user_id\":23,\"username\":\"UserB\",\"password\":\"b\",\"email\":\"userB@gmail.com\"},{\"user_id\":59,\"username\":\"UserD\",\"password\":\"d\",\"email\":\"userD@gmail.com\"}]}", null));

        assertions.put(new MethodReference("getAUser",
                        "org.unlogged.demo.controller.VarOpsController"),
                new AssertionOptions("{\"user_id\":199,\"username\":\"user199\",\"password\":\"userpass199\",\"email\":\"user199@gmail.com\"}", null));

        assertions.put(new MethodReference("primitivesWrapped",
                        "org.unlogged.demo.controller.VarOpsController"),
                new AssertionOptions("[1,1,1.0,1.0,\"a\",\"String\",[\"v1\",\"v2\",\"v3\"]]", null));

        //SDK - 0.6.3 - Var non reactive candidates
        ScanTestModel varNonReactice = new ScanTestModel("var-non-reactive", assertions);
        scanTests.add(varNonReactice);

        //requires string format
        assertions = new HashMap<>();
        assertions.put(new MethodReference("findById",
                        "org.unlogged.demo.resttemplate.PostsClient"),
                new AssertionOptions("\"{\\n  \\\"userId\\\": 1,\\n  \\\"id\\\": 1,\\n  \\\"title\\\": \\\"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\\\",\\n  \\\"body\\\": \\\"quia et suscipit\\\\nsuscipit recusandae consequuntur expedita et cum\\\\nreprehenderit molestiae ut ut quas totam\\\\nnostrum rerum est autem sunt rem eveniet architecto\\\"\\n}\"", null));

        assertions.put(new MethodReference("findAll",
                        "org.unlogged.demo.resttemplate.PostsClient"),
                new AssertionOptions("[{\"userId\":1,\"id\":1,\"title\":\"sunt aut facere repellat provident occaecati excepturi optio reprehenderit\",\"body\":\"quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto\"},{\"userId\":1,\"id\":2,\"title\":\"qui est esse\",\"body\":\"est rerum tempore vitae\\nsequi sint nihil reprehenderit dolor beatae ea dolores neque\\nfugiat blanditiis voluptate porro vel nihil molestiae ut reiciendis\\nqui aperiam non debitis possimus qui neque nisi nulla\"},{\"userId\":1,\"id\":3,\"title\":\"ea molestias quasi exercitationem repellat qui ipsa sit aut\",\"body\":\"et iusto sed quo iure\\nvoluptatem occaecati omnis eligendi aut ad\\nvoluptatem doloribus vel accusantium quis pariatur\\nmolestiae porro eius odio et labore et velit aut\"},{\"userId\":1,\"id\":4,\"title\":\"eum et est occaecati\",\"body\":\"ullam et saepe reiciendis voluptatem adipisci\\nsit amet autem assumenda provident rerum culpa\\nquis hic commodi nesciunt rem tenetur doloremque ipsam iure\\nquis sunt voluptatem rerum illo velit\"},{\"userId\":1,\"id\":5,\"title\":\"nesciunt quas odio\",\"body\":\"repudiandae veniam quaerat sunt sed\\nalias aut fugiat sit autem sed est\\nvoluptatem omnis possimus esse voluptatibus quis\\nest aut tenetur dolor neque\"},{\"userId\":1,\"id\":6,\"title\":\"dolorem eum magni eos aperiam quia\",\"body\":\"ut aspernatur corporis harum nihil quis provident sequi\\nmollitia nobis aliquid molestiae\\nperspiciatis et ea nemo ab reprehenderit accusantium quas\\nvoluptate dolores velit et doloremque molestiae\"},{\"userId\":1,\"id\":7,\"title\":\"magnam facilis autem\",\"body\":\"dolore placeat quibusdam ea quo vitae\\nmagni quis enim qui quis quo nemo aut saepe\\nquidem repellat excepturi ut quia\\nsunt ut sequi eos ea sed quas\"},{\"userId\":1,\"id\":8,\"title\":\"dolorem dolore est ipsam\",\"body\":\"dignissimos aperiam dolorem qui eum\\nfacilis quibusdam animi sint suscipit qui sint possimus cum\\nquaerat magni maiores excepturi\\nipsam ut commodi dolor voluptatum modi aut vitae\"},{\"userId\":1,\"id\":9,\"title\":\"nesciunt iure omnis dolorem tempora et accusantium\",\"body\":\"consectetur animi nesciunt iure dolore\\nenim quia ad\\nveniam autem ut quam aut nobis\\net est aut quod aut provident voluptas autem voluptas\"},{\"userId\":1,\"id\":10,\"title\":\"optio molestias id quia eum\",\"body\":\"quo et expedita modi cum officia vel magni\\ndoloribus qui repudiandae\\nvero nisi sit\\nquos veniam quod sed accusamus veritatis error\"},{\"userId\":2,\"id\":11,\"title\":\"et ea vero quia laudantium autem\",\"body\":\"delectus reiciendis molestiae occaecati non minima eveniet qui voluptatibus\\naccusamus in eum beatae sit\\nvel qui neque voluptates ut commodi qui incidunt\\nut animi commodi\"},{\"userId\":2,\"id\":12,\"title\":\"in quibusdam tempore odit est dolorem\",\"body\":\"itaque id aut magnam\\npraesentium quia et ea odit et ea voluptas et\\nsapiente quia nihil amet occaecati quia id voluptatem\\nincidunt ea est distinctio odio\"},{\"userId\":2,\"id\":13,\"title\":\"dolorum ut in voluptas mollitia et saepe quo animi\",\"body\":\"aut dicta possimus sint mollitia voluptas commodi quo doloremque\\niste corrupti reiciendis voluptatem eius rerum\\nsit cumque quod eligendi laborum minima\\nperferendis recusandae assumenda consectetur porro architecto ipsum ipsam\"},{\"userId\":2,\"id\":14,\"title\":\"voluptatem eligendi optio\",\"body\":\"fuga et accusamus dolorum perferendis illo voluptas\\nnon doloremque neque facere\\nad qui dolorum molestiae beatae\\nsed aut voluptas totam sit illum\"},{\"userId\":2,\"id\":15,\"title\":\"eveniet quod temporibus\",\"body\":\"reprehenderit quos placeat\\nvelit minima officia dolores impedit repudiandae molestiae nam\\nvoluptas recusandae quis delectus\\nofficiis harum fugiat vitae\"},{\"userId\":2,\"id\":16,\"title\":\"sint suscipit perspiciatis velit dolorum rerum ipsa laboriosam odio\",\"body\":\"suscipit nam nisi quo aperiam aut\\nasperiores eos fugit maiores voluptatibus quia\\nvoluptatem quis ullam qui in alias quia est\\nconsequatur magni mollitia accusamus ea nisi voluptate dicta\"},{\"userId\":2,\"id\":17,\"title\":\"fugit voluptas sed molestias voluptatem provident\",\"body\":\"eos voluptas et aut odit natus earum\\naspernatur fuga molestiae ullam\\ndeserunt ratione qui eos\\nqui nihil ratione nemo velit ut aut id quo\"},{\"userId\":2,\"id\":18,\"title\":\"voluptate et itaque vero tempora molestiae\",\"body\":\"eveniet quo quis\\nlaborum totam consequatur non dolor\\nut et est repudiandae\\nest voluptatem vel debitis et magnam\"},{\"userId\":2,\"id\":19,\"title\":\"adipisci placeat illum aut reiciendis qui\",\"body\":\"illum quis cupiditate provident sit magnam\\nea sed aut omnis\\nveniam maiores ullam consequatur atque\\nadipisci quo iste expedita sit quos voluptas\"},{\"userId\":2,\"id\":20,\"title\":\"doloribus ad provident suscipit at\",\"body\":\"qui consequuntur ducimus possimus quisquam amet similique\\nsuscipit porro ipsam amet\\neos veritatis officiis exercitationem vel fugit aut necessitatibus totam\\nomnis rerum consequatur expedita quidem cumque explicabo\"},{\"userId\":3,\"id\":21,\"title\":\"asperiores ea ipsam voluptatibus modi minima quia sint\",\"body\":\"repellat aliquid praesentium dolorem quo\\nsed totam minus non itaque\\nnihil labore molestiae sunt dolor eveniet hic recusandae veniam\\ntempora et tenetur expedita sunt\"},{\"userId\":3,\"id\":22,\"title\":\"dolor sint quo a velit explicabo quia nam\",\"body\":\"eos qui et ipsum ipsam suscipit aut\\nsed omnis non odio\\nexpedita earum mollitia molestiae aut atque rem suscipit\\nnam impedit esse\"},{\"userId\":3,\"id\":23,\"title\":\"maxime id vitae nihil numquam\",\"body\":\"veritatis unde neque eligendi\\nquae quod architecto quo neque vitae\\nest illo sit tempora doloremque fugit quod\\net et vel beatae sequi ullam sed tenetur perspiciatis\"},{\"userId\":3,\"id\":24,\"title\":\"autem hic labore sunt dolores incidunt\",\"body\":\"enim et ex nulla\\nomnis voluptas quia qui\\nvoluptatem consequatur numquam aliquam sunt\\ntotam recusandae id dignissimos aut sed asperiores deserunt\"},{\"userId\":3,\"id\":25,\"title\":\"rem alias distinctio quo quis\",\"body\":\"ullam consequatur ut\\nomnis quis sit vel consequuntur\\nipsa eligendi ipsum molestiae et omnis error nostrum\\nmolestiae illo tempore quia et distinctio\"},{\"userId\":3,\"id\":26,\"title\":\"est et quae odit qui non\",\"body\":\"similique esse doloribus nihil accusamus\\nomnis dolorem fuga consequuntur reprehenderit fugit recusandae temporibus\\nperspiciatis cum ut laudantium\\nomnis aut molestiae vel vero\"},{\"userId\":3,\"id\":27,\"title\":\"quasi id et eos tenetur aut quo autem\",\"body\":\"eum sed dolores ipsam sint possimus debitis occaecati\\ndebitis qui qui et\\nut placeat enim earum aut odit facilis\\nconsequatur suscipit necessitatibus rerum sed inventore temporibus consequatur\"},{\"userId\":3,\"id\":28,\"title\":\"delectus ullam et corporis nulla voluptas sequi\",\"body\":\"non et quaerat ex quae ad maiores\\nmaiores recusandae totam aut blanditiis mollitia quas illo\\nut voluptatibus voluptatem\\nsimilique nostrum eum\"},{\"userId\":3,\"id\":29,\"title\":\"iusto eius quod necessitatibus culpa ea\",\"body\":\"odit magnam ut saepe sed non qui\\ntempora atque nihil\\naccusamus illum doloribus illo dolor\\neligendi repudiandae odit magni similique sed cum maiores\"},{\"userId\":3,\"id\":30,\"title\":\"a quo magni similique perferendis\",\"body\":\"alias dolor cumque\\nimpedit blanditiis non eveniet odio maxime\\nblanditiis amet eius quis tempora quia autem rem\\na provident perspiciatis quia\"},{\"userId\":4,\"id\":31,\"title\":\"ullam ut quidem id aut vel consequuntur\",\"body\":\"debitis eius sed quibusdam non quis consectetur vitae\\nimpedit ut qui consequatur sed aut in\\nquidem sit nostrum et maiores adipisci atque\\nquaerat voluptatem adipisci repudiandae\"},{\"userId\":4,\"id\":32,\"title\":\"doloremque illum aliquid sunt\",\"body\":\"deserunt eos nobis asperiores et hic\\nest debitis repellat molestiae optio\\nnihil ratione ut eos beatae quibusdam distinctio maiores\\nearum voluptates et aut adipisci ea maiores voluptas maxime\"},{\"userId\":4,\"id\":33,\"title\":\"qui explicabo molestiae dolorem\",\"body\":\"rerum ut et numquam laborum odit est sit\\nid qui sint in\\nquasi tenetur tempore aperiam et quaerat qui in\\nrerum officiis sequi cumque quod\"},{\"userId\":4,\"id\":34,\"title\":\"magnam ut rerum iure\",\"body\":\"ea velit perferendis earum ut voluptatem voluptate itaque iusto\\ntotam pariatur in\\nnemo voluptatem voluptatem autem magni tempora minima in\\nest distinctio qui assumenda accusamus dignissimos officia nesciunt nobis\"},{\"userId\":4,\"id\":35,\"title\":\"id nihil consequatur molestias animi provident\",\"body\":\"nisi error delectus possimus ut eligendi vitae\\nplaceat eos harum cupiditate facilis reprehenderit voluptatem beatae\\nmodi ducimus quo illum voluptas eligendi\\net nobis quia fugit\"},{\"userId\":4,\"id\":36,\"title\":\"fuga nam accusamus voluptas reiciendis itaque\",\"body\":\"ad mollitia et omnis minus architecto odit\\nvoluptas doloremque maxime aut non ipsa qui alias veniam\\nblanditiis culpa aut quia nihil cumque facere et occaecati\\nqui aspernatur quia eaque ut aperiam inventore\"},{\"userId\":4,\"id\":37,\"title\":\"provident vel ut sit ratione est\",\"body\":\"debitis et eaque non officia sed nesciunt pariatur vel\\nvoluptatem iste vero et ea\\nnumquam aut expedita ipsum nulla in\\nvoluptates omnis consequatur aut enim officiis in quam qui\"},{\"userId\":4,\"id\":38,\"title\":\"explicabo et eos deleniti nostrum ab id repellendus\",\"body\":\"animi esse sit aut sit nesciunt assumenda eum voluptas\\nquia voluptatibus provident quia necessitatibus ea\\nrerum repudiandae quia voluptatem delectus fugit aut id quia\\nratione optio eos iusto veniam iure\"},{\"userId\":4,\"id\":39,\"title\":\"eos dolorem iste accusantium est eaque quam\",\"body\":\"corporis rerum ducimus vel eum accusantium\\nmaxime aspernatur a porro possimus iste omnis\\nest in deleniti asperiores fuga aut\\nvoluptas sapiente vel dolore minus voluptatem incidunt ex\"},{\"userId\":4,\"id\":40,\"title\":\"enim quo cumque\",\"body\":\"ut voluptatum aliquid illo tenetur nemo sequi quo facilis\\nipsum rem optio mollitia quas\\nvoluptatem eum voluptas qui\\nunde omnis voluptatem iure quasi maxime voluptas nam\"},{\"userId\":5,\"id\":41,\"title\":\"non est facere\",\"body\":\"molestias id nostrum\\nexcepturi molestiae dolore omnis repellendus quaerat saepe\\nconsectetur iste quaerat tenetur asperiores accusamus ex ut\\nnam quidem est ducimus sunt debitis saepe\"},{\"userId\":5,\"id\":42,\"title\":\"commodi ullam sint et excepturi error explicabo praesentium voluptas\",\"body\":\"odio fugit voluptatum ducimus earum autem est incidunt voluptatem\\nodit reiciendis aliquam sunt sequi nulla dolorem\\nnon facere repellendus voluptates quia\\nratione harum vitae ut\"},{\"userId\":5,\"id\":43,\"title\":\"eligendi iste nostrum consequuntur adipisci praesentium sit beatae perferendis\",\"body\":\"similique fugit est\\nillum et dolorum harum et voluptate eaque quidem\\nexercitationem quos nam commodi possimus cum odio nihil nulla\\ndolorum exercitationem magnam ex et a et distinctio debitis\"},{\"userId\":5,\"id\":44,\"title\":\"optio dolor molestias sit\",\"body\":\"temporibus est consectetur dolore\\net libero debitis vel velit laboriosam quia\\nipsum quibusdam qui itaque fuga rem aut\\nea et iure quam sed maxime ut distinctio quae\"},{\"userId\":5,\"id\":45,\"title\":\"ut numquam possimus omnis eius suscipit laudantium iure\",\"body\":\"est natus reiciendis nihil possimus aut provident\\nex et dolor\\nrepellat pariatur est\\nnobis rerum repellendus dolorem autem\"},{\"userId\":5,\"id\":46,\"title\":\"aut quo modi neque nostrum ducimus\",\"body\":\"voluptatem quisquam iste\\nvoluptatibus natus officiis facilis dolorem\\nquis quas ipsam\\nvel et voluptatum in aliquid\"},{\"userId\":5,\"id\":47,\"title\":\"quibusdam cumque rem aut deserunt\",\"body\":\"voluptatem assumenda ut qui ut cupiditate aut impedit veniam\\noccaecati nemo illum voluptatem laudantium\\nmolestiae beatae rerum ea iure soluta nostrum\\neligendi et voluptate\"},{\"userId\":5,\"id\":48,\"title\":\"ut voluptatem illum ea doloribus itaque eos\",\"body\":\"voluptates quo voluptatem facilis iure occaecati\\nvel assumenda rerum officia et\\nillum perspiciatis ab deleniti\\nlaudantium repellat ad ut et autem reprehenderit\"},{\"userId\":5,\"id\":49,\"title\":\"laborum non sunt aut ut assumenda perspiciatis voluptas\",\"body\":\"inventore ab sint\\nnatus fugit id nulla sequi architecto nihil quaerat\\neos tenetur in in eum veritatis non\\nquibusdam officiis aspernatur cumque aut commodi aut\"},{\"userId\":5,\"id\":50,\"title\":\"repellendus qui recusandae incidunt voluptates tenetur qui omnis exercitationem\",\"body\":\"error suscipit maxime adipisci consequuntur recusandae\\nvoluptas eligendi et est et voluptates\\nquia distinctio ab amet quaerat molestiae et vitae\\nadipisci impedit sequi nesciunt quis consectetur\"},{\"userId\":6,\"id\":51,\"title\":\"soluta aliquam aperiam consequatur illo quis voluptas\",\"body\":\"sunt dolores aut doloribus\\ndolore doloribus voluptates tempora et\\ndoloremque et quo\\ncum asperiores sit consectetur dolorem\"},{\"userId\":6,\"id\":52,\"title\":\"qui enim et consequuntur quia animi quis voluptate quibusdam\",\"body\":\"iusto est quibusdam fuga quas quaerat molestias\\na enim ut sit accusamus enim\\ntemporibus iusto accusantium provident architecto\\nsoluta esse reprehenderit qui laborum\"},{\"userId\":6,\"id\":53,\"title\":\"ut quo aut ducimus alias\",\"body\":\"minima harum praesentium eum rerum illo dolore\\nquasi exercitationem rerum nam\\nporro quis neque quo\\nconsequatur minus dolor quidem veritatis sunt non explicabo similique\"},{\"userId\":6,\"id\":54,\"title\":\"sit asperiores ipsam eveniet odio non quia\",\"body\":\"totam corporis dignissimos\\nvitae dolorem ut occaecati accusamus\\nex velit deserunt\\net exercitationem vero incidunt corrupti mollitia\"},{\"userId\":6,\"id\":55,\"title\":\"sit vel voluptatem et non libero\",\"body\":\"debitis excepturi ea perferendis harum libero optio\\neos accusamus cum fuga ut sapiente repudiandae\\net ut incidunt omnis molestiae\\nnihil ut eum odit\"},{\"userId\":6,\"id\":56,\"title\":\"qui et at rerum necessitatibus\",\"body\":\"aut est omnis dolores\\nneque rerum quod ea rerum velit pariatur beatae excepturi\\net provident voluptas corrupti\\ncorporis harum reprehenderit dolores eligendi\"},{\"userId\":6,\"id\":57,\"title\":\"sed ab est est\",\"body\":\"at pariatur consequuntur earum quidem\\nquo est laudantium soluta voluptatem\\nqui ullam et est\\net cum voluptas voluptatum repellat est\"},{\"userId\":6,\"id\":58,\"title\":\"voluptatum itaque dolores nisi et quasi\",\"body\":\"veniam voluptatum quae adipisci id\\net id quia eos ad et dolorem\\naliquam quo nisi sunt eos impedit error\\nad similique veniam\"},{\"userId\":6,\"id\":59,\"title\":\"qui commodi dolor at maiores et quis id accusantium\",\"body\":\"perspiciatis et quam ea autem temporibus non voluptatibus qui\\nbeatae a earum officia nesciunt dolores suscipit voluptas et\\nanimi doloribus cum rerum quas et magni\\net hic ut ut commodi expedita sunt\"},{\"userId\":6,\"id\":60,\"title\":\"consequatur placeat omnis quisquam quia reprehenderit fugit veritatis facere\",\"body\":\"asperiores sunt ab assumenda cumque modi velit\\nqui esse omnis\\nvoluptate et fuga perferendis voluptas\\nillo ratione amet aut et omnis\"},{\"userId\":7,\"id\":61,\"title\":\"voluptatem doloribus consectetur est ut ducimus\",\"body\":\"ab nemo optio odio\\ndelectus tenetur corporis similique nobis repellendus rerum omnis facilis\\nvero blanditiis debitis in nesciunt doloribus dicta dolores\\nmagnam minus velit\"},{\"userId\":7,\"id\":62,\"title\":\"beatae enim quia vel\",\"body\":\"enim aspernatur illo distinctio quae praesentium\\nbeatae alias amet delectus qui voluptate distinctio\\nodit sint accusantium autem omnis\\nquo molestiae omnis ea eveniet optio\"},{\"userId\":7,\"id\":63,\"title\":\"voluptas blanditiis repellendus animi ducimus error sapiente et suscipit\",\"body\":\"enim adipisci aspernatur nemo\\nnumquam omnis facere dolorem dolor ex quis temporibus incidunt\\nab delectus culpa quo reprehenderit blanditiis asperiores\\naccusantium ut quam in voluptatibus voluptas ipsam dicta\"},{\"userId\":7,\"id\":64,\"title\":\"et fugit quas eum in in aperiam quod\",\"body\":\"id velit blanditiis\\neum ea voluptatem\\nmolestiae sint occaecati est eos perspiciatis\\nincidunt a error provident eaque aut aut qui\"},{\"userId\":7,\"id\":65,\"title\":\"consequatur id enim sunt et et\",\"body\":\"voluptatibus ex esse\\nsint explicabo est aliquid cumque adipisci fuga repellat labore\\nmolestiae corrupti ex saepe at asperiores et perferendis\\nnatus id esse incidunt pariatur\"},{\"userId\":7,\"id\":66,\"title\":\"repudiandae ea animi iusto\",\"body\":\"officia veritatis tenetur vero qui itaque\\nsint non ratione\\nsed et ut asperiores iusto eos molestiae nostrum\\nveritatis quibusdam et nemo iusto saepe\"},{\"userId\":7,\"id\":67,\"title\":\"aliquid eos sed fuga est maxime repellendus\",\"body\":\"reprehenderit id nostrum\\nvoluptas doloremque pariatur sint et accusantium quia quod aspernatur\\net fugiat amet\\nnon sapiente et consequatur necessitatibus molestiae\"},{\"userId\":7,\"id\":68,\"title\":\"odio quis facere architecto reiciendis optio\",\"body\":\"magnam molestiae perferendis quisquam\\nqui cum reiciendis\\nquaerat animi amet hic inventore\\nea quia deleniti quidem saepe porro velit\"},{\"userId\":7,\"id\":69,\"title\":\"fugiat quod pariatur odit minima\",\"body\":\"officiis error culpa consequatur modi asperiores et\\ndolorum assumenda voluptas et vel qui aut vel rerum\\nvoluptatum quisquam perspiciatis quia rerum consequatur totam quas\\nsequi commodi repudiandae asperiores et saepe a\"},{\"userId\":7,\"id\":70,\"title\":\"voluptatem laborum magni\",\"body\":\"sunt repellendus quae\\nest asperiores aut deleniti esse accusamus repellendus quia aut\\nquia dolorem unde\\neum tempora esse dolore\"},{\"userId\":8,\"id\":71,\"title\":\"et iusto veniam et illum aut fuga\",\"body\":\"occaecati a doloribus\\niste saepe consectetur placeat eum voluptate dolorem et\\nqui quo quia voluptas\\nrerum ut id enim velit est perferendis\"},{\"userId\":8,\"id\":72,\"title\":\"sint hic doloribus consequatur eos non id\",\"body\":\"quam occaecati qui deleniti consectetur\\nconsequatur aut facere quas exercitationem aliquam hic voluptas\\nneque id sunt ut aut accusamus\\nsunt consectetur expedita inventore velit\"},{\"userId\":8,\"id\":73,\"title\":\"consequuntur deleniti eos quia temporibus ab aliquid at\",\"body\":\"voluptatem cumque tenetur consequatur expedita ipsum nemo quia explicabo\\naut eum minima consequatur\\ntempore cumque quae est et\\net in consequuntur voluptatem voluptates aut\"},{\"userId\":8,\"id\":74,\"title\":\"enim unde ratione doloribus quas enim ut sit sapiente\",\"body\":\"odit qui et et necessitatibus sint veniam\\nmollitia amet doloremque molestiae commodi similique magnam et quam\\nblanditiis est itaque\\nquo et tenetur ratione occaecati molestiae tempora\"},{\"userId\":8,\"id\":75,\"title\":\"dignissimos eum dolor ut enim et delectus in\",\"body\":\"commodi non non omnis et voluptas sit\\nautem aut nobis magnam et sapiente voluptatem\\net laborum repellat qui delectus facilis temporibus\\nrerum amet et nemo voluptate expedita adipisci error dolorem\"},{\"userId\":8,\"id\":76,\"title\":\"doloremque officiis ad et non perferendis\",\"body\":\"ut animi facere\\ntotam iusto tempore\\nmolestiae eum aut et dolorem aperiam\\nquaerat recusandae totam odio\"},{\"userId\":8,\"id\":77,\"title\":\"necessitatibus quasi exercitationem odio\",\"body\":\"modi ut in nulla repudiandae dolorum nostrum eos\\naut consequatur omnis\\nut incidunt est omnis iste et quam\\nvoluptates sapiente aliquam asperiores nobis amet corrupti repudiandae provident\"},{\"userId\":8,\"id\":78,\"title\":\"quam voluptatibus rerum veritatis\",\"body\":\"nobis facilis odit tempore cupiditate quia\\nassumenda doloribus rerum qui ea\\nillum et qui totam\\naut veniam repellendus\"},{\"userId\":8,\"id\":79,\"title\":\"pariatur consequatur quia magnam autem omnis non amet\",\"body\":\"libero accusantium et et facere incidunt sit dolorem\\nnon excepturi qui quia sed laudantium\\nquisquam molestiae ducimus est\\nofficiis esse molestiae iste et quos\"},{\"userId\":8,\"id\":80,\"title\":\"labore in ex et explicabo corporis aut quas\",\"body\":\"ex quod dolorem ea eum iure qui provident amet\\nquia qui facere excepturi et repudiandae\\nasperiores molestias provident\\nminus incidunt vero fugit rerum sint sunt excepturi provident\"},{\"userId\":9,\"id\":81,\"title\":\"tempora rem veritatis voluptas quo dolores vero\",\"body\":\"facere qui nesciunt est voluptatum voluptatem nisi\\nsequi eligendi necessitatibus ea at rerum itaque\\nharum non ratione velit laboriosam quis consequuntur\\nex officiis minima doloremque voluptas ut aut\"},{\"userId\":9,\"id\":82,\"title\":\"laudantium voluptate suscipit sunt enim enim\",\"body\":\"ut libero sit aut totam inventore sunt\\nporro sint qui sunt molestiae\\nconsequatur cupiditate qui iste ducimus adipisci\\ndolor enim assumenda soluta laboriosam amet iste delectus hic\"},{\"userId\":9,\"id\":83,\"title\":\"odit et voluptates doloribus alias odio et\",\"body\":\"est molestiae facilis quis tempora numquam nihil qui\\nvoluptate sapiente consequatur est qui\\nnecessitatibus autem aut ipsa aperiam modi dolore numquam\\nreprehenderit eius rem quibusdam\"},{\"userId\":9,\"id\":84,\"title\":\"optio ipsam molestias necessitatibus occaecati facilis veritatis dolores aut\",\"body\":\"sint molestiae magni a et quos\\neaque et quasi\\nut rerum debitis similique veniam\\nrecusandae dignissimos dolor incidunt consequatur odio\"},{\"userId\":9,\"id\":85,\"title\":\"dolore veritatis porro provident adipisci blanditiis et sunt\",\"body\":\"similique sed nisi voluptas iusto omnis\\nmollitia et quo\\nassumenda suscipit officia magnam sint sed tempora\\nenim provident pariatur praesentium atque animi amet ratione\"},{\"userId\":9,\"id\":86,\"title\":\"placeat quia et porro iste\",\"body\":\"quasi excepturi consequatur iste autem temporibus sed molestiae beatae\\net quaerat et esse ut\\nvoluptatem occaecati et vel explicabo autem\\nasperiores pariatur deserunt optio\"},{\"userId\":9,\"id\":87,\"title\":\"nostrum quis quasi placeat\",\"body\":\"eos et molestiae\\nnesciunt ut a\\ndolores perspiciatis repellendus repellat aliquid\\nmagnam sint rem ipsum est\"},{\"userId\":9,\"id\":88,\"title\":\"sapiente omnis fugit eos\",\"body\":\"consequatur omnis est praesentium\\nducimus non iste\\nneque hic deserunt\\nvoluptatibus veniam cum et rerum sed\"},{\"userId\":9,\"id\":89,\"title\":\"sint soluta et vel magnam aut ut sed qui\",\"body\":\"repellat aut aperiam totam temporibus autem et\\narchitecto magnam ut\\nconsequatur qui cupiditate rerum quia soluta dignissimos nihil iure\\ntempore quas est\"},{\"userId\":9,\"id\":90,\"title\":\"ad iusto omnis odit dolor voluptatibus\",\"body\":\"minus omnis soluta quia\\nqui sed adipisci voluptates illum ipsam voluptatem\\neligendi officia ut in\\neos soluta similique molestias praesentium blanditiis\"},{\"userId\":10,\"id\":91,\"title\":\"aut amet sed\",\"body\":\"libero voluptate eveniet aperiam sed\\nsunt placeat suscipit molestias\\nsimilique fugit nam natus\\nexpedita consequatur consequatur dolores quia eos et placeat\"},{\"userId\":10,\"id\":92,\"title\":\"ratione ex tenetur perferendis\",\"body\":\"aut et excepturi dicta laudantium sint rerum nihil\\nlaudantium et at\\na neque minima officia et similique libero et\\ncommodi voluptate qui\"},{\"userId\":10,\"id\":93,\"title\":\"beatae soluta recusandae\",\"body\":\"dolorem quibusdam ducimus consequuntur dicta aut quo laboriosam\\nvoluptatem quis enim recusandae ut sed sunt\\nnostrum est odit totam\\nsit error sed sunt eveniet provident qui nulla\"},{\"userId\":10,\"id\":94,\"title\":\"qui qui voluptates illo iste minima\",\"body\":\"aspernatur expedita soluta quo ab ut similique\\nexpedita dolores amet\\nsed temporibus distinctio magnam saepe deleniti\\nomnis facilis nam ipsum natus sint similique omnis\"},{\"userId\":10,\"id\":95,\"title\":\"id minus libero illum nam ad officiis\",\"body\":\"earum voluptatem facere provident blanditiis velit laboriosam\\npariatur accusamus odio saepe\\ncumque dolor qui a dicta ab doloribus consequatur omnis\\ncorporis cupiditate eaque assumenda ad nesciunt\"},{\"userId\":10,\"id\":96,\"title\":\"quaerat velit veniam amet cupiditate aut numquam ut sequi\",\"body\":\"in non odio excepturi sint eum\\nlabore voluptates vitae quia qui et\\ninventore itaque rerum\\nveniam non exercitationem delectus aut\"},{\"userId\":10,\"id\":97,\"title\":\"quas fugiat ut perspiciatis vero provident\",\"body\":\"eum non blanditiis soluta porro quibusdam voluptas\\nvel voluptatem qui placeat dolores qui velit aut\\nvel inventore aut cumque culpa explicabo aliquid at\\nperspiciatis est et voluptatem dignissimos dolor itaque sit nam\"},{\"userId\":10,\"id\":98,\"title\":\"laboriosam dolor voluptates\",\"body\":\"doloremque ex facilis sit sint culpa\\nsoluta assumenda eligendi non ut eius\\nsequi ducimus vel quasi\\nveritatis est dolores\"},{\"userId\":10,\"id\":99,\"title\":\"temporibus sit alias delectus eligendi possimus magni\",\"body\":\"quo deleniti praesentium dicta non quod\\naut est molestias\\nmolestias et officia quis nihil\\nitaque dolorem quia\"},{\"userId\":10,\"id\":100,\"title\":\"at nam consequatur ea labore ea harum\",\"body\":\"cupiditate quo est a modi nesciunt soluta\\nipsa voluptas error itaque dicta in\\nautem qui minus magnam et distinctio eum\\naccusamus ratione error aut\"}]", null));

        //SDK - 0.6.4 - restTemplate non reactive candidates
        ScanTestModel restTemplateNonReactive = new ScanTestModel("restTemplate-non-reactive", assertions);
        scanTests.add(restTemplateNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("scheduledThreadFixedDelay",
                        "org.unlogged.demo.controller.ThreadingOpsController"),
                new AssertionOptions("\"#R1#R1#R1\"", null));

        assertions.put(new MethodReference("scheduledThreadFixedRate",
                        "org.unlogged.demo.controller.ThreadingOpsController"),
                new AssertionOptions("\"\"", null));

        assertions.put(new MethodReference("scheduledThread",
                        "org.unlogged.demo.controller.ThreadingOpsController"),
                new AssertionOptions("\"E\"", null));

        assertions.put(new MethodReference("executorServiceCallablesAll",
                        "org.unlogged.demo.controller.ThreadingOpsController"),
                new AssertionOptions("[\"E\",\"E\",\"E\"]", null));

        assertions.put(new MethodReference("executorServiceRunnable",
                        "org.unlogged.demo.controller.ThreadingOpsController"),
                new AssertionOptions("\"#R1\"", null));

        //SDK - 0.6.4 - threads non reactive candidates
        ScanTestModel threadsNonReactive = new ScanTestModel("threads-non-reactive", assertions);
        scanTests.add(threadsNonReactive);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("getById",
                        "org.unlogged.demo.service.abstractions.PropertyServiceCEImpl"),
                new AssertionOptions("{\"propertyId\":1,\"propertyName\":\"Default Property\",\"description\":\"Default location\",\"ownerDetails\":{\"ownerId\":1,\"name\":\"Owner1\",\"email\":\"Owner1@ownermail.com\",\"phone\":\"OwnPhone\",\"officeLocationDetails\":{\"locationId\":2,\"addressString\":\"Address line own, l23, l24\",\"city\":\"City l12\",\"zipCode\":\"ZIP-012\",\"country\":\"country 123\",\"state\":\"State 44\",\"latitude\":123.34,\"longitude\":25.12}},\"locationDetails\":{\"locationId\":1,\"addressString\":\"Address line, l1, l2\",\"city\":\"City1\",\"zipCode\":\"ZIP-098\",\"country\":\"country1\",\"state\":\"State1\",\"latitude\":1.0,\"longitude\":2.0},\"roomDetailsList\":[{\"roomId\":1,\"parentPropertyId\":1,\"roomName\":\"Room1\",\"roomDescription\":\"Default Room1\",\"roomType\":\"PRIVATE_ROOM\",\"pricePerDay\":1200.0,\"maxCapacity\":4,\"occupiedCapacity\":4,\"customPriceMap\":{\"WEEKDAY\":null,\"HOLIDAY\":null,\"WEEKEND\":1.18},\"amenitiesList\":[\"AC\",\"WIFI\"]},{\"roomId\":2,\"parentPropertyId\":1,\"roomName\":\"Room2\",\"roomDescription\":\"Default Room2\",\"roomType\":\"DORM_ROOM\",\"pricePerDay\":750.0,\"maxCapacity\":8,\"occupiedCapacity\":3,\"customPriceMap\":{\"WEEKDAY\":null,\"HOLIDAY\":null,\"WEEKEND\":1.18},\"amenitiesList\":[\"AC\",\"WIFI\",\"WASHING_MACHINE\"]}]}", null));

        assertions.put(new MethodReference("deleteById",
                        "org.unlogged.demo.service.abstractions.PropertyServiceCEImpl"),
                new AssertionOptions("{\"propertyId\":1,\"propertyName\":\"Default Property#Deleted\",\"description\":\"Default location\",\"ownerDetails\":{\"ownerId\":1,\"name\":\"Owner1\",\"email\":\"Owner1@ownermail.com\",\"phone\":\"OwnPhone\",\"officeLocationDetails\":{\"locationId\":2,\"addressString\":\"Address line own, l23, l24\",\"city\":\"City l12\",\"zipCode\":\"ZIP-012\",\"country\":\"country 123\",\"state\":\"State 44\",\"latitude\":123.34,\"longitude\":25.12}},\"locationDetails\":{\"locationId\":1,\"addressString\":\"Address line, l1, l2\",\"city\":\"City1\",\"zipCode\":\"ZIP-098\",\"country\":\"country1\",\"state\":\"State1\",\"latitude\":1.0,\"longitude\":2.0},\"roomDetailsList\":[{\"roomId\":1,\"parentPropertyId\":1,\"roomName\":\"Room1\",\"roomDescription\":\"Default Room1\",\"roomType\":\"PRIVATE_ROOM\",\"pricePerDay\":1200.0,\"maxCapacity\":4,\"occupiedCapacity\":4,\"customPriceMap\":{\"WEEKDAY\":null,\"HOLIDAY\":null,\"WEEKEND\":1.18},\"amenitiesList\":[\"AC\",\"WIFI\"]},{\"roomId\":2,\"parentPropertyId\":1,\"roomName\":\"Room2\",\"roomDescription\":\"Default Room2\",\"roomType\":\"DORM_ROOM\",\"pricePerDay\":750.0,\"maxCapacity\":8,\"occupiedCapacity\":3,\"customPriceMap\":{\"WEEKDAY\":null,\"HOLIDAY\":null,\"WEEKEND\":1.18},\"amenitiesList\":[\"AC\",\"WIFI\",\"WASHING_MACHINE\"]}]}", null));

        assertions.put(new MethodReference("getGlobalFilterAdditive",
                        "org.unlogged.demo.filter.GlobalFilter"),
                new AssertionOptions("\"global-filter\"", null));

        assertions.put(new MethodReference("findById",
                        "org.unlogged.demo.controller.abstractions.PropertyControllerImpl"),
                new AssertionOptions("{\"propertyId\":1,\"propertyName\":\"Default Property\",\"description\":\"Default location\",\"ownerDetails\":{\"ownerId\":1,\"name\":\"Owner1\",\"email\":\"Owner1@ownermail.com\",\"phone\":\"OwnPhone\",\"officeLocationDetails\":{\"locationId\":2,\"addressString\":\"Address line own, l23, l24\",\"city\":\"City l12\",\"zipCode\":\"ZIP-012\",\"country\":\"country 123\",\"state\":\"State 44\",\"latitude\":123.34,\"longitude\":25.12}},\"locationDetails\":{\"locationId\":1,\"addressString\":\"Address line, l1, l2\",\"city\":\"City1\",\"zipCode\":\"ZIP-098\",\"country\":\"country1\",\"state\":\"State1\",\"latitude\":1.0,\"longitude\":2.0},\"roomDetailsList\":[{\"roomId\":1,\"parentPropertyId\":1,\"roomName\":\"Room1\",\"roomDescription\":\"Default Room1\",\"roomType\":\"PRIVATE_ROOM\",\"pricePerDay\":1200.0,\"maxCapacity\":4,\"occupiedCapacity\":4,\"customPriceMap\":{\"WEEKDAY\":null,\"HOLIDAY\":null,\"WEEKEND\":1.18},\"amenitiesList\":[\"AC\",\"WIFI\"]},{\"roomId\":2,\"parentPropertyId\":1,\"roomName\":\"Room2\",\"roomDescription\":\"Default Room2\",\"roomType\":\"DORM_ROOM\",\"pricePerDay\":750.0,\"maxCapacity\":8,\"occupiedCapacity\":3,\"customPriceMap\":{\"WEEKDAY\":null,\"HOLIDAY\":null,\"WEEKEND\":1.18},\"amenitiesList\":[\"AC\",\"WIFI\",\"WASHING_MACHINE\"]}]}", null));

        assertions.put(new MethodReference("getAll",
                        "org.unlogged.demo.service.abstractions.PropertyServiceCEImpl"),
                new AssertionOptions("[{\"propertyId\":1,\"propertyName\":\"Default Property\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null},{\"propertyId\":2,\"propertyName\":\"Property 2\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null}]", null));

        assertions.put(new MethodReference("findAll",
                        "org.unlogged.demo.controller.abstractions.PropertyControllerImpl"),
                new AssertionOptions("[{\"propertyId\":1,\"propertyName\":\"Default Property\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null},{\"propertyId\":2,\"propertyName\":\"Property 2\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null}]", null));

        assertions.put(new MethodReference("parseJwtClaims",
                        "org.unlogged.demo.security.util.JwtUtil"),
                new AssertionOptions("{\"sub\":\"amg@amg.com\",\"exp\":1925639307}", null));

        assertions.put(new MethodReference("updateExisting",
                        "org.unlogged.demo.service.abstractions.PropertyServiceCEImpl"),
                new AssertionOptions("{\"propertyId\":3,\"propertyName\":\"New Property#Updated\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null}", null));

        assertions.put(new MethodReference("update",
                        "org.unlogged.demo.controller.abstractions.PropertyControllerImpl"),
                new AssertionOptions("{\"propertyId\":3,\"propertyName\":\"New Property#Updated\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null}", null));

        assertions.put(new MethodReference("insertNew",
                        "org.unlogged.demo.service.abstractions.PropertyServiceCEImpl"),
                new AssertionOptions("{\"propertyId\":3,\"propertyName\":\"New Property#Added\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null}", null));

        assertions.put(new MethodReference("insertNew",
                        "org.unlogged.demo.controller.abstractions.PropertyControllerImpl"),
                new AssertionOptions("{\"propertyId\":3,\"propertyName\":\"New Property#Added\",\"description\":null,\"ownerDetails\":null,\"locationDetails\":null,\"roomDetailsList\":null}", null));


        //SDK - 0.6.4 - abstractions non reactive candidates
        ScanTestModel abstractionsNonReactive = new ScanTestModel("abstractions-non-reactive", assertions);
        scanTests.add(abstractionsNonReactive);

        return scanTests;
    }
}
