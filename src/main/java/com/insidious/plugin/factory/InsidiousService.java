package com.insidious.plugin.factory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.insidious.plugin.Constants;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.VideobugNetworkClient;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.SigninRequest;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.extension.InsidiousExecutor;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.InsidiousRunConfigType;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.factory.callbacks.SearchResultsCallbackHandler;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.ui.*;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.visitor.GradleFileVisitor;
import com.insidious.plugin.visitor.PomFileVisitor;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class InsidiousService implements Disposable {
    public static final String HOSTNAME = System.getProperty("user.name");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String DEFAULT_PACKAGE_NAME = "YOUR.PACKAGE.NAME";
    private TestCaseService testCaseService;
    private Project project;
    private InsidiousConfigurationState insidiousConfiguration;
    private VideobugClientInterface client;
    private Module currentModule;
    private String packageName = "YOUR.PACKAGE.NAME";
    private SearchByTypesWindow searchByTypesWindow;
    private SearchByValueWindow searchByValueWindow;
    private SingleWindowView singleWindowView;
    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private InsidiousJDIConnector connector;
    private ConfigurationWindow credentialsToolbarWindow;
    private ToolWindow toolWindow;
    private CredentialAttributes insidiousCredentials;
    private String appToken;
    private String javaAgentString;
    private TracePoint pendingTrace;
    private TracePoint pendingSelectTrace;
    private AboutUsWindow aboutUsWindow;
    private LiveViewWindow liveViewWindow;

    public InsidiousService(Project project) {
        try {


            this.project = project;
            ExecutorService threadPool = Executors.newFixedThreadPool(4);

            logger.info("started insidious service - project name - " + project.getName());
            if (ModuleManager.getInstance(project).getModules().length == 0) {
                logger.warn("no module found in the project");
            } else {
                currentModule = ModuleManager.getInstance(project).getModules()[0];
                logger.info("current module - " + currentModule.getName());
            }

            String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions";
            Path.of(pathToSessions).toFile().mkdirs();
            this.client = new VideobugLocalClient(pathToSessions);
            this.testCaseService = new TestCaseService(client.getSessionInstance());
            this.insidiousConfiguration = project.getService(InsidiousConfigurationState.class);

            debugSession = getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions());

            ReadAction.run(this::getProjectPackageName);
//            threadPool.submit(this::startDebugSession);

            ReadAction.run(InsidiousService.this::checkAndEnsureJavaAgentCache);
            ReadAction.run(this::initiateUI);


        } catch (ServiceNotReadyException snre) {
            logger.info("service not ready exception -> " + snre.getMessage());
        } catch (ProcessCanceledException ignored) {
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("exception in videobug service init", e);
        }
    }

    private void setupNewExceptionListener() {
        Set<String> exceptionClassList = insidiousConfiguration.exceptionClassMap.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());
        this.client.onNewException(exceptionClassList, new VideobugExceptionCallback() {
            final Set<TracePoint> mostRecentTracePoints = new HashSet<>();

            @Override
            public void onNewTracePoints(Collection<TracePoint> tracePoints) {
                if (mostRecentTracePoints.size() > 0) {
                    List<TracePoint> newTracePoints = new LinkedList<>();
                    for (TracePoint tracePoint : tracePoints) {
                        if (!mostRecentTracePoints.contains(tracePoint)) {
                            newTracePoints.add(tracePoint);
                        }
                    }
                    mostRecentTracePoints.clear();
                    mostRecentTracePoints.addAll(tracePoints);
                    if (newTracePoints.size() == 0) {
                        return;
                    }
                    tracePoints = newTracePoints;
                } else {
                    mostRecentTracePoints.addAll(tracePoints);
                }

                String messageContent = "Got " + tracePoints.size() + " new matching trace points";
                StringBuilder messageBuilder = new StringBuilder();
                Map<String, List<TracePoint>> pointsByClass = tracePoints.stream().collect(Collectors.groupingBy(TracePoint::getClassname));
                for (Map.Entry<String, List<TracePoint>> classTracePoint : pointsByClass.entrySet()) {
                    String className = classTracePoint.getKey();
                    List<TracePoint> classTracePoints = classTracePoint.getValue();
                    Map<String, List<TracePoint>> tracePointsByException = classTracePoints.stream().collect(Collectors.groupingBy(TracePoint::getExceptionClass));

                    for (Map.Entry<String, List<TracePoint>> exceptionTracePoint : tracePointsByException.entrySet()) {
                        String exceptionClassName = exceptionTracePoint.getKey();
                        Map<Long, List<TracePoint>> pointsByLine = exceptionTracePoint.getValue().stream().collect(Collectors.groupingBy(TracePoint::getLineNumber));

                        for (Map.Entry<Long, List<TracePoint>> lineExceptionPoints : pointsByLine.entrySet()) {

                            String[] exceptionClassNameParts = exceptionClassName.split("\\.");
                            String[] classNameParts = className.split("/");
                            messageBuilder.append(lineExceptionPoints.getValue().size());
                            messageBuilder.append(" ");
                            messageBuilder.append(exceptionClassNameParts[exceptionClassNameParts.length - 1]);
                            messageBuilder.append(" on line ").append(lineExceptionPoints.getKey());
                            messageBuilder.append(" in ").append("<a>").append(classNameParts[classNameParts.length - 1]).append("</a>");
                            messageBuilder.append("<br>");
                        }


                    }


                }


//                    Notification notification = InsidiousNotification.
//                            balloonNotificationGroup.createNotification("New exception",
//                                    "These just happened",
//                                    messageBuilder.toString(), NotificationType.INFORMATION);
//                    Notifications.Bus.notify(notification);
//                    getHorBugTable().setTracePoints(tracePoints);
            }
        });
    }

    private XDebugSession getActiveDebugSession(XDebugSession[] debugSessions) {
        for (XDebugSession session : debugSessions) {
            if (session.getDebugProcess() instanceof InsidiousJavaDebugProcess) {
                return session;
            }
        }
        return null;
    }

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

        File insidiousFolder = new File(Constants.VIDEOBUG_HOME_PATH.toString());
        if (!insidiousFolder.exists()) {
            insidiousFolder.mkdir();
        }

        if (overwrite) {
            Constants.VIDEOBUG_AGENT_PATH.toFile().delete();
        }

        if (!Constants.VIDEOBUG_AGENT_PATH.toFile().exists() && !overwrite) {
            InsidiousNotification.notifyMessage("java agent does not exist, downloading to $HOME/.videobug/videobug-java-agent.jar. Please wait for download to finish.", NotificationType.INFORMATION);
        }

        if (Constants.VIDEOBUG_AGENT_PATH.toFile().exists()) {
            return;
        }


        client.getAgentDownloadUrl(new AgentDownloadUrlCallback() {
            @Override
            public void error(String error) {
                logger.error("failed to get url from server to download the java agent - " + error);
                agentJarDownloadCompleteCallback.error("failed to get url from server to download the java agent - " + error);
            }

            @Override
            public void success(String url) {
                try {
                    logger.info("agent download link: " + url + ", downloading to path " + Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath());
                    client.downloadAgentFromUrl(url, Constants.VIDEOBUG_AGENT_PATH.toString(), overwrite);
                    setAppTokenOnUi();
                    agentJarDownloadCompleteCallback.success(url, Constants.VIDEOBUG_AGENT_PATH.toString());
                } catch (Exception e) {
                    logger.info("failed to download agent - ", e);
                }
            }
        });

    }

    private void getProjectPackageName() {
        if (currentModule == null) {
            return;
        }
        logger.info("looking up package name for the module [" + currentModule.getName() + "]");
        @NotNull PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));
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

        @NotNull PsiFile[] gradleFileSearchResult = FilenameIndex.getFilesByName(project, "build.gradle", GlobalSearchScope.projectScope(project));
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

    public void init() {
        ApplicationManager.getApplication().invokeLater(this::initiateUI);

//        if (!StringUtil.isEmpty(insidiousConfiguration.getUsername())) {
//            logger.info("username is not empty in configuration - [" + insidiousConfiguration.getUsername() + "] with server url " + insidiousConfiguration.getServerUrl());
//            insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
//            if (insidiousCredentials != null) {
//                Credentials credentials = PasswordSafe.getInstance().get(insidiousCredentials);
//                if (credentials != null) {
//                    String password = credentials.getPasswordAsString();
//                    try {
//                        if (password != null) {
////                            signin(insidiousConfiguration.serverUrl, insidiousConfiguration.username, password);
//                        }
//                    } catch (Exception e) {
//                        logger.error("failed to signin", e);
//                        InsidiousNotification.notifyMessage("Failed to sign in -" + e.getMessage(), NotificationType.ERROR);
//                    }
//                }
//            }
//        }
    }

    public boolean isLoggedIn() {
        return this.client.getToken() != null;
    }

    public void signup(String serverUrl, String usernameText, String passwordText, SignUpCallback signupCallback) {
        this.client = new VideobugNetworkClient(serverUrl);
        this.client.signup(serverUrl, usernameText, passwordText, signupCallback);
    }

    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public void signin(String serverUrl, String usernameText, String passwordText) throws IOException {
        this.client = new VideobugNetworkClient(serverUrl);
        logger.info("signin with username [" + usernameText + "] on server " + serverUrl);

        if (!isValidEmailAddress(usernameText)) {
            credentialsToolbarWindow.setErrorLabel("Enter a valid email address");
            return;
        }

        if (passwordText == null || passwordText.length() < 4) {
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLabel("Enter a valid password, at least 4 characters");
            }
            return;
        }

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("email", usernameText);
        eventProperties.put("server", serverUrl);
        UsageInsightTracker.getInstance().RecordEvent("SignInAttempt", eventProperties);

        insidiousConfiguration.setServerUrl(serverUrl);
        insidiousConfiguration.setUsername(usernameText);

        try {
            client.signin(SigninRequest.from(serverUrl, usernameText, passwordText), new SignInCallback() {
                @Override
                public void error(String errorMessage) {
                    if (credentialsToolbarWindow != null) {
                        credentialsToolbarWindow.setErrorLabel("Sign in failed: " + errorMessage);
                    }

                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("email", usernameText);
                    eventProperties.put("server", serverUrl);
                    UsageInsightTracker.getInstance().RecordEvent("SignInFailed", eventProperties);


                    InsidiousNotification.notifyMessage("Failed to login VideoBug at [" + serverUrl + "] for module [" + currentModule.getName() + "]", NotificationType.ERROR);

                }

                @Override
                public void success(String token) {
                    ReadAction.run(() -> InsidiousService.this.ensureAgentJar(false));
                    ReadAction.run(InsidiousService.this::setupProject);

                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("email", usernameText);
                    eventProperties.put("server", serverUrl);
                    UsageInsightTracker.getInstance().RecordEvent("SignInSuccess", eventProperties);


                    Credentials credentials = new Credentials(insidiousConfiguration.getUsername(), passwordText);
                    insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
                    PasswordSafe.getInstance().set(insidiousCredentials, credentials);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        InsidiousNotification.notifyMessage("VideoBug logged in at [" + serverUrl + "] for module [" + currentModule.getName() + "]", NotificationType.INFORMATION);

                    });
                }
            });


        } catch (UnauthorizedException e) {

            logger.error("Failed to signin for user " + usernameText, e);
            e.printStackTrace();
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLabel("Sign in failed!");
            }
        } catch (Throwable e) {
            logger.error("failed to connect with server", e);
            InsidiousNotification.notifyMessage("Failed to connect with server - " + e.getMessage(), NotificationType.ERROR);
        }
        ApplicationManager.getApplication().invokeLater(this::initiateUI);
    }

    private void setupProject() {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
        }

        try {
            logger.info("try to set project to - " + currentModule.getName());
            client.setProject(currentModule.getName());
            generateAppToken();
        } catch (ProjectDoesNotExistException e1) {
            createProject(currentModule.getName(), new NewProjectCallback() {
                @Override
                public void error(String errorMessage) {

                    logger.error("failed to create project - {}", errorMessage);

                    InsidiousNotification.notifyMessage("Failed to create new project for [" + currentModule.getName() + "] on server [" + insidiousConfiguration.serverUrl, NotificationType.ERROR);

                }

                @Override
                public void success(String projectId) {
                    logger.info("created new project for " + currentModule.getName() + " -> " + projectId);
                    ApplicationManager.getApplication().invokeLater(() -> setupProject());
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
                InsidiousNotification.notifyMessage("Failed to generate app token for module [" + currentModule.getName() + "]", NotificationType.ERROR);

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

    public VirtualFile saveTestSuite(TestSuite testSuite) throws IOException {
        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {


            Map<String, JsonElement> valueResourceMap = testCaseScript.getTestGenerationState().getValueResourceMap();
            if (valueResourceMap.values().size() > 0) {
                String testResourcesDirPath =
                        project.getBasePath() + "/src/test/resources/unlogged-fixtures/" + testCaseScript.getClassName();
                File resourcesDirFile = new File(testResourcesDirPath);
                resourcesDirFile.mkdirs();
                String testResourcesFilePath = testResourcesDirPath + "/" + testCaseScript.getTestMethodName() + ".json";
                String resourceJson = gson.toJson(valueResourceMap);
                try (FileOutputStream resourceFile = new FileOutputStream(testResourcesFilePath)) {
                    resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                }
                VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(Path.of(testResourcesFilePath).toUri().toString());
            }


            String testOutputDirPath =
                    project.getBasePath() + "/src/test/java/"
                            + testCaseScript.getPackageName().replaceAll("\\.", "/");
            File outputDir = new File(testOutputDirPath);
            outputDir.mkdirs();
            File testcaseFile = new File(testOutputDirPath + "/" + testCaseScript.getClassName() + ".java");

            try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
//                @NotNull PsiTypeCodeFragment codePsiElement = JavaCodeFragmentFactory
//                        .getInstance(project)
//                        .createTypeCodeFragment(testCaseScript.getCode(), null, true);
//                @NotNull Collection<? extends TextRange> ranges = ContainerUtil.newArrayList(codePsiElement.getTextRange());
//                CodeStyleManager.getInstance(project).reformatText(codePsiElement, ranges);
//                out.write(codePsiElement.getText().getBytes(StandardCharsets.UTF_8));
//                FormatterServiceImpl formatterService = new FormatterServiceImpl();
//                String formattedSource = formatterService.formatSourceReflowStringsAndFixImports(testCaseScript.getCode());
                out.write(testCaseScript.getCode().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                InsidiousNotification.notifyMessage(
                        "Failed to write test case: " + testCaseScript + " -> "
                                + e.getMessage(), NotificationType.ERROR
                );
            }

            @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                    .refreshAndFindFileByUrl(Path.of(testcaseFile.getAbsolutePath()).toUri().toString());
            if (newFile == null) {
                return null;
            }


            FileContentUtil.reparseFiles(project, List.of(newFile), true);
            @Nullable Document newDocument = FileDocumentManager.getInstance().getDocument(newFile);

            FileEditorManager.getInstance(project).openFile(newFile, true, true);

//            ApplicationManager.getApplication().runWriteAction(new Runnable() {
//                @Override
//                public void run() {
//                    @Nullable PsiFile testFilePsiInstance = PsiManager.getInstance(project).findFile(newFile);
//                    @NotNull Collection<? extends TextRange> ranges = ContainerUtil.newArrayList(testFilePsiInstance.getTextRange());
//                    CodeStyleManager.getInstance(project).reformatText(testFilePsiInstance, ranges);
//                }
//            });


            logger.info("Test case generated in [" + testCaseScript.getClassName() + "]\n" + testCaseScript);
            return newFile;
        }
//        VirtualFileManager.getInstance().syncRefresh();
        return null;
    }

    public void doSearch(SearchQuery searchQuery) throws APICallException, IOException, SQLException {


        refreshSession();
        if (this.client.getCurrentSession() == null) {
            UsageInsightTracker.getInstance().RecordEvent("Get" + searchQuery.getQueryType() + "NoSession", null);
            loadSession();
            return;
        }

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("query", searchQuery.getQuery());
        eventProperties.put("sessionId", client.getCurrentSession().getSessionId());
        eventProperties.put("projectId", client.getCurrentSession().getProjectId());
        UsageInsightTracker.getInstance().RecordEvent("GetTracesByValue", eventProperties);

        insidiousConfiguration.addSearchQuery((String) searchQuery.getQuery(),
                0);
        searchByValueWindow.updateQueryList();

        long start = System.currentTimeMillis();
        SearchResultsCallbackHandler searchResultsHandler = new SearchResultsCallbackHandler(searchQuery);

        ProgressManager.getInstance().run(new Task.Modal(project, "Unlogged", true) {
            public void run(@NotNull ProgressIndicator indicator) {

                List<ExecutionSession> sessionList =
                        null;
                try {
                    sessionList = client.fetchProjectSessions().getItems();
                } catch (APICallException | IOException e) {
                    InsidiousNotification.notifyMessage("Failed to get " +
                            "project sessions", NotificationType.ERROR);
                    return;
                }
                if (sessionList.size() > 10) {
                    sessionList = sessionList.subList(0, 10);
                }


                indicator.setText("Searching across " + sessionList.size() + " session");
                indicator.setText2("Filtering by class types");
                AtomicInteger done = new AtomicInteger(0);

                for (ExecutionSession executionSession : sessionList) {
                    switch (searchQuery.getQueryType()) {
                        case BY_TYPE:
                            client.queryTracePointsByTypes(searchQuery, executionSession.getSessionId(), -1, searchResultsHandler);
                            break;
                        case BY_VALUE:
                            client.queryTracePointsByValue(searchQuery, executionSession.getSessionId(), searchResultsHandler);
                            break;
                        case BY_PROBE:
                            client.queryTracePointsByEventType(searchQuery,
                                    executionSession.getSessionId(), searchResultsHandler);
                            break;
                    }

                }

                while (client instanceof VideobugNetworkClient &&
                        searchResultsHandler.getDoneCount() != sessionList.size()) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new ProcessCanceledException(e);
                    }
                }

                if (searchQuery.getQueryType().equals(QueryType.BY_TYPE)) {
                    searchByTypesWindow.addTracePoints(searchResultsHandler.getResults());
                } else {
                    searchByValueWindow.addTracePoints(searchResultsHandler.getResults());
                    insidiousConfiguration.addSearchQuery((String) searchQuery.getQuery(),
                            searchResultsHandler.getResults().size());
                    searchByValueWindow.updateQueryList();
                }

                if (searchResultsHandler.getResults().size() == 0) {

                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("query", searchQuery.getQuery());
                    UsageInsightTracker.getInstance().RecordEvent("NoResult" + searchQuery.getQueryType(), eventProperties);

                    InsidiousNotification.notifyMessage("No data events matched", NotificationType.INFORMATION);
                }

            }
        });


    }


    private synchronized void startDebugSession() {
        logger.info("start debug session");
        if (true) {
            return;
        }

        debugSession = getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions());


        if (debugSession != null) {
            return;
        }
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("module", currentModule.getName());
        UsageInsightTracker.getInstance().RecordEvent("StartDebugSession", eventProperties);

        @NotNull RunConfiguration runConfiguration = ConfigurationTypeUtil.findConfigurationType(InsidiousRunConfigType.class).createTemplateConfiguration(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(project, new InsidiousExecutor(), runConfiguration).build();
                ProgramRunnerUtil.executeConfiguration(env, false, false);
            } catch (Throwable e) {
                logger.error("failed to execute configuration", e);
            }
        });


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
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Content exceptionContent = toolWindow.getContentManager().findContent("Exceptions");
                Content traceContent = toolWindow.getContentManager().findContent("Traces");
                if (exceptionContent != null) {
                    toolWindow.getContentManager().removeContent(exceptionContent, true);
                }
                if (traceContent != null) {
                    toolWindow.getContentManager().removeContent(traceContent, true);
                }
                ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
                ConfigurationWindow credentialsToolbar = new ConfigurationWindow(project, toolWindow);
                Content credentialContent = contentFactory.createContent(credentialsToolbar.getContent(), "Credentials", false);
                toolWindow.getContentManager().addContent(credentialContent);
            }
        });
    }

    private void addAgentToRunConfig() {


        List<RunnerAndConfigurationSettings> allSettings = project.getService(RunManager.class).getAllSettings();

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


    private void initiateUI() {
        logger.info("initiate ui");
        ContentFactory contentFactory = ApplicationManager.getApplication().getService(ContentFactory.class);
        if (this.toolWindow == null) {
            return;
        }

        ContentManager contentManager = this.toolWindow.getContentManager();
        if (credentialsToolbarWindow == null) {
            credentialsToolbarWindow = new ConfigurationWindow(project, this.toolWindow);
//            @NotNull Content credentialContent = contentFactory.createContent(credentialsToolbarWindow.getContent(), "Credentials", false);
//            contentManager.addContent(credentialContent);

//            singleWindowView = new SingleWindowView(project, this);
//            Content singleWindowContent = contentFactory.createContent(singleWindowView.getContent(), "Raw View", false);
//            contentManager.addContent(singleWindowContent);

            liveViewWindow = new LiveViewWindow(project, this);
            Content liveWindowContent = contentFactory.createContent(liveViewWindow.getContent(), "Live View", false);
            contentManager.addContent(liveWindowContent);

            setupProject();
            return;
        }

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
        return Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath().toString();
    }

    private void setAppTokenOnUi() {
        logger.info("set app token - " + appToken + " with package name " + packageName);

        String[] vmParamsToAdd = new String[]{"--add-opens=java.base/java.util=ALL-UNNAMED", "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH + "=i=" + (packageName == null ? DEFAULT_PACKAGE_NAME : packageName.replaceAll("\\.", "/")) + ",server=" + (insidiousConfiguration != null ? insidiousConfiguration.serverUrl : "https://cloud.bug.video") + ",token=" + appToken + "\""};

        javaAgentString = String.join(" ", vmParamsToAdd);

        if (credentialsToolbarWindow != null) {
            credentialsToolbarWindow.setText(javaAgentString);
        }
        if (appToken != null && !Objects.equals(packageName, "YOUR.PACKAGE.NAME")) {
            addAgentToRunConfig();
        }
    }

    public void loadSession() throws IOException {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
        }

        this.client.getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                logger.error("failed to load project sessions - {}", message);
                ApplicationManager.getApplication().invokeLater(() -> InsidiousNotification.notifyMessage("No sessions found for module [" + currentModule.getName() + "]", NotificationType.INFORMATION));
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                logger.info("got [" + executionSessionList.size() + "] sessions for project");
                if (executionSessionList.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {

                        if (InsidiousNotification.balloonNotificationGroup != null) {
                            InsidiousNotification.notifyMessage("No sessions found for project " + currentModule.getName() + ". Start recording new sessions with the java agent", NotificationType.INFORMATION);
                        } else {

                            InsidiousNotification.notifyMessage("No sessions found" + " for project " + currentModule.getName() + " start recording new sessions with the java agent", NotificationType.INFORMATION);
                        }
                    });
                    return;
                }
                try {
                    client.setSessionInstance(new SessionInstance(executionSessionList.get(0)));
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage("Failed to set session - " + e.getMessage(), NotificationType.ERROR);
                }
            }
        });
    }

    public void loadTracePoint(TracePoint selectedTrace) {

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("value", selectedTrace.getMatchedValueId());
        eventProperties.put("classId", selectedTrace.getClassId());
        eventProperties.put("className", selectedTrace.getClassname());
        eventProperties.put("dataId", selectedTrace.getDataId());
        eventProperties.put("fileName", selectedTrace.getFilename());
        eventProperties.put("nanoTime", selectedTrace.getNanoTime());
        eventProperties.put("lineNumber", selectedTrace.getLineNumber());
        eventProperties.put("threadId", selectedTrace.getThreadId());

        UsageInsightTracker.getInstance().RecordEvent("FetchByTracePoint", eventProperties);

        if (debugSession == null || getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions()) == null) {
            UsageInsightTracker.getInstance().RecordEvent("StartDebugSessionAtSelectTracepoint", null);
            pendingSelectTrace = selectedTrace;
            startDebugSession();
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, "Unlogged", true) {
            public void run(@NotNull ProgressIndicator indicator) {

                try {
                    logger.info("set trace point in connector => " + selectedTrace.getClassname());
                    indicator.setText("Loading trace point " + selectedTrace.getClassname() + ":" + selectedTrace.getLineNumber() + " for value " + selectedTrace.getMatchedValueId() + " in thread " + selectedTrace.getThreadId() + " at time " + DateFormat.getInstance().format(new Date(selectedTrace.getNanoTime())));
                    if (connector != null) {
                        connector.setTracePoint(selectedTrace, indicator);
                    } else {
                        pendingTrace = selectedTrace;
                    }


                } catch (ProcessCanceledException pce) {
                    throw pce;
                } catch (Exception e) {
                    logger.error("failed to set trace point", e);
                    InsidiousNotification.notifyMessage("Failed to set select trace point " + e.getMessage(), NotificationType.ERROR);
                    return;
                } finally {
                    indicator.stop();
                }

                if (debugSession.isPaused()) {
                    debugSession.resume();
                }
                debugSession.pause();


            }
        });


    }

    public void setExceptionClassList(Map<String, Boolean> exceptionClassList) {
        insidiousConfiguration.exceptionClassMap = exceptionClassList;
    }


    public VideobugClientInterface getClient() {
        return client;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        init();
    }

    public Map<String, Boolean> getDefaultExceptionClassList() {
        return insidiousConfiguration.exceptionClassMap;
    }

    public InsidiousConfigurationState getConfiguration() {
        return insidiousConfiguration;
    }

    public void ensureAgentJar(boolean overwrite) {
        checkAndEnsureJavaAgent(overwrite, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {
                InsidiousNotification.notifyMessage("Failed to download java agent: " + message, NotificationType.ERROR);
            }

            @Override
            public void success(String url, String path) {
                InsidiousNotification.notifyMessage("Agent jar download complete", NotificationType.INFORMATION);
            }
        });

    }

    public void refreshSession() throws APICallException, IOException, SQLException {
        logger.info("fetch latest session for module: " + currentModule.getName());
        client.setProject(currentModule.getName());

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        if (sessions.getItems().size() == 0) {
            InsidiousNotification.notifyMessage("No sessions available for module [" + currentModule.getName() + "]", NotificationType.ERROR);
            return;
        }
        if (client.getCurrentSession() == null || !client.getCurrentSession()
                .getSessionId().equals(sessions.getItems().get(0).getSessionId())) {
            client.setSessionInstance(new SessionInstance(sessions.getItems().get(0)));
        }
    }

    public void initiateUseLocal() {
        client = new VideobugLocalClient(Constants.VIDEOBUG_HOME_PATH + "/sessions");
        UsageInsightTracker.getInstance().RecordEvent("InitiateUseLocal", null);


        ReadAction.run(() -> this.ensureAgentJar(false));
        ReadAction.run(InsidiousService.this::setupProject);

        ApplicationManager.getApplication().invokeLater(() -> {
            InsidiousService.this.initiateUI();
            InsidiousNotification.notifyMessage("VideoBug logged in at [" + "disk://localhost" + "] for module [" + currentModule.getName() + "]", NotificationType.INFORMATION);
            Messages.showMessageDialog("Copy the JVM parameter and configure it for your application" + " and start running your application to start record.", "Videobug", Messages.getInformationIcon());
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
        UsageInsightTracker.getInstance().RecordEvent("DiagnosticReport", null);
        DiagnosticService diagnosticService = new DiagnosticService(new VersionManager(), this.project, this.currentModule);
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

        String utilFilePath = testOutputDirPath + "/UnloggedTestUtils.java";
        File utilFile = new File(utilFilePath);
        if (utilFile.exists()) {
            // util file already exist
            return;
        }
        try (FileOutputStream writer = new FileOutputStream(utilFilePath)) {
            InputStream testUtilClassCode = this.getClass().getClassLoader().getResourceAsStream("code/UnloggedTestUtil.java");
            assert testUtilClassCode != null;
            IOUtils.copy(testUtilClassCode, writer);
        }
        @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                .refreshAndFindFileByUrl(Path.of(utilFile.getAbsolutePath()).toUri().toString());

//        @Nullable PsiFile testFilePsiInstance = PsiManager.getInstance(project).findFile(newFile);
//        @NotNull Collection<? extends TextRange> ranges = ContainerUtil.newArrayList(testFilePsiInstance.getTextRange());
//        CodeStyleManager.getInstance(project).reformatText(testFilePsiInstance, ranges);

//        @NotNull PsiElement formattedCode = CodeStyleManagerImpl.getInstance(project).reformat(testFilePsiInstance);

    }
}
