package com.insidious.plugin.ui.Components;

import javax.swing.*;

public class Obv3_Documentation_Generic {
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel topPanel;
    private JLabel headingLabel;
    private JPanel centerPanel;
    private JScrollPane scrollParent;
    private JTextArea documentationTextArea;

    public Obv3_Documentation_Generic() {

    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void setContent(String documentationText) {
        this.documentationTextArea.setText(documentationText);
    }
}
