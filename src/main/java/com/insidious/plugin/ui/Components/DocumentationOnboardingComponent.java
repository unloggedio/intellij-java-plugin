package com.insidious.plugin.ui.Components;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.VMoptionsConstructionService;
import com.insidious.plugin.ui.UI_Utils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class DocumentationOnboardingComponent {
    private JPanel mainPanel;
    private JPanel documentationSection;
    private JScrollPane scrollParent;
    private JPanel sectionParent;
    private JLabel DocumentationLabel;
    private JPanel borderLayoutParent;
    private JPanel bottomContent;
    private JPanel buttonGroupPanel;
    private JScrollPane vmOptsScroll;
    private JTextPane vmOptionsPanel_1;
    private JLabel label_1;
    private JLabel label_2;
    private JButton copyVMbutton;
    private JButton documentationButton;
    private String currentBasePackage;
    private VMoptionsConstructionService vmOptionsConstructionService = new VMoptionsConstructionService();
    private InsidiousService insidiousService;
    private boolean addOpens = false;
    private static final Logger logger = LoggerUtil.getInstance(DocumentationOnboardingComponent.class);

    public DocumentationOnboardingComponent(InsidiousService insidiousService)
    {
        this.insidiousService = insidiousService;
        copyVMbutton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                copyVMoptions();
            }
        });
        documentationButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDocumentationpage();
            }
        });
        this.documentationSection.setBorder(new LineBorder(UI_Utils.pink));
    }

    public void setAddOpens(boolean add)
    {
        this.addOpens = add;
        vmOptionsConstructionService.setAddopens(add);
        updateUIvmops();
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void setBasePackage(String basepackage)
    {
        this.currentBasePackage = basepackage;
        updateUIvmops();
    }

    public void setVMtext(String vmoptions) {
        vmOptionsPanel_1.setText(vmoptions);
    }

    public void triggerUpdate() {
        updateUIvmops();
    }

    private void updateUIvmops()
    {
        setVMtext(this.vmOptionsConstructionService.getPrettyVMtext(this.currentBasePackage));
    }

    private void copyVMoptions()
    {
        String params = vmOptionsConstructionService.getVMParameters(this.currentBasePackage);
        if(vmOptionsConstructionService.addopens)
        {
           params = new StringBuilder(params).
                   append(" --add-opens=java.base/java.util=ALL-UNNAMED").toString();
        }
        insidiousService.copyToClipboard(params.toString());
        InsidiousNotification.notifyMessage("VM options copied to clipboard.",
                NotificationType.INFORMATION);
    }

    private void routeToDocumentationpage() {
        String link = "https://docs.unlogged.io?parms=" + this.vmOptionsConstructionService
                .getVMParameters(this.currentBasePackage);
        //System.out.println("URL for docs " + link);
        try {
            String decodedURL = URLDecoder.decode(link, StandardCharsets.UTF_8);
            URL url = new URL(decodedURL);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                    url.getQuery(), url.getRef());
            if (Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop.getDesktop()
                            .browse(uri);
                } catch (Exception e) {
                    logger.error("Exception sending parameters to docs. "+e);
                }
            } else {
                //no browser
            }
        } catch (Exception e) {
            link = "https://docs.unlogged.io";
            if (Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop.getDesktop()
                            .browse(java.net.URI.create(link));
                } catch (Exception ex) {
                }
            } else {
                //no browser
            }
        }
    }
}
