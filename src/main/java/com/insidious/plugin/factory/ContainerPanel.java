package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.ui.methodscope.RouterPanel;
import com.insidious.plugin.ui.stomp.StompComponent;

import javax.swing.*;
import java.awt.*;

public class ContainerPanel extends JPanel {
    private StompComponent stompComponent;
    private RouterPanel routerPanel;
    private JComponent currentContent;

    public ContainerPanel(LayoutManager layoutManager) {
        super(layoutManager);
    }

    public void setStompComponent(StompComponent stompComponent, RouterPanel routerPanel) {
        this.stompComponent = stompComponent;
        this.routerPanel = routerPanel;
        add(routerPanel.getComponent(), BorderLayout.NORTH);
    }

    public void setMethod(MethodAdapter methodAdapter) {
        routerPanel.setMethod(methodAdapter);
        stompComponent.onMethodFocussed(methodAdapter);
        setViewport(null);
    }

    public void showStompComponent() {
        setViewport(stompComponent.getComponent());
    }

    public synchronized void setViewport(JComponent content) {
        if (currentContent != null) {
            remove(currentContent);
        }
        if (content != null) {
            add(content, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
        this.currentContent = content;
    }
}
