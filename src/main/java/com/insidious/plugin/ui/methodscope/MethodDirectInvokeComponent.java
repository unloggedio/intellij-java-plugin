package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.ExceptionUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.MethodUtils;
import com.insidious.plugin.util.TestCandidateUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Date;
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
    private JButton executeButton;
    private JScrollPane returnValuePanel;
    private JTextArea returnValueTextArea;
    private JPanel methodParameterScrollContainer;
    private JPanel scrollerContainer;
    private JLabel methodNameLabel;
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
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("className", methodElement.getContainingClass().getQualifiedName());
        eventProperties.put("methodName", methodElement.getName());

        UsageInsightTracker.getInstance().RecordEvent("DIRECT_INVOKE", eventProperties);
        List<String> methodArgumentValues = new ArrayList<>();
        ParameterAdapter[] parameters = methodElement.getParameters();
        for (int i = 0; i < parameterInputComponents.size(); i++) {
            ParameterInputComponent parameterInputComponent = parameterInputComponents.get(i);
            ParameterAdapter parameter = parameters[i];
            String parameterValue = parameterInputComponent.getParameterValue();
            if (parameter.getType().equals("java.lang.String") && !parameterValue.startsWith("\"")) {
                try {
                    parameterValue = objectMapper.writeValueAsString(parameterValue);
                } catch (JsonProcessingException e) {
                    // should never happen
                }
            }
            methodArgumentValues.add(parameterValue);
        }

        AgentCommandRequest agentCommandRequest =
                MethodUtils.createRequestWithParameters(methodElement, methodArgumentValues);
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
                                        (Computable<String>) () -> methodElement.getReturnType().getPresentableText());
                        panelTitledBoarder.setTitle("Method response: " + returnTypePresentableText);
                        ObjectMapper objectMapper = insidiousService.getObjectMapper();
                        try {
                            JsonNode jsonNode = objectMapper.readValue(methodReturnValue.toString(), JsonNode.class);
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
    }

    public void renderForMethod(MethodAdapter methodElement) {
        if (methodElement == null || methodElement.equals(this.methodElement)) {
            return;
        }

        executeButton.setToolTipText("");
        executeButton.setEnabled(true);

        String methodName = methodElement.getName();
        String classQualifiedName = methodElement.getContainingClass().getQualifiedName();

        logger.warn("render method executor for: " + methodName);
        this.methodElement = methodElement;
        String methodNameForLabel = methodName.length() > 25 ? methodName.substring(0, 25) : methodName;
        methodNameLabel.setText(methodNameForLabel);
        TitledBorder titledBorder = (TitledBorder) actionControlPanel.getBorder();
        titledBorder.setTitle(methodElement.getContainingClass().getName());


        ParameterAdapter[] methodParameters = methodElement.getParameters();


//        TestCandidateMetadata mostRecentTestCandidate = null;
        List<String> methodArgumentValues = null;
        AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(methodElement,
                methodArgumentValues);

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
                    parameterValue = createDummyValue(parameterType);
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
//        parameterScrollPanel.setMinimumSize(new Dimension(-1, 500));
//        parameterScrollPanel.setMaximumSize(new Dimension(-1, 500));

        methodParameterScrollContainer.add(parameterScrollPanel, BorderLayout.CENTER);
        methodParameterScrollContainer.revalidate();
        methodParameterScrollContainer.repaint();
    }

    private String createDummyValue(PsiType parameterType) {
        String creationKey = parameterType.getCanonicalText();
        if (creationStack.contains(creationKey)) {
            return "";
        }

        try {
            creationStack.add(creationKey);
            StringBuilder dummyValue = new StringBuilder();
            if (parameterType.getCanonicalText().equals("java.lang.String")) {
                return "\"string\"";
            }
            if (parameterType.getCanonicalText().startsWith("java.lang.")) {
                return "0";
            }

            if (parameterType.getCanonicalText().equals("java.util.Random")) {
                return "";
            }
            if (parameterType.getCanonicalText().equals("java.util.Date")) {
//                try {
                return "\"" + new Date().toGMTString() + "\"";
//                } catch (JsonProcessingException e) {
//                     should never happen
//                }
            }
            if (parameterType instanceof PsiArrayType) {
                PsiArrayType arrayType = (PsiArrayType) parameterType;
                dummyValue.append("[");
                dummyValue.append(createDummyValue(arrayType.getComponentType()));
                dummyValue.append("]");
                return dummyValue.toString();
            }

            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                if (classReferenceType.rawType().getCanonicalText().equals("java.util.List") ||
                        classReferenceType.rawType().getCanonicalText().equals("java.util.Set")
                ) {
                    dummyValue.append("[");
                    dummyValue.append(createDummyValue(classReferenceType.getParameters()[0]));
                    dummyValue.append("]");
                    return dummyValue.toString();
                }

                PsiClass resolvedClass = JavaPsiFacade.getInstance(
                                insidiousService.getProject())
                        .findClass(classReferenceType.getCanonicalText(), parameterType.getResolveScope());

                if (resolvedClass == null) {
                    // class not resolved
                    return dummyValue.toString();
                }

                if (resolvedClass.isEnum()) {
                    PsiField[] enumValues = resolvedClass.getAllFields();
                    if (enumValues.length == 0) {
                        return "";
                    }
                    return "\"" + enumValues[0].getName() + "\"";
                }

                PsiField[] parameterObjectFieldList = resolvedClass.getAllFields();
//                Map<String, String> fieldValueMap = new HashMap<>();
                dummyValue.append("{");
                boolean firstField = true;
                for (PsiField psiField : parameterObjectFieldList) {
                    if (!firstField) {
                        dummyValue.append(", ");
                    }
//                    fieldValueMap.put(psiField.getName(), createDummyValue(psiField.getType()));
                    dummyValue.append("\"");
                    dummyValue.append(psiField.getName());
                    dummyValue.append("\"");
                    dummyValue.append(": ");
                    dummyValue.append(createDummyValue(psiField.getType()));
                    firstField = false;
                }
                dummyValue.append("}");
            } else if (parameterType instanceof PsiPrimitiveType) {
                PsiPrimitiveType primitiveType = (PsiPrimitiveType) parameterType;
                switch (primitiveType.getName()) {
                    case "boolean":
                        return "true";
                }
                return "0";
            }
            return dummyValue.toString();

        } finally {
            creationStack.remove(creationKey);
        }

    }

    public JComponent getContent() {
        return mainContainer;
    }
}
