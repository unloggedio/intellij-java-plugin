package com.insidious.plugin.ui;

import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class UI_Utils {

    static final String gifPath = "/icons/gif/";
    public static Color teal = new Color(1, 204, 245);
    public static Color pink = new Color(254,100,216);
    public static Color red = new Color(245,101,101);
    public static Color green = new Color(56,161,105);
    public static Color yellow_alert = new Color(225,163,54);

    public static Icon UNLOGGED_ICON_DARK = IconLoader.getIcon("/icons/png/logo_unlogged.png",UI_Utils.class);
    public static Icon ONBOARDING_ICON_DARK = IconLoader.getIcon("/icons/png/onboarding_icon_dark.png",UI_Utils.class);
    public static Icon ONBOARDING_ICON_PINK = IconLoader.getIcon("/icons/png/onboarding_icon_pink.png",UI_Utils.class);
    public static Icon ONBOARDING_ICON_TEAL = IconLoader.getIcon("/icons/png/onboarding_icon_teal.png",UI_Utils.class);
    public static Icon TEST_CASES_ICON_DARK = IconLoader.getIcon("/icons/png/test_case_icon_dark.png",UI_Utils.class);
    public static Icon TEST_CASES_ICON_PINK = IconLoader.getIcon("/icons/png/test_cases_icon_pink.png",UI_Utils.class);
    public static Icon TEST_CASES_ICON_TEAL = IconLoader.getIcon("/icons/png/test_cases_icon_teal.png",UI_Utils.class);
    public static Icon WAITING_COMPONENT_WAITING = IconLoader.getIcon("/icons/png/waiting_icon_yellow_64.png",UI_Utils.class);
    public static Icon WAITING_COMPONENT_SUCCESS = IconLoader.getIcon("/icons/png/success_icon_green_64.png",UI_Utils.class);
    public static Icon ARROW_YELLOW_RIGHT = IconLoader.getIcon("/icons/png/arrow_yellow_right.png",UI_Utils.class);
    public static Icon MAVEN_ICON = IconLoader.getIcon("/icons/png/maven_Icon_20.png",UI_Utils.class);
    public static Icon GRADLE_ICON = IconLoader.getIcon("/icons/png/gradle_icon_20.png",UI_Utils.class);
    public static Icon INTELLIJ_ICON = IconLoader.getIcon("/icons/png/intelliJ_icon_20.png",UI_Utils.class);
    public static Icon JAVA_ICON = IconLoader.getIcon("/icons/png/java_logo_20.png",UI_Utils.class);

    public static void setDividerColorForSplitPane(JSplitPane splitPane, Color color)
    {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {
                    }

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(color);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
    }

    public static void setGifIconForButton(JButton button, String gif, Icon fallback)
    {
        try {
                ImageIcon loadingIcon = new ImageIcon(UI_Utils.class.getResource(gifPath+gif));
                button.setIcon(loadingIcon);
        }
        catch(Exception e)
        {
            System.out.println("Exception setting Gif icon for button "+e);
            e.printStackTrace();
            button.setIcon(fallback);
        }
    }

    public static void setGifIconForLabel(JLabel label, String gif, Icon fallback)
    {
        try {
            ImageIcon loadingIcon = new ImageIcon(UI_Utils.class.getResource(gifPath+gif));
            label.setIcon(loadingIcon);
        }
        catch(Exception e)
        {
            System.out.println("Exception setting Gif icon for label "+e);
            e.printStackTrace();
            label.setIcon(fallback);
        }
    }

    public static Icon getIconForRuntype(ProjectTypeInfo.RUN_TYPES type)
    {
        switch (type)
        {
            case MAVEN_CLI:
                return MAVEN_ICON;
            case GRADLE_CLI:
                return GRADLE_ICON;
            case INTELLIJ_APPLICATION:
                return INTELLIJ_ICON;
            case JAVA_JAR_CLI:
                return JAVA_ICON;
        }

        return null;
    }

    public static String getDisplayNameForType(ProjectTypeInfo.RUN_TYPES type)
    {
        switch (type)
        {
            case MAVEN_CLI:
                return "Maven CLI Application";
            case GRADLE_CLI:
                return "Gradle CLI Application";
            case INTELLIJ_APPLICATION:
                return "IntelliJ Idea Application";
            case JAVA_JAR_CLI:
                return "Java jar command";
        }

        return null;
    }
}
