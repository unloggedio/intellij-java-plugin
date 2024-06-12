
package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.UiTestInteractionUtils;
import com.insidious.plugin.UITests.wrapper.*;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.List;

import static com.insidious.plugin.UITests.Utils.UITestUtils.*;
import static com.insidious.plugin.UITests.Utils.UiTestInteractionUtils.*;
import static java.awt.event.KeyEvent.*;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UiTestsV3 {
    private RemoteRobotController controller;
    private final List<GitProjectInfo> projectsToTest = new ArrayList<>();

    public UiTestsV3() {
        RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
        Keyboard keyboard = new Keyboard(remoteRobot);
        controller = new RemoteRobotController(remoteRobot, keyboard);

        LocalProjectInfo mavenDemoLocal = new LocalProjectInfo("unlogged-spring-maven-demo", "start_project.sh",
                "git_rollback.sh", "remove_local_sessions.sh", "clear_tests.sh", "UnloggedDemoApplication.java", 30);

        GitProjectInfo mavenDemo = new GitProjectInfo("unlogged-spring-maven-demo",
                "https://github.com/unloggedio/unlogged-spring-maven-demo.git",
                "ui_test_clean", "pom.xml", LocalProjectInfo.BuildSystem.MAVEN, 30,
                true, "17", "src/test/java/org/unlogged/demo");
        mavenDemo.setLocalProjectInfo(mavenDemoLocal);

        projectsToTest.add(mavenDemo);

        GitProjectInfo gradleDemo = new GitProjectInfo("unlogged-spring-gradle-demo",
                "https://github.com/unloggedio/unlogged-spring-gradle-demo.git",
                "ui_test_clean", "build.gradle", LocalProjectInfo.BuildSystem.GRADLE, 30,
                true, "17", "src/test/java/org/unlogged/demo");
        LocalProjectInfo gradleDemoLocal = new LocalProjectInfo("unlogged-spring-gradle-demo", "start_project.sh",
                "git_rollback.sh", "remove_local_sessions.sh", "clear_tests.sh", "Application.java", 30);
        gradleDemo.setLocalProjectInfo(gradleDemoLocal);
        projectsToTest.add(gradleDemo);

        GitProjectInfo multimoduleDemo = new GitProjectInfo("multimodule-demo-1",
                "https://github.com/unloggedio/multimodule-demo-1.git",
                "ui_test_clean", "pom.xml", LocalProjectInfo.BuildSystem.MAVEN, 30,
                true, "11", "src/test/java/org/unlogged/demo");
        LocalProjectInfo multiModuleDemo = new LocalProjectInfo("multimodule-demo-1", "start_project.sh",
                "git_rollback.sh", "remove_local_sessions.sh", "clear_tests.sh", "CustomerApplication.java", 30);
        multimoduleDemo.setLocalProjectInfo(multiModuleDemo);
        multimoduleDemo.setLoginOptions(new GitLoginOptions("your Personal Access Token here")); //your git personal access token here
        projectsToTest.add(multimoduleDemo);
    }

    @Test
    @Order(1)
//    @Disabled
    public void cloneAndAddSDK() {
        int projectIndex = 0;
        step("Clone project - fresh state, change branch and setup sdk version", () -> {
            cloneAndOpenProject(controller, projectsToTest.get(projectIndex));
            //setup sdk version and wait till index is complete, then start tests in normal flow
            setSdkVersion(controller, projectsToTest.get(projectIndex));
            controller.waitForIndex();
        });

        step("Remove local sessions", () -> {
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getRemoveScriptName(), 2);
        });

        step("Add unlogged dependency (Mac)", () -> {
            addUnloggedDependenciesToBuildFile(controller, projectsToTest.get(projectIndex), true);
        });
    }

    //remote mode start - start of remote chain tests for maven - demo
    @Test
    @Order(2)
//    @Disabled
    public void remote_mode_general() {
        int projectIndex = 0;
        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";

        //TODO: stop process if running already
        //TODO : revert changes if annotations are already present

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getStartScriptName(), projectsToTest.get(projectIndex).getLocalProjectInfo().getStartupWaitDuration());
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

            pause(ofSeconds(5).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            pause(ofSeconds(5).toMillis());
            clearGotIts(controller);
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
    }

    //remote mode start - debug DirectInvoke for this method
    @Test
    @Order(3)
    @Disabled
    public void serverIssue_14() {
        int projectIndex = 0;
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
                    projectsToTest.get(projectIndex), false, JunitGenerationMethod.DUMMY_DATA,
                    JunitGenerationOptions.defaultOptions());
            junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.gradle.service.ReactiveStudentService"),
                    new ArrayList<>(), List.of("updateStudent"), new ArrayList<>(), true));
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
        });
    }

    @Test
    @Order(4)
