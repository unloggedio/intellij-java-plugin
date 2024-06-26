package com.insidious.plugin.ui.testdesigner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.FieldAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.TestAssertion;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.JavaParserUtils;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.ValueResourceContainer;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.FileContentUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.objectweb.asm.Opcodes;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TestCaseDesignerLite {
    private static final Logger logger = LoggerUtil.getInstance(TestCaseDesignerLite.class);
    private static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final LightVirtualFile testCaseScriptFile;
    private final MethodAdapter methodAdapter;
    private final Project project;
    private final InsidiousService insidiousService;
    Random random = new Random(new Date().getTime());
    private JPanel mainPanel;
    private JPanel configurationPanel;
    private JPanel bottomControlPanel;
    private JTextField saveLocationTextField;
    private JCheckBox addFieldMocksCheckBox;
    private JCheckBox useMockitoAnnotationsMockCheckBox;
    private JComboBox<TestFramework> testFrameworkComboBox;
    private JComboBox<MockFramework> mockFrameworkComboBox;
    private JComboBox<JsonFramework> jsonFrameworkComboBox;
    private JComboBox<ResourceEmbedMode> resourceEmberModeComboBox;
    private JPanel testFrameWorkPanel;
    private JLabel testFrameworkLabel;
    private JPanel mockFrameworkPanel;
    private JLabel mockFrameworkLabel;
    private JPanel jsonFrameworkChoicePanel;
    private JPanel resourceEmbedModeChoicePanel;
    private JPanel useMockitoConfigPanel;
    private JPanel mockDownstreamContainerPanel;
    private JPanel saveDetailsPanel;
    private JPanel controlPanel;
    private TestCaseGenerationConfiguration currentTestGenerationConfiguration;
    private TestCaseUnit testCaseScript;
    private List<String> methodChecked;
    private Map<String, Parameter> fieldMapByName;
    private Editor editorReference;
    private FileEditor fileEditorReference;

    public TestCaseDesignerLite(MethodAdapter currentMethod,
                                TestCaseGenerationConfiguration configuration,
                                boolean generateBoilerPlate,
                                InsidiousService insidiousService) {

        this.methodAdapter = currentMethod;
        this.mainPanel.setMaximumSize(new Dimension(-1, 300));
        this.currentTestGenerationConfiguration = configuration;
        this.insidiousService = insidiousService;
        this.project = insidiousService.getProject();
        testCaseScriptFile =
                new LightVirtualFile("Test" + currentMethod.getContainingClass().getName() + "V.java");


//        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        closeButton.setIcon(UIUtils.CLOSE_FILE_RED_SVG);
//        closeButton.setForeground(UIUtils.defaultForeground);


        testFrameworkComboBox.setModel(new DefaultComboBoxModel<>(TestFramework.values()));
        mockFrameworkComboBox.setModel(new DefaultComboBoxModel<>(MockFramework.values()));

        jsonFrameworkComboBox.setModel(new DefaultComboBoxModel<>(JsonFramework.values()));
        jsonFrameworkComboBox.setSelectedItem(JsonFramework.Jackson);

        resourceEmberModeComboBox.setModel(new DefaultComboBoxModel<>(ResourceEmbedMode.values()));
        resourceEmberModeComboBox.setSelectedItem(ResourceEmbedMode.IN_CODE);

        if (generateBoilerPlate || currentTestGenerationConfiguration == null) {
            generateTestCaseBoilerPlace();
        }

        addFieldMocksCheckBox.addActionListener(e -> {
            generateTestCaseBoilerPlace();
            updatePreviewTestCase();
        });

        testFrameworkComboBox.addActionListener(e -> {
            currentTestGenerationConfiguration.setTestFramework(
                    (TestFramework) testFrameworkComboBox.getSelectedItem());
            updatePreviewTestCase();
        });
        mockFrameworkComboBox.addActionListener(e -> {
            currentTestGenerationConfiguration.setMockFramework(
                    (MockFramework) mockFrameworkComboBox.getSelectedItem());
            updatePreviewTestCase();
        });
        jsonFrameworkComboBox.addActionListener(e -> {
            currentTestGenerationConfiguration.setJsonFramework(
                    (JsonFramework) jsonFrameworkComboBox.getSelectedItem());
            updatePreviewTestCase();
        });

        useMockitoAnnotationsMockCheckBox.addActionListener((e) -> {
            currentTestGenerationConfiguration.setUseMockitoAnnotations(useMockitoAnnotationsMockCheckBox.isSelected());
            updatePreviewTestCase();
        });

        resourceEmberModeComboBox.addActionListener((e) -> {
            currentTestGenerationConfiguration.setResourceEmbedMode(
                    (ResourceEmbedMode) resourceEmberModeComboBox.getSelectedItem());
            updatePreviewTestCase();
        });


        List<AnAction> action11 = new ArrayList<>();

        action11.add(new AnAction(() -> "Preview", AllIcons.Actions.Preview) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                FileEditorManager fileEditorManager = insidiousService.previewTestCase(
                        getLightVirtualFile());
                Editor selectedTextEditor = fileEditorManager.getSelectedTextEditor();
                FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
                setEditorReferences(selectedTextEditor, selectedEditor);

            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        });

        action11.add(new AnAction(() -> "Save", AllIcons.Actions.MenuSaveall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveFinal(currentMethod, insidiousService);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        });

        ActionToolbarImpl actionToolbar = new ActionToolbarImpl(
                "JUnit Test Generator", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);
        controlPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            generateAndPreviewTestCase(currentTestGenerationConfiguration);
        });
    }

    private void saveFinal(MethodAdapter currentMethod, InsidiousService insidiousService) {
        UsageInsightTracker.getInstance().RecordEvent(
                "SAVE_JUNIT_TEST_CASE",
                null
        );

        ClassAdapter currentClass = methodAdapter.getContainingClass();
        String saveLocation = saveLocationTextField.getText();
        String basePath = insidiousService.guessModuleBasePath(currentMethod.getContainingClass());
        try {
            insidiousService.getJUnitTestCaseWriter().ensureTestUtilClass(basePath);
        } catch (IOException ex) {
            JSONObject properties = new JSONObject();
            properties.put("message", ex.getMessage());
            UsageInsightTracker.getInstance().RecordEvent(
                    "FAIL_SAVE_TEST_CASE",
                    properties
            );
            throw new RuntimeException(ex);
        }

        File testcaseFile = new File(saveLocation);
        testcaseFile.getParentFile().mkdirs();

        logger.info("[TEST CASE SAVE] testcaseFile : " + testcaseFile.getAbsolutePath());
        UsageInsightTracker.getInstance().RecordEvent("TestCaseSaved", new JSONObject());

        TestCaseGenerationConfiguration generationConfig = currentTestGenerationConfiguration;
        if (generationConfig.getResourceEmbedMode() == ResourceEmbedMode.IN_FILE) {
            String resourceDirectory = insidiousService.getJUnitTestCaseWriter()
                    .getTestResourcesDirectory(basePath) + "unlogged-fixtures" + File.separator;

            ValueResourceContainer valueResourceContainer = testCaseScript.getTestGenerationState()
                    .getValueResourceMap();
            String resourceFileName = valueResourceContainer.getResourceFileName();
            new File(resourceDirectory).mkdirs();
            File resourceFile = new File(resourceDirectory + resourceFileName);

            try (FileOutputStream resourceFileOutput = new FileOutputStream(resourceFile)) {
                resourceFileOutput.write(
                        objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsBytes(valueResourceContainer)
                );
            } catch (Exception e1) {

                JSONObject properties = new JSONObject();
                properties.put("message", e1.getMessage());
                UsageInsightTracker.getInstance().RecordEvent(
                        "FAIL_SAVE_TEST_CASE_RESOURCE",
                        properties
                );

                InsidiousNotification.notifyMessage(
                        "Failed to write test resource case: " + e1.getMessage(), NotificationType.ERROR
                );
                return;
            }
        }
        String fileContents = testCaseScriptFile.getContent().toString();
        if (!testcaseFile.exists()) {
            try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                out.write(fileContents.getBytes());
            } catch (Exception e1) {
                InsidiousNotification.notifyMessage(
                        "Failed to write test case: " + e1.getMessage(), NotificationType.ERROR
                );
            }
        } else {
            saveMethodToExistingFile(testcaseFile);
        }

        VirtualFile newFile = VirtualFileManager.getInstance()
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

        FileEditorManager.getInstance(currentClass.getProject())
                .openFile(newFile, true, true);
        InsidiousNotification.notifyMessage("Created test case: " + testcaseFile.getName(),
                NotificationType.WARNING);
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
                        logger.warn("unhandled modifier: " + child.getText());
                }
            }
        }
        return methodAccess;
    }


    public JComponent getComponent() {
        return mainPanel;
    }


    private void saveMethodToExistingFile(File testcaseFile) {
        try {
            JavaParser javaParser = new JavaParser(new ParserConfiguration());
            ParseResult<CompilationUnit> parsedFile = javaParser.parse(testcaseFile);
            if (!parsedFile.getResult().isPresent() || !parsedFile.isSuccessful()) {
                InsidiousNotification.notifyMessage("<html>Failed to parse existing test case in the file, unable" +
                        " to" +
                        " add new test case. <br/>" + parsedFile.getProblems() + "</html>", NotificationType.ERROR);
                return;
            }
            CompilationUnit existingCompilationUnit = parsedFile.getResult().get();
            String testContents = testCaseScriptFile.getContent().toString();
            ParseResult<CompilationUnit> parseResult = javaParser.parse(
                    new ByteArrayInputStream(testContents.getBytes()));
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

            JavaParserUtils.mergeCompilationUnits(existingCompilationUnit, newCompilationUnit);

            try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                out.write(existingCompilationUnit.toString()
                        .getBytes(StandardCharsets.UTF_8));
            } catch (Exception e1) {
                InsidiousNotification.notifyMessage(
                        "Failed to write test case for class " + methodAdapter.getContainingClass().getName() + " -> "
                                + e1.getMessage(), NotificationType.ERROR
                );
            }
        } catch (Exception ex) {
            System.out.println("Exception " + ex);
            ex.printStackTrace();
        }
    }

    public void generateTestCaseBoilerPlace() {

        PsiFile containingFile = methodAdapter.getContainingFile();
        if (containingFile.getVirtualFile() == null || containingFile.getVirtualFile().getPath().contains("/test/")) {
            InsidiousNotification.notifyMessage("Failed to get method source code", NotificationType.ERROR);
            return;
        }

        saveLocationTextField.setText("");
        List<TestCandidateMetadata> testCandidateMetadataList = null;
        try {
            testCandidateMetadataList = ApplicationManager.getApplication()
                    .executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(
                            (Computable<List<TestCandidateMetadata>>) () -> {
                                try {
                                    return createTestCandidate();
                                } catch (ExecutionException | InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            })).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (testCandidateMetadataList == null) {
            InsidiousNotification.notifyMessage("Failed to create test case boilerplate", NotificationType.ERROR);
            return;

        }

        String testMethodName = "testMethod" + ClassTypeUtils.upperInstanceName(methodAdapter.getName());
        currentTestGenerationConfiguration = new TestCaseGenerationConfiguration(
                (TestFramework) testFrameworkComboBox.getSelectedItem(),
                (MockFramework) mockFrameworkComboBox.getSelectedItem(),
                (JsonFramework) jsonFrameworkComboBox.getSelectedItem(),
                ResourceEmbedMode.IN_CODE
        );

        if (useMockitoAnnotationsMockCheckBox.isSelected()) {
            currentTestGenerationConfiguration.setUseMockitoAnnotations(true);
        }

        for (TestCandidateMetadata testCandidateMetadata : testCandidateMetadataList) {
            // mock all calls by default
            currentTestGenerationConfiguration.getCallExpressionList().addAll(testCandidateMetadata.getCallsList());
        }

        currentTestGenerationConfiguration.setTestMethodName(testMethodName);
        currentTestGenerationConfiguration.getTestCandidateMetadataList().clear();
        currentTestGenerationConfiguration.getTestCandidateMetadataList().addAll(testCandidateMetadataList);
    }

    public void updatePreviewTestCase() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            generateAndPreviewTestCase(currentTestGenerationConfiguration);
        });
    }

    public void generateAndPreviewTestCase(TestCaseGenerationConfiguration testCaseGenerationConfiguration) {
        ClassAdapter currentClass = methodAdapter.getContainingClass();
        currentTestGenerationConfiguration = testCaseGenerationConfiguration;

        currentTestGenerationConfiguration.setTestFramework((TestFramework) testFrameworkComboBox.getSelectedItem());
        currentTestGenerationConfiguration.setMockFramework((MockFramework) mockFrameworkComboBox.getSelectedItem());
        currentTestGenerationConfiguration.setJsonFramework((JsonFramework) jsonFrameworkComboBox.getSelectedItem());
        currentTestGenerationConfiguration.setUseMockitoAnnotations(useMockitoAnnotationsMockCheckBox.isSelected());
        currentTestGenerationConfiguration.setResourceEmbedMode(
                (ResourceEmbedMode) resourceEmberModeComboBox.getSelectedItem());

        InsidiousService insidiousService = methodAdapter.getProject().getService(InsidiousService.class);
        EditorFactory editorFactory = EditorFactory.getInstance();
        try {
            testCaseScript = methodAdapter
                    .getProject()
                    .getService(InsidiousService.class)
                    .getTestCandidateCode(testCaseGenerationConfiguration);
            if (testCaseScript == null) {
                InsidiousNotification.notifyMessage("Failed to generate test case", NotificationType.ERROR);
                return;
            }

            String moduleBasePath = insidiousService.guessModuleBasePath(currentClass);
            PsiJavaFileImpl containingFile = currentClass.getContainingFile();
            String packageName = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) containingFile::getPackageName);
            String testOutputDirPath = insidiousService.getJUnitTestCaseWriter()
                    .getTestDirectory(packageName, moduleBasePath);

            saveLocationTextField.setText(testOutputDirPath + "/Test" + currentClass.getName() + "V.java");

            String testCaseScriptCode = testCaseScript.getCode();
            String[] codeLines = testCaseScriptCode.split("\n");
            int classStartIndex = 0;
            for (String codeLine : codeLines) {
                if (codeLine.contains(testCaseGenerationConfiguration.getTestMethodName())) {
                    break;
                }
                classStartIndex++;
            }

            Document document = editorFactory.createDocument(testCaseScriptCode);
            ApplicationManager.getApplication().invokeLater(() -> {
                Editor editor = editorFactory.createEditor(document, methodAdapter.getProject(), JavaFileType.INSTANCE,
                        false);

                testCaseScriptFile.setContent(this, editor.getDocument().getText(), true);
                bottomControlPanel.setEnabled(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream stringWriter = new PrintStream(out);
            e.printStackTrace(stringWriter);
            String exceptionText = out.toString().replace("\r", "");
            Document document = editorFactory.createDocument(exceptionText);

            ApplicationManager.getApplication().invokeLater(() -> {
                Editor editor = editorFactory.createEditor(document, methodAdapter.getProject(),
                        PlainTextFileType.INSTANCE,
                        true);

                testCaseScriptFile.setContent(this, editor.getDocument().getText(), true);
                bottomControlPanel.setEnabled(false);
            });

        }
        updateFileContents();
    }

    private void updateFileContents() {
        if (this.editorReference != null) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

            ApplicationManager.getApplication().invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    Document document = editorReference.getDocument();
                    document.setText(testCaseScript.getCode());
                    fileEditorManager.openFile(testCaseScriptFile, true);
                });
            });
        }
    }

    private List<TestCandidateMetadata> createTestCandidate() throws ExecutionException, InterruptedException {

        if (methodAdapter.getContainingClass() == null) {
            return new ArrayList<>();
        }
        ClassAdapter currentClass = methodAdapter.getContainingClass();
        PsiClass parentOfType = (PsiClass) currentClass.getSource();
        List<TestCandidateMetadata> testCandidateMetadataList = new ArrayList<>();

        fieldMapByName = new HashMap<>();
        VariableContainer fieldContainer = new VariableContainer();

        FieldAdapter[] fields = currentClass.getFields();
        for (FieldAdapter field : fields) {
            if (field.hasModifier(JvmModifier.STATIC)) {
                continue;
            }
            Parameter fieldParameter = new Parameter();
            String fieldName = field.getName();
            fieldParameter.setName(fieldName);

            PsiType fieldType = field.getType();

            String canonicalText = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> {
                        TestCaseWriter.setParameterTypeFromPsiType(fieldParameter, fieldType, false);
                        return fieldType.getCanonicalText();
                    });
            if (!(fieldType instanceof PsiClassReferenceType)
                    || canonicalText.equals("java.lang.String")
                    || canonicalText.startsWith("org.apache.commons.logging")
                    || canonicalText.startsWith("org.slf4j")
            ) {
                continue;
            }

            fieldParameter.setValue(random.nextLong());
            fieldParameter.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());
            fieldContainer.add(fieldParameter);
            fieldMapByName.put(fieldName, fieldParameter);
        }

        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();
        testCandidateMetadata.setLines(List.of());
        testCandidateMetadata.setCreatedAt(new Date().getTime());
        Parameter testSubjectParameter = new Parameter();
        testSubjectParameter.setType(currentClass.getQualifiedName());
        testSubjectParameter.setValue(random.nextLong());
        DataEventWithSessionId testSubjectParameterProbe = new DataEventWithSessionId();
        testSubjectParameter.setProbeAndProbeInfo(testSubjectParameterProbe, new DataInfo());

        List<TestCandidateMetadata> constructorCandidate = buildConstructorCandidate(currentClass, testSubjectParameter,
                fieldContainer);
        testCandidateMetadataList.addAll(constructorCandidate);

        Parameter returnValue = null;
        PsiType returnType1 = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiType>) () -> methodAdapter.getReturnType());
        if (returnType1 != null) {
            returnValue = new Parameter();
            returnValue.setValue(random.nextLong());

            PsiType returnType = returnType1;
            TestCaseWriter.setParameterTypeFromPsiType(returnValue, returnType, true);

            DataEventWithSessionId returnValueProbe = new DataEventWithSessionId();
            returnValue.setProbeAndProbeInfo(returnValueProbe, new DataInfo());
        }

        // method parameters
        ParameterAdapter[] parameterList = ApplicationManager.getApplication().runReadAction(
                (Computable<ParameterAdapter[]>) methodAdapter::getParameters);
        List<Parameter> arguments = new ArrayList<>(parameterList.length);
        for (ParameterAdapter parameter : parameterList) {
            Parameter argumentParameter = new Parameter();

            argumentParameter.setValue(random.nextLong());

            PsiType parameterPsiType = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiType>) () -> {
                        TestCaseWriter.setParameterTypeFromPsiType(argumentParameter, parameter.getType(), false);
                        return parameter.getType();
                    });


            DataEventWithSessionId parameterProbe = new DataEventWithSessionId();
            argumentParameter.setProbeAndProbeInfo(parameterProbe, new DataInfo());
            String parameterName = parameter.getName();
            argumentParameter.setName(parameterName);

            if (argumentParameter.getType().equals("java.lang.String")) {
                argumentParameter.getProb().setSerializedValue(("\"" + parameterName + "\"").getBytes());
            } else if (argumentParameter.isPrimitiveType()) {
                argumentParameter.getProb().setSerializedValue(("0").getBytes());
            } else {
                argumentParameter.getProb().setSerializedValue(("{" + "}").getBytes());
            }

            arguments.add(argumentParameter);
        }


        PsiModifierList modifierList = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiModifierList>) () -> methodAdapter.getModifierList());

        int methodAccess = buildMethodAccessModifier(modifierList);
        MethodCallExpression mainMethod = new MethodCallExpression(
                ApplicationManager.getApplication().runReadAction((Computable<String>) () -> methodAdapter.getName()),
                testSubjectParameter, arguments,
                returnValue, 0
        );
        mainMethod.setSubject(testSubjectParameter);
        mainMethod.setMethodAccess(methodAccess);

        testCandidateMetadata.setMainMethod(mainMethod);

        testCandidateMetadata.setTestSubject(testSubjectParameter);

        if (returnType1 != null && !returnType1.getCanonicalText().equals("void")) {
            Parameter assertionExpectedValue = new Parameter();
            assertionExpectedValue.setName(returnValue.getName() + "Expected");
            assertionExpectedValue.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());

            TestCaseWriter.setParameterTypeFromPsiType(assertionExpectedValue, returnType1, true);
            TestAssertion testAssertion = new TestAssertion(AssertionType.EQUAL, assertionExpectedValue, returnValue);
            testCandidateMetadata.getAssertionList().add(testAssertion);
        }

        // fields
        if (addFieldMocksCheckBox.isSelected()) {
            // field parameters are going to be mocked and then injected
            fieldContainer.all().forEach(e -> testCandidateMetadata.getFields().add(e));
        }

        methodChecked = new ArrayList<>();
        List<MethodCallExpression> collectedMceList = ApplicationManager.getApplication().runReadAction(
                (Computable<List<MethodCallExpression>>) () -> extractMethodCalls(methodAdapter));
        for (int i = 0; i < collectedMceList.size(); i++) {
            MethodCallExpression methodCallExpression = collectedMceList.get(i);
            DataEventWithSessionId entryProbe = new DataEventWithSessionId();
            entryProbe.setEventId(i);
            methodCallExpression.setEntryProbe(entryProbe);
        }

        testCandidateMetadata.getCallsList().addAll(collectedMceList);
        testCandidateMetadataList.add(testCandidateMetadata);
        return testCandidateMetadataList;
    }

    private List<MethodCallExpression> extractMethodCalls(MethodAdapter method) {
        List<MethodCallExpression> collectedMceList = new ArrayList<>();
        Map<Object, Boolean> valueUsed = new HashMap<>();

        List<PsiMethodCallExpression> mceList = collectMethodCallExpressions(method.getBody());
        for (PsiMethodCallExpression psiMethodCallExpression : mceList) {
            if (psiMethodCallExpression == null) {
                continue;
            }
            PsiElement[] callExpressionChildren = psiMethodCallExpression.getChildren();
            if (callExpressionChildren[0] instanceof PsiReferenceExpressionImpl) {
                PsiReferenceExpressionImpl callReferenceExpression = (PsiReferenceExpressionImpl) callExpressionChildren[0];
                PsiExpressionListImpl callParameterExpression = null;
                if (callExpressionChildren[1] instanceof PsiExpressionListImpl) {
                    callParameterExpression = (PsiExpressionListImpl) callExpressionChildren[1];
                } else if (callExpressionChildren[2] instanceof PsiExpressionListImpl) {
                    callParameterExpression = (PsiExpressionListImpl) callExpressionChildren[2];
                }
                if (callParameterExpression == null) {
                    logger.warn("failed to extract call from call expression: " + psiMethodCallExpression.getText());
                    continue;
                }

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

                        String fieldName = subjectReferenceChild.getText();
                        Parameter fieldByName = fieldMapByName.get(fieldName);
                        if (fieldByName == null) {
                            // no such field
                            continue;
                        }

                        List<Parameter> methodArguments = new ArrayList<>();
                        Parameter methodReturnValue = new Parameter();
                        DataInfo probeInfo1 = new DataInfo(
                                0, 0, 0, 0, 0, EventType.ARRAY_LENGTH, Descriptor.Boolean, ""
                        );

                        methodReturnValue.setProbeAndProbeInfo(new DataEventWithSessionId(), probeInfo1);

                        ClassAdapter calledMethodClassReference = getClassByName(fieldByName.getType());
                        if (calledMethodClassReference == null) {
                            logger.warn("Class reference[" + fieldByName.getType() + "] for method call expression " +
                                    "not found [" + psiMethodCallExpression.getMethodExpression() + "]");
                            continue;
                        }


                        String methodName = methodNameNode.getText();
                        MethodAdapter matchedMethod = getMatchingMethod(calledMethodClassReference, methodName,
                                callParameterExpression);
                        if (matchedMethod == null) {
                            logger.warn("could not resolve reference to method: [" +
                                    methodName + "] in class: " + fieldByName.getType());
                            continue;
                        }

                        PsiExpression[] actualParameterExpressions = callParameterExpression.getExpressions();
                        ParameterAdapter[] parameters = matchedMethod.getParameters();

                        PsiClass parentOfType = PsiTreeUtil.getParentOfType(callParameterExpression, PsiClass.class);

                        PsiField callOnField = null;
                        for (PsiField field : parentOfType.getFields()) {
                            if (field.getName().equals(fieldName)) {
                                callOnField = field;
                                break;
                            }
                        }

                        PsiSubstitutor classSubstitutor = ClassUtils.getSubstitutorForCallExpression(
                                psiMethodCallExpression);
                        PsiType fieldType = ClassTypeUtils.substituteClassRecursively(callOnField.getType(),
                                classSubstitutor);

                        TestCaseWriter.setParameterTypeFromPsiType(fieldByName, fieldType, false);

                        for (int i = 0; i < parameters.length; i++) {
                            ParameterAdapter parameter = parameters[i];
                            PsiExpression parameterExpression = actualParameterExpressions[i];

                            Parameter callParameter = new Parameter();
                            PsiType typeToAssignFrom = parameterExpression.getType();


                            if (typeToAssignFrom == null || typeToAssignFrom.getCanonicalText().equals("null")) {
                                typeToAssignFrom = parameter.getType();
                            }
                            final PsiType ungenericType = ClassTypeUtils.substituteClassRecursively(typeToAssignFrom,
                                    classSubstitutor);

                            TestCaseWriter.setParameterTypeFromPsiType(callParameter, ungenericType, false);


                            long nextValue;

                            if (!(typeToAssignFrom instanceof PsiPrimitiveType)) {
                                nextValue = random.nextLong();
                            } else {
                                PsiPrimitiveType primitiveType = ((PsiPrimitiveType) typeToAssignFrom);
                                switch (primitiveType.getName()) {
                                    case "int":
                                        nextValue = random.nextInt();
                                        break;
                                    case "long":
                                        nextValue = random.nextLong();
                                        break;
                                    case "short":
                                        nextValue = random.nextInt();
                                        break;
                                    case "byte":
                                        nextValue = random.nextInt();
                                        break;
                                    case "boolean":
                                        nextValue = random.nextBoolean() ? 1L : 0L;
                                        break;
                                    case "float":
                                        nextValue = Float.floatToIntBits(random.nextFloat());
                                        break;
                                    case "double":
                                        nextValue = Double.doubleToLongBits(random.nextDouble());
                                        break;
                                    default:
                                        nextValue = random.nextInt();
                                        break;
                                }
                            }
                            valueUsed.put(nextValue, true);

                            callParameter.setValue(nextValue);
                            DataEventWithSessionId prob = new DataEventWithSessionId();
                            if (callParameter.isPrimitiveType()) {
                                prob.setSerializedValue("0".getBytes());
                            } else if (callParameter.isStringType()) {
                                prob.setSerializedValue(("\"" + parameter.getName() + "\"").getBytes());
                            } else {
                                String serializedStringValue = ClassUtils.createDummyValue(
                                        typeToAssignFrom, new LinkedList<>(),
                                        methodAdapter.getContainingClass().getProject()
                                );
                                prob.setSerializedValue(serializedStringValue.getBytes());

                            }
                            callParameter.setName(parameter.getName());
                            DataInfo probeInfo = new DataInfo(
                                    0, 0, 0, 0, 0, EventType.ARRAY_LENGTH, Descriptor.Boolean, ""
                            );
                            callParameter.setProbeAndProbeInfo(prob, probeInfo);
                            methodArguments.add(callParameter);
                        }

                        PsiType ungenericReturnClassType = ClassTypeUtils.substituteClassRecursively(
                                matchedMethod.getReturnType(),
                                classSubstitutor);

                        methodReturnValue.setValue(random.nextLong());
                        TestCaseWriter.setParameterTypeFromPsiType(methodReturnValue, ungenericReturnClassType, true);
                        DataInfo probeInfo = new DataInfo(
                                0, 0, 0, 0, 0, EventType.ARRAY_LENGTH, Descriptor.Boolean, ""
                        );
                        DataEventWithSessionId returnValueDataEvent = new DataEventWithSessionId();

                        String dummyValue = ClassUtils.createDummyValue(ungenericReturnClassType,
                                new LinkedList<>(), methodAdapter.getContainingClass().getProject());


                        returnValueDataEvent.setSerializedValue(dummyValue.getBytes());
                        methodReturnValue.setProbeAndProbeInfo(returnValueDataEvent, probeInfo);


                        MethodCallExpression mce = new MethodCallExpression(
                                methodName, fieldByName, methodArguments, methodReturnValue, 0
                        );
                        int methodAccess = buildMethodAccessModifier(matchedMethod.getModifierList());
                        methodAccess = methodAccess | Opcodes.ACC_PUBLIC;
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
        MethodAdapter matchedMethod = getMatchingMethod(methodAdapter.getContainingClass(), invokedMethodName,
                callParameterExpression);
        if (matchedMethod == null) {
            return Collections.emptyList();
        }

        return extractMethodCalls(matchedMethod);
    }

    private MethodAdapter getMatchingMethod(
            ClassAdapter classReference,
            String methodName,
            PsiExpressionListImpl callParameterExpression
    ) {
        logger.debug("Find matching method for [" + methodName + "] - " + classReference.getName());
        List<ClassAdapter> classesToCheck = new ArrayList<>();
        classesToCheck.add(classReference);
        Set<ClassAdapter> interfaces = getInterfaces(classReference);
        classesToCheck.addAll(interfaces);

        for (ClassAdapter psiClass : classesToCheck) {
            MethodAdapter[] methods = psiClass.getMethods();
            List<MethodAdapter> matchedMethods = Arrays.stream(methods)
                    .filter(e -> e.getName().equals(methodName))
                    .filter(e -> e.getParameters().length == callParameterExpression.getExpressionCount())
                    .collect(Collectors.toList());
            if (matchedMethods.size() == 0) {
                continue;
            }
            for (MethodAdapter matchedMethod : matchedMethods) {
                boolean isMismatch = false;
                logger.debug(
                        "Check matched method: [" + matchedMethod + "] in class [" + psiClass.getQualifiedName() + "]");
                PsiType[] expectedExpressionType = callParameterExpression.getExpressionTypes();
                ParameterAdapter[] parameters = matchedMethod.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    ParameterAdapter parameter = parameters[i];
                    PsiType type = parameter.getType();
                    if (type instanceof PsiClassReferenceType && type.getCanonicalText().length() == 1) {
                        // this is a generic type, wont match with the actual expression type
                        continue;
                    }
                    if (expectedExpressionType[i] == null || !type.isAssignableFrom(expectedExpressionType[i])) {
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

    private Set<ClassAdapter> getInterfaces(ClassAdapter psiClass) {
        Set<ClassAdapter> interfacesList = new HashSet<>();
        ClassAdapter[] interfaces = psiClass.getInterfaces();
        for (ClassAdapter anInterface : interfaces) {
            interfacesList.add(anInterface);
            interfacesList.addAll(getInterfaces(anInterface));
        }
        ClassAdapter[] supers = psiClass.getSupers();
        for (ClassAdapter aSuper : supers) {
            interfacesList.add(aSuper);
            interfacesList.addAll(getInterfaces(aSuper));
        }
        return interfacesList;
    }

    private List<TestCandidateMetadata> buildConstructorCandidate(
            ClassAdapter currentClass, Parameter testSubject, VariableContainer fieldContainer) {
        List<TestCandidateMetadata> candidateList = new ArrayList<>();

        MethodAdapter[] constructors = currentClass.getConstructors();
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
        MethodAdapter selectedConstructor = null;
        for (MethodAdapter constructor : constructors) {
            if (selectedConstructor == null) {
                selectedConstructor = constructor;
                continue;
            }
            ParameterAdapter[] constructorParameterList = constructor.getParameters();
            ParameterAdapter[] selectedConstructorParameterList = selectedConstructor.getParameters();
            if (constructorParameterList.length > selectedConstructorParameterList.length) {
                boolean isRecursiveConstructor = false;
                for (ParameterAdapter parameter : constructorParameterList) {
                    PsiType type = parameter.getType();
                    if (currentClass.getQualifiedName().equals(type.getCanonicalText())) {
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
        List<Parameter> methodArguments = new ArrayList<>(selectedConstructor.getParameters().length);

        for (ParameterAdapter parameter : selectedConstructor.getParameters()) {

            PsiType parameterType = parameter.getType();
            List<Parameter> fieldParameterByType = fieldContainer
                    .getParametersByType(parameterType.getCanonicalText());
            String parameterName = parameter.getName();

            if (fieldParameterByType.size() > 0) {

                Parameter closestNameMatch = fieldParameterByType.get(0);
                int currentDistance = Integer.MAX_VALUE;

                for (Parameter fieldParameter : fieldParameterByType) {
                    int distance = StringUtils.getLevenshteinDistance(fieldParameter.getName(), parameterName);
                    if (distance < currentDistance) {
                        closestNameMatch = fieldParameter;
                        currentDistance = distance;
                    }
                }
                methodArguments.add(closestNameMatch);

            } else {
                Parameter methodArgumentParameter = new Parameter();
                methodArgumentParameter.setName(parameterName);
                TestCaseWriter.setParameterTypeFromPsiType(methodArgumentParameter, parameterType, false);
                methodArgumentParameter.setValue(random.nextLong());
                DataEventWithSessionId argumentProbe = new DataEventWithSessionId();

                if (methodArgumentParameter.isPrimitiveType()) {
                    argumentProbe.setSerializedValue("0".getBytes());
                } else if (methodArgumentParameter.isStringType()) {
                    argumentProbe.setSerializedValue("\"\"".getBytes());
                } else {

                    String parameterClassName = parameterType.getCanonicalText();
                    if (parameterType instanceof PsiClassReferenceType) {
                        parameterClassName = ((PsiClassReferenceType) parameterType).rawType().getCanonicalText();
                    }
                    ClassAdapter parameterClassReference = getClassByName(parameterClassName);

                    if (parameterClassReference == null) {
                        logger.warn("did not find class reference: " + parameterClassName +
                                " for parameter: " + parameterName +
                                " in class " + currentClass.getQualifiedName());
                        continue;
                    }

                    if (parameterClassReference.getQualifiedName().equals(currentClass.getQualifiedName())) {
                        continue;
                    }

                    List<TestCandidateMetadata> constructorMetadata =
                            buildConstructorCandidate(parameterClassReference, methodArgumentParameter, fieldContainer);
                    candidateList.addAll(constructorMetadata);
                }
                methodArgumentParameter.setProbeAndProbeInfo(argumentProbe, new DataInfo());
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

    private ClassAdapter getClassByName(String className) {
        PsiClass aClass = JavaPsiFacade.getInstance(methodAdapter.getProject())
                .findClass(ClassTypeUtils.getDescriptorToDottedClassName(className),
                        GlobalSearchScope.allScope(methodAdapter.getProject()));
        if (aClass == null) {
            return null;
        }
        return new JavaClassAdapter(aClass);
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

    public LightVirtualFile getLightVirtualFile() {
        return testCaseScriptFile;
    }

    public void setEditorReferences(Editor viewerFileEditor, FileEditor fileEditor) {
        this.editorReference = viewerFileEditor;
        this.fileEditorReference = fileEditor;
        fileEditorReference.addPropertyChangeListener(evt -> {
            logger.warn("editor property changed" + evt);
        });
    }
}
