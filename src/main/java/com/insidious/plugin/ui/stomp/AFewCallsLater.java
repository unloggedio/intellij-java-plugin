package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AFewCallsLater {
    private final long gapStartIndex;
    private final long gapEndIndex;
    private JPanel mainPanel;

    public AFewCallsLater(long gapStartIndex, long gapEndIndex, int count, OnExpandListener onExpandListener) {

        this.gapStartIndex = gapStartIndex;
        this.gapEndIndex = gapEndIndex;

        JLabel laterLabel = new JLabel(String.format("<html><small>%s calls later</small></html>", count));
        laterLabel.setForeground(new JBColor(
                Gray._156,
                Gray._156
        ));
        mainPanel.setLayout(new BorderLayout());
        Border lineBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#D9D9D9"), 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        );

        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                lineBorder
        ));
        mainPanel.add(laterLabel, BorderLayout.WEST);
        JLabel iconLabel = new JLabel();
//        iconLabel.setIcon(UIUtils.EXPAND_UP_DOWN);

        mainPanel.add(iconLabel, BorderLayout.EAST);

//        mainPanel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                onExpandListener.onExpand(AFewCallsLater.this);
//            }
//        });

    }

    public long getGapStartIndex() {
        return gapStartIndex;
    }

    public long getGapEndIndex() {
        return gapEndIndex;
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
