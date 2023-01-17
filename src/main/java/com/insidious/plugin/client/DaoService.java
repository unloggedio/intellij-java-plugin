package com.insidious.plugin.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.plugin.Constants;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.ThreadProcessingState;
import com.insidious.plugin.pojo.dao.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.Strings;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DaoService {


    public static final String TEST_CANDIDATE_AGGREGATE_QUERY = "select p.type, p.value, count(*)\n" +
            "from test_candidate tc\n" +
            "         join parameter p on p.value = testSubject_id and p.type != 'java.lang.Object' and length(p.type) > 1\n" +
            "         join method_call mce on mce.id = tc.mainMethod_id\n" +
            "where (mce.methodAccess & 1 == 1) and mce.methodName != '<init>'\n" +
            "group by p.type, p.value\n" +
            "having count(*) > 0\n" +
            "order by p.type";
    public static final String TEST_CANDIDATE_METHOD_AGGREGATE_QUERY = "select mc.methodName, count(*)\n" +
            "from test_candidate tc\n" +
            "         join parameter p on p.value = testSubject_id\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "where p.type = ? and (mc.methodAccess & 1 == 1)\n" +
            "and mc.methodName != '<init>'\n" +
            "group by mc.methodName\n" +
            "order by mc.methodName;";


    public static final String CALLS_TO_MOCK_SELECT_QUERY_BY_PARENT = "select mc.*\n" +
            "from method_call mc\n" +
            "         left join parameter subject on subject.value == mc.subject_id\n" +
            "         left join probe_info pi on subject.probeInfo_id = pi.dataId\n" +
            "where (subject.type is null or subject.type not like 'java.lang%')\n" +
            "    and (methodAccess & 1 == 1 or methodAccess & 4 == 4)\n" +
            "    and (mc.id = ? or mc.parentId = ?)";

    public static final String CALLS_TO_MOCK_SELECT_QUERY_BY_PARENT_CHILD_CALLS = "select mc.*\n" +
            "from method_call mc\n" +
            "         left join parameter subject on subject.value == mc.subject_id\n" +
            "         left join probe_info pi on subject.probeInfo_id = pi.dataId\n" +
            "where (subject.type is null or subject.type not like 'java.lang%')\n" +
            "  and (methodAccess & 1 == 1 or methodAccess & 4 == 4)\n" +
            "  and mc.parentId in (select mc.id\n" +
            "                      from method_call mc\n" +
            "                               left join parameter subject on subject.value == mc.subject_id\n" +
            "                               left join probe_info pi on subject.probeInfo_id = pi.dataId\n" +
            "                      where (subject.type is null or subject.type not like 'java.lang%')\n" +
            "                        and ((mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
            "                              mc.subject_id = ? and mc.threadId = ?)\n" +
            "                          or (mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
            "                              mc.isStaticCall = true and mc.usesFields = true and mc.subject_id != 0 and mc.threadId = ?)))";
    public static final String TEST_CANDIDATE_BY_METHOD_SELECT = "select tc.*\n" +
            "from test_candidate tc\n" +
            "         join parameter p on p.value = testSubject_id\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "where p.type = ?\n" +
            "  and mc.methodName = ?" +
            "  and mc.methodAccess & 1 == 1\n" +
            "order by mc.methodName asc, tc.entryProbeIndex desc limit 50;";
    public static final Type LIST_STRING_TYPE = new TypeToken<ArrayList<String>>() {
    }.getType();
    public static final Type LIST_CANDIDATE_TYPE = new TypeToken<ArrayList<TestCandidateMetadata>>() {
    }.getType();
    public static final Type LIST_MCE_TYPE = new TypeToken<ArrayList<MethodCallExpression>>() {
    }.getType();
    private final static Logger logger = LoggerUtil.getInstance(DaoService.class);
    private final static Gson gson = new Gson();
    private static final String METHOD_DEFINITIONS_BY_ID_IN = "select * from method_definition where id in (IDS)";
    private final ConnectionSource connectionSource;
    private final Dao<DataEventWithSessionId, Long> dataEventDao;
    private final Dao<MethodCallExpression, Long> methodCallExpressionDao;
    private final Dao<MethodDefinition, Long> methodDefinitionsDao;
    private final Dao<ClassDefinition, Long> classDefinitionsDao;
    private final Dao<IncompleteMethodCallExpression, Long> incompleteMethodCallExpressionDao;
    private final Dao<Parameter, Long> parameterDao;
    private final Dao<LogFile, Long> logFilesDao;
    private final Dao<ArchiveFile, String> archiveFileDao;
    private final Dao<ThreadState, Integer> threadStateDao;
    private final Dao<ProbeInfo, Long> probeInfoDao;
    private final Dao<TestCandidateMetadata, Long> testCandidateDao;
    private String fieldA;

    public DaoService(ConnectionSource connectionSource) throws SQLException {
        this.connectionSource = connectionSource;

        // instantiate the DAO to handle Account with String id
        testCandidateDao = DaoManager.createDao(connectionSource, TestCandidateMetadata.class);
        probeInfoDao = DaoManager.createDao(connectionSource, ProbeInfo.class);
        parameterDao = DaoManager.createDao(connectionSource, Parameter.class);
        logFilesDao = DaoManager.createDao(connectionSource, LogFile.class);
        archiveFileDao = DaoManager.createDao(connectionSource, ArchiveFile.class);
        methodCallExpressionDao = DaoManager.createDao(connectionSource, MethodCallExpression.class);
        incompleteMethodCallExpressionDao = DaoManager.createDao(connectionSource,
                IncompleteMethodCallExpression.class);
        threadStateDao = DaoManager.createDao(connectionSource, ThreadState.class);
        dataEventDao = DaoManager.createDao(connectionSource, DataEventWithSessionId.class);
        methodDefinitionsDao = DaoManager.createDao(connectionSource, MethodDefinition.class);
        classDefinitionsDao = DaoManager.createDao(connectionSource, ClassDefinition.class);

        TableUtils.createTableIfNotExists(connectionSource, ThreadState.class);
        TableUtils.createTableIfNotExists(connectionSource, IncompleteMethodCallExpression.class);
        TableUtils.createTableIfNotExists(connectionSource, ClassDefinition.class);
        TableUtils.createTableIfNotExists(connectionSource, MethodDefinition.class);

    }


    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidateForSubjectId(Long id) throws Exception {

//        parameterDao

        List<TestCandidateMetadata> candidateList = testCandidateDao.queryForEq("testSubject_id", id);


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateList = new LinkedList<>();

        for (TestCandidateMetadata testCandidateMetadata : candidateList) {
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                    convertTestCandidateMetadata(testCandidateMetadata, true);
            com.insidious.plugin.pojo.MethodCallExpression mainMethod = (com.insidious.plugin.pojo.MethodCallExpression) converted.getMainMethod();
            if (!mainMethod.getMethodName()
                    .equals("<init>")) {
                if (!mainMethod.isMethodPublic()) {
                    continue;
                }
            }
            if (mainMethod.getReturnValue() == null) {
                continue;
            }

            testCandidateList.add(converted);
        }


        return testCandidateList;
    }

    @NotNull
    private com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata convertTestCandidateMetadata(
            TestCandidateMetadata testCandidateMetadata, Boolean loadCalls
    ) throws Exception {
//        logger.warn("Build test candidate - " + testCandidateMetadata.getEntryProbeIndex());
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                TestCandidateMetadata.toTestCandidate(testCandidateMetadata);

        converted.setTestSubject(getParameterByValue(testCandidateMetadata.getTestSubject()));

        Set<Long> calls = new HashSet<>(testCandidateMetadata.getCallsList());
        List<com.insidious.plugin.pojo.MethodCallExpression> callsList = new ArrayList<>(0);
        if (loadCalls) {
            List<com.insidious.plugin.pojo.MethodCallExpression> methodCallsFromDb =
                    getMethodCallExpressionToMockFast(testCandidateMetadata);

            // this assertion can fail because we don't actually load private calls
            // or calls on org.springframework type variables
//            assert calls.size() == methodCallsFromDb.size();
            Optional<com.insidious.plugin.pojo.MethodCallExpression> mainMethodOption = methodCallsFromDb.stream()
                    .filter(e -> e.getId() == testCandidateMetadata.getMainMethod())
                    .findFirst();
            com.insidious.plugin.pojo.MethodCallExpression mainMethod = null;
            if (mainMethodOption.isPresent()) {
                mainMethod = mainMethodOption.get();
            }

            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpressionById : methodCallsFromDb) {
                if (methodCallExpressionById.getSubject() == null ||
                        methodCallExpressionById.getSubject()
                                .getType()
                                .startsWith("java.lang")) {
                    continue;
                }
                if (mainMethod != null && methodCallExpressionById.getId() == mainMethod.getId()) {
                    continue;
                }
//            logger.warn("Add call [" + methodCallExpressionById.getMethodName() + "] - " + methodCallExpressionById);
                if (methodCallExpressionById.isMethodPublic()
                        || methodCallExpressionById.isMethodProtected()
                        || "INVOKEVIRTUAL".equals(
                        methodCallExpressionById.getEntryProbeInfo()
                                .getAttribute("Instruction", ""))
                ) {
                    callsList.add(methodCallExpressionById);
                } else {
                    logger.debug("skip call - " + methodCallExpressionById.getId());
                }
            }
            if (mainMethodOption.isPresent()) {
                converted.setMainMethod(mainMethodOption.get());
            } else {
                com.insidious.plugin.pojo.MethodCallExpression mainMethodCallExpression = getMethodCallExpressionById(
                        testCandidateMetadata.getMainMethod());
                logger.warn("main method isn't public: " + mainMethodCallExpression);
                converted.setMainMethod(mainMethodCallExpression);
            }


        } else {
            for (Long call : calls) {
                com.insidious.plugin.pojo.MethodCallExpression mce = new com.insidious.plugin.pojo.MethodCallExpression();
                mce.setId(call);
                callsList.add(mce);
            }
            converted.setMainMethod(getMethodCallExpressionById(testCandidateMetadata.getMainMethod()));
        }

        List<Long> fieldParameters = testCandidateMetadata.getFields();
//        logger.warn("\tloading " + fieldParameters.size() + " fields");
        for (Long fieldParameterValue : fieldParameters) {
            if (fieldParameterValue == 0L) {
                continue;
            }
            if (loadCalls) {
                Optional<com.insidious.plugin.pojo.MethodCallExpression> callOnField = callsList.stream()
                        .filter(e -> e.getSubject()
                                .getValue() == fieldParameterValue)
                        .findFirst();
                if (callOnField.isPresent()) {
                    converted.getFields()
                            .add(callOnField.get()
                                    .getSubject());
                    continue;
                }
            }
            com.insidious.plugin.pojo.Parameter fieldParameter = getParameterByValue(fieldParameterValue);
            converted.getFields()
                    .add(fieldParameter);
        }


        converted.setCallList(callsList);
        return converted;
    }

    private List<com.insidious.plugin.pojo.MethodCallExpression>
    getMethodCallExpressionToMockFast(TestCandidateMetadata testCandidateMetadata) {

        long start = Date.from(Instant.now())
                .getTime();
        try {
            List<MethodCallExpression> mceList = getMethodCallExpressionsInCandidate(testCandidateMetadata);
            List<Long> constructedValues = mceList.stream()
                    .filter(e -> e.getMethodName()
                            .equals("<init>"))
                    .map(MethodCallExpression::getReturnValue_id)
                    .collect(Collectors.toList());

            List<MethodCallExpressionInterface> callsToBuild = mceList.stream()
                    .filter(e -> !constructedValues.contains(e.getSubject()))
                    .filter(e -> !e.getMethodName()
                            .equals("<clinit>"))
                    .collect(Collectors.toList());

            List<com.insidious.plugin.pojo.MethodCallExpression> callsList = buildFromDbMce(callsToBuild);
            long end = Date.from(Instant.now())
                    .getTime();
//            logger.warn("Load calls took[1]: " + (end - start) + " ms");
            return callsList;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            InsidiousNotification.notifyMessage("Failed to load test candidate - " + e.getMessage(),
                    NotificationType.ERROR);
            throw new RuntimeException(e);
        }

    }

    @NotNull
    private List<MethodCallExpression> getMethodCallExpressionsInCandidate(TestCandidateMetadata testCandidateMetadata) throws Exception {
        long mainMethodId = testCandidateMetadata.getMainMethod();
        GenericRawResults<MethodCallExpression> results = methodCallExpressionDao
                .queryRaw(CALLS_TO_MOCK_SELECT_QUERY_BY_PARENT, methodCallExpressionDao.getRawRowMapper(),
                        String.valueOf(mainMethodId), String.valueOf(mainMethodId));

        List<MethodCallExpression> mceList = new ArrayList<>(results.getResults());
        results.close();
        if (mceList.size() > 0) {
            MethodCallExpression mce = mceList.get(0);

        /*
        "                        and ((mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
        "                              mc.subject_id = ? and mc.threadId = ?)\n" +
        "                          or (mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
        "                              mc.isStaticCall = true and mc.usesFields = true and mc.subject_id != 0 and mc.threadId = ?)))";
         */
            GenericRawResults<MethodCallExpression> subCalls = methodCallExpressionDao.queryRaw(
                    CALLS_TO_MOCK_SELECT_QUERY_BY_PARENT_CHILD_CALLS,
                    methodCallExpressionDao.getRawRowMapper(),
                    String.valueOf(mainMethodId), String.valueOf(testCandidateMetadata.getExitProbeIndex()),
                    String.valueOf(testCandidateMetadata.getEntryProbeIndex()),
                    String.valueOf(testCandidateMetadata.getTestSubject()),
                    String.valueOf(mce.getThreadId()),
                    String.valueOf(mainMethodId), String.valueOf(testCandidateMetadata.getExitProbeIndex()),
                    String.valueOf(testCandidateMetadata.getEntryProbeIndex()), String.valueOf(mce.getThreadId())
            );
            mceList.addAll(subCalls.getResults());
            subCalls.close();
        }
        return mceList;
    }

    @NotNull
    private com.insidious.plugin.pojo.MethodCallExpression convertIncompleteDbMCE(IncompleteMethodCallExpression dbMce) {
        com.insidious.plugin.pojo.MethodCallExpression convertedCallExpression = MethodCallExpression.ToMCE(dbMce);
        try {

            long mainSubject = dbMce.getSubject();

            // first we load the subject parameter
            convertedCallExpression.setEntryProbeInfo(this.getProbeInfoById(dbMce.getEntryProbeInfo_id()));
            convertedCallExpression.setEntryProbe(this.getDataEventById(dbMce.getEntryProbe_id()));
            if (mainSubject == 0) {
                DataInfo entryProbeInfo = convertedCallExpression.getEntryProbeInfo();
                com.insidious.plugin.pojo.Parameter staticSubject = new com.insidious.plugin.pojo.Parameter();
                staticSubject.setType(ClassTypeUtils.getDottedClassName(entryProbeInfo.getAttribute("Owner", "V")));
                staticSubject.setProb(convertedCallExpression.getEntryProbe());
                staticSubject.setProbeInfo(entryProbeInfo);
                staticSubject.setName(ClassTypeUtils.createVariableName(staticSubject.getType()));
                convertedCallExpression.setSubject(staticSubject);
            } else {
                com.insidious.plugin.pojo.Parameter subjectParam = this.getParameterByValue(mainSubject);
                convertedCallExpression.setSubject(subjectParam);
            }


            // second we load the method argument parameters
            List<Long> argumentParameters = dbMce.getArguments();
            List<Long> argumentProbes = dbMce.getArgumentProbes();
            for (int i = 0; i < argumentParameters.size(); i++) {
                Long argumentParameter = argumentParameters.get(i);
                DataEventWithSessionId dataEvent = this.getDataEventById(argumentProbes.get(i));
                DataInfo eventProbe = this.getProbeInfoById(dataEvent.getDataId());
                com.insidious.plugin.pojo.Parameter argument = this.getParameterByValue(argumentParameter);
                if (argument == null) {
                    argument = new com.insidious.plugin.pojo.Parameter(0L);
                    argument.setTypeForced(eventProbe.getValueDesc()
                            .getString());
                }
                argument.setProbeInfo(eventProbe);

                String argTypeFromProbe = eventProbe.getAttribute("Type", "V");
                if (argument.getType() == null || argument.getType()
                        .equals("") || (!argTypeFromProbe.equals("V") && !argTypeFromProbe.equals(
                        "Ljava/lang/Object;"))) {
                    argument.setTypeForced(ClassTypeUtils.getDottedClassName(argTypeFromProbe));
                }
                argument.setProb(dataEvent);
                convertedCallExpression.addArgument(argument);
                convertedCallExpression.addArgumentProbe(dataEvent);
            }

            // third and finally we load the return parameter
            if (dbMce.getReturnValue_id() != 0) {
                com.insidious.plugin.pojo.Parameter returnParam;
                returnParam = this.getParameterByValue(dbMce.getReturnValue_id());
                if (returnParam == null) {
                    returnParam = new com.insidious.plugin.pojo.Parameter();
                }
                convertedCallExpression.setReturnValue(returnParam);

                DataEventWithSessionId returnDataEvent = this.getDataEventById(dbMce.getReturnDataEvent());
                returnParam.setProb(returnDataEvent);
                DataInfo eventProbe = this.getProbeInfoById(returnDataEvent.getDataId());

                String returnParamType = returnParam.getType();
                if ((returnParamType == null || returnParamType == "" || returnParam.isPrimitiveType()) && eventProbe.getValueDesc() != Descriptor.Object && eventProbe.getValueDesc() != Descriptor.Void) {
                    returnParam.setTypeForced(ClassTypeUtils.getJavaClassName(eventProbe.getValueDesc()
                            .getString()));
                }

                returnParam.setProbeInfo(eventProbe);
                String typeFromProbe = eventProbe.getAttribute("Type", null);
                if (returnParam.getType() == null || returnParam.getType()
                        .equals("") || (typeFromProbe != null && !typeFromProbe.equals("Ljava/lang/Object;"))) {
                    returnParam.setTypeForced(ClassTypeUtils.getDottedClassName(typeFromProbe));
                }

                if (returnParam.getType() != null && returnDataEvent.getSerializedValue().length == 0) {
                    switch (returnParam.getType()) {
                        case "okhttp3.Response":
                            List<com.insidious.plugin.pojo.MethodCallExpression> callsOnReturnParameter =
                                    this.getMethodCallExpressionOnParameter(returnParam.getValue());

                            Optional<com.insidious.plugin.pojo.MethodCallExpression> bodyResponseParameter
                                    = callsOnReturnParameter
                                    .stream()
                                    .filter(e -> e.getMethodName()
                                            .equals("body"))
                                    .findFirst();

                            if (bodyResponseParameter.isEmpty()) {
                                throw new RuntimeException("expecting a body call on the " +
                                        "return parameter okhttp3.Response was not found");
                            }
                            com.insidious.plugin.pojo.MethodCallExpression bodyParameter =
                                    bodyResponseParameter.get();

                            // we need the return value on this return parameter which is going to be the actual body
                            // since the ResponseBody is also not serializable
                            List<com.insidious.plugin.pojo.MethodCallExpression> responseBodyCalls =
                                    this.getMethodCallExpressionOnParameter(bodyParameter.getReturnValue()
                                            .getValue());
                            if (responseBodyCalls.size() == 0 || !responseBodyCalls.get(0)
                                    .getMethodName()
                                    .equals("string")) {
                                // we wanted the return value from the "string" call on ResponseBody
                                // but, we did not find that method call, so we cannot reconstruct the response from the
                                // http call, so just throw for now until we come across a real scenario where
                                // this is happening
                                throw new RuntimeException("expected 'string' call on the ResponseBody " +
                                        "object was not found - " + convertedCallExpression);
                            }

                            com.insidious.plugin.pojo.MethodCallExpression stringCall = responseBodyCalls.get(0);

                            VariableContainer variableContainer = VariableContainer.from(
                                    List.of(stringCall.getReturnValue())
                            );

                            // TODO: also use header and code method call response to create more accurate response

                            com.insidious.plugin.pojo.MethodCallExpression buildOkHttpResponseFromString =
                                    MethodCallExpressionFactory.MethodCallExpression("buildOkHttpResponseFromString",
                                            null, variableContainer, returnParam);

                            returnParam.setCreator(buildOkHttpResponseFromString);
                            break;
                        default:
                            // now we can end up in this call recursively
                            // so instead of throwing, lets return ?
                            break;
//                            throw new RuntimeException("return value serialized value is empty - " + convertedCallExpression);
                    }
                }

                convertedCallExpression.setReturnDataEvent(returnDataEvent);
            } else {
                // nothing to do for the return value
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return convertedCallExpression;
    }


    @NotNull
    private List<com.insidious.plugin.pojo.MethodCallExpression> buildFromDbMce(
            List<MethodCallExpressionInterface> mceList
    ) throws Exception {
        Map<Long, MethodCallExpressionInterface> dbMceMap = mceList.stream()
                .collect(Collectors.toMap(MethodCallExpressionInterface::getId, e -> e));
        List<com.insidious.plugin.pojo.MethodCallExpression> callsList =
                mceList.parallelStream()
                        .map(MethodCallExpression::ToMCE)
                        .collect(Collectors.toList());

//            List<com.insidious.plugin.pojo.MethodCallExpression> callsList1 =
//                    mceList.parallelStream().map(this::convertDbMCE).collect(Collectors.toList());

        Set<Long> probesToLoad = new HashSet<>();
        Set<Integer> probeInfoToLoad = new HashSet<>();
        Set<Long> parametersToLoad = new HashSet<>();

        Set<Integer> methodDefinitionIds = mceList.stream()
                .map(MethodCallExpressionInterface::getMethodDefinitionId)
                .collect(Collectors.toSet());

        GenericRawResults<MethodDefinition> methodDefinitionsResultSet = methodDefinitionsDao.queryRaw(
                METHOD_DEFINITIONS_BY_ID_IN.replace("IDS", Strings.join(methodDefinitionIds
                        , ", ")),
                methodDefinitionsDao.getRawRowMapper());
        List<MethodDefinition> methodDefinitionList = methodDefinitionsResultSet.getResults();

        methodDefinitionsResultSet.close();
        Map<Long, MethodDefinition> methodDefinitionMap = methodDefinitionList.stream()
                .collect(Collectors.toMap(MethodDefinition::getId, e -> e));

        for (MethodCallExpressionInterface methodCallExpression : mceList) {

            parametersToLoad.add(methodCallExpression.getSubject());
            parametersToLoad.addAll(methodCallExpression.getArguments());
            parametersToLoad.add(methodCallExpression.getReturnValue_id());

            probesToLoad.add(methodCallExpression.getEntryProbe_id());
            probesToLoad.addAll(methodCallExpression.getArgumentProbes());
            probesToLoad.add(methodCallExpression.getReturnDataEvent());

            probeInfoToLoad.add(methodCallExpression.getEntryProbeInfo_id());
        }


        List<Parameter> dbParameters = getParameterByValue(parametersToLoad);

        for (Parameter parameter : dbParameters) {
            probesToLoad.add(parameter.getProb_id());
            probeInfoToLoad.add(parameter.getProbeInfo_id());
        }

        List<DataEventWithSessionId> allProbes = getProbes(probesToLoad);
        for (DataEventWithSessionId allProbe : allProbes) {
            probeInfoToLoad.add((int) allProbe.getDataId());
        }


        List<DataInfo> allProbesInfo = getProbesInfo(probeInfoToLoad);

        Map<Long, DataEventWithSessionId> probesMap = allProbes.stream()
                .collect(Collectors.toMap(DataEventWithSessionId::getNanoTime, e -> e));

        Map<Integer, DataInfo> probeInfoMap = allProbesInfo.stream()
                .collect(Collectors.toMap(DataInfo::getDataId, e -> e));

        Map<Object, Parameter> dbParameterMap = dbParameters.stream()
                .collect(Collectors.toMap(Parameter::getValue, e -> e));

        List<com.insidious.plugin.pojo.Parameter> parameters = dbParameters.stream()
                .map(Parameter::toParameter)
                .collect(Collectors.toList());

        for (com.insidious.plugin.pojo.Parameter parameter : parameters) {
            Parameter dbParameter = dbParameterMap.get(parameter.getValue());
            parameter.setProb(probesMap.get(dbParameter.getProb_id()));
            parameter.setProbeInfo(probeInfoMap.get(dbParameter.getProbeInfo_id()));
        }

        Map<Long, com.insidious.plugin.pojo.Parameter> parameterMap = parameters.stream()
                .collect(Collectors.toMap(com.insidious.plugin.pojo.Parameter::getValue, e -> e));

        List<com.insidious.plugin.pojo.MethodCallExpression> finalCallsList = new ArrayList<>();
        for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsList) {

            MethodCallExpressionInterface dbMce = dbMceMap.get(methodCallExpression.getId());

            MethodDefinition methodDefinition = methodDefinitionMap.get(
                    (long) methodCallExpression.getMethodDefinitionId());


            if (methodCallExpression.isStaticCall() || dbMce.getSubject() == 0) {
                com.insidious.plugin.pojo.Parameter staticSubject = com.insidious.plugin.pojo.Parameter.cloneParameter(
                        parameterMap.get(dbMce.getSubject()));
                staticSubject.setName(ClassTypeUtils.createVariableName(staticSubject.getType()));
                methodCallExpression.setSubject(staticSubject);
            } else {
                methodCallExpression.setSubject(
                        com.insidious.plugin.pojo.Parameter.cloneParameter(parameterMap.get(dbMce.getSubject()))
                );
            }

            com.insidious.plugin.pojo.Parameter subject = methodCallExpression.getSubject();
            finalCallsList.add(methodCallExpression);


            List<com.insidious.plugin.pojo.Parameter> arguments = new LinkedList<>();

            List<Long> dbMceArguments = dbMce.getArguments();
            String[] argumentTypesFromMethodDefinition = new String[0];
            if (methodDefinition != null) {
                argumentTypesFromMethodDefinition = methodDefinition.getArgumentTypes()
                        .split(",");
            }

            for (int i = 0; i < dbMceArguments.size(); i++) {
                String argumentType = null;
                if (methodDefinition != null && methodDefinition.getId() != 0) {
                    argumentType = argumentTypesFromMethodDefinition[i];
                }
                Long argument = dbMceArguments.get(i);
                DataEventWithSessionId dataEvent = probesMap.get(dbMce.getArgumentProbes()
                        .get(i));
                DataInfo probeInfo = probeInfoMap.get((int) dataEvent.getDataId());
                com.insidious.plugin.pojo.Parameter paramArgument = parameterMap.get(argument);
                if (paramArgument == null) {
                    paramArgument = new com.insidious.plugin.pojo.Parameter(0L);
                } else {
                    paramArgument = com.insidious.plugin.pojo.Parameter.cloneParameter(paramArgument);
                }
                paramArgument.setProbeInfo(probeInfo);

                String paramArgTypeFromProbe = probeInfo.getAttribute("Type", probeInfo.getValueDesc()
                        .getString());
                // only set param type if the type is not already null or empty
                String existingType = paramArgument.getType();
                if ((existingType == null || existingType.equals("") || existingType.length() == 1)
                        && (!paramArgTypeFromProbe.equals("V")
                        && !paramArgTypeFromProbe.equals("Ljava/lang/Object;"))) {
                    paramArgument.setTypeForced(
                            ClassTypeUtils.getDottedClassName(paramArgTypeFromProbe));
                }

                paramArgument.setProb(dataEvent);

                arguments.add(paramArgument);
            }

            methodCallExpression.setArguments(arguments);
            List<DataEventWithSessionId> argumentProbes = new LinkedList<>();
            for (Long argumentProbe : dbMce.getArgumentProbes()) {
                argumentProbes.add(probesMap.get(argumentProbe));
            }

            methodCallExpression.setArgumentProbes(argumentProbes);


            com.insidious.plugin.pojo.Parameter returnParam;
            returnParam = parameterMap.get(dbMce.getReturnValue_id());
            if (returnParam == null) {
                returnParam = new com.insidious.plugin.pojo.Parameter();
            } else {
                returnParam = com.insidious.plugin.pojo.Parameter.cloneParameter(returnParam);
            }
            methodCallExpression.setReturnValue(returnParam);

            DataEventWithSessionId returnDataEvent = probesMap.get(dbMce.getReturnDataEvent());
            returnParam.setProb(returnDataEvent);
            DataInfo eventProbe = probeInfoMap.get((int) returnDataEvent.getDataId());

            String returnParamType = returnParam.getType();
            if ((returnParamType == null || returnParamType.equals(
                    "") || returnParam.isPrimitiveType()) && eventProbe.getValueDesc() != Descriptor.Object && eventProbe.getValueDesc() != Descriptor.Void) {
                returnParam.setTypeForced(ClassTypeUtils.getJavaClassName(eventProbe.getValueDesc()
                        .getString()));
            }
            returnParam.setProbeInfo(eventProbe);
            String typeFromProbe = ClassTypeUtils.getDottedClassName(eventProbe.getAttribute("Type", null));
            if (typeFromProbe != null && !typeFromProbe.equals("java.lang.Object")) {
                returnParam.setTypeForced(typeFromProbe);
            }

            if (returnParam.getType() != null && returnDataEvent.getSerializedValue().length == 0) {
                switch (returnParam.getType()) {
                    case "okhttp3.Response":
                        List<com.insidious.plugin.pojo.MethodCallExpression> callsOnReturnParameter =
                                this.getMethodCallExpressionOnParameter(returnParam.getValue());

                        Optional<com.insidious.plugin.pojo.MethodCallExpression> bodyResponseParameter
                                = callsOnReturnParameter
                                .stream()
                                .filter(e -> e.getMethodName()
                                        .equals("body"))
                                .findFirst();

                        if (bodyResponseParameter.isEmpty()) {
                            throw new RuntimeException("expecting a body call on the " +
                                    "return parameter okhttp3.Response was not found");
                        }
                        com.insidious.plugin.pojo.MethodCallExpression bodyParameter =
                                bodyResponseParameter.get();

                        // we need the return value on this return parameter which is going to be the actual body
                        // since the ResponseBody is also not serializable
                        List<com.insidious.plugin.pojo.MethodCallExpression> responseBodyCalls =
                                this.getMethodCallExpressionOnParameter(
                                        bodyParameter.getReturnValue()
                                                .getValue());
                        if (responseBodyCalls.size() == 0 || !responseBodyCalls.get(0)
                                .getMethodName()
                                .equals("string")) {
                            // we wanted the return value from the "string" call on ResponseBody
                            // but, we did not find that method call, so we cannot reconstruct the response from the
                            // http call, so just throw for now until we come across a real scenario where
                            // this is happening
                            throw new RuntimeException("expected 'string' call on the ResponseBody " +
                                    "object was not found - " + methodCallExpression);
                        }

                        com.insidious.plugin.pojo.MethodCallExpression stringCall = responseBodyCalls.get(0);

                        VariableContainer variableContainer = VariableContainer.from(
                                List.of(stringCall.getReturnValue())
                        );

                        // TODO: also use header and code method call response to create more accurate response

                        com.insidious.plugin.pojo.MethodCallExpression buildOkHttpResponseFromString =
                                MethodCallExpressionFactory.MethodCallExpression(
                                        "buildOkHttpResponseFromString",
                                        null, variableContainer, returnParam);

                        returnParam.setCreator(buildOkHttpResponseFromString);
                        break;
                    default:
                        // now we can end up in this call recursively
                        // so instead of throwing, lets return ?
                        break;
//                            throw new RuntimeException("return value serialized value is empty - " + convertedCallExpression);
                }
            }

            methodCallExpression.setReturnDataEvent(returnDataEvent);

            methodCallExpression.setEntryProbe(probesMap.get(dbMce.getEntryProbe_id()));
            methodCallExpression.setEntryProbeInfo(probeInfoMap.get(dbMce.getEntryProbeInfo_id()));
        }
        return finalCallsList;
    }


    public List<Parameter>
    getParameterByValue(Collection<Long> values) throws Exception {
        if (values.size() == 0) {
            return List.of();
        }

        String query = "select * from parameter where value in (" + StringUtils.join(values, ",") + ")";

        GenericRawResults<Parameter> queryResult = parameterDao.queryRaw(query, parameterDao.getRawRowMapper());
        List<Parameter> resultList = queryResult.getResults();
        queryResult.close();
        if (resultList.size() == 0) {
            return List.of();
        }

        return new ArrayList<>(resultList);
    }

    public List<DataEventWithSessionId>
    getProbes(Collection<Long> values) throws Exception {
        if (values.size() == 0) {
            return List.of();
        }

        String query = "select * from data_event where nanotime in (" + StringUtils.join(values, ",") + ")";

        GenericRawResults<DataEventWithSessionId> queryResult = dataEventDao.queryRaw(query,
                dataEventDao.getRawRowMapper());
        List<DataEventWithSessionId> resultList = queryResult.getResults();
        queryResult.close();
        if (resultList.size() == 0) {
            return List.of();
        }

        return new ArrayList<>(resultList);
    }

    public List<DataInfo>
    getProbesInfo(Collection<Integer> values) throws Exception {
        if (values.size() == 0) {
            return List.of();
        }

        String query = "select * from probe_info where dataid in (" + StringUtils.join(values, ",") + ")";

        GenericRawResults<ProbeInfo> queryResult = probeInfoDao.queryRaw(query, probeInfoDao.getRawRowMapper());
        List<DataInfo> resultList = queryResult.getResults()
                .stream()
                .map(ProbeInfo::ToProbeInfo)
                .collect(Collectors.toList());
        queryResult.close();
        return new ArrayList<>(resultList);
    }


    public com.insidious.plugin.pojo.MethodCallExpression getMethodCallExpressionById(Long methodCallId) throws Exception {
        MethodCallExpression dbMce = null;
        try {
            dbMce = methodCallExpressionDao.queryForId(methodCallId);
            if (dbMce == null) {
                IncompleteMethodCallExpression incompleteDbMce = incompleteMethodCallExpressionDao.queryForId(
                        methodCallId);
                return convertIncompleteDbMCE(incompleteDbMce);
//                return buildFromDbMce(List.of(incompleteDbMce)).get(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return buildFromDbMce(List.of(dbMce)).get(0);
    }

    private List<com.insidious.plugin.pojo.MethodCallExpression>
    getMethodCallExpressionOnParameter(long subjectId) {
        try {
            List<MethodCallExpression> callListFromDb = methodCallExpressionDao.queryForEq("subject_id", subjectId);

            List<MethodCallExpressionInterface> callsToLoad = new LinkedList<>();
            Set<String> loadedMethods = new HashSet<>();
            for (MethodCallExpression methodCallExpression : callListFromDb) {
                if (loadedMethods.contains(methodCallExpression.getMethodName())) {
                    // we want to load calls on the parameter only once
                    // since we are expecting all calls to return the same value
                    continue;
                }
                loadedMethods.add(methodCallExpression.getMethodName());
                callsToLoad.add(methodCallExpression);
            }

            return buildFromDbMce(callsToLoad);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public com.insidious.plugin.pojo.Parameter getParameterByValue(Long value) throws SQLException {
        if (value == 0) {
            return null;
        }
        List<Parameter> parameterList = parameterDao.queryForEq("value", value);
        if (parameterList.size() == 0) {
            return null;
        }
        Parameter parameter = parameterList.get(0);
        com.insidious.plugin.pojo.Parameter convertedParameter = Parameter.toParameter(parameter);

        DataEventWithSessionId dataEvent = this.getDataEventById(parameter.getProb_id());
        if (dataEvent != null) {
            DataInfo probeInfo = getProbeInfoById(parameter.getProbeInfo_id());
            convertedParameter.setProbeInfo(probeInfo);
            convertedParameter.setProb(dataEvent);
        }

        return convertedParameter;
    }

    private DataInfo getProbeInfoById(long dataId) throws SQLException {
        ProbeInfo dataInfo = probeInfoDao.queryForId(dataId);
        return ProbeInfo.ToProbeInfo(dataInfo);
    }

    private DataEventWithSessionId getDataEventById(Long id) throws SQLException {
        return dataEventDao.queryForId(id);
    }

    public void createOrUpdateDataEvent(Collection<DataEventWithSessionId> dataEvent) {
        try {
            dataEventDao.create(dataEvent);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<Integer> getProbes() throws SQLException {
        return probeInfoDao.queryBuilder()
                .selectColumns("dataId")
                .query()
                .stream()
                .map(ProbeInfo::getDataId)
                .collect(Collectors.toList());
    }

    public void createOrUpdateCall(Collection<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        long start = new Date().getTime();
        try {
//            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsToSave) {
////                logger.warn("Save MCE: " + methodCallExpression.getId());
//                methodCallExpressionDao.create(MethodCallExpression.FromMCE(methodCallExpression));
//
//            }

            methodCallExpressionDao.create(
                    callsToSave
                            .stream()
                            .map(MethodCallExpression::FromMCE)
                            .collect(Collectors.toList())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            long end = new Date().getTime();
            logger.warn("saving " + callsToSave.size() + " calls took: " + ((end - start) / 1000) + " sec");
        }
    }

    public void createOrUpdateIncompleteCall(Collection<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            List<IncompleteMethodCallExpression> items = callsToSave
                    .stream()
                    .map(MethodCallExpression::IncompleteFromMCE)
                    .collect(Collectors.toList());
            for (IncompleteMethodCallExpression item : items) {
                incompleteMethodCallExpressionDao.createOrUpdate(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCalls(Collection<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsToSave) {
                methodCallExpressionDao.update(MethodCallExpression.FromMCE(methodCallExpression));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateTestCandidate(
            Collection<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> candidatesToSave) {
        try {
            TestCandidateMetadata toSave;
            for (com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata : candidatesToSave) {
                toSave = TestCandidateMetadata.FromTestCandidateMetadata(testCandidateMetadata);
                testCandidateDao.create(toSave);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateProbeInfo(Collection<DataInfo> probeInfo) {
        try {
            probeInfoDao.create(probeInfo.stream()
                    .map(ProbeInfo::FromProbeInfo)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void createOrUpdateParameter(Collection<com.insidious.plugin.pojo.Parameter> parameterList) {
        try {
            int i = 0;
            int total = parameterList.size();
            Parameter newParam = new Parameter();
            long start = new Date().getTime();


            List<Parameter> daoParamList = parameterList.stream()
                    .map(Parameter::fromParameter)
                    .collect(Collectors.toList());
            try {
                parameterDao.executeRaw("DELETE FROM parameter");
            } catch (Exception e) {
                logger.warn("Failed to truncate parameter table: " + e.getMessage(), e);
                return;
            }
            int createdCount = parameterDao.create(daoParamList);
//            for (com.insidious.plugin.pojo.Parameter parameter : parameterList) {
//                if (i % 100 == 0) {
//                    checkProgressIndicator(null, i + " / " + total);
//                }
//                i++;
//                newParam.setContainer(parameter.isContainer());
//                newParam.setException(parameter.isException());
//                newParam.setProb_id(parameter.getProb());
//                newParam.setType(parameter.getType());
//                List<com.insidious.plugin.pojo.Parameter> templateMap1 = parameter.getTemplateMap();
//                List<Parameter> transformedTemplateMap = new ArrayList<>();
//                for (com.insidious.plugin.pojo.Parameter param : templateMap1) {
//                    transformedTemplateMap.add(Parameter.fromParameter(param));
//                }
//                newParam.setNames(parameter.getNames());
//                newParam.setTemplateMap(transformedTemplateMap);
//                newParam.setProbeInfo_id(parameter.getProbeInfo());
//                newParam.setValue(parameter.getValue());
//
//                parameterDao.createOrUpdate(newParam);
//            }
            long end = new Date().getTime();
            long timeInSeconds = (end - start) / 1000;
            if (timeInSeconds == 0) {
                timeInSeconds = 1;
            }
            logger.warn("updated " + parameterList.size() + " parameters took [" + timeInSeconds + "] seconds" +
                    " => " + parameterList.size() / timeInSeconds + " params/second");
        } catch (Exception e) {
            logger.error("failed to save parameters: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public List<com.insidious.plugin.pojo.Parameter> getParametersByType(String typeName) {
        try {
            return parameterDao.queryForEq("type", typeName)
                    .stream()
                    .map(Parameter::toParameter)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public long getMaxCallId() {
        try {
            long result = methodCallExpressionDao.queryRawValue("select max(id) from method_call");
            long result2 = methodCallExpressionDao.queryRawValue("select max(id) from incomplete_method_call");
            return Math.max(result, result2);
        } catch (SQLException e) {
            return 0;
        }
    }

    public void close() throws Exception {
        connectionSource.close();
    }

    public List<String> getPackageNames() {
        List<String> packageList = new LinkedList<>();

        try {
            GenericRawResults<Object[]> parameterIdList = parameterDao
                    .queryRaw("select distinct(type) from parameter order by type;", new DataType[]{DataType.STRING});

            for (Object[] objects : parameterIdList) {
                String className = (String) objects[0];
                packageList.add(className);
            }
            parameterIdList.close();

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }

        return packageList;
    }

    public List<LogFile> getLogFiles() {
        try {
            return logFilesDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<ArchiveFile> getArchiveList() {
        try {
            return archiveFileDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<ThreadProcessingState> getThreadList() {
        try {
            return threadStateDao.queryForAll()
                    .stream()
                    .map(e -> new ThreadProcessingState(1))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void updateArchiveFile(ArchiveFile archiveFile) {
        try {
            archiveFileDao.createOrUpdate(archiveFile);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateLogFile(LogFile logFile) {
        try {
            logFilesDao.createOrUpdate(logFile);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidatesForClass(String className) {
        try {
            long result = parameterDao.queryRawValue("select value from parameter where type = '" + className + "'");

            return getTestCandidateForSubjectId(result);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<VideobugTreeClassAggregateNode> getTestCandidateAggregates() {
        try {
            List<VideobugTreeClassAggregateNode> aggregateList = new LinkedList<>();
            Map<String, Integer> packageCountAggregate = new HashMap<>();

            GenericRawResults<String[]> rows = testCandidateDao.queryRaw(TEST_CANDIDATE_AGGREGATE_QUERY);
            for (String[] result : rows.getResults()) {
                Integer value = packageCountAggregate.computeIfAbsent(result[0], s -> 0);
                packageCountAggregate.put(result[0], value + Integer.parseInt(result[2]));
            }
            for (String s : packageCountAggregate.keySet()) {
                aggregateList.add(new VideobugTreeClassAggregateNode(s, packageCountAggregate.get(s)));
            }
            rows.close();
            return aggregateList;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<TestCandidateMethodAggregate> getTestCandidateAggregatesForType(String typeName) {

        try {
            List<TestCandidateMethodAggregate> results = new LinkedList<>();
            GenericRawResults<String[]> rows = testCandidateDao.queryRaw(TEST_CANDIDATE_METHOD_AGGREGATE_QUERY,
                    typeName);
            for (String[] result : rows.getResults()) {
                results.add(
                        new TestCandidateMethodAggregate(typeName, result[0], Integer.valueOf(result[1]))
                );
            }

            rows.close();

            return results;


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidatesForMethod(String className, String methodName, boolean loadCalls) {

        try {

            GenericRawResults<TestCandidateMetadata> parameterIds = parameterDao
                    .queryRaw(TEST_CANDIDATE_BY_METHOD_SELECT, testCandidateDao.getRawRowMapper(), className,
                            methodName);

            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> resultList = new LinkedList<>();

            List<TestCandidateMetadata> testCandidates = parameterIds.getResults();
            for (TestCandidateMetadata testCandidate : testCandidates) {
                com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                        convertTestCandidateMetadata(testCandidate, loadCalls);
                resultList.add(converted);
            }

            parameterIds.close();
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    getConstructorCandidate(com.insidious.plugin.pojo.Parameter parameter) throws Exception {

        List<MethodCallExpression> callListFromDb =
                methodCallExpressionDao.queryBuilder()
                        .where()
                        .eq("subject_id", parameter.getValue())
                        .and()
                        .eq("methodName", "<init>")
                        .query();

        if (callListFromDb.size() > 1) {
            logger.warn("found more than 1 constructor for subject: " + parameter.getValue());
        }
        // for static classes we wont have a constructor call captured
        if (callListFromDb.size() == 0) {

            List<Parameter> parametersOfSameType = parameterDao.queryForEq("type", parameter.getType());
            if (parametersOfSameType.size() > 1) {

                List<Parameter> theOtherParameter = parametersOfSameType.stream()
                        .filter(e -> e.getValue() != parameter.getValue())
                        .collect(Collectors.toList());
                // for the case where we have exactly one other parameter of this type on which the constructor was
                // called and then spring hijacked it ?
                if (theOtherParameter.size() == 1) {
                    Parameter thatOtherParameter = theOtherParameter.get(0);
                    List<MethodCallExpression> theCallOnOtherParam = methodCallExpressionDao.queryBuilder()
                            .where()
                            .eq("subject_id", thatOtherParameter.getValue())
                            .and()
                            .eq("methodName", "<init>")
                            .query();
                    if (theCallOnOtherParam.size() == 1 && theCallOnOtherParam.get(0)
                            .getMethodName()
                            .equals("<init>")) {
                        MethodCallExpression theRealInitCall = theCallOnOtherParam.get(0);
                        TestCandidateMetadata testCandidate = new TestCandidateMetadata();

                        testCandidate.setTestSubject(parameter.getValue());
                        testCandidate.setExitProbeIndex(theRealInitCall.getReturnDataEvent());
                        testCandidate.setEntryProbeIndex(theRealInitCall.getEntryProbe_id());
                        testCandidate.setMainMethod(theRealInitCall.getId());
                        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata constructorCandidate
                                = convertTestCandidateMetadata(testCandidate, true);
                        constructorCandidate.setTestSubject(parameter);
                        ((com.insidious.plugin.pojo.MethodCallExpression) constructorCandidate.getMainMethod()).setSubject(parameter);
                        ((com.insidious.plugin.pojo.MethodCallExpression) constructorCandidate.getMainMethod()).setReturnValue(parameter);
                        return constructorCandidate;
                    }
                }

            }

            return null;
        }
        MethodCallExpression constructorMethodExpression = callListFromDb.get(0);

        TestCandidateMetadata testCandidate = new TestCandidateMetadata();

        testCandidate.setTestSubject(parameter.getValue());
        testCandidate.setExitProbeIndex(constructorMethodExpression.getReturnDataEvent());
        testCandidate.setEntryProbeIndex(constructorMethodExpression.getEntryProbe_id());
        testCandidate.setMainMethod(constructorMethodExpression.getId());

        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata constructorCandidate
                = convertTestCandidateMetadata(testCandidate, true);


        return constructorCandidate;


    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidates(long value, long entryProbeIndex, long mainMethodId, boolean loadCalls) {
        try {
            List<TestCandidateMetadata> candidates = testCandidateDao.queryBuilder()
                    .where()
                    .eq("testSubject_id", value)
                    .and()
                    .le("entryProbeIndex", entryProbeIndex)
                    .and()
                    .le("mainMethod_id", mainMethodId)
                    .query();
            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> results = new LinkedList<>();

            candidates.sort(Comparator.comparingLong(TestCandidateMetadata::getEntryProbeIndex));

            for (TestCandidateMetadata candidate : candidates) {
                results.add(convertTestCandidateMetadata(candidate, loadCalls));
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    getTestCandidateById(Long testCandidateId, boolean loadCalls) {
        try {
            TestCandidateMetadata dbCandidate = testCandidateDao.queryForId(testCandidateId);
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata =
                    convertTestCandidateMetadata(dbCandidate, loadCalls);
            return testCandidateMetadata;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArchiveFile getArchiveFileByName(String name) throws SQLException {
        return archiveFileDao.queryForId(name);
    }

    @NotNull Map<String, ArchiveFile> getArchiveFileMap() {
        List<ArchiveFile> archiveFileList = getArchiveList();
        Map<String, ArchiveFile> archiveFileMap = new HashMap<>();
        for (ArchiveFile archiveFile : archiveFileList) {
            archiveFileMap.put(archiveFile.getName(), archiveFile);
        }
        return archiveFileMap;
    }

    public List<LogFile> getPendingLogFilesToProcess() throws SQLException {
        return logFilesDao.queryForEq("status", Constants.PENDING);
    }

    public ThreadProcessingState getThreadState(Integer threadId) throws Exception {
        ThreadState threadState = threadStateDao.queryForId(threadId);
        if (threadState == null) {
            return new ThreadProcessingState(threadId);
        }
        ThreadProcessingState threadProcessingState = new ThreadProcessingState(threadId);

        String[] callIdsList = threadState.getCallStack()
                .split(",");
        @NotNull List<com.insidious.plugin.pojo.MethodCallExpression> callStack;
        callStack = new ArrayList<>(callIdsList.length);
        for (String callId : callIdsList) {
            if (callId.equals("")) {
                continue;
            }
            com.insidious.plugin.pojo.MethodCallExpression call = getMethodCallExpressionById(Long.parseLong(callId));
            callStack.add(call);
        }


//        callStack = buildFromDbMce(dbCallStack);


        if (threadState.getMostRecentReturnedCall() != 0) {
            threadProcessingState.setMostRecentReturnedCall(
                    getMethodCallExpressionById(threadState.getMostRecentReturnedCall()));
        }
        threadProcessingState.setCallStack(callStack);
        threadProcessingState.setNextNewObjectType(
                gson.fromJson(threadState.getNextNewObjectStack(), LIST_STRING_TYPE)
        );
        threadProcessingState.setValueStack(
                gson.fromJson(threadState.getValueStack(), LIST_STRING_TYPE)

        );
        List<TestCandidateMetadata> dbCandidateStack = gson.fromJson(threadState.getCandidateStack(),
                LIST_CANDIDATE_TYPE);

        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> candidateStack =
                dbCandidateStack.stream()
                        .map(e -> {
                            try {
                                return convertTestCandidateMetadata(e, true);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                        .collect(Collectors.toList());


        threadProcessingState.setCandidateStack(candidateStack);
        return threadProcessingState;
    }


    public void createOrUpdateThreadState(ThreadProcessingState threadState) throws SQLException {
        ThreadState daoThreadState = new ThreadState();
        daoThreadState.setThreadId(threadState.getThreadId());
        if (threadState.getMostRecentReturnedCall() != null) {
            daoThreadState.setMostRecentReturnedCall(threadState.getMostRecentReturnedCall()
                    .getId());
        }

        List<com.insidious.plugin.pojo.MethodCallExpression> callStack = threadState.getCallStack();
        @NotNull String callStackList = Strings.join(callStack.stream()
                .map(com.insidious.plugin.pojo.MethodCallExpression::getId)
                .collect(Collectors.toList()), ",");


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> candidateStack = threadState.getCandidateStack();

        List<TestCandidateMetadata> dbCandidateStack = candidateStack.stream()
                .map(TestCandidateMetadata::FromTestCandidateMetadata)
                .collect(Collectors.toList());

        daoThreadState.setCandidateStack(gson.toJson(dbCandidateStack));

        daoThreadState.setCallStack(callStackList);
        daoThreadState.setValueStack(gson.toJson(threadState.getValueStack()));
        daoThreadState.setNextNewObjectStack(gson.toJson(threadState.getNextNewObjectTypeStack()));
        threadStateDao.createOrUpdate(daoThreadState);
    }

    public String getFieldA() {
        return fieldA;
    }

    public void setFieldA(String fieldA) {
        this.fieldA = fieldA;
    }

    public void createOrUpdateClassDefinitions(Collection<ClassDefinition> classDefinitions) {
        try {
            classDefinitionsDao.executeRaw("delete from class_definition");
            classDefinitionsDao.create(classDefinitions);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public void createOrUpdateMethodDefinitions(List<MethodDefinition> methodDefinitions) {
        try {
            methodDefinitionsDao.executeRaw("delete from method_definition");
            methodDefinitionsDao.create(methodDefinitions);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidatePaginated(int page, int limit) throws SQLException {
        List<TestCandidateMetadata> dbCandidateList = testCandidateDao.queryBuilder()
                .offset((long) page * limit)
                .limit((long) limit)
                .orderBy("entryProbeIndex", true)
                .query();

        return dbCandidateList
                .stream()
                .map(e -> {
                    try {
                        return convertTestCandidateMetadata(e, true);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.toList());
    }
}
