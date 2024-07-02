package com.insidious.plugin.UITests.Utils;

import com.insidious.plugin.UITests.UIElementNotFoundException;
import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.insidious.plugin.UITests.wrapper.*;
import com.insidious.plugin.UITests.wrapper.JunitGenerationRequest;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.Locators;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.junit.jupiter.api.Assertions;
import org.junit.Assert;
import static java.time.Duration.*;

import java.rmi.Remote;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.*;

import static com.insidious.plugin.UITests.Utils.UITestUtils.setFilterOptionsForCurrentView;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.KeyEvent.VK_A;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;

public class UiTestInteractionUtils {

    public static void clearGotIts(RemoteRobotController controller) {
        List<ComponentFixture> gotItTexts = controller.getIdeaFrame().getGotItTexts();
        gotItTexts.forEach(text -> text.click());
    }

    public static void addUnloggedToStartFile(RemoteRobotController controller, String filename, String annotationText, boolean openFile) {
        if (openFile) {
            openFileIfNeeded(filename, controller);
        }
        TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
        try {
            expandJavaFile(textEditorFixture.getEditor());
        } catch (NoSuchElementException e) {
            //package text not found
        }

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
        RemoteText publicMethodLabel = getFirstPublicKeywordOnLeft(mainClassContents, mainLabelIndex);
        if (publicMethodLabel == null) {
            Assertions.fail("Main class is not valid");
        }
        publicMethodLabel.click();
        controller.getKeyboard().hotKey(VK_ENTER);
        controller.getKeyboard().enterText(annotationText);
        controller.getKeyboard().hotKey(VK_ENTER);
    }

    public static List<GutterIcon> getAllUnloggedEntryPointGutterIconsSortedForOpenFile(RemoteRobotController controller) {
        return controller.getIdeaFrame().textEditor().getGutter().getIcons()
                .stream().filter(gutterIcon -> gutterIcon.toString().contains("profileBlue.svg"))
                .sorted((icon1, icon2) -> {
                    return icon1.getLineNumber() - icon2.getLineNumber();
                }).toList();
    }

    private static RemoteText getFirstPublicKeywordOnLeft(List<RemoteText> sourceTexts, int indexOfMain) {
        for (int i = indexOfMain - 1; i >= 0; i--) {
            RemoteText text = sourceTexts.get(i);
            if (text.getText().equals("public")) {
                return text;
            }
        }
        return null;
    }

    public static void scrollToIcon(TextEditorFixture editorFixture, GutterIcon icon) {
        Integer startingOffset = editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineStartOffset(" + (icon.getLineNumber()) + ")", true);
        editorFixture.getEditor().scrollToOffset(startingOffset);
    }

    public static int getLineNumberFromOffset(TextEditorFixture editorFixture, int offset) {
        return editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineNumber(" + offset + ")", true);
    }

