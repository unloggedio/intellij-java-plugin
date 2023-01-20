package com.insidious.plugin.ui;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.OnboardingService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.Components.ModulePanel;
import com.insidious.plugin.ui.Components.OnboardingV2Scaffold;
import com.insidious.plugin.ui.Components.WaitingScreen;
import com.insidious.plugin.ui.Components.WaitingStateComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;

public class OnboardingConfigurationWindow implements ModuleSelectionListener, OnboardingService {
    private static final Logger logger = LoggerUtil.getInstance(OnboardingConfigurationWindow.class);
    public TreeMap<String, String> dependencies_status = new TreeMap<>();
    private JPanel mainPanel;
    private JPanel modulesParentPanel;
    private JLabel selectionHeading1;
    private Project project;
    private InsidiousService insidiousService;
    private List<ModulePanel> modulePanelList;

    //these are packages that will be excluded in the vm params
    private HashSet<String> selectedPackages = new HashSet<>();
    private HashSet<String> selectedDependencies = new HashSet<>();
    private String JVMoptionsBase = "";
    private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
    private Icon moduleIcon = IconLoader.getIcon("icons/png/moduleIcon.png",
            OnboardingConfigurationWindow.class);
    private Icon packageIcon = IconLoader.getIcon("icons/png/package_v1.png",
            OnboardingConfigurationWindow.class);
    private boolean agentDownloadInitiated = false;
    private boolean addopens = false;
    private WaitingScreen waitingScreen;
    private boolean dependenciesAdditionAttempted = false;

