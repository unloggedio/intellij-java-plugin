package com.insidious.plugin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.agent.UnloggedSdkApiAgentClient;
import com.insidious.plugin.client.TypeInfoDocumentClient.TypeInfoDocumentClientDeserializer;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.ExecutionSessionSourceMode;
import com.insidious.plugin.coverage.CodeCoverageData;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.dao.MethodDefinition;
import com.insidious.plugin.ui.NewTestCandidateIdentifiedListener;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.StringUtils;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class NetworkSessionInstanceClient implements SessionInstanceInterface {

    private static final TypeReference<List<TestCandidateMetadata>> TYPE_REFERENCE_LIST_TCM = new TypeReference<>() {};
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final Logger logger = LoggerUtil.getInstance(NetworkSessionInstanceClient.class);
    private final Map<String, ClassInfo> classInfoIndexByName = new HashMap<>();
    private final UnloggedSdkApiAgentClient unloggedSdkApiAgentClient;
    // client attributes
    private final String endpoint;
    private String token;
    private final OkHttpClient client;
    private boolean shutdown = false;
    // endpoint attributes
    private static final String ping = "/session/ping";
    private static final String isScanEnableEndpoint = "/session/isScanEnable";
    private static final String getTypeInfoTypeString = "/session/getTypeInfoTypeString";
    private static final String getTypeInfoTypeInt = "/session/getTypeInfoTypeInt";
    private static final String getTotalFileCount = "/session/getTotalFileCount";
    private static final String getTimingTags = "/session/getTimingTags";
    private static final String getTestCandidatesForAllMethod = "/session/getTestCandidatesForAllMethod";
    private static final String getTestCandidateById = "/session/getTestCandidateById";
    private static final String getTestCandidateBetween = "/session/getTestCandidateBetween";
    private static final String getTestCandidateAggregatesByClassName = "/session/getTestCandidateAggregatesByClassName";
    private static final String getProcessedFileCount = "/session/getProcessedFileCount";
    private static final String getMethodDefinition = "/session/getMethodDefinition";
    private static final String getMethodCallsBetween = "/session/getMethodCallsBetween";
    private static final String getMethodCallExpressions = "/session/getMethodCallExpressions";
    private static final String getMethodCallCountBetween = "/session/getMethodCallCountBetween";
    private static final String getInitTimestamp = "/session/getInitTimestamp";
    private static final String getClassWeaveInfo = "/session/getClassWeaveInfo";
    private static final String getClassIndex = "/session/getClassIndex";
    private static final String getAllTypes = "/session/getAllTypes";
    private static final String getTestCandidatePaginatedByStompFilterModel = "/session/getTestCandidatePaginatedByStompFilterModel";
    private static final String getConstructorCandidate = "/session/getConstructorCandidate";
    private static final String getExecutionSession = "/session/getExecutionSession";
    private static final String discovery = "/discovery";
    // session instance attributes
    private boolean isConnected = false;
    private boolean scanEnable;
    private String sessionURL;

    public NetworkSessionInstanceClient(String endpoint, String sessionId, ServerMetadata serverMetadata) {
        this.endpoint = endpoint;
        this.sessionURL = "?sessionId=" + sessionId;
        this.client = new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();

        String agentServerUrl = serverMetadata.getAgentServerUrl();
        if (agentServerUrl == null || agentServerUrl.length() < 6) {
            agentServerUrl = "http://localhost:12100";
        }
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TypeInfoDocumentClient.class, new TypeInfoDocumentClientDeserializer());
        objectMapper.registerModule(module);

        this.unloggedSdkApiAgentClient = new UnloggedSdkApiAgentClient(agentServerUrl);
    }

    private void get(String url, Callback callback) {
        Request.Builder builder = new Request.Builder().url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder = builder.addHeader("Authorization", "Bearer " + token);
        }
        Request request = builder.build();
        Call call = this.client.newCall(request);
        call.enqueue(callback);


    }

    private void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON);

        Request.Builder builder = new Request.Builder();

        builder.url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);
        Request request = builder.build();
        this.client.newCall(request)
                .enqueue(callback);
    }

    private Response postSync(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);

        Request.Builder builder = new Request.Builder();

        builder.url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        return this.client.newCall(request)
                .execute();
    }

    @Override
    public boolean isScanEnable() {

        String url = this.endpoint + this.isScanEnableEndpoint + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Map<String, Boolean> jsonVal = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    scanEnable = jsonVal.get("scanEnable");
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return scanEnable;
    }

    @Override
    public TypeInfo getTypeInfo(String name) {

        String url = this.endpoint + this.getTypeInfoTypeString + this.sessionURL + "&name=" + name;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TypeInfo> typeInfo = new AtomicReference<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    TypeInfoClient typeInfoClient = objectMapper.readValue(responseBody, TypeInfoClient.class);
                    typeInfo.set(typeInfoClient.getTypeInfo());
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return typeInfo.get();
    }

    @Override
    public TypeInfo getTypeInfo(Integer typeId) {

        String url = this.endpoint + this.getTypeInfoTypeInt + this.sessionURL + "&typeId=" + typeId;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TypeInfo> typeInfo = new AtomicReference<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    TypeInfoClient typeInfoClient = objectMapper.readValue(responseBody, TypeInfoClient.class);
                    typeInfo.set(typeInfoClient.getTypeInfo());
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return typeInfo.get();
    }

    @Override
    public int getTotalFileCount() {

        String url = this.endpoint + this.getTotalFileCount + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger totalFileCount = new AtomicInteger();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Map<String, Integer> jsonVal = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    totalFileCount.set(jsonVal.get("totalFileCount"));
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return totalFileCount.get();
    }

    @Override
    public List<UnloggedTimingTag> getTimingTags(long id) {

        String url = this.endpoint + this.getTimingTags + this.sessionURL + "&id=" + id;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<UnloggedTimingTag> unloggedTimingTags = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    // define unloggedTimingTags
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<UnloggedTimingTag> UnloggedTimingTagClientList = objectMapper.readValue(responseBody,
                            new TypeReference<>() {
                            });
                    unloggedTimingTags.addAll(UnloggedTimingTagClientList);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return unloggedTimingTags;
    }

    @Override
    public List<TestCandidateMetadata> getTestCandidatesForAllMethod(CandidateSearchQuery candidateSearchQuery) {

        boolean loadCalls = candidateSearchQuery.isLoadCalls();
        List<String> interfaceNames = candidateSearchQuery.getInterfaceNames();
        String argumentsDescriptor = candidateSearchQuery.getArgumentsDescriptor();
        CandidateFilterType candidateFilterType = candidateSearchQuery.getCandidateFilterType();
        String methodSignature = "string";
        try {
            methodSignature = candidateSearchQuery.getMethodSignature();
        } catch (Exception e) {
        }
        String className = "string";
        try {
            className = candidateSearchQuery.getClassName();
        } catch (Exception e) {
        }
        String methodName = "string";
        try {
            methodName = candidateSearchQuery.getMethodName();
        } catch (Exception e) {
        }

        String interfaceDataString = "";
        for (int i = 0; i <= interfaceNames.size() - 1; i++) {
            interfaceDataString += "&interfaceNames=" + interfaceNames.get(i);
        }

        String url = this.endpoint + this.getTestCandidatesForAllMethod + this.sessionURL + "&loadCalls=" + loadCalls + interfaceDataString + "&argumentsDescriptor=" + argumentsDescriptor + "&candidateFilterType=" + candidateFilterType + "&methodSignature=" + methodSignature + "&className=" + className + "&methodName=" + methodName;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<TestCandidateMetadata> localtcml = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<TestCandidateMetadata> val = objectMapper.readValue(responseBody, TYPE_REFERENCE_LIST_TCM);
                    localtcml.addAll(val);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localtcml;
    }

    @Override
    public TestCandidateMetadata getTestCandidateById(Long testCandidateId, boolean loadCalls) {

        String url = this.endpoint + this.getTestCandidateById + this.sessionURL + "&testCandidateId=" + testCandidateId + "&loadCalls=" + loadCalls;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TestCandidateMetadata> testCandidateMetadata = new AtomicReference<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    TestCandidateMetadata testCandidateMetadataR = objectMapper.readValue(responseBody,
                            TestCandidateMetadata.class);
                    testCandidateMetadata.set(testCandidateMetadataR);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return testCandidateMetadata.get();
    }

    @Override
    public List<TestCandidateMetadata> getTestCandidateBetween(long eventIdStart, long eventIdEnd) throws SQLException {

        String url = this.endpoint + this.getTestCandidateBetween + this.sessionURL + "&eventIdStart=" + eventIdStart + "&eventIdEnd=" + eventIdEnd;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<TestCandidateMetadata> localtcml = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<TestCandidateMetadata> val = objectMapper.readValue(responseBody, TYPE_REFERENCE_LIST_TCM);
                    localtcml.addAll(val);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localtcml;
    }

    @Override
    public List<TestCandidateMethodAggregate> getTestCandidateAggregatesByClassName(String className) {

        String url = this.endpoint + this.getTestCandidateAggregatesByClassName + this.sessionURL + "&className=" + className;
        CountDownLatch latch = new CountDownLatch(1);

        ArrayList<TestCandidateMethodAggregate> localtcma = new ArrayList<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<TestCandidateMethodAggregate> val = objectMapper.readValue(responseBody,
                            new TypeReference<>() {
                            });
                    localtcma.addAll(val);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localtcma;
    }

    @Override
    public int getProcessedFileCount() {

        String url = this.endpoint + this.getProcessedFileCount + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger processedFileCount = new AtomicInteger();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Map<String, Integer> jsonVal = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    processedFileCount.set(jsonVal.get("processedFileCount"));
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return processedFileCount.get();
    }

    @Override
    public MethodDefinition getMethodDefinition(MethodUnderTest methodUnderTest) {

        String name = methodUnderTest.getName();
        String signature = methodUnderTest.getSignature();
        String className = methodUnderTest.getClassName();
        int methodHash = methodUnderTest.getMethodHash();

        String url = this.endpoint + this.getMethodDefinition + this.sessionURL +
                "&name=" + name + "&signature=" + signature + "&className=" + className + "&methodHash=" + methodHash;
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<MethodDefinition> methodDefinition = new AtomicReference<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    methodDefinition.set(objectMapper.readValue(responseBody, MethodDefinition.class));
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return methodDefinition.get();
    }

    @Override
    public List<MethodCallExpression> getMethodCallsBetween(long start, long end) {

        String url = this.endpoint + this.getMethodCallsBetween + this.sessionURL + "&start=" + start + "&end=" + end;
        CountDownLatch latch = new CountDownLatch(1);

        ArrayList<MethodCallExpression> localMethodCallExpression = new ArrayList<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<MethodCallExpression> val = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    localMethodCallExpression.addAll(val);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localMethodCallExpression;
    }

    @Override
    public List<MethodCallExpression> getMethodCallExpressions(CandidateSearchQuery candidateSearchQuery) {

        boolean loadCalls = candidateSearchQuery.isLoadCalls();
        List<String> interfaceNames = candidateSearchQuery.getInterfaceNames();
        String argumentsDescriptor = candidateSearchQuery.getArgumentsDescriptor();
        CandidateFilterType candidateFilterType = candidateSearchQuery.getCandidateFilterType();
        String methodSignature = "string";
        try {
            methodSignature = candidateSearchQuery.getMethodSignature();
        } catch (Exception e) {
        }
        String className = "string";
        try {
            className = candidateSearchQuery.getClassName();
        } catch (Exception e) {
        }
        String methodName = "string";
        try {
            methodName = candidateSearchQuery.getMethodName();
        } catch (Exception e) {
        }

        String interfaceDataString = "";
        for (int i = 0; i <= interfaceNames.size() - 1; i++) {
            interfaceDataString += "&interfaceNames=" + interfaceNames.get(i);
        }

        String url = this.endpoint + this.getMethodCallExpressions + this.sessionURL + "&loadCalls=" + loadCalls + interfaceDataString + "&argumentsDescriptor=" + argumentsDescriptor + "&candidateFilterType=" + candidateFilterType + "&methodSignature=" + methodSignature + "&className=" + className + "&methodName=" + methodName;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<MethodCallExpression> localMethodCallExpression = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<MethodCallExpression> val = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    localMethodCallExpression.addAll(val);

                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localMethodCallExpression;
    }

    @Override
    public int getMethodCallCountBetween(long start, long end) {

        String url = this.endpoint + this.getMethodCallCountBetween + this.sessionURL +
                "&start=" + start + "&end=" + end;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger methodCallCount = new AtomicInteger();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Map<String, Integer> jsonVal = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    methodCallCount.set(jsonVal.get("methodCallCountBetween"));
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return methodCallCount.get();
    }

    public long getInitTimestamp() {

        String url = this.endpoint + this.getInitTimestamp + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong initTimestamp = new AtomicLong();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Map<String, Long> jsonVal = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    initTimestamp.set(jsonVal.get("initTimestamp"));
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return initTimestamp.get();
    }

    @Override
    public ClassWeaveInfo getClassWeaveInfo() {

        String url = this.endpoint + this.getClassWeaveInfo + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ClassWeaveInfo> classWeaveInfo = new AtomicReference<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    classWeaveInfo.set(objectMapper.readValue(responseBody, ClassWeaveInfo.class));
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return classWeaveInfo.get();
    }

    @Override
    public Map<String, ClassInfo> getClassIndex() {

        String url = this.endpoint + this.getClassIndex + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<Map<String, ClassInfo>> classIndex = new AtomicReference<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    classIndex.set(objectMapper.readValue(responseBody, new TypeReference<>() {
                    }));
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return classIndex.get();
    }

    @Override
    public List<TypeInfoDocument> getAllTypes() {

        String url = this.endpoint + this.getAllTypes + this.sessionURL;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<TypeInfoDocument> listTypeInfoDocument = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {

                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<TypeInfoDocumentClient> val = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    listTypeInfoDocument.addAll(val.stream()
                            .map(TypeInfoDocumentClient::getTypeInfoDocument)
                            .collect(Collectors.toList()));
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return listTypeInfoDocument;
    }

    @Override
    public boolean isConnected() {

        String url = this.endpoint + this.ping;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.warn("failure encountered in isConnected", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Map<String, Boolean> jsonVal = objectMapper.readValue(responseBody, new TypeReference<>() {
                    });
                    isConnected = jsonVal.get("status");
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return isConnected;
    }

    @Override
    public void getTestCandidates(Consumer<List<TestCandidateBareBone>> testCandidateReceiver, long afterEventId, StompFilterModel stompFilterModel, AtomicInteger cdl) {

        int page = 0;
        int limit = 50;
        int count = 0;
        int attempt = 0;
        long currentAfterEventId = afterEventId;
        while (true) {
            attempt++;
//            if (shutdown) {
//                cdl.decrementAndGet();
//                break;
//            }
            if (cdl.get() < 1) {
                logger.warn(
                        "shutting down query started at [" + afterEventId + "] currently at item [" + count +
                                "] => [" + currentAfterEventId + "] attempt [" + attempt + "]");
                break;
            }
            // server block
            List<TestCandidateBareBone> testCandidateMetadataList = getTestCandidatePaginatedByStompFilterModel(
                    stompFilterModel,
                    currentAfterEventId,
                    limit);
            if (cdl.get() < 1) {
                logger.warn(
                        "shutting down query started at [" + afterEventId + "] currently at item [" + count +
                                "] => [" + currentAfterEventId + "] attempt [" + attempt + "]");
                break;
            }
            if (!testCandidateMetadataList.isEmpty()) {
                count += testCandidateMetadataList.size();
                testCandidateReceiver.accept(testCandidateMetadataList);
                currentAfterEventId = testCandidateMetadataList.get(0).getId() + 1;
            }
            if (testCandidateMetadataList.size() < limit) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public List<TestCandidateBareBone> getTestCandidatePaginatedByStompFilterModel(StompFilterModel stompFilterModel,
                                                                                   long currentAfterEventId,
                                                                                   int limit) {
        StringBuilder includedClassNamePart = new StringBuilder();
        Set<String> includedClassNames = stompFilterModel.getIncludedClassNames();
        for (String localIncludedClassName : includedClassNames) {
            includedClassNamePart.append("&includedClassNames=").append(localIncludedClassName);
        }

        StringBuilder excludedClassNamePart = new StringBuilder();
        Set<String> excludedClassNames = stompFilterModel.getExcludedClassNames();
        for (String localExcludedClassName : excludedClassNames) {
            excludedClassNamePart.append("&excludedClassNames").append(localExcludedClassName);
        }

        StringBuilder includedMethodName = new StringBuilder();
        Set<String> includedMethodNames = stompFilterModel.getIncludedMethodNames();
        for (String localIncludedMethodName : includedMethodNames) {
            includedMethodName.append("&includedMethodNames=").append(localIncludedMethodName);
        }

        StringBuilder excludedMethodName = new StringBuilder();
        Set<String> excludedMethodNames = stompFilterModel.getExcludedMethodNames();
        for (String localExcludedMethodNames : excludedMethodNames) {
            excludedMethodName.append("&excludedMethodNames=").append(localExcludedMethodNames);
        }

        boolean followEditor = stompFilterModel.isFollowEditor();
        CandidateFilterType candidateFilterType = stompFilterModel.getCandidateFilterType();


        String url = this.endpoint + this.getTestCandidatePaginatedByStompFilterModel + this.sessionURL +
                includedClassNamePart + excludedClassNamePart + includedMethodName + excludedMethodName +
                "&followEditor=" + followEditor + "&candidateFilterType=" + candidateFilterType +
                "&currentAfterEventId=" + currentAfterEventId + "&limit=" + limit;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<TestCandidateBareBone> localTestCandidateBareBone = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.warn("failure getTestCandidatePaginatedByStompFilterModel: ", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        response.close();
                        latch.countDown();
                        return;
                    }
                    List<TestCandidateBareBone> val = objectMapper.readValue(body.byteStream(), new TypeReference<>() {
                    });
                    localTestCandidateBareBone.addAll(val);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localTestCandidateBareBone;

    }

    @Override
    public TestCandidateMetadata getConstructorCandidate(Parameter parameter) {

        // {
        // 	"value": 0,
        // 	"type": null,
        // 	"exception": false,
        // 	"prob": null,
        // 	"names": [],
        // 	"stringValue": "string",
        // 	"index": 0,
        // 	"creatorExpression": null,
        // 	"templateMap": [],
        // 	"isEnum": true,
        // 	"iscontainer": true
        // }


        long value = parameter.getValue();
        String type = parameter.getType();
        boolean exception = parameter.isException();
        String stringValue = parameter.getStringValue();
        int index = parameter.getIndex();
        MethodCallExpression methodCallExpression = parameter.getCreatorExpression();
        boolean isEnum = parameter.getIsEnum();
        boolean isContainer = parameter.isContainer();

        String url = this.endpoint + this.getConstructorCandidate + this.sessionURL +
                "&value=" + value + "&type=" + type + "&exception=" + exception + "&prob=" +
                "&stringValue=" + stringValue + "&index=" + index + "&creatorExpression=" +
                "&isEnum=" + isEnum + "&iscontainer=" + isContainer;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TestCandidateMetadata> localTestCandidateMetadata = new AtomicReference<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    localTestCandidateMetadata.set(objectMapper.readValue(responseBody, TestCandidateMetadata.class));
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return localTestCandidateMetadata.get();

    }

    @Override
    public ExecutionSession getExecutionSession() {

        String url = this.endpoint + this.getExecutionSession + this.sessionURL;
        logger.info("url get execution session = " + url);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicReference<ExecutionSession> executionSession = new AtomicReference<>();
        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    ExecutionSession newValue = objectMapper.readValue(responseBody, ExecutionSession.class);
                    newValue.setSessionMode(ExecutionSessionSourceMode.REMOTE);
                    executionSession.set(newValue);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return executionSession.get();
    }

    @Override
    public synchronized void close() {
        this.shutdown = true;
    }

    @Override
    public void addSessionScanEventListener(SessionScanEventListener listener) {
        return;
    }

    @Override
    public void addTestCandidateListener(NewTestCandidateIdentifiedListener testCandidateListener) {
        return;
    }

    @Override
    public CodeCoverageData createCoverageData() {
        return null;
    }

    @Override
    public ClassMethodAggregates getClassMethodAggregates(String qualifiedName) {
        // TODO: implement this method
        return null;
    }

    @Override
    public UnloggedSdkApiAgentClient getAgent() {
        return this.unloggedSdkApiAgentClient;
    }

    @Override
    public void createParamEnumPropertyTrueIfTheyAre(MethodCallExpression methodCallExpression) {
        List<Parameter> methodArguments = methodCallExpression.getArguments();

        for (Parameter methodArgument : methodArguments) {
            //param is enum then we set it to enum type
            checkAndSetParameterEnumIfYesMakeNameCamelCase(methodArgument);
        }
        // check for the return value type if its enum
        checkAndSetParameterEnumIfYesMakeNameCamelCase(methodCallExpression.getReturnValue());
    }


    private void checkAndSetParameterEnumIfYesMakeNameCamelCase(Parameter param) {
        if (param == null || param.getType() == null)
            return;

        String currParamType = param.getType();

        // TODO: this.classInfoIndexByName.get(currParamType) should be a API
        ClassInfo currClass = this.classInfoIndexByName.get(currParamType);

        if (currClass != null && currClass.isEnum()) {
            param.setIsEnum(true);

            // curr param name converted to camelCase
            List<String> names = param.getNamesList();
            if (names != null && !names.isEmpty()) {
                String modifiedName = StringUtils.convertSnakeCaseToCamelCase(names.get(0));
                names.remove(0);
                names.add(0, modifiedName);
            }
        }
    }

    @Override
    public void unlockNextScan() {
        return;
    }

    @Override
    public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest) {
        // TODO: implement this
        return null;
    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) {
        return null;
    }

    public List<ExecutionSession> sessionDiscovery(String packageName) {
        String url = this.endpoint + this.discovery + "?packageName=" + packageName;
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<ExecutionSession> executionSessionList = new ArrayList<>();

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered", e);
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    List<ExecutionSession> executionSessionLocal = objectMapper.readValue(responseBody,
                            new TypeReference<>() {
                            });
                    executionSessionLocal.forEach(e -> e.setSessionMode(ExecutionSessionSourceMode.REMOTE));
                    executionSessionList.addAll(executionSessionLocal);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {

        }

        return executionSessionList;
    }

}
