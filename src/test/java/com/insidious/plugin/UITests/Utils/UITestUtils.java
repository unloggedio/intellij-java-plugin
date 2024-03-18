package com.insidious.plugin.UITests.Utils;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;

import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

public class UITestUtils {

    //These tag values can be fetched when you open localhost:8082 as you run with "runIdeForUiTests".
    //The component will only show up in that page if it's currently visible in the Ui.
    public enum UITags {
        PROJECT_TREE_VIEW("//div[@class='ProjectViewTree']"),
        PROJECT_NAV_ITEM("//div[contains(@text.key, 'project.scheme')]"),
        UNLOGGED_TOOL_BAR_COMPONENT("//div[@text='Unlogged']"),
        UNLOGGED_DIRECT_INVOKE_TAB_HEADER("//div[@text='Direct Invoke']"),
        UNLOGGED_DIRECT_INVOKE_EXECUTE_BUTTON("//div[@class='JButton' and @text='Execute method']"),
        DEBUG_BUTTON("//div[@myicon='startDebugger.svg']"),
        CREATE_NEW_PROJECT("//div[(@class='MainButton' and @text='New Project') or (@accessiblename='New Project' and @class='JButton')]"),
        OPEN_PROJECT("//div[@accessiblename.key='action.WelcomeScreen.OpenProject.text']"),
        MORE_ACTIONS("//div[@accessiblename='More Actions']"),
        OPEN_PROJECT_TEXT_FIELD("//div[@class='BorderlessTextField']"),
        EXPAND_ALL("//div[contains(@myaction.key, 'action.ExpandAll.text')]"),
        STOP_BUTTON("//div[contains(@myaction.key, 'action.stop')]"),
        HIDE_DEBUG_TOOLBAR("//div[contains(@myvisibleactions, '[Options')]//div[@myaction.key='tool.window.hide.action.name']"),
        OPEN_PROJECT_OK_BUTTON("//div[@text.key='button.ok']"),
        LOCATE_FILE("//div[@tooltiptext.key='action.SelectOpenedFileInProjectView.text']"),
        REPLAY_TAB("//div[@text='Replay']"),
        REPLAY_EXECUTE_BUTTON("//div[@defaulticon='execute-button-outlined.svg']"),
        SAVE_REPLAY_BUTTON("//div[@text='Save Replay']"),
        SAVE_AND_CLOSE_SAVE_FORM("//div[@text='Save and close']"),
        DIRECT_INVOKE_EXECUTE_NEW("//div[@visible_text='Execute Method']"),
        TEST_NAME_TF("//div[@class='JTextField']"),
        FILTER_BUTTON_NEW("//div[@myicon='filter.svg']"),
        FILTER_FOLLOW_CHECK_BOX("//div[@class='JTabbedPane']//div[@class='JCheckBox']"),
        FILTER_APPLY("//div[@text='Apply']"),
        SELECT_ALL_FILTER("//div[@visible_text='Select all']"),
        SAVE_FORM_CONFIRM("//div[@text='Confirm']"),
        SAVE_GLOBAL("//div[@visible_text='Save']"),
        CLOSE_DI_COMPONENT_BUTTON("//div[@visible_text='Close']"),
        REPLAY_CANDIDATE_BUTTON("//div[@myicon='replay-all-pink.svg']"),
        EDITOR_SCROLL_BAR("//div[@class='MyScrollPane']");

        private String value;

        UITags(String s) {
            value = s;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static ComponentFixture getComponentFixture(RemoteRobot remoteRobot, UITags tag) {
        return remoteRobot.find(ComponentFixture.class, byXpath(tag.toString()));
    }

    public static ContainerFixture getContainerFixture(RemoteRobot remoteRobot, UITags tag) {
        return remoteRobot.find(ContainerFixture.class, byXpath(tag.toString()));
    }

    public static List<ComponentFixture> getComponentFixtures(RemoteRobot remoteRobot, UITags tag) {
        return remoteRobot.findAll(ComponentFixture.class, byXpath(tag.toString()));
    }
}
