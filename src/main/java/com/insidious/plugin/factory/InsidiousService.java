package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.*;
import com.insidious.plugin.auth.RequestAuthentication;
import com.insidious.plugin.auth.SimpleAuthority;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.coverage.ClassCoverageData;
import com.insidious.plugin.coverage.CodeCoverageData;
import com.insidious.plugin.coverage.MethodCoverageData;
import com.insidious.plugin.coverage.PackageCoverageData;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.pojo.dao.MethodDefinition;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.*;
import com.insidious.plugin.ui.assertions.SaveForm;
import com.insidious.plugin.ui.eventviewer.SingleWindowView;
import com.insidious.plugin.ui.methodscope.*;
import com.insidious.plugin.ui.stomp.StompComponent;
import com.insidious.plugin.ui.testdesigner.JUnitTestCaseWriter;
import com.insidious.plugin.ui.testdesigner.TestCaseDesigner;
import com.insidious.plugin.ui.testdesigner.TestCaseDesignerLite;
import com.insidious.plugin.util.*;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.HotSwapStatusListener;
import com.intellij.debugger.ui.HotSwapUIImpl;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.insidious.plugin.Constants.HOSTNAME;
import static com.insidious.plugin.agent.AgentCommandRequestType.DIRECT_INVOKE;
import static com.intellij.psi.PsiModifier.ABSTRACT;

@Storage("insidious.xml")
final public class InsidiousService implements
        Disposable, NewTestCandidateIdentifiedListener,
        GutterStateProvider {
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final static ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    final static private int TOOL_WINDOW_WIDTH = 500;
    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(5);
    private final UnloggedSdkApiAgent unloggedSdkApiAgent;
    private final SessionLoader sessionLoader;
    private final Map<String, DifferenceResult> executionRecord = new TreeMap<>();
    private final Map<String, Integer> methodHash = new TreeMap<>();
    private final DefaultMethodArgumentValueCache methodArgumentValueCache = new DefaultMethodArgumentValueCache();
    private final Map<String, String> candidateIndividualContextMap = new TreeMap<>();
    private final JUnitTestCaseWriter junitTestCaseWriter;
    private final Map<String, Boolean> classModifiedFlagMap = new HashMap<>();
    private final Map<SaveForm, FileEditor> saveFormEditorMap = new HashMap<>();
    private final InsidiousConfigurationState configurationState;
    private final Project project;
    private final Map<String, GutterState> cachedGutterState = new HashMap<>();
    private final GetProjectSessionsCallback sessionListener;
    private final ActiveSessionManager sessionManager;
    private final CurrentState currentState = new CurrentState();
    private VideobugClientInterface client;
    private Content singleWindowContent;
    private boolean rawViewAdded = false;
    private TestCaseService testCaseService;
    private TestCaseDesigner testCaseDesignerWindow;
    private MethodDirectInvokeComponent methodDirectInvokeComponent;
    private CoverageReportComponent coverageReportComponent;
    private StompComponent stompWindow;
    private AtomicTestComponent atomicTestComponentWindow;
    private Content testDesignerContent;
    private Content directMethodInvokeContent;
    private Content atomicTestContent;
    private Content stompWindowContent;
    private Content introPanelContent = null;


    public InsidiousService(Project project) {
        this.project = project;
        logger.info("starting insidious service: " + project);
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("projectName", project.getName());
        UsageInsightTracker.getInstance().RecordEvent("UNLOGGED_INIT", eventProperties);

        sessionManager = ApplicationManager.getApplication().getService(ActiveSessionManager.class);

        String pathToSessions = Constants.HOME_PATH + "/sessions";
        FileSystems.getDefault().getPath(pathToSessions).toFile().mkdirs();
        this.client = new VideobugLocalClient(pathToSessions, project, sessionManager);
        this.sessionLoader = ApplicationManager.getApplication().getService(SessionLoader.class);
        this.sessionLoader.setClient(this.client);
        sessionListener = new GetProjectSessionsCallback() {
            private final Map<String, Boolean> checkCache = new HashMap<>();
            private ExecutionSession currentSession;

            @Override
            public void error(String message) {
                // never called
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                if (executionSessionList.size() == 0) {
                    logger.debug("no sessions found");
                    // the currently loaded session has been deleted
                    if (currentSession != null && currentSession.getSessionId().equals("na")) {
                        // already na is set
                        return;
                    }

                    loadDefaultSession();
                    currentSession = getCurrentExecutionSession();
                    return;

                }
                ExecutionSession mostRecentSession = executionSessionList.get(0);
                logger.debug(
                        "New session: [" + mostRecentSession.getSessionId() + "] vs existing session: " + currentSession);

                if (currentSession == null) {
                    // no session currently loaded, and we can load a new sessions
                    if (!checkSessionBelongsToProject(mostRecentSession, project)) {
                        return;
                    }
                    currentSession = mostRecentSession;
                    setSession(mostRecentSession);

                } else if (!currentSession.getSessionId().equals(mostRecentSession.getSessionId())) {
                    if (!checkSessionBelongsToProject(mostRecentSession, project)) {
                        return;
                    }
                    logger.warn(
                            "Current loaded session [" + currentSession.getSessionId() + "] is different from most " +
                                    "recent session found [" + mostRecentSession.getSessionId() + "]");
                    currentSession = mostRecentSession;
                    setSession(mostRecentSession);
                }
            }

            private boolean checkSessionBelongsToProject(ExecutionSession mostRecentSession, Project project) {
                if (checkCache.containsKey(mostRecentSession.getSessionId())) {
                    return checkCache.get(mostRecentSession.getSessionId());
                }
                try {
                    String executionLogFile = mostRecentSession.getLogFilePath();
                    File logFile = new File(executionLogFile);
                    BufferedReader logFileInputStream = new BufferedReader(
                            new InputStreamReader(Files.newInputStream(logFile.toPath())));
                    // do not remove
                    String javaVersionLine = logFileInputStream.readLine();
                    String agentVersionLine = logFileInputStream.readLine();
                    String agentParamsLine = logFileInputStream.readLine();
                    if (!agentParamsLine.startsWith("Params: ")) {
                        logger.warn(
                                "The third line is not Params line, marked as session not matching: " + mostRecentSession.getLogFilePath());
                        checkCache.put(mostRecentSession.getSessionId(), false);
                        return false;
                    }
                    String[] paramParts = agentParamsLine.substring("Params: ".length()).split(",");
                    boolean foundIncludePackage = false;
                    String includedPackagedName = null;
                    for (String paramPart : paramParts) {
                        if (paramPart.startsWith("i=")) {
                            foundIncludePackage = true;
                            includedPackagedName = paramPart.substring("i=".length());
                            break;
                        }
                    }
                    if (!foundIncludePackage) {
                        checkCache.put(mostRecentSession.getSessionId(), false);
                        logger.warn(
                                "Package not found in the params, marked as session not matching" + mostRecentSession.getLogFilePath());
                        return false;
                    }

                    String finalIncludedPackagedName = includedPackagedName.replace('/', '.');
                    PsiPackage locatedPackage = ApplicationManager.getApplication().runReadAction(
                            (Computable<PsiPackage>) () -> JavaPsiFacade.getInstance(project)
                                    .findPackage(finalIncludedPackagedName));
                    if (locatedPackage == null) {
                        logger.warn("Package for agent [" + finalIncludedPackagedName + "] NOTFOUND in current " +
                                "project [" + project.getName() + "]" +
                                " -> " + mostRecentSession.getLogFilePath());
                        checkCache.put(mostRecentSession.getSessionId(), false);
                        return false;
                    }
                    logger.warn("Package for agent [" + finalIncludedPackagedName + "] FOUND in current " +
                            "project [" + project.getName() + "]" +
                            " -> " + mostRecentSession.getLogFilePath());


                    checkCache.put(mostRecentSession.getSessionId(), true);
                    return true;


                } catch (Exception e) {
                    return false;
                }

            }

        };
        this.sessionLoader.addSessionCallbackListener(sessionListener);


        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        InsidiousCaretListener listener = new InsidiousCaretListener();
        multicaster.addEditorMouseListener(listener, this);
        unloggedSdkApiAgent = new UnloggedSdkApiAgent("http://localhost:12100");
        ConnectionCheckerService connectionCheckerService = new ConnectionCheckerService(unloggedSdkApiAgent);
        threadPoolExecutor.submit(connectionCheckerService);
        junitTestCaseWriter = new JUnitTestCaseWriter(project, objectMapper);
        configurationState = project.getService(InsidiousConfigurationState.class);

    }

    //    @Unlogged
    public static void main(String[] args) {

    }

    private static String getClassMethodHashKey(AgentCommandRequest agentCommandRequest) {
        return agentCommandRequest.getClassName() + "#" + agentCommandRequest.getMethodName() + "#" + agentCommandRequest.getMethodSignature();
    }

    public void chooseClassImplementation(String className, ClassChosenListener classChosenListener) {


        PsiClass psiClass1 = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.projectScope(project));

        ImplementationSearcher implementationSearcher = new ImplementationSearcher();
        PsiElement[] implementations = implementationSearcher.searchImplementations(
                psiClass1, null, true, false
        );
        if (implementations == null || implementations.length == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + className,
                    NotificationType.ERROR);
            return;
        }
        if (implementations.length == 1) {
            PsiClass singleImplementation = (PsiClass) implementations[0];
            if (singleImplementation.isInterface() || singleImplementation.hasModifierProperty(ABSTRACT)) {
                InsidiousNotification.notifyMessage("No implementations found for " + className,
                        NotificationType.ERROR);
                return;
            }
            classChosenListener.classSelected(new ClassUnderTest(JvmClassUtil.getJvmClassName(singleImplementation)));
            return;
        }

        List<PsiClass> implementationOptions = Arrays.stream(implementations)
                .map(e -> (PsiClass) e)
                .filter(e -> !e.isInterface())
                .filter(e -> !e.hasModifierProperty(ABSTRACT))
                .collect(Collectors.toList());

        if (implementationOptions.size() == 0) {
            InsidiousNotification.notifyMessage("No implementations found for " + className,
                    NotificationType.ERROR);
            return;
        }
        if (implementationOptions.size() == 1) {
            classChosenListener.classSelected(
                    new ClassUnderTest(JvmClassUtil.getJvmClassName(implementationOptions.get(0))));
            return;
        }
        JBPopup implementationChooserPopup = JBPopupFactory
                .getInstance()
                .createPopupChooserBuilder(implementationOptions.stream()
                        .map(PsiClass::getQualifiedName)
                        .sorted()
                        .collect(Collectors.toList()))
                .setTitle("Run using implementation for " + className)
                .setItemChosenCallback(psiElementName -> {
                    Arrays.stream(implementations)
                            .filter(e -> Objects.equals(((PsiClass) e).getQualifiedName(), psiElementName))
                            .findFirst().ifPresent(e -> {
                                classChosenListener.classSelected(
                                        new ClassUnderTest(JvmClassUtil.getJvmClassName((PsiClass) e)));
                            });
                })
                .createPopup();
        implementationChooserPopup.showInFocusCenter();

    }

    public MethodAdapter getCurrentMethod() {
        return this.currentState.getCurrentMethod();
    }

    public void init() {

        try {
            logger.info("started insidious service - project name - " + project.getName());
            AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
            initiateUI();

        } catch (Throwable e) {
            logger.error("exception in unlogged service init", e);
            JSONObject properties = new JSONObject();
            properties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent(
                    "PLUGIN_START_FAILED", properties
            );
        }


    }

    public void copyToClipboard(String string) {
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        clipboard.setContents(selection, null);
    }


    public TestCaseUnit getTestCandidateCode(TestCaseGenerationConfiguration generationConfiguration) throws Exception {
        TestCaseService testCaseService = getTestCaseService();
        if (testCaseService == null) {
            return null;
        }
        return testCaseService.buildTestCaseUnit(new TestCaseGenerationConfiguration(generationConfiguration));
    }

