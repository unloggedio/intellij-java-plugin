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

        ScanTestModel modelMapperNonReactive = new ScanTestModel("modelmapper-recordings", assertions);
        scanTests.add(modelMapperNonReactive);

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
        System.out.println("In test for : " + sessionFolder);
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
