package com.insidious.plugin.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.insidious.plugin.Constants;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.*;
import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.inlay.InsidiousInlayHintsCollector;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.GutterClickNavigationStates.ComponentScaffold;
import com.insidious.plugin.ui.*;
import com.insidious.plugin.ui.eventviewer.SingleWindowView;
import com.insidious.plugin.ui.methodscope.MethodDirectInvokeComponent;
import com.insidious.plugin.ui.methodscope.MethodExecutorComponent;
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
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
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
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.*;
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
import com.intellij.ui.GotItTooltip;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.FileContentUtil;
import com.intellij.util.indexing.FileBasedIndex;
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
import java.util.stream.Collectors;

@Storage("insidious.xml")
final public class InsidiousService implements Disposable,
        NewTestCandidateIdentifiedListener, BranchChangeListener, ConnectionStateListener {
    public static final String HOSTNAME = System.getProperty("user.name");
    private static final Key<Long> PSI_MODIFICATION_STAMP = Key.create("psi.modification.stamp");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PACKAGE_NAME = "YOUR.PACKAGE.NAME";
    private final Map<String, Pair<AgentCommandRequest, AgentCommandResponse>> executionPairs = new HashMap<>();
    private final ProjectTypeInfo projectTypeInfo = new ProjectTypeInfo();
    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(5);
    private final AgentClient agentClient = new AgentClient("http://localhost:12100", this);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionLoader sessionLoader;
    private final Map<String, Boolean> executionRecord = new TreeMap<>();
    private final Map<String, Integer> methodHash = new TreeMap<>();
    private final DefaultMethodArgumentValueCache methodArgumentValueCache = new DefaultMethodArgumentValueCache();
    private Project project;
    private VideobugClientInterface client;
    private Module currentModule;
    private String packageName = "YOUR.PACKAGE.NAME";
    private SingleWindowView singleWindowView;
    //    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private ToolWindow toolWindow;
    private String appToken;
    private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH + "=i=YOUR.PACKAGE.NAME\"";
    private TracePoint pendingTrace;
    private TracePoint pendingSelectTrace;
    private LiveViewWindow liveViewWindow;
    private int TOOL_WINDOW_HEIGHT = 430;
    private int TOOL_WINDOW_WIDTH = 600;
    private Content singleWindowContent;
    private boolean rawViewAdded = false;
    private OnboardingConfigurationWindow onboardingConfigurationWindow;
    private Content onboardingConfigurationWindowContent;
    private boolean liveViewAdded = false;
    private Content liveWindowContent;
    private Content onboardingContent;
    private Map<String, ModuleInformation> moduleMap = new TreeMap<>();
    private String selectedModule = null;
    //    private List<ProgramRunner> programRunners = new ArrayList<>();
    private TestCaseDesigner testCaseDesignerWindow;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private boolean initiated = false;
    private Content testDesignerContent;
    private UnloggedGPT gptWindow;
    private Content gptContent;
    private InsidiousInlayHintsCollector inlayHintsCollector;
    private MethodExecutorComponent methodExecutorToolWindow;
    private Content methodExecutorWindow;
    private boolean agentJarExists = false;
    private MethodDirectInvokeComponent methodDirectInvokeComponent;
    private Content manualMethodExecutorWindow;
    private boolean isAgentServerRunning = false;
    private Content componentScaffoldContent;
    private ComponentScaffold componentScaffoldWindow;

    public InsidiousService(Project project) {
        this.project = project;
        logger.info("starting insidious service: " + project);

        String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions";
        FileSystems.getDefault().getPath(pathToSessions).toFile().mkdirs();
        this.client = new VideobugLocalClient(pathToSessions, project);
        this.sessionLoader = new SessionLoader(client, this);
        threadPoolExecutor.submit(this.sessionLoader);
        threadPoolExecutor.submit(() -> {
            while (true) {
//                String path = Constants.VIDEOBUG_AGENT_PATH.toString();
                File agentFile = Constants.VIDEOBUG_AGENT_PATH.toFile();
                if (agentFile.exists()) {
                    logger.warn("Found agent jar at: " + Constants.VIDEOBUG_AGENT_PATH);
                    System.out.println("Found agent jar.");
                    agentJarExists = true;
                    triggerGutterIconReload();
                    promoteState();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        InsidiousCaretListener listener = new InsidiousCaretListener(project);
        multicaster.addEditorMouseListener(listener, this);
//        multicaster.add

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

            this.initiateUI();

        } catch (ServiceNotReadyException snre) {
            snre.printStackTrace();
            logger.info("service not ready exception -> " + snre.getMessage());
        } catch (ProcessCanceledException ignored) {
            ignored.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("exception in videobug service init", e);
        }

    }

    public ProjectTypeInfo getProjectTypeInfo() {
        return projectTypeInfo;
    }

    public void copyToClipboard(String string) {
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        clipboard.setContents(selection, selection);
        System.out.println(selection);
    }

    private List<Module> selectJavaModules(List<Module> modules) {
        return modules.stream()
                .filter(module -> module.getModuleTypeName() == null || module.getModuleTypeName()
                        .equals(
                                "JAVA_MODULE"))
                .collect(Collectors.toList());
    }

    private void registerModules(List<Module> modules) {

        this.moduleMap = new TreeMap<>();
        for (Module module : modules) {
            if (module.getName()
                    .endsWith(".main") || module.getName()
                    .endsWith(".test")) {
                continue;
            }
            ModuleInformation info = new ModuleInformation(module.getName(), module.getModuleTypeName(),
                    module.getModuleFilePath());
            info.setModule(module);
            if (selectedModule == null) {
                selectedModule = info.getName();
            }
            this.moduleMap.put(info.getName(), info);
        }
    }

    public PROJECT_BUILD_SYSTEM findBuildSystemForModule(String modulename) {
        Module module = moduleMap.get(modulename)
                .getModule();
        System.out.println("Fetching build system type");
        System.out.println("MODULE - > " + module.toString());
        System.out.println("MOD MAP - > " + moduleMap.toString());

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

    public List<String> fetchModules() {
        List<Module> modules = Arrays.asList(ModuleManager.getInstance(project)
                .getModules());
        modules = selectJavaModules(modules);
        if (modules.size() > 0) {
            registerModules(modules);
            return new ArrayList<>(this.moduleMap.keySet());
        } else {
            List<String> res = new ArrayList<>();
            if (!this.moduleMap.containsKey(this.project.getName())) {
                ModuleInformation info = new ModuleInformation(this.project.getName(),
                        "JAVA_MODULE", this.project.getBasePath());
                this.moduleMap.put(this.project.getName(), info);
            }
            if (this.selectedModule == null) {
                this.selectedModule = this.project.getName();
            }
            res.add(project.getName());
            return res;
        }
    }

    public boolean isValidJavaModule(String modulename) {
        Collection<VirtualFile> virtualFiles =
                FileBasedIndex.getInstance()
                        .getContainingFiles(FileTypeIndex.NAME, JavaFileType.INSTANCE,
                                GlobalSearchScope.projectScope(project));
        List<String> components = new ArrayList<String>();
        for (VirtualFile vf : virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project)
                    .findFile(vf);
            if (psifile instanceof PsiJavaFile && vf.getPath()
                    .contains(modulename)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getModulesListFromString(String pomSection) {
        Set<String> modules = new HashSet<>();
        String[] parts = pomSection.split("<modules>");
        for (int i = 0; i < parts.length; i++) {
            String[] mps = parts[i].split("\\n");
            for (int j = 0; j < mps.length; j++) {
                if (mps[j].contains("<module>")) {
                    String[] line_segments = mps[j].split("<module>");
                    String module = line_segments[1].split("</module>")[0];
                    modules.add(module);
                }
            }
        }
        System.out.println("Modules - from pom string : " + modules);
        return modules;
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

    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        Pattern p = Pattern.compile(ePattern);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    public String fetchPathToSaveTestCase(TestCaseUnit testCaseScript) {
        StringBuilder sb = new StringBuilder(testCaseScript.getClassName()
                .replaceFirst("Test", ""));
        sb.deleteCharAt(sb.length() - 1);

        @NotNull PsiFile[] classBase = FilenameIndex.getFilesByName(project, sb.toString() + ".java",
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
        String basePath = path.substring(0, last_index);
        return basePath;
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
            @Nullable Document newDocument = FileDocumentManager.getInstance()
                    .getDocument(newFile);

            FileEditorManager.getInstance(project)
                    .openFile(newFile, true, true);

            logger.info("Test case generated in [" + testCaseScript.getClassName() + "]\n" + testCaseScript);
            return newFile;
        }
//        VirtualFileManager.getInstance().syncRefresh();
        return null;
    }

    public void ensureTestUtilClass(String basePath) throws IOException {
        String testOutputDirPath = null;
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

    public InsidiousJavaDebugProcess getDebugProcess() {
        return debugProcess;
    }

    public void setDebugProcess(InsidiousJavaDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    // works for current sessions structure,
    // will need refactor when project/module based logs are stored
    public boolean areLogsPresent() {
        File sessionDir = new File(Constants.VIDEOBUG_SESSIONS_PATH.toString());
        File[] files = sessionDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                File[] files_l2 = file.listFiles();
                for (File file1 : files_l2) {
                    if (file1.getName()
                            .contains(".selog") || file1.getName()
                            .startsWith("index-")) {
                        return true;
                    }
                }
            }
        }
        return false;
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

//
//        onboardingConfigurationWindow = new OnboardingConfigurationWindow(project, this);
//        onboardingConfigurationWindowContent = contentFactory.createContent(
//                onboardingConfigurationWindow.getContent(), "Get Started", false);
//
//        singleWindowView = new SingleWindowView(project, this);
//        singleWindowContent = contentFactory.createContent(singleWindowView.getContent(), "Raw View", false);

        // test case designer form
        testCaseDesignerWindow = new TestCaseDesigner();
        Disposer.register(this, testCaseDesignerWindow);
        @NotNull Content testCaseCreatorWindowContent =
                contentFactory.createContent(testCaseDesignerWindow.getContent(), "TestCase Boilerplate", false);
        this.testDesignerContent = testCaseCreatorWindowContent;
        testCaseCreatorWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        testCaseCreatorWindowContent.setIcon(UIUtils.UNLOGGED_ICON_DARK);
        contentManager.addContent(testCaseCreatorWindowContent);

        // method executor window
        componentScaffoldWindow = new ComponentScaffold(this);
        componentScaffoldContent =
                contentFactory.createContent(componentScaffoldWindow.getContent(), "Atomic Tests", false);
        //this.methodExecutorWindow = methodExecutorWindow;
        componentScaffoldContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        componentScaffoldContent.setIcon(UIUtils.ATOMIC_TESTS);
        contentManager.addContent(componentScaffoldContent);


        methodDirectInvokeComponent = new MethodDirectInvokeComponent(this);
        @NotNull Content manualMethodExecutorWindow =
                contentFactory.createContent(methodDirectInvokeComponent.getContent(), "Execute Method", false);
        this.manualMethodExecutorWindow = manualMethodExecutorWindow;
        manualMethodExecutorWindow.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        manualMethodExecutorWindow.setIcon(UIUtils.EXECUTE_METHOD);
        contentManager.addContent(manualMethodExecutorWindow);


        gptWindow = new UnloggedGPT(this);
        gptContent = contentFactory.createContent(gptWindow.getComponent(), "UnloggedGPT", false);
        gptContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        gptContent.setIcon(UIUtils.UNLOGGED_GPT_ICON_PINK);
        contentManager.addContent(gptContent);

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

    public String getJavaAgentString() {
        return javaAgentString;
    }

    public String getVideoBugAgentPath() {
        return Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath().toString();
    }

    public VideobugClientInterface getClient() {
        return client;
    }

    @Override
    public void dispose() {
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

    public void setCurrentModule(String module) {
        this.selectedModule = module;
    }

    public ModuleInformation getSelectedModuleInstance() {
        return moduleMap.get(selectedModule);
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

    public String fetchVersionFromLibName(String name, String lib) {
        String[] parts = name.split(lib + ":");
        return trimVersion(parts[parts.length - 1].trim());
    }

    public String trimVersion(String version) {
        String[] versionParts = version.split("\\.");
        if (versionParts.length > 2) {
            return versionParts[0] + "." + versionParts[1];
        }
        return version;
    }

    public String suggestAgentVersion() {
        String version = null;
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            if (lib.getName().contains("jackson-databind:")) {
                version = fetchVersionFromLibName(lib.getName(), "jackson-databind");
            }
            count++;
        }
        if (count == 0) {
            //libs not ready
            return getProjectTypeInfo().DEFAULT_PREFERRED_JSON_MAPPER();

        } else {
            if (version == null) {
                return getProjectTypeInfo().DEFAULT_PREFERRED_JSON_MAPPER();
            } else {
                return "jackson-" + version;
            }
        }
    }

    public boolean hasProgramRunning() {
        return false;
//        return programRunners.size() > 0;
    }

    public void methodFocussedHandler(final MethodAdapter method) {

        if (method == null) {
            return;
        }
        final ClassAdapter psiClass = method.getContainingClass();
        if (psiClass.getName() == null) {
            return;
        }

        DumbService dumbService = project.getService(DumbService.class);
        String methodName = method.getName();

        if (dumbService.isDumb()) {
            InsidiousNotification.notifyMessage("Please wait for IDE indexing to finish to start creating tests",
                    NotificationType.WARNING);
            dumbService.runWhenSmart(() -> {
                if (testCaseDesignerWindow == null) {
                    logger.warn("test case designer window is not ready to create test case for " + methodName);
                    return;
                }
                methodFocussedHandler(method);
            });
            return;
        }

        if (this.gptWindow != null) {
            this.gptWindow.updateUI(psiClass, method);
        }

        if (testCaseDesignerWindow == null || !this.toolWindow.isVisible()) {
            logger.warn("test case designer window is not ready to create test case for " + methodName);
            return;
        }

        //methodExecutorToolWindow.refreshAndReloadCandidates(method);
        componentScaffoldWindow.triggerMethodExecutorRefresh(method);
        methodDirectInvokeComponent.renderForMethod(method);
        testCaseDesignerWindow.renderTestDesignerInterface(psiClass, method);
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
                AgentCommandResponse agentCommandResponse = agentClient.executeCommand(agentCommandRequest);
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
            InsidiousNotification.notifyMessage(
                    "Session isn't loaded yet, loading session", NotificationType.WARNING);
            return null;
        }
        return testCaseService;
    }

    public synchronized void setSession(ExecutionSession executionSession) throws SQLException, IOException {
        if (client == null) {
            String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions";
            FileSystems.getDefault().getPath(pathToSessions).toFile().mkdirs();
            this.client = new VideobugLocalClient(pathToSessions, project);
        }

        if (sessionInstance != null) {
            try {
                sessionInstance.close();
            } catch (Exception e) {
                logger.error("Failed to close existing session before opening new session: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        this.executionRecord.clear();
        logger.warn("Loading session: " + executionSession.getSessionId());
        sessionInstance = new SessionInstance(executionSession, project);
        sessionInstance.setTestCandidateListener(this);
        client.setSessionInstance(sessionInstance);
        testCaseService = new TestCaseService(sessionInstance);
    }

    public void openTestCaseDesigner(Project project) {
        if (!this.initiated) {
            if (this.project == null) {
                this.project = project;
            }
            this.init(this.project, ToolWindowManager.getInstance(project).getToolWindow("Unlogged"));
        }
//        toolWindow.getContentManager().setSelectedContent(this.testDesignerContent);
        toolWindow.show(null);

    }

    public void refreshGPTWindow() {
//        if (this.gptContent != null) {
//            this.toolWindow.getContentManager().setSelectedContent(this.gptContent);
//        }
    }

    @Override
    public void onNewTestCandidateIdentified(int completedCount, int totalCount) {
        logger.warn("new test cases identified [" + completedCount + "/" + totalCount + "]");

        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
        Editor[] currentOpenEditorsList = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : currentOpenEditorsList) {
            @Nullable PsiFile psiFile = ApplicationManager.getApplication().runReadAction(
                    (ThrowableComputable<PsiFile, RuntimeException>) () -> PsiDocumentManager.getInstance(project)
                            .getPsiFile(editor.getDocument()));
            if (psiFile == null) {
                continue;
            }
            ApplicationManager.getApplication().runReadAction(
                    () -> DaemonCodeAnalyzer.getInstance(project).restart(psiFile));
        }


        @NotNull ContentManager contentManager = toolWindow.getContentManager();
        @Nullable Content directInvokeContent = contentManager.getContent(2);

        new GotItTooltip("io.unlogged.candidate.new", "New candidates processed", this)
                .withLink("Disable for all files", new Runnable() {
                    @Override
                    public void run() {
                        compile(null, null);
                    }
                })
                .show(directInvokeContent.getComponent(), GotItTooltip.TOP_MIDDLE);

        if (componentScaffoldWindow != null) {
            componentScaffoldWindow.refresh();
        }

    }

//    public Pair<AgentCommandRequest, AgentCommandResponse> getExecutionPairs(String executionPairKey) {
//        return executionPairs.get(executionPairKey);
//    }

    public ClassMethodAggregates getClassMethodAggregates(String qualifiedName) {
        if (sessionInstance == null) {
            loadDefaultSession();
        }
        return sessionInstance.getClassMethodAggregates(qualifiedName);
    }

    public InsidiousInlayHintsCollector getInlayHintsCollector() {
        return inlayHintsCollector;
    }

    public void loadDefaultSession() {
        String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions/na";
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

    public void executeWithAgentForMethod(PsiMethod method, boolean execute) {
        if (this.componentScaffoldWindow != null && this.componentScaffoldContent != null) {
            this.toolWindow.getContentManager().setSelectedContent(this.componentScaffoldContent);
            componentScaffoldWindow.triggerMethodExecutorRefresh(new JavaMethodAdapter(method));
            if(execute)
            {
                componentScaffoldWindow.triggerCompileAndExecute();
            }
        }
    }

    public void openDirectExecuteWindow() {
        if (this.manualMethodExecutorWindow != null && this.methodDirectInvokeComponent != null) {
            this.toolWindow.getContentManager().setSelectedContent(this.manualMethodExecutorWindow);
        }
    }

    public GutterState getGutterStateFor(MethodAdapter method) {
        //check for agent here before other comps
        if (!doesAgentExist()) {
            return GutterState.NO_AGENT;
        }

        // agent exists but cannot connect with agent server
        // so no process is running with the agent
        if (!this.isAgentServerRunning) {
            return GutterState.PROCESS_NOT_RUNNING;
        }

        String methodClassQualifiedName = method.getContainingClass().getQualifiedName();
        String methodName = method.getName();
        List<TestCandidateMetadata> candidates = sessionInstance.getTestCandidatesForAllMethod(
                methodClassQualifiedName, methodName, false);
        // process is running, but no test candidates for this method
        if (candidates.size() == 0) {
            return GutterState.PROCESS_RUNNING;
        }

        // process is running, and there were test candidates for this method
        // so check if we have executed this before

        //check for change
        String hashKey = methodClassQualifiedName + "." + methodName;

        // we havent checked anything for this method earlier
        // store method hash for diffs
        if (!this.methodHash.containsKey(hashKey)) {
            //register new hash
            this.methodHash.put(hashKey, method.getText().hashCode());
        }

        int lastHash = this.methodHash.get(hashKey);
        int currentHash = method.getText().hashCode();

        if (lastHash != currentHash) {
            //re-execute as there are hash diffs
            //update hash after execution is complete for this method,
            //to prevent state change before exec complete.
            return GutterState.EXECUTE;
        }

        if (!executionRecord.containsKey(methodName)) {
            return GutterState.DATA_AVAILABLE;
        }

        return executionRecord.get(methodName) ? GutterState.DIFF : GutterState.NO_DIFF;
    }

    public void updateMethodHashForExecutedMethod(PsiMethod method) {
        if (this.executionRecord.containsKey(method.getName())) {
            Application application = ApplicationManager.getApplication();
            PsiClass containingClass = application.runReadAction((Computable<PsiClass>) method::getContainingClass);
            String qualifiedName = application.runReadAction((Computable<String>) containingClass::getQualifiedName);

            String classMethodHashKey = qualifiedName + "." + method.getName();
            String methodBody = application.runReadAction((Computable<String>) method::getText);
            int methodBodyHashCode = methodBody.hashCode();
            this.methodHash.put(classMethodHashKey, methodBodyHashCode);
            DaemonCodeAnalyzer.getInstance(project).restart(method.getContainingFile());
        } else {
            //don't update hash
            //failed execution
        }
    }

    public Map<String, Boolean> getExecutionRecord() {
        return this.executionRecord;
    }

    public boolean doesAgentExist() {
        return agentJarExists;
    }

    @Override
    public void branchWillChange(@NotNull String branchName) {

    }

    @Override
    public void branchHasChanged(@NotNull String branchName) {
        logger.warn("branch has changed: " + branchName);
    }

    @Override
    public void onConnectedToAgentServer() {
        logger.warn("connected to agent");
        // connected to agent
        this.isAgentServerRunning = true;
        triggerGutterIconReload();
        promoteState();

        InsidiousNotification.notifyMessage("Connected to process", NotificationType.INFORMATION);
        focusDirectInvokeTab();
    }

    @Override
    public void onDisconnectedFromAgentServer() {
        logger.warn("disconnected from agent");
        // disconnected from agent
        this.isAgentServerRunning = false;
        triggerGutterIconReload();
        demoteState();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void triggerGutterIconReload() {

        DumbService instance = DumbService.getInstance(project);
        if (instance.isDumb()) {
            instance.runWhenSmart(this::triggerGutterIconReload);
            return;
        }

        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    public void updateScaffoldForState(GutterState state) {
        if (this.componentScaffoldWindow != null) {
            componentScaffoldWindow.loadComponentForState(state);
            if (this.componentScaffoldContent != null) {
                this.toolWindow.getContentManager().setSelectedContent(this.componentScaffoldContent);
            }
        }
    }

    public void focusDirectInvokeTab() {
        toolWindow.getContentManager().setSelectedContent(manualMethodExecutorWindow, false);
    }

    public enum PROJECT_BUILD_SYSTEM {MAVEN, GRADLE, DEF}

    public String fetchBasePackage() {
        Set<String> ret = new HashSet<String>();
        Collection<VirtualFile> virtualFiles =
                FileBasedIndex.getInstance()
                        .getContainingFiles(FileTypeIndex.NAME, JavaFileType.INSTANCE,
                                GlobalSearchScope.projectScope(project));
        List<String> components = new ArrayList<String>();
        for (VirtualFile vf : virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project)
                    .findFile(vf);
            if (psifile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if (packageName.contains(".")) {
                    ret.add(packageName);
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
        String basePackage = buildPackageNameFromList(components);
        return basePackage;
    }

    public String fetchBasePackageForModule(String modulename) {
        Set<String> ret = new HashSet<>();
        Collection<VirtualFile> virtualFiles =
                FileBasedIndex.getInstance()
                        .getContainingFiles(FileTypeIndex.NAME, JavaFileType.INSTANCE,
                                GlobalSearchScope.projectScope(project));
        List<String> components = new ArrayList<>();
        for (VirtualFile vf : virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project)
                    .findFile(vf);
            if (psifile instanceof PsiJavaFile && vf.getPath()
                    .contains(modulename)) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if (packageName.contains(".") && !packageName.equals("io.unlogged")) {
                    ret.add(packageName);
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
                else
                {
                    //generic package name
                    ret.add(packageName);
                }
            }
        }
        String basePackage = buildPackageNameFromList(components);
        if (basePackage.equals("?")) {
            return fetchBasePackage();
        }
        return basePackage;
    }

    public String fetchPackagePathForModule(String modulename) {
        String source = fetchBasePackageForModule(modulename);
        return source.replaceAll("\\.", "/");
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

    public void promoteState()
    {
        if(this.componentScaffoldWindow!=null)
        {
            GutterState state = this.componentScaffoldWindow.getCurrentState();
            if(state.equals(GutterState.NO_AGENT))
            {
                System.out.println("Promoting to PROCESS_NOT_RUNNING");
                componentScaffoldWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
            }
            if(state.equals(GutterState.PROCESS_NOT_RUNNING))
            {
                System.out.println("Promoting to PROCESS_RUNNING");
                componentScaffoldWindow.loadComponentForState(GutterState.PROCESS_RUNNING);
            }
        }
    }

    public void demoteState()
    {
        if(this.componentScaffoldWindow!=null)
        {
            GutterState state = this.componentScaffoldWindow.getCurrentState();
            if(state.equals(GutterState.PROCESS_RUNNING))
            {
                System.out.println("Demoting to PROCESS_NOT_RUNNING");
                componentScaffoldWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
            }
//            if(state.equals(GutterState.PROCESS_NOT_RUNNING))
//            {
//                System.out.println("Demoting to NO_AGENT");
//                componentScaffoldWindow.loadComponentForState(GutterState.NO_AGENT);
//            }
        }
    }
}
