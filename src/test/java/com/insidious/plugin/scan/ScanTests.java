package com.insidious.plugin.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.ActiveSessionManager;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.scan.model.AssertionResult;
import com.insidious.plugin.scan.model.MethodReference;
import com.insidious.plugin.scan.model.ScanTestModel;
import com.insidious.plugin.scan.model.ScanTestResult;
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
        Map<MethodReference, String> assertions = new HashMap<>();
        assertions.put(new MethodReference("coffeeFlixList",
                        "org.unlogged.springwebfluxdemo.controller.flow1.CustomControllerCE"),
                "[{\"id\":\"cee4e721-93ac-4b5e-a212-047b56b0aab9\",\"name\":\"Black Alert Redis\"},{\"id\":\"3aedcd81-a39d-4fc3-a1e8-4330d46ff65e\",\"name\":\"Darth Redis\"},{\"id\":\"f8228860-300e-4354-984a-2f0d351f1539\",\"name\":\"Jet Black Redis\"}]");

        assertions.put(new MethodReference("connector",
                        "org.unlogged.springwebfluxdemo.client.GreetingClient"),
                "{\"httpClient\":{},\"resourceFactory\":null,\"running\":true,\"lifecycleMonitor\":{}}");

        ScanTestModel reactiveSessionTest = new ScanTestModel("selogger-4", assertions);
        scanTests.add(reactiveSessionTest);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("setLocationForStaticAssets",
                "com.videobug.testbed.config.WebConfigurer"), "0");

        ScanTestModel session2 = new ScanTestModel("selogger-2", assertions);
        scanTests.add(session2);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("enrich", "org.unlogged.springwebfluxdemo.enrich.nonreactive.PersonNameEnricherV1"), "{\"id\":\"string\",\"name\":\"string#En\",\"age\":0}");
        assertions.put(new MethodReference("saveStaff", "org.unlogged.springwebfluxdemo.controller.RXJavaSQLOpsController"), "1");
        assertions.put(new MethodReference("getBlockedString", "org.unlogged.springwebfluxdemo.controller.MonoOpsController"), "\"MonoString\"");
        assertions.put(new MethodReference("getTypeWrapped", "org.unlogged.springwebfluxdemo.handler.GreetingHandler"), "{\"statusCode\":200,\"headers\":{\"Content-Type\":[\"application/json\"]},\"cookies\":{},\"hints\":{},\"inserter\":{\"arg$1\":{\"intValue\":0,\"longValue\":0,\"charValue\":\"\\u0000\",\"doubleValue\":0.0,\"floatValue\":0.0,\"stringValue\":null,\"shortValue\":0,\"byteValue\":0,\"boolValue\":false,\"intWrapper\":null,\"longWrapper\":null,\"charWrapper\":null,\"doubleWrapper\":null,\"floatWrapper\":null,\"shortWrapper\":null,\"ByteWrapper\":null,\"booleanWrapper\":null,\"object\":null}}}");
        assertions.put(new MethodReference("mapAndFilter", "org.unlogged.springwebfluxdemo.controller.RXJavaController"), "\"E,678,1015,760,740,489_Completed\"");
        assertions.put(new MethodReference("parallelExecutor", "org.unlogged.springwebfluxdemo.controller.ReactorSchedulerOpsController"), "\"action\"");
        assertions.put(new MethodReference("returnMockString", "org.unlogged.springwebfluxdemo.controller.MockUtils"), "\"Test String\"");
        assertions.put(new MethodReference("returnMockString", "org.unlogged.springwebfluxdemo.controller.MockUtils"), "\"Test String\"");
        assertions.put(new MethodReference("all", "org.unlogged.springwebfluxdemo.repository.RedisCoffeeInteractionRepoImpl"), "[{\"id\":\"999ea222-d5a9-4236-a547-57132e017d2e\",\"name\":\"Black Alert Redis\"},{\"id\":\"ce39296e-c4f5-49e7-befd-e82d4a173a90\",\"name\":\"Jet Black Redis\"},{\"id\":\"ed300b41-679a-4727-bfd1-4d4ee4186c58\",\"name\":\"Darth Redis\"}]");
        assertions.put(new MethodReference("notifyShopV1", "org.unlogged.springwebfluxdemo.controller.CasesController"), "true");
        assertions.put(new MethodReference("updateStaffNameForId", "org.unlogged.springwebfluxdemo.repository.flow1.RXjavaSqlRepoImpl"), "false");
        ScanTestModel webflux11 = new ScanTestModel("webflux-java11-auto", assertions);
        scanTests.add(webflux11);

        assertions = new HashMap<>();
        assertions.put(new MethodReference("getGreeting", "org.unlogged.springwebfluxdemo.controller.EntryPointController"), "{\"message\":\"Hello, Spring!\",\"typeWrapper\":null,\"someBean\":{},\"listOfStrings\":[\"123\",\"65\",\"513\",\"3\",\"47\",\"23\",\"255\",\"363\"]}");
        ScanTestModel webflux11Controller = new ScanTestModel("webflux-java-11-controller", assertions);
        scanTests.add(webflux11Controller);

        List<ScanTestResult> scanTestResults = new ArrayList<>();
        for (ScanTestModel scanTestModel : scanTests) {
            Map<MethodReference, AssertionResult> assertionResults = assertScannedValuesFromSession(scanTestModel.getAssertions(), scanTestModel.getSessionFolder());
            ScanTestResult scanTestResult = new ScanTestResult(scanTestModel.getSessionFolder(), assertionResults, scanTestModel);
            scanTestResults.add(scanTestResult);
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
                System.out.println("Expected value : " + value.getExpectedValue());
                System.out.println("Recorded value : " + value.getScannedValue());
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

    public Map<MethodReference, AssertionResult> assertScannedValuesFromSession(Map<MethodReference, String> assetions, String sessionFolder) throws SQLException, IOException, InterruptedException {
        String sessionPath = SESSIONS_PATH + sessionFolder;
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getName()).thenReturn("test-project");
        new VideobugLocalClient(sessionPath, project, new ActiveSessionManager());
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
            waiting.set(false);
            assetions.forEach((key, value) -> {
                String classname = key.getContainingClass();
                String methodname = key.getMethodName();
                String expectedValue = value;

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
                    assertionResults.put(key, new AssertionResult(value, scannedValueReference, passing));
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
