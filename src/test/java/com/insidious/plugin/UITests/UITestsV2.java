package com.insidious.plugin.UITests;

import com.insidious.plugin.UITests.Utils.CustomGutterIconComparator;
import com.insidious.plugin.UITests.pages.IdeaFrame;
import com.insidious.plugin.UITests.wrapper.MockPopupEntry;
import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.GutterIcon;
import com.intellij.remoterobot.fixtures.TextEditorFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static java.awt.event.KeyEvent.*;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.swing.timing.Pause.pause;

public class UITestsV2 {

    private RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:8082");
    private final Keyboard keyboard = new Keyboard(remoteRobot);

    @Test
    public void testTC2_maven_demo() {

        //keep the UI test file open
        IdeaFrame idea = remoteRobot.find(IdeaFrame.class, ofSeconds(10));
        CustomGutterIconComparator iconComparator = new CustomGutterIconComparator();
        TextEditorFixture editor = idea.textEditor(Duration.ofSeconds(2));

        int mainIconLinenumber = 49;

        GutterIcon mainIcon = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("profileBlue.svg") && icon.getLineNumber() == mainIconLinenumber)
                .toList().get(0);

        List<GutterIcon> unloggedMockIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg") && icon.getLineNumber() >= mainIconLinenumber)
                .toList();
        List<GutterIcon> mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        assert mockIcons.size() == 10;

        for (int i = 0; i < mockIcons.size(); i++) {
            GutterIcon mockIcon = mockIcons.get(i);
            mockIcon.moveMouse();
            mockIcon.click();
            pause(ofMillis(250).toMillis());

            //assert empty
//            try {
//                ComponentFixture mockScrollPanel = idea.getMockPopupScrollPanel();
//                List<RemoteText> remoteTexts = mockScrollPanel.getData().getAll();
//                assert remoteTexts.isEmpty();
//            } catch (UIElementNotFoundException timeoutException) {
//                assert true;
//            }ca mock

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
            closeButton.click();
            pause(ofMillis(250).toMillis());
        }

        //Execute DirectInvoke the method
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

        //reload mock icons -> needed to ensure proper coordinates are taken
        editor = idea.textEditor(Duration.ofSeconds(2));
        unloggedMockIcons = editor.getGutter().getIcons().stream()
                .filter(icon -> icon.toString().contains("mock_ghost_icon_v2.svg") && icon.getLineNumber() >= mainIconLinenumber)
                .toList();
        mockIcons = new ArrayList<>(unloggedMockIcons);
        Collections.sort(mockIcons, iconComparator);

        for (int i = 0; i < mockIcons.size(); i++) {
            GutterIcon mockIcon = mockIcons.get(i);
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

        //rename and delete mocks
        //open library
        ComponentFixture libraryTabHeader = idea.getlibraryTabHeader();
        libraryTabHeader.click();

        //keep mocks tab open
//            idea.getLibraryMocksButton().click();
//            pause(ofSeconds(2).toMillis());

        //alt click on 1 active mock on editor and then clear selections

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
        tryToSaveAll(idea);
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

    private void tryToSaveAll(IdeaFrame idea) {
        ComponentFixture selectAllIcon = idea.getSelectAllicon();
        pause(ofMillis(500).toMillis());
        selectAllIcon.click();
        idea.getSaveGlobalButton().click();
        pause(ofSeconds(5).toMillis());

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
}
