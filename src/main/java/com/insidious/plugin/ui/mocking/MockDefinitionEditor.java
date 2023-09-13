package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionEditor {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionEditor.class);
    private final MethodUnderTest methodUnderTest;
    private final DeclaredMock declaredMock;
    private final List<WhenParameterInputPanel> whenPanelList = new ArrayList<>();
    private final List<ThenParameterInputPanel> thenPanelList = new ArrayList<>();
    private final Project project;
    //    private final CancelOrOkListener<DeclaredMock> cancelOrOkListener;
    private JPanel mainPanel;
    private JPanel namePanel;
    private JTextField nameTextField;
    private JRadioButton passThruRadioButton;
    private JRadioButton returnNullByDefaultRadioButton;
    private JPanel titlePanel;
    private JPanel nameAndSettingPanel;
    private JPanel whenThenParentPanel;
    private JPanel whenPanel;
    private JPanel whenTitlePanel;
    private JPanel parameterListContainerPanel;
    private JPanel thenPanel;
    private JPanel thenTitlePanel;
    private JButton chainAnotherReturnButton;
    private JPanel returnItemList;
    private JPanel bottomControlPanel;
    private JButton saveButton;
    private JLabel callExpressionLabel;
    private JPanel mockTypeParentPanel;
    private String returnDummyValue;
    private String methodReturnTypeName;
    private JBPopup editorPopup;

    public MockDefinitionEditor(
            MethodUnderTest methodUnderTest,
            PsiMethodCallExpression methodCallExpression,
            Project project) {
        this.methodUnderTest = methodUnderTest;
        this.project = project;
        mockTypeParentPanel.setVisible(false);
        PsiMethod destinationMethod = methodCallExpression.resolveMethod();

        PsiType returnType = destinationMethod.getReturnType();

        returnDummyValue = ClassUtils.createDummyValue(returnType, new ArrayList<>(), destinationMethod.getProject());
        methodReturnTypeName = returnType.getCanonicalText();

        PsiClass parentClass = PsiTreeUtil.getParentOfType(methodCallExpression,
                PsiClass.class);
        if (parentClass == null) {
            InsidiousNotification.notifyMessage("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]", NotificationType.ERROR);
            throw new RuntimeException("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]");
        }
        String expressionText = methodCallExpression.getMethodExpression().getText();
        callExpressionLabel.setText(expressionText);

        PsiParameterList methodParameters = destinationMethod.getParameterList();
        List<ParameterMatcher> parameterList = new ArrayList<>();
        for (int i = 0; i < methodParameters.getParametersCount(); i++) {
//            String methodSignatureItem = methodSignatureItems.get(i);
            PsiParameter methodParameter = methodParameters.getParameter(i);
            PsiType parameterType = methodParameter.getType();
            String parameterTypeName = parameterType.getCanonicalText();
            ParameterMatcher parameterMatcher = new ParameterMatcher(methodParameter.getName(),
                    ParameterMatcherType.ANY, parameterTypeName);
            parameterList.add(parameterMatcher);
        }


        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();
        thenParameterList.add(createDummyThenParameter());
        this.declaredMock = new DeclaredMock(
                "mock response " + expressionText,
                methodUnderTest.getClassName(), parentClass.getQualifiedName(),
                methodCallExpression.getMethodExpression().getQualifier().getText(),
                methodUnderTest.getName(), parameterList, thenParameterList
        );
        updateUiValues();
        addListeners();
    }

    public MockDefinitionEditor(MethodUnderTest methodUnderTest, DeclaredMock declaredMock, Project project) {
        this.methodUnderTest = methodUnderTest;
        this.project = project;
        this.declaredMock = declaredMock;
//        this.cancelOrOkListener = cancelOrOkListener;
        updateUiValues();
        addListeners();
    }

    private void addListeners() {
        chainAnotherReturnButton.setVisible(false);
        chainAnotherReturnButton.addActionListener(e -> {
            ThenParameter newThenParameter = createDummyThenParameter();
            declaredMock.getThenParameter().add(newThenParameter);
            returnItemList.setLayout(new GridLayout(declaredMock.getThenParameter().size(), 1));
            ThenParameterInputPanel thenParameterInputPanel = new ThenParameterInputPanel(newThenParameter);
            GridConstraints constraints = new GridConstraints(
                    declaredMock.getThenParameter().size() - 1, 0, 1, 1, ANCHOR_NORTH,
                    FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK, SIZEPOLICY_FIXED,
                    new Dimension(-1, 75),
                    new Dimension(-1, 75),
                    new Dimension(-1, 75)
            );

            returnItemList.add(thenParameterInputPanel.getComponent(), constraints);
            thenPanelList.add(thenParameterInputPanel);
            returnItemList.revalidate();
            returnItemList.repaint();
        });

        saveButton.addActionListener(e -> {
            InsidiousService insidiousService = project.getService(InsidiousService.class);
            insidiousService.saveMockDefinition(declaredMock, methodUnderTest);
            editorPopup.cancel();
        });
    }

    public DeclaredMock getDeclaredMock() {
        return declaredMock;
    }

    @NotNull
    private ThenParameter createDummyThenParameter() {
        ReturnValue returnValue = new ReturnValue(returnDummyValue, methodReturnTypeName, ReturnValueType.REAL);
        return new ThenParameter(returnValue, MethodExitType.NORMAL);
    }

    private void updateUiValues() {
        parameterListContainerPanel.removeAll();
        whenPanelList.clear();
        thenPanelList.clear();


        nameTextField.setText(declaredMock.getName());

        List<ParameterMatcher> whenParameter = declaredMock.getWhenParameter();
        parameterListContainerPanel.setLayout(new GridLayout(whenParameter.size(), 1));
        for (int i = 0; i < whenParameter.size(); i++) {
            ParameterMatcher parameterMatcher = whenParameter.get(i);
            WhenParameterInputPanel parameterInputPanel = new WhenParameterInputPanel(parameterMatcher);
            GridConstraints constraints = new GridConstraints(
                    i, 0, 1, 1, ANCHOR_NORTH,
                    FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK, SIZEPOLICY_FIXED,
                    new Dimension(-1, 75),
                    new Dimension(-1, 75),
                    new Dimension(-1, 75)
            );

            parameterListContainerPanel.add(parameterInputPanel.getComponent(), constraints);
            whenPanelList.add(parameterInputPanel);
        }

        List<ThenParameter> thenParameters = declaredMock.getThenParameter();
        returnItemList.setLayout(new GridLayout(thenParameters.size(), 1));
        for (int i = 0; i < thenParameters.size(); i++) {
            ThenParameter thenParameter = thenParameters.get(i);
            ThenParameterInputPanel thenParameterInputPanel = new ThenParameterInputPanel(thenParameter);
            GridConstraints constraints = new GridConstraints(
                    i, 0, 1, 1, ANCHOR_NORTH,
                    FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK, SIZEPOLICY_FIXED,
                    new Dimension(-1, 75),
                    new Dimension(-1, 75),
                    new Dimension(-1, 75)
            );

            returnItemList.add(thenParameterInputPanel.getComponent(), constraints);
            thenPanelList.add(thenParameterInputPanel);
        }


    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void setPopupHandle(JBPopup editorPopup) {
        this.editorPopup = editorPopup;
    }
}
