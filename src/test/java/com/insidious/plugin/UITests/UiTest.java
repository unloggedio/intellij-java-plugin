package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.CustomGutterIconComparator;
import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.pages.WelcomeFrame;
import com.insidious.plugin.UITests.wrapper.MethodWiseGutterIcons;
import com.insidious.plugin.UITests.wrapper.MockPopupEntry;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
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


    @Test
    public void testOnSuiteFile()
    {
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(4), () -> !idea.isDumbMode());


        CustomGutterIconComparator iconComparator = new CustomGutterIconComparator();
        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        editor.getEditor().scrollToOffset(1);
        //pick based on open file
        List<GutterIcon> icons = editor.getGutter().getIcons();

        List<GutterIcon> unloggedAccessIcons = icons.stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList();

        List<GutterIcon> mainIcons = new ArrayList<>(unloggedAccessIcons);
        Collections.sort(mainIcons, iconComparator);

        List<GutterIcon> unloggedMockIcons = icons.stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg"))
                .toList();
        List<GutterIcon> mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        System.out.println("Main icons : ");
        mainIcons.forEach(icon -> System.out.println("(M) -> : " + icon.toString()));
        System.out.println("Mock icons : ");
        mockIcons.forEach(icon -> System.out.println("(m) -> : " + icon.toString()));

        List<MethodWiseGutterIcons> methodWiseGutterIconsList = new ArrayList<>();
        for (int i = 0; i < mainIcons.size(); i++) {
            GutterIcon mainIcon = mainIcons.get(i);
            scrollDownToIcon(editor, mainIcon);
            pause(ofSeconds(2).toMillis());
            List<GutterIcon> subMockIcons = new ArrayList<>();
            if (i + 1 < mainIcons.size()) {
                int nextIconStart = mainIcons.get(i + 1).getLineNumber();
                subMockIcons = mockIcons.stream()
                        .filter(mockIcon -> mockIcon.getLineNumber() >= mainIcon.getLineNumber()
                                && mockIcon.getLineNumber() <= nextIconStart).toList();
            } else {
                subMockIcons = mockIcons.stream()
                        .filter(mockIcon -> mockIcon.getLineNumber() >= mainIcon.getLineNumber())
                        .toList();
            }

            MethodWiseGutterIcons currentMethodIcons = new MethodWiseGutterIcons(mainIcon, subMockIcons);
            methodWiseGutterIconsList.add(currentMethodIcons);

            //click on each mockable icon and make sure there's nothing them
            subMockIcons.stream().forEach(ghostIcon -> {
                pause(ofMillis(500).toMillis());
                scrollDownToIcon(editor, ghostIcon);
                pause(ofSeconds(1).toMillis());
                System.out.println("Clicking Ghost Icon (1) : " + ghostIcon.toString());
                ghostIcon.click();
                pause(ofSeconds(1).toMillis());
                try {
                    ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
                    List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
                    assert remoteTexts.isEmpty();
                } catch (WaitForConditionTimeoutException timeoutException) {
                    assert true;
                }
                //keep closing this recursively until all are gone (strange ui behaviour)
                ComponentFixture closeButton = idea.getMockPopupCloseButton();
                closeButton.moveMouse();
                pause(ofSeconds(1).toMillis());
                closeButton.click();
                pause(ofMillis(500).toMillis());
            });


            scrollDownToIcon(editor, mainIcon);
            //click on main icon and directInvoke
            currentMethodIcons.getMainGutterIcon().click();
            pause(ofSeconds(1).toMillis());

            idea.getDirectInvokeExecuteButtonNew().click();
            pause(ofSeconds(10).toMillis());

            //now ensure that there are new mocks for mockable method
            subMockIcons.stream().forEach(ghostIcon -> {
                pause(ofMillis(500).toMillis());
                scrollDownToIcon(editor, ghostIcon);
                pause(ofSeconds(1).toMillis());
                System.out.println("Clicking Ghost Icon (2) : " + ghostIcon.toString());
                ghostIcon.moveMouse();
                pause(ofMillis(500).toMillis());
                ghostIcon.click();

                pause(ofSeconds(2).toMillis());

                ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
                List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
                assert remoteTexts.size() > 1;
                List<MockPopupEntry> mockPopupEntries = new ArrayList<>();
                //alt make this start with mock
                if (remoteTexts.size() % 3 == 0) {
//                    System.out.println("Correct format");
                    int index = 0;
                    for (int x = 0; x < remoteTexts.size() / 3; x++) {
                        RemoteText mockname = remoteTexts.get(index++);
                        RemoteText returnTypeText = remoteTexts.get(index++);
                        RemoteText methodNameText = remoteTexts.get(index++);
                        MockPopupEntry mpe = new MockPopupEntry(mockname, returnTypeText, methodNameText);
                        ComponentFixture panelFixture = idea.getComponentByXpath(mpe.getPanelXpath());
                        mpe.setPanel(panelFixture);
                        mockPopupEntries.add(mpe);
                    }
                }

                ComponentFixture closeButton = idea.getMockPopupCloseButton();
                closeButton.moveMouse();
                pause(ofSeconds(1).toMillis());
                closeButton.click();
                pause(ofMillis(500).toMillis());
            });
        }


    }

    //Expecting project to be open already
    @Test
    public void testOnboardingFlowOnFreshProject() {
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        waitFor(ofMinutes(5), () -> !idea.isDumbMode());

        //Open a java file, search and click on a gutter icon
        //You should see a message asking you to start the application
        //Click on done from onboarding
        //You should see a blank screen/write after temp setup screen is incorporated
        //Start application and wait for 20 seconds (depends on project)
        //the view should automatically change to Live view
        //pass the case here
    }

    //keep open - DeliveryService for this.
    //expand the imports section
    //Start with fresh session
    //Go mock by mock
    //Make sure they are empty, click on create new
    //Close popup and save
    //repoen popup, you should have entires.
    @Test
    public void testMockCreationFlow() {

        IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));


        try {
            ComponentFixture mockSelectionPanel = idea.getMockMultiSelectPanel();
            //selection panel is up
//            ComponentFixture parentPanel = idea.get("//div[@class='MyContentPanel']");
//            parentPanel.getData();
        } catch (WaitForConditionTimeoutException timeoutException) {
            System.out.println("No Multi Select panel");
        }

        try {
            //selection panel is up
            ComponentFixture parentPanel = idea.getComponentByXpath("//div[@class='MyContentPanel']");
        } catch (WaitForConditionTimeoutException timeoutException) {
            System.out.println("No Multi Select panel 2");
        }

        try {
            //selection panel is up
            ComponentFixture pp = idea.getComponentByXpath("//div[@name='null.contentPane']");
        } catch (WaitForConditionTimeoutException timeoutException) {
            System.out.println("No Multi Select panel 2");
        }

