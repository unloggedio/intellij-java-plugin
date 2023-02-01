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
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.*;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.visitor.GradleFileVisitor;
import com.insidious.plugin.visitor.PomFileVisitor;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
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
    private final String DEFAULT_PACKAGE_NAME = "YOUR.PACKAGE.NAME";
    private final ProjectTypeInfo projectTypeInfo = new ProjectTypeInfo();
    private Project project;
    //    private InsidiousConfigurationState insidiousConfiguration;
    private VideobugClientInterface client;
    private Module currentModule;
    private String packageName = "YOUR.PACKAGE.NAME";
    private SingleWindowView singleWindowView;
    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private InsidiousJDIConnector connector;
    private ConfigurationWindow credentialsToolbarWindow;
    private ToolWindow toolWindow;
    private CredentialAttributes insidiousCredentials;
    private String appToken;
    private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH + "=i=YOUR.PACKAGE.NAME\"";
    private TracePoint pendingTrace;
    private TracePoint pendingSelectTrace;
    private AboutUsWindow aboutUsWindow;
    private LiveViewWindow liveViewWindow;
    private int TOOL_WINDOW_HEIGHT = 430;
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

    public InsidiousService() {
        logger.info("starting insidious service");

//        Module module = ModuleManager.getInstance(project)
//                .getModules()[0];

//        @NotNull LibraryTable libraryTable = ModuleRootManager.getInstance(module)
//                .getModifiableModel()
//                .getModuleLibraryTable();
//        @NotNull Library library = libraryTable.createLibrary("library ?");
//        library.getModifiableModel()
//        ModuleRootModificationUtil.addDependency(module, library);
//        start();
    }

    private void start() {
        try {

            logger.info("started insidious service - project name - " + project.getName());
            if (ModuleManager.getInstance(project)
                    .getModules().length == 0) {
                logger.warn("no module found in the project");
            } else {
                currentModule = ModuleManager.getInstance(project)
                        .getModules()[0];
                logger.info("current module - " + currentModule.getName());
            }

            String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions";
            FileSystems.getDefault()
                    .getPath(pathToSessions)
                    .toFile()
                    .mkdirs();
            this.client = new VideobugLocalClient(pathToSessions, project);


            DumbService dumbService = DumbService.getInstance(project);
            if (dumbService.isDumb()) {
                InsidiousNotification.notifyMessage("Unlogged is waiting for the indexing to complete.",
                        NotificationType.INFORMATION);
                dumbService.runWhenSmart(this::initiateUI);
            } else {
                this.initiateUI();
            }


            ApplicationManager.getApplication()
                    .runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            getProjectPackageName();
                            checkAndEnsureJavaAgentCache();
                        }
                    });


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

    private XDebugSession getActiveDebugSession(XDebugSession[] debugSessions) {
        for (XDebugSession session : debugSessions) {
            if (session.getDebugProcess() instanceof InsidiousJavaDebugProcess) {
                return session;
            }
        }
        return null;
    }

