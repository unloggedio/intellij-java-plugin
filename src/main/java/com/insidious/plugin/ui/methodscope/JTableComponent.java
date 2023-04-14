package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.ui.Components.ResponseMapTable;

import javax.swing.*;

public class JTableComponent {
    private JPanel mainPanel;
    private JTable mainTable;

    public JTableComponent(ResponseMapTable model) {
        this.mainTable.setModel(model);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }
}
