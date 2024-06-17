package com.insidious.plugin.ui.testdesigner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.JavaParserUtils;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.ValueResourceContainer;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaFileType;
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
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.FileContentUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.*;

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
    private TestCaseWriter testCaseWriter;

    public TestCaseDesignerLite(MethodAdapter currentMethod,
                                TestCaseGenerationConfiguration configuration,
                                boolean generateBoilerPlate,
                                InsidiousService insidiousService) {

        this.methodAdapter = currentMethod;
        this.testCaseWriter = new TestCaseWriter();
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

        TestCaseGenerationConfiguration currentTestGenerationConfiguration = new TestCaseGenerationConfiguration(
                (TestFramework) testFrameworkComboBox.getSelectedItem(),
                (MockFramework) mockFrameworkComboBox.getSelectedItem(),
                (JsonFramework) jsonFrameworkComboBox.getSelectedItem(),
                ResourceEmbedMode.IN_CODE
        );
        if (useMockitoAnnotationsMockCheckBox.isSelected()) {
            currentTestGenerationConfiguration.setUseMockitoAnnotations(true);
        }
        if (addFieldMocksCheckBox.isSelected()) {
            currentTestGenerationConfiguration.setAddFieldMocksCheckBox(true);
        }
        saveLocationTextField.setText("");

        if (generateBoilerPlate) {
            String testMethodName = "testMethod" + ClassTypeUtils.upperInstanceName(methodAdapter.getName());
            currentTestGenerationConfiguration.setTestMethodName(testMethodName);
            List<TestCandidateMetadata> candidates = testCaseWriter.generateTestCaseBoilerPlace(
                    methodAdapter, currentTestGenerationConfiguration);
            currentTestGenerationConfiguration.getTestCandidateMetadataList().addAll(candidates);
            for (TestCandidateMetadata tcm : candidates) {
                // mock all calls by default
                currentTestGenerationConfiguration.getCallExpressionList().addAll(tcm.getCallsList());
            }

        } else {
            currentTestGenerationConfiguration.getTestCandidateMetadataList().addAll(configuration.getTestCandidateMetadataList());
            currentTestGenerationConfiguration.getCallExpressionList().addAll(configuration.getCallExpressionList());
            currentTestGenerationConfiguration.setTestMethodName(configuration.getTestMethodName());

        }

        addFieldMocksCheckBox.addActionListener(e -> {
            List<TestCandidateMetadata> candidates = testCaseWriter.generateTestCaseBoilerPlace(methodAdapter,
                    currentTestGenerationConfiguration);
            currentTestGenerationConfiguration.getTestCandidateMetadataList().addAll(candidates);
            for (TestCandidateMetadata tcm : candidates) {
                currentTestGenerationConfiguration.getCallExpressionList()
                        .addAll(tcm.getCallsList());
            }

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
