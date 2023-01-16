package com.insidious.plugin.ui;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.OnboardingService;
import com.insidious.plugin.factory.VideobugUtils;
import com.insidious.plugin.ui.Components.ModulePanel;
import com.insidious.plugin.ui.Components.OnboardingV2Scaffold;
import com.insidious.plugin.ui.Components.WaitingScreen;
import com.insidious.plugin.ui.Components.WaitingStateComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
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
import io.minio.messages.Progress;
import org.jetbrains.annotations.NotNull;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;

public class OnboardingConfigurationWindow implements ModuleSelectionListener, OnboardingService {
    private static final Logger logger = LoggerUtil.getInstance(OnboardingConfigurationWindow.class);
    private JPanel mainPanel;
    private JPanel modulesParentPanel;
    private JLabel selectionHeading1;
    private Project project;
    private InsidiousService insidiousService;
    private List<ModulePanel> modulePanelList;
    private HashSet<String> selectedPackages = new HashSet<>(); //these are packages that will be excluded in the vm params
    private HashSet<String> selectedDependencies = new HashSet<>();
    private String JVMoptionsBase = "";
    private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
    private Icon moduleIcon = IconLoader.getIcon("icons/png/moduleIcon.png", OnboardingConfigurationWindow.class);
    private Icon packageIcon = IconLoader.getIcon("icons/png/package_v1.png", OnboardingConfigurationWindow.class);
    private boolean agentDownloadInitiated = false;
    private boolean addopens = false;
    public TreeMap<String,String> dependencies_status = new TreeMap<>();

    public OnboardingConfigurationWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        System.out.println("Init waiting screen");
        WaitingScreen waitingScreen = new WaitingScreen();
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0,0,0,0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(waitingScreen.getCompenent(), constraints);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
        System.out.println("waiting screen added");

//        applyConfigButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                routeToDocumentationpage();
//            }
//        });
//        applyConfigButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        linkToDiscordButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                routeToDiscord();
//            }
//        });
//        linkToDiscordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        copyVMoptionsButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                copyVMoptions();
//            }
//        });
//        copyVMoptionsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        addToDependenciesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        Color color = new Color(225,163,54,25);
//        Border border = new LineBorder(UI_Utils.yellow_alert);
//        highlightPanel.setBorder(border);
//        highlightPanel.setBackground(color);
//
//        highlightPanel.setVisible(false);
//        bottomPanel.setVisible(false);
//
//
//        });

        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        if (dumbService.isDumb()) {
            InsidiousNotification.notifyMessage("Unlogged is waiting for the indexing to complete.",
                    NotificationType.INFORMATION);
        }
        dumbService.runWhenSmart(() -> {
            startSetupInBackground_v3();
        });
//        addToDependenciesButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                postProcessDependencies(dependencies_status, selectedDependencies);
//            }
//        });
    }

    public void setAddopens(boolean addopens)
    {
        this.addopens=addopens;
    }