//    private void setupNewExceptionListener() {
//        Set<String> exceptionClassList = insidiousConfiguration.exceptionClassMap
//                .entrySet()
//                .stream()
//                .filter(Map.Entry::getValue)
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toSet());
////        this.client.onNewException(exceptionClassList, new VideobugExceptionCallback() {
////            final Set<TracePoint> mostRecentTracePoints = new HashSet<>();
////
////            @Override
////            public void onNewTracePoints(Collection<TracePoint> tracePoints) {
////                if (mostRecentTracePoints.size() > 0) {
////                    List<TracePoint> newTracePoints = new LinkedList<>();
////                    for (TracePoint tracePoint : tracePoints) {
////                        if (!mostRecentTracePoints.contains(tracePoint)) {
////                            newTracePoints.add(tracePoint);
////                        }
////                    }
////                    mostRecentTracePoints.clear();
////                    mostRecentTracePoints.addAll(tracePoints);
////                    if (newTracePoints.size() == 0) {
////                        return;
////                    }
////                    tracePoints = newTracePoints;
////                } else {
////                    mostRecentTracePoints.addAll(tracePoints);
////                }
////
////                String messageContent = "Got " + tracePoints.size() + " new matching trace points";
////                StringBuilder messageBuilder = new StringBuilder();
////                Map<String, List<TracePoint>> pointsByClass = tracePoints.stream().collect(Collectors.groupingBy(TracePoint::getClassname));
////                for (Map.Entry<String, List<TracePoint>> classTracePoint : pointsByClass.entrySet()) {
////                    String className = classTracePoint.getKey();
////                    List<TracePoint> classTracePoints = classTracePoint.getValue();
////                    Map<String, List<TracePoint>> tracePointsByException = classTracePoints.stream().collect(Collectors.groupingBy(TracePoint::getExceptionClass));
////
////                    for (Map.Entry<String, List<TracePoint>> exceptionTracePoint : tracePointsByException.entrySet()) {
////                        String exceptionClassName = exceptionTracePoint.getKey();
////                        Map<Long, List<TracePoint>> pointsByLine = exceptionTracePoint.getValue().stream().collect(Collectors.groupingBy(TracePoint::getLineNumber));
////
////                        for (Map.Entry<Long, List<TracePoint>> lineExceptionPoints : pointsByLine.entrySet()) {
////
////                            String[] exceptionClassNameParts = exceptionClassName.split("\\.");
////                            String[] classNameParts = className.split("/");
////                            messageBuilder.append(lineExceptionPoints.getValue().size());
////                            messageBuilder.append(" ");
////                            messageBuilder.append(exceptionClassNameParts[exceptionClassNameParts.length - 1]);
////                            messageBuilder.append(" on line ").append(lineExceptionPoints.getKey());
////                            messageBuilder.append(" in ").append("<a>").append(classNameParts[classNameParts.length - 1]).append("</a>");
////                            messageBuilder.append("<br>");
////                        }
////
////
////                    }
////
////
////                }
////
////
//////                    Notification notification = InsidiousNotification.
//////                            balloonNotificationGroup.createNotification("New exception",
//////                                    "These just happened",
//////                                    messageBuilder.toString(), NotificationType.INFORMATION);
//////                    Notifications.Bus.notify(notification);
//////                    getHorBugTable().setTracePoints(tracePoints);
////            }
////        });
//    }

    public void checkAndEnsureJavaAgentCache() {
        checkAndEnsureJavaAgent(false, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {

            }

            @Override
            public void success(String url, String path) {
                InsidiousNotification.notifyMessage("Agent jar download complete", NotificationType.INFORMATION);
            }
        });
    }

    public void checkAndEnsureJavaAgent(boolean overwrite, AgentJarDownloadCompleteCallback agentJarDownloadCompleteCallback) {

        //returning from this to prevent downloading agent 1.8.1
        if (true) {
            return;
        }
        File insidiousFolder = new File(Constants.VIDEOBUG_HOME_PATH.toString());
        if (!insidiousFolder.exists()) {
            insidiousFolder.mkdir();
        }

        if (overwrite) {
            Constants.VIDEOBUG_AGENT_PATH.toFile()
                    .delete();
        }

        if (!Constants.VIDEOBUG_AGENT_PATH.toFile()
                .exists() && !overwrite) {
            InsidiousNotification.notifyMessage(
                    "Downloading Unlogged java agent jar to $HOME/.videobug/videobug-java-agent.jar",
                    NotificationType.INFORMATION);
        }

        if (Constants.VIDEOBUG_AGENT_PATH.toFile()
                .exists()) {
            return;
        }


        client.getAgentDownloadUrl(new AgentDownloadUrlCallback() {
            @Override
            public void error(String error) {
                logger.error("failed to get url from server to download the java agent - " + error);
                agentJarDownloadCompleteCallback.error(
                        "failed to get url from server to download the java agent - " + error);
            }

            @Override
            public void success(String url) {
                try {
                    logger.info(
                            "agent download link: " + url + ", downloading to path " + Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath());
                    client.downloadAgentFromUrl(url, Constants.VIDEOBUG_AGENT_PATH.toString(), overwrite);
                    setAppTokenOnUi();
                    agentJarDownloadCompleteCallback.success(url, Constants.VIDEOBUG_AGENT_PATH.toString());
                } catch (Exception e) {
                    logger.info("failed to download agent - ", e);
                }
            }
        });

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

    private void registerModules_Manual(List<String> modules) {

        this.moduleMap = new TreeMap<>();
        for (String module : modules) {
            if (this.selectedModule == null) {
                this.selectedModule = module;
            }
            ModuleInformation info = new ModuleInformation();
            info.setName(module);
            if (!this.moduleMap.containsKey(module)) {
                this.moduleMap.put(info.getName(), info);
            }
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
//            try {
//                logger.info("Fetching from POM.xml/settings.gradle");
//                Set<String> modules_from_pg = fetchModuleNamesFromFiles();
//                if (modules_from_pg == null) {
//                    logger.warn("No modules found");
//                } else {
//                    registerModules_Manual(new ArrayList<>(modules_from_pg));
//                    return new ArrayList<>(modules_from_pg);
//                }
//            } catch (Exception e) {
//                logger.error("Exception fetching modules " + e);
//                e.printStackTrace();
//            }
//        }
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

    private Set<String> filterModules(Set<String> modules) {
        Set<String> final_Modules = new TreeSet<>();
        for (String module : modules) {
            if (!(module.endsWith(".main") || module.endsWith(".test"))) {
                String[] parts = module.split("\\.");
                String module_last = parts[parts.length - 1];
                if (isValidJavaModule(module_last)) {
                    final_Modules.add(module_last);
                }
            }
        }
        return final_Modules;
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

    //to be simplified and cleaned up
    public Set<String> fetchModuleNamesFromFiles() {
        if (!project.isInitialized()) {
            return null;
        }
        if (currentModule == null) {
            return null;
        }
        //fetch module names from all pom files in the project, also fetch base packages (not always useful from pom)
        @NotNull PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml",
                GlobalSearchScope.projectScope(project));
        Set<String> modules = new HashSet<String>();
        if (pomFileSearchResult.length > 0) {
            projectTypeInfo.setMaven(true);
            for (int x = 0; x < pomFileSearchResult.length; x++) {
                @NotNull XmlFile pomPsiFile = (XmlFile) pomFileSearchResult[x];
                String text = pomPsiFile.getText();
                if (text.contains("<modules>")) {
                    int modulesIndexStart = text.indexOf("<modules>");
                    int modulesIndexEnd = text.indexOf("</modules>");

                    String substring_modules = text.substring(modulesIndexStart, modulesIndexEnd);
                    //System.out.println("Modules Section - ");
                    //System.out.println(substring_modules);
                    Set<String> modulesFromPom = getModulesListFromString(substring_modules);
                    return modulesFromPom;
                } else if (text.contains("<java.version>")) {
                    String java_version = text.substring(text.indexOf("<java.version>") + 1,
                            text.indexOf("</java.version>"));
                    //System.out.println("Java version");
                    //System.out.println("" + java_version);
                }
            }
            return modules;
        }
        //System.out.println("[Modules] Pom -");
        //System.out.println(modules.toString());

        try {
            modules = new HashSet<String>();
            @NotNull PsiFile[] gradleFileSearchResult = FilenameIndex.getFilesByName(project, "settings.gradle",
                    GlobalSearchScope.projectScope(project));
            if (gradleFileSearchResult.length > 0) {
                logger.info("found setting.gradle file");
                for (int x = 0; x < gradleFileSearchResult.length; x++) {
                    PsiFile settingsGradle = gradleFileSearchResult[x];
                    String text = settingsGradle.getText();
                    //System.out.println("Gradle settings text");
                    //System.out.println("" + text);

                    String[] lines = text.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        if (line.contains("rootProject.name")) {
                            int start = line.indexOf("'") + 1;
                            int end = line.lastIndexOf("'");
                            String moduleName = line.substring(start, end);
                            //System.out.println("Module name root : " + moduleName);
                            modules.add(moduleName);
                        }
                        if (line.startsWith("include") && line.contains("(")) {
                            String modulesSection = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
                            System.out.println("(Include) " + modulesSection);
                            String[] temp = modulesSection.replaceAll("'", "")
                                    .split(",");
                            for (int c = 0; c < temp.length; c++) {
                                //System.out.println("Gradle module found :" + temp[c]);
                                modules.add(temp[c].trim());
                            }
                        } else if (line.startsWith("include")) {
                            String[] parts = line.split("\'");
                            //System.out.println("Parts "+Arrays.asList(parts));
                            //System.out.println("Module name [i] s " + parts[1]);
                            modules.add(parts[1].trim());
                        }
                    }
                }
                //System.out.println("[Modules] Gradle - ");
                //System.out.println(modules.toString());
                return modules;
            }
        } catch (Exception ex) {
            logger.info("Exception fetching gradle modules " + ex);
            ex.printStackTrace();
        }
        return null;
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

    private void getProjectPackageName() {
        if (!project.isInitialized()) {
            return;
        }
        if (currentModule == null) {
            return;
        }
        logger.info("looking up package name for the module [" + currentModule.getName() + "]");
        @NotNull PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml",
                GlobalSearchScope.projectScope(project));
        if (pomFileSearchResult.length > 0) {
            @NotNull XmlFile pomPsiFile = (XmlFile) pomFileSearchResult[0];
            logger.info("found pom.xml file at");

            PomFileVisitor visitor = new PomFileVisitor();
            pomPsiFile.accept(visitor);
            if (visitor.getPackageName() != null) {
                packageName = visitor.getPackageName();
                setAppTokenOnUi();
                return;
            }
        }

        @NotNull PsiFile[] gradleFileSearchResult = FilenameIndex.getFilesByName(project, "build.gradle",
                GlobalSearchScope.projectScope(project));
        if (gradleFileSearchResult.length > 0) {
            logger.info("found build.gradle file at");
            @NotNull PsiFile gradlePsiFile = gradleFileSearchResult[0];
            GradleFileVisitor visitor = new GradleFileVisitor();
            gradlePsiFile.accept(visitor);
            if (visitor.getPackageName() != null) {
                packageName = visitor.getPackageName();
                setAppTokenOnUi();
            }
        }
        packageName = "org/company/package";
        setAppTokenOnUi();
    }

    public void init(@NotNull Project project, @NotNull ToolWindow toolWindow) {
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

    private void setupProject() {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project)
                    .getModules()[0];
        }

        String currentModuleName = currentModule.getName();
        try {
            logger.info("try to set project to - " + currentModuleName);
            client.setProject(currentModuleName);
            generateAppToken();
        } catch (ProjectDoesNotExistException e1) {
            createProject(currentModuleName, new NewProjectCallback() {
                @Override
                public void error(String errorMessage) {

                    logger.error("failed to create project - {}", errorMessage);

                    InsidiousNotification.notifyMessage(
                            "Failed to create new project for [" + currentModuleName + "] on server ["
                                    + InsidiousConfigurationState.getInstance()
                                    .getServerUrl(),
                            NotificationType.ERROR);

                }

                @Override
                public void success(String projectId) {
                    logger.info("created new project for " + currentModuleName + " -> " + projectId);
                    ApplicationManager.getApplication()
                            .invokeLater(() -> setupProject());
                }
            });

        } catch (UnauthorizedException | IOException e) {
            logger.error("failed to setup project", e);
            InsidiousNotification.notifyMessage(e.getMessage(), NotificationType.ERROR);

        }
    }

    private void generateAppToken() {
        getProjectToken(new ProjectTokenCallback() {
            @Override
            public void error(String message) {
                InsidiousNotification.notifyMessage(
                        "Failed to generate app token for module [" + currentModule.getName() + "]",
                        NotificationType.ERROR);

                credentialsToolbarWindow.setErrorLabel(message);
            }

            @Override
            public void success(String token) {
                InsidiousService.this.appToken = token;
                setAppTokenOnUi();
            }
        });
    }

    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        this.client.createProject(projectName, newProjectCallback);
    }

    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        this.client.getProjectToken(projectTokenCallback);
    }

