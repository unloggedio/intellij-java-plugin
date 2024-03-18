package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.mocking.MethodExitType;
import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.mocking.ThenParameter;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionEditor {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionEditor.class);
    private final MethodUnderTest methodUnderTest;
    private final DeclaredMock declaredMock;
    private final List<WhenParameterInputPanel> whenPanelList = new ArrayList<>();
    private final List<ThenParameterInputPanel> thenPanelList = new ArrayList<>();
    private final Project project;
    private final OnSaveListener onSaveListener;
    //    private final CancelOrOkListener<DeclaredMock> cancelOrOkListener;
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel parameterListContainerPanel;
    private JButton chainAnotherReturnButton;
    private JPanel returnItemList;
    private JButton saveButton;
    private JLabel callExpressionLabel;
    private JPanel nameAndSettingPanel;
    private JPanel nameContainerPanel;
    private JPanel whenThenParentPanel;
    private JPanel whenPanel;
    private JPanel whenTitlePanel;
    private JPanel thenPanel;
    private JPanel thenTitlePanel;
    private JLabel changeThenType;
    private JPanel thenReturnLabelContainer;
    private JButton cancelButton;
    private JLabel thenTextLabel;
    private JLabel returnValueLabel;
    private JPanel bottomControlPanel;
    private String returnDummyValue;
    private String methodReturnTypeName;
    private JBPopup yeditorPopup;
    private ComponentLifecycleListener<Void> componentLifecycleListener;

    public MockDefinitionEditor(
            MethodUnderTest methodUnderTest,
            PsiMethodCallExpression methodCallExpression,
            Project project, OnSaveListener onSaveListener, ComponentLifecycleListener<Void> componentLifecycleListener) {
        this.onSaveListener = onSaveListener;
        this.componentLifecycleListener = componentLifecycleListener;
        this.methodUnderTest = methodUnderTest;
        this.project = project;
        cancelButton.addActionListener(e -> componentLifecycleListener.onClose(null));
//        mockTypeParentPanel.setVisible(false);

        String expressionText = ApplicationManager.getApplication().runReadAction(
                (Computable<String>) () -> methodCallExpression.getMethodExpression().getText());
        callExpressionLabel.setText(expressionText);

        changeThenType.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnValueLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter returnTypeSelectorAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                SelectorPanel<MethodExitType> selectorPanel = new SelectorPanel<>(
                        Arrays.asList(MethodExitType.NORMAL, MethodExitType.NULL, MethodExitType.EXCEPTION),
                        o -> {
                            logger.warn("selected response: " + o);

                            switch (o) {
                                case NORMAL:
                                    returnValueLabel.setText("Return");
                                    thenPanelList.get(0).setExitType(MethodExitType.NORMAL);
                                    break;
                                case EXCEPTION:
                                    returnValueLabel.setText("Throw");
                                    thenPanelList.get(0).setExitType(MethodExitType.EXCEPTION);
                                    break;
                                case NULL:
                                    returnValueLabel.setText("Return NULL");
                                    thenPanelList.get(0).setExitType(MethodExitType.NULL);
                                    break;
                            }

                        }, null);

                ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(selectorPanel.getContent(), null);


                JBPopup popup = gutterMethodComponentPopup
                        .setProject(project)
                        .setShowBorder(true)
                        .setShowShadow(true)
                        .setFocusable(true)
                        .setRequestFocus(true)
                        .setCancelOnClickOutside(true)
                        .setCancelOnOtherWindowOpen(true)
                        .setCancelKeyEnabled(true)
                        .setBelongsToGlobalPopupStack(false)
                        .createPopup();
                popup.showUnderneathOf(returnValueLabel);
                selectorPanel.setPopup(popup);


            }
        };
        changeThenType.addMouseListener(returnTypeSelectorAdapter);
        returnValueLabel.addMouseListener(returnTypeSelectorAdapter);

        this.declaredMock = ApplicationManager.getApplication().runReadAction(
                (Computable<DeclaredMock>) () -> ClassUtils.createDefaultMock(methodCallExpression));
        updateUiValues();
        addListeners();
    }

    public MockDefinitionEditor(
            MethodUnderTest methodUnderTest,
            DeclaredMock declaredMock,
            Project project,
            OnSaveListener onSaveListener
    ) {
        this.onSaveListener = onSaveListener;
        this.methodUnderTest = methodUnderTest;
        this.project = project;
        this.declaredMock = declaredMock;
        callExpressionLabel.setText(declaredMock.getSourceClassName() + "." + declaredMock.getMethodName() + "()");

        updateUiValues();
        addListeners();
    }

    private void addListeners() {
        chainAnotherReturnButton.setVisible(false);
        chainAnotherReturnButton.addActionListener(e -> {
            ThenParameter newThenParameter = ClassUtils.createDummyThenParameter(returnDummyValue,
                    methodReturnTypeName);
            declaredMock.getThenParameter().add(newThenParameter);
            returnItemList.setLayout(new GridLayout(declaredMock.getThenParameter().size(), 1));
            ThenParameterInputPanel thenParameterInputPanel = new ThenParameterInputPanel(newThenParameter, project);

            Dimension thenParameterInputPanelDimension = new Dimension(-1, 200);

            GridConstraints constraints = new GridConstraints(
                    declaredMock.getThenParameter().size() - 1, 0, 1, 1, ANCHOR_NORTH,
                    FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK, SIZEPOLICY_FIXED,
                    thenParameterInputPanelDimension,
                    thenParameterInputPanelDimension,
                    thenParameterInputPanelDimension
            );

            returnItemList.add(thenParameterInputPanel.getComponent(), constraints);
            thenPanelList.add(thenParameterInputPanel);
            returnItemList.revalidate();
            returnItemList.repaint();
        });

        saveButton.addActionListener(e -> {
            onSaveListener.onSaveDeclaredMock(declaredMock);
            componentLifecycleListener.onClose(null);
        });
    }

    private void updateUiValues() {
        parameterListContainerPanel.removeAll();
        whenPanelList.clear();
        thenPanelList.clear();

        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitleColor(JBColor.BLACK);


        nameTextField.setText(declaredMock.getName());
        nameTextField.setSelectionStart(0);

        nameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                declaredMock.setName(nameTextField.getText());
            }
        });

        List<ParameterMatcher> whenParameter = declaredMock.getWhenParameter();
        parameterListContainerPanel.setLayout(new GridLayout(whenParameter.size(), 1));
        for (int i = 0; i < whenParameter.size(); i++) {
            ParameterMatcher parameterMatcher = whenParameter.get(i);
            WhenParameterInputPanel parameterInputPanel = new WhenParameterInputPanel(parameterMatcher, project);
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
            ThenParameterInputPanel thenParameterInputPanel = new ThenParameterInputPanel(thenParameter, project);
            Dimension thenParameterInputPanelDimension = new Dimension(-1, 200);
            GridConstraints constraints = new GridConstraints(
                    i, 0, 1, 1, ANCHOR_NORTH,
                    FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK, SIZEPOLICY_FIXED,
                    thenParameterInputPanelDimension,
                    thenParameterInputPanelDimension,
                    thenParameterInputPanelDimension
            );

            returnItemList.add(thenParameterInputPanel.getComponent(), constraints);
            thenPanelList.add(thenParameterInputPanel);
        }


    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
