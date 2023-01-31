package com.insidious.plugin.ui.Components;

import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.ui.UIUtils;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Obv3_Run_Mode_Selector {
    private JPanel mainPanel;
    private JPanel primaryContainer;
    private JPanel headingPanel;
    private JLabel headingText;
    private JPanel container;
    private JPanel buttonPanel;
    private JButton continueButton;
    private List<String> runModes = new ArrayList<>();
    private ButtonGroup buttonGroup;
    private ProjectTypeInfo.RUN_TYPES currentType = null;
    private CardActionListener cardActionListener;
    public Obv3_Run_Mode_Selector(ProjectTypeInfo.RUN_TYPES defaultType, CardActionListener actionListener)
    {
        this.cardActionListener = actionListener;
        this.currentType = defaultType;
        continueButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                continueClicked();
            }
        });
        loadRunOptions();
    }

    public void loadRunOptions()
    {
        this.container.removeAll();
        ProjectTypeInfo.RUN_TYPES[] types = ProjectTypeInfo.RUN_TYPES.values();
        int GridRows = 8;
        if (types.length > GridRows) {
            GridRows = types.length;
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        Dimension d = new Dimension();
        d.setSize(-1, 30);
        JPanel gridPanel = new JPanel(gridLayout);
        gridLayout.setVgap(4);
        buttonGroup = new ButtonGroup();
        int c=0;
        for (int i=0;i<types.length;i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(c);
            c++;
            constraints.setIndent(4);
            JRadioButton label = new JRadioButton();
            label.setText(getNameForType(types[i]));
            label.setBorder(new EmptyBorder(4, 8, 0, 0));
            ProjectTypeInfo.RUN_TYPES type = types[i];
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedRunType(type.toString());
                }
            });

            Obv3_RunType_Element element = new Obv3_RunType_Element(type);
            element.setContent(label);
            element.setIcon_rs(UIUtils.getIconForRuntype(types[i]));

            gridPanel.add(element.getComponent(), constraints);
            buttonGroup.add(label);
            if(i==0 && this.currentType==null)
            {
                label.doClick();
                //set this as def
            }
            if(this.currentType!=null && type.equals(currentType))
            {
                label.doClick();
            }
            if(i!=types.length-1)
            {
                JSeparator separator = new JSeparator();
                separator.setOrientation(JSeparator.HORIZONTAL);
                constraints = new GridConstraints();
                constraints.setRow(c);
                c++;
                gridPanel.add(separator,constraints);
            }
        }
        gridPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        EmptyBorder emptyBorder = new EmptyBorder(0, 0, 0, 0);
        scrollPane.setBorder(emptyBorder);
        container.setPreferredSize(scrollPane.getSize());
        container.add(scrollPane, BorderLayout.CENTER);
        if (types.length<= 3) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        this.container.revalidate();
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    public void selectedRunType(String seletion)
    {
        System.out.println("Selected : "+seletion);
        List<Map<OnboardingScaffoldV3.ONBOARDING_ACTION,String>> actions = new ArrayList<>();
        Map<OnboardingScaffoldV3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.UPDATE_SELECTION,"runType:"+seletion);
        actions.add(action);
        cardActionListener.performActions(actions);
    }

    public void continueClicked()
    {
        List<Map<OnboardingScaffoldV3.ONBOARDING_ACTION,String>> actions = new ArrayList<>();
        Map<OnboardingScaffoldV3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.NEXT_STATE,"");
        actions.add(action);
        cardActionListener.performActions(actions);
    }

    private String getNameForType(ProjectTypeInfo.RUN_TYPES type)
    {
        switch (type)
        {
            case MAVEN_CLI:
                return "Maven";
            case GRADLE_CLI:
                return "Gradle";
            case INTELLIJ_APPLICATION:
                return "IntelliJ Application";
            case JAVA_JAR_CLI:
                return "Java jar command";
        }
        return "IntelliJ Application";
    }
}
