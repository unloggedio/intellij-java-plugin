package com.insidious.plugin.ui.treeeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.ui.methodscope.InsidiousCellEditor;
import com.insidious.plugin.ui.methodscope.InsidiousTreeListener;
import com.insidious.plugin.ui.mocking.OnChangeListener;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class JsonTreeEditor {
    private static final Logger logger = LoggerUtil.getInstance(JsonTreeEditor.class);
    private final JsonNode jsonNode;
    private TreeModel treeModel;
    private Tree valueTree;
    private JPanel mainPanel;
    private JPanel northPanel;
    private JPanel centralPanel;
    private JPanel southPanel;
    private JButton editButton;
    private JButton saveJsonModeButton;
    private JButton cancelButton;
    private JButton saveTreeModeButton;
    JTextArea textArea = new JTextArea();
    private List<OnChangeListener<JsonNode>> listeners = new ArrayList<>();
    Boolean editable;

    public JsonTreeEditor(JsonNode jsonNode, String title, Boolean editable) {
        this.jsonNode = jsonNode;
        viewStateButton(true);
        treeModel = JsonTreeUtils.jsonToTreeModel(jsonNode, title);
        this.editable = editable;

        valueTree = new Tree(treeModel);


        valueTree.setCellEditor(new InsidiousCellEditor(valueTree, null));
        valueTree.getModel().addTreeModelListener(new InsidiousTreeListener() {

            @Override
            public void treeNodesChanged(TreeModelEvent e) {

                TreePath path = e.getTreePath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                try {
                    String editedValue = (String) node.getUserObject();
                    // Here, you can further process the edited value if necessary
                    // For example, validate or parse it before saving
                    node.setUserObject(editedValue);
                    notifyListeners();
                } catch (Exception ex) {
                    logger.error("Failed to read value", ex);
                    InsidiousNotification.notifyMessage("Failed to read value, please report this on github",
                            NotificationType.ERROR);
                }
            }
        });

        valueTree.setBackground(JBColor.WHITE);
        valueTree.setUI(new BasicTreeUI());
//            valueTree.setBorder(BorderFactory.createLineBorder(new Color(97, 97, 97, 255)));
        valueTree.setInvokesStopCellEditing(true);
        valueTree.addMouseListener(getMouseListener(valueTree));

        int nodeCount = expandAllNodes();
        valueTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                valueTree.startEditingAtPath(valueTree.getSelectionPath());
            }
        });


        // JTP is not editable
        if (!this.editable) {
            valueTree.setEditable(false);
            mainPanel.removeAll();
            mainPanel.add(northPanel);
            mainPanel.add(centralPanel);
            mainPanel.revalidate();
        }

        centralPanel.add(valueTree, BorderLayout.CENTER);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralPanel.removeAll();
                String dataVal = getValue().toString();
                JSONObject dataJson = new JSONObject(dataVal);
                textArea.setText(dataJson.toString(4));
                centralPanel.add(textArea, BorderLayout.CENTER);
                viewStateButton(false);
                centralPanel.revalidate();
                centralPanel.repaint();
            }
        });

        saveJsonModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralPanel.removeAll();
                ObjectMapper objectMapper = new ObjectMapper();
                String textAreaData = textArea.getText();
                JsonNode jsonNodeNew = null;
                try {
                    jsonNodeNew = objectMapper.readTree(textAreaData);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                treeModel = JsonTreeUtils.jsonToTreeModel(jsonNodeNew, title);
                valueTree = new Tree(treeModel);
                valueTree.setEditable(true);
                expandAllNodes();
                viewStateButton(true);
                centralPanel.add(valueTree, BorderLayout.CENTER);
                centralPanel.revalidate();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralPanel.removeAll();
                treeModel = JsonTreeUtils.jsonToTreeModel(jsonNode, title);
                valueTree = new Tree(treeModel);
                valueTree.setEditable(true);
                expandAllNodes();
                viewStateButton(true);
                centralPanel.add(valueTree, BorderLayout.CENTER);
                centralPanel.revalidate();
            }
        });
        saveTreeModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centralPanel.removeAll();
                ObjectMapper objectMapper = new ObjectMapper();
                String dataVal = getValue().toString();
                JsonNode jsonNodeNew = null;
                try {
                    jsonNodeNew = objectMapper.readTree(dataVal);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                treeModel = JsonTreeUtils.jsonToTreeModel(jsonNodeNew, title);
                valueTree = new Tree(treeModel);
                valueTree.setEditable(true);
                expandAllNodes();
                centralPanel.add(valueTree, BorderLayout.CENTER);
                centralPanel.revalidate();
                expandAllNodes();
            }
        });
    }

    public void viewStateButton(Boolean viewState){
        if (viewState) {
            saveTreeModeButton.setVisible(true);
            editButton.setVisible(true);
            saveJsonModeButton.setVisible(false);
            cancelButton.setVisible(false);
        }
        else {
            saveTreeModeButton.setVisible(false);
            editButton.setVisible(false);
            saveJsonModeButton.setVisible(true);
            cancelButton.setVisible(true);
        }
    }

    private static MouseListener getMouseListener(final JTree tree) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(tree.getRowForLocation(e.getX(),e.getY()) == -1) {
                    tree.clearSelection();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {
                if(tree.getRowForLocation(e.getX(),e.getY()) == -1) {
                    tree.clearSelection();
                }
            }
        };
    }

    private void notifyListeners() {
        for (OnChangeListener<JsonNode> listener : listeners) {
            listener.onChange(JsonTreeUtils.treeModelToJson(treeModel));
        }

    }

    public int expandAllNodes() {
        TreeNode root = (TreeNode) valueTree.getModel().getRoot();
        return 1 + expandAll(valueTree, new TreePath(root));
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

    public Component getContent() {
        return mainPanel;
    }

    public JsonNode getValue() {
        return JsonTreeUtils.treeModelToJson(treeModel);
    }

    public void addChangeListener(OnChangeListener<JsonNode> onChangeListener) {
        this.listeners.add(onChangeListener);
    }

    public void setEditable(Boolean editState) {
        valueTree.setEditable(editState);
    }
}