//        if (true) {
//            return;
//        }

        CustomGutterIconComparator iconComparator = new CustomGutterIconComparator();
//        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        editor.getEditor().scrollToOffset(1);
        //pick based on open file
        List<GutterIcon> unloggedMockIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg"))
                .toList();
        List<GutterIcon> mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        System.out.println("Mock icons : ");
        mockIcons.forEach(icon -> System.out.println("(m) -> : " + icon.toString()));
        for (GutterIcon mockIcon : mockIcons) {
            scrollDownToIcon(editor, mockIcon);
            pause(ofMillis(500).toMillis());
            mockIcon.moveMouse();
            pause(ofMillis(250).toMillis());
            mockIcon.click();
            pause(ofSeconds(2).toMillis());

            //assert empty
            try {
                ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
                List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
                assert remoteTexts.isEmpty();
            } catch (WaitForConditionTimeoutException timeoutException) {
                assert true;
            }

            //check for multi mock selection panel
            try {
                ComponentFixture mockSelectionPanel = idea.getMockMultiSelectPanel();
            } catch (WaitForConditionTimeoutException timeoutException) {

            }

            //click on create
            ComponentFixture createNewMockButton = idea.getCreateNewMockButton();
            createNewMockButton.moveMouse();
            pause(ofMillis(250).toMillis());
            createNewMockButton.click();

            ComponentFixture closeButton = idea.getMockPopupCloseButton();
            closeButton.moveMouse();
            pause(ofMillis(500).toMillis());
            closeButton.click();

            pause(ofSeconds(2).toMillis());

            ComponentFixture mockEditPanelSaveButton = idea.getMockEditSaveButton();
            mockEditPanelSaveButton.moveMouse();
            pause(ofMillis(250).toMillis());
            mockEditPanelSaveButton.click();
            pause(ofSeconds(5).toMillis());

            //reopen mock popup and make sure newly saved mocks are available
            mockIcon.moveMouse();
            pause(ofSeconds(1).toMillis());
            mockIcon.click();
            pause(ofSeconds(2).toMillis());

            ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
            List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
            assert remoteTexts.size() > 1;
            List<MockPopupEntry> mockPopupEntries = new ArrayList<>();
            //alt make this start with mock
            if (remoteTexts.size() % 3 == 0) {
                int index = 0;
                for (int x = 0; x < remoteTexts.size() / 3; x++) {
                    RemoteText mockname = remoteTexts.get(index++);
                    RemoteText returnTypeText = remoteTexts.get(index++);
                    RemoteText methodNameText = remoteTexts.get(index++);
                    MockPopupEntry mpe = new MockPopupEntry(mockname, returnTypeText, methodNameText);
                    ComponentFixture panelFixture = idea.getComponentByXpath(mpe.getPanelXpath());
                    mpe.setPanel(panelFixture);
                    mockPopupEntries.add(mpe);
                }
            }

            closeButton = idea.getMockPopupCloseButton();
            closeButton.moveMouse();
            pause(ofSeconds(1).toMillis());
            closeButton.click();
            pause(ofMillis(500).toMillis());

        }
    }

    //keep open - DeliveryService for this.
    //expand the imports section
    //Start with fresh session
    //Go method by method and click on each mock gutter icon
    //Make sure they are empty, DirectInvoke each method in the file and see if you have entries.
    //you should have entires.
    @Test
    public void testMockingFlow() {

        CustomGutterIconComparator iconComparator = new CustomGutterIconComparator();
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));
        editor.getEditor().scrollToOffset(1);
        //pick based on open file
        List<GutterIcon> icons = editor.getGutter().getIcons();

        List<GutterIcon> unloggedAccessIcons = icons.stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg"))
                .toList();

        List<GutterIcon> mainIcons = new ArrayList<>(unloggedAccessIcons);
        Collections.sort(mainIcons, iconComparator);

        List<GutterIcon> unloggedMockIcons = icons.stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg"))
                .toList();
        List<GutterIcon> mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        System.out.println("Main icons : ");
        mainIcons.forEach(icon -> System.out.println("(M) -> : " + icon.toString()));
        System.out.println("Mock icons : ");
        mockIcons.forEach(icon -> System.out.println("(m) -> : " + icon.toString()));

        List<MethodWiseGutterIcons> methodWiseGutterIconsList = new ArrayList<>();
        for (int i = 0; i < mainIcons.size(); i++) {
            GutterIcon mainIcon = mainIcons.get(i);
            scrollDownToIcon(editor, mainIcon);
            pause(ofSeconds(2).toMillis());
            List<GutterIcon> subMockIcons = new ArrayList<>();
            if (i + 1 < mainIcons.size()) {
                int nextIconStart = mainIcons.get(i + 1).getLineNumber();
                subMockIcons = mockIcons.stream()
                        .filter(mockIcon -> mockIcon.getLineNumber() >= mainIcon.getLineNumber()
                                && mockIcon.getLineNumber() <= nextIconStart).toList();
            } else {
                subMockIcons = mockIcons.stream()
                        .filter(mockIcon -> mockIcon.getLineNumber() >= mainIcon.getLineNumber())
                        .toList();
            }

            MethodWiseGutterIcons currentMethodIcons = new MethodWiseGutterIcons(mainIcon, subMockIcons);
            methodWiseGutterIconsList.add(currentMethodIcons);

            //click on each mockable icon and make sure there's nothing them
            subMockIcons.stream().forEach(ghostIcon -> {
                pause(ofMillis(500).toMillis());
                scrollDownToIcon(editor, ghostIcon);
                pause(ofSeconds(1).toMillis());
                System.out.println("Clicking Ghost Icon (1) : " + ghostIcon.toString());
                ghostIcon.click();
                pause(ofSeconds(1).toMillis());
                try {
                    ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
                    List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
                    assert remoteTexts.isEmpty();
                } catch (WaitForConditionTimeoutException timeoutException) {
                    assert true;
                }
                //keep closing this recursively until all are gone (strange ui behaviour)
                ComponentFixture closeButton = idea.getMockPopupCloseButton();
                closeButton.moveMouse();
                pause(ofSeconds(1).toMillis());
                closeButton.click();
                pause(ofMillis(500).toMillis());
            });


            scrollDownToIcon(editor, mainIcon);
            //click on main icon and directInvoke
            currentMethodIcons.getMainGutterIcon().click();
            pause(ofSeconds(1).toMillis());

            idea.getDirectInvokeExecuteButtonNew().click();
            pause(ofSeconds(10).toMillis());

            //now ensure that there are new mocks for mockable method
            subMockIcons.stream().forEach(ghostIcon -> {
                pause(ofMillis(500).toMillis());
                scrollDownToIcon(editor, ghostIcon);
                pause(ofSeconds(1).toMillis());
                System.out.println("Clicking Ghost Icon (2) : " + ghostIcon.toString());
                ghostIcon.moveMouse();
                pause(ofMillis(500).toMillis());
                ghostIcon.click();

                pause(ofSeconds(2).toMillis());

                ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
                List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
                assert remoteTexts.size() > 1;
                List<MockPopupEntry> mockPopupEntries = new ArrayList<>();
                //alt make this start with mock
                if (remoteTexts.size() % 3 == 0) {
//                    System.out.println("Correct format");
                    int index = 0;
                    for (int x = 0; x < remoteTexts.size() / 3; x++) {
                        RemoteText mockname = remoteTexts.get(index++);
                        RemoteText returnTypeText = remoteTexts.get(index++);
                        RemoteText methodNameText = remoteTexts.get(index++);
                        MockPopupEntry mpe = new MockPopupEntry(mockname, returnTypeText, methodNameText);
                        ComponentFixture panelFixture = idea.getComponentByXpath(mpe.getPanelXpath());
                        mpe.setPanel(panelFixture);
                        mockPopupEntries.add(mpe);
                    }
                }

                ComponentFixture closeButton = idea.getMockPopupCloseButton();
                closeButton.moveMouse();
                pause(ofSeconds(1).toMillis());
                closeButton.click();
                pause(ofMillis(500).toMillis());
            });
        }
    }

    @Test
    public void testFullFlow() {
        //How the test will go -

        //Go file by file, method by method
        //Before clicking on the gutter, track a list of candidate visible in candidate view on live traffic tab

        //Step : DirectInvoke
        //Click on method gutter icon and DirectInvoke
        //Once you get the response, click on Re-Execute wait for 2 seconds and click on change args
        //Close the DirectInvoke tab

        //v0 add just the first candidate to save, as long as it's not the same as before after wait.
        //alt addition : save the candidates based on the inputs recorded before DirectInvoke
        //if the old candidate is not visible, add just the top candidate

        //On save form, just click on save/confirm
        //Post save open the candidates for the method you saved, make sure that method is there or its saved candidate count goes up


        //Step : replay all
        //select all and replay all
        //make sure you see a message saying replayed 'x' candidates or something similar, there should be no Exceptions
        //click on clear all

        //search for any instances of "create mock" on the method
        //open library and check the number of mocks available for this method, record the count of mocks
        //If they exist, for each of these - click them, ensure that there's a mock editor panel that opens up
        //save that mock, refresh in mock library and make sure the count goes up

        boolean multiModule = false;
        final IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));

        final ComponentFixture filterEntryPoint = idea.getFilterButton();
        filterEntryPoint.click();

        pause(ofSeconds(2).toMillis());

        final ComponentFixture followCheckBox = idea.getFilterFollowCheckbox();
        followCheckBox.click();

        idea.getFilterApplyButton().click();

        //disable this after testing
