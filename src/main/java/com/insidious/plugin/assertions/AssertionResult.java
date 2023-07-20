package com.insidious.plugin.assertions;

import java.util.HashMap;
import java.util.Map;

public class AssertionResult {
    private final Map<AtomicAssertion, Boolean> results = new HashMap<>();
    private boolean passing;

    public void addResult(AtomicAssertion assertion, boolean result) {
        this.results.put(assertion, result);
    }

    public Map<AtomicAssertion, Boolean> getResults() {
        return results;
    }

    public boolean isPassing() {
        return passing;
    }

    public void setPassing(boolean finalResult) {
        this.passing = finalResult;
    }
}
