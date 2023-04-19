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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class MethodDirectInvokeComponent {
    private static final Logger logger = LoggerUtil.getInstance(MethodDirectInvokeComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private JPanel mainContainer;
    private JPanel actionControlPanel;
    private JButton executeButton;
    private JScrollPane returnValuePanel;
    private JTextArea returnValueTextArea;
    private JPanel methodParameterScrollContainer;
    private MethodAdapter methodElement;

    public MethodDirectInvokeComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.objectMapper = this.insidiousService.getObjectMapper();

        executeButton.addActionListener(e -> executeMethodWithParameters());
        methodParameterScrollContainer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeMethodWithParameters();
                }
            }
        });

    }

    private void executeMethodWithParameters() {
        List<String> methodArgumentValues = new ArrayList<>();
        for (ParameterInputComponent parameterInputComponent : parameterInputComponents) {
            methodArgumentValues.add(parameterInputComponent.getParameterValue());
        }

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
                        returnValueTextArea.setText(responseMessage + agentCommandResponse.getMethodReturnValue());
                        return;
                    }

                    if (responseType.equals(ResponseType.NORMAL)) {
                        String returnTypePresentableText = ApplicationManager.getApplication()
                                .runReadAction((Computable<String>) () ->
                                        methodElement.getReturnType().getPresentableText());
                        ((TitledBorder) returnValuePanel.getBorder()).setTitle(
                                returnTypePresentableText);
                        ObjectMapper objectMapper = insidiousService.getObjectMapper();
                        try {
                            JsonNode jsonNode = objectMapper.readValue(agentCommandResponse.getMethodReturnValue(),
                                    JsonNode.class);
                            returnValueTextArea.setText(objectMapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(jsonNode));
                            returnValueTextArea.setLineWrap(false);
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

        JPanel methodParameterContainer = new JPanel();

        parameterInputComponents.clear();

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(0, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                ParameterAdapter methodParameter = methodParameters[i];

                String parameterValue = "";
                if (methodArgumentValues != null) {
                    parameterValue = methodArgumentValues.get(i);
                } else {
                    PsiType parameterType = methodParameter.getType();
                    parameterValue = createDummyValue(parameterType);
                }

                ParameterInputComponent parameterContainer =
                        new ParameterInputComponent(methodParameter, parameterValue);
                parameterInputComponents.add(parameterContainer);
                methodParameterContainer.add(parameterContainer.getContent());
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
            try {
                return "\"" + objectMapper.writeValueAsString(new Date()) + "\"";
            } catch (JsonProcessingException e) {
                // should never happen
            }
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
            if (classReferenceType.rawType().getCanonicalText().equals("java.util.List")) {
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
            Map<String, String> fieldValueMap = new HashMap<>();
            dummyValue.append("{");
            boolean firstField = true;
            for (PsiField psiField : parameterObjectFieldList) {
                if (!firstField) {
                    dummyValue.append(", ");
                }
                fieldValueMap.put(psiField.getName(), createDummyValue(psiField.getType()));
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
    }

    public JComponent getContent() {
        return mainContainer;
    }
}
