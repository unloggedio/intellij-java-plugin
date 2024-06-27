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

        List<ScanTestResult> scanTestResults = new ArrayList<>();
        for (ScanTestModel scanTestModel : scanTests) {
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
}
