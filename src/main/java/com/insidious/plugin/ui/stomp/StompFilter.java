package com.insidious.plugin.ui.stomp;

import javax.swing.*;

public class StompFilter {
    private JPanel mainPanel;
    private FilterModel filterModel;
    private JCheckBox followEditorCheckBox;

    public StompFilter(FilterModel filterModel) {
        this.filterModel = filterModel;
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
