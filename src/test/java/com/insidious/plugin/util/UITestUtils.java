package com.insidious.plugin.util;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;

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
        OPEN_PROJECT_OK_BUTTON("//div[@text.key='button.ok']");

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
}
