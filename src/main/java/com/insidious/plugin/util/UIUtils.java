package com.insidious.plugin.util;

import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;

import javax.swing.*;
import java.awt.*;

public class UIUtils {

    public static final String gifPath = "/icons/gif/";
    public static final Icon DISCORD_ICON = IconLoader.getIcon("/icons/png/discordIcon.png", UIUtils.class);
    public static final Icon NEW_LOGS_TO_PROCESS_ICON = IconLoader.getIcon("/icons/png/new_logs_to_process_icon.png",
            UIUtils.class);
    public static final Icon NO_NEW_LOGS_TO_PROCESS_ICON = IconLoader.getIcon("/icons/png/assert-not-equals.png",
            UIUtils.class);
    public static final Icon FIND_BUGS_GREY = IconLoader.getIcon("/icons/png/findbug_grey.png", UIUtils.class);
    public static final Icon CLASS_ICON = IconLoader.getIcon("/icons/png/class.png", UIUtils.class);
    public static final Icon OPTIMIZE_GREY = IconLoader.getIcon("/icons/png/optimize_grey.png", UIUtils.class);
    public static final Icon REFACTOR_GREY = IconLoader.getIcon("/icons/png/refactor_grey.png", UIUtils.class);
    public static final Icon EXPLAIN_GREY = IconLoader.getIcon("/icons/png/explain_grey.png", UIUtils.class);
    public static final Icon FIND_BUGS_TEAL = IconLoader.getIcon("/icons/png/findBugsTeal.png", UIUtils.class);
    public static final Icon OPTIMIZE_TEAL = IconLoader.getIcon("/icons/png/optimizeTeal.png", UIUtils.class);
    public static final Icon REFACTOR_TEAL = IconLoader.getIcon("/icons/png/refactorTeal.png", UIUtils.class);
    public static final Icon EXPLAIN_TEAL = IconLoader.getIcon("/icons/png/explainTeal.png", UIUtils.class);
    public static final Icon SAVE_CANDIDATE_GREY = IconLoader.getIcon("/icons/png/save_candidate.png", UIUtils.class);
    public static final Icon SAVE_CANDIDATE_PINK = IconLoader.getIcon("/icons/png/save-line-pink.png", UIUtils.class);
    public static final Icon SAVE_CANDIDATE_GREEN_SVG = IconLoader.getIcon("/icons/svg/save-line.svg", UIUtils.class);
    public static final Icon DELETE_CANDIDATE_RED_SVG = IconLoader.getIcon("/icons/svg/delete-bin-6-line.svg",
            UIUtils.class);
    public static final Icon REPLAY_PINK = IconLoader.getIcon("/icons/svg/replay-all-pink.svg", UIUtils.class);
    public static final Icon ICON_EXECUTE_METHOD = IconLoader.getIcon("/icons/png/play-icon.png", UIUtils.class);
    public static final Icon ICON_EXECUTE_METHOD_SMALLER = IconUtil.scale(ICON_EXECUTE_METHOD, null,
            JBUIScale.scale(16.0f) / ICON_EXECUTE_METHOD.getIconWidth());
    public static final JBColor HIGHLIGHT_BACKGROUND_COLOR = new JBColor(
            new Color(1, 204, 245, 100),
            new Color(1, 204, 245, 50)
    );
    public static Color teal = new JBColor(new Color(1, 204, 245), new Color(1, 204, 245));
    public static Color pink = new JBColor(new Color(254, 100, 216), new Color(254, 100, 216));
    public static Color red = new JBColor(new Color(245, 101, 101), new Color(245, 101, 101));
    public static Color green = new JBColor(new Color(56, 161, 105), new Color(56, 161, 105));
    public static Color yellow_alert = new JBColor(new Color(225, 163, 54), new Color(225, 163, 54));
    public static Color orange = new JBColor(new Color(221, 107, 32), new Color(221, 107, 32));
    public static Color defaultForeground = Gray._187;
    public static Color NeutralGrey = new JBColor(new Color(81, 85, 87), new Color(81, 85, 87));
    public static Color WARNING_RED = new JBColor(new Color(239, 144, 160), new Color(239, 144, 160));
    public static Color black_custom = new JBColor(Gray._32, Gray._32);

