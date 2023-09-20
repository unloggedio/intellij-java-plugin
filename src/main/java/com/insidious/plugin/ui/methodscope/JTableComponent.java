package com.insidious.plugin.ui.methodscope;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class JTableComponent {
    private JPanel mainPanel;
    private JTable mainTable;

    public JTableComponent(ResponseMapTable model) {
        this.mainTable.setModel(model);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void setBorderTitle(String title)
    {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(title);
    }
}
