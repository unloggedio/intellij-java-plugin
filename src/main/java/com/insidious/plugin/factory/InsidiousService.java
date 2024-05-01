package com.insidious.plugin.factory;

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
import com.insidious.plugin.autoexecutor.AutoExecutorReportRecord;
import com.insidious.plugin.autoexecutor.AutomaticExecutorService;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.*;
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
import com.insidious.plugin.ui.InsidiousCaretListener;
import com.insidious.plugin.ui.NewTestCandidateIdentifiedListener;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.UnloggedSDKOnboarding;
import com.insidious.plugin.ui.eventviewer.SingleWindowView;
import com.insidious.plugin.ui.library.ItemFilterType;
import com.insidious.plugin.ui.library.LibraryComponent;
import com.insidious.plugin.ui.library.LibraryFilterState;
import com.insidious.plugin.ui.methodscope.*;
import com.insidious.plugin.ui.mocking.OnSaveListener;
import com.insidious.plugin.ui.stomp.StompComponent;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.insidious.plugin.ui.testdesigner.JUnitTestCaseWriter;
import com.insidious.plugin.util.*;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.ui.HotSwapStatusListener;
import com.intellij.debugger.ui.HotSwapUIImpl;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.lang.jvm.util.JvmClassUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.impl.EditorFactoryImpl;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.insidious.plugin.Constants.HOSTNAME;
import static com.insidious.plugin.agent.AgentCommandRequestType.DIRECT_INVOKE;
import static com.intellij.psi.PsiModifier.ABSTRACT;

