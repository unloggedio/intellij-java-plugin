package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.GutterState;
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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.ui.KeyStrokeAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.BevelBorder;
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
    //    private JPanel scrollerContainer;
    //    private JLabel methodNameLabel;
    private JButton executeButton;
    private JButton createJUnitBoilerplateButton;
    private JLabel candidateCountLinkLabel;
    private JPanel candidateCountPanel;
    private JPanel candidateCountPanelParent;
    private JPanel candidateCountLinkLabelParent;
    private JLabel coveragePercentLabel;
    private JButton modifyArgumentsButton;
    private JLabel closeButton;
    private JLabel createBoilerPlate;
    private MethodAdapter methodElement;
    //    private Font SOURCE_CODE = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/fonts" +
//            "/SourceCodePro-Regular.ttf"));
    private Tree argumentValueTree;
    private DefaultMutableTreeNode argumentsValueTreeNode;
    private JsonNode argumentsValueJsonNode;
    private JBScrollPane parameterScrollPanel;
    private DefaultTreeCellEditor cellEditor;


    public MethodDirectInvokeComponent(InsidiousService insidiousService, OnCloseListener onCloseListener) {
        this.insidiousService = insidiousService;
        this.objectMapper = this.insidiousService.getObjectMapper();
        modifyArgumentsButton.setVisible(false);

        candidateCountLinkLabel.setVisible(false);
        coveragePercentLabel.setVisible(false);
//        scrollerContainer.setVisible(false);
        candidateCountPanelParent.setVisible(false);
//        setActionPanelTitle("This will be available after IDEA indexing is complete");
//        executeButton.setEnabled(false);
        createBoilerPlate.setText("<html><u><s>Create JUnit Boilerplate</s></u></html>");
        createBoilerPlate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createBoilerPlate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                insidiousService.previewTestCase(methodElement, null, true);
            }
        });

        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                onCloseListener.onClose(MethodDirectInvokeComponent.this);
            }
        });
        executeButton.addActionListener(e -> executeMethodWithParameters());
        methodParameterScrollContainer.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeMethodWithParameters();
                }
            }
        });
        executeButton.setIcon(UIUtils.DIRECT_INVOKE_EXECUTE);


//        permanentMocksCheckBox.addActionListener(e -> {
//            if (permanentMocksCheckBox.isSelected()) {
//                insidiousService.injectMocksInRunningProcess(null);
//            } else {
//                insidiousService.removeMocksInRunningProcess(null);
//            }
//        });

