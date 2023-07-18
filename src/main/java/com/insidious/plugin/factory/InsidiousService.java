package com.insidious.plugin.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.insidious.plugin.Constants;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.*;
import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.datafile.AtomicRecordService;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.ModuleInformation;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.TestSuite;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.GutterClickNavigationStates.AtomicTestContainer;
import com.insidious.plugin.ui.InsidiousCaretListener;
import com.insidious.plugin.ui.NewTestCandidateIdentifiedListener;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.eventviewer.SingleWindowView;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.ui.methodscope.MethodDirectInvokeComponent;
import com.insidious.plugin.ui.testdesigner.TestCaseDesigner;
import com.insidious.plugin.ui.testgenerator.LiveViewWindow;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.HotSwapStatusListener;
import com.intellij.debugger.ui.HotSwapUIImpl;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.DataManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vcs.BranchChangeListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.FileContentUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.insidious.plugin.util.AtomicRecordUtils.filterStoredCandidates;

@Storage("insidious.xml")
final public class InsidiousService implements Disposable,
        NewTestCandidateIdentifiedListener, BranchChangeListener, GutterStateProvider, ConnectionStateListener {
    public static final String HOSTNAME = System.getProperty("user.name");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PACKAGE_NAME = "YOUR.PACKAGE.NAME";
    private final ProjectTypeInfo projectTypeInfo = new ProjectTypeInfo();
    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(5);
    private final AgentClient agentClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionLoader sessionLoader;
    private final Map<String, DifferenceResult> executionRecord = new TreeMap<>();
    private final Map<String, Integer> methodHash = new TreeMap<>();
    private final DefaultMethodArgumentValueCache methodArgumentValueCache = new DefaultMethodArgumentValueCache();
    final private int TOOL_WINDOW_HEIGHT = 430;
    final private int TOOL_WINDOW_WIDTH = 600;
    final private AgentStateProvider agentStateProvider;
    private final Map<String, ModuleInformation> moduleMap = new TreeMap<>();
    private Project project;
    private VideobugClientInterface client;
    private Module currentModule;
    private ToolWindow toolWindow;
    private LiveViewWindow liveViewWindow;
    private Content singleWindowContent;
    private boolean rawViewAdded = false;
    private boolean liveViewAdded = false;
    private Content liveWindowContent;
    private String selectedModule = null;
    private TestCaseDesigner testCaseDesignerWindow;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private boolean initiated = false;
    private Content testDesignerContent;
    private MethodDirectInvokeComponent methodDirectInvokeComponent;
    private Content directMethodInvokeContent;
    private Content atomicTestContent;
    private AtomicTestContainer atomicTestContainerWindow;
    private MethodAdapter currentMethod;
    private boolean hasShownIndexWaitNotification = false;
    private String basePackage = null;
    private ReportingService reportingService = new ReportingService(this);
    private AtomicRecordService atomicRecordService;
    private Map<String, String> candidateIndividualContextMap = new TreeMap<>();

    public InsidiousService(Project project) {
        this.project = project;
        logger.info("starting insidious service: " + project);
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("projectName", project.getName());
        UsageInsightTracker.getInstance().RecordEvent("UNLOGGED_INIT", eventProperties);


        String pathToSessions = Constants.HOME_PATH + "/sessions";
        FileSystems.getDefault().getPath(pathToSessions).toFile().mkdirs();
        this.client = new VideobugLocalClient(pathToSessions, project);
        this.sessionLoader = new SessionLoader(client, this);
        threadPoolExecutor.submit(sessionLoader);


        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        InsidiousCaretListener listener = new InsidiousCaretListener(project);
        multicaster.addEditorMouseListener(listener, this);
        agentStateProvider = new DefaultAgentStateProvider(this);
//        threadPoolExecutor.submit(agentStateProvider);
        agentClient = new AgentClient("http://localhost:12100", (ConnectionStateListener) agentStateProvider);

        ensureToolWindow();
    }

    @NotNull
    private static String getClassMethodHashKey(MethodAdapter method) {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<String>) () -> method.getContainingClass().getQualifiedName() + "#" + method.getName());
    }

    public void addAgentToRunConfig(String javaAgentString) {


        RunManager runManager = project.getService(RunManager.class);
        RunnerAndConfigurationSettings selectedConfig = runManager.getSelectedConfiguration();

        if (selectedConfig != null && selectedConfig.getConfiguration() instanceof ApplicationConfiguration) {
            ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) selectedConfig.getConfiguration();
            String currentVMParams = applicationConfiguration.getVMParameters();
            String newVmOptions = VideobugUtils.addAgentToVMParams(currentVMParams, javaAgentString);
            applicationConfiguration.setVMParameters(newVmOptions.trim());
            InsidiousNotification.notifyMessage("Updated VM parameter for " + selectedConfig.getName(),
                    NotificationType.INFORMATION
            );
        } else {
//            ToolWindowManager.getInstance(project)
//                    .notifyByBalloon("Unlogged", MessageType.ERROR, "Current run configuration [" + selectedConfig.getName() + "] is not " +
//                            "a Java application run configuration. Select an existing Java Application in RunConfig of create" +
//                            " new. Cannot add VM parameter.");
            if (selectedConfig != null) {
                InsidiousNotification.notifyMessage(
                        "Current run configuration [" + selectedConfig.getName() + "] is not " +
                                "a Java application run configuration. Select an existing Java Application in RunConfig of create" +
                                " new. Cannot add VM parameter.",
                        NotificationType.WARNING);

            } else {
                InsidiousNotification.notifyMessage("Please select an existing Java " +
                                "Application in RunConfig of create new.",
                        NotificationType.WARNING);

            }
        }
    }

    @NotNull
    public String getTestDirectory(String packageName, String basePath) {
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        return basePath + "src/test/java/" + packageName.replaceAll("\\.", "/");
    }

    private synchronized void start() {
        try {

            logger.info("started insidious service - project name - " + project.getName());
            if (ModuleManager.getInstance(project).getModules().length == 0) {
                logger.warn("no module found in the project");
            } else {
                currentModule = ModuleManager.getInstance(project).getModules()[0];
                logger.info("current module - " + currentModule.getName());
            }
            this.atomicRecordService = new AtomicRecordService(this);
            this.initiateUI();

        } catch (ServiceNotReadyException snre) {
            snre.printStackTrace();
            logger.info("service not ready exception -> " + snre.getMessage());
        } catch (ProcessCanceledException ignored) {
            ignored.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("exception in unlogged service init", e);
        }

    }

    public ProjectTypeInfo getProjectTypeInfo() {
        return projectTypeInfo;
    }

    public void copyToClipboard(String string) {
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        clipboard.setContents(selection, null);
//        logger.info(selection);
    }


    public PROJECT_BUILD_SYSTEM findBuildSystemForModule(String modulename) {
        Module module = moduleMap.get(modulename)
                .getModule();
//        System.out.println("Fetching build system type");
//        System.out.println("MODULE - > " + module.toString());
//        System.out.println("MOD MAP - > " + moduleMap.toString());

        PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml",
                GlobalSearchScope.moduleScope(module));
        if (pomFileSearchResult.length > 0 || moduleHasFileOfType(modulename, "pom.xml")) {
            return PROJECT_BUILD_SYSTEM.MAVEN;
        }
        PsiFile[] gradleSearchResults = FilenameIndex.getFilesByName(project, "build.gradle",
                GlobalSearchScope.moduleScope(moduleMap.get(modulename)
                        .getModule()));
        if (gradleSearchResults.length > 0 || moduleHasFileOfType(modulename, "build.gradle")) {
            return PROJECT_BUILD_SYSTEM.GRADLE;
        }
        return PROJECT_BUILD_SYSTEM.DEF;
    }


    public synchronized void init(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (this.initiated) {
            return;
        }
        this.initiated = true;
        this.project = project;
        this.toolWindow = toolWindow;
        start();
    }

    public String fetchPathToSaveTestCase(TestCaseUnit testCaseScript) {
        StringBuilder sb = new StringBuilder(testCaseScript.getClassName()
                .replaceFirst("Test", ""));
        sb.deleteCharAt(sb.length() - 1);

        @NotNull PsiFile[] classBase = FilenameIndex.getFilesByName(project, sb + ".java",
                GlobalSearchScope.projectScope(project));

        if (classBase.length > 0) {
            if (classBase.length > 1) {
                //compare packages
                for (int i = 0; i < classBase.length; i++) {
                    PsiFile file = classBase[i];
                    if (file instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                        String packageName = psiJavaFile.getPackageName();
                        if (testCaseScript.getPackageName()
                                .equals(packageName)) {
                            return getBasePathForVirtualFile(classBase[i].getVirtualFile());
                        }
                    }
                }
                return getBasePathForVirtualFile(classBase[0].getVirtualFile());
            }
            return getBasePathForVirtualFile(classBase[0].getVirtualFile());
        }
        return null;
    }

    public String getBasePathForVirtualFile(VirtualFile classFound) {
        String path = classFound.getPath();
        int last_index = path.lastIndexOf("src");
        if (last_index == -1) {
            return null;
        }
        return path.substring(0, last_index);
    }

    public String getTestCandidateCode(TestCaseGenerationConfiguration generationConfiguration) throws Exception {
        TestCaseService testCaseService = getTestCaseService();
        if (testCaseService == null) {
            return null;
        }
        @NotNull TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
        return testCaseUnit.getCode();
    }

    public void generateAndSaveTestCase(TestCaseGenerationConfiguration generationConfiguration) throws Exception {
        TestCaseService testCaseService = getTestCaseService();
        if (testCaseService == null) {
            return;
        }
        @NotNull TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
        ArrayList<TestCaseUnit> testCaseScripts = new ArrayList<>();
        testCaseScripts.add(testCaseUnit);
        TestSuite testSuite = new TestSuite(testCaseScripts);
        saveTestSuite(testSuite);
    }

    public VirtualFile saveTestSuite(TestSuite testSuite) throws IOException {
        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
            String basePath = fetchPathToSaveTestCase(testCaseScript);
            if (basePath == null) {
                basePath = project.getBasePath();
            }
            logger.info("[TEST CASE SAVE] basepath : " + basePath);
            Map<String, Object> valueResourceMap = testCaseScript.getTestGenerationState()
                    .getValueResourceMap();
            if (valueResourceMap.values().size() > 0) {
                String testResourcesDirPath =
                        basePath + "/src/test/resources/unlogged-fixtures/" + testCaseScript.getClassName();
                File resourcesDirFile = new File(testResourcesDirPath);
                resourcesDirFile.mkdirs();
                String resourceJson = gson.toJson(valueResourceMap);

                String testResourcesFilePath = testResourcesDirPath + "/" + testCaseScript.getTestMethodName() + ".json";
                try (FileOutputStream resourceFile = new FileOutputStream(testResourcesFilePath)) {
                    resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                }
                VirtualFileManager.getInstance().refreshAndFindFileByUrl(FileSystems.getDefault()
                        .getPath(testResourcesFilePath).toUri().toString());

                if (testCaseScript.getTestGenerationState().isSetupNeedsJsonResources()) {

                    String setupJsonFilePath = testResourcesDirPath + "/" + "setup" + ".json";
                    try (FileOutputStream resourceFile = new FileOutputStream(setupJsonFilePath)) {
                        resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                    }
                    VirtualFileManager.getInstance()
                            .refreshAndFindFileByUrl(FileSystems.getDefault()
                                    .getPath(setupJsonFilePath).toUri().toString());
                }
            }


            String testOutputDirPath = getTestDirectory(testCaseScript.getPackageName(), basePath);

            File outputDir = new File(testOutputDirPath);
            outputDir.mkdirs();
            File testcaseFile = new File(testOutputDirPath + "/" + testCaseScript.getClassName() + ".java");
            logger.info("[TEST CASE SAVE] testcaseFile : " + testcaseFile.getAbsolutePath());

            if (!testcaseFile.exists()) {
                try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                    out.write(testCaseScript.getCode()
                            .getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage(
                            "Failed to write test case: " + testCaseScript + " -> "
                                    + e.getMessage(), NotificationType.ERROR);
                }


            } else {
                JavaParser javaParser = new JavaParser(new ParserConfiguration());
                ParseResult<CompilationUnit> parsedFile = javaParser.parse(
                        testcaseFile);
                if (!parsedFile.getResult()
                        .isPresent() || !parsedFile.isSuccessful()) {
                    InsidiousNotification.notifyMessage("<html>Failed to parse existing test case in the file, unable" +
                            " to" +
                            " add new test case. <br/>" + parsedFile.getProblems() + "</html>", NotificationType.ERROR);
                    return null;
                }
                CompilationUnit existingCompilationUnit = parsedFile.getResult().get();
//                ClassOrInterfaceDeclaration classDeclaration = existingCompilationUnit.getClassByName(
//                                testCaseScript.getClassName()).get();

//                TypeSpec newTestSpec = testCaseScript.getTestClassSpec();

                ParseResult<CompilationUnit> parseResult = javaParser.parse(testCaseScript.getCode());
                if (!parseResult.isSuccessful()) {
                    logger.error("Failed to parse test case to be written: \n" + testCaseScript.getCode() +
                            "\nProblems");
                    List<Problem> problems = parseResult.getProblems();
                    for (int i = 0; i < problems.size(); i++) {
                        Problem problem = problems.get(i);
                        logger.error("Problem [" + i + "] => " + problem);
                    }

                    InsidiousNotification.notifyMessage("Failed to parse test case to write " +
                            parseResult.getProblems(), NotificationType.ERROR
                    );
                    return null;
                }
                CompilationUnit newCompilationUnit = parseResult.getResult().get();

//                MethodDeclaration newMethodDeclaration =
//                        newCompilationUnit.getClassByName(testCaseScript.getClassName())
//                                .get().getMethodsByName(newTestSpec.methodSpecs.get(1).name).get(0);

                JavaParserUtils.mergeCompilationUnits(existingCompilationUnit, newCompilationUnit);

                try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                    out.write(existingCompilationUnit.toString()
                            .getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage(
                            "Failed to write test case: " + testCaseScript + " -> "
                                    + e.getMessage(), NotificationType.ERROR
                    );
                }


            }
            try {
                ensureTestUtilClass(basePath);
            } catch (Exception e) {
                logger.info("[ERROR] Failed to save UnloggedUtils to correct spot.");
            }

            @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                    .refreshAndFindFileByUrl(FileSystems.getDefault()
                            .getPath(testcaseFile.getAbsolutePath())
                            .toUri()
                            .toString());
            if (newFile == null) {
                return null;
            }
            newFile.refresh(true, false);


            List<VirtualFile> newFile1 = new ArrayList<>();
            newFile1.add(newFile);
            FileContentUtil.reparseFiles(project, newFile1, true);
            @Nullable Document newDocument = FileDocumentManager.getInstance().getDocument(newFile);

            FileEditorManager.getInstance(project).openFile(newFile, true, true);

            logger.info("Test case generated in [" + testCaseScript.getClassName() + "]\n" + testCaseScript);
            return newFile;
        }

        return null;
    }

    public void ensureTestUtilClass(String basePath) throws IOException {
        String testOutputDirPath;
        if (basePath != null) {
            testOutputDirPath = basePath + "src/test/java/io/unlogged";
        } else {
            basePath = project.getBasePath();
            testOutputDirPath = project.getBasePath() + "/src/test/java/io/unlogged";
        }
        if (basePath.charAt(basePath.length() - 1) == '/') {
            basePath = new StringBuilder(basePath).
                    deleteCharAt(basePath.length() - 1)
                    .toString();
        }
        File dirPath = new File(testOutputDirPath);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        logger.info("[TEST CASE SAVE] UnloggedUtils path : " + basePath);
        // yikes right
        try {
            String oldFolderPath = basePath + "/src/test/java/io.unlogged";
            String oldFilePath = basePath + "/src/test/java/io.unlogged/UnloggedTestUtils.java";
            File oldFolder = FileSystems.getDefault()
                    .getPath(oldFolderPath)
                    .toFile();
            File oldUtilFile = FileSystems.getDefault()
                    .getPath(oldFilePath)
                    .toFile();
            if (oldUtilFile.exists()) {
                @Nullable VirtualFile oldFileInstance = VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(FileSystems.getDefault()
                                .getPath(oldUtilFile.getAbsolutePath())
                                .toUri()
                                .toString());
                oldUtilFile.delete();
                oldFolder.delete();
                if (oldFileInstance != null) {
                    oldFileInstance.refresh(true, false);
                }
                @Nullable VirtualFile oldFolderInstance = VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(FileSystems.getDefault()
                                .getPath(oldFolder.getAbsolutePath())
                                .toUri()
                                .toString());
                if (oldFolderInstance != null) {
                    oldFolderInstance.refresh(true, false);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to delete the old version of the file: " + e.getMessage());
            // this is absolutely almost a mess
        }

        String utilFilePath = testOutputDirPath + "/UnloggedTestUtils.java";
        File utilFile = new File(utilFilePath);
        if (utilFile.exists()) {
            String utilFileContents = IOUtils.toString(new FileInputStream(utilFile), StandardCharsets.UTF_8);
            Pattern versionCaptureRegex = Pattern.compile("UnloggedTestUtils.Version: V([0-9]+)");
            Matcher versionMatcher = versionCaptureRegex.matcher(utilFileContents);
            if (versionMatcher.find()) {
                int version = Integer.parseInt(versionMatcher.group(1));
                if (version == 1) {
                    return;
                }
            }
            // util file already exist
            utilFile.delete();
        }

        String jacksonDatabindVersion = getProjectTypeInfo().getJacksonDatabindVersion();


        try (FileOutputStream writer = new FileOutputStream(utilFilePath)) {
            InputStream testUtilClassCode = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream("code/jackson/UnloggedTestUtil.java");

            if (projectTypeInfo.getUsesGson()) {
                if (jacksonDatabindVersion == null)
                    testUtilClassCode = this.getClass()
                            .getClassLoader()
                            .getResourceAsStream("code/gson/UnloggedTestUtil.java");

            }

            assert testUtilClassCode != null;
            IOUtils.copy(testUtilClassCode, writer);
        }
        @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                .refreshAndFindFileByUrl(
                        FileSystems.getDefault().getPath(utilFile.getAbsolutePath()).toUri().toString());

        if (newFile == null) {
            InsidiousNotification.notifyMessage("UnloggedTestUtil file was not created: "
                    + utilFile.getAbsolutePath(), NotificationType.WARNING);
            return;
        }

        newFile.refresh(true, false);

    }


    private synchronized void initiateUI() {
        logger.info("initiate ui");
        if (testCaseDesignerWindow != null) {
            return;
        }

        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        if (this.toolWindow == null) {
            UsageInsightTracker.getInstance().RecordEvent("ToolWindowNull", new JSONObject());
            logger.warn("tool window is null");
            return;
        }
        toolWindow.setIcon(UIUtils.UNLOGGED_ICON_DARK_SVG);
        try {
            ToolWindowEx ex = (ToolWindowEx) toolWindow;
            ex.stretchWidth(TOOL_WINDOW_WIDTH - ex.getDecorator().getWidth());
        } catch (NullPointerException npe) {
            // ignored
        }
        ContentManager contentManager = this.toolWindow.getContentManager();

        // test case designer form
        testCaseDesignerWindow = new TestCaseDesigner();
        Disposer.register(this, testCaseDesignerWindow);
        testDesignerContent =
                contentFactory.createContent(testCaseDesignerWindow.getContent(), "TestCase Boilerplate", false);
        testDesignerContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        testDesignerContent.setIcon(UIUtils.UNLOGGED_ICON_DARK);
//        contentManager.addContent(testDesignerContent);

        // method executor window
        atomicTestContainerWindow = new AtomicTestContainer(this);
        atomicTestContent =
                contentFactory.createContent(atomicTestContainerWindow.getComponent(), "Get Started", false);
        atomicTestContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        atomicTestContent.setIcon(UIUtils.ATOMIC_TESTS);
        contentManager.addContent(atomicTestContent);

        if (agentStateProvider.isAgentRunning()) {
            atomicTestContainerWindow.loadComponentForState(GutterState.PROCESS_RUNNING);
        } else if (agentStateProvider.doesAgentExist()) {
            atomicTestContainerWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
        }
//        else {
//            atomicTestContainerWindow.loadComponentForState(GutterState.NO_AGENT);
//        }


        methodDirectInvokeComponent = new MethodDirectInvokeComponent(this);
        this.directMethodInvokeContent =
                contentFactory.createContent(methodDirectInvokeComponent.getContent(), "Direct Invoke", false);
        this.directMethodInvokeContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        this.directMethodInvokeContent.setIcon(UIUtils.EXECUTE_METHOD);
//        contentManager.addContent(this.directMethodInvokeContent);
        SingleWindowView singleWindowView = new SingleWindowView(project, this);
        singleWindowContent = contentFactory.createContent(singleWindowView.getContent(),
                "Raw Cases", false);


//        gptWindow = new UnloggedGPT(this);
//        gptContent = contentFactory.createContent(gptWindow.getComponent(), "UnloggedGPT", false);
//        gptContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        gptContent.setIcon(UIUtils.UNLOGGED_GPT_ICON_PINK);
//        contentManager.addContent(gptContent);

        // test candidate list by packages
        liveViewWindow = new LiveViewWindow(project);
        liveWindowContent = contentFactory.createContent(liveViewWindow.getContent(), "Test Cases", false);
        liveWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        liveWindowContent.setIcon(UIUtils.TEST_CASES_ICON_PINK);
        //contentManager.addContent(liveWindowContent);

//        contentManager.addContent(onboardingConfigurationWindowContent);
//        if (areLogsPresent()) {
//            contentManager.setSelectedContent(liveWindowContent, true);
//            liveViewAdded = true;
//        }
    }

    public VideobugClientInterface getClient() {
        return client;
    }

    @Override
    public void dispose() {
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("projectName", project.getName());
        UsageInsightTracker.getInstance().RecordEvent("UNLOGGED_DISPOSED", eventProperties);
        logger.warn("Disposing InsidiousService for project: " + project.getName());
        threadPoolExecutor.shutdownNow();
        if (this.client != null) {
            this.client.close();
            this.client = null;
            currentModule = null;
        }
        agentClient.close();
        if (this.sessionInstance != null) {
            try {
                this.sessionInstance.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void generateAndUploadReport() {
        UsageInsightTracker.getInstance()
                .RecordEvent("DiagnosticReport", null);
        DiagnosticService diagnosticService = new DiagnosticService(new VersionManager(), this.project,
                this.currentModule);
        diagnosticService.generateAndUploadReport();
    }

    public Project getProject() {
        return project;
    }

    public void attachRawView() {
        if (rawViewAdded) {
            return;
        }
        ContentManager contentManager = this.toolWindow.getContentManager();
        contentManager.addContent(singleWindowContent);
        rawViewAdded = true;
    }

    public void addLiveView() {
        UsageInsightTracker.getInstance().RecordEvent("ProceedingToLiveView", null);
        if (!liveViewAdded) {
            toolWindow.getContentManager().addContent(liveWindowContent);
            toolWindow.getContentManager().setSelectedContent(liveWindowContent, true);
            liveViewAdded = true;
            try {
                liveViewWindow.setTreeStateToLoading();
                liveViewWindow.loadInfoBanner();
            } catch (Exception e) {
                //exception setting state
                logger.error("Failed to start scan after proceed.");
            }
        } else {
            toolWindow.getContentManager().setSelectedContent(liveWindowContent);
        }
    }


    public boolean moduleHasFileOfType(String module, String key) {
        Collection<VirtualFile> searchResult = FilenameIndex.getVirtualFilesByName(project, key,
                GlobalSearchScope.projectScope(project));
        for (VirtualFile virtualFile : searchResult) {
            if (virtualFile.getPath()
                    .contains(module)) {
                return true;
            }
        }
        return false;
    }


    public void methodFocussedHandler(final MethodAdapter method) {

        if (method == null) {
            return;
        }
        if (currentMethod != null && currentMethod.equals(method)) {
            return;
        }
        currentMethod = method;
        final ClassAdapter psiClass;
        try {
            psiClass = method.getContainingClass();
        } catch (Exception e) {
            // not a focussable element. return silently
            return;
        }

        if (psiClass.getName() == null) {
            return;
        }

        DumbService dumbService = project.getService(DumbService.class);

        if (dumbService.isDumb()) {
//            hasShownIndexWaitNotification = true;
//            InsidiousNotification.notifyMessage("Please wait for IDE indexing to finish to start creating tests",
//                    NotificationType.WARNING);
//            dumbService.runWhenSmart(() -> {
//                if (testCaseDesignerWindow == null) {
//                    logger.warn("test case designer window is not ready to create test case");
//                    return;
//                }
//                methodFocussedHandler(method);
//            });
            return;
        }

//        if (this.gptWindow != null) {
//            this.gptWindow.updateUI(psiClass, method);
//        }

        if (testCaseDesignerWindow == null || !this.toolWindow.isVisible()) {
            logger.warn("test case designer window is not ready to create test case");
            return;
        }

        methodDirectInvokeComponent.renderForMethod(method);
        atomicTestContainerWindow.triggerMethodExecutorRefresh(method);
//        testCaseDesignerWindow.renderTestDesignerInterface(method);
    }

    public void compile(ClassAdapter psiClass, CompileStatusNotification compileStatusNotification) {
//        ModuleManager moduleManager = ModuleManager.getInstance(project);
//        BuildManager buildManager = BuildManager.getInstance();
        XDebuggerManager xDebugManager = XDebuggerManager.getInstance(project);
        Optional<XDebugSession> currentSessionOption = Arrays
                .stream(xDebugManager.getDebugSessions())
                .filter(e -> e.getProject().equals(project))
                .findFirst();
        if (!currentSessionOption.isPresent()) {
            InsidiousNotification.notifyMessage("No debugger session found, cannot trigger hot-reload",
                    NotificationType.WARNING);
            return;
        }

        DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx(project);
        XDebugSession currentXDebugSession = currentSessionOption.get();

        Optional<DebuggerSession> currentDebugSession = debuggerManager.getSessions().stream()
                .filter(e -> e.getXDebugSession().equals(currentXDebugSession)).findFirst();

        if (!currentDebugSession.isPresent()) {
            InsidiousNotification.notifyMessage("No debugger session found, cannot trigger hot-reload",
                    NotificationType.WARNING);
            return;
        }

        CompilerManager compilerManager = CompilerManager.getInstance(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            compilerManager.compile(
                    new VirtualFile[]{psiClass.getContainingFile().getVirtualFile()},
                    (aborted, errors, warnings, compileContext) -> {
                        logger.warn("compiled class: " + compileContext);
                        if (aborted || errors > 0) {
                            compileStatusNotification.finished(aborted, errors, warnings, compileContext);
                            return;
                        }
                        HotSwapUIImpl.getInstance(project).reloadChangedClasses(currentDebugSession.get(), false,
                                new HotSwapStatusListener() {
                                    @Override
                                    public void onCancel(List<DebuggerSession> sessions) {
                                        compileStatusNotification.finished(true, errors, warnings, compileContext);
                                    }

                                    @Override
                                    public void onSuccess(List<DebuggerSession> sessions) {
                                        compileStatusNotification.finished(false, 0, warnings, compileContext);
                                    }

                                    @Override
                                    public void onFailure(List<DebuggerSession> sessions) {
                                        compileStatusNotification.finished(false, 1, warnings, compileContext);
                                    }
                                });
                    }
            );
        });


//        Object myClassClass = Reflect.compile(psiClass.getQualifiedName(),
//                psiClass.getContainingFile().getText()).create().get();
//            Constructor<?> firstConstructor = myClassClass.getConstructors()[0];
//            Object classInstance = firstConstructor.newInstance();

    }

    public AgentCommandRequest getAgentCommandRequests(AgentCommandRequest agentCommandRequest) {
        return methodArgumentValueCache.getArgumentSets(agentCommandRequest);
    }

    public void executeMethodInRunningProcess(
            AgentCommandRequest agentCommandRequest,
            ExecutionResponseListener executionResponseListener
    ) {

        methodArgumentValueCache.addArgumentSet(agentCommandRequest);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                AgentCommandResponse<String> agentCommandResponse = agentClient.executeCommand(agentCommandRequest);
                logger.warn("agent command response - " + agentCommandResponse);
                if (executionResponseListener != null) {
                    executionResponseListener.onExecutionComplete(agentCommandRequest, agentCommandResponse);
                } else {
                    logger.warn("no body listening for the response");
                }
            } catch (IOException e) {
                logger.warn("failed to execute command - " + e.getMessage(), e);
            }
        });
    }

    public boolean areModulesRegistered() {
        return this.moduleMap.size() > 0;
    }

    public TestCaseService getTestCaseService() {
        if (testCaseService == null) {
//            InsidiousNotification.notifyMessage(
//                    "Session isn't loaded yet, loading session", NotificationType.WARNING);
            loadDefaultSession();
//            return testCaseService;
        }
        return testCaseService;
    }

    public synchronized void setSession(ExecutionSession executionSession) throws SQLException, IOException {
        if (client == null) {
            String pathToSessions = Constants.HOME_PATH + "/sessions";
            FileSystems.getDefault().getPath(pathToSessions).toFile().mkdirs();
            this.client = new VideobugLocalClient(pathToSessions, project);
        }

        if (sessionInstance != null) {
            try {
                logger.info("Closing existing session: " + sessionInstance.getExecutionSession().getSessionId());
                sessionInstance.close();
            } catch (Exception e) {
                logger.error("Failed to close existing session before opening new session: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        this.executionRecord.clear();
        logger.info("Loading new session: " + executionSession.getSessionId());
        sessionInstance = new SessionInstance(executionSession, project);
        sessionInstance.setTestCandidateListener(this);
        client.setSessionInstance(sessionInstance);
        testCaseService = new TestCaseService(sessionInstance);

    }

    public void openToolWindow() {
        toolWindow.show(null);
    }

    public void refreshGPTWindow() {
//        if (this.gptContent != null) {
//            this.toolWindow.getContentManager().setSelectedContent(this.gptContent);
//        }
    }

//    public Pair<AgentCommandRequest, AgentCommandResponse> getExecutionPairs(String executionPairKey) {
//        return executionPairs.get(executionPairKey);
//    }

    @Override
    public void onNewTestCandidateIdentified(int completedCount, int totalCount) {
        logger.warn("new test cases identified [" + completedCount + "/" + totalCount + "]");
        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
        Editor[] currentOpenEditorsList = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : currentOpenEditorsList) {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (virtualFile instanceof LightVirtualFile) {
                continue;
            }

            @Nullable PsiFile psiFile = ApplicationManager.getApplication().runReadAction(
                    (ThrowableComputable<PsiFile, RuntimeException>) () -> PsiDocumentManager.getInstance(project)
                            .getPsiFile(editor.getDocument()));
            if (psiFile == null) {
                continue;
            }
            ApplicationManager.getApplication().runReadAction(
                    () -> DaemonCodeAnalyzer.getInstance(project).restart(psiFile));
        }
        if (this.toolWindow == null) {
            return;
        }

        if (atomicTestContainerWindow == null) {
            return;
        }

        if (currentMethod == null) {
            return;
        }

        if (currentMethod.equals(atomicTestContainerWindow.getCurrentMethod())) {
            ApplicationManager.getApplication().invokeLater(() -> {
                //show both
                ContentManager manager = this.toolWindow.getContentManager();
                if (manager.getContent(atomicTestContent.getComponent()) == null) {
                    //only add atomic if method has candidates.
                    if (getStoredCandidatesFor(currentMethod).size() > 0) {
                        manager.addContent(atomicTestContent, 0);
                        focusAtomicTestsWindow();
                    }
                }
                if (manager.getContent(directMethodInvokeContent.getComponent()) == null) {
                    manager.addContent(directMethodInvokeContent);
                }
            });
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            atomicTestContainerWindow.triggerMethodExecutorRefresh(currentMethod);
            methodDirectInvokeComponent.renderForMethod(currentMethod);
        });

    }

    public ClassMethodAggregates getClassMethodAggregates(String qualifiedName) {
        if (sessionInstance == null) {
            loadDefaultSession();
        }
        return sessionInstance.getClassMethodAggregates(qualifiedName);
    }


    public void loadDefaultSession() {
        String pathToSessions = Constants.HOME_PATH + "/sessions/na";
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setPath(pathToSessions);
        executionSession.setSessionId("na");
        executionSession.setCreatedAt(new Date());
        executionSession.setLastUpdateAt(new Date().getTime());

        try {
            setSession(executionSession);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ExecutionSession getCurrentExecutionSession() {
        return sessionInstance.getExecutionSession();
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }


    @Override
    public GutterState getGutterStateFor(MethodAdapter method) {
        // agent exists but cannot connect with agent server
        // so no process is running with the agent
        if (!agentStateProvider.isAgentRunning() || sessionInstance == null) {
            return GutterState.PROCESS_NOT_RUNNING;
        }

        List<TestCandidateMetadata> candidates = ApplicationManager.getApplication().runReadAction(
                (Computable<List<TestCandidateMetadata>>) () -> getTestCandidateMetadata(method));

        //check for stored candidates here
        boolean hasStoredCandidates = atomicRecordService.hasStoredCandidateForMethod(
                method.getContainingClass().getQualifiedName(),
                method.getName() + "#" + method.getJVMSignature());

        GutterState gutterState = atomicRecordService.computeGutterState(method.getContainingClass().getQualifiedName(),
                method.getName() + "#" + method.getJVMSignature(), method.getText().hashCode());

        // process is running, but no test candidates for this method
        if (candidates.size() == 0 && !hasStoredCandidates) {
            return GutterState.PROCESS_RUNNING;
        }

        // process is running, and there were test candidates for this method
        // so check if we have executed this before

        //check for change
        String hashKey = method.getContainingClass().getQualifiedName() + "#" + method.getName();

        // we havent checked anything for this method earlier
        // store method hash for diffs
        String methodText = method.getText();
        if (!this.methodHash.containsKey(hashKey)) {
            //register new hash
            this.methodHash.put(hashKey, methodText.hashCode());
        }

        int lastHash = this.methodHash.get(hashKey);
        int currentHash = methodText.hashCode();

        if (lastHash != currentHash) {
            //re-execute as there are hash diffs
            //update hash after execution is complete for this method,
            //to prevent state change before exec complete.
            return GutterState.EXECUTE;
        }

        if (!executionRecord.containsKey(hashKey) && hasStoredCandidates && gutterState != null) {
            return gutterState;
        } else {
            if (!executionRecord.containsKey(hashKey)) {
                return GutterState.DATA_AVAILABLE;
            }
        }

        DifferenceResult differenceResult = executionRecord.get(hashKey);
        String methodKey = method.getContainingClass().getQualifiedName()
                + "#" + method.getName() + "#" + method.getJVMSignature();

        if (this.candidateIndividualContextMap.get(methodKey) != null &&
                differenceResult.isUseIndividualContext()) {
            logger.info("Using flow ind : " + this.candidateIndividualContextMap.get(methodKey));
            switch (this.candidateIndividualContextMap.get(methodKey)) {
                case "Diff":
                    return GutterState.DIFF;
                case "NoRun":
                    return GutterState.EXECUTE;
                default:
                    return GutterState.NO_DIFF;
            }
        }
        logger.info("Using flow normal : "
                + differenceResult.getDiffResultType());
        switch (differenceResult.getDiffResultType()) {
            case DIFF:
                return GutterState.DIFF;
            case NO_ORIGINAL:
                return GutterState.NO_DIFF;
            case SAME:
                return GutterState.NO_DIFF;
            default:
                return GutterState.DIFF;
        }
    }

    public GutterState getGutterStateBasedOnAgentState() {
//        if(!agentStateProvider.doesAgentExist())
//        {
//            return GutterState.NO_AGENT;
//        }
        if (agentStateProvider.isAgentRunning()) {
            return GutterState.PROCESS_RUNNING;
        } else {
            return GutterState.PROCESS_NOT_RUNNING;
        }
    }

    public List<TestCandidateMetadata> getTestCandidateMetadata(MethodAdapter method) {
        if (method == null) {
            return List.of();
        }
        if (DumbService.getInstance(project).isDumb()) {
            return List.of();
        }

        String methodName = method.getName();
        ClassAdapter containingClass = method.getContainingClass();
        String methodClassQualifiedName = containingClass.getQualifiedName();

        String methodArgsDescriptor = getMethodArgsDescriptor(method);

        List<TestCandidateMetadata> candidates = sessionInstance.getTestCandidatesForAllMethod(
                methodClassQualifiedName, methodName, methodArgsDescriptor, false);

        ClassAdapter[] interfaceList = containingClass.getInterfaces();
        for (ClassAdapter classInterface : interfaceList) {
            boolean hasMethod = false;
            for (MethodAdapter interfaceMethod : classInterface.getMethods()) {
                if (interfaceMethod.getName().equals(methodName) &&
                        interfaceMethod.getJVMSignature().equals(method.getJVMSignature())) {
                    hasMethod = true;
                }
            }

            if (hasMethod) {
                List<TestCandidateMetadata> interfaceCandidates = sessionInstance.getTestCandidatesForAllMethod(
                        classInterface.getQualifiedName(), methodName, methodArgsDescriptor, false);
                candidates.addAll(interfaceCandidates);
            }
        }
        return candidates;
    }

    public List<StoredCandidate> getStoredCandidatesFor(MethodAdapter method) {
        if (method == null) {
            return List.of();
        }
        if (DumbService.getInstance(project).isDumb()) {
            return List.of();
        }

        List<TestCandidateMetadata> candidateMetadataList = getTestCandidateMetadata(method);
        List<StoredCandidate> candidates = atomicRecordService
                .getStoredCandidatesForMethod(method.getContainingClass().getQualifiedName(),
                        method.getName() + "#" + method.getJVMSignature());

        List<StoredCandidate> storedCandidates = new ArrayList<>(candidates);

        candidateMetadataList.stream()
                .map(StoredCandidate::new)
                .forEach(storedCandidates::add);

        logger.info("StoredCandidates pre filter for " + method.getName() + " -> " + storedCandidates);
        return filterStoredCandidates(storedCandidates);
    }

    @NotNull
    public String getMethodArgsDescriptor(MethodAdapter method) {
        ParameterAdapter[] methodParams = method.getParameters();
        StringBuilder methodArgumentsClassnames = new StringBuilder();
        boolean first = true;
        for (ParameterAdapter methodParam : methodParams) {
            if (!first) {
                methodArgumentsClassnames.append(",");
            }
            String canonicalText;
            canonicalText = methodParam.getType().getCanonicalText();
            if (methodParam.getType() instanceof PsiPrimitiveType) {
                canonicalText = ((PsiPrimitiveType) methodParam.getType()).getKind().getBinaryName();
            }
            if (canonicalText.contains("<")) {
                canonicalText = canonicalText.substring(0, canonicalText.indexOf("<"));
            }
            methodArgumentsClassnames.append(canonicalText);
            first = false;
        }
        return methodArgumentsClassnames.toString();
    }

    public void updateMethodHashForExecutedMethod(MethodAdapter method) {
        String classMethodHashKey = getClassMethodHashKey(method);
        if (this.executionRecord.containsKey(classMethodHashKey)) {

            String methodBody = ApplicationManager.getApplication()
                    .runReadAction((Computable<String>) method::getText);
            int methodBodyHashCode = methodBody.hashCode();
            this.methodHash.put(classMethodHashKey, methodBodyHashCode);
            DaemonCodeAnalyzer.getInstance(project).restart(method.getContainingFile());
        }
    }

    @Override
    public void branchWillChange(@NotNull String branchName) {

    }

    @Override
    public void branchHasChanged(@NotNull String branchName) {
        logger.warn("branch has changed: " + branchName);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void triggerGutterIconReload() {
        if (project.isDisposed()) {
            return;
        }

        DumbService instance = DumbService.getInstance(project);
        if (instance.isDumb()) {
            instance.runWhenSmart(this::triggerGutterIconReload);
            return;
        }

        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    public void updateScaffoldForState(GutterState state) {
        if (this.atomicTestContainerWindow != null) {
            atomicTestContainerWindow.loadComponentForState(state);
            if (this.atomicTestContent != null) {
                this.toolWindow.getContentManager().setSelectedContent(this.atomicTestContent);
            }
        }
    }

    public void focusDirectInvokeTab() {
        if (toolWindow == null || directMethodInvokeContent == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(
                () -> toolWindow.getContentManager().setSelectedContent(directMethodInvokeContent, false));
    }


    public void generateCompareWindows(String before, String after) {
        DocumentContent content1 = DiffContentFactory.getInstance().create(getPrettyJsonString(before));
        DocumentContent content2 = DiffContentFactory.getInstance().create(getPrettyJsonString(after));
        SimpleDiffRequest request = new SimpleDiffRequest("Comparing Before and After", content1, content2, "Before",
                "After");
        showDiffEditor(request);
    }

    private String getPrettyJsonString(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        JsonElement je = gson.fromJson(input, JsonElement.class);
        return gson.toJson(je);
    }


    public void showDiffEditor(SimpleDiffRequest request) {
        DiffManager.getInstance().showDiff(project, request);
    }

    public void addDiffRecord(MethodAdapter methodElement, DifferenceResult newDiffRecord) {
        if (newDiffRecord == null) {
            return;
        }
        String keyName = getClassMethodHashKey(methodElement);
        if (!executionRecord.containsKey(keyName)) {
            executionRecord.put(keyName, newDiffRecord);
            return;
        }
        if (!executionRecord.get(keyName).isUseIndividualContext() &&
                !newDiffRecord.isUseIndividualContext()) {
            if (!newDiffRecord.getBatchID().equals(executionRecord.get(keyName).getBatchID())) {
                //different replay all batch
                executionRecord.put(keyName, newDiffRecord);
            } else {
                //same replay all batch
                if (executionRecord.get(keyName).getDiffResultType().equals(DiffResultType.SAME)) {
                    executionRecord.put(keyName, newDiffRecord);
                }
            }
        } else {
            executionRecord.put(keyName, newDiffRecord);
        }
        addExecutionRecord(newDiffRecord);
    }

    public void addExecutionRecord(DifferenceResult result) {
        reportingService.addRecord(result);
    }

    public void openDirectExecuteWindow() {
        toolWindow.getContentManager().setSelectedContent(directMethodInvokeContent, true);
    }

    public synchronized String fetchBasePackage() {
        if (basePackage != null) {
            return basePackage;
        }

        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE,
                GlobalSearchScope.projectScope(project));


        List<String> components = new ArrayList<String>();
        for (VirtualFile vf : virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project).findFile(vf);
            if (psifile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if (packageName.contains(".")) {
                    if (components.size() == 0) {
                        String[] parts = packageName.split("\\.");
                        components = Arrays.asList(parts);
                    } else {
                        List<String> sp = Arrays.asList(packageName.split("\\."));
                        List<String> intersection = intersection(components, sp);
                        if (intersection.size() >= 2) {
                            components = intersection;
                        }
                    }
                }
            }
        }
        basePackage = buildPackageNameFromList(components);
        return basePackage;
    }

    private String buildPackageNameFromList(List<String> parts) {
        if (parts.size() < 2) {
            return "?";
        }
        StringBuilder packagename = new StringBuilder();
        for (String part : parts) {
            packagename.append(part)
                    .append(".");
        }
        packagename.deleteCharAt(packagename.length() - 1);
        return packagename.toString();
    }

    public <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();
        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

    public void setAgentProcessState(GutterState newState) {
        loadSingleWindowForState(newState);
        if (this.atomicTestContainerWindow != null) {
            atomicTestContainerWindow.loadComponentForState(newState);
        }
    }

    public void ensureToolWindow() {
        ApplicationManager.getApplication().invokeLater(() -> {
            this.init(this.project, ToolWindowManager.getInstance(project).getToolWindow("Unlogged"));
        });
    }

    public void compileAndExecuteWithAgentForMethod(JavaMethodAdapter methodAdapter) {
        atomicTestContainerWindow.triggerMethodExecutorRefresh(methodAdapter);
        atomicTestContainerWindow.triggerCompileAndExecute();
    }

//    public void loadMethodInAtomicTests(JavaMethodAdapter methodAdapter) {
//        atomicTestContainerWindow.triggerMethodExecutorRefresh(methodAdapter);
//    }

    public void showNewTestCandidateGotIt() {
//        new GotItTooltip("io.unlogged.newtestcase." + new Date().getTime(), "New test candidate found",
//                this).show();
    }

    public String fetchVersionFromLibName(String name, String dependency) {
        return agentStateProvider.fetchVersionFromLibName(name, dependency);
    }

    public void startProjectWithUnloggedAgent(String javaAgentString) {
        RunManager runManager = RunManagerEx.getInstance(project);

        RunnerAndConfigurationSettings selectedConfig = runManager.getSelectedConfiguration();

        if (selectedConfig != null && selectedConfig.getConfiguration() instanceof ApplicationConfiguration) {
            ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) selectedConfig.getConfiguration();
            String currentVMParams = applicationConfiguration.getVMParameters();
            String newVmOptions = VideobugUtils.addAgentToVMParams(currentVMParams, javaAgentString);
            applicationConfiguration.setVMParameters(newVmOptions.trim());

            ExecutionManager executionManager = ExecutionManager.getInstance(project);

            DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(dataContext -> {
                ExecutorRegistry executorRegistry = ExecutorRegistryImpl.getInstance();
                @Nullable Executor debugExecutor = executorRegistry.getExecutorById("Debug");
                if (debugExecutor == null) {
                    InsidiousNotification.notifyMessage("Debug run config not found for: " + selectedConfig.getName()
                            + ". Failed to start process in debug mode", NotificationType.ERROR);
                    return;
                }

                ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(debugExecutor,
                        selectedConfig);

                executionManager.restartRunProfile(
                        builder.activeTarget().dataContext(dataContext).build());
                UsageInsightTracker.getInstance().RecordEvent("START_WITH_UNLOGGED_SUCCESS", new JSONObject());

            });

        } else {

            List<RunConfiguration> allConfigurations = runManager.getAllConfigurationsList();

            JSONObject eventProperties = new JSONObject();
            for (RunConfiguration allConfiguration : allConfigurations) {
                eventProperties.put(allConfiguration.getName(), allConfiguration.getType().getDisplayName());
//                if (allConfiguration instanceof ApplicationConfiguration) {
//                    OkCancelDialogBuilder.okCancel()
//                }
            }


            UsageInsightTracker.getInstance().RecordEvent("START_WITH_UNLOGGED_NO_CONFIG", eventProperties);
            if (selectedConfig == null) {
                InsidiousNotification.notifyMessage("Please select or configure a JAVA Application Run configuration " +
                                "in IntelliJ",
                        NotificationType.WARNING);
            } else {
                InsidiousNotification.notifyMessage(
                        "Current run configuration [" + selectedConfig.getName() + "] is not " +
                                "a java application run configuration. Failed to start.",
                        NotificationType.WARNING);
            }
        }

    }

    public void focusAtomicTestsWindow() {
        if (this.atomicTestContent != null) {
            this.toolWindow.getContentManager().setSelectedContent(this.atomicTestContent);
        }
    }

    public AgentStateProvider getAgentStateProvider() {
        return agentStateProvider;
    }

    @Override
    public void onConnectedToAgentServer(ServerMetadata serverMetadata) {
        atomicTestContainerWindow.loadComponentForState(GutterState.PROCESS_RUNNING);
    }

    @Override
    public void onDisconnectedFromAgentServer() {
        atomicTestContainerWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
    }

    public void clearAtomicBoard()
    {
        atomicTestContainerWindow.clearBoardOnMethodExecutor();
    }

//    public void triggerAtomicTestsWindowRefresh() {
//        GutterState state = getGutterStateFor(currentMethod);
//        if (state.equals(GutterState.PROCESS_NOT_RUNNING) || state.equals(GutterState.PROCESS_RUNNING)) {
//            loadSingleWindowForState(state);
//        } else {
//            atomicTestContainerWindow.triggerMethodExecutorRefresh(null);
//        }
//    }

    public void setAtomicWindowHeading(String name) {
        atomicTestContent.setDisplayName(name);
        if (name.startsWith("Get")) {
            atomicTestContent.setIcon(UIUtils.ONBOARDING_ICON_PINK);
        } else {
            atomicTestContent.setIcon(UIUtils.ATOMIC_TESTS);
        }
    }

    public void loadSingleWindowForState(GutterState state) {
        ContentManager manager = this.toolWindow.getContentManager();
        List<Content> contentList = Arrays.asList(manager.getContents());
        if (state.equals(GutterState.PROCESS_RUNNING)) {
            if (contentList.contains(atomicTestContent)) {
                manager.removeContent(atomicTestContent, false);
            }
            if (!contentList.contains(directMethodInvokeContent)) {
                manager.addContent(directMethodInvokeContent);
            }
        } else if (state.equals(GutterState.PROCESS_NOT_RUNNING)) {
            //show get started only
            if (contentList.contains(directMethodInvokeContent)) {
                manager.removeContent(directMethodInvokeContent, false);
            }
            if (!contentList.contains(atomicTestContent)) {
                manager.addContent(atomicTestContent);
            }
        } else {
            //show both
            if (!contentList.contains(atomicTestContent)) {
                manager.addContent(atomicTestContent, 0);
            }
            if (!contentList.contains(directMethodInvokeContent)) {
                manager.addContent(directMethodInvokeContent);
            }
        }
    }

    public Map<String, String> getIndividualCandidateContextMap() {
        return this.candidateIndividualContextMap;
    }

    public AtomicRecordService getAtomicRecordService() {
        return this.atomicRecordService;
    }

    public void toggleReportGeneration() {
        this.reportingService.toggleReportMode();
    }

    public void initAtomicRecordService() {
        this.atomicRecordService = new AtomicRecordService(this);
    }

    public enum PROJECT_BUILD_SYSTEM {MAVEN, GRADLE, DEF}
}
