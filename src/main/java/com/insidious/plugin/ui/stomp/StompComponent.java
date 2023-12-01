package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.client.ScanProgress;
import com.insidious.plugin.client.SessionScanEventListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class StompComponent implements Consumer<TestCandidateMetadata>, TestCandidateLifeListener {
    public static final int component_height = 93;
    private final InsidiousService insidiousService;
    private final JPanel itemPanel;
    private final StompStatusComponent stompStatusComponent;
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JScrollPane historyStreamScrollPanel;
    private JPanel scrollContainer;
    private JButton reloadButton;
    private JButton filterButton;
    private JButton saveReplayButton;
    private JButton replayButton;
    private JButton saveAsMockButton;
    private JButton generateJUnitButton;
    private JPanel filterButtonContainer;
    private long lastEventId = 0;
    private List<StompItem> stompItems = new ArrayList<>(500);
    private ConnectedAndWaiting connectedAndWaiting;
    private DisconnectedAnd disconnectedAnd;
    private SessionScanEventListener scanEventListener;
    private SimpleDateFormat dateFormat;

    public StompComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        itemPanel = new JPanel();
        GridLayout mgr = new GridLayout(0, 1, 0, 6);
        itemPanel.setLayout(mgr);
        itemPanel.setBackground(JBColor.WHITE);
        itemPanel.setAlignmentY(0);
        itemPanel.setAlignmentX(0);


        itemPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 50));

        historyStreamScrollPanel.setViewportView(itemPanel);
        historyStreamScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        scrollContainer.setBorder(BorderFactory.createEmptyBorder());
        reloadButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                {
                    clear();
                    loadNewCandidates();
                    itemPanel.revalidate();
                    itemPanel.repaint();
                    historyStreamScrollPanel.revalidate();
                    historyStreamScrollPanel.repaint();
                }
            }
        });


//        filterButton.setSize(new Dimension(98, 32));
//        filterButton.setPreferredSize(new Dimension(98, 32));
//        filterButton.setMinimumSize(new Dimension(98, 32));
//        filterButton.setMaximumSize(new Dimension(98, 32));
//        filterButton.setBorder(new RoundBtnBorder(15));
//        Font font = filterButton.getFont();
//        Font boldFont = font.deriveFont(Font.BOLD);
//        filterButton.setFont(boldFont);


        reloadButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                StompFilter stompFilter = new StompFilter();
                ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(stompFilter.getComponent(), null);

                gutterMethodComponentPopup
                        .setProject(insidiousService.getProject())
                        .setShowBorder(true)
                        .setShowShadow(true)
                        .setFocusable(true)
                        .setRequestFocus(true)
                        .setCancelOnClickOutside(true)
                        .setCancelOnOtherWindowOpen(true)
                        .setCancelKeyEnabled(true)
                        .setBelongsToGlobalPopupStack(false)
                        .setTitle("Filter")
                        .setTitleIcon(new ActiveIcon(UIUtils.FILTER_LINE))
                        .createPopup()
                        .show(new RelativePoint(e));


            }
        });


        ConnectedAndWaiting connectedAndWaiting = new ConnectedAndWaiting();
        JPanel component = (JPanel) connectedAndWaiting.getComponent();
        component.setAlignmentY(1.0f);

        scanEventListener = new SessionScanEventListener() {
            @Override
            public void started() {
                lastEventId = 0;
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
            }

            @Override
            public void waiting() {
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
                stompStatusComponent.addRightStatus("scan-progress", "Waiting");

            }

            @Override
            public void paused() {
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
                stompStatusComponent.removeRightStatus("scan-progress");
            }

            @Override
            public void ended() {
                stompStatusComponent.removeRightStatus("last-updated");
                stompStatusComponent.removeRightStatus("scan-progress");
            }

            @Override
            public void progress(ScanProgress scanProgress) {
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
                stompStatusComponent.addRightStatus("scan-progress", String.format(
                        "Scanning %d of %d", scanProgress.getCount(), scanProgress.getTotal()
                ));
            }
        };

        dateFormat = new SimpleDateFormat("HH:mm:ss");
        stompStatusComponent = new StompStatusComponent();
        mainPanel.add(stompStatusComponent.getComponent(), BorderLayout.SOUTH);