//    public void generateTestCases(ObjectWithTypeInfo object) throws Exception {
//
//        TestSuite testSuite = ProgressManager.getInstance().run(new Task.WithResult<TestSuite, Exception>(project,
//                "Videobug", true) {
//            @Override
//            protected TestSuite compute(@NotNull ProgressIndicator indicator) throws Exception {
//                TestCaseRequest testCaseRequest = new TestCaseRequest(
//                        List.of(object), List.of(
//                        "com.fasterxml",
//                        "com.google"
//                ), Set.of());
//
//                TestSuite testSuite = null;
//                try {
//                    testSuite = testCaseService.generateTestCase(testCaseRequest);
//                } catch (SessionNotSelectedException e) {
//                    InsidiousNotification.notifyMessage(
//                            "Failed to generate test suite: " + e.getMessage(), NotificationType.ERROR
//                    );
//                }
//                return testSuite;
//            }
//        });
//        if (testSuite == null) {
//            return;
//        }
//        logger.warn("testsuite: \n" + testSuite.toString());
//
//        @Nullable VirtualFile newFile = saveTestSuite(testSuite);
//        if (newFile == null) {
//            logger.warn("Test case generated for [" + object + "] but failed to write");
//            InsidiousNotification.notifyMessage("Failed to write test case to file", NotificationType.ERROR);
//        } else {
//            InsidiousNotification.notifyMessage(
//                    "Test case saved at [" + newFile.getCanonicalPath() + "]", NotificationType.INFORMATION
//            );
//        }
//
//    }

