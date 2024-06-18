package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.autoexecutor.AutoExecutorReportRecord;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.testdesigner.TestCaseDesignerLite;
import com.insidious.plugin.ui.treeeditor.JsonTreeEditor;
import com.insidious.plugin.util.*;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;

public class MethodDirectInvokeComponent
        implements Disposable, ComponentProvider {
    private static final Logger logger = LoggerUtil.getInstance(MethodDirectInvokeComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private final ObjectMapper objectMapper;
    //    private final RouterPanel routerPanel;
    private JPanel mainContainer;
    private Editor returnValueTextArea;
    private JButton createBoilerplateButton;
    private JPanel centerPanel;
    private MethodAdapter methodElement;
    private JBScrollPane parameterScrollPanel = null;
    private TestCaseDesignerLite designerLite;
    private JsonTreeEditor parameterEditor;
    private AnAction executeAction;
    private AnAction modifyArgumentsAction;

    private boolean isShowingRouter = true;
    private JPanel methodParameterContainer;


    public MethodDirectInvokeComponent(InsidiousService insidiousService,
                                       ComponentLifecycleListener<MethodDirectInvokeComponent> componentLifecycleListener) {
        this.insidiousService = insidiousService;
        this.objectMapper = ObjectMapperInstance.getInstance();

//        centerPanel.add(routerPanel.getComponent(), BorderLayout.CENTER);


//        AnAction closeAction = new AnAction(() -> "Back", AllIcons.Actions.Back) {
//            @Override
//            public void actionPerformed(@NotNull AnActionEvent e) {
//                centerPanel.removeAll();
////                if (isShowingRouter) {
//                isShowingRouter = false;
//                componentLifecycleListener.onClose();
////                } else {
////                    centerPanel.add(routerPanel.getComponent(), BorderLayout.CENTER);
////                    isShowingRouter = true;
////                }
//                centerPanel.revalidate();
//                centerPanel.repaint();
//            }
//
//            @Override
//            public boolean isDumbAware() {
//                return false;
//            }
//
//            @Override
//            public boolean displayTextInToolbar() {
//                return true;
//            }
//
//        };

//        List<AnAction> action11 = new ArrayList<>();
//        action11.add(closeAction);

//        ActionToolbarImpl actionToolbar = new ActionToolbarImpl(
//                "MDIC ActionToolBar", new DefaultActionGroup(action11), true);
//        actionToolbar.setMiniMode(false);
//        actionToolbar.setForceMinimumSize(true);
//        actionToolbar.setTargetComponent(centerPanel);
//        controlPanel.add(actionToolbar.getComponent(), BorderLayout.EAST);


//        closeButton.setIcon(UIUtils.CLOSE_LINE_SVG);


        this.executeAction = new AnAction(() -> "Execute Method", UIUtils.DIRECT_INVOKE_EXECUTE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        parameterScrollPanel.setViewportView(new JLabel("Executing..."));
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            executeMethodWithParameters();
                        });
                    } catch (Exception e1) {
                        parameterScrollPanel.setViewportView(methodParameterContainer);
                    }
                });
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };

        this.modifyArgumentsAction = new AnAction(() -> "Edit Arguments", UIUtils.EDIT) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    renderForMethod(methodElement, null);
                });
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };
    }


    private int expandAll(JTree tree, TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements(); ) {
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


        if (!isConnected) {
            String message = "Application is not running with unlogged-sdk.";
            InsidiousNotification.notifyMessage(message, NotificationType.INFORMATION);
            return;
        }

        if (methodElement == null) {
            String message = "No method selected in editor.";
            InsidiousNotification.notifyMessage(message, NotificationType.WARNING);
            return;
        }
//        createBoilerplateButton.setVisible(false);
        DumbService.getInstance(insidiousService.getProject()).runWhenSmart(
                this::chooseClassAndDirectInvoke);
    }

    public void renderForMethod(MethodAdapter methodElement1, List<String> methodArgumentValues) {
        if (methodElement1 == null) {
            logger.info("DirectInvoke got null method");
            return;
        }
        this.methodElement = methodElement1;
        ClassAdapter containingClass = methodElement.getContainingClass();

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
//            SessionInstanceInterface sessionInstance = this.insidiousService.getSessionInstance();
//            if (sessionInstance != null) {
//                CandidateSearchQuery query = insidiousService.createSearchQueryForMethod(
//                        methodElement, CandidateFilterType.METHOD, false);
//
//                List<TestCandidateMetadata> methodTestCandidates = sessionInstance.getTestCandidatesForAllMethod(query);
//                int candidateCount = methodTestCandidates.size();
//                if (candidateCount > 0) {
//                    TestCandidateMetadata mostRecentTestCandidate = methodTestCandidates.get(0);
//                    methodArgumentValues = TestCandidateUtils.buildArgumentValuesFromTestCandidate(
//                            mostRecentTestCandidate);
//                }
//            }
        }

        methodParameterContainer = new JPanel();
        BorderLayout boxLayout = new BorderLayout();
        methodParameterContainer.setLayout(boxLayout);

        parameterInputComponents.clear();
        Project project = methodElement.getProject();
        ProjectAndLibrariesScope projectAndLibrariesScope = new ProjectAndLibrariesScope(project);

        if (methodParameters.length > 0) {
//            methodParameterContainer.setLayout(new GridLayout(methodParameters.length, 1));
            Map<String, JsonNode> methodArgumentsMap = new HashMap<>();

            for (int i = 0; i < methodParameters.length; i++) {
                ParameterAdapter methodParameter = methodParameters[i];
//
                String parameterValue = "";
                PsiType methodParameterType = methodParameter.getType();
                if (methodArgumentValues != null && i < methodArgumentValues.size()) {
                    parameterValue = methodArgumentValues.get(i);
                } else {
                    parameterValue = ApplicationManager.getApplication()
                            .runReadAction((Computable<String>) () ->
                                    ClassUtils.createDummyValue(methodParameterType, new ArrayList<>(4),
                                            insidiousService.getProject()));
                }
                try {
                    methodArgumentsMap.put(methodParameter.getName(), objectMapper.readTree(parameterValue));
                } catch (JsonProcessingException e) {
                    methodArgumentsMap.put(methodParameter.getName(),
                            objectMapper.getNodeFactory().textNode(parameterValue));
                }

            }
            String source = "{}";
            try {
                source = objectMapper.writeValueAsString(methodArgumentsMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(source);
            } catch (JsonProcessingException e) {
                jsonNode = objectMapper.getNodeFactory().textNode(source);
            }
            parameterEditor = new JsonTreeEditor(jsonNode, "Method Arguments", true, executeAction);
            parameterEditor.setEditable(true);
        } else {
            parameterEditor = new JsonTreeEditor(objectMapper.getNodeFactory().objectNode(), "No method arguments",
                    false, executeAction);
        }

        methodParameterContainer.add(parameterEditor.getComponent(), BorderLayout.CENTER);

        if (parameterScrollPanel == null) {
            parameterScrollPanel = new JBScrollPane(methodParameterContainer);
            parameterScrollPanel.setBorder(BorderFactory.createEmptyBorder());

        }


        ApplicationManager.getApplication().invokeLater(() -> {
            centerPanel.removeAll();
            parameterScrollPanel.setViewportView(methodParameterContainer);
            centerPanel.add(parameterScrollPanel, BorderLayout.CENTER);
            parameterScrollPanel.revalidate();
            parameterScrollPanel.repaint();
            centerPanel.revalidate();
            centerPanel.repaint();
        });

    }

    public JComponent getComponent() {
        return mainContainer;
    }

    @Override
    public String getTitle() {
        return "Direct Invoke";
    }

    public void triggerExecute() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            executeMethodWithParameters();
        });
    }

    private void classSelected(ClassUnderTest psiClass) {
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("className", psiClass.getQualifiedClassName());
        eventProperties.put("methodName", methodElement.getName());
        eventProperties.put("methodSignature", methodElement.getJVMSignature());

        UsageInsightTracker.getInstance().RecordEvent("DIRECT_INVOKE", eventProperties);
        List<String> methodArgumentValues = new ArrayList<>();
        ParameterAdapter[] parameters = methodElement.getParameters();
        for (int i = 0; i < parameters.length; i++) {
//                ParameterInputComponent parameterInputComponent = parameterInputComponents.get(i);
            ParameterAdapter parameter = parameters[i];

            String selectedKey = "/" + ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) parameter::getName);

            JsonNode valueFromJsonNode = parameterEditor.getValue();

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
                        methodArgumentValues, false, null);
        agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);

        if (returnValueTextArea != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    returnValueTextArea.getDocument().setText("Executing...");
                });
            });
        }

        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                (agentCommandRequest1, agentCommandResponse) -> {
                    ApplicationManager.getApplication().invokeLater(() -> {

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

                        String toolTipText = "Timestamp: " +
                                new Timestamp(agentCommandResponse.getTimestamp()) + " from "
                                + targetClassName + "." + targetMethodName + "( " + " )";

                        if (responseType == null) {
                            returnValueTextArea.getDocument().setText(responseMessage + "\n" + methodReturnValue);
                            return;
                        }

                        if (responseType.equals(ResponseType.NORMAL)) {

                            try {
                                String returnValueString = String.valueOf(methodReturnValue);

                                String responseClassName = agentCommandResponse.getResponseClassName();
                                if (responseClassName.equals("float") ||
                                        responseClassName.equals("java.lang.Float")) {
                                    returnValueString = ParameterUtils.getFloatValue(returnValueString);
                                }

                                if (responseClassName.equals("double") ||
                                        responseClassName.equals("java.lang.Double")) {
                                    returnValueString = ParameterUtils.getDoubleValue(returnValueString);
                                }

                                JsonNode jsonNode = null;
                                if (responseClassName.equals("java.lang.String")) {
                                    JsonNodeFactory jsonNodeFactory = objectMapper.getNodeFactory();
                                    jsonNode = jsonNodeFactory.objectNode().put("String", returnValueString);
                                } else {
                                    jsonNode = objectMapper.readTree(returnValueString);
                                }

                                // pass execute and modify buttons
                                JsonTreeEditor jsonTreeEditor = new JsonTreeEditor(jsonNode, responseClassName, false,
                                        this.executeAction, this.modifyArgumentsAction);
                                parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());

                            } catch (JsonProcessingException ex) {
                                JsonTreeEditor jsonTreeEditor = getJsonTreeEditor(methodReturnValue.toString());

                                parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());
                            }
                        } else if (responseType.equals(ResponseType.EXCEPTION)) {

                            if (methodReturnValue != null) {
                                String exceptionString = ExceptionUtils.prettyPrintException(
                                        methodReturnValue.toString());

                                String editorText = ExceptionUtils.prettyPrintException(exceptionString);
                                JsonTreeEditor jsonTreeEditor = getJsonTreeEditor(editorText);

                                parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());


                            } else {
                                String editorText = agentCommandResponse.getMessage();
                                JsonTreeEditor jsonTreeEditor = getJsonTreeEditor(editorText);

                                parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());

                            }
                        } else if (responseType.equals(ResponseType.FAILED)) {

                            if (methodReturnValue != null) {
                                String editorText = ExceptionUtils.prettyPrintException(
                                        methodReturnValue.toString());
                                JsonTreeEditor jsonTreeEditor = getJsonTreeEditor(editorText);

                                parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());

                            } else {
                                String editorText = ExceptionUtils.prettyPrintException(
                                        agentCommandResponse.getMessage());
                                JsonTreeEditor jsonTreeEditor = getJsonTreeEditor(editorText);

                                parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());


                            }
                        } else {
                            String editorText = responseMessage + methodReturnValue;
                            JsonTreeEditor jsonTreeEditor = getJsonTreeEditor(editorText);

                            parameterScrollPanel.setViewportView(jsonTreeEditor.getComponent());


                        }

                        ResponseType responseType1 = agentCommandResponse.getResponseType();
                        DiffResultType diffResultType = responseType1.equals(
                                ResponseType.NORMAL) ? DiffResultType.NO_ORIGINAL : DiffResultType.ACTUAL_EXCEPTION;
                        DifferenceResult diffResult = new DifferenceResult(null,
                                diffResultType, null,
                                DiffUtils.getFlatMapFor(agentCommandResponse.getMethodReturnValue()));
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE);
                        diffResult.setResponse(agentCommandResponse);
                        diffResult.setCommand(agentCommandRequest);
                        insidiousService.addExecutionRecord(new AutoExecutorReportRecord(diffResult,
                                insidiousService.getSessionInstance().getProcessedFileCount(),
                                insidiousService.getSessionInstance().getTotalFileCount()));
                    });
                });
    }

    private JsonTreeEditor getJsonTreeEditor(String editorText) {
        if (returnValueTextArea != null) {
            EditorFactory.getInstance().releaseEditor(returnValueTextArea);
            returnValueTextArea = null;
        }
        Document document = EditorFactory.getInstance().createDocument(editorText);
        returnValueTextArea = EditorFactory.getInstance().createEditor(document, insidiousService.getProject());
        JsonTreeEditor jsonTreeEditor = new JsonTreeEditor(returnValueTextArea, false,
                this.executeAction, this.modifyArgumentsAction);
        return jsonTreeEditor;
    }

    private void chooseClassAndDirectInvoke() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            insidiousService.chooseClassImplementation(methodElement.getContainingClass().getQualifiedName(),
                    this::classSelected);
        });
    }

    @Override
    public void dispose() {
        if (returnValueTextArea != null) {
            EditorFactory.getInstance().releaseEditor(returnValueTextArea);
            returnValueTextArea = null;
        }
    }

    public void setMethod(MethodAdapter method) {
        this.methodElement = method;
    }

}
