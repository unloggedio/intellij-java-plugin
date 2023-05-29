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
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MethodDirectInvokeComponent {
    private static final Logger logger = LoggerUtil.getInstance(MethodDirectInvokeComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final List<String> creationStack = new LinkedList<>();
    private JPanel mainContainer;
    private JPanel actionControlPanel;
    private JScrollPane returnValuePanel;
    private JTextArea returnValueTextArea;
    private JPanel methodParameterScrollContainer;
    private JPanel scrollerContainer;
    private JLabel methodNameLabel;
    private JButton executeButton;
    private MethodAdapter methodElement;

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
        if (!agentStateProvider.doesAgentExist()) {
            InsidiousNotification.notifyMessage("Start your application with Unlogged JAVA agent to start using " +
                    "method DirectInvoke", NotificationType.INFORMATION);
            insidiousService.updateScaffoldForState(GutterState.NO_AGENT);
            return;
        }

        if (!agentStateProvider.isAgentRunning()) {
            InsidiousNotification.notifyMessage("Start your application with Unlogged JAVA agent to start using " +
                    "method DirectInvoke", NotificationType.INFORMATION);
            insidiousService.updateScaffoldForState(GutterState.PROCESS_NOT_RUNNING);
            return;
        }

        if (methodElement == null) {
            InsidiousNotification.notifyMessage("No method selected in editor.", NotificationType.WARNING);
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


            insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                    (agentCommandRequest1, agentCommandResponse) -> {
                        logger.warn("Agent command execution response: " + agentCommandResponse);

                        ResponseType responseType = agentCommandResponse.getResponseType();
                        String responseMessage = agentCommandResponse.getMessage() == null ? "" :
                                agentCommandResponse.getMessage() + "\n";
                        TitledBorder panelTitledBoarder = (TitledBorder) scrollerContainer.getBorder();
                        String responseObjectClassName = agentCommandResponse.getResponseClassName();
                        Object methodReturnValue = agentCommandResponse.getMethodReturnValue();
                        if (responseType == null) {
                            panelTitledBoarder.setTitle("Method response: " + responseObjectClassName);
                            returnValueTextArea.setText(responseMessage + methodReturnValue);
                            return;
                        }

                        if (responseType.equals(ResponseType.NORMAL)) {
                            String returnTypePresentableText = ApplicationManager.getApplication()
                                    .runReadAction(
                                            (Computable<String>) () -> methodElement.getReturnType()
                                                    .getPresentableText());
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
                    });
        });


    }

    public void renderForMethod(MethodAdapter methodElement) {
        if (methodElement == null || methodElement.equals(this.methodElement)) {
            return;
        }

//        executeButton.setToolTipText("");
//        executeButton.setEnabled(true);
        executeButton.revalidate();
        executeButton.repaint();

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
                        .getTestCandidatesForAllMethod(classQualifiedName, methodName, false);
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

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(0, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                ParameterAdapter methodParameter = methodParameters[i];

                String parameterValue = "";
                if (methodArgumentValues != null && i < methodArgumentValues.size()) {
                    parameterValue = methodArgumentValues.get(i);
                } else {
                    PsiType parameterType = methodParameter.getType();
                    parameterValue = ClassUtils.createDummyValue(parameterType, new LinkedList<>(),
                            insidiousService.getProject());
                    creationStack.clear();
                }

                ParameterInputComponent parameterContainer =
                        new ParameterInputComponent(methodParameter, parameterValue);
                parameterInputComponents.add(parameterContainer);
                JPanel content = parameterContainer.getContent();
                content.setMinimumSize(new Dimension(-1, 150));
                methodParameterContainer.add(content);
            }
        } else {
            JBLabel noParametersLabel = new JBLabel("Method " + methodName + "() has no parameters");
            methodParameterContainer.add(noParametersLabel);
        }

        methodParameterScrollContainer.removeAll();

        JBScrollPane parameterScrollPanel = new JBScrollPane(methodParameterContainer);
        parameterScrollPanel.setSize(new Dimension(-1, 500));
        parameterScrollPanel.setBorder(BorderFactory.createEmptyBorder());
//        parameterScrollPanel.setMinimumSize(new Dimension(-1, 500));
//        parameterScrollPanel.setMaximumSize(new Dimension(-1, 500));

        methodParameterScrollContainer.setMinimumSize(new Dimension(-1, Math.min(methodParameters.length * 150, 150)));
        methodParameterScrollContainer.add(parameterScrollPanel, BorderLayout.CENTER);
//        methodParameterScrollContainer.revalidate();
//        methodParameterScrollContainer.repaint();
        mainContainer.revalidate();
        mainContainer.repaint();
    }


    public JComponent getContent() {
        return mainContainer;
    }
}