//    @Disabled
    public void serverIssue_44() {
        int projectIndex = 0;
        step("Open main class and ensure InlayHint render behaviour is as expected", () -> {
            openFileIfNeeded(projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), controller);
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
    }

    @Test
    @Order(5)
//    @Disabled
    public void serverIssues_7() {
        step("Close method options menu if open", () -> {
            closeOptionsTabIfOpen(controller);
        });
        step("Verify that candidates are visible", () -> {
            pause(ofSeconds(10).toMillis());
            try {
                controller.getIdeaFrame().getFirstCheckbox();
            } catch (Exception e) {
                Assertions.fail("No Candidates found");
            }
        });
    }

    @Test
    @Order(6)
//    @Disabled
    public void junitRemoteModeGeneration_sanity_remote() {
        int projectIndex = 0;
        step("Clear filters", () -> {
            clearStompFilter(controller);
        });

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
                projectsToTest.get(projectIndex), false, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));

        step("Generate Junit from Icon", () -> {
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(1, count);
        });

        step("Generate Junit from Dummy data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.DUMMY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(2, count);
        });

        step("Generate Junit from Replay data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.REPLAY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(3, count);
        });
    }

    @Test
    @Order(7)
//    @Disabled
    public void replayCaseSave_sanity_remote() {
        step("Open toolbar if not already open", () -> {
            closeOptionsTabIfOpen(controller);
            openUnloggedToolbarIfNotOpen(controller, 2);
        });
        step("Set Filter to FutureController and save it's candidates as replay cases", () -> {
            FilterOptions futureControllerOptions = new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                    new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true);
            setFilterOptionsForCurrentView(controller, futureControllerOptions);
            selectAllAndSave(controller, 10);
        });
    }

    @Test
    @Order(8)
//    @Disabled
    public void serverIssue_52() {
        int projectIndex = 0;
        step("Generate a new Junit test case for a particular method", () -> {
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
                    projectsToTest.get(projectIndex), false, JunitGenerationMethod.JUNIT_ICON,
                    JunitGenerationOptions.defaultOptions());
            junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                    new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);

            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);
        });
    }

    //remote mode - ending case
    @Test
    @Order(9)
//    @Disabled
    public void serverIssue_51() {
        step("Clear notifications", () -> {
            controller.getIdeaFrame().getNotificationTab().click();
            try {
                controller.getIdeaFrame().getNotificationsClearAll().click();
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
            controller.getIdeaFrame().getNotificationsClearAll().click();
            Assertions.fail("Got a notification when not expected");
        } catch (Exception e) {
            //pass
        }

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    //------------------------------
    //local mode start and sanity
    @Test
    @Order(10)
//    @Disabled
    public void run_mode_local_general() {
        int projectIndex = 0;
        final String annotationText = "@Unlogged";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), controller);
            addUnloggedToStartFile(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getClearTestsScriptName(), 2);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getStartScriptName(), projectsToTest.get(projectIndex).getLocalProjectInfo().getStartupWaitDuration());
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
            pause(ofSeconds(3).toMillis());
            controller.getIdeaFrame().getToolBarDeleteButton().click();
            pause(ofSeconds(3).toMillis());
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
    }

    @Test
    @Order(11)
//    @Disabled
    public void junitLocalModeGeneration_sanity_local() {
        int projectIndex = 0;
        step("Clear filters", () -> {
            clearStompFilter(controller);
        });

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
                projectsToTest.get(projectIndex), false, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));

        step("Generate Junit from Icon", () -> {
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(1, count);
        });

        step("Generate Junit from Dummy data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.DUMMY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(2, count);
        });

        step("Generate Junit from Replay data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.REPLAY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(3, count);
        });
    }

    @Test
    @Order(12)
//    @Disabled
    public void replayCaseSave_sanity_local() {
        step("Clear filters and selections before save", () -> {
            clearStompFilter(controller);

        });
        step("Set Filter to FutureController and save it's candidates as replay cases", () -> {
            FilterOptions futureControllerOptions = new FilterOptions(List.of("org.unlogged.demo.controller.FutureController"),
                    new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true);
            setFilterOptionsForCurrentView(controller, futureControllerOptions);
            selectAllAndSave(controller, 10);
        });
    }

    //Server Issues Sheet - Issue 73
    @Test
    @Order(13)
