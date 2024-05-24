package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.CustomGutterIconComparator;
import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.insidious.plugin.UITests.wrapper.MockPopupEntry;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.rmi.Remote;
import java.time.Duration;
import java.util.*;
import java.util.List;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static java.awt.event.KeyEvent.*;
import static java.time.Duration.*;
import static org.assertj.swing.timing.Pause.pause;

public class UITestsV2 {

    private RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
    private final Keyboard keyboard = new Keyboard(remoteRobot);


    @Test
    public void testRemoteStartup() {
        final WelcomeFrame welcomeFrame = remoteRobot.find(WelcomeFrame.class, ofSeconds(10));
        welcomeFrame.getOpenProjectButton().click();

        welcomeFrame.getProjectSelectorComboBox().click();
        String projectName = "unlogged-spring-maven-demo";

        keyboard.enterText("/" + projectName);
        pause(ofSeconds(1).toMillis());
        welcomeFrame.getOpenConfirmButton().click();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(10), () -> !idea.isDumbMode());

        //wait for a small duration so that file searches don't turn up empty
        pause(ofSeconds(30).toMillis());

        //for mac, open pom.xml
        openFile("pom.xml", idea);

        //open unlogged toolbar, copy and paste dependency
        ComponentFixture unloggedToolbar = idea.getUnloggedToolbarComponent();
        unloggedToolbar.moveMouse();
        unloggedToolbar.click();
        pause(ofSeconds(1).toMillis());

        ComponentFixture copyButton = idea.findCopyButton();
        copyButton.moveMouse();
        copyButton.click();

        //paste right after dependencies
        TextEditorFixture textEditorFixture = idea.textEditor();
        List<RemoteText> pomContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText dependencyText = pomContents.stream().filter(remoteText -> remoteText.getText().equals("dependencies")).toList().get(0);
        int indexOfDependencies = pomContents.indexOf(dependencyText);
        RemoteText closingTag = pomContents.get(indexOfDependencies + 1);
        closingTag.click();
        keyboard.hotKey(VK_RIGHT);
        keyboard.hotKey(VK_ENTER);
        keyboard.hotKey(VK_META, VK_V);

        addUnloggedToStartFile("UnloggedDemoApplication", idea, "@Unlogged(serverEndpoint = \"http://18.188.85.130:8123\")");

        //refresh maven before proceeding
        ComponentFixture mavenIcon = idea.getMavenToolbarIcon();
        mavenIcon.click();
        pause(ofSeconds(1).toMillis());
        idea.getMavenToolBarRefreshIcon().click();
        pause(ofSeconds(1).toMillis());
        mavenIcon.click();

        //start application
        openFile("start_project.sh", idea);
        TextEditorFixture shellScript = idea.textEditor();
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

        pause(ofSeconds(60).toMillis());
        idea.getUnloggedToolbarComponent().click();
        pause(ofMillis(250).toMillis());
        clearGotIts(idea);
        idea.getTerminalToolWindowHideButton().click();

        idea.getFilterButton().click();
        pause(ofMillis(250).toMillis());

        ComponentFixture titlePanel = idea.getMyContentPanel();
        RemoteText sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
        sourcesTabText.click();
        idea.getRemoteButtonRadioLabel().click();
        pause(ofMillis(250).toMillis());

        ComponentFixture textField = idea.getFirstJTextField();
        textField.click();

        keyboard.hotKey(VK_META, VK_A);
        keyboard.hotKey(VK_DELETE);

        keyboard.enterText("http://18.188.85.130:8123");
        idea.getListSessionsButton().click();

        pause(ofSeconds(10).toMillis());

        idea.getAllVisibleRadioButtons().get(2).click();
        idea.getFilterApplyButton().click();

        pause(ofSeconds(10).toMillis());
        idea.getRefreshButton().click();

        openFile("FutureController.java", idea);
        pause(ofSeconds(2).toMillis());
        expandJavaFile(idea.textEditor().getEditor());
        clearGotIts(idea);

        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        List<GutterIcon> gutterIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList();

        List<Integer> lineNumbers = new ArrayList<>(gutterIcons.stream().map(GutterIcon::getLineNumber).toList());
        Collections.sort(lineNumbers);

