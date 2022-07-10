package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.pojo.*;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class TestCaseServiceTest {


    private final static Logger logger = Logger.getLogger(TestCaseServiceTest.class.getName());

    @Test
    void testGeneration() throws APICallException, IOException {
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(
                System.getenv("USERPROFILE") + "/.videobug/sessions");

        TestCaseService testCaseService = new TestCaseService(project, client);

        int typeId = 174;
        testCaseService.listTestCandidatesByMethods(typeId,
                new ClientCallBack<>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        assert false;
                    }

                    @Override
                    public void success(Collection<TestCandidate> testCandidates) {

                        if (testCandidates.size() == 0) {
                            return;
                        }
                        TestSuite testSuite = null;
                        logger.info("generating test cases for " + testCandidates.size());
                        try {
                            testSuite = testCaseService.generateTestCase(testCandidates);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(testSuite);

                    }

                    public void completed() {
                        logger.info("Received all test candidates");
                    }
                }
        );
    }

    @Test
    void testGenerationUsingClassWeave() throws APICallException, IOException, InterruptedException {
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(
                System.getenv("USERPROFILE") + "/.videobug/sessions");

        TestCaseService testCaseService = new TestCaseService(project, client);

        List<TestCandidate> testCandidateList = new LinkedList<>();
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);
        testCaseService.getTestCandidates(
                new ClientCallBack<>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        assert false;
                    }

                    @Override
                    public void success(Collection<TestCandidate> testCandidates) {

                        if (testCandidates.size() == 0) {
                            return;
                        }
                        testCandidateList.addAll(testCandidates);

                    }

                    public void completed() {
                        waiter.offer("ok");
                    }
                }
        );
        waiter.take();

        TestSuite testSuite = null;
        testSuite = testCaseService.generateTestCase(testCandidateList);
        System.out.println(testSuite);

    }

    @Test
    void testGenerateByObjects() throws InterruptedException, APICallException, IOException {


        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(
                System.getenv("USERPROFILE") + "/.videobug/sessions");

        TestCaseService testCaseService = new TestCaseService(project, client);

        List<TestCandidate> testCandidateList = new LinkedList<>();
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);


        List<String> targetClasses = List.of("org.zerhusen.service.Adder");

        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

        List<ObjectsWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);

        client.getObjectsByType(
                searchQuery, session.getSessionId(), new ClientCallBack<ObjectsWithTypeInfo>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(Collection<ObjectsWithTypeInfo> tracePoints) {
                        allObjects.addAll(tracePoints);
                    }

                    @Override
                    public void completed() {
                        waiter.offer("done");
                    }
                }
        );
        waiter.take();

        TestSuite testSuite = testCaseService.generateTestCase(targetClasses, allObjects);

        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
            System.out.println(testCaseScript);
        }


    }
}