//    @Disabled
    public void serverIssues_73() {
        int projectIndex = 0;
        step("Close Options menu if open", () -> {
            closeOptionsTabIfOpen(controller);
        });

        step("Open main class and enusre InlayHint render behaviour is as expected", () -> {
            openFileIfNeeded(projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), controller);
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
    }

    @Test
    @Order(14)
//    @Disabled
    public void serverIssues_72() {
        //project is already up and running in local mode
        step("Save Candidates from one of FutureController's methods", () -> {

            //clear notifications before generating tests
            controller.getIdeaFrame().getNotificationTab().click();
            //click on clear all if it exists
            try {
                controller.getIdeaFrame().getNotificationsClearAll().click();
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
    @Order(15)
//    @Disabled
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
    }

    @Test
    @Order(16)
//    @Disabled
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
    }

    //start in local mode - ending case
    @Test
    @Order(17)
//    @Disabled
    public void serverIssue_46() {
        //set filter to remote mode
        //don't select a session from remote, try to click on apply
        //assert that the filter tab is still open

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

    //start in local mode
    //an ending case
    @Test
    @Order(18)
//    @Disabled
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
            clearStompFilter(controller);
            pause(ofMillis(250).toMillis());
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
            pause(ofSeconds(3).toMillis());

            //2 candidates are supposed to be generated for a successful direct Invoke form the above method
            Assertions.assertEquals(2, controller.getIdeaFrame().getAllVisibleCheckBoxes().size());
        });

        step("Close options menu if open", () -> {
            closeOptionsTabIfOpen(controller);
        });
    }

    //start in local mode
    //an ending case
    @Test
    @Order(19)
//    @Disabled
    public void serverIssue_37() {
        //set filter to remote mode
        //don't select a session from remote, try to click on apply
        //asser that the filter tab is still open

        step("Clear all previous notifications if they are present", () -> {
            controller.getIdeaFrame().getNotificationTab().click();
            try {
                controller.getIdeaFrame().getNotificationsClearAll().click();
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

            controller.getIdeaFrame().getFilterCancel().click();
        });

        //Assert that nothing shows up in notifications
        step("Assert that there are no notifications", () -> {
            controller.getIdeaFrame().getNotificationTab().click();
            try {
                controller.getIdeaFrame().getNotificationsClearAll().click();
                Assertions.fail("Got a notification when not expected");
            } catch (Exception e) {
                //pass
            }
        });

        step("stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    //doesn't need project to start
    @Test
    @Order(20)
//    @Disabled
    public void serverIssues_23() {
        int switchCount = 10;
        step("open filters tab", () -> {
            openUnloggedToolbarIfNotOpen(controller, 1);
            controller.getIdeaFrame().getFilterButton().click();

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
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

    //----------------------------
    //switch to gradle project
    @Test
    @Order(21)
    @Disabled
    public void gradle_project_onboarding() {
        int projectIndex = 1;
        try {
            if (controller.getIdeaFrame() != null) {
                UiTestInteractionUtils.runIntelliJIdeaAction(controller, "Close Project", 7);
            }
        } catch (Exception e) {
            openFileIfNeeded("README.md", controller);
            pause(ofMillis(500).toMillis());
            UiTestInteractionUtils.runIntelliJIdeaAction(controller, "Close Project", 7);
        }

        step("Clone project - fresh state, change branch and setup sdk version", () -> {
            controller.unsertIdeaFrame();
            cloneAndOpenProject(controller, projectsToTest.get(projectIndex));
            //setup sdk version and wait till index is complete, then start tests in normal flow
            setSdkVersion(controller, projectsToTest.get(projectIndex));
            pause(ofSeconds(20).toMillis());
        });

        System.out.println("Setting sdk version");
        step("Set gradle Options : ", () -> {
            setIntelliJAsGradleBuilder(controller, projectsToTest.get(projectIndex));
            pause(ofSeconds(20).toMillis());
        });

        //add assertions for exceptions popping up in notifications

        step("Revert all changes made to project, remove local sessions", () -> {
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getRevertScriptName(), 5);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getRemoveScriptName(), 2);
        });

        //add gradle dependencies
        step("Add Gradle dependencies", () -> {
            addUnloggedDependenciesToBuildFile(controller, projectsToTest.get(projectIndex), true);
        });
    }

    @Test
    @Order(22)
    @Disabled
    public void remote_mode_general_gradle() {
        int projectIndex = 1;
        final String annotationText = "@Unlogged(serverEndpoint = \"" + TestConstants.REMOTE_URL + "\")";
        System.out.println("Main class name = " + projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName());
        step("Add annotation and start project", () -> {
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getClearTestsScriptName(), 2);
            addUnloggedToStartFile(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), annotationText, true);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getStartScriptName(), projectsToTest.get(projectIndex).getLocalProjectInfo().getStartupWaitDuration());
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

            pause(ofSeconds(5).toMillis());

            controller.getIdeaFrame().getAllVisibleRadioButtons().get(2).click();
            controller.getIdeaFrame().getApplyButtonGeneric().click();

            pause(ofSeconds(5).toMillis());
        });

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
    }

    @Test
    @Order(23)
    @Disabled
    public void junitRemoteModeGeneration_sanity_remote_gradle() {
        int projectIndex = 1;
        step("Clear filters", () -> {
            clearStompFilter(controller);
        });

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
                projectsToTest.get(projectIndex), false, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.gradle.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));

        step("Generate Junit from Icon", () -> {
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(1, count);
        });

        step("Generate Junit from Dummy data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.DUMMY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(2, count);
        });

        step("Close options before next step", () -> {
            closeOptionsTabIfOpen(controller);
        });

        step("Generate Junit from Replay data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.REPLAY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(3, count);
        });

        step("Close options before next test", () -> {
            closeOptionsTabIfOpen(controller);
        });
    }

    @Test
    @Order(24)
    @Disabled
    public void replayCaseSave_sanity_remote_gradle() {
        step("Set Filter to FutureController and save it's candidates as replay cases", () -> {
            FilterOptions futureControllerOptions = new FilterOptions(List.of("org.unlogged.demo.gradle.controller.FutureController"),
                    new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true);
            setFilterOptionsForCurrentView(controller, futureControllerOptions);

            selectAllAndSave(controller, 10);
        });

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    @Test
    @Order(25)
    @Disabled
    public void run_mode_local_general_gradle() {
        int projectIndex = 1;
        final String annotationText = "@Unlogged";

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), controller);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getClearTestsScriptName(), 2);
            addUnloggedToStartFile(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getMainClassName(), annotationText, false);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getStartScriptName(), projectsToTest.get(projectIndex).getLocalProjectInfo().getStartupWaitDuration());
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
            pause(ofSeconds(3).toMillis());
            controller.getIdeaFrame().getToolBarDeleteButton().click();
            pause(ofSeconds(3).toMillis());
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
    }

    @Test
    @Order(26)
    @Disabled
    public void junitLocalModeGeneration_sanity_local_gradle() {
        int projectIndex = 1;
        step("Clear filters", () -> {
            clearStompFilter(controller);
        });

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
                projectsToTest.get(projectIndex), false, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("org.unlogged.demo.gradle.controller.FutureController"),
                new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true));

        step("Generate Junit from Icon", () -> {
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(1, count);
        });

        step("Generate Junit from Dummy data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.DUMMY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(2, count);
        });

        step("Generate Junit from Replay data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.REPLAY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestFutureControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(3, count);
        });
    }

    @Test
    @Order(27)
    @Disabled
    public void replayCaseSave_sanity_local_gradle() {
        step("Set Filter to FutureController and save it's candidates as replay cases", () -> {
            FilterOptions futureControllerOptions = new FilterOptions(List.of("org.unlogged.demo.gradle.controller.FutureController"),
                    new ArrayList<>(), List.of("getFutureResult"), new ArrayList<>(), true);
            setFilterOptionsForCurrentView(controller, futureControllerOptions);
            selectAllAndSave(controller, 10);
        });

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    @Test
    @Order(28)
    @Disabled
    public void close_LastProject() {
        try {
            if (controller.getIdeaFrame() != null) {
                UiTestInteractionUtils.runIntelliJIdeaAction(controller, "Close Project", 7);
            }
        } catch (Exception e) {
            openFileIfNeeded("README.md", controller);
            pause(ofMillis(500).toMillis());
            UiTestInteractionUtils.runIntelliJIdeaAction(controller, "Close Project", 7);
        }
    }

    //Add multimodule cases from multi-module-demo1
    //----------------
    @Test
    @Order(29)
    @Disabled
    public void onboarding_multimodule() {
        int projectIndex = 2;

        //enter token text ->
        step("Clone multimodule project and switch branch", () -> {
            cloneAndOpenProject(controller, projectsToTest.get(2));
            pause(ofSeconds(5).toMillis());
            setSdkVersion(controller, projectsToTest.get(projectIndex));
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getRevertScriptName(), 3);
        });

        step("Open Pom and add dependency", () -> {
            ContainerFixture projectViewTree = controller.getIdeaFrame().getProjectViewTree();
            RemoteText firstPom = projectViewTree.getData().getAll().stream().filter(text -> text.getText().equals("pom.xml")).toList().get(0);
            firstPom.doubleClick();

            addUnloggedDependenciesToBuildFile(controller, projectsToTest.get(projectIndex), false);
        });
    }

    @Test
    @Order(30)
    @Disabled
    public void multimodule_local_sanity_multimodule() {
        int projectIndex = 2;
        //don't add annotations
        //only base pom has unlogged sdk at this stage, assert the number of candidates generated for method

        step("Start project", () -> {
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getRemoveScriptName(), 3);
            executeShellScriptAndWait(controller, projectsToTest.get(projectIndex).getLocalProjectInfo().getStartScriptName(), projectsToTest.get(projectIndex).getStartupWaitDuration());
        });
        step("Clear candidates and DirectInvoke controller method", () -> {
            openUnloggedToolbarIfNotOpen(controller, 2);
            controller.getIdeaFrame().getToolBarDeleteButton().click();
            pause(ofMillis(500).toMillis());

            List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
            List<DirectInvokeTreeLine> assertions = new ArrayList<>();
            inputLines.add(new DirectInvokeTreeLine(1, "1"));
            assertions.add(new DirectInvokeTreeLine(1, "com.purnima.jain.customer.domain.aggregate.Customer"));
            assertions.add(new DirectInvokeTreeLine(2, "customerId: 131"));
            assertions.add(new DirectInvokeTreeLine(3, "customerName: 1321"));
            DirectInvokeRequest request = new DirectInvokeRequest("CustomerController.java",
                    "public Customer getCustomer(@PathVariable Integer customerId)",
                    inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                    true);
            UiTestInteractionUtils.directInvokeAndAssertResponse(request, controller);
        });

        step("Assert number of candidates generated as 7", () -> {
            controller.getIdeaFrame().getSelectAllicon().click();
            pause(ofMillis(250).toMillis());
            try {
                ComponentFixture numberOfCandidates = controller.getIdeaFrame().getComponentByXpath("//div[@text='7 selected']");
            } catch (Exception e) {
                Assertions.fail("Either no candidates were generated or wrong number of candidates generated");
            } finally {
                try {
                    controller.getIdeaFrame().getClearSelectionHyperlink().click();
                } catch (Exception ex) {
                }
            }
        });

        step("Save replay cases", () -> {
            controller.getIdeaFrame().getSelectAllicon().click();
            pause(ofSeconds(1).toMillis());

            controller.getIdeaFrame().getSaveGlobalButton().click();
            pause(ofSeconds(10).toMillis());

            controller.getIdeaFrame().getSaveFromConfirmButton().click();
            pause(ofSeconds(10).toMillis());
        });

        step("Open Library and confirm the number of candidates is 7", () -> {
            controller.getIdeaFrame().getlibraryTabHeader().click();
            pause(ofMillis(500).toMillis());

            controller.getIdeaFrame().getSelectAllicon().click();
            pause(ofMillis(250).toMillis());

            try {
                controller.getIdeaFrame().getComponentByXpath("//div[@text='7 selected']");
            } catch (Exception e) {
                Assertions.fail("Wrong number of candidates");
            }
        });
    }

    @Test
    @Order(31)
    @Disabled
    public void junitLocalModeGeneration_sanity_local_multimodule() {
        int projectIndex = 2;
        step("Clear filters", () -> {
            clearStompFilter(controller);
        });

        List<DirectInvokeTreeLine> inputLines = new ArrayList<>();
        List<DirectInvokeTreeLine> assertions = new ArrayList<>();
        inputLines.add(new DirectInvokeTreeLine(1, "1"));
        assertions.add(new DirectInvokeTreeLine(1, "com.purnima.jain.customer.domain.aggregate.Customer"));
        assertions.add(new DirectInvokeTreeLine(2, "customerId: 131"));
        assertions.add(new DirectInvokeTreeLine(3, "customerName: 1321"));
        DirectInvokeRequest request = new DirectInvokeRequest("CustomerController.java",
                "public Customer getCustomer(@PathVariable Integer customerId)",
                inputLines, assertions, Arrays.asList(AssertionOptions.DIRECT_INVOKE_RESPONSE),
                true);
        JunitGenerationRequest junitGenerationRequest = JunitGenerationRequest.fromDirectInvokeRequest(request,
                projectsToTest.get(projectIndex), true, JunitGenerationMethod.JUNIT_ICON,
                JunitGenerationOptions.defaultOptions());
        junitGenerationRequest.setFilterOptions(new FilterOptions(List.of("com.purnima.jain.customer.controller.CustomerController"),
                new ArrayList<>(), List.of("getCustomer"), new ArrayList<>(), true));

        step("Generate Junit from Icon", () -> {
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestCustomerControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(1, count);
        });

        step("Generate Junit from Dummy data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.DUMMY_DATA);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestCustomerControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(2, count);
        });

        step("Generate Junit from Replay data option", () -> {
            junitGenerationRequest.setJunitGenerationMethod(JunitGenerationMethod.REPLAY_DATA);
            junitGenerationRequest.setExecuteOnDemand(false);
            generateJunitTestCaseForMethod(controller, junitGenerationRequest);
            String currentFileName = controller.getIdeaFrame().textEditor().getEditor().getFileName();
            //assert test case file is created
            Assertions.assertEquals("TestCustomerControllerV.java", currentFileName);

            Long count = getNumberofTestsInCurrentFile(controller);
            Assertions.assertEquals(3, count);
        });
    }

