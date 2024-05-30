
package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.UiTestInteractionUtils;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.insidious.plugin.UITests.wrapper.RemoteRobotController;
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

    public UiTestsV3() {
        RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
        Keyboard keyboard = new Keyboard(remoteRobot);
        controller = new RemoteRobotController(remoteRobot, keyboard);
    }

    @Test
    @Order(1)
    public void openProjectAndAddSDK() {

        final String projectPath = "unlogged-spring-maven-demo";
        step("Open Project", () -> {
            final WelcomeFrame welcomeFrame = controller.getRemoteRobot().find(WelcomeFrame.class, ofSeconds(10));
            welcomeFrame.getOpenProjectButton().click();
            welcomeFrame.getProjectSelectorComboBox().click();

            controller.getKeyboard().enterText("/" + projectPath);
            pause(ofSeconds(1).toMillis());
            welcomeFrame.getOpenConfirmButton().click();
        });

        step("Load idea frame and wait till IDE is in Smart mode", () -> {
            controller.getIdeaFrame();
        });

        step("Revert all changes made to project, remove local sessions", () -> {
            executeShellScriptAndWait(controller, "git_rollback.sh", 5);
            executeShellScriptAndWait(controller, "remove_local_sessions.sh", 2);
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
    @Order(2)
    public void runRemoteModeTest() {

        final String mainClassname = "UnloggedDemoApplication";
        final String remoteURL = "http://18.188.85.130:8123";
        final String annotationText = "@Unlogged(serverEndpoint = \"" + remoteURL + "\")";
        final String startupScriptName = "start_project.sh";
        final int startUpWaitDuration = 60;

        //TODO: stop process if running already
        //TODO : revert changes if annotations are already present

        step("Add annotation and start project", () -> {
            addUnloggedToStartFile(controller, mainClassname, annotationText);
            executeShellScriptAndWait(controller, startupScriptName, startUpWaitDuration);
        });

        step("Set Source to remote URL", () -> {
            controller.getIdeaFrame().getUnloggedToolbarComponent().click();
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

            controller.getKeyboard().enterText(remoteURL);
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
            openFile("FutureController.java", controller);
            pause(ofSeconds(2).toMillis());
            expandJavaFile(controller.getIdeaFrame().textEditor().getEditor());
            clearGotIts(controller);

            TextEditorFixture editor = controller.getIdeaFrame().textEditor(Duration.ofSeconds(2));
            List<GutterIcon> gutterIcons = editor.getGutter().getIcons().stream()
                    .filter(icon -> icon.toString().contains("profileBlue.svg"))
                    .toList();

            List<Integer> lineNumbers = new ArrayList<>(gutterIcons.stream().map(GutterIcon::getLineNumber).toList());
            Collections.sort(lineNumbers);

            controller.getIdeaFrame().getToolBarDeleteButton().click();
            gutterIcons.forEach(icon -> {
                UiTestInteractionUtils.scrollToIcon(editor, icon);
                icon.click();
                pause(ofMillis(250).toMillis());

                String responseExpected = "String: string";
                controller.getIdeaFrame().getGoToDirectInvokeButton().click();
                pause(ofMillis(250).toMillis());

                if (icon.getLineNumber() == lineNumbers.get(0)) {
                    responseExpected = "String: yolo";
                } else {
                    responseExpected = "String: method2";
                    ComponentFixture argumentsTree = controller.getIdeaFrame().getTree();
                    List<RemoteText> remoteTexts = argumentsTree.getData().getAll();
                    remoteTexts.get(remoteTexts.size() - 1).click();

                    controller.getKeyboard().enterText("method2");
                    controller.getKeyboard().hotKey(VK_ENTER);
                }
                controller.getIdeaFrame().getDirectInvokeExecuteButtonNew().click();
                pause(ofSeconds(3).toMillis());

                ComponentFixture responseTree = controller.getIdeaFrame().getTree();
                List<RemoteText> remoteTexts = responseTree.getData().getAll();
                RemoteText responseValue = remoteTexts.get(remoteTexts.size() - 1);

                Assertions.assertEquals(responseExpected, responseValue.getText());
            });
            pause(ofSeconds(10).toMillis());
            ComponentFixture terminalToolbar = controller.getIdeaFrame().getTerminalToolBarSelectable();
            terminalToolbar.click();
            ComponentFixture terminalContents = controller.getIdeaFrame().getTerminalPanel();
            RemoteText lastText = terminalContents.getData().getAll().get(terminalContents.getData().getAll().size() - 1);
            lastText.click();
        });

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }

    @Test
    @Order(3)
    public void runLocalMode() {

        final String mainClassname = "UnloggedDemoApplication";
        final String annotationText = "@Unlogged";
        final String startupScriptName = "start_project.sh";
        final int startUpWaitDuration = 60;

        step("Add annotation and start project", () -> {
            UiTestInteractionUtils.openAndRevertGitChangesForFile(mainClassname, controller);
            addUnloggedToStartFile(controller, mainClassname, annotationText);
            executeShellScriptAndWait(controller, startupScriptName, startUpWaitDuration);
        });

        step("Set source filter to Localhost", () -> {
            controller.getIdeaFrame().getFilterButton().click();
            pause(ofMillis(250).toMillis());

            ComponentFixture titlePanel = controller.getIdeaFrame().getMyContentPanel();
            RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
            sourcesTabText.click();
            controller.getIdeaFrame().getLocalHostRadioButton().click();
            controller.getIdeaFrame().getFilterApplyButton().click();
        });

        step("DirectInvoke and assert results", () -> {
            openFile("FutureController.java", controller);
            pause(ofSeconds(2).toMillis());
            expandJavaFile(controller.getIdeaFrame().textEditor().getEditor());
            clearGotIts(controller);

            TextEditorFixture editor = controller.getIdeaFrame().textEditor(Duration.ofSeconds(2));
            List<GutterIcon> gutterIcons = editor.getGutter().getIcons().stream()
                    .filter(icon -> icon.toString().contains("profileBlue.svg"))
                    .toList();

            List<Integer> lineNumbers = new ArrayList<>(gutterIcons.stream().map(GutterIcon::getLineNumber).toList());
            Collections.sort(lineNumbers);

            controller.getIdeaFrame().getToolBarDeleteButton().click();
            gutterIcons.forEach(icon -> {
                scrollToIcon(editor, icon);
                icon.click();
                pause(ofMillis(250).toMillis());

                String responseExpected = "String: string";
                controller.getIdeaFrame().getGoToDirectInvokeButton().click();
                pause(ofMillis(250).toMillis());

                if (icon.getLineNumber() == lineNumbers.get(0)) {
                    responseExpected = "String: yolo";
                } else {
                    responseExpected = "String: method2";
                    ComponentFixture argumentsTree = controller.getIdeaFrame().getTree();
                    List<RemoteText> remoteTexts = argumentsTree.getData().getAll();
                    remoteTexts.get(remoteTexts.size() - 1).click();

                    controller.getKeyboard().enterText("method2");
                    controller.getKeyboard().hotKey(VK_ENTER);
                }
                controller.getIdeaFrame().getDirectInvokeExecuteButtonNew().click();
                pause(ofSeconds(3).toMillis());

                ComponentFixture responseTree = controller.getIdeaFrame().getTree();
                List<RemoteText> remoteTexts = responseTree.getData().getAll();
                RemoteText responseValue = remoteTexts.get(remoteTexts.size() - 1);

                Assertions.assertEquals(responseExpected, responseValue.getText());
            });
        });

        step("Stop running process", () -> {
            stopProcessInTerminal(controller);
        });
    }
}
