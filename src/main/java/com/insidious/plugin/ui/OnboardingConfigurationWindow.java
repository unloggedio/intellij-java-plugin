package com.insidious.plugin.ui;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.VideobugUtils;
import com.insidious.plugin.ui.Components.ModulePanel;
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
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

public class OnboardingConfigurationWindow implements ModuleSelectionListener{
    private JPanel mainPanel;
    private JPanel modulesSelectionPanel;
    private JPanel packagesSelectionPanel;
    private JPanel documentationSection;
    private JLabel modulesHeading;
    private JPanel modulesParentPanel;
    private JPanel packagesParentPanel;
    private JButton applyConfigButton;
    private JButton linkToDiscordButton;
    private JLabel selectionHeading1;
    private JTextPane vmOptionsPanel_1;
    private JPanel packageManagementBorderParent;
    private JPanel includePanel;
    private JPanel excludePanel;
    private JPanel BasePackagePanel;
    private JLabel basePackageLabel;
    private JScrollPane scrollParent;
    private JPanel sectionParent;
    private JPanel borderLayoutParent;
    private JPanel bottomContent;
    private JPanel buttonGroupPanel;
    private JTextPane documentationTextArea;
    private JLabel DocumentationLabel;
    private JScrollPane vmOptsScroll;
    private JLabel includeHeadingLabel;
    private JPanel topButtonGroup;
    private JButton copyVMoptionsButton;
    private Project project;
    private InsidiousService insidiousService;
    private List<ModulePanel> modulePanelList;
    private HashSet<String> selectedPackages = new HashSet<>(); //these are packages that will be excluded in the vm params
    private String JVMoptionsBase = "";
    private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
    private Icon moduleIcon = IconLoader.getIcon("icons/png/moduleIcon.png", OnboardingConfigurationWindow.class);
    private Icon packageIcon = IconLoader.getIcon("icons/png/package_v1.png", OnboardingConfigurationWindow.class);
    private static final Logger logger = LoggerUtil.getInstance(OnboardingConfigurationWindow.class);

    private boolean agentDownloadInitiated=false;

    public OnboardingConfigurationWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;
        this.JVMoptionsBase = getJVMoptionsBase();
        applyConfigButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDocumentationpage();
            }
        });
        applyConfigButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkToDiscordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
        linkToDiscordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyVMoptionsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                copyVMoptions();
            }
        });
        copyVMoptionsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        if(dumbService.isDumb())
        {
            InsidiousNotification.notifyMessage("Unlogged is waiting for the indexing to complete.",
                    NotificationType.INFORMATION);
        }
        dumbService.runWhenSmart(() -> {startSetupInBackground_v2();});
    }

    private void startSetupInBackground_v2()
    {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    public void run() {
                        setupWindowContent();
                    }
                });
            }
        });
    }

    private void setupWindowContent()
    {
        fetchModules();
        findAllPackages();
        updateVMparameter();
        try {
            this.basePackageLabel.setToolTipText("Base package for " + modulePanelList.get(0).getText());
        }
        catch (Exception e)
        {
            System.out.println("No modules, can't set tooltip text");
        }
        try
        {
            if(insidiousService.getProjectTypeInfo().isDetectDependencies())
            {
                searchJacksonDatabindVersion();
            }
        }
        catch (Exception e)
        {
            System.out.println("Exception downloading agent"+e);
            e.printStackTrace();
        }
    }

    private void copyVMoptions()
    {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("i="+basePackageLabel.getText());
        if(selectedPackages.size()>0)
        {
            newVMParams.append(",");
            for(String packageName : selectedPackages)
            {
                newVMParams.append("e="+packageName+",");
            }
            newVMParams.deleteCharAt(newVMParams.length()-1);
            newVMParams.append("\"");
        }
        else
        {
            newVMParams.append("\"");
        }
        insidiousService.copyToClipboard(newVMParams.toString());
        InsidiousNotification.notifyMessage("VM options copied to clipboard.",
                NotificationType.INFORMATION);
    }
    private String getJVMoptionsBase()
    {
            String vmoptions = javaAgentString+"=";
            return vmoptions;
    }
    private void routeToDiscord()
    {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
            } catch (Exception e) { }
        } else {
            //no browser
        }
    }

    private void routeToDocumentationpage()
    {
        String link = "https://docs.unlogged.io";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
            } catch (Exception e) { }
        } else {
            //no browser
        }
    }

    public JComponent getContent() {
        return mainPanel;
    }

    //add 1 for base package if not in file, search in pom file and add to set before sorting
    public void findAllPackages()
    {
        Set<String> ret = new HashSet<String>();
        Collection<VirtualFile> virtualFiles =
                FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, JavaFileType.INSTANCE,
                        GlobalSearchScope.projectScope(project));

        List<String> components = new ArrayList<String>();
        for (VirtualFile vf: virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project).findFile(vf);
            if (psifile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if(packageName.contains("."))
                {
                    ret.add(packageName);
                    if(components.size()==0)
                    {
                        String[] parts = packageName.split("\\.");
                        components = Arrays.asList(parts);
                    }
                    else
                    {
                        List<String> sp = Arrays.asList(packageName.split("\\."));
                        List<String> intersection = intersection(components,sp);
                        if(intersection.size()>=2)
                        {
                            components=intersection;
                        }
                    }
                }
            }
        }