    public OnboardingConfigurationWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        waitingScreen = new WaitingScreen();
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(waitingScreen.getCompenent(), constraints);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();

        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        if (dumbService.isDumb()) {
            InsidiousNotification.notifyMessage("Unlogged is waiting for the indexing to complete.",
                    NotificationType.INFORMATION);
        }
        dumbService.runWhenSmart(() -> {
            startSetupInBackground_v3();
        });
    }

    private void startSetupInBackground_v3() {

        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        setup();
                    }
                });
    }

    //go to docs/missing deps
    private void setup() {
        if (insidiousService.areLogsPresent()) {
            //go to live
            runDownloadCheckWhenLogsExist();
            setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES.SWITCH_TO_LIVE_VIEW,
                    this);
            //insidiousService.addLiveView();
        } else {
            //check for dependencies
            processCheck();
        }
    }

    private void runDownloadCheckWhenLogsExist() {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        searchDependencies_jacksonDatabind();
                    }
                });
    }

    public void setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES state,
                               OnboardingService onboardingService) {
        this.mainPanel.removeAll();
        OnboardingV2Scaffold scaffold = new OnboardingV2Scaffold(this.insidiousService, state,
                this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(scaffold.getComponent(), constraints);
        this.mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
    }

    public void setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES state,
                               Map<String, String> missing_dependencies,
                               OnboardingService onboardingService) {
        this.mainPanel.removeAll();
        OnboardingV2Scaffold scaffold = new OnboardingV2Scaffold(this.insidiousService,
                state, missing_dependencies,
                onboardingService);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(scaffold.getComponent(), constraints);
        this.mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
    }

    public void setupWithState_PostAddition(WaitingStateComponent.WAITING_COMPONENT_STATES state,
                                            Map<String, String> missing_dependencies,
                                            OnboardingService onboardingService) {
        this.mainPanel.removeAll();
        OnboardingV2Scaffold scaffold = new OnboardingV2Scaffold(this.insidiousService,
                state, missing_dependencies,
                onboardingService);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(scaffold.getComponent(), constraints);
        this.mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
    }

    @Override
    public Map<String, String> fetchMissingDependencies() {
        TreeMap<String, String> missing_dependencies = new TreeMap<>();
        for (String key : dependencies_status.keySet()) {
            if (dependencies_status.get(key) == null &&
                    !insidiousService.getProjectTypeInfo().
                            getDependencies_addedManually()
                            .contains(key)) {
                missing_dependencies.put(key, dependencies_status.get(key));
            }
        }
        return missing_dependencies;
    }

    @Override
    public void onSelect(String moduleName) {
    }

    public JComponent getContent() {
        return mainPanel;
    }

    private String buildPackageNameFromList(List<String> parts) {
        if (parts.size() < 2) {
            return "?";
        }
        StringBuilder packagename = new StringBuilder();
        for (String part : parts) {
            packagename.append(part + ".");
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

    @Override
    public List<String> fetchModules() {
        List<Module> modules = List.of(ModuleManager.getInstance(project)
                .getModules());
        Set<String> modules_from_mm = new HashSet<>();
        for (Module module : modules) {
            modules_from_mm.add(module.getName());
        }
        //System.out.println(modules);
        try {
            logger.info("Fetching from POM.xml/settings.gradle");
            Set<String> modules_from_pg = insidiousService.fetchModuleNames();
            modules_from_mm.addAll(modules_from_pg);
        } catch (Exception e) {
            logger.error("Exception fetching modules " + e);
            e.printStackTrace();
            if (modules.size() > 0) {
                List<String> modules_s = new ArrayList<>();
                for (Module module : modules) {
                    modules_s.add(module.getName());
                }
            }
        } finally {
            return new ArrayList<>(modules_from_mm);
        }
    }

    @Override
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

    @Override
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
            }
        }
        String basePackage = buildPackageNameFromList(components);
        if (basePackage.equals("?")) {
            return fetchBasePackage();
        }
        return basePackage;
    }

    public String trimVersion(String version) {
        String versionParts[] = version.split("\\.");
        if (versionParts.length > 2) {
            return versionParts[0] + "." + versionParts[1];
        }
        return version;
    }

    public String[] trimVersions(String[] versions) {
        String[] trimmedVersions = new String[versions.length];
        for (int i = 0; i < versions.length; i++) {
            String versionParts[] = versions[i].split("\\.");
            if (versionParts.length > 2) {
                trimmedVersions[i] = versionParts[0] + "." + versionParts[1];
            } else {
                trimmedVersions[i] = versions[i];
            }
        }
        return trimmedVersions;
    }

    public boolean shouldDownloadAgent() {
        if (!insidiousService.getProjectTypeInfo()
                .isDownloadAgent()) {
            return false;
        }
        Path fileURiString = Path.of(Constants.VIDEOBUG_AGENT_PATH.toUri());
        String absolutePath = fileURiString.toAbsolutePath()
                .toString();
        File agentFile = new File(absolutePath);
        if (agentFile.exists() == false) {
            return true;
        }
        String version = insidiousService.getProjectTypeInfo()
                .getJacksonDatabindVersion();
        if (version != null) {
            version = "jackson-" + version;
        } else {
            version = "gson";
        }
        if (md5Check(version, agentFile)) {
            return false;
        }
        return true;
    }

    private void downloadAgent() {
        if (!shouldDownloadAgent()) {
            logger.info("No need to download agent. Required version is already present");
            //agent already exists with correct version
            return;
        }
        agentDownloadInitiated = true;
        String host = "https://s3.us-west-2.amazonaws.com/dev.bug.video/videobug-java-agent-1.10.1-SNAPSHOT-";
        String type = insidiousService.getProjectTypeInfo()
                .getDefaultAgentType();
        String extention = ".jar";

        if (insidiousService.getProjectTypeInfo()
                .getJacksonDatabindVersion() != null) {
            //fetch jackson
            String version = insidiousService.getProjectTypeInfo()
                    .getJacksonDatabindVersion();
            if (version != null) {
                type = "jackson-" + version;
            }
        }
        checkProgressIndicator("Downloading Unlogged agent", "version : " + type);
        String url = (host + type + extention).trim();
        logger.info("[Downloading from] " + url);
        InsidiousNotification.notifyMessage(
                "Downloading agent for dependency : " + type,
                NotificationType.INFORMATION);
        downloadAgent(url, true);
    }

    private void downloadAgent(String url, boolean overwrite) {
        logger.info("[Starting agent download]");
        UsageInsightTracker.getInstance()
                .RecordEvent("AgentDownloadStart", null);
        Path fileURiString = Path.of(Constants.VIDEOBUG_AGENT_PATH.toUri());
        String absolutePath = fileURiString.toAbsolutePath()
                .toString();

        File agentFile = new File(absolutePath);
        if (agentFile.exists() && !overwrite) {
            return;
        }
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOS = new FileOutputStream(absolutePath)) {
            byte[] data = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
            logger.info("[Agent download complete]");
            if (md5Check(fetchVersionFromUrl(url), agentFile)) {
                InsidiousNotification.notifyMessage("Agent downloaded.",
                        NotificationType.INFORMATION);

            } else {
                InsidiousNotification.notifyMessage(
                        "Agent md5 check failed."
                                + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                        NotificationType.ERROR);
                logger.info("Agent MD5 check failed, checksums are different.");
                UsageInsightTracker.getInstance()
                        .RecordEvent("MD5checkFailed", null);
            }
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("agent_version", fetchVersionFromUrl(url));
            UsageInsightTracker.getInstance()
                    .RecordEvent("AgentDownloadDone", eventProperties);
        } catch (Exception e) {
            logger.info("[Agent download failed]");
            InsidiousNotification.notifyMessage(
                    "Failed to download agent."
                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                    NotificationType.ERROR);

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("exception", e.getMessage());
            UsageInsightTracker.getInstance()
                    .RecordEvent("AgentDownloadException", eventProperties);
        }
    }

    public String fetchVersionFromUrl(String url) {
        if (url.contains("gson")) {
            return "gson";
        } else {
            return url.substring(url.indexOf("-jackson-") + 1, url.indexOf(".jar"));
        }
    }

    public boolean md5Check(String agentVersion, File agent) {
        checkProgressIndicator("Checking md5 checksum", null);
        try {
            byte[] data = Files.readAllBytes(Paths.get(agent.getPath()));
            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(data);
            String checksum = new BigInteger(1, hash).toString(16);
            //System.out.println("Checksum of file " + checksum);
            switch (agentVersion) {
                case "gson":
                    if (checksum.equals(Checksums.AGENT_GSON)) {
                        return true;
                    }
                    break;
                case "jackson-2.9":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_9)) {
                        return true;
                    }
                    break;
                case "jackson-2.10":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_10)) {
                        return true;
                    }
                    break;
                case "jackson-2.11":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_11)) {
                        return true;
                    }
                    break;
                case "jackson-2.12":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_12)) {
                        return true;
                    }
                    break;
                case "jackson-2.13":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_13)) {
                        return true;
                    }
                    break;
                case "jackson-2.14":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_14)) {
                        return true;
                    }
                    break;
            }
        } catch (Exception e) {
            logger.info("Failed to get checksum of downloaded file.");
        }
        return false;
    }

    //fetch all the dependencies from agent.
    private void searchDependencies_generic() {
        UsageInsightTracker.getInstance()
                .RecordEvent("DependencyScanStart", null);
        TreeMap<String, String> depVersions = new TreeMap<>();
        for (String dependency : insidiousService.getProjectTypeInfo()
                .getDependenciesToWatch()) {
            depVersions.put(dependency, null);
        }
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            for (String dependency : insidiousService.getProjectTypeInfo()
                    .getDependenciesToWatch()) {
                if (lib.getName()
                        .contains(dependency + ":")) {
                    String version = fetchVersionFromLibName(lib.getName(), dependency);
                    logger.info("Version of " + dependency + " is " + version);
                    depVersions.replace(dependency, version);
                }
            }

            count++;
        }
        if (count == 0) {
            //logger.info("Project import not complete, waiting.");
            Timer timer = new Timer(3000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    searchDependencies_generic();
                }
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            //search is complete
            this.dependencies_status = depVersions;
            logger.info("[Dependency search status] " + depVersions.toString());
            if (this.dependencies_status.get("jackson-databind") != null) {
                this.insidiousService.getProjectTypeInfo().
                        setJacksonDatabindVersion(this.dependencies_status.get("jackson-databind"));
            }
            if (!agentDownloadInitiated) {
                downloadAgentinBackground();
            }
            UsageInsightTracker.getInstance()
                    .RecordEvent("DependencyScanEnd", null);
            if (fetchMissingDependencies().size() == 0) {
                setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES.WAITING_FOR_LOGS,
                        this);
            } else {
                if (dependenciesAdditionAttempted) {
                    System.out.println("[SYNC FAILED POST WRITE]");
                    setupWithState_PostAddition(WaitingStateComponent.WAITING_COMPONENT_STATES.SWITCH_TO_DOCUMENTATION,
                            fetchMissingDependencies(), this);
                } else {
                    System.out.println("[NO ATTEMPT TO WRITE]");
                    setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES.AWAITING_DEPENDENCY_ADDITION,
                            fetchMissingDependencies(), this);
                }
            }
        }
    }

    private void searchDependencies_jacksonDatabind() {
        TreeMap<String, String> depVersions = new TreeMap<>();
        for (String dependency : insidiousService.getProjectTypeInfo()
                .getDependenciesToWatch()) {
            depVersions.put(dependency, null);
        }
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            if (lib.getName()
                    .contains("jackson-databind:")) {
                insidiousService.getProjectTypeInfo()
                        .setJacksonDatabindVersion(fetchVersionFromLibName(lib.getName(), "jackson-databind"));
            }
            count++;
        }
        if (count == 0) {
            //import of project not complete, wait and rerun
            //System.out.println("Project import not complete, waiting.");
            Timer timer = new Timer(3000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    searchDependencies_generic();
                }
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            //search is complete
            if (!agentDownloadInitiated) {
                downloadAgentinBackground();
            }
        }
    }

    @Override
    public boolean canGoToDocumention() {
        TreeMap<String, String> depVersions = new TreeMap<>();
        for (String dependency : insidiousService.getProjectTypeInfo()
                .getDependenciesToWatch()) {
            depVersions.put(dependency, null);
        }
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            for (String dependency : insidiousService.getProjectTypeInfo()
                    .getDependenciesToWatch()) {
                if (lib.getName()
                        .contains(dependency)) {
                    String version = fetchVersionFromLibName(lib.getName(), dependency);
                    logger.info("Version of " + dependency + " is " + version);
                    depVersions.replace(dependency, version);
                }
            }

            count++;
        }
        if (count == 0) {
            return false;
        } else {
            logger.info("[DEP SEARCH] Can go to Doc section");
            logger.info(depVersions.toString());
            this.dependencies_status = depVersions;
            if (fetchMissingDependencies().size() == 0) {
                return true;
            } else {
                return false;
            }
        }
    }


    public void downloadAgentinBackground() {
        Task.Backgroundable dl_task =
                new Task.Backgroundable(project, "Unlogged, Inc.", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Downloading Unlogged agent", null);
                        downloadAgent();
                    }
                };
        ProgressManager.getInstance()
                .run(dl_task);
    }

    public String fetchVersionFromLibName(String name, String lib) {
        String[] parts = name
                .split(lib + ":");
        String version = trimVersion(parts[parts.length - 1].trim());
        return version;
    }

    @Override
    public void postProcessDependencies(Map<String, String> dependencies, HashSet<String> selections) {
        TreeMap<String, String> dependencies_local = new TreeMap<>();
        for (String dep : selections) {
            if (dependencies.containsKey(dep)) {
                dependencies_local.put(dep, dependencies.get(dep));
            }
        }

        if (dependencies_local.containsKey("jackson-databind")) {
            dependencies_local.remove("jackson-databind");
        }
        if (insidiousService.getProjectTypeInfo()
                .isMaven()) {
            System.out.println("[WRTITING TO POM]");
            writeToPom(dependencies_local);
        } else {
            //check if has build.gradle
            if (true) {
                System.out.println("[WRTITING TO GRADLE]");
                writeToGradle(dependencies_local);
            } else {
                //add to lib
                System.out.println("[NOT MVN OR GRADLE]");
            }
        }
        postprocessCheck();
    }

    @Override
    public Map<String, String> getDependencyStatus() {
        return this.dependencies_status;
    }

    public boolean writeToGradle(TreeMap<String, String> dependencies) {
        @NotNull PsiFile[] gradleFileSearchResult = FilenameIndex.getFilesByName(project, "build.gradle",
                GlobalSearchScope.projectScope(project));
        PsiFile targetFile;
        if (gradleFileSearchResult.length == 1) {
            targetFile = gradleFileSearchResult[0];
        } else if (gradleFileSearchResult.length > 1) {
            targetFile = fetchBaseFile(gradleFileSearchResult);
        } else {
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (String dependency : dependencies.keySet()) {
            if (!shouldWriteDependency(targetFile, dependency) ||
                    insidiousService.getProjectTypeInfo()
                            .getDependencies_addedManually()
                            .contains(dependency)) {
                continue;
            } else {
                insidiousService.getProjectTypeInfo().
                        getDependencies_addedManually()
                        .add(dependency);
            }
            String group_name = "com.fasterxml.jackson.datatype";
            String artifact_name = dependency;
            String version = dependencies.get(dependency);
            if (version == null) {
                sb.append("implementation '" + group_name + ":" + artifact_name + "'\n");
            }
        }
        logger.info("Adding to build.gradle");
        logger.info(sb.toString());
        dependenciesAdditionAttempted = true;
        if (sb.toString()
                .trim()
                .equals("")) {
            //nothing to write
            logger.info("Noting to write into build.gradle");
            return false;
        }
        write_gradle(targetFile, sb.toString());
        return true;
    }

    private boolean writeToPom(TreeMap<String, String> dependencies) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        PsiFile targetFile;
        @NotNull PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml",
                GlobalSearchScope.projectScope(project));
        if (pomFileSearchResult.length == 1) {
            targetFile = pomFileSearchResult[0];
        } else if (pomFileSearchResult.length > 1) {
            targetFile = fetchBaseFile(pomFileSearchResult);
        } else {
            return false;
        }

        for (String dependency : dependencies.keySet()) {
            if (!shouldWriteDependency(targetFile, dependency) ||
                    insidiousService.getProjectTypeInfo()
                            .getDependencies_addedManually()
                            .contains(dependency)) {
                continue;
            } else {
                insidiousService.getProjectTypeInfo().
                        getDependencies_addedManually()
                        .add(dependency);
            }
            String group_id = "com.fasterxml.jackson.datatype";
            String artifact_id = dependency;
            String version = dependencies.get(dependency);
            if (version == null) {
                sb.append("<dependency>\n");
                sb.append("<groupId>" + group_id + "</groupId>\n");
                sb.append("<artifactId>" + artifact_id + "</artifactId>\n");
//                sb.append("<version>"+version+"</version>\n");
                sb.append("</dependency>\n");
            }
        }
