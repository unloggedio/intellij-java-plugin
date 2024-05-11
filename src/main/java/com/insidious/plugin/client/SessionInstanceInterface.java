package com.insidious.plugin.client;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.agent.UnloggedSdkApiAgentClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
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
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.intellij.openapi.project.Project;

public interface SessionInstanceInterface {
    public boolean isScanEnable();
	public TypeInfo getTypeInfo(String name);
	public TypeInfo getTypeInfo(Integer typeId);
	public int getTotalFileCount();
    public List<UnloggedTimingTag> getTimingTags(long id);
	public List<TestCandidateMetadata> getTestCandidatesForAllMethod(CandidateSearchQuery candidateSearchQuery);
	public TestCandidateMetadata getTestCandidateById(Long testCandidateId, boolean loadCalls);
	public List<TestCandidateMetadata> getTestCandidateBetween(long eventId, long eventId1) throws SQLException;
	public List<TestCandidateMethodAggregate> getTestCandidateAggregatesByClassName(String className);
	public int getProcessedFileCount();
	public MethodDefinition getMethodDefinition(MethodUnderTest methodUnderTest1);
	public List<MethodCallExpression> getMethodCallsBetween(long start, long end);
	public List<MethodCallExpression> getMethodCallExpressions(CandidateSearchQuery candidateSearchQuery);
	public int getMethodCallCountBetween(long start, long end);
	public ClassWeaveInfo getClassWeaveInfo();
    public Map<String, ClassInfo> getClassIndex();
	public List<TypeInfoDocument> getAllTypes();
	public void getTestCandidates(Consumer<List<TestCandidateBareBone>> testCandidateReceiver, long afterEventId, StompFilterModel stompFilterModel, AtomicInteger cdl);
	public boolean isConnected();
	public List<TestCandidateBareBone> getTestCandidatePaginatedByStompFilterModel(StompFilterModel stompFilterModel,
																				   long currentAfterEventId,
																				   int limit);
	public TestCandidateMetadata getConstructorCandidate(Parameter parameter) throws Exception;
	public ExecutionSession getExecutionSession();
	public void close();
	public void addSessionScanEventListener(SessionScanEventListener listener);
	public void addTestCandidateListener(NewTestCandidateIdentifiedListener testCandidateListener);
	public Project getProject();
	public CodeCoverageData createCoverageData();
	public ClassMethodAggregates getClassMethodAggregates(String qualifiedName);
	public UnloggedSdkApiAgentClient getAgent();
	public void createParamEnumPropertyTrueIfTheyAre(MethodCallExpression methodCallExpression);
	public void unlockNextScan();
	public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest);
	public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest);
}