//        System.out.println("Project packages all - "+ret);
//        System.out.println("[Components] from all "+components);
        String basePackage = buildPackageNameFromList(components);
        this.basePackageLabel.setText(basePackage);
        if(basePackage.equals("?"))
        {
            this.basePackageLabel.setToolTipText("If you see a ? please wait till index is complete and click the module again");
        }
        ArrayList<String> packages = new ArrayList<String>(ret);
        Collections.sort(packages);
        populatePackages(packages);
    }

    public void findPackagesForModule(String modulename)
    {
        Set<String> ret = new HashSet<String>();
        Collection<VirtualFile> virtualFiles =
                FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, JavaFileType.INSTANCE,
                        GlobalSearchScope.projectScope(project));

        List<String> components = new ArrayList<String>();
        for (VirtualFile vf: virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project).findFile(vf);
            if (psifile instanceof PsiJavaFile && vf.getPath().contains(modulename)) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if(packageName.contains("."))
                {
                    ret.add(packageName);
                    if(components.size()==0)
                    {
                        String[] parts = packageName.split("\\.");
                        components = Arrays.asList(parts);
                    }
                    else
                    {
                        List<String> sp = Arrays.asList(packageName.split("\\."));
                        List<String> intersection = intersection(components,sp);
                        if(intersection.size()>=2)
                        {
                            components=intersection;
                        }
                    }
                }
            }
        }
