package com.insidious.plugin.client;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.dao.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.support.ConnectionSource;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
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


    public static final String CALLS_TO_MOCK_SELECT_QUERY = "select mc.*\n" +
            "from method_call mc\n" +
            "         left join parameter subject on subject.value == mc.subject_id\n" +
            "         left join probe_info pi on subject.probeInfo_id = pi.dataId\n" +
            "where id in (CALL_IDS)\n" +
            "  and (subject.type is null or subject.type not like 'java.lang%')\n" +
            "  and (subject.type is null or subject.type not like 'org.springframework%')\n" +
            "  and (methodAccess & 1 == 1 or methodAccess & 4 == 4)\n" +
            "  and (pi.eventType is null or pi.eventType != 'CALL')\n" +
            "  and (pi.eventType is null or pi.eventType != 'CALL_RETURN')\n" +
            "  and (names is null or names != '')";
    public static final String TEST_CANDIDATE_BY_METHOD_SELECT = "select tc.*\n" +
            "from test_candidate tc\n" +
            "         join parameter p on p.value = testSubject_id\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "where p.type = ?\n" +
            "  and mc.methodName = ?\n" +
            "order by mc.methodName;";
    private final static Logger logger = LoggerUtil.getInstance(DaoService.class);
    private final ConnectionSource connectionSource;
    private final Dao<DataEventWithSessionId, Long> dataEventDao;
    private final Dao<MethodCallExpression, Long> methodCallExpressionDao;
    private final Dao<Parameter, Long> parameterDao;
    private final Dao<LogFile, Long> logFilesDao;
    private final Dao<ArchiveFile, Long> archiveFileDao;
    private final Dao<ProbeInfo, Long> probeInfoDao;
    private final Dao<TestCandidateMetadata, Long> testCandidateDao;
    private final Dao<TypeInfo, Integer> typeInfoDao;

    public DaoService(ConnectionSource connectionSource) throws SQLException {
        this.connectionSource = connectionSource;

        // instantiate the DAO to handle Account with String id
        testCandidateDao = DaoManager.createDao(connectionSource, TestCandidateMetadata.class);
        probeInfoDao = DaoManager.createDao(connectionSource, ProbeInfo.class);
        parameterDao = DaoManager.createDao(connectionSource, Parameter.class);
        logFilesDao = DaoManager.createDao(connectionSource, LogFile.class);
        archiveFileDao = DaoManager.createDao(connectionSource, ArchiveFile.class);
        methodCallExpressionDao = DaoManager.createDao(connectionSource, MethodCallExpression.class);
        dataEventDao = DaoManager.createDao(connectionSource, DataEventWithSessionId.class);
        typeInfoDao = DaoManager.createDao(connectionSource, TypeInfo.class);

    }


    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidateForSubjectId(Long id) throws SQLException {

//        parameterDao

        List<TestCandidateMetadata> candidateList = testCandidateDao.queryForEq("testSubject_id", id);


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateList = new LinkedList<>();

        for (TestCandidateMetadata testCandidateMetadata : candidateList) {
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                    convertTestCandidateMetadata(testCandidateMetadata, true);
            com.insidious.plugin.pojo.MethodCallExpression mainMethod = (com.insidious.plugin.pojo.MethodCallExpression) converted.getMainMethod();
            if (!mainMethod.getMethodName().equals("<init>")) {
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
    ) throws SQLException {
        logger.warn("Build test candidate - " + testCandidateMetadata.getEntryProbeIndex());
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                TestCandidateMetadata.toTestCandidate(testCandidateMetadata);

        converted.setTestSubject(getParameterByValue((Long) testCandidateMetadata.getTestSubject().getValue()));
        converted.setMainMethod(getMethodCallExpressionById(testCandidateMetadata.getMainMethod().getId()));

        List<com.insidious.plugin.pojo.MethodCallExpression> callsList = new LinkedList<>();
        if (loadCalls) {
            List<Long> calls = testCandidateMetadata.getCallsList();
            List<com.insidious.plugin.pojo.MethodCallExpression> methodCallsFromDb = getMethodCallExpressionToMock(calls);
            logger.warn("\tloading " + calls.size() + " call methods");
            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpressionById : methodCallsFromDb) {
                if (methodCallExpressionById.getSubject() == null ||
                        methodCallExpressionById.getSubject().getType().startsWith("java.lang")) {
                    continue;
                }
//            logger.warn("Add call [" + methodCallExpressionById.getMethodName() + "] - " + methodCallExpressionById);
                if (methodCallExpressionById.isMethodPublic()
                        || methodCallExpressionById.isMethodProtected()
                        || "INVOKEVIRTUAL".equals(methodCallExpressionById.getEntryProbeInfo().getAttribute("Instruction", ""))
                ) {
                    callsList.add(methodCallExpressionById);
                } else {
                    logger.debug("skip call - " + methodCallExpressionById.getId());
                }
            }
        }

        List<Long> fieldParameters = testCandidateMetadata.getFields();
        logger.warn("\tloading " + fieldParameters.size() + " fields");
        for (Long fieldParameterValue : fieldParameters) {
            com.insidious.plugin.pojo.Parameter fieldParameter = getParameterByValue(fieldParameterValue);
            converted.getFields().add(fieldParameter);
        }


        converted.setCallList(callsList);
        return converted;
    }

    private List<com.insidious.plugin.pojo.MethodCallExpression> getMethodCallExpressionByIds(List<Long> callIds) {
        try {
            List<MethodCallExpression> callsFromDb = methodCallExpressionDao.queryBuilder().where().in("id", callIds).query();
            return callsFromDb.stream().map(this::convertDbMCE).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<com.insidious.plugin.pojo.MethodCallExpression> getMethodCallExpressionToMock(List<Long> callIds) {
        try {
            String query = CALLS_TO_MOCK_SELECT_QUERY.replace("CALL_IDS", StringUtils.join(callIds, ","));
            GenericRawResults<MethodCallExpression> results = methodCallExpressionDao
                    .queryRaw(query, methodCallExpressionDao.getRawRowMapper());
            List<com.insidious.plugin.pojo.MethodCallExpression> callsList =
                    results.getResults().stream().map(this::convertDbMCE).collect(Collectors.toList());
            results.close();
            return callsList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @NotNull
//    private com.insidious.plugin.pojo.MethodCallExpression convertDbMCE(
//            MethodCallExpression dbMce,
//            Map<Long, com.insidious.plugin.pojo.Parameter> parameterMap
//    ) {
//        com.insidious.plugin.pojo.MethodCallExpression mce = MethodCallExpression.ToMCE(dbMce);
//        try {
//
//            Parameter mainSubject = dbMce.getSubject();
//            if (dbMce.getReturnValue() != null) {
//                Parameter returnValue = dbMce.getReturnValue();
//                com.insidious.plugin.pojo.Parameter returnParam = null;
//                returnParam = parameterMap.get((Long) returnValue.getValue());
//                mce.setReturnValue(returnParam);
//                if (dbMce.getReturnDataEvent() != 0 && returnParam != null) {
//                    DataEventWithSessionId returnDataEvent = null;
//                    returnDataEvent = getDataEventById(dbMce.getReturnDataEvent());
//                    returnParam.setProb(returnDataEvent);
//                    mce.setReturnDataEvent(returnDataEvent);
//                }
//            } else {
//
//            }
//
//            List<Long> argumentParameters = dbMce.getArguments();
//            List<Long> argumentProbes = dbMce.getArgumentProbes();
//            for (int i = 0; i < argumentParameters.size(); i++) {
//                Long argumentParameter = argumentParameters.get(i);
//                DataEventWithSessionId dataEvent = getDataEventById(argumentProbes.get(i));
//                DataInfo eventProbe = getProbeInfoById(dataEvent.getDataId());
//                com.insidious.plugin.pojo.Parameter argument = parameterMap.get(argumentParameter);
//                if (argument == null) {
//                    argument = new com.insidious.plugin.pojo.Parameter(0L);
//                }
//                argument.setProbeInfo(eventProbe);
//                argument.setType(ClassTypeUtils.getDottedClassName(eventProbe.getAttribute("Type", "V")));
//                argument.setProb(dataEvent);
//                mce.addArgument(argument);
//            }
//
//            mce.setEntryProbeInfo(getProbeInfoById(dbMce.getEntryProbeInfo().getDataId()));
//            if (!mce.isStaticCall()) {
//                com.insidious.plugin.pojo.Parameter subjectParam = parameterMap.get((Long) mainSubject.getValue());
//                mce.setSubject(subjectParam);
//            } else {
//                DataInfo entryProbeInfo = mce.getEntryProbeInfo();
//                com.insidious.plugin.pojo.Parameter staticSubject = new com.insidious.plugin.pojo.Parameter();
//                staticSubject.setType(ClassTypeUtils.getDottedClassName(entryProbeInfo.getAttribute("Owner", "V")));
//                staticSubject.setProb(mce.getEntryProbe());
//                staticSubject.setProbeInfo(entryProbeInfo);
//                staticSubject.setName(ClassTypeUtils.createVariableName(staticSubject.getType()));
//                mce.setSubject(staticSubject);
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//
//
//        return mce;
//    }


    public com.insidious.plugin.pojo.MethodCallExpression getMethodCallExpressionById(Long methodCallId) {
        MethodCallExpression dbMce = null;
        try {
            dbMce = methodCallExpressionDao.queryForId(methodCallId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return convertDbMCE(dbMce);
    }

    @NotNull
    private com.insidious.plugin.pojo.MethodCallExpression convertDbMCE(MethodCallExpression dbMce) {
        com.insidious.plugin.pojo.MethodCallExpression convertedCallExpression = MethodCallExpression.ToMCE(dbMce);
        try {

            Parameter mainSubject = dbMce.getSubject();

            // first we load the subject parameter
            convertedCallExpression.setEntryProbeInfo(getProbeInfoById(dbMce.getEntryProbeInfo().getDataId()));
            if (!convertedCallExpression.isStaticCall()) {
                com.insidious.plugin.pojo.Parameter subjectParam = getParameterByValue((Long) mainSubject.getValue());
                convertedCallExpression.setSubject(subjectParam);
            } else {
                DataInfo entryProbeInfo = convertedCallExpression.getEntryProbeInfo();
                com.insidious.plugin.pojo.Parameter staticSubject = new com.insidious.plugin.pojo.Parameter();
                staticSubject.setType(ClassTypeUtils.getDottedClassName(entryProbeInfo.getAttribute("Owner", "V")));
                staticSubject.setProb(convertedCallExpression.getEntryProbe());
                staticSubject.setProbeInfo(entryProbeInfo);
                staticSubject.setName(ClassTypeUtils.createVariableName(staticSubject.getType()));
                convertedCallExpression.setSubject(staticSubject);
            }


            // second we load the method argument parameters
            List<Long> argumentParameters = dbMce.getArguments();
            List<Long> argumentProbes = dbMce.getArgumentProbes();
            for (int i = 0; i < argumentParameters.size(); i++) {
                Long argumentParameter = argumentParameters.get(i);
                DataEventWithSessionId dataEvent = getDataEventById(argumentProbes.get(i));
                DataInfo eventProbe = getProbeInfoById(dataEvent.getDataId());
                com.insidious.plugin.pojo.Parameter argument = getParameterByValue(argumentParameter);
                if (argument == null) {
                    argument = new com.insidious.plugin.pojo.Parameter(0L);
                }
                argument.setProbeInfo(eventProbe);
                argument.setTypeForced(ClassTypeUtils.getDottedClassName(eventProbe.getAttribute("Type", "V")));
                argument.setProb(dataEvent);
                convertedCallExpression.addArgument(argument);
            }

            // third and finally we load the return parameter
            if (dbMce.getReturnValue() != null) {
                Parameter returnValue = dbMce.getReturnValue();
                com.insidious.plugin.pojo.Parameter returnParam = null;
                returnParam = getParameterByValue((Long) returnValue.getValue());
                if (returnParam == null) {
                    returnParam = new com.insidious.plugin.pojo.Parameter();
                }
                convertedCallExpression.setReturnValue(returnParam);

                DataEventWithSessionId returnDataEvent = getDataEventById(dbMce.getReturnDataEvent());
                returnParam.setProb(returnDataEvent);
                DataInfo eventProbe = getProbeInfoById(returnDataEvent.getDataId());
                returnParam.setProbeInfo(eventProbe);
                returnParam.setTypeForced(ClassTypeUtils.getDottedClassName(eventProbe.getAttribute("Type", "V")));

                if (returnParam.getType() != null && returnDataEvent.getSerializedValue().length == 0) {
                    switch (returnParam.getType()) {
                        case "okhttp3.Response":
                            List<com.insidious.plugin.pojo.MethodCallExpression> callsOnReturnParameter =
                                    getMethodCallExpressionOnParameter(returnParam.getValue());

                            Optional<com.insidious.plugin.pojo.MethodCallExpression> bodyResponseParameter
                                    = callsOnReturnParameter
                                    .stream()
                                    .filter(e -> e.getMethodName().equals("body"))
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
                                    getMethodCallExpressionOnParameter(bodyParameter.getReturnValue().getValue());
                            if (responseBodyCalls.size() == 0 || !responseBodyCalls.get(0).getMethodName().equals("string")) {
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

    private List<com.insidious.plugin.pojo.MethodCallExpression>
    getMethodCallExpressionOnParameter(long subjectId) {
        try {
            List<com.insidious.plugin.pojo.MethodCallExpression> callList = new LinkedList<>();
            List<MethodCallExpression> callListFromDb = methodCallExpressionDao.queryForEq("subject_id", subjectId);

            List<MethodCallExpression> callsToLoad = new LinkedList<>();
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


            List<com.insidious.plugin.pojo.MethodCallExpression> callsList =
                    callListFromDb.stream().map(this::convertDbMCE).collect(Collectors.toList());
            return callsList;

        } catch (SQLException e) {
            e.printStackTrace();
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

        DataEventWithSessionId dataEvent = getDataEventById(convertedParameter.getProb().getNanoTime());
        if (dataEvent != null) {
            DataInfo probeInfo = getProbeInfoById(parameter.getProbeInfo().getDataId());
            convertedParameter.setProbeInfo(probeInfo);
            convertedParameter.setProb(dataEvent);
        }


        if (parameter.getCreatorExpression() != null) {
            convertedParameter.setCreator(getMethodCallExpressionById(parameter.getCreatorExpression().getId()));
        }


        return convertedParameter;
    }

    private DataInfo getProbeInfoById(long dataId) throws SQLException {
        ProbeInfo dataInfo = probeInfoDao.queryForId(dataId);
        if (dataInfo == null) {
            throw new RuntimeException("data info not found - " + dataId);
        }
        return ProbeInfo.ToProbeInfo(dataInfo);
    }

    private DataEventWithSessionId getDataEventById(Long id) throws SQLException {
        return dataEventDao.queryForId(id);
    }

    public void createOrUpdateParameter(com.insidious.plugin.pojo.Parameter existingParameterInstance) throws SQLException {
        if (existingParameterInstance.getProb() == null) {
            return;
        }
        parameterDao.createOrUpdate(Parameter.fromParameter(existingParameterInstance));
    }

    public void createOrUpdateProbeInfo(DataInfo probeInfo) throws SQLException {
        probeInfoDao.createOrUpdate(ProbeInfo.FromProbeInfo(probeInfo));
    }


    public void createOrUpdateDataEvent(DataEventWithSessionId dataEvent) throws SQLException {
        dataEventDao.createOrUpdate(dataEvent);
    }

    public void createOrUpdateDataEvent(Collection<DataEventWithSessionId> dataEvent) {
        try {
            dataEventDao.create(dataEvent);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateCall(com.insidious.plugin.pojo.MethodCallExpression topCall) throws SQLException {
        methodCallExpressionDao.createOrUpdate(MethodCallExpression.FromMCE(topCall));
    }

    public void createOrUpdateTestCandidate(com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata completed) throws SQLException {
        testCandidateDao.createOrUpdate(com.insidious.plugin.pojo.dao.TestCandidateMetadata.
                FromTestCandidateMetadata(completed));
    }

    public List<Integer> getProbes() throws SQLException {
        return probeInfoDao.queryBuilder()
                .selectColumns("dataId")
                .query()
                .stream()
                .map(ProbeInfo::getDataId)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getTypesMap() throws SQLException {
        return typeInfoDao.queryBuilder()
                .selectColumns("id", "name")
                .query()
                .stream()
                .collect(Collectors.toMap(TypeInfo::getName, TypeInfo::getId));
    }

    public void createOrUpdateCall(Collection<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            methodCallExpressionDao.create(
                    callsToSave
                            .stream()
                            .map(MethodCallExpression::FromMCE)
                            .collect(Collectors.toList())
            );
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
            testCandidateDao.create(candidatesToSave.stream()
                    .map(TestCandidateMetadata::FromTestCandidateMetadata)
                    .collect(Collectors.toList()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateProbeInfo(Collection<DataInfo> probeInfo) {
        try {
            probeInfoDao.create(probeInfo.stream().map(ProbeInfo::FromProbeInfo).collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createOrUpdateParameter(List<com.insidious.plugin.pojo.Parameter> parameterList) {
        try {
            for (com.insidious.plugin.pojo.Parameter parameter : parameterList) {
                Parameter e = Parameter.fromParameter(parameter);
                parameterDao.createOrUpdate(e);
            }
            logger.warn("updated " + parameterList.size() + " parameters");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<com.insidious.plugin.pojo.Parameter> getParametersByType(String typeName) {
        try {
            return parameterDao.queryForEq("type", typeName).stream()
                    .map(Parameter::toParameter).collect(Collectors.toList());
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public long getMaxCallId() {
        try {
            long result = methodCallExpressionDao.queryRawValue("select max(id) from method_call");
            return result;
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
            GenericRawResults<String[]> rows = testCandidateDao.queryRaw(TEST_CANDIDATE_METHOD_AGGREGATE_QUERY, typeName);
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
                    .queryRaw(TEST_CANDIDATE_BY_METHOD_SELECT, testCandidateDao.getRawRowMapper(), className, methodName);

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

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidates(long value, long entryProbeIndex, long mainMethodId, boolean loadCalls) {
        try {
            List<TestCandidateMetadata> candidates = testCandidateDao.queryBuilder()
                    .where().eq("testSubject_id", value)
                    .and().le("entryProbeIndex", entryProbeIndex)
                    .and().le("mainMethod_id", mainMethodId)
                    .query();
            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> results = new LinkedList<>();

            for (TestCandidateMetadata candidate : candidates) {
                results.add(convertTestCandidateMetadata(candidate, loadCalls));
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    getTestCandidateById(Long testCandidateId) {
        try {
            TestCandidateMetadata dbCandidate = testCandidateDao.queryForId(testCandidateId);
            return convertTestCandidateMetadata(dbCandidate, true);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void createType(TypeInfo typeInfoForDb) throws SQLException {
        typeInfoDao.create(typeInfoForDb);
    }
}
