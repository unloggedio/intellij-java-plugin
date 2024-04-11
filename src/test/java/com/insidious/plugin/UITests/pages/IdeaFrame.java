package com.insidious.plugin.UITests.pages;

import com.insidious.plugin.UITests.Utils.UITestUtils;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    public ComponentFixture getEditorScrollBar() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.EDITOR_SCROLL_BAR);
    }

    public ComponentFixture getLocateButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.LOCATE_FILE);
    }

    public ComponentFixture getReplayTab() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.REPLAY_TAB);
    }

    public ComponentFixture getFirstReplayButton() {
        List<ComponentFixture> fixtureList = UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.REPLAY_EXECUTE_BUTTON);
        return fixtureList.get(0);
    }

    public ComponentFixture getFirstJtextFiled() {
        List<ComponentFixture> fixtureList = UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.TEST_NAME_TF);
        return fixtureList.get(0);
    }

    public ComponentFixture getSaveReplayButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.SAVE_REPLAY_BUTTON);
    }

    public ComponentFixture getSaveAndClose() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.SAVE_AND_CLOSE_SAVE_FORM);
    }

    public ComponentFixture getDirectInvokeExecuteButtonNew() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.DIRECT_INVOKE_EXECUTE_NEW);
    }

    public ComponentFixture getFilterButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.FILTER_BUTTON_NEW);
    }

    public ComponentFixture getDICloseButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.CLOSE_DI_COMPONENT_BUTTON);
    }

    public ComponentFixture getReplayButtonNew() {
        List<ComponentFixture> fixtureList = UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.REPLAY_CANDIDATE_BUTTON);
        return fixtureList.get(0);
    }

    public ComponentFixture getFilterFollowCheckbox() {
        List<ComponentFixture> fixtureList = UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.FILTER_FOLLOW_CHECK_BOX);
        return fixtureList.get(0);
    }

    public List<ComponentFixture> getAllVisibleCheckBoxes() {
        return UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.CHECK_BOX);
    }

    public List<ComponentFixture> getAllVisibleRadioButtons() {
        return UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.RADIO_BUTTON);
    }

    public ComponentFixture getFilterApplyButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.FILTER_APPLY);
    }

    public ComponentFixture getSelectAllText() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.SELECT_ALL_FILTER);
    }

    public ComponentFixture getSaveGlobalButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.SAVE_GLOBAL);
    }

    public ComponentFixture getSaveFromConfirmButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.SAVE_FORM_CONFIRM);
    }

    public ComponentFixture getMockPopupScrollPanel() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MOCK_POPUP_SCROLL_PANEL);
    }

    public ComponentFixture getMyContentPanel() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MY_CONTENT_PANEL);
    }

    public ComponentFixture getMavenToolbarIcon() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MAVEN_TOOLBAR_BUTTON);
    }

    public ComponentFixture getMavenToolBarRefreshIcon() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MAVEN_REFRESH_SVG);
    }

    public List<ComponentFixture> getMockPopupScrollPanelCandidates() {
        return UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.MOCK_POPUP_SCROLL_PANEL);
    }

    public List<ComponentFixture> getMockPopupScrollPanelCandidatesAsContainers() {
        return UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.MOCK_POPUP_SCROLL_PANEL);
    }

    public ComponentFixture getComponentByXpath(String panelXpath) {
        return UITestUtils.getComponentFixture(remoteRobot, panelXpath);
    }

    public ContainerFixture getContainerByXpath(String panelXpath) {
        return UITestUtils.getContainerFixture(remoteRobot, panelXpath);
    }

    public ComponentFixture getMockPopupCloseButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MOCK_POPUP_CLOSE_ICON);
    }

    public ComponentFixture getCreateNewMockButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.ADD_NEW_MOCK_POPUP_BUTTON);
    }

    public ComponentFixture getUnlinkMockButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.UNLINK_MOCK_POPUP_BUTTON);
    }

    public ComponentFixture getMockEditSaveButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.SAVE_MOCK_BUTTON);
    }

    public ComponentFixture getMockMultiSelectPanel() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MULTI_MOCK_SINGL_LINE_PANEL);
    }

    public ComponentFixture getSelectAllicon() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.TOOLBAR_SELECT_ALL);
    }


    public ComponentFixture getMockEditReturnTypeHeader() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MOCK_EDIT_PANEL_RETURN_TYPE_SELECTOR);
    }

    public ComponentFixture getMockEditPanelReturnNull() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MOCK_EDIT_RETURN_TYPE_NULL);
    }

    public ComponentFixture getMockEditPanelReturnException() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.MOCK_EDIT_RETURN_TYPE_EXCEPTION);
    }

    public ComponentFixture getlibraryTabHeader() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.LIBRARY_HEADER_TAB);
    }

    public ComponentFixture getLibraryMocksButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.LIBRARY_MOCKS_RADIO_BUTTON);
    }

    public ComponentFixture getRefreshButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.TOOLBAR_REFRESH);
    }

    public ComponentFixture getToolBarDeleteButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.TOOLBAR_DELETE_ICON);
    }

    public ComponentFixture getLiveTabHeader() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.LIVE_HEADER_TAB);
    }

    public List<ComponentFixture> getGotItTexts() {
        return UITestUtils.getComponentFixtures(remoteRobot, UITestUtils.UITags.GOT_IT_TEXT);
    }

    public ComponentFixture findCopyButton() {
        return UITestUtils.getComponentFixtures(remoteRobot, "//div[@class='JButton']").stream()
                .filter(component ->
                        component.getData().getAll().isEmpty())
                .toList().get(0);
    }

    public ComponentFixture getTerminalToolWindowHideButton() {
        return UITestUtils.getComponentFixture(remoteRobot, UITestUtils.UITags.TERMINAL_TOOL_WINDOW_HIDE_BUTTON);
    }

    public ContainerFixture getShellWidget() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.SHELL_WIDGET);
    }

    public ContainerFixture getJunitTopToolbarIcon() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.JUNIT_TOP_TOOLBAR);
    }

    public ComponentFixture getmavenToolBarHideIcon() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.MAVEN_TOOLBAR_HIDE);
    }

    public ComponentFixture getConnectedLabel() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.CONNECTED_STATE_LABEL);
    }

    public ComponentFixture getDisconnectedLabel() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.DISCONNECTED_STATE_LABEL);
    }

    public ComponentFixture getTerminalToolBarSelectable() {
        return UITestUtils.getContainerFixture(remoteRobot, UITestUtils.UITags.TERMINAL_TOOLBAR_SELECTABLE);
    }
}
