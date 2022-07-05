package com.insidious.plugin.factory;

import com.insidious.common.weaver.EventType;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.TestCandidate;
import com.insidious.plugin.pojo.TracePoint;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class TestCaseService {
    private final Project project;
    private final VideobugClientInterface client;

    public TestCaseService(Project project, VideobugLocalClient client) {
        this.project = project;
        this.client = client;
    }

    public void listTestCandidates(ClientCallBack<TestCandidate> tracePointsCallback) throws APICallException, IOException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            tracePointsCallback.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);

        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
//                EventType.METHOD_EXCEPTIONAL_EXIT,
//                EventType.METHOD_OBJECT_INITIALIZED,
//                EventType.METHOD_PARAM,
//                EventType.METHOD_THROW,
//                EventType.METHOD_NORMAL_EXIT,
                        EventType.METHOD_ENTRY
                )
        );
        this.client.queryTracePointsByDataIds(searchQuery,
                session.getSessionId(), tracePointsCallback);
    }
    public void listTestCandidatesByMethods(ClientCallBack<TestCandidate> tracePointsCallback) throws APICallException, IOException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            tracePointsCallback.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);

//        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
////                EventType.METHOD_EXCEPTIONAL_EXIT,
////                EventType.METHOD_OBJECT_INITIALIZED,
////                EventType.METHOD_PARAM,
////                EventType.METHOD_THROW,
////                EventType.METHOD_NORMAL_EXIT,
//                        EventType.METHOD_ENTRY
//                )
//        );

        this.client.getMethods(session.getSessionId(), tracePointsCallback);
    }

    @NotNull
    private TracePoint dummyTracePoint() {
        return new TracePoint(
                1,
                2,
                3, 4, 5,
                "file.java",
                "ClassName",
                "ExceptionClassName",
                1234,
                1235);
    }

    public String generateTestCase(TestCandidate testCandidate) throws IOException {

        ClassNode classNode = new ClassNode();
        classNode.version = V1_8;
        classNode.access = ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE;

        classNode.name = "pkg/Comparable";
        classNode.superName = "java/lang/Object";
        classNode.interfaces.add("pkg/Mesurable");

        classNode.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "LESS", "I", null, -1));

        classNode.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "EQUAL", "I", null, 0));

        classNode.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "GREATER", "I", null, 1));

        classNode.methods.add(new MethodNode(ACC_PUBLIC + ACC_ABSTRACT, "compareTo", "(Ljava/lang/Object;)I", null, null));


        String classString = classNode.toString();

        return classString;

//        BufferedOutputStream boss = new BufferedOutputStream(outputStream);
//        DataOutputStream doss = new DataOutputStream(boss);
//        doss.write("package com.package.test\n".getBytes());
//        doss.write("\t@Test".getBytes());
//        doss.write("\t\tpublic void testMethod1() {\n".getBytes());
//        doss.write("\t}\n".getBytes());


    }
}
