
package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.UiTestInteractionUtils;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.insidious.plugin.UITests.wrapper.*;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.List;

import static com.insidious.plugin.UITests.Utils.UiTestInteractionUtils.*;
import static java.awt.event.KeyEvent.*;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UiTestsV3 {
    private RemoteRobotController controller;
    private ProjectInfo projectInfo;

    public UiTestsV3() {
        RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
        Keyboard keyboard = new Keyboard(remoteRobot);
        controller = new RemoteRobotController(remoteRobot, keyboard);

        projectInfo = new ProjectInfo("unlogged-spring-maven-demo",
                "unlogged-spring-maven-demo",
                "pom.xml",
                ProjectInfo.BuildSystem.MAVEN,
                "UnloggedDemoApplication"
        );
        projectInfo.setStartScriptName("start_project.sh");
        projectInfo.setRemoveScriptName("remove_local_sessions.sh");
        projectInfo.setRevertScriptName("git_rollback.sh");
        projectInfo.setStartupWaitDuration(60);
    }

    @Test
    @Order(1)
//    @Disabled
    public void openProjectAndAddSDK() {

        step("Open Project", () -> {
            final WelcomeFrame welcomeFrame = controller.getRemoteRobot().find(WelcomeFrame.class, ofSeconds(10));
            welcomeFrame.getOpenProjectButton().click();
            welcomeFrame.getProjectSelectorComboBox().click();

            controller.getKeyboard().enterText("/" + projectInfo.getProjectPath());
            pause(ofSeconds(1).toMillis());
            welcomeFrame.getOpenConfirmButton().click();
        });

        step("Load idea frame and wait till IDE is in Smart mode", () -> {
            controller.getIdeaFrame();
        });

        step("Revert all changes made to project, remove local sessions", () -> {
            executeShellScriptAndWait(controller, projectInfo.getRevertScriptName(), 5);
            executeShellScriptAndWait(controller, projectInfo.getRemoveScriptName(), 2);
        });

        step("Add unlogged dependency (Mac)", () -> {
            openFile("pom.xml", controller);
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
    @Disabled
    @Order(2)
    public void runRemoteModeTest() {

        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";

        //TODO: stop process if running already
        //TODO : revert changes if annotations are already present

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, projectInfo.getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectInfo.getStartScriptName(), projectInfo.getStartupWaitDuration());
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
            controller.getIdeaFrame().getFilterApplyButton().click();

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
            DirectInvokeRequest request = new DirectInvokeRequest("FutureController",
                    "public String getFutureResult(String s1)",
                    inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);

            inputLines = new ArrayList<>();
            assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "OptionalTest"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: OptionalTest"));
            request = new DirectInvokeRequest("FutureController",
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
//    @Disabled
    @Order(3)
    public void runLocalMode() {

        final String annotationText = "@Unlogged";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectInfo.getMainClassName(), controller);
            addUnloggedToStartFile(controller, projectInfo.getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectInfo.getStartScriptName(), projectInfo.getStartupWaitDuration());
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
            controller.getIdeaFrame().getFilterApplyButton().click();
        });

        step("DirectInvoke and assert results", () -> {
            List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
            List<DirectInvokeTreeLine> assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "Amg"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: yolo"));
            DirectInvokeRequest request = new DirectInvokeRequest("FutureController",
                    "public String getFutureResult(String s1)",
                    inputLines, assertions, List.of(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);

            inputLines = new ArrayList<>();
            assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "OptionalTest"));
            assertions.add(new DirectInvokeTreeLine(1, "java.lang.String"));
            assertions.add(new DirectInvokeTreeLine(2, "String: OptionalTest"));
            request = new DirectInvokeRequest("FutureController",
                    "public String getFutureResultOptional(String s1)",
                    inputLines, assertions, List.of(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);
        });

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    //Server Issues Sheet - Issue 73
    //Also is the start of local test chain
    @Test
    @Order(4)
//    @Disabled
    public void serverIssues_73() {
        //start project in local mode
        //after main method candidate Inlayhint click, inlayhints should not disappear

        final String annotationText = "@Unlogged";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectInfo.getMainClassName(), controller);
            addUnloggedToStartFile(controller, projectInfo.getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectInfo.getStartScriptName(), projectInfo.getStartupWaitDuration());
        });

        step("Set source filter to Localhost", () -> {
            openUnloggedToolbarIfNotOpen(controller, 1);

            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getLocalHostRadioButton().click();
            controller.getIdeaFrame().getFilterApplyButton().click();
        });

        step("Open main class and enusre InlayHint render behaviour is as expected", () -> {
            openFile(projectInfo.getMainClassName(), controller);
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

        step("Stop process", () -> {
            stopProcessInTerminal(controller);
        });
    }


    @Test
    @Order(5)
//    @Disabled
    public void serverIssues_20_local() {
        //Ensure that the hyperlink text "Local" is visible in Plugin and you open filters when you open it.
        //unlogged toolbar assumed to be open before this.
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
    @Order(6)
//    @Disabled
    public void serverIssues_30_local() {
        //On Clicking on remote in Filter -> Sources -> Remote, you should see a pre-populated URL
        //Assumes unlogged plugin window is open
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
}
