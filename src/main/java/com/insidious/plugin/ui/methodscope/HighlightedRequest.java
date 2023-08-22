package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.pojo.atomic.MethodUnderTest;

import java.util.Objects;
import java.util.Set;

public class HighlightedRequest {
    private final MethodUnderTest methodUnderTest;
    private final Set<Integer> linesToHighlight;

    public HighlightedRequest(MethodUnderTest methodUnderTest, Set<Integer> linesToHighlight) {

        this.methodUnderTest = methodUnderTest;
        this.linesToHighlight = linesToHighlight;
    }

    public MethodUnderTest getMethodUnderTest() {
        return methodUnderTest;
    }

    public Set<Integer> getLinesToHighlight() {
        return linesToHighlight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HighlightedRequest that = (HighlightedRequest) o;
        return methodUnderTest.equals(that.methodUnderTest) && linesToHighlight.equals(that.linesToHighlight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodUnderTest, linesToHighlight);
    }
}
