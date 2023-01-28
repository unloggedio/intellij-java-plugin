package com.insidious.plugin.ui.Components;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.VMoptionsConstructionService;
import com.insidious.plugin.ui.UIUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;
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
    private JButton runWithUnlogged;
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
        this.documentationSection.setBorder(new LineBorder(UIUtils.pink));
        runWithUnlogged.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                runApplicationWithUnlogged();
            }
        });
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
        this.vmOptionsConstructionService.setBasePackage(this.currentBasePackage);
        setVMtext(this.vmOptionsConstructionService.getPrettyVMtext());
    }

    private void copyVMoptions()
    {
        this.vmOptionsConstructionService.setBasePackage(this.currentBasePackage);
        String params = vmOptionsConstructionService.getVMParametersFull();
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
        this.vmOptionsConstructionService.setBasePackage(this.currentBasePackage);
        String link = "https://docs.unlogged.io?parms=" + this.vmOptionsConstructionService
                .getVMParametersFull();
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

    private void runApplicationWithUnlogged() {
        //make run configuration selectable or add vm options to existing run config
        //wip
        System.out.println("[RUNNING WITH UNLOGGED]");
        this.vmOptionsConstructionService.setBasePackage(this.currentBasePackage);
        String params = vmOptionsConstructionService.getVMParametersFull();

            System.out.println("[PARAMS RUN]" + params);
            List<RunnerAndConfigurationSettings> allSettings = insidiousService.getProject().getService(RunManager.class)
                    .getAllSettings();
            for (RunnerAndConfigurationSettings runSetting : allSettings) {
                System.out.println("runner config - " + runSetting.getName());
                if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {

                    logger.info("ApplicationConfiguration config - " + runSetting.getConfiguration()
                            .getName());
                    final ProgramRunner runner = DefaultJavaProgramRunner.getInstance();
                    final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
                    ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
                    applicationConfiguration.setVMParameters(params.trim());
                    try {
                             runner.execute(new ExecutionEnvironment(executor, runner, runSetting,
                                     insidiousService.getProject()), null);
                        break;
                    } catch (Exception e) {
                        logger.error("Failed to start application");
                        System.out.println(e);
                        e.printStackTrace();
                    }
                }
            }
        }
}
