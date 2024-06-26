package com.insidious.plugin.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.MethodSignatureParser;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.TestAssertion;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.ThreadProcessingState;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.dao.*;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.StringUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class DaoService {


    public static final String QUERY_TEST_CANDIDATE_AGGREGATE = "select md.ownerType, count(*)\n" +
            "from test_candidate tc\n" +
            "         join method_call mce on mce.id = tc.mainMethod_id\n" +
            "         join method_definition md on md.id = mce.methodDefinitionId\n" +
            "where (mce.methodAccess & 1 == 1)\n" +
            "  and mce.methodName != '<init>'\n" +
            "  and md.ownerType != 'java.lang.Object'\n" +
            "group by md.ownerType, md.ownerType\n" +
            "having count(*) > 0\n" +
            "order by md.ownerType";
    public static final String QUERY_TEST_CANDIDATE_METHOD_AGGREGATE = "select mc.methodName, count(*)\n" +
            "from test_candidate tc\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "         join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where md.ownerType = ? \n" +
            "  and (mc.methodAccess & 1 == 1)\n" +
            "  and mc.methodName != '<init>'\n" +
            "group by mc.methodName\n" +
            "order by mc.methodName;";


    public static final String QUERY_CALLS_TO_MOCK_SELECT_BY_PARENT = "select mc.*\n" +
            "from method_call mc\n" +
            "         left join method_definition md on md.id == mc.methodDefinitionId\n" +
            "where (md.ownerType is null or md.ownerType not like 'java.lang%')\n" +
            "  and (mc.methodAccess & 1 == 1 or mc.methodAccess & 4 == 4)\n" +
            "  and (mc.id = ? or mc.parentId = ?)";

    public static final String QUERY_CALLS_TO_MOCK_SELECT_BY_PARENT_CHILD_CALLS = "select mc.*\n" +
            "from method_call mc\n" +
            "         left join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where (md.ownerType is null or md.ownerType not like 'java.lang%')\n" +
            "  and (mc.methodAccess & 1 == 1 or mc.methodAccess & 4 == 4)\n" +
            "  and mc.parentId in (select mc.id\n" +
            "                      from method_call mc\n" +
            "                               left join method_definition md on md.id = mc.methodDefinitionId\n" +
            "                      where (md.ownerType is null or md.ownerType not like 'java.lang%')\n" +
            "                        and ((mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
            "                              mc.subject_id = ? and mc.threadId = ?)\n" +
            "                          or (mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
            "                              mc.isStaticCall = true and mc.usesFields = true and mc.subject_id != 0 and\n" +
            "                              mc.threadId = ?)))";

    public static final String QUERY_CALLS_TO_MOCK_COUNT_SELECT_BY_PARENT_CHILD_CALLS = "select count(*)\n" +
            "from method_call mc\n" +
            "         left join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where (md.ownerType is null or md.ownerType not like 'java.lang%')\n" +
            "  and (mc.methodAccess & 1 == 1 or mc.methodAccess & 4 == 4)\n" +
            "  and mc.parentId in (select mc.id\n" +
            "                      from method_call mc\n" +
            "                               left join method_definition md on md.id = mc.methodDefinitionId\n" +
            "                      where (md.ownerType is null or md.ownerType not like 'java.lang%')\n" +
            "                        and ((mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
            "                              mc.subject_id = ? and mc.threadId = ?)\n" +
            "                          or (mc.parentId >= ? and mc.returnDataEvent < ? and entryProbe_id > ? and\n" +
            "                              mc.isStaticCall = true and mc.usesFields = true and mc.subject_id != 0 and\n" +
            "                              mc.threadId = ?)))";

    public static final String QUERY_TEST_CANDIDATE_BY_METHOD_SELECT = "select tc.*\n" +
            "from test_candidate tc\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where md.ownerType = ?\n" +
            "  and md.methodName = ?\n" +
            "  and md.methodDescriptor = ?\n" +
            "order by tc.entryProbeIndex desc limit 10;";
    public static final String QUERY_MCE_BY_METHOD_SELECT = "select mc.*\n" +
            "from method_call mc \n" +
            "left join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where (md.ownerType is null or md.ownerType = ?)\n" +
            "  and (mc.methodName = ?)\n" +
            "  and (md.methodDescriptor is null or md.methodDescriptor = ?)\n" +
            "order by mc.entryProbe_id desc limit 10;";
    public static final String QUERY_TEST_CANDIDATE_BY_CLASS_SELECT = "select tc.*\n" +
            "from test_candidate tc\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where md.ownerType = ?\n" +
            "order by tc.entryProbeIndex desc limit 20;";
    public static final String QUERY_TEST_CANDIDATE_BY_ALL_SELECT = "select tc.*\n" +
            "from test_candidate tc\n" +
            "         join method_call mc on mc.id = mainMethod_id\n" +
            "join method_definition md on md.id = mc.methodDefinitionId\n" +
            "order by tc.entryProbeIndex desc limit 10;";

    public static final TypeReference<ArrayList<String>> LIST_STRING_TYPE = new TypeReference<ArrayList<String>>() {
    };
    public static final TypeReference<List<Long>> LIST_LONG_TYPE = new TypeReference<List<Long>>() {
    };
    public static final TypeReference<ArrayList<TestCandidateMetadata>> LIST_CANDIDATE_TYPE = new TypeReference<ArrayList<TestCandidateMetadata>>() {
    };
    public static final TypeReference<ArrayList<MethodCallExpression>> LIST_MCE_TYPE = new TypeReference<ArrayList<MethodCallExpression>>() {
    };
    public static final String QUERY_CLASS_METHOD_CALL_AGGREGATE = "select mc.methodName,\n" +
            "       count(*) as count,\n" +
            "       min(mc.callTimeNano / 1000) as minimum,\n" +
            "       max(mc.callTimeNano / 1000) as maximum,\n" +
            "       median(mc.callTimeNano / 1000) as median,\n" +
            "       stdev(mc.callTimeNano / 1000) as stddev,\n" +
            "       avg(mc.callTimeNano / 1000) as avg\n" +
            "from method_call mc\n" +
            " join method_definition md on md.id = mc.methodDefinitionId\n" +
            "where md.ownerType = ?\n" +
            "group by mc.methodName\n" +
            "order by mc.methodName;";

    public static final String QUERY_PROCESSED_FILE_COUNT = "select count(*)\n" +
            "from log_file lf\n" +
            "where lf.status = 'completed'";
    public static final String QUERY_FILE_COUNT = "select count(*)\n" +
            "from log_file lf'";

    private final static Logger logger = LoggerUtil.getInstance(DaoService.class);
    private static final String QUERY_METHOD_DEFINITIONS_BY_ID_IN = "select * from method_definition where id in (IDS)";
    private final ObjectMapper objectMapper;
    private final JdbcConnectionSource connectionSource;
    private final Dao<DataEventWithSessionId, Long> dataEventDao;
    private final Dao<MethodCallExpression, Long> methodCallExpressionDao;
    private final Dao<MethodDefinition, Long> methodDefinitionsDao;
    private final Dao<ClassDefinition, Long> classDefinitionsDao;
    private final Dao<IncompleteMethodCallExpression, Long> incompleteMethodCallExpressionDao;
    //    private final Dao<Parameter, Long> parameterDao;
    private final Dao<LogFile, Long> logFilesDao;
    private final Dao<ArchiveFile, String> archiveFileDao;
    private final Dao<ThreadState, Integer> threadStateDao;
    private final Dao<ProbeInfo, Long> probeInfoDao;
    private final Dao<TestCandidateMetadata, Long> testCandidateDao;
    private final ParameterProvider parameterProvider;
    private final Lock dbBulkUpdate = new ReentrantLock();
    private boolean shutDown = false;

    public DaoService(JdbcConnectionSource connectionSource, ParameterProvider parameterProvider, ObjectMapper objectMapper) throws SQLException {
        this.objectMapper = objectMapper;
        this.connectionSource = connectionSource;
        this.parameterProvider = parameterProvider;

        // instantiate the DAO to handle Account with String id
        testCandidateDao = DaoManager.createDao(connectionSource, TestCandidateMetadata.class);

        testCandidateDao.executeRaw("PRAGMA synchronous=OFF");
        testCandidateDao.executeRaw("PRAGMA locking_mode=EXCLUSIVE");
        testCandidateDao.executeRaw("PRAGMA journal_mode=OFF");

        probeInfoDao = DaoManager.createDao(connectionSource, ProbeInfo.class);
//        parameterDao = DaoManager.createDao(connectionSource, Parameter.class);
        logFilesDao = DaoManager.createDao(connectionSource, LogFile.class);
        archiveFileDao = DaoManager.createDao(connectionSource, ArchiveFile.class);
        methodCallExpressionDao = DaoManager.createDao(connectionSource, MethodCallExpression.class);
        incompleteMethodCallExpressionDao = DaoManager.createDao(connectionSource,
                IncompleteMethodCallExpression.class);
        threadStateDao = DaoManager.createDao(connectionSource, ThreadState.class);
        dataEventDao = DaoManager.createDao(connectionSource, DataEventWithSessionId.class);
        methodDefinitionsDao = DaoManager.createDao(connectionSource, MethodDefinition.class);
        classDefinitionsDao = DaoManager.createDao(connectionSource, ClassDefinition.class);

        TableUtils.createTableIfNotExists(connectionSource, com.insidious.plugin.pojo.dao.Parameter.class);
        TableUtils.createTableIfNotExists(connectionSource, ArchiveFile.class);

        TableUtils.createTableIfNotExists(connectionSource,
                com.insidious.plugin.pojo.dao.MethodCallExpression.class);
        TableUtils.createTableIfNotExists(connectionSource, DataEventWithSessionId.class);
        TableUtils.createTableIfNotExists(connectionSource, LogFile.class);
        TableUtils.createTableIfNotExists(connectionSource, ProbeInfo.class);
        TableUtils.createTableIfNotExists(connectionSource,
                com.insidious.plugin.pojo.dao.TestCandidateMetadata.class);
        TableUtils.createTableIfNotExists(connectionSource, ThreadState.class);
        TableUtils.createTableIfNotExists(connectionSource, IncompleteMethodCallExpression.class);
        TableUtils.createTableIfNotExists(connectionSource, ClassDefinition.class);
        TableUtils.createTableIfNotExists(connectionSource, MethodDefinition.class);

    }

    /**
     * Removes all substrings enclosed in < and >, including nested ones, using an optimized single-pass stack-based algorithm.
     *
     * @param input The input string from which substrings enclosed in < and > should be removed.
     * @return A new string with all matching substrings removed.
     */
    public static String removeSubstringsSinglePass(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        Stack<Integer> stack = new Stack<>();
        int lastRemovedEnd = -1;

        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '<') {
                stack.push(result.length());
            } else if (input.charAt(i) == '>') {
                stack.pop();
            } else if (stack.isEmpty()) {
                // Only append characters outside of <> blocks
                result.append(input.charAt(i));
            }
        }

        return result.toString();
    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidateForSubjectId(Long id) throws Exception {

//        parameterDao

        List<TestCandidateMetadata> candidateList = testCandidateDao.queryForEq("testSubject_id", id);


        List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateList = new LinkedList<>();

        for (TestCandidateMetadata testCandidateMetadata : candidateList) {
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                    convertTestCandidateMetadata(testCandidateMetadata, true);
            com.insidious.plugin.pojo.MethodCallExpression mainMethod = converted.getMainMethod();
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

    private com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    convertTestCandidateMetadata(TestCandidateMetadata testCandidateMetadata, Boolean loadCalls) throws Exception {
//        logger.warn("Build test candidate - " + testCandidateMetadata.getEntryProbeIndex());
        if (testCandidateMetadata == null) {
            return null;
        }
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                TestCandidateMetadata.toTestCandidate(testCandidateMetadata);


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
                        methodCallExpressionById.getSubject().getType().startsWith("java.lang")) {
                    continue;
                }
                if (mainMethod != null && methodCallExpressionById.getId() == mainMethod.getId()) {
                    continue;
                }
                com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata subTcm = getTestCandidateByMethodCallId(
                        methodCallExpressionById.getId(), true);
                if (subTcm != null) {
                    converted.getLineNumbers().addAll(subTcm.getLineNumbers());
                }
//            logger.warn("Add call [" + methodCallExpressionById.getMethodName() + "] - " + methodCallExpressionById);
                if (methodCallExpressionById.isMethodPublic()
                        || methodCallExpressionById.isMethodProtected()
                        || "INVOKEVIRTUAL".equals(methodCallExpressionById.getEntryProbeInfo()
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
                MethodCallExpression mainMethodCallExpression = getMethodCallExpressionById(
                        testCandidateMetadata.getMainMethod());
                logger.warn("main method isn't public: " + mainMethodCallExpression);
                List<com.insidious.plugin.pojo.MethodCallExpression> methodCallExpressions = buildFromDbMce(
                        Collections.singletonList(mainMethodCallExpression));
                com.insidious.plugin.pojo.MethodCallExpression mainMethod1 = methodCallExpressions.get(0);
                converted.setMainMethod(mainMethod1);
            }


        } else {

//            int methodCallsCountFromDb = getMethodCallExpressionToMockCount(testCandidateMetadata);
            callsList = new ArrayList<>(0);
//            for (int i = 0; i < methodCallsCountFromDb; i++) {
//                callsList.add(new com.insidious.plugin.pojo.MethodCallExpression());
//            }

            List<MethodCallExpressionInterface> mces = new ArrayList<>();
            mces.add(getMethodCallExpressionById(testCandidateMetadata.getMainMethod()));
            com.insidious.plugin.pojo.MethodCallExpression mainMethod = buildFromDbMce(mces).get(0);
            converted.setMainMethod(mainMethod);
        }

        if (converted.getTestSubject() == null) {
            if (converted.getMainMethod().getSubject() != null) {
                converted.setTestSubject(converted.getMainMethod().getSubject());
            }
        }

//        int threadId = converted.getMainMethod().getThreadId();
        com.insidious.plugin.pojo.Parameter parameterByValueFromDb = getParameterByValue(
                testCandidateMetadata.getTestSubject());
        if (parameterByValueFromDb != null && converted.getTestSubject() == null) {
            converted.setTestSubject(parameterByValueFromDb);
        }

        Set<Long> fieldParameters = testCandidateMetadata.getFields();

        for (Long fieldParameterValue : fieldParameters) {
            if (fieldParameterValue == 0L) {
                continue;
            }
            if (loadCalls) {
                Optional<com.insidious.plugin.pojo.MethodCallExpression> callOnField =
                        callsList.stream()
                                .filter(e -> e.getSubject().getValue() == fieldParameterValue)
                                .findFirst();
                if (callOnField.isPresent()) {
                    converted.getFields().add(callOnField.get().getSubject());
                    continue;
                }
            }
            com.insidious.plugin.pojo.Parameter fieldParameter = getParameterByValue(fieldParameterValue);
            converted.getFields().add(fieldParameter);
        }

        if (converted.getMainMethod() != null && !converted.getMainMethod().getMethodName().equals("<init>")) {
            com.insidious.plugin.pojo.MethodCallExpression mainMethodCall = converted.getMainMethod();
            com.insidious.plugin.pojo.Parameter mainMethodReturnValue = mainMethodCall.getReturnValue();
            if (mainMethodReturnValue != null && mainMethodReturnValue.getType() != null
                    && !mainMethodReturnValue.getType().equals("V")) {

                // deserialize and compare objects
                com.insidious.plugin.pojo.Parameter expectedValue;
                String expectedParameterName = (mainMethodReturnValue.getName() == null ? "value" :
                        mainMethodReturnValue.getName()) + "Expected";
                expectedValue = new com.insidious.plugin.pojo.Parameter(mainMethodReturnValue);
                expectedValue.clearNames();
                expectedValue.setName(expectedParameterName);
                DataEventWithSessionId expectedValueProbe = new DataEventWithSessionId();
                if (mainMethodReturnValue.getProb() != null) {
                    expectedValueProbe.setSerializedValue(mainMethodCall.getReturnDataEvent().getSerializedValue());
                }
                expectedValue.setProbeAndProbeInfo(expectedValueProbe, new DataInfo());


                TestAssertion testAssertion = new TestAssertion(AssertionType.EQUAL, expectedValue,
                        mainMethodReturnValue);
                converted.getAssertionList().add(testAssertion);
            }
        }


        converted.setCallList(callsList);
        return converted;
    }

    private List<com.insidious.plugin.pojo.MethodCallExpression>
    getMethodCallExpressionToMockFast(TestCandidateMetadata testCandidateMetadata) {

        long start = Date.from(Instant.now()).getTime();
        try {
            List<MethodCallExpression> mceList = getMethodCallExpressionsInCandidate(testCandidateMetadata);
            List<Long> constructedValues = mceList.stream()
                    .filter(e -> e.getMethodName().equals("<init>"))
                    .map(MethodCallExpression::getReturnValue_id)
                    .collect(Collectors.toList());

            List<MethodCallExpressionInterface> callsToBuild = mceList.stream()
                    .filter(e -> !constructedValues.contains(
                            e.getSubject()) || testCandidateMetadata.getTestSubject() == e.getSubject())
                    .filter(e -> !e.getMethodName().equals("<clinit>"))
                    .filter(e -> !e.getMethodName().equals("intercept"))
                    .collect(Collectors.toList());

            List<com.insidious.plugin.pojo.MethodCallExpression> callsList = buildFromDbMce(callsToBuild);
            long end = Date.from(Instant.now()).getTime();
//            logger.warn("Load calls took[1]: " + (end - start) + " ms");
            return callsList;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            InsidiousNotification.notifyMessage("Failed to load test candidate - " + e.getMessage(),
                    NotificationType.ERROR);
            throw new RuntimeException(e);
        }

    }

    private int getMethodCallExpressionToMockCount(TestCandidateMetadata testCandidateMetadata) {

        long start = Date.from(Instant.now()).getTime();
        try {
            int callCount = getMethodCallExpressionsCountInCandidate(testCandidateMetadata);
            return callCount;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            InsidiousNotification.notifyMessage("Failed to load test candidate - " + e.getMessage(),
                    NotificationType.ERROR);
            return 0;
        }

    }

    private List<MethodCallExpression> getMethodCallExpressionsInCandidate(TestCandidateMetadata testCandidateMetadata) throws Exception {
        long mainMethodId = testCandidateMetadata.getMainMethod();
        GenericRawResults<MethodCallExpression> results = methodCallExpressionDao
                .queryRaw(QUERY_CALLS_TO_MOCK_SELECT_BY_PARENT, methodCallExpressionDao.getRawRowMapper(),
                        String.valueOf(mainMethodId), String.valueOf(mainMethodId));

        List<MethodCallExpression> mceList = new ArrayList<>(results.getResults());
        results.close();
        if (!mceList.isEmpty()) {
            MethodCallExpression mce = mceList.get(0);
            GenericRawResults<MethodCallExpression> subCalls = methodCallExpressionDao.queryRaw(
                    QUERY_CALLS_TO_MOCK_SELECT_BY_PARENT_CHILD_CALLS,
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

    private int getMethodCallExpressionsCountInCandidate(TestCandidateMetadata testCandidateMetadata) throws Exception {

        long mainMethodId = testCandidateMetadata.getMainMethod();
        MethodCallExpression mainMethod = getMethodCallExpressionById(mainMethodId);

        long subCallsCount = methodCallExpressionDao.queryRawValue(
                QUERY_CALLS_TO_MOCK_COUNT_SELECT_BY_PARENT_CHILD_CALLS,
                String.valueOf(mainMethodId), String.valueOf(testCandidateMetadata.getExitProbeIndex()),
                String.valueOf(testCandidateMetadata.getEntryProbeIndex()),
                String.valueOf(testCandidateMetadata.getTestSubject()),
                String.valueOf(mainMethod.getThreadId()),
                String.valueOf(mainMethodId), String.valueOf(testCandidateMetadata.getExitProbeIndex()),
                String.valueOf(testCandidateMetadata.getEntryProbeIndex()), String.valueOf(mainMethod.getThreadId())
        );
        return Math.toIntExact(subCallsCount);
    }

    public ClassMethodAggregates getClassMethodCallAggregates(String className) {

        ClassMethodAggregates methodClassAggregates = new ClassMethodAggregates();

        try {
            GenericRawResults<String[]> queryResult = methodCallExpressionDao.queryRaw(
                    QUERY_CLASS_METHOD_CALL_AGGREGATE, className
            );
            for (String[] result : queryResult.getResults()) {
                MethodCallAggregate callAggregate = new MethodCallAggregate();
                callAggregate.setMethodName(result[0]);
                callAggregate.setCount(Integer.valueOf(result[1]));
                callAggregate.setMinimum(Float.valueOf(result[2]));
                callAggregate.setMaximum(Float.valueOf(result[3]));
                callAggregate.setMedian(Float.valueOf(result[4]));
                callAggregate.setStdDev(Float.valueOf(result[5]));
                callAggregate.setAverage(Float.valueOf(result[6]));
                methodClassAggregates.addMethodAggregate(callAggregate);
            }

        } catch (SQLException e) {
        }

        return methodClassAggregates;
    }

    private List<com.insidious.plugin.pojo.MethodCallExpression> buildFromDbMce(
            List<MethodCallExpressionInterface> mceList
    ) throws Exception {
        Map<Long, MethodCallExpressionInterface> dbMceMap = mceList.stream()
                .collect(Collectors.toMap(MethodCallExpressionInterface::getId, e -> e));
        HashSet<Integer> probeInfoToLoad = new HashSet<>();
        List<com.insidious.plugin.pojo.MethodCallExpression> callsList =
                mceList.stream()
                        .peek(e -> {
                            probeInfoToLoad.add(e.getEntryProbeInfo_id());
                        })
                        .map(MethodCallExpression::ToMCEFromDao)
                        .collect(Collectors.toList());

//            List<com.insidious.plugin.pojo.MethodCallExpression> callsList1 =
//                    mceList.parallelStream().map(this::convertDbMCE).collect(Collectors.toList());

        Set<Long> probesToLoad = new HashSet<>();
        Set<Long> parametersToLoad = new HashSet<>();

        Set<Integer> methodDefinitionIds = mceList.stream()
                .map(MethodCallExpressionInterface::getMethodDefinitionId)
                .collect(Collectors.toSet());

        GenericRawResults<MethodDefinition> methodDefinitionsResultSet = methodDefinitionsDao.queryRaw(
                QUERY_METHOD_DEFINITIONS_BY_ID_IN.replace("IDS", StringUtils.join(methodDefinitionIds, ", ")),
                methodDefinitionsDao.getRawRowMapper());
        List<MethodDefinition> methodDefinitionList = methodDefinitionsResultSet.getResults();

        methodDefinitionsResultSet.close();
        Map<Integer, MethodDefinition> methodDefinitionMap = methodDefinitionList.stream()
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


        List<com.insidious.plugin.pojo.Parameter> dbParameters = getParameterByValue(parametersToLoad);

//        for (Parameter parameter : dbParameters) {
//            probesToLoad.add(parameter.getEventId());
//            probeInfoToLoad.add(parameter.getProbeInfo_id());
//        }

        List<DataEventWithSessionId> allProbes = getProbes(probesToLoad);
        for (DataEventWithSessionId allProbe : allProbes) {
            probeInfoToLoad.add((int) allProbe.getProbeId());
        }


        List<DataInfo> allProbesInfo = getProbesInfo(probeInfoToLoad);

        Map<Long, DataEventWithSessionId> probesMap = allProbes.stream()
                .collect(Collectors.toMap(DataEventWithSessionId::getEventId, e -> e));

        Map<Integer, DataInfo> probeInfoMap = allProbesInfo.stream()
                .collect(Collectors.toMap(DataInfo::getDataId, e -> e));

//        Map<Object, com.insidious.plugin.pojo.Parameter> dbParameterMap = dbParameters.stream()
//                .collect(Collectors.toMap(com.insidious.plugin.pojo.Parameter::getValue, e -> e));


        Map<Long, com.insidious.plugin.pojo.Parameter> parameterMap = dbParameters.stream()
                .collect(Collectors.toMap(com.insidious.plugin.pojo.Parameter::getValue, e -> e));

        List<com.insidious.plugin.pojo.MethodCallExpression> finalCallsList = new ArrayList<>();
        for (int j = 0; j < callsList.size(); j++) {
            com.insidious.plugin.pojo.MethodCallExpression methodCallExpression = callsList.get(j);
            MethodCallExpressionInterface call = mceList.get(j);

            MethodCallExpressionInterface dbMce = dbMceMap.get(methodCallExpression.getId());

            MethodDefinition methodDefinition = null;
            if (methodCallExpression.getMethodDefinitionId() != 0) {
                methodDefinition = methodDefinitionMap.get(methodCallExpression.getMethodDefinitionId());
            }


            if (methodCallExpression.isStaticCall() || dbMce.getSubject() == 0) {
                com.insidious.plugin.pojo.Parameter staticSubject =
                        parameterMap.get(dbMce.getSubject()) != null ? new com.insidious.plugin.pojo.Parameter(
                                parameterMap.get(dbMce.getSubject())) : new com.insidious.plugin.pojo.Parameter();
                staticSubject.setName(ClassTypeUtils.createVariableName(staticSubject.getType()));
                methodCallExpression.setSubject(staticSubject);
            } else {
                methodCallExpression.setSubject(
                        new com.insidious.plugin.pojo.Parameter(parameterMap.get(dbMce.getSubject()))
                );
            }
            com.insidious.plugin.pojo.Parameter subjectParameter = methodCallExpression.getSubject();
            String subjectParameterType = subjectParameter.getType();
            if (subjectParameterType == null) {
                DataInfo probeInfo = subjectParameter.getProbeInfo();
                if (probeInfo == null) {
                    probeInfo = probeInfoMap.get(call.getEntryProbeInfo_id());
                }
                if (probeInfo != null && probeInfo.getEventType().equals(EventType.CALL)) {
                    subjectParameterType = ClassTypeUtils.getDescriptorToDottedClassName(
                            probeInfo.getAttribute("Owner", null));
                    subjectParameter.setType(subjectParameterType);
                } else if (probeInfo != null && probeInfo.getEventType().equals(EventType.METHOD_ENTRY)) {
                    ClassDefinition probeClassOwner = classDefinitionsDao.queryForId(
                            (long) probeInfo.getClassId());
                    subjectParameterType = ClassTypeUtils.getDescriptorToDottedClassName(
                            probeClassOwner.getClassName());
                    subjectParameter.setType(subjectParameterType);
                } else {
                    logger.warn("type for subject of method call [" + dbMce + "] is null [" + subjectParameter + "]");
                    continue;
                }
            } else {
                if (methodDefinition != null) {
                    subjectParameter.setTypeForced(methodDefinition.getOwnerType());
                    subjectParameterType = methodDefinition.getOwnerType();
                }
            }
            if (subjectParameterType.startsWith("java.util.")) {
                continue;
            }
            if (subjectParameterType.startsWith("org.slf4j.Logger")) {
                continue;
            }
            if (subjectParameterType.startsWith("org.apache.commons.")) {
                continue;
            }
            if (subjectParameterType.startsWith("org.springframework.cglib")) {
                continue;
            }
            if (subjectParameterType.startsWith("org.springframework.aop")) {
                continue;
            }

//            com.insidious.plugin.pojo.Parameter subject = methodCallExpression.getSubject();
//            DataInfo subjectProbeInfo = subject.getProbeInfo();
//            String subjectTypeFromProbe = subjectProbeInfo.getAttribute("Type", null);
//            if (subjectTypeFromProbe != null && !subjectTypeFromProbe.equals(subject.getType())) {
//                subject.setTypeForced(subjectTypeFromProbe);
//            }
            finalCallsList.add(methodCallExpression);
            methodCallExpression.setEntryProbe(probesMap.get(dbMce.getEntryProbe_id()));
            methodCallExpression.setEntryProbeInfo(probeInfoMap.get(dbMce.getEntryProbeInfo_id()));

            if (subjectParameterType.contains(".$")) {
                logger.warn(
                        "call subject type contains '.$', replacing with type from somewhere else: " + subjectParameterType);
                // this comes up when a proxy for an object is created
                // eg jdk.proxy2.$Proxy127
                String subjectTypeFromMethodDefinition = null;
                if (methodDefinition != null) {
                    subjectTypeFromMethodDefinition = methodDefinition.getOwnerType();
                }
                String subjectTypeFromProbeInfo = null;
                DataInfo probeInfo = subjectParameter.getProbeInfo();
                switch (probeInfo.getEventType()) {
                    default:
                        subjectTypeFromProbeInfo = probeInfo.getAttribute("Type", null);
                }
                if (subjectTypeFromProbeInfo != null) {
                    subjectParameter.setTypeForced(
                            ClassTypeUtils.getDescriptorToDottedClassName(subjectTypeFromProbeInfo));
                } else if (subjectTypeFromMethodDefinition != null) {
                    subjectParameter.setTypeForced(subjectTypeFromMethodDefinition);
                } else {
                    String callOwnerFromProbe = methodCallExpression.getEntryProbeInfo()
                            .getAttribute("Owner", null);
                    if (callOwnerFromProbe != null) {
                        subjectParameter.setTypeForced(
                                ClassTypeUtils.getDescriptorToDottedClassName(callOwnerFromProbe));
                    }
                }

            }


            List<com.insidious.plugin.pojo.Parameter> arguments = new LinkedList<>();

            List<Long> dbMceArguments = dbMce.getArguments();
            String[] argumentTypesFromMethodDefinition = new String[0];
            if (methodDefinition != null) {
                argumentTypesFromMethodDefinition = methodDefinition.getArgumentTypes()
                        .split(",");
            } else if (methodCallExpression.getEntryProbeInfo()
                    .getEventType() == EventType.CALL) {
                String callDescFromEntryProbe = methodCallExpression.getEntryProbeInfo()
                        .getAttribute("Desc", null);
                if (callDescFromEntryProbe != null) {
                    List<String> descriptorData = MethodSignatureParser.parseMethodSignature(callDescFromEntryProbe);
                    String returnType = descriptorData.remove(descriptorData.size() - 1);
                    argumentTypesFromMethodDefinition =
                            descriptorData.stream()
                                    .map(ClassTypeUtils::getDescriptorToDottedClassName)
                                    .toArray(String[]::new);
                }
            }

            for (int i = 0; i < dbMceArguments.size(); i++) {
                String argumentTypeFromMethodDefinition = null;
                if (argumentTypesFromMethodDefinition.length > 0) {
                    argumentTypeFromMethodDefinition = argumentTypesFromMethodDefinition[i];
                }
                Long argument = dbMceArguments.get(i);
                DataEventWithSessionId dataEvent = probesMap.get(dbMce.getArgumentProbes().get(i));
                DataInfo probeInfo = probeInfoMap.get((int) dataEvent.getProbeId());
                com.insidious.plugin.pojo.Parameter paramArgument = parameterMap.get(argument);
                if (paramArgument == null) {
                    paramArgument = new com.insidious.plugin.pojo.Parameter(0L);
                } else {
                    paramArgument = new com.insidious.plugin.pojo.Parameter(paramArgument);
                }
                paramArgument.setProbeAndProbeInfo(dataEvent, probeInfo);

                String paramArgTypeFromProbe = probeInfo.getAttribute("Type", probeInfo.getValueDesc().getString());
                String argumentTypeFromProbe = ClassTypeUtils.getDescriptorToDottedClassName(paramArgTypeFromProbe);
                // only set param type if the type is not already null or empty
                String existingType = paramArgument.getType();
                if ((existingType == null || existingType.equals("") || existingType.length() == 1)
                        && (!paramArgTypeFromProbe.equals("V")
                        && !paramArgTypeFromProbe.equals("Ljava/lang/Object;"))) {
                    paramArgument.setTypeForced(argumentTypeFromProbe);
                }
                if (argumentTypeFromProbe.equals(
                        argumentTypeFromMethodDefinition) && (existingType == null || existingType.contains("$"))) {
                    paramArgument.setTypeForced(argumentTypeFromMethodDefinition);
                }

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
                returnParam = new com.insidious.plugin.pojo.Parameter(returnParam);
            }
            methodCallExpression.setReturnValue(returnParam);

            DataEventWithSessionId returnDataEvent = probesMap.get(dbMce.getReturnDataEvent());
            DataInfo eventProbe = probeInfoMap.get((int) returnDataEvent.getProbeId());

            if (returnDataEvent.getSerializedValue().length == 8) {
                DataEventWithSessionId referenceId = getReferencedEvent(returnDataEvent.getSerializedValue());
                if (referenceId != null) {
                    returnDataEvent.setSerializedValue(referenceId.getSerializedValue());
                }

            }

            String returnParamType = returnParam.getType();
            if ((returnParamType == null || returnParamType.equals("") || returnParam.isPrimitiveType())
                    && eventProbe.getValueDesc() != Descriptor.Object && eventProbe.getValueDesc() != Descriptor.Void) {
                returnParam.setTypeForced(
                        ClassTypeUtils.getDescriptorToDottedClassName(eventProbe.getValueDesc().getString()));
            }
            if (returnParam.getType() != null && returnParam.getType()
                    .contains("$HibernateProxy")) {
                returnParam.setTypeForced(
                        returnParam.getType().substring(0, returnParam.getType().indexOf("$Hibernate"))
                );
            }
            returnParam.setProbeAndProbeInfo(returnDataEvent, eventProbe);

            String typeFromProbe = ClassTypeUtils.getDescriptorToDottedClassName(eventProbe.getAttribute("Type", null));
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

                        if (!bodyResponseParameter.isPresent()) {
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
                                Collections.singletonList(stringCall.getReturnValue())
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

        }
        return finalCallsList;
    }

    private DataEventWithSessionId getReferencedEvent(byte[] serializedValue) throws SQLException {
        long longVlaue = ByteBuffer.wrap(serializedValue).getLong();
        Collection<DataEventWithSessionId> ref = getDataEventByValue(longVlaue);
        if (ref.isEmpty()) {
            return null;
        }

        DataEventWithSessionId firstEvent = ref.stream().findFirst().get();
        if (ref.size() == 1) {
            return firstEvent;
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write('[');
            AtomicInteger first = new AtomicInteger(1);
            ref.forEach(E -> {
                try {
                    if (first.decrementAndGet() != 0) {
                        outputStream.write(',');
                    }
                    outputStream.write(E.getSerializedValue());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            outputStream.write(']');
            firstEvent.setSerializedValue(outputStream.toByteArray());
        }
        return firstEvent;
    }

    private Collection<DataEventWithSessionId> getDataEventByValue(long longVlaue) throws SQLException {

        String[] count = dataEventDao.queryRaw("select count(*) from data_event where value = " + longVlaue)
                .getFirstResult();
        List<DataEventWithSessionId> resultList = new ArrayList<>();
        if (!count[0].equals("0")) {
            String query = "select * from data_event where value = " + longVlaue;

            GenericRawResults<Object[]> result = dataEventDao.queryRaw(query,
                    new DataType[]{DataType.LONG, DataType.LONG, DataType.LONG, DataType.LONG, DataType.LONG,
                            DataType.BYTE_ARRAY});

            for (Object[] allProbeValues : result.getResults()) {

                DataEventWithSessionId newDataEvent = new DataEventWithSessionId((Long) allProbeValues[0]);
                newDataEvent.setEventId((Long) allProbeValues[1]);
                newDataEvent.setRecordedAt((Long) allProbeValues[2]);
                newDataEvent.setProbeId((Long) allProbeValues[3]);
                newDataEvent.setValue((Long) allProbeValues[4]);
                byte[] serializedValue = (byte[]) allProbeValues[5];
                if (serializedValue.length == 8) {
                    DataEventWithSessionId ref = getReferencedEvent(serializedValue);
                    if (ref != null) {
                        newDataEvent.setSerializedValue(ref.getSerializedValue());
                    }
                } else {
                    newDataEvent.setSerializedValue(serializedValue);
                }
                resultList.add(newDataEvent);
            }
        }
        return resultList;
    }

    public List<com.insidious.plugin.pojo.Parameter> getParameterByValue(Collection<Long> values) {
        if (values.size() == 0) {
            return Collections.emptyList();
        }

//        String query = "select * from parameter where value in (" + StringUtils.join(values, ",") + ")";

        return values.stream()
                .filter(e -> e != 0)
                .map(e -> {
                    try {
                        return getParameterByValue(e);
                    } catch (SQLException ex) {
                        logger.warn("parameter not found: " + ex.getMessage());
//                        ex.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

//        GenericRawResults<Parameter> queryResult = parameterDao.queryRaw(query, parameterDao.getRawRowMapper());
//        List<Parameter> resultList = queryResult.getResults();
//        queryResult.close();
//        if (resultList.size() == 0) {
//            return Collections.emptyList();
//        }

//        return new ArrayList<>(resultList);
    }

    public List<DataEventWithSessionId> getProbes(Collection<Long> values) throws Exception {
        if (values.size() == 0) {
            return Collections.emptyList();
        }

        String query = "select de.threadId, de.eventId, de.recordedAt, de.probeId, de.value, serializedValue from " +
                "data_event de where eventId in (" + StringUtils.join(values, ",") + ")";

        GenericRawResults<Object[]> queryResult = dataEventDao.queryRaw(query,
                new DataType[]{DataType.LONG, DataType.LONG, DataType.LONG, DataType.LONG, DataType.LONG, DataType.BYTE_ARRAY});
        List<Object[]> resultListValues = queryResult.getResults();
        queryResult.close();
        if (resultListValues.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<DataEventWithSessionId> dataEventWithSessionIds = new ArrayList<>();

        for (Object[] allProbeValues : resultListValues) {
            DataEventWithSessionId newDataEvent = new DataEventWithSessionId((Long) allProbeValues[0]);
            newDataEvent.setEventId((Long) allProbeValues[1]);
            newDataEvent.setRecordedAt((Long) allProbeValues[2]);
            newDataEvent.setProbeId((Long) allProbeValues[3]);
            newDataEvent.setValue((Long) allProbeValues[4]);
            byte[] serializedValue = (byte[]) allProbeValues[5];
            newDataEvent.setSerializedValue(serializedValue);
            if (serializedValue.length == 8) {
                DataEventWithSessionId ref = getReferencedEvent(serializedValue);
                if (ref != null) {
                    newDataEvent.setSerializedValue(ref.getSerializedValue());
                }
            }
            dataEventWithSessionIds.add(newDataEvent);
        }


        return dataEventWithSessionIds;
    }

    public List<DataInfo>
    getProbesInfo(Collection<Integer> values) throws Exception {
        if (values.size() == 0) {
            return Collections.emptyList();
        }

        String query = "select * from probe_info where probeId in (" + StringUtils.join(values, ",") + ")";

        GenericRawResults<ProbeInfo> queryResult = probeInfoDao.queryRaw(query, probeInfoDao.getRawRowMapper());
        List<DataInfo> resultList = queryResult.getResults()
                .stream()
                .map(ProbeInfo::ToProbeInfo)
                .collect(Collectors.toList());
        queryResult.close();
        return new ArrayList<>(resultList);
    }

    public MethodCallExpression getMethodCallExpressionById(Long methodCallId) throws Exception {
        MethodCallExpression dbMce = null;
        try {
            dbMce = methodCallExpressionDao.queryForId(methodCallId);
            if (dbMce == null) {
                IncompleteMethodCallExpression incompleteDbMce = incompleteMethodCallExpressionDao.queryForId(
                        methodCallId);
                return MethodCallExpression.FromMCE(incompleteDbMce);
//                return buildFromDbMce(List.of(incompleteDbMce)).get(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dbMce;
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


        List<DataEventWithSessionId> dataEventList = dataEventDao.queryForEq("value", value);
        if (dataEventList.isEmpty()) {
            return null;
        }


        com.insidious.plugin.pojo.Parameter parameter = new com.insidious.plugin.pojo.Parameter(
                value
        );
        for (DataEventWithSessionId dataEvent : dataEventList) {
            ProbeInfo probeInfo = probeInfoDao.queryForId(dataEvent.getProbeId());
            String attributes = probeInfo.getAttributes();

            if (attributes.contains("Type=")) {
                DataInfo dataInfo = KaitaiUtils.toDataInfo(probeInfo);
                parameter.setType(ClassTypeUtils.getDescriptorToDottedClassName(probeInfo.getAttribute("Type", null)));
                parameter.setProbeAndProbeInfo(dataEvent, dataInfo);
                break;
            } else if (attributes.contains("Owner=")) {
                parameter.setType(ClassTypeUtils.getDescriptorToDottedClassName(probeInfo.getAttribute("Owner", null)));
                DataInfo dataInfo = KaitaiUtils.toDataInfo(probeInfo);
                parameter.setProbeAndProbeInfo(dataEvent, dataInfo);
                break;
            } else if (probeInfo.getEventType().equals(EventType.METHOD_OBJECT_INITIALIZED)
                    || probeInfo.getEventType().equals(EventType.METHOD_ENTRY)) {
                ClassDefinition classInfo = classDefinitionsDao.queryForId((long) probeInfo.getClassId());
                DataInfo dataInfo = KaitaiUtils.toDataInfo(probeInfo);
                parameter.setType(ClassTypeUtils.getDescriptorToDottedClassName(classInfo.getClassName()));
                parameter.setProbeAndProbeInfo(dataEvent, dataInfo);
            }
        }


//        parameter.setType();


//        List<Parameter> parameterList = parameterDao.queryForEq("value", value);
//        if (parameterList.size() == 0) {
//            return null;
//        }
//        com.insidious.plugin.pojo.Parameter parameterByValue = parameterProvider.getParameterByValue(value);
//        if (parameterByValue.getProb() != null
//                && parameterByValue.getProb().getSerializedValue().length == 8) {
//            @Nullable DataEventWithSessionId fk = getReferencedEvent(parameterByValue.getProb().getSerializedValue());
//            if (fk != null) {
//                parameterByValue.getProb().setSerializedValue(fk.getSerializedValue());
//            }
//        }
        return parameter;
//        com.insidious.plugin.pojo.Parameter convertedParameter = Parameter.toParameter(parameter);
//
//        DataEventWithSessionId dataEvent = this.getDataEventById(parameter.getEventId());
//        if (dataEvent != null) {
//            DataInfo probeInfo = getProbeInfoById(parameter.getProbeInfo_id());
//            convertedParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
//        }
//
//        return convertedParameter;
    }

//    private DataInfo getProbeInfoById(long probeId) throws SQLException {
//        ProbeInfo dataInfo = probeInfoDao.queryForId(probeId);
//        return ProbeInfo.ToProbeInfo(dataInfo);
//    }

//    private DataEventWithSessionId getDataEventById(Long id) throws SQLException {
//        DataEventWithSessionId dataEventWithSessionId = dataEventDao.queryForId(id);
//        return dataEventWithSessionId;
//    }

    public void createOrUpdateDataEvent(Collection<DataEventWithSessionId> dataEvent) {
        try {
            dataEventDao.create(dataEvent);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to insert data events", e);
        }
    }

    public List<Integer> getProbes() throws SQLException {
        return probeInfoDao.queryBuilder()
                .selectColumns("probeId")
                .query()
                .stream()
                .map(ProbeInfo::getProbeId)
                .collect(Collectors.toList());
    }

    public void createOrUpdateCall(Collection<MethodCallExpression> callsToSave) {
        long start = new Date().getTime();
        try {
            methodCallExpressionDao.create(callsToSave);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to update method call data", e);
        } finally {
            long end = new Date().getTime();
            if (end - start > 200) {
                logger.warn("saving " + callsToSave.size() + " calls took: " + (end - start) + " ms");
            }
        }
    }

    public void createOrUpdateIncompleteCall(Collection<MethodCallExpression> callsToSave) {
        try {
            List<IncompleteMethodCallExpression> items = callsToSave
                    .stream()
                    .map(MethodCallExpression::IncompleteFromMCE)
                    .collect(Collectors.toList());
            for (IncompleteMethodCallExpression item : items) {
                incompleteMethodCallExpressionDao.createOrUpdate(item);
            }
        } catch (SQLException e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to insert data events", e);
        }
    }

    public void updateCalls(Collection<MethodCallExpression> callsToSave) {
        try {
            for (MethodCallExpression methodCallExpression : callsToSave) {
                methodCallExpressionDao.update(methodCallExpression);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateTestCandidate(Collection<TestCandidateMetadata> candidatesToSave) {
        try {
//            TestCandidateMetadata toSave;
            long time = new Date().getTime();
            candidatesToSave.forEach(e -> e.setCreatedAt(time));
            testCandidateDao.create(candidatesToSave);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to insert data events", e);
        }
    }

    public void createOrUpdateProbeInfo(Collection<DataInfo> probeInfo) {
        try {
            probeInfoDao.create(probeInfo.stream()
                    .map(ProbeInfo::FromProbeInfo)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to update probe info", e);
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

    public synchronized void close() throws Exception {
        if (shutDown) {
            return;
        }
        shutDown = true;
        connectionSource.close();
    }

    public List<LogFile> getLogFiles() {
        try {
            return logFilesDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

//    public List<ThreadProcessingState> getThreadList() {
//        try {
//            return threadStateDao.queryForAll()
//                    .stream()
//                    .map(e -> new ThreadProcessingState(1))
//                    .collect(Collectors.toList());
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return Collections.emptyList();
//        }
//    }

    public List<ArchiveFile> getArchiveList() {
        try {
            return archiveFileDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public void createArchiveFileEntry(ArchiveFile archiveFile) {
        try {
            archiveFileDao.create(archiveFile);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to create log archive entry", e);
        }
    }

    public void updateArchiveFileEntry(ArchiveFile archiveFile) {
        try {
            archiveFileDao.update(archiveFile);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createLogFileEntry(LogFile logFile) {
        logger.info("Create log file entry: " + logFile);
        try {
            logFilesDao.create(logFile);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.warn("log file entry already exists: " + logFile, e);
        }
    }

//    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
//    getTestCandidatesForClass(String className) {
//        try {
//            long result = parameterDao.queryRawValue("select value from parameter where type = '" + className + "'");
//
//            return getTestCandidateForSubjectId(result);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException(e);
//        }
//    }

    public void updateLogFileEntry(LogFile logFile) {
        try {
            logFilesDao.update(logFile);
        } catch (SQLException e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to update log entry", e);
        }
    }

    public List<VideobugTreeClassAggregateNode> getTestCandidateAggregates() {
        try {
            List<VideobugTreeClassAggregateNode> aggregateList = new LinkedList<>();
            Map<String, Integer> packageCountAggregate = new HashMap<>();

            GenericRawResults<String[]> rows = testCandidateDao.queryRaw(QUERY_TEST_CANDIDATE_AGGREGATE);
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
            GenericRawResults<String[]> rows = testCandidateDao.queryRaw(QUERY_TEST_CANDIDATE_METHOD_AGGREGATE,
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
    getTestCandidatesForAllMethod(CandidateSearchQuery candidateSearchQuery) {
        logger.warn("query test candidates: " + candidateSearchQuery);
        try {
            long start = new Date().getTime();

            GenericRawResults<TestCandidateMetadata> parameterIds;
            switch (candidateSearchQuery.getCandidateFilterType()) {

                case ALL:
                    parameterIds = testCandidateDao.queryRaw(QUERY_TEST_CANDIDATE_BY_ALL_SELECT,
                            testCandidateDao.getRawRowMapper());
                    break;
                case CLASS:
                    parameterIds = testCandidateDao.queryRaw(QUERY_TEST_CANDIDATE_BY_CLASS_SELECT,
                            testCandidateDao.getRawRowMapper(),
                            candidateSearchQuery.getClassName());

                    break;
                case METHOD:
                    parameterIds = testCandidateDao.queryRaw(QUERY_TEST_CANDIDATE_BY_METHOD_SELECT,
                            testCandidateDao.getRawRowMapper(),
                            candidateSearchQuery.getClassName(),
                            candidateSearchQuery.getMethodName(),
                            candidateSearchQuery.getArgumentsDescriptor());

                    break;
                default:
                    parameterIds = testCandidateDao.queryRaw(QUERY_TEST_CANDIDATE_BY_ALL_SELECT,
                            testCandidateDao.getRawRowMapper());
            }

            List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> resultList = new LinkedList<>();

            List<TestCandidateMetadata> testCandidates = parameterIds.getResults();
            for (TestCandidateMetadata testCandidate : testCandidates) {
                com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata converted =
                        convertTestCandidateMetadata(testCandidate, candidateSearchQuery.isLoadCalls());
                String candidateArgs = StringUtils.join(converted.getMainMethod().getArguments().stream()
                        .map(e -> e.getProb().getSerializedValue().length > 0 ?
                                e.getProb().getSerializedValue().length : e.getValue())
                        .collect(Collectors.toList()), ",");

                resultList.add(converted);
            }

            parameterIds.close();
            long end = new Date().getTime();
            logger.warn("found [" + resultList.size() + "] candidates in " + (end - start) + " ms");
            return resultList;
        } catch (Exception e) {
            logger.warn("failed to getTestCandidatesForAllMethod [" + candidateSearchQuery + "]:  " +
                    e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<com.insidious.plugin.pojo.MethodCallExpression>
    getMethodCallExpressions(CandidateSearchQuery candidateSearchQuery) {
//        logger.warn("query test candidates: " + candidateSearchQuery);
        try {
            long start = new Date().getTime();

            GenericRawResults<MethodCallExpression> parameterIds;
            String argumentsDescriptor = candidateSearchQuery.getArgumentsDescriptor();
            argumentsDescriptor = removeSubstringsSinglePass(argumentsDescriptor);
            parameterIds = methodCallExpressionDao.queryRaw(QUERY_MCE_BY_METHOD_SELECT,
                    methodCallExpressionDao.getRawRowMapper(),
                    candidateSearchQuery.getClassName(),
                    candidateSearchQuery.getMethodName(),
                    argumentsDescriptor);

            List<MethodCallExpressionInterface> mceInterface = new ArrayList<>();
            mceInterface.addAll(parameterIds.getResults());
            parameterIds.close();

            List<com.insidious.plugin.pojo.MethodCallExpression> built = buildFromDbMce(mceInterface);
            List<com.insidious.plugin.pojo.MethodCallExpression> returnList = new ArrayList<>();

            for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : built) {
                if (methodCallExpression.getMethodDefinitionId() != 0) {
                    returnList.add(methodCallExpression);
                } else {
                    if (!methodCallExpression.getSubject().getType().equals(candidateSearchQuery.getClassName())) {
                        continue;
                    }
                    if (!IsSignatureMatch(methodCallExpression, argumentsDescriptor)) {
                        continue;
                    }
                    returnList.add(methodCallExpression);
                }
            }


            long end = new Date().getTime();
            logger.warn("found [" + mceInterface.size() + "] mce in " + (end - start) + " ms");
            return returnList;
        } catch (Exception e) {
            logger.warn("failed to getTestCandidatesForAllMethod [" + candidateSearchQuery + "]:  " +
                    e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private boolean IsSignatureMatch(com.insidious.plugin.pojo.MethodCallExpression methodCallExpression, String argumentsDescriptor) {
        List<String> expectedTypes = MethodSignatureParser.parseMethodSignature(argumentsDescriptor);
        String returnType = expectedTypes.remove(0);
        List<com.insidious.plugin.pojo.Parameter> arguments = methodCallExpression.getArguments();
        int actualArgumentCount = arguments.size();
        int expectedArgumentCount = expectedTypes.size();
        if (actualArgumentCount != expectedArgumentCount) {
            return false;
        }

        //
//        if (!returnType.startsWith(methodCallExpression.getReturnValue().getType())) {
//            return false;
//        }

        for (int i = 0; i < actualArgumentCount; i++) {
            String expectedType = expectedTypes.get(i);
            com.insidious.plugin.pojo.Parameter parameter = arguments.get(i);
            if (parameter.getType() == null || !parameter.getType().startsWith(expectedType)) {
                return false;
            }

        }


        return true;
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

//            List<Parameter> parametersOfSameType = parameterDao.queryForEq("type", parameter.getType());
            List<Parameter> parametersOfSameType = new ArrayList<>();
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
                        constructorCandidate.getMainMethod().setSubject(parameter);
                        constructorCandidate.getMainMethod().setReturnValue(parameter);
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


        return convertTestCandidateMetadata(testCandidate, true);


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
            return convertTestCandidateMetadata(dbCandidate, loadCalls);
        } catch (Exception e) {
            logger.error("failed to load test candidate by id [" + testCandidateId + "]", e);
            return null;
        }
    }

    public com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    getTestCandidateByMethodCallId(Long mainMethodId, boolean loadCalls) {
        try {
            GenericRawResults<TestCandidateMetadata> dbCandidate = testCandidateDao.queryRaw(
                    "select tc.* from test_candidate tc where " +
                            "tc.mainMethod_id = ?", testCandidateDao.getRawRowMapper(), String.valueOf(mainMethodId));
            TestCandidateMetadata firstResult = dbCandidate.getFirstResult();
            dbCandidate.close();
            return convertTestCandidateMetadata(firstResult, loadCalls);
        } catch (Exception e) {
            logger.warn("failed to getTestCandidateByMethodCallId [" + mainMethodId + "]", e);
            return null;
        }
    }


    public ArchiveFile getArchiveFileByName(String name) throws SQLException {
        return archiveFileDao.queryForId(name);
    }

    Map<String, ArchiveFile> getArchiveFileMap() {
        List<ArchiveFile> archiveFileList = getArchiveList();
        Map<String, ArchiveFile> archiveFileMap = new HashMap<>();
        for (ArchiveFile archiveFile : archiveFileList) {
            archiveFileMap.put(archiveFile.getName(), archiveFile);
        }
        return archiveFileMap;
    }

    public List<LogFile> getPendingLogFilesToProcess(String processorId) throws SQLException {
        logger.info("query for log files by processor: " + processorId);
        int modifiedCount = logFilesDao.executeRaw(
                "update log_file set processorId=? where status=? and processorId is null",
                processorId,
                Constants.PENDING);
        if (modifiedCount == 0) {
            return Collections.emptyList();
        }

        return logFilesDao
                .queryBuilder()
                .where()
                .eq("processorId", processorId)
                .and()
                .eq("status", Constants.PENDING)
                .query();
    }


    public long getPendingLogFilesToProcessCount() throws SQLException {
        return logFilesDao.queryBuilder().where().eq("status", Constants.PENDING).countOf();
    }

    public ThreadProcessingState getThreadState(Integer threadId) throws Exception {
        ThreadState threadState = threadStateDao.queryForId(threadId);
        if (threadState == null) {
            return new ThreadProcessingState(threadId);
        }
        ThreadProcessingState threadProcessingState = new ThreadProcessingState(threadId);

        String[] callIdsList = threadState.getCallStack()
                .split(",");
        List<MethodCallExpression> callStack;
        callStack = new ArrayList<>(callIdsList.length);
        for (String callId : callIdsList) {
            if (callId.equals("")) {
                continue;
            }
            MethodCallExpression call = getMethodCallExpressionById(Long.parseLong(callId));
            callStack.add(call);
        }


//        callStack = buildFromDbMce(dbCallStack);


        if (threadState.getMostRecentReturnedCall() != 0) {
            threadProcessingState.setMostRecentReturnedCall(
                    getMethodCallExpressionById(threadState.getMostRecentReturnedCall()));
        }
        threadProcessingState.setCallStack(callStack);
        threadProcessingState.setNextNewObjectType(
                objectMapper.readValue(threadState.getNextNewObjectStack(), LIST_STRING_TYPE));
        threadProcessingState.setValueStack(objectMapper.readValue(threadState.getValueStack(), LIST_LONG_TYPE));
        List<TestCandidateMetadata> dbCandidateStack = objectMapper.readValue(threadState.getCandidateStack(),
                LIST_CANDIDATE_TYPE);

        threadProcessingState.setCandidateStack(dbCandidateStack);
        return threadProcessingState;
    }


    public void createOrUpdateThreadState(ThreadProcessingState threadState) throws JsonProcessingException {
        ThreadState daoThreadState = new ThreadState();
        daoThreadState.setThreadId(threadState.getThreadId());
        if (threadState.getMostRecentReturnedCall() != null) {
            daoThreadState.setMostRecentReturnedCall(threadState.getMostRecentReturnedCall()
                    .getId());
        }

        List<MethodCallExpression> callStack = threadState.getCallStack();
        String callStackList = StringUtils.join(callStack.stream()
                .map(MethodCallExpression::getId)
                .collect(Collectors.toList()), ",");


        List<TestCandidateMetadata> candidateStack = threadState.getCandidateStack();

        daoThreadState.setCandidateStack(objectMapper.writeValueAsString(candidateStack));

        daoThreadState.setCallStack(callStackList);
        daoThreadState.setValueStack(objectMapper.writeValueAsString(threadState.getValueStack()));
        daoThreadState.setNextNewObjectStack(objectMapper.writeValueAsString(threadState.getNextNewObjectTypeStack()));
        try {
            threadStateDao.createOrUpdate(daoThreadState);
        } catch (SQLException e) {
            if (shutDown) {
                return;
            }
            logger.warn("Failed to save thread state", e);
            throw new RuntimeException(e);
        }
    }

    public void createOrUpdateClassDefinitions(Collection<ClassDefinition> classDefinitions) {
        if (!dbBulkUpdate.tryLock()) {
            return;
        }

        try {
            logger.warn("create [" + classDefinitions.size() + "] class definitions");
            classDefinitionsDao.executeRaw("delete from class_definition");
            classDefinitionsDao.create(classDefinitions);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to create class definitions", e);
        } finally {
            dbBulkUpdate.unlock();
        }

    }

    public void createOrUpdateMethodDefinitions(List<MethodDefinition> methodDefinitions) {
        if (!dbBulkUpdate.tryLock()) {
            return;
        }

        try {
            methodDefinitionsDao.executeRaw("delete from method_definition");
            methodDefinitionsDao.create(methodDefinitions);
        } catch (Exception e) {
            if (shutDown) {
                return;
            }
            logger.error("Failed to create method definitions", e);
        } finally {
            dbBulkUpdate.unlock();
        }
    }

    public List<MethodDefinition> getAllMethodDefinitions() {
        try {
            return methodDefinitionsDao.queryForAll();
        } catch (SQLException e) {
            if (shutDown) {
                return new ArrayList<>();
            }
            logger.error("Failed to query method definitions", e);
            throw new RuntimeException(e);
        }
    }

    public MethodDefinition getAllMethodDefinitionBySignature(String className, String methodName,
                                                              String methodSignature) {
        try {
            GenericRawResults<MethodDefinition> queryResult = methodDefinitionsDao.queryRaw(
                    "select * from method_definition where ownerType = ? and methodName = ? and methodDescriptor = ? " +
                            "order by id desc limit 1",
                    methodDefinitionsDao.getRawRowMapper(),
                    className, methodName, methodSignature
            );
            List<MethodDefinition> methodDefinitionList = queryResult.getResults();
            queryResult.close();
            if (methodDefinitionList.size() == 0) {
                return new MethodDefinition();
            }
            return methodDefinitionList.get(0);
        } catch (Exception e) {
            logger.warn("Method definition not found: " + className + "." + methodName + methodSignature);
            return new MethodDefinition();
        }
    }

    public int getCallCountBetween(long probe1, long probe2) {
        try {
            GenericRawResults<String[]> dbCandidateList = testCandidateDao.queryRaw("" +
                            "select count(*)\n" +
                            "from method_call mc\n" +
                            "where mc.entryProbe_id > ? and mc.returnDataEvent < ?",
                    String.valueOf(probe1), String.valueOf(probe2)
            );
            return Integer.parseInt(dbCandidateList.getFirstResult()[0]);
        } catch (SQLException e) {
            //
            logger.warn("Failed to get method call count between");
            return 0;
        }

    }

    public List<com.insidious.plugin.pojo.MethodCallExpression> getCallsBetween(long probe1, long probe2) {
        try {
            GenericRawResults<MethodCallExpression> mceList = methodCallExpressionDao.queryRaw("" +
                            "select mc.*\n" +
                            "from method_call mc\n" +
                            "where mc.entryProbe_id > ? and mc.returnDataEvent < ?",
                    methodCallExpressionDao.getRawRowMapper(),
                    String.valueOf(probe1), String.valueOf(probe2)
            );
            List<MethodCallExpression> results = mceList.getResults();
            mceList.close();
            List<MethodCallExpressionInterface> resultsI = new ArrayList<>(results);

            return buildFromDbMce(resultsI);
        } catch (Exception e) {
            //
            logger.warn("Failed to get method calls between: " + probe1 + ":" + probe2);
            return new ArrayList<>();
        }

    }

    public List<UnloggedTimingTag> getTimingTags(long methodCallId) {


        MethodCallExpression methodCallExpression;
        try {
            methodCallExpression = getMethodCallExpressionById(methodCallId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
        String startEventId = String.valueOf(methodCallExpression.getEntryProbe_id());
        String endEventId = String.valueOf(methodCallExpression.getReturnDataEvent());
        String methodDefinitionId = String.valueOf(methodCallExpression.getMethodDefinitionId());
        try {
            List<UnloggedTimingTag> timingTagList = new ArrayList<>();
            GenericRawResults<Object[]> result = dataEventDao.queryRaw("" +
                            "select pi.line, de.recordedAt \n" +
                            "from data_event de\n" +
                            "         join probe_info pi on pi.probeId = de.probeId\n" +
                            "where de.eventId > ? and de.eventId < ? and pi.methodId = ?\n" +
                            "and pi.eventType = 'LINE_NUMBER' order by eventId;",
                    new DataType[]{DataType.INTEGER, DataType.LONG},
                    startEventId, endEventId, methodDefinitionId);

            for (Object[] objects : result) {
                UnloggedTimingTag utt = new UnloggedTimingTag((Integer) objects[0], (Long) objects[1]);
                timingTagList.add(utt);
            }


            return timingTagList;
        } catch (SQLException e) {
            return new ArrayList<>();
        }

    }

    public List<TestCandidateBareBone>
    getTestCandidatePaginated(long afterEventId, int page, int limit, StompFilterModel stompFilterModel) {
        List<String[]> dbCandidateList;
        try {


            StringBuilder query =
                    new StringBuilder("select tc.entryProbeIndex as id,\n" +
                            "       md.methodName,\n" +
                            "       md.ownerType as className,\n" +
                            "       md.methodDescriptor as signature,\n" +
                            "       tc.exitProbeIndex,\n" +
                            "       tc.lines,\n" +
                            "       mc.callTimeNano,\n" +
                            "       tc.createdAt\n" +
                            "from test_candidate tc\n" +
                            "         join method_call mc on mc.id = tc.mainMethod_id\n" +
                            "         join method_definition md on md.id = mc.methodDefinitionId ");


            List<String> argumentsList = new ArrayList<>();
            boolean first = true;
            query.append(" where ");
            if (!stompFilterModel.isEmpty()) {
                if (!stompFilterModel.getIncludedMethodNames().isEmpty()) {
                    query.append(" ( ");
                    for (String includedMethodName : stompFilterModel.getIncludedMethodNames()) {
                        if (!first) {
                            query.append(" or ");
                        }
                        query.append(" md.methodName like ? ");

                        argumentsList.add(includedMethodName);
                        first = false;
                    }
                    query.append(" ) ");
                }
                if (!stompFilterModel.getIncludedClassNames().isEmpty()) {
                    if (!first) {
                        query.append(" and ");
                    }
                    first = true;
                    query.append(" ( ");
                    for (String includedClassName : stompFilterModel.getIncludedClassNames()) {
                        if (!first) {
                            query.append(" or ");
                        }
                        query.append(" md.ownerType like ? ");
                        argumentsList.add(includedClassName);
                        first = false;
                    }
                    query.append(" ) ");
                }
                if (!stompFilterModel.getExcludedClassNames().isEmpty()) {
                    if (!first) {
                        query.append(" and ");
                    }
                    first = true;
                    query.append(" ( ");
                    for (String excludedClassName : stompFilterModel.getExcludedClassNames()) {
                        if (!first) {
                            query.append(" and ");
                        }
                        query.append(" md.ownerType not like ? ");
                        argumentsList.add(excludedClassName);
                        first = false;
                    }
                    query.append(" ) ");

                }
                if (!stompFilterModel.getExcludedMethodNames().isEmpty()) {

                    if (!first) {
                        query.append(" and ");
                    }
                    first = true;
                    query.append(" ( ");
                    for (String excludedMethodName : stompFilterModel.getExcludedMethodNames()) {
                        if (!first) {
                            query.append(" and ");
                        }
                        query.append(" md.methodName not like ? ");
                        argumentsList.add(excludedMethodName);
                        first = false;
                    }
                    query.append(" ) ");
                }

            }

            if (!first) {
                query.append(" and  ");
            }
            query.append(
                    "tc.entryProbeIndex >= ? "
            );
            argumentsList.add(String.valueOf(afterEventId));
            query.append(" order by tc.entryProbeIndex desc");
            query.append(" limit ? ");
            argumentsList.add(String.valueOf(limit));
            query.append(" offset ? ");
            argumentsList.add(String.valueOf(page * limit));

//            dbCandidateList = testCandidateDao.queryBuilder()
//                    .where()
//                    .ge("entryProbeIndex", afterEventId)
//                    .queryBuilder()
//                    .offset((long) page * limit)
//                    .limit((long) limit)
//                    .orderBy("entryProbeIndex", true)
//                    .query();


            GenericRawResults<String[]> resultList = testCandidateDao.queryRaw(
                    query.toString(), argumentsList.toArray(new String[0])
            );
            dbCandidateList = resultList.getResults();

        } catch (Exception e) {
            logger.warn("Failed to query database: " + e.getMessage(), e);
            return new ArrayList<>();
        }

        return dbCandidateList
                .stream()
                .map(e -> {
                    try {
//                        long start = System.currentTimeMillis();
//                        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata = convertTestCandidateMetadata(
//                                e, false);
//                        logger.warn("Convert test case took: " + (System.currentTimeMillis() - start) + " ms");
                        TestCandidateBareBone testCandidateMetadata = new TestCandidateBareBone();

                        testCandidateMetadata.setId(Long.parseLong(e[0]));
                        MethodUnderTest methodUnderTest = new MethodUnderTest();
                        methodUnderTest.setName(e[1]);
                        methodUnderTest.setClassName(e[2]);
                        methodUnderTest.setSignature(e[3]);
                        testCandidateMetadata.setMethodUnderTest(methodUnderTest);
                        testCandidateMetadata.setExitProbeIndex(Long.parseLong(e[4]));
                        if (e[5] != null) {
                            testCandidateMetadata.setLineNumbers(
                                    Arrays.stream(e[5].split(",")).map(Integer::valueOf).collect(Collectors.toList())
                            );
                        } else {
                            testCandidateMetadata.setLineNumbers(new ArrayList<>());
                        }
                        testCandidateMetadata.setTimeSpentNano(Long.parseLong(e[6]));
                        testCandidateMetadata.setCreatedAt(Long.parseLong(e[7]));
                        return testCandidateMetadata;
                    } catch (Exception ex) {
                        logger.warn("failed to convert test candidate ", ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTopLevelTestCandidatePaginated(long afterEventId, int page, int limit) throws SQLException {

        List<TestCandidateMetadata> dbCandidateList = testCandidateDao.queryRaw("select t.*\n" +
                        "from test_candidate t\n" +
                        "         join method_call mc on mc.id = t.mainMethod_id\n" +
                        "where t.entryProbeIndex > ?\n" +
                        "and mc.parentId == 0\n" +
                        "order by t.entryProbeIndex\n" +
                        "limit ? offset ?; ", testCandidateDao.getRawRowMapper(),
                String.valueOf(afterEventId), String.valueOf(limit), String.valueOf(page * limit)).getResults();

        return dbCandidateList
                .stream()
                .parallel()
                .map(e -> {
                    try {
                        return convertTestCandidateMetadata(e, false);
                    } catch (Exception ex) {
                        logger.warn("failed to convert test candidate metadata", ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata>
    getTestCandidateBetween(long afterEventId, long beforeEventId) {

        List<TestCandidateMetadata> dbCandidateList = null;
        try {
            dbCandidateList = testCandidateDao.queryRaw("select t.*\n" +
                            "from test_candidate t\n" +
                            "         join method_call mc on mc.id = t.mainMethod_id\n" +
                            "where t.entryProbeIndex > ?\n" +
                            "and t.exitProbeIndex < ?\n" +
                            "and mc.parentId == 0\n" +
                            "order by t.entryProbeIndex\n" +
                            "; ", testCandidateDao.getRawRowMapper(),
                    String.valueOf(afterEventId), String.valueOf(beforeEventId)).getResults();
        } catch (SQLException e) {
            // should never happen
            logger.error("Failed to query db for test candidates", e);
            return new ArrayList<>();
        }

        return dbCandidateList
                .stream()
                .map(e -> {
                    try {
                        return convertTestCandidateMetadata(e, false);
                    } catch (Exception ex) {
                        logger.warn("failed to convert test candidate metadata", ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public int getProcessedFileCount() {
        try {
            GenericRawResults<String[]> rows = logFilesDao.queryRaw(QUERY_PROCESSED_FILE_COUNT);
            Optional<String> processed_count = Arrays.stream(rows.getFirstResult()).findFirst();
            return Integer.parseInt(processed_count.get());
        } catch (Exception e) {
            logger.warn("failed to get count of processed files: " + e.getMessage());
            return 0;
        }
    }

    public int getTotalFileCount() {
        try {
            long count = logFilesDao.countOf();
            return (int) count;
        } catch (Exception e) {
            logger.warn("failed to get count of total files: " + e.getMessage());
            return 0;
        }
    }
}
