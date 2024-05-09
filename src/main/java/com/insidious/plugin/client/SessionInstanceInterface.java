package com.insidious.plugin.client;

import java.sql.SQLException;
import java.util.List;

import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

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
}
