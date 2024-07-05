package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.SessionInstanceInterface;
import com.insidious.plugin.ui.methodscope.HighlightedRequest;

public class CurrentState {
    private MethodAdapter currentMethod;
    private SessionInstanceInterface sessionInstance;
    private boolean codeCoverageHighlightEnabled = true;
    private HighlightedRequest currentHighlightedRequest = null;
    private boolean testCaseDesignerWindowAdded = false;
    private ActiveHighlight currentActiveHighlight = null;
    private boolean isAgentServerRunning;

    public ActiveHighlight getCurrentActiveHighlight() {
        return currentActiveHighlight;
    }

    public void setCurrentActiveHighlight(ActiveHighlight currentActiveHighlight) {
        this.currentActiveHighlight = currentActiveHighlight;
    }

    public MethodAdapter getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(MethodAdapter currentMethod) {
        this.currentMethod = currentMethod;
    }

    public SessionInstanceInterface getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstanceInterface sessionInstance) {
        this.sessionInstance = sessionInstance;
        this.isAgentServerRunning = sessionInstance != null;
    }

    public boolean isCodeCoverageHighlightEnabled() {
        return codeCoverageHighlightEnabled;
    }

    public void setCodeCoverageHighlightEnabled(boolean codeCoverageHighlightEnabled) {
        this.codeCoverageHighlightEnabled = codeCoverageHighlightEnabled;
    }

    public HighlightedRequest getCurrentHighlightedRequest() {
        return currentHighlightedRequest;
    }

    public void setCurrentHighlightedRequest(HighlightedRequest currentHighlightedRequest) {
        this.currentHighlightedRequest = currentHighlightedRequest;
    }

    public boolean isTestCaseDesignerWindowAdded() {
        return testCaseDesignerWindowAdded;
    }

    public void setTestCaseDesignerWindowAdded(boolean testCaseDesignerWindowAdded) {
        this.testCaseDesignerWindowAdded = testCaseDesignerWindowAdded;
    }

    public void setAgentServerRunning(boolean isAgentServerRunning) {
        this.isAgentServerRunning = isAgentServerRunning;
    }

    public boolean isAgentServerRunning() {
        return isAgentServerRunning;
    }
}
