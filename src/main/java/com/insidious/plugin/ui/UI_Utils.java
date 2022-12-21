package com.insidious.plugin.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class UI_Utils {

    static final String gifPath = "/icons/gif/";
    static Color teal = new Color(1, 204, 245);
    static Color pink = new Color(254,100,216);
    static Color red = new Color(245,101,101);
    static Color green = new Color(56,161,105);

    public static void setDividerColorForSplitPane(JSplitPane splitPane, Color color)
    {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {
                    }

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(color);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
    }

    public static void setGifIconForButton(JButton button, String gif, Icon fallback)
    {
        try {
                ImageIcon loadingIcon = new ImageIcon(UI_Utils.class.getResource(gifPath+gif));
                button.setIcon(loadingIcon);
        }
        catch(Exception e)
        {
            System.out.println("Exception setting Gif icon for button "+e);
            e.printStackTrace();
            button.setIcon(fallback);
        }
    }
}