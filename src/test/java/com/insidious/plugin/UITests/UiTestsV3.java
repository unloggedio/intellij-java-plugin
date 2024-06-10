
package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.UiTestInteractionUtils;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.insidious.plugin.UITests.wrapper.*;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.List;

import static com.insidious.plugin.UITests.Utils.UITestUtils.setSdkVersion;
import static com.insidious.plugin.UITests.Utils.UiTestInteractionUtils.*;
import static java.awt.event.KeyEvent.*;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.insidious.plugin.UITests.Utils.UITestUtils.setFilterOptionsForCurrentView;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UiTestsV3 {
    private RemoteRobotController controller;
    private GitProjectInfo projectUnderTest;

    public UiTestsV3() {
        RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
        Keyboard keyboard = new Keyboard(remoteRobot);
        controller = new RemoteRobotController(remoteRobot, keyboard);

        LocalProjectInfo localProjectInfo = new LocalProjectInfo("unlogged-spring-maven-demo", "start_project.sh",
                "git_rollback.sh", "remove_local_sessions.sh", "UnloggedDemoApplication.java", 60);

        projectUnderTest = new GitProjectInfo("unlogged-spring-maven-demo",
                "https://github.com/unloggedio/unlogged-spring-maven-demo.git",
                "ui_test_clean", "pom.xml", LocalProjectInfo.BuildSystem.MAVEN, 60,
                true, "17", "src/test/java/org/unlogged/demo");
        projectUnderTest.setLocalProjectInfo(localProjectInfo);
    }

    //    @Test
//    @Order(1)
//    @Disabled
    public void openProjectAndAddSDK() {

        step("Open Project", () -> {
            final WelcomeFrame welcomeFrame = controller.getRemoteRobot().find(WelcomeFrame.class, ofSeconds(10));
            welcomeFrame.getOpenProjectButton().click();
            welcomeFrame.getProjectSelectorComboBox().click();

            controller.getKeyboard().enterText("/" + projectUnderTest.getLocalProjectInfo().getProjectPath());
            pause(ofSeconds(1).toMillis());
            welcomeFrame.getOpenConfirmButton().click();
        });

        step("Load idea frame and wait till IDE is in Smart mode", () -> {
            controller.getIdeaFrame();
        });

        step("Revert all changes made to project, remove local sessions", () -> {
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRevertScriptName(), 5);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRemoveScriptName(), 2);
        });

        step("Add unlogged dependency (Mac)", () -> {
            openFileIfNeeded("pom.xml", controller);
            ComponentFixture unloggedToolbar = controller.getIdeaFrame().getUnloggedToolbarComponent();
            unloggedToolbar.moveMouse();
            unloggedToolbar.click();
            pause(ofSeconds(1).toMillis());

            ComponentFixture copyButton = controller.getIdeaFrame().findCopyButton();
            copyButton.moveMouse();
            copyButton.click();

            //TODO: Update logic to take gradle projects as well
            //paste right after dependencies
            TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
            List<RemoteText> pomContents = textEditorFixture.getEditor().getData().getAll();
            RemoteText dependencyText = pomContents.stream().filter(remoteText -> remoteText.getText().equals("dependencies")).toList().get(0);
            int indexOfDependencies = pomContents.indexOf(dependencyText);
            RemoteText closingTag = pomContents.get(indexOfDependencies + 1);
            closingTag.click();
            controller.getKeyboard().hotKey(VK_RIGHT);
            controller.getKeyboard().hotKey(VK_ENTER);
            controller.getKeyboard().hotKey(VK_META, VK_V);

            performMavenSync(controller);
            controller.waitForIndex();
        });
    }

    @Test
    @Order(1)
    @Disabled
    public void cloneAndAddSDK() {
        step("Clone project - fresh state, change branch and setup sdk version", () -> {
            cloneAndOpenProject(controller, projectUnderTest);
            //setup sdk version and wait till index is complete, then start tests in normal flow
            setSdkVersion(controller, projectUnderTest);
            controller.waitForIndex();
        });

        step("Revert all changes made to project, remove local sessions", () -> {
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRevertScriptName(), 5);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRemoveScriptName(), 2);
        });

        step("Add unlogged dependency (Mac)", () -> {
            addUnloggedDependenciesToBuildFile(controller, projectUnderTest);
        });
    }

    @Test
    @Order(2)
    @Disabled
    public void remote_mode_general() {

        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";

        //TODO: stop process if running already
        //TODO : revert changes if annotations are already present

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set Source to remote URL", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();

            pause(ofSeconds(10).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            pause(ofSeconds(5).toMillis());

            //TODO: Add an assertion to number of candidates expected here or assert candidates
        });

        //TODO : abstract method to navigate and DirectInvoke methods and assert their responses
        //TODO : re-add inlay hint assertion once #44 is merged
        step("Execute methods and assert responses", () -> {

            List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
            List<DirectInvokeTreeLine> assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
            DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                    "public String getFutureResult(String s1)",
                    inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);

            inputLines = new ArrayList<>();
            assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "OptionalTest"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: OptionalTest"));
            request = new DirectInvokeRequest("FutureController.java",
                    "public String getFutureResultOptional(String s1)",
                    inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);
        });

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    @Test
    @Disabled
    @Order(3)
    public void run_mode_local_general() {

        final String annotationText = "@Unlogged";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectUnderTest.getLocalProjectInfo().getMainClassName(), controller);
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set source filter to Localhost", () -> {
            UiTestInteractionUtils.openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getLocalHostRadioButton().click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            clearGotIts(controller);
        });

        step("Clear reminiscent candidates", () -> {
            controller.getIdeaFrame().getRefreshButton().click();
            pause(ofSeconds(10).toMillis());
            controller.getIdeaFrame().getToolBarDeleteButton().click();
            pause(ofSeconds(10).toMillis());
        });

        step("DirectInvoke and assert results", () -> {
            List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
            List<DirectInvokeTreeLine> assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
            DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                    "public String getFutureResult(String s1)",
                    inputLines, assertions, List.of(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);

            inputLines = new ArrayList<>();
            assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "OptionalTest"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: OptionalTest"));
            request = new DirectInvokeRequest("FutureController.java",
                    "public String getFutureResultOptional(String s1)",
                    inputLines, assertions, List.of(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);
        });

        step("Refresh loaded Candidates", () -> {
            controller.getIdeaFrame().getRefreshButton().click();
            pause(ofSeconds(2).toMillis());
            closeOptionsTabIfOpen(controller);
            controller.getIdeaFrame().getToolBarDeleteButton().click();
        });

        //moving junit flow to it's own cases
    }

    //Server Issues Sheet - Issue 73
    //Also is the start of local test chain
    @Test
    @Order(4)
    @Disabled
    public void serverIssues_73() {
        //start project in local mode
        //after main method candidate Inlayhint click, inlayhints should not disappear

        final String annotationText = "@Unlogged";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectUnderTest.getLocalProjectInfo().getMainClassName(), controller);
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set source filter to Localhost", () -> {
            openUnloggedToolbarIfNotOpen(controller, 1);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getLocalHostRadioButton().click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();
        });

        step("Close Options menu if open", () -> {
            closeOptionsTabIfOpen(controller);
        });

        step("Open main class and enusre InlayHint render behaviour is as expected", () -> {
            openFileIfNeeded(projectUnderTest.getLocalProjectInfo().getMainClassName(), controller);
            //look for inlayHints and assert that clicking on it will not hide it
            TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
            List<RemoteText> texts = textEditorFixture.getData().getAll();
            RemoteText inlayHintText = texts.stream().filter(elem -> elem.getText().equals("1 call")).toList().get(0);

            inlayHintText.click();
            pause(ofMillis(250).toMillis());

            textEditorFixture = controller.getIdeaFrame().textEditor();
            texts = textEditorFixture.getData().getAll();

            try {
                inlayHintText = texts.stream().filter(elem -> elem.getText().equals("1 call")).toList().get(0);
            } catch (Exception e) {
                Assertions.fail("Did not find inlayHints for main method");
            }
        });

