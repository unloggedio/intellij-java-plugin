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
import com.insidious.plugin.atomicrecord.AtomicRecordService;
import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.coverage.*;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.dao.MethodDefinition;
import com.insidious.plugin.ui.*;
import com.insidious.plugin.ui.assertions.SaveForm;
import com.insidious.plugin.ui.eventviewer.SingleWindowView;
import com.insidious.plugin.ui.methodscope.*;
import com.insidious.plugin.ui.testdesigner.JUnitTestCaseWriter;
import com.insidious.plugin.ui.testdesigner.TestCaseDesigner;
import com.insidious.plugin.util.ClassUtils;
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
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
//import io.unlogged.Unlogged;
import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.insidious.plugin.util.AtomicRecordUtils.filterStoredCandidates;

@Storage("insidious.xml")
final public class InsidiousService implements
        Disposable, NewTestCandidateIdentifiedListener,
        GutterStateProvider, ConnectionStateListener {
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();
    final static private int TOOL_WINDOW_HEIGHT = 430;
    final static private int TOOL_WINDOW_WIDTH = 500;
    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(5);
    private final AgentClient agentClient;
    private final SessionLoader sessionLoader;
    private final Map<String, DifferenceResult> executionRecord = new TreeMap<>();
    private final Map<String, Integer> methodHash = new TreeMap<>();
    private final DefaultMethodArgumentValueCache methodArgumentValueCache = new DefaultMethodArgumentValueCache();
    final private AgentStateProvider agentStateProvider;
    private final ReportingService reportingService = new ReportingService(this);
    private final Map<String, String> candidateIndividualContextMap = new TreeMap<>();
    private final ActiveSessionManager sessionManager;
    private final JUnitTestCaseWriter junitTestCaseWriter;
    private final Map<String, Boolean> classModifiedFlagMap = new HashMap<>();
    private final Map<SaveForm, FileEditor> saveFormEditorMap = new HashMap<>();
    private final InsidiousConfigurationState configurationState;
    private final ApplicationConfigurationState applicationConfigurationState;
    private ActiveHighlight currentActiveHighlight = null;
    private Project project;
    private VideobugClientInterface client;
    private Module currentModule;
    private ToolWindow toolWindow;
    private Content singleWindowContent;
    private boolean rawViewAdded = false;
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
    private AtomicRecordService atomicRecordService;
    private CoverageReportComponent coverageReportComponent;
    private boolean codeCoverageHighlightEnabled = true;
    private HighlightedRequest currentHighlightedRequest = null;
    private boolean testCaseDesignerWindowAdded = false;
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
        this.sessionLoader = new SessionLoader(client, this);
        threadPoolExecutor.submit(sessionLoader);


        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        InsidiousCaretListener listener = new InsidiousCaretListener();
        multicaster.addEditorMouseListener(listener, this);
        agentStateProvider = new DefaultAgentStateProvider(this);
        agentClient = new AgentClient("http://localhost:12100", (ConnectionStateListener) agentStateProvider);
        junitTestCaseWriter = new JUnitTestCaseWriter(project, objectMapper);
        configurationState = project.getService(InsidiousConfigurationState.class);
        applicationConfigurationState =
                ApplicationManager.getApplication().getService(ApplicationConfigurationState.class);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            ToolWindow unlogged = ToolWindowManager.getInstance(project).getToolWindow("Unlogged");
            if (unlogged != null) {
                this.init(this.project, unlogged);
            } else {
                project.getService(DumbService.class)
                        .runWhenSmart(() -> {
                            this.init(this.project, ToolWindowManager.getInstance(project).getToolWindow("Unlogged"));
                        });
            }
        });
    }

    //    @Unlogged
    public static void main(String[] args) {

    }

    private static String getClassMethodHashKey(AgentCommandRequest agentCommandRequest) {
        return agentCommandRequest.getClassName() + "#" + agentCommandRequest.getMethodName() + "#" + agentCommandRequest.getMethodSignature();
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
            this.atomicRecordService.init();
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

    public void copyToClipboard(String string) {
        StringSelection selection = new StringSelection(string);
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        clipboard.setContents(selection, null);
//        logger.info(selection);
    }

//    @Unlogged
    public synchronized void init(Project project, ToolWindow toolWindow) {
        if (toolWindow == null) {
            return;
        }
        if (this.initiated) {
            return;
        }
        this.initiated = true;
        this.project = project;
        this.toolWindow = toolWindow;
        start();
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

    public synchronized void previewTestCase(MethodAdapter methodElement, TestCaseGenerationConfiguration generationConfiguration) {

        if (!testCaseDesignerWindowAdded) {
            ContentManager contentManager = toolWindow.getContentManager();
            contentManager.addContent(testDesignerContent);
            testCaseDesignerWindowAdded = true;
        }

        UsageInsightTracker.getInstance().RecordEvent(
                "CREATE_JUNIT_TEST",
                null
        );
        testCaseDesignerWindow.generateAndPreviewTestCase(generationConfiguration, methodElement);
        focusTestCaseDesignerTab();
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

        if (!configurationState.hasShownFeatures()) {
            configurationState.setShownFeatures();
            IntroductionPanel introPanel = new IntroductionPanel(this);
            introPanelContent =
                    contentFactory.createContent(introPanel.getContent(), "Unlogged Features", false);
            introPanelContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
            introPanelContent.setIcon(UIUtils.ONBOARDING_ICON_PINK);
            contentManager.addContent(introPanelContent);
        } else {
            addAllTabs();
        }


    }

    public void addAllTabs() {

        if (testCaseDesignerWindow != null) {
            return;
        }
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        ContentManager contentManager = this.toolWindow.getContentManager();

        // test case designer form
        testCaseDesignerWindow = new TestCaseDesigner();
        Disposer.register(this, testCaseDesignerWindow);
        testDesignerContent =
                contentFactory.createContent(testCaseDesignerWindow.getContent(), "JUnit Test Preview", false);
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
        } else {
            atomicTestContainerWindow.loadComponentForState(GutterState.PROCESS_NOT_RUNNING);
        }


        methodDirectInvokeComponent = new MethodDirectInvokeComponent(this);
        this.directMethodInvokeContent =
                contentFactory.createContent(methodDirectInvokeComponent.getContent(), "Direct Invoke", false);
        this.directMethodInvokeContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        this.directMethodInvokeContent.setIcon(UIUtils.EXECUTE_METHOD);
        contentManager.addContent(this.directMethodInvokeContent);


        SingleWindowView singleWindowView = new SingleWindowView(project, this);
        singleWindowContent = contentFactory.createContent(singleWindowView.getContent(),
                "Raw Cases", false);


        coverageReportComponent = new CoverageReportComponent();
        Content coverageComponent = contentFactory.createContent(coverageReportComponent.getContent(),
                "Coverage", false);

        coverageComponent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        coverageComponent.setIcon(UIUtils.COVERAGE_TOOL_WINDOW_ICON);
        contentManager.addContent(coverageComponent);

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
        UsageInsightTracker.getInstance().close();
    }

    public void generateAndUploadReport() throws APICallException, IOException {
        UsageInsightTracker.getInstance()
                .RecordEvent("DiagnosticReport", null);
        DiagnosticService diagnosticService = new DiagnosticService(new VersionManager(), this.project,
                this.currentModule);
        diagnosticService.generateAndUploadReport();
    }

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
        ContentManager contentManager = this.toolWindow.getContentManager();
        contentManager.addContent(singleWindowContent);
        rawViewAdded = true;
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
        DumbService dumbService = project.getService(DumbService.class);
        if (dumbService.isDumb()) {
            return;
        }

        currentMethod = method;
        MethodUnderTest currentMethodUnderTest = MethodUnderTest.fromMethodAdapter(method);
        final ClassAdapter psiClass;
        try {
            psiClass = method.getContainingClass();
        } catch (Exception e) {
            // not a focusable element. return silently
            return;
        }

        if (psiClass.getName() == null) {
            return;
        }


        if (!this.toolWindow.isVisible()) {
            logger.warn("test case designer window is not ready to create test case");
            return;
        }
        if (methodDirectInvokeComponent == null) {
            return;
        }

        methodDirectInvokeComponent.renderForMethod(method);
        atomicTestContainerWindow.triggerMethodExecutorRefresh(method);
//        testCaseDesignerWindow.renderTestDesignerInterface(method);
    }

    public void compile(ClassAdapter psiClass, CompileStatusNotification compileStatusNotification) {
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


    }

    public AgentCommandRequest getAgentCommandRequests(AgentCommandRequest agentCommandRequest) {
        return methodArgumentValueCache.getArgumentSets(agentCommandRequest);
    }

    public void injectMocksInRunningProcess(List<DeclaredMock> allDeclaredMocks) {
        if (allDeclaredMocks == null || allDeclaredMocks.size() == 0) {
            allDeclaredMocks = getAllDeclaredMocks();
            allDeclaredMocks.stream()
                    .map(e -> e.getSourceClassName() + "." + e.getFieldName())
                    .forEach(configurationState::addFieldMock);
        }
        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.INJECT_MOCKS);
        agentCommandRequest.setDeclaredMocks(allDeclaredMocks);


        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            try {
                AgentCommandResponse<String> agentCommandResponse = agentClient.executeCommand(agentCommandRequest);
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
                AgentCommandResponse<String> agentCommandResponse = agentClient.executeCommand(agentCommandRequest);
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

        List<DeclaredMock> availableMocks = getDeclaredMocksFor(new MethodUnderTest(
                agentCommandRequest.getMethodName(), agentCommandRequest.getMethodSignature(),
                0, agentCommandRequest.getClassName()
        ));

        List<DeclaredMock> activeMocks = availableMocks
                .stream()
//                    .filter(e -> isFieldMockActive(e.getSourceClassName(), e.getFieldName()))
                .filter(this::isMockEnabled)
                .collect(Collectors.toList());

        agentCommandRequest.setDeclaredMocks(activeMocks);

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

    public TestCaseService getTestCaseService() {
        if (testCaseService == null) {
            loadDefaultSession();
        }
        return testCaseService;
    }

    public synchronized void setSession(ExecutionSession executionSession) throws SQLException, IOException {

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
        sessionInstance.addTestCandidateListener(this);
        client.setSessionInstance(sessionInstance);
        testCaseService = new TestCaseService(sessionInstance);

    }

    public void openToolWindow() {
        toolWindow.show(null);
    }


    @Override
    public void onNewTestCandidateIdentified(int completedCount, int totalCount) {
        logger.warn("new test cases identified [" + completedCount + "/" + totalCount + "] => " + project.getName());
        DumbService dumbService = DumbService.getInstance(project);
        if (dumbService.isDumb()) {
            dumbService.runWhenSmart(() -> {
                onNewTestCandidateIdentified(completedCount, totalCount);
            });
            return;
        }

        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();


        Editor[] currentOpenEditorsList = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : currentOpenEditorsList) {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (virtualFile instanceof LightVirtualFile) {
                continue;
            }

            PsiFile psiFile = ApplicationManager.getApplication().runReadAction(
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

        ApplicationManager.getApplication().invokeLater(() -> {
            atomicTestContainerWindow.triggerMethodExecutorRefresh(currentMethod);
            methodDirectInvokeComponent.renderForMethod(currentMethod);
        });

    }

    public void updateCoverageReport() {
        if (coverageReportComponent == null || sessionInstance == null) {
            return;
        }
        if (sessionInstance.getProject() != project) {
            return;
        }
        CodeCoverageData coverageData = sessionInstance.createCoverageData();

        List<PackageCoverageData> updatedPackageList = new ArrayList<>();

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


    public CandidateSearchQuery createSearchQueryForMethod(MethodAdapter currentMethod) {
        return CandidateSearchQuery.fromMethod(currentMethod,
                getInterfacesWithSameSignature(currentMethod),
                getMethodArgsDescriptor(currentMethod));
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
            logger.error("Failed to set default session: " + e.getMessage(), e);
            InsidiousNotification.notifyMessage("Failed to set default session: " + e.getMessage(),
                    NotificationType.ERROR);
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

        CandidateSearchQuery query = createSearchQueryForMethod(method);
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(method);

        List<TestCandidateMetadata> candidates = getTestCandidateMetadata(query);

        //check for stored candidates here
        boolean hasStoredCandidates = atomicRecordService.hasStoredCandidateForMethod(methodUnderTest);


        GutterState gutterState = atomicRecordService.computeGutterState(methodUnderTest);

        // process is running, but no test candidates for this method
        if (candidates.size() == 0 && !hasStoredCandidates) {
            return GutterState.PROCESS_RUNNING;
        }

        // process is running, and there were test candidates for this method
        // so check if we have executed this before

        //check for change
        final String methodHashKey = methodUnderTest.getMethodHashKey();

        // we havent checked anything for this method earlier
        // store method hash for diffs
        String methodText = method.getText();
        if (!this.methodHash.containsKey(methodHashKey)) {
            //register new hash
            this.methodHash.put(methodHashKey, methodText.hashCode());
        }

        int lastHash = this.methodHash.get(methodHashKey);
        int currentHash = methodText.hashCode();

        if (lastHash != currentHash) {
            //re-execute as there are hash diffs
            //update hash after execution is complete for this method,
            //to prevent state change before exec complete.
            classModifiedFlagMap.put(methodUnderTest.getClassName(), true);
            ApplicationManager.getApplication().invokeLater(() -> highlightLines(currentHighlightedRequest));
            return GutterState.EXECUTE;
        }

        if (!executionRecord.containsKey(methodHashKey) && hasStoredCandidates && gutterState != null) {
            return gutterState;
        } else {
            if (!executionRecord.containsKey(methodHashKey)) {
                return GutterState.DATA_AVAILABLE;
            }
        }

        DifferenceResult differenceResult = executionRecord.get(methodHashKey);

        if (this.candidateIndividualContextMap.get(methodHashKey) != null &&
                differenceResult.isUseIndividualContext()) {
            logger.info("Using flow ind : " + this.candidateIndividualContextMap.get(methodHashKey));
            switch (this.candidateIndividualContextMap.get(methodHashKey)) {
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

    public List<TestCandidateMetadata> getTestCandidateMetadata(CandidateSearchQuery candidateSearchQuery) {
        if (candidateSearchQuery == null) {
            return List.of();
        }
        if (DumbService.getInstance(project).isDumb()) {
            return List.of();
        }
        List<TestCandidateMetadata> candidates = sessionInstance.getTestCandidatesForAllMethod(candidateSearchQuery);


        for (String interfaceName : candidateSearchQuery.getInterfaceNames()) {
            CandidateSearchQuery interfaceSearchQuery =
                    CandidateSearchQuery.cloneWithNewClassName(candidateSearchQuery, interfaceName);
            List<TestCandidateMetadata> interfaceCandidates = sessionInstance.getTestCandidatesForAllMethod(
                    interfaceSearchQuery);
            candidates.addAll(interfaceCandidates);

        }


        return candidates;
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


        List<TestCandidateMetadata> candidateMetadataList = getTestCandidateMetadata(candidateSearchQuery);
        MethodUnderTest methodUnderTest = MethodUnderTest.fromCandidateSearchQuery(candidateSearchQuery);

        List<StoredCandidate> candidates = atomicRecordService.getStoredCandidatesForMethod(methodUnderTest);

        List<StoredCandidate> storedCandidates = new ArrayList<>(candidates);

        candidateMetadataList.stream()
                .map(StoredCandidate::new)
                .forEach(storedCandidates::add);

//        logger.info("StoredCandidates pre filter for " + method.getName() + " -> " + storedCandidates);
        FilteredCandidateResponseList filterStoredCandidates = filterStoredCandidates(storedCandidates);
        List<String> updatedCandidateIds = filterStoredCandidates.getUpdatedCandidateIds();
        if (updatedCandidateIds.size() > 0) {
            atomicRecordService.setUseNotifications(false);
            // because we are dealing with objects
            // and line numbers are changed in the objects originally returned
            // so they are changed in the cache map of the ARS
            // so updating just one record by this call will actually persist all the changes we have made
            atomicRecordService.saveCandidate(methodUnderTest, filterStoredCandidates.getCandidateList().get(0));
//            filterStoredCandidates.getCandidateList().stream()
//                    .filter(e -> updatedCandidateIds.contains(e.getCandidateId()))
//                    .forEach(e -> atomicRecordService.saveCandidate(methodUnderTest, e));
            atomicRecordService.setUseNotifications(true);
        }
        return filterStoredCandidates.getCandidateList();
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

    public void focusTestCaseDesignerTab() {
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
                    candidate.getMethod().getClassName(), this);
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
        reportingService.addRecord(result);
    }

    public void setAgentProcessState(GutterState newState) {
        if (this.atomicTestContainerWindow != null) {
            atomicTestContainerWindow.loadComponentForState(newState);
        }
    }

    public void compileAndExecuteWithAgentForMethod(JavaMethodAdapter methodAdapter) {
        atomicTestContainerWindow.triggerMethodExecutorRefresh(methodAdapter);
        atomicTestContainerWindow.triggerCompileAndExecute();
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
        configurationState.clearPermanentFieldMockSetting();
//        methodDirectInvokeComponent.uncheckPermanentMocks();
    }

    public void clearAtomicBoard() {
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

    public Map<String, String> getIndividualCandidateContextMap() {
        return this.candidateIndividualContextMap;
    }

    public AtomicRecordService getAtomicRecordService() {
        return this.atomicRecordService;
    }

    public void toggleReportGeneration() {
        this.reportingService.toggleReportMode();
    }

    public MethodDefinition getMethodInformation(MethodUnderTest methodUnderTest) {
        return sessionInstance.getMethodDefinition(methodUnderTest);
    }

    public void clearClassModifiedMap() {
        classModifiedFlagMap.clear();
    }

    public void highlightLines(HighlightedRequest highlightRequest) {
        if (highlightRequest == null) {
            return;
        }

        if (currentHighlightedRequest != null && classModifiedFlagMap.containsKey(
                currentHighlightedRequest.getMethodUnderTest().getClassName())) {
            removeCurrentActiveHighlights();
            currentHighlightedRequest = null;
        }

        if (classModifiedFlagMap.containsKey(highlightRequest.getMethodUnderTest().getClassName())) {
            return;
        }


        // no new highlight if disabled or tool window is hidden
        if (!codeCoverageHighlightEnabled || (toolWindow == null || !toolWindow.isVisible()) || !agentStateProvider.isAgentRunning()) {
            currentHighlightedRequest = highlightRequest;
            removeCurrentActiveHighlights();
            return;
        }
        if (Objects.equals(highlightRequest, currentHighlightedRequest) && currentActiveHighlight != null) {
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
        currentActiveHighlight = new ActiveHighlight(newHighlightList, selectedTextEditor);

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
            currentActiveHighlight = null;
        }
    }

    public void setCodeCoverageHighlightEnabled(boolean state) {
        this.codeCoverageHighlightEnabled = state;
        highlightLines(currentHighlightedRequest);
    }

    public ToolWindow getToolWindow() {
        return toolWindow;
    }

    public HighlightedRequest getCurrentHighlightRequest() {
        return currentHighlightedRequest;
    }

    public JUnitTestCaseWriter getJUnitTestCaseWriter() {
        return junitTestCaseWriter;
    }

    public String guessModuleBasePath(ClassAdapter currentClass) {
        return atomicRecordService.guessModuleBasePath((PsiClass) currentClass.getSource());
    }

    public List<DeclaredMock> getDeclaredMocksOf(MethodUnderTest methodUnderTest) {
        return atomicRecordService.getDeclaredMocksOf(methodUnderTest);
    }

    public List<DeclaredMock> getDeclaredMocksFor(MethodUnderTest methodUnderTest) {
        return atomicRecordService.getDeclaredMocksFor(methodUnderTest);
    }

    public List<DeclaredMock> getAllDeclaredMocks() {
        return atomicRecordService.getAllDeclaredMocks();
    }

    public void saveMockDefinition(DeclaredMock declaredMock, MethodUnderTest methodUnderTest) {
        atomicRecordService.saveMockDefinition(methodUnderTest, declaredMock);
        if (!isMockEnabled(declaredMock)) {
            enableMock(declaredMock);
        } else if (isFieldMockActive(declaredMock.getSourceClassName(), declaredMock.getFieldName())) {
            injectMocksInRunningProcess(List.of(declaredMock));
        }
    }

    public void deleteMockDefinition(MethodUnderTest methodUnderTest, DeclaredMock declaredMock) {
        disableMock(declaredMock);
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
        removeMocksInRunningProcess(List.of(declaredMock));
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
}
