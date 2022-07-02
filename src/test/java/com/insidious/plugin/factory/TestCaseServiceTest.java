package com.insidious.plugin.factory;

import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.pojo.TracePoint;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class TestCaseServiceTest {


    @Test
    void testGeneration() throws IOException {
        Project project = Mockito.mock(Project.class);
        Mockito.when(project.getBasePath()).thenReturn("./");

        VideobugLocalClient client = new VideobugLocalClient(System.getenv("user.home") + "/.videobug/sessions");

        TestCaseService testCaseService = new TestCaseService(project, client);

        List<TracePoint> testCandidates = testCaseService.listTestCandidates();


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        String testCaseScript = testCaseService.generateTestCase(testCandidates.get(0));

//        byte[] testCaseScript = outputStream.toByteArray();

        System.out.println(testCaseScript);

    }
}
