package com.insidious.plugin.factory;

import com.esotericsoftware.asm.Opcodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestCaseService {
    public static final ClassName JUNIT_CLASS_NAME = ClassName.get("org.junit", "Test");
    private final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final Project project;
    private final VideobugClientInterface client;
    final private int MAX_TEST_CASE_LINES = 1000;
    private final CompareMode MODE = CompareMode.SERIALIZED_JSON;
    private final ClassName assertClass = ClassName.bestGuess("org.junit.Assert");
    private final ClassName mockitoClass = ClassName.bestGuess("org.mockito.Mockito");

    public TestCaseService(Project project, VideobugClientInterface client) {
        this.project = project;
        this.client = client;
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

    void buildTestFromTestMetadataSet(ObjectRoutine objectRoutine) {
        assert objectRoutine.getMetadata().size() != 0;
        List<TestCandidateMetadata> metadataCollection = objectRoutine.getMetadata();
        VariableContainer variableContainer = objectRoutine.getVariableContainer();
        VariableContainer createdVariableContainer = new VariableContainer();


        for (TestCandidateMetadata testCandidateMetadata : metadataCollection) {

            MethodCallExpression mainMethodExpression = testCandidateMetadata.getMainMethod();

            objectRoutine.addComment("");
            objectRoutine.addComment("Test candidate method ["
                    + mainMethodExpression.getMethodName()
                    + "] [ " + mainMethodExpression.getReturnValue().getProb().getNanoTime() + "] - took " +
                    Long.valueOf(testCandidateMetadata.getCallTimeNanoSecond() / (1000000)).intValue() + "ms");

            if (mainMethodExpression.getArguments().count() > 0) {


                objectRoutine.addComment("");
                for (Parameter parameter : mainMethodExpression.getArguments().all()) {
                    if (parameter.getName() == null && parameter.getProb().getSerializedValue().length > 0) {
                        String serializedValue = new String(parameter.getProb().getSerializedValue());
                        if (parameter.getType().equals("java.lang.String")) {
                            serializedValue = '"' + serializedValue + '"';
                        }
                        parameter.setValue(serializedValue);
                    }
                    objectRoutine.addParameterComment(parameter);
                }
                objectRoutine.addComment("");
                objectRoutine.addComment("");
            }


            if (testCandidateMetadata.getCallsList().size() > 0) {


                objectRoutine.addComment("");
                for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
                    TestCaseWriter.createMethodCallComment(objectRoutine, variableContainer,
                            methodCallExpression);
                }

                for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
                    TestCaseWriter.createMethodCall(objectRoutine, variableContainer, createdVariableContainer, methodCallExpression);
                }

                objectRoutine.addComment("");
                objectRoutine.addComment("");
            }


            TypeName returnValueSquareClass = null;
            String returnParameterType = mainMethodExpression.getReturnValue().getType();
            if (returnParameterType == null) {
                logger.warn("parameter return type is null: " + testCandidateMetadata);
                return;
            }
            returnValueSquareClass = createTypeFromName(returnParameterType);


            //////////////////////// FUNCTION CALL ////////////////////////

            String parameterString =
                    TestCaseWriter.createMethodParametersString(mainMethodExpression.getArguments());

            // return type == V ==> void return type => no return value
            Parameter testSubject = testCandidateMetadata.getTestSubject();
            if (mainMethodExpression.getMethodName().equals("<init>")) {


                ClassName squareClassName = ClassName.get(testCandidateMetadata.getPackageName(),
                        testCandidateMetadata.getUnqualifiedClassname());


                objectRoutine.addStatement("$T $L = new $T(" + parameterString + ")",
                        squareClassName,
                        testSubject.getName(),
                        squareClassName);


            } else {
                Parameter returnParameter = mainMethodExpression.getReturnValue();
                testSubject = testCandidateMetadata.getTestSubject();
                String testSubjectName = testSubject.getName();


                if (returnParameter.getType().equals("V")) {

                    objectRoutine.addStatement("$L.$L(" + parameterString + ")",
                            testSubjectName, mainMethodExpression.getMethodName());

                } else {
                    Object returnValue = returnParameter.getValue();

                    String returnSubjectInstanceName = returnParameter.getName();

                    if (variableContainer.contains(returnSubjectInstanceName)) {
                        objectRoutine.addStatement("$L = $L.$L(" + parameterString + ")",
                                returnSubjectInstanceName,
                                testSubjectName,
                                mainMethodExpression.getMethodName());

                    } else {
                        objectRoutine.addStatement("$T $L = $L.$L(" + parameterString + ")",
                                returnValueSquareClass, returnSubjectInstanceName,
                                testSubjectName,
                                mainMethodExpression.getMethodName());
                        variableContainer.add(returnParameter);
                    }


                    String returnType = returnParameter.getType();


                    //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


                    // deserialize and compare objects
                    byte[] serializedBytes = returnParameter.getProb().getSerializedValue();
                    if (serializedBytes == null) {
                        serializedBytes = new byte[0];
                    }
                    String serializedValue = "";
                    if (serializedBytes.length > 0) {
                        serializedValue = new String(serializedBytes);
//                        objectRoutine.addComment("Serialized value: " + serializedValue);
                    }

                    // reconstruct object from the serialized form to an object instance in the
                    // test method to compare it with the new object, or do it the other way
                    // round ? Maybe serializing the object and then comparing the serialized
                    // string forms would be more readable ? string comparison would fail if the
                    // serialization has fields serialized in random order
                    if (serializedBytes.length > 0) {
                        if (MODE == CompareMode.OBJECT) {
                            objectRoutine.addStatement("$T $L = gson.fromJson($S, $T.class)",
                                    returnValueSquareClass,
                                    returnSubjectInstanceName + "Expected",
                                    serializedValue,
                                    returnValueSquareClass
                            );
                            returnValue = returnSubjectInstanceName + "Expected";
                        } else if (MODE == CompareMode.SERIALIZED_JSON) {
                            objectRoutine.addStatement("$T $L = gson.toJson($L)",
                                    String.class,
                                    returnSubjectInstanceName + "Json",
                                    returnSubjectInstanceName
                            );
                            objectRoutine.addStatement("$T $L = $S",
                                    String.class,
                                    returnSubjectInstanceName + "ExpectedJson",
                                    serializedValue
                            );
                            returnValue = returnSubjectInstanceName + "ExpectedJson";
                            returnSubjectInstanceName = returnSubjectInstanceName + "Json";
                        }
                    }


                    if (returnType.equals("Ljava/lang/String;") && returnParameter.getName() == null) {
                        objectRoutine.addStatement("$T.assertEquals($L, $L)",
                                assertClass,
                                returnParameter.getValue(),
                                returnSubjectInstanceName
                        );
                    } else {
                        if (returnType.equals("Ljava.lang.Boolean;") || returnType.equals("Z")) {
                            if (returnValue instanceof String) {

                            } else if (returnValue instanceof Long) {
                                if ((long) returnValue == 1) {
                                    returnValue = "true";
                                } else {
                                    returnValue = "false";
                                }

                            }
                        }
                        objectRoutine.addStatement("$T.assertEquals($L, $L)",
                                assertClass,
                                returnValue,
                                returnSubjectInstanceName
                        );
                    }


                    objectRoutine.addComment("");

                }
            }
        }
    }

    @Nullable
    private TypeName createTypeFromName(String returnParameterType) {
        TypeName returnValueSquareClass;
        if (returnParameterType.startsWith("L") || returnParameterType.startsWith("[")) {

//            switch (returnParameterType) {
//                case "Ljava/lang/Integer;":
//                    return TypeName.INT;
//                break;
//                case "Ljava/lang/Long;":
//                    return TypeName.LONG;
//                break;
//                case "Ljava/lang/Short;":
//                    return TypeName.LONG;
//                break;
//                case "Ljava/lang/Char;":
//                    return TypeName.LONG;
//                break;
//                case "Ljava/lang/Long;":
//                    return TypeName.LONG;
//                break;
//                default:
                    return constructClassName(returnParameterType);
//            }

        } else if (returnParameterType.contains(".")) {
            returnValueSquareClass = ClassName.bestGuess(returnParameterType);
        } else {
            returnValueSquareClass = getClassFromDescriptor(returnParameterType);
        }
        return returnValueSquareClass;
    }

    @NotNull
    private MethodSpec.Builder buildJUnitTestCaseSkeleton(String testMethodName) {
        return MethodSpec.methodBuilder(testMethodName)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .returns(void.class)
                .addAnnotation(JUNIT_CLASS_NAME);
    }

    private ClassName constructClassName(String methodReturnValueType) {
        char firstChar = methodReturnValueType.charAt(0);
        switch (firstChar) {
            case 'V':
                return ClassName.get(void.class);
            case 'Z':
                return ClassName.get(boolean.class);
            case 'B':
                return ClassName.get(byte.class);
            case 'C':
                return ClassName.get(char.class);
            case 'S':
                return ClassName.get(short.class);
            case 'I':
                return ClassName.get(int.class);
            case 'J':
                return ClassName.get(long.class);
            case 'F':
                return ClassName.get(float.class);
            case 'D':
                return ClassName.get(double.class);
            case 'L':
                String returnValueClass = methodReturnValueType.substring(1).split(";")[0];
                return ClassName.bestGuess(returnValueClass.replace("/", "."));
            case '[':
                String returnValueClass1 = methodReturnValueType.substring(1);
                return ClassName.bestGuess(returnValueClass1.replace("/", ".") + "[]");

            default:
                assert false;

        }
        return null;
    }

    private TypeName getClassFromDescriptor(String descriptor) {
        char firstChar = descriptor.charAt(0);
        switch (firstChar) {
            case 'V':
                return TypeName.VOID;
            case 'Z':
                return TypeName.BOOLEAN;
            case 'B':
                return TypeName.BYTE;
            case 'C':
                return TypeName.CHAR;
            case 'S':
                return TypeName.SHORT;
            case 'I':
                return TypeName.INT;
            case 'J':
                return TypeName.LONG;
            case 'F':
                return TypeName.FLOAT;
            case 'D':
                return TypeName.DOUBLE;
        }
        return null;

    }

    private String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    private String lowerInstanceName(String methodName) {
        return methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText(text1);
            }
        }
    }

    public TestSuite generateTestCase(
            TestCaseRequest testCaseRequest
    ) throws APICallException, IOException, SessionNotSelectedException {

        List<ObjectWithTypeInfo> allObjects = testCaseRequest.getTargetObjectList();

        Map<Long, List<ObjectWithTypeInfo>> objectsByType =
                allObjects.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getTypeInfo().getTypeId()));

        checkProgressIndicator("Generating test cases using " + allObjects.size() + " objects", null);

        HashMap<Integer, Boolean> doneSignatures = new HashMap<>();
        List<TestCaseUnit> testCases = new LinkedList<>();
        for (Long typeId : objectsByType.keySet()) {
            List<MethodSpec> testCaseScripts = new LinkedList<>();

            List<ObjectWithTypeInfo> objectsList = objectsByType.get(typeId);
            TypeInfo typeInfo = objectsList.get(0).getTypeInfo();
            String fullClassName = typeInfo.getTypeNameFromClass();
            List<String> classNameParts = new LinkedList<>(Arrays.asList(fullClassName.split("\\.")));
            String simpleClassName = classNameParts.get(classNameParts.size() - 1);
            classNameParts.remove(classNameParts.size() - 1);
            @NotNull String packageName = StringUtil.join(classNameParts, ".");

            int i = 0;
            int total = objectsList.size();
            for (ObjectWithTypeInfo testableObject : objectsList) {
                i++;
                long objectId = testableObject.getObjectInfo().getObjectId();
                assert objectId != 0;
                checkProgressIndicator("Generating test case using history of object [ " + i +
                        "/" + total + " ]", null);

                VariableContainer variableContainer = new VariableContainer();

                Parameter objectParameter = new Parameter();
                objectParameter.setValue(objectId);

                ObjectRoutineContainer classTestSuite = generateTestCaseFromObjectHistory(
                        objectParameter, testCaseRequest, variableContainer);
                if (classTestSuite.getObjectRoutines().size() == 1) {
                    continue;
                }


                int testHash = Arrays.hashCode(classTestSuite.getStatements().toArray());


                // dropping some kind of duplicates
                if (doneSignatures.containsKey(testHash)) {
                    continue;
                }
                doneSignatures.put(testHash, true);

                ObjectRoutine constructorRoutine = classTestSuite.getConstructor();


                MethodSpec.Builder fieldInjectorMethod = MethodSpec.methodBuilder("injectField");
                fieldInjectorMethod.addModifiers(javax.lang.model.element.Modifier.PRIVATE);
                fieldInjectorMethod.addException(Exception.class);

                fieldInjectorMethod.addParameter(Object.class, "targetInstance");
                fieldInjectorMethod.addParameter(String.class, "name");
                fieldInjectorMethod.addParameter(Object.class, "targetObject");

                fieldInjectorMethod.addCode(CodeBlock.of("        Class<?> aClass = targetInstance.getClass();\n" +
                                "\n" +
                                "        while (!aClass.equals(Object.class)) {\n" +
                                "            try {\n" +
                                "                $T targetField = aClass.getDeclaredField(name);" +
                                "\n" +
                                "                targetField.setAccessible(true);\n" +
                                "                targetField.set(targetInstance, targetObject);\n" +
                                "            } catch (NoSuchFieldException nsfe) {\n" +
                                "                // nothing to set\n" +
                                "            }\n" +
                                "            aClass = aClass.getSuperclass();\n" +
                                "        }\n",
                        ClassName.bestGuess("java.lang.reflect.Field")));

                MethodSpec injectorMethod = fieldInjectorMethod.build();
                testCaseScripts.add(injectorMethod);


                for (ObjectRoutine objectRoutine : classTestSuite.getObjectRoutines()) {
                    if (objectRoutine.getRoutineName().equals("<init>")) {
                        continue;
                    }
                    if (objectRoutine.getStatements().size() == 0) {
                        continue;
                    }

                    MethodSpec.Builder builder = MethodSpec.methodBuilder(
                            "testAsInstance" + upperInstanceName(objectRoutine.getRoutineName()));

                    builder.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
                    builder.addException(Exception.class);

                    ObjectRoutineContainer e1 =
                            new ObjectRoutineContainer(List.of(constructorRoutine, objectRoutine));

                    addRoutinesToMethodBuilder(builder, List.of(e1), new LinkedList<>());


                    builder.addAnnotation(JUNIT_CLASS_NAME);

                    MethodSpec methodTestScript = builder.build();


                    testCaseScripts.add(methodTestScript);

                }


            }

//            if (testCaseScripts.size() > MAX_TEST_CASE_LINES) {
//                logger.warn("have more than " + MAX_TEST_CASE_LINES + " lines in the script, dropping some");
//                testCaseScripts = testCaseScripts.subList(0, MAX_TEST_CASE_LINES);
//            }


            checkProgressIndicator(null, "Generate java source for test scenario");
            if (simpleClassName.contains("$")) {
                simpleClassName = simpleClassName.split("\\$")[0];
            }

            String generatedTestClassName = "Test" + simpleClassName + "V";
            TypeSpec.Builder typeSpecBuilder = TypeSpec
                    .classBuilder(generatedTestClassName)
                    .addModifiers(
                            javax.lang.model.element.Modifier.PUBLIC,
                            javax.lang.model.element.Modifier.FINAL)
                    .addMethods(testCaseScripts);

            ClassName gsonClass = ClassName.get("com.google.gson", "Gson");

            typeSpecBuilder
                    .addField(FieldSpec
                            .builder(gsonClass,
                                    "gson", javax.lang.model.element.Modifier.PRIVATE)
                            .initializer("new $T()", gsonClass)
                            .build());


            TypeSpec helloWorld = typeSpecBuilder.build();

            JavaFile javaFile = JavaFile.builder(packageName, helloWorld)
                    .build();

            TestCaseUnit testCaseUnit = new TestCaseUnit(
                    javaFile.toString(), packageName, generatedTestClassName);

            testCases.add(testCaseUnit);

        }


        checkProgressIndicator(null, "Generated" + testCases.size() + " test cases");
        return new TestSuite(testCases);


    }

    private void addRoutinesToMethodBuilder(
            MethodSpec.Builder builder,
            List<ObjectRoutineContainer> dependentObjectsList, List<String> variableContainer) {
        for (ObjectRoutineContainer objectRoutineContainer : dependentObjectsList) {

            if (variableContainer.contains(objectRoutineContainer.getName())) {
                // variable has already been initialized
            } else {
                ObjectRoutine constructorRoutine = objectRoutineContainer.getConstructor();
                addRoutinesToMethodBuilder(builder, constructorRoutine.getDependentList(), variableContainer);

                for (Pair<CodeLine, Object[]> statement : constructorRoutine.getStatements()) {
                    CodeLine line = statement.getFirst();
                    if (line instanceof StatementCodeLine) {
                        builder.addStatement(line.getLine(), statement.getSecond());
                    } else {
                        String commentLine = line.getLine();
                        if (commentLine.contains("$")) {
                            commentLine = commentLine.replace('$', '_');
                        }
                        builder.addComment(commentLine, statement.getSecond());
                    }
                }
                variableContainer.add(objectRoutineContainer.getName());
            }

            for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {
                if (objectRoutine.getRoutineName().equals("<init>")) {
                    continue;
                }

                addRoutinesToMethodBuilder(builder, objectRoutine.getDependentList(), variableContainer);

                for (Pair<CodeLine, Object[]> statement : objectRoutine.getStatements()) {
                    CodeLine line = statement.getFirst();
                    if (line instanceof StatementCodeLine) {
                        logger.warn("Add statement: [" + line.getLine() + "]");
                        builder.addStatement(line.getLine(), statement.getSecond());
                    } else {
                        String line1 = line.getLine();
                        if (line1.contains("$")) {
                            line1 = line1.replace('$', '_');
                        }
                        builder.addComment(line1, statement.getSecond());
                    }

                }

            }


        }

    }

    /**
     * this is our main man in the team
     *
     * @param targetParameter         the parameter for which the routine is going to be generated
     * @param testCaseRequest         the configuration we need to generate a test case. primarily need
     *                                to hold the list of classes which we do not want to mock, but more
     *                                to come here, in terms of flavor of the test case generated, maybe
     *                                even language level things
     * @param globalVariableContainer carries the list of variables which have been identified
     *                                and created (so that we dont initialize them multiple times)
     * @return returns the routine for the object, each routine has a test case metadata and set
     * of associated test script in the form of statements
     * @throws APICallException when something fails in the data access network layer
     * @throws IOException      when something fails in the data access storage layer
     */
    private ObjectRoutineContainer
    generateTestCaseFromObjectHistory(
            Parameter targetParameter,
            TestCaseRequest testCaseRequest,
            VariableContainer globalVariableContainer
    ) throws APICallException,
            IOException, SessionNotSelectedException {
        logger.warn("[" + testCaseRequest.getBuildLevel() + "] Create test case from object: " + targetParameter +
                " -- " +
                "dependent object" +
                " ids: " + testCaseRequest.getDependentObjectList());

        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer();


        // we want to create the objects from java.lang.* namespace using their real values, so
        // in the test case it looks something like
        // Integer varName = value;
        if (targetParameter.getType() != null && targetParameter.getType().startsWith("java.lang")) {

            if (globalVariableContainer.contains(targetParameter.getName())) {
                logger.warn("variable already exists: " + targetParameter.getName());
                return objectRoutineContainer;
            }


            buildTestCandidateForBaseClass(objectRoutineContainer, targetParameter);
            return objectRoutineContainer;

        }

        if (targetParameter.getType() != null && targetParameter.getType().length() == 1) {

            if (globalVariableContainer.contains(targetParameter.getName())) {
                logger.warn("variable already exists: " + targetParameter.getName());
                return objectRoutineContainer;
            }

            buildTestCandidateForBaseClass(objectRoutineContainer, targetParameter);
            return objectRoutineContainer;


        }

        if (testCaseRequest.getBuildLevel() > 1) {
            return objectRoutineContainer;
        }

        Set<Long> dependentObjectIds = testCaseRequest.getDependentObjectList();


        PageInfo pagination = new PageInfo(0, 200, PageInfo.Order.ASC);
        pagination.setBufferSize(0);

        FilteredDataEventsRequest request = new FilteredDataEventsRequest();
        request.setPageInfo(pagination);
        request.setObjectId(Long.valueOf(String.valueOf(targetParameter.getValue())));


        ReplayData objectReplayData = client.fetchObjectHistoryByObjectId(request);


        testCaseRequest.setTargetParameter(targetParameter);
        String subjectName = buildTestCandidates(testCaseRequest, objectRoutineContainer,
                objectReplayData, testCaseRequest.getBuildLevel());

        if (subjectName != null) {
            objectRoutineContainer.setName(subjectName);
        }

        ObjectRoutine constructorRoutine = objectRoutineContainer.getConstructor();


        List<Parameter> callSubjects = objectRoutineContainer
                .getObjectRoutines().stream()
                .map(ObjectRoutine::getMetadata)
                .flatMap(Collection::stream)
                .map(TestCandidateMetadata::getCallsList)
                .flatMap(Collection::stream)
                .map(MethodCallExpression::getSubject)
                .collect(Collectors.toList());


        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {

            if (objectRoutine.getMetadata().size() == 0) {
                continue;
            }

            VariableContainer variableContainer = globalVariableContainer.clone();
            addTestSubjectToVariableContainer(objectRoutine.getMetadata(), variableContainer);
            if (objectRoutine.getRoutineName().equals("<init>")) {
//                globalVariableContainer = variableContainer;
                for (String name : variableContainer.getNames()) {
                    globalVariableContainer.add(variableContainer.getParameterByName(name));
                }
            }


            objectRoutine.setVariableContainer(variableContainer);

            // normalizing variable names by id, need to refactor to its own method
            for (String name : variableContainer.getNames()) {
                Parameter localVariable = variableContainer.getParameterByName(name);
                if (globalVariableContainer.getParameterByName(localVariable.getName()) == null) {
                    Optional<Parameter> byId = globalVariableContainer.getParametersById(localVariable.getType());
                    byId.ifPresent(value -> value.setName(localVariable.getName()));
                }
            }


            List<Parameter> dependentParameters = objectRoutine
                    .getMetadata()
                    .stream()
                    .map(TestCandidateMetadata::getMainMethod)
                    .map(MethodCallExpression::getArguments)
                    .map(VariableContainer::all)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toCollection(LinkedList::new));

            if (objectRoutine.getRoutineName().equals("<init>") && testCaseRequest.getBuildLevel() == 0) {

                for (Parameter dependentParameter : dependentParameters) {

                    ObjectRoutineContainer dependentObjectMockCreation;
                    if (dependentParameter.getType().startsWith("Ljava/")) {
                        dependentObjectMockCreation = generateTestCaseFromObjectHistory(
                                dependentParameter, TestCaseRequest.nextLevel(testCaseRequest),
                                variableContainer);
                    } else {
                        dependentObjectMockCreation = createMock(dependentParameter);
                    }

                    dependentObjectMockCreation.setName(dependentParameter.getName());
                    objectRoutine.addDependent(dependentObjectMockCreation);

                }
                continue;
            }


            for (Parameter callSubject : callSubjects) {
                ObjectRoutineContainer dependentObjectMockCreation;
                Parameter subjectVariable = globalVariableContainer.getParameterByName(callSubject.getName());
                if (subjectVariable == null) {

                    if (callSubject.getType().startsWith("java.")) {
                        dependentObjectMockCreation = generateTestCaseFromObjectHistory(
                                callSubject, TestCaseRequest.nextLevel(testCaseRequest),
                                variableContainer);
                    } else {
                        dependentObjectMockCreation = createMock(callSubject);
                    }

                    dependentObjectMockCreation.setName(callSubject.getName());
                    objectRoutine.addComment(" inject parameter " + callSubject + "]");
                    objectRoutine.addStatement("injectField($L, $S, $L)",
                            targetParameter.getName(), callSubject.getName(), callSubject.getName());
                    objectRoutine.addDependent(dependentObjectMockCreation);

                    globalVariableContainer.add(callSubject);
                }


            }


            for (Parameter callSubject : callSubjects) {
                Parameter subjectVariable = globalVariableContainer.getParameterByName(callSubject.getName());
                if (subjectVariable == null) {
                    globalVariableContainer.add(callSubject);
                }
            }


            Long parameterValue;
            if (targetParameter.getValue() instanceof Long) {
                parameterValue = (Long) targetParameter.getValue();
            } else {
                parameterValue = Long.valueOf(String.valueOf(targetParameter.getValue()));
            }
            dependentObjectIds.add(parameterValue);


            for (Parameter dependentParameter : dependentParameters) {
                if (variableContainer.contains(dependentParameter.getName())) {
                    continue;
                }

                if (dependentParameter.getName() == null) {
                    continue;
                }
                if (variableContainer.contains(dependentParameter.getName())) {
                    logger.warn("variable already exists: " + dependentParameter.getName());
                    continue;
                }

                DataEventWithSessionId parameterProbe = dependentParameter.getProb();
                dependentObjectIds.add(parameterProbe.getValue());

                ObjectRoutineContainer dependentObjectCreation =
                        generateTestCaseFromObjectHistory(
                                dependentParameter, TestCaseRequest.nextLevel(testCaseRequest),
                                variableContainer);


                variableContainer.add(dependentParameter);
                if (dependentObjectCreation.getName() != null) {
                    dependentParameter.setName(dependentObjectCreation.getName());
                } else {
                    dependentObjectCreation.setName(dependentParameter.getName());
                    for (ObjectRoutine routine : dependentObjectCreation.getObjectRoutines()) {
                        for (TestCandidateMetadata metadatum : routine.getMetadata()) {
                            if (metadatum.getTestSubject() == null) {
                                metadatum.setTestSubject(dependentParameter);
                            } else {
                                metadatum.getTestSubject().setName(dependentParameter.getName());
                            }
                        }

                    }

                }

                objectRoutine.addDependent(dependentObjectCreation);
            }
        }

        String containerJson = new ObjectMapper().writeValueAsString(objectRoutineContainer);
