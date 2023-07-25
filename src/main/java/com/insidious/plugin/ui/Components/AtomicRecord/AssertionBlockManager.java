package com.insidious.plugin.ui.Components.AtomicRecord;

public interface AssertionBlockManager {
    void addFirstRule();
    void addNewRule();
    void addNewGroup();
    void removeAssertionElement(AssertionElement element);
    void removeAssertionGroup();
    String getCurrentTreeKey();
    void removeAssertionBlock(AssertionBlock block);
}
