package com.insidious.plugin.client;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.pojo.dao.MethodCallExpression;
import com.insidious.plugin.pojo.dao.Parameter;
import com.insidious.plugin.pojo.dao.ProbeInfo;
import com.insidious.plugin.pojo.dao.TestCandidateMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DaoService {


    private final static Logger logger = LoggerUtil.getInstance(DaoService.class);
    private final ConnectionSource connectionSource;
    private final Dao<DataEventWithSessionId, Long> dataEventDao;
    private final Dao<MethodCallExpression, Long> callExpressionsDao;
    private final Dao<Parameter, Long> parameterDao;
    private final Dao<ProbeInfo, Long> probeInfoDao;
    private final Dao<TestCandidateMetadata, Long> candidateDao;

    public DaoService(ConnectionSource connectionSource) throws SQLException {
        this.connectionSource = connectionSource;

        // instantiate the DAO to handle Account with String id
        candidateDao = DaoManager.createDao(connectionSource, TestCandidateMetadata.class);

        probeInfoDao = DaoManager.createDao(connectionSource, ProbeInfo.class);

        parameterDao = DaoManager.createDao(connectionSource, Parameter.class);

        callExpressionsDao = DaoManager.createDao(connectionSource, MethodCallExpression.class);

        dataEventDao = DaoManager.createDao(connectionSource, DataEventWithSessionId.class);

    }


    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidateForSubjectId(Long id) throws SQLException {

//        parameterDao

        List<TestCandidateMetadata> candidateList = candidateDao.queryForEq("testSubject_id", id);


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateList = new LinkedList<>();

        for (TestCandidateMetadata testCandidateMetadata : candidateList) {
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                    getTestCandidateMetadataById(testCandidateMetadata);
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
    private com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata getTestCandidateMetadataById(
            TestCandidateMetadata testCandidateMetadata) throws SQLException {
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                TestCandidateMetadata.toTestCandidate(testCandidateMetadata);

        converted.setTestSubject(getParameterByValue((Long) testCandidateMetadata.getTestSubject().getValue()));
        converted.setMainMethod(getMethodCallExpressionById(testCandidateMetadata.getMainMethod().getId()));

        List<com.insidious.plugin.pojo.MethodCallExpression> callsList = new LinkedList<>();
        Long[] calls = testCandidateMetadata.getCallsList();

        for (Long call : calls) {
            com.insidious.plugin.pojo.MethodCallExpression methodCallExpressionById = getMethodCallExpressionById(call);
            if (!methodCallExpressionById.isMethodPublic()) {
                continue;
            }
            callsList.add(methodCallExpressionById);
        }

        Long[] fieldParameters = testCandidateMetadata.getFields();
        for (Long fieldParameterValue : fieldParameters) {
            com.insidious.plugin.pojo.Parameter fieldParameter = getParameterByValue(fieldParameterValue);
            converted.getFields().add(fieldParameter);
        }


        converted.setCallList(callsList);
        return converted;
    }

    public com.insidious.plugin.pojo.MethodCallExpression getMethodCallExpressionById(Long methodCallId) throws SQLException {


        MethodCallExpression dbMce = callExpressionsDao.queryForId(methodCallId);
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

        Long[] argumentParameters = dbMce.getArguments();
        Long[] argumentProbes = dbMce.getArgumentProbes();
        for (int i = 0; i < argumentParameters.length; i++) {
            Long argumentParameter = argumentParameters[i];
            DataEventWithSessionId dataEvent = getDataEventById(argumentProbes[i]);

            com.insidious.plugin.pojo.Parameter argument = getParameterByValue(argumentParameter);
            if (argument == null) {
                argument = new com.insidious.plugin.pojo.Parameter(0L);
            }
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
        dataEventDao.create(dataEvent);
    }

    public void createOrUpdateCall(com.insidious.plugin.pojo.MethodCallExpression topCall) throws SQLException {
        callExpressionsDao.createOrUpdate(MethodCallExpression.FromMCE(topCall));
    }

    public void createOrUpdateTestCandidate(com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata completed) throws SQLException {
        candidateDao.createOrUpdate(com.insidious.plugin.pojo.dao.TestCandidateMetadata.
                FromTestCandidateMetadata(completed));
    }

    public List<Integer> getProbes() throws SQLException {
        return probeInfoDao.queryBuilder().selectColumns("dataId").query().stream().map(ProbeInfo::getDataId).collect(Collectors.toList());
    }

    public void createOrUpdateCall(Set<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            callExpressionsDao.create(callsToSave.stream().map(MethodCallExpression::FromMCE).collect(Collectors.toList()));
//            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsToSave) {
//                callExpressionsDao.create(MethodCallExpression.FromMCE(methodCallExpression));
//            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCalls(Set<com.insidious.plugin.pojo.MethodCallExpression> callsToSave) {
        try {
            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : callsToSave) {
                callExpressionsDao.update(MethodCallExpression.FromMCE(methodCallExpression));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateTestCandidate(List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> candiateToSave) {
        try {
            candidateDao.create(candiateToSave.stream()
                    .map(TestCandidateMetadata::FromTestCandidateMetadata)
                    .collect(Collectors.toList()));
//            for (com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata : candiateToSave) {
//                candidateDao.create(TestCandidateMetadata.FromTestCandidateMetadata(testCandidateMetadata));
//            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateProbeInfo(Collection<DataInfo> probeInfo) throws SQLException {
        try {

            probeInfoDao.create(probeInfo.stream().map(ProbeInfo::FromProbeInfo).collect(Collectors.toList()));
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
            for (com.insidious.plugin.pojo.Parameter parameter : parameterList) {
                Parameter e = Parameter.fromParameter(parameter);
                parameterDao.createOrUpdate(e);
            }
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
            long result = callExpressionsDao.queryRawValue("select max(id) from method_call");
            return result;
        } catch (SQLException e) {
            return 0;
        }
    }

    public void close() throws Exception {
        connectionSource.close();
    }
}
