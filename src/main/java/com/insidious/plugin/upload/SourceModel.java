package com.insidious.plugin.upload;

import java.util.ArrayList;
import java.util.List;

import com.insidious.plugin.constants.SessionMode;

public class SourceModel {
	private String serverEndpoint;
	private SourceFilter sourceFilter;
	private List<String> selectedSessionId;
	private SessionMode sessionMode; 

	public SourceModel() {
		this.serverEndpoint = "http://localhost:8123";
		this.sourceFilter = SourceFilter.ALL;
		this.selectedSessionId = new ArrayList<>();
		this.sessionMode = SessionMode.REMOTE;
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

	public List<String> getSelectedSessionId() {
		return this.selectedSessionId;
	}

	public void setSelectedSessionId(List<String> selectedSessionId) {
		this.selectedSessionId = selectedSessionId;
	}

	public SessionMode getSessionMode() {
		return this.sessionMode;
	}

	public void setSessionMode (SessionMode sessionMode) {
		this.sessionMode = sessionMode;
	}
}
