package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.TestCaseRequest;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.pojo.ObjectWithTypeInfo;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.TestCandidate;
import com.insidious.plugin.pojo.TestSuite;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class InsidiousServiceTestGenerateTestCaseFlows extends TestCase {


    @Test
    public void newMethod() throws InterruptedException, APICallException {


        Project project = Mockito.mock(Project.class);

        List<TestCandidate> testCandidateList = new LinkedList<>();
        BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);
        VideobugLocalClient client = new VideobugLocalClient("C:\\Users\\artpa\\.videobug\\sessions");
        DataResponse<ExecutionSession> sessionList = client.fetchProjectSessions();
        client.setSessionInstance(sessionList.getItems().get(0));

        TestCaseService testCaseService = new TestCaseService(client);


        List<String> targetClasses = List.of("com.repyute.service.zoho.ZohoService");

        SearchQuery searchQuery = SearchQuery.ByType(targetClasses);

        List<ObjectWithTypeInfo> allObjects = new LinkedList<>();

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        ExecutionSession session = sessions.getItems().get(0);

        client.getObjectsByType(
                searchQuery, session.getSessionId(), new ClientCallBack<>() {
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

        if (allObjects.size() == 0) {
            InsidiousNotification.notifyMessage("Could not find any instances of [" + targetClasses + "]",
                    NotificationType.WARNING);
        }

        TestCaseRequest testRequest = new TestCaseRequest(
                allObjects, List.of(
                "com.fasterxml",
                "com.google"
        ), Set.of()
        );
        TestSuite testSuite = null;
        try {
            testSuite = testCaseService.generateTestCase(testRequest);
        } catch (SessionNotSelectedException e) {
            InsidiousNotification.notifyMessage(
                    "Failed to generate test suite: " + e.getMessage(), NotificationType.ERROR
            );
        }


//        @Nullable VirtualFile newFile = saveTestSuite(testSuite);
//        if (newFile == null) {
//            logger.warn("Test case generated for [" + targetClasses + "] but failed to write");
//            InsidiousNotification.notifyMessage("Failed to write test case to file", NotificationType.ERROR);
//        }


    }
}