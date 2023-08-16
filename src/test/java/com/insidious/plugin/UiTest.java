package com.insidious.plugin;

import com.insidious.plugin.pages.IdeaFrame;
import com.insidious.plugin.pages.WelcomeFrame;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;

//To run these :
//1. Run "runIdeForUiTests"
//2. Once an ide instace is created run the tests below.
public class UiTest {
    private RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
    private final Keyboard keyboard = new Keyboard(remoteRobot);
//
//    @BeforeEach
//    public void waitForIde(RemoteRobot remoteRobot) {
//        waitForIgnoringError(ofMinutes(3), () ->
//                remoteRobot.callJs("true"));
//    }

//    @AfterEach
//    public void closeProject(final RemoteRobot remoteRobot) {
//        step("Close the project", () -> {
//            if (remoteRobot.isMac()) {
//                keyboard.hotKey(VK_SHIFT, VK_META, VK_A);
//                keyboard.enterText("Close Project");
//                keyboard.enter();
//            }
//        });
//    }

    //To be run after clean, will fail in other cases
    @Test
    public void cleanStart() {
        final WelcomeFrame welcomeFrame = remoteRobot.find(WelcomeFrame.class, ofSeconds(10));
        welcomeFrame.getOpenProjectButton().click();

        welcomeFrame.getProjectSelectorComboBox().click();
        String projectName = "autoUItest";

        //note : place the project to test in your user directory : {username}/{project}.
        //This is to ensure that intelliJ's intrusive folder path update doesn't mess with the typing.
        keyboard.enterText("/" + projectName);

        welcomeFrame.getOpenConfirmButton().click();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

//        step("Open Unlogged and click DirectInvoke test invoke", () -> {
//            pause(ofSeconds(5).toMillis());
//            idea.getUnloggedToolbarComponent().click();
//            pause(ofSeconds(5).toMillis());
//            idea.getDirectInvokeTabHeader().click();
//            idea.getExecuteMethodButton().click();
//        });

        idea.getProjectNavItem().click();
        pause(ofSeconds(3).toMillis());
        final ContainerFixture projectView = idea.getProjectViewTree();
        if (!projectView.hasText("src")) {
            projectView.findText(idea.getProjectName()).doubleClick();
            waitFor(() -> projectView.hasText("src"));
        }

        step("Open TestController", () -> {
            projectView.findText("src").doubleClick();
            projectView.findText("main").doubleClick();
            projectView.findText("java").doubleClick();
            projectView.findText("TestController").doubleClick();
        });

//        idea.getDebugButton().click();
//        step("Check console output", () -> {
//            final Locator locator = byXpath("//div[@class='ConsoleViewImpl']");
//            waitFor(ofMinutes(1), () -> idea.findAll(ContainerFixture.class, locator).size() > 0);
//            waitFor(ofMinutes(1), () -> idea.find(ComponentFixture.class, locator)
//                    .hasText("Started AutoUItestApplication"));
//            System.out.println("Started application");
//        });
    }

    //to be run once a project is indexed, will open and execute
    @Test
    public void indexedStart() {
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

        String fileName = "TestController";

        final ContainerFixture projectView = idea.getProjectViewTree();
        projectView.findText(fileName).doubleClick();

        final TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        idea.getDebugButton().click();

//        step("Check console output", () -> {
//            final Locator locator = byXpath("//div[@class='ConsoleViewImpl']");
//            waitFor(ofMinutes(1), () -> idea.findAll(ContainerFixture.class, locator).size() > 0);
//            waitFor(ofMinutes(1), () -> idea.find(ComponentFixture.class, locator)
//                    .hasText("Started AutoUItestApplication"));
//            System.out.println("Started application");
//        });

        //wait till process starts - 20 seconds as default
        pause(ofSeconds(20).toMillis());
        List<GutterIcon> icons = editor.getGutter().getIcons();
        System.out.println("Icons : " + icons.toString());

        for (GutterIcon icon : icons) {
            if (icons.get(0).toString().contains("overriddenPath='/icons/svg/process_running.svg'")) {
                //process running icon found, click it.
                icon.click();
                pause(ofSeconds(5).toMillis());
                idea.getExecuteMethodButton().click();
            }
        }
    }
}