    public static JBColor agentResponseBaseColor = new JBColor(
            Gray._255,
            new Color(43, 45, 48));

    public static JBColor inputViewerTreeForeground = new JBColor(
            JBColor.BLACK,
            defaultForeground);

    public static JBColor buttonBorderColor = new JBColor(
            new Color(196, 196, 196),
            new Color(94, 96, 96));

    public static Icon UNLOGGED_ICON_DARK = IconLoader.getIcon("/icons/png/logo_unlogged.png", UIUtils.class);
    public static Icon UNLOGGED_GPT_ICON_PINK = IconLoader.getIcon("/icons/png/unloggedGPT_pink.png", UIUtils.class);
    public static Icon UNLOGGED_ICON_DARK_SVG = IconLoader.getIcon("/icons/svg/unlogged_logo.svg", UIUtils.class);
    public static Icon UNLOGGED_ICON_LIGHT_SVG = IconLoader.getIcon("/icons/svg/unlogged_logo_light.svg",
            UIUtils.class);
    public static Icon UNLOGGED_GPT_PINK = IconLoader.getIcon("/icons/svg/unloggedGPTIcon_pink.svgg", UIUtils.class);
    public static Icon TEST_BEAKER_TEAL = IconLoader.getIcon("/icons/svg/test_beaker_icon_teal.svg", UIUtils.class);
    public static Icon TEST_TUBE_ICON_SVG = IconLoader.getIcon("/icons/svg/test_tube_icon_green.svg", UIUtils.class);
    public static Icon TEST_TUBE_FILL = IconLoader.getIcon("/icons/svg/test_tube_unlogged.svg", UIUtils.class);
    public static Icon RE_EXECUTE = IconLoader.getIcon("/icons/svg/execute_v2.svg", UIUtils.class);
    public static Icon DIFF_GUTTER = IconLoader.getIcon("/icons/svg/diff_gutter.svg", UIUtils.class);
    public static Icon NO_DIFF_GUTTER = IconLoader.getIcon("/icons/svg/no_diff_gutter.svg", UIUtils.class);
    public static Icon NO_AGENT_GUTTER = IconLoader.getIcon("/icons/svg/no_agent_gutter.svg", UIUtils.class);
    public static Icon EXECUTE_COMPONENT = IconLoader.getIcon("/icons/svg/execute_component.svg", UIUtils.class);
    public static Icon EXCEPTION_CASE = IconLoader.getIcon("/icons/svg/exception_diff_case.svg", UIUtils.class);
    public static Icon PROCESS_RUNNING = IconLoader.getIcon("/icons/svg/process_running.svg", UIUtils.class);
    public static Icon PROCESS_NOT_RUNNING = IconLoader.getIcon("/icons/svg/process_not_running.svg", UIUtils.class);
    public static Icon DATA_AVAILABLE = IconLoader.getIcon("/icons/svg/data_available_v2.svg", UIUtils.class);
    public static Icon ATOMIC_TESTS = IconLoader.getIcon("/icons/svg/atomic_tests_v2.svg", UIUtils.class);
    public static Icon DIRECT_INVOKE_EXECUTE = IconLoader.getIcon("/icons/svg/direct-invoke-execute.svg",
            UIUtils.class);
    public static Icon EXECUTE_METHOD = IconLoader.getIcon("/icons/svg/execute_method.svg", UIUtils.class);
    public static Icon COVERAGE_TOOL_WINDOW_ICON = IconLoader.getIcon("/icons/svg/coverage-tool-window-icon.png",
            UIUtils.class);
    public static Icon ORANGE_EXCEPTION = IconLoader.getIcon("/icons/svg/exception_orange.svg", UIUtils.class);
    public static Icon EXECUTE_ICON_OUTLINED_SVG = IconLoader.getIcon("/icons/svg/execute-button-outlined.svg",
            UIUtils.class);
    public static Icon ONBOARDING_ICON_DARK = IconLoader.getIcon("/icons/png/onboarding_icon_dark.png", UIUtils.class);
    public static Icon ONBOARDING_ICON_PINK = IconLoader.getIcon("/icons/png/onboarding_icon_pink.png", UIUtils.class);
    public static Icon ONBOARDING_ICON_TEAL = IconLoader.getIcon("/icons/png/onboarding_icon_teal.png", UIUtils.class);
    public static Icon TEST_CASES_ICON_DARK = IconLoader.getIcon("/icons/png/test_case_icon_dark.png", UIUtils.class);
    public static Icon TEST_CASES_ICON_PINK = IconLoader.getIcon("/icons/png/test_cases_icon_pink.png", UIUtils.class);
    public static Icon TEST_CASES_ICON_TEAL = IconLoader.getIcon("/icons/png/test_cases_icon_teal.png", UIUtils.class);
    public static Icon WAITING_COMPONENT_WAITING = IconLoader.getIcon("/icons/png/waiting_icon_yellow_64.png",
            UIUtils.class);
    public static Icon WAITING_COMPONENT_SUCCESS = IconLoader.getIcon("/icons/png/success_icon_green_64.png",
            UIUtils.class);
    public static Icon ARROW_YELLOW_RIGHT = IconLoader.getIcon("/icons/png/arrow_yellow_right.png", UIUtils.class);
    public static Icon COMPARE_TAB = IconLoader.getIcon("/icons/png/compare_tab_icon.png", UIUtils.class);
    public static Icon JSON_KEY = IconLoader.getIcon("/icons/png/json_key.png", UIUtils.class);
    public static Icon OLD_KEY = IconLoader.getIcon("/icons/png/old_key.png", UIUtils.class);
    public static Icon NEW_KEY = IconLoader.getIcon("/icons/png/new_key_icon.png", UIUtils.class);
    public static Icon CHECK_GREEN_SMALL = IconLoader.getIcon("/icons/png/green_check_mark_16.png", UIUtils.class);
    public static Icon MAVEN_ICON = IconLoader.getIcon("/icons/png/maven_Icon_20.png", UIUtils.class);
    public static Icon GRADLE_ICON = IconLoader.getIcon("/icons/png/gradle_icon_20.png", UIUtils.class);
    public static Icon INTELLIJ_ICON = IconLoader.getIcon("/icons/png/intelliJ_icon_20.png", UIUtils.class);
    public static Icon JAVA_ICON = IconLoader.getIcon("/icons/png/java_logo_20.png", UIUtils.class);
    public static Icon ICON_1_TEAL = IconLoader.getIcon("/icons/png/numbered_icon_1_teal.png", UIUtils.class);
    public static Icon ICON_2_TEAL = IconLoader.getIcon("/icons/png/numbered_icon_2_teal.png", UIUtils.class);
    public static Icon ICON_3_TEAL = IconLoader.getIcon("/icons/png/numbered_icon_3_teal.png", UIUtils.class);
    public static Icon ICON_4_TEAL = IconLoader.getIcon("/icons/png/numbered_icon_4_teal.png", UIUtils.class);
    public static Icon ICON_5_TEAL = IconLoader.getIcon("/icons/png/numbered_icon_5_teal.png", UIUtils.class);
    //    public static Icon ICON_MINUS = IconLoader.getIcon("/icons/png/ph-minus-square-bold.png", UIUtils.class);
//    public static Icon ICON_PLUS = IconLoader.getIcon("/icons/png/material-symbols-add-box-outline-rounded.png", UIUtils.class);
    public static Icon SEND_TEAL_ICON = IconLoader.getIcon("/icons/png/sendButton.png", UIUtils.class);
    public static Icon MISSING_DEPENDENCIES_ICON = IconLoader.getIcon("/icons/png/alert_icon_yellow.png",
            UIUtils.class);
    public static Icon NO_MISSING_DEPENDENCIES_ICON = IconLoader.getIcon(
            "/icons/png/no_missing_dependencies_icon_20.png", UIUtils.class);
    public static Icon GENERATE_ICON = IconLoader.getIcon("/icons/png/generate_icon.png", UIUtils.class);
    public static Icon DOWNLOAD_WHITE = IconLoader.getIcon("/icons/png/download_white.png", UIUtils.class);
    public static Icon NO_AGENT_HEADER = IconLoader.getIcon("/icons/png/no_agent_header.png", UIUtils.class);
    public static Icon PROCESS_RUNNING_HEADER = IconLoader.getIcon("/icons/png/process_running_header.png",
            UIUtils.class);
    public static Icon DATA_AVAILABLE_HEADER = IconLoader.getIcon("/icons/png/data_available_header.png",
            UIUtils.class);

