package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.List;

import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;

//To run these :
//1. Run "runIdeForUiTests"
//2. Once an ide instance is created run the tests below.
public class UiTest {
    private RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
    private final Keyboard keyboard = new Keyboard(remoteRobot);

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

        //wait till process starts - 10 seconds as default
        pause(ofSeconds(10).toMillis());
        List<GutterIcon> icons = editor.getGutter().getIcons();

        for (GutterIcon icon : icons) {
            if (icon.toString().contains("overriddenPath='/icons/svg/process_running.svg'")) {
                //process running icon found, click it.
                icon.click();
                pause(ofSeconds(5).toMillis());
                idea.getExecuteMethodButton().click();
            }
        }
    }

    //Needs your project to be up and running to run this test.
    //Also needs the project tree to be visible.
    //disable debugging on code when running this/don't run oit in debug mode.
    @Test
    public void DirectInvokeAll() {
        //set to true if project has multiple modules
        boolean multiModule = false;
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));

        //ensure this file is visible in the project tree if using startFrom
        // possible cause for corruption : PaymentTerminalDao
        // OUT OF BOUNDS exception for click in EntityTagMap, AppointmentRes
        // Keep an eye out for ui freezes (of runIdeForTests Instance)
        String startWith = "PatientCaseAuditService";
        boolean startFrom = true;
        ContainerFixture projectView;
        if (!startFrom) {
            idea.getExpandAllButton().click();
            pause(ofSeconds(1).toMillis());

            projectView = idea.getProjectViewTree();
            projectView.getData().getAll().get(0).click();
            keyboard.enterText("src");
        } else {
            projectView = idea.getProjectViewTree();
        }

        String currentFile = "";
        try {
            TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
            currentFile = editor.getEditor().getFileName();
        } catch (Exception e) {
            //noting to do
            //this is for when no files are open in editor/no editor
        }

        List<RemoteText> treeNodes = projectView.getData().getAll();
        if (startFrom) {
            treeNodes = filterCustomStart(treeNodes, startWith);
        }
        List<RemoteText> toVisit = new ArrayList<>();

        //0 - main package not found yet
        //1 - inside main/java
        //2 - found resources
        int index = 0;
        int status = 0;
        if (startFrom) {
            status = 1;
        }
        boolean done = false;
        toVisit.addAll(treeNodes);
        while (!done) {
            if (index == toVisit.size()) {
                //should update map and click locate to ensure expand all moves again
                idea.getLocateButton().click();
                pause(ofSeconds(1).toMillis());
                RemoteText text = toVisit.get(index - 1);
                toVisit = updateToVisit(toVisit, text, idea, projectView);
            }
            RemoteText text = toVisit.get(index);
            if (status == 0) {
                if (text.getText().equals("java")) {
                    status = 1;
                }
            } else if (status == 1) {
                if (text.getText().equals("resources")
                        || text.getText().equals("test")
                        || text.getText().equals("target")) {
                    status = 2;
                } else {

                    if (text.getText().contains(".")) {
                        index++;
                        if (index == toVisit.size()) {
                            toVisit = updateToVisit(toVisit, text, idea, projectView);
                        }
                        continue;
                    }

                    text.doubleClick();
                    pause(ofSeconds(2).toMillis());

                    TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
                    if (editor.getEditor().getFileName().equals(currentFile)) {
                        if (!currentFile.contains(text.getText())) {
                            idea.getExpandAllButton().click();
                            index++;
                            if (index == toVisit.size()) {
                                toVisit = updateToVisit(toVisit, text, idea, projectView);
                            }
                            //skip directories
                            continue;
                        }
                    }

                    if (!editor.getEditor().getFileName().endsWith(".java")) {
                        idea.getExpandAllButton().click();
                        index++;
                        if (index == toVisit.size()) {
                            toVisit = updateToVisit(toVisit, text, idea, projectView);
                        }
                        //skip non java files
                        continue;
                    }

                    pause(ofSeconds(1).toMillis());

                    //start from the top of the file, useful if that file was previously open
                    editor.getEditor().scrollToOffset(1);
                    pause(ofSeconds(1).toMillis());
                    RemoteText packageText = editor.getEditor().findText("package");
                    packageText.click();

                    expandJavaFile(editor.getEditor());
                    List<GutterIcon> icons = editor.getGutter().getIcons();
                    TreeMap<Integer, GutterIcon> iconTreeMap = new TreeMap<>();
                    for (GutterIcon icon : icons) {
                        iconTreeMap.put(icon.getLineNumber(), icon);
                    }

                    if (iconTreeMap.size() == 0) {
                        index++;
                        if (index == toVisit.size()) {
                            toVisit = updateToVisit(toVisit, text, idea, projectView);
                        }
                        currentFile = editor.getEditor().getFileName();
                        idea.getExpandAllButton().click();
                        //skip files with no gutter icons
                        continue;
                    }

                    editor = idea.textEditor(Duration.ofSeconds(2));
                    pause(ofSeconds(1).toMillis());

                    for (Integer key : iconTreeMap.keySet()) {
                        GutterIcon icon = iconTreeMap.get(key);
                        String iconAsString = icon.toString();
                        if (iconAsString.contains("name=Unlogged")
                                && (iconAsString.contains("process_running.svg")
                                || iconAsString.contains("data_available_v2.svg"))) {
                            //unlogged icon found, click it.
                            scrollDownToIcon(editor, icon);
                            pause(ofSeconds(1).toMillis());
                            try {
                                icon.click();
                            } catch (Exception e) {
                                scrollDownToIcon(editor, icon);
                                icon.click();
                            }

                            if (icon.toString().contains("overriddenPath='/icons/svg/execute_v2.svg'")) {
                                //skip if execute all, no need to hot reload as this test is for
                                //Direct Invoke only
                                continue;
                            }

                            step("Direct Invoke method", () -> {
                                pause(ofSeconds(1).toMillis());
                                idea.getDirectInvokeTabHeader().click();
                                try {
                                    idea.getExecuteMethodButton().click();
                                } catch (Exception exception) {
                                    //atomic window in focus right after button click
                                    System.out.println("Atomic window in view when trying to click direct Invoke");
                                    idea.getDirectInvokeTabHeader().click();
                                    idea.getExecuteMethodButton().click();
                                }
                                //wait for response
                                pause(ofSeconds(5).toMillis());
                            });
                        }
                    }
                    currentFile = editor.getEditor().getFileName();
                    idea.getExpandAllButton().click();
                }
            } else {
                //break for single module projects.
                //continue for multi module
                //break;
                done = true;

                //go to state 1, new module found with java base package.
                if (!multiModule) {
                    break;
                }
                //found a new java directory
                if (text.getText().equals("java")) {
                    status = 1;
                }
            }
            index++;
            if (index == toVisit.size()) {
                toVisit = updateToVisit(toVisit, text, idea, projectView);
            }
        }
    }

    private List<RemoteText> filterCustomStart(List<RemoteText> treeNodes, String startWith) {
        int index = 0;
        for (RemoteText text : treeNodes) {
            if (text.getText().contains(startWith)) {
                break;
            }
            index++;
        }
        return treeNodes.subList(index, treeNodes.size());
    }

    private void expandJavaFile(EditorFixture editor) {
        for (RemoteText text : editor.getData().getAll()) {
            if (text.getText().equals("...")) {
                text.click();
            }
        }
    }

    private void scrollDownToIcon(TextEditorFixture editorFixture, GutterIcon icon) {
        int startingOffset = editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineStartOffset(" + icon.getLineNumber() + ")", true);
        editorFixture.getEditor().scrollToOffset(startingOffset);
        System.out.println("Scrolling down to line number : " + icon.getLineNumber() + ", Offset : " + startingOffset);
    }

    private List<RemoteText> updateToVisit(List<RemoteText> toVisit, RemoteText text,
                                           IdeaFrame idea, ContainerFixture projectView) {
        projectView = idea.getProjectViewTree();
        List<RemoteText> newRefs = projectView.getData().getAll();
        newRefs = getNewSubList(newRefs, text);
        toVisit.addAll(newRefs);
        return toVisit;
    }

    private List<RemoteText> getNewSubList(List<RemoteText> newRefs, RemoteText current) {
        int index = 0;
        for (RemoteText remoteText : newRefs) {
            if (remoteText.getText().equals(current.getText())) {
                index++;
                break;
            }
            index++;
        }
        return newRefs.subList(index, newRefs.size());
    }

    @Test
    public void fullCoverageFlow() {
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));

        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        EditorFixture editorFixture = editor.getEditor();

        editor.getEditor().scrollToOffset(1);
        pause(ofSeconds(1).toMillis());
        RemoteText packageText = editor.getEditor().findText("package");
        packageText.click();

        expandJavaFile(editor.getEditor());
        List<GutterIcon> icons = editor.getGutter().getIcons();
        TreeMap<Integer, GutterIcon> iconTreeMap = new TreeMap<>();
        for (GutterIcon icon : icons) {
            iconTreeMap.put(icon.getLineNumber(), icon);
        }

        if (iconTreeMap.size() == 0) {
            return;
        }

        editor = idea.textEditor(Duration.ofSeconds(2));
        pause(ofSeconds(1).toMillis());

        for (Integer key : iconTreeMap.keySet()) {
            GutterIcon icon = iconTreeMap.get(key);
            String iconAsString = icon.toString();
            if (iconAsString.contains("name=Unlogged")
                    && (iconAsString.contains("process_running.svg")
                    || iconAsString.contains("data_available_v2.svg"))) {
                //unlogged icon found, click it.
                scrollDownToIcon(editor, icon);
                pause(ofSeconds(1).toMillis());
                try {
                    icon.click();
                } catch (Exception e) {
                    scrollDownToIcon(editor, icon);
                    icon.click();
                }

                if (icon.toString().contains("overriddenPath='/icons/svg/execute_v2.svg'")) {
                    //skip if execute all, no need to hot reload as this test is for
                    //Direct Invoke only
                    continue;
                }

                step("Direct Invoke method", () -> {
                    pause(ofSeconds(1).toMillis());
                    idea.getDirectInvokeTabHeader().click();
                    try {
                        idea.getExecuteMethodButton().click();
                    } catch (Exception exception) {
                        //atomic window in focus right after button click
                        System.out.println("Atomic window in view when trying to click direct Invoke");
                        idea.getDirectInvokeTabHeader().click();
                        idea.getExecuteMethodButton().click();
                    }
                    //wait for response
                    pause(ofSeconds(5).toMillis());

                    //now execute replay
                    idea.getReplayTab().click();
                    idea.getFirstReplayButton().click();
                    pause(ofSeconds(1).toMillis());

                    idea.getSaveReplayButton().click();
                    idea.getFirstJtextFiled().click();
                    keyboard.enterText("def1");
                    idea.getSaveAndClose().click();
                });
                //stop after 1 run
                break;
            }
        }
    }
}
