package com.insidious.plugin.ui.Components;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestAssertion;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.AssertionType;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.util.FileContentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.objectweb.asm.Opcodes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.List;

public class TestCaseDesigner {
    private static final Logger logger = LoggerUtil.getInstance(TestCaseDesigner.class);
    private final List<AssertionEditorForm> assertionForms = new ArrayList<>();
    Random random = new Random(new Date().getTime());
    private JPanel mainContainer;
    private JPanel selectedClassDetailsPanel;
    private JLabel selectedMethodNameLabel;
    private JLabel returnValueTypeLabel;
    private JPanel testCasePreviewPanel;
    private JTextField saveLocationTextField;
    private JButton saveTestCaseButton;
    private JPanel bottomControlPanel;
    private JPanel configurationControlPanel;
    private JTable assertionTable;
    //    private JButton addNewAssertionButton;
    private PsiMethod currentMethod;
    private PsiClass currentClass;
    private TestCaseGenerationConfiguration testCaseGenerationConfiguration;
    private String basePath;
    private Editor editor;

    public TestCaseDesigner() {
    }

    public JComponent getContent() {
        return mainContainer;
    }

    public void renderTestDesignerInterface(PsiClass psiClass, PsiMethod method) {

        if (this.currentMethod != null && this.currentMethod.equals(method)) {
            return;
        }
        InsidiousService insidiousService = method.getProject().getService(InsidiousService.class);

        if (method.getContainingFile().getVirtualFile().getPath().contains("/test/")) {
            return;
        }
        basePath = insidiousService.getBasePathForVirtualFile(method.getContainingFile().getVirtualFile());


        saveLocationTextField.setText("");
        String testMethodName = "testMethod" + ClassTypeUtils.upperInstanceName(method.getName());

        testCaseGenerationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUNIT5, MockFramework.MOCKITO, JsonFramework.GSON, ResourceEmbedMode.IN_CODE
        );

        testCaseGenerationConfiguration.setTestMethodName(testMethodName);

        this.currentMethod = method;
        this.currentClass = psiClass;


        List<TestCandidateMetadata> testCandidateMetadataList = createTestCandidate();

        testCaseGenerationConfiguration.getTestCandidateMetadataList().addAll(testCandidateMetadataList);

        selectedMethodNameLabel.setText(psiClass.getName() + "." + method.getName() + "()");


        saveTestCaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String saveLocation = saveLocationTextField.getText();
                InsidiousService insidiousService = currentMethod.getProject().getService(InsidiousService.class);
                try {
                    insidiousService.ensureTestUtilClass(basePath);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }


                File testcaseFile = new File(saveLocation);
                testcaseFile.getParentFile().mkdirs();

                logger.info("[TEST CASE SAVE] testcaseFile : " + testcaseFile.getAbsolutePath());
                UsageInsightTracker.getInstance().RecordEvent("TestCaseSaved", new JSONObject());