//        step("Stop process", () -> {
//            stopProcessInTerminal(controller);
//        });
    }

    //72
    @Test
    @Order(5)
    @Disabled
    public void serverIssues_72() {

        //project is already up and running in local mode
        step("Save Candidates from one of FutureController's methods", () -> {

            //clear notifications before generating tests
            controller.getIdeaFrame().getNotificationTab().click();
            //click on clear all if it exists
            try {
                controller.getIdeaFrame().getComponentByXpath("//div[@class='LinkLabel']").click();
            } catch (Exception e) {
            }

            openUnloggedToolbarIfNotOpen(controller, 1);

            //save all
            controller.getIdeaFrame().getSelectAllicon().click();
            pause(ofMillis(500).toMillis());

            controller.getIdeaFrame().getJunitTopToolbarIcon().click();
            pause(ofMillis(250).toMillis());

            //Notification shouldn't pop up saying test case failed to generate
            Assertions.assertTrue(controller.getIdeaFrame().getTestGenerationFailureBalloonNotification().isEmpty() ||
                    controller.getIdeaFrame().getTestGenerationFailureBalloonNotification() == null);
        });
    }

    @Test
    @Order(6)
    @Disabled
    public void serverIssue_46() {
        //set filter to remote mode
        //don't select a session from remote, try to click on apply
        //asser that the filter tab is still open

        step("Set Source to remote URL, but click on apply without selecting a session", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();

            pause(ofSeconds(7).toMillis());
            int numberOfRadioButtons = controller.getIdeaFrame().getAllVisibleRadioButtons().size();

            controller.getIdeaFrame().getApplyButtonGeneric().click();
            pause(ofSeconds(5).toMillis());

            Assertions.assertTrue(controller.getIdeaFrame().getAllVisibleRadioButtons().size() == numberOfRadioButtons);

            controller.getIdeaFrame().getFilterCancel().click();
        });
    }

    @Test
    @Order(7)
    @Disabled
    public void serverIssue_36_local() {
        step("Select remote mode filter, then cancel, ensure that candidates are generated afterwards", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();

            pause(ofSeconds(7).toMillis());
            controller.getIdeaFrame().getFilterCancel().click();
        });

        step("Clear all previous candidates", () -> {
            controller.getIdeaFrame().getToolBarDeleteButton().click();
            pause(ofMillis(500).toMillis());
        });

        step("DirectInvoke a method and make sure that ", () -> {
            List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
            List<DirectInvokeTreeLine> assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
            DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                    "public String getFutureResult(String s1)",
                    inputLines, assertions, List.of(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);
            pause(ofSeconds(2).toMillis());

            //2 candidates are supposed to be generated for a successful direct Invoke form the above method
            Assertions.assertEquals(2, controller.getIdeaFrame().getAllVisibleCheckBoxes().size());
        });
    }

    @Test
    @Order(8)
    @Disabled
    public void serverIssue_37() {
        //set filter to remote mode
        //don't select a session from remote, try to click on apply
        //asser that the filter tab is still open

        step("Clear all previous notifications if they are present", () -> {
            controller.getIdeaFrame().getNotificationTab().click();
            try {
                controller.getIdeaFrame().getComponentByXpath("//div[@class='LinkLabel']").click();
            } catch (Exception e) {
            }
        });

        step("Set Source to remote URL after a failed attempt at applying remote changes", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();
            pause(ofSeconds(7).toMillis());

            controller.getIdeaFrame().getApplyButtonGeneric().click();
            pause(ofSeconds(5).toMillis());
        });

        //Assert that nothing shows up in notifications
        controller.getIdeaFrame().getNotificationTab().click();
        try {
            controller.getIdeaFrame().getComponentByXpath("//div[@class='LinkLabel']").click();
            Assertions.fail("Got a notification when not expected");
        } catch (Exception e) {
            //pass
        }
    }

    @Test
    @Order(9)
    public void serverIssue_51() {
        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectUnderTest.getLocalProjectInfo().getMainClassName(), controller);
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set Source to remote URL", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();
            pause(ofSeconds(7).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            pause(ofSeconds(5).toMillis());
        });

        step("Clear notifications", () -> {
            controller.getIdeaFrame().getNotificationTab().click();
            try {
                controller.getIdeaFrame().getComponentByXpath("//div[@class='LinkLabel']").click();
            } catch (Exception e) {
            }
        });

        step("Switch to localhost source", () -> {
            openUnloggedToolbarIfNotOpen(controller, 1);
            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getLocalHostRadioButton().click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();
        });

        //Assert no new notifications
        controller.getIdeaFrame().getNotificationTab().click();
        try {
            controller.getIdeaFrame().getComponentByXpath("//div[@class='LinkLabel']").click();
            Assertions.fail("Got a notification when not expected");
        } catch (Exception e) {
            //pass
        }
    }

    @Test
    @Disabled
    public void serverIssues_20_local() {
        //Ensure that the hyperlink text "Local" is visible in Plugin and you open filters when you open it.
        //unlogged toolbar assumed to be open before this.

        step("Close Options menu if open", () -> {
            closeOptionsTabIfOpen(controller);
        });

        step("Look for Local and ensure it opens filters", () -> {
            openUnloggedToolbarIfNotOpen(controller, 1);

            ComponentFixture localMarker = controller.getIdeaFrame().getLocalModeHyperlink();
            localMarker.click();
            pause(ofMillis(250).toMillis());

            Assertions.assertTrue(controller.getIdeaFrame().getLocalHostRadioButton().isShowing());
            Assertions.assertTrue(controller.getIdeaFrame().getRemoteButtonRadioLabel().isShowing());

            controller.getIdeaFrame().getFilterCancel().click();
        });

//        step("Stop process", () -> {
//            stopProcessInTerminal(controller);
//        });
    }

    @Test
    @Disabled
    public void serverIssues_30_local() {
        //On Clicking on remote in Filter -> Sources -> Remote, you should see a pre-populated URL
        //Assumes unlogged plugin window is open

        step("Close Options menu if open", () -> {
            closeOptionsTabIfOpen(controller);
        });
        step("open Filters, switch to remote and assert JTextField contains default PrePopulated URL", () -> {
            openUnloggedToolbarIfNotOpen(controller, 1);

            ComponentFixture localMarker = controller.getIdeaFrame().getLocalModeHyperlink();
            localMarker.click();
            pause(ofMillis(250).toMillis());

            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            RemoteText prePopulatedUrl = textField.getData().getAll().get(0);

            Assertions.assertEquals(TestConstants.DEFAULT_PRE_POPULATED_URL, prePopulatedUrl.getText());
            controller.getIdeaFrame().getFilterCancel().click();
        });

        step("Stop process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    @Test
    @Disabled
    public void filterInteraction_sanity() {
        List<String> includedClasses = new ArrayList<>();
        includedClasses.add("org.unlogged.demo.jspdemo.wfm.SerializationUtils");
        List<String> includedMethods = new ArrayList<>();
        includedMethods.add("getObjectFor");
        FilterOptions filterOptions = new FilterOptions(includedClasses,
                new ArrayList<>(),
                includedMethods,
                new ArrayList<>(),
                true);
        setFilterOptionsForCurrentView(controller, filterOptions);
    }

    @Test
    @Disabled
    public void junit_icon_sanity() {
        List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
        List<DirectInvokeTreeLine> assertions = new ArrayList<>();
        inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
        assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
        assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
        DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                "public String getFutureResult(String s1)",
                inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                true);
        JunitGenerationRequest junitGenerationRequest = JunitGenerationRequest.fromDirectInvokeRequest(request,
                projectUnderTest, true, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));
        generateJunitTestCaseForMethod(controller, junitGenerationRequest);
    }

    @Test
    @Disabled
    public void junit_dummy_data_boiler_plate_sanity() {
        List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
        List<DirectInvokeTreeLine> assertions = new ArrayList<>();
        inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
        assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
        assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
        DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                "public String getFutureResult(String s1)",
                inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                true);
        JunitGenerationRequest junitGenerationRequest = JunitGenerationRequest.fromDirectInvokeRequest(request,
                projectUnderTest, false, JunitGenerationMethod.DUMMY_DATA,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));
        generateJunitTestCaseForMethod(controller, junitGenerationRequest);
    }

    @Test
    @Disabled
    public void junit_replay_data_sanity() {
        List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
        List<DirectInvokeTreeLine> assertions = new ArrayList<>();
        inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
        assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
        assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
        DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                "public String getFutureResult(String s1)",
                inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                true);
        JunitGenerationRequest junitGenerationRequest = JunitGenerationRequest.fromDirectInvokeRequest(request,
                projectUnderTest, true, JunitGenerationMethod.REPLAY_DATA,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));
        generateJunitTestCaseForMethod(controller, junitGenerationRequest);
    }

    //TODO: Needs improvements
    @Test
    @Disabled
    public void ide_errors_checkIDEFatalExceptions_sanity() {
        List<String> listIDE = listIDEFatalExceptions(controller);
        if (!listIDE.isEmpty()) {
            Assertions.fail("Assertion failure due to exceptions: " + listIDE);
        } else {
            Assertions.assertTrue(true);
        }
    }

    @Test
    @Disabled
    public void serverIssue_44() {
        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";

        //TODO: stop process if running already
        //TODO : revert changes if annotations are already present

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set Source to remote URL", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();

            pause(ofSeconds(10).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            pause(ofSeconds(5).toMillis());
        });

        step("Open main class and enusre InlayHint render behaviour is as expected", () -> {
            openFileIfNeeded(projectUnderTest.getLocalProjectInfo().getMainClassName(), controller);
            //look for inlayHints and assert that clicking on it will not hide it
            TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
            //Type something to reload inlayhints
            List<RemoteText> texts = textEditorFixture.getData().getAll();

            texts.get(0).click();
            controller.getKeyboard().enterText("//");
            controller.getKeyboard().hotKey(VK_BACK_SPACE, VK_BACK_SPACE);

            //reload text
            textEditorFixture = controller.getIdeaFrame().textEditor();
            texts = textEditorFixture.getData().getAll();
            RemoteText inlayHintText = texts.stream().filter(elem -> elem.getText().equals("1 call")).toList().get(0);

            inlayHintText.click();
            pause(ofMillis(250).toMillis());

            textEditorFixture = controller.getIdeaFrame().textEditor();
            texts = textEditorFixture.getData().getAll();

            try {
                inlayHintText = texts.stream().filter(elem -> elem.getText().equals("1 call")).toList().get(0);
            } catch (Exception e) {
                Assertions.fail("Did not find inlayHints for main method");
            }
        });
        //dont stop

