package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaParameterAdapter;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private final OnSaveListener onSaveListener;
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
            Project project, OnSaveListener onSaveListener) {
        this.onSaveListener = onSaveListener;
        this.methodUnderTest = methodUnderTest;
        this.project = project;
        mockTypeParentPanel.setVisible(false);

        PsiMethod destinationMethod = methodCallExpression.resolveMethod();

        PsiType returnType = identifyReturnType(methodCallExpression);
        if (returnType != null) {
            returnDummyValue = ClassUtils.createDummyValue(returnType, new ArrayList<>(),
                    destinationMethod.getProject());
            methodReturnTypeName = buildJvmClassName(returnType);
        } else {
            methodReturnTypeName = "java.lang.Object";
            returnDummyValue = "{}";
        }

        PsiClass parentClass = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        if (parentClass == null) {
            InsidiousNotification.notifyMessage("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]", NotificationType.ERROR);
            throw new RuntimeException("Failed to identify parent class for the call [" +
                    methodCallExpression.getText() + "]");
        }
        String expressionText = methodCallExpression.getMethodExpression().getText();
        callExpressionLabel.setText(expressionText);

        PsiType[] methodParameterTypes = methodCallExpression.getArgumentList().getExpressionTypes();
        JvmParameter[] jvmParameters = destinationMethod.getParameters();
        List<ParameterMatcher> parameterList = new ArrayList<>();

        PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        PsiClass containingClass = destinationMethod.getContainingClass();
        PsiSubstitutor classSubstitutor = null;
        if (containingClass != null && parentOfType != null) {
            classSubstitutor = TypeConversionUtil
                    .getClassSubstitutor(containingClass, parentOfType, PsiSubstitutor.EMPTY);
        }

        for (int i = 0; i < methodParameterTypes.length; i++) {
            JavaParameterAdapter param = new JavaParameterAdapter(jvmParameters[i]);
            PsiType parameterType = methodParameterTypes[i];

            if (classSubstitutor != null) {
                parameterType = classSubstitutor.substitute(parameterType);
            }

            String parameterTypeName = parameterType.getCanonicalText();
            if (parameterType instanceof PsiClassReferenceType) {
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterType;
                parameterTypeName = classReferenceType.rawType().getCanonicalText();
            }
            ParameterMatcher parameterMatcher = new ParameterMatcher(param.getName(),
                    ParameterMatcherType.ANY_OF_TYPE, parameterTypeName);
            parameterList.add(parameterMatcher);
        }


        ArrayList<ThenParameter> thenParameterList = new ArrayList<>();
        thenParameterList.add(createDummyThenParameter());
        PsiElement callerQualifier = methodCallExpression.getMethodExpression().getQualifier();
        String fieldName = callerQualifier.getText();
        PsiElement[] callerQualifierChildren = callerQualifier.getChildren();
        if (callerQualifierChildren.length > 1) {
            fieldName = callerQualifierChildren[callerQualifierChildren.length - 1].getText();
        }
        this.declaredMock = new DeclaredMock(
                "mock response " + expressionText,
                methodUnderTest.getClassName(), parentClass.getQualifiedName(),
                fieldName,
                methodUnderTest.getName(), parameterList, thenParameterList
        );
        updateUiValues();
        addListeners(methodUnderTest);
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
        addListeners(methodUnderTest);
    }

    private String buildJvmClassName(PsiType returnType) {
        if (returnType == null) {
            return "java.lang.Object";
        }

        if (!(returnType instanceof PsiClassReferenceType)) {
            return returnType.getCanonicalText();
        }
        PsiClassReferenceType classReferenceType = (PsiClassReferenceType) returnType;
        if (classReferenceType.resolve() == null) {
            return "java.lang.Object";
        }

        String jvmClassName1 = JvmClassUtil.getJvmClassName(classReferenceType.resolve());
        if (jvmClassName1 == null) {
            jvmClassName1 = "java.lang.Object";
        }
        StringBuilder jvmClassName =
                new StringBuilder(jvmClassName1);

        int paramCount = classReferenceType.getParameterCount();
        if (paramCount > 0) {
            jvmClassName.append("<");

            PsiType[] parameterArray = classReferenceType.getParameters();
            for (int i = 0; i <= paramCount - 1; i++) {
                jvmClassName.append(buildJvmClassName(parameterArray[i]));
                if (i != paramCount - 1) {
                    jvmClassName.append(",");
                }
            }
            jvmClassName.append(">");
        }

        return jvmClassName.toString();
    }

    @Nullable
    private PsiType identifyReturnType(PsiExpression methodCallExpression) {
        PsiType returnType = null;

        if (methodCallExpression.getParent() instanceof PsiConditionalExpressionImpl) {
            return identifyReturnType((PsiConditionalExpressionImpl) methodCallExpression.getParent());
        } else if (methodCallExpression.getParent() instanceof PsiLocalVariableImpl
                && methodCallExpression.getParent().getParent() instanceof PsiDeclarationStatementImpl) {
            // this is an assignment and we can probably get a better return type from the variable type which
            // this is being assigned to
            returnType = ((PsiLocalVariableImpl) methodCallExpression.getParent()).getType();
        } else if (methodCallExpression.getParent() instanceof PsiAssignmentExpressionImpl
                && methodCallExpression.getParent().getParent() instanceof PsiExpressionStatement) {
            // this is an assignment and we can probably get a better return type from the variable type which
            // this is being assigned to
            returnType = ((PsiAssignmentExpressionImpl) methodCallExpression.getParent()).getType();
        } else if (methodCallExpression.getParent() instanceof PsiExpressionListImpl
                && methodCallExpression.getParent().getParent() instanceof PsiMethodCallExpressionImpl) {
            // the return value is being passed to another method as a parameter
            PsiExpressionListImpl expressionList = (PsiExpressionListImpl) methodCallExpression.getParent();
            PsiType[] expressionTypes = expressionList.getExpressionTypes();
            PsiExpression[] allExpressions = expressionList.getExpressions();
            // identify the return value is which index
            int i = 0;
            for (PsiExpression expression : allExpressions) {
                if (expression == methodCallExpression) {
                    break;
                }
                i++;
            }

            if (i < expressionTypes.length) {
                returnType = expressionTypes[i];
            }

            PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
            PsiMethodCallExpression parentCall = PsiTreeUtil.getParentOfType(expressionList,
                    PsiMethodCallExpression.class);
            if (parentCall != null && parentCall.resolveMethod() != null) {
                PsiMethod psiMethod = parentCall.resolveMethod();
                PsiClass containingClass = psiMethod.getContainingClass();
                if (containingClass != null && parentOfType != null) {
                    PsiSubstitutor classSubstitutor = TypeConversionUtil
                            .getClassSubstitutor(containingClass,
                                    parentOfType, PsiSubstitutor.EMPTY);
                    returnType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
                }
            }
        } else if (methodCallExpression.getParent() instanceof PsiReturnStatementImpl) {
            // value is being returned, so we can use the return type of the method which contains this call
            PsiMethod parentMethod = PsiTreeUtil.getParentOfType(methodCallExpression, PsiMethod.class);
            if (parentMethod != null && parentMethod.getReturnType() != null) {
                returnType = parentMethod.getReturnType();
                PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
                PsiClass containingClass = parentMethod.getContainingClass();
                if (containingClass != null && parentOfType != null) {
                    PsiSubstitutor classSubstitutor = TypeConversionUtil
                            .getClassSubstitutor(containingClass, parentOfType, PsiSubstitutor.EMPTY);
                    returnType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
                }
            }
        } else if (methodCallExpression instanceof PsiMethodCallExpression) {
            PsiMethod psiMethod = ((PsiMethodCallExpression) methodCallExpression).resolveMethod();

            PsiField fieldImpl = (PsiField) ((PsiReferenceExpression)
                    ((PsiMethodCallExpression) methodCallExpression)
                            .getMethodExpression().getQualifierExpression()).resolve();

            PsiClass fieldClass = ((PsiClassReferenceType) fieldImpl.getType()).resolve();

            if (psiMethod != null) {

                returnType = psiMethod.getReturnType();
                PsiClass containingClass = psiMethod.getContainingClass();

                PsiSubstitutor classSubstitutor = TypeConversionUtil.getClassSubstitutor(containingClass,
                        fieldClass, PsiSubstitutor.EMPTY);
                returnType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
            }
        }

        return returnType;
    }

    private void addListeners(MethodUnderTest methodUnderTest) {
        chainAnotherReturnButton.setVisible(false);
        chainAnotherReturnButton.addActionListener(e -> {
            ThenParameter newThenParameter = createDummyThenParameter();
            declaredMock.getThenParameter().add(newThenParameter);
            returnItemList.setLayout(new GridLayout(declaredMock.getThenParameter().size(), 1));
            ThenParameterInputPanel thenParameterInputPanel = new ThenParameterInputPanel(newThenParameter, project);
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
            onSaveListener.onSaveDeclaredMock(declaredMock, methodUnderTest);
            editorPopup.cancel();
        });
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
