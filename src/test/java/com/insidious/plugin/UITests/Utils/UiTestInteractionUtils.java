package com.insidious.plugin.UITests.Utils;

import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.wrapper.DirectInvokeRequest;
import com.insidious.plugin.UITests.wrapper.RemoteRobotController;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.awt.event.KeyEvent.*;
import static java.awt.event.KeyEvent.VK_A;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.swing.timing.Pause.pause;

public class UiTestInteractionUtils {

    public static void clearGotIts(RemoteRobotController controller) {
        List<ComponentFixture> gotItTexts = controller.getIdeaFrame().getGotItTexts();
        gotItTexts.forEach(text -> text.click());
    }

    public static void addUnloggedToStartFile(RemoteRobotController controller, String filename, String annotationText, boolean openFile) {
        if (openFile) {
            openFile(filename, controller);
        }
        TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
        expandJavaFile(textEditorFixture.getEditor());

        List<RemoteText> mainClassContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText firstSemicolon = mainClassContents.stream().filter(text -> text.getText().equals(";")).toList().get(0);
        firstSemicolon.click();
        controller.getKeyboard().hotKey(VK_RIGHT);
        controller.getKeyboard().hotKey(VK_ENTER);
        controller.getKeyboard().enterText("import io.unlogged.Unlogged;");

        //refresh contents post text addition
        mainClassContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText mainLabel = mainClassContents.stream().filter(text -> text.getText().equals("main")).toList().get(0);
        int mainLabelIndex = mainClassContents.indexOf(mainLabel);
        RemoteText spaceLabel = mainClassContents.get(mainLabelIndex - 7);
        spaceLabel.click();
        controller.getKeyboard().hotKey(VK_ENTER);
        controller.getKeyboard().enterText(annotationText);
        controller.getKeyboard().hotKey(VK_ENTER);
    }

    public static void scrollToIcon(TextEditorFixture editorFixture, GutterIcon icon) {
        Integer startingOffset = editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineStartOffset(" + (icon.getLineNumber()) + ")", true);
        editorFixture.getEditor().scrollToOffset(startingOffset);
    }

    public static int getLineNumberFromOffset(TextEditorFixture editorFixture, int offset) {
        return editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineNumber(" + offset + ")", true);
    }

    public static void interactWithMockEditPanel(String mockname, Map<Integer, String> subValues, IdeaFrame ideaFrame, String type, Keyboard keyboard) {
        //name the mock
        ComponentFixture replayCaseName = ideaFrame.getComponentByXpath("//div[@class='JTextField']");
        replayCaseName.moveMouse();
        replayCaseName.click();

        keyboard.hotKey(VK_META, VK_A);
        keyboard.enterText(mockname);

        ComponentFixture typeSelectHeader = ideaFrame.getMockEditReturnTypeHeader();
        if (type.equals("error")) {
            typeSelectHeader.moveMouse();
            typeSelectHeader.click();
            pause(ofMillis(250).toMillis());

            List<ComponentFixture> radioButtons = ideaFrame.getAllVisibleRadioButtons();
            radioButtons.get(2).click();
        } else if (type.equals("null")) {
            typeSelectHeader.moveMouse();
            typeSelectHeader.click();
            pause(ofMillis(250).toMillis());

            List<ComponentFixture> radioButtons = ideaFrame.getAllVisibleRadioButtons();
            radioButtons.get(1).click();
        }

        replayCaseName.click();

        for (Integer index : subValues.keySet()) {
            //change the values based on tree index (vertical order)
            ContainerFixture inputTreeFixture = ideaFrame.getContainerByXpath("//div[@class='Tree']");
            RemoteText customerNameElement = inputTreeFixture.findAllText().get(index);
            customerNameElement.moveMouse();
            customerNameElement.click();
            keyboard.enterText(subValues.get(index));
            pause(ofMillis(125).toMillis());
            keyboard.hotKey(VK_ENTER);
        }
    }

    public static void openFile(String filename, RemoteRobotController controller) {
        controller.getKeyboard().hotKey(VK_META, VK_SHIFT, VK_O);
        controller.getKeyboard().enterText(filename);
        controller.getKeyboard().hotKey(VK_ENTER);
    }

    public static void searchFirstInCurrentFile(RemoteRobotController controller, String identifier) {
        controller.getKeyboard().hotKey(VK_META, VK_F);
        controller.getKeyboard().enterText(identifier);
        controller.getKeyboard().hotKey(VK_ENTER);
    }

    public static void expandJavaFile(EditorFixture editor) {
        for (RemoteText text : editor.getData().getAll()) {
            if (text.getText().equals("...")) {
                text.click();
            }
        }
    }

