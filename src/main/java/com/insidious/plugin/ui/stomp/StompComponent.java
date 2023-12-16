package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.client.ScanProgress;
import com.insidious.plugin.client.SessionScanEventListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.ui.methodscope.MethodDirectInvokeComponent;
import com.insidious.plugin.util.UIUtils;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
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
    private JPanel controlPanel;
    private JPanel filterButtonContainer;
    private long lastEventId = 0;
    private List<StompItem> stompItems = new ArrayList<>(500);
    private ConnectedAndWaiting connectedAndWaiting;
    private DisconnectedAnd disconnectedAnd;
    private SessionScanEventListener scanEventListener;
    private SimpleDateFormat dateFormat;

    private Map<TestCandidateMetadata, Component> candidateMetadataStompItemMap = new HashMap<>();
    private MethodDirectInvokeComponent directInvokeComponent = null;

    public StompComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        itemPanel = new JPanel();
        GridBagLayout mgr = new GridBagLayout();
        itemPanel.setLayout(mgr);
        itemPanel.setAlignmentY(0);
        itemPanel.setAlignmentX(0);

        itemPanel.add(new JPanel(), createGBCForFakeComponent());

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


        replayButton.addActionListener(e -> {
            for (TestCandidateMetadata selectedCandidate : selectedCandidates) {
                PsiClass classPsiElement = JavaPsiFacade
                        .getInstance(insidiousService.getProject())
                        .findClass(selectedCandidate.getFullyQualifiedClassname(),
                                GlobalSearchScope.projectScope(insidiousService.getProject()));
                PsiMethod methodPsiElement = null;
                JvmMethod[] methodsByName = classPsiElement.findMethodsByName(
                        selectedCandidate.getMainMethod().getMethodName());
                for (JvmMethod jvmMethod : methodsByName) {
                    if (selectedCandidate.getMainMethod().getArguments().size() == jvmMethod.getParameters().length) {
                        methodPsiElement = (PsiMethod) jvmMethod.getSourceElement();
                    }

                }
                long batchTime = System.currentTimeMillis();

                insidiousService.executeSingleCandidate(
                        new StoredCandidate(selectedCandidate),
                        new ClassUnderTest(selectedCandidate.getFullyQualifiedClassname()),
                        "all-" + batchTime,
                        new AgentCommandResponseListener<StoredCandidate, String>() {
                            @Override
                            public void onSuccess(StoredCandidate testCandidate, AgentCommandResponse<String> agentCommandResponse, DifferenceResult diffResult) {

                            }
                        },
                        new JavaMethodAdapter(methodPsiElement)
                );
            }
        });

    }

    public String simpleTime(Date currentDate) {
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
//        if (stompItems.size() > 0) {
//            StompItem last = stompItems.get(stompItems.size() - 1);
//            int count = insidiousService.getMethodCallCountBetween(last.getTestCandidate().getExitProbeIndex(),
//                    testCandidateMetadata.getEntryProbeIndex());
//            if (count > 0) {
//                JLabel laterLabel = new JLabel(String.format("<html><small>%s calls later</small></html>", count));
//                laterLabel.setForeground(new JBColor(
//                        Gray._156,
//                        Gray._156
//                ));
//                JPanel labelPanel = new JPanel();
//                labelPanel.setLayout(new BorderLayout());
//                Border lineBorder = BorderFactory.createCompoundBorder(
//                        BorderFactory.createLineBorder(Color.decode("#D9D9D9"), 1, true),
//                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
//                );
//                labelPanel.setBorder(lineBorder);
//                labelPanel.add(laterLabel, BorderLayout.WEST);
//                JLabel iconLabel = new JLabel();
//                iconLabel.setIcon(UIUtils.EXPAND_UP_DOWN);
//
//                labelPanel.add(iconLabel, BorderLayout.EAST);
//
//                labelPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//                itemPanel.add(labelPanel, createGBCForLeftMainComponent(), 0);
//                itemPanel.add(createLinePanel(createLineComponent()), createGBCForLinePanel(), 1);
//            }
//        }

        addCandidateToUi(testCandidateMetadata, 0);

    }

    private void addCandidateToUi(TestCandidateMetadata testCandidateMetadata, int index) {
        StompItem stompItem = new StompItem(testCandidateMetadata, this, insidiousService);

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout());

        stompItems.add(stompItem);

        JPanel component = stompItem.getComponent();
        candidateMetadataStompItemMap.put(testCandidateMetadata, component);
        component.setMaximumSize(new Dimension(itemPanel.getWidth(), component_height));
        component.setMinimumSize(new Dimension(itemPanel.getWidth(), component_height));


        rowPanel.add(component, BorderLayout.CENTER);

        JPanel dateAndTimePanel = createDateAndTimePanel(createTimeLineComponent(),
                Date.from(Instant.ofEpochMilli(testCandidateMetadata.getCreatedAt())));
        rowPanel.add(dateAndTimePanel, BorderLayout.EAST);


        makeSpace();
        itemPanel.add(rowPanel, createGBCForLeftMainComponent());
        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();
    }

    private GridBagConstraints createGBCForLeftMainComponent() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }

//    private GridBagConstraints createGBCForLeftDirectInvokeComponent() {
//        GridBagConstraints gbc = new GridBagConstraints();
//
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//        gbc.gridwidth = 1;
//        gbc.gridheight = 1;
//
//        gbc.weightx = 1;
//        gbc.weighty = 1;
//        gbc.anchor = GridBagConstraints.LINE_START;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//
//        gbc.insets = JBUI.insetsBottom(8);
//        gbc.ipadx = 0;
//        gbc.ipady = 0;
//        return gbc;
//    }


    private GridBagConstraints createGBCForProcessStartedComponent() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }
    private GridBagConstraints createGBCForFakeComponent() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(8);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }

//    private GridBagConstraints createGBCForDateAndTimePanel() {
//        GridBagConstraints gbc1 = new GridBagConstraints();
//
//        gbc1.gridx = 1;
//        gbc1.gridy = GridBagConstraints.RELATIVE;
//        gbc1.gridwidth = 1;
//        gbc1.gridheight = 1;
//
//        gbc1.weightx = 0.1;
//        gbc1.weighty = 0.3;
//        gbc1.anchor = GridBagConstraints.LINE_START;
//        gbc1.fill = GridBagConstraints.BOTH;
//
//        gbc1.insets = JBUI.emptyInsets();
//        gbc1.ipadx = 0;
//        gbc1.ipady = 0;
//        return gbc1;
//    }