//        System.out.println("Project packages from module - "+ret);
//        System.out.println("[Components] from module "+components);
        String basePackage = buildPackageNameFromList(components);
        this.basePackageLabel.setText(basePackage);
        if(basePackage.equals("?"))
        {
            this.basePackageLabel.setToolTipText("If you see a ? please wait till index is complete and click the module again");
        }
        ArrayList<String> packages = new ArrayList<String>(ret);
        Collections.sort(packages);
        populatePackages(packages);
    }

    private String buildPackageNameFromList(List<String> parts)
    {
        if(parts.size()<2)
        {
            return "?";
        }
        StringBuilder packagename = new StringBuilder();
        for(String part : parts)
        {
            packagename.append(part+".");
        }
        packagename.deleteCharAt(packagename.length()-1);
        return packagename.toString();
    }

    public <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();
        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }

    //ModuleManager doesn't point to correct modules,
    //refer to pom/gradle modules section, else fallback to moduleManager and display all packages
    public void fetchModules()
    {
        List<Module> modules = List.of(ModuleManager.getInstance(project).getModules());
        Set<String> modules_from_mm = new HashSet<>();
        for(Module module: modules)
        {
            modules_from_mm.add(module.getName());
        }
        System.out.println(modules);
        try
        {
            System.out.println("Fetching from POM.xml/settings.gradle");
            Set<String> modules_from_pg = insidiousService.fetchModuleNames();
            modules_from_mm.addAll(modules_from_pg);
            populateModules_v1(new ArrayList<>(modules_from_mm));
        }
        catch (Exception e)
        {
            System.out.println("Exception fetching modules");
            System.out.println(e);
            e.printStackTrace();
            if(modules.size()>0)
            {
                List<String> modules_s = new ArrayList<>();
                for(Module module : modules)
                {
                    modules_s.add(module.getName());
                }
                populateModules_v1(modules_s);
            }
        }
    }

    public void populateModules_v1(List<String> modules)
    {
        this.modulesParentPanel.removeAll();
        int GridRows = 20;
        if (modules.size() > GridRows) {
            GridRows = modules.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
//        gridLayout.setVgap(8);
        JPanel gridPanel = new JPanel(gridLayout);
        Dimension d = new Dimension();
        d.setSize(-1,30);
        modulePanelList = new ArrayList<ModulePanel>();
        for (int i = 0; i < modules.size(); i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            ModulePanel modulePanel = new ModulePanel(modules.get(i),this);
            modulePanelList.add(modulePanel);
            JPanel mainPanel = modulePanel.getMainPanel();
            mainPanel.setPreferredSize(d);
            mainPanel.setMaximumSize(d);
            mainPanel.setMaximumSize(d);
            gridPanel.add(modulePanel.getMainPanel(), constraints);
        }
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        EmptyBorder emptyBorder = new EmptyBorder(0,0,0,0);
        scrollPane.setBorder(emptyBorder);
        modulesParentPanel.setPreferredSize(scrollPane.getSize());
        modulesParentPanel.add(scrollPane, BorderLayout.CENTER);
        if (modules.size() <= 8) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        if (modules.size() > 0) {
        }
        this.modulesParentPanel.revalidate();
    }

    public void populatePackages(List<String> packages)
    {
        this.packagesParentPanel.removeAll();
        int GridRows = 50;
        if (packages.size() > GridRows) {
            GridRows = packages.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        Dimension d = new Dimension();
        d.setSize(-1,20);
        JPanel gridPanel = new JPanel(gridLayout);
        int i = 0;
        for(String packagename : packages)
        {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            constraints.setIndent(16);
            JCheckBox checkBox = new JCheckBox();
            checkBox.setMinimumSize(d);
            checkBox.setPreferredSize(d);
            checkBox.setMaximumSize(d);
            checkBox.setOpaque(true);
            checkBox.setText(packagename);
            checkBox.setIcon(packageIcon);
            checkBox.getInsets().set(4,16,4,0);
            checkBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Component component = e.getComponent();
                    if(component instanceof JCheckBox)
                    {
                        JCheckBox checkBox = (JCheckBox) component;
                        String package_selected = checkBox.getText();
                        if(!checkBox.isSelected() && selectedPackages.contains(package_selected))
                        {
                            selectedPackages.remove(package_selected);
                        }
                        else
                        {
                            if(!selectedPackages.contains(package_selected))
                            {
                                selectedPackages.add(package_selected);
                            }
                        }
                        updateVMparameter();
                    }
                }
            });
            //checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            gridPanel.add(checkBox, constraints);
            i++;
        }
        gridPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        EmptyBorder emptyBorder = new EmptyBorder(0,0,0,0);
        scrollPane.setBorder(emptyBorder);
        packagesParentPanel.setPreferredSize(scrollPane.getSize());
        packagesParentPanel.add(scrollPane, BorderLayout.CENTER);
