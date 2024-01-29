package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.*;
import com.insidious.plugin.autoexecutor.AutoExecutorReportRecord;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.util.*;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.ui.JBColor;
import com.intellij.ui.KeyStrokeAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;

public class MethodDirectInvokeComponent implements ActionListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodDirectInvokeComponent.class);
    //    private static final ActionListener NOP_KEY_ADAPTER = e -> {
//    };
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private JPanel mainContainer;
    private JPanel actionControlPanel;
    private JTextArea returnValueTextArea;
    private JPanel methodParameterScrollContainer;
    private JButton executeButton;
    private JButton modifyArgumentsButton;
    private JLabel closeButton;
    private JLabel editValueLabel;
    private JLabel createBoilerPlate;
    private MethodAdapter methodElement;
    private Tree argumentValueTree = null;
    private TreeModel argumentsValueTreeNode;
    private JsonNode argumentsValueJsonNode;
    private JBScrollPane parameterScrollPanel;
    private DefaultTreeCellEditor cellEditor;


    public MethodDirectInvokeComponent(InsidiousService insidiousService, OnCloseListener onCloseListener) {
        this.insidiousService = insidiousService;
        this.objectMapper = this.insidiousService.getObjectMapper();
        modifyArgumentsButton.setVisible(false);

        configureCreateBoilerplateButton(insidiousService);
        configureEditButton();
        configureCloseButton(onCloseListener);


        methodParameterScrollContainer.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeMethodWithParameters();
                }
            }
        });

        executeButton.addActionListener(e -> executeMethodWithParameters());
        executeButton.setIcon(UIUtils.DIRECT_INVOKE_EXECUTE);

        modifyArgumentsButton.addActionListener(e -> {
            try {
                renderForMethod(methodElement, null);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void configureEditButton() {

        editValueLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final Border closeButtonOriginalBorder = editValueLabel.getBorder();
        final Border actuallyOriginalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                closeButtonOriginalBorder);
        editValueLabel.setBorder(actuallyOriginalBorder);
        editValueLabel.setToolTipText("Hide direct invoke");
        editValueLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                editValueLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        closeButtonOriginalBorder));
//                editValueLabel.setIcon(UIUtils.CLOSE_LINE_BLACK_PNG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                editValueLabel.setBorder(actuallyOriginalBorder);
//                editValueLabel.setIcon(UIUtils.CLOSE_LINE_PNG);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                insidiousService.previewTestCase(methodElement, null, true);
            }
        });
    }

    private void configureCreateBoilerplateButton(InsidiousService insidiousService) {

        createBoilerPlate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final Border closeButtonOriginalBorder = createBoilerPlate.getBorder();
        final Border actuallyOriginalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                closeButtonOriginalBorder);
        createBoilerPlate.setBorder(actuallyOriginalBorder);
        createBoilerPlate.setToolTipText("Hide direct invoke");
        createBoilerPlate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                createBoilerPlate.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        closeButtonOriginalBorder));
