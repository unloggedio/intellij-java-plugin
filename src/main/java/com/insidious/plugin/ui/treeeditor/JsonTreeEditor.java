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
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

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
    private final static ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final JsonNode jsonNode;
    Editor jsonTextEditor;
    Boolean editable;
    private TreeModel treeModel;
    private Tree valueTree;
    private JPanel mainPanel;
    private JPanel dataPanel;
    private JPanel actionPanelMain;
    private JPanel actionPanel;
    private List<AnAction> listAnAction;
    private AnAction editAction;
    private AnAction cancelAction;
    private AnAction saveAction;
    private AnAction buildJsonAction;
    private List<OnChangeListener<JsonNode>> listeners = new ArrayList<>();

    public JsonTreeEditor(JsonNode jsonNode, String title, Boolean editable, AnAction... otherAction) {
        this.jsonNode = jsonNode;
        this.listAnAction = List.of(otherAction);


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
            mainPanel.add(dataPanel);
            mainPanel.revalidate();
        }
        dataPanel.add(valueTree, BorderLayout.CENTER);

        this.editAction = new AnAction(() -> "Edit", UIUtils.EDIT) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                dataPanel.removeAll();
                String dataVal = getValue().toString();
                JsonNode dataJson = null;
                try {
                    dataJson = objectMapper.readTree(dataVal);
                } catch (JsonProcessingException ex) {
                    dataJson = objectMapper.getNodeFactory().textNode(dataVal);

                }

                if (jsonTextEditor != null) {
//                    Disposer.dispose(jsonTextEditor);
                    jsonTextEditor = null;
                }

                String valueAsString;
                try {
                    valueAsString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataJson);
                } catch (JsonProcessingException ex) {
                    valueAsString = dataJson.toString();
                }

                @NotNull Document jsonTextDocument = EditorFactory.getInstance()
                        .createDocument(valueAsString);

                jsonTextEditor = EditorFactory.getInstance().createEditor(jsonTextDocument);



                dataPanel.add(jsonTextEditor.getComponent(), BorderLayout.CENTER);
                viewStateButton(false);
                dataPanel.revalidate();
                dataPanel.repaint();
            }
        };

        this.saveAction = new AnAction(() -> "Build", UIUtils.SAVE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                dataPanel.removeAll();
                ObjectMapper objectMapper = new ObjectMapper();
                String textAreaData = jsonTextEditor.getDocument().getText();
                JsonNode jsonNodeNew = null;
                try {
                    jsonNodeNew = objectMapper.readTree(textAreaData);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                treeModel = JsonTreeUtils.jsonToTreeModel(jsonNodeNew, title);
                valueTree = new Tree(treeModel);
                valueTree.setEditable(true);
                valueTree.setCellEditor(new InsidiousCellEditor(valueTree, null));
                expandAllNodes();
                viewStateButton(true);
                dataPanel.add(valueTree, BorderLayout.CENTER);
                dataPanel.revalidate();
            }
        };

        this.cancelAction = new AnAction(() -> "Cancel", UIUtils.CANCEL) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                dataPanel.removeAll();
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
                valueTree.setCellEditor(new InsidiousCellEditor(valueTree, null));
                expandAllNodes();
                viewStateButton(true);
                dataPanel.add(valueTree, BorderLayout.CENTER);
                dataPanel.revalidate();
            }
        };

        this.buildJsonAction = new AnAction(() -> "Save", UIUtils.SAVE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                dataPanel.removeAll();
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
                valueTree.setCellEditor(new InsidiousCellEditor(valueTree, null));
                expandAllNodes();
                dataPanel.add(valueTree, BorderLayout.CENTER);
                dataPanel.revalidate();
                expandAllNodes();
            }
        };

        viewStateButton(true);
    }

    private static MouseListener getMouseListener(final JTree tree) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tree.getRowForLocation(e.getX(), e.getY()) == -1) {
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
                if (tree.getRowForLocation(e.getX(), e.getY()) == -1) {
                    tree.clearSelection();
                }
            }
        };
    }

    public void viewStateButton(Boolean viewState) {

        List<AnAction> localListAction = new ArrayList<>(this.listAnAction);
        if (viewState) {
            localListAction.add(this.editAction);
            localListAction.add(this.buildJsonAction);
        } else {
            localListAction.add(this.cancelAction);
            localListAction.add(this.saveAction);
        }
        ActionToolbarImpl actionToolbar = new ActionToolbarImpl("JTE actionToolbar", new DefaultActionGroup(localListAction),
                true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);

        // make the actionPanel component
        actionPanel.removeAll();
        JComponent component = actionToolbar.getComponent();

        actionPanel.add(component, BorderLayout.WEST);
        actionPanel.revalidate();
        actionPanelMain.revalidate();
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