//        logger.warn("Routine: \n " + containerJson);

        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {
            if (objectRoutine.getMetadata().size() == 0) {
                continue;
            }
            buildTestFromTestMetadataSet(objectRoutine);
        }


        return objectRoutineContainer;

    }

    private ObjectRoutineContainer createMock(Parameter dependentParameter) {
        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer();


        buildMockCandidateForBaseClass(objectRoutineContainer, dependentParameter);
        return objectRoutineContainer;
    }

    /**
     * the method uses the parameter as a filter, and iterates thru all the events in the
     * replayData parameter. Any matching CALL or METHOD_ENTRY is a potential call and its
     * parameters and return value is captured, which serves the bases of the test candidate.
     *
     * @param testCaseRequest        the value of the parameter is required to work
     * @param objectRoutineContainer the identified method called on this parameter will be added
     *                               to the object routine container
     * @param objectReplayData       the series of events based on which we are rebuilding the object history
     * @param buildLevel             buildLevel is how deep we have gone for building dependent objects
     * @return a name for the target object (which was originally a long id),
     * @throws APICallException this happens when we fail to read the data from the disk or the
     *                          network
     */
    private String
    buildTestCandidates(
            final TestCaseRequest testCaseRequest,
            ObjectRoutineContainer objectRoutineContainer,
            ReplayData objectReplayData,
            Integer buildLevel) throws APICallException, SessionNotSelectedException {

        Parameter parameter = testCaseRequest.getTargetParameter();

        List<DataEventWithSessionId> objectEvents = objectReplayData.getDataEvents();
        if (objectEvents.size() == 0) {
            return parameter.getName();
        }

        logger.warn("build test candidate for [" + parameter.getValue() + "] using " + objectReplayData.getDataEvents().size() + " events");


        List<DataEventWithSessionId> objectEventsReverse =
                new ArrayList<>(objectReplayData.getDataEvents());
        Collections.reverse(objectEventsReverse);
        objectReplayData.setDataEvents(objectEventsReverse);


        // we need to identify a name for this object some way by using a variable name or a
        // parameter name
        String subjectName = null;

        TypeInfo subjectTypeInfo = null;
        if (parameter.getType() != null) {

            if (parameter.getType().contains("javax") ||
                    parameter.getType().contains("spring") ||
                    parameter.getType().contains("reactor") ||
                    parameter.getType().contains("mongo")) {
                return parameter.getName();
            }

            subjectTypeInfo = objectReplayData.getTypeInfoByName(parameter.getType());
        }

        final Map<String, TypeInfo> typeInfoMap = objectReplayData.getTypeInfoMap();
        final Map<String, DataInfo> probeInfoMap = objectReplayData.getProbeInfoMap();
        final Map<String, ClassInfo> classInfoMap = objectReplayData.getClassInfoMap();
        final Map<String, MethodInfo> methodInfoMap = objectReplayData.getMethodInfoMap();
        final Map<String, ObjectInfo> objectInfoMap = objectReplayData.getObjectInfoMap();

        final ObjectInfo subjectObjectInfo =
                objectInfoMap.get(String.valueOf(parameter.getValue()));
        if (subjectTypeInfo == null) {
            subjectTypeInfo =
                    typeInfoMap.get(String.valueOf(subjectObjectInfo.getTypeId()));
        }
        if (subjectTypeInfo == null) {
            return parameter.getName();
        }

//        List<String> typeNameHierarchyList = new LinkedList<>();

        List<String> typeNameHierarchyList =
                objectReplayData.buildHierarchyFromType(subjectTypeInfo);

        String className = subjectTypeInfo.getTypeNameFromClass();
//        if (!className.startsWith("org.zerhusen")) {
//            return parameter.getName();
//        }
        long currentTypeId = subjectTypeInfo.getTypeId();


        assert typeNameHierarchyList.size() != 0;


        long threadId = -1;

//        Map<Integer, Boolean> ignoredProbes = new HashMap<>();
        int totalEventCount = objectEvents.size();
        int by10 = 100;

        long nanoIndex = 0;

        for (int eventIndex = 0; eventIndex < totalEventCount; eventIndex++) {
            DataEventWithSessionId dataEvent = objectEvents.get(eventIndex);
            if (eventIndex % by10 == 0) {
                logger.warn("completed [" + eventIndex + "/" + totalEventCount + "]");
            }


            if (dataEvent.getNanoTime() < nanoIndex) {
                continue;
            }


            final long eventValue = dataEvent.getValue();
            String eventValueString = String.valueOf(eventValue);
            ObjectInfo objectInfo = objectInfoMap.get(eventValueString);

            String objectInfoString = "";
            TypeInfo objectTypeInfo = null;
            if (objectInfo != null) {
                long objectTypeId = objectInfo.getTypeId();
                objectTypeInfo = typeInfoMap.get(String.valueOf(objectTypeId));
                objectInfoString = "[Object:" + objectInfo.getObjectId() + "]";
                objectTypeInfo = typeInfoMap.get(String.valueOf(objectInfo.getTypeId()));
            }
            if (objectTypeInfo == null) {
                logger.warn("[" + eventValueString + "] object info not found: " + objectInfo);
                continue;
            }
            Set<String> objectTypeHierarchy =
                    new HashSet<>(objectReplayData.buildHierarchyFromType(objectTypeInfo));

            DataInfo probeInfo = probeInfoMap.get(String.valueOf(dataEvent.getDataId()));

            int callStack = 0;
            ClassInfo currentClassInfo = classInfoMap.get(String.valueOf(probeInfo.getClassId()));
            String ownerClassName;
            ownerClassName = ClassTypeUtils.getDescriptorName(currentClassInfo.getClassName());

            MethodInfo methodInfo = methodInfoMap.get(String.valueOf(probeInfo.getMethodId()));

            LoggerUtil.logEvent("SearchCall", callStack, eventIndex,
                    dataEvent, probeInfo, currentClassInfo, methodInfo);


            switch (probeInfo.getEventType()) {


                case METHOD_PARAM:
                case CALL:
                case CALL_PARAM:
                case LOCAL_STORE:
                case INVOKE_DYNAMIC_PARAM:
                case INVOKE_DYNAMIC_RESULT:
                case CALL_RETURN:
                case PUT_INSTANCE_FIELD:
                case GET_INSTANCE_FIELD:
                case PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION:
                case PUT_INSTANCE_FIELD_VALUE:
                case METHOD_NORMAL_EXIT:
                case NEW_OBJECT_CREATED:
                case NEW_OBJECT:
                case LOCAL_LOAD:
                case GET_INSTANCE_FIELD_RESULT:
                    continue;
                case METHOD_OBJECT_INITIALIZED:
                case METHOD_ENTRY:
                default:

                    if (StringUtil.isEmpty(ownerClassName)) {
                        logger.warn("constructorOwnerClass is empty, skipping: " + ownerClassName);
                        continue;
                    }

                    Set<String> intersectSet = new HashSet<>(objectTypeHierarchy);
                    intersectSet.retainAll(typeNameHierarchyList);

                    if (intersectSet.size() == 0) {
                        logger.warn("subject class mismatch in call, skipping: " + ownerClassName);
                        continue;
                    }

                    if (methodInfo.getMethodName().equals("<clinit>")) {
                        continue;
                    }

                    if (!methodInfo.getMethodName().equals("<init>") && buildLevel > 0) {
                        logger.info("skip method [" + methodInfo.getMethodName() + "] on build " +
                                "level [" + buildLevel + "] for class [" + ownerClassName + "]");
                        continue;
                    }

                    int methodAccess = methodInfo.getAccess();
                    if (!((methodAccess & Opcodes.ACC_PUBLIC) == 1)) {
                        continue;
                    }

                    checkProgressIndicator("Building test candidate from [ " + eventIndex + " / " + totalEventCount + " ]", null);

                    FilteredDataEventsRequest requestBefore = new FilteredDataEventsRequest();
                    requestBefore.setThreadId(dataEvent.getThreadId());
                    requestBefore.setNanotime(dataEvent.getNanoTime());
                    requestBefore.setPageInfo(new PageInfo(0, 1000, PageInfo.Order.DESC));
                    ReplayData replayEventsBefore = client.fetchObjectHistoryByObjectId(requestBefore);


                    List<DataEventWithSessionId> allEvents = replayEventsBefore.getDataEvents();

                    FilteredDataEventsRequest requestAfter = new FilteredDataEventsRequest();
                    requestAfter.setThreadId(dataEvent.getThreadId());
                    requestAfter.setNanotime(dataEvent.getNanoTime());
                    requestAfter.setPageInfo(new PageInfo(0, 1000, PageInfo.Order.ASC));
                    ReplayData replayEventsAfter = client.fetchObjectHistoryByObjectId(requestAfter);


                    replayEventsBefore.mergeReplayData(replayEventsAfter);

                    int matchedProbe = replayEventsAfter.getDataEvents().size();
                    // need to go back until we find the method entry since the following method
                    // to create test case metadata is expecting a METHOD_ENTRY probe


                    int backCallStack = 0;
                    while (matchedProbe < allEvents.size()) {
                        DataEventWithSessionId backEvent = allEvents.get(matchedProbe);
                        DataInfo backEventProbe = replayEventsBefore.getProbeInfoMap().get(
                                String.valueOf(backEvent.getDataId())
                        );
                        if (backEventProbe.getEventType() == EventType.METHOD_ENTRY && backCallStack == 0) {
                            break;
                        }

                        switch (backEventProbe.getEventType()) {
                            case METHOD_NORMAL_EXIT:
                                backCallStack++;
                                break;
                            case METHOD_ENTRY:
                                if (backCallStack > 0) {
                                    backCallStack--;
                                }
                                break;
                        }

                        // going back in time ?
                        matchedProbe++;
                    }
                    if (matchedProbe == allEvents.size()) {
                        return parameter.getName();
                    }


                    DataEventWithSessionId backEvent = allEvents.get(matchedProbe);
//                    MethodInfo backEventMethodInfo = replayEventsBefore.getMethodInfo(
//                            replayEventsBefore.getProbeInfo(backEvent.getDataId()).getMethodId()
//                    );


                    TestCandidateMetadata newTestCaseMetadata =
                            TestCandidateMetadata.create(
                                    typeNameHierarchyList, methodInfo,
                                    backEvent.getNanoTime(), replayEventsBefore,
                                    testCaseRequest);

                    Parameter testSubject = newTestCaseMetadata.getTestSubject();
                    if (testSubject == null) {
                        // whats happening here, if we were unable to pick a test subject
                        // parameter then we dont know which variable to invoke this method on.
                        // Potentially be a pagination issue also.
                        // this can also be a super() call
                        if (parameter.getValue() != null) {
                            newTestCaseMetadata.setTestSubject(parameter);
                            testSubject = parameter;
                        } else {
                            continue;
                        }


                    }
                    if (testSubject.getValue() == null ||
                            !(String.valueOf(testSubject.getValue()))
                                    .equals(String.valueOf(parameter.getValue()))
                    ) {
                        logger.warn("subject not matched: " + parameter.getValue() + " vs " + testSubject.getValue()
                                + " for method call " + methodInfo.getMethodName());
//                        ignoredProbes.put(dataEvent.getDataId(), true);
                        continue;
                    }


                    // we should find a return parameter even if the type of the return
                    // is void, return parameter marks the completion of the method
                    if (newTestCaseMetadata.getMainMethod().getReturnValue() == null) {
                        logger.warn("skipping method_entry, failed to find call return: " + newTestCaseMetadata);
                        continue;
                    }

                    logger.warn("Created test case candidate: " +
                            className + ":" + methodInfo.getMethodName());

                    if (methodInfo.getMethodName().equals("<init>")) {
                        objectRoutineContainer.getConstructor().setMetadata(newTestCaseMetadata);
                    } else {
                        if (subjectName == null) {
                            subjectName = newTestCaseMetadata.getTestSubject().getName();
                        }
                        long currentThreadId = dataEvent.getThreadId();
                        String routineName = "thread" + currentThreadId;
                        if (currentThreadId != threadId) {
                            // this is happening on a different thread
                            objectRoutineContainer.newRoutine(routineName);
                            threadId = currentThreadId;
                        }
                        logger.warn("adding new test case metadata: " + newTestCaseMetadata);
                        objectRoutineContainer.addMetadata(newTestCaseMetadata);
                        nanoIndex =
                                newTestCaseMetadata.getMainMethod().getReturnValue().getProb().getNanoTime();
                    }
                    break;

            }

        }
        return subjectName;
    }

    private void buildTestCandidateForBaseClass(ObjectRoutineContainer objectRoutineContainer, Parameter parameter) {

        String javaClassName;
        if (parameter.getType().length() > 1) {
            String[] nameParts = parameter.getType().split(";")[0].split("/");
            javaClassName = nameParts[nameParts.length - 1];
        } else {
            javaClassName = parameter.getProbeInfo().getValueDesc().name();
        }

        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();

        ClassName targetClassname = ClassName.bestGuess(javaClassName);
        testCandidateMetadata.setTestSubject(null);
        Parameter returnStringParam = new Parameter();


        testCandidateMetadata.setFullyQualifiedClassname("java.lang." + javaClassName);
        testCandidateMetadata.setPackageName("java.lang");
        testCandidateMetadata.setTestMethodName("<init>");
        testCandidateMetadata.setUnqualifiedClassname(javaClassName);

        testCandidateMetadata.setMainMethod(
                new MethodCallExpression(
                        "<init>", null, new VariableContainer(), returnStringParam, null
                )
        );

        ObjectRoutine constructor = objectRoutineContainer.getConstructor();
        if (parameter.getProb().getSerializedValue().length > 0) {
            constructor.addStatement("$T $L = $L", targetClassname, parameter.getName(),
                    new String(parameter.getProb().getSerializedValue()));
        } else {
            constructor.addStatement("$T $L = $L", targetClassname, parameter.getName(), parameter.getValue());
        }
        constructor.setMetadata(testCandidateMetadata);
    }

    private void buildMockCandidateForBaseClass(ObjectRoutineContainer objectRoutineContainer,
                                                Parameter parameter) {

        boolean isArray = false;
        String parameterTypeName = parameter.getType();
        if (parameterTypeName.startsWith("[")) {
            isArray = true;
            parameterTypeName = parameterTypeName.substring(1);
        }

        parameterTypeName = ClassTypeUtils.getDottedClassName(parameterTypeName);
        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();


        ClassName targetClassname = ClassName.bestGuess(parameterTypeName);
        testCandidateMetadata.setTestSubject(null);
        Parameter returnStringParam = new Parameter();

        testCandidateMetadata.setFullyQualifiedClassname(targetClassname.canonicalName());
        testCandidateMetadata.setPackageName(targetClassname.packageName());
        testCandidateMetadata.setIsArray(isArray);
        testCandidateMetadata.setTestMethodName("<init>");


        testCandidateMetadata.setMainMethod(
                new MethodCallExpression(
                        "<init>", null, new VariableContainer(), returnStringParam, null
                )
        );


        testCandidateMetadata.setUnqualifiedClassname(targetClassname.simpleName());
        ObjectRoutine constructor = objectRoutineContainer.getConstructor();
        constructor.addStatement("$T $L = $T.mock($T.class)", targetClassname, parameter.getName(), mockitoClass, targetClassname);
        constructor.setMetadata(testCandidateMetadata);
    }


    private void addTestSubjectToVariableContainer(
            List<TestCandidateMetadata> metadataList,
            VariableContainer variableContainer
    ) {
        for (TestCandidateMetadata testCandidateMetadata : metadataList) {

            Parameter testSubject = testCandidateMetadata.getTestSubject();

            // metadata from constructor have methodname = <init> and since they are constructors
            // we should probably not have that in the variable container, so we add it.
            if (testCandidateMetadata.getMainMethod().getMethodName().equals("<init>")) {

                if (testSubject.getValue() == null) {
                    // this is a constructor call, going directly to as a parameter
                    // something.method(new ClassHere(), ..)
                    // we will create a variable for this first and then use it
                    // we could potentially deal with it directly when generating the actual user
                    // call instead of creating a variable for every anon constructor

                    String variableName =
                            variableContainer.createVariableName(testCandidateMetadata.getUnqualifiedClassname());
                    testSubject.setName(variableName);
                }
                variableContainer.add(testSubject);
                continue;

            }

            if (variableContainer.contains(testSubject.getName())) {
                // nothing to do
            } else {

                // else we check if there is a an existing variable with the same value and if
                // yes we use that variables name as the test subject name
                // if we do not find a variable by id also, then add the test subject to the
                // variable container
                Optional<Parameter> parameterByValue
                        = variableContainer.getParametersById(String.valueOf(testSubject.getValue()));
                if (parameterByValue.isPresent()) {
                    Parameter existingParameter = parameterByValue.get();
                    if (existingParameter.getName() == null && testSubject.getName() != null) {
                        existingParameter.setName(String.valueOf(testSubject.getName()));
                    } else if (existingParameter.getName() != null && testSubject.getName() == null) {
                        testSubject.setName(existingParameter.getName());
                    } else if (!existingParameter.getName().equals(testSubject.getName())) {
                        existingParameter.setName(testSubject.getName());
                    }
                } else {
                    variableContainer.add(testSubject);
                }
            }


        }
    }

}