                if (!testcaseFile.exists()) {
                    try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                        out.write(editor.getDocument().getText().getBytes());
                    } catch (Exception e1) {
                        InsidiousNotification.notifyMessage(
                                "Failed to write test case: " + e1.getMessage(), NotificationType.ERROR
                        );
                    }
                } else {
                    InsidiousNotification.notifyMessage("File already exists: " + testcaseFile.getAbsolutePath(),
                            NotificationType.ERROR);
                    return;
                }

                @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(FileSystems.getDefault()
                                .getPath(testcaseFile.getAbsolutePath())
                                .toUri()
                                .toString());
                if (newFile == null) {
                    return;
                }
                newFile.refresh(true, false);


                List<VirtualFile> newFile1 = new ArrayList<>();
                newFile1.add(newFile);
                FileContentUtil.reparseFiles(currentClass.getProject(), newFile1, true);
                @Nullable Document newDocument = FileDocumentManager.getInstance()
                        .getDocument(newFile);

                FileEditorManager.getInstance(currentClass.getProject())
                        .openFile(newFile, true, true);
                InsidiousNotification.notifyMessage("Created test case: " + testcaseFile.getName(),
                        NotificationType.WARNING);

            }
        });
        updatePreviewTestCase();

    }

    public void addChangeListeners() {
//        testMethodNameField.addFocusListener(new FocusListener() {
//            @Override
//            public void focusGained(FocusEvent e) {
//
//            }
//
//            @Override
//            public void focusLost(FocusEvent e) {
//                testCaseGenerationConfiguration.setTestMethodName(testMethodNameField.getText());
//                updatePreviewTestCase();
//            }
//        });
    }

    public void updatePreviewTestCase() {

        InsidiousService insidiousService = currentMethod.getProject().getService(InsidiousService.class);

        EditorFactory editorFactory = EditorFactory.getInstance();
        int scrollIndex = 0;
        int offset = 0;
        try {
            String testCaseUnit = currentMethod.getProject()
                    .getService(InsidiousService.class)
                    .getTestCandidateCode(testCaseGenerationConfiguration);
            if (testCaseUnit == null) {
                InsidiousNotification.notifyMessage("Session not found, please try again", NotificationType.WARNING);
                return;
            }


            if (saveLocationTextField.getText().isEmpty()) {

                String packageName = ((PsiJavaFileImpl) currentClass.getContainingFile()).getPackageName();
                String testOutputDirPath = insidiousService.getTestDirectory(packageName, basePath);

                saveLocationTextField.setText(testOutputDirPath + "/Test" + currentClass.getName() + "V.java");
            }


            String testCaseScript = testCaseUnit;

            String[] codeLines = testCaseScript.split("\n");
            int classStartIndex = 0;
            offset = testCaseScript.indexOf(testCaseGenerationConfiguration.getTestMethodName());
            for (String codeLine : codeLines) {
                if (codeLine.contains(testCaseGenerationConfiguration.getTestMethodName())) {
                    break;
                }
                classStartIndex++;
            }
            scrollIndex = Math.min(classStartIndex + 10, codeLines.length);

            Document document = editorFactory.createDocument(testCaseScript);
            editor = editorFactory.createEditor(document, currentMethod.getProject(), JavaFileType.INSTANCE, false);
        } catch (Exception e) {
            e.printStackTrace();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream stringWriter = new PrintStream(out);
            e.printStackTrace(stringWriter);
            Document document = editorFactory.createDocument(out.toString());
            editor = editorFactory.createEditor(document, currentMethod.getProject(), PlainTextFileType.INSTANCE,
                    false);
        }


        testCasePreviewPanel.removeAll();
        testCasePreviewPanel.add(editor.getComponent(), BorderLayout.CENTER);
        editor.getCaretModel().getCurrentCaret().moveToOffset(offset);
        logger.warn("scroll to line: " + scrollIndex);
        editor.getScrollingModel().scrollVertically(scrollIndex);

    }


    private List<TestCandidateMetadata> createTestCandidate() {

        List<TestCandidateMetadata> testCandidateMetadataList = new ArrayList<>();


        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();

        // test subject
        Parameter testSubjectParameter = new Parameter();
        testSubjectParameter.setType(currentClass.getQualifiedName());
        testSubjectParameter.setValue(random.nextLong());
        DataEventWithSessionId testSubjectParameterProbe = new DataEventWithSessionId();
        testSubjectParameter.setProb(testSubjectParameterProbe);

        // method return value
        Parameter returnValue = null;
        if (currentMethod.getReturnType() != null) {
            returnValue = new Parameter();
            returnValue.setValue(random.nextLong());

            PsiType returnType = currentMethod.getReturnType();
            if (returnType instanceof PsiClassReferenceType) {
                PsiClassReferenceType returnClassType = (PsiClassReferenceType) returnType;
                returnValue.setType(psiTypeToJvmType(returnClassType.rawType().getCanonicalText()));
                if (returnClassType.hasParameters()) {
                    SessionInstance.extractTemplateMap(returnClassType, returnValue.getTemplateMap());
                    returnValue.setContainer(true);
                }
            } else {
                returnValue.setType(psiTypeToJvmType(returnType.getCanonicalText()));
            }
            DataEventWithSessionId returnValueProbe = new DataEventWithSessionId();
            returnValue.setProb(returnValueProbe);
        }

        // method parameters
        PsiParameterList parameterList = currentMethod.getParameterList();
        List<Parameter> arguments = new ArrayList<>(parameterList.getParametersCount());
        for (PsiParameter parameter : parameterList.getParameters()) {
            Parameter argumentParameter = new Parameter();

            argumentParameter.setValue(random.nextLong());

            PsiType parameterPsiType = parameter.getType();
            if (parameterPsiType instanceof PsiClassReferenceType) {
                argumentParameter.setType(psiTypeToJvmType(((PsiClassReferenceType) parameterPsiType).rawType().getCanonicalText()));
                PsiClassReferenceType classReferenceType = (PsiClassReferenceType) parameterPsiType;
                if (classReferenceType.hasParameters()) {
                    SessionInstance.extractTemplateMap(classReferenceType, argumentParameter.getTemplateMap());
                    argumentParameter.setContainer(true);
                }
            } else {
                argumentParameter.setType(psiTypeToJvmType(parameterPsiType.getCanonicalText()));
            }
            DataEventWithSessionId parameterProbe = new DataEventWithSessionId();
            argumentParameter.setProb(parameterProbe);
            argumentParameter.setName(parameter.getName());

            if (argumentParameter.getType().equals("java.lang.String")) {
                argumentParameter.getProb().setSerializedValue(("\"" + parameter.getName() + "\"").getBytes());
            } else if (argumentParameter.isPrimitiveType()) {

            } else {
                argumentParameter.getProb().setSerializedValue(("{" + "}").getBytes());
            }

            arguments.add(argumentParameter);
        }


        MethodCallExpression mainMethod = new MethodCallExpression(
                currentMethod.getName(), testSubjectParameter, arguments, returnValue, 0
        );
        mainMethod.setSubject(testSubjectParameter);

        int methodAccess = 0;
        for (PsiElement child : currentMethod.getModifierList().getChildren()) {
            switch (child.getText()) {
                case "private":
                    methodAccess = methodAccess | Opcodes.ACC_PRIVATE;
                    break;
                case "public":
                    methodAccess = methodAccess | Opcodes.ACC_PUBLIC;
                    break;
                case "protected":
                    methodAccess = methodAccess | Opcodes.ACC_PROTECTED;
                    break;
                case "static":
                    methodAccess = methodAccess | Opcodes.ACC_STATIC;
                    break;
                case "final":
                    methodAccess = methodAccess | Opcodes.ACC_FINAL;
                    break;
                default:
                    logger.warn("unhandled modifier: " + child);
            }
        }


        mainMethod.setMethodAccess(methodAccess);

        testCandidateMetadata.setMainMethod(mainMethod);

        testCandidateMetadata.setTestSubject(testSubjectParameter);


        if (currentMethod.getReturnType() != null) {
            Parameter assertionExpectedValue = new Parameter();
            assertionExpectedValue.setName(returnValue.getName() + "Expected");
            assertionExpectedValue.setProb(new DataEventWithSessionId());
            assertionExpectedValue.setProbeInfo(new DataInfo());


            PsiType returnType = currentMethod.getReturnType();
            if (returnType instanceof PsiClassReferenceType) {
                PsiClassReferenceType returnClassType = (PsiClassReferenceType) returnType;
                assertionExpectedValue.setType(psiTypeToJvmType(returnClassType.rawType().getCanonicalText()));
                if (returnClassType.hasParameters()) {
                    SessionInstance.extractTemplateMap(returnClassType, assertionExpectedValue.getTemplateMap());
                    assertionExpectedValue.setContainer(true);
                }
            } else {
                assertionExpectedValue.setType(psiTypeToJvmType(returnType.getCanonicalText()));
            }

            TestAssertion testAssertion = new TestAssertion(
                    AssertionType.EQUAL, assertionExpectedValue, returnValue);

            testCandidateMetadata.getAssertionList().add(testAssertion);
        }


        // fields

        Map<String, PsiField> fieldMap = new HashMap<>();
        PsiField[] fields = currentClass.getFields();
        for (PsiField field : fields) {
            fieldMap.put(field.getName(), field);
        }


        List<PsiMethodCallExpression> mceList = collectMethodCallExpressions(currentMethod.getBody());
        for (PsiMethodCallExpression psiMethodCallExpression : mceList) {
            PsiElement[] callExpressionChildren = psiMethodCallExpression.getChildren();
            for (PsiElement callExpressionChild : callExpressionChildren) {

            }
        }


        testCandidateMetadataList.add(testCandidateMetadata);

        return testCandidateMetadataList;
    }

    public List<PsiMethodCallExpression> collectMethodCallExpressions(PsiElement element) {
        ArrayList<PsiMethodCallExpression> returnList = new ArrayList<>();

        if (element instanceof PsiMethodCallExpression) {
            returnList.add((PsiMethodCallExpression) element);
        }

        PsiElement[] children = element.getChildren();

        for (PsiElement child : children) {
            returnList.addAll(collectMethodCallExpressions(child));
        }


        return returnList;
    }

    private String psiTypeToJvmType(String canonicalText) {
        if (canonicalText.endsWith("[]")) {
            canonicalText = psiTypeToJvmType(canonicalText.substring(0, canonicalText.length() - 2));
            return "[" + canonicalText;
        }
        switch (canonicalText) {
            case "void":
                canonicalText = "V";
                break;
            case "boolean":
                canonicalText = "Z";
                break;
            case "byte":
                canonicalText = "B";
                break;
            case "char":
                canonicalText = "C";
                break;
            case "short":
                canonicalText = "S";
                break;
            case "int":
                canonicalText = "I";
                break;
            case "long":
                canonicalText = "J";
                break;
            case "float":
                canonicalText = "F";
                break;
            case "double":
                canonicalText = "D";
                break;
            default:
        }
        return canonicalText;
    }
}
