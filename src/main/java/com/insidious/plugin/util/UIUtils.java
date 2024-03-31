package com.insidious.plugin.util;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class UIUtils {

    public static final String gifPath = "/icons/gif/";
    public static final Icon DISCORD_ICON = IconLoader.getIcon("/icons/png/discordIcon.png", UIUtils.class);
    public static final Icon LINK_ICON = IconLoader.getIcon("/icons/svg/link.svg", UIUtils.class);
    public static final Icon CHECK_ICON = IconLoader.getIcon("/icons/svg/check-line.svg", UIUtils.class);
    public static final Icon UNLINK_ICON = IconLoader.getIcon("/icons/svg/link-unlink-m.svg", UIUtils.class);
    public static final Icon DELETE_BIN_3_LINE = IconLoader.getIcon("icons/png/delete-bin-3-line.png", UIUtils.class);
    public static final Icon DELETE_BIN_CROSS_BLACK = IconLoader.getIcon("icons/svg/delete-bin-2-line.svg", UIUtils.class);
    public static final Icon DELETE_BIN_PARALLEL_RED = IconLoader.getIcon("icons/svg/delete-bin-6-line.svg", UIUtils.class);
//    public static final Icon REFRESH_TEAL = IconLoader.getIcon("icons/png/refresh_icon_teal.png", UIUtils.class);
//    public static final Icon REFRESH_SVG = IconLoader.getIcon("icons/svg/refresh-line-16.svg", UIUtils.class);
    public static final Icon REFRESH_SVG = IconLoader.getIcon("icons/svg/refresh-line-gray.svg", UIUtils.class);
    public static final Icon TEST_TUBE_ICON = IconLoader.getIcon("icons/svg/test-tube-line-pink.svg", UIUtils.class);
    public static final Icon FLASHLIGHT_BLUE = IconLoader.getIcon("icons/svg/flashlight-line-blue.svg", UIUtils.class);
    public static final Icon NEW_LOGS_TO_PROCESS_ICON = IconLoader.getIcon("/icons/png/new_logs_to_process_icon.png",
            UIUtils.class);
    public static final Icon NO_NEW_LOGS_TO_PROCESS_ICON = IconLoader.getIcon("/icons/png/assert-not-equals.png",
            UIUtils.class);
    public static final Icon FIND_BUGS_GREY = IconLoader.getIcon("/icons/png/findbug_grey.png", UIUtils.class);
    public static final Icon CLASS_ICON = IconLoader.getIcon("/icons/png/class.png", UIUtils.class);
    public static final Icon LIBRARY_ICON = AllIcons.ObjectBrowser.ShowLibraryContents;
    public static final Icon OPTIMIZE_GREY = IconLoader.getIcon("/icons/png/optimize_grey.png", UIUtils.class);
    public static final Icon REFACTOR_GREY = IconLoader.getIcon("/icons/png/refactor_grey.png", UIUtils.class);
    public static final Icon EXPLAIN_GREY = IconLoader.getIcon("/icons/png/explain_grey.png", UIUtils.class);
    public static final Icon FIND_BUGS_TEAL = IconLoader.getIcon("/icons/png/findBugsTeal.png", UIUtils.class);
    public static final Icon OPTIMIZE_TEAL = IconLoader.getIcon("/icons/png/optimizeTeal.png", UIUtils.class);
    public static final Icon EXPAND_UP_DOWN = IconLoader.getIcon("/icons/png/expand-up-down-line.png", UIUtils.class);
    public static final Icon EQUALIZER = IconLoader.getIcon("/icons/svg/equalizer-3-line.svg", UIUtils.class);
    public static final Icon DISCONNECTED_ICON = IconLoader.getIcon("/icons/png/record-circle-fill-black.png",
            UIUtils.class);
    public static final Icon CIRCLE_EMPTY = IconLoader.getIcon("/icons/png/checkbox-blank-circle-line.png",
            UIUtils.class);
    public static final Icon CONNECTED_ICON = IconLoader.getIcon("/icons/png/record-circle-fill.png", UIUtils.class);
    public static final Icon REFACTOR_TEAL = IconLoader.getIcon("/icons/png/refactorTeal.png", UIUtils.class);
    public static final Icon EXPLAIN_TEAL = IconLoader.getIcon("/icons/png/explainTeal.png", UIUtils.class);
    public static final Icon SAVE_CANDIDATE_GREY = IconLoader.getIcon("/icons/png/save_candidate.png", UIUtils.class);
    public static final Icon SAVE_CANDIDATE_PINK = IconLoader.getIcon("/icons/png/save-line-pink.png", UIUtils.class);
    public static final Icon CLOSE_LINE_PNG = IconLoader.getIcon("/icons/png/close-line.png", UIUtils.class);
    public static final Icon CLOSE_LINE_SVG = IconLoader.getIcon("/icons/svg/close-fill.svg", UIUtils.class);
    public static final Icon CLOSE_LINE_BLACK_PNG = IconLoader.getIcon("/icons/png/close-line-black.png", UIUtils.class);
    public static final Icon TRASH_PROMPT = IconLoader.getIcon("/icons/svg/trash_prompt.svg", UIUtils.class);
    public static final Icon SAVE_CANDIDATE_GREEN_SVG = IconLoader.getIcon("/icons/svg/save-line.svg", UIUtils.class);
    public static final Icon CLOSE_FILE_RED_SVG = IconLoader.getIcon("/icons/svg/close_case_preview.svg",
            UIUtils.class);
    public static final Icon DELETE_CANDIDATE_RED_SVG = IconLoader.getIcon("/icons/svg/delete-bin-6-line.svg",
            UIUtils.class);
    public static final Icon REPLAY_PINK = IconLoader.getIcon("/icons/svg/replay-all-pink.svg", UIUtils.class);
    public static final Icon ICON_EXECUTE_METHOD = IconLoader.getIcon("/icons/png/play-icon.png", UIUtils.class);
    public static final Icon ICON_EXECUTE_METHOD_SMALLER = IconUtil.scale(ICON_EXECUTE_METHOD, null,
            JBUIScale.scale(16.0f) / ICON_EXECUTE_METHOD.getIconWidth());
    public static final JBColor HIGHLIGHT_BACKGROUND_COLOR = new JBColor(
            new Color(236, 227, 227, 255),
            new Color(56, 53, 53, 255)
    );
    public static final JBColor ASSERTION_PASSING_COLOR = new JBColor(new Color(171, 232, 206),
            new Color(171, 232, 206));
    public static final JBColor ASSERTION_FAILING_COLOR = new JBColor(new Color(197, 48, 48), new Color(197, 48, 48));
    public static final Icon COTRACT_UP_DOWN_ICON = IconLoader.getIcon("/icons/png/contract-up-down-line-black.png",
            UIUtils.class);
    public static final Icon EXPAND_UP_DOWN_ICON = IconLoader.getIcon("/icons/png/expand-up-down-line-black.png",
            UIUtils.class);
    public static Color teal = new JBColor(new Color(1, 204, 245), new Color(1, 204, 245));  // 01CCF5
    public static Color tealdark = new JBColor(new Color(1, 172, 245), new Color(1, 172, 245));  // 01CCF5
    public static Color pink = new JBColor(new Color(254, 100, 216), new Color(254, 100, 216));  // FE64D8
    public static Color red = new JBColor(new Color(245, 101, 101), new Color(245, 101, 101));
    public static Color green = new JBColor(new Color(56, 161, 105), new Color(56, 161, 105));
    public static Color yellow_alert = new JBColor(new Color(225, 163, 54), new Color(225, 163, 54));
    public static Color orange = new JBColor(new Color(221, 107, 32), new Color(221, 107, 32));
    public static JBColor defaultForeground = new JBColor(Color.black, Gray._187);
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
    public static Icon CREATED_BY = IconLoader.getIcon("/icons/svg/created_by.svg", UIUtils.class);
    public static Icon STATUS = IconLoader.getIcon("/icons/svg/status.svg", UIUtils.class);
    public static Icon MACHINE = IconLoader.getIcon("/icons/svg/machine.svg", UIUtils.class);
    public static Icon CLOCK = IconLoader.getIcon("/icons/svg/clock.svg", UIUtils.class);
    public static Icon TESTTUBE = IconLoader.getIcon("/icons/svg/testtube.svg", UIUtils.class);
    public static Icon MOCK_DATA = IconLoader.getIcon("/icons/svg/mock_data.svg", UIUtils.class);
    public static Icon DIFF_GUTTER = IconLoader.getIcon("/icons/svg/diff_gutter.svg", UIUtils.class);
    public static Icon NO_DIFF_GUTTER = IconLoader.getIcon("/icons/svg/no_diff_gutter.svg", UIUtils.class);
    public static Icon MOCK_ADD = IconLoader.getIcon("/icons/svg/mock_add.svg", UIUtils.class);

    // onboarding page image asset
    public static Icon POSTMAN = IconLoader.getIcon("/icons/svg/postman.svg", UIUtils.class);
    public static Icon SWAGGER = IconLoader.getIcon("/icons/svg/swagger.svg", UIUtils.class);
    public static Icon UNLOGGED_ONBOARDING = IconLoader.getIcon("/icons/svg/unlogged_onboarding.svg", UIUtils.class);
    public static Icon PLAY_ARROW = IconLoader.getIcon("/icons/svg/play_arrow.svg", UIUtils.class);
    public static Icon LINK_ARROW = IconLoader.getIcon("/icons/svg/link_arrow.svg", UIUtils.class);
    public static Icon VIDEO_BANNER = IconLoader.getIcon("/icons/png/video_banner.png", UIUtils.class);
    public static Icon BELL_ICON = IconLoader.getIcon("/icons/svg/bell_icon.svg", UIUtils.class);
    public static Icon UNLOGGED_SETUP = IconLoader.getIcon("/icons/svg/unlogged_setup.svg", UIUtils.class);

    public static Icon EDIT = IconLoader.getIcon("/icons/svg/edit.svg", UIUtils.class);
    public static Icon SAVE = IconLoader.getIcon("/icons/svg/save.svg", UIUtils.class);
    public static Icon CANCEL = IconLoader.getIcon("/icons/svg/cancel.svg", UIUtils.class);

    public static Icon NO_AGENT_GUTTER = IconLoader.getIcon("/icons/svg/no_agent_gutter.svg", UIUtils.class);
    public static Icon EXECUTE_COMPONENT = IconLoader.getIcon("/icons/svg/execute_component.svg", UIUtils.class);
    public static Icon EXCEPTION_CASE = IconLoader.getIcon("/icons/svg/exception_diff_case.svg", UIUtils.class);
    public static Icon PROCESS_RUNNING = IconLoader.getIcon("/icons/svg/process_running.svg", UIUtils.class);
    public static Icon EXECUTE = AllIcons.Actions.ProfileBlue;
    public static Icon PROCESS_NOT_RUNNING = IconLoader.getIcon("/icons/svg/process_not_running.svg", UIUtils.class);
    public static Icon DATA_AVAILABLE = IconLoader.getIcon("/icons/svg/data_available_v2.svg", UIUtils.class);
    public static Icon ATOMIC_TESTS = IconLoader.getIcon("/icons/svg/atomic_tests.svg", UIUtils.class);
    public static Icon DIRECT_INVOKE_EXECUTE = IconLoader.getIcon("/icons/svg/direct-invoke-execute.svg",
            UIUtils.class);
    public static Icon EXECUTE_METHOD = IconLoader.getIcon("/icons/svg/execute_method.svg", UIUtils.class);
    public static Icon COVERAGE_TOOL_WINDOW_ICON = IconLoader.getIcon("/icons/svg/coverage-tool-window-icon.png",
            UIUtils.class);
    public static Icon ORANGE_EXCEPTION = IconLoader.getIcon("/icons/svg/exception_orange.svg", UIUtils.class);
    public static Icon FLASH_LIGHT_LINE = IconLoader.getIcon("/icons/svg/flashlight-line.svg", UIUtils.class);
    public static Icon TEST_TUBE_LINE = IconLoader.getIcon("/icons/svg/test-tube-line.svg", UIUtils.class);
    public static Icon SAVE_LINE = IconLoader.getIcon("/icons/png/save_candidate.png", UIUtils.class);
    public static Icon EXECUTE_ICON_OUTLINED_SVG = IconLoader.getIcon("/icons/svg/execute-button-outlined.svg",
            UIUtils.class);
    public static Icon ONBOARDING_ICON_DARK = IconLoader.getIcon("/icons/png/onboarding_icon_dark.png", UIUtils.class);
    public static Icon ONBOARDING_ICON_PINK = IconLoader.getIcon("/icons/png/onboarding_icon_pink.png", UIUtils.class);
    public static Icon ONBOARDING_ICON_TEAL = IconLoader.getIcon("/icons/png/onboarding_icon_teal.png", UIUtils.class);
    public static Icon TEST_CASES_ICON_DARK = IconLoader.getIcon("/icons/png/test_case_icon_dark.png", UIUtils.class);
    public static Icon TEST_CASES_ICON_PINK = IconLoader.getIcon("/icons/png/test_cases_icon_pink.png", UIUtils.class);
    public static Icon COMPASS_DISCOVER_LINE = IconLoader.getIcon("/icons/png/compass-discover-line.png",
            UIUtils.class);
    public static Icon PUSHPIN_LINE = IconLoader.getIcon("/icons/png/pushpin-line.png", UIUtils.class);
    public static Icon PUSHPIN_2_FILL = IconLoader.getIcon("/icons/png/pushpin-2-fill.png", UIUtils.class);
    public static Icon PUSHPIN_2_LINE = IconLoader.getIcon("/icons/png/pushpin-2-line.png", UIUtils.class);
    public static Icon UNPIN_LINE = IconLoader.getIcon("/icons/png/unpin-line.png", UIUtils.class);
    public static Icon FILTER_LINE = IconLoader.getIcon("/icons/png/filter-line.png", UIUtils.class);
    public static Icon FILTER_LINE_2 = IconLoader.getIcon("/icons/png/filter-2-line.png", UIUtils.class);
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
    public static Icon INFO_ICON = AllIcons.General.ShowInfos;
    public static Icon GHOST_MOCK = IconLoader.getIcon("/icons/svg/mock_ghost_icon_v2.svg", UIUtils.class);

    public UIUtils() throws IOException, FontFormatException {
    }


    public static Icon getGutterIconForState() {
        return EXECUTE;
    }
}