//        filterButton.setContentAreaFilled(false);
//        filterButton.setOpaque(false);
//
//
//        reloadButton.setContentAreaFilled(false);
//        reloadButton.setOpaque(false);
//
//        saveReplayButton.setContentAreaFilled(false);
//        saveReplayButton.setOpaque(false);
//
//
//        replayButton.setContentAreaFilled(false);
//        replayButton.setOpaque(false);
//
//        generateJUnitButton.setContentAreaFilled(false);
//        generateJUnitButton.setOpaque(false);
//
//        saveAsMockButton.setContentAreaFilled(false);
//        saveAsMockButton.setOpaque(false);

    }

    private static void addComponentTop(JPanel panel, GridConstraints gbc, Component comp, List<Component> components) {
        // Increment gridy for existing components
//        for (Component existingComp : components) {
//            GridConstraints existingGbc = ((GridLayout) panel.getLayout()).(existingComp);
//            existingGbc.gridy++;
//            panel.add(existingComp, existingGbc); // Re-add with new constraints
//        }

        // Add the new component at the top
//        gbc.gridy = 0;
        panel.add(comp, gbc);

        // Refresh the panel
        panel.revalidate();
        panel.repaint();
    }

    public String simpleTime(Date currentDate) {
        // Use the format method to format the date as a string
        return dateFormat.format(currentDate);
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void accept(TestCandidateMetadata testCandidateMetadata) {
        if (testCandidateMetadata.getExitProbeIndex() > lastEventId) {
            lastEventId = testCandidateMetadata.getExitProbeIndex();
        }
        StompItem stompItem = new StompItem(testCandidateMetadata, this, insidiousService);
        if (stompItems.size() > 0) {
            StompItem last = stompItems.get(stompItems.size() - 1);
            int count = insidiousService.getMethodCallCountBetween(last.getTestCandidate().getExitProbeIndex(),
                    stompItem.getTestCandidate().getEntryProbeIndex());
        }
        stompItems.add(stompItem);
        JPanel component = stompItem.getComponent();
//        Dimension currentSize = component.getSize();
//        currentSize.height = component_height;
//        component.setSize(currentSize);
        component.setMaximumSize(new Dimension(itemPanel.getWidth(), component_height));
//        component.setPreferredSize(new Dimension(itemPanel.getWidth(), component_height));
        component.setMinimumSize(new Dimension(itemPanel.getWidth(), component_height));
//        component.setSize(new Dimension(itemPanel.getWidth(), component_height));
//        component.setPreferredSize(currentSize);
//        GridConstraints gbc = new GridConstraints();
//        addComponentTop(itemPanel, gbc, component, Arrays.asList(itemPanel.getComponents()));
        itemPanel.add(component, 0);
//        JPanel spacer = new JPanel();
//        spacer.setOpaque(false);
//        spacer.setMaximumSize(new Dimension(itemPanel.getWidth(), 4));
//        spacer.setMinimumSize(new Dimension(itemPanel.getWidth(), 4));
//        spacer.setPreferredSize(new Dimension(itemPanel.getWidth(), 4));
//        itemPanel.add(spacer, 1);

        int stompItemCount = stompItems.size();
        if (stompItemCount > 500) {
            for (int i = stompItemCount - 500; i > 0; i--) {
                StompItem last = stompItems.remove(0);
                itemPanel.remove(last.getComponent());
            }
        }

        if (stompItems.size() * component_height < 500) {
//            itemPanel.setMinimumSize(new Dimension(-1, component_height * stompItems.size() + 10));
//            itemPanel.setPreferredSize(new Dimension(-1, component_height * stompItems.size() + 10));
//            historyStreamScrollPanel.setSize(new Dimension(-1, component_height * stompItems.size() + 10));
        }

        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();

    }

    @Override
    public void executeCandidate(List<TestCandidateMetadata> metadata,
                                 ClassUnderTest classUnderTest, String source,
                                 AgentCommandResponseListener<TestCandidateMetadata, String> responseListener) {

    }

    @Override
    public void displayResponse(Component responseComponent, boolean isExceptionFlow) {

    }

    @Override
    public void onSaved(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onSaveRequest(TestCandidateMetadata storedCandidate, AgentCommandResponse<String> agentCommandResponse) {

    }

    @Override
    public void onDeleteRequest(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onDeleted(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onUpdated(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onUpdateRequest(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onGenerateJunitTestCaseRequest(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onCandidateSelected(TestCandidateMetadata testCandidateMetadata) {

    }

    @Override
    public void onCancel() {

    }

    public void clear() {
        lastEventId = 0;
        itemPanel.removeAll();
        stompItems.clear();
        stompStatusComponent.setDisconnected();
    }

    public void disconnected() {
        stompStatusComponent.setDisconnected();
        scanEventListener.ended();
    }

    public void setConnectedAndWaiting() {
        stompStatusComponent.setConnected();
    }

    public void loadNewCandidates() {
        try {
            insidiousService.getSessionInstance().getTopLevelTestCandidates(this, lastEventId);
        } catch (SQLException e) {
            // failed to load candidates hmm
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public SessionScanEventListener getScanEventListener() {
        return scanEventListener;
    }

}
