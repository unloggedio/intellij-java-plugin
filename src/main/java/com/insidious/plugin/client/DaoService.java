package com.insidious.plugin.client;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.pojo.dao.MethodCallExpression;
import com.insidious.plugin.pojo.dao.Parameter;
import com.insidious.plugin.pojo.dao.ProbeInfo;
import com.insidious.plugin.pojo.dao.TestCandidateMetadata;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class DaoService {


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


    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> getTestCandidateForSubjectId(Long id) throws SQLException {
        List<TestCandidateMetadata> candidateList = candidateDao.queryForEq("testSubject_id", id);


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateList = new LinkedList<>();

        for (TestCandidateMetadata testCandidateMetadata : candidateList) {
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                    TestCandidateMetadata.toTestCandidate(testCandidateMetadata);

            converted.setTestSubject(getParameterById((Long) testCandidateMetadata.getTestSubject().getValue()));
            converted.setMainMethod(getMethodCallExpressionById(testCandidateMetadata.getMainMethod().getEntryTime()));

            List<com.insidious.plugin.pojo.MethodCallExpression> callsList = new LinkedList<>();
            Long[] calls = testCandidateMetadata.getCallsList();

            for (Long call : calls) {
                callsList.add(getMethodCallExpressionById(call));
            }


            converted.setCallList(callsList);
//            testCandidateMetadata.getMainMethod().geten;
            testCandidateList.add(converted);

        }


        return testCandidateList;
    }

    public com.insidious.plugin.pojo.MethodCallExpression getMethodCallExpressionById(Long methodCallId) throws SQLException {


        MethodCallExpression dbMce = callExpressionsDao.queryForId(methodCallId);
        com.insidious.plugin.pojo.MethodCallExpression mce = MethodCallExpression.ToMCE(dbMce);

        Parameter mainSubject = dbMce.getSubject();
        if (dbMce.getReturnValue() != null) {
            com.insidious.plugin.pojo.Parameter returnParam = getParameterById((Long) dbMce.getReturnValue().getValue());
            mce.setReturnValue(returnParam);
        } else {

        }


        if (!mce.isStaticCall()) {
            com.insidious.plugin.pojo.Parameter subjectParam = getParameterById((Long) mainSubject.getValue());
            mce.setSubject(subjectParam);
        }

        mce.setEntryProbeInfo(getProbeInfoById(dbMce.getEntryProbeInfo().getDataId()));


        return mce;
    }

    public com.insidious.plugin.pojo.Parameter getParameterById(Long value) throws SQLException {
        Parameter parameter = parameterDao.queryForId(value);
        if (parameter == null) {
            return null;
        }
        com.insidious.plugin.pojo.Parameter convertedParameter = Parameter.toParameter(parameter);

        DataEventWithSessionId dataEvent = getDataEventById(convertedParameter.getProb().getNanoTime());
        if (dataEvent != null) {
            DataInfo probeInfo = getProbeInfoById(dataEvent.getDataId());
            convertedParameter.setProbeInfo(probeInfo);
        }


        convertedParameter.setProb(dataEvent);

        if (parameter.getCreatorExpression() != null) {
            convertedParameter.setCreator(getMethodCallExpressionById(parameter.getCreatorExpression().getEntryTime()));
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

    public void createOrUpdate(com.insidious.plugin.pojo.Parameter existingParameterInstance) throws SQLException {
        parameterDao.createOrUpdate(Parameter.fromParameter(existingParameterInstance));
    }

    public void createOrUpdate(DataInfo probeInfo) throws SQLException {
        probeInfoDao.createOrUpdate(ProbeInfo.FromProbeInfo(probeInfo));
    }

    public void createOrUpdate(DataEventWithSessionId dataEvent) throws SQLException {
        dataEventDao.createOrUpdate(dataEvent);
    }

    public void createOrUpdate(com.insidious.plugin.pojo.MethodCallExpression topCall) throws SQLException {
        callExpressionsDao.createOrUpdate(MethodCallExpression.FromMCE(topCall));
    }

    public void createOrUpdate(com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata completed) throws SQLException {
        candidateDao.createOrUpdate(com.insidious.plugin.pojo.dao.TestCandidateMetadata.
                FromTestCandidateMetadata(completed));
    }
}
