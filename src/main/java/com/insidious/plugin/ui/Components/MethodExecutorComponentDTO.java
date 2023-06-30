package com.insidious.plugin.ui.Components;

import com.insidious.plugin.ui.methodscope.TestCandidateListedItemComponent;

import javax.swing.*;
import java.util.Map;

public class MethodExecutorComponentDTO {
    private int panelHeight;
    private Map<Long, TestCandidateListedItemComponent> componentMap;
    private JScrollPane scrollPane;

    public MethodExecutorComponentDTO(Map<Long, TestCandidateListedItemComponent> componentMap,
                                      JScrollPane scrollPane, int panelHeight) {
        this.componentMap = componentMap;
        this.scrollPane = scrollPane;
        this.panelHeight = panelHeight;
    }

    public Map<Long, TestCandidateListedItemComponent> getComponentMap() {
        return componentMap;
    }


    public JScrollPane getMainPanel() {
        return scrollPane;
    }

    public void setMainPanel(JScrollPane mainPanel) {
        this.scrollPane = mainPanel;
    }

    public int getPanelHeight() {
        return this.panelHeight;
    }
}
