package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.util.UIUtils;

import javax.swing.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SaveFormMetadataPanel {
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
    private JPanel actionPanel;
    private JPanel eastPanel;
    private JButton saveAndCloseButton;
    private JButton cancelButton;
    private JPanel westPanel;

    public SaveFormMetadataPanel(MetadataViewPayload payload) {
        loadView(payload);
    }

    public void loadView(MetadataViewPayload payload) {
        this.nameText.setText(payload.getName() != null ? payload.getName() : "");
        this.descriptionText.setText(payload.getDescription() != null ? payload.getDescription() : "");
        this.createdByLabel.setText(payload.getStoredCandidateMetadata().getRecordedBy());
        this.hostLabel.setText(payload.getStoredCandidateMetadata().getHostMachineName());
        this.timestampLabel.setText(convertTimestamp(payload.getStoredCandidateMetadata().getTimestamp()));
        this.timestampLabel.setToolTipText(payload.getStoredCandidateMetadata().getTimestamp() + "");
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
    }

    public MetadataViewPayload getPayload() {
        StoredCandidateMetadata metadata = new StoredCandidateMetadata();
        StoredCandidateMetadata.CandidateStatus status = StoredCandidateMetadata.CandidateStatus.PASSING;
        if (this.statusLabel.getText().equalsIgnoreCase("failing")) {
            status = StoredCandidateMetadata.CandidateStatus.FAILING;
        }
        metadata.setCandidateStatus(status);
        metadata.setTimestamp(Long.parseLong(timestampLabel.getToolTipText()));
        metadata.setRecordedBy(createdByLabel.getText());
        metadata.setHostMachineName(hostLabel.getText());

        MetadataViewPayload payload = new MetadataViewPayload(
                nameText.getText(),
                descriptionText.getText(),
                metadata
        );
        return payload;
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
