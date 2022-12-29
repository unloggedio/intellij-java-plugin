package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.VideobugUtils;
import com.insidious.plugin.ui.Components.ModulePanel;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.indexing.FileBasedIndex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private JLabel documentationText;
    private JPanel borderLayoutParent;
    private JPanel bottomContent;
    private JPanel textParentPanel;
    private JLabel docText;
    private JButton applyConfigButton;
    private Project project;
    private InsidiousService insidiousService;

    private HashSet<String> selectedPackages = new HashSet<>();
    private Icon moduleIcon = IconLoader.getIcon("icons/png/moduleIcon.png", OnboardingConfigurationWindow.class);
    private Icon packageIcon = IconLoader.getIcon("icons/png/package_v1.png", OnboardingConfigurationWindow.class);
    public OnboardingConfigurationWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;
        fetchModules();
        findAllPackages();
        applyConfigButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                runApplicationWithUnlogged();
            }
        });
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

        // Get only java files which is contained by PROJECT
        for (VirtualFile vf: virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project).findFile(vf);
            if (psifile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if(packageName.contains("."))
                {
                    ret.add(packageName);
                }
            }
        }
        System.out.println("Project packages - "+ret);
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

        for (VirtualFile vf: virtualFiles) {
            PsiFile psifile = PsiManager.getInstance(project).findFile(vf);
            if (psifile instanceof PsiJavaFile && vf.getPath().contains(modulename)) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psifile;
                String packageName = psiJavaFile.getPackageName();
                if(packageName.contains("."))
                {
                    ret.add(packageName);
                }
            }
        }
        System.out.println("Project packages - "+ret);
        ArrayList<String> packages = new ArrayList<String>(ret);
        Collections.sort(packages);
        populatePackages(packages);
    }

    //ModuleManager doesn't point to correct modules,
    //refer to pom/gradle modules section, else fallback to moduleManager and display all packages
    public void fetchModules()
    {
        //modules
        System.out.println("Details from PSI project - ");
        System.out.println("Modules - ");
        List<Module> modules = List.of(ModuleManager.getInstance(project).getModules());
        Set<String> modules_from_mm = new HashSet<>();
        for(Module module: modules)
        {
            modules_from_mm.add(module.getName());
        }
        System.out.println(modules);
        populateModules(modules);
        Project fromMod = modules.get(0).getProject();
        if(fromMod.equals(project))
        {
            System.out.println("Projects are same");
        }
        else
        {
            System.out.println("Projects are not the same");
        }
        try
        {
            System.out.println("Fetching from POM.xml/build.gradle");
            Map<String, String> module_package_mapping = insidiousService.fetchModuleNames();
            Set<String> keys = module_package_mapping.keySet();
            modules_from_mm.addAll(keys);
            populateModules_v1(new ArrayList<String>(modules_from_mm));
        }
        catch (Exception e)
        {
            System.out.println("Exception fetching modules");
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public void populateModules(List<Module> modules)
    {
        this.modulesParentPanel.removeAll();
        int GridRows = 6;
        if (modules.size() > GridRows) {
            GridRows = modules.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        gridLayout.setVgap(8);
        Dimension d = new Dimension();
        JPanel gridPanel = new JPanel(gridLayout);
        for (int i = 0; i < modules.size(); i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            ModulePanel modulePanel = new ModulePanel(modules.get(i).getName(),this);
            gridPanel.add(modulePanel.getMainPanel(), constraints);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        modulesParentPanel.setPreferredSize(scrollPane.getSize());
        modulesParentPanel.add(scrollPane, BorderLayout.CENTER);
        if (modules.size() <= 4) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        if (modules.size() > 0) {
        }
        this.modulesParentPanel.revalidate();
    }

    public void populateModules_v1(List<String> modules)
    {
        this.modulesParentPanel.removeAll();
        int GridRows = 6;
        if (modules.size() > GridRows) {
            GridRows = modules.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        gridLayout.setVgap(8);
        Dimension d = new Dimension();
        JPanel gridPanel = new JPanel(gridLayout);
        for (int i = 0; i < modules.size(); i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            ModulePanel modulePanel = new ModulePanel(modules.get(i),this);
            gridPanel.add(modulePanel.getMainPanel(), constraints);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        modulesParentPanel.setPreferredSize(scrollPane.getSize());
        modulesParentPanel.add(scrollPane, BorderLayout.CENTER);
        if (modules.size() <= 4) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        if (modules.size() > 0) {
        }
        this.modulesParentPanel.revalidate();
    }

    public void populatePackages(List<String> packages)
    {
        this.packagesParentPanel.removeAll();
        int GridRows = 12;
        if (packages.size() > GridRows) {
            GridRows = packages.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        Dimension d = new Dimension();
        JPanel gridPanel = new JPanel(gridLayout);
        int i = 0;
        for(String packagename : packages)
        {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            constraints.setIndent(16);
            JCheckBox checkBox = new JCheckBox();
            d.setSize(-1,20);
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
                    }
                }
            });
            //checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            gridPanel.add(checkBox, constraints);
            i++;
        }
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        packagesParentPanel.setPreferredSize(scrollPane.getSize());
        packagesParentPanel.add(scrollPane, BorderLayout.CENTER);
        if (packages.size() <= 16) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        this.packagesParentPanel.revalidate();
    }

    private void runApplicationWithUnlogged()
    {
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
                    applicationConfiguration.setVMParameters(newVmOptions.trim());
                    try {
                         runner.execute(new ExecutionEnvironment(executor, runner, runSetting, project), null);
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
        findPackagesForModule(moduleName);
    }
}
