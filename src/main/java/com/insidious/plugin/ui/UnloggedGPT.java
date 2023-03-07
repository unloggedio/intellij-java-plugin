package com.insidious.plugin.ui;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.Components.UnloggedGPTNavigationBar;
import com.insidious.plugin.ui.Components.UnloggedGptListener;
import com.intellij.notification.NotificationType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.jcef.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UnloggedGPT implements UnloggedGptListener {

    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel navigationPanel;
    private JScrollPane parentScroll;
    private JPanel mainContentPanel;
    private JPanel footerPanel;
    private JButton discordButton;
    public JBCefBrowser jbCefBrowser;
    private UnloggedGPTNavigationBar navigationBar;
    private PsiClass currentClass;
    private PsiMethod currentMethod;
    private String chatURL = "https://chat.openai.com/chat";

    public JComponent getComponent()
    {
        return mainPanel;
    }
    public UnloggedGPT()
    {
        loadNav();
        loadChatGPTBrowserView();
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
        discordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void loadNav()
    {
        this.navigationPanel.removeAll();
        navigationBar = new UnloggedGPTNavigationBar(this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(navigationBar.getComponent(), constraints);
        this.navigationPanel.add(gridPanel, BorderLayout.CENTER);
        this.navigationPanel.revalidate();
    }

    public void loadChatGPTBrowserView()
    {
        if (!JBCefApp.isSupported()) {
            return;
        }
        jbCefBrowser = new JBCefBrowser();
        this.borderParent.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        JBCefCookieManager jbCefCookieManager = new JBCefCookieManager();
        //jbCefBrowser.setProperty("userAgent","Chrome");
        jbCefBrowser.loadURL(chatURL);
    }

    public void triggerClick(String type)
    {
        String mode = type;
        String queryPrefix = "";
        String methodCode = "";

        if(jbCefBrowser==null)
        {
            return;
        }
        if(this.currentMethod==null)
        {
            InsidiousNotification.notifyMessage("Please select a method from the editor.",
                    NotificationType.INFORMATION);
            return;
        }
        String lastMethodCode = this.currentMethod.getText();
        methodCode = lastMethodCode;

        switch (mode.trim())
        {
            case "Optimize":
//                System.out.println("Opt");
                queryPrefix = "Optimize the following code "+methodCode;
                break;
            case "Find Bugs":
//                System.out.println("Find bugs");
                queryPrefix = "Find possible bugs in the following code "+methodCode;
                break;
            case "Refactor":
//                System.out.println("Refactor");
                queryPrefix = "Refactor the following code "+methodCode;
                break;
            default:
//                System.out.println("Explain");
                queryPrefix = "Explain the following code  "+methodCode;
        }

        String code = ("var textAreaE = document.getElementsByTagName(\"textArea\")[0];" +
                "textAreaE.value = '"+queryPrefix+"';" +
                "var btn = textAreaE.parentNode.childNodes[1];" +
                "btn.click();"
        ).trim();
        code = code.replaceAll("[\r\n]+", " ");
//        System.out.println("Code -> "+code);
        jbCefBrowser.getCefBrowser().executeJavaScript(code,jbCefBrowser.getCefBrowser().getURL(),0);

    }

    @Override
    public void triggerCallOfType(String type) {
        triggerClick(type);
    }

    @Override
    public void refreshPage() {
        if(jbCefBrowser!=null)
        {
            jbCefBrowser.loadURL(chatURL);
        }
        else {
            loadChatGPTBrowserView();
        }
    }

    @Override
    public void goBack()
    {
        String code = ("history.back();").trim();
        code = code.replaceAll("[\r\n]+", " ");
        jbCefBrowser.getCefBrowser().executeJavaScript(code,jbCefBrowser.getCefBrowser().getURL(),0);

    }

    public void updateUI(PsiClass psiClass, PsiMethod method) {
        this.currentMethod = method;
        this.currentClass = psiClass;
        if(this.navigationBar!=null)
        {
            this.navigationBar.updateSelection(psiClass.getName()+"."+method.getName()+"()");
        }
    }

    private void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_GPT",null);
    }
}
