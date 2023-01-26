package com.insidious.plugin.ui.Components;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OBv2_Selectors_VM {
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel topParentPanel;
    private JPanel moduleSelectionPanel;
    private JComboBox moduleSelectionBox;
    private JLabel modulesHeading;
    private JPanel JavaVersionSelectionPanel;
    private JLabel javaVersionText;
    private JComboBox javaSelectionBox;
    private JPanel includePanel;
    private JLabel includeHeadingLabel;
    private JPanel BasePackagePanel;
    private JLabel basePackageLabel;
    private JPanel bottomPanelparent;
    private CardActionListener listener;
    private Integer defaultIndex = null;
    private Integer java_defaultIndex = null;
    public OBv2_Selectors_VM(List<String> modules, List<Integer> defaultindices ,CardActionListener listener)
    {
        this.defaultIndex = defaultindices.get(0);
        this.java_defaultIndex = defaultindices.get(1);
        this.listener = listener;
        setupProjectInformationSection(modules);
    }

    private void setupProjectInformationSection(List<String> modules) {
        populateModules(modules);
        javaSelectionBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    String version = event.getItem().toString();
                    boolean add = version.startsWith(">");
                    javaVersionChanged(add);
                }
            }
        });
        javaSelectionBox.setSelectedIndex(java_defaultIndex);
        moduleSelectionPanel.setBorder(new LineBorder(new Color(32, 32, 32)));
        JavaVersionSelectionPanel.setBorder(new LineBorder(new Color(32, 32, 32)));
        includePanel.setBorder(new LineBorder(new Color(32, 32, 32)));
    }

    public void populateModules(List<String> modules) {
        DefaultComboBoxModel module_model = new DefaultComboBoxModel();
        module_model.addAll(modules);
        moduleSelectionBox.setModel(module_model);
        moduleSelectionBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String moduleName = event.getItem().toString();
                String basePackage = listener.getBasePackageForModule(moduleName);
                this.basePackageLabel.setText(basePackage);
                moduleChanged(moduleName);
            }
        });
        moduleSelectionBox.setSelectedIndex(defaultIndex);
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    public void moduleChanged(String module)
    {
        List<Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String>> actions = new ArrayList<>();
        Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.UPDATE_SELECTION,"module:"+module);
        actions.add(action);
        listener.performActions(actions);
    }

    public void javaVersionChanged(boolean addOpens)
    {
        List<Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String>> actions = new ArrayList<>();
        Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.UPDATE_SELECTION,"addopens:"+addOpens);
        actions.add(action);
        listener.performActions(actions);
    }


}
