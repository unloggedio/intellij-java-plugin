package com.insidious.plugin.client;

import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.ActiveSessionManager;
import com.intellij.openapi.project.Project;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class SessionInstanceTest extends TestCase {


    public static final String SESSIONS_PATH = "/Users/artpar/workspace/code/insidious/plugin/src/test/resources/test-sessions/";

    @Test
    public void testScan1() throws SQLException, IOException, InterruptedException {
        String sessionPath = SESSIONS_PATH + "selogger-2";
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getName()).thenReturn("test-project");
        new VideobugLocalClient(sessionPath, project, new ActiveSessionManager());
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setPath(sessionPath);
        executionSession.setSessionId("1");
        executionSession.setHostname("test-host");
        executionSession.setCreatedAt(new Date());
        executionSession.setLastUpdateAt(0);
        SessionInstance sessionInstance = new SessionInstance(executionSession, project);

        while (sessionInstance.getProcessedFileCount() < 6) {
            continue;
        }

        CountDownLatch cdl = new CountDownLatch(1);

        sessionInstance.getAllTestCandidates(testCandidateMetadata -> {
            cdl.countDown();
        });


        cdl.await();

        sessionInstance.close();


        cleanUpFolder(new File(sessionPath));
    }


    @Test
    public void testScan2() throws SQLException, IOException, InterruptedException {
        String sessionPath = SESSIONS_PATH + "selogger-3";
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getName()).thenReturn("test-project");
        new VideobugLocalClient(sessionPath, project, new ActiveSessionManager());
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setPath(sessionPath);
        executionSession.setSessionId("1");
        executionSession.setHostname("test-host");
        executionSession.setCreatedAt(new Date());
        executionSession.setLastUpdateAt(0);
        SessionInstance sessionInstance = new SessionInstance(executionSession, project);

        int zipCount = new File(sessionPath).listFiles().length;
        while (sessionInstance.getProcessedFileCount() < zipCount) {
            continue;
        }

        CountDownLatch cdl = new CountDownLatch(1);

        sessionInstance.getAllTestCandidates(testCandidateMetadata -> {
            cdl.countDown();
        });


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
}