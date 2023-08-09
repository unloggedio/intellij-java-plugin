package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.coverage.CoverageTreeTableModel;
import com.insidious.plugin.coverage.ClassCoverageData;
import com.insidious.plugin.coverage.PackageCoverageData;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CoverageReportComponent {
    private static final Logger logger = LoggerUtil.getInstance(CoverageReportComponent.class);
    private JPanel coveragePercentSummaryBoxPanel;
    private JPanel classPercentBoxPanel;
    private JPanel methodPercentBoxPanel;
    private JPanel linePercentBoxPanel;
    private JPanel branchPercentBoxPanel;
    private JProgressBar progressBar1;
    private JScrollPane reportTableContainerPanel;
    private JPanel rootPanel;

    public CoverageReportComponent() {

//        Border border = reportTableContainerPanel.getBorder();
//        CompoundBorder borderWithMargin = BorderFactory.createCompoundBorder(
//                border, BorderFactory.createEmptyBorder(5, 5, 5, 5)
//        );
//
//
//        reportTableContainerPanel.setBorder(borderWithMargin);
        List<PackageCoverageData> packageCoverageDataList = new ArrayList<>();
        List<ClassCoverageData> package1ClassList = new ArrayList<>();
        package1ClassList.add(new ClassCoverageData("class1", 10, 5, 10, 5, 10, 5));
        package1ClassList.add(new ClassCoverageData("class2", 10, 5, 10, 5, 10, 5));
        package1ClassList.add(new ClassCoverageData("class3", 10, 5, 10, 5, 10, 5));
        packageCoverageDataList.add(new PackageCoverageData("package1", package1ClassList));

        List<ClassCoverageData> package2ClassList = new ArrayList<>();
        package2ClassList.add(new ClassCoverageData("class1", 10, 5, 10, 5, 10, 5));
        package2ClassList.add(new ClassCoverageData("class2", 10, 5, 10, 5, 10, 5));
        package2ClassList.add(new ClassCoverageData("class3", 10, 5, 10, 5, 10, 5));
        packageCoverageDataList.add(new PackageCoverageData("package2", package2ClassList));

        String[] columnNames = new String[]{
                "Element",
                "Class, %",
                "Method, %",
                "Line, %",
                "Branch, %"
        };

        CoverageTreeTableModel treeTableModel = new CoverageTreeTableModel(packageCoverageDataList, columnNames);

        JXTreeTable treeTable = new JXTreeTable(treeTableModel);
        treeTable.createDefaultColumnsFromModel();
        treeTable.expandAll();
        treeTable.setRootVisible(false);
        treeTable.setAutoResizeMode(4);
        treeTable.setAutoscrolls(true);
        treeTable.setAutoCreateRowSorter(true);
        reportTableContainerPanel.setViewportView(treeTable);
    }

    public JComponent getContent() {
        return rootPanel;
    }
}
