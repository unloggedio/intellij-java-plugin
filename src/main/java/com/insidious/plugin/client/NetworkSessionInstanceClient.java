package com.insidious.plugin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.TypeInfoClient.TypeInfoClientDeserializer;
import com.insidious.plugin.client.TypeInfoDocumentClient.TypeInfoDocumentClientDeserializer;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.dao.MethodDefinition;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;

import okhttp3.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class NetworkSessionInstanceClient implements SessionInstanceInterface {

    private final Logger logger = LoggerUtil.getInstance(NetworkSessionInstanceClient.class);

	// client attributes
    private String endpoint;
    private String token;
	private OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	// endpoint attributes
	private String ping = "/ping";
	private String isScanEnableEndpoint = "/isScanEnable";
	private String getTypeInfoTypeString = "/getTypeInfoTypeString";
	private String getTypeInfoTypeInt = "/getTypeInfoTypeInt";
	private String getTotalFileCount = "/getTotalFileCount";
	private String getTimingTags = "/getTimingTags";
	private String getTestCandidatesForAllMethod = "/getTestCandidatesForAllMethod";
	private String getTestCandidateById = "/getTestCandidateById";
	private String getTestCandidateBetween = "/getTestCandidateBetween";
	private String getTestCandidateAggregatesByClassName = "/getTestCandidateAggregatesByClassName";
	private String getProcessedFileCount = "/getProcessedFileCount";
	private String getMethodDefinition = "/getMethodDefinition";
	private String getMethodCallsBetween = "/getMethodCallsBetween";
	private String getMethodCallExpressions = "/getMethodCallExpressions";
	private String getMethodCallCountBetween = "/getMethodCallCountBetween";
	private String getInitTimestamp = "/getInitTimestamp";
	private String getClassWeaveInfo = "/getClassWeaveInfo";
	private String getClassIndex = "/getClassIndex";
	private String getAllTypes = "/getAllTypes";
    private String getTestCandidatePaginatedByStompFilterModel = "/getTestCandidatePaginatedByStompFilterModel";

	// session instance attributes
	private boolean isConnected = false;
    private String sessionId = "0";
	private boolean scanEnable;
	private TypeInfo typeInfo;
	private int totalFileCount;
	private List<UnloggedTimingTag> unloggedTimingTags;
	private List<TestCandidateMetadata> localtcml;
	private List<TestCandidateMethodAggregate> localtcma;
	private List<MethodCallExpression> localMethodCallExpression;
	private TestCandidateMetadata testCandidateMetadata;
	private Integer processedFileCount;
	private MethodDefinition localMethodDefinition;
	private int methodCallCount;
	private long initTimestamp;
	private ClassWeaveInfo classWeaveInfo;
	private Map<String, ClassInfo> classIndex;
	private List<TypeInfoDocument> listTypeInfoDocument;
    private List<TestCandidateBareBone> localTestCandidateBareBone;

    public NetworkSessionInstanceClient(String endpoint) {
        this.endpoint = endpoint;
        this.client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    private void get(String url, Callback callback) {
        Request.Builder builder = new Request.Builder()
                .url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder = builder.addHeader("Authorization", "Bearer " + token);
        }
        Request request = builder.build();
        Call call = this.client.newCall(request);
        call.enqueue(callback);

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            String dots = "";
            while (true) {
                try {
                    Thread.sleep(500);
                    if (call.isExecuted()) {
                        break;
                    }

                    dots = dots + ".";
                    if (dots.length() > 3) {
                        dots = ".";
                    }
                    ProgressIndicatorProvider.getGlobalProgressIndicator()
                            .setText2("Query is in progress " + dots);
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                            .isCanceled()) {
                        throw new ProcessCanceledException();
                    }

                } catch (InterruptedException e) {
                    throw new ProcessCanceledException(e);
                }
            }
        }

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
        
		String url = this.endpoint + this.isScanEnableEndpoint + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Boolean> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Boolean>>() {});
                    scanEnable = jsonVal.get("scanEnable");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return scanEnable;
    }

	@Override
    public TypeInfo getTypeInfo(String name) {
        
		String url = this.endpoint + this.getTypeInfoTypeString + "?sessionId=" + this.sessionId + "&name=" + name;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					SimpleModule module = new SimpleModule();
					module.addDeserializer(TypeInfoClient.class, new TypeInfoClientDeserializer());
					objectMapper.registerModule(module);

					String responseBody = Objects.requireNonNull(response.body()).string();
					TypeInfoClient typeInfoClient = objectMapper.readValue(responseBody, TypeInfoClient.class);
					typeInfo = typeInfoClient.getTypeInfo();
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return typeInfo;
    }

	@Override
    public TypeInfo getTypeInfo(Integer typeId) {
        
		String url = this.endpoint + this.getTypeInfoTypeInt + "?sessionId=" + this.sessionId + "&typeId=" + typeId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					SimpleModule module = new SimpleModule();
					module.addDeserializer(TypeInfoClient.class, new TypeInfoClientDeserializer());
					objectMapper.registerModule(module);

					String responseBody = Objects.requireNonNull(response.body()).string();
					TypeInfoClient typeInfoClient = objectMapper.readValue(responseBody, TypeInfoClient.class);
					typeInfo = typeInfoClient.getTypeInfo();
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return typeInfo;
    }

	@Override
    public int getTotalFileCount() {
        
		String url = this.endpoint + this.getTotalFileCount + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Integer> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Integer>>() {});
                    totalFileCount = jsonVal.get("totalFileCount");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return totalFileCount;
    }

	@Override
    public List<UnloggedTimingTag> getTimingTags(long id) {
        
		String url = this.endpoint + this.getTimingTags + "?sessionId=" + this.sessionId + "&id=" + id;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					// define unloggedTimingTags
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					UnloggedTimingTag[] UnloggedTimingTagClientList = objectMapper.readValue(responseBody, UnloggedTimingTag[].class);
					unloggedTimingTags = new ArrayList<>();
					for (int i=0;i<=UnloggedTimingTagClientList.length-1;i++) {
						unloggedTimingTags.add(UnloggedTimingTagClientList[i]);
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
		for (int i=0;i<=interfaceNames.size()-1;i++) {
			interfaceDataString += "&interfaceNames=" + interfaceNames.get(i);
		}

		String url = this.endpoint + this.getTestCandidatesForAllMethod + "?sessionId=" + this.sessionId + "&loadCalls=" + loadCalls + interfaceDataString + "&argumentsDescriptor=" + argumentsDescriptor + "&candidateFilterType=" + candidateFilterType + "&methodSignature=" + methodSignature + "&className=" + className + "&methodName=" + methodName;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					TestCandidateMetadata[] val = objectMapper.readValue(responseBody, TestCandidateMetadata[].class);

					localtcml = new ArrayList<>();
					for (int i=0;i<=val.length-1;i++) {
						localtcml.add(val[i]);
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localtcml;
	}

	@Override
	public TestCandidateMetadata getTestCandidateById(Long testCandidateId, boolean loadCalls) {

		String url = this.endpoint + this.getTestCandidateById + "?sessionId=" + this.sessionId + "&testCandidateId=" + testCandidateId + "&loadCalls=" + loadCalls;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					testCandidateMetadata = objectMapper.readValue(responseBody, TestCandidateMetadata.class);
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return testCandidateMetadata;
	}

	@Override
	public List<TestCandidateMetadata> getTestCandidateBetween(long eventIdStart, long eventIdEnd) throws SQLException {

		String url = this.endpoint + this.getTestCandidateBetween + "?sessionId=" + this.sessionId + "&eventIdStart=" + eventIdStart + "&eventIdEnd=" + eventIdEnd;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					TestCandidateMetadata[] val = objectMapper.readValue(responseBody, TestCandidateMetadata[].class);

					localtcml = new ArrayList<>();
					for (int i=0;i<=val.length-1;i++) {
						localtcml.add(val[i]);
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localtcml;
	}

	@Override
	public List<TestCandidateMethodAggregate> getTestCandidateAggregatesByClassName(String className) {

		String url = this.endpoint + this.getTestCandidateAggregatesByClassName + "?sessionId=" + this.sessionId + "&className=" + className;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					TestCandidateMethodAggregate[] val = objectMapper.readValue(responseBody, TestCandidateMethodAggregate[].class);

					localtcma = new ArrayList<>();
					for (int i=0;i<=val.length-1;i++) {
						localtcma.add(val[i]);
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localtcma;
	}

	@Override
	public int getProcessedFileCount() {

		String url = this.endpoint + this.getProcessedFileCount + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Integer> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Integer>>() {});
                    processedFileCount = jsonVal.get("processedFileCount");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return processedFileCount;
	}

	@Override
	public MethodDefinition getMethodDefinition(MethodUnderTest methodUnderTest) {

		String name = methodUnderTest.getName();
		String signature = methodUnderTest.getSignature();
		String className = methodUnderTest.getClassName();
		int methodHash = methodUnderTest.getMethodHash();

		String url = this.endpoint + this.getMethodDefinition + "?sessionId=" + this.sessionId + "&name=" + name + "&signature=" + signature + "&className=" + className + "&methodHash=" + methodHash;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					localMethodDefinition = objectMapper.readValue(responseBody, MethodDefinition.class);
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localMethodDefinition;
	}

	@Override
	public List<MethodCallExpression> getMethodCallsBetween(long start, long end) {

		String url = this.endpoint + this.getMethodCallsBetween + "?sessionId=" + this.sessionId + "&start=" + start + "&end=" + end;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					MethodCallExpression[] val = objectMapper.readValue(responseBody, MethodCallExpression[].class);

					localMethodCallExpression = new ArrayList<>();
					for (int i=0;i<=val.length-1;i++) {
						localMethodCallExpression.add(val[i]);
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localMethodCallExpression;
	}

	@Override
	public List<MethodCallExpression> getMethodCallExpressions(CandidateSearchQuery candidateSearchQuery){

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
		for (int i=0;i<=interfaceNames.size()-1;i++) {
			interfaceDataString += "&interfaceNames=" + interfaceNames.get(i);
		}
		
		String url = this.endpoint + this.getMethodCallExpressions + "?sessionId=" + this.sessionId + "&loadCalls=" + loadCalls + interfaceDataString + "&argumentsDescriptor=" + argumentsDescriptor + "&candidateFilterType=" + candidateFilterType + "&methodSignature=" + methodSignature + "&className=" + className + "&methodName=" + methodName;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					MethodCallExpression[] val = objectMapper.readValue(responseBody, MethodCallExpression[].class);

					localMethodCallExpression = new ArrayList<>();
					for (int i=0;i<=val.length-1;i++) {
						localMethodCallExpression.add(val[i]);
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localMethodCallExpression;
	}

	@Override
	public int getMethodCallCountBetween(long start, long end) {

		String url = this.endpoint + this.getMethodCallCountBetween + "?sessionId=" + this.sessionId + "&start=" + start + "&end=" + end;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Integer> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Integer>>() {});
                    methodCallCount = jsonVal.get("methodCallCountBetween");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return methodCallCount;
	}

	public long getInitTimestamp() {

		String url = this.endpoint + this.getInitTimestamp + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Long> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Long>>() {});
                    initTimestamp = jsonVal.get("initTimestamp");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return initTimestamp;
	}

	@Override
	public ClassWeaveInfo getClassWeaveInfo() {

		String url = this.endpoint + this.getClassWeaveInfo + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					classWeaveInfo = objectMapper.readValue(responseBody, ClassWeaveInfo.class);
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return classWeaveInfo;
	}


	@Override
	public Map<String, ClassInfo> getClassIndex() {

		String url = this.endpoint + this.getClassIndex + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					classIndex = objectMapper.readValue(responseBody, new TypeReference<Map<String, ClassInfo>>() {});
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return classIndex;
	}

	@Override
	public List<TypeInfoDocument> getAllTypes() {

		String url = this.endpoint + this.getAllTypes + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					SimpleModule module = new SimpleModule();
					module.addDeserializer(TypeInfoDocumentClient.class, new TypeInfoDocumentClientDeserializer());
					objectMapper.registerModule(module);

					String responseBody = Objects.requireNonNull(response.body()).string();
					TypeInfoDocumentClient[] val = objectMapper.readValue(responseBody, TypeInfoDocumentClient[].class);
					listTypeInfoDocument = new ArrayList<>();
					for (int i=0;i<=val.length-1;i++) {
						listTypeInfoDocument.add(val[i].getTypeInfoDocument());
					}
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Boolean> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Boolean>>() {});
                    isConnected = jsonVal.get("status");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

		try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return isConnected;
	}

	@Override
	public void getTestCandidates (Consumer<List<TestCandidateBareBone>> testCandidateReceiver, long afterEventId, StompFilterModel stompFilterModel, AtomicInteger cdl) {

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
            if (testCandidateMetadataList.size() > 0) {
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
        String includedClassNamePart = "";
        Set<String> includedClassNames = stompFilterModel.getIncludedClassNames();
        for (String localIncludedClassName: includedClassNames) {
            includedClassNamePart += "&includedClassNames=" + localIncludedClassName;
        }

        String excludedClassNamePart = "";
        Set<String> excludedClassNames = stompFilterModel.getExcludedClassNames();
        for (String localExcludedClassName: excludedClassNames) {
            excludedClassNamePart += "&excludedClassNames" + localExcludedClassName;
        }

        String includedMethodName = "";
        Set<String> includedMethodNames = stompFilterModel.getIncludedMethodNames();
        for (String localIncludedMethodName: includedMethodNames) {
            includedMethodName += "&includedMethodNames=" + localIncludedMethodName;
        }

        String excludedMethodName = "";
        Set<String> excludedMethodNames = stompFilterModel.getExcludedMethodNames();
        for (String localExcludedMethodNames: excludedMethodNames) {
            excludedMethodName += "&excludedMethodNames=" + localExcludedMethodNames;
        }

        Boolean followEditor = stompFilterModel.isFollowEditor();
        CandidateFilterType candidateFilterType = stompFilterModel.getCandidateFilterType();


        String url = this.endpoint + this.getTestCandidatePaginatedByStompFilterModel + "?sessionId=" + this.sessionId +
                includedClassNamePart + excludedClassNamePart + includedMethodName + excludedMethodName +
                "&followEditor=" + followEditor + "&candidateFilterType=" + candidateFilterType +
                "&currentAfterEventId=" + currentAfterEventId + "&limit=" + limit;
        System.out.println("url = " + url);
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    SimpleModule module = new SimpleModule();
                    module.addDeserializer(TypeInfoDocumentClient.class, new TypeInfoDocumentClientDeserializer());
                    objectMapper.registerModule(module);

                    String responseBody = Objects.requireNonNull(response.body()).string();
                    TestCandidateBareBone[] val = objectMapper.readValue(responseBody, TestCandidateBareBone[].class);
                    localTestCandidateBareBone = new ArrayList<>();
                    for (int i=0;i<=val.length-1;i++) {
                        localTestCandidateBareBone.add(val[i]);
                    }
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return localTestCandidateBareBone;

    }


//    @Override
//    public ExecutionSession getExecutionSession() {
//        // TODO: implement
//        // TODO: test
//    }
//
//    @Override
//    public TestCandidateMetadata getConstructorCandidate(Parameter parameter) throws Exception {
//        // TODO: implement
//        // TODO: test
//    }
}
