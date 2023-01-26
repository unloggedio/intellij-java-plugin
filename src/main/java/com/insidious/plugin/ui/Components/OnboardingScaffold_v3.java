package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.OnboardingService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.VMoptionsConstructionService;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class OnboardingScaffold_v3 implements CardActionListener {
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
    private Obv3_Documentation_Generic documentation_generic;
    private OnboardingService onboardingService;
    public enum DROP_TYPES {MODULE,JAVA_VERSION, SERIALIZER}
    public enum ONBOARDING_ACTION {UPDATE_SELECTION,DOWNLOAD_AGENT,ADD_DEPENDENCIES,NEXT_STATE}
    private OnBoardingStatus status = new OnBoardingStatus();
    private VMoptionsConstructionService vmOptionsConstructionService = new VMoptionsConstructionService();
    private Run_Component_Obv3 runComponent = null;
    private InsidiousService insidiousService;
    private NavigatorComponent navigator;
    @Override
    public void performActions(List<Map<ONBOARDING_ACTION, String>> actions) {

        for(Map<ONBOARDING_ACTION,String> action : actions)
        {
            ONBOARDING_ACTION action_to_perform = new ArrayList<>(action.keySet()).get(0);
            switch (action_to_perform)
            {
                case DOWNLOAD_AGENT:
                    String version = action.get(ONBOARDING_ACTION.DOWNLOAD_AGENT);
                    System.out.println("DOWNLOADING AGENT FOR - "+version);
                    onboardingService.downloadAgentForVersion(version);
                    break;
                case ADD_DEPENDENCIES:
                    //trigger add dependencies, no need to pass anything
                    System.out.println("ADD DEPENDENCIES TRIGGERED");
                    String dependencies_string = action.get(ONBOARDING_ACTION.ADD_DEPENDENCIES);
                    System.out.println("[SELECTED DEPENDENCIES] "+dependencies_string);
                    dependencies_string =  dependencies_string
                            .replaceAll("\\[","")
                            .replaceAll("\\]","");
                    HashSet<String> deps = new HashSet<>(Arrays.asList(dependencies_string.split(",")));
                    Map<String,String> refs = onboardingService.getMissingDependencies_v3();
                    if(refs.size()>0)
                    {
                        onboardingService.postProcessDependencies(refs,
                                deps);
                    }
                    loadDependenciesManagementSection();
                    break;
                case UPDATE_SELECTION:
                    String parameter = action.get(ONBOARDING_ACTION.UPDATE_SELECTION).trim();
                    String[] parts = parameter.split(":");
                    System.out.println("UPDATE SELECTION TRIGGERED "+parameter);
                    switch (parts[0])
                    {
                        case "module":
                            this.status.setCurrentModule(parts[1]);
                            updateVMParams(parts[1]);
                            break;
                        case "addopens":
                            if(parts[1].equals("true"))
                            {
                                this.status.setAddOpens(true);
                                updateVMParams(true);
                            }
                            else
                            {
                                this.status.setAddOpens(false);
                                updateVMParams(false);
                            }
                            break;
                        case "runType":
                            ProjectTypeInfo.RUN_TYPES type = ProjectTypeInfo.RUN_TYPES.valueOf(parts[1]);
                            this.status.setRunType(type);
                            break;
                    }
                    break;
                case NEXT_STATE:
                    navigator.loadNextState();
            }
        }
    }

    public void updateVMParams(String modulename)
    {
        this.vmOptionsConstructionService.setBasePackage(onboardingService.fetchBasePackageForModule(modulename));
        if(this.runComponent!=null)
        {
            this.runComponent.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.runType));
        }
    }

    public void updateVMParams(boolean addopens)
    {
        this.vmOptionsConstructionService.setAddopens(addopens);
        if(this.runComponent!=null)
        {
            this.runComponent.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.runType));
        }
    }

    @Override
    public String getBasePackageForModule(String moduleName) {
        return onboardingService.fetchBasePackageForModule(moduleName);
    }

    public OnboardingScaffold_v3(OnboardingService onboardingService, InsidiousService insidiousService)
    {
        this.insidiousService = insidiousService;
        this.onboardingService = onboardingService;
        loadNavigator();
        loadModuleSection();
        loadRightMock();
    }

    public void loadRightMock()
    {
        this.rightContainer.removeAll();
        Obv3_Documentation_Generic docs = new Obv3_Documentation_Generic();
        this.documentation_generic = docs;
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(docs.getComponent(), constraints);
        this.rightContainer.add(gridPanel, BorderLayout.CENTER);
        this.rightContainer.revalidate();
    }

    public void loadNavigator()
    {
        this.navGrid.removeAll();
        navigator = new NavigatorComponent(this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(navigator.getComponent(), constraints);
        this.navGrid.add(gridPanel, BorderLayout.CENTER);
        this.navGrid.revalidate();
    }

    public JPanel getComponent()
    {
        return this.mainParentPanel;
    }

    public void loadModuleSection()
    {
        List<DropdownCardInformation> content = new ArrayList<>();
        List<String> modules = onboardingService.fetchModules();
        DropdownCardInformation info =new DropdownCardInformation("Select a Module :",
                modules,
                "Unlogged will generate unit tests for this module");
        info.setType(DROP_TYPES.MODULE);
        info.setShowRefresh(true);
        if(this.status.currentModule!=null)
        {
            info.setDefaultSelected(modules.indexOf(this.status.currentModule));
        }
        else
        {
            info.setDefaultSelected(modules.indexOf(modules.get(0)));
        }
        content.add(info);
        this.leftContainer.removeAll();
        Obv3_CardParent cardparent = new Obv3_CardParent(content, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadRightMock();
    }

    public void loadDependenciesManagementSection()
    {
        List<DependencyCardInformation> content = new ArrayList<>();
        List<String> dependencies = new ArrayList<>(onboardingService.getMissingDependencies_v3().keySet());
        content.add(new DependencyCardInformation(dependencies.size()==0 ? "No Missing Dependencies" : "Missing dependencies",
                "Add these dependencies so that we can serialise/deserialise data properly.",
                dependencies));
        this.leftContainer.removeAll();
        Obv3_CardParent cardparent = new Obv3_CardParent(content,true, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadRightMock();
    }

    public void loadProjectConfigSection()
    {
        List<DropdownCardInformation> content = new ArrayList<>();
        List<String> java_versions = new ArrayList<>();
        java_versions.add("<11");
        java_versions.add(">=11");
        DropdownCardInformation info_java = new DropdownCardInformation("Java Version : ",
                java_versions,
                "Select the java version of your project.");
        info_java.setType(DROP_TYPES.JAVA_VERSION);
        info_java.setDefaultSelected(0);
        if(this.status.isAddOpens()!=null && this.status.isAddOpens())
        {
            info_java.setDefaultSelected(1);
        }
        content.add(info_java);
        List<String> serializers = new ArrayList<>();
        serializers.add("jackson-2.9");
        serializers.add("jackson-2.10");
        serializers.add("jackson-2.11");
        serializers.add("jackson-2.12");
        serializers.add("jackson-2.13");
        serializers.add("jackson-2.14");
        serializers.add("gson");
        DropdownCardInformation info_dependencies = new DropdownCardInformation("Serializer : ",
                serializers,
                "Select the serializer that your project uses. ");
        info_dependencies.setType(DROP_TYPES.SERIALIZER);
        content.add(info_dependencies);
        this.leftContainer.removeAll();
        Obv3_CardParent cardparent = new Obv3_CardParent(content, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadRightMock();
    }

    public void loadRunConfigSection()
    {
        this.leftContainer.removeAll();
        Obv3_Run_Mode_Selector cardparent;
        cardparent = new Obv3_Run_Mode_Selector(this.status.getRunType(),this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        loadRightMock();
    }

    public void loadRunSection(boolean logsPresent)
    {
        this.leftContainer.removeAll();
        List<Integer> defIndices = new ArrayList<>();
        Integer defIndex = 0;
        List<String> modules = onboardingService.fetchModules();
        if(this.status.currentModule!=null)
        {
            defIndex = modules.indexOf(this.status.currentModule);
        }
        defIndices.add(defIndex);
        Integer java_version_index = 0;
        if(this.status.isAddOpens()!=null && this.status.isAddOpens())
        {
            java_version_index=1;
        }
        defIndices.add(java_version_index);
        OBv2_Selectors_VM cardparent = new OBv2_Selectors_VM(modules, defIndices,this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(cardparent.getComponent(), constraints);
        this.leftContainer.add(gridPanel, BorderLayout.CENTER);
        this.leftContainer.revalidate();

        this.rightContainer.removeAll();
        ProjectTypeInfo.RUN_TYPES defaultType = this.status.getRunType();
        if(defaultType==null)
        {
            defaultType= ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION;
        }
        Run_Component_Obv3 runSection = new Run_Component_Obv3(defaultType, logsPresent, this);
        this.runComponent = runSection;
        if(this.status.currentModule!=null)
        {
            runSection.setVMtext(vmOptionsConstructionService.getVMOptionsForRunType(this.status.runType));
        }
        gridLayout = new GridLayout(1, 1);
        gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(runSection.getComponent(), constraints);
        this.rightContainer.add(gridPanel, BorderLayout.CENTER);
        this.rightContainer.revalidate();
    }

    class OnBoardingStatus
    {
        String currentModule;
        Boolean addOpens;
        ProjectTypeInfo.RUN_TYPES runType = ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION;

        public String getCurrentModule() {
            return currentModule;
        }

        public void setCurrentModule(String currentModule) {
            this.currentModule = currentModule;
            System.out.println("SET CURRENT MODULE to "+currentModule);
        }

        public Boolean isAddOpens() {
            return addOpens;
        }

        public void setAddOpens(Boolean addOpens) {
            this.addOpens = addOpens;
            System.out.println("SET ADD OPENS to "+addOpens);
        }

        public ProjectTypeInfo.RUN_TYPES getRunType() {
            return runType;
        }

        public void setRunType(ProjectTypeInfo.RUN_TYPES runType) {
            System.out.println("SET RUN TYPE to "+runType.toString());
            this.runType = runType;
        }
    }

    @Override
    public void checkForSelogs() {
        recursiveFileCheck();
    }

    @Override
    public void loadLiveLiew() {
        insidiousService.addLiveView();
    }

    @Override
    public void refreshModules() {
        loadModuleSection();
    }

    @Override
    public void refreshDependencies() {
        loadDependenciesManagementSection();
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
        if (insidiousService.areLogsPresent()) {
            //switch state
            System.out.println("Can Switch to LIVE VIEW");
            loadRunSection(true);
            UsageInsightTracker.getInstance()
                    .RecordEvent("LogsReady", null);
        } else {
            //System.out.println("TIMER CHECK FOR SELOGS");
            //run till you have selogs (5 seconds at a time)
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

    public boolean checkIfLogsArePresent()
    {
        return insidiousService.areLogsPresent();
    }
}