//        createJUnitBoilerplateButton.setIcon(UIUtils.TEST_TUBE_FILL);
//        createJUnitBoilerplateButton.addActionListener(
//                e -> {
//                    TestCaseGenerationConfiguration generationConfiguration = insidiousService.generateMethodBoilerplate(
//                            methodElement);
//                    if (generationConfiguration == null) {
//                        InsidiousNotification.notifyMessage("Failed to create boilerplate test case",
//                                NotificationType.ERROR);
//                        return;
//                    }
//                    insidiousService.previewTestCase(methodElement, generationConfiguration);
//                    InsidiousNotification.notifyMessage(
//                            "Created JUnit test boilerplate. \nRecord with unlogged-sdk to create full JUnit " +
//                                    "test case.", NotificationType.INFORMATION
//                    );
//                });


        candidateCountLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                insidiousService.focusAtomicTestsWindow();
            }
        });
        coveragePercentLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                insidiousService.focusAtomicTestsWindow();
            }
        });
        modifyArgumentsButton.addActionListener(e -> {
            modifyArgumentsButton.setVisible(false);
            executeButton.setText("Execute");
            renderForMethod(methodElement, null);
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

    public void updateCandidateCount(int candidateCount) {
        if (candidateCount == 0) {
            candidateCountLinkLabel.setVisible(false);
        } else if (candidateCount > 0) {
            candidateCountLinkLabel.setVisible(true);
            candidateCountLinkLabel.setText("<HTML><U>" + candidateCount + " recorded execution" + "</U></HTML>");
            candidateCountLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            candidateCountLinkLabel.setForeground(UIUtils.tealdark);
            ;
        }
    }

    public void setCoveragePercent(int coveragePercent) {
        if (coveragePercent == 0) {
            coveragePercentLabel.setVisible(false);
        } else if (coveragePercent > 0) {
            coveragePercentLabel.setVisible(true);
            coveragePercentLabel.setText("<HTML><U>+" + coveragePercent + " line coverage" + "</U></HTML>");
            coveragePercentLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            coveragePercentLabel.setForeground(UIUtils.green);

        }
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
            insidiousService.updateScaffoldForState(GutterState.PROCESS_NOT_RUNNING);
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


        insidiousService.chooseClassImplementation(methodElement.getContainingClass().getQualifiedName(), psiClass -> {
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
                JsonNode valueFromJsonNode = JsonTreeUtils.getValueFromJsonNode(argumentsValueJsonNode, selectedKey);

                String parameterValue = valueFromJsonNode.toString();
                if ("java.lang.String".equals(parameter.getType().getCanonicalText()) &&
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
                    MethodUtils.createExecuteRequestWithParameters(methodElement, psiClass, methodArgumentValues,
                            false, null);
            agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);


//            returnValueTextArea.setText("Executing method [" + agentCommandRequest.getMethodName() + "()]\nin class ["
//                    + agentCommandRequest.getClassName() + "].\nWaiting for response...");
            insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                    (agentCommandRequest1, agentCommandResponse) -> {
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
                        targetClassName = targetClassName.substring(targetClassName.lastIndexOf(".") + 1);
                        String targetMethodName = agentCommandResponse.getTargetMethodName();
                        if (targetMethodName == null) {
                            targetMethodName = agentCommandRequest.getMethodName();
                        }
                        ApplicationManager.getApplication()
                                .runWriteAction(() -> {
                                    argumentValueTree.collapsePath(new TreePath(argumentValueTree.getModel().getRoot()));
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
                                if (responseClassName.equals("float") || responseClassName.equals("java.lang.Float")) {
                                    returnValueString = ParameterUtils.getFloatValue(returnValueString);
                                }

                                if (responseClassName.equals("double") || responseClassName.equals(
                                        "java.lang.Double")) {
                                    returnValueString = ParameterUtils.getDoubleValue(returnValueString);
                                }

                                JsonNode jsonNode = objectMapper.readValue(returnValueString, JsonNode.class);

                                DefaultMutableTreeNode responseObjectTree = JsonTreeUtils.buildJsonTree(
                                        objectMapper.writeValueAsString(jsonNode),
                                        responseClassName);

//                                returnValueTextArea.setText(objectMapper.writerWithDefaultPrettyPrinter()
//                                        .writeValueAsString(jsonNode));

//                                returnValuePanel.setViewportView(returnValueTextArea);
                                Tree comp = new Tree(responseObjectTree);
                                comp.setToolTipText(toolTipText);
                                int totalNodeCount = expandAllNodes(comp);
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
                                        ExceptionUtils.prettyPrintException(methodReturnValue.toString()));
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
                        insidiousService.addExecutionRecord(diffResult);
                        methodParameterScrollContainer.revalidate();
                        methodParameterScrollContainer.repaint();
                    });
        });


    }

    public void renderForMethod(MethodAdapter methodElement1, List<String> methodArgumentValues) {
        if (methodElement1 == null) {
            logger.info("DirectInvoke got null method");
            return;
        }

//        clearOutputSection();

        this.methodElement = methodElement1;
        String methodName = methodElement.getName();
        ((TitledBorder) mainContainer.getBorder()).setTitle(methodName);
        ClassAdapter containingClass = methodElement.getContainingClass();

        logger.warn("render method executor for: " + methodName);
        String methodNameForLabel = methodName.length() > 40 ? methodName.substring(0, 40) + "..." : methodName;
        String title = methodNameForLabel + "( " + ")";
        setActionPanelTitle(title);

        ParameterAdapter[] methodParameters = methodElement.getParameters();

//        TestCandidateMetadata mostRecentTestCandidate = null;
        AgentCommandRequest agentCommandRequest = MethodUtils.createExecuteRequestWithParameters(methodElement,
                new ClassUnderTest(JvmClassUtil.getJvmClassName((PsiClass) containingClass.getSource())),
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
            argumentsValueTreeNode = JsonTreeUtils.buildJsonTree(source, "Method Arguments");
            argumentValueTree = new Tree(argumentsValueTreeNode);
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
                    editor = new JTextField();
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

            argumentValueTree.setCellEditor(cellEditor);
            expandAllNodes(argumentValueTree);
            methodParameterContainer.add(argumentValueTree, BorderLayout.CENTER);
        } else {
            JBLabel noParametersLabel = new JBLabel("Method " + methodName + "() has no parameters");
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

//    public void uncheckPermanentMocks() {
//        permanentMocksCheckBox.setSelected(false);
//    }
}
