package com.insidious.plugin.factory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.insidious.plugin.Constants;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.Components.TestCaseDesigner;
import com.insidious.plugin.ui.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.FileContentUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.xdebugger.XDebugSession;
import com.squareup.javapoet.TypeSpec;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Storage("insidious.xml")
final public class InsidiousService implements Disposable {
    public static final String HOSTNAME = System.getProperty("user.name");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
            .create();
    private static final String DEFAULT_PACKAGE_NAME = "YOUR.PACKAGE.NAME";
    private final ProjectTypeInfo projectTypeInfo = new ProjectTypeInfo();
    private Project project;
    private VideobugClientInterface client;
    private Module currentModule;
    private String packageName = "YOUR.PACKAGE.NAME";
    private SingleWindowView singleWindowView;
    private XDebugSession debugSession;
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

    private List<ProgramRunner> programRunners = new ArrayList<>();
    private TestCaseDesigner testCaseDesignerWindow;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private boolean initiated = false;
    private Content testDesignerContent;

    public InsidiousService(Project project) {
        this.project = project;
        logger.info("starting insidious service");

        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        InsidiousCaretListener listener = new InsidiousCaretListener(project);
        multicaster.addEditorMouseListener(listener, this);

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

            String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions";
            FileSystems.getDefault().getPath(pathToSessions).toFile().mkdirs();
            this.client = new VideobugLocalClient(pathToSessions, project);

            this.loadSession();
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
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
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
                        .getPath(testResourcesFilePath)
                        .toUri()
                        .toString());

                if (testCaseScript.getTestGenerationState()
                        .isSetupNeedsJsonResources()) {

                    String setupJsonFilePath = testResourcesDirPath + "/" + "setup" + ".json";
                    try (FileOutputStream resourceFile = new FileOutputStream(setupJsonFilePath)) {
                        resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                    }
                    VirtualFileManager.getInstance()
                            .refreshAndFindFileByUrl(FileSystems.getDefault()
                                    .getPath(setupJsonFilePath)
                                    .toUri()
                                    .toString());
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
                                    + e.getMessage(), NotificationType.ERROR
                    );
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
                CompilationUnit existingCompilationUnit = parsedFile.getResult()
                        .get();
                ClassOrInterfaceDeclaration classDeclaration = existingCompilationUnit.getClassByName(
                                testCaseScript.getClassName())
                        .get();

                TypeSpec newTestSpec = testCaseScript.getTestClassSpec();

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
                CompilationUnit newCompilationUnit = parseResult
                        .getResult()
                        .get();

