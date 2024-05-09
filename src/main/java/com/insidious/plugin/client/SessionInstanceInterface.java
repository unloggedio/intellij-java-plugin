package com.insidious.plugin.client;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.dao.MethodDefinition;

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
}
