package com.insidious.plugin.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.ActiveSessionManager;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.intellij.openapi.project.Project;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionInstanceTest extends TestCase {
    public static final String SESSIONS_PATH = "/Users/artpar/workspace/code/insidious/plugin/src/test/resources/test-sessions/";

    @Test
    public void testScan1() throws SQLException, IOException, InterruptedException {
        String sessionPath = SESSIONS_PATH + "selogger-3";
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getName()).thenReturn("test-project");
        new UnloggedLocalClient(sessionPath);
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setPath(sessionPath);
        executionSession.setSessionId("1");
        executionSession.setHostname("test-host");
        executionSession.setCreatedAt(new Date());
        executionSession.setLastUpdateAt(0);
        SessionInstance sessionInstance = new SessionInstance(executionSession, new ServerMetadata(), project);

        while (sessionInstance.getProcessedFileCount() < 6) {
            continue;
        }

        CountDownLatch cdl = new CountDownLatch(1);

        sessionInstance.getTestCandidates(testCandidateMetadata -> {
            cdl.countDown();
        }, 0, new StompFilterModel(), new AtomicInteger(1));

        cdl.await();
        sessionInstance.close();
        cleanUpFolder(new File(sessionPath));
    }


    @Test
    public void testScan2() throws SQLException, IOException, InterruptedException {
        String sessionPath = SESSIONS_PATH + "selogger-3";
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getName()).thenReturn("test-project");
        new UnloggedLocalClient(sessionPath);
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setPath(sessionPath);
        executionSession.setSessionId("1");
        executionSession.setHostname("test-host");
        executionSession.setCreatedAt(new Date());
        executionSession.setLastUpdateAt(0);
        SessionInstance sessionInstance = new SessionInstance(executionSession, new ServerMetadata(), project);

        int zipCount = Objects.requireNonNull(new File(sessionPath).listFiles()).length;
        while (sessionInstance.getProcessedFileCount() < zipCount) {
            continue;
        }

        CountDownLatch cdl = new CountDownLatch(1);

        sessionInstance.getTestCandidates(testCandidateMetadata -> {
            cdl.countDown();
        }, 0, new StompFilterModel(), new AtomicInteger(1));

        cdl.await();
        sessionInstance.close();
        cleanUpFolder(new File(sessionPath));
    }

    @Test
    public void testScanReactive() throws SQLException, IOException, InterruptedException {
        String sessionPath = SESSIONS_PATH + "selogger-6";
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
        AtomicBoolean done = new AtomicBoolean(false);
        sessionInstance.getTestCandidates(testCandidateMetadata -> {
            System.out.println("Candidates size : " + testCandidateMetadata.size());
            for (TestCandidateBareBone bareBoneCandidate : testCandidateMetadata) {
                TestCandidateMetadata testCandidateMetadataLoaded = sessionInstance.getTestCandidateById(bareBoneCandidate.getId(), true);
                String returnValue = getOutputText(testCandidateMetadataLoaded);
                System.out.println("Class : " + bareBoneCandidate.getMethodUnderTest().getClassName());
                System.out.println("Method : " + bareBoneCandidate.getMethodUnderTest().getName());
                System.out.println("Candidate output : " + returnValue);
                System.out.println("\n");
            }
            if (!done.get()) {
                cdl.countDown();
            }
        }, 0, new StompFilterModel(), new AtomicInteger(1));

        cdl.await();
        sessionInstance.close();
        cleanUpFolder(new File(sessionPath));
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
            // not a serializable value
//                            valueForParameter = objectMapper.createObjectNode();
        } else {
            return valueForParameter.toString();
        }
        return "Not serializable";
    }
}