        idea.getToolBarDeleteButton().click();
        List<Integer> finalLineNumbers = lineNumbers;
        TextEditorFixture finalEditor = editor;
        gutterIcons.forEach(icon -> {
            scrollToIcon(finalEditor, icon);
            icon.click();
            pause(ofMillis(250).toMillis());

            String responseExpected = "String: string";
            idea.getGoToDirectInvokeButton().click();
            pause(ofMillis(250).toMillis());

            if (icon.getLineNumber() == finalLineNumbers.get(0)) {
                responseExpected = "String: yolo";
            } else {
                responseExpected = "String: method2";
                ComponentFixture argumentsTree = idea.getTree();
                List<RemoteText> remoteTexts = argumentsTree.getData().getAll();
                remoteTexts.get(remoteTexts.size() - 1).click();

                keyboard.enterText("method2");
                keyboard.hotKey(VK_ENTER);
            }
            idea.getDirectInvokeExecuteButtonNew().click();
            pause(ofSeconds(3).toMillis());

            ComponentFixture responseTree = idea.getTree();
            List<RemoteText> remoteTexts = responseTree.getData().getAll();
            RemoteText responseValue = remoteTexts.get(remoteTexts.size() - 1);

            Assertions.assertEquals(responseExpected, responseValue.getText());
        });
        pause(ofSeconds(10).toMillis());

        ComponentFixture terminalToolbar = idea.getTerminalToolBarSelectable();
        terminalToolbar.click();

        ComponentFixture terminalContents = idea.getTerminalPanel();
        RemoteText lastText = terminalContents.getData().getAll().get(terminalContents.getData().getAll().size() - 1);
        lastText.click();

        keyboard.hotKey(VK_CONTROL, VK_C);
        pause(ofSeconds(10).toMillis());

        idea.getTerminalToolWindowHideButton().click();
        //restart and repeat in local mode
        openAndRevertGitChangesForFile("UnloggedDemoApplication", idea);
        addUnloggedToStartFile("UnloggedDemoApplication", idea, "@Unlogged");

        openFile("start_project.sh", idea);
        shellScript = idea.textEditor();
        started = false;
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

        idea.getTerminalToolWindowHideButton().click();

        idea.getFilterButton().click();
        pause(ofMillis(250).toMillis());

        titlePanel = idea.getMyContentPanel();
        sourcesTabText = titlePanel.getData().getAll().stream().filter(remoteText -> remoteText.getText().equals("Sources")).toList().get(0);
        sourcesTabText.click();
        idea.getLocalHostRadioButton().click();
        idea.getFilterApplyButton().click();

        idea.getRefreshButton().click();

        openFile("FutureController.java", idea);
        pause(ofSeconds(2).toMillis());
        expandJavaFile(idea.textEditor().getEditor());
        clearGotIts(idea);

        editor = idea.textEditor(Duration.ofSeconds(2));
        gutterIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList();

        lineNumbers = new ArrayList<>(gutterIcons.stream().map(GutterIcon::getLineNumber).toList());
        Collections.sort(lineNumbers);

