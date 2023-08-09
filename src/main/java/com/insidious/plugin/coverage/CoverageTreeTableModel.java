package com.insidious.plugin.coverage;


import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public class CoverageTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    private final String[] columnNames;
    protected List<PackageCoverageData> candidateRefactoringGroups;

    public CoverageTreeTableModel(List<PackageCoverageData> candidateRefactoringGroups,
                                  String[] columnNames) {
        super(new DefaultMutableTreeNode("root"));
        this.candidateRefactoringGroups = candidateRefactoringGroups;
        this.columnNames = columnNames;
    }

    public List<PackageCoverageData> getCandidateRefactoringGroups() {
        return this.candidateRefactoringGroups;
    }

    public void setCandidateRefactoringGroups(List<PackageCoverageData> candidateRefactoringGroups) {
        this.candidateRefactoringGroups = candidateRefactoringGroups;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public int getHierarchicalColumn() {
        return 0;
    }

    @Override
    public Class getColumnClass(int column) {
        if (column == 0) {
            return TreeTableModel.class;
        }
        return String.class;
    }

    @Override
    public Object getValueAt(Object o, int index) {
        if (o instanceof PackageCoverageData) {
            PackageCoverageData packageCoverageData = (PackageCoverageData) o;
            if (index == 0) {
                return packageCoverageData.getPackageName();
            }
            return "";
        }
        if (o instanceof ClassCoverageData) {
            ClassCoverageData ccd = (ClassCoverageData) o;
            if (index == 0) {
                return ccd.getClassName();
            }
            if (index == 1) {
                return "100% (1/1)";
            }
            if (index == 2) {
                return formatAsPercent(ccd.getCoveredLineCount(), ccd.getTotalLineCount());
            }
            if (index == 3) {
                return formatAsPercent(ccd.getCoveredMethodCount(), ccd.getTotalMethodCount());
            }
            if (index == 4) {
                return formatAsPercent(ccd.getCoveredBranchCount(), ccd.getTotalBranchCount());
            }
        }
        return "";
    }

    private String formatAsPercent(int numerator, int denominator) {
        return (numerator * 100 / denominator) + "% (" + numerator + "/" + denominator + ")";
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    @Override
    public void setValueAt(Object aValue, Object node, int column) {
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof ClassCoverageData;
    }

    public List<?> getChildren(Object parent) {
        if (parent instanceof PackageCoverageData) {
            PackageCoverageData group = (PackageCoverageData) parent;
            return group.getClassCoverageDataList();
        }

        if (parent instanceof ClassCoverageData) {
            return null;
        }

        return candidateRefactoringGroups;
    }

    @Override
    public Object getChild(Object parent, int index) {
        List<?> children = getChildren(parent);
        if (children != null) {
            return children.get(index);
        }

        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        List<?> children = getChildren(parent);
        if (children != null) {
            return children.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        List<?> children = getChildren(parent);
        if (children != null) {
            return children.indexOf(child);
        } else {
            return -1;
        }
    }
}