//                createBoilerPlate.setIcon(UIUtils.CLOSE_LINE_BLACK_PNG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                createBoilerPlate.setBorder(actuallyOriginalBorder);
//                createBoilerPlate.setIcon(UIUtils.CLOSE_LINE_PNG);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                insidiousService.previewTestCase(methodElement, null, true);
            }
        });
    }

    private void configureCloseButton(OnCloseListener onCloseListener) {
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final Border closeButtonOriginalBorder = closeButton.getBorder();
        final Border actuallyOriginalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                closeButtonOriginalBorder);
        closeButton.setBorder(actuallyOriginalBorder);
        closeButton.setToolTipText("Hide direct invoke");
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                closeButton.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        closeButtonOriginalBorder));
//                closeButton.setIcon(UIUtils.CLOSE_LINE_BLACK_PNG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                closeButton.setBorder(actuallyOriginalBorder);
//                closeButton.setIcon(UIUtils.CLOSE_LINE_PNG);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                onCloseListener.onClose(MethodDirectInvokeComponent.this);
            }
        });
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

    // Utility method to initialize the expansion
    public int expandAllNodes(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        return 1 + expandAll(tree, new TreePath(root));
    }

    private void executeMethodWithParameters() {
        boolean isConnected = insidiousService.isAgentConnected();
//        returnValueTextArea.setFont(SOURCE_CODE);

        returnValueTextArea = new JTextArea();
        returnValueTextArea.setLineWrap(true);

        if (!isConnected) {
            String message = "Start your application with Java unlogged-sdk to start using " +
                    "method DirectInvoke";
            InsidiousNotification.notifyMessage(message, NotificationType.INFORMATION);
            parameterScrollPanel.setViewportView(returnValueTextArea);

            returnValueTextArea.setText(message);
            return;
        }

        if (methodElement == null) {
            String message = "No method selected in editor.";
            parameterScrollPanel.setViewportView(returnValueTextArea);
            returnValueTextArea.setText(message);
            InsidiousNotification.notifyMessage(message, NotificationType.WARNING);
            return;
        }
        executeButton.setText("Executing...");
        executeButton.setEnabled(false);
        createBoilerPlate.setVisible(false);

        ApplicationManager.getApplication().executeOnPooledThread(this::chooseClassAndDirectInvoke);
    }

    public void renderForMethod(MethodAdapter methodElement1, List<String> methodArgumentValues) throws JsonProcessingException {
        if (methodElement1 == null) {
            logger.info("DirectInvoke got null method");
            return;
        }

        this.methodElement = methodElement1;
        String methodName = methodElement.getName();
        ((TitledBorder) mainContainer.getBorder()).setTitle(methodName);
        ClassAdapter containingClass = methodElement.getContainingClass();

        modifyArgumentsButton.setVisible(false);
        createBoilerPlate.setVisible(true);
        executeButton.setText("Execute");

        logger.warn("render method executor for: " + methodName);
        String methodNameForLabel = methodName.length() > 40 ? methodName.substring(0, 40) + "..." : methodName;
        String title = methodNameForLabel + "( " + ")";
        setActionPanelTitle(title);

        ParameterAdapter[] methodParameters = methodElement.getParameters();

//        TestCandidateMetadata mostRecentTestCandidate = null;
        AgentCommandRequest agentCommandRequest = MethodUtils.createExecuteRequestWithParameters(methodElement,
                new ClassUnderTest(ApplicationManager.getApplication().runReadAction(
                        (Computable<String>) () ->
                                JvmClassUtil.getJvmClassName((PsiClass) containingClass.getSource()))),
                methodArgumentValues, false, null);

        AgentCommandRequest existingRequests = insidiousService.getAgentCommandRequests(agentCommandRequest);
        if (existingRequests != null) {
            methodArgumentValues = existingRequests.getMethodParameters();
        } else {
            SessionInstance sessionInstance = this.insidiousService.getSessionInstance();
            if (sessionInstance != null) {
                CandidateSearchQuery query = insidiousService.createSearchQueryForMethod(
                        methodElement, CandidateFilterType.METHOD, false);

                List<TestCandidateMetadata> methodTestCandidates = sessionInstance.getTestCandidatesForAllMethod(query);
                int candidateCount = methodTestCandidates.size();
                if (candidateCount > 0) {
                    TestCandidateMetadata mostRecentTestCandidate = methodTestCandidates.get(candidateCount - 1);
                    methodArgumentValues = TestCandidateUtils.buildArgumentValuesFromTestCandidate(
                            mostRecentTestCandidate);
                }
            }
        }

        JPanel methodParameterContainer = new JPanel();

        parameterInputComponents.clear();
        Project project = methodElement.getProject();
        ProjectAndLibrariesScope projectAndLibrariesScope = new ProjectAndLibrariesScope(project);

        if (methodParameters.length > 0) {
//            methodParameterContainer.setLayout(new GridLayout(methodParameters.length, 1));
            BorderLayout boxLayout = new BorderLayout();
            methodParameterContainer.setLayout(boxLayout);
            Map<String, JsonNode> methodArgumentsMap = new HashMap<>();

            for (int i = 0; i < methodParameters.length; i++) {
                ParameterAdapter methodParameter = methodParameters[i];
//
                String parameterValue = "";
                PsiType methodParameterType = methodParameter.getType();
                if (methodArgumentValues != null && i < methodArgumentValues.size()) {
                    parameterValue = methodArgumentValues.get(i);
                } else {
                    parameterValue = ClassUtils.createDummyValue(methodParameterType, new ArrayList<>(4),
                            insidiousService.getProject());
                }
                try {
                    methodArgumentsMap.put(methodParameter.getName(), objectMapper.readTree(parameterValue));
                } catch (JsonProcessingException e) {
                    methodArgumentsMap.put(methodParameter.getName(),
                            objectMapper.getNodeFactory().textNode(parameterValue));
                }
//
//                ParameterInputComponent parameterContainer;
//                JPanel content;
//                String typeCanonicalName = methodParameterType.getCanonicalText();
//                if (methodParameterType instanceof PsiPrimitiveType
//                        || isBoxedPrimitive(typeCanonicalName)
//                ) {
//                    parameterContainer = new ParameterInputComponent(methodParameter, parameterValue,
//                            JBTextField.class, this);
//                    content = parameterContainer.getContent();
//                    content.setMinimumSize(new Dimension(100, 150));
////                    content.setPreferredSize(new Dimension(100, 80));
////                    content.setMaximumSize(new Dimension(-1, 80));
//                } else {
//                    PsiClass typeClassReference = JavaPsiFacade
//                            .getInstance(project)
//                            .findClass(typeCanonicalName, projectAndLibrariesScope);
//                    if (typeClassReference != null && (typeClassReference.isEnum())) {
//                        parameterContainer = new ParameterInputComponent(methodParameter, parameterValue,
//                                JBTextArea.class, NOP_KEY_ADAPTER);
//                    } else {
//                        parameterContainer = new ParameterInputComponent(methodParameter, parameterValue,
//                                JBTextArea.class, NOP_KEY_ADAPTER);
//                    }
//                    content = parameterContainer.getContent();
//                    content.setMinimumSize(new Dimension(100, 150));
////                    content.setMaximumSize(new Dimension(-1, 300));
//                }
//
//
//                parameterInputComponents.add(parameterContainer);
//                methodParameterContainer.add(content);
            }
            String source = "{}";
            try {
                source = objectMapper.writeValueAsString(methodArgumentsMap);
                argumentsValueJsonNode = objectMapper.readTree(source);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            argumentsValueTreeNode = JsonTreeUtils.jsonToTreeModel(objectMapper.readTree(source), "Method Arguments");
            argumentValueTree = new Tree(argumentsValueTreeNode);
            argumentValueTree.setBackground(JBColor.WHITE);
            argumentValueTree.setBorder(BorderFactory.createLineBorder(new Color(97, 97, 97, 255)));

            argumentValueTree.setEditable(true);
            cellEditor = new DefaultTreeCellEditor(argumentValueTree, null) {
                private JTextField editor;
                private String key;

                @Override
                public boolean isCellEditable(EventObject event) {
                    boolean isEditable = super.isCellEditable(event);
                    if (isEditable) {
                        // Make sure only leaf nodes are editable
                        TreePath path = tree.getSelectionPath();
                        return path != null && tree.getModel().isLeaf(path.getLastPathComponent());
                    }
                    return false;
                }

                @Override
                public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {

//                    JPanel editorPanel = new JPanel();
//                    editorPanel.setMinimumSize(new Dimension(100, 50));
//                    editorPanel.setLayout(new BorderLayout());

                    editor = new JBTextField();
//                    editor.setBorder(BorderFactory.createCompoundBorder(
//                            BorderFactory.createEmptyBorder(5, 5, 5, 5),
//                            BorderFactory.createLineBorder(JBColor.BLACK)
//                    ));
//                    editor.setMinimumSize(new Dimension(100, 80));
//                    editor.setMaximumSize(new Dimension(100, 80));
                    if (value instanceof DefaultMutableTreeNode) {
                        Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                        String[] parts = userObject.toString().split(":");
                        key = parts[0];
                        editor.setText(parts[1].trim());
                        editor.addKeyListener(new KeyStrokeAdapter() {
                            @Override
                            public void keyTyped(KeyEvent event) {
                                super.keyTyped(event);
                                if (event.getKeyChar() == KeyEvent.VK_ENTER) {
                                    cellEditor.stopCellEditing();
                                }
                            }
                        });
                    }
                    editor.setBorder(null);
//                    editorPanel.add(editor, BorderLayout.CENTER);
                    return editor;
                }

                @Override
                public boolean stopCellEditing() {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) argumentValueTree.getLastSelectedPathComponent();
                    if (node != null && node.isLeaf()) {
                        DefaultTreeModel model = (DefaultTreeModel) argumentValueTree.getModel();
                        node.setUserObject(getCellEditorValue());
                        model.nodeChanged(node); // Notify the model that the node has changed
                    }
                    return super.stopCellEditing();
                }

                @Override
                public Object getCellEditorValue() {
                    return key + ": " + editor.getText().trim();
                }
            };

            // Listener to handle changes in the tree nodes
            argumentValueTree.getModel().addTreeModelListener(new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent e) {
                    TreePath path = e.getTreePath();
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    try {
                        String editedValue = (String) node.getUserObject();
                        // Here, you can further process the edited value if necessary
                        // For example, validate or parse it before saving
                        node.setUserObject(editedValue);
                    } catch (Exception ex) {
                        logger.error("Failed to read value", ex);
                        InsidiousNotification.notifyMessage("Failed to read value, please report this on github",
                                NotificationType.ERROR);
                    }
                }

                public void treeNodesInserted(TreeModelEvent e) {
                }

                public void treeNodesRemoved(TreeModelEvent e) {
                }

                public void treeStructureChanged(TreeModelEvent e) {
                }
            });

