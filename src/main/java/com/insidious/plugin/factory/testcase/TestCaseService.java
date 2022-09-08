package com.insidious.plugin.factory.testcase;

import com.esotericsoftware.asm.Opcodes;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.mock.MockFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutine;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.insidious.plugin.factory.testcase.writer.TestScriptWriter.in;

public class TestCaseService {
    public static final ClassName JUNIT_CLASS_NAME = ClassName.get("org.junit", "Test");
    private final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final Project project;
    private final VideobugClientInterface client;
    final private int MAX_TEST_CASE_LINES = 1000;


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
    ) throws APICallException, SessionNotSelectedException {

        List<ObjectWithTypeInfo> allObjects = testCaseRequest.getTargetObjectList();

        checkProgressIndicator("Generating test cases using " + allObjects.size() + " objects", null);

        HashMap<Integer, Boolean> doneSignatures = new HashMap<>();
        List<TestCaseUnit> testCases = new LinkedList<>();
        List<FieldSpec> fieldSpecs = new LinkedList<>();
        List<MethodSpec> methodSpecs = new LinkedList<>();

//        TypeInfo typeInfo = allObjects.get(0).getTypeInfo();

        List<ObjectWithTypeInfo> objectsList = testCaseRequest.getTargetObjectList();
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
            checkProgressIndicator("Generating test case using history of object [ " + i + "/" + total + " ]", null);


            Parameter objectParameter = new Parameter();
            objectParameter.setValue(objectId);

            testCaseRequest.setTargetParameter(objectParameter);
            ObjectRoutineContainer classTestSuite = generateTestCaseFromObjectHistory(testCaseRequest);

            if (classTestSuite.getObjectRoutines().size() == 1) {
                continue;
            }
            // part 2
            VariableContainer variableContainer = postProcessObjectRoutine(classTestSuite);

            processFields(classTestSuite, variableContainer);

            // part 3
            createDependentRoutines(testCaseRequest, variableContainer, classTestSuite);

//            for (ObjectRoutine objectRoutine : classTestSuite.getObjectRoutines()) {
//                for (TestCandidateMetadata metadatum : objectRoutine.getMetadata()) {
//                    ObjectRoutineScript objectRoutineScript =
//                            TestScriptWriter.generateTestScriptFromTestMetadataSet(metadatum, objectRoutine);
//                    objectRoutineScriptContainer.getStatements().addAll(objectRoutineScript.getStatements());
//                }
//            }


//            int testHash = Arrays.hashCode(objectRoutineScriptContainer.getStatements().toArray());


            // dropping some kind of duplicates
//            if (doneSignatures.containsKey(testHash)) {
//                continue;
//            }
//            doneSignatures.put(testHash, true);

            ObjectRoutineScriptContainer testCaseScript = writeTestCase(classTestSuite);

            methodSpecs.addAll(testCaseScript.getMethodSpecList());
            fieldSpecs.addAll(testCaseScript.toFieldSpecsList());
//            finalTestCaseScript.getMethodSpecs().addAll(testCaseScript.getMethodSpecs());
//            finalTestCaseScript.getFieldsSpecs().addAll(testCaseScript.getFieldsSpecs());


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
                .addFields(fieldSpecs)
                .addMethods(methodSpecs);

        ClassName gsonClass = ClassName.get("com.google.gson", "Gson");


        typeSpecBuilder
                .addField(FieldSpec
                        .builder(gsonClass,
                                "gson", javax.lang.model.element.Modifier.PRIVATE)
                        .initializer("new $T()", gsonClass)
                        .build());


        TypeSpec helloWorld = typeSpecBuilder.build();

        JavaFile javaFile = JavaFile.builder(packageName, helloWorld)
                .addStaticImport(ClassName.bestGuess("org.mockito.ArgumentMatchers"),
                        "any", "matches")
                .build();


        TestCaseUnit testCaseUnit = new TestCaseUnit(
                javaFile.toString(), packageName, generatedTestClassName);

        testCases.add(testCaseUnit);


        checkProgressIndicator(null, "Generated" + testCases.size() + " test cases");
        return new TestSuite(testCases);


    }

    private ObjectRoutineScriptContainer writeTestCase(
            ObjectRoutineContainer classTestSuite
    ) {


        ObjectRoutineScriptContainer routineScriptContainer = createBuilderMethod(classTestSuite);

        //        routineScriptContainer = createBuilderMethod(classTestSuite);
        //        routineScriptContainer.getObjectRoutines().add(builderMethodScript);


        MethodSpec.Builder methodTestScript = MethodSpec.methodBuilder("setup");
        routineScriptContainer.getObjectRoutines().get(0).writeToMethodSpecBuilder(methodTestScript);



        List<MethodSpec> methodSpecList = new LinkedList<>();
        List<FieldSpec> fieldSpecList = new LinkedList<>();
        methodSpecList.add(methodTestScript.build());

        for (ObjectRoutine objectRoutine : classTestSuite.getObjectRoutines()) {


            if (objectRoutine.getRoutineName().equals("<init>")) {
                continue;
            }


            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                    "testAsInstance" + ClassTypeUtils.upperInstanceName(objectRoutine.getRoutineName()));

            methodBuilder.addModifiers(Modifier.PUBLIC);
            methodBuilder.addException(Exception.class);

//            ObjectRoutineContainer container =
//                    new ObjectRoutineContainer(List.of(objectRoutine));

            ObjectRoutineScript objectScript = objectRoutine.toObjectScript(new LinkedList<>());
//            ObjectRoutineScriptContainer script =
//                    container.toRoutineScriptContainer(objectRoutine.getVariableContainer());

            objectScript.writeToMethodSpecBuilder(methodBuilder);
//            script.writeAsCode(methodBuilder);
            methodBuilder.addAnnotation(JUNIT_CLASS_NAME);
            MethodSpec testMethodSpec = methodBuilder.build();

            methodSpecList.add(testMethodSpec);


        }


        methodSpecList.add(createInjectFieldMethod());

        List<Parameter> okHttpVariables = classTestSuite.getVariablesOfType("okhttp3.");

        if (okHttpVariables.size() > 0) {
            methodSpecList.add(createOkHttpMockCreator());
        }
        return new ObjectRoutineScriptContainer(fieldSpecList, methodSpecList);
    }

    private ObjectRoutineScriptContainer createBuilderMethod(ObjectRoutineContainer classTestSuite) {
        ObjectRoutineScriptContainer container = new ObjectRoutineScriptContainer();
        ObjectRoutineScript builderMethodScript = container.getConstructor();

        ObjectRoutine constructorRoutine = classTestSuite.getConstructor();

        MethodSpec.Builder builder = MethodSpec.methodBuilder("setup");

        builder.addModifiers(Modifier.PUBLIC);
        builder.addAnnotation(ClassName.bestGuess("org.junit.Before"));
        builder.addException(Exception.class);


        Set<? extends Parameter> allFields = classTestSuite.getObjectRoutines().stream()
                .map(ObjectRoutine::getTestCandidateList)
                .flatMap(Collection::stream)
                .map(e -> e.getFields().all())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        TestCandidateMetadata firstTestMetadata = constructorRoutine.getTestCandidateList().get(0);
        MethodCallExpression mainSubjectConstructorExpression = (MethodCallExpression) firstTestMetadata.getMainMethod();
        Parameter mainSubject = mainSubjectConstructorExpression.getSubject();
        Parameter returnValue = mainSubjectConstructorExpression.getReturnValue();
        builderMethodScript.getCreatedVariables().add(mainSubject);

//
//        fieldSpecList.add(
//                FieldSpec.builder(
//                        ClassName.bestGuess(mainSubject.getType()),
//                        mainSubject.getName(), Modifier.PRIVATE
//                ).build()
//        );

        in(builderMethodScript).assignVariable(returnValue).writeExpression(mainSubjectConstructorExpression).endStatement();


        for (Parameter parameter : allFields) {
//            fieldSpecList.add(
//                    FieldSpec.builder(
//                            ClassName.bestGuess(parameter.getType()),
//                            parameter.getName(), Modifier.PRIVATE
//                    ).build()
//            );

            builderMethodScript.getCreatedVariables().add(parameter);
            in(builderMethodScript).assignVariable(parameter).writeExpression(
                    MethodCallExpressionFactory.MockClass(ClassName.bestGuess(parameter.getType()))
            ).endStatement();

            in(builderMethodScript).writeExpression(
                    new MethodCallExpression("injectField", null,
                            VariableContainer.from(List.of(
                                    mainSubject, parameter
                            )), null, null)).endStatement();

        }

        return container;
    }

    private void createDependentRoutines(
            TestCaseRequest testCaseRequest,
            VariableContainer variableContainer,
            ObjectRoutineContainer classTestSuite)
            throws APICallException, SessionNotSelectedException {

        for (ObjectRoutine objectRoutine : classTestSuite.getObjectRoutines()) {
            createDependentRoutine(testCaseRequest, variableContainer, objectRoutine);
        }

    }

    private void createDependentRoutine(
            TestCaseRequest testCaseRequest,
            VariableContainer variableContainer,
            ObjectRoutine objectRoutine
    ) throws APICallException, SessionNotSelectedException {

        if (objectRoutine.getTestCandidateList().size() == 0) {
            return;
        }
        if (objectRoutine.getRoutineName().equals("<init>")) {
            return;
        }

        @NotNull LinkedList<Parameter> dependentParameters = getDependentParameters(objectRoutine);
        for (Parameter dependentParameter : dependentParameters) {

            ObjectRoutineContainer dependentObjectCreation =
                    createDependentRoutine(testCaseRequest, variableContainer, dependentParameter);
            objectRoutine.addDependent(dependentObjectCreation);
        }

    }

    private ObjectRoutineContainer createDependentRoutine(
            TestCaseRequest testCaseRequest,
            VariableContainer variableContainer,
            Parameter dependentParameter) throws APICallException, SessionNotSelectedException {

        // we want to create the objects from java.lang.* namespace using their real values, so
        // in the test case it looks something like
        // Integer varName = value;
        ObjectRoutineContainer dependentObjectCreation;
        TestCandidateMetadata testCandidateMetadata;
        String dependentParameterType = dependentParameter.getType();

        assert dependentParameterType != null;


        if (ClassTypeUtils.IsBasicType(dependentParameterType)) {

            dependentObjectCreation = new ObjectRoutineContainer();
            testCandidateMetadata = MockFactory.createParameterMock(dependentParameter);
            dependentObjectCreation.getConstructor().addMetadata(testCandidateMetadata);

        } else {

            dependentObjectCreation =
                    generateTestCaseFromObjectHistory(TestCaseRequest.nextLevel(testCaseRequest));

            // part 2
            variableContainer = postProcessObjectRoutine(dependentObjectCreation);


            processFields(dependentObjectCreation, variableContainer);

            // part 3
            for (ObjectRoutine subObjectRoutine : dependentObjectCreation.getObjectRoutines()) {
                if (subObjectRoutine.getTestCandidateList().size() == 0) {
                    continue;
                }

                variableContainer.all().forEach(subObjectRoutine.getVariableContainer()::add);
//                                subObjectRoutine.getCreatedVariables().add();
                for (TestCandidateMetadata metadatum : subObjectRoutine.getTestCandidateList()) {

                    ObjectRoutineScript objectScript = metadatum.toObjectScript();

//                        TestScriptWriter.generateObjectRoutineFromTestMetadataSet(metadatum, subObjectRoutine);
                    VariableContainer createdVariables = subObjectRoutine.getVariableContainer();
                    createdVariables.all().forEach(variableContainer::add);
                }

            }


        }


        variableContainer.add(dependentParameter);

        for (ObjectRoutine routine : dependentObjectCreation.getObjectRoutines()) {
            VariableContainer createdVariablesContainer = routine.getVariableContainer();
            createdVariablesContainer.all().forEach(variableContainer::add);
        }


        if (dependentObjectCreation.getName() != null) {
            dependentParameter.setName(dependentObjectCreation.getName());
        } else {
            dependentObjectCreation.setName(dependentParameter.getName());
            for (ObjectRoutine routine : dependentObjectCreation.getObjectRoutines()) {
                for (TestCandidateMetadata metadatum : routine.getTestCandidateList()) {
                    if (metadatum.getTestSubject() == null) {
                        metadatum.setTestSubject(dependentParameter);
                    } else {
                        metadatum.getTestSubject().setName(dependentParameter.getName());
                    }
                }

            }

        }
        return dependentObjectCreation;

    }


    private void processFields(
            ObjectRoutineContainer objectRoutineContainer,
            VariableContainer globalVariableContainer
    ) {


        VariableContainer fields = VariableContainer.from(
                objectRoutineContainer
                        .getObjectRoutines().stream()
                        .map(ObjectRoutine::getTestCandidateList)
                        .flatMap(Collection::stream)
                        .map(TestCandidateMetadata::getFields)
                        .map(VariableContainer::all)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
        );


//        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {

        ObjectRoutine constructor = objectRoutineContainer.getConstructor();

        // gotta mock'em all
        for (Parameter fieldParameter : fields.all()) {
            Parameter subjectVariable = globalVariableContainer.getParameterByName(fieldParameter.getName());
            if (subjectVariable == null) {
                TestCandidateMetadata metadata = MockFactory.createParameterMock(fieldParameter);
                constructor.addMetadata(metadata);

                if (fieldParameter.getName() != null && objectRoutineContainer.getName() == null) {
                    objectRoutineContainer.setName(fieldParameter.getName());
                }

                globalVariableContainer.add(fieldParameter);
            }

        }


    }

    private MethodSpec createInjectFieldMethod() {
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

        return fieldInjectorMethod.build();
    }

    private MethodSpec createOkHttpMockCreator() {
        MethodSpec.Builder fieldInjectorMethod = MethodSpec.methodBuilder("buildOkHttpResponseFromString");
        fieldInjectorMethod.addModifiers(javax.lang.model.element.Modifier.PRIVATE);

        fieldInjectorMethod.addParameter(String.class, "responseBodyString");
        fieldInjectorMethod.addException(Exception.class);

        fieldInjectorMethod.addCode(CodeBlock.of("" +
                        "        $T responseBody = $T.create($T.parse" +
                        "(\"application/json\"), responseBodyString);\n" +
                        "        $T request = new Request.Builder().url(\"http://example.com\").build();\n" +
                        "        return new Response.Builder()\n" +
                        "                .request(request)\n" +
                        "                .protocol($T.HTTP_2)\n" +
                        "                .body(responseBody)\n" +
                        "                .code(200)\n" +
                        "                .message(\"message ?\")\n" +
                        "                .build();\n",
                ClassName.bestGuess("okhttp3.ResponseBody"),
                ClassName.bestGuess("okhttp3.ResponseBody"),
                ClassName.bestGuess("okhttp3.MediaType"),
                ClassName.bestGuess("okhttp3.Request"),
                ClassName.bestGuess("okhttp3.Protocol")
        ));

        fieldInjectorMethod.returns(ClassName.bestGuess("okhttp3.Response"));

        return fieldInjectorMethod.build();
    }


    /**
     * this is our main man in the team
     *
     * @param testCaseRequest the configuration we need to generate a test case. primarily need
     *                        to hold the list of classes which we do not want to mock, but more
     *                        to come here, in terms of flavor of the test case generated, maybe
     *                        even language level things
     *                        //     * @param globalVariableContainer carries the list of variables which have been identified
     *                        //     *                                and created (so that we dont initialize them multiple times)
     * @return returns the routine for the object, each routine has a test case metadata and set
     * of associated test script in the form of statements
     * @throws APICallException when something fails in the data access network layer
     */
    private ObjectRoutineContainer
    generateTestCaseFromObjectHistory(
            TestCaseRequest testCaseRequest
    ) throws APICallException, SessionNotSelectedException {
        logger.warn("[" + testCaseRequest.getBuildLevel() + "] Create test case from object: " +
                testCaseRequest.getTargetParameter() + " -- " +
                "dependent object ids: " + testCaseRequest.getDependentObjectList());


        if (testCaseRequest.getBuildLevel() > 1) {
            return null;
        }


        PageInfo pagination = new PageInfo(0, 200, PageInfo.Order.ASC);
        pagination.setBufferSize(0);

        FilteredDataEventsRequest request = new FilteredDataEventsRequest();
        request.setPageInfo(pagination);
        request.setObjectId(Long.valueOf(String.valueOf(testCaseRequest.getTargetParameter().getValue())));


        ReplayData objectReplayData = client.fetchObjectHistoryByObjectId(request);


        // this is part 1
        ObjectRoutineContainer objectRoutineContainer = buildTestCandidates(testCaseRequest, objectReplayData,
                testCaseRequest.getBuildLevel());
        // part 1 ends here

        return objectRoutineContainer;

    }

    /**
     * this needs a better name, and need to be split
     *
     * @param testCaseRequest         has the target parameter for which we are generating test case
     * @param globalVariableContainer is going to keep track of all variables from other routines
     *                                as well ?
     * @param objectRoutineContainer  this is the sink and the result will be statements in the
     *                                routine
     * @return
     * @throws APICallException
     * @throws SessionNotSelectedException
     */
    /**
     * run through all the identified variables and identify a unique name for them across
     * multiple choices
     *
     * @param objectRoutineContainer is the set of routines
     * @return globalVariableContainer set of already identified variables, might be with or
     * without names, also acts as the sink for all the identified
     * new parameters in the routines
     */
    private VariableContainer
    postProcessObjectRoutine(
            ObjectRoutineContainer objectRoutineContainer
    ) {
        VariableContainer globalVariableContainer = new VariableContainer();

        // this is part 2
//        Parameter targetParameter = testCaseRequest.getTargetParameter();
//        Set<Long> dependentObjectIds = testCaseRequest.getDependentObjectList();


        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {

            if (objectRoutine.getTestCandidateList().size() == 0) {
                continue;
            }

            VariableContainer variableContainer = globalVariableContainer.clone();
            addTestSubjectToVariableContainer(objectRoutine.getTestCandidateList(), variableContainer);
            if (objectRoutine.getRoutineName().equals("<init>")) {

                Collection<String> variableNames = variableContainer.getNames();
                for (String name : variableNames) {
                    Parameter parameterByName = variableContainer.getParameterByName(name);
                    globalVariableContainer.add(parameterByName);
                }
                variableContainer = globalVariableContainer;
            }


            objectRoutine.setVariableContainer(variableContainer);

            // normalizing variable names by id, need to refactor out this block to its own method
            variableContainer.normalize(globalVariableContainer);


            // dependent parameters need to be created before the routine can be invoked
            // need to create them, so they will be mocked or created
            List<Parameter> dependentParameters = getDependentParameters(objectRoutine);

            for (Parameter dependentParameter : dependentParameters) {

                // we want to create the objects from java.lang.* namespace using their real values, so
                // in the test case it looks something like
                // Integer varName = value;
                ObjectRoutineContainer dependentContainer = new ObjectRoutineContainer();
                TestCandidateMetadata testCandidate = MockFactory.createParameterMock(dependentParameter);
                dependentContainer.getConstructor().addMetadata(testCandidate);
                dependentContainer.setName(dependentParameter.getName());
                objectRoutine.addDependent(dependentContainer);
                variableContainer.add(dependentParameter);
            }
        }
        return globalVariableContainer;
    }


    @NotNull
    private LinkedList<Parameter> getDependentParameters(ObjectRoutine objectRoutine) {
        return objectRoutine
                .getTestCandidateList()
                .stream()
                .map(TestCandidateMetadata::getMainMethod)
                .filter(e -> e instanceof MethodCallExpression)
                .map(e -> (MethodCallExpression) e)
                .map(MethodCallExpression::getArguments)
                .map(VariableContainer::all)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * the method uses the parameter as a filter, and iterates thru all the events in the
     * replayData parameter. Any matching CALL or METHOD_ENTRY is a potential call and its
     * parameters and return value is captured, which serves the bases of the test candidate.
     *
     * @param testCaseRequest  the value of the parameter is required to work
     * @param objectReplayData the series of events based on which we are rebuilding the object history
     * @param buildLevel       buildLevel is how deep we have gone for building dependent objects
     * @return objectRoutineContainer the identified method called on this parameter will be added
     * to the object routine container
     * @throws APICallException this happens when we fail to read the data from the disk or the
     *                          network
     */
    private ObjectRoutineContainer
    buildTestCandidates(
            final TestCaseRequest testCaseRequest,
            ReplayData objectReplayData,
            Integer buildLevel
    ) throws APICallException, SessionNotSelectedException {


        Parameter parameter = testCaseRequest.getTargetParameter();


        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer();
        if (parameter.getName() != null) {
            objectRoutineContainer.setName(parameter.getName());
        }


        List<DataEventWithSessionId> objectEvents = objectReplayData.getDataEvents();
        if (objectEvents.size() == 0) {
            return objectRoutineContainer;
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
                return objectRoutineContainer;
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
//            objectRoutineContainer.setName(parameter.getName());
            return objectRoutineContainer;
        }

//        List<String> typeNameHierarchyList = new LinkedList<>();

        List<String> typeNameHierarchyList =
                objectReplayData.buildHierarchyFromType(subjectTypeInfo);

        String className = subjectTypeInfo.getTypeNameFromClass();


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

            TypeInfo objectTypeInfo = null;
            if (objectInfo != null) {
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
//                        objectRoutineContainer.setName(parameter.getName());
                        return objectRoutineContainer;
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
                        continue;
                    }


                    // we should find a return parameter even if the type of the return
                    // is void, return parameter marks the completion of the method
                    MethodCallExpression mainMethodExpression = (MethodCallExpression) newTestCaseMetadata.getMainMethod();
                    if (mainMethodExpression == null || mainMethodExpression.getReturnValue() == null) {
                        logger.warn("skipping method_entry, failed to find call return: " + newTestCaseMetadata);
                        continue;
                    }


                    // capture extra data for okhttp/libraries whose return objects we were not able to
                    // serialize and they need to be reconstructed
                    for (MethodCallExpression methodCallExpression : newTestCaseMetadata.getCallsList()) {
                        Parameter returnValue = methodCallExpression.getReturnValue();
                        if (returnValue != null && returnValue.getType() != null &&
                                returnValue.getType().length() > 1 &&
                                returnValue.getProb().getSerializedValue().length == 0) {
                            MethodCallExpression createrExpression =
                                    TestCandidateMetadata.buildObject(replayEventsBefore, returnValue);
                            returnValue.setCreator(createrExpression);
                        }
                    }


                    logger.warn("Created test case candidate: " +
                            className + ":" + methodInfo.getMethodName());

                    if (methodInfo.getMethodName().equals("<init>")) {
                        objectRoutineContainer.getConstructor().setTestCandidateList(newTestCaseMetadata);
                    } else {
                        if (subjectName == null) {
                            subjectName = newTestCaseMetadata.getTestSubject().getName();
                            objectRoutineContainer.setName(subjectName);
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
                                mainMethodExpression.getReturnValue().getProb().getNanoTime();
                    }
                    break;

            }

        }
        objectRoutineContainer.setName(subjectName);
        return objectRoutineContainer;
    }


    private void addTestSubjectToVariableContainer(
            List<TestCandidateMetadata> metadataList,
            VariableContainer variableContainer
    ) {
        for (TestCandidateMetadata testCandidateMetadata : metadataList) {

            Parameter testSubject = testCandidateMetadata.getTestSubject();

            // metadata from constructor have methodname = <init> and since they are constructors
            // we should probably not have that in the variable container, so we add it.
            Expression mainExpression = testCandidateMetadata.getMainMethod();
            if (mainExpression instanceof MethodCallExpression &&
                    ((MethodCallExpression) mainExpression).getMethodName().equals("<init>")) {

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
                Parameter existingVariableByName = variableContainer.getParameterByName(testSubject.getName());
                if (!Objects.equals(existingVariableByName.getValue(), testSubject.getValue())) {
                    if (existingVariableByName.getName() != null &&
                            !Objects.equals(existingVariableByName.getName(),
                                    testSubject.getName()))
                        testSubject.setName(existingVariableByName.getName());
                }
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
                        existingParameter.setName(testSubject.getName());
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