//    private void startSetupInBackground_v2() {
//
//        ApplicationManager.getApplication()
//                .runReadAction(new Runnable() {
//                    public void run() {
//                        setupWindowContent();
//                    }
//                });
//    }

    private void startSetupInBackground_v3() {

        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        setup();
                    }
                });
    }

    //go to docs/missing deps
    private void setup()
    {
        if(insidiousService.areLogsPresent())
        {
            //go to live
            setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES.SWITCH_TO_LIVE_VIEW,this);
            insidiousService.addLiveView();
        }
        else
        {
            //check for dependencies
            processCheck();
        }
    }

    public void setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES state, OnboardingService onboardingService)
    {
        System.out.println("Init scaffold screen");
        this.mainPanel.removeAll();
        OnboardingV2Scaffold scaffold = new OnboardingV2Scaffold(this.insidiousService, state, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0,0,0,0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(scaffold.getComponent(), constraints);
        this.mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
        System.out.println("Init scaffold done");
    }

    public void setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES state, Map<String,String> missing_dependencies, OnboardingService onboardingService)
    {
        System.out.println("Init scaffold screen");
        this.mainPanel.removeAll();
        OnboardingV2Scaffold scaffold = new OnboardingV2Scaffold(this.insidiousService, state, missing_dependencies, onboardingService);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0,0,0,0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(scaffold.getComponent(), constraints);
        this.mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
        System.out.println("Init scaffold done");
    }

    @Override
    public Map<String,String> fetchMissingDependencies()
    {
        TreeMap<String,String> missing_dependencies = new TreeMap<>();
        for(String key : dependencies_status.keySet())
        {
            if(dependencies_status.get(key)==null &&
                !insidiousService.getProjectTypeInfo().
                        getDependencies_addedManually().contains(key))
            {
                missing_dependencies.put(key,dependencies_status.get(key));
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
        System.out.println(modules);
        try {
            System.out.println("Fetching from POM.xml/settings.gradle");
            Set<String> modules_from_pg = insidiousService.fetchModuleNames();
            modules_from_mm.addAll(modules_from_pg);
            //populateModules_v2(new ArrayList<>(modules_from_mm));
        } catch (Exception e) {
            System.out.println("Exception fetching modules");
            System.out.println(e);
            e.printStackTrace();
            if (modules.size() > 0) {
                List<String> modules_s = new ArrayList<>();
                for (Module module : modules) {
                    modules_s.add(module.getName());
                }
            }
        }
        finally {
            return new ArrayList<>(modules_from_mm);
        }
    }

    @Override
    public String fetchBasePackage()
    {
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
    public String fetchBasePackageForModule(String modulename)
    {
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

//    private void runApplicationWithUnlogged() {
//        if (true) {
//            return;
//        }
//        //make run configuration selectable or add vm options to existing run config
//        //wip
//        System.out.println("[VM OPTIONS FROM SELECTION]");
//        if (selectedPackages.size() > 0) {
//            String unloggedVMOptions = buildVmOptionsFromSelections();
//            System.out.println("" + unloggedVMOptions);
//            List<RunnerAndConfigurationSettings> allSettings = project.getService(RunManager.class)
//                    .getAllSettings();
//            for (RunnerAndConfigurationSettings runSetting : allSettings) {
//                System.out.println("runner config - " + runSetting.getName());
//                if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {
//
//                    System.out.println("ApplicationConfiguration config - " + runSetting.getConfiguration()
//                            .getName());
//                    final ProgramRunner runner = DefaultJavaProgramRunner.getInstance();
//                    final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
//                    ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
//                    String currentVMParams = applicationConfiguration.getVMParameters();
//                    String newVmOptions = "";
//                    newVmOptions = VideobugUtils.addAgentToVMParams(currentVMParams, unloggedVMOptions);
//                    //applicationConfiguration.setVMParameters(newVmOptions.trim());
//                    try {
//                        //     runner.execute(new ExecutionEnvironment(executor, runner, runSetting, project), null);
//                        break;
//                    } catch (Exception e) {
//                        System.out.println("Failed to start application");
//                        System.out.println(e);
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
//


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

    public boolean shouldDownloadAgent()
    {
        if(!insidiousService.getProjectTypeInfo().isDownloadAgent())
        {
            return false;
        }
        Path fileURiString = Path.of(Constants.VIDEOBUG_AGENT_PATH.toUri());
        String absolutePath = fileURiString.toAbsolutePath()
                .toString();
        File agentFile = new File(absolutePath);
        System.out.println("Does agent exist? "+agentFile.exists());
        if(agentFile.exists()==false)
        {
            return true;
        }
        String version = insidiousService.getProjectTypeInfo()
                .getJacksonDatabindVersion();
        if (version != null) {
            version = "jackson-" + version;
        }
        else
        {
            version="gson";
        }
        if(md5Check(version,agentFile))
        {
            return false;
        }
        return true;
    }

    private void downloadAgent() {
        if(!shouldDownloadAgent())
        {
            System.out.println("No need to download agent. Required version is already present");
            //agent already exists with correct version
            //prompt
//            InsidiousNotification.notifyMessage(
//                    "Agent with required dependency already present",
//                    NotificationType.INFORMATION);
            return;
        }
        agentDownloadInitiated = true;
//        logger.info("[Downloading agent/Jackson dependency ? ] "+insidiousService.getProjectTypeInfo().getJacksonDatabindVersion());
//        System.out.println("[Downloading agent/Jackson dependency ? ] "+insidiousService.getProjectTypeInfo().getJacksonDatabindVersion());
        String host = "https://s3.us-west-2.amazonaws.com/dev.bug.video/videobug-java-agent-1.8.29-SNAPSHOT-";
        String type = "gson";
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
        String url = (host + type + extention).trim();
        logger.info("[Downloading from] " + url);
        InsidiousNotification.notifyMessage(
                "Downloading agent from link ." + url + ". Downloading to " + Constants.VIDEOBUG_AGENT_PATH,
                NotificationType.INFORMATION);
        downloadAgent(url, true);
    }

    private void downloadAgent(String url, boolean overwrite) {
        logger.info("[starting download]");
        Path fileURiString = Path.of(Constants.VIDEOBUG_AGENT_PATH.toUri());
        String absolutePath = fileURiString.toAbsolutePath()
                .toString();
//        System.out.println("Downloading agent to path - " + absolutePath);

        File agentFile = new File(absolutePath);
//        System.out.println("URL in Download "+url);
        if (agentFile.exists() && !overwrite) {
//            System.out.println("java agent already exists at the path");
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
            }
        } catch (Exception e) {
            logger.info("[Agent download failed]");
//            System.out.println("failed to download java agent"+ e);
            InsidiousNotification.notifyMessage(
                    "Failed to download agent."
                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                    NotificationType.ERROR);
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
        try {
            byte[] data = Files.readAllBytes(Paths.get(agent.getPath()));
            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(data);
            String checksum = new BigInteger(1, hash).toString(16);
            System.out.println("Checksum of file " + checksum);
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
            }
        } catch (Exception e) {
            System.out.println("Failed to get checksum of downloaded file.");
        }
        return false;
    }

    //fetch all the dependencies from agent.
    private void searchDependencies_generic() {
        TreeMap<String,String> depVersions = new TreeMap<>();
        for(String dependency : insidiousService.getProjectTypeInfo().getDependenciesToWatch())
        {
            depVersions.put(dependency,null);
        }
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            for(String dependency : insidiousService.getProjectTypeInfo().getDependenciesToWatch())
            {
                if(lib.getName()
                        .contains(dependency))
                {
                    String version = fetchVersionFromLibName(lib.getName(),dependency);
                    System.out.println("Version of "+dependency+" is "+version);
                    depVersions.replace(dependency,version);
                }
            }

            count++;
        }
        if (count == 0) {
            //import of project not complete, wait and rerun
            System.out.println("Project import not complete, waiting.");
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
            System.out.println("[DEP SEARCH] Dependencies final found");
            System.out.println(depVersions);
            this.dependencies_status=depVersions;
            //updateDependenciesTab();
            //move to another bg thread
            if (!agentDownloadInitiated) {
                downloadAgent();
            }
            if(fetchMissingDependencies().size()==0)
            {
                //go to docs
                System.out.println("Going to docs");
                setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES.WAITING_FOR_LOGS, this);
            }
            else
            {
                //go to dep mgmt
                System.out.println("Dep mgmt");
                setupWithState(WaitingStateComponent.WAITING_COMPONENT_STATES.AWAITING_DEPENDENCY_ADDITION, fetchMissingDependencies(),this);
            }
            //postProcessDependencies(depVersions);
        }
    }

    @Override
    public boolean canGoToDocumention() {
        TreeMap<String,String> depVersions = new TreeMap<>();
        for(String dependency : insidiousService.getProjectTypeInfo().getDependenciesToWatch())
        {
            depVersions.put(dependency,null);
        }
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            for(String dependency : insidiousService.getProjectTypeInfo().getDependenciesToWatch())
            {
                if(lib.getName()
                        .contains(dependency))
                {
                    String version = fetchVersionFromLibName(lib.getName(),dependency);
                    System.out.println("Version of "+dependency+" is "+version);
                    depVersions.replace(dependency,version);
                }
            }

            count++;
        }
        if (count == 0) {
            return false;
        } else {
            System.out.println("[DEP SEARCH] Can go to Doc section");
            System.out.println(depVersions);
            this.dependencies_status=depVersions;
            if(fetchMissingDependencies().size()==0)
            {
                System.out.println("can go to docs");
                return true;
            }
            else
            {
                System.out.println("can't go to docs");
                return false;
            }
        }
    }