//    private GridBagConstraints createGBCForLinePanel() {
//        GridBagConstraints gbc1 = new GridBagConstraints();
//
//        gbc1.gridx = 1;
//        gbc1.gridy = GridBagConstraints.RELATIVE;
//        gbc1.gridwidth = 1;
//        gbc1.gridheight = 1;
//
//        gbc1.weightx = 0.1;
//        gbc1.weighty = 0;
//        gbc1.anchor = GridBagConstraints.LINE_START;
//        gbc1.fill = GridBagConstraints.VERTICAL;
//
//        gbc1.insets = JBUI.insets(0, 16, 0, 0);
//        gbc1.ipadx = 0;
//        gbc1.ipady = 0;
//        return gbc1;
//    }

    private JPanel createDateAndTimePanel(JPanel lineContainer, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
        JLabel hello = new JLabel(String.format("          %s", sdf.format(date)));
        Font font = hello.getFont();
        hello.setFont(font.deriveFont(10.0f));
        hello.setForeground(Color.decode("#8C8C8C"));
        hello.setUI(new VerticalLabelUI(true));
        comp.add(hello, BorderLayout.CENTER);


        comp.add(lineContainer, BorderLayout.WEST);
        comp.setSize(new Dimension(50, -1));
        comp.setPreferredSize(new Dimension(50, -1));
        return comp;
    }

    private JPanel createLinePanel(JPanel lineContainer) {
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
        JLabel comp1 = new JLabel("      ");
        comp1.setUI(new VerticalLabelUI(true));

//        comp.add(comp1, BorderLayout.CENTER);
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 4));
        comp.add(lineContainer, BorderLayout.WEST);
        comp.setSize(new Dimension(50, -1));
        comp.setPreferredSize(new Dimension(50, -1));

        return comp;
    }

    private JPanel createLineComponent() {
        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new GridBagLayout());
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.fill = GridBagConstraints.BOTH;
        lineContainer.add(createSeparator(), constraints);

        return lineContainer;
    }

    private JPanel createTimeLineComponent() {
        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new GridBagLayout());
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));

        JLabel comp1 = new JLabel();
        comp1.setIcon(UIUtils.CIRCLE_EMPTY);
        comp1.setMaximumSize(new Dimension(16, 16));
        comp1.setMinimumSize(new Dimension(16, 16));
        comp1.setPreferredSize(new Dimension(16, 16));

        JPanel comp2 = new JPanel();
        comp2.add(comp1);

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
        return lineContainer;
    }

    @NotNull
    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        Dimension size = new Dimension(2, (component_height / 4) - 2); // Set preferred size for the separator
        separator.setForeground(Color.decode("#D9D9D9"));
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
        updateControlPanel();
    }

    private void updateControlPanel() {
        if (selectedCandidates.size() > 0 && !controlPanel.isEnabled()) {
//            reloadButton.setEnabled(true);
            generateJUnitButton.setEnabled(true);
            saveAsMockButton.setEnabled(true);
            replayButton.setEnabled(true);
            saveReplayButton.setEnabled(true);
            controlPanel.setEnabled(true);
        } else if (selectedCandidates.size() == 0 && controlPanel.isEnabled()) {
//            reloadButton.setEnabled(false);
            generateJUnitButton.setEnabled(false);
            saveAsMockButton.setEnabled(false);
            replayButton.setEnabled(false);
            saveReplayButton.setEnabled(false);
            controlPanel.setEnabled(false);
        }
    }

    @Override
    public void unSelected(TestCandidateMetadata storedCandidate) {
        this.selectedCandidates.remove(storedCandidate);
        updateControlPanel();
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

    @Override
    public void onExpandChildren(TestCandidateMetadata candidateMetadata) {
        try {
            List<TestCandidateMetadata> childCandidates =
                    insidiousService.getTestCandidateBetween(
                            candidateMetadata.getMainMethod().getEntryProbe().getEventId(),
                            candidateMetadata.getMainMethod().getReturnDataEvent().getEventId());

            Component component = candidateMetadataStompItemMap.get(candidateMetadata);
            int parentIndex = itemPanel.getComponentZOrder(component);

            for (TestCandidateMetadata childCandidate : childCandidates) {
                addCandidateToUi(childCandidate, parentIndex);
            }


        } catch (SQLException e) {
            InsidiousNotification.notifyMessage("Failed to load child calls: " + e.getMessage(),
                    NotificationType.ERROR);
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        lastEventId = 0;
        itemPanel.removeAll();
        itemPanel.add(new JPanel(), createGBCForFakeComponent());
        stompItems.clear();
    }

    public void disconnected() {
        stompStatusComponent.setDisconnected();
        scanEventListener.ended();
    }

    public void showDirectInvoke(MethodAdapter method) throws IOException, FontFormatException {
        if (directInvokeComponent == null) {
            directInvokeComponent = new MethodDirectInvokeComponent(insidiousService);
            JComponent content = directInvokeComponent.getContent();
            content.setMaximumSize(new Dimension(-1, 500));
            content.setPreferredSize(new Dimension(-1, 300));
//            content.setSize(new Dimension(-1, 500));

            JPanel anotherRowPanel = new JPanel();
            anotherRowPanel.setLayout(new BorderLayout());


            anotherRowPanel.add(content, BorderLayout.CENTER);
            anotherRowPanel.add(createDateAndTimePanel(createTimeLineComponent(),
                    Date.from(Instant.now())), BorderLayout.EAST);
            makeSpace();
            itemPanel.add(anotherRowPanel, createGBCForLeftMainComponent(), 0);
        }
        directInvokeComponent.renderForMethod(method);

        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();
    }

    void makeSpace() {
        Component[] components = itemPanel.getComponents();
        for (Component component : components) {
            GridBagConstraints gbc = ((GridBagLayout) itemPanel.getLayout()).getConstraints(component);
            gbc.gridy += 1;
            itemPanel.add(component, gbc);
        }
    }

    public void removeDirectInvoke() {
        int index = itemPanel.getComponentZOrder(directInvokeComponent.getContent());
        itemPanel.remove(index);
        itemPanel.remove(index);
        itemPanel.revalidate();
        itemPanel.repaint();
    }

    public void setConnectedAndWaiting() {
        JLabel process_started = new JLabel("Process started");
        process_started.setIcon(UIUtils.LINK);
        JPanel startedPanel = new JPanel();
        startedPanel.setLayout(new BorderLayout());
        process_started.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.GREEN, 3, true),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)
                )
        );
        startedPanel.add(process_started, BorderLayout.EAST);

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout());


        rowPanel.add(startedPanel, BorderLayout.CENTER);
        rowPanel.add(createLinePanel(createLineComponent()), BorderLayout.EAST);
        makeSpace();
        itemPanel.add(rowPanel, createGBCForProcessStartedComponent());
        if (directInvokeComponent != null) {

            JPanel anotherRowPanel = new JPanel();
            anotherRowPanel.setLayout(new BorderLayout());


            anotherRowPanel.add(directInvokeComponent.getContent(), BorderLayout.CENTER);
            anotherRowPanel.add(createDateAndTimePanel(createTimeLineComponent(),
                    Date.from(Instant.now())), BorderLayout.EAST);


            makeSpace();
            itemPanel.add(anotherRowPanel, createGBCForLeftMainComponent(), 0);
        }
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
