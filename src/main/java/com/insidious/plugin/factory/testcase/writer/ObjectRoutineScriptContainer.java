package com.insidious.plugin.factory.testcase.writer;


import com.insidious.plugin.factory.CodeLine;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutine;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
@AllArgsConstructor
public class ObjectRoutineScriptContainer {
    private final static Logger logger = LoggerUtil.getInstance(ObjectRoutineScript.class);
    private final List<ObjectRoutineScript> objectRoutines = new LinkedList<>();
    private List<FieldSpec> fieldSpecList = new LinkedList<>();
    private List<MethodSpec> methodSpecList = new LinkedList<>();
    private ObjectRoutineScript currentRoutine;
    private ObjectRoutineScript constructor = newRoutine("<init>");
    /**
     * Name for variable for this particular object
     */
    private String name;


    public ObjectRoutineScriptContainer(List<ObjectRoutineScript> constructorRoutine) {
        for (ObjectRoutineScript objectRoutine : constructorRoutine) {
            if (objectRoutine.getRoutineName().equals("<init>")) {
                this.constructor = objectRoutine;
            }
        }
        this.objectRoutines.clear();
        this.objectRoutines.addAll(constructorRoutine);
        this.currentRoutine = constructorRoutine.get(constructorRoutine.size() - 1);

    }

    public ObjectRoutineScriptContainer() {
    }

    public ObjectRoutineScriptContainer(List<FieldSpec> fieldSpecList, List<MethodSpec> methodSpecList) {

        this.fieldSpecList = fieldSpecList;
        this.methodSpecList = methodSpecList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ObjectRoutineScript> getObjectRoutines() {
        return objectRoutines;
    }


    public void addField(FieldSpec field) {
        this.fieldSpecList.add(field);
    }

    public ObjectRoutineScript newRoutine(String routineName) {
        if (routineName == null) {
            return new ObjectRoutineScript();
        }
        for (ObjectRoutineScript objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals(routineName)) {
                this.currentRoutine = objectRoutine;
                return objectRoutine;
            }
        }

        ObjectRoutineScript newRoutine = new ObjectRoutineScript(routineName);
        this.objectRoutines.add(newRoutine);
        this.currentRoutine = newRoutine;
        return newRoutine;
    }

    public List<Pair<CodeLine, Object[]>> getStatements() {
        return currentRoutine.getStatements();
    }

    public ObjectRoutineScript getConstructor() {
        return constructor;
    }

//    public void addScriptsFromRoutineContainer(
//            ObjectRoutineContainer objectRoutineContainer,
//            List<String> variableContainer
//    ) {
//        ObjectRoutineScript scriptContainer = this.newRoutine(objectRoutineContainer.getName());
//
//
//        if (!variableContainer.contains(scriptContainer.getName())) {
//
//            ObjectRoutine constructorRoutine = objectRoutineContainer.getConstructor();
//            ObjectRoutineScript scripts = constructorRoutine.toObjectScript(variableContainer);
//            this.objectRoutines.add(scripts);
////            ObjectRoutineScript outputScript = new ObjectRoutineScript(constructorRoutine.getRoutineName());
//
////            dependentObjectsList.addAll(0, constructorRoutine.getDependentList());
//
//
////                for (Pair<CodeLine, Object[]> statement : constructorRoutine.getCreatedVariables()  ) {
////                    CodeLine line = statement.getFirst();
////                    if (line instanceof StatementCodeLine) {
////                        outputScript.addStatement(line.getLine(), statement.getSecond());
////                    } else {
////                        String commentLine = line.getLine();
////                        if (commentLine.contains("$")) {
////                            commentLine = commentLine.replace('$', '_');
////                        }
////                        outputScript.addComment(commentLine, statement.getSecond());
////                    }
////                }
//        } else {
//            // variable has already been initialized
//
//        }
//
//        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {
//            if (objectRoutine.getRoutineName().equals("<init>")) {
//                continue;
//            }
////            dependentObjectsList.addAll(0, objectRoutine.getDependentList());
//            ObjectRoutineScriptContainer dependentScript = createScriptsFromRoutines(
//                    objectRoutine.getDependentList(), variableContainer);
//
//
//            this.fieldSpecList.addAll(dependentScript.getFieldSpecList());
//            this.objectRoutines.addAll(dependentScript.getObjectRoutines());
//
//            ObjectRoutineScript scripts = objectRoutine.toObjectScript(variableContainer);
//            this.objectRoutines.add(scripts);
//
//
////            for (Pair<CodeLine, Object[]> statement : dependentScript.getStatements()) {
////                CodeLine line = statement.getFirst();
////                if (line instanceof StatementCodeLine) {
////                    logger.warn("Add statement: [" + line.getLine() + "]");
////                    scriptContainer.addStatement(line.getLine(), statement.getSecond());
////                } else {
////                    String line1 = line.getLine();
////                    if (line1.contains("$")) {
////                        line1 = line1.replace('$', '_');
////                    }
////                    scriptContainer.addComment(line1, statement.getSecond());
////                }
////
////            }
//
//        }
//
//    }

    /**
     * this is part 4, not a heavy lifter
     *
     * @param dependentObjectsList source
     * @param variableContainer    context
     * @return ObjectRoutineScriptContainer real statements with args
     */
    public ObjectRoutineScriptContainer createScriptsFromRoutines(
            List<ObjectRoutineContainer> dependentObjectsList, List<String> variableContainer) {
        ObjectRoutineScriptContainer routineScriptContainer = new ObjectRoutineScriptContainer();


        dependentObjectsList = new LinkedList<>(dependentObjectsList);
        while (dependentObjectsList.size() > 0) {
            ObjectRoutineContainer objectRoutineContainer = dependentObjectsList.remove(0);

            for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {

                ObjectRoutineScript script = objectRoutine.toObjectScript(variableContainer);
                routineScriptContainer.getObjectRoutines().add(script);
            }

        }
        return routineScriptContainer;
    }


//    public void writeAsCode(MethodSpec.Builder methodBuilder) {
//        constructor.writeToMethodSpecBuilder(methodBuilder);
//    }

    public List<MethodSpec> getMethodSpecList() {
        return this.methodSpecList;
//        return objectRoutines.stream()
//                .map(e -> {
//                    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(e.getRoutineName());
//                    e.writeToMethodSpecBuilder(methodBuilder);
//                    return methodBuilder.build();
//                }).collect(Collectors.toList());
    }

    public Collection<FieldSpec> toFieldSpecsList() {
        return this.fieldSpecList;
    }

    public List<FieldSpec> getFieldSpecList() {
        return fieldSpecList;
    }
}
