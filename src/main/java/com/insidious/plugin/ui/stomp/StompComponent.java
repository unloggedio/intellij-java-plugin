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
import com.intellij.ui.awt.RelativePoint;
import com.intellij.uiDesigner.core.GridConstraints;
import org.gradle.internal.impldep.org.joda.time.Instant;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class StompComponent implements Consumer<TestCandidateMetadata>, TestCandidateLifeListener {
    public static final int component_height = 93;
    private final InsidiousService insidiousService;
    private final JPanel itemPanel;
    private final StompStatusComponent stompStatusComponent;
    private final List<TestCandidateMetadata> selectedCandidates = new ArrayList<>();
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
        GridBagLayout mgr = new GridBagLayout();
        itemPanel.setLayout(mgr);
//        itemPanel.setBackground(JBColor.WHITE);
        itemPanel.setAlignmentY(0);
        itemPanel.setAlignmentX(0);


        itemPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));

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


        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
//        itemPanel.add(new JLabel("Exec tue a metho d"), gbc, 0);
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
        StompItem stompItem = new StompItem(testCandidateMetadata, this);
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
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.ipadx = 0;
        gbc.ipady = 0;

        itemPanel.add(component, gbc, 0);

        GridBagConstraints gbc1 = new GridBagConstraints();

        gbc1.gridx = 1;
        gbc1.gridy = GridBagConstraints.RELATIVE;
        gbc1.gridwidth = 1;
        gbc1.gridheight = 1;

        gbc1.weightx = 0.1;
        gbc1.weighty = 1;
        gbc1.anchor = GridBagConstraints.NORTH;
        gbc1.fill = GridBagConstraints.BOTH;

        gbc1.insets = new Insets(0, 0, 0, 0);
        gbc1.ipadx = 0;
        gbc1.ipady = 0;


        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
        JLabel hello = new JLabel(String.format(
                "          %s",
                sdf.format(
                        Instant.ofEpochMilli((testCandidateMetadata.getCallTimeNanoSecond() / (1000 * 1000))).toDate())
        ));
        Font font = hello.getFont();
        hello.setFont(font.deriveFont(10.0f));
        hello.setForeground(Color.decode("#8C8C8C"));
        hello.setUI(new VerticalLabelUI(true));
        comp.add(hello, BorderLayout.CENTER);


        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new GridBagLayout());
//        separator.setBorder();
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
//        separator.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));

        JLabel comp1 = new JLabel();
        comp1.setIcon(UIUtils.CIRCLE_EMPTY);
        comp1.setMaximumSize(new Dimension(16, 16));
        comp1.setMinimumSize(new Dimension(16, 16));
        comp1.setPreferredSize(new Dimension(16, 16));

        JPanel comp2 = new JPanel();
        comp2.add(comp1);
//        comp2.setMaximumSize(new Dimension(20, 20));
//        comp2.setMinimumSize(new Dimension(20, 20));
//        comp2.setPreferredSize(new Dimension(20, 20));
        comp2.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.fill = GridBagConstraints.VERTICAL;

        lineContainer.add(createSeparator(), constraints);
        GridBagConstraints constraints1 = new GridBagConstraints();
        constraints1.gridx = 0;
        constraints1.gridy = 1;
        constraints1.weightx = 0;
        constraints1.weighty = 0;
        constraints1.anchor = GridBagConstraints.CENTER;
        constraints1.fill = GridBagConstraints.NONE;

        lineContainer.add(comp2, constraints1);
        GridBagConstraints constraints2 = new GridBagConstraints();
        constraints2.gridx = 0;
        constraints2.gridy = 2;
        constraints2.weightx = 0;
        constraints2.weighty = 1;
        constraints2.anchor = GridBagConstraints.SOUTH;
        constraints2.fill = GridBagConstraints.VERTICAL;
        lineContainer.add(createSeparator(), constraints2);

        comp.add(lineContainer, BorderLayout.WEST);
//        comp.setBorder(BorderFactory.createEmptyBorder());
//        comp.setMaximumSize(new Dimension(10, component_height));
//        comp.setMinimumSize(new Dimension(10, component_height));
//        comp.setPreferredSize(new Dimension(10, component_height));
        itemPanel.add(comp, gbc1, 1);


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

    @NotNull
    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        Dimension size = new Dimension(2, (component_height / 4) - 2); // Set preferred size for the separator
        separator.setForeground(Color.RED);
        separator.setPreferredSize(size);
        separator.setMaximumSize(size);
        separator.setMinimumSize(size);
        return separator;
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
    public void onSelected(TestCandidateMetadata storedCandidate) {
        this.selectedCandidates.add(storedCandidate);
    }

    @Override
    public void unSelected(TestCandidateMetadata storedCandidate) {
        this.selectedCandidates.remove(storedCandidate);
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
