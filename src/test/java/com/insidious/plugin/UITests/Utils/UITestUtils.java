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
        CHECK_BOX("//div[@class='JCheckBox']"),
        FILTER_APPLY("//div[@text='Apply']"),
        SELECT_ALL_FILTER("//div[@visible_text='Select all']"),
        SAVE_FORM_CONFIRM("//div[@text='Confirm']"),
        SAVE_GLOBAL("//div[@visible_text='Save']"),
        CLOSE_DI_COMPONENT_BUTTON("//div[@visible_text='Close']"),
        REPLAY_CANDIDATE_BUTTON("//div[@myicon='replay-all-pink.svg']"),
        LINK_MOCK_POPUP_BUTTON("//div[@myicon='link.svg']"),
        ADD_NEW_MOCK_POPUP_BUTTON("//div[@myicon='add.svg']"),
        UNLINK_MOCK_POPUP_BUTTON("//div[@visible_text='Un-Mock']"),
        EDITOR_SCROLL_BAR("//div[@class='MyScrollPane']"),
        SAVE_MOCK_BUTTON("//div[@text='Save']"),
        MULTI_MOCK_SINGL_LINE_PANEL("//div[@class='EngravedLabel']"),
        EDIT_MOCK_ENTRY_BUTTON("//div[@myicon='edit.svg']"),
        MOCK_POPUP_SCROLL_PANEL("//div[@class='JScrollPane'][.//div[@class='JCheckBox']]"),
        MOCK_POPUP_CLOSE_ICON("//div[@tooltiptext='Close']"),
        TOOLBAR_SELECT_ALL("//div[@myicon='selectall.svg']"),
        TOOLBAR_REFRESH("//div[@myicon='refresh.svg']"),
        TOOLBAR_DELETE_ICON("//div[@myicon='gc.svg']"),
        MOCK_EDIT_PANEL_RETURN_TYPE_SELECTOR("//div[@visible_text='Return']"),
        MOCK_EDIT_RETURN_TYPE_NULL("//div[@text='Return null']"),
        LIBRARY_HEADER_TAB("//div[@text='Library']"),
        LIVE_HEADER_TAB("//div[@text='Live']"),
        RADIO_BUTTON("//div[@class='JRadioButton']"),
        LIBRARY_MOCKS_RADIO_BUTTON("//div[@visible_text='Mocks']]"),
        MOCK_EDIT_RETURN_TYPE_EXCEPTION("//div[@text='Throw exception']"),
        CANCEL_MOCK_BUTTON("//div[@text='Cancel']");

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

//    public static JPanelFixture getJpanelFixture(RemoteRobot remoteRobot, String xapth) {
//        return remoteRobot.find(JPanelFixture.class, byXpath(xapth));
//    }

    public static ContainerFixture getContainerFixture(RemoteRobot remoteRobot, UITags tag) {
        return remoteRobot.find(ContainerFixture.class, byXpath(tag.toString()));
    }

    public static List<ComponentFixture> getComponentFixtures(RemoteRobot remoteRobot, UITags tag) {
        return remoteRobot.findAll(ComponentFixture.class, byXpath(tag.toString()));
    }

    public static ComponentFixture getComponentFixture(RemoteRobot remoteRobot, String xpath) {
        return remoteRobot.find(ComponentFixture.class, byXpath(xpath));
    }

    public static List<ComponentFixture> getComponentFixtures(RemoteRobot remoteRobot, String xpath) {
        return remoteRobot.findAll(ComponentFixture.class, byXpath(xpath));
    }

    public static ContainerFixture getContainerFixture(RemoteRobot remoteRobot, String xpath) {
        return remoteRobot.find(ContainerFixture.class, byXpath(xpath));
    }
}
