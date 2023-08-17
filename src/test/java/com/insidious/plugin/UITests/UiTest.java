package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
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
            if (icon.toString().contains("overriddenPath='/icons/svg/process_running.svg'")) {
                //process running icon found, click it.
                icon.click();
                pause(ofSeconds(5).toMillis());
                idea.getExecuteMethodButton().click();
            }
        }
    }

    @Test
    public void executeAllMethodsInProject() {
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

        idea.getDebugButton().click();
        pause(ofSeconds(10).toMillis());

        idea.getHideDebugToolBarIcon().click();
        pause(ofSeconds(1).toMillis());

        idea.getExpandAllButton().click();
        pause(ofSeconds(3).toMillis());

        final ContainerFixture projectView = idea.getProjectViewTree();
        String currentFile = "";
        try {
            TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
            currentFile = editor.getEditor().getFileName();
        } catch (Exception e) {
            //noting to do
            //this is for when no files are open in editor/no editor
        }

        List<RemoteText> treeNodes = projectView.getData().getAll();
        int fullSize = treeNodes.size();
        //0 - main package not found yet
        //1 - inside main/java
        //2 - found resources
        int status = 0;
        for (RemoteText text : treeNodes) {
            if (status == 0) {
                if (text.getText().equals("java")) {
                    status = 1;
                }
            } else if (status == 1) {
                if (text.getText().equals("resources")) {
                    status = 2;
                } else {

                    if (text.getText().contains(".")) {
                        continue;
                    }

                    text.doubleClick();
                    pause(ofSeconds(2).toMillis());
                    //try to execute

                    TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
                    if (editor.getEditor().getFileName().equals(currentFile)) {
                        if (!currentFile.contains(text.getText())) {
                            //skip directories
                            idea.getExpandAllButton().click();
                            pause(ofSeconds(1).toMillis());
                            continue;
                        }
                    }

                    List<GutterIcon> icons = editor.getGutter().getIcons();

                    for (GutterIcon icon : icons) {
                        if (icon.toString().contains("name=Unlogged")) {
                            //unlogged icon found, click it.
                            icon.click();
                            if (!icon.toString().contains("overriddenPath='/icons/svg/process_running.svg'")) {
                                //open direct Invoke if not process running
                                idea.getDirectInvokeTabHeader().click();
                            }
                            pause(ofSeconds(1).toMillis());
                            idea.getExecuteMethodButton().click();
                            //wait for response
                            pause(ofSeconds(5).toMillis());
                        }
                    }
                    currentFile = editor.getEditor().getFileName();
                    pause(ofSeconds(1).toMillis());
                }
            } else {
                //break for single module projects.
                break;
            }
        }
        idea.getStopButton().click();

//        projectView.findText(fileName).doubleClick();
//        Project project
//        = idea.getProject();

//        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE,
//                GlobalSearchScope.projectScope(project));
//
//        System.out.println("virtual files :"+virtualFiles.toString());
    }
}
