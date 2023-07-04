package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.AgentStateProvider;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.*;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MethodDirectInvokeComponent extends KeyAdapter {
    private static final Logger logger = LoggerUtil.getInstance(MethodDirectInvokeComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private JPanel mainContainer;
    private JPanel actionControlPanel;
    private JScrollPane returnValuePanel;
    private JTextArea returnValueTextArea;
    private JPanel methodParameterScrollContainer;
    private JPanel scrollerContainer;
    private JLabel methodNameLabel;
    private JButton executeButton;
    private MethodAdapter methodElement;
    private KeyAdapter NOP_KEY_ADAPTER = new KeyAdapter() {
    };

    public MethodDirectInvokeComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.objectMapper = this.insidiousService.getObjectMapper();

        executeButton.addActionListener(e -> executeMethodWithParameters());
        methodParameterScrollContainer.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeMethodWithParameters();
                }
            }
        });

    }

    private void executeMethodWithParameters() {

        AgentStateProvider agentStateProvider = insidiousService.getAgentStateProvider();

        if (!agentStateProvider.isAgentRunning()) {
            String message = "Start your application with Unlogged JAVA agent to start using " +
                    "method DirectInvoke";
            InsidiousNotification.notifyMessage(message, NotificationType.INFORMATION);
            returnValueTextArea.setText(message);
            insidiousService.updateScaffoldForState(GutterState.PROCESS_NOT_RUNNING);
            return;
        }

        if (methodElement == null) {
            String message = "No method selected in editor.";
            returnValueTextArea.setText(message);
            InsidiousNotification.notifyMessage(message, NotificationType.WARNING);
            return;
        }


        ClassUtils.chooseClassImplementation(methodElement.getContainingClass(), psiClass -> {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("className", psiClass.getQualifiedName());
            eventProperties.put("methodName", methodElement.getName());

            UsageInsightTracker.getInstance().RecordEvent("DIRECT_INVOKE", eventProperties);
            List<String> methodArgumentValues = new ArrayList<>();
            ParameterAdapter[] parameters = methodElement.getParameters();
            for (int i = 0; i < parameterInputComponents.size(); i++) {
                ParameterInputComponent parameterInputComponent = parameterInputComponents.get(i);
                ParameterAdapter parameter = parameters[i];
                String parameterValue = parameterInputComponent.getParameterValue();
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
                    MethodUtils.createRequestWithParameters(methodElement, psiClass, methodArgumentValues);
            agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
            returnValueTextArea.setText("");


            returnValueTextArea.setText("Executing method [" + agentCommandRequest.getMethodName() + "()]\nin class ["
                    + agentCommandRequest.getClassName() + "].\nWaiting for response...");
            insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                    (agentCommandRequest1, agentCommandResponse) -> {
                        logger.warn("Agent command execution response: " + agentCommandResponse);
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
                        TitledBorder panelTitledBoarder = (TitledBorder) scrollerContainer.getBorder();
                        String responseObjectClassName = agentCommandResponse.getResponseClassName();
                        Object methodReturnValue = agentCommandResponse.getMethodReturnValue();


                        String targetClassName = agentCommandResponse.getTargetClassName();
                        if (targetClassName == null) {
                            targetClassName = agentCommandRequest.getClassName();
                        }
                        targetClassName = targetClassName.substring(targetClassName.lastIndexOf(".") + 1);
                        returnValueTextArea.setToolTipText("Timestamp: " +
                                new Timestamp(agentCommandResponse.getTimestamp()).toString() + " from "
                                + targetClassName + "." + agentCommandResponse.getTargetMethodName() + "( " + " )");
                        if (responseType == null) {
                            panelTitledBoarder.setTitle("Method response: " + responseObjectClassName);
                            returnValueTextArea.setText(responseMessage + methodReturnValue);
                            return;
                        }

                        if (responseType.equals(ResponseType.NORMAL)) {
                            String returnTypePresentableText = ApplicationManager.getApplication()
                                    .runReadAction(
                                            (Computable<String>) () -> {
                                                PsiType returnType = methodElement.getReturnType();
                                                if (returnType == null) {
                                                    return "void";
                                                }
                                                return returnType.getPresentableText();
                                            });
                            panelTitledBoarder.setTitle("Method response: " + returnTypePresentableText);
                            ObjectMapper objectMapper = insidiousService.getObjectMapper();
                            try {
                                JsonNode jsonNode = objectMapper.readValue(methodReturnValue.toString(),
                                        JsonNode.class);
                                returnValueTextArea.setText(objectMapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(jsonNode));
                            } catch (JsonProcessingException ex) {
                                returnValueTextArea.setText(methodReturnValue.toString());
                            }
                        } else if (responseType.equals(ResponseType.EXCEPTION)) {
                            panelTitledBoarder.setTitle("Method response: " + responseObjectClassName);
                            if (methodReturnValue != null) {
                                returnValueTextArea.setText(
                                        ExceptionUtils.prettyPrintException(methodReturnValue.toString()));
                            } else {
                                returnValueTextArea.setText(agentCommandResponse.getMessage());
                            }
                        } else {
                            panelTitledBoarder.setTitle("Method response: " + responseObjectClassName);
                            returnValueTextArea.setText(responseMessage + methodReturnValue);
                        }
                        scrollerContainer.revalidate();
                        scrollerContainer.repaint();

                        ResponseType responseType1 = agentCommandResponse.getResponseType();
                        DifferenceResult diffResult = new DifferenceResult(null,
                                responseType1.equals(
                                        ResponseType.NORMAL) ? DiffResultType.NO_ORIGINAL : DiffResultType.ACTUAL_EXCEPTION,
                                null,
                                DiffUtils.getFlatMapFor(agentCommandResponse.getMethodReturnValue()));
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE);
                        diffResult.setMethodAdapter(methodElement);
                        diffResult.setResponse(agentCommandResponse);
                        diffResult.setCommand(agentCommandRequest);
                        insidiousService.addExecutionRecord(diffResult);
                    });
        });


    }

    public void renderForMethod(MethodAdapter methodElement) {
        if (methodElement == null) {
            logger.info("DirectInvoke got null method");
            return;
        }
        if (this.methodElement == methodElement) {
            return;
        }

        clearOutputSection();

        String methodName = methodElement.getName();
        ClassAdapter containingClass = methodElement.getContainingClass();
        String classQualifiedName = containingClass.getQualifiedName();

        logger.warn("render method executor for: " + methodName);
        this.methodElement = methodElement;
        String methodNameForLabel = methodName.length() > 25 ? methodName.substring(0, 25) : methodName;
        methodNameLabel.setText(methodNameForLabel);
        TitledBorder titledBorder = (TitledBorder) actionControlPanel.getBorder();
        titledBorder.setTitle(containingClass.getName());


        ParameterAdapter[] methodParameters = methodElement.getParameters();


//        TestCandidateMetadata mostRecentTestCandidate = null;
        List<String> methodArgumentValues = null;
        AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(methodElement,
                (PsiClass) containingClass.getSource(), methodArgumentValues);

        AgentCommandRequest existingRequests = insidiousService.getAgentCommandRequests(agentCommandRequest);
        if (existingRequests != null) {
            methodArgumentValues = existingRequests.getMethodParameters();
        } else {
            SessionInstance sessionInstance = this.insidiousService.getSessionInstance();
            if (sessionInstance != null) {
                List<TestCandidateMetadata> methodTestCandidates = sessionInstance
                        .getTestCandidatesForAllMethod(classQualifiedName, methodName,
                                insidiousService.getMethodArgsDescriptor(methodElement), false);
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
        Project project = containingClass.getProject();
        ProjectAndLibrariesScope projectAndLibrariesScope = new ProjectAndLibrariesScope(project);

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(methodParameters.length, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                ParameterAdapter methodParameter = methodParameters[i];

                String parameterValue = "";
                PsiType methodParameterType = methodParameter.getType();
                if (methodArgumentValues != null && i < methodArgumentValues.size()) {
                    parameterValue = methodArgumentValues.get(i);
                } else {
                    parameterValue = ClassUtils.createDummyValue(methodParameterType, new ArrayList<>(4),
                            insidiousService.getProject());
                }

                ParameterInputComponent parameterContainer;
                JPanel content;
                String typeCanonicalName = methodParameterType.getCanonicalText();
                if (methodParameters.length > 3
                        || methodParameterType instanceof PsiPrimitiveType
                        || isBoxedPrimitive(typeCanonicalName)
                ) {
                    parameterContainer = new ParameterInputComponent(methodParameter, parameterValue,
                            JBTextField.class, this);
                    content = parameterContainer.getContent();
                    content.setMaximumSize(new Dimension(-1, 80));
                } else {
                    PsiClass typeClassReference = JavaPsiFacade
                            .getInstance(project)
                            .findClass(typeCanonicalName, projectAndLibrariesScope);
                    if (typeClassReference != null && (typeClassReference.isEnum())) {
                        parameterContainer = new ParameterInputComponent(methodParameter, parameterValue,
                                JBTextField.class, NOP_KEY_ADAPTER);
                    } else {
                        parameterContainer = new ParameterInputComponent(methodParameter, parameterValue,
                                JBTextArea.class, NOP_KEY_ADAPTER);
                    }
                    content = parameterContainer.getContent();
                    content.setMinimumSize(new Dimension(-1, 150));
                    content.setMaximumSize(new Dimension(-1, 300));
                }


                parameterInputComponents.add(parameterContainer);
                methodParameterContainer.add(content);
            }
        } else {
            JBLabel noParametersLabel = new JBLabel("Method " + methodName + "() has no parameters");
            methodParameterContainer.add(noParametersLabel);
        }

//        Spacer spacer = new Spacer();
//        methodParameterContainer.add(spacer);
        methodParameterScrollContainer.removeAll();

        JBScrollPane parameterScrollPanel = new JBScrollPane(methodParameterContainer);
        parameterScrollPanel.setSize(new Dimension(-1, 500));
        parameterScrollPanel.setBorder(BorderFactory.createEmptyBorder());
//        parameterScrollPanel.setMinimumSize(new Dimension(-1, 500));
//        parameterScrollPanel.setMaximumSize(new Dimension(-1, 500));

        methodParameterScrollContainer.setMinimumSize(new Dimension(-1, Math.min(methodParameters.length * 100, 150)));
        methodParameterScrollContainer.add(parameterScrollPanel, BorderLayout.CENTER);
        mainContainer.revalidate();
        mainContainer.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        super.keyPressed(e);
        super.keyReleased(e);
        if (e.getKeyCode() == 10) {
            executeMethodWithParameters();
        }
    }

    private void clearOutputSection() {
        TitledBorder panelTitledBoarder = (TitledBorder) scrollerContainer.getBorder();
        panelTitledBoarder.setTitle("Method Response");
        returnValueTextArea.setText("");
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
}
