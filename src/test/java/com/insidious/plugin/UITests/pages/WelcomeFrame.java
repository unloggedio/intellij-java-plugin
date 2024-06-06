package com.insidious.plugin.UITests.pages;

import com.insidious.plugin.UITests.Utils.UITestUtils;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

@FixtureName(name = "Welcome Frame")
@DefaultXpath(by = "type", xpath = "//div[@class='FlatWelcomeFrame']")
public class WelcomeFrame extends CommonContainerFixture {

    private RemoteRobot remoteRobot;

    public WelcomeFrame(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
        this.remoteRobot = remoteRobot;
    }

    public ComponentFixture getCreateProjectLink() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.CREATE_NEW_PROJECT);
    }

    public ComponentFixture getOpenProjectButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.OPEN_PROJECT);
    }

    public ComponentFixture getMoreActions() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MORE_ACTIONS);
    }

    public ComponentFixture getProjectSelectorComboBox() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.OPEN_PROJECT_TEXT_FIELD);
    }

    public ComponentFixture getOpenConfirmButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.OPEN_PROJECT_OK_BUTTON);
    }

    public ComponentFixture getVcsCreateOption() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.CREATE_FROM_VCS_OPTION);
    }

    public ComponentFixture getVcsRepoUrlTextField() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.VCS_CREATE_URL_TEXT_FIELD);
    }

    public ComponentFixture getVcsCloneButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.VCS_CREATE_CLONE_BUTTON);
    }
}