//        System.out.println("DEPENDENCIES mvn "+dependencies);
        logger.info("Adding to pom.xml");
        logger.info(sb.toString());
        dependenciesAdditionAttempted = true;
        if (sb.toString()
                .trim()
                .equals("")) {
            //nothing to write
            logger.info("Noting to write into pox.xml");
            return false;
        }
        write_pom(targetFile, sb.toString());
        return true;
    }

    //use dom?
    void write_pom(PsiFile psipomFile, String text) {
        try {
            VirtualFile file = psipomFile.getVirtualFile();
            File pomFile = new File(file.getPath());
            String source = psipomFile.getText();
            String[] parts = source.split("<dependencies>");
            String finalstring = parts[0];
            StringBuilder builder = new StringBuilder(finalstring);
            //+ "\n<dependencies>" + text + "" + parts[1];
            for (int i = 1; i < parts.length; i++) {
                builder.append("\n<dependencies>" + text + "" + parts[i]);
            }
            finalstring = builder.toString();
            try (FileOutputStream out = new FileOutputStream(pomFile)) {
                out.write(finalstring
                        .getBytes(StandardCharsets.UTF_8));

            } catch (Exception e) {
                InsidiousNotification.notifyMessage(
                        "Failed to add dependencies to pom.xml", NotificationType.ERROR);
            }
            InsidiousNotification.notifyMessage(
                    "Dependencies added to pom.xml " + text, NotificationType.INFORMATION
            );
            UsageInsightTracker.getInstance()
                    .RecordEvent("PomDependenciesAdded", null);
        } catch (Exception e) {
            logger.info("Failed to write to pom file " + e);
            e.printStackTrace();
            InsidiousNotification.notifyMessage(
                    "Failed to write to pom."
                            + e.getMessage(), NotificationType.ERROR
            );
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("exception", e.getMessage());
            UsageInsightTracker.getInstance()
                    .RecordEvent("FailedToAddPomDependencies", eventProperties);
        }
    }

    void write_gradle(PsiFile psiGradleFile, String text) {
        try {
            VirtualFile file = psiGradleFile.getVirtualFile();
            File pomFile = new File(file.getPath());
            String source = psiGradleFile.getText();
            String[] parts = source.split("dependencies \\{", 2);
            String finalstring = parts[0] + "\ndependencies {" + text + "" + parts[1];

            try (FileOutputStream out = new FileOutputStream(pomFile)) {
                out.write(finalstring
                        .getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                InsidiousNotification.notifyMessage(
                        "Failed to add dependencies to build.gradle", NotificationType.INFORMATION);
            }
            InsidiousNotification.notifyMessage(
                    "Dependencies added to build.gradle " + text, NotificationType.INFORMATION
            );
            UsageInsightTracker.getInstance()
                    .RecordEvent("GradleDependenciesAdded", null);
        } catch (Exception e) {
            logger.info("Failed to write to build.gradle file " + e);
            e.printStackTrace();
            InsidiousNotification.notifyMessage(
                    "Failed to write to build.gradle. "
                            + e.getMessage(), NotificationType.ERROR
            );
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("exception", e.getMessage());
            UsageInsightTracker.getInstance()
                    .RecordEvent("FailedToAddGradleDependencies", eventProperties);
        }
    }

    public void postprocessCheck() {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        searchDependencies_generic();
                    }
                });
    }

    public void processCheck() {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        searchDependencies_generic();
                    }
                });
    }

    public boolean shouldWriteDependency(PsiFile file, String dependency) {
        String text = file.getText();
        if (text.contains(dependency)) {
            logger.info("Should write dependency? " + dependency + " : false");
            return false;
        } else {
            logger.info("Should write dependency? " + dependency + " : true");
            return true;
        }
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

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText(text1);
            }
        }
    }

    public void copyDependenciesToClipboard(Map<String, String> dependencies) {
        StringBuilder sb = new StringBuilder();
        for (String dependency : dependencies.keySet()) {
            sb.append(getDependencyAdditionText(dependency));
        }
        String final_str = sb.toString();
        insidiousService.copyToClipboard(final_str);
        InsidiousNotification.notifyMessage("Dependencies copied to clipboard.",
                NotificationType.INFORMATION);
    }

    public String getDependencyAdditionText(String dependency) {
        StringBuilder sb = new StringBuilder();
        if (insidiousService.getProjectTypeInfo().isMaven()) {
            String group_id = "com.fasterxml.jackson.datatype";
            String artifact_id = dependency;
            sb.append("<dependency>\n");
            sb.append("<groupId>" + group_id + "</groupId>\n");
            sb.append("<artifactId>" + artifact_id + "</artifactId>\n");
            sb.append("</dependency>\n");
            return sb.toString();
        } else {
            String group_name = "com.fasterxml.jackson.datatype";
            String artifact_name = dependency;
            sb.append("implementation '" + group_name + ":" + artifact_name + "'\n");
            return sb.toString();
        }
    }
}
