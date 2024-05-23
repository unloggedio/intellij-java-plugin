package com.insidious.plugin.upload;

import java.util.ArrayList;
import java.util.List;

import com.insidious.plugin.constants.SessionMode;

public class SourceModel {
	private SessionMode sessionMode;
	private String serverEndpoint;
	private SourceFilter sourceFilter;
	private List<String> sessionId;

	public SourceModel() {
	}

	public String getServerEndpoint() {
		return this.serverEndpoint;
	}

	public void setServerEndpoint(String serverEndpoint) {
		this.serverEndpoint = serverEndpoint;
	}

	public SourceFilter getSourceFilter() {
		return this.sourceFilter;
	}

	public void setSourceFilter(SourceFilter sourceFilter) {
		this.sourceFilter = sourceFilter;
	}

	public List<String> getSessionId() {
		return this.sessionId;
	}

	public void setSessionId(List<String> sessionId) {
		this.sessionId = sessionId;
	}

	public SessionMode getSessionMode() {
		return this.sessionMode;
	}

	public void setSessionMode (SessionMode sessionMode) {
		this.sessionMode = sessionMode;
	}
}
