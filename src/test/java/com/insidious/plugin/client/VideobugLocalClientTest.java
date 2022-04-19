package com.insidious.plugin.client;

import com.insidious.common.DebugPoint;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.SigninRequest;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


class VideobugLocalClientTest {

    private VideobugLocalClient videobugLocalClientUnderTest;

    @BeforeEach
    void setUp() {
        videobugLocalClientUnderTest = new VideobugLocalClient("pathToSessions");
    }

    @Test
    void testGetCurrentSession() {
        // Setup
        // Run the test
        final ExecutionSession result = videobugLocalClientUnderTest.getCurrentSession();

        // Verify the results
    }

    @Test
    void testSignup() {
        // Setup
        final SignUpCallback mockCallback = mock(SignUpCallback.class);

        // Run the test
        videobugLocalClientUnderTest.signup("serverUrl", "username", "password", mockCallback);

        // Verify the results
    }

    @Test
    void testSignin() {
        // Setup
        final SigninRequest signinRequest = new SigninRequest("serverUrl", "usernameText", "passwordText");
        final SignInCallback mockSignInCallback = mock(SignInCallback.class);

        // Run the test
        videobugLocalClientUnderTest.signin(signinRequest, mockSignInCallback);

        // Verify the results
    }

    @Test
    void testGetProjectByName() {
        // Setup
        final GetProjectCallback mockGetProjectCallback = mock(GetProjectCallback.class);

        // Run the test
        videobugLocalClientUnderTest.getProjectByName("projectName", mockGetProjectCallback);

        // Verify the results
    }

    @Test
    void testFetchProjectByName() {
        // Setup
        // Run the test
        final ProjectItem result = videobugLocalClientUnderTest.fetchProjectByName("projectName");

        // Verify the results
    }

    @Test
    void testCreateProject() {
        // Setup
        final NewProjectCallback mockNewProjectCallback = mock(NewProjectCallback.class);

        // Run the test
        videobugLocalClientUnderTest.createProject("projectName", mockNewProjectCallback);

        // Verify the results
    }

    @Test
    void testGetProjectToken() {
        // Setup
        final ProjectTokenCallback mockProjectTokenCallback = mock(ProjectTokenCallback.class);

        // Run the test
        videobugLocalClientUnderTest.getProjectToken(mockProjectTokenCallback);

        // Verify the results
    }

    @Test
    void testGetProjectSessions() throws Exception {
        // Setup
        final GetProjectSessionsCallback mockGetProjectSessionsCallback = mock(GetProjectSessionsCallback.class);

        // Run the test
        videobugLocalClientUnderTest.getProjectSessions(mockGetProjectSessionsCallback);

        // Verify the results
    }

    @Test
    void testGetProjectSessions_ThrowsIOException() {
        // Setup
        final GetProjectSessionsCallback mockGetProjectSessionsCallback = mock(GetProjectSessionsCallback.class);

        // Run the test
        assertThrows(IOException.class,
                () -> videobugLocalClientUnderTest.getProjectSessions(mockGetProjectSessionsCallback));
    }

    @Test
    void testFetchProjectSessions() {
        // Setup
        // Run the test
        final DataResponse<ExecutionSession> result = videobugLocalClientUnderTest.fetchProjectSessions();

        // Verify the results
    }

    @Test
    void testGetTracesByObjectType() throws Exception {
        // Setup
        final GetProjectSessionErrorsCallback mockGetProjectSessionErrorsCallback = mock(
                GetProjectSessionErrorsCallback.class);

        // Run the test
        videobugLocalClientUnderTest.getTracesByObjectType(List.of("value"), -1, mockGetProjectSessionErrorsCallback);

        // Verify the results
    }

    @Test
    void testGetTracesByObjectType_ThrowsIOException() {
        // Setup
        final GetProjectSessionErrorsCallback mockGetProjectSessionErrorsCallback = mock(
                GetProjectSessionErrorsCallback.class);

        // Run the test
        assertThrows(IOException.class, () -> videobugLocalClientUnderTest.getTracesByObjectType(List.of("value"), -1,
                mockGetProjectSessionErrorsCallback));
    }

