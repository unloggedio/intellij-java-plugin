package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.*;
import com.insidious.plugin.pojo.InsidiousOnboardingStatus;
import com.insidious.plugin.pojo.OnBoardingStatus;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class OnboardingScaffoldV3 implements CardActionListener {
    private static final Logger logger = LoggerUtil.getInstance(OnboardingScaffoldV3.class);
    private final OnboardingService onboardingService;
    private final VMoptionsConstructionService vmOptionsConstructionService = new VMoptionsConstructionService();
    private final Project project;
    List<String> jdkVersions_ref = Arrays.asList("8", "11", "17", "18");
    private OnBoardingStatus status = new OnBoardingStatus();
    private JPanel mainParentPanel;
    private JPanel MainBorderParent;
    private JPanel NavigationPanelParent;
    private JPanel MainContentPanel;
    private JPanel navGrid;
    private JPanel leftContainer;
    private JPanel rightContainer;
    private JPanel leftGrid;
    private JPanel rightGrid;
    private JSplitPane splitParent;
    private Run_Component_Obv3 runComponent = null;
    private NavigatorComponent navigator;

    public OnboardingScaffoldV3(OnboardingService onboardingService, Project project) {
        this.project = project;
        this.onboardingService = onboardingService;
        loadNavigator();
        InsidiousOnboardingStatus status_per =
                project.getService(InsidiousConfigurationState.class).getOnboardingStatus();
        if (status_per != null) {
            if (status_per.isCompleted()) {
                this.status = status_per.getStatus();
                loadRunSection(project.getService(InsidiousService.class).areLogsPresent());
                this.navigator.loadState("Run!");
            } else {
                this.status = status_per.getStatus();
                loadModuleSection();
            }
        } else {
            status_per = new InsidiousOnboardingStatus();
            status_per.setCompleted(false);
            status_per.setStatus(this.status);
            project.getService(InsidiousConfigurationState.class).setOnboardingStatus(status_per);
            loadModuleSection();
        }
    }

    @Override
    public void performActions(List<Map<ONBOARDING_ACTION, String>> actions) {

        for (Map<ONBOARDING_ACTION, String> action : actions) {
            ONBOARDING_ACTION action_to_perform = new ArrayList<>(action.keySet()).get(0);
            switch (action_to_perform) {
                case DOWNLOAD_AGENT:
                    String version = action.get(ONBOARDING_ACTION.DOWNLOAD_AGENT);
                    if (version.startsWith("jackson")) {
                        project.getService(InsidiousService.class).getProjectTypeInfo()
                                .setJacksonDatabindVersion(version);
                    }
                    System.out.println("DOWNLOADING AGENT FOR - " + version);
                    onboardingService.downloadAgentForVersion(version);
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("version", version);
                    UsageInsightTracker.getInstance()
                            .RecordEvent("Agent_Download_v3", eventProperties);
                    break;
                case ADD_DEPENDENCIES:
                    //trigger add dependencies
                    System.out.println("ADD DEPENDENCIES TRIGGERED");
                    String dependencies_string = action.get(ONBOARDING_ACTION.ADD_DEPENDENCIES);
                    System.out.println("[SELECTED DEPENDENCIES] " + dependencies_string);
                    dependencies_string = dependencies_string
                            .replaceAll("\\[", "")
                            .replaceAll("\\]", "");
                    HashSet<String> deps =
                            Arrays.stream(dependencies_string.split(","))
                                    .filter(e -> e.length() > 0)
                                    .collect(Collectors.toCollection(HashSet::new)
                                    );
                    Map<String, String> refs = onboardingService.getMissingDependencies_v3();
                    if (refs.size() > 0) {
                        onboardingService.postProcessDependencies(refs, deps);
                    }
                    eventProperties = new JSONObject();
                    eventProperties.put("adding_dependencies", dependencies_string);
                    UsageInsightTracker.getInstance()
                            .RecordEvent("Add_Dependencies_v3", eventProperties);
                    navigator.loadNextState();
                    break;
                case UPDATE_SELECTION:
                    String parameter = action.get(ONBOARDING_ACTION.UPDATE_SELECTION)
                            .trim();
                    String[] parts = parameter.split(":");
                    System.out.println("UPDATE SELECTION TRIGGERED " + parameter);
                    switch (parts[0]) {
                        case "module":
                            System.out.println("Updating module");
                            this.status.setCurrentModule(parts[1]);
                            onboardingService.setSelectedModule(parts[1]);
                            updateVMParams(parts[1]);
                            if (navigator.shouldReloadDocumentation()) {
                                loadDocumentation(DOCUMENTATION_TYPE.MODULE);
                            }
                            eventProperties = new JSONObject();
                            eventProperties.put("selection", parts[1]);
                            UsageInsightTracker.getInstance()
                                    .RecordEvent("Module_Update_v3", eventProperties);
                            break;
                        case "jdk":
                            this.status.setJdkVersion(parts[1]);
                            boolean addOpens = false;
                            if (Integer.parseInt(parts[1]) >= 17) {
                                addOpens = true;
                            }
                            updateVMParams(addOpens, parts[1]);
                            eventProperties = new JSONObject();
                            eventProperties.put("selection", parts[1]);
                            UsageInsightTracker.getInstance()
                                    .RecordEvent("JDK_selection_v3", eventProperties);
                            break;
                        case "runType":
                            ProjectTypeInfo.RUN_TYPES type = ProjectTypeInfo.RUN_TYPES.valueOf(parts[1]);
                            this.status.setRunType(type);
                            if (navigator.shouldReloadRun()) {
                                loadRunSection(checkIfLogsArePresent());
                            }
                            eventProperties = new JSONObject();
                            eventProperties.put("selection", parts[1]);
                            UsageInsightTracker.getInstance()
                                    .RecordEvent("RunType_selection_v3", eventProperties);
                            break;
                    }
                    break;
                case NEXT_STATE:
                    navigator.loadNextState();
            }
        }
    }

    public void updateVMParams(String modulename) {
        this.vmOptionsConstructionService.setBasePackage(onboardingService.fetchBasePackageForModule(modulename));
        if (this.runComponent != null) {
            this.runComponent.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.getRunType()));
            this.runComponent.setBasePackageText(vmOptionsConstructionService.getBasePackage());
            this.runComponent.setFallbackPackage(vmOptionsConstructionService.getBasePackage());
        }
    }

    public void updateVMParams(boolean addopens, String jdkversion) {
        this.vmOptionsConstructionService.setAddopens(addopens);
        if (this.runComponent != null) {
            this.runComponent.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.getRunType()));
            this.runComponent.setBasePackageText(vmOptionsConstructionService.getBasePackage());
            this.runComponent.setFallbackPackage(vmOptionsConstructionService.getBasePackage());
        }
    }

    @Override
    public String getBasePackageForModule(String moduleName) {
        String packageName = onboardingService.fetchBasePackageForModule(moduleName);
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("module", moduleName);
        eventProperties.put("package", packageName);
        UsageInsightTracker.getInstance()
                .RecordEvent("BasePackageForModule", eventProperties);
        return packageName;
    }

    public void loadDocumentation(DOCUMENTATION_TYPE type) {
        this.rightContainer.removeAll();
        Obv3_Documentation_Generic docs = new Obv3_Documentation_Generic();
        docs.setContent(getDocumentationTextFor(type));
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(docs.getComponent(), constraints);
        this.rightContainer.add(gridPanel, BorderLayout.CENTER);
        this.rightContainer.revalidate();
    }

    public String getDocumentationTextFor(DOCUMENTATION_TYPE type) {
        StringBuilder sb = new StringBuilder();
        InsidiousService insidiousService = project.getService(InsidiousService.class);
        switch (type) {
            case MODULE:
                sb.append("The plugin will generate unit tests in the directory relative to the selected module. " +
                        "Choosing the correct module is important since the imports in the unit test work out of the box.\n" +
                        "\n");
                if (insidiousService.getSelectedModuleInstance() != null && insidiousService.getSelectedModuleInstance()
                        .getPath() != null) {
                    sb.append(
                            "Based on your current selection, the test cases will be generated at the following location");
                    sb.append("\n" + insidiousService.getSelectedModuleInstance()
                            .getPath() + "/src/test/java/{your.package.name}"
                            + "\n");
                }
                break;
            case PROJECT_CONFIG:
                sb.append("JDK Version\n");
                sb.append(
                        "Select the JDK you use to run your application. We need the right JDK version so that we can construct the right VM argument\n" +
                                "\n");
                sb.append("JSON Serializer\n");
                sb.append(
                        "Select GSON or Jackson based on what you are already using in the module. This ensures that the serialized data uses correct field names respecting your existing annotations.\n" +
                                "\n" +
                                "If you are not using either, select the latest Jackson version.");
                break;
            case DEPENDENCIES:
                sb.append("Required dependencies \n\n");
                sb.append("Add the following dependencies to record data - \n\n" +
                        "‣ GSON Based on your selection\n" +
                        "\n" +
                        "‣ Jackson Based on your selection\n" +
                        "\n" +
                        "‣ JavaTimeModule (only for jackson) Support for Java date/time types (Instant, LocalDateTime, etc)\n" +
                        "\n" +
                        "‣ JodaModule (only for jackson) Support for Joda data types\n" +
                        "\n" +
                        "‣ Hibernate5Module (only for jackson) Support for Hibernate (https://hibernate.org) specific datatypes and properties; especially lazy-loading aspects\n" +
                        "\n" +
                        "‣ Jdk8Module (only for jackson) Support for new Java 8 datatypes outside of date/time: most notably Optional, OptionalLong, OptionalDouble\n");
                break;
            case RUN_TYPE:
                sb.append("Run Config\n");
                sb.append(
                        "Choose your preferred way of running your application. Based on your selection we can show you the easiest way to add the javaagent to your application and get started with generating tests.\n" +
                                "\n" +
                                "Support for Docker/Kubernetes/Tomcat deployment is coming soon.");
                break;
        }
        return sb.toString();
    }

    public void loadNavigator() {
        this.navGrid.removeAll();
        navigator = new NavigatorComponent(this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(navigator.getComponent(), constraints);
        this.navGrid.add(gridPanel, BorderLayout.CENTER);
        this.navGrid.revalidate();
    }

    public JPanel getComponent() {
        return this.mainParentPanel;
    }

    public void loadModuleSection() {
        List<DropdownCardInformation> content = new ArrayList<>();
        List<String> modules = onboardingService.fetchModules();
        DropdownCardInformation info = new DropdownCardInformation("Select a Module :",
                modules, "Unlogged will generate unit tests for this module");
        info.setType(DROP_TYPES.MODULE);
        info.setShowRefresh(true);
        if (this.status.getCurrentModule() != null) {
            info.setDefaultSelected(modules.indexOf(this.status.getCurrentModule()));
        } else if (modules.size() > 0) {
            info.setDefaultSelected(modules.indexOf(modules.get(0)));
        } else {
            logger.warn("No modules identified");
        }
        content.add(info);
        this.leftContainer.removeAll();
        Obv3_CardParent cardparent = new Obv3_CardParent(content, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadDocumentation(DOCUMENTATION_TYPE.MODULE);

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("default_module", this.status.getCurrentModule());
        UsageInsightTracker.getInstance()
                .RecordEvent("ModuleSelection_Load", eventProperties);
    }

    public void loadDependenciesManagementSection() {
        List<DependencyCardInformation> content = new ArrayList<>();
        List<String> dependencies = new ArrayList<>(onboardingService.getMissingDependencies_v3()
                .keySet());
        System.out.println("DEPENDENCIES missing -> " + dependencies);
        content.add(new DependencyCardInformation(
                dependencies.size() == 0 ? "No Missing Dependencies" : "Missing dependencies",
                dependencies.size() == 0 ? "No other dependencies needed." : "Add these dependencies so that we can serialise/deserialise data properly.",
                dependencies));
        content.get(0)
                .setShowSkipButton(dependencies.size() > 0);
        content.get(0)
                .setPrimaryButtonText(dependencies.size() == 0 ? "Proceed" : "Add Dependencies");
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("dependencies_needed", dependencies);
        UsageInsightTracker.getInstance()
                .RecordEvent("RequiredDependencies_Load", eventProperties);
        this.leftContainer.removeAll();
        Obv3_CardParent cardparent = new Obv3_CardParent(content, true, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadDocumentation(DOCUMENTATION_TYPE.DEPENDENCIES);
    }

    public void loadProjectConfigSection() {
        List<DropdownCardInformation> content = new ArrayList<>();
        List<String> java_versions = new ArrayList<>();
        java_versions.add("8");
        java_versions.add("11");
        java_versions.add("17");
        java_versions.add("18");
        DropdownCardInformation info_java = new DropdownCardInformation("JDK version : ",
                java_versions,
                "Select the JDK version of your project.");
        info_java.setType(DROP_TYPES.JAVA_VERSION);
        info_java.setDefaultSelected(0);
        if (this.status.getJdkVersion() != null) {
            info_java.setDefaultSelected(java_versions.indexOf(this.status.getJdkVersion()));
        }
        content.add(info_java);
        List<String> serializers = new ArrayList<>();
        serializers.add("jackson-2.8");
        serializers.add("jackson-2.9");
        serializers.add("jackson-2.10");
        serializers.add("jackson-2.11");
        serializers.add("jackson-2.12");
        serializers.add("jackson-2.13");
        serializers.add("jackson-2.14");
        serializers.add("gson");

        String suggestedAgent = project.getService(InsidiousService.class).suggestAgentVersion();
        System.out.println("[SUGGESTED AGENT] " + suggestedAgent);

        int defaultIntex = 0;
        if (serializers.contains(suggestedAgent)) {
            defaultIntex = serializers.indexOf(suggestedAgent);
        }

        DropdownCardInformation info_dependencies = new DropdownCardInformation("JSON Serializer : ",
                serializers,
                "Select the serializer that your project uses. ");
        info_dependencies.setType(DROP_TYPES.SERIALIZER);
        info_dependencies.setDefaultSelected(defaultIntex);
        info_dependencies.setShowRefresh(true);
        content.add(info_dependencies);
        this.leftContainer.removeAll();
        Obv3_CardParent cardparent = new Obv3_CardParent(content, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadDocumentation(DOCUMENTATION_TYPE.PROJECT_CONFIG);

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("suggested_agent", suggestedAgent);
        UsageInsightTracker.getInstance()
                .RecordEvent("JDKandSerializerSelection_Load", eventProperties);
    }

    public void loadRunConfigSection() {
        this.leftContainer.removeAll();
        Obv3_Run_Mode_Selector cardparent;
        cardparent = new Obv3_Run_Mode_Selector(this.status.getRunType(), this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadDocumentation(DOCUMENTATION_TYPE.RUN_TYPE);
    }

    public void loadRunSection(boolean logsPresent) {

        project.getService(InsidiousConfigurationState.class).getOnboardingStatus().setCompleted(true);
        onboardingService.setSelectedModule(this.status.getCurrentModule());
        updateVMParams(this.status.getCurrentModule());
        if (!project.getService(InsidiousService.class).areModulesRegistered()) {
            project.getService(InsidiousService.class).setCurrentModule(this.status.getCurrentModule());
            //registering modules
            project.getService(InsidiousService.class).fetchModules();
        }
        this.leftContainer.removeAll();
        Obv3_Run_Mode_Selector cardparent;
        cardparent = new Obv3_Run_Mode_Selector(this.status.getRunType(), this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        this.rightContainer.removeAll();
        ProjectTypeInfo.RUN_TYPES defaultType = this.status.getRunType();
        if (defaultType == null) {
            defaultType = ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION;
        }
        Run_Component_Obv3 runSection = new Run_Component_Obv3(defaultType, logsPresent, this);
        this.runComponent = runSection;
        if (this.status.getCurrentModule() != null) {
            runSection.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.getRunType()));
            runSection.setBasePackageText(vmOptionsConstructionService.getBasePackage());
            runSection.setFallbackPackage(vmOptionsConstructionService.getBasePackage());
        }
        gridLayout = new GridLayout(1, 1);
        gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(runSection.getComponent(), constraints);
        this.rightContainer.add(gridPanel, BorderLayout.CENTER);
        this.rightContainer.revalidate();

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("default_module", this.status.getCurrentModule());
        eventProperties.put("default_jdk", this.status.getJdkVersion());
        eventProperties.put("default_run_type", this.status.getRunType()
                .toString());
        UsageInsightTracker.getInstance()
                .RecordEvent("Run_Load", eventProperties);
    }

    @Override
    public void checkForSelogs() {
        recursiveFileCheck();
    }

    @Override
    public void loadLiveLiew() {
        project.getService(InsidiousService.class).addLiveView();
    }

    @Override
    public void refreshModules() {
        loadModuleSection();
    }

    @Override
    public void refreshDependencies() {
        loadDependenciesManagementSection();
    }

    @Override
    public void refreshSerializers() {
        loadProjectConfigSection();
    }

    private void recursiveFileCheck() {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        runSelogCheck();
                    }
                });
    }

    private void runSelogCheck() {
        //System.out.println("In check for selogs");
        if (project.getService(InsidiousService.class).areLogsPresent()) {
            //switch state
            System.out.println("Can Switch to LIVE VIEW");
            loadRunSection(true);
            UsageInsightTracker.getInstance()
                    .RecordEvent("LogsReady", null);
        } else {
            Timer timer = new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    runSelogCheck();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    public boolean checkIfLogsArePresent() {
        return project.getService(InsidiousService.class).areLogsPresent();
    }

    @Override
    public boolean runApplicationWithUnlogged() {
        InsidiousService insidiousService = project.getService(InsidiousService.class);
        if (insidiousService.hasProgramRunning()) {
            return false;
        }

        System.out.println("[RUNNING WITH UNLOGGED]");
        String params = vmOptionsConstructionService.getVMParametersFull();

        System.out.println("[PARAMS RUN]" + params);
        List<RunnerAndConfigurationSettings> allSettings = insidiousService.getProject()
                .getService(RunManager.class)
                .getAllSettings();
        for (RunnerAndConfigurationSettings runSetting : allSettings) {
            System.out.println("runner config - " + runSetting.getName());
            if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {

                logger.info("ApplicationConfiguration config - " + runSetting.getConfiguration()
                        .getName());
                final ProgramRunner runner = DefaultJavaProgramRunner.getInstance();
                final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
                ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
                applicationConfiguration.setVMParameters(params.trim());
                try {
                    runner.execute(new ExecutionEnvironment(executor, runner, runSetting,
                            insidiousService.getProject()), null);
                    return true;
                } catch (Exception e) {
                    System.out.println("Failed to start application");
                    System.out.println(e);
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasRunnableApplicationConfig() {
        List<RunnerAndConfigurationSettings> allSettings = project.getService(InsidiousService.class).getProject()
                .getService(RunManager.class)
                .getAllSettings();
        for (RunnerAndConfigurationSettings runSetting : allSettings) {
            System.out.println("runner config - " + runSetting.getName());
            if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isApplicationRunning() {
        return project.getService(InsidiousService.class).hasProgramRunning();
    }

    public void setDividerLocation(int location) {
        this.splitParent.setDividerLocation(location);
    }

    @Override
    public void triggerOnboardingRestart() {
        navigator.restartOnboarding();
    }

    @Override
    public String getCurrentBasePackage() {
        return onboardingService.fetchBasePackageForModule(this.status.getCurrentModule());
    }

    public void updateBasePackage(String text) {
        this.vmOptionsConstructionService.setBasePackage(text);
        if (this.runComponent != null) {
            this.runComponent.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.getRunType()));
            this.runComponent.setBasePackageText(vmOptionsConstructionService.getBasePackage());
            this.runComponent.setFallbackPackage(vmOptionsConstructionService.getBasePackage());
        }
    }

    public enum DOCUMENTATION_TYPE {MODULE, PROJECT_CONFIG, DEPENDENCIES, RUN_TYPE}

    public enum DROP_TYPES {MODULE, JAVA_VERSION, SERIALIZER}

    public enum ONBOARDING_ACTION {UPDATE_SELECTION, DOWNLOAD_AGENT, ADD_DEPENDENCIES, NEXT_STATE}
}
