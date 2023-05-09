package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.VMoptionsConstructionService;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.DumbService;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class AgentConfigComponent {
    public static final String CALENDLY_UNLOGGED_LINK_STRING = "https://calendly.com/unlogged/unlogged-onboarding";
    private URI calendlyUri;
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
    //    private JEditorPane imagePane;
    private JPanel supportPanel;
    private JButton discordButton;
    private JButton addToCurrentRunConfigButton;
    private JPanel calendlyLinkPanel;
    private InsidiousService insidiousService;
    private String currentModuleName;
    private VMoptionsConstructionService vmoptsConstructionService = new VMoptionsConstructionService();
    private boolean addOpens = false;
    private ProjectTypeInfo.RUN_TYPES currentType = ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION;

    public AgentConfigComponent(InsidiousService insidiousService) {
        try {
            this.calendlyUri = new URI(CALENDLY_UNLOGGED_LINK_STRING);
        } catch (URISyntaxException e) {
            this.calendlyUri = null;
            // should never happen
        }
        this.insidiousService = insidiousService;
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        dumbService.runWhenSmart(() -> {
//            setModuleList();
            //vmoptsConstructionService.setBasePackage(insidiousService.fetchBasePackage());
            setRunModes();
            updateVMParams();
        });

        //loadHintGif();
//        moduleCombobox.addItemListener(event -> {
//            if (event.getStateChange() == ItemEvent.SELECTED) {
//                String moduleName = event.getItem()
//                        .toString();
//                this.currentModuleName = moduleName;
//                updateVMParams();
//            }
//        });

        addToCurrentRunConfigButton.addActionListener(
                e -> {
                    UsageInsightTracker.getInstance().RecordEvent("ADD_AGENT_TO_RUN_CONFIG", new JSONObject());
                    insidiousService.addAgentToRunConfig(
                            getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION));
                });

        javaComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                int javaVersion = Integer.parseInt(event.getItem().toString());
                this.addOpens = javaVersion >= 17;
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
                routeToDiscord("https://discord.gg/Hhwvay8uTa");
            }
        });

        copyToClipboardButton.addActionListener(e -> {
            UsageInsightTracker.getInstance().RecordEvent("COPY_VM_PARAMS", new JSONObject());
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(
                            new StringSelection(vmparamsArea.getText()),
                            null
                    );
            InsidiousNotification.notifyMessage("Copied JVM param to clipboard", NotificationType.INFORMATION);
        });

        JButton button = new JButton();
        button.setText("<HTML>" +
                "<a href=\"" + CALENDLY_UNLOGGED_LINK_STRING + "\">" + CALENDLY_UNLOGGED_LINK_STRING + "</a>" +
                "</HTML>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(JBColor.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setToolTipText(calendlyUri != null ? calendlyUri.toString() : CALENDLY_UNLOGGED_LINK_STRING);
        button.setMargin(JBUI.insets(5));
        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported() && calendlyUri != null) {
                    try {
                        Desktop.getDesktop().browse(calendlyUri);
                    } catch (IOException e1) { /* TODO: error handling */ }
                } else { /* TODO: error handling */ }
            }
        });
        calendlyLinkPanel.add(button, BorderLayout.CENTER);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void updateVMParams() {
        this.vmparamsArea.setText(getCurrentJVMOpts(currentType));
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

    public String getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES currentType1) {
        String basePackage = insidiousService.fetchBasePackage();

        //String basePackage = insidiousService.fetchBasePackageForModule(this.currentModuleName);
//        this.basePackageTextField.setText(basePackage);

        vmoptsConstructionService.setBasePackage(basePackage);
        vmoptsConstructionService.setAddopens(addOpens);
        return vmoptsConstructionService.getVMOptionsForRunType(currentType1);
    }

//    public void loadHintGif() {
//        imagePane.setContentType("text/html");
//        String htmlString = "<html><body>" +
//                "<div align=\"left\"><img src=\"" + this.getClass().getClassLoader()
//                .getResource("icons/gif/not_running.gif").toString() + "\" /></div></body></html>";
//        imagePane.setText(htmlString);
//    }

    private void routeToDiscord(String link) {
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_EXE", null);
    }

}
