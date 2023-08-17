package com.insidious.plugin.UITests.pages;

import com.insidious.plugin.UITests.Utils.UITestUtils;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.*;
import org.jetbrains.annotations.NotNull;

@FixtureName(name = "Idea frame")
@DefaultXpath(by = "IdeFrameImpl type", xpath = "//div[@class='IdeFrameImpl']")
public class IdeaFrame extends CommonContainerFixture {

    private RemoteRobot remoteRobot;

    public IdeaFrame(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
        this.remoteRobot = remoteRobot;
    }

    public ContainerFixture getProjectViewTree() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.PROJECT_TREE_VIEW);
    }

    public ComponentFixture getProjectNavItem() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.PROJECT_NAV_ITEM);
    }

    public String getProjectName() {
        return callJs("component.getProject().getName();");
    }

//    public ComponentFixture getMenuBar() {
//        Locator menuBarFixture = byXpath("JMenuBarFixture", "//div[@class='JMenuBarFixture']");
//        ComponentFixture projectViewTree = remoteRobot.find(JMenuBarFixture.class, menuBarFixture);
//        return projectViewTree;
//    }

    public boolean isDumbMode() {
        return callJs(
                """
                            const frameHelper = com.intellij.openapi.wm.impl.ProjectFrameHelper.getFrameHelper(component)
                            if (frameHelper) {
                                const project = frameHelper.getProject()
                                project ? com.intellij.openapi.project.DumbService.isDumb(project) : true
                            } else { 
                                true 
                            }
                        """, true
        );
    }

    public ComponentFixture getUnloggedToolbarComponent() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.UNLOGGED_TOOL_BAR_COMPONENT);
    }

    public ComponentFixture getDirectInvokeTabHeader() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.UNLOGGED_DIRECT_INVOKE_TAB_HEADER);
    }

    public ComponentFixture getExecuteMethodButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.UNLOGGED_DIRECT_INVOKE_EXECUTE_BUTTON);
    }

    public ComponentFixture getDebugButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.DEBUG_BUTTON);
    }

    public ComponentFixture getExpandAllButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.EXPAND_ALL);
    }

    public ComponentFixture getStopButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.STOP_BUTTON);
    }

    public ComponentFixture getHideDebugToolBarIcon() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.HIDE_DEBUG_TOOLBAR);
    }
}