//        step("Stop process", () -> {
//            stopProcessInTerminal(controller);
//        });
    }

    @Test
    @Disabled
    public void serverIssue_52() {
        //assume project  is running in remote mode
        List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
        List<DirectInvokeTreeLine> assertions = new ArrayList<>();
        inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
        assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
        assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
        DirectInvokeRequest request = new DirectInvokeRequest("FutureController.java",
                "public String getFutureResult(String s1)",
                inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                true);
        JunitGenerationRequest junitGenerationRequest = JunitGenerationRequest.fromDirectInvokeRequest(request,
                projectUnderTest, true, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));
        generateJunitTestCaseForMethod(controller, junitGenerationRequest);

        String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
        Assertions.assertEquals("TestFutureControllerV.java", currentFileName);
        //don't stop
    }


    //Does not require project to be running
    @Test
    @Disabled
    public void serverIssues_23() {
        int switchCount = 10;
        step("open filters tab", () -> {
            controller.getIdeaFrame().getFilterButton().click();

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            List<RemoteText> tabHeaders = titlePanel.getData().getAll();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            RemoteText classFilterTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Class Filters")).toList().get(0);
            for (int i = 0; i < switchCount; i++) {
                sourcesTabText.click();
                classFilterTabText.click();
            }
        });
        step("Close Filters", () -> {
            controller.getIdeaFrame().getFilterCancel().click();
        });
    }

    @Test
    @Disabled
    public void serverIssues_7() {
        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "/" + "\")";

        //TODO: stop process if running already
        //TODO : revert changes if annotations are already present

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set Source to remote URL", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL + "/");
            controller.getIdeaFrame().getListSessionsButton().click();

            pause(ofSeconds(10).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();
        });

        step("Verify that candidates are visible", () -> {
            pause(ofSeconds(10).toMillis());
            try {
                controller.getIdeaFrame().getFirstCheckbox();
            } catch (Exception e) {
                Assertions.fail("No Candidates found");
            }
        });

        step("Stop the process", () -> {
            stopProcessInTerminal(controller);
        });
    }


    //change count from this point
    @Test
    @Disabled
    public void gradle_project_run() {
        projectUnderTest = new GitProjectInfo("unlogged-spring-gradel-demo",
                "https://github.com/unloggedio/unlogged-spring-gradle-demo.git",
                "ui_test_clean", "build.gradle", LocalProjectInfo.BuildSystem.GRADLE, 65,
                true, "17", "src/test/java/org/unlogged/demo");
        LocalProjectInfo localProjectInfo = new LocalProjectInfo("unlogged-spring-maven-demo", "start_project.sh",
                "git_rollback.sh", "remove_local_sessions.sh", "Application.java", 60);
        projectUnderTest.setLocalProjectInfo(localProjectInfo);

        try {
            if (controller.getIdeaFrame() != null) {
                UiTestInteractionUtils.runIntelliJIdeaAction(controller, "Close Project", 20);
            }
        } catch (Exception e) {

        }

        step("Clone project - fresh state, change branch and setup sdk version", () -> {
            cloneAndOpenProject(controller, projectUnderTest);
            //setup sdk version and wait till index is complete, then start tests in normal flow
            setSdkVersion(controller, projectUnderTest);
            controller.waitForIndex();
        });

        step("Set gradle Options : ", () -> {
            setIntelliJAsGradleBuilder(controller, projectUnderTest);
            controller.waitForIndex();
        });

        //add assertions for exceptions popping up in notifications

        step("Revert all changes made to project, remove local sessions", () -> {
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRevertScriptName(), 5);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRemoveScriptName(), 2);
        });

        //add gradle dependencies
        step("Add Gradle dependencies", () -> {
            addUnloggedDependenciesToBuildFile(controller, projectUnderTest);
        });
    }

    @Test
    @Disabled
    public void serverIssue_14() {
        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, projectUnderTest.getLocalProjectInfo().getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getStartScriptName(), projectUnderTest.getLocalProjectInfo().getStartupWaitDuration());
        });

        step("Set Source to remote URL", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            clearGotIts(controller);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getRemoteButtonRadioLabel().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture textField = controller.getIdeaFrame().getFirstJTextField();
            textField.click();

            controller.getKeyboard().hotKey(VK_META, VK_A);
            controller.getKeyboard().hotKey(VK_DELETE);

            controller.getKeyboard().enterText(TestConstants.REMOTE_URL);
            controller.getIdeaFrame().getListSessionsButton().click();

            pause(ofSeconds(10).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            pause(ofSeconds(5).toMillis());

            //TODO: Add an assertion to number of candidates expected here or assert candidates
        });

        step("Try to generate dummy data boilerplate Junit code.", () -> {
            List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
            List<DirectInvokeTreeLine> assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
            DirectInvokeRequest request = new DirectInvokeRequest("ReactiveStudentService.java",
                    "public boolean updateStudent(Student student)",
                    inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            JunitGenerationRequest junitGenerationRequest = JunitGenerationRequest.fromDirectInvokeRequest(request,
                    projectUnderTest, false, JunitGenerationMethod.DUMMY_DATA,
                    JunitGenerationOptions.defaultOptions());
            junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.gradle.service.ReactiveStudentService"),
                    new ArrayList<>(), List.of("updateStudent"), new ArrayList<>(), true));
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
        });


        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    @Test
    @Disabled
    public void config_sanity_debug() {
        projectUnderTest = new GitProjectInfo("unlogged-spring-gradel-demo",
                "https://github.com/unloggedio/unlogged-spring-gradle-demo.git",
                "ui_test_clean", "build.gradle", LocalProjectInfo.BuildSystem.GRADLE, 65,
                true, "17", "src/test/java/org/unlogged/demo");
        LocalProjectInfo localProjectInfo = new LocalProjectInfo("unlogged-spring-maven-demo", "start_project.sh",
                "git_rollback.sh", "remove_local_sessions.sh", "UnloggedDemoApplication.java", 60);
        projectUnderTest.setLocalProjectInfo(localProjectInfo);


    }

//    private void assertNumberOfTestCases(JunitGenerationRequest request) {
//        openFileIfNeeded(request.getTestBasePath().substring(request.getTestBasePath().lastIndexOf("/") + 1), controller);
//        expandJavaFile(controller.getIdeaFrame().textEditor().getEditor());
//        clearGotIts(controller);
//        TextEditorFixture editor = controller.getIdeaFrame().textEditor(Duration.ofSeconds(2));
//        int numberOfTestsPresent = editor.getGutter().getIcons()
//                .stream()
//                .filter(icon -> icon.toString().contains("testState/run.svg"))
//                .toList().size();
//
//        Assert.assertEquals("Expected and actual number of test cases are", expectedNumberOfTestCasesForFutureController, numberOfTestsPresent);
//        expectedNumberOfTestCasesForFutureController += 1;
//        pause(ofSeconds(10).toMillis());
//    }
}