//    @Test
//    @Disabled
//    @Order(25)
//    public void ide_errors_checkIDEFatalExceptions_sanity() {
//        List<String> listIDE = listIDEFatalExceptions(controller);
//        if (!listIDE.isEmpty()) {
//            Assertions.fail("Assertion failure due to exceptions: " + listIDE);
//        } else {
//            Assertions.assertTrue(true);
//        }
//    }

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

    //    @Test
//    @Order(1)
//    @Disabled
//    public void openProjectAndAddSDK() {
//
//        step("Open Project", () -> {
//            final WelcomeFrame welcomeFrame = controller.getRemoteRobot().find(WelcomeFrame.class, ofSeconds(10));
//            welcomeFrame.getOpenProjectButton().click();
//            welcomeFrame.getProjectSelectorComboBox().click();
//
//            controller.getKeyboard().enterText("/" + projectUnderTest.getLocalProjectInfo().getProjectPath());
//            pause(ofSeconds(1).toMillis());
//            welcomeFrame.getOpenConfirmButton().click();
//        });
//
//        step("Load idea frame and wait till IDE is in Smart mode", () -> {
//            controller.getIdeaFrame();
//        });
//
//        step("Revert all changes made to project, remove local sessions", () -> {
//            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRevertScriptName(), 5);
//            executeShellScriptAndWait(controller, projectUnderTest.getLocalProjectInfo().getRemoveScriptName(), 2);
//        });
//
//        step("Add unlogged dependency (Mac)", () -> {
//            openFileIfNeeded("pom.xml", controller);
//            ComponentFixture unloggedToolbar = controller.getIdeaFrame().getUnloggedToolbarComponent();
//            unloggedToolbar.moveMouse();
//            unloggedToolbar.click();
//            pause(ofSeconds(1).toMillis());
//
//            ComponentFixture copyButton = controller.getIdeaFrame().findCopyButton();
//            copyButton.moveMouse();
//            copyButton.click();
//
//            //paste right after dependencies
//            TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
//            List<RemoteText> pomContents = textEditorFixture.getEditor().getData().getAll();
//            RemoteText dependencyText = pomContents.stream().filter(remoteText -> remoteText.getText().equals("dependencies")).toList().get(0);
//            int indexOfDependencies = pomContents.indexOf(dependencyText);
//            RemoteText closingTag = pomContents.get(indexOfDependencies + 1);
//            closingTag.click();
//            controller.getKeyboard().hotKey(VK_RIGHT);
//            controller.getKeyboard().hotKey(VK_ENTER);
//            controller.getKeyboard().hotKey(VK_META, VK_V);
//
//            performMavenSync(controller);
//            controller.waitForIndex();
//        });
//    }
}
