package com.insidious.plugin.UITests.wrapper;

import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;

public class MockPopupEntry {

    private RemoteText mockName;
    private RemoteText returnTypeText;
    private RemoteText methodName;
    private ComponentFixture panel;
    private ComponentFixture editButton;
    private ComponentFixture checkBox;

    public MockPopupEntry(RemoteText mockName, RemoteText returnTypeText, RemoteText methodName) {
        this.mockName = mockName;
        this.returnTypeText = returnTypeText;
        this.methodName = methodName;
    }

    public RemoteText getMockName() {
        return mockName;
    }

    public RemoteText getReturnTypeText() {
        return returnTypeText;
    }

    public RemoteText getMethodName() {
        return methodName;
    }

    public ComponentFixture getPanel() {
        return panel;
    }

    public void setPanel(ComponentFixture panel) {
        this.panel = panel;
    }

    public String getPanelXpath() {
        return "//div[@accessiblename='" + mockName.getText() + "' and @class='JPanel']";
    }

    public ComponentFixture getEditButton() {
        return editButton;
    }

    public ComponentFixture getCheckBox() {
        return checkBox;
    }
}