//    public void generateTestCases(List<String> targetClasses) throws Exception {
//
//        TestSuite testSuite = ProgressManager.getInstance().run(new Task.WithResult<TestSuite, Exception>(project,
//                "Videobug", true) {
//            @Override
//            protected TestSuite compute(@NotNull ProgressIndicator indicator) throws Exception {
//
//
//                List<TestCandidate> testCandidateList = new LinkedList<>();
//                BlockingQueue<String> waiter = new ArrayBlockingQueue<>(1);
//
//
////        List<String> targetClasses = List.of("org.zerhusen.service.Adder");
//
//                SearchQuery searchQuery = SearchQuery.ByType(targetClasses);
//
//                List<ObjectWithTypeInfo> allObjects = new LinkedList<>();
//
//                DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
//                ExecutionSession session = sessions.getItems().get(0);
//
//                client.getObjectsByType(
//                        searchQuery, session.getSessionId(), new ClientCallBack<>() {
//                            @Override
//                            public void error(ExceptionResponse errorResponse) {
//
//                            }
//
//                            @Override
//                            public void success(Collection<ObjectWithTypeInfo> tracePoints) {
//                                allObjects.addAll(tracePoints);
//                            }
//
//                            @Override
//                            public void completed() {
//                                waiter.offer("done");
//                            }
//                        }
//                );
//                waiter.take();
//
//                if (allObjects.size() == 0) {
//                    InsidiousNotification.notifyMessage("Could not find any instances of [" + targetClasses + "]",
//                            NotificationType.WARNING);
//                }
//
//                TestCaseRequest testRequest = new TestCaseRequest(
//                        allObjects, List.of(
//                        "com.fasterxml",
//                        "com.google"
//                ), Set.of()
//                );
//                TestSuite testSuite = null;
//                try {
//                    testSuite = testCaseService.generateTestCase(testRequest);
//                } catch (SessionNotSelectedException e) {
//                    InsidiousNotification.notifyMessage(
//                            "Failed to generate test suite: " + e.getMessage(), NotificationType.ERROR
//                    );
//                    return null;
//                }
//                return testSuite;
//
//            }
//        });
//        if (testSuite == null) {
//            return;
//        }
//
//        @Nullable VirtualFile newFile = saveTestSuite(testSuite);
//        if (newFile == null) {
//            logger.warn("Test case generated for [" + targetClasses + "] but failed to write");
//            InsidiousNotification.notifyMessage("Failed to write test case to file", NotificationType.ERROR);
//        }
//
//
//    }

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
        String basePath = path.substring(0, last_index);
        return basePath;
    }

    public VirtualFile saveTestSuite(TestSuite testSuite) throws IOException {
        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
            String basePath = fetchPathToSaveTestCase(testCaseScript);
            if (basePath == null) {
                basePath = project.getBasePath();
            }
            Map<String, Object> valueResourceMap = testCaseScript.getTestGenerationState()
                    .getValueResourceMap();
            if (valueResourceMap.values()
                    .size() > 0) {
                String testResourcesDirPath =
                        basePath + "/src/test/resources/unlogged-fixtures/" + testCaseScript.getClassName();
                File resourcesDirFile = new File(testResourcesDirPath);
                resourcesDirFile.mkdirs();
                String resourceJson = gson.toJson(valueResourceMap);

                String testResourcesFilePath = testResourcesDirPath + "/" + testCaseScript.getTestMethodName() + ".json";
                try (FileOutputStream resourceFile = new FileOutputStream(testResourcesFilePath)) {
                    resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                }
                VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(FileSystems.getDefault()
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


            String testOutputDirPath =
                    basePath + "/src/test/java/"
                            + testCaseScript.getPackageName()
                            .replaceAll("\\.", "/");
            File outputDir = new File(testOutputDirPath);
            outputDir.mkdirs();
            File testcaseFile = new File(testOutputDirPath + "/" + testCaseScript.getClassName() + ".java");


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
            try {
                ensureTestUtilClass(basePath);
            } catch (Exception e) {
                logger.error("Failed to save UnloggedUtils to correct spot.");
            }
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
                .refreshAndFindFileByUrl(FileSystems.getDefault()
                        .getPath(utilFile.getAbsolutePath())
                        .toUri()
                        .toString());

        newFile.refresh(true, false);

    }

    public InsidiousJavaDebugProcess getDebugProcess() {
        return debugProcess;
    }

    public void setDebugProcess(InsidiousJavaDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    public void setDebugSession(XDebugSession session) {
        this.debugSession = session;
        if (session != null && pendingSelectTrace != null) {
            TracePoint selectNow = pendingSelectTrace;
            pendingSelectTrace = null;
            loadTracePoint(selectNow);
        }
    }

    public void setConnector(InsidiousJDIConnector connector) {
        this.connector = connector;
        if (pendingTrace != null) {
            try {
                connector.setTracePoint(pendingTrace, null);
                debugProcess.startPausing();
            } catch (Exception e) {
                logger.error("failed to set trace point", e);
            }
            pendingTrace = null;
        }
    }

    public void showCredentialsWindow() {
        ApplicationManager.getApplication()
                .invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Content exceptionContent = toolWindow.getContentManager()
                                .findContent("Exceptions");
                        Content traceContent = toolWindow.getContentManager()
                                .findContent("Traces");
                        if (exceptionContent != null) {
                            toolWindow.getContentManager()
                                    .removeContent(exceptionContent, true);
                        }
                        if (traceContent != null) {
                            toolWindow.getContentManager()
                                    .removeContent(traceContent, true);
                        }
                        ContentFactory contentFactory = ApplicationManager.getApplication()
                                .getService(ContentFactory.class);
                        ConfigurationWindow credentialsToolbar = new ConfigurationWindow(project, toolWindow);
                        Content credentialContent = contentFactory.createContent(credentialsToolbar.getContent(),
                                "Credentials",
                                false);
                        toolWindow.getContentManager()
                                .addContent(credentialContent);
                    }
                });
    }

    private void addAgentToRunConfig() {


//        List<RunnerAndConfigurationSettings> allSettings = ServiceManager.getService(RunManager.class)
//                .getAllSettings();

//        for (RunnerAndConfigurationSettings runSetting : allSettings) {
//            logger.info("runner config - " + runSetting.getName());
//
//            if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {
//                ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
//                String currentVMParams = applicationConfiguration.getVMParameters();
//                String newVmOptions = currentVMParams;
//                newVmOptions = VideobugUtils.addAgentToVMParams(currentVMParams, javaAgentString);
//                applicationConfiguration.setVMParameters(newVmOptions.trim());
//            }
//        }
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

    private void initiateUI() {
        logger.info("initiate ui");
        ContentFactory contentFactory = ApplicationManager.getApplication()
                .getService(ContentFactory.class);
        if (this.toolWindow == null) {
            return;
        }
        toolWindow.setIcon(UIUtils.UNLOGGED_ICON_DARK);
        ToolWindowEx ex = (ToolWindowEx) toolWindow;
        ex.stretchHeight(TOOL_WINDOW_HEIGHT - ex.getDecorator()
                .getHeight());
        ContentManager contentManager = this.toolWindow.getContentManager();
        //if (liveViewWindow == null) {
//            credentialsToolbarWindow = new ConfigurationWindow(project, this.toolWindow);
//            @NotNull Content credentialContent = contentFactory.createContent(credentialsToolbarWindow.getContent(), "Credentials", false);
//            contentManager.addContent(credentialContent);

        liveViewWindow = new LiveViewWindow(project, this);
        liveWindowContent = contentFactory.createContent(liveViewWindow.getContent(), "Test Cases", false);
        liveWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        liveWindowContent.setIcon(UIUtils.TEST_CASES_ICON_PINK);

        onboardingConfigurationWindow = new OnboardingConfigurationWindow(project, this);
        onboardingConfigurationWindowContent = contentFactory.createContent(
                onboardingConfigurationWindow.getContent(), "Get Started", false);

        singleWindowView = new SingleWindowView(project, this);
        singleWindowContent = contentFactory.createContent(singleWindowView.getContent(), "Raw View", false);


        onboardingConfigurationWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        onboardingConfigurationWindowContent.setIcon(UIUtils.ONBOARDING_ICON_PINK);
        contentManager.addContent(onboardingConfigurationWindowContent);
        setupProject();

        if (areLogsPresent()) {
            contentManager.addContent(liveWindowContent);
            contentManager.setSelectedContent(liveWindowContent, true);
            liveViewAdded = true;
        }
        //}

//        Content rawViewContent2 = contentManager.findContent("Raw");
//        if (rawViewContent2 == null) {
//        Content singleWindowContent = contentFactory.createContent(singleWindowView.getContent(), "Raw View", false);
//            contentManager.addContent(singleWindowContent);
//        }


//        if (isLoggedIn() && searchByTypesWindow == null) {
//
//            searchByTypesWindow = new SearchByTypesWindow(project, this);
//            searchByValueWindow = new SearchByValueWindow(project, this);
//            singleWindowView = new SingleWindowView(project, this);
//            liveViewWindow = new LiveViewWindow(project, this);
//            aboutUsWindow = new AboutUsWindow();
//
//            // create the windows
//            Content bugsContent = contentFactory.createContent(searchByTypesWindow.getContent(), "Exceptions", false);
//            Content traceContent = contentFactory.createContent(searchByValueWindow.getContent(), "Traces", false);
//            Content liveWindowContent = contentFactory.createContent(liveViewWindow.getContent(), "Live View", false);
//            Content aboutWindowContent = contentFactory.createContent(aboutUsWindow.getContent(), "About", false);
//
//
//            Content content = contentManager.findContent("Exceptions");
//            if (content == null) {
//                contentManager.addContent(bugsContent);
//            }
//            Content traceContent2 = contentManager.findContent("Traces");
//            if (traceContent2 == null) {
//                contentManager.addContent(traceContent);
//            }
//
//
//
//            Content liveWindowContent2 = contentManager.findContent("Live");
//            if (liveWindowContent2 == null) {
////                contentManager.addContent(liveWindowContent);
//            }
//            Content aboutVideobug = contentManager.findContent("About");
//            if (aboutVideobug == null) {
////                contentManager.addContent(aboutWindowContent);
//            }
//        }
//        if (isLoggedIn() && client.getProject() == null) {
//            logger.info("user is logged in by project is null, setting up project");
//            setupProject();
//        }


    }

    public String getJavaAgentString() {
        return javaAgentString;
    }

    public String getVideoBugAgentPath() {
        return Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath()
                .toString();
    }

    private void setAppTokenOnUi() {
        logger.info("set app token - " + appToken + " with package name " + packageName);

        String[] vmParamsToAdd = new String[]{
                "--add-opens=java.base/java.util=ALL-UNNAMED", "-javaagent:\""
                + Constants.VIDEOBUG_AGENT_PATH + "=i=" + DEFAULT_PACKAGE_NAME
//                + ",server=" + (insidiousConfiguration != null ? insidiousConfiguration.serverUrl : "https://cloud.bug.video")
//                + ",token=" + "localhost-token"
                + "\""
        };

        javaAgentString = String.join(" ", vmParamsToAdd);

        if (credentialsToolbarWindow != null) {
            credentialsToolbarWindow.setText(javaAgentString);
        }
        if (appToken != null && !Objects.equals(packageName, "YOUR.PACKAGE.NAME")) {
            addAgentToRunConfig();
        }
    }

    public void loadSession() {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project)
                    .getModules()[0];
        }

        this.client.getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                logger.error("failed to load project sessions - {}", message);
                ApplicationManager.getApplication()
                        .invokeLater(() -> InsidiousNotification.notifyMessage(
                                "No sessions found for module [" + currentModule.getName() + "]",
                                NotificationType.INFORMATION));
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                logger.info("got [" + executionSessionList.size() + "] sessions for project");
                if (executionSessionList.size() == 0) {
                    ApplicationManager.getApplication()
                            .invokeLater(() -> {

                                if (InsidiousNotification.balloonNotificationGroup != null) {
                                    InsidiousNotification.notifyMessage(
                                            "No sessions found for project " + currentModule.getName() + ". Start recording new sessions with the java agent",
                                            NotificationType.INFORMATION);
                                } else {

                                    InsidiousNotification.notifyMessage(
                                            "No sessions found" + " for project " + currentModule.getName() + " start recording new sessions with the java agent",
                                            NotificationType.INFORMATION);
                                }
                            });
                    return;
                }
                try {
                    client.setSessionInstance(new SessionInstance(executionSessionList.get(0), project));
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage("Failed to set session - " + e.getMessage(),
                            NotificationType.ERROR);
                }
            }
        });
    }

    public void loadTracePoint(TracePoint selectedTrace) {

//        JSONObject eventProperties = new JSONObject();
//        eventProperties.put("value", selectedTrace.getMatchedValueId());
//        eventProperties.put("classId", selectedTrace.getClassId());
//        eventProperties.put("className", selectedTrace.getClassname());
//        eventProperties.put("dataId", selectedTrace.getDataId());
//        eventProperties.put("fileName", selectedTrace.getFilename());
//        eventProperties.put("nanoTime", selectedTrace.getNanoTime());
//        eventProperties.put("lineNumber", selectedTrace.getLineNumber());
//        eventProperties.put("threadId", selectedTrace.getThreadId());
//
//        UsageInsightTracker.getInstance()
//                .RecordEvent("FetchByTracePoint", eventProperties);
//
//        if (debugSession == null || getActiveDebugSession(
//                ServiceManager.getService(XDebuggerManager.class)
//                        .getDebugSessions()) == null) {
//            UsageInsightTracker.getInstance()
//                    .RecordEvent("StartDebugSessionAtSelectTracepoint", null);
//            pendingSelectTrace = selectedTrace;
//            startDebugSession();
//            return;
//        }
//
//        ProgressManager.getInstance()
//                .run(new Task.Modal(project, "Unlogged", true) {
//                    public void run(@NotNull ProgressIndicator indicator) {
//
//                        try {
//                            logger.info("set trace point in connector => " + selectedTrace.getClassname());
//                            indicator.setText(
//                                    "Loading trace point " + selectedTrace.getClassname() + ":" + selectedTrace.getLineNumber() + " for value " + selectedTrace.getMatchedValueId() + " in thread " + selectedTrace.getThreadId() + " at time " + DateFormat.getInstance()
//                                            .format(new Date(selectedTrace.getNanoTime())));
//                            if (connector != null) {
//                                connector.setTracePoint(selectedTrace, indicator);
//                            } else {
//                                pendingTrace = selectedTrace;
//                            }
//
//
//                        } catch (ProcessCanceledException pce) {
//                            throw pce;
//                        } catch (Exception e) {
//                            logger.error("failed to set trace point", e);
//                            InsidiousNotification.notifyMessage("Failed to set select trace point " + e.getMessage(),
//                                    NotificationType.ERROR);
//                            return;
//                        } finally {
//                            indicator.stop();
//                        }
//
//                        if (debugSession.isPaused()) {
//                            debugSession.resume();
//                        }
//                        debugSession.pause();
//
//
//                    }
//                });


    }

    public void setExceptionClassList(Map<String, Boolean> exceptionClassList) {
        ApplicationManager.getApplication()
                .getService(InsidiousConfigurationState.class).exceptionClassMap = exceptionClassList;
    }

    public VideobugClientInterface getClient() {
        return client;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        init(project, toolWindow);
    }

    public void ensureAgentJar(boolean overwrite) {
        checkAndEnsureJavaAgent(overwrite, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {
                InsidiousNotification.notifyMessage("Failed to download java agent: " + message,
                        NotificationType.ERROR);
            }

            @Override
            public void success(String url, String path) {
                InsidiousNotification.notifyMessage("Agent jar download complete", NotificationType.INFORMATION);
            }
        });
    }