                MethodDeclaration newMethodDeclaration =
                        newCompilationUnit.getClassByName(testCaseScript.getClassName())
                                .get()
                                .getMethodsByName(newTestSpec.methodSpecs.get(1).name)
                                .get(0);

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
        @NotNull Content testCaseCreatorWindowContent =
                contentFactory.createContent(testCaseDesignerWindow.getContent(), "Test designer", false);
        this.testDesignerContent = testCaseCreatorWindowContent;
        testCaseCreatorWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        testCaseCreatorWindowContent.setIcon(UIUtils.UNLOGGED_ICON_DARK);
        contentManager.addContent(testCaseCreatorWindowContent);

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
        if (this.client != null) {
            this.client.close();
            this.client = null;
            currentModule = null;
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

    public void generateAllTestCandidateCases() throws Exception {
        SessionInstance sessionInstance = getClient().getSessionInstance();
        TestCaseService testCaseService = new TestCaseService(sessionInstance);

        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.GSON, ResourceEmbedMode.IN_FILE
        );

        sessionInstance.getAllTestCandidates(testCandidateMetadata -> {
            @NotNull TestCaseUnit testCaseUnit = null;
            try {
                Parameter testSubject = testCandidateMetadata.getTestSubject();
                if (testSubject.isException()) {
                    return;
                }
                MethodCallExpression callExpression = (MethodCallExpression) testCandidateMetadata.getMainMethod();
                logger.warn(
                        "Generating test case: " + testSubject.getType() + "." + callExpression.getMethodName() + "()");
                generationConfiguration.getTestCandidateMetadataList()
                        .clear();
                generationConfiguration.getTestCandidateMetadataList()
                        .add(testCandidateMetadata);

                generationConfiguration.getCallExpressionList()
                        .clear();
                generationConfiguration.getCallExpressionList()
                        .addAll(testCandidateMetadata.getCallsList());

                testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
                List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
                testCaseUnit1.add(testCaseUnit);
                TestSuite testSuite = new TestSuite(testCaseUnit1);
                saveTestSuite(testSuite);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }

    public void addLiveView() {
        UsageInsightTracker.getInstance()
                .RecordEvent("ProceedingToLiveView", null);
        if (!liveViewAdded) {
            toolWindow.getContentManager()
                    .addContent(liveWindowContent);
            toolWindow.getContentManager()
                    .setSelectedContent(liveWindowContent, true);
            liveViewAdded = true;
            try {
                liveViewWindow.setTreeStateToLoading();
                liveViewWindow.loadInfoBanner();
                liveViewWindow.loadSession();
            } catch (Exception e) {
                //exception setting state
                logger.error("Failed to start scan after proceed.");
            }
        } else {
            toolWindow.getContentManager()
                    .setSelectedContent(liveWindowContent);
        }
    }


    public void setCurrentModule(String module) {
        this.selectedModule = module;
    }

    public String getSelectedModuleName() {
        return selectedModule;
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
        String[] parts = name
                .split(lib + ":");
        String version = trimVersion(parts[parts.length - 1].trim());
        return version;
    }

    public String trimVersion(String version) {
        String versionParts[] = version.split("\\.");
        if (versionParts.length > 2) {
            return versionParts[0] + "." + versionParts[1];
        }
        return version;
    }


    public String suggestAgentVersion() {
        String version = null;
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            if (lib.getName()
                    .contains("jackson-databind:")) {
                version = fetchVersionFromLibName(lib.getName(), "jackson-databind");
            }
            count++;
        }
        if (count == 0) {
            //libs not ready
            return getProjectTypeInfo()
                    .DEFAULT_PREFERRED_JSON_MAPPER();

        } else {
            if (version == null) {
                return getProjectTypeInfo()
                        .DEFAULT_PREFERRED_JSON_MAPPER();
            } else {
                return "jackson-" + version;
            }
        }
    }

    public boolean hasProgramRunning() {
        return programRunners.size() > 0;
    }

    public void showTestCreatorInterface(PsiClass psiClass, PsiMethod method) {

        if (method == null) {
            return;
        }
        if (psiClass.getName() == null) {
            return;
        }
        JSONObject eventProperties = new JSONObject();
        if (testCaseDesignerWindow == null) {
//            UsageInsightTracker.getInstance().RecordEvent("ToolWindowNull", eventProperties);
            logger.warn("test case designer window is not ready to create test case for " + method.getName());
            return;
        }
        eventProperties.put("method_name", method);
        eventProperties.put("class_name", psiClass.getName());
        UsageInsightTracker.getInstance().RecordEvent("GenerateTestCaseRequest", eventProperties);

        DumbService dumbService = project.getService(DumbService.class);
        if (dumbService.isDumb()) {
            InsidiousNotification.notifyMessage("Please wait for IDE indexing to finish to start creating tests",
                    NotificationType.WARNING);
            dumbService.runWhenSmart(new Runnable() {
                @Override
                public void run() {
                    if (testCaseDesignerWindow == null) {
                        logger.warn(
                                "test case designer window is not ready to create test case for " + method.getName());
                        return;
                    }
                    testCaseDesignerWindow.renderTestDesignerInterface(psiClass, method);
                }
            });
            return;
        }


        testCaseDesignerWindow.renderTestDesignerInterface(psiClass, method);
    }

    public boolean areModulesRegistered() {
        return this.moduleMap.size() > 0 ? true : false;
    }

    public TestCaseService getTestCaseService() {
        if (testCaseService == null) {
            loadSession();
//            InsidiousNotification.notifyMessage("Session isn't loaded yet, loading session", NotificationType.WARNING);
//            return null;
        }
        return testCaseService;
    }

    public synchronized void loadSession() {
        try {
            String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions/na";
            ExecutionSession executionSession = new ExecutionSession();
            executionSession.setPath(pathToSessions);
            executionSession.setSessionId("na");
            sessionInstance = new SessionInstance(executionSession, project);
            client.setSessionInstance(sessionInstance);
            testCaseService = new TestCaseService(sessionInstance);

        } catch (SQLException | IOException e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("stack", e.toString());
            UsageInsightTracker.getInstance().RecordEvent("SESSION_LOAD_ERROR", eventProperties);
            throw new RuntimeException(e);
        }


//        Task.Backgroundable task =
//                new Task.Backgroundable(project, "Unlogged, Inc.", true) {
//                    @Override
//                    public void run(@NotNull ProgressIndicator indicator) {
//                        client.getProjectSessions(new GetProjectSessionsCallback() {
//                            @Override
//                            public void error(String message) {
//                                JSONObject eventProperties = new JSONObject();
//                                eventProperties.put("message", message);
//                                UsageInsightTracker.getInstance().RecordEvent("SESSION_LOAD_ERROR", eventProperties);
//                                String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions/na";
//                                ExecutionSession executionSession = new ExecutionSession();
//                                executionSession.setPath(pathToSessions);
//                                executionSession.setSessionId("na");
//                                try {
//                                    sessionInstance = new SessionInstance(executionSession, project);
//                                } catch (SQLException | IOException e) {
//                                    eventProperties.put("stack", e.toString());
//                                    UsageInsightTracker.getInstance()
//                                            .RecordEvent("SESSION_LOAD_ERROR", eventProperties);
//                                    throw new RuntimeException(e);
//                                }
//                            }
//
//                            @Override
//                            public void success(List<ExecutionSession> executionSessionList) {
//                                try {
//                                    if (executionSessionList.size() == 0) {
//                                        String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions/na";
//                                        ExecutionSession executionSession = new ExecutionSession();
//                                        executionSession.setPath(pathToSessions);
//                                        executionSession.setSessionId("na");
//                                        sessionInstance = new SessionInstance(executionSession, project);
//                                    } else {
//                                        ExecutionSession executionSession = executionSessionList.get(0);
//                                        sessionInstance = new SessionInstance(executionSession, project);
//                                    }
//                                    client.setSessionInstance(sessionInstance);
//                                    testCaseService = new TestCaseService(sessionInstance);
//
//                                } catch (SQLException | IOException e) {
//                                    JSONObject eventProperties = new JSONObject();
//                                    eventProperties.put("stack", e.toString());
//                                    UsageInsightTracker.getInstance()
//                                            .RecordEvent("SESSION_LOAD_ERROR", eventProperties);
//                                    throw new RuntimeException(e);
//                                }
//
//                            }
//                        });
//                    }
//                };
//
//        ProgressManager.getInstance().run(task);

    }

    public void openTestCaseDesigner(Project project) {
        if (!this.initiated) {
            if (this.project == null) {
                this.project = project;
            }
            this.init(this.project, ToolWindowManager.getInstance(project).getToolWindow("Unlogged"));
        }
        System.out.println("[Init UI call]");
        toolWindow.getContentManager().setSelectedContent(this.testDesignerContent);
        toolWindow.show(null);

    }

    public enum PROJECT_BUILD_SYSTEM {MAVEN, GRADLE, DEF}

}
