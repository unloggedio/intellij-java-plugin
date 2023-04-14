package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ManualMethodExecutor {
    private static final Logger logger = LoggerUtil.getInstance(ManualMethodExecutor.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private JPanel mainContainer;
    private JPanel methodParameterContainer;
    private JPanel actionControlPanel;
    private JButton executeButton;
    private PsiMethod methodElement;

    public ManualMethodExecutor(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        executeButton.addActionListener(e -> {
            List<String> methodArgumentValues = new ArrayList<>();
            for (ParameterInputComponent parameterInputComponent : parameterInputComponents) {
                methodArgumentValues.add(parameterInputComponent.getParameterValue());
            }

            JvmParameter[] parameters = methodElement.getParameters();

            Map<String, String> parameterInputMap = new TreeMap<>();
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    JvmParameter methodParameter = parameters[i];
                    String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                    parameterInputMap.put(methodParameter.getName(), parameterValue);
                }
            }

//            execute(null, methodArgumentValues, parameterInputMap);
        });

    }

    public void renderForMethod(PsiMethod methodElement) {
        logger.warn("render method executor for: " + methodElement.getName());
        this.methodElement = methodElement;
        JvmParameter[] methodParameters = methodElement.getParameters();

        List<TestCandidateMetadata> methodTestCandidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(methodElement.getContainingClass().getQualifiedName(),
                        methodElement.getName(),
                        false);


        TestCandidateMetadata mostRecentTestCandidate = null;
        List<String> methodArgumentValues = null;
        if (methodTestCandidates.size() > 0) {
            mostRecentTestCandidate = methodTestCandidates.get(methodTestCandidates.size() - 1);
            methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    mostRecentTestCandidate);
        }

        methodParameterContainer.removeAll();
        parameterInputComponents.clear();

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(0, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                JvmParameter methodParameter = methodParameters[i];
                String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                ParameterInputComponent parameterContainer = new ParameterInputComponent(methodParameter,
                        parameterValue);
                parameterInputComponents.add(parameterContainer);
                methodParameterContainer.add(parameterContainer.getContent());
            }
        } else {
            JBLabel noParametersLabel = new JBLabel("Method has no parameters");
            methodParameterContainer.add(noParametersLabel);
        }
    }
}
