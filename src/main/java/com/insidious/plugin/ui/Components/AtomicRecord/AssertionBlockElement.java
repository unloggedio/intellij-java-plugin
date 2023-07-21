package com.insidious.plugin.ui.Components.AtomicRecord;

import javax.swing.*;

public class AssertionBlockElement {
    public enum AssertionBlockElementType {CONNECTOR,RULE}
    private AssertionBlockElementType elementType;
    private String connector;
    private AssertionBlockModel assertionBlockModel;
    private final JPanel panel;

    public AssertionBlockElement(AssertionBlockElementType elementType, String connector,
                                 AssertionBlockModel assertionBlockModel,
                                 JPanel panel) {
        this.elementType = elementType;
        this.connector = connector;
        this.assertionBlockModel = assertionBlockModel;
        this.panel = panel;
    }

    public AssertionBlockElementType getElementType() {
        return elementType;
    }

    public void setElementType(AssertionBlockElementType elementType) {
        this.elementType = elementType;
    }

    public String getConnector() {
        return connector;
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    public AssertionBlockModel getAssertionBlockModel() {
        return assertionBlockModel;
    }

    public void setAssertionBlockModel(AssertionBlockModel assertionBlockModel) {
        this.assertionBlockModel = assertionBlockModel;
    }

    @Override
    public String toString() {
        return "AssertionBlockElement{" +
                "elementType=" + elementType +
                ", connector='" + connector  +
                ", assertionBlockModel=" + assertionBlockModel +
                '}';
    }

    public JPanel getPanel() {
        return panel;
    }
}
