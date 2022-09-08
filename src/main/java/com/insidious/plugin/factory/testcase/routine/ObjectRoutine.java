package com.insidious.plugin.factory.testcase.routine;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * ObjectRoutine is representing a block of code, close to a method, containing all the
 * statements and dependent variables along with their own object routines (the whole hierarchy
 * should be available to recreate this object inside a test case)
 */
@AllArgsConstructor
public class ObjectRoutine {
    private final String routineName;
    private final Map<String, ObjectRoutineContainer> dependentMap = new HashMap<>();
    private final List<ObjectRoutineContainer> dependentList = new LinkedList<>();
    private VariableContainer variableContainer = new VariableContainer();
    private List<TestCandidateMetadata> testCandidateList = new LinkedList<>();
    private final static Logger logger = LoggerUtil.getInstance(ObjectRoutine.class);

    public ObjectRoutine() {
        routineName = "<init>";
    }
    public ObjectRoutine(String routineName) {
        this.routineName = routineName;
    }

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }


//    public void setMetadata(TestCandidateMetadata metadata) {
//        this.metadata = new LinkedList<>(List.of(metadata));
//    }

    public List<TestCandidateMetadata> getTestCandidateList() {
        return testCandidateList;
    }

    public void setTestCandidateList(TestCandidateMetadata newTestCaseMetadata) {
        List<TestCandidateMetadata> testCandidateMetadata = new java.util.ArrayList<>();
        testCandidateMetadata.add(newTestCaseMetadata);
        this.testCandidateList = new LinkedList<>(testCandidateMetadata);
    }

    public void addMetadata(TestCandidateMetadata newTestCaseMetadata) {
        if (this.testCandidateList == null) {
            this.testCandidateList = new LinkedList<>();
        }
        if (!this.testCandidateList.contains(newTestCaseMetadata)) {
            this.testCandidateList.add(newTestCaseMetadata);
        }
    }

    public String getRoutineName() {
        return routineName;
    }

    public List<ObjectRoutineContainer> getDependentList() {
        return dependentList;
    }

    public void addDependent(ObjectRoutineContainer dependentObjectCreation) {
        if (this.dependentMap.containsKey(dependentObjectCreation.getName())) {
            // throw new RuntimeException("dependent already exists");
            return;
        }
        this.dependentList.add(dependentObjectCreation);
        this.dependentMap.put(dependentObjectCreation.getName(), dependentObjectCreation);
    }


    public ObjectRoutineScript toObjectScript(
            List<String> variableContainer
    ) {
        ObjectRoutineScript scriptContainer = new ObjectRoutineScript(
                this.routineName
        );

            if (getRoutineName().equals("<init>")) {
                return scriptContainer;
            }

        for (TestCandidateMetadata testCandidateMetadata : this.testCandidateList) {
            ObjectRoutineScript script = testCandidateMetadata.toObjectScript();

            script.getCreatedVariables().all().forEach(scriptContainer.getCreatedVariables()::add);
            scriptContainer.getStatements().addAll(script.getStatements());

        }

//            dependentObjectsList.addAll(0, objectRoutine.getDependentList());
//            ObjectRoutineScriptContainer dependentScript = createScriptsFromRoutines(
//                    objectRoutine.getDependentList(), variableContainer);

//            for (Pair<CodeLine, Object[]> statement : dependentScript.getStatements()) {
//                CodeLine line = statement.getFirst();
//                if (line instanceof StatementCodeLine) {
//                    logger.warn("Add statement: [" + line.getLine() + "]");
//                    scriptContainer.addStatement(line.getLine(), statement.getSecond());
//                } else {
//                    String line1 = line.getLine();
//                    if (line1.contains("$")) {
//                        line1 = line1.replace('$', '_');
//                    }
//                    scriptContainer.addComment(line1, statement.getSecond());
//                }
//
//            }

            return scriptContainer;

    }

}