    //The shell script file here is expected to have one command to run
    //waitForSeconds is the duration to wait for script execution.
    public static void executeShellScriptAndWait(RemoteRobotController controller, String filename, int waitForSeconds) {
        openFile(filename, controller);
        TextEditorFixture shellScript = controller.getIdeaFrame().textEditor();
        //click the first icon
        boolean started = false;
        while (!started) {
            try {
                GutterIcon gutterIcon = shellScript.getGutter().getIcons().get(0);
                gutterIcon.moveMouse();
                pause(ofMillis(250).toMillis());
                gutterIcon.click();
                started = true;
            } catch (Exception e) {
                pause(ofSeconds(2).toMillis());
            }
        }

        pause(ofSeconds(waitForSeconds).toMillis());
        controller.getIdeaFrame().getTerminalToolWindowHideButton().click();
    }

    public static void openAndRevertGitChangesForFile(String filename, RemoteRobotController controller) {
        openFile(filename, controller);
        pause(ofMillis(500).toMillis());

        TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
        RemoteText text = textEditorFixture.getEditor().getData().getAll().get(0);
        text.click();

        controller.getKeyboard().hotKey(VK_ALT, VK_META, VK_Z);
        pause(ofMillis(250).toMillis());
        controller.getIdeaFrame().getGitRollbackButton().click();
        pause(ofSeconds(1).toMillis());
    }

    public static void stopProcessInTerminal(RemoteRobotController controller) {
        try {
            if (controller.getIdeaFrame().getTerminalPanel().isShowing()) {
                stopAndCloseTerminalProcess(controller);
            } else {
                controller.getIdeaFrame().getTerminalToolBarSelectable().click();
            }
        } catch (Exception e) {
            //terminal content is not visible
            controller.getIdeaFrame().getTerminalToolBarSelectable().click();
            stopAndCloseTerminalProcess(controller);
        }
    }

    private static void stopAndCloseTerminalProcess(RemoteRobotController controller) {
        controller.getKeyboard().hotKey(VK_CONTROL, VK_C);
        pause(ofSeconds(10).toMillis());
        controller.getIdeaFrame().getTerminalToolWindowHideButton().click();
    }

    public static void performMavenSync(RemoteRobotController controller) {
        ComponentFixture mavenIcon = controller.getIdeaFrame().getMavenToolbarIcon();
        mavenIcon.click();
        pause(ofSeconds(1).toMillis());
        controller.getIdeaFrame().getMavenToolBarRefreshIcon().click();
        pause(ofSeconds(1).toMillis());
        mavenIcon.click();
    }

    public static void directInvokeAndAssertResponse(DirectInvokeRequest request, RemoteRobotController controller) {
        if (request.isOpenFile()) {
            openFile(request.getClassname(), controller);
            pause(ofMillis(500).toMillis());
        }
        expandJavaFile(controller.getIdeaFrame().textEditor().getEditor());
        searchFirstInCurrentFile(controller, request.getMethodIdentifier());

        EditorFixture editorFixture = controller.getIdeaFrame().textEditor().getEditor();
        int caretOffset = editorFixture.getCaretOffset();
        int lineNumber = getLineNumberFromOffset(controller.getIdeaFrame().textEditor(), caretOffset) + 1;

        GutterIcon selectedMethodIcon = controller.getIdeaFrame().textEditor().getGutter().getIcons().stream()
                .filter(gutterIcon -> gutterIcon.getLineNumber() == lineNumber)
                .limit(1).toList().get(0);

        selectedMethodIcon.click();
        controller.getIdeaFrame().getGoToDirectInvokeButton().click();

        ComponentFixture argumentsTree = controller.getIdeaFrame().getTree();
        List<RemoteText> remoteTexts = argumentsTree.getData().getAll();
        request.getInputs().forEach(line -> {
            remoteTexts.get(line.getIndex()).click();
            controller.getKeyboard().enterText(line.getValue());
            controller.getKeyboard().hotKey(VK_ENTER);
        });

        controller.getIdeaFrame().getDirectInvokeExecuteButtonNew().click();
        pause(ofSeconds(3).toMillis());

        ComponentFixture responseTree = controller.getIdeaFrame().getTree();
        Assertions.assertNotNull(responseTree);
        List<RemoteText> responseRemoteTexts = responseTree.getData().getAll();

        TreeMap<Integer, Map<String, String>> failingAssertions = new TreeMap<>();
        request.getExpectedOutputs().forEach(assertion -> {
            String responseTextLine = responseRemoteTexts.get(assertion.getIndex() - 1).getText();
            if (!responseTextLine.equals(assertion.getValue())) {
                TreeMap<String, String> status = new TreeMap<>();
                status.put("Expected", assertion.getValue());
                status.put("Actual", responseTextLine);
                failingAssertions.put(assertion.getIndex(), status);
            }
        });

        if (failingAssertions.isEmpty()) {
            System.out.println("Passing");
        } else {
            System.out.println("You have failing assertions");
            failingAssertions.forEach((index, status) -> {
                System.out.println("Line : " + index);
                System.out.println("Expected : " + status.get("Expected"));
                System.out.println("Actual : " + status.get("Actual"));
            });
            Assertions.fail("Failing");
        }
    }
}
