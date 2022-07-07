package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class TestCases extends SearchByWindowCommon {
    private JPanel panel1;
    private JTable testcasesTable;
    private static final Logger logger = LoggerUtil.getInstance(SearchByTypesWindow.class);
    private final InsidiousService insidiousService;
    private DefaultTableModel testcaseTableModal;
    private DefaultTableCellRenderer centerRenderer;

    public TestCases(Project project, InsidiousService insidiousService) {
        super(new Vector<>(List.of("Test Candidate", "Class", "Input Data",
                "Return Value", "Time", "Positive", "Negative")), project);
        this.insidiousService = insidiousService;

        testcaseTableModal = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }
                return String.class;
            }
        };

        init();
    }

    private void init() {
        JTableHeader header = this.testcasesTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.testcasesTable.setCellEditor(this.testcasesTable.getDefaultEditor(Boolean.class));

        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.testcasesTable.setModel(testcaseTableModal);
        this.testcasesTable.setDefaultRenderer(Object.class, centerRenderer);
        this.testcasesTable.setAutoCreateRowSorter(true);


    }
}
