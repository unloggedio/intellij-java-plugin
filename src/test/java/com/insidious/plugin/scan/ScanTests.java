package com.insidious.plugin.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.UnloggedLocalClient;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanTests {

    public static String SESSIONS_PATH;

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

        List<ScanTestResult> scanTestResults = new ArrayList<>();
        for (ScanTestModel scanTestModel : scanTests) {
            Map<MethodReference, AssertionResult> assertionResults = assertScannedValuesFromSession(scanTestModel.getAssertions(), scanTestModel.getSessionFolder());
            ScanTestResult scanTestResult = new ScanTestResult(scanTestModel.getSessionFolder(), assertionResults);
            scanTestResults.add(scanTestResult);
        }

        AtomicBoolean overallPassing = new AtomicBoolean(true);
        scanTestResults.forEach(scanTestResult -> {
            System.out.println("Results from session : " + scanTestResult.getSessionFolder() + "\n");
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
                }
            });
            System.out.println("---------------------");
        });
        Assertions.assertEquals(true, overallPassing.get());
    }

    public Map<MethodReference, AssertionResult> assertScannedValuesFromSession(Map<MethodReference, String> assetions, String sessionFolder) throws SQLException, IOException, InterruptedException {
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

        int zipCount = new File(sessionPath).listFiles().length;
        while (sessionInstance.getProcessedFileCount() < zipCount) {
            continue;
        }

        CountDownLatch cdl = new CountDownLatch(1);
        Map<MethodReference, AssertionResult> assertionResults = new HashMap<>();
        sessionInstance.getTestCandidates(testCandidateMetadata -> {
            assetions.forEach((key, value) -> {
                String classname = key.getContainingClass();
                String methodname = key.getMethodName();
                String expectedValue = value;

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
        }, 0, new StompFilterModel(), new AtomicInteger(1));
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