//    public void generateAndSaveTestCase(TestCaseGenerationConfiguration generationConfiguration) throws Exception {
//        TestCaseService testCaseService = getTestCaseService();
//        if (testCaseService == null) {
//            return;
//        }
//        TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
//        ArrayList<TestCaseUnit> testCaseScripts = new ArrayList<>();
//        testCaseScripts.add(testCaseUnit);
//        TestSuite testSuite = new TestSuite(testCaseScripts);
//        junitTestCaseWriter.saveTestSuite(testSuite);
//    }

//    public TestCaseGenerationConfiguration generateMethodBoilerplate(MethodAdapter methodAdapter) {
//        return testCaseDesignerWindow.generateTestCaseBoilerPlace(methodAdapter);
//    }

    public synchronized void previewTestCase(MethodAdapter methodElement,
                                             TestCaseGenerationConfiguration generationConfiguration,
                                             boolean generateOnlyBoilerPlate) {

        UsageInsightTracker.getInstance().RecordEvent(
                "CREATE_BOILERPLATE",
                null
        );
        showDesignerLiteForm(methodElement, generationConfiguration, generateOnlyBoilerPlate);
    }

    private synchronized void initiateUI() throws IOException, FontFormatException {
        logger.info("initiate ui");
//        if (atomicTestContainerWindow != null) {
//            return;
//        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        if (toolWindow == null) {
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
        ContentManager contentManager = toolWindow.getContentManager();

//        if (!configurationState.hasShownFeatures()) {
//            configurationState.setShownFeatures();
//            IntroductionPanel introPanel = new IntroductionPanel(this);
//            introPanelContent =
//                    contentFactory.createContent(introPanel.getContent(), "Unlogged Features", false);
//            introPanelContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//            introPanelContent.setIcon(UIUtils.ONBOARDING_ICON_PINK);
//            contentManager.addContent(introPanelContent);
//        } else {
        addAllTabs();
//        }
    }

    public void addAllTabs() throws IOException, FontFormatException {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        ContentManager contentManager = toolWindow.getContentManager();

        // test case designer form
        testCaseDesignerWindow = new TestCaseDesigner();
        Disposer.register(this, testCaseDesignerWindow);
        testDesignerContent =
                contentFactory.createContent(testCaseDesignerWindow.getContent(), "JUnit Test Preview", false);
        testDesignerContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        testDesignerContent.setIcon(UIUtils.UNLOGGED_ICON_DARK);
//        contentManager.addContent(testDesignerContent);

        // stomp window
        stompWindow = new StompComponent(this);
        stompWindowContent =
                contentFactory.createContent(stompWindow.getComponent(), "Stomp", false);
        stompWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        stompWindowContent.setIcon(UIUtils.ATOMIC_TESTS);
        contentManager.addContent(stompWindowContent);

        // method executor window
//        atomicTestComponentWindow = new AtomicTestComponent(this);
//        atomicTestContent =
//                contentFactory.createContent(atomicTestComponentWindow.getComponent(), "Get Started", false);
//        atomicTestContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        atomicTestContent.setIcon(UIUtils.ATOMIC_TESTS);
//        contentManager.addContent(atomicTestContent);

//        if (currentState.isAgentServerRunning()) {
//            atomicTestComponentWindow.loadComponentForState(GutterState.PROCESS_RUNNING);
//        } else {
//            atomicTestComponentWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
//        }


//        methodDirectInvokeComponent = new MethodDirectInvokeComponent(this);
//        this.directMethodInvokeContent =
//                contentFactory.createContent(methodDirectInvokeComponent.getContent(), "Direct Invoke", false);
//        this.directMethodInvokeContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        this.directMethodInvokeContent.setIcon(UIUtils.EXECUTE_METHOD);
//        contentManager.addContent(this.directMethodInvokeContent);


        SingleWindowView singleWindowView = new SingleWindowView(project);
        singleWindowContent = contentFactory.createContent(singleWindowView.getContent(),
                "Raw Cases", false);


//        coverageReportComponent = new CoverageReportComponent();
//        Content coverageComponent = contentFactory.createContent(coverageReportComponent.getContent(),
//                "Coverage", false);
//
//        coverageComponent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        coverageComponent.setIcon(UIUtils.COVERAGE_TOOL_WINDOW_ICON);
//        contentManager.addContent(coverageComponent);
//
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
        sessionLoader.removeListener(sessionListener);
        if (client != null) {
            client.close();
            client = null;
        }
        unloggedSdkApiAgent.close();
        if (currentState.getSessionInstance() != null) {
            try {
                currentState.getSessionInstance().close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        UsageInsightTracker.getInstance().close();
    }

//    public void generateAndUploadReport() throws APICallException, IOException {
//        UsageInsightTracker.getInstance()
//                .RecordEvent("DiagnosticReport", null);
//        DiagnosticService diagnosticService = new DiagnosticService(new VersionManager(), this.project,
//                this.currentModule);
//        diagnosticService.generateAndUploadReport();
//    }

    public Project getProject() {
        return project;
    }

//    public void addLiveView() {
//        UsageInsightTracker.getInstance().RecordEvent("ProceedingToLiveView", null);
//        if (!liveViewAdded) {
//            toolWindow.getContentManager().addContent(liveWindowContent);
//            toolWindow.getContentManager().setSelectedContent(liveWindowContent, true);
//            liveViewAdded = true;
//            try {
//                liveViewWindow.setTreeStateToLoading();
//                liveViewWindow.loadInfoBanner();
//            } catch (Exception e) {
//                //exception setting state
//                logger.error("Failed to start scan after proceed.");
//            }
//        } else {
//            toolWindow.getContentManager().setSelectedContent(liveWindowContent);
//        }
//    }

    public void attachRawView() {
        if (rawViewAdded) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(singleWindowContent);
        rawViewAdded = true;
    }


    public void methodFocussedHandler(final MethodAdapter method) {

//        if (method == null) {
//            return;
//        }
//        DumbService dumbService = project.getService(DumbService.class);
//        if (dumbService.isDumb()) {
//            return;
//        }

        currentState.setCurrentMethod(method);
//        final ClassAdapter psiClass;
//        try {
//            psiClass = method.getContainingClass();
//        } catch (Exception e) {
//            // not a focusable element. return silently
//            return;
//        }

//        if (psiClass.getName() == null) {
//            return;
//        }


//        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
//        if (!toolWindow.isVisible()) {
//            logger.warn("test case designer window is not ready to create test case");
//            return;
//        }
//        if (methodDirectInvokeComponent == null) {
//            return;
//        }

//        methodDirectInvokeComponent.renderForMethod(method);
//        atomicTestComponentWindow.triggerMethodExecutorRefresh(method);

    }

    public void showDirectInvoke(MethodAdapter method) {
        stompWindow.showDirectInvoke(method);

    }

    public void compile(ClassAdapter psiClass, CompileStatusNotification compileStatusNotification) {
        XDebuggerManager xDebugManager = XDebuggerManager.getInstance(project);
        Optional<XDebugSession> currentSessionOption = Arrays
                .stream(xDebugManager.getDebugSessions())
                .filter(e -> e.getProject().equals(project))
                .findFirst();
        if (currentSessionOption.isEmpty()) {
            InsidiousNotification.notifyMessage("No debugger session found, cannot trigger hot-reload",
                    NotificationType.WARNING);
            return;
        }

        DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx(project);
        XDebugSession currentXDebugSession = currentSessionOption.get();

        Optional<DebuggerSession> currentDebugSession = debuggerManager.getSessions().stream()
                .filter(e -> Objects.equals(e.getXDebugSession(), currentXDebugSession)).findFirst();

        if (currentDebugSession.isEmpty()) {
            InsidiousNotification.notifyMessage("No debugger session found, cannot trigger hot-reload",
                    NotificationType.WARNING);
            return;
        }

        CompilerManager compilerManager = CompilerManager.getInstance(project);

        ApplicationManager.getApplication().invokeLater(() -> compilerManager.compile(
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
        ));


    }

    public AgentCommandRequest getAgentCommandRequests(AgentCommandRequest agentCommandRequest) {
        return methodArgumentValueCache.getArgumentSets(agentCommandRequest);
    }

    public void injectMocksInRunningProcess(List<DeclaredMock> allDeclaredMocks) {
        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.INJECT_MOCKS);
        agentCommandRequest.setDeclaredMocks(allDeclaredMocks);


        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                AgentCommandResponse<String> agentCommandResponse = unloggedSdkApiAgent.executeCommand(
                        agentCommandRequest);
                logger.warn("agent command response - " + agentCommandResponse);
                InsidiousNotification.notifyMessage(
                        agentCommandResponse.getMessage(), NotificationType.INFORMATION
                );
            } catch (IOException e) {
                logger.warn("failed to execute command - " + e.getMessage(), e);
                InsidiousNotification.notifyMessage(
                        "Failed to inject mocks [" + e.getMessage() + "]", NotificationType.ERROR
                );
            }
        });

    }

    public void removeMocksInRunningProcess(List<DeclaredMock> declaredMocks) {
        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.REMOVE_MOCKS);
        agentCommandRequest.setDeclaredMocks(declaredMocks);

        if (declaredMocks == null || declaredMocks.size() == 0) {
            List<DeclaredMock> existingMocks = getAllDeclaredMocks();
            existingMocks.stream()
                    .map(e -> e.getSourceClassName() + "." + e.getFieldName())
                    .forEach(configurationState::removeFieldMock);
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                AgentCommandResponse<String> agentCommandResponse = unloggedSdkApiAgent.executeCommand(
                        agentCommandRequest);
                logger.warn("agent command response - " + agentCommandResponse);
                InsidiousNotification.notifyMessage(
                        agentCommandResponse.getMessage(), NotificationType.INFORMATION
                );
            } catch (IOException e) {
                logger.warn("failed to execute command - " + e.getMessage(), e);
                InsidiousNotification.notifyMessage(
                        "Failed to remove mocks [" + e.getMessage() + "]", NotificationType.ERROR
                );
            }
        });

    }

    public void executeMethodInRunningProcess(
            AgentCommandRequest agentCommandRequest,
            ExecutionResponseListener executionResponseListener
    ) {

        methodArgumentValueCache.addArgumentSet(agentCommandRequest);
        agentCommandRequest.setRequestAuthentication(getRequestAuthentication());

        MethodUnderTest methodUnderTest = new MethodUnderTest(
                agentCommandRequest.getMethodName(), agentCommandRequest.getMethodSignature(),
                0, agentCommandRequest.getClassName()
        );

        List<DeclaredMock> availableMocks = getDeclaredMocksFor(methodUnderTest);
        if (agentCommandRequest.getRequestType().equals(DIRECT_INVOKE)) {
            List<DeclaredMock> activeMocks = availableMocks
                    .stream()
//              .filter(e -> isFieldMockActive(e.getSourceClassName(), e.getFieldName()))
                    .filter(this::isMockEnabled)
                    .collect(Collectors.toList());

            agentCommandRequest.setDeclaredMocks(activeMocks);
        } else {
            List<DeclaredMock> enabledMock = agentCommandRequest.getDeclaredMocks();
            ArrayList<DeclaredMock> setMock = new ArrayList<>();

            for (DeclaredMock localMock : enabledMock) {
                if (availableMocks.contains(localMock)) {
                    setMock.add(localMock);
                }
            }
            agentCommandRequest.setDeclaredMocks(setMock);
        }

        ApplicationManager.getApplication().executeOnPooledThread(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        AgentCommandResponse<String> agentCommandResponse = unloggedSdkApiAgent.executeCommand(
                                agentCommandRequest);
                        logger.warn("agent command response - " + agentCommandResponse);
                        if (executionResponseListener != null) {
                            cachedGutterState.remove(methodUnderTest.getMethodHashKey());

                            // TODO: search by signature and remove loop
                            PsiMethod[] methodPsiList = JavaPsiFacade.getInstance(project)
                                    .findClass(agentCommandRequest.getClassName(),
                                            GlobalSearchScope.projectScope(project))
                                    .findMethodsByName(agentCommandRequest.getMethodName(), true);
                            for (PsiMethod psiMethod : methodPsiList) {
                                if (psiMethod.getName().equals(agentCommandRequest.getMethodName())) {
                                    updateMethodHashForExecutedMethod(new JavaMethodAdapter(psiMethod));
                                }
                            }
                            triggerGutterIconReload();

                            executionResponseListener.onExecutionComplete(agentCommandRequest, agentCommandResponse);
                        } else {
                            logger.warn("no body listening for the response");
                        }
                    } catch (IOException e) {
                        logger.warn("failed to execute command - " + e.getMessage(), e);
                    }

                }));
    }

    private RequestAuthentication getRequestAuthentication() {
        RequestAuthentication requestAuthentication = new RequestAuthentication();

        PsiClass springUserDetailsClass = JavaPsiFacade.getInstance(project)
                .findClass("org.springframework.security.core.userdetails.UserDetails",
                        GlobalSearchScope.allScope(project));
        requestAuthentication.setAuthenticated(true);
        requestAuthentication.setAuthorities(List.of(new SimpleAuthority("ROLE_ADMIN")));
        requestAuthentication.setCredential("password");
        requestAuthentication.setDetails("details");
        requestAuthentication.setName("user name");

        if (springUserDetailsClass == null) {
            return requestAuthentication;
        }


        ImplementationSearcher implementationSearcher = new ImplementationSearcher();
        PsiElement[] implementations = implementationSearcher.searchImplementations(
                springUserDetailsClass, null, false, false
        );
        if (implementations == null || implementations.length == 0) {
            return requestAuthentication;
        }

        List<String> classNameOptions = new ArrayList<>();
        for (PsiElement implementation : implementations) {
            boolean match = false;
            PsiClass implementsPsi = (PsiClass) implementation;
            for (PsiClassType implementsListType : implementsPsi.getImplementsListTypes()) {
                if (implementsListType.getName().endsWith("UserDetails")) {
                    // yes match
                    match = true;
                    String qualifiedName = implementsPsi.getQualifiedName();
                    if (qualifiedName == null) {
                        continue;
                    }
                    classNameOptions.add(qualifiedName);
                    break;
                }
            }
        }

        if (classNameOptions.size() > 0) {
            Optional<String> classNameNotFromSprint = classNameOptions.stream()
                    .filter(e -> !e.contains("springframework"))
                    .findFirst();
            String userAuthClassName = classNameNotFromSprint.orElseGet(() -> classNameOptions.get(0));
            requestAuthentication.setPrincipalClassName(userAuthClassName);
            PsiClassType typeInstance = PsiClassType.getTypeByName(userAuthClassName, project,
                    GlobalSearchScope.projectScope(project));
            String dummyValue = ClassUtils.createDummyValue(typeInstance, new ArrayList<>(), project);

            requestAuthentication.setPrincipal(dummyValue);
        }


        return requestAuthentication;
    }

    public TestCaseService getTestCaseService() {
        if (testCaseService == null) {
            setSession(sessionManager.loadDefaultSession());
        }
        return testCaseService;
    }

    public synchronized void setSession(ExecutionSession executionSession) {

        SessionInstance sessionInstance = currentState.getSessionInstance();
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
        this.methodHash.clear();
        this.classModifiedFlagMap.clear();
        logger.info("Loading new session: " + executionSession.getSessionId() + " => " + project.getName());
        sessionInstance = sessionManager.createSessionInstance(executionSession, project);
        currentState.setSessionInstance(sessionInstance);
        sessionInstance.addTestCandidateListener(this);

        client.setSessionInstance(sessionInstance);
        testCaseService = new TestCaseService(sessionInstance);
        if (stompWindow != null) {
            sessionInstance.addSessionScanEventListener(stompWindow.getScanEventListener());
            stompWindow.loadNewCandidates();
        }
    }

    public void openToolWindow() {
        ToolWindowManager.getInstance(project).getToolWindow("Unlogged")
                .show(null);
    }


    @Override
    public void onNewTestCandidateIdentified(int completedCount, int totalCount) {
        if (stompWindow != null) {
            stompWindow.loadNewCandidates();
        }
    }

    public void updateCoverageReport() {

        SessionInstance sessionInstance = currentState.getSessionInstance();
        if (coverageReportComponent == null || sessionInstance == null) {
            return;
        }
        if (sessionInstance.getProject() != project) {
            return;
        }
        CodeCoverageData coverageData = sessionInstance.createCoverageData();

        List<PackageCoverageData> updatedPackageList = new ArrayList<>();
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);

        for (PackageCoverageData packageCoverageData : coverageData.getPackageCoverageDataList()) {
            List<ClassCoverageData> updatedClassList = new ArrayList<>();

            for (ClassCoverageData classCoverageData : packageCoverageData.getClassCoverageDataList()) {
                String fqcn = packageCoverageData.getPackageName() + "." + classCoverageData.getClassName();
                Map<String, List<StoredCandidate>> candidateMapByMethodName =
                        atomicRecordService.getCandidatesByClass(fqcn);
                if (candidateMapByMethodName == null) {
                    updatedClassList.add(classCoverageData);
                    continue;
                }

                List<MethodCoverageData> methodCoverageList = classCoverageData.getMethodCoverageData();
                List<MethodCoverageData> updatedMethodList = new ArrayList<>();
                for (MethodCoverageData methodCoverageData : methodCoverageList) {
                    String key = fqcn + "#" + methodCoverageData.getMethodName() + "#" + methodCoverageData.getMethodSignature();
                    List<StoredCandidate> methodCandidates = candidateMapByMethodName.get(key);
                    if (methodCandidates != null && methodCandidates.size() > 0) {
                        int coveredLineCount = methodCandidates.stream()
                                .flatMap(e -> e.getLineNumbers().stream())
                                .collect(Collectors.toSet())
                                .size();
                        methodCoverageData.setCoveredLineCount(coveredLineCount);
                    }
                    updatedMethodList.add(methodCoverageData);
                }

                updatedClassList.add(new ClassCoverageData(classCoverageData.getClassName(), updatedMethodList));


            }
            updatedPackageList.add(new PackageCoverageData(packageCoverageData.getPackageName(), updatedClassList));


        }


        coverageReportComponent.setCoverageData(new CodeCoverageData(updatedPackageList));
    }


    public CandidateSearchQuery createSearchQueryForMethod(
            MethodAdapter currentMethod,
            CandidateFilterType candidateFilterType,
            boolean loadCalls) {
        return CandidateSearchQuery.fromMethod(currentMethod,
                getInterfacesWithSameSignature(currentMethod),
                getMethodArgsDescriptor(currentMethod),
                candidateFilterType,
                loadCalls
        );
    }


    public CandidateSearchQuery createSearchQueryForMethod(MethodAdapter currentMethod, CandidateFilterType candidateFilterType) {
        return CandidateSearchQuery.fromMethod(currentMethod,
                getInterfacesWithSameSignature(currentMethod),
                getMethodArgsDescriptor(currentMethod), candidateFilterType);
    }

    public ClassMethodAggregates getClassMethodAggregates(String qualifiedName) {
        if (currentState.getSessionInstance() == null) {
            setSession(sessionManager.loadDefaultSession());
            return new ClassMethodAggregates();
        }
        return currentState.getSessionInstance().getClassMethodAggregates(qualifiedName);
    }


    public ExecutionSession getCurrentExecutionSession() {
        return currentState.getSessionInstance().getExecutionSession();
    }

    public SessionInstance getSessionInstance() {
        return currentState.getSessionInstance();
    }

    @Override
    public GutterState getGutterStateFor(MethodAdapter method) {
        // agent exists but cannot connect with agent server
        // so no process is running with the agent
        return GutterState.PROCESS_RUNNING;
//        SessionInstance sessionInstance = currentState.getSessionInstance();
//        if (!currentState.isAgentServerRunning() || sessionInstance == null) {
//            return GutterState.PROCESS_NOT_RUNNING;
//        }
//
//        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(method);
//        final String methodHashKey = methodUnderTest.getMethodHashKey();
//        if (cachedGutterState.containsKey(methodHashKey)) {
//            return cachedGutterState.get(methodHashKey);
//        }
//
//        CandidateSearchQuery query = createSearchQueryForMethod(method, CandidateFilterType.METHOD);
//
//        List<TestCandidateMetadata> candidates = getTestCandidatesFromSession(query);
//        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
//
//        //check for stored candidates here
//        boolean hasStoredCandidates = atomicRecordService.hasStoredCandidateForMethod(methodUnderTest);
//
//
//        GutterState gutterState = atomicRecordService.computeGutterState(methodUnderTest);
//
//        // process is running, but no test candidates for this method
//        if (candidates.size() == 0 && !hasStoredCandidates) {
//            cachedGutterState.put(methodHashKey, GutterState.PROCESS_RUNNING);
//            return GutterState.PROCESS_RUNNING;
//        }
//
//        // process is running, and there were test candidates for this method
//        // so check if we have executed this before
//
//        //check for change
//
//        // we haven't checked anything for this method earlier
//        // store method hash for diffs
//        String methodText = method.getText();
//        if (!this.methodHash.containsKey(methodHashKey)) {
//            //register new hash
//            this.methodHash.put(methodHashKey, methodText.hashCode());
//        }
//
//        int lastHash = this.methodHash.get(methodHashKey);
//        int currentHash = methodText.hashCode();
//
//        if (lastHash != currentHash) {
//            //re-execute as there are hash diffs
//            //update hash after execution is complete for this method,
//            //to prevent state change before exec complete.
//            classModifiedFlagMap.put(methodUnderTest.getClassName(), true);
//            ApplicationManager.getApplication()
//                    .invokeLater(() -> highlightLines(currentState.getCurrentHighlightedRequest()));
//            cachedGutterState.put(methodHashKey, GutterState.EXECUTE);
//            return GutterState.EXECUTE;
//        }
//
//        if (!executionRecord.containsKey(methodHashKey) && hasStoredCandidates && gutterState != null) {
//            cachedGutterState.put(methodHashKey, gutterState);
//            return gutterState;
//        } else {
//            if (!executionRecord.containsKey(methodHashKey)) {
//                cachedGutterState.put(methodHashKey, GutterState.DATA_AVAILABLE);
//                return GutterState.DATA_AVAILABLE;
//            }
//        }
//
//        DifferenceResult differenceResult = executionRecord.get(methodHashKey);
//
//        if (this.candidateIndividualContextMap.get(methodHashKey) != null &&
//                differenceResult.isUseIndividualContext()) {
////            logger.info("Using flow ind : " + this.candidateIndividualContextMap.get(methodHashKey));
//            switch (this.candidateIndividualContextMap.get(methodHashKey)) {
//                case "Diff":
//                    cachedGutterState.put(methodHashKey, GutterState.DIFF);
//                    return GutterState.DIFF;
//                case "NoRun":
//                    cachedGutterState.put(methodHashKey, GutterState.EXECUTE);
//                    return GutterState.EXECUTE;
//                default:
//                    cachedGutterState.put(methodHashKey, GutterState.NO_DIFF);
//                    return GutterState.NO_DIFF;
//            }
//        }
////        logger.info("Using flow normal : " + differenceResult.getDiffResultType());
//        switch (differenceResult.getDiffResultType()) {
//            case DIFF:
//                cachedGutterState.put(methodHashKey, GutterState.DIFF);
//                return GutterState.DIFF;
//            case NO_ORIGINAL:
//                cachedGutterState.put(methodHashKey, GutterState.NO_DIFF);
//                return GutterState.NO_DIFF;
//            case SAME:
//                cachedGutterState.put(methodHashKey, GutterState.NO_DIFF);
//                return GutterState.NO_DIFF;
//            default:
//                cachedGutterState.put(methodHashKey, GutterState.DIFF);
//                return GutterState.DIFF;
//        }
    }

    private boolean shouldShowReExecute(MethodAdapter adapter, MethodUnderTest methodUnderTest) {
        String methodText = adapter.getText();
        if (!this.methodHash.containsKey(methodUnderTest.getMethodHashKey())) {
            //register new hash
            this.methodHash.put(methodUnderTest.getMethodHashKey(), methodText.hashCode());
        }

        int lastHash = this.methodHash.get(methodUnderTest.getMethodHashKey());
        int currentHash = methodText.hashCode();

        if (lastHash != currentHash) {
            //re-execute as there are hash diffs
            //update hash after execution is complete for this method,
            //to prevent state change before exec complete.
            classModifiedFlagMap.put(methodUnderTest.getClassName(), true);
            ApplicationManager.getApplication()
                    .invokeLater(() -> highlightLines(currentState.getCurrentHighlightedRequest()));
            cachedGutterState.put(methodUnderTest.getMethodHashKey(), GutterState.EXECUTE);
            return true;
        }
        return false;
    }

    public GutterState getGutterStateBasedOnAgentState() {
//        if(!agentStateProvider.doesAgentExist())
//        {
//            return GutterState.NO_AGENT;
//        }
        if (currentState.isAgentServerRunning()) {
            return GutterState.PROCESS_RUNNING;
        } else {
            return GutterState.PROCESS_NOT_RUNNING;
        }
    }


    public List<String> getInterfacesWithSameSignature(MethodAdapter method) {
        String methodName = method.getName();
        ClassAdapter containingClass = method.getContainingClass();
        ClassAdapter[] interfaceList = containingClass.getInterfaces();
        List<String> interfaceQualifiedNamesWithSameMethod = new ArrayList<>();

        for (ClassAdapter classInterface : interfaceList) {
            boolean hasMethod = false;
            for (MethodAdapter interfaceMethod : classInterface.getMethods()) {
                if (interfaceMethod.getName().equals(methodName) &&
                        interfaceMethod.getJVMSignature().equals(method.getJVMSignature())) {
                    hasMethod = true;
                    break;
                }
            }

            if (hasMethod) {
                interfaceQualifiedNamesWithSameMethod.add(classInterface.getQualifiedName());
            }
        }
        return interfaceQualifiedNamesWithSameMethod;
    }

    public List<StoredCandidate> getStoredCandidatesFor(CandidateSearchQuery candidateSearchQuery) {
        if (candidateSearchQuery == null) {
            logger.warn("get stored candidates query is null");
            return List.of();
        }
        if (DumbService.getInstance(project).isDumb()) {
            logger.warn("get stored candidates project is in dumb mode [" + project.getName() + "]");
            return List.of();
        }
        return List.of();

//
//        List<TestCandidateMetadata> candidateMetadataList = getTestCandidatesFromSession(candidateSearchQuery);
//        MethodUnderTest methodUnderTest = MethodUnderTest.fromCandidateSearchQuery(candidateSearchQuery);
//        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
//
//        List<StoredCandidate> candidates = atomicRecordService.getStoredCandidatesForMethod(methodUnderTest);
//
//        List<StoredCandidate> storedCandidates = new ArrayList<>(candidates);
//
//        candidateMetadataList.stream()
//                .map(StoredCandidate::new)
//                .peek(e -> e.setMethod(methodUnderTest))
//                .forEach(storedCandidates::add);
//
////        logger.info("StoredCandidates pre filter for " + method.getName() + " -> " + storedCandidates);
//        FilteredCandidateResponseList filterStoredCandidates = filterStoredCandidates(storedCandidates);
//        List<String> updatedCandidateIds = filterStoredCandidates.getUpdatedCandidateIds();
//        updateProbeIdsForSavedCandidatesWithOldProbeIndex(
//                filterStoredCandidates.getCandidateList(),
//                candidateMetadataList);
//        if (updatedCandidateIds.size() > 0) {
//            atomicRecordService.setUseNotifications(false);
//            // because we are dealing with objects
//            // and line numbers are changed in the objects originally returned
//            // so they are changed in the cache map of the ARS
//            // so updating just one record by this call will actually persist all the changes we have made
//            atomicRecordService.saveCandidate(methodUnderTest, filterStoredCandidates.getCandidateList().get(0));
////            filterStoredCandidates.getCandidateList().stream()
////                    .filter(e -> updatedCandidateIds.contains(e.getCandidateId()))
////                    .forEach(e -> atomicRecordService.saveCandidate(methodUnderTest, e));
//            atomicRecordService.setUseNotifications(true);
//        }
//        return filterStoredCandidates.getCandidateList();
    }

    private void updateProbeIdsForSavedCandidatesWithOldProbeIndex(List<StoredCandidate> storedCandidates, List<TestCandidateMetadata> candidateMetadataList) {
        List<StoredCandidate> savedCandidatesWithOldProbes = storedCandidates.stream()
                .filter(e ->
                        e.getCandidateId() != null &&
                                currentState.getSessionInstance()
                                        .getTestCandidateById(e.getEntryProbeIndex(), true) == null)
                .collect(Collectors.toList());
        savedCandidatesWithOldProbes.forEach(e -> {
            for (TestCandidateMetadata candidateMetadata : candidateMetadataList) {
                List<String> arguments = TestCandidateUtils.buildArgumentValuesFromTestCandidate(candidateMetadata);
                if (arguments.toString().equals(e.getMethodArguments().toString())) {
                    e.setEntryProbeIndex(candidateMetadata.getEntryProbeIndex());
                    break;
                }
            }
        });

    }

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
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(method);
        String classMethodHashKey = methodUnderTest.getMethodHashKey();
        if (this.executionRecord.containsKey(classMethodHashKey)) {

            String methodBody = method.getText();
            int methodBodyHashCode = methodBody.hashCode();
            this.methodHash.put(classMethodHashKey, methodBodyHashCode);
            this.cachedGutterState.remove(classMethodHashKey);
            DaemonCodeAnalyzer.getInstance(project).restart(method.getContainingFile());
        }
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
        if (this.atomicTestComponentWindow != null) {
            atomicTestComponentWindow.loadComponentForState(state);
            if (this.atomicTestContent != null) {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
                toolWindow.getContentManager().setSelectedContent(this.atomicTestContent);
            }
        }
    }

    public MethodDirectInvokeComponent getDirectInvokeTab() {
        return methodDirectInvokeComponent;
    }

    public void focusDirectInvokeTab() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        if (toolWindow == null || directMethodInvokeContent == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(
                () -> toolWindow.getContentManager().setSelectedContent(directMethodInvokeContent, false));
    }

    public void focusTestCaseDesignerTab() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        if (toolWindow == null || testDesignerContent == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(
                () -> toolWindow.getContentManager().setSelectedContent(testDesignerContent, true));
    }

    public void generateCompareWindows(String before, String after) {
        DocumentContent content1 = DiffContentFactory.getInstance().create(getPrettyJsonString(before));
        DocumentContent content2 = DiffContentFactory.getInstance().create(getPrettyJsonString(after));
        SimpleDiffRequest request = new SimpleDiffRequest(
                "Comparing Before and After", content1, content2, "Before", "After");
        showDiffEditor(request);
    }

    private String getPrettyJsonString(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(input));
        } catch (JsonProcessingException e) {
            return input;
        }
    }

    public void showDiffEditor(SimpleDiffRequest request) {
        DiffManager.getInstance().showDiff(project, request);
    }

    public void showCandidateSaveForm(SaveForm saveForm) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
        if (selectedEditor == null) {
            StoredCandidate candidate = saveForm.getStoredCandidate();
            selectedEditor = InsidiousUtils.focusProbeLocationInEditor(0,
                    candidate.getMethod().getClassName(), this.getProject());
            if (selectedEditor == null) {
                InsidiousNotification.notifyMessage(
                        "No editor tab is open, please open an editor tab",
                        NotificationType.ERROR
                );
                return;
            }
        }
        fileEditorManager.addTopComponent(selectedEditor, saveForm.getComponent());
        saveFormEditorMap.put(saveForm, selectedEditor);
    }

    public void showDesignerLiteForm(MethodAdapter methodAdapter,
                                     @Nullable TestCaseGenerationConfiguration configuration,
                                     boolean generateOnlyBoilerPlate) {
        if (methodAdapter == null) {
            InsidiousNotification.notifyMessage("Please select a method to generate a Junit test case.",
                    NotificationType.INFORMATION);
            return;
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        TestCaseDesignerLite designerLite = new TestCaseDesignerLite(methodAdapter,
                configuration,
                generateOnlyBoilerPlate,
                project);
        fileEditorManager.openFile(designerLite.getLightVirtualFile(), true);
        FileEditor selectedEditor = fileEditorManager.getSelectedEditor();
        if (selectedEditor == null) {
            selectedEditor = InsidiousUtils.focusProbeLocationInEditor(0,
                    methodAdapter.getContainingClass().getQualifiedName(), project);
            if (selectedEditor == null) {
                InsidiousNotification.notifyMessage(
                        "No editor tab is open, please open an editor tab",
                        NotificationType.ERROR
                );
                return;
            }
        }
        fileEditorManager.addBottomComponent(selectedEditor, designerLite.getMainPanel());
        designerLite.setEditorReferences(fileEditorManager.getSelectedTextEditor(), selectedEditor);
    }

    public void hideCandidateSaveForm(SaveForm saveFormReference) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = saveFormEditorMap.get(saveFormReference);
        if (selectedEditor != null) {
            fileEditorManager.removeTopComponent(selectedEditor, saveFormReference.getComponent());
        }
    }

    public void addDiffRecord(DifferenceResult newDiffRecord) {
        if (newDiffRecord == null) {
            return;
        }
        AgentCommandRequest agentCommandRequest = newDiffRecord.getCommand();
        String keyName = getClassMethodHashKey(agentCommandRequest);
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
//        reportingService.addRecord(result);
    }

    public void setAgentProcessState(GutterState newState) {
        if (this.atomicTestComponentWindow != null) {
            atomicTestComponentWindow.loadComponentForState(newState);
        }
    }

    public void compileAndExecuteWithAgentForMethod(JavaMethodAdapter methodAdapter) {
        atomicTestComponentWindow.triggerMethodExecutorRefresh(methodAdapter);
        atomicTestComponentWindow.triggerCompileAndExecute();
    }


    public void focusAtomicTestsWindow() {
        if (this.atomicTestContent != null) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
            toolWindow.getContentManager().setSelectedContent(this.atomicTestContent);
            toolWindow.show(null);
        }
    }

    public boolean isAgentConnected() {
        return currentState.isAgentServerRunning();
    }


    public void setAtomicWindowHeading(String name) {
//        atomicTestContent.setDisplayName(name);
//        if (name.startsWith("Get")) {
//            atomicTestContent.setIcon(UIUtils.ONBOARDING_ICON_PINK);
//        } else {
//            atomicTestContent.setIcon(UIUtils.ATOMIC_TESTS);
//        }
    }

    public Map<String, String> getIndividualCandidateContextMap() {
        return this.candidateIndividualContextMap;
    }

    public void toggleReportGeneration() {
//        this.reportingService.toggleReportMode();
    }

    public MethodDefinition getMethodInformation(MethodUnderTest methodUnderTest) {
        return currentState.getSessionInstance().getMethodDefinition(methodUnderTest);
    }

    public void highlightLines(HighlightedRequest highlightRequest) {
        if (highlightRequest == null) {
            return;
        }

        HighlightedRequest currentHighlightedRequest = currentState.getCurrentHighlightedRequest();
        if (currentHighlightedRequest != null && classModifiedFlagMap.containsKey(
                currentHighlightedRequest.getMethodUnderTest().getClassName())) {
            removeCurrentActiveHighlights();
            currentState.setCurrentHighlightedRequest(null);
        }

        if (classModifiedFlagMap.containsKey(highlightRequest.getMethodUnderTest().getClassName())) {
            return;
        }


        // no new highlight if disabled or tool window is hidden
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        if (!currentState.isCodeCoverageHighlightEnabled() || (toolWindow == null || !toolWindow.isVisible()) || !currentState.isAgentServerRunning()) {
            currentState.setCurrentHighlightedRequest(highlightRequest);
            removeCurrentActiveHighlights();
            return;
        }
        if (Objects.equals(highlightRequest,
                currentHighlightedRequest) && currentState.getCurrentHighlightedRequest() != null) {
            // its already highlighted ?
            return;
        } else {
            removeCurrentActiveHighlights();
        }
        currentHighlightedRequest = highlightRequest;
        MethodUnderTest methodUnderTest = currentHighlightedRequest.getMethodUnderTest();
        if (methodUnderTest.getClassName() == null) {
            return;
        }

        // add new highlights to the current editor
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        MarkupModel markupModel = selectedTextEditor.getMarkupModel();
        Document currentDocument = selectedTextEditor.getDocument();

        String simpleClassName = ClassUtils.getSimpleName(methodUnderTest.getClassName());


        // check the method under test is present in the current editor
        // check by class name and method name
        if (!currentDocument.getText().contains("class " + simpleClassName)
                || !currentDocument.getText().contains(" " + methodUnderTest.getName())) {
            // the requested highlight method is not in current editor
            return;
        } else {
            // method found in current editor, can highlight
        }

        List<RangeHighlighter> newHighlightList = new ArrayList<>();
        ActiveHighlight currentActiveHighlight = new ActiveHighlight(newHighlightList, selectedTextEditor);
        currentState.setCurrentActiveHighlight(currentActiveHighlight);

        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(UIUtils.HIGHLIGHT_BACKGROUND_COLOR);
        for (Integer coveredLine : highlightRequest.getLinesToHighlight()) {
            try {
                RangeHighlighter addedHighlighters = markupModel.addLineHighlighter(coveredLine - 1,
                        HighlighterLayer.ERROR, attributes);
                newHighlightList.add(addedHighlighters);
            } catch (Throwable e) {
                logger.warn("Failed to highlight: " + e.getMessage());
            }
        }
    }

    public void removeCurrentActiveHighlights() {
        // remove existing highlighters
        ActiveHighlight currentActiveHighlight = currentState.getCurrentActiveHighlight();
        if (currentActiveHighlight != null) {
            Editor editor = currentActiveHighlight.getEditor();
            MarkupModel markupModel = editor.getMarkupModel();
            for (RangeHighlighter currentActiveHighlighter : currentActiveHighlight.getRangeHighlighterList()) {
                try {
                    markupModel.removeHighlighter(currentActiveHighlighter);
                } catch (Throwable e) {
                    logger.warn("failed to remove highlight: " + e.getMessage());
                }
            }
            currentState.setCurrentActiveHighlight(null);
        }
    }

    public void setCodeCoverageHighlightEnabled(boolean state) {
        currentState.setCodeCoverageHighlightEnabled(state);
        highlightLines(currentState.getCurrentHighlightedRequest());
    }

    public HighlightedRequest getCurrentHighlightRequest() {
        return currentState.getCurrentHighlightedRequest();
    }

    public JUnitTestCaseWriter getJUnitTestCaseWriter() {
        return junitTestCaseWriter;
    }

    public String guessModuleBasePath(ClassAdapter currentClass) {
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        return atomicRecordService.guessModuleBasePath((PsiClass) currentClass.getSource());
    }

    public List<DeclaredMock> getDeclaredMocksOf(MethodUnderTest methodUnderTest) {
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        return atomicRecordService.getDeclaredMocksOf(methodUnderTest);
    }

    public List<DeclaredMock> getDeclaredMocksFor(MethodUnderTest methodUnderTest) {
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        return atomicRecordService.getDeclaredMocksFor(methodUnderTest);
    }

    public List<DeclaredMock> getAllDeclaredMocks() {
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        return atomicRecordService.getAllDeclaredMocks();
    }

    public void saveMockDefinition(DeclaredMock declaredMock, MethodUnderTest methodUnderTest) {
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        atomicRecordService.saveMockDefinition(methodUnderTest, declaredMock);
        if (!isMockEnabled(declaredMock)) {
            enableMock(declaredMock);
        } else if (isFieldMockActive(declaredMock.getSourceClassName(), declaredMock.getFieldName())) {
            injectMocksInRunningProcess(List.of(declaredMock));
        }
    }

    public void deleteMockDefinition(MethodUnderTest methodUnderTest, DeclaredMock declaredMock) {
        disableMock(declaredMock);
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        atomicRecordService.deleteMockDefinition(methodUnderTest, declaredMock);
        if (isFieldMockActive(declaredMock.getSourceClassName(), declaredMock.getFieldName())) {
            removeMocksInRunningProcess(List.of(declaredMock));
        }
    }


    public void disableFieldMock(String className, String fieldName) {
        configurationState.removeFieldMock(className + "." + fieldName);
    }

    public void enableFieldMock(String className, String fieldName) {
        configurationState.addFieldMock(className + "." + fieldName);
    }

    public boolean isFieldMockActive(String className, String fieldName) {
        return configurationState.isFieldMockActive(className + "." + fieldName);
    }

    public void disableMock(DeclaredMock declaredMock) {
        configurationState.removeMock(declaredMock.getId());
        if (isFieldMockActive(declaredMock.getSourceClassName(), declaredMock.getFieldName())) {
            removeMocksInRunningProcess(List.of(declaredMock));
        }
    }

    public void enableMock(DeclaredMock declaredMock) {
        configurationState.addMock(declaredMock.getId());
        if (isFieldMockActive(declaredMock.getSourceClassName(), declaredMock.getFieldName())) {
            injectMocksInRunningProcess(List.of(declaredMock));
        }
    }

    public boolean isMockEnabled(DeclaredMock declaredMock) {
        return configurationState.isActiveMock(declaredMock.getId());
    }

    public void showIntroductionPanel() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        ContentManager contentManager = toolWindow.getContentManager();
        if (introPanelContent == null) {
            ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
            IntroductionPanel introPanel = new IntroductionPanel(this);
            introPanelContent =
                    contentFactory.createContent(introPanel.getContent(), "Unlogged Features", false);
            introPanelContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
            introPanelContent.setIcon(UIUtils.ONBOARDING_ICON_PINK);
            contentManager.addContent(introPanelContent);
        }
        contentManager.setSelectedContent(introPanelContent);
    }

    public void loadDefaultSession() {
        setSession(sessionManager.loadDefaultSession());
    }

    public void onAgentConnected(ServerMetadata serverMetadata) {
        currentState.setAgentServerRunning(true);
        Collection<? extends AnAction> actions = Arrays.asList(
                new AnAction("Direct Invoke") {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        focusDirectInvokeTab();
                        openToolWindow();
                    }
                },
                new AnAction("Replay List") {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        focusAtomicTestsWindow();
                        openToolWindow();
                    }
                }
        );