    public static void setGifIconForButton(JButton button, String gif, Icon fallback) {
        try {
            ImageIcon loadingIcon = new ImageIcon(UIUtils.class.getResource(gifPath + gif));
            button.setIcon(loadingIcon);
        } catch (Exception e) {
            System.out.println("Exception setting Gif icon for button " + e);
            e.printStackTrace();
            button.setIcon(fallback);
        }
    }

    public static Image getImageForGif(String gif) {
        try {
            ImageIcon loadingIcon = new ImageIcon(UIUtils.class.getResource(gifPath + gif));
            return loadingIcon.getImage();
        } catch (Exception e) {
            return null;
        }
    }

    public static void setGifIconForLabel(JLabel label, String gif, Icon fallback) {
        try {
            ImageIcon loadingIcon = new ImageIcon(UIUtils.class.getResource(gifPath + gif));
            label.setIcon(loadingIcon);
        } catch (Exception e) {
            System.out.println("Exception setting Gif icon for label " + e);
            e.printStackTrace();
            label.setIcon(fallback);
        }
    }

    public static void setGifIconForEditorPane(JEditorPane pane, String gif) {
        try {
//            ImageIcon loadingIcon = new ImageIcon(UIUtils.class.getResource(gifPath + gif));
            //get abs path
            pane.setText("<html><body><img src = \"" + gifPath + gif + "\"></body></html>");

//            this.getClass().getClassLoader()
//                    .getResource("images/logo_html.png").toString()
        } catch (Exception e) {
            System.out.println("Exception setting Gif icon for label " + e);
            e.printStackTrace();
        }
    }

