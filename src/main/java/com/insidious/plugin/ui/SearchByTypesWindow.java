package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SearchByTypesWindow extends SearchByWindowCommon {

    private static final Logger logger = LoggerUtil.getInstance(SearchByTypesWindow.class);
    private final InsidiousService insidiousService;
    DefaultTableModel varsDefaultTableModel, classTypeListTableModel, searchHistoryTableModel;
    DefaultTableCellRenderer centerRenderer;
    private Map<String, Boolean> exceptionMap = new HashMap<>();
    private JPanel mainPanel;
    private JTable resultsTable;
    private JPanel resultContainer;
    private JButton fetchSessionButton;
    private JButton refreshButton;
    private JTable classTypeListTable;
    private JPanel searchControl;
    private JButton addNewTypeButton;
    private JPanel searchContainer;
    private JScrollPane searchConfig;
    private JPanel resultControl;
    private JScrollPane resultList;
    private JTextField typeNameInput;

    public SearchByTypesWindow(Project project, InsidiousService insidiousService) {
        super(new Vector<>(List.of("Matched", "Class", "# Line", "# Thread", "Time")), project);

        this.insidiousService = insidiousService;
        fetchSessionButton.addActionListener(actionEvent -> {
            loadTracePoint(resultsTable.getSelectedRow());
        });

        classTypeListTableModel = new DefaultTableModel() {
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
        addComponentEventListeners();


    }

    private void addComponentEventListeners() {
        refreshButton.addActionListener(actionEvent -> doSearch());
        addNewTypeButton.addActionListener(actionEvent -> checkInputAndSearch());
        typeNameInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    checkInputAndSearch();
                    return;
                }
                super.keyPressed(e);
            }
        });
    }

    private void doSearch() {
//        clearResultsTable();
//        List<String> exceptionClassnameList = exceptionMap.entrySet()
//                .stream()
//                .filter(Map.Entry::getValue)
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toList());
//
//        try {
//            insidiousService.doSearch(SearchQuery.ByType(exceptionClassnameList));
//        } catch (Exception e) {
//            logger.error("failed to load sessions for module", e);
//        }

    }

    private void checkInputAndSearch() {

        String typeName = typeNameInput.getText();

        if (typeName != null && typeName.trim().length() > 0) {
            exceptionMap.put(typeName.trim(), true);
            insidiousService.setExceptionClassList(exceptionMap);
        }

        updateTypeNameTable();

    }

    public JPanel getContent() {
        return mainPanel;
    }


    public void init() {


//        JTableHeader header = this.resultsTable.getTableHeader();
//        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
//        this.resultsTable.setCellEditor(this.resultsTable.getDefaultEditor(Boolean.class));
//
//        centerRenderer = new DefaultTableCellRenderer();
//        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
//        this.resultsTable.setModel(searchResultsTableModel);
//        this.resultsTable.setDefaultRenderer(Object.class, centerRenderer);
//        this.resultsTable.setAutoCreateRowSorter(true);
//
//
//        JTableHeader typeNameTableHeaders = this.classTypeListTable.getTableHeader();
//        typeNameTableHeaders.setFont(new Font("Fira Code", Font.PLAIN, 14));
//
//
//        exceptionMap = insidiousService.getDefaultExceptionClassList();
//        updateTypeNameTable();
//
//
//        classTypeListTable.getModel().addTableModelListener(tableModelEvent -> {
//            if (tableModelEvent.getFirstRow() == -1) {
//                return;
//            }
//            String exceptionClassName = (String) classTypeListTable.getModel().getValueAt(tableModelEvent.getFirstRow(), 0);
//            Boolean isSelected = (Boolean) classTypeListTable.getModel().getValueAt(tableModelEvent.getFirstRow(), 1);
//            exceptionMap.put(exceptionClassName, isSelected);
//        });

    }

    private void updateTypeNameTable() {
        Object[] headers = {"Exception Classname", "Include in search"};

        Set<Map.Entry<String, Boolean>> entries = exceptionMap.entrySet();
        Object[][] errorTypes = new Object[entries.size()][];
        List<String> typeNamesSorted = entries.stream().map(Map.Entry::getKey).sorted().collect(Collectors.toList());

        int i = 0;
        for (String typeName : typeNamesSorted) {
            Object[] errorType = new Object[]{
                    typeName,
                    exceptionMap.get(typeName)
            };
            errorTypes[i] = errorType;
            i++;
        }


        classTypeListTableModel.setDataVector(errorTypes, headers);
        classTypeListTable.setModel(classTypeListTableModel);


    }
}
