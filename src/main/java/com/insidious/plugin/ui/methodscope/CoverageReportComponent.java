package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.coverage.CodeCoverageData;
import com.insidious.plugin.coverage.CoverageTreeTableModel;
import com.insidious.plugin.coverage.PackageCoverageData;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CoverageReportComponent {
    private static final Logger logger = LoggerUtil.getInstance(CoverageReportComponent.class);
    private JPanel coveragePercentSummaryBoxPanel;
    private JPanel classPercentBoxPanel;
    private JPanel methodPercentBoxPanel;
    private JPanel linePercentBoxPanel;
    private JPanel branchPercentBoxPanel;
    private JProgressBar classPercentProgressBar;
    private JScrollPane reportTableContainerPanel;
    private JPanel rootPanel;
    private JLabel classPercentLabel;
    private JLabel classCoverageNumbers;
    private JProgressBar methodPercentProgressBar;
    private JLabel methodPercentLabel;
    private JLabel methodCoverageNumbers;
    private JProgressBar linePercentProgressBar;
    private JLabel linePercentLabel;
    private JLabel lineCoverageNumbers;
    private JProgressBar branchPercentProgressBar;
    private JLabel branchPercentLabel;
    private JLabel branchCoverageNumbers;

    public CoverageReportComponent() {

        Border border = reportTableContainerPanel.getBorder();
        CompoundBorder borderWithMargin = BorderFactory.createCompoundBorder(
                border, BorderFactory.createEmptyBorder(5, 5, 0, 5)
        );
        reportTableContainerPanel.setBorder(borderWithMargin);
        branchPercentBoxPanel.setVisible(false);

    }

    public void setCoverageData(CodeCoverageData codeCoverageData) {
        String[] columnNames = new String[]{
                "Element"
                , "Class, %"
                , "Method, %"
                , "Line, %"
//                , "Branch, %"
        };

        List<PackageCoverageData> packageCoverageDataList = codeCoverageData.getPackageCoverageDataList();
        CoverageTreeTableModel treeTableModel = new CoverageTreeTableModel(packageCoverageDataList, columnNames);
        AtomicInteger totalClassCount = new AtomicInteger();
        AtomicInteger coveredClassCount = new AtomicInteger();

        AtomicInteger totalMethodCount = new AtomicInteger();
        AtomicInteger coveredMethodCount = new AtomicInteger();

        AtomicInteger totalLineCount = new AtomicInteger();
        AtomicInteger coveredLineCount = new AtomicInteger();

        AtomicInteger totalBranchCount = new AtomicInteger();
        AtomicInteger coveredBranchCount = new AtomicInteger();

        packageCoverageDataList.stream()
                .flatMap(packageCoverageData -> packageCoverageData.getClassCoverageDataList().stream())
                .flatMap(classCoverageData -> {
                    totalClassCount.incrementAndGet();
                    if (classCoverageData.getCoveredLineCount() > 0) {
                        coveredClassCount.incrementAndGet();
                    }
                    return classCoverageData.getMethodCoverageData().stream();
                }).forEach(methodCoverageData -> {
                    totalMethodCount.incrementAndGet();
                    if (methodCoverageData.getCoveredLineCount() > 0) {
                        coveredMethodCount.incrementAndGet();
                    }
                    totalLineCount.addAndGet(methodCoverageData.getTotalLineCount());
                    coveredLineCount.addAndGet(methodCoverageData.getCoveredLineCount());
                    totalBranchCount.addAndGet(methodCoverageData.getTotalBranchCount());
                    coveredBranchCount.addAndGet(methodCoverageData.getCoveredBranchCount());
                });

        if (totalClassCount.get() > 0) {
            int classCoveragePercent = coveredClassCount.get() * 100 / totalClassCount.get();
            classPercentProgressBar.setValue(classCoveragePercent);
            classPercentLabel.setText(classCoveragePercent + "%");
            classCoverageNumbers.setText(coveredClassCount.get() + "/" + totalClassCount.get());
        } else {
            classPercentProgressBar.setValue(0);
            classPercentLabel.setText("0%");
            classCoverageNumbers.setText("0/0");
        }

        if (totalMethodCount.get() > 0) {
            int methodCoveragePercent = coveredMethodCount.get() * 100 / totalMethodCount.get();
            methodPercentProgressBar.setValue(methodCoveragePercent);
            methodPercentLabel.setText(methodCoveragePercent + "%");
            methodCoverageNumbers.setText(coveredMethodCount.get() + "/" + totalMethodCount.get());
        } else {
            methodPercentProgressBar.setValue(0);
            methodPercentLabel.setText("0%");
            methodCoverageNumbers.setText("0/0");
        }


        if (totalLineCount.get() > 0) {
            int lineCoveragePercent = coveredLineCount.get() * 100 / totalLineCount.get();
            linePercentProgressBar.setValue(lineCoveragePercent);
            linePercentLabel.setText(lineCoveragePercent + "%");
            lineCoverageNumbers.setText(coveredLineCount.get() + "/" + totalLineCount.get());
        } else {
            linePercentProgressBar.setValue(0);
            linePercentLabel.setText("0%");
            lineCoverageNumbers.setText("0/0");
        }



        if (totalBranchCount.get() > 0) {
            int branchCoveragePercent = coveredBranchCount.get() * 100 / totalBranchCount.get();
            branchPercentProgressBar.setValue(branchCoveragePercent);
            branchPercentLabel.setText(branchCoveragePercent + "%");
            branchCoverageNumbers.setText(coveredBranchCount.get() + "/" + totalBranchCount.get());
        } else {
            branchPercentProgressBar.setValue(0);
            branchPercentLabel.setText("0%");
            branchCoverageNumbers.setText("0/0");
        }


        JXTreeTable treeTable = new JXTreeTable(treeTableModel);
        treeTable.createDefaultColumnsFromModel();
        treeTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        treeTable.expandAll();
        treeTable.setRootVisible(false);
        treeTable.setAutoResizeMode(4);
        treeTable.setAutoscrolls(true);
        treeTable.setAutoCreateRowSorter(true);
//        DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
//        treeTable.setTreeCellRenderer(cellRenderer);
        TreeCellRenderer renderer = treeTable.getTreeCellRenderer();
        reportTableContainerPanel.setViewportView(treeTable);
    }

    public JComponent getContent() {
        return rootPanel;
    }
}