    public static Icon getIconForRuntype(ProjectTypeInfo.RUN_TYPES type) {
        switch (type) {
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

    public static String getDisplayNameForType(ProjectTypeInfo.RUN_TYPES type) {
        switch (type) {
            case MAVEN_CLI:
                return "Maven CLI Application";
            case GRADLE_CLI:
                return "Gradle Application";
            case INTELLIJ_APPLICATION:
                return "IntelliJ Idea Application";
            case JAVA_JAR_CLI:
                return "Java jar command";
        }

        return null;
    }

    public static Icon getNumberedIconFor(int number) {
        switch (number) {
            case 1:
                return ICON_1_TEAL;
            case 2:
                return ICON_2_TEAL;
            case 3:
                return ICON_3_TEAL;
            case 4:
                return ICON_4_TEAL;
            case 5:
                return ICON_5_TEAL;
            default:
                return ICON_1_TEAL;

        }
    }

    public static Icon getGutterIconForState(GutterState state) {
        switch (state) {
            case NO_DIFF:
                return NO_DIFF_GUTTER;
            case DIFF:
                return DIFF_GUTTER;
//            case NO_AGENT:
//                return NO_AGENT_GUTTER;
            case PROCESS_RUNNING:
                return PROCESS_RUNNING;
            case PROCESS_NOT_RUNNING:
                return PROCESS_NOT_RUNNING;
            case DATA_AVAILABLE:
                return DATA_AVAILABLE;
            default:
                return UIUtils.RE_EXECUTE;
        }
    }
}
