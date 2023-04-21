package com.insidious.plugin.ui;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.DependencyService;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.OnboardingService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.Components.OnboardingScaffoldV3;
import com.insidious.plugin.ui.Components.WaitingScreen;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;

public class OnboardingConfigurationWindow implements OnboardingService {
    private static final Logger logger = LoggerUtil.getInstance(OnboardingConfigurationWindow.class);
    private final Project project;
    public TreeMap<String, String> dependencies_status = new TreeMap<>();
    private JPanel mainPanel;
    private JPanel modulesParentPanel;
    private JLabel selectionHeading1;
    private WaitingScreen waitingScreen;

    private InsidiousService insidiousService;

    public OnboardingConfigurationWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;
        UsageInsightTracker.getInstance()
                .RecordEvent("OnboardingFlowStarted", null);

        waitingScreen = new WaitingScreen();
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
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
        dumbService.runWhenSmart(this::loadOBV3Scaffold);
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

    @Override
    public List<String> fetchModules() {
        return project.getService(InsidiousService.class)
                .fetchModules();
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

    private void downloadAgent(String version) {
//        agentDownloadInitiated = true;
        String host = "https://builds.bug.video/videobug-java-agent-1.10.3-SNAPSHOT-";
        String type = version;
        String extention = ".jar";

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
        Path fileURiString = Constants.AGENT_PATH;
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
            while (checksum.length() < 32) {
                checksum = "0" + checksum;
            }
            switch (agentVersion) {
                case "gson":
                    if (checksum.equals(Checksums.AGENT_GSON)) {
                        return true;
                    }
                    break;
                case "jackson-2.8":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_8)) {
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

    public Map<String, String> getMissingDependencies_v3() {
        InsidiousService insidiousService = project.getService(InsidiousService.class);
        TreeMap<String, String> depVersions = new TreeMap<>();
        List<String> dependenciesToWatch = insidiousService.getProjectTypeInfo()
                .getDependenciesToWatch();
        for (String dependency : dependenciesToWatch) {
            depVersions.put(dependency, null);
        }
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance()
                .getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            for (String dependency : dependenciesToWatch) {
                if (lib.getName()
                        .contains(dependency + ":")) {
                    String version = insidiousService.fetchVersionFromLibName(lib.getName(), dependency);
                    logger.info("Version of " + dependency + " is " + version);
                    depVersions.replace(dependency, version);
                }
            }
            count++;
        }
        System.out.println("[DEP VERSIONS] " + depVersions.toString());
        logger.info("[DEP VERSIONS] Results of dependency search : " + depVersions.toString());
        if (count == 0) {
            //returns everything if not indexed/project import not done.
            return depVersions;
        } else {
            try {
                return computeMissingDependenciesFromStatus(depVersions);
            } catch (Exception e) {
                System.out.println("Exception removing unnecessary deps. "+e);
                e.printStackTrace();
            }
            return depVersions;
        }
    }

    @Override
    public void setSelectedModule(String module) {
        project.getService(InsidiousService.class)
                .setCurrentModule(module);
    }

    private Map<String, String> computeMissingDependenciesFromStatus(Map<String, String> deps) {
        Map<String, String> missing = new TreeMap<>();
        for (String dep : deps.keySet()) {
            if (deps.get(dep) == null) {
                missing.put(dep, deps.get(dep));
            }
        }
        if(missing.containsKey("junit"))
        {
            if(!missing.containsKey("junit-jupiter-engine"))
            {
                //has junit 5, prefer this
                insidiousService.getProjectTypeInfo().setJunitVersion("5");
                missing.remove("junit");
            }
            else
            {
                //no junit 5, use 4
                insidiousService.getProjectTypeInfo().setJunitVersion("4");
                if(missing.containsKey("junit-platform-runner"))
                {
                    missing.remove("junit-platform-runner");
                }
                System.out.println("Case 2");
            }
        }
        else
        {
            insidiousService.getProjectTypeInfo().setJunitVersion("5");
            if(!missing.containsKey("junit-jupiter-engine"))
            {
                missing.put("junit-jupiter-engine",null);
            }
            if(!missing.containsKey("junit-platform-runner"))
            {
                missing.put("junit-platform-runnern",null);
            }
        }
        return missing;
    }

    private void loadOBV3Scaffold() {
        this.mainPanel.removeAll();
        OnboardingScaffoldV3 scaffold = new OnboardingScaffoldV3(this, project);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(scaffold.getComponent(), constraints);
        this.mainPanel.add(gridPanel, BorderLayout.CENTER);
        this.mainPanel.revalidate();
    }

    public void downloadAgentinBackground(String version) {
        Task.Backgroundable dl_task =
                new Task.Backgroundable(project, "Unlogged, Inc.", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Downloading Unlogged agent", null);
                        downloadAgent(version);
                    }
                };
        ProgressManager.getInstance()
                .run(dl_task);
    }


    @Override
    public void postProcessDependencies(Map<String, String> dependencies, Set<String> selections) {
        TreeMap<String, String> dependencies_local = new TreeMap<>();
        for (String dep : selections) {
            if (dependencies.containsKey(dep)) {
                dependencies_local.put(dep, dependencies.get(dep));
            }
        }

        if (dependencies_local.containsKey("jackson-databind")) {
            dependencies_local.remove("jackson-databind");
        }
        new DependencyService().addDependency(project, selections);

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

    @Override
    public void downloadAgentForVersion(String version) {
        downloadAgentinBackground(version);
    }
}