//        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        if (packages.size() <= 15) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        this.packagesParentPanel.revalidate();
    }

    private void runApplicationWithUnlogged()
    {
        if(true)
        {
            return;
        }
        //make run configuration selectable or add vm options to existing run config
        //wip
        System.out.println("[VM OPTIONS FROM SELECTION]");
        if(selectedPackages.size()>0) {
            String unloggedVMOptions = buildVmOptionsFromSelections();
            System.out.println("" + unloggedVMOptions);
            List<RunnerAndConfigurationSettings> allSettings = project.getService(RunManager.class)
                    .getAllSettings();
            for (RunnerAndConfigurationSettings runSetting : allSettings) {
                System.out.println("runner config - " + runSetting.getName());
                if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {

                    System.out.println("ApplicationConfiguration config - " + runSetting.getConfiguration().getName());
                    final ProgramRunner runner = DefaultJavaProgramRunner.getInstance();
                    final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
                    ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
                    String currentVMParams = applicationConfiguration.getVMParameters();
                    String newVmOptions = "";
                    newVmOptions = VideobugUtils.addAgentToVMParams(currentVMParams, unloggedVMOptions);
                    //applicationConfiguration.setVMParameters(newVmOptions.trim());
                    try {
                    //     runner.execute(new ExecutionEnvironment(executor, runner, runSetting, project), null);
                         break;
                    }
                    catch (Exception e)
                    {
                        System.out.println("Failed to start application");
                        System.out.println(e);
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String buildVmOptionsFromSelections()
    {
        String javaAgentVMString = insidiousService.getJavaAgentString();
        String parts[] = javaAgentVMString.split("i=");
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]);
        for(String selection : selectedPackages)
        {
            sb.append("i="+selection+",");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public void onSelect(String moduleName) {
        this.selectedPackages = new HashSet<>();
        findPackagesForModule(moduleName);
        updateVMparameter();
        this.basePackageLabel.setToolTipText("Base package for "+moduleName);
    }

    private void updateVMparameter()
    {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("\ni="+basePackageLabel.getText());
        if(selectedPackages.size()>0)
        {
            newVMParams.append(",");
            for(String packageName : selectedPackages)
            {
                newVMParams.append("\ne="+packageName+",");
            }
            newVMParams.deleteCharAt(newVMParams.length()-1);
            newVMParams.append("\"");
            vmOptionsPanel_1.setText(newVMParams.toString());
        }
        else
        {
            newVMParams.append("\"");
            vmOptionsPanel_1.setText(newVMParams.toString());
        }
    }

    public void fetchDependencies()
    {
        logger.info("Starting dependency search");
        String command="";
        if(insidiousService.getProjectTypeInfo().isMaven())
        {
            command = "mvn dependency:tree";

        }
        else
        {
            command = "gradle dependencies";
        }
        try
        {
            String outlist[] = runCommandGeneric(command);
            if(insidiousService.getProjectTypeInfo().isMaven()) {
                processMavenDependencyTree(outlist);
            }
            else
            {
                processGradleDependencyTree(outlist);
            }
        }
        catch (IOException e) {
            logger.info(e);
            e.printStackTrace();
            InsidiousNotification.notifyMessage(
                    "Couldn't detect dependencies."
                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                    NotificationType.ERROR);
        }
    }

    public void fetchRunnerVersion()
    {
        logger.info("Starting dependency search");
        String command="";
        if(insidiousService.getProjectTypeInfo().isMaven())
        {
            command = "mvn -v";

        }
        else
        {
            command = "gradle -v";
        }
        try
        {
            String outlist[] = runCommandGeneric(command);
            System.out.println("[VERSION TEXT]");
            for(int i=0;i<outlist.length;i++)
            {
                System.out.println(""+outlist[i]);
            }
        }
        catch (IOException e) {
            logger.info(e);
            e.printStackTrace();
            InsidiousNotification.notifyMessage(
                    "Couldn't detect dependencies."
                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                    NotificationType.ERROR);
        }
    }

    public String[] runCommandGeneric(String cmd) throws IOException
    {
        System.out.println("Running command [Dependency Tree]");
        ArrayList list = new ArrayList();
        Process proc = Runtime.getRuntime().exec(cmd,new String[0],new File(project.getBasePath()));
        InputStream istr = proc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(istr));
        String str;
        while ((str = br.readLine()) != null)
            list.add(str);
        try {
            proc.waitFor();
        }
        catch (InterruptedException e) {
            System.err.println("Process was interrupted");
        }
        br.close();
        return (String[])list.toArray(new String[0]);
    }

    // currently only picks up jackson - databind version
    public void processMavenDependencyTree(String[] stringBase)
    {
        System.out.println("Processing Maven Dependency Tree");
        HashMap<String,String> dependencies = new HashMap<String,String>();
        for(int i=0;i<stringBase.length;i++)
        {
            String temp = stringBase[i];
            if(temp.contains("jackson") && temp.contains("databind")) //if(temp.contains("jackson") || temp.contains("gson")) for all gson and jackson dependencies
            {
                //System.out.println("Temp Jackson "+temp);
                String[] parts = temp.split(" ");
                for(int x=0;x<parts.length;x++)
                {
                    if(parts[x].startsWith("com."))
                    {
                        String depStr = parts[x];
                        String[] depSlices = depStr.split(":");
                        dependencies.put(""+depSlices[0]+":"+depSlices[1],trimVersion(depSlices[3]));
                    }
                }
            }
        }
        System.out.println("Maven Serializer Dependencies");
        for(String key : dependencies.keySet())
        {
            System.out.println("Dep [MVN] - "+key+" -> "+dependencies.get(key));
        }
        insidiousService.getProjectTypeInfo().getSerializers().add(dependencies);
    }

    public String trimVersion(String version)
    {
        String versionParts[] = version.split("\\.");
        if(versionParts.length>2)
        {
            return versionParts[0]+"."+versionParts[1];
        }
        return version;
    }

    public String[] trimVersions(String[] versions)
    {
        String[] trimmedVersions = new String[versions.length];
        for(int i=0;i<versions.length;i++)
        {
            String versionParts[] = versions[i].split("\\.");
            if(versionParts.length>2)
            {
                trimmedVersions[i] = versionParts[0]+"."+versionParts[1];
            }
            else {
                trimmedVersions[i]=versions[i];
            }
        }
        return trimmedVersions;
    }

    //picks up only jackson-databind
    public void processGradleDependencyTree(String[] stringBase)
    {
        System.out.println("Processing Gradle Dependency Tree");
        HashMap<String,String> dependencies = new HashMap<String,String>();
        for(int i=0;i<stringBase.length;i++)
        {
            String temp = stringBase[i];
            if(temp.contains("jackson") && temp.contains("databind"))
            {
                //System.out.println(""+temp);
                String[] parts = temp.split(" ");
                for(int x=0;x<parts.length;x++)
                {
                    if(parts[x].startsWith("com."))
                    {
                        String[] dependencyparts = parts[x].split(":");
                        dependencies.put(""+dependencyparts[0]+":"+dependencyparts[1],trimVersion(dependencyparts[2]));
                    }
                }
            }
        }
        System.out.println("Gradle Serializer Dependencies");
        for(String key : dependencies.keySet())
        {
            System.out.println("Dep [Gradle] - "+key+" -> "+dependencies.get(key));
        }
        insidiousService.getProjectTypeInfo().getSerializers().add(dependencies);
    }

    private void downloadAgent()
    {
        agentDownloadInitiated=true;
//        logger.info("[Downloading agent/Jackson dependency ? ] "+insidiousService.getProjectTypeInfo().getJacksonDatabindVersion());
//        System.out.println("[Downloading agent/Jackson dependency ? ] "+insidiousService.getProjectTypeInfo().getJacksonDatabindVersion());
        String host = "https://s3.us-west-2.amazonaws.com/dev.bug.video/videobug-java-agent-1.8.29-SNAPSHOT-";
        String type = "gson";
        String extention = ".jar";

        if(insidiousService.getProjectTypeInfo().getJacksonDatabindVersion()!=null)
        {
            //fetch jackson
            String version = insidiousService.getProjectTypeInfo().getJacksonDatabindVersion();
            if(version!=null)
            {
                type="jackson-"+version;
            }
        }
        String url = (host+type+extention).trim();
        logger.info("[Downloading from] "+url);
        InsidiousNotification.notifyMessage("Downloading agent from link ." +url+". Downloading to "+Constants.VIDEOBUG_AGENT_PATH,
                NotificationType.INFORMATION);
        downloadAgent(url,true);
    }

    private void downloadAgent(String url, boolean overwrite)
    {
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
            if(md5Check(fetchVersionFromUrl(url),agentFile))
            {
                InsidiousNotification.notifyMessage("Agent downloaded.",
                        NotificationType.INFORMATION);
            }
            else
            {
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

    //needs to be post indexing
    private void searchJacksonDatabindVersion()
    {
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(insidiousService.getProject());
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext())
        {
            Library lib = lib_iterator.next();
            if(lib.getName().contains("com.fasterxml.jackson.core:jackson-databind"))
            {
                String[] parts = lib.getName().split("com.fasterxml.jackson.core:jackson-databind:");
                String version = trimVersion(parts[parts.length-1].trim());
//                System.out.println("Jackson databind version = "+version);
                insidiousService.getProjectTypeInfo().setJacksonDatabindVersion(version);
                if(!agentDownloadInitiated)
                {
                    downloadAgent();
                }
                return;
            }
            count++;
        }
        if(count==0)
        {
            //import of project not complete, wait and rerun
            System.out.println("Project import not complete, waiting.");
            Timer timer = new Timer(3000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    searchJacksonDatabindVersion();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
        else
        {
            //import complete, but no jackson dependency found. Use GSON
            if(!agentDownloadInitiated)
            {
                downloadAgent();
            }
            return;
        }
    }

    public String fetchVersionFromUrl(String url)
    {
        if(url.contains("gson"))
        {
            return "gson";
        }
        else
        {
            return url.substring(url.indexOf("-jackson-")+1,url.indexOf(".jar"));
        }
    }
    public boolean md5Check(String agentVersion, File agent)
    {
        try {
            byte[] data = Files.readAllBytes(Paths.get(agent.getPath()));
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            String checksum = new BigInteger(1, hash).toString(16);
            System.out.println("Checksum of file "+checksum);
            switch (agentVersion)
            {
                case "gson" :
                    if (checksum.equals(Checksums.AGENT_GSON))
                    {
                        return true;
                    }
                    break;
                case "jackson-2.9":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_9))
                    {
                        return true;
                    }
                    break;
                case "jackson-2.10":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_10))
                    {
                        return true;
                    }
                    break;
                case "jackson-2.11":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_11))
                    {
                        return true;
                    }
                    break;
                case "jackson-2.12":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_12))
                    {
                        return true;
                    }
                    break;
                case "jackson-2.13":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_13))
                    {
                        return true;
                    }
                    break;
            }
        }
        catch (Exception e)
        {
            System.out.println("Failded to get checksum of downloaded file.");
        }
        return false;
    }
}
