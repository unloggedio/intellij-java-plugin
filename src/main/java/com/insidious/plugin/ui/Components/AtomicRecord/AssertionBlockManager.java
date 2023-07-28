package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.KeyValue;

public interface AssertionBlockManager {
    void addNewRule();
    void addNewGroup();
    void deleteAssertionRule(AssertionRule assertionRule);
    void removeAssertionGroup();
    KeyValue getCurrentTreeKey();
    void removeAssertionGroup(AssertionBlock block);
}
