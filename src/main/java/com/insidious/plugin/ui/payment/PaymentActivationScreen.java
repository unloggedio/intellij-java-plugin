package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.util.CommonPaymentUtil;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PaymentActivationScreen {
    private JPanel mainPanel;
    private JPanel textPanel;
    private JPanel buttonPanel;
    private JButton payButton;
    private JButton activateButton;
    private JTextArea keyArea;
    private JLabel description;
    private JLabel keyLabel;
    private JLabel status;

    private final InsidiousService insidiousService;

    public PaymentActivationScreen(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        // Add ActionListener to payButton
        payButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommonPaymentUtil.routeToPayment();
            }
        });

        AbstractDocument limitedTextDocument=(AbstractDocument)keyArea.getDocument();

        limitedTextDocument.setDocumentFilter(new DocumentFilter() {
            final int maxChars = 350; // Set the maximum number of characters
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (fb.getDocument().getLength() + string.length() <= maxChars) {
                    super.insertString(fb, offset, string, attr);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attrs) throws BadLocationException {
                if (fb.getDocument().getLength() + string.length() - length <= maxChars) {
                    super.replace(fb, offset, length, string, attrs);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });

        // Add ActionListener to activateButton
        activateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus();
            }
        });
    }

    /**
     * Returns the main panel of the UI.
     * @return the main JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Updates the status label based on the text entered in keyArea.
     */
    private void updateStatus() {
        String enteredText = keyArea.getText().trim();

        //TODO: Will be a boolean here received from authentication service
        if ("Akshat Jain".equals(enteredText)) {
            setStatus("Activated \u2714", Color.decode("#1F8A3C"));
            //TODO: Not the most elegant solution. What if somehow someclicks on this again. Need a better state management
            insidiousService.removeGetPremiumPanel();
        } else {
            setStatus("Error - wrong product key \u26A0", Color.decode("#E46A76"));
        }
    }

    /**
     * Sets the status text and color.
     * @param text the status text
     * @param color the color for the status text
     */
    private void setStatus(String text, Color color) {
        status.setText(text);
        status.setForeground(color);
    }
}
