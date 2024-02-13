package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.assertions.AssertionEngine;
import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.intellij.notification.NotificationType;

import javax.swing.*;

public class AssertionBlockManagerListener implements AssertionBlockManager {

    private final Object value;

    public AssertionBlockManagerListener(Object value) {
        this.value = value;
    }

    @Override
    public void addNewRule() {

    }

    @Override
    public void addNewGroup() {

    }

    @Override
    public AssertionResult executeAssertion(AtomicAssertion atomicAssertion) {
        return AssertionEngine.executeAssertions(atomicAssertion, null);
    }

    @Override
    public void deleteAssertionRule(AssertionRule element) {

    }

    @Override
    public void removeAssertionGroup() {
        InsidiousNotification.notifyMessage("Cannot delete this group.", NotificationType.ERROR);
    }

    @Override
    public KeyValue getCurrentTreeKey() {
        return new KeyValue("/", value);
    }

    @Override
    public void removeAssertionGroup(AssertionBlock block) {

    }
}
