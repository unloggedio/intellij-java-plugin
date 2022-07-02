package com.insidious.plugin.factory;

import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.pojo.TracePoint;
import com.intellij.openapi.project.Project;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class TestCaseService {
    private final Project project;

    public TestCaseService(Project project, VideobugLocalClient client) {
        this.project = project;
    }

    public List<TracePoint> listTestCandidates() {
        return List.of(
                new TracePoint(1, 2, 3, 4,
                        5, "file.java", "ClassName",
                        "ExceptionClassName", 1234, 1235)
        );
    }

    public String generateTestCase(TracePoint tracePoint) throws IOException {

        ClassNode classNode = new ClassNode();
        classNode.version = V1_8;
        classNode.access = ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE;

        classNode.name = "pkg/Comparable";
        classNode.superName = "java/lang/Object";
        classNode.interfaces.add("pkg/Mesurable");

        classNode.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
                "LESS", "I", null, -1));

        classNode.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
                "EQUAL", "I", null, 0));

        classNode.fields.add(new FieldNode(ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
                "GREATER", "I", null, 1));

        classNode.methods.add(new MethodNode(ACC_PUBLIC + ACC_ABSTRACT,
                "compareTo", "(Ljava/lang/Object;)I", null, null));


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
