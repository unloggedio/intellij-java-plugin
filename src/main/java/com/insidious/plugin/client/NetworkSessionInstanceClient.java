package com.insidious.plugin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.TypeInfoClient.TypeInfoClientDeserializer;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;

import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class NetworkSessionInstanceClient implements SessionInstanceInterface {

    private final Logger logger = LoggerUtil.getInstance(NetworkSessionInstanceClient.class);

	// client attributes
    private String endpoint;
    private String token;
	private OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	// endpoint attributes
	private String isScanEnableEndpoint = "/isScanEnable";
	private String getTypeInfoTypeString = "/getTypeInfoTypeString";
	private String getTypeInfoTypeInt = "/getTypeInfoTypeInt";
	private String getTotalFileCount = "/getTotalFileCount";
	private String getTimingTags = "/getTimingTags";
	private String getTestCandidatesForAllMethod = "/getTestCandidatesForAllMethod";
	private String getTestCandidateById = "/getTestCandidateById";

	// session instance attributes
    private String sessionId = "0";
	private boolean scanEnable;
	private TypeInfo typeInfo;
	private int totalFileCount;
	private List<UnloggedTimingTag> unloggedTimingTags;
	private List<TestCandidateMetadata> localtcml;
	private TestCandidateMetadata testCandidateMetadata;

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

}
