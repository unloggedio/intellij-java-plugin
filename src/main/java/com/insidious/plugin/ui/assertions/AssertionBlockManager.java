package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;

public interface AssertionBlockManager {
    void addNewRule();
    void addNewGroup();

    AssertionResult executeAssertion(AtomicAssertion atomicAssertion);

    void deleteAssertionRule(AssertionRule assertionRule);
    void removeAssertionGroup();
    KeyValue getCurrentTreeKey();
    void removeAssertionGroup(AssertionBlock block);
}