//            argumentValueTree.setCellRenderer(new DefaultTreeCellRenderer() {
////                @Override
////                public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
////                    JLabel jLabel = new JLabel("Hello - " + String.valueOf(value));
////                    JPanel rowPanel = new JPanel();
////                    rowPanel.setLayout(new BorderLayout());
////                    rowPanel.add(jLabel, BorderLayout.CENTER);
////                    Border border = jLabel.getBorder();
////                    jLabel.setBorder(
////                            BorderFactory.createCompoundBorder(
////                                    BorderFactory.createEmptyBorder(5, 5, 5, 5),
////                                    border
////                            ));
////                    rowPanel.setMinimumSize(new Dimension(200, 80));
////                    rowPanel.setMaximumSize(new Dimension(200, 80));
////                    rowPanel.setPreferredSize(new Dimension(200, 80));
////                    return rowPanel;
////                }
//            });
            argumentValueTree.setCellEditor(cellEditor);
            expandAllNodes(argumentValueTree);
            methodParameterContainer.add(argumentValueTree, BorderLayout.CENTER);
        } else {
            JBLabel noParametersLabel = new JBLabel("No method arguments");
            methodParameterContainer.add(noParametersLabel, BorderLayout.CENTER);
        }

//        Spacer spacer = new Spacer();
//        methodParameterContainer.add(spacer);
        methodParameterScrollContainer.removeAll();

        parameterScrollPanel = new JBScrollPane(methodParameterContainer);
        parameterScrollPanel.setMaximumSize(new Dimension(-1, 300));
        parameterScrollPanel.setBorder(BorderFactory.createEmptyBorder());


        methodParameterScrollContainer.setMinimumSize(new Dimension(-1, Math.min(methodParameters.length * 100, 100)));
        methodParameterScrollContainer.add(parameterScrollPanel, BorderLayout.CENTER);


        mainContainer.revalidate();
        mainContainer.repaint();

    }

    private void setActionPanelTitle(String title) {
//        TitledBorder titledBorder = (TitledBorder) actionControlPanel.getBorder();
//        titledBorder.setTitle(title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        executeMethodWithParameters();
    }

    private void clearOutputSection() {
//        TitledBorder panelTitledBoarder = (TitledBorder) scrollerContainer.getBorder();
//        panelTitledBoarder.setTitle("Method Response");
//        returnValueTextArea.setText("");
    }

    private boolean isBoxedPrimitive(String typeCanonicalName) {
        return typeCanonicalName.equals("java.lang.String")
                || typeCanonicalName.equals("java.lang.Integer")
                || typeCanonicalName.equals("java.lang.Long")
                || typeCanonicalName.equals("java.lang.Double")
                || typeCanonicalName.equals("java.lang.Short")
                || typeCanonicalName.equals("java.lang.Float")
                || typeCanonicalName.equals("java.lang.Byte")
                || typeCanonicalName.equals("java.math.BigDecimal");
    }


    public JComponent getContent() {
        return mainContainer;
    }

    public void triggerExecute() {
        executeMethodWithParameters();
    }

    private void classSelected(ClassUnderTest psiClass) {
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("className", psiClass.getQualifiedClassName());
        eventProperties.put("methodName", methodElement.getName());

        UsageInsightTracker.getInstance().RecordEvent("DIRECT_INVOKE", eventProperties);
        List<String> methodArgumentValues = new ArrayList<>();
        ParameterAdapter[] parameters = methodElement.getParameters();
        for (int i = 0; i < parameters.length; i++) {
//                ParameterInputComponent parameterInputComponent = parameterInputComponents.get(i);
            ParameterAdapter parameter = parameters[i];

            String selectedKey = "/" + parameter.getName();

            JsonNode valueFromJsonNode = JsonTreeUtils.treeModelToJson(argumentsValueTreeNode);

            String parameterValue = null;
            try {
                parameterValue = objectMapper.writeValueAsString(
                        valueFromJsonNode.get(parameter.getName()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            String canonicalText = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> parameter.getType().getCanonicalText());
            if ("java.lang.String".equals(canonicalText) &&
                    !parameterValue.startsWith("\"")) {
                try {
                    parameterValue = objectMapper.writeValueAsString(parameterValue);
                } catch (JsonProcessingException e) {
// should never happen
                }
            }
            methodArgumentValues.add(parameterValue);
        }

        AgentCommandRequest agentCommandRequest =
                MethodUtils.createExecuteRequestWithParameters(methodElement, psiClass,
                        methodArgumentValues,
                        false, null);
        agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);


//            returnValueTextArea.setText("Executing method [" + agentCommandRequest.getMethodName() + "()]\nin class ["
//                    + agentCommandRequest.getClassName() + "].\nWaiting for response...");
        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                new ExecutionResponseListener() {
                    @Override
                    public void onExecutionComplete(AgentCommandRequest agentCommandRequest1, AgentCommandResponse<String> agentCommandResponse) {

                        ApplicationManager.getApplication().invokeLater(() -> {
                            executeButton.setEnabled(true);
                            executeButton.setText("Re-execute");
//                        logger.warn("Agent command execution response: " + agentCommandResponse);
                            if (ResponseType.EXCEPTION.equals(agentCommandResponse.getResponseType())) {
                                if (agentCommandResponse.getMessage() == null && agentCommandResponse.getResponseClassName() == null) {
                                    InsidiousNotification.notifyMessage(
                                            "Exception thrown when trying to direct invoke " + agentCommandRequest.getMethodName(),
                                            NotificationType.ERROR
                                    );
                                    return;
                                }
                            }

                            ResponseType responseType = agentCommandResponse.getResponseType();
                            String responseMessage = agentCommandResponse.getMessage() == null ? "" :
                                    agentCommandResponse.getMessage() + "\n";
//                        TitledBorder panelTitledBoarder = (TitledBorder) scrollerContainer.getBorder();
                            String responseObjectClassName = agentCommandResponse.getResponseClassName();
                            Object methodReturnValue = agentCommandResponse.getMethodReturnValue();
                            modifyArgumentsButton.setVisible(true);

                            String targetClassName = agentCommandResponse.getTargetClassName();
                            if (targetClassName == null) {
                                targetClassName = agentCommandRequest.getClassName();
                            }
                            targetClassName = targetClassName.substring(
                                    targetClassName.lastIndexOf(".") + 1);
                            String targetMethodName = agentCommandResponse.getTargetMethodName();
                            if (targetMethodName == null) {
                                targetMethodName = agentCommandRequest.getMethodName();
                            }
                            ApplicationManager.getApplication()
                                    .invokeLater(() -> {
                                        if (argumentValueTree != null) {
                                            argumentValueTree.collapsePath(
                                                    new TreePath(argumentValueTree.getModel().getRoot()));
                                        }
                                    });
                            String toolTipText = "Timestamp: " +
                                    new Timestamp(agentCommandResponse.getTimestamp()) + " from "
                                    + targetClassName + "." + targetMethodName + "( " + " )";
                            returnValueTextArea.setToolTipText(toolTipText);
                            if (responseType == null) {
//                            panelTitledBoarder.setTitle(responseObjectClassName);
                                parameterScrollPanel.setViewportView(returnValueTextArea);
                                returnValueTextArea.setText(responseMessage + methodReturnValue);
                                return;
                            }

                            if (responseType.equals(ResponseType.NORMAL)) {

                                ObjectMapper objectMapper = insidiousService.getObjectMapper();
                                try {
                                    String returnValueString = String.valueOf(methodReturnValue);

                                    String responseClassName = agentCommandResponse.getResponseClassName();
                                    if (responseClassName.equals("float") || responseClassName.equals(
                                            "java.lang.Float")) {
                                        returnValueString = ParameterUtils.getFloatValue(returnValueString);
                                    }

                                    if (responseClassName.equals("double") || responseClassName.equals(
                                            "java.lang.Double")) {
                                        returnValueString = ParameterUtils.getDoubleValue(
                                                returnValueString);
                                    }

                                    JsonNode jsonNode = objectMapper.readValue(returnValueString,
                                            JsonNode.class);

                                    TreeModel responseObjectTree = JsonTreeUtils.jsonToTreeModel(
                                            jsonNode, responseClassName);

//                                returnValueTextArea.setText(objectMapper.writerWithDefaultPrettyPrinter()
//                                        .writeValueAsString(jsonNode));

//                                returnValuePanel.setViewportView(returnValueTextArea);
                                    Tree comp = new Tree(responseObjectTree);
                                    comp.setToolTipText(toolTipText);
                                    comp.setBackground(JBColor.WHITE);
                                    comp.setBorder(
                                            BorderFactory.createLineBorder(new Color(97, 97, 97, 255)));
                                    int totalNodeCount = MethodDirectInvokeComponent.this.expandAllNodes(comp);
//                                JPanel responseTreeContainer = new JPanel(new BorderLayout());
//                                responseTreeContainer.add(comp, BorderLayout.CENTER);
                                    parameterScrollPanel.setViewportView(comp);
//                                if (totalNodeCount > 4) {
//                                    int min = Math.min(totalNodeCount * 30, 300);
//                                    returnValuePanel.setSize(new Dimension(-1, min));
//                                    returnValuePanel.setPreferredSize(new Dimension(-1, min));
//                                    returnValuePanel.setMaximumSize(new Dimension(-1, min));
//                                } else {
//                                    returnValuePanel.setSize(new Dimension(-1, 100));
//                                    returnValuePanel.setPreferredSize(new Dimension(-1, 100));
//                                    returnValuePanel.setMaximumSize(new Dimension(-1, 100));
//
//                                }
                                } catch (JsonProcessingException ex) {
                                    parameterScrollPanel.setViewportView(returnValueTextArea);
                                    returnValueTextArea.setText(methodReturnValue.toString());
                                }
                            } else if (responseType.equals(ResponseType.EXCEPTION)) {
//                            panelTitledBoarder.setTitle(responseObjectClassName);
                                if (methodReturnValue != null) {
                                    parameterScrollPanel.setViewportView(returnValueTextArea);
                                    returnValueTextArea.setText(
                                            ExceptionUtils.prettyPrintException(
                                                    methodReturnValue.toString()));
                                } else {
                                    parameterScrollPanel.setViewportView(returnValueTextArea);
                                    returnValueTextArea.setText(agentCommandResponse.getMessage());
                                }
                            } else {
//                            panelTitledBoarder.setTitle(responseObjectClassName);
                                parameterScrollPanel.setViewportView(returnValueTextArea);
                                returnValueTextArea.setText(responseMessage + methodReturnValue);
                            }
//                        scrollerContainer.revalidate();
//                        scrollerContainer.repaint();

                            ResponseType responseType1 = agentCommandResponse.getResponseType();
                            DiffResultType diffResultType = responseType1.equals(
                                    ResponseType.NORMAL) ? DiffResultType.NO_ORIGINAL : DiffResultType.ACTUAL_EXCEPTION;
                            DifferenceResult diffResult = new DifferenceResult(null,
                                    diffResultType, null,
                                    DiffUtils.getFlatMapFor(agentCommandResponse.getMethodReturnValue()));
                            diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE);
//                        diffResult.setMethodAdapter(methodElement);
                            diffResult.setResponse(agentCommandResponse);
                            diffResult.setCommand(agentCommandRequest);
                            insidiousService.addExecutionRecord(new AutoExecutorReportRecord(diffResult,
                                    insidiousService.getSessionInstance().getProcessedFileCount(),
                                    insidiousService.getSessionInstance().getTotalFileCount()));
                        });

                    }
                });
    }

    private void chooseClassAndDirectInvoke() {
        insidiousService.chooseClassImplementation(methodElement.getContainingClass().getQualifiedName(),
                this::classSelected);
    }

//    public void uncheckPermanentMocks() {
//        permanentMocksCheckBox.setSelected(false);
//    }
}
