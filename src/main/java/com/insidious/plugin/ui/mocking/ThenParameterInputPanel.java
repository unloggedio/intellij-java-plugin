package com.insidious.plugin.ui.mocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.mocking.MethodExitType;
import com.insidious.plugin.mocking.ThenParameter;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class ThenParameterInputPanel {

    private final static Set<String> baseClassNames = new HashSet<>(Arrays.asList(
            "int",
            "short",
            "byte",
            "char",
            "boolean",
            "float",
            "double",
            "void",
            "long"
    ));

    private static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final ThenParameter thenParameter;
    private final Project project;
    private final Color originalBackgroundColor;
    private JPanel mainPanel;
    private JLabel returnTypeTextField;
    private JLabel returnType;
    private JScrollPane valueScrollPanel;
    private JPanel textAreaScrollParent;

    public ThenParameterInputPanel(ThenParameter thenParameter, Project project) {
        this.project = project;
        this.thenParameter = thenParameter;
        this.originalBackgroundColor = valueScrollPanel.getBackground();
        String simpleClassName = thenParameter.getReturnParameter().getClassName();
        if (simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
        returnTypeTextField.setText(simpleClassName);
        String thenParamValue = thenParameter.getReturnParameter().getValue();
        valueScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        try {
            thenParamValue = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(thenParamValue));
        } catch (JsonProcessingException e) {
            // no pretty print for this value
        }


        try {
            TreeModel tree = JsonTreeUtils.jsonToTreeModel(objectMapper.readTree(thenParamValue), simpleClassName);
            Tree comp = new Tree(tree);
//            comp.setToolTipText(toolTipText);
            comp.setBackground(JBColor.WHITE);
//            comp.setBorder(BorderFactory.createLineBorder(new Color(97, 97, 97, 255)));
            int totalNodeCount = expandAllNodes(comp);

            valueScrollPanel.setViewportView(comp);


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

//        returnValueTextArea.setText(thenParamValue);
//
//        returnValueTextArea.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyReleased(KeyEvent e) {
//                thenParameter.getReturnParameter().setValue(returnValueTextArea.getText());
//                validateValueValid();
//            }
//        });

        returnType.setText(MethodExitType.NORMAL.toString());

//        returnType.setModel(new DefaultComboBoxModel<>(MethodExitType.values()));
//        returnType.setSelectedItem(thenParameter.getMethodExitType());
//        returnType.addActionListener(e -> {
//            MethodExitType selectedItem = (MethodExitType) returnType.getSelectedItem();
//            thenParameter.setMethodExitType(selectedItem);
//            updateVisibleControls();
//        });

        returnTypeTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = returnTypeTextField.getText();
                thenParameter.getReturnParameter().setClassName(text);
                validateTypeValid();
            }
        });
        updateVisibleControls();
        validateTypeValid();
    }

    public int expandAllNodes(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        return 1 + expandAll(tree, new TreePath(root));
    }

    private int expandAll(JTree tree, TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path);
            }
        }
        tree.expandPath(parent);
        return 1 + node.getChildCount();
    }


    public void updateVisibleControls() {
        if (thenParameter.getMethodExitType() == MethodExitType.NULL) {
            returnTypeTextField.setEnabled(false);
            valueScrollPanel.setEnabled(false);
        } else {
            returnTypeTextField.setEnabled(true);
            valueScrollPanel.setEnabled(true);
        }

    }

    public void validateTypeValid() {
        String className = thenParameter.getReturnParameter().getClassName();
        if (baseClassNames.contains(className)) {
            returnTypeTextField.setBackground(originalBackgroundColor);
            return;
        }
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<"));
        }
        if (className.contains("[")) {
            className = className.substring(0, className.indexOf("["));
        }

        String finalClassName = className;
        PsiClass locatedClass = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiClass>) () -> JavaPsiFacade.getInstance(project)
                        .findClass(finalClassName.replace("$", "."), GlobalSearchScope.allScope(project)));
        if (locatedClass == null) {
            returnTypeTextField.setBackground(UIUtils.WARNING_RED);
        } else {
            returnTypeTextField.setBackground(originalBackgroundColor);
        }

    }

    public void validateValueValid() {
        String value = thenParameter.getReturnParameter().getValue();
        try {
            JsonNode jsonNode = objectMapper.readTree(value);
            valueScrollPanel.setBackground(originalBackgroundColor);
        } catch (Exception e) {
            valueScrollPanel.setBackground(UIUtils.WARNING_RED);
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
