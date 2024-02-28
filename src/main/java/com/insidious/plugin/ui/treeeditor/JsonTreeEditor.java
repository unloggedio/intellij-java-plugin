package com.insidious.plugin.ui.treeeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class JsonTreeEditor {
    private static final Logger logger = LoggerUtil.getInstance(JsonTreeEditor.class);
    private final JsonNode jsonNode;
    private TreeModel treeModel;
    private final Tree valueTree;
    private JPanel mainPanel;
    private JPanel northPanel;
    private JPanel centralPanel;
    private List<OnChangeListener<JsonNode>> listeners = new ArrayList<>();

    public JsonTreeEditor(JsonNode jsonNode, String title) {
        this.jsonNode = jsonNode;
        treeModel = JsonTreeUtils.jsonToTreeModel(jsonNode, title);

        valueTree = new Tree(treeModel);
        valueTree.setCellEditor(new InsidiousCellEditor(valueTree, null));
        valueTree.getModel().addTreeModelListener(new InsidiousTreeListener() {

            @Override
            public void treeNodesChanged(TreeModelEvent e) {

                TreePath path = e.getTreePath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                for (Object child : e.getChildren()) {
                    String stringVal = (String) ((DefaultMutableTreeNode) child).getUserObject();
                    if (stringVal.matches("\\{.*\\}")) {
                        // this is JSON node

                        String jsonData = stringVal;
//                        if (stringVal.length()<=4) {
//                            jsonData = stringVal.substring(2, stringVal.length()-2);
//                        }
//                        else {
//                            jsonData = "json data is small";
//                        }

                        // remove all old children
                        ((DefaultTreeModel) treeModel).removeNodeFromParent((MutableTreeNode) node.getChildAt(0));

                        // make new node and add them as children
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        int baseIndex = treeModel.getIndexOfChild(node.getParent(), node);
                        int count=0;
                        for (java.util.Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                            String key = entry.getKey();
                            JsonElement value = entry.getValue();
                            System.out.println("Key: " + key + ", Value: " + value);

                            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("{\"" + key + "\" : " + value + "}");
                            ((DefaultTreeModel) treeModel).insertNodeInto(newNode, (MutableTreeNode) node, baseIndex+count);
                            count++;
                        }
                    }
                    else {
                        String[] parts = stringVal.split(": ", 2);
                        String key = parts[0];
                        String value = parts[1];
                    }
                }

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
        valueTree.addMouseListener(getMouseListener(valueTree, this));

        int nodeCount = expandAllNodes();
        valueTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                valueTree.startEditingAtPath(valueTree.getSelectionPath());
            }
        });


        centralPanel.add(valueTree, BorderLayout.CENTER);

    }

    private void reload() {
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private static MouseListener getMouseListener(final JTree tree, JsonTreeEditor jsonTreeEditor) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(tree.getRowForLocation(e.getX(),e.getY()) == -1) {
                    tree.clearSelection();
                    jsonTreeEditor.reload();
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
                    jsonTreeEditor.reload();
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