    @Test
    void testGetTracesByObjectValue() throws Exception {
        // Setup
        final GetProjectSessionErrorsCallback mockGetProjectSessionErrorsCallback = mock(
                GetProjectSessionErrorsCallback.class);

        // Run the test
        videobugLocalClientUnderTest.getTracesByObjectValue("value", mockGetProjectSessionErrorsCallback);

        // Verify the results
    }

    @Test
    void testGetTracesByObjectValue_ThrowsIOException() {
        // Setup
        final GetProjectSessionErrorsCallback mockGetProjectSessionErrorsCallback = mock(
                GetProjectSessionErrorsCallback.class);

        // Run the test
        assertThrows(IOException.class, () -> videobugLocalClientUnderTest.getTracesByObjectValue("value",
                mockGetProjectSessionErrorsCallback));
    }

    @Test
    void testFetchDataEvents() throws Exception {
        // Setup
        final FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId("sessionId");
        filteredDataEventsRequest.setThreadId(0L);
        filteredDataEventsRequest.setValueId(List.of(0L));
        filteredDataEventsRequest.setPageSize(0);
        filteredDataEventsRequest.setPageNumber(0);
        final DebugPoint debugPoint = new DebugPoint();
        debugPoint.setFile("file");
        debugPoint.setLineNumber(0);
        filteredDataEventsRequest.setDebugPoints(List.of(debugPoint));
        filteredDataEventsRequest.setSortOrder("sortOrder");
        filteredDataEventsRequest.setNanotime(0L);
        filteredDataEventsRequest.setProbeId(0);

        // Run the test
        final ReplayData result = videobugLocalClientUnderTest.fetchDataEvents(filteredDataEventsRequest);

        // Verify the results
    }

    @Test
    void testFetchDataEvents_ThrowsIOException() {
        // Setup
        final FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId("sessionId");
        filteredDataEventsRequest.setThreadId(0L);
        filteredDataEventsRequest.setValueId(List.of(0L));
        filteredDataEventsRequest.setPageSize(0);
        filteredDataEventsRequest.setPageNumber(0);
        final DebugPoint debugPoint = new DebugPoint();
        debugPoint.setFile("file");
        debugPoint.setLineNumber(0);
        filteredDataEventsRequest.setDebugPoints(List.of(debugPoint));
        filteredDataEventsRequest.setSortOrder("sortOrder");
        filteredDataEventsRequest.setNanotime(0L);
        filteredDataEventsRequest.setProbeId(0);

        // Run the test
        assertThrows(IOException.class, () -> videobugLocalClientUnderTest.fetchDataEvents(filteredDataEventsRequest));
    }

    @Test
    void testGetToken() {
        assertEquals("localhost-token", videobugLocalClientUnderTest.getToken());
    }

    @Test
    void testGetProject() {
        // Setup
        // Run the test
        final ProjectItem result = videobugLocalClientUnderTest.getProject();

        // Verify the results
    }

    @Test
    void testSetProject() {
        // Setup
        // Run the test
        videobugLocalClientUnderTest.setProject("projectName");

        // Verify the results
    }

    @Test
    void testGetEndpoint() {
        assertEquals("pathToSessions", videobugLocalClientUnderTest.getEndpoint());
    }

    @Test
    void testGetAgentDownloadUrl() {
        // Setup
        final AgentDownloadUrlCallback mockAgentDownloadUrlCallback = mock(AgentDownloadUrlCallback.class);

        // Run the test
        videobugLocalClientUnderTest.getAgentDownloadUrl(mockAgentDownloadUrlCallback);

        // Verify the results
    }

    @Test
    void testDownloadAgentFromUrl() {
        // Setup
        // Run the test
        videobugLocalClientUnderTest.downloadAgentFromUrl("url", "insidiousLocalPath", false);

        // Verify the results
    }
}