        idea.getToolBarDeleteButton().click();
        TextEditorFixture finalEditor1 = editor;
        List<Integer> finalLineNumbers1 = lineNumbers;
        gutterIcons.forEach(icon -> {
            scrollToIcon(finalEditor1, icon);
            icon.click();
            pause(ofMillis(250).toMillis());

            String responseExpected = "String: string";
            idea.getGoToDirectInvokeButton().click();
            pause(ofMillis(250).toMillis());

            if (icon.getLineNumber() == finalLineNumbers1.get(0)) {
                responseExpected = "String: yolo";
            } else {
                responseExpected = "String: method2";
                ComponentFixture argumentsTree = idea.getTree();
                List<RemoteText> remoteTexts = argumentsTree.getData().getAll();
                remoteTexts.get(remoteTexts.size() - 1).click();

                keyboard.enterText("method2");
                keyboard.hotKey(VK_ENTER);
            }
            idea.getDirectInvokeExecuteButtonNew().click();
            pause(ofSeconds(3).toMillis());

            ComponentFixture responseTree = idea.getTree();
            List<RemoteText> remoteTexts = responseTree.getData().getAll();
            RemoteText responseValue = remoteTexts.get(remoteTexts.size() - 1);

            Assertions.assertEquals(responseExpected, responseValue.getText());
        });
        pause(ofSeconds(10).toMillis());
    }

    private void addUnloggedToStartFile(String filename, IdeaFrame ideaFrame, String annotationText) {
        openFile(filename, ideaFrame);
        TextEditorFixture textEditorFixture = ideaFrame.textEditor();
        expandJavaFile(textEditorFixture.getEditor());

        List<RemoteText> mainClassContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText firstSemicolon = mainClassContents.stream().filter(text -> text.getText().equals(";")).toList().get(0);
        firstSemicolon.click();
        keyboard.hotKey(VK_RIGHT);
        keyboard.hotKey(VK_ENTER);
        keyboard.enterText("import io.unlogged.Unlogged;");

        //refresh contents post text addition
        mainClassContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText mainLabel = mainClassContents.stream().filter(text -> text.getText().equals("main")).toList().get(0);
        int mainLabelIndex = mainClassContents.indexOf(mainLabel);
        RemoteText spaceLabel = mainClassContents.get(mainLabelIndex - 7);
        spaceLabel.click();
        keyboard.hotKey(VK_ENTER);
        keyboard.enterText(annotationText);
        keyboard.hotKey(VK_ENTER);
    }

    private void openAndRevertGitChangesForFile(String filename, IdeaFrame ideaFrame) {
        openFile(filename, ideaFrame);
        pause(ofMillis(500).toMillis());

        TextEditorFixture textEditorFixture = ideaFrame.textEditor();
        RemoteText text = textEditorFixture.getEditor().getData().getAll().get(0);
        text.click();

        keyboard.hotKey(VK_ALT, VK_META, VK_Z);
        pause(ofMillis(250).toMillis());
        ideaFrame.getGitRollbackButton().click();
        pause(ofSeconds(1).toMillis());
    }

    @Test
    public void testFullFlow() {
        final WelcomeFrame welcomeFrame = remoteRobot.find(WelcomeFrame.class, ofSeconds(10));
        welcomeFrame.getOpenProjectButton().click();

        welcomeFrame.getProjectSelectorComboBox().click();
        String projectName = "unlogged-spring-maven-demo";

        keyboard.enterText("/" + projectName);
        pause(ofSeconds(1).toMillis());
        welcomeFrame.getOpenConfirmButton().click();

        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(10), () -> !idea.isDumbMode());

        //wait for a small duration so that file searches don't turn up empty
        pause(ofSeconds(30).toMillis());

        //for mac, open pom.xml
        openFile("pom.xml", idea);

        //open unlogged toolbar, copy and paste dependency
        ComponentFixture unloggedToolbar = idea.getUnloggedToolbarComponent();
        unloggedToolbar.moveMouse();
        unloggedToolbar.click();
        pause(ofSeconds(1).toMillis());

        ComponentFixture copyButton = idea.findCopyButton();
        copyButton.moveMouse();
        copyButton.click();

        //paste right after dependencies
        TextEditorFixture textEditorFixture = idea.textEditor();
        List<RemoteText> pomContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText dependencyText = pomContents.stream().filter(remoteText -> remoteText.getText().equals("dependencies")).toList().get(0);
        int indexOfDependencies = pomContents.indexOf(dependencyText);
        RemoteText closingTag = pomContents.get(indexOfDependencies + 1);
        closingTag.click();
        keyboard.hotKey(VK_RIGHT);
        keyboard.hotKey(VK_ENTER);
        keyboard.hotKey(VK_META, VK_V);

        //sync and add @unlogged over main method
        openFile("UnloggedDemoApplication", idea);
        textEditorFixture = idea.textEditor();

        expandJavaFile(textEditorFixture.getEditor());

        List<RemoteText> mainClassContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText firstSemicolon = mainClassContents.stream().filter(text -> text.getText().equals(";")).toList().get(0);
        firstSemicolon.click();
        keyboard.hotKey(VK_RIGHT);
        keyboard.hotKey(VK_ENTER);
        keyboard.enterText("import io.unlogged.Unlogged;");

        //refresh contents post text addition
        mainClassContents = textEditorFixture.getEditor().getData().getAll();
        RemoteText mainLabel = mainClassContents.stream().filter(text -> text.getText().equals("main")).toList().get(0);
        int mainLabelIndex = mainClassContents.indexOf(mainLabel);
        RemoteText spaceLabel = mainClassContents.get(mainLabelIndex - 7);
        spaceLabel.click();
        keyboard.hotKey(VK_ENTER);
        keyboard.enterText("@Unlogged");
        keyboard.hotKey(VK_ENTER);

        //refresh maven before proceeding
        ComponentFixture mavenIcon = idea.getMavenToolbarIcon();
        mavenIcon.click();
        pause(ofSeconds(1).toMillis());
        idea.getMavenToolBarRefreshIcon().click();
        pause(ofSeconds(1).toMillis());
        mavenIcon.click();

        //start application
        openFile("start_project.sh", idea);
        TextEditorFixture shellScript = idea.textEditor();
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

        //wait for 2 mins - or how much time docker compose would take
        //todo - hook to terminal stdout and search for start indicator
        pause(ofSeconds(70).toMillis());
        clearGotIts(idea);

        //Ensure that the status shows "Connected"
        try {
            ComponentFixture toolBarNav = idea.getUnloggedToolbarComponent();
            toolBarNav.click();
            pause(ofSeconds(5).toMillis());
            ComponentFixture connectedLabel = idea.getConnectedLabel();
            toolBarNav.click();
        } catch (Exception e) {
            Assertions.assertTrue(false);
        }
        idea.getTerminalToolWindowHideButton().click();
        //call prep1
        prep_TC2(idea);

        //call test
        test_TC2(idea);

        restart_test(idea);

        idea.getJunitTopToolbarIcon().click();
        pause(ofSeconds(2).toMillis());

        Assertions.assertTrue(true);
    }

    private void clearGotIts(IdeaFrame idea) {
        List<ComponentFixture> gotItTexts = idea.getGotItTexts();
        gotItTexts.forEach(text -> text.click());
    }

    private void prep_TC2(IdeaFrame idea) {

        openFile("UiTestPrepHelper", idea);
        expandJavaFile(idea.textEditor().getEditor());

        //clear got its
        clearGotIts(idea);


        //one more round of got it clear
        clearGotIts(idea);

        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        GutterIcon addDataGutter = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList().get(0);

        scrollToIcon(editor, addDataGutter);
        addDataGutter.click();

        idea.getGoToDirectInvokeButton().click();
        idea.getDirectInvokeExecuteButtonNew().click();
        pause(ofSeconds(5).toMillis());

        clearGotIts(idea);
    }

    public void test_TC2(IdeaFrame idea) {

        openFile("UiTestEntryClass", idea);
        CustomGutterIconComparator iconComparator = new CustomGutterIconComparator();
        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        expandJavaFile(editor.getEditor());
        editor = idea.textEditor(Duration.ofSeconds(2));

        GutterIcon mainIcon = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList().get(0);

        scrollToIcon(editor, mainIcon);

        List<GutterIcon> unloggedMockIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg"))
                .toList();
        List<GutterIcon> mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        assert mockIcons.size() == 10;

        for (int i = 0; i < mockIcons.size(); i++) {
            GutterIcon mockIcon = mockIcons.get(i);
            scrollToIcon(editor, mockIcon);
            mockIcon.moveMouse();
            mockIcon.click();
            pause(ofMillis(250).toMillis());

            //click on create
            ComponentFixture createNewMockButton = idea.getCreateNewMockButton();
            createNewMockButton.moveMouse();
            createNewMockButton.click();

            ComponentFixture closeButton = idea.getMockPopupCloseButton();
            closeButton.moveMouse();
            closeButton.click();
            pause(ofMillis(500).toMillis());

            //edit the then parameter ->
            switch (i) {
                case 0:
                    Map<Integer, String> interactionMap = new TreeMap<>();
                    interactionMap.put(2, "balgruf");
                    interactWithMockEditPanel("db call mock", interactionMap, idea, "default");
                    break;
                case 1:
                    interactionMap = new TreeMap<>();
                    interactionMap.put(0, "0.05");
                    interactWithMockEditPanel("discount rate mock", interactionMap, idea, "default");
                    break;
                case 2:
                    interactionMap = new TreeMap<>();
                    interactionMap.put(0, "0.5");
                    interactWithMockEditPanel("max discount mock", interactionMap, idea, "default");
                    break;
                case 3:
                    interactionMap = new TreeMap<>();
                    interactionMap.put(0, "500");
                    interactWithMockEditPanel("delivery cost mock", interactionMap, idea, "default");
                    break;
                case 4:
                    interactionMap = new TreeMap<>();
                    interactionMap.put(2, "windhelm");
                    interactionMap.put(3, "central skyrim");
                    interactWithMockEditPanel("weather api mock", interactionMap, idea, "default");
                    break;
                case 5:
                    interactionMap = new TreeMap<>();
                    interactWithMockEditPanel("redis call mock 1", interactionMap, idea, "default");
                    break;
                case 6:
                    interactionMap = new TreeMap<>();
                    interactWithMockEditPanel("redis add call mock", interactionMap, idea, "default");
                    break;
                case 7:
                    interactionMap = new TreeMap<>();
                    interactWithMockEditPanel("null return mock", interactionMap, idea, "null");
                    break;
                case 8:
                    interactionMap = new TreeMap<>();
                    interactWithMockEditPanel("error return mock", interactionMap, idea, "error");
                    break;
                case 9:
                    interactionMap = new TreeMap<>();
                    interactionMap.put(0, "false");
                    interactWithMockEditPanel("file write mock", interactionMap, idea, "default");
                    break;
                default:
                    break;
            }

            pause(ofMillis(250).toMillis());
            ComponentFixture mockEditPanelSaveButton = idea.getMockEditSaveButton();
            mockEditPanelSaveButton.moveMouse();
            mockEditPanelSaveButton.click();
            pause(ofMillis(250).toMillis());

            //re-open mock popup and make sure newly saved mocks are available
            mockIcon.moveMouse();
            mockIcon.click();
            pause(ofMillis(250).toMillis());

            closeButton = idea.getMockPopupCloseButton();
            closeButton.moveMouse();
            closeButton.click();
            pause(ofMillis(250).toMillis());
        }

        //DirectInvoke the method
        scrollToIcon(editor, mainIcon);
        mainIcon.moveMouse();
        mainIcon.click();

        idea.getDirectInvokeExecuteButtonNew().click();
        pause(ofSeconds(5).toMillis());

        boolean found = false;
        int tries = 3;
        ContainerFixture responseTreeFixture = null;
        while (tries >= 0 && !found) {
            try {
                responseTreeFixture = idea.getContainerByXpath("//div[@class='Tree']");
                found = true;
            } catch (Exception e) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }

        //clear got its
        clearGotIts(idea);

        //record the output for verification
        List<RemoteText> remoteTexts = responseTreeFixture.getData().getAll();
        boolean passing = true;
        for (int i = 0; i < remoteTexts.size(); i++) {
            RemoteText currentText = remoteTexts.get(i);
            switch (i) {
                case 1:
                    if (!currentText.getText().equals("customerId: 0")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                case 2:
                    if (!currentText.getText().equals("productCost: 1000.0")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                case 3:
                    if (!currentText.getText().equals("deliveryCost: 500.0")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                case 4:
                    if (!currentText.getText().equals("totalAmount: 550.0")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                case 5:
                    if (!currentText.getText().equals("reportStatus: false")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                case 6:
                    if (!currentText.getText().equals("region: central skyrim")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                case 7:
                    if (!currentText.getText().equals("groupId: null")) {
                        System.out.println("Assertion " + i);
                        System.out.println("Current text : " + currentText.getText());
                        passing = false;
                    }
                    break;
                case 8:
                    if (!currentText.getText().startsWith("errorStatus: class java.lang.String cannot be cast to class java.lang.Throwable")) {
                        System.out.println("Assertion " + i);
                        passing = false;
                    }
                    break;
                default:
                    break;
            }
        }
        Assertions.assertTrue(passing);
        //assertion done


        //clear liveView
        clearGotIts(idea);
        ComponentFixture deleteButton = idea.getToolBarDeleteButton();
        deleteButton.click();

        //reload mock icons -> needed to ensure proper coordinates are taken
        editor = idea.textEditor(Duration.ofSeconds(2));
        unloggedMockIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg"))
                .toList();
        mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        clearGotIts(idea);

        for (int i = 0; i < mockIcons.size(); i++) {
            clearGotIts(idea);
            GutterIcon mockIcon = mockIcons.get(i);
            scrollToIcon(editor, mockIcon);
            mockIcon.moveMouse();
            mockIcon.click();
            pause(ofMillis(250).toMillis());

            List<ComponentFixture> mockScrollCandidates = idea.getMockPopupScrollPanelCandidates();
            ComponentFixture mockScrollPanel = null;
            if (mockScrollCandidates.size() > 1) {
                mockScrollPanel = mockScrollCandidates.get(1);
            } else {
                mockScrollPanel = idea.getMockPopupScrollPanel();
            }
            List<RemoteText> mockEntries = mockScrollPanel.getData().getAll();
            assert mockEntries.size() > 1;

            idea.getToolBarDeleteButton().click();
            pause(ofMillis(250).toMillis());

            List<ComponentFixture> checkBoxes = idea.getAllVisibleCheckBoxes();
            checkBoxes.forEach(checkBox -> {
                checkBox.moveMouse();
                checkBox.click();
                pause(ofMillis(250).toMillis());
            });

            ComponentFixture unmockButton = idea.getUnlinkMockButton();
            unmockButton.click();

            List<MockPopupEntry> mockPopupEntries = new ArrayList<>();
            //alt make this start with mock
            if (mockEntries.size() % 3 == 0) {
                int index = 0;
                for (int x = 0; x < mockEntries.size() / 3; x++) {
                    RemoteText mockname = mockEntries.get(index++);
                    RemoteText returnTypeText = mockEntries.get(index++);
                    RemoteText methodNameText = mockEntries.get(index++);
                    MockPopupEntry mpe = new MockPopupEntry(mockname, returnTypeText, methodNameText);
                    ComponentFixture panelFixture = idea.getComponentByXpath(mpe.getPanelXpath());
                    mpe.setPanel(panelFixture);
                    mockPopupEntries.add(mpe);
                }
            }

            ComponentFixture closeButton = idea.getMockPopupCloseButton();
            closeButton.moveMouse();
            closeButton.click();
            pause(ofMillis(250).toMillis());
        }

        mainIcon = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList().get(0);
        scrollToIcon(editor, mainIcon);
        mainIcon.moveMouse();
        mainIcon.click();
        pause(ofMillis(250).toMillis());

        idea.getDirectInvokeExecuteButtonNew().click();
        pause(ofSeconds(5).toMillis());

        found = false;
        tries = 3;
        responseTreeFixture = null;
        while (tries >= 0 && !found) {
            try {
                responseTreeFixture = idea.getContainerByXpath("//div[@class='Tree']");
                found = true;
            } catch (Exception e) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }

        //record the output for verification
        remoteTexts = responseTreeFixture.getData().getAll();
        passing = true;
        for (int i = 0; i < remoteTexts.size(); i++) {
            RemoteText currentText = remoteTexts.get(i);
            switch (i) {
                case 1:
                    if (!currentText.getText().equals("customerId: 1")) {
                        passing = false;
                    }
                    break;
                case 2:
                    if (!currentText.getText().equals("productCost: 1000.0")) {
                        passing = false;
                    }
                    break;
                case 3:
                    if (!currentText.getText().equals("deliveryCost: 220.0")) {
                        passing = false;
                    }
                    break;
                case 4:
                    if (!currentText.getText().equals("totalAmount: 820.0000000000001")) {
                        passing = false;
                    }
                    break;
                case 5:
                    if (!currentText.getText().equals("reportStatus: true")) {
                        passing = false;
                    }
                    break;
                case 6:
                    if (!currentText.getText().equals("region: Mashonaland East")) {
                        passing = false;
                    }
                    break;
                case 7:
                    if (!currentText.getText().equals("groupId: default-1")) {
                        passing = false;
                    }
                    break;
                case 8:
                    if (!currentText.getText().startsWith("errorStatus: no error")) {
                        passing = false;
                    }
                    break;
                default:
                    break;
            }
        }
        Assertions.assertTrue(passing);

        //clear liveView
        clearGotIts(idea);
        deleteButton = idea.getToolBarDeleteButton();
        deleteButton.click();

        //rename and delete mocks
        //open library
        ComponentFixture libraryTabHeader = idea.getlibraryTabHeader();
        libraryTabHeader.click();

        //keep the mocks tab open
        try {
            ComponentFixture cf = idea.getComponentByXpath("//div[@text='Mocks']");
            cf.click();
        } catch (Exception e) {
            TextEditorFixture tef = idea.textEditor();
            List<RemoteText> remoteTexts1 = tef.getEditor().getData().getAll();
            List<RemoteText> mockInlayHints = remoteTexts1.stream()
                    .filter(remoteText -> remoteText.getText().equals("1 saved mock")).toList();
            mockInlayHints.get(0).click();
            pause(ofMillis(250).toMillis());

            ComponentFixture clearFiltersText = idea.getComponentByXpath("//div[@visible_text='Clear filters']");
            pause(ofMillis(250).toMillis());
            clearFiltersText.click();
        }

        ComponentFixture firstMockEditButton = idea.getComponentByXpath("//div[@accessiblename='db call mock']//div[@class='ActionButton']");
        firstMockEditButton.click();

        interactWithMockEditPanel("db mock renamed", new TreeMap<>(), idea, "default");

        pause(ofMillis(250).toMillis());
        ComponentFixture mockEditPanelSaveButton = idea.getMockEditSaveButton();
        mockEditPanelSaveButton.moveMouse();
        mockEditPanelSaveButton.click();
        pause(ofMillis(250).toMillis());

        ComponentFixture refreshButton = idea.getRefreshButton();
        refreshButton.click();

        //confirm name changed
        ComponentFixture renamedMockEntry = idea.getComponentByXpath("//div[@accessiblename='db mock renamed']");
        //Exists if a timeout error is not thrown.

        //select all and delete all mocks
        ComponentFixture selectAllToolbarButton = idea.getSelectAllicon();
        selectAllToolbarButton.click();
        pause(ofMillis(500).toMillis());

        ComponentFixture deleteToolbarButton = idea.getToolBarDeleteButton();
        deleteToolbarButton.click();
        pause(ofMillis(500).toMillis());

        //click on delete confirmation
        idea.getComponentByXpath("//div[@text='OK']").click();
        pause(ofMillis(500).toMillis());

        boolean mockExists = true;
        try {
            renamedMockEntry = idea.getComponentByXpath("//div[@accessiblename='db mock renamed']");
        } catch (Exception e) {
            mockExists = false;
        }
        Assertions.assertFalse(mockExists);
        //switch to live view
        idea.getLiveTabHeader().click();
        //save all candidates in the end
        idea.getRefreshButton().click();
        pause(ofSeconds(2).toMillis());
        tryToSaveAll(idea);
        pause(ofSeconds(2).toMillis());
    }

    private void scrollToIcon(TextEditorFixture editorFixture, GutterIcon icon) {
        Integer startingOffset = editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineStartOffset(" + (icon.getLineNumber()) + ")", true);
        editorFixture.getEditor().scrollToOffset(startingOffset);
        System.out.println("Scrolling down to line number : " + icon.getLineNumber() + ", Offset : " + startingOffset);
    }

    private void interactWithMockEditPanel(String mockname, Map<Integer, String> subValues, IdeaFrame ideaFrame, String type) {
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

    private void restart_test(IdeaFrame ideaFrame) {
        ComponentFixture terminalToolbar = ideaFrame.getTerminalToolBarSelectable();
        terminalToolbar.click();

        List<RemoteText> remoteTexts = ideaFrame.getShellWidget().getData().getAll();
        RemoteText lastText = remoteTexts.get(remoteTexts.size() - 1);
        lastText.click();
        //stop the running process
        keyboard.hotKey(VK_CONTROL, VK_C);
        //wait till process stops
        pause(ofSeconds(7).toMillis());

        //make sure Disconnected is seen
        try {
            ComponentFixture disconnectedLabel = ideaFrame.getDisconnectedLabel();
        } catch (Exception e) {
            assert false;
        }

        openFile("start_project.sh", ideaFrame);

        TextEditorFixture shellScript = ideaFrame.textEditor();
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

        pause(ofSeconds(60).toMillis());
        //make sure Connected is seen
        try {
            ComponentFixture connectedLabel = ideaFrame.getConnectedLabel();
        } catch (Exception e) {
            assert false;
        }

        Assertions.assertTrue(true);
    }

    private void tryToSaveAll(IdeaFrame idea) {
        ComponentFixture selectAllIcon = idea.getSelectAllicon();
        pause(ofMillis(500).toMillis());
        selectAllIcon.click();
        clearGotIts(idea);
        idea.getSaveGlobalButton().click();
        pause(ofSeconds(20).toMillis());

        boolean found = false;
        int tries = 3;
        ComponentFixture confirmSaveButton = null;
        while (tries >= 0 && !found) {
            try {
                confirmSaveButton = idea.getSaveFromConfirmButton();
                found = true;
            } catch (Exception e) {
                tries--;
                pause(ofSeconds(1).toMillis());
            }
        }
        confirmSaveButton.click();
    }

    private void openFile(String filename, IdeaFrame ideaFrame) {
        keyboard.hotKey(VK_META, VK_SHIFT, VK_O);
        keyboard.enterText(filename);
        keyboard.hotKey(VK_ENTER);
    }

    private void expandJavaFile(EditorFixture editor) {
        for (RemoteText text : editor.getData().getAll()) {
            if (text.getText().equals("...")) {
                text.click();
            }
        }
    }
}
