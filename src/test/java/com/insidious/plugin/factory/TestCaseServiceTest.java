package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.pojo.TestCandidate;
import com.insidious.plugin.pojo.TestSuite;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collection;
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

        testCaseService.listTestCandidatesByMethods(
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
                        try {
                            logger.info("generating test cases for " + testCandidates.size());
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
                        TestSuite testSuite = null;
                        try {
                            testSuite = testCaseService.generateTestCase(testCandidates);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(testSuite);

                    }

                    public void completed() {

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

                        for (TestCandidate testCandidate : testCandidates) {
                            try {
                                TestCaseScript testCaseScript =
                                        testCaseService.generateTestCase(testCandidate);
                                System.out.println(testCaseScript);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    public void completed() {

                    }
                }
        );
    }
}