//
//    public void downloadAgentinBackground()
//    {
//        Task.Backgroundable dl_task =
//            new Task.Backgroundable(project, "Unlogged, Inc.", true) {
//                @Override
//                public void run(@NotNull ProgressIndicator indicator) {
//                }
//            };
//        ProgressManager.getInstance().run(dl_task);
//    }
//
    public String fetchVersionFromLibName(String name, String lib)
    {
        String[] parts = name
                .split(lib+":");
        String version = trimVersion(parts[parts.length - 1].trim());
        return version;
    }

    @Override
    public void postProcessDependencies(Map<String,String> dependencies, HashSet<String> selections)
    {
        TreeMap<String,String> dependencies_local = new TreeMap<>();
        for(String dep : selections)
        {
            if(dependencies.containsKey(dep))
            {
                dependencies_local.put(dep,dependencies.get(dep));
            }
        }

        if(dependencies_local.containsKey("jackson-databind"))
        {
            dependencies_local.remove("jackson-databind");
        }
        if(insidiousService.getProjectTypeInfo().isMaven())
        {
            writeToPom(dependencies_local);
        }
        else
        {
            //check if has build.gradle
            if(true)
            {
                writeToGradle(dependencies_local);
            }
            else
            {
                //add to lib
            }
        }
        postprocessCheck();
    }

    @Override
    public Map<String, String> getDependencyStatus() {
        return this.dependencies_status;
    }

    public void writeToGradle(TreeMap<String,String> dependencies)
    {
        @NotNull PsiFile[] gradleFileSearchResult = FilenameIndex.getFilesByName(project, "build.gradle",
                GlobalSearchScope.projectScope(project));
        PsiFile targetFile;
        if(gradleFileSearchResult.length == 1)
        {
            targetFile = gradleFileSearchResult[0];
        } else if (gradleFileSearchResult.length>1) {
            targetFile = fetchBaseFile(gradleFileSearchResult);
        }
        else
        {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for(String dependency : dependencies.keySet())
        {
            if(!shouldWriteDependency(targetFile,dependency) ||
                    insidiousService.getProjectTypeInfo()
                            .getDependencies_addedManually().contains(dependency))
            {
                continue;
            }
            else
            {
                insidiousService.getProjectTypeInfo().
                        getDependencies_addedManually().add(dependency);
            }
            String group_name = "com.fasterxml.jackson.datatype";
            String artifact_name = dependency;
            String version = dependencies.get(dependency);
            if(version==null)
            {
                sb.append("implementation '"+group_name+":"+artifact_name+"'\n");
            }
        }
        System.out.println("Adding to build.gradle");
        System.out.println(sb.toString());
        if(sb.toString().trim().equals(""))
        {
            //nothing to write
            InsidiousNotification.notifyMessage("Nothing to write into build.gradle",NotificationType.ERROR);
            return;
        }
        write_gradle(targetFile,sb.toString());

    }

    public void writeToPom(TreeMap<String,String> dependencies)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        PsiFile targetFile;
        @NotNull PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml",
                GlobalSearchScope.projectScope(project));
        if(pomFileSearchResult.length == 1)
        {
            targetFile=pomFileSearchResult[0];
        } else if (pomFileSearchResult.length>1) {
            targetFile=fetchBaseFile(pomFileSearchResult);
        }
        else{
            return;
        }

        for(String dependency : dependencies.keySet())
        {
            if(!shouldWriteDependency(targetFile,dependency) ||
                    insidiousService.getProjectTypeInfo()
                            .getDependencies_addedManually().contains(dependency))
            {
                continue;
            }
            else
            {
                insidiousService.getProjectTypeInfo().
                        getDependencies_addedManually().add(dependency);
            }
            String group_id = "com.fasterxml.jackson.datatype";
            String artifact_id = dependency;
            String version = dependencies.get(dependency);
            if(version==null)
            {
                sb.append("<dependency>\n");
                sb.append("<groupId>"+group_id+"</groupId>\n");
                sb.append("<artifactId>"+artifact_id+"</artifactId>\n");
//                sb.append("<version>"+version+"</version>\n");
                sb.append("</dependency>\n");
            }
        }

        System.out.println("Adding to Pom");
        System.out.println(sb.toString());
        if(sb.toString().trim().equals(""))
        {
            //nothing to write
            InsidiousNotification.notifyMessage("Nothing to write into pom.xml",NotificationType.ERROR);
            return;
        }
        write_pom(targetFile,sb.toString());
    }

    //use dom?
    void write_pom(PsiFile psipomFile, String text)
    {
        try {
            VirtualFile file = psipomFile.getVirtualFile();
            File pomFile = new File(file.getPath());
            String source = psipomFile.getText();
            String[] parts = source.split("<dependencies>");
            String finalstring = parts[0]+"\n<dependencies>"+text+""+parts[1];

                try (FileOutputStream out = new FileOutputStream(pomFile)) {
                    out.write(finalstring
                            .getBytes(StandardCharsets.UTF_8));

                } catch (Exception e) {
                    InsidiousNotification.notifyMessage(
                    "Failed to add dependencies to pom.xml", NotificationType.ERROR);
                }
            InsidiousNotification.notifyMessage(
                    "Dependencies added to pom.xml "+text, NotificationType.INFORMATION
            );
        }
        catch (Exception e)
        {
            System.out.println("Failed to write to pom file "+e);
            e.printStackTrace();
            InsidiousNotification.notifyMessage(
                    "Failed to write to pom."
                            + e.getMessage(), NotificationType.ERROR
            );
        }
    }

    void write_gradle(PsiFile psiGradleFile, String text)
    {
        try {
            VirtualFile file = psiGradleFile.getVirtualFile();
            File pomFile = new File(file.getPath());
            String source = psiGradleFile.getText();
            String[] parts = source.split("dependencies \\{");
            String finalstring = parts[0]+"\ndependencies {"+text+""+parts[1];

            try (FileOutputStream out = new FileOutputStream(pomFile)) {
                out.write(finalstring
                        .getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                InsidiousNotification.notifyMessage(
                        "Failed to add dependencies to build.gradle", NotificationType.INFORMATION);
            }
            InsidiousNotification.notifyMessage(
                    "Dependencies added to build.gradle "+text, NotificationType.INFORMATION
            );
        }
        catch (Exception e)
        {
            System.out.println("Failed to write to build.gradle file "+e);
            e.printStackTrace();
            InsidiousNotification.notifyMessage(
                    "Failed to write to build.gradle. "
                            + e.getMessage(), NotificationType.ERROR
            );
        }
    }

    public void postprocessCheck()
    {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        searchDependencies_generic();
                    }
                });
    }

    public void processCheck()
    {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        searchDependencies_generic();
                    }
                });
    }

    public boolean shouldWriteDependency(PsiFile file, String dependency)
    {
        String text = file.getText();
        if(text.contains(dependency))
        {
            System.out.println("Should write dependency? "+dependency +" : false");
            return false;
        }
        else
        {
            System.out.println("Should write dependency? "+dependency +" : true");
            return true;
        }
    }

    public PsiFile fetchBaseFile(PsiFile[] files)
    {
        Map<Integer,PsiFile> sizemaps = new HashMap<>();
        for(PsiFile file : files)
        {
            Integer ps = file.getVirtualFile().getPath().length();
            sizemaps.put(ps,file);
        }
        List<Integer> keys = new ArrayList<>(sizemaps.keySet());
        Collections.sort(keys);
        return sizemaps.get(keys.get(0));
    }
}