    public static void interactWithMockEditPanel(String mockname, Map<Integer, String> subValues, IdeaFrame
            ideaFrame, String type, Keyboard keyboard) {
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

    public static void openFileIfNeeded(String filename, RemoteRobotController controller) {
        try {
            TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
            if (textEditorFixture.getEditor().getFileName().equals(filename)) {
                //don't open if file is already open
                return;
            }
        } catch (Exception e) {
        }

        controller.getKeyboard().hotKey(VK_META, VK_SHIFT, VK_O);
        pause(ofMillis(250).toMillis());
        controller.getKeyboard().enterText(filename);
        pause(ofMillis(250).toMillis());
        controller.getKeyboard().hotKey(VK_ENTER);
    }

    public static void searchFirstInCurrentFile(RemoteRobotController controller, String identifier) {
        controller.getKeyboard().hotKey(VK_META, VK_F);
        controller.getKeyboard().enterText(identifier);
        controller.getKeyboard().hotKey(VK_ENTER);
    }

    public static void expandJavaFile(EditorFixture editor) {
        editor.scrollToOffset(1);
        RemoteText packageText = editor.findText("package");
        packageText.click();
        for (RemoteText text : editor.getData().getAll()) {
            if (text.getText().equals("...")) {
                text.click();
            }
        }
    }

    //The shell script file here is expected to have one command to run
    //waitForSeconds is the duration to wait for script execution.
    public static void executeShellScriptAndWait(RemoteRobotController controller, String filename,
                                                 int waitForSeconds) {
        openFileIfNeeded(filename, controller);
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
        openFileIfNeeded(filename, controller);
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

    public static void performGradleSync(RemoteRobotController controller) {
        ComponentFixture gradleIcon = controller.getIdeaFrame().getGradleToolbarIcon();
        gradleIcon.click();
        pause(ofSeconds(1).toMillis());
        controller.getIdeaFrame().getGradleToolBarRefreshIcon().click();
        pause(ofSeconds(1).toMillis());
        gradleIcon.click();
    }

    public static void directInvokeMethod(DirectInvokeRequest request, RemoteRobotController controller) {
        if (request.isOpenFile()) {
            openFileIfNeeded(request.getClassname(), controller);
            pause(ofMillis(500).toMillis());
        }
        expandJavaFile(controller.getIdeaFrame().textEditor().getEditor());
        pause(ofMillis(250).toMillis());
        searchFirstInCurrentFile(controller, request.getMethodIdentifier());

        EditorFixture editorFixture = controller.getIdeaFrame().textEditor().getEditor();
        int caretOffset = editorFixture.getCaretOffset();
        int lineNumber = getLineNumberFromOffset(controller.getIdeaFrame().textEditor(), caretOffset) + 1;

        GutterIcon selectedMethodIcon = controller.getIdeaFrame().textEditor().getGutter().getIcons().stream()
                .filter(gutterIcon -> gutterIcon.getLineNumber() == lineNumber)
                .limit(1).toList().get(0);

        selectedMethodIcon.click();
        controller.getIdeaFrame().getGoToDirectInvokeButton().click();
        pause(ofSeconds(3).toMillis());

        ComponentFixture argumentsTree = controller.getIdeaFrame().getTree();
        List<RemoteText> remoteTexts = argumentsTree.getData().getAll();
        request.getInputs().forEach(line -> {
            remoteTexts.get(line.getIndex()).click();
            controller.getKeyboard().enterText(line.getValue());
            controller.getKeyboard().hotKey(VK_ENTER);
        });
        controller.getIdeaFrame().getDirectInvokeExecuteButtonNew().click();
        pause(ofSeconds(3).toMillis());
    }

    public static void directInvokeAndAssertResponse(DirectInvokeRequest request, RemoteRobotController controller) {
        directInvokeMethod(request, controller);
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

    public static void openUnloggedToolbarIfNotOpen(RemoteRobotController controller, int waitDurationSeconds) {
        try {
            //Check if filter button is visible
            controller.getIdeaFrame().getlibraryTabHeader();
        } catch (UIElementNotFoundException notFoundException) {
            controller.getIdeaFrame().getUnloggedToolbarComponent().click();
            pause(ofSeconds(waitDurationSeconds).toMillis());
        }
    }

    public static List<String> listIDEFatalExceptions(RemoteRobotController controller) {
        List<String> ideExceptions = new ArrayList<>();
        try {
            controller.getIdeaFrame().getFatalIdeExceptionIcon().click();
            int totalIdeExceptions = controller.getIdeaFrame().getFatalErrorNextButton().getData().getAll().size() + 2;

            int i = 0;
            do {
                ideExceptions.add(controller.getIdeaFrame().getFatalErrorMessageTab().getData().getAll().get(0).getText());
                controller.getIdeaFrame().getFatalErrorNextButton().click();
                i++;
            } while (i < totalIdeExceptions);
        } catch (Exception e) {
            Assert.assertTrue("No IDE exceptions occured", true);
        }
        return ideExceptions;
    }

    public static void generateJunitTestCaseForMethod(RemoteRobotController controller, JunitGenerationRequest
            request) {
        if (request.getJunitGenerationMethod().equals(JunitGenerationMethod.JUNIT_ICON)) {
            generateJunitUsingIcon(controller, request);
        } else if (request.getJunitGenerationMethod().equals(JunitGenerationMethod.REPLAY_DATA)) {
            junitReplayDataGeneration(controller, request);
        } else {
            junitDummyDataGeneration(controller, request);
        }
    }

    public static void injectUnloggedTestFile(RemoteRobotController controller, boolean isMultiModule) {
        controller.getIdeaFrame().getRunReplayTestIcon().click();
        if(isMultiModule) {
            //TODO: Add selection using dropdown for multi-module
        }
        controller.getIdeaFrame().getInjectFileButton().click();
        controller.getIdeaFrame().getInjectPopupCloseButton().click();
    }

    //reorder steps, make directInvoke only if candidates are not present
    public static void generateJunitUsingIcon(RemoteRobotController controller, JunitGenerationRequest request) {
        if (request.isExecuteOnDemand()) {
            directInvokeMethod(request.getDirectInvokeRequest(), controller);
        }
        backToMenuIfOpen(controller);
        controller.getIdeaFrame().getFilterOnTimelineMenuOption().click();
        //doesn't need to open file
        clearGotIts(controller);
        pause(ofMillis(500).toMillis());

        if (request.getFilterOptions() != null) {
            setFilterOptionsForCurrentView(controller, request.getFilterOptions());
        }
        pause(ofSeconds(1).toMillis());
        try {
            controller.getIdeaFrame().getFirstCheckbox().click();
        } catch (Exception e) {
            directInvokeMethod(request.getDirectInvokeRequest(), controller);
            backToMenuIfOpen(controller);
            controller.getIdeaFrame().getFilterOnTimelineMenuOption().click();
            pause(ofSeconds(1).toMillis());
            controller.getIdeaFrame().getFirstCheckbox().click();
        }
        clearGotIts(controller);

        controller.getIdeaFrame().getJunitTopToolbarIcon().click();
        pause(ofSeconds(5).toMillis());
    }

    public static void junitDummyDataGeneration(RemoteRobotController controller, JunitGenerationRequest request) {
        DirectInvokeRequest directInvokeRequest = request.getDirectInvokeRequest();
        openFileIfNeeded(request.getDirectInvokeRequest().getClassname(), controller);

        expandJavaFile(controller.getIdeaFrame().textEditor().getEditor());
        searchFirstInCurrentFile(controller, directInvokeRequest.getMethodIdentifier());

        EditorFixture editorFixture = controller.getIdeaFrame().textEditor().getEditor();
        int caretOffset = editorFixture.getCaretOffset();
        int lineNumber = getLineNumberFromOffset(controller.getIdeaFrame().textEditor(), caretOffset) + 1;

        List<GutterIcon> icons = controller.getIdeaFrame().textEditor().getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList();

        System.out.println("Line number selected : " + lineNumber);
        for (GutterIcon icon : icons) {
            System.out.println("Line number - > " + icon.getLineNumber());
        }

        GutterIcon selectedMethodIcon = controller.getIdeaFrame().textEditor().getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .filter(gutterIcon -> gutterIcon.getLineNumber() == lineNumber)
                .limit(1).toList().get(0);

        selectedMethodIcon.click();
        controller.getIdeaFrame().getGoToBoilerplateTestDummyData().click();
        pause(ofSeconds(3).toMillis());

        ComponentFixture savePathFile = controller.getIdeaFrame().getFirstJTextField();
        if (savePathFile.getData().getAll().isEmpty()) {
            Assertions.fail("No Path found for JUNIT CASE");
            return;
        }
        //TODO: Add steps to use other elements of the Junit request
        //TODO: Can also assert test case path here
        controller.getIdeaFrame().getBoilerplateTestSaveButton().click();
        pause(ofSeconds(3).toMillis());

        backToMenuIfOpen(controller);
    }

    public static void junitReplayDataGeneration(RemoteRobotController controller, JunitGenerationRequest request) {

        if (request.isExecuteOnDemand()) {
            directInvokeMethod(request.getDirectInvokeRequest(), controller);
        } else {
            openFileIfNeeded(request.getDirectInvokeRequest().getClassname(), controller);
        }

        searchFirstInCurrentFile(controller, request.getDirectInvokeRequest().getMethodIdentifier());
        EditorFixture editorFixture = controller.getIdeaFrame().textEditor().getEditor();
        int caretOffset = editorFixture.getCaretOffset();
        int lineNumber = getLineNumberFromOffset(controller.getIdeaFrame().textEditor(), caretOffset) + 1;

        GutterIcon selectedMethodIcon = controller.getIdeaFrame().textEditor().getGutter().getIcons().stream()
                .filter(gutterIcon -> gutterIcon.getLineNumber() == lineNumber)
                .limit(1).toList().get(0);

        selectedMethodIcon.click();
        controller.getIdeaFrame().getGoToBoilerplateTestReplayData().click();
        pause(ofSeconds(3).toMillis());
        //TODO: Add steps to use other elements of the Junit request
        //TODO: Can also assert test case path here
        controller.getIdeaFrame().getBoilerplateTestSaveButton().click();
        pause(ofSeconds(3).toMillis());

        backToMenuIfOpen(controller);
    }

    public static void executeDeterministicShellCommand(RemoteRobotController controller, String command,
                                                        int waitDurationInSeconds) {
        controller.getIdeaFrame().getTerminalToolBarSelectable().click();
        pause(ofSeconds(10).toMillis());

        //click in the window to shift focus there
        List<RemoteText> terminalCharacters = controller.getIdeaFrame().getTerminalPanel().getData().getAll();
        if (terminalCharacters.size() > 0) {
            terminalCharacters.get(terminalCharacters.size() - 1).click();
        }

        controller.getKeyboard().enterText(command);
        pause(ofMillis(50).toMillis());

        controller.getKeyboard().hotKey(VK_ENTER);
        pause(ofSeconds(waitDurationInSeconds).toMillis());
        controller.getIdeaFrame().getTerminalToolWindowHideButton().click();
    }

    //selects buttons based on state - if other projects are present or not
    public static void cloneAndOpenProject(RemoteRobotController controller, GitProjectInfo projectUnderTest) {
        WelcomeFrame welcomeFrame = controller.getRemoteRobot().find(WelcomeFrame.class, ofSeconds(10));
        try {
            welcomeFrame.getVcsCreateOption().click();
            pause(ofMillis(250).toMillis());
        } catch (Exception e) {
            welcomeFrame.getVcsCreateButtonV2().click();
            pause(ofMillis(250).toMillis());
        }
        welcomeFrame.getVcsRepoUrlTextField().click();

        controller.getKeyboard().enterText(projectUnderTest.getGitUrl());
        pause(ofMillis(250).toMillis());
        welcomeFrame.getVcsCloneButton().click();

        pause(ofSeconds(30).toMillis());

        if (projectUnderTest.getLoginOptions() != null) {
            System.out.println("Not null");
            try {
                welcomeFrame.getGitUseTokenOprionButton().click();
                pause(ofSeconds(1).toMillis());

                controller.getKeyboard().enterText(projectUnderTest.getLoginOptions().getPersonalAccessToken());
                pause(ofMillis(250).toMillis());

                welcomeFrame.getGitPatLoginButton().click();
                pause(ofSeconds(10).toMillis());
            } catch (Exception e) {
                System.out.println("Exception e : " + e);
                e.printStackTrace();
            }
        }

        controller.unsertIdeaFrame();

        if (projectUnderTest.isSwitchBranchOnOpen()) {
            executeDeterministicShellCommand(controller, "git checkout " + projectUnderTest.getGitBranch(), 2);
        }
    }

    public static void closeOptionsTabIfOpen(RemoteRobotController controller) {
        boolean done = false;
        while (!done) {
            try {
                controller.getIdeaFrame().getBackButtonFromOptions().click();
                pause(ofMillis(250).toMillis());
            } catch (Exception e) {
                done = true;
            }
        }
    }

    public static void backToMenuIfOpen(RemoteRobotController controller) {
        try {
            controller.getIdeaFrame().getBackToMenuButton().click();
        } catch (Exception e) {

        }
    }

    public static void runIntelliJIdeaAction(RemoteRobotController controller, String option,
                                             int waitDurationInSeconds) {
        controller.getKeyboard().hotKey(VK_META, VK_SHIFT, VK_A);
        pause(ofMillis(250).toMillis());
        controller.getKeyboard().enterText(option);
        pause(ofSeconds(2).toMillis());
        controller.getKeyboard().hotKey(VK_ENTER);
        pause(ofSeconds(waitDurationInSeconds).toMillis());
    }

    public static void runIntelliJIdeaActionV2(RemoteRobotController controller, String option,
                                               int waitDurationInSeconds) {
        controller.getKeyboard().hotKey(VK_META, VK_SHIFT, VK_O);
        pause(ofMillis(250).toMillis());
        controller.getKeyboard().enterText(option);
        pause(ofSeconds(2).toMillis());
        controller.getKeyboard().hotKey(VK_ENTER);
        pause(ofSeconds(waitDurationInSeconds).toMillis());
    }

    public static void addUnloggedDependenciesToBuildFile(RemoteRobotController controller, GitProjectInfo
            projectUnderTest, boolean open) {
        if (projectUnderTest.getBuildSystem().equals(LocalProjectInfo.BuildSystem.MAVEN)) {
            addMavenDependenciesAndSync(controller, projectUnderTest.getBuildFile(), open);
        } else if (projectUnderTest.getBuildSystem().equals(LocalProjectInfo.BuildSystem.GRADLE)) {
            addGradleDependencies(controller, projectUnderTest.getBuildFile(), open);
        }
    }

    public static void addMavenDependenciesAndSync(RemoteRobotController controller, String pomFile, boolean open) {
        if (open) {
            openFileIfNeeded(pomFile, controller);
        }
        ComponentFixture unloggedToolbar = controller.getIdeaFrame().getUnloggedToolbarComponent();
        unloggedToolbar.moveMouse();
        unloggedToolbar.click();
        pause(ofSeconds(1).toMillis());

        //go to index where dependency needs to be added
        TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
        List<RemoteText> pomContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText dependencyText = pomContents.stream().filter(remoteText -> remoteText.getText().equals("dependencies")).toList().get(0);
        int indexOfDependencies = pomContents.indexOf(dependencyText);
        RemoteText closingTag = pomContents.get(indexOfDependencies + 1);

        try {
            ComponentFixture copyButton = controller.getIdeaFrame().findCopyButton();
            copyButton.moveMouse();
            copyButton.click();

            pause(ofMillis(250).toMillis());

            closingTag.click();
            //paste
            controller.getKeyboard().hotKey(VK_RIGHT);
            controller.getKeyboard().hotKey(VK_ENTER);
            controller.getKeyboard().hotKey(VK_META, VK_V);
        } catch (Exception e) {
            //copy button not in sight, manually add text
            closingTag.click();
            controller.getKeyboard().enterText(TestConstants.MAVEN_DEPENDENCY_TEMPLATE);
        }
        performMavenSync(controller);
        controller.waitForIndex();
    }

    public static void addGradleDependencies(RemoteRobotController controller, String buildGradleFile, boolean open) {
        if (open) {
            openFileIfNeeded(buildGradleFile, controller);
        }
        ComponentFixture unloggedToolbar = controller.getIdeaFrame().getUnloggedToolbarComponent();
        unloggedToolbar.moveMouse();
        unloggedToolbar.click();
        pause(ofSeconds(1).toMillis());

        //go to index where dependency needs to be added
        TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
        List<RemoteText> gradleFileContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText dependencyText = gradleFileContents.stream().filter(remoteText -> remoteText.getText().equals("dependencies")).toList().get(0);
        int indexOfDependencies = gradleFileContents.indexOf(dependencyText);
        RemoteText previousElement = gradleFileContents.get(indexOfDependencies - 1);

        try {
            ComponentFixture JTabbedPaneFixture = controller.getIdeaFrame().getJTabbedPane();
            RemoteText gradleOption = JTabbedPaneFixture.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Gradle")).toList().get(0);

            gradleOption.click();
            pause(ofMillis(125).toMillis());

            ComponentFixture copyButton = controller.getIdeaFrame().findCopyButton();
            copyButton.moveMouse();
            copyButton.click();

            pause(ofMillis(250).toMillis());
            previousElement.click();
            //paste
            controller.getKeyboard().hotKey(VK_RIGHT);
            controller.getKeyboard().hotKey(VK_ENTER);
            controller.getKeyboard().hotKey(VK_META, VK_V);
        } catch (Exception e) {
            //copy button not in sight, manually add text
            previousElement.click();
            controller.getKeyboard().enterText(TestConstants.GRADLE_DEPENDENCY_TEMPLATE);
        }

        performGradleSync(controller);
        controller.waitForIndex();
    }

    public static void setIntelliJAsGradleBuilder(RemoteRobotController controller, GitProjectInfo projectInfo) {
        runIntelliJIdeaAction(controller, "Settings", 3);
        ComponentFixture myTreeComponent = controller.getIdeaFrame().getMyTreeComponent();
        try {
            RemoteText gradleOption = myTreeComponent.getData().getAll().stream().filter(text -> text.getText().equals("Gradle")).toList().get(0);
            gradleOption.click();
        } catch (Exception e) {
            try {
                RemoteText buildTools = myTreeComponent.getData().getAll().stream().filter(text -> text.getText().equals("Build Tools")).toList().get(0);
                buildTools.doubleClick();
                myTreeComponent = controller.getIdeaFrame().getMyTreeComponent();
                RemoteText gradleOption = myTreeComponent.getData().getAll().stream().filter(text -> text.getText().equals("Gradle")).toList().get(0);
                gradleOption.click();
            } catch (Exception e1) {
                RemoteText buildOptions = myTreeComponent.getData().getAll().stream().filter(text -> text.getText().equals("Build, Execution, Deployment")).toList().get(0);
                buildOptions.doubleClick();
                myTreeComponent = controller.getIdeaFrame().getMyTreeComponent();
                RemoteText buildTools = myTreeComponent.getData().getAll().stream().filter(text -> text.getText().equals("Build Tools")).toList().get(0);
                buildTools.doubleClick();
                myTreeComponent = controller.getIdeaFrame().getMyTreeComponent();
                RemoteText gradleOption = myTreeComponent.getData().getAll().stream().filter(text -> text.getText().equals("Gradle")).toList().get(0);
                gradleOption.click();
            }
        }

        ComponentFixture comboBoxFixture = controller.getIdeaFrame().getGradleBuildWithOption();
        List<RemoteText> remoteTexts = comboBoxFixture.getData().getAll();

        boolean needsSwitch = remoteTexts.stream().anyMatch(text -> text.getText().equals("Gradle"));
        if (needsSwitch) {
            remoteTexts.get(0).click();
            controller.getKeyboard().hotKey(VK_DOWN);
            controller.getKeyboard().hotKey(VK_ENTER);

            pause(ofMillis(250).toMillis());
            controller.getIdeaFrame().getApplyButtonGeneric().click();
        }
        ComponentFixture sdkComboBox = controller.getIdeaFrame().getSdkComboBox();
        remoteTexts = sdkComboBox.getData().getAll();

        remoteTexts.forEach(text -> System.out.println("Text : " + text.getText()));
        boolean shouldSwitch = remoteTexts.stream().noneMatch(text -> text.getText().contains(projectInfo.getJdkVersion()) ||
                text.getText().contains("Project SDK"));
        if (shouldSwitch) {
            remoteTexts.get(0).click();
            controller.getKeyboard().hotKey(VK_UP);
            controller.getKeyboard().hotKey(VK_ENTER);

            pause(ofMillis(250).toMillis());
            controller.getIdeaFrame().getApplyButtonGeneric().click();
        }
        controller.getIdeaFrame().getOKButtonGeneric().click();
    }

    public static Long getNumberofTestsInCurrentFile(RemoteRobotController controller) {
        TextEditorFixture textEditorFixture = controller.getIdeaFrame().textEditor();
        //assumes no tests are run
        return textEditorFixture.getGutter().getIcons().stream().filter(icon -> icon.toString().contains("/run.svg")).count();
    }

    public static void selectAllAndSave(RemoteRobotController controller, int waitDurationInSeconds) {
        //assumes unlogged toolbar is open
        controller.getIdeaFrame().getSelectAllicon().click();
        pause(ofMillis(500).toMillis());

        controller.getIdeaFrame().getSaveGlobalButton().click();
        pause(ofSeconds(waitDurationInSeconds).toMillis());

        controller.getIdeaFrame().getSaveFromConfirmButton().click();
        pause(ofSeconds(2).toMillis());
    }
}
