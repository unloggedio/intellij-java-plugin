package com.insidious.plugin.ui.mocking;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SelectorPanel<T>  {
    public static final JBColor HOVER_COLER = new JBColor(
            new Color(150, 188, 199),
            new Color(150, 188, 199));
    private final PopUpGetter popUpGetter;
    private JPanel mainPanel;
    private JPanel itemContainerPanel;
    private JBPopup popup;

    public PopUpGetter getPopUpGetter() {
        return popUpGetter;
    }

    public SelectorPanel(List<T> options, OnSelectListener<T> onSelectListener, PopUpGetter popUpGetter) {
        this.popUpGetter = popUpGetter;

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

    public JBPopup getPopup() {
        return popup;
    }

    public void setPopup(JBPopup popup) {
        this.popup = popup;
    }
}
