package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.pojo.atomic.TestType;
import com.insidious.plugin.util.UIUtils;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;

public class SaveFormMetadataPanel {
    private final OnTestTypeChangeListener onTestTypeChangeListener;
    private JPanel mainPanel;
    private JPanel topAlignPanel;
    private JTextField nameText;
    private JTextArea descriptionText;
    private JScrollPane textAreaScrollParent;
    private JPanel bottomPanel;
    private JPanel createdByPanel;
    private JPanel timestampPanel;
    private JPanel hostPanel;
    private JPanel statusPanel;
    private JLabel createdByLabel;
    private JLabel statusLabel;
    private JLabel hostLabel;
    private JLabel timestampLabel;
    private JButton saveAndCloseButton;
    private JButton cancelButton;
    private JLabel createdByKeyLabel;
    private JLabel statusKeyLabel;
    private JLabel hostKeyPanel;
    private JLabel timestampKeyPanel;
    private JComboBox<TestType> testTypeComboBox;
    private JLabel testType;

    public SaveFormMetadataPanel(MetadataViewPayload metadataViewPayload, OnTestTypeChangeListener onTestTypeChangeListener) {
        loadView(metadataViewPayload);
        this.onTestTypeChangeListener = onTestTypeChangeListener;

        this.testTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            JSONObject testChange = new JSONObject();
            UsageInsightTracker.getInstance().RecordEvent("MOCK_LINKING_TEST_TYPE", testChange);
            TestType selectedTestType = (TestType) e.getItem();
            metadataViewPayload.setType(selectedTestType);
            onTestTypeChangeListener.onTestTypeChange(selectedTestType);
        });

    }

    public void loadView(MetadataViewPayload payload) {
        this.nameText.setText(payload.getName() != null ? payload.getName() : "");
        this.descriptionText.setText(payload.getDescription() != null ? payload.getDescription() : "");

        this.createdByKeyLabel.setIcon(UIUtils.CREATED_BY);
        this.createdByKeyLabel.setEnabled(false);
        this.createdByLabel.setText(payload.getStoredCandidateMetadata().getRecordedBy());
        this.createdByLabel.setEnabled(false);

        this.statusKeyLabel.setIcon(UIUtils.STATUS);
        this.statusKeyLabel.setEnabled(false);
        StoredCandidateMetadata.CandidateStatus status = payload.getStoredCandidateMetadata().getCandidateStatus();
        if (status.equals(StoredCandidateMetadata.CandidateStatus.FAILING)) {
            this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
            this.statusLabel.setForeground(UIUtils.red);
            this.statusLabel.setText("Failing");
        } else {
            this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
            this.statusLabel.setForeground(UIUtils.green);
            this.statusLabel.setText("Passing");
        }

        this.hostKeyPanel.setIcon(UIUtils.MACHINE);
        this.hostKeyPanel.setEnabled(false);
        this.hostLabel.setText(payload.getStoredCandidateMetadata().getHostMachineName());
        this.hostLabel.setEnabled(false);

        this.timestampKeyPanel.setIcon(UIUtils.CLOCK);
        this.timestampKeyPanel.setEnabled(false);
        this.timestampLabel.setText(convertTimestamp(payload.getStoredCandidateMetadata().getTimestamp()));
        this.timestampLabel.setToolTipText(payload.getStoredCandidateMetadata().getTimestamp() + "");
        this.timestampLabel.setEnabled(false);

        this.testTypeComboBox.setModel(
                new DefaultComboBoxModel<>(new Vector<>(List.of(TestType.UNIT, TestType.INTEGRATION))));
    }

    public MetadataViewPayload getPayload() {
        StoredCandidateMetadata.CandidateStatus status = StoredCandidateMetadata.CandidateStatus.PASSING;
        if (this.statusLabel.getText().equalsIgnoreCase("failing")) {
            status = StoredCandidateMetadata.CandidateStatus.FAILING;
        }
        StoredCandidateMetadata metadata = new StoredCandidateMetadata(
                hostLabel.getText(), createdByLabel.getText(),
                Long.parseLong(timestampLabel.getToolTipText()), status
        );

        return new MetadataViewPayload(
                nameText.getText(),
                descriptionText.getText(), TestType.INTEGRATION, metadata
        );
    }

    private String convertTimestamp(long timestamp) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Kolkata"));
        String dateFormatted = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm:ss").format(dateTime);
        return dateFormatted;
    }

    public JPanel getMainPanel() {
        return this.mainPanel;
    }

    public AbstractButton getCancelButton() {
        return cancelButton;
    }

    public AbstractButton getSaveButton() {
        return saveAndCloseButton;
    }
}
