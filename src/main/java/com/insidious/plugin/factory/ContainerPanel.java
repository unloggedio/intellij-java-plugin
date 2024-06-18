package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.ui.UnloggedSDKOnboarding;
import com.insidious.plugin.ui.methodscope.ComponentProvider;
import com.insidious.plugin.ui.methodscope.RouterPanel;
import com.insidious.plugin.ui.stomp.StompComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class ContainerPanel extends JPanel {
    private StompComponent stompComponent;
    private RouterPanel routerPanel;
    private ComponentProvider currentContent;
    private JPanel container;

    public ContainerPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public void setStompComponent(StompComponent stompComponent, RouterPanel routerPanel) {
        GridBagLayout mgr = new GridBagLayout();
        container = new JPanel(new GridLayout());
        this.stompComponent = stompComponent;
        this.routerPanel = routerPanel;
        add(routerPanel.getComponent(), BorderLayout.NORTH);
        add(container, BorderLayout.CENTER);
        add(new JSeparator(), BorderLayout.SOUTH);
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


    public void setMethod(MethodAdapter methodAdapter) {
        routerPanel.setMethod(methodAdapter);
        stompComponent.onMethodFocussed(methodAdapter);
        setViewport(null);
    }

    public synchronized void setViewport(ComponentProvider content) {
        ApplicationManager.getApplication().invokeLater(() -> {
            container.removeAll();
            if (content == null) {
                routerPanel.setMiniMode(false);
            } else {
                container.add(content.getComponent(), new GridConstraints());
                routerPanel.setTitle(content.getTitle());
                if (!(content instanceof UnloggedSDKOnboarding)) {
                    routerPanel.setMiniMode(true);
                }
            }
            container.revalidate();
            container.repaint();
            this.currentContent = content;
            revalidate();
            repaint();
        });
    }

    private GridBagConstraints createGBCForMain() {
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

}
