package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.VMoptionsConstructionService;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.intellij.openapi.project.DumbService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public class AgentConfigComponent {
    private JPanel mainPanel;
    private JPanel aligner;
    private JPanel topPanel;
    private JLabel iconLabel;
    private JTextArea messagearea1;
    private JPanel selectionsParent;
    private JPanel javaVersionSelectorPanel;
    private JPanel runModeSelectorPanel;
    private JComboBox moduleCombobox;
    private JLabel jvsplabel;
    private JComboBox javaComboBox;
    private JLabel rmspLabel;
    private JComboBox<ProjectTypeInfo.RUN_TYPES> runComboBox;
    private JTextField basePackageTextField;
    private JPanel vmparamsSection;
    private JTextArea vmparamsArea;
    private JButton copyToClipboardButton;
    private JEditorPane imagePane;
    private JPanel supportPanel;
    private JButton discordButton;
    private JButton addToCurrentRunConfigButton;
    private InsidiousService insidiousService;
    private String currentModuleName;
    private VMoptionsConstructionService vmoptsConstructionService = new VMoptionsConstructionService();
    private boolean addOpens = false;
    private ProjectTypeInfo.RUN_TYPES currentType = ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION;

    public AgentConfigComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        dumbService.runWhenSmart(() -> {
//            setModuleList();
            //vmoptsConstructionService.setBasePackage(insidiousService.fetchBasePackage());
            setRunModes();
            updateVMParams();
        });

        loadHintGif();
//        moduleCombobox.addItemListener(event -> {
//            if (event.getStateChange() == ItemEvent.SELECTED) {
//                String moduleName = event.getItem()
//                        .toString();
//                this.currentModuleName = moduleName;
//                updateVMParams();
//            }
//        });

        addToCurrentRunConfigButton.addActionListener(
                e -> insidiousService.addAgentToRunConfig(
                        getCurrentVMopts(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION)));

        javaComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                Integer java_version = Integer.parseInt(event.getItem().toString());
                this.addOpens = (java_version >= 17) ? true : false;
                updateVMParams();
            }
        });

        runComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                Object type = event.getItem();
                if (type instanceof ProjectTypeInfo.RUN_TYPES) {
                    currentType = (ProjectTypeInfo.RUN_TYPES) type;
                }
                updateVMParams();
            }
        });
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void updateVMParams() {
        this.vmparamsArea.setText(getCurrentVMopts(currentType));
    }

//    private void setModuleList() {
//        List<String> modules = insidiousService.fetchModules();
//        DefaultComboBoxModel module_model = new DefaultComboBoxModel();
//        module_model.addAll(modules);
//        moduleCombobox.setModel(module_model);
//        moduleCombobox.revalidate();
//        if (this.currentModuleName == null) {
//            this.currentModuleName = modules.get(0);
//            moduleCombobox.setSelectedIndex(0);
//        }
//    }

    private void setRunModes() {
        ProjectTypeInfo.RUN_TYPES[] types = ProjectTypeInfo.RUN_TYPES.values();
        List<ProjectTypeInfo.RUN_TYPES> listTypes = Arrays.asList(types);
        DefaultComboBoxModel<ProjectTypeInfo.RUN_TYPES> run_model = new DefaultComboBoxModel<>();
        run_model.addAll(listTypes);
        runComboBox.setModel(run_model);
        runComboBox.revalidate();
        runComboBox.setSelectedIndex(0);
    }

    public void copyParamsToClipboard() {
//        insidiousService.copyToClipboard(getCurrentVMopts());
    }

    public String getCurrentVMopts(ProjectTypeInfo.RUN_TYPES currentType1) {
        String basePackage = insidiousService.fetchBasePackage();

        //String basePackage = insidiousService.fetchBasePackageForModule(this.currentModuleName);
//        this.basePackageTextField.setText(basePackage);

        vmoptsConstructionService.setBasePackage(basePackage);
        vmoptsConstructionService.setAddopens(addOpens);
        return vmoptsConstructionService.getVMOptionsForRunType(currentType1);
    }

    public void loadHintGif() {
        imagePane.setContentType("text/html");
        String htmlString = "<html><body>" +
                "<div align=\"left\"><img src=\"" + this.getClass().getClassLoader()
                .getResource("icons/gif/not_running.gif").toString() + "\" /></div></body></html>";
        imagePane.setText(htmlString);
    }

    private void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_EXE", null);
    }

}