//        tryToSaveAll(idea);


        String startWith = "PatientCaseAuditService";
        boolean startFrom = false;
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

                    //for each unlogged Icon :
                    System.out.println("Icon tree map : ");
                    for (Integer key : iconTreeMap.keySet()) {
                        GutterIcon icon = iconTreeMap.get(key);
                        String iconAsString = icon.toString();
                        System.out.println("Icon info : " + iconAsString);
                    }

//                    if (true) {
//                        return;
//                    }

                    for (Integer key : iconTreeMap.keySet()) {
                        GutterIcon icon = iconTreeMap.get(key);
                        String iconAsString = icon.toString();
                        if (iconAsString.contains("profileBlue.svg")) {
                            //unlogged icon found, click it.
                            scrollDownToIcon(editor, icon);
                            pause(ofSeconds(1).toMillis());
                            try {
                                icon.moveMouse();
                                pause(ofMillis(250).toMillis());
                                icon.moveMouse();
                                pause(ofMillis(250).toMillis());
                                icon.click();
                            } catch (Exception e) {
                                scrollDownToIcon(editor, icon);
                                icon.click();
                            }

                            step("Direct Invoke method", () -> {
                                pause(ofSeconds(1).toMillis());
                                try {
                                    idea.getDirectInvokeExecuteButtonNew().click();
                                } catch (Exception exception) {
                                    //atomic window in focus right after button click
                                    System.out.println("Direct Invoke click failed");
//                                    idea.getDirectInvokeTabHeader().click();
//                                    idea.getExecuteMethodButton().click();
                                }
                                //wait for response
                                pause(ofSeconds(5).toMillis());
                            });
                            pause(ofSeconds(10).toMillis());

                            //try to replay
                            idea.getDICloseButton().click();
                            try {
                                idea.getReplayButtonNew().click();
                                pause(ofSeconds(10).toMillis());
                            } catch (Exception e) {
                                //no candidate found
                            }
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
        followCheckBox.click();
        tryToSaveAll(idea);
    }

    private void tryToSaveAll(IdeaFrame idea) {
        ComponentFixture selectAllIcon = idea.getSelectAllicon();
        pause(ofSeconds(1).toMillis());
        selectAllIcon.click();
        pause(ofMillis(250).toMillis());
        idea.getSaveGlobalButton().click();
        pause(ofSeconds(30).toMillis());
        idea.getSaveFromConfirmButton().click();
    }

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
        boolean startFrom = false;
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
        int offsetIncrement = 2;
        Integer startingOffset = null;
        try {
            startingOffset = editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineStartOffset(" + (icon.getLineNumber() + offsetIncrement) + ")", true);
        } catch (IndexOutOfBoundsException outOfBoundsException) {
            startingOffset = editorFixture.getEditor().callJs("local.get('editor').getDocument().getLineStartOffset(" + (icon.getLineNumber()) + ")", true);
        }
        assert startingOffset != null;
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

    //@Test
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
