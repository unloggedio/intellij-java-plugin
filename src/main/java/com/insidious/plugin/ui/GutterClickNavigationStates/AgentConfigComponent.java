package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.InsidiousService;

import javax.swing.*;

public class AgentConfigComponent {
    private JPanel mainPanel;
    private JPanel aligner;
    private JPanel topPanel;
    private JTextArea moduleRunStatus;
    private JLabel iconLabel;
    private JTextArea messagearea1;
    private JPanel selectionsParent;
    private JPanel moduleSelectorPanel;
    private JPanel javaVersionSelectorPanel;
    private JPanel runModeSelectorPanel;
    private JPanel packageSelectorPanel;
    private JLabel mspLabel;
    private JComboBox moduleCombobox;
    private JLabel jvsplabel;
    private JComboBox javaComboBox;
    private JLabel rmspLabel;
    private JComboBox runComboBox;
    private JTextField textField1;
    private JPanel vmparamsSection;
    private JTextArea vmparamsArea;
    private JButton copyVMParameterButton;
    private JPanel hintImagePanel;
    private InsidiousService insidiousService;

    public JPanel getComponent()
    {
        return this.mainPanel;
    }

    public AgentConfigComponent(InsidiousService insidiousService)
    {
        this.insidiousService = insidiousService;
    }
}
