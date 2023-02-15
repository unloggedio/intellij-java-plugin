package com.insidious.plugin.ui.Components;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.psi.*;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class TestCaseCreator {
    Random random = new Random(new Date().getTime());
    private JPanel mainContainer;
    private JPanel namePanel;
    private JTextField testMethodNameField;
    private JPanel selectedClassDetailsPanel;
    private JLabel selectedMethodNameLabel;
    private JPanel argumentsPanel;
    private JPanel returnValuePanel;
    private JPanel callMockPanel;
    private JPanel actionPanel;
    private JButton previewTestCaseButton;
    private JLabel returnValueTypeLabel;
    private JTextField returnValueTextField;
    private JPanel testCasePreviewPanel;
    private PsiMethod currentMethod;
    private PsiClass currentClass;

    public TestCaseCreator() {
    }

    public JComponent getContent() {
        return mainContainer;
    }

    public void renderTestCreator(PsiClass psiClass, PsiMethod method) {

        if (this.currentMethod != null && this.currentMethod.equals(method)) {
            return;
        }


        this.currentMethod = method;
        this.currentClass = psiClass;

        selectedMethodNameLabel.setText(psiClass.getQualifiedName() + "." + method.getName() + "()");
        testMethodNameField.setText("testMethod" + ClassTypeUtils.upperInstanceName(method.getName()));

        PsiParameterList methodParameterList = method.getParameterList();

        argumentsPanel.removeAll();
        argumentsPanel.setAlignmentX(0);

        JPanel argumentContainerPanel = new JPanel();
//        argumentContainerPanel.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (methodParameterList.getParametersCount() > 0) {
            argumentContainerPanel.setLayout(new GridLayout(methodParameterList.getParametersCount(), 0));
            argumentContainerPanel.setAlignmentX(0);
            JBScrollPane parameterScrollPane = new JBScrollPane(argumentContainerPanel);

            for (PsiParameter parameter : methodParameterList.getParameters()) {

                ParameterEditorForm parameterEditor = new ParameterEditorForm(parameter);
                JPanel contentPanel = parameterEditor.getContent();
                contentPanel.setSize(-1, 100);
                contentPanel.setMaximumSize(new Dimension(-1, 100));
                contentPanel.setPreferredSize(new Dimension(-1, 100));
                contentPanel.setAlignmentX(0);
                parameterScrollPane.add(contentPanel);
            }
            argumentsPanel.add(parameterScrollPane, BorderLayout.CENTER);
        } else {
            argumentContainerPanel.add(new JLabel("Method " + method.getName() + " has no arguments"));
            argumentsPanel.add(argumentContainerPanel);
        }


        @Nullable PsiType returnValueType = method.getReturnType();
        returnValueTypeLabel.setText(returnValueType.getPresentableText());

        updatePreviewTestCase();

    }

    public void updatePreviewTestCase() {

        TestCandidateMetadata testCandidate = createTestCandidate();


        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUNIT5, MockFramework.MOCKITO, JsonFramework.GSON, ResourceEmbedMode.IN_FILE
        );
        generationConfiguration.getTestCandidateMetadataList().add(testCandidate);

        Editor editor;
        EditorFactory editorFactory = EditorFactory.getInstance();
        try {
            String testCaseUnit = currentMethod.getProject().getService(InsidiousService.class)
                    .getTestCandidateCode(generationConfiguration);
            Document document = editorFactory.createDocument(testCaseUnit);
            editor = editorFactory.createEditor(document, currentMethod.getProject(), JavaFileType.INSTANCE, true);
        } catch (Exception e) {
            e.printStackTrace();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream stringWriter = new PrintStream(out);
            e.printStackTrace(stringWriter);
            Document document = editorFactory.createDocument(out.toString());
            editor = editorFactory.createEditor(document, currentMethod.getProject(), PlainTextFileType.INSTANCE, true);
        }


        testCasePreviewPanel.removeAll();
        testCasePreviewPanel.add(editor.getComponent(), BorderLayout.CENTER);
    }


    private TestCandidateMetadata createTestCandidate() {
        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();

        Parameter testSubjectParameter = new Parameter();
        testSubjectParameter.setType(currentClass.getQualifiedName());
        testSubjectParameter.setValue(random.nextLong());
        testSubjectParameter.setProb(new DataEventWithSessionId());

        Parameter returnValue = new Parameter();
        returnValue.setValue(random.nextLong());
        returnValue.setType(psiTypeToJvmType(currentMethod.getReturnType().getCanonicalText()));
        returnValue.setProb(new DataEventWithSessionId());

        PsiParameterList parameterList = currentMethod.getParameterList();
        List<Parameter> arguments = new ArrayList<>(parameterList.getParametersCount());
        for (PsiParameter parameter : parameterList.getParameters()) {
            Parameter argumentParameter = new Parameter();
            argumentParameter.setValue(random.nextLong());
            argumentParameter.setType(psiTypeToJvmType(parameter.getType().getCanonicalText()));
            argumentParameter.setProb(new DataEventWithSessionId());
            arguments.add(argumentParameter);
        }


        MethodCallExpression mainMethod = new MethodCallExpression(
                currentMethod.getName(), testSubjectParameter, arguments, returnValue, 0
        );
        mainMethod.setSubject(testSubjectParameter);
        testCandidateMetadata.setMainMethod(mainMethod);

        testCandidateMetadata.setTestSubject(testSubjectParameter);

        return testCandidateMetadata;
    }

    private String psiTypeToJvmType(String canonicalText) {
        switch (canonicalText) {
            case "void":
                return "V";
            case "boolean":
                return "Z";
            case "byte":
                return "B";
            case "char":
                return "C";
            case "short":
                return "S";
            case "int":
                return "I";
            case "long":
                return "J";
            case "float":
                return "F";
            case "double":
                return "D";
            default:
                return canonicalText;
        }
    }
}
