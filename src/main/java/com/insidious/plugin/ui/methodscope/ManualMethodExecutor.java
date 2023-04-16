package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.ExceptionUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.MethodUtils;
import com.insidious.plugin.util.TestCandidateUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ManualMethodExecutor {
    private static final Logger logger = LoggerUtil.getInstance(ManualMethodExecutor.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private JPanel mainContainer;
    private JPanel methodParameterContainer;
    private JPanel actionControlPanel;
    private JButton executeButton;
    private JScrollPane returnValuePanel;
    private JTextArea returnValueTextArea;
    private MethodAdapter methodElement;

    public ManualMethodExecutor(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        executeButton.addActionListener(e -> {
            List<String> methodArgumentValues = new ArrayList<>();
            for (ParameterInputComponent parameterInputComponent : parameterInputComponents) {
                methodArgumentValues.add(parameterInputComponent.getParameterValue());
            }

            ParameterAdapter[] parameters = methodElement.getParameters();

//            Map<String, String> parameterInputMap = new TreeMap<>();
//            if (parameters != null) {
//                for (int i = 0; i < parameters.length; i++) {
//                    ParameterAdapter methodParameter = parameters[i];
//                    String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
//                    parameterInputMap.put(methodParameter.getName(), parameterValue);
//                }
//            }

            AgentCommandRequest agentCommandRequest =
                    MethodUtils.createRequestWithParameters(methodElement, methodArgumentValues);
            agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
            returnValueTextArea.setText("");
            insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                    (request, agentCommandResponse) -> {
                        logger.warn("Agent command execution response: " + agentCommandResponse);


                        ResponseType responseType = agentCommandResponse.getResponseType();
                        String responseMessage = agentCommandResponse.getMessage() == null ? "" :
                                agentCommandResponse.getMessage() + "\n";
                        if (responseType == null) {
                            ((TitledBorder) returnValuePanel.getBorder()).setTitle(
                                    agentCommandResponse.getResponseClassName());
                            returnValueTextArea.setText(responseMessage
                                    + agentCommandResponse.getMethodReturnValue());
                            return;
                        }

                        if (responseType.equals(ResponseType.NORMAL)) {
                            ((TitledBorder) returnValuePanel.getBorder()).setTitle(
                                    methodElement.getReturnType().getPresentableText());
                            ObjectMapper objectMapper = insidiousService.getObjectMapper();
                            try {
                                JsonNode jsonNode = objectMapper.readValue(agentCommandResponse.getMethodReturnValue(),
                                        JsonNode.class);
                                returnValueTextArea.setText(objectMapper
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(jsonNode));
                                returnValueTextArea.setLineWrap(true);
                            } catch (JsonProcessingException ex) {
                                throw new RuntimeException(ex);
                            }
                        } else if (responseType.equals(ResponseType.EXCEPTION)) {
                            ((TitledBorder) returnValuePanel.getBorder()).setTitle(
                                    agentCommandResponse.getResponseClassName());
                            returnValueTextArea.setText(
                                    ExceptionUtils.prettyPrintException(agentCommandResponse.getMethodReturnValue()));
                        } else {
                            ((TitledBorder) returnValuePanel.getBorder()).setTitle(
                                    agentCommandResponse.getResponseClassName());
                            returnValueTextArea.setText(responseMessage + agentCommandResponse.getMethodReturnValue());
                        }
                    });

        });

    }

    public void renderForMethod(MethodAdapter methodElement) {
        if (methodElement == null || methodElement.equals(this.methodElement)) {
            return;
        }
        String methodName = methodElement.getName();
        String classQualifiedName = methodElement.getContainingClass().getQualifiedName();

        logger.warn("render method executor for: " + methodName);
        this.methodElement = methodElement;
        ParameterAdapter[] methodParameters = methodElement.getParameters();


        List<TestCandidateMetadata> methodTestCandidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(classQualifiedName, methodName, false);


        TestCandidateMetadata mostRecentTestCandidate = null;
        List<String> methodArgumentValues = null;
        if (methodTestCandidates.size() > 0) {
            mostRecentTestCandidate = methodTestCandidates.get(methodTestCandidates.size() - 1);
            methodArgumentValues = TestCandidateUtils.buildArgumentValuesFromTestCandidate(
                    mostRecentTestCandidate);
        }

        methodParameterContainer.removeAll();
        parameterInputComponents.clear();

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(0, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                ParameterAdapter methodParameter = methodParameters[i];
                String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                ParameterInputComponent parameterContainer = new ParameterInputComponent(methodParameter,
                        parameterValue);
                parameterInputComponents.add(parameterContainer);
                methodParameterContainer.add(parameterContainer.getContent());
            }
        } else {
            JBLabel noParametersLabel = new JBLabel("Method " + methodName + "() has no parameters");
            methodParameterContainer.add(noParametersLabel);
        }
    }

    public JComponent getContent() {
        return mainContainer;
    }
}