//    public Map<String, Boolean> getDefaultExceptionClassList() {
//        return ApplicationManager.getApplication().getService(InsidiousConfigurationState.class).exceptionClassMap;
//    }

//    public InsidiousConfigurationState getConfiguration() {
//        return insidiousConfiguration;
//    }

    public void refreshSession() throws APICallException, IOException, SQLException {
        logger.info("fetch latest session for module: " + currentModule.getName());
        client.setProject(currentModule.getName());

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        if (sessions.getItems()
                .size() == 0) {
            InsidiousNotification.notifyMessage("No sessions available for module [" + currentModule.getName() + "]",
                    NotificationType.ERROR);
            return;
        }
        if (client.getCurrentSession() == null || !client.getCurrentSession()
                .getSessionId()
                .equals(sessions.getItems()
                        .get(0)
                        .getSessionId())) {
            client.setSessionInstance(new SessionInstance(sessions.getItems()
                    .get(0), project));
        }
    }

    public void initiateUseLocal() {
        client = new VideobugLocalClient(Constants.VIDEOBUG_HOME_PATH + "/sessions", project);
        UsageInsightTracker.getInstance()
                .RecordEvent("InitiateUseLocal", null);


        ReadAction.run(() -> this.ensureAgentJar(false));
        ReadAction.run(InsidiousService.this::setupProject);

        ApplicationManager.getApplication()
                .invokeLater(() -> {
                    InsidiousService.this.initiateUI();
                    InsidiousNotification.notifyMessage(
                            "VideoBug logged in at [" + "disk://localhost" + "] for module [" + currentModule.getName() + "]",
                            NotificationType.INFORMATION);
                    Messages.showMessageDialog(
                            "Copy the JVM parameter and configure it for your application" + " and start running your application to start record.",
                            "Videobug", Messages.getInformationIcon());
                });
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

    public void ensureTestUtilClass() throws IOException {
        String testOutputDirPath = project.getBasePath() + "/src/test/java/io/unlogged";
        File dirPath = new File(testOutputDirPath);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }

        // yikes right
        try {


            String oldFolderPath = project.getBasePath() + "/src/test/java/io.unlogged";
            String oldFilePath = project.getBasePath() + "/src/test/java/io.unlogged/UnloggedTestUtils.java";
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

        String jacksonDatabindVersion = projectTypeInfo.getJacksonDatabindVersion();

        try (FileOutputStream writer = new FileOutputStream(utilFilePath)) {
            InputStream testUtilClassCode = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream("code/jackson/UnloggedTestUtil.java");

            // if project only uses Gson inject Gson else prefer jackson
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
                .refreshAndFindFileByUrl(FileSystems.getDefault()
                        .getPath(utilFile.getAbsolutePath())
                        .toUri()
                        .toString());

        newFile.refresh(true, false);

//        @Nullable PsiFile testFilePsiInstance = PsiManager.getInstance(project).findFile(newFile);
//        @NotNull Collection<? extends TextRange> ranges = ContainerUtil.newArrayList(testFilePsiInstance.getTextRange());
//        CodeStyleManager.getInstance(project).reformatText(testFilePsiInstance, ranges);

//        @NotNull PsiElement formattedCode = CodeStyleManagerImpl.getInstance(project).reformat(testFilePsiInstance);

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
                TestFramework.JUNIT5, MockFramework.MOCKITO, JsonFramework.GSON, ResourceEmbedMode.IN_FILE
        );

        sessionInstance.getAllTestCandidates(new TestCandidateReceiver() {

            @Override
            public void handleTestCandidate(TestCandidateMetadata testCandidateMetadata) {
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

            }
        });
    }

    public void addLiveView() {
        UsageInsightTracker.getInstance().RecordEvent("ProceedingToLiveView", null);
        if (!liveViewAdded) {
            toolWindow.getContentManager()
                    .addContent(liveWindowContent);
            toolWindow.getContentManager()
                    .setSelectedContent(liveWindowContent, true);
            liveViewAdded = true;
            try {
                liveViewWindow.setTreeStateToLoading();
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

    public void runActivity(@NotNull Project project) {
        this.project = project;
        start();
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

    public PsiFile getTargetFileForModule(String selectedModule, PROJECT_BUILD_SYSTEM type) {
        Module module = moduleMap.get(selectedModule)
                .getModule();
        PsiFile[] searchResult = null;
        String fileToSearch;
        if (type.equals(PROJECT_BUILD_SYSTEM.MAVEN)) {
            fileToSearch = "pom.xml";
        } else if (type.equals(PROJECT_BUILD_SYSTEM.GRADLE)) {
            fileToSearch = "build.gradle";
        } else {
            return null;
        }
        if (module != null) {
            searchResult = FilenameIndex.getFilesByName(project, fileToSearch,
                    GlobalSearchScope.moduleScope(module));
        }
        if (searchResult != null && searchResult.length == 0) {
            searchResult = FilenameIndex.getFilesByName(project, fileToSearch,
                    GlobalSearchScope.projectScope(project));
            searchResult = findFilesUnderModule(getSelectedModuleName(), searchResult);
        }
        if (searchResult.length > 1) {
            return fetchBaseFile(searchResult);
        } else {
            if (searchResult.length > 0) {
                return searchResult[0];
            }
        }
        return null;
    }

    private PsiFile[] findFilesUnderModule(String module, PsiFile[] files) {
        List<PsiFile> res = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getVirtualFile()
                    .getPath()
                    .contains(module)) {
                res.add(files[i]);
            }
        }
        return res.toArray(new PsiFile[res.size()]);
    }

    public boolean moduleHasFileOfType(String module, String key) {
        PsiFile[] searchResult = FilenameIndex.getFilesByName(project, key,
                GlobalSearchScope.projectScope(project));
        for (int i = 0; i < searchResult.length; i++) {
            if (searchResult[i].getVirtualFile()
                    .getPath()
                    .contains(module)) {
                return true;
            }
        }
        return false;
    }

    public PsiFile fetchBaseFile(PsiFile[] files) {
        Map<Integer, PsiFile> sizemaps = new HashMap<>();
        for (PsiFile file : files) {
            Integer ps = file.getVirtualFile()
                    .getPath()
                    .length();
            sizemaps.put(ps, file);
        }
        List<Integer> keys = new ArrayList<>(sizemaps.keySet());
        Collections.sort(keys);
        return sizemaps.get(keys.get(0));
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
        if (programRunners.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean registerProgramRunner(ProgramRunner runner) {
        if (this.programRunners.size() > 0) {
            return false;
        } else {
            this.programRunners.add(runner);
            return true;
        }
    }

    public void removeRunners() {
        this.programRunners = new ArrayList<>();
    }

    public enum PROJECT_BUILD_SYSTEM {MAVEN, GRADLE, DEF}
}
