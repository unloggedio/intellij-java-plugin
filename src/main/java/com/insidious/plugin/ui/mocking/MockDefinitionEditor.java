package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MockDefinitionEditor {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionEditor.class);
    private final MethodUnderTest methodUnderTest;
    private final DeclaredMock declaredMock;
    private JPanel mainPanel;
    private JPanel namePanel;
    private JTextField textField1;
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
    private JButton cancelButton;
    private JButton saveButton;

    public MockDefinitionEditor(MethodUnderTest methodUnderTest, String fieldName) {
        this.methodUnderTest = methodUnderTest;
        List<String> methodSignatureItems = ClassTypeUtils.splitMethodDesc(methodUnderTest.getSignature());
        String returnTypeName = methodSignatureItems.remove(methodSignatureItems.size() - 1);
        ReturnValue returnValue = new ReturnValue("", returnTypeName, ReturnValueType.REAL);


        List<ParameterMatcher> parameterList = new ArrayList<>();
        for (String methodSignatureItem : methodSignatureItems) {
            ParameterMatcher parameterMatcher = new ParameterMatcher(
                    "any", methodSignatureItem
            );
            parameterList.add(parameterMatcher);
        }


        this.declaredMock = new DeclaredMock(
                "new mock for method " + methodUnderTest.getName(),
                methodUnderTest.getClassName(), fieldName, methodUnderTest.getName(),
                parameterList, returnValue, MethodExitType.NORMAL
        );
    }

    public MockDefinitionEditor(MethodUnderTest methodUnderTest, DeclaredMock declaredMock) {
        this.methodUnderTest = methodUnderTest;
        this.declaredMock = declaredMock;
    }

    private void updateUiValues() {

    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
