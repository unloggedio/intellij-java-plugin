package com.insidious.plugin.ui.Components;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.JavaParserUtils;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestAssertion;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.AssertionType;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jvm.JvmModifier;
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
import com.intellij.psi.impl.source.tree.java.PsiExpressionListImpl;
import com.intellij.psi.impl.source.tree.java.PsiJavaTokenImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.impl.source.tree.java.PsiThisExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.FileContentUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.objectweb.asm.Opcodes;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class TestCaseDesigner {
    private static final Logger logger = LoggerUtil.getInstance(TestCaseDesigner.class);
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
    private JCheckBox addFieldMocksCheckBox;
    private JTextArea instructionsArea;
    private JCheckBox useMockitoAnnotationsMockCheckBox;
    private JPanel testFrameWorkPanel;
    private JComboBox<TestFramework> testFrameworkComboBox;
    private JComboBox<MockFramework> mockFrameworkComboBox;
    private JLabel mockFrameworkLabel;
    private JLabel testFrameworkLabel;
    private JPanel mockFrameworkPanel;
    private JPanel useMocktoConfigPanel;
    private JPanel addFieldMocksConfigPanel;
    private JTable assertionTable;
    //    private JButton addNewAssertionButton;
    private PsiMethod currentMethod;
    private PsiClass currentClass;
    private String basePath;
    private Editor editor;
    private MethodCallExpression mainMethod;
    private List<String> methodChecked;
    private Map<String, Parameter> fieldMapByName;

    public TestCaseDesigner() {
        saveTestCaseButton.setEnabled(false);
        saveTestCaseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        testFrameworkComboBox.setModel(new DefaultComboBoxModel<>(TestFramework.values()));
        mockFrameworkComboBox.setModel(new DefaultComboBoxModel<>(MockFramework.values()));

        addFieldMocksCheckBox.addActionListener(e -> updatePreviewTestCase());
        testFrameworkComboBox.addActionListener(e -> updatePreviewTestCase());
        mockFrameworkComboBox.addActionListener(e -> updatePreviewTestCase());
        useMockitoAnnotationsMockCheckBox.addActionListener((e) -> updatePreviewTestCase());


        saveTestCaseButton.addActionListener(e -> {
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
                saveMethodToExistingFile(testcaseFile);
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

        });
    }

    private static int buildMethodAccessModifier(PsiModifierList modifierList) {
        int methodAccess = 0;
        if (modifierList != null) {
            for (PsiElement child : modifierList.getChildren()) {
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
        }
        return methodAccess;
    }

    private void saveMethodToExistingFile(File testcaseFile) {
        try {
            JavaParser javaParser = new JavaParser(new ParserConfiguration());
            ParseResult<CompilationUnit> parsedFile = javaParser.parse(
                    testcaseFile);
            if (!parsedFile.getResult()
                    .isPresent() || !parsedFile.isSuccessful()) {
                InsidiousNotification.notifyMessage("<html>Failed to parse existing test case in the file, unable" +
                        " to" +
                        " add new test case. <br/>" + parsedFile.getProblems() + "</html>", NotificationType.ERROR);
                return;
            }
            CompilationUnit existingCompilationUnit = parsedFile.getResult()
                    .get();

            ParseResult<CompilationUnit> parseResult = javaParser.parse(
                    new ByteArrayInputStream(editor.getDocument().getText().getBytes()));
            if (!parseResult.isSuccessful()) {
                logger.error("Failed to parse test case to be written: \n" +
                        "\nProblems");
                List<Problem> problems = parseResult.getProblems();
                for (int i = 0; i < problems.size(); i++) {
                    Problem problem = problems.get(i);
                    logger.error("Problem [" + i + "] => " + problem);
                }

                InsidiousNotification.notifyMessage("Failed to parse test case to write " +
                        parseResult.getProblems(), NotificationType.ERROR
                );
                return;
            }
            CompilationUnit newCompilationUnit = parseResult
                    .getResult()
                    .get();

            MethodDeclaration newMethodDeclaration =
                    newCompilationUnit.getClassByName("Test" + currentClass.getName() + "V")
                            .get()
                            .getMethodsByName("testMethod" + ClassTypeUtils.upperInstanceName(currentMethod.getName()))
                            .get(0);

            JavaParserUtils.mergeCompilationUnits(existingCompilationUnit, newCompilationUnit);

            try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                out.write(existingCompilationUnit.toString()
                        .getBytes(StandardCharsets.UTF_8));
            } catch (Exception e1) {
                InsidiousNotification.notifyMessage(
                        "Failed to write test case for class " + currentClass.getName() + " -> "
                                + e1.getMessage(), NotificationType.ERROR
                );
            }
        } catch (Exception ex) {
            System.out.println("Exception " + ex);
            ex.printStackTrace();
        }
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
        if (basePath == null) {
            basePath = currentMethod.getProject().getBasePath();
        }


        saveLocationTextField.setText("");

        this.currentMethod = method;
        this.currentClass = psiClass;


        selectedMethodNameLabel.setText(psiClass.getName() + "." + method.getName() + "()");
        updatePreviewTestCase();
        saveTestCaseButton.setEnabled(true);
        bottomControlPanel.setEnabled(true);

    }

    public void updatePreviewTestCase() {

        List<TestCandidateMetadata> testCandidateMetadataList = createTestCandidate();

        String testMethodName = "testMethod" + ClassTypeUtils.upperInstanceName(currentMethod.getName());
        TestCaseGenerationConfiguration testCaseGenerationConfiguration = new TestCaseGenerationConfiguration(
                (TestFramework) testFrameworkComboBox.getSelectedItem(),
                (MockFramework) mockFrameworkComboBox.getSelectedItem(),
                JsonFramework.GSON,
                ResourceEmbedMode.IN_CODE
        );

        if (useMockitoAnnotationsMockCheckBox.isSelected()) {
            testCaseGenerationConfiguration.setUseMockitoAnnotations(true);
        }

        for (TestCandidateMetadata testCandidateMetadata : testCandidateMetadataList) {
            // mock all calls by default
            testCaseGenerationConfiguration.getCallExpressionList().addAll(testCandidateMetadata.getCallsList());
        }


        testCaseGenerationConfiguration.setTestMethodName(testMethodName);


        testCaseGenerationConfiguration.getTestCandidateMetadataList().clear();
        testCaseGenerationConfiguration.getTestCandidateMetadataList().addAll(testCandidateMetadataList);

        InsidiousService insidiousService = currentMethod.getProject().getService(InsidiousService.class);

        EditorFactory editorFactory = EditorFactory.getInstance();
        int scrollIndex = 0;
        int offset = 0;
        try {
            if (mainMethod.isMethodPublic() && !currentMethod.isConstructor()) {


                String testCaseScriptCode = currentMethod.getProject().getService(InsidiousService.class)
                        .getTestCandidateCode(testCaseGenerationConfiguration);
                if (testCaseScriptCode == null) {
                    UsageInsightTracker.getInstance().RecordEvent("SESSION_NOT_FOUND", new JSONObject());
                    InsidiousNotification.notifyMessage("Session not found, please try again",
                            NotificationType.WARNING);
                    return;
                }


                if (saveLocationTextField.getText().isEmpty()) {

                    String packageName = ((PsiJavaFileImpl) currentClass.getContainingFile()).getPackageName();
                    String testOutputDirPath = insidiousService.getTestDirectory(packageName, basePath);

                    saveLocationTextField.setText(testOutputDirPath + "/Test" + currentClass.getName() + "V.java");
                }


                String[] codeLines = testCaseScriptCode.split("\n");
                int classStartIndex = 0;
                offset = testCaseScriptCode.indexOf(testCaseGenerationConfiguration.getTestMethodName());
                for (String codeLine : codeLines) {
                    if (codeLine.contains(testCaseGenerationConfiguration.getTestMethodName())) {
                        break;
                    }
                    classStartIndex++;
                }
                scrollIndex = Math.min(classStartIndex + 10, codeLines.length);

                Document document = editorFactory.createDocument(testCaseScriptCode);
                editor = editorFactory.createEditor(document, currentMethod.getProject(), JavaFileType.INSTANCE, false);
            } else {
                editor = editorFactory.createEditor(
                        editorFactory.createDocument("Test case can be generated only for public methods."),
                        currentMethod.getProject(), JavaFileType.INSTANCE, true);

            }
        } catch (Exception e) {
            e.printStackTrace();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream stringWriter = new PrintStream(out);
            e.printStackTrace(stringWriter);
            Document document = editorFactory.createDocument(out.toString());
            editor = editorFactory.createEditor(document, currentMethod.getProject(), PlainTextFileType.INSTANCE, true);
            currentClass = null;
            currentMethod = null;

        }


        testCasePreviewPanel.removeAll();
        testCasePreviewPanel.add(editor.getComponent(), BorderLayout.CENTER);
        editor.getCaretModel().getCurrentCaret().moveToOffset(offset);
        logger.warn("scroll to line: " + scrollIndex);
        editor.getScrollingModel().scrollVertically(scrollIndex);
        testCasePreviewPanel.revalidate();
        testCasePreviewPanel.repaint();

    }

    private List<TestCandidateMetadata> createTestCandidate() {

        List<TestCandidateMetadata> testCandidateMetadataList = new ArrayList<>();

        fieldMapByName = new HashMap<>();
        VariableContainer fieldContainer = new VariableContainer();

        PsiField[] fields = currentClass.getFields();
        for (PsiField field : fields) {
            if (field.hasModifier(JvmModifier.STATIC)) {
                continue;
            }
            Parameter fieldParameter = new Parameter();
            fieldParameter.setName(field.getName());
            if (!(field.getType() instanceof PsiClassReferenceType)
                    || field.getType().getCanonicalText().equals("java.lang.String")
                    || field.getType().getCanonicalText().startsWith("org.apache.commons.logging")
                    || field.getType().getCanonicalText().startsWith("org.slf4j")
            ) {
                continue;
            }
            setParameterTypeFromPsiType(fieldParameter, field.getType());
            fieldParameter.setValue(random.nextLong());
            fieldParameter.setProb(new DataEventWithSessionId());
            fieldContainer.add(fieldParameter);
            fieldMapByName.put(field.getName(), fieldParameter);
        }

        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();

        Parameter testSubjectParameter = new Parameter();
        testSubjectParameter.setType(currentClass.getQualifiedName());
        testSubjectParameter.setValue(random.nextLong());
        DataEventWithSessionId testSubjectParameterProbe = new DataEventWithSessionId();
        testSubjectParameter.setProb(testSubjectParameterProbe);

        // constructor

        List<TestCandidateMetadata> constructorCandidate = buildConstructorCandidate(currentClass, testSubjectParameter,
                fieldContainer);
        testCandidateMetadataList.addAll(constructorCandidate);

        // test subject


        // method return value
        Parameter returnValue = null;
        if (currentMethod.getReturnType() != null) {
            returnValue = new Parameter();
            returnValue.setValue(random.nextLong());

            PsiType returnType = currentMethod.getReturnType();
            setParameterTypeFromPsiType(returnValue, returnType);

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
            setParameterTypeFromPsiType(argumentParameter, parameterPsiType);

            DataEventWithSessionId parameterProbe = new DataEventWithSessionId();
            argumentParameter.setProb(parameterProbe);
            argumentParameter.setName(parameter.getName());

            if (argumentParameter.getType().equals("java.lang.String")) {
                argumentParameter.getProb().setSerializedValue(("\"" + parameter.getName() + "\"").getBytes());
            } else if (argumentParameter.isPrimitiveType()) {
                argumentParameter.getProb().setSerializedValue(("0").getBytes());
            } else {
                argumentParameter.getProb().setSerializedValue(("{" + "}").getBytes());
            }

            arguments.add(argumentParameter);
        }


        PsiModifierList modifierList = currentMethod.getModifierList();
        int methodAccess = buildMethodAccessModifier(modifierList);
        mainMethod = new MethodCallExpression(
                currentMethod.getName(), testSubjectParameter, arguments, returnValue, 0
        );
        mainMethod.setSubject(testSubjectParameter);
        mainMethod.setMethodAccess(methodAccess);

        testCandidateMetadata.setMainMethod(mainMethod);

        testCandidateMetadata.setTestSubject(testSubjectParameter);


        if (currentMethod.getReturnType() != null && !currentMethod.getReturnType().getCanonicalText().equals("void")) {
            Parameter assertionExpectedValue = new Parameter();
            assertionExpectedValue.setName(returnValue.getName() + "Expected");
            assertionExpectedValue.setProb(new DataEventWithSessionId());
            assertionExpectedValue.setProbeInfo(new DataInfo());


            setParameterTypeFromPsiType(assertionExpectedValue, currentMethod.getReturnType());

            TestAssertion testAssertion = new TestAssertion(AssertionType.EQUAL, assertionExpectedValue, returnValue);

            testCandidateMetadata.getAssertionList().add(testAssertion);
        }


        // fields

        if (addFieldMocksCheckBox.isSelected()) {
            // field parameters are going to be mocked and then injected
            fieldContainer.all().forEach(e -> testCandidateMetadata.getFields().add(e));
        }

        methodChecked = new ArrayList<>();
        List<MethodCallExpression> collectedMceList = extractMethodCalls(currentMethod);
        for (int i = 0; i < collectedMceList.size(); i++) {
            MethodCallExpression methodCallExpression = collectedMceList.get(i);
            DataEventWithSessionId entryProbe = new DataEventWithSessionId();
            entryProbe.setNanoTime(i);
            methodCallExpression.setEntryProbe(entryProbe);
        }

        testCandidateMetadata.getCallsList().addAll(collectedMceList);


        testCandidateMetadataList.add(testCandidateMetadata);

        return testCandidateMetadataList;
    }

    private List<MethodCallExpression> extractMethodCalls(PsiMethod method) {
        List<MethodCallExpression> collectedMceList = new ArrayList<>();

        List<PsiMethodCallExpression> mceList = collectMethodCallExpressions(method.getBody());
        for (PsiMethodCallExpression psiMethodCallExpression : mceList) {
            if (psiMethodCallExpression == null) {
                continue;
            }
            PsiElement[] callExpressionChildren = psiMethodCallExpression.getChildren();
            logger.warn("call expression child: " + psiMethodCallExpression);
            if (callExpressionChildren[0] instanceof PsiReferenceExpressionImpl) {
                PsiReferenceExpressionImpl callReferenceExpression = (PsiReferenceExpressionImpl) callExpressionChildren[0];
                PsiExpressionListImpl callParameterExpression = (PsiExpressionListImpl) callExpressionChildren[1];

                PsiElement[] referenceChildren = callReferenceExpression.getChildren();
                if (referenceChildren.length == 4) {

                    // <fieldName><dot><methodTemplateParams(implicit, mostly empty)><methodName>

                    PsiElement subjectReferenceChild = referenceChildren[0];
                    PsiElement dotChild = referenceChildren[1];
                    PsiElement paramChild = referenceChildren[2];
                    PsiElement methodNameNode = referenceChildren[3];
                    if (!(dotChild instanceof PsiJavaTokenImpl) || !dotChild.getText().equals(".")) {
                        // second child was supposed to be a dot
                        continue;
                    }

                    if (subjectReferenceChild instanceof PsiThisExpressionImpl) {
                        String invokedMethodName = methodNameNode.getText();
                        if (methodChecked.contains(invokedMethodName)) {
                            continue;
                        }
                        methodChecked.add(invokedMethodName);
                        List<MethodCallExpression> callExpressions = getCallsFromMethod(callParameterExpression,
                                invokedMethodName);
                        collectedMceList.addAll(callExpressions);
                    } else {

                        String callSubjectName = subjectReferenceChild.getText();
                        Parameter fieldByName = fieldMapByName.get(callSubjectName);
                        if (fieldByName == null) {
                            // no such field
                            continue;
                        }

                        List<Parameter> methodArguments = new ArrayList<>();
                        Parameter methodReturnValue = new Parameter();

                        PsiClass calledMethodClassReference = getClassByName(fieldByName.getType());
                        if (calledMethodClassReference == null) {
                            logger.error("called class reference for method not found ["
                                    + psiMethodCallExpression + "]");
                            continue;
                        }


                        String methodName = methodNameNode.getText();
                        PsiMethod matchedMethod = getMatchingMethod(calledMethodClassReference, methodName,
                                callParameterExpression);
                        if (matchedMethod == null) {
                            logger.warn("could not resolve reference to method: [" +
                                    methodName + "] in class: " + fieldByName.getType());
                            continue;
                        }

                        PsiExpression[] actualParameterExpressions = callParameterExpression.getExpressions();
                        PsiParameter @NotNull [] parameters = matchedMethod.getParameterList().getParameters();
                        for (int i = 0; i < parameters.length; i++) {
                            PsiParameter parameter = parameters[i];
                            PsiExpression parameterExpression = actualParameterExpressions[i];

                            Parameter callParameter = new Parameter();
                            callParameter.setValue(random.nextLong());
                            PsiType typeToAssignFrom = parameterExpression.getType();
                            if (typeToAssignFrom.getCanonicalText().equals("null")) {
                                typeToAssignFrom = parameter.getType();
                            }
                            setParameterTypeFromPsiType(callParameter, typeToAssignFrom);
                            DataEventWithSessionId prob = new DataEventWithSessionId();
                            if (callParameter.isPrimitiveType()) {
                                prob.setSerializedValue("0".getBytes());
                            } else if (callParameter.isStringType()) {
                                prob.setSerializedValue("\"\"".getBytes());
                            }
                            callParameter.setProb(prob);
                            callParameter.setName(parameter.getName());
                            callParameter.setProbeInfo(new DataInfo());
                            methodArguments.add(callParameter);
                        }

                        @Nullable PsiType methodReturnPsiReference = matchedMethod.getReturnType();

                        methodReturnValue.setValue(random.nextLong());
                        setParameterTypeFromPsiType(methodReturnValue, methodReturnPsiReference);
                        methodReturnValue.setProbeInfo(new DataInfo());
                        methodReturnValue.setProb(new DataEventWithSessionId());


                        MethodCallExpression mce = new MethodCallExpression(
                                methodName, fieldByName, methodArguments, methodReturnValue, 0
                        );
                        int methodAccess = buildMethodAccessModifier(matchedMethod.getModifierList());
                        if (calledMethodClassReference.isInterface()) {
                            methodAccess = methodAccess | Opcodes.ACC_PUBLIC;
                        }
                        mce.setMethodAccess(methodAccess);

                        collectedMceList.add(mce);
                    }


                } else if (referenceChildren.length == 2) {
                    // call to a method in same class
                    PsiElement methodNameNode = referenceChildren[1];
                    String invokedMethodName = methodNameNode.getText();
                    if (methodChecked.contains(invokedMethodName)) {
                        continue;
                    }
                    methodChecked.add(invokedMethodName);
                    List<MethodCallExpression> callExpressions = getCallsFromMethod(callParameterExpression,
                            invokedMethodName);
                    collectedMceList.addAll(callExpressions);
                }


            } else {
                logger.error("unknown type of child: " + callExpressionChildren[0]);
            }
        }
        return collectedMceList;
    }

    private List<MethodCallExpression> getCallsFromMethod(PsiExpressionListImpl callParameterExpression, String invokedMethodName) {
        PsiMethod matchedMethod = getMatchingMethod(currentClass, invokedMethodName, callParameterExpression);
        if (matchedMethod == null) {
            return Collections.emptyList();
        }

        return extractMethodCalls(matchedMethod);
    }

    private PsiMethod getMatchingMethod(
            PsiClass classReference,
            String methodName,
            PsiExpressionListImpl callParameterExpression
    ) {

        logger.warn("Find matching method for [" + methodName + "] - " + classReference.getName());

        Set<PsiClass> classesToCheck = new HashSet<>();
        classesToCheck.add(classReference);
        Set<PsiClass> interfaces = getInterfaces(classReference);
        classesToCheck.addAll(interfaces);

        for (PsiClass psiClass : classesToCheck) {
            List<PsiMethod> matchedMethods = Arrays.stream(psiClass.getMethods())
                    .filter(e -> e.getName().equals(methodName))
                    .filter(e -> e.getParameterList().getParametersCount()
                            == callParameterExpression.getExpressionCount())
                    .collect(Collectors.toList());
            if (matchedMethods.size() == 0) {
                continue;
            }
            for (PsiMethod matchedMethod : matchedMethods) {
                boolean isMismatch = false;
                logger.warn(
                        "Check matched method: [" + matchedMethod + "] in class [" + psiClass.getQualifiedName() + "]");
                PsiType[] expectedExpressionType = callParameterExpression.getExpressionTypes();
                PsiParameter @NotNull [] parameters = matchedMethod.getParameterList().getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    PsiParameter parameter = parameters[i];
                    PsiType type = parameter.getType();
                    if (type instanceof PsiClassReferenceType && type.getCanonicalText().length() == 1) {
                        // this is a generic type, wont match with the actual expression type
                        continue;
                    }
                    if (!type.isAssignableFrom(expectedExpressionType[i])) {
                        logger.warn("parameter [" + i + "] mismatch [" + type + " vs " + expectedExpressionType[i]);
                        isMismatch = true;
                        break;
                    }
                }
                if (!isMismatch) {
                    return matchedMethod;
                }
            }
        }


        return null;
    }

    private Set<PsiClass> getInterfaces(PsiClass psiClass) {
        Set<PsiClass> interfacesList = new HashSet<>();
        PsiClass[] interfaces = psiClass.getInterfaces();
        for (PsiClass anInterface : interfaces) {
            interfacesList.add(anInterface);
            interfacesList.addAll(getInterfaces(anInterface));
        }
        PsiClass[] supers = psiClass.getSupers();
        for (PsiClass aSuper : supers) {
            interfacesList.add(aSuper);
            interfacesList.addAll(getInterfaces(aSuper));
        }


        return interfacesList;
    }

    private List<TestCandidateMetadata> buildConstructorCandidate(
            PsiClass currentClass, Parameter testSubject, VariableContainer fieldContainer) {
        List<TestCandidateMetadata> candidateList = new ArrayList<>();

        PsiMethod[] constructors = currentClass.getConstructors();
        if (constructors.length == 0) {
            TestCandidateMetadata newTestCaseMetadata = new TestCandidateMetadata();
            MethodCallExpression constructorMethod = new MethodCallExpression("<init>", testSubject,
                    Collections.emptyList(), testSubject, 0);
            constructorMethod.setMethodAccess(1);
            newTestCaseMetadata.setMainMethod(constructorMethod);
            newTestCaseMetadata.setTestSubject(testSubject);
            newTestCaseMetadata.setFields(VariableContainer.from(Collections.emptyList()));
            candidateList.add(newTestCaseMetadata);

            return candidateList;
        }
        PsiMethod selectedConstructor = null;
        for (PsiMethod constructor : constructors) {
            if (selectedConstructor == null) {
                selectedConstructor = constructor;
                continue;
            }
            if (constructor.getParameterList().getParametersCount() > selectedConstructor.getParameterList()
                    .getParametersCount()) {
                boolean isRecursiveConstructor = false;
                for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
                    if (currentClass.getQualifiedName().equals(parameter.getType().getCanonicalText())) {
                        isRecursiveConstructor = true;
                    }
                }
                if (!isRecursiveConstructor) {
                    selectedConstructor = constructor;
                }
            }
        }

        if (selectedConstructor == null) {
            logger.error("selectedConstructor should not have been null: " + currentClass.getQualifiedName());
            return candidateList;
        }

        logger.warn("selected constructor for [" + currentClass.getQualifiedName()
                + "] -> " + selectedConstructor.getName());


        TestCandidateMetadata candidate = new TestCandidateMetadata();
        List<Parameter> methodArguments = new ArrayList<>(selectedConstructor.getParameterList().getParametersCount());

        for (PsiParameter parameter : selectedConstructor.getParameterList().getParameters()) {

            List<Parameter> fieldParameterByType = fieldContainer.getParametersByType(
                    parameter.getType().getCanonicalText());

            if (fieldParameterByType.size() > 0) {

                Parameter closestNameMatch = fieldParameterByType.get(0);
                int currentDistance = Integer.MAX_VALUE;

                for (Parameter fieldParameter : fieldParameterByType) {
                    int distance = StringUtils.getLevenshteinDistance(fieldParameter.getName(), parameter.getName());
                    if (distance < currentDistance) {
                        closestNameMatch = fieldParameter;
                        currentDistance = distance;
                    }
                }
                methodArguments.add(closestNameMatch);

            } else {
                Parameter methodArgumentParameter = new Parameter();
                methodArgumentParameter.setName(parameter.getName());
                setParameterTypeFromPsiType(methodArgumentParameter, parameter.getType());
                methodArgumentParameter.setValue(random.nextLong());
//                methodArgumentParameter.setProbeInfo(new DataInfo());
                DataEventWithSessionId argumentProbe = new DataEventWithSessionId();

                if (methodArgumentParameter.isPrimitiveType()) {
                    argumentProbe.setSerializedValue("0".getBytes());
                } else if (methodArgumentParameter.isStringType()) {
                    argumentProbe.setSerializedValue("\"\"".getBytes());
                } else {

                    String parameterClassName = parameter.getType().getCanonicalText();
                    if (parameter.getType() instanceof PsiClassReferenceType) {
                        parameterClassName = ((PsiClassReferenceType) parameter.getType()).rawType().getCanonicalText();
                    }
                    @Nullable PsiClass parameterClassReference = getClassByName(parameterClassName);

                    if (parameterClassReference == null) {
                        logger.error("did not find class reference: " + parameterClassName +
                                " for parameter: " + parameter.getName() +
                                " in class " + currentClass.getQualifiedName());
                        continue;
                    }

                    List<TestCandidateMetadata> constructorMetadata =
                            buildConstructorCandidate(parameterClassReference, methodArgumentParameter, fieldContainer);
                    candidateList.addAll(constructorMetadata);
                }

                methodArgumentParameter.setProb(argumentProbe);

                methodArguments.add(methodArgumentParameter);
            }


        }


        MethodCallExpression constructorMethod = new MethodCallExpression("<init>", testSubject, methodArguments,
                testSubject, 0);
        constructorMethod.setMethodAccess(Opcodes.ACC_PUBLIC);

        candidate.setMainMethod(constructorMethod);
        candidate.setTestSubject(testSubject);
        candidateList.add(candidate);
        return candidateList;
    }

    @Nullable
    private PsiClass getClassByName(String className) {
        return JavaPsiFacade.getInstance(currentClass.getProject())
                .findClass(ClassTypeUtils.getJavaClassName(className),
                        GlobalSearchScope.allScope(currentClass.getProject()));
    }

    private void setParameterTypeFromPsiType(Parameter parameter, PsiType psiType) {
        if (psiType instanceof PsiClassReferenceType) {
            PsiClassReferenceType returnClassType = (PsiClassReferenceType) psiType;
            if (returnClassType.getCanonicalText().equals(returnClassType.getName())) {
                logger.warn("return class type canonical text[" + returnClassType.getCanonicalText()
                        + "] is same as its name [" + returnClassType.getName() + "]");
                // this is a generic template type <T>, and not a real class
                parameter.setType("java.lang.Object");
                return;
            }
            parameter.setType(psiTypeToJvmType(returnClassType.rawType().getCanonicalText()));
            if (returnClassType.hasParameters()) {
                SessionInstance.extractTemplateMap(returnClassType, parameter.getTemplateMap());
                parameter.setContainer(true);
            }
        } else {
            parameter.setType(psiTypeToJvmType(psiType.getCanonicalText()));
        }
    }

    public List<PsiMethodCallExpression> collectMethodCallExpressions(PsiElement element) {
        ArrayList<PsiMethodCallExpression> returnList = new ArrayList<>();
        if (element == null) {
            return returnList;
        }

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
            case "java.util.Map":
                canonicalText = "java.util.HashMap";
                break;
            case "java.util.List":
                canonicalText = "java.util.ArrayList";
                break;
            default:
        }
        return canonicalText;
    }
}