@Storage("insidious.xml")
@Service(Service.Level.PROJECT)
final public class InsidiousService implements
        Disposable, NewTestCandidateIdentifiedListener {
    public static final Set<String> SKIP_METHOD_IN_FOLLOW_FILTER = new HashSet<>(
            Arrays.asList("Object", "clone", "toString",
                    "equals", "finalize", "notify", "hashCode", "wait",
                    "getClass"));
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final static ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    final static private int TOOL_WINDOW_WIDTH = 400;
    private final ExecutorService connectionCheckerThreadPool = Executors.newFixedThreadPool(1,
            new DefaultThreadFactory("UnloggedProjectThreadPool", true));
    private final Map<String, DifferenceResult> executionRecord = new TreeMap<>();
    private final Map<String, Integer> methodHash = new TreeMap<>();
    private final DefaultMethodArgumentValueCache methodArgumentValueCache = new DefaultMethodArgumentValueCache();
    private final Map<String, String> candidateIndividualContextMap = new TreeMap<>();
    private final JUnitTestCaseWriter junitTestCaseWriter;
    private final Map<String, Boolean> classModifiedFlagMap = new HashMap<>();
    private final InsidiousConfigurationState configurationState;
    private final Project project;
    private final Map<String, GutterState> cachedGutterState = new HashMap<>();
    private final GetProjectSessionsCallback sessionListener;
    private final ActiveSessionManager sessionManager;
    private final CurrentState currentState = new CurrentState();
    private final MockManager mockManager;
    Map<MethodUnderTest, List<UnloggedTimingTag>> availableTimingTags = new HashMap<>();
    private ScheduledExecutorService stompComponentThreadPool = null;
    private SessionLoader sessionLoader;
    private VideobugClientInterface client;
    private Content singleWindowContent;
    private boolean rawViewAdded = false;
    private TestCaseService testCaseService;
    private MethodDirectInvokeComponent methodDirectInvokeComponent;
    private CoverageReportComponent coverageReportComponent;
    private StompComponent stompWindow;
    private Content testDesignerContent;
    private Content directMethodInvokeContent;
    private Content atomicTestContent;
    private Content stompWindowContent;
    private AutomaticExecutorService automaticExecutorService = new AutomaticExecutorService(this);
    private ReportingService reportingService = new ReportingService(this);
    private Content onboardingWindowContent;
    private UnloggedSDKOnboarding onboardingWindow;
    private boolean addedStompWindow;
    private Content libraryWindowContent;
    private LibraryComponent libraryToolWindow;
    private ToolWindow toolWindow;

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
        sessionListener = new GetProjectSessionsCallback() {
            private final Map<String, ServerMetadata> checkCache = new HashMap<>();
            private ExecutionSession currentSession;

            @Override
            public void error(String message) {
                // never called
            }

            @Override
            public synchronized void success(List<ExecutionSession> executionSessionList) {
                if (DumbService.getInstance(project).isDumb()) {
//                    DumbService.getInstance(project).runWhenSmart(() -> {
//                        setSession(executionSession);
//                    });
                    return;
                }

                if (executionSessionList.isEmpty()) {
                    logger.debug("no sessions found");
                    // the currently loaded session has been deleted
                    if (currentSession != null && currentSession.getSessionId().equals("na")) {
                        // already na is set
                        return;
                    }
                    currentSession = getCurrentExecutionSession();
                    return;

                }
                ExecutionSession mostRecentSession = executionSessionList.get(0);
                logger.debug(
                        "New session: [" + mostRecentSession.getSessionId() + "] vs existing session: " + currentSession);

                ServerMetadata serverMetadata;
                if (currentSession == null) {
                    serverMetadata = checkSessionBelongsToProject(mostRecentSession, project);
                    // no session currently loaded, and we can load a new sessions
                    if (serverMetadata == null) {
                        return;
                    }
                    onAgentConnected(serverMetadata);

                } else if (currentSession.getSessionId().equals(mostRecentSession.getSessionId())) {
                    // already using this session
                    return;
                } else {
                    serverMetadata = checkSessionBelongsToProject(mostRecentSession, project);
                    if (null == serverMetadata) {
                        return;
                    }
                    logger.warn(
                            "Current loaded session [" + currentSession.getSessionId() + "] is different from most " +
                                    "recent session found [" + mostRecentSession.getSessionId() + "]");
                }
                currentSession = mostRecentSession;
                try {
                    setSession(mostRecentSession, serverMetadata);
                } catch (Throwable t) {
                    InsidiousNotification.notifyMessage("Failed to load session: " + t.getMessage(),
                            NotificationType.ERROR);
                }
            }

            private ServerMetadata checkSessionBelongsToProject(ExecutionSession session, Project project) {
                if (checkCache.containsKey(session.getSessionId())) {
                    return checkCache.get(session.getSessionId());
                }
                try {
                    String executionLogFile = session.getLogFilePath();
                    File logFile = new File(executionLogFile);
                    if (!logFile.exists()) {
                        return null;
                    }
                    BufferedReader logFileInputStream = new BufferedReader(
                            new InputStreamReader(Files.newInputStream(logFile.toPath())));
                    // do not remove
                    String javaVersionLine = logFileInputStream.readLine();
                    String agentVersionLine = logFileInputStream.readLine();
                    String agentParamsLine = logFileInputStream.readLine();
                    if (!agentParamsLine.startsWith("Params: ")) {
                        logger.warn(
                                "The third line is not Params line, marked as session not matching: " + session.getLogFilePath());
                        checkCache.put(session.getSessionId(), null);
                        return null;
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
//                        checkCache.put(session.getSessionId(), new ServerMetadata());
                        logger.warn(
                                "Package not found in the params, marked as session not matching: " + session.getLogFilePath());
//                        return null;
                    }
                    String serverMetadataJson = logFileInputStream.readLine();
                    ServerMetadata serverMetadata;
                    try {
                        serverMetadata = objectMapper.readValue(serverMetadataJson,
                                ServerMetadata.class);
                    } catch (Exception e) {
                        checkCache.put(session.getSessionId(), new ServerMetadata());
                        logger.warn("Failed to read server metadata from log: [" + serverMetadataJson + "]", e);
                        InsidiousNotification.notifyMessage("Found session at [" + session.getPath() + "] " +
                                        "but couldn't connect to server because of missing server metadata.",
                                NotificationType.ERROR);
                        checkCache.put(session.getSessionId(), null);
                        return null;
                    }

                    String finalIncludedPackagedName = includedPackagedName.replace('/', '.');
                    PsiPackage locatedPackage = ApplicationManager.getApplication().runReadAction(
                            (Computable<PsiPackage>) () -> JavaPsiFacade.getInstance(project)
                                    .findPackage(finalIncludedPackagedName));
                    if (locatedPackage == null) {
                        logger.warn("Package for agent [" + finalIncludedPackagedName + "] NOTFOUND in current " +
                                "project [" + project.getName() + "]" +
                                " -> " + session.getLogFilePath());
//                        checkCache.put(session.getSessionId(), new ServerMetadata());
//                        return new ServerMetadata();
                    } else {

                        logger.warn("Package for agent [" + finalIncludedPackagedName + "] FOUND in current " +
                                "project [" + project.getName() + "]" +
                                " -> " + session.getLogFilePath());
                    }


                    checkCache.put(session.getSessionId(), serverMetadata);
                    return serverMetadata;


                } catch (Exception e) {
                    logger.warn("Failed to check session: " + session.getLogFilePath(), e);
                    checkCache.put(session.getSessionId(), null);
                    return null;
                }

            }

        };


        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        InsidiousCaretListener listener = new InsidiousCaretListener();
        multicaster.addEditorMouseListener(listener, this);
        multicaster.addCaretListener(listener, this);
        multicaster.addDocumentListener(listener, this);

        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorCreated(@NotNull EditorFactoryEvent event) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    DumbService.getInstance(project)
                            .runReadActionInSmartMode(() -> {
                                populateFromEditors(null);
                            });
                });

            }

            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    populateFromEditors(event);
                });
            }
        }, this);


