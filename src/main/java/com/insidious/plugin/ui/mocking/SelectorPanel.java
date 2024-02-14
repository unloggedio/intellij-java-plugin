package com.insidious.plugin.ui.mocking;

import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;

public class SelectorPanel<T> {
    public static final JBColor HOVER_COLER = new JBColor(
            new Color(150, 188, 199),
            new Color(150, 188, 199));
    private JPanel mainPanel;
    private JPanel itemContainerPanel;

    public SelectorPanel(java.util.List<T> options, OnSelectListener<T> onSelectListener) {

        OnSelectListener<T> selectListener = o -> onSelectListener.onSelect(o);


        itemContainerPanel.setLayout(new GridLayout(0, 1));
        itemContainerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        for (T option : options) {

            SelectorPanelItem<T> selectorPanelItem = new SelectorPanelItem<>(option, selectListener);
            itemContainerPanel.add(selectorPanelItem.getContent(), new GridConstraints());
        }


    }

    public JComponent getContent() {
        return mainPanel;
    }
}
