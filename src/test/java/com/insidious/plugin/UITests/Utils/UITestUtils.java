package com.insidious.plugin.UITests.Utils;

import com.insidious.plugin.UITests.UIElementNotFoundException;
import com.insidious.plugin.UITests.wrapper.FilterOptions;
import com.insidious.plugin.UITests.wrapper.RemoteRobotController;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;

import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.swing.timing.Pause.pause;

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
        ADD_ICON("//div[@accessiblename='Add' and @class='ActionButton' and @myaction='Add (null)']"),
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
        JTEXT_FIELD("//div[@class='JTextField']"),
        FILTER_BUTTON_NEW("//div[@myicon='filter.svg']"),
        FILTER_FOLLOW_CHECK_BOX("//div[@class='JTabbedPane']//div[@class='JCheckBox']"),
        CHECK_BOX("//div[@class='JCheckBox']"),
        FILTER_APPLY("//div[@text='Apply']"),
        FILTER_CANCEL("//div[@text='Cancel']"),
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
        GIT_ROLLBACK_BUTTON("//div[@text='Rollback']"),
        MULTI_MOCK_SINGL_LINE_PANEL("//div[@class='EngravedLabel']"),
        EDIT_MOCK_ENTRY_BUTTON("//div[@myicon='edit.svg']"),
        MOCK_POPUP_SCROLL_PANEL("//div[@class='JScrollPane'][.//div[@class='JCheckBox']]"),
        MOCK_POPUP_CLOSE_ICON("//div[@tooltiptext='Close']"),
        TOOLBAR_SELECT_ALL("//div[@myicon='selectall.svg']"),
        TOOLBAR_REFRESH("//div[@myicon='refresh.svg']"),
        TOOLBAR_DELETE_ICON("//div[@myicon='gc.svg']"),
        JUNIT_TOP_TOOLBAR("//div[@myicon='tests.svg']"),
        CONNECTED_STATE_LABEL("//div[@text='Connected']"),
        DISCONNECTED_STATE_LABEL("//div[@text='Disconnected']"),
        MOCK_EDIT_PANEL_RETURN_TYPE_SELECTOR("//div[@visible_text='Return']"),
        MOCK_EDIT_RETURN_TYPE_NULL("//div[@text='Return null']"),
        LIBRARY_HEADER_TAB("//div[@text='Library']"),
        LIVE_HEADER_TAB("//div[@text='Live']"),
        RADIO_BUTTON("//div[@class='JRadioButton']"),
        LIBRARY_MOCKS_RADIO_BUTTON("//div[@visible_text='Mocks']]"),
        MOCK_EDIT_RETURN_TYPE_EXCEPTION("//div[@text='Throw exception']"),
        GOT_IT_TEXT("//div[@text='Got It']"),
        TERMINAL_TOOLBAR_SELECTABLE("//div[@text='Terminal']"),
        SHELL_WIDGET("//div[@class='ShellTerminalWidget']"),
        TERMINAL_TOOL_WINDOW_HIDE_BUTTON("//div[@class='ToolWindowHeader'][.//div[@text='Terminal:']]//div[@tooltiptext='Hide']"),
        CANCEL_MOCK_BUTTON("//div[@text='Cancel']"),
        MAVEN_TOOLBAR_BUTTON("//div[@text='Maven']"),
        MAVEN_REFRESH_SVG("//div[@myicon='refresh.svg']"),
        MAVEN_TOOLBAR_HIDE("//div[contains(@myvisibleactions, '[Options')]//div[@tooltiptext='Hide']]"),
        MY_CONTENT_PANEL("//div[@class='MyContentPanel']"),
        FILTER_TITLE_PANEL("//div[@class='TitlePanel']"),
        REMOTE_SERVER_RADIO_BUTTON_LABEL("//div[@accessiblename='Remote Server Scanning for logs on the server' and @class='JRadioButton' and @text='<html>Remote Server<br><small>Scanning for logs on the server<small></html>']"),
        LOCALHOST_RADIO_BUTTON_LABEL("//div[@accessiblename='Localhost Logs on your local machine' and @class='JRadioButton' and @text='<html>Localhost<br><small>Logs on your local machine</small></html>']"),
        SESSIONS_LIST_BUTTON("//div[@text='Check for sessions']"),
        TREE("//div[@class='Tree']"),
        TERMINAL_PANEL("//div[@class='JBTerminalPanel']"),
        GIT_ROLLBACK_CHANGES_VIEWPORT("//div[@class='ChangesBrowserTreeList']"),
        LOCAL_HYPERLINK_FILTER("//div[@accessiblename='[Local]']"),
        CLEAR_FILTERS_LABEL("//div[@visible_text='Clear filters']"),
        GO_TO_DIRECT_INVOKE("//div[@defaulticon='execute.svg']");

        private String value;

        UITags(String s) {
            value = s;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static ComponentFixture getComponentFixture(RemoteRobot remoteRobot, UITags tag) throws UIElementNotFoundException {
        int tries = 5;
        boolean found = false;
        while (tries > 0 && !found) {
            try {
                return remoteRobot.find(ComponentFixture.class, byXpath(tag.toString()));
            } catch (WaitForConditionTimeoutException timeoutException) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        throw new UIElementNotFoundException("Component with Tag : " + tag.name() + " not found on UI");
    }

    public static ContainerFixture getContainerFixture(RemoteRobot remoteRobot, UITags tag) throws UIElementNotFoundException {
        int tries = 5;
        boolean found = false;
        while (tries > 0 && !found) {
            try {
                ContainerFixture result = remoteRobot.find(ContainerFixture.class, byXpath(tag.toString()));
                found = true;
                return result;
            } catch (WaitForConditionTimeoutException timeoutException) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        throw new UIElementNotFoundException("Container with Tag : " + tag.name() + " not found on UI");
    }

    public static List<ComponentFixture> getComponentFixtures(RemoteRobot remoteRobot, UITags tag) throws UIElementNotFoundException {
        int tries = 5;
        boolean found = false;
        while (tries > 0 && !found) {
            try {
                List<ComponentFixture> result = remoteRobot.findAll(ComponentFixture.class, byXpath(tag.toString()));
                found = true;
                return result;
            } catch (WaitForConditionTimeoutException timeoutException) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        throw new UIElementNotFoundException("Components with Tag : " + tag.name() + " not found on UI");
    }

    public static ComponentFixture getComponentFixture(RemoteRobot remoteRobot, String xpath) throws UIElementNotFoundException {
        int tries = 5;
        boolean found = false;
        while (tries > 0 && !found) {
            try {
                ComponentFixture result = remoteRobot.find(ComponentFixture.class, byXpath(xpath));
                found = true;
                return result;
            } catch (WaitForConditionTimeoutException timeoutException) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        throw new UIElementNotFoundException("Component with XPATH : " + xpath + " not found on UI");
    }

    public static List<ComponentFixture> getComponentFixtures(RemoteRobot remoteRobot, String xpath) throws UIElementNotFoundException {
        int tries = 5;
        boolean found = false;
        while (tries > 0 && !found) {
            try {
                List<ComponentFixture> result = remoteRobot.findAll(ComponentFixture.class, byXpath(xpath));
                found = true;
                return result;
            } catch (WaitForConditionTimeoutException timeoutException) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        throw new UIElementNotFoundException("Components with XPATH : " + xpath + " not found on UI");
    }

    public static ContainerFixture getContainerFixture(RemoteRobot remoteRobot, String xpath) throws UIElementNotFoundException {
        int tries = 5;
        boolean found = false;
        while (tries > 0 && !found) {
            try {
                ContainerFixture result = remoteRobot.find(ContainerFixture.class, byXpath(xpath));
                found = true;
                return result;
            } catch (WaitForConditionTimeoutException timeoutException) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        throw new UIElementNotFoundException("Component with XPATH : " + xpath + " not found on UI");
    }

    public static void setFilterOptionsForCurrentView(RemoteRobotController controller, FilterOptions filterOptions) {
        if (filterOptions.isClearFilters()) {
            try {
                controller.getIdeaFrame().getClearFiltersLabel().click();
            } catch (Exception e) {
                //No filters were active
            }
        }

        controller.getIdeaFrame().getFilterButton().click();
        pause(ofMillis(500).toMillis());
        List<ComponentFixture> fixtures = controller.getIdeaFrame().getAllAddIconComponents();
        System.out.println("Fixture List Size : " + fixtures.size());
        assert fixtures.size() == 4;

        //set Included classes
        filterOptions.getIncludedClasses().forEach(option -> {
            fixtures.get(0).click();
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().enterText(option);
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().hotKey(VK_ENTER);
        });

        //set Excluded classes
        filterOptions.getExcludedClasses().forEach(option -> {
            fixtures.get(1).click();
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().enterText(option);
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().hotKey(VK_ENTER);
        });

        //set included methods
        filterOptions.getIncludedMethods().forEach(option -> {
            fixtures.get(2).click();
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().enterText(option);
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().hotKey(VK_ENTER);
        });

        //set excluded methods
        filterOptions.getExcludedMethods().forEach(option -> {
            fixtures.get(3).click();
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().enterText(option);
            pause(ofMillis(250).toMillis());
            controller.getKeyboard().hotKey(VK_ENTER);
        });

        controller.getIdeaFrame().getFilterApplyButton().click();
    }
}