//        ConnectionCheckerService connectionCheckerService = new ConnectionCheckerService(unloggedSdkApiAgentClient);
//        connectionCheckerThreadPool.submit(connectionCheckerService);
        junitTestCaseWriter = new JUnitTestCaseWriter(project, objectMapper);
        configurationState = project.getService(InsidiousConfigurationState.class);

        mockManager = new MockManager(configurationState);

        if (configurationState.getFilterModel().isFollowEditor()) {
            populateFromEditors(null);
        }

    }

    //    @Unlogged
    public static void main(String[] args) {

    }

    private static String getClassMethodHashKey(AgentCommandRequest agentCommandRequest) {
        return agentCommandRequest.getClassName() + "#" + agentCommandRequest.getMethodName() + "#" + agentCommandRequest.getMethodSignature();
    }


    public void chooseClassImplementation(String className, ClassChosenListener classChosenListener) {


        PsiClass psiClass1 = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiClass>) () -> JavaPsiFacade.getInstance(project)
                        .findClass(className, GlobalSearchScope.projectScope(project)));

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
            if (ApplicationManager.getApplication().runReadAction(
                    (Computable<Boolean>) singleImplementation::isInterface) || ApplicationManager.getApplication()
                    .runReadAction(
                            (Computable<Boolean>) () -> singleImplementation.hasModifierProperty(ABSTRACT))) {
                InsidiousNotification.notifyMessage("No implementations found for " + className,
                        NotificationType.ERROR);
                return;
            }
            classChosenListener.classSelected(new ClassUnderTest(ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> JvmClassUtil.getJvmClassName(singleImplementation))));
            return;
        }

        List<PsiClass> implementationOptions = ApplicationManager.getApplication().runReadAction(
                (Computable<? extends List<PsiClass>>) () -> Arrays.stream(implementations)
                        .map(e -> (PsiClass) e)
                        .filter(e -> !e.isInterface())
                        .filter(e -> !e.hasModifierProperty(ABSTRACT))
                        .collect(Collectors.toList()));

        if (implementationOptions.isEmpty()) {
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
                        .map((e) -> ApplicationManager.getApplication().runReadAction(
                                (Computable<String>) e::getQualifiedName))
                        .filter(Objects::nonNull)
                        .sorted()
                        .collect(Collectors.toList()))
                .setTitle("Run using implementation for " + className)
                .setItemChosenCallback(psiElementName -> {
                    Arrays.stream(implementations)
                            .filter(e -> Objects.equals(ApplicationManager.getApplication().runReadAction(
                                            (Computable<String>) () -> ((PsiClass) e).getQualifiedName()),
                                    psiElementName))
                            .findFirst().ifPresent(e -> {
                                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                    classChosenListener.classSelected(
                                            new ClassUnderTest(ApplicationManager.getApplication().runReadAction(
                                                    (Computable<String>) () -> JvmClassUtil.getJvmClassName((PsiClass) e))));
                                });
                            });
                })
                .createPopup();
        ApplicationManager.getApplication().invokeLater(() -> {
            Point cursorLocation = MouseInfo.getPointerInfo().getLocation();
            // Show the popup under the cursor
            implementationChooserPopup.show(new RelativePoint(new Point(cursorLocation.x, cursorLocation.y)));
        });
    }

    public ReportingService getReportingService() {
        return reportingService;
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

    public MethodAdapter getCurrentMethod() {
        return this.currentState.getCurrentMethod();
    }

    public void init() {

        try {
            logger.info("started insidious service - project name - " + project.getName());
            AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
            initiateUI();

        } catch (Throwable e) {
            e.printStackTrace();
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
//        TestCaseService testCaseService;
        if (testCaseService == null) {
            return null;
        }
        return ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                return testCaseService.buildTestCaseUnit(new TestCaseGenerationConfiguration(generationConfiguration));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();
    }

    public synchronized FileEditorManager previewTestCase(LightVirtualFile lightVirtualFile) {

        UsageInsightTracker.getInstance().RecordEvent(
                "CREATE_BOILERPLATE",
                null
        );
        return showDesignerLiteForm(lightVirtualFile);

    }

//    public void generateAndUploadReport() throws APICallException, IOException {
//        UsageInsightTracker.getInstance()
//                .RecordEvent("DiagnosticReport", null);
//        DiagnosticService diagnosticService = new DiagnosticService(new VersionManager(), this.project,
//                this.currentModule);
//        diagnosticService.generateAndUploadReport();
//    }

    private synchronized void initiateUI() {
        logger.info("initiate ui");
        if (toolWindow != null) {
            return;
        }
        toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
        toolWindow.setIcon(UIUtils.UNLOGGED_ICON_DARK_SVG);
        try {
            ToolWindowEx ex = (ToolWindowEx) toolWindow;
            ex.stretchWidth(TOOL_WINDOW_WIDTH - ex.getDecorator().getWidth());
        } catch (NullPointerException npe) {
            // ignored
        }

        addAllTabs();

        CountDownLatch cdl = new CountDownLatch(1);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            this.sessionLoader = ApplicationManager.getApplication().getService(SessionLoader.class);
            this.sessionLoader.setClient(this.client);
            this.sessionLoader.addSessionCallbackListener(sessionListener);
            cdl.countDown();
        });
        try {
            cdl.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return;
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

    public void addAllTabs() {
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        ContentManager contentManager = toolWindow.getContentManager();

        // test case designer form
//        testCaseDesignerWindow = new TestCaseDesigner();
//        Disposer.register(this, testCaseDesignerWindow);
//        testDesignerContent =
//                contentFactory.createContent(testCaseDesignerWindow.getContent(), "JUnit Test Preview", false);
//        testDesignerContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        testDesignerContent.setIcon(UIUtils.UNLOGGED_ICON_DARK);
//        contentManager.addContent(testDesignerContent);

        stompWindow = new StompComponent(this);
        if (stompComponentThreadPool != null) {
            stompComponentThreadPool.shutdownNow();
        }
        stompComponentThreadPool = Executors.newScheduledThreadPool(2);
        stompComponentThreadPool.scheduleWithFixedDelay(stompWindow, 0, 1, TimeUnit.MILLISECONDS);

        stompWindowContent =
                contentFactory.createContent(stompWindow.getComponent(), "Live", false);
        stompWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        stompWindowContent.setIcon(UIUtils.ATOMIC_TESTS);
        contentManager.addContent(stompWindowContent, 0);
        contentManager.setSelectedContent(stompWindowContent);


        libraryToolWindow = new LibraryComponent(project);

        libraryWindowContent = contentFactory.createContent(
                libraryToolWindow.getComponent(), "Library", false);
        libraryWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        libraryWindowContent.setIcon(UIUtils.LIBRARY_ICON);
        contentManager.addContent(libraryWindowContent);


//        onboardingWindow = new UnloggedSDKOnboarding(this);
//
//        onboardingWindowContent = contentFactory.createContent(
//                onboardingWindow.getComponent(), "Setup", false);
//
//        onboardingWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        onboardingWindowContent.setIcon(UIUtils.UNLOGGED_SETUP);
//        contentManager.addContent(onboardingWindowContent);


        // stomp window
//        stompWindow = new StompComponent(this);
//        threadPoolExecutor.submit(stompWindow);
//        stompWindowContent =
//                contentFactory.createContent(stompWindow.getComponent(), "Stomp", false);
//        stompWindowContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
//        stompWindowContent.setIcon(UIUtils.ATOMIC_TESTS);
//        contentManager.addContent(stompWindowContent);

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
        connectionCheckerThreadPool.shutdownNow();
        if (stompComponentThreadPool != null) {
            stompComponentThreadPool.shutdownNow();
        }
        if (sessionLoader != null) {
            sessionLoader.removeListener(sessionListener);
        }
        if (client != null) {
            client.close();
            client = null;
        }
//        unloggedSdkApiAgent.close();
        if (currentState.getSessionInstance() != null) {
            try {
                sessionManager.closeSession(currentState.getSessionInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        UsageInsightTracker.getInstance().close();
    }

    public Project getProject() {
        return project;
    }

    public void attachRawView() {
        if (rawViewAdded) {
            return;
        }
        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(singleWindowContent);
        rawViewAdded = true;
    }

    public void methodFocussedHandler(final MethodAdapter method) {
        if (method == null || method.getContainingClass() == null) {
            return;
        }
        currentState.setCurrentMethod(method);
        if (stompWindow != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                stompWindow.onMethodFocussed(method);
            });
        }
        if (libraryToolWindow != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                libraryToolWindow.onMethodFocussed(method);
            });
        }
    }

    public void setLibraryFilterState(LibraryFilterState libraryFilterState) {
        libraryToolWindow.setLibraryFilterState(libraryFilterState);
    }

    public void showStompAndFilterForMethod(MethodAdapter method) {
        if (stompWindow == null) {
            InsidiousNotification.notifyMessage(
                    "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                    NotificationType.WARNING
            );
            return;
        }

        StompFilterModel stompFilterModel = configurationState.getFilterModel();
        stompFilterModel.setFollowEditor(false);
        stompFilterModel.clearIncluded();
        stompFilterModel.addClassAndSuperClasses(method.getContainingClass().getSource());
        stompFilterModel.getIncludedMethodNames().clear();
        stompFilterModel.getIncludedMethodNames().add(ApplicationManager.getApplication().runReadAction(
                (Computable<String>) method::getName));
        stompWindow.onMethodFocussed(null);
        stompWindow.onMethodFocussed(method);
        stompWindow.resetAndReload();
//        toolWindow.getContentManager().setSelectedContent(stompWindowContent, true);

    }

    public void showLibrary() {
        if (toolWindow == null) {
            initiateUI();
        } else {
            if (libraryWindowContent == null) {
                InsidiousNotification.notifyMessage(
                        "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                        NotificationType.WARNING
                );
                return;
            }
        }
        toolWindow.show();
        if (libraryWindowContent != null) {
            toolWindow.getContentManager().setSelectedContent(libraryWindowContent, true);
        }
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

    private void injectMocksInRunningProcess(Collection<DeclaredMock> allDeclaredMocks) {
        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.INJECT_MOCKS);
        agentCommandRequest.setDeclaredMocks(allDeclaredMocks);


        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                AgentCommandResponse<String> agentCommandResponse =
                        getSessionInstance().getAgent().executeCommand(
                                agentCommandRequest);
                logger.warn("agent command response - " + agentCommandResponse);
                InsidiousNotification.notifyMessage(
                        agentCommandResponse.getMessage(), NotificationType.INFORMATION

                );


                forceRedrawInlayHints();

            } catch (IOException e) {
                logger.warn("failed to execute command - " + e.getMessage(), e);
                InsidiousNotification.notifyMessage(
                        "Failed to inject mocks [" + e.getMessage() + "]", NotificationType.ERROR
                );
            }
        });

    }

    public void forceRedrawInlayHints() {
        final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();

        if (selectedEditor != null) {
            final VirtualFile file = selectedEditor.getFile();
            if (file != null) {
                final PsiFile psiFile = ReadAction.compute(
                        () -> PsiManager.getInstance(project).findFile(file));
                logger.warn("trigger for " + psiFile.getName());
                InlayHintsFactoryBridge.refreshInlayHints(psiFile, EditorFactoryImpl.getInstance()
                        .getAllEditors(), false);
            }
        }
    }

    private void removeMocksInRunningProcess(Collection<DeclaredMock> declaredMocks) {
        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.REMOVE_MOCKS);
        agentCommandRequest.setDeclaredMocks(declaredMocks);

        if (declaredMocks == null || declaredMocks.isEmpty()) {
            List<DeclaredMock> existingMocks = getAllDeclaredMocks();
            existingMocks.stream()
                    .map(e -> e.getSourceClassName() + "." + e.getFieldName())
                    .forEach(configurationState::markMockDisable);
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                AgentCommandResponse<String> agentCommandResponse = getSessionInstance().getAgent().executeCommand(
                        agentCommandRequest);
                logger.warn("agent command response - " + agentCommandResponse);
                InsidiousNotification.notifyMessage(
                        agentCommandResponse.getMessage(), NotificationType.INFORMATION
                );
                forceRedrawInlayHints();
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

        if (DumbService.getInstance(project).isDumb()) {
            InsidiousNotification.notifyMessage("Please try after ide indexing is complete", NotificationType.WARNING);
            return;
        }

        methodArgumentValueCache.addArgumentSet(agentCommandRequest);
        agentCommandRequest.setRequestAuthentication(
                this.getRequestAuthentication());

        String methodName = agentCommandRequest.getMethodName();
        if (methodName.startsWith("lambda$")) {
            methodName = methodName.split("\\$")[1];
        }
        MethodUnderTest methodUnderTest = new MethodUnderTest(
                methodName, agentCommandRequest.getMethodSignature(),
                0, agentCommandRequest.getClassName()
        );
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        atomicRecordService.checkPreRequisites();
        List<DeclaredMock> availableMocks = getDeclaredMocksFor(methodUnderTest);
        if (agentCommandRequest.getRequestType().equals(DIRECT_INVOKE)) {
            List<DeclaredMock> activeMocks = availableMocks
                    .stream()
//              .filter(e -> isFieldMockActive(e.getSourceClassName(), e.getFieldName()))
                    .filter(this::isMockEnabled)
                    .collect(Collectors.toList());

            agentCommandRequest.setDeclaredMocks(activeMocks);
        } else {
            Collection<DeclaredMock> enabledMock = agentCommandRequest.getDeclaredMocks();
            ArrayList<DeclaredMock> setMock = new ArrayList<>();

            for (DeclaredMock localMock : enabledMock) {
                if (availableMocks.contains(localMock)) {
                    setMock.add(localMock);
                }
            }
            agentCommandRequest.setDeclaredMocks(setMock);
        }

        String finalMethodName = methodName;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (DumbService.getInstance(project).isDumb()) {
                    InsidiousNotification.notifyMessage("Please try after ide indexing is complete",
                            NotificationType.WARNING);
                    return;
                }

                AgentCommandResponse<String> agentCommandResponse = getSessionInstance().getAgent().executeCommand(
                        agentCommandRequest);
                logger.warn("agent command response - " + agentCommandResponse);
                if (executionResponseListener != null) {
                    cachedGutterState.remove(methodUnderTest.getMethodHashKey());

                    // TODO: search by signature and remove loop
                    PsiMethod[] methodPsiList = ApplicationManager.getApplication().runReadAction(
                            (Computable<PsiMethod[]>) () -> {
                                if (DumbService.getInstance(project).isDumb()) {
                                    InsidiousNotification.notifyMessage("Please try after ide indexing is complete",
                                            NotificationType.WARNING);
                                    return new PsiMethod[0];
                                }

                                PsiClass aClass = JavaPsiFacade.getInstance(project)
                                        .findClass(agentCommandRequest.getClassName(),
                                                GlobalSearchScope.projectScope(project)
                                        );
                                if (aClass == null) {
                                    return new PsiMethod[0];
                                }
                                PsiMethod[] list = aClass.findMethodsByName(finalMethodName, true);
                                for (PsiMethod psiMethod : list) {
                                    if (psiMethod.getName().equals(finalMethodName)) {
                                        updateMethodHashForExecutedMethod(new JavaMethodAdapter(psiMethod));
                                    }
                                }
                                return list;
                            });
//                    triggerGutterIconReload();
                    executionResponseListener.onExecutionComplete(agentCommandRequest, agentCommandResponse);
                } else {
                    logger.warn("no body listening for the response");
                }
            } catch (IOException e) {
                logger.warn("failed to execute command - " + e.getMessage(), e);
            }

        });
    }

    public void executeMethodInRunningProcessSync(
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
            Collection<DeclaredMock> enabledMock = agentCommandRequest.getDeclaredMocks();
            ArrayList<DeclaredMock> setMock = new ArrayList<>();

            for (DeclaredMock localMock : enabledMock) {
                if (availableMocks.contains(localMock)) {
                    setMock.add(localMock);
                }
            }
            agentCommandRequest.setDeclaredMocks(setMock);
        }

        try {
            AgentCommandResponse<String> agentCommandResponse = getSessionInstance().getAgent().executeCommand(
                    agentCommandRequest);
            logger.warn("agent command response - " + agentCommandResponse);
            if (executionResponseListener != null) {
                cachedGutterState.remove(methodUnderTest.getMethodHashKey());
                executionResponseListener.onExecutionComplete(agentCommandRequest, agentCommandResponse);
            } else {
                logger.warn("no body listening for the response");
            }
        } catch (IOException e) {
            logger.warn("failed to execute command - " + e.getMessage(), e);
        }
    }

    private RequestAuthentication getRequestAuthentication() {
        RequestAuthentication requestAuthentication = new RequestAuthentication();

        PsiClass springUserDetailsClass = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiClass>) () -> JavaPsiFacade.getInstance(project)
                        .findClass("org.springframework.security.core.userdetails.UserDetails",
                                GlobalSearchScope.allScope(project)));
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
            PsiClass implementsPsi = (PsiClass) implementation;
            PsiClassType[] implementsListTypes = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiClassType[]>) implementsPsi::getImplementsListTypes);
            for (PsiClassType implementsListType : implementsListTypes) {
                if (implementsListType.getName().endsWith("UserDetails")) {
                    // yes match
                    String qualifiedName = ApplicationManager.getApplication().runReadAction(
                            (Computable<String>) implementsPsi::getQualifiedName);
                    if (qualifiedName == null) {
                        continue;
                    }
                    classNameOptions.add(qualifiedName);
                    break;
                }
            }
        }

        if (!classNameOptions.isEmpty()) {
            Optional<String> classNameNotFromSprint = classNameOptions.stream()
                    .filter(e -> !e.contains("springframework"))
                    .findFirst();
            String userAuthClassName = classNameNotFromSprint.orElseGet(() -> classNameOptions.get(0));
            requestAuthentication.setPrincipalClassName(userAuthClassName);
            PsiClassType typeInstance = PsiClassType.getTypeByName(userAuthClassName, project,
                    GlobalSearchScope.projectScope(project));
            String dummyValue = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> ClassUtils.createDummyValue(typeInstance, new ArrayList<>(), project));

            requestAuthentication.setPrincipal(dummyValue);
        }


        return requestAuthentication;
    }

    public TestCaseService getTestCaseService() {
        return testCaseService;
    }

    public void setSession(ExecutionSession executionSession, ServerMetadata serverMetadata) {

        SessionInstance currentSession = currentState.getSessionInstance();
        if (currentSession != null) {
            try {
                logger.info("Closing existing session: " + currentSession.getExecutionSession().getSessionId());
                sessionManager.closeSession(currentSession);
            } catch (Exception e) {
                logger.error("Failed to close existing session before opening new session: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        this.executionRecord.clear();
        this.methodHash.clear();
        this.classModifiedFlagMap.clear();
        logger.info("Loading new session: " + executionSession.getSessionId() + " => " + project.getName());
        final SessionInstance sessionInstance = sessionManager.createSessionInstance(executionSession, serverMetadata,
                project);
        currentState.setSessionInstance(sessionInstance);
        sessionInstance.addTestCandidateListener(this);

        client.setSessionInstance(sessionInstance);
        testCaseService = new TestCaseService(sessionInstance);


        if (!executionSession.getSessionId().equals("na")) {
            removeOnboardingTab();

            ApplicationManager.getApplication().invokeLater(() -> {
//                ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
//                ContentManager contentManager = toolWindow.getContentManager();

//                if (stompWindowContent != null) {
//                    contentManager.removeContent(stompWindowContent, true);
//                }

                stompWindow.setSession(sessionInstance);
                sessionInstance.removeAllScanEventListeners();
                sessionInstance.addSessionScanEventListener(stompWindow.getScanEventListener());
                stompWindow.loadNewCandidates();


            });

        }
    }

    public void openToolWindow() {
        if (toolWindow == null) {
            return;
        }
        toolWindow.show(null);
    }

    @Override
    public void onNewTestCandidateIdentified(int completedCount, int totalCount) {
//        if (stompWindow != null) {
//            ApplicationManager.getApplication().invokeLater(() -> {
//                stompWindow.loadNewCandidates();
//            });
//        }
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
                    if (methodCandidates != null && !methodCandidates.isEmpty()) {
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
                ApplicationManager.getApplication().runReadAction(
                        (Computable<String>) () -> getMethodArgsDescriptor(currentMethod)),
                candidateFilterType,
                loadCalls
        );
    }

    public ClassMethodAggregates getClassMethodAggregates(String qualifiedName) {
        SessionInstance sessionInstance = currentState.getSessionInstance();
        if (sessionInstance == null || sessionInstance.getExecutionSession().getSessionId().equals("na")) {
            return new ClassMethodAggregates();
        }
        return sessionInstance.getClassMethodAggregates(qualifiedName);
    }

    public ExecutionSession getCurrentExecutionSession() {
        if (currentState.getSessionInstance() == null) {
            return null;
        }
        return currentState.getSessionInstance().getExecutionSession();
    }

    public SessionInstance getSessionInstance() {
        return currentState.getSessionInstance();
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

    public String getMethodArgsDescriptor(MethodAdapter method) {
        ParameterAdapter[] methodParams = method.getParameters();
        StringBuilder methodArgumentsClassnames = new StringBuilder();
        boolean first = true;
        for (ParameterAdapter methodParam : methodParams) {
            if (!first) {
                methodArgumentsClassnames.append(",");
            }
            String canonicalText;
            PsiType paramType = methodParam.getType();
            canonicalText = paramType.getCanonicalText();
            if (paramType instanceof PsiPrimitiveType) {
                canonicalText = ApplicationManager.getApplication()
                        .runReadAction(
                                (Computable<String>) () -> ((PsiPrimitiveType) paramType).getKind().getBinaryName());
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

    public void removeOnboardingTab() {
        if (toolWindow == null || onboardingWindow == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    toolWindow.getContentManager().removeContent(onboardingWindowContent, true);
                    onboardingWindow = null;
                    onboardingWindowContent = null;
                });
    }

    public void showDiffEditor(SimpleDiffRequest request) {
        DiffManager.getInstance().showDiff(project, request);
    }

    public FileEditorManager showDesignerLiteForm(VirtualFile lightVirtualFile) {

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(lightVirtualFile, true);

        return fileEditorManager;
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
        addExecutionRecord(new AutoExecutorReportRecord(newDiffRecord,
                currentState.getSessionInstance().getProcessedFileCount(),
                currentState.getSessionInstance().getTotalFileCount()));
    }

    public void addExecutionRecord(AutoExecutorReportRecord result) {
        reportingService.addRecord(result);
    }

    public void focusAtomicTestsWindow() {
        if (this.atomicTestContent != null) {
            toolWindow.getContentManager().setSelectedContent(this.atomicTestContent);
            toolWindow.show(null);
        }
    }

    public boolean isAgentConnected() {
        return currentState.isAgentServerRunning();
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

    public void toggleReportGeneration() {
        this.reportingService.toggleReportMode();
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

    public String saveMockDefinition(DeclaredMock declaredMock) {
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        String mockId = atomicRecordService.saveMockDefinition(declaredMock);
        reloadLibrary();
        enableMock(declaredMock);
        return mockId;
    }

    public void deleteMockDefinition(DeclaredMock declaredMock) {
        disableMock(declaredMock);
        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);
        atomicRecordService.deleteMockDefinition(declaredMock);
    }

    public void disableMock(DeclaredMock declaredMock) {
        configurationState.removeMock(declaredMock.getId());
        removeMocksInRunningProcess(List.of(declaredMock));
    }

    public void disableMock(Collection<DeclaredMock> declaredMock) {
        removeMocksInRunningProcess(declaredMock);
        for (DeclaredMock mock : declaredMock) {
            configurationState.removeMock(mock.getId());
        }
    }

    public void enableMock(DeclaredMock declaredMock) {
        configurationState.addMock(declaredMock.getId());
        injectMocksInRunningProcess(List.of(declaredMock));
    }

    public void enableMock(Collection<DeclaredMock> declaredMock) {
        for (DeclaredMock mock : declaredMock) {
            configurationState.addMock(mock.getId());
        }
        injectMocksInRunningProcess(declaredMock);
    }

    public boolean isMockEnabled(DeclaredMock declaredMock) {
        return configurationState.isMockActive(declaredMock.getId());
    }

    public void executeAllMethodsInCurrentClass() {
        automaticExecutorService.executeAllJavaMethodsInProject();
    }

    public void onAgentConnected(ServerMetadata serverMetadata) {
        JSONObject properties = new JSONObject();
        properties.put("agentVersion", serverMetadata.getAgentVersion());
        properties.put("package", serverMetadata.getIncludePackageName());
        properties.put("project", project.getName());

        UsageInsightTracker.getInstance().RecordEvent("AGENT_CONNECTED", properties);

        logger.info("unlogged agent connected - " + serverMetadata);

        if (stompWindow != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
//                stompWindow.resetTimeline();
//                stompWindow.setConnectedAndWaiting();
            });

            SemanticVersion currentVersion = new SemanticVersion(serverMetadata.getAgentVersion());
            SemanticVersion requiredVersion = new SemanticVersion(Constants.AGENT_VERSION);
            if (currentVersion.isBelow(requiredVersion)) {
                stompWindow.showVersionBadge(currentVersion, requiredVersion);
            }

        }

//        triggerGutterIconReload();
    }

    public void onAgentDisconnected() {
        UsageInsightTracker.getInstance().RecordEvent("AGENT_DISCONNECTED", null);

        if (libraryToolWindow != null) {
            libraryToolWindow.setMockStatus(false);
        }
        if (stompWindow != null) {
            stompWindow.getScanEventListener().ended();
        }

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

    public TestCandidateMetadata getTestCandidateById(long eventId, boolean loadCalls) {
        SessionInstance sessionInstance = getSessionInstance();
        TestCandidateMetadata testCandidateMetadata = sessionInstance.getTestCandidateById(eventId, loadCalls);
        if (testCandidateMetadata == null) {
            return null;
        }

        TestCandidateMetadata tm = ApplicationManager.getApplication()
                .runReadAction((Computable<TestCandidateMetadata>) () -> {
                    ClassUtils.resolveTemplatesInCall(testCandidateMetadata.getMainMethod(), project);
                    return testCandidateMetadata;
                });
        // check if the param are ENUM
        sessionInstance.createParamEnumPropertyTrueIfTheyAre(testCandidateMetadata.getMainMethod());

        if (loadCalls) {
            for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
                MethodCallExpression finalMethodCallExpression = methodCallExpression;
                methodCallExpression = ApplicationManager.getApplication().runReadAction(
                        (Computable<MethodCallExpression>) () -> ClassUtils.resolveTemplatesInCall(
                                finalMethodCallExpression, project));
                sessionInstance.createParamEnumPropertyTrueIfTheyAre(methodCallExpression);
            }
        }


        return testCandidateMetadata;
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

    private StoredCandidateMetadata.CandidateStatus getStatusForState(DiffResultType type) {
        switch (type) {
            case SAME:
            case NO_ORIGINAL:
                return StoredCandidateMetadata.CandidateStatus.PASSING;
            default:
                return StoredCandidateMetadata.CandidateStatus.FAILING;
        }
    }

    public boolean isMockingEnabled() {
        return true;
    }

    public void onMethodCallExpressionInlayClick(List<PsiMethodCallExpression> mockableCallExpressions, MouseEvent mouseEvent, Point point) {

        logger.warn("inlay clicked create mock");
        if (toolWindow == null) {
            initiateUI();
        }

        LibraryFilterState libraryFilerModel = new LibraryFilterState();

        for (PsiMethodCallExpression mockableCallExpression : mockableCallExpressions) {
            MethodUnderTest mut = MethodUnderTest.fromPsiCallExpression(mockableCallExpression);
            libraryFilerModel.getIncludedMethodNames().add(mut.getName());
            libraryFilerModel.getIncludedClassNames().add(mut.getClassName());
        }

        libraryFilerModel.setItemFilterType(ItemFilterType.SavedMocks);

        showLibrary();
        if (libraryToolWindow != null) {
            libraryToolWindow.setLibraryFilterState(libraryFilerModel);
            toolWindow.getContentManager().setSelectedContent(libraryWindowContent);
        }

    }

    public void showMockEditor(DeclaredMock declaredMock, OnSaveListener onSaveListener) {
        showLibrary();
        libraryToolWindow.showMockEditor(declaredMock, onSaveListener);
    }

    public void showMockCreator(JavaMethodAdapter method, PsiMethodCallExpression callExpression, OnSaveListener onSaveListener) {
        if (stompWindow == null) {
            if (toolWindow == null) {
                initiateUI();
            } else {
                if (stompWindowContent == null) {
                    InsidiousNotification.notifyMessage(
                            "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                            NotificationType.WARNING
                    );
                    return;
                }
            }
        }


        ApplicationManager.getApplication().invokeLater(() -> {
            toolWindow.show();
            if (stompWindowContent != null) {
                toolWindow.getContentManager().setSelectedContent(stompWindowContent, true);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    stompWindow.showNewDeclaredMockCreator(method, callExpression, onSaveListener);
                    stompWindow.onMethodFocussed(method);
                });
            }
        });

    }

    public void showRouterForMethod(MethodAdapter method) {
        if (stompWindow == null) {
            if (toolWindow == null) {
                initiateUI();
            } else {
                if (stompWindowContent == null) {
                    InsidiousNotification.notifyMessage(
                            "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                            NotificationType.WARNING
                    );
                    return;
                }
            }
        }
        if (stompWindow != null) {
            stompWindow.onMethodFocussed(method);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            toolWindow.show();
            if (stompWindowContent != null) {
                stompWindow.showRouterForMethod(method);
                toolWindow.getContentManager().setSelectedContent(stompWindowContent, true);
            }
        });


    }

    public void reloadLibrary() {
        libraryToolWindow.reloadItems();
    }


    public void highlightTimingInformation(TestCandidateMetadata candidateMetadata, MethodUnderTest methodUnderTest) {


        List<UnloggedTimingTag> timingTags = getSessionInstance()
                .getTimingTags(candidateMetadata.getMainMethod().getId());
//        if (availableTimingTags.containsKey(methodUnderTest)) {
//            availableTimingTags.remove(methodUnderTest);
//        } else {
        availableTimingTags.put(methodUnderTest, timingTags);
//        }

        forceRedrawInlayHints();

    }

    public List<UnloggedTimingTag> getTimingInformation(MethodUnderTest methodUnderTest) {
        return availableTimingTags.get(methodUnderTest);
    }

    public void removeTimingInformation() {
        availableTimingTags.clear();
        forceRedrawInlayHints();
    }

    public void populateFromEditors(EditorFactoryEvent event) {
        Editor[] allEditors = ApplicationManager.getApplication().getService(EditorFactory.class)
                .getAllEditors();

        Editor skip = null;
        if (event != null) {
            skip = event.getEditor();
        }

        List<Editor> selected = new ArrayList<>();
        for (Editor editor : allEditors) {
            if (editor.getProject() == null) {
                continue;
            }
            if (editor.getProject().equals(project) && editor.getEditorKind().equals(EditorKind.MAIN_EDITOR)) {
                if (skip != null && skip.equals(editor)) {
                    continue;
                }
                selected.add(editor);

            }
        }
        InsidiousConfigurationState ics = getProject().getService(InsidiousConfigurationState.class);
        StompFilterModel stompFilterModel = ics.getFilterModel();
        if (!stompFilterModel.isFollowEditor()) {
            return;
        }
        boolean changed = false;


        stompFilterModel.clearIncluded();

        for (Editor editor : selected) {
            if (editor.getEditorKind().equals(EditorKind.MAIN_EDITOR)) {


                PsiFile psiFile = ApplicationManager.getApplication().runReadAction(
                        (Computable<PsiFile>) () -> PsiDocumentManager.getInstance(project)
                                .getPsiFile(editor.getDocument()));
                if (!(psiFile instanceof PsiJavaFile)) {
                    continue;
                }

                Collection<PsiClass> classes = ApplicationManager.getApplication().runReadAction(
                        (Computable<Collection<PsiClass>>) () -> PsiTreeUtil.findChildrenOfType(psiFile,
                                PsiClass.class));

                for (PsiClass psiClass : classes) {
                    changed = stompFilterModel.addFromClassRecursively(psiClass) || changed;
                }
            }
        }
        stompWindow.resetAndReload();
    }

    public void hideBottomSplit() {
        stompWindow.hideBottomSplit();
    }

    public void createJunitFromSelectedReplay() {
        List<TestCandidateBareBone> selectedCandidates = stompWindow.getSelectedCandidates();


    }

    public int selectVisibleCandidates() {
        return stompWindow.selectVisibleCandidates();
    }

    public List<TestCandidateBareBone> getSelectedCandidates() {
        return stompWindow.getSelectedCandidates();
    }

    public List<TestCandidateMetadata> getCandidatesForMethod(MethodAdapter methodElement) {
        StompFilterModel filterModel = new StompFilterModel();
        filterModel.addFromClassRecursively(methodElement.getContainingClass().getSource());
        filterModel.getIncludedMethodNames().clear();
        filterModel.getIncludedMethodNames().add(methodElement.getName());
        List<TestCandidateBareBone> candidateBones = currentState.getSessionInstance()
                .getTestCandidatePaginatedByStompFilterModel(filterModel, 0, 10);
        return candidateBones.stream().map(e -> getTestCandidateById(e.getId(), true)).collect(Collectors.toList());

    }
}