//        InsidiousNotification.notifyMessage("Recording in progress package " +
//                        serverMetadata.getIncludePackageName() + " " +
//                        "Executed methods and saved replays will show up in the " +
//                        "Replay tab. Use DirectInvoke to execute methods from here.",
//                NotificationType.INFORMATION, actions);

        stompWindow.clear();
        stompWindow.setConnectedAndWaiting();

        triggerGutterIconReload();
        setAgentProcessState(GutterState.PROCESS_RUNNING);
//        focusDirectInvokeTab();
    }

    public void onAgentDisconnected() {
        if (atomicTestComponentWindow != null) {
            atomicTestComponentWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
        }
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        if (atomicRecordService != null) {
            ApplicationManager.getApplication().runWriteAction(atomicRecordService::writeAll);
        }
        stompWindow.disconnected();
        removeCurrentActiveHighlights();
        triggerGutterIconReload();
        configurationState.clearPermanentFieldMockSetting();
        cachedGutterState.clear();
    }

    public int getMethodCallCountBetween(long start, long end) {
        return getSessionInstance().getMethodCallCountBetween(start, end);
    }

    public List<MethodCallExpression> getMethodCallsBetween(long start, long end) {
        return getSessionInstance().getMethodCallsBetween(start, end);
    }

    public List<TestCandidateMetadata> getTestCandidateBetween(long eventId, long eventId1) throws SQLException {
        return getSessionInstance().getTestCandidateBetween(eventId, eventId1);
    }

    public void executeSingleCandidate(
            StoredCandidate testCandidate,
            ClassUnderTest classUnderTest,
            ExecutionRequestSourceType source,
            AgentCommandResponseListener<StoredCandidate, String> agentCommandResponseListener,
            MethodAdapter methodElement) {
        MethodUnderTest methodUnderTest = testCandidate.getMethod();
        List<String> methodArgumentValues = testCandidate.getMethodArguments();
        List<DeclaredMock> testCandidateStoredEnabledMockDefinition = getDeclaredMocksFor(methodUnderTest);
        AgentCommandRequest agentCommandRequest = MethodUtils.createExecuteRequestWithParameters(
                methodElement, classUnderTest, methodArgumentValues, true, testCandidateStoredEnabledMockDefinition);


        executeMethodInRunningProcess(agentCommandRequest,
                (request, agentCommandResponse) -> {

                    DifferenceResult diffResult = DiffUtils.calculateDifferences(testCandidate, agentCommandResponse);

                    logger.info("Source [EXEC]: " + source);
                    if (source == ExecutionRequestSourceType.Bulk) {
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_REPLAY);
                        diffResult.setIndividualContext(false);
                        String batchID = String.valueOf(new Date().getTime());
                        diffResult.setBatchID(batchID);
                    } else {
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_INDIVIDUAL);
                        //check other statuses and add them for individual execution
//                        String status = getExecutionStatusFromCandidates(
//                                getKeyForCandidate(testCandidate),
//                                diffResult.getDiffResultType());
//                        String methodKey = agentCommandRequest.getClassName()
//                                + "#" + agentCommandRequest.getMethodName() + "#" + agentCommandRequest.getMethodSignature();
//                        logger.info("Setting status multi run : " + status);
//                        getIndividualCandidateContextMap().put(methodKey, status);
                        diffResult.setIndividualContext(true);
                    }

                    diffResult.setResponse(agentCommandResponse);
                    diffResult.setCommand(agentCommandRequest);

                    addDiffRecord(diffResult);

                    StoredCandidateMetadata meta = testCandidate.getMetadata();
                    if (meta == null) {
                        meta = new StoredCandidateMetadata(HOSTNAME, HOSTNAME, agentCommandResponse.getTimestamp(),
                                getStatusForState(diffResult.getDiffResultType()));
                    }
                    meta.setTimestamp(agentCommandResponse.getTimestamp());
                    meta.setCandidateStatus(getStatusForState(diffResult.getDiffResultType()));
                    if (testCandidate.getCandidateId() != null) {
                        project.getService(AtomicRecordService.class)
                                .setCandidateStateForCandidate(
                                        testCandidate.getCandidateId(),
                                        agentCommandRequest.getClassName(),
                                        methodUnderTest.getMethodHashKey(),
                                        testCandidate.getMetadata().getCandidateStatus());
                    }
                    agentCommandResponseListener.onSuccess(testCandidate, agentCommandResponse, diffResult);
                });
    }

    private String getKeyForCandidate(StoredCandidate testCandidateMetadata) {
        return testCandidateMetadata.getCandidateId() == null ? String.valueOf(
                testCandidateMetadata.getSessionIdentifier()) : testCandidateMetadata.getCandidateId();
    }

    private StoredCandidateMetadata.CandidateStatus getStatusForState(DiffResultType type) {
        switch (type) {
            case SAME:
            case NO_ORIGINAL:
                return StoredCandidateMetadata.CandidateStatus.PASSING;
            default:
                return StoredCandidateMetadata.CandidateStatus.FAILING;
        }
    }

    private boolean showDifferentStatus(DiffResultType type) {
        return type != DiffResultType.SAME;
    }


//    public String getExecutionStatusFromCandidates(String excludeKey, DiffResultType type) {
//        if (showDifferentStatus(type)) {
//            return "Diff";
//        }
//        boolean hasDiff = false;
//        boolean hasNoRun = false;
//        for (String key : candidateComponentMap.keySet()) {
//            if (Objects.equals(key, excludeKey)) {
//                continue;
//            }
//            TestCandidateListedItemComponent component = candidateComponentMap.get(key);
//            String status = component.getExecutionStatus().trim();
//            if (status.isEmpty() || status.isBlank()) {
//                hasNoRun = true;
//            }
//            if (status.contains("Diff")) {
//                hasDiff = true;
//            }
//        }
//        if (hasDiff) {
//            return "Diff";
//        } else if (hasNoRun) {
//            return "NoRun";
//        }
//        return "Same";
//    }


}
