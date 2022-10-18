package com.insidious.plugin.client;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.dao.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.support.ConnectionSource;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DaoService {


    public static final String TEST_CANDIDATE_AGGREGATE_QUERY = "select p.type, p.value, count(*)\n" +
            "from test_candidate\n" +
            "         join parameter p on p.value = testSubject_id and p.type != 'java.lang.Object' and length(p.type) > 1\n" +
            "group by p.type, p.value\n" +
            "having count(*) > 1\n" +
            "order by p.type";
    public static final String TEST_CANDIDATE_METHOD_AGGREGATE_QUERY = "select mc.methodName, count(*)\n" +
            "from test_candidate tc\n" +
            "         join parameter p on p.value = testSubject_id\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "where p.type = ?\n" +
            "and mc.methodName != '<init>'\n" +
            "group by mc.methodName\n" +
            "order by mc.methodName;";

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

    }


    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidateForSubjectId(Long id) throws SQLException {

//        parameterDao

        List<TestCandidateMetadata> candidateList = testCandidateDao.queryForEq("testSubject_id", id);


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateList = new LinkedList<>();

        for (TestCandidateMetadata testCandidateMetadata : candidateList) {
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                    convertTestCandidateMetadata(testCandidateMetadata);
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
            TestCandidateMetadata testCandidateMetadata) throws SQLException {
        logger.warn("Build test candidate - " + testCandidateMetadata.getEntryProbeIndex());
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                TestCandidateMetadata.toTestCandidate(testCandidateMetadata);

        converted.setTestSubject(getParameterByValue((Long) testCandidateMetadata.getTestSubject().getValue()));
        converted.setMainMethod(getMethodCallExpressionById(testCandidateMetadata.getMainMethod().getId()));

        List<com.insidious.plugin.pojo.MethodCallExpression> callsList = new LinkedList<>();
        List<Long> calls = testCandidateMetadata.getCallsList();

        logger.warn("\tloading " + calls.size() + " call methods");
        for (Long call : calls) {
            com.insidious.plugin.pojo.MethodCallExpression methodCallExpressionById = getMethodCallExpressionById(call);
            if (methodCallExpressionById.isMethodPublic() || methodCallExpressionById.isMethodProtected()) {
                callsList.add(methodCallExpressionById);
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

    public com.insidious.plugin.pojo.MethodCallExpression getMethodCallExpressionById(Long methodCallId) throws SQLException {


        MethodCallExpression dbMce = methodCallExpressionDao.queryForId(methodCallId);
        com.insidious.plugin.pojo.MethodCallExpression mce = MethodCallExpression.ToMCE(dbMce);

        Parameter mainSubject = dbMce.getSubject();
        if (dbMce.getReturnValue() != null) {
            Parameter returnValue = dbMce.getReturnValue();
            com.insidious.plugin.pojo.Parameter returnParam = getParameterByValue((Long) returnValue.getValue());
            mce.setReturnValue(returnParam);
            if (dbMce.getReturnDataEvent() != 0 && returnParam != null) {
                DataEventWithSessionId returnDataEvent = getDataEventById(dbMce.getReturnDataEvent());
                returnParam.setProb(returnDataEvent);
                mce.setReturnDataEvent(returnDataEvent);
            }
        } else {

        }

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
            argument.setType(ClassTypeUtils.getDottedClassName(eventProbe.getAttribute("Type", "V")));
            argument.setProb(dataEvent);
            mce.addArgument(argument);
        }

        if (!mce.isStaticCall()) {
            com.insidious.plugin.pojo.Parameter subjectParam = getParameterByValue((Long) mainSubject.getValue());
            mce.setSubject(subjectParam);
        }

        mce.setEntryProbeInfo(getProbeInfoById(dbMce.getEntryProbeInfo().getDataId()));


        return mce;
    }

    //    public com.insidious.plugin.pojo.Parameter getParameterById(Long id) throws SQLException {
//        if (id == 0) {
//            return null;
//        }
//        Parameter parameter = parameterDao.queryForId(id);
//        if (parameter == null) {
//            return null;
//        }
//        com.insidious.plugin.pojo.Parameter convertedParameter = Parameter.toParameter(parameter);
//
//        DataEventWithSessionId dataEvent = getDataEventById(convertedParameter.getProb().getNanoTime());
//        if (dataEvent != null) {
//            DataInfo probeInfo = getProbeInfoById(dataEvent.getDataId());
//            convertedParameter.setProbeInfo(probeInfo);
//        }
//
//
//        convertedParameter.setProb(dataEvent);
//
//        if (parameter.getCreatorExpression() != null) {
//            convertedParameter.setCreator(getMethodCallExpressionById(parameter.getCreatorExpression().getEntryTime()));
//        }
//        convertedParameter.setProbeInfo(getProbeInfoById(parameter.getProbeInfo().getDataId()));
//        convertedParameter.setProb(getDataEventById(parameter.getProb().getNanoTime()));
//
//
//        return convertedParameter;
//    }
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
            DataInfo probeInfo = getProbeInfoById(dataEvent.getDataId());
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

    public void createOrUpdateDataEvent(Collection<DataEventWithSessionId> dataEvent) throws SQLException {
        Date start = new Date();
        dataEventDao.create(dataEvent);
//        logger.warn("saving " + dataEvent.size() + " events took " + (new Date().getTime() - start.getTime()) + " ms");
    }

    public void createOrUpdateCall(com.insidious.plugin.pojo.MethodCallExpression topCall) throws SQLException {
        methodCallExpressionDao.createOrUpdate(MethodCallExpression.FromMCE(topCall));
    }

    public void createOrUpdateTestCandidate(com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata completed) throws SQLException {
        testCandidateDao.createOrUpdate(com.insidious.plugin.pojo.dao.TestCandidateMetadata.
                FromTestCandidateMetadata(completed));
    }

    public List<Integer> getProbes() throws SQLException {
        return probeInfoDao.queryBuilder().selectColumns("dataId").query().stream().map(ProbeInfo::getDataId).collect(Collectors.toList());
    }

    public void createOrUpdateCall(Set<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            Date start = new Date();
            methodCallExpressionDao.create(callsToSave.stream().map(MethodCallExpression::FromMCE).collect(Collectors.toList()));
//            logger.warn("saving " + callsToSave.size() + " methods took " + (new Date().getTime() - start.getTime()) + " ms");

//            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsToSave) {
//                callExpressionsDao.create(MethodCallExpression.FromMCE(methodCallExpression));
//            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCalls(Set<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            Date start = new Date();
            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsToSave) {
                methodCallExpressionDao.update(MethodCallExpression.FromMCE(methodCallExpression));
            }
//            logger.warn("updating " + callsToSave.size() + " methods took " + (new Date().getTime() - start.getTime()) + " ms");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateTestCandidate(List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> candiateToSave) {
        try {
            Date start = new Date();
            testCandidateDao.create(candiateToSave.stream()
                    .map(TestCandidateMetadata::FromTestCandidateMetadata)
                    .collect(Collectors.toList()));
//            logger.warn("saving " + candiateToSave.size() + " candidates took " + (new Date().getTime() - start.getTime()) + " ms");

//            for (com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata : candiateToSave) {
//                candidateDao.create(TestCandidateMetadata.FromTestCandidateMetadata(testCandidateMetadata));
//            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateProbeInfo(Collection<DataInfo> probeInfo) {
        try {

            Date start = new Date();
            probeInfoDao.create(probeInfo.stream().map(ProbeInfo::FromProbeInfo).collect(Collectors.toList()));
//            logger.warn("saving " + probeInfo.size() + " probes took " + (new Date().getTime() - start.getTime()) + " ms");
//            for (DataInfo dataInfo : probeInfo) {
//                logger.warn("Save -> "  + dataInfo.getDataId());
//                probeInfoDao.create(ProbeInfo.FromProbeInfo(dataInfo));
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createOrUpdateParameter(List<com.insidious.plugin.pojo.Parameter> parameterList) {
        try {
            Date start = new Date();
            for (com.insidious.plugin.pojo.Parameter parameter : parameterList) {
                Parameter e = Parameter.fromParameter(parameter);
                parameterDao.createOrUpdate(e);
            }
//            logger.warn("saving " + parameterList.size() + " parameters took " + (new Date().getTime() - start.getTime()) + " ms");
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

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> getTestCandidatesForClass(String className) {
        try {
            long result = parameterDao.queryRawValue("select value from parameter where type = '" + className + "'");

            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> results = getTestCandidateForSubjectId(result);

            return results;

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
    getTestCandidatesForMethod(String className, String methodName) {

        try {

            GenericRawResults<TestCandidateMetadata> parameterIds = parameterDao
                    .queryRaw(TEST_CANDIDATE_BY_METHOD_SELECT, testCandidateDao.getRawRowMapper(), className, methodName);

            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> resultList = new LinkedList<>();

            List<TestCandidateMetadata> testCandidates = parameterIds.getResults();
            for (TestCandidateMetadata testCandidate : testCandidates) {
                com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted = convertTestCandidateMetadata(testCandidate);
                resultList.add(converted);
            }

            parameterIds.close();
            return resultList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> getTestCandidates(long value, long entryProbeIndex) {
        try {
            List<TestCandidateMetadata> candidates = testCandidateDao.queryBuilder()
                    .where().eq("testSubject_id", value)
                    .and().lt("entryProbeIndex", entryProbeIndex).query();
            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> results = new LinkedList<>();

            for (TestCandidateMetadata candidate : candidates) {
                results.add(convertTestCandidateMetadata(candidate));
            }

            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
