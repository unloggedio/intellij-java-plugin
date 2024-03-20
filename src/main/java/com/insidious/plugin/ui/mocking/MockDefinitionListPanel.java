package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.library.DeclaredMockItemPanel;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;
import com.insidious.plugin.ui.stomp.TestCandidateSaveForm;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MockDefinitionListPanel implements OnSaveListener {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionListPanel.class);
    private final InsidiousService insidiousService;
    private final MethodUnderTest methodUnderTest;
    private final PsiMethodCallExpression methodCallExpression;
    //    private final OnOffButton fieldMockSwitch;
    private final PsiMethod targetMethod;
    private final Set<DeclaredMock> selectedMocks = new HashSet<>();
    private final List<DeclaredMock> unsavedMocks = new ArrayList<>();
    private JLabel mockedMethodText;
    //    private JButton addNewMockButton;
    private JPanel savedMocksListParent;    private final ItemLifeCycleListener<DeclaredMock> itemLifeCycleListener = new ItemLifeCycleListener<>() {
        @Override
        public void onSelect(DeclaredMock item) {
            selectedMocks.add(item);
        }

        @Override
        public void onClick(DeclaredMock item) {

        }

        @Override
        public void onUnSelect(DeclaredMock item) {
            selectedMocks.remove(item);

        }

        @Override
        public void onDelete(DeclaredMock item) {
            insidiousService.deleteMockDefinition(item);
            loadDefinitions(false, true);
        }

        @Override
        public void onEdit(DeclaredMock item) {
            insidiousService.showMockEditor(item, declaredMock -> {
                unsavedMocks.remove(item);
                loadDefinitions(false, true);
            });
        }
    };
    private JPanel controlPanel;
    private JPanel mainPanel;
    private JScrollPane savedItemScrollPanel;
    private JLabel mockCountLabel;
    private JPanel titlePanelParent;
    private JPanel mockDefinitionTitlePanel;
    private JPanel titleEastPanel;
    private JPanel northPanel;
    private JPanel mockInfoPanel;
    private JLabel infoPanelTitleLabel;
    private JLabel infoItemLine1;
    private JLabel infoItemLine2;
    private JLabel infoItemLine3;
    private JLabel enabledMockInfoLabel;
    private JBPopup componentPopUp;
    private List<DeclaredMock> declaredMockList;
    public MockDefinitionListPanel(PsiMethodCallExpression methodCallExpression) {
        this.methodCallExpression = methodCallExpression;
        this.insidiousService = methodCallExpression.getProject().getService(InsidiousService.class);

        infoPanelTitleLabel.setIcon(AllIcons.General.Information);
        enabledMockInfoLabel.setIcon(AllIcons.General.Information);
        infoItemLine1.setIcon(AllIcons.General.Modified);
        infoItemLine2.setIcon(AllIcons.General.Modified);
        infoItemLine3.setIcon(AllIcons.General.Modified);


        targetMethod = methodCallExpression.resolveMethod();
        assert targetMethod != null;


        methodUnderTest = MethodUnderTest.fromPsiCallExpression(methodCallExpression);

        CandidateSearchQuery query = new CandidateSearchQuery(
                methodUnderTest, methodUnderTest.getSignature(), new ArrayList<>(),
                CandidateFilterType.METHOD, false
        );
        SessionInstance sessionInstance = insidiousService.getSessionInstance();
        if (sessionInstance == null) {
            InsidiousNotification.notifyMessage("" +
                    "No session found. Please start your application with unlogged-sdk to create a session",
                    NotificationType.ERROR);
            return;
        }
        List<MethodCallExpression> mceList = sessionInstance
                .getMethodCallExpressions(query);

        @Nullable PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCallExpression,
                PsiClass.class);
        if (containingClass == null) {
            InsidiousNotification.notifyMessage("Failed to locate parent class for method call expression: " + methodCallExpression.getText()
            , NotificationType.ERROR);
            throw new IllegalArgumentException(methodCallExpression.getText());
        }
        String sourceClassName = containingClass.getQualifiedName();
        for (MethodCallExpression methodCallExpression1 : mceList) {
            @Nullable DeclaredMock unsavedMock = TestCandidateSaveForm.getDeclaredMock(
                    methodCallExpression1, methodCallExpression, sourceClassName
            );
            if (unsavedMock == null) {
                continue;
            }
            unsavedMock.setName("#unsaved - " + unsavedMock.getName());
            unsavedMocks.add(unsavedMock);
        }


        targetMethod.getParameterList();
        int argumentCount = targetMethod.getParameterList().getParametersCount();
        String argumentCountText = "<small>" + (argumentCount == 1 ? "1 Argument" : (argumentCount + " Arguments")) +
                "</small>";
        String text = "<html>" + methodCallExpression.getMethodExpression().getText()
                + "( " + argumentCountText + " )" + "</html>";
        mockedMethodText.setText(
                text
        );


        AnAction addAction = new AnAction(() -> "Create New", AllIcons.General.Add) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                insidiousService.showMockCreator(
                        new JavaMethodAdapter(targetMethod),
                        methodCallExpression, declaredMock -> loadDefinitions(false, true));
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };

        AnAction refreshAction = new AnAction(AllIcons.Actions.Refresh) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                loadDefinitions(false, true);
            }
        };

        AnAction enableMocksAction = new AnAction(() -> "Mock", UIUtils.LINK_ICON) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!insidiousService.isAgentConnected()) {
                    InsidiousNotification.notifyMessage(
                            "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                            NotificationType.WARNING
                    );
                    return;
                }
                if (selectedMocks.isEmpty()) {
                    InsidiousNotification.notifyMessage(
                            "Select mocks to inject",
                            NotificationType.WARNING
                    );
                    return;
                }
                insidiousService.enableMock(selectedMocks);
                loadDefinitions(false, false);
            }


            @Override
            public boolean displayTextInToolbar() {
                return true;
            }

        };


        AnAction disableMocksAction = new AnAction(() -> "Un-Mock", UIUtils.UNLINK_ICON) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!insidiousService.isAgentConnected()) {
                    InsidiousNotification.notifyMessage(
                            "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                            NotificationType.WARNING
                    );
                    return;
                }
                if (selectedMocks.isEmpty()) {
                    InsidiousNotification.notifyMessage(
                            "Select mocks to remove",
                            NotificationType.WARNING
                    );
                    return;
                }
                insidiousService.disableMock(selectedMocks);
                loadDefinitions(false, false);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };


        List<AnAction> action11 = new ArrayList<>();
        action11.add(refreshAction);
        action11.add(addAction);
        action11.add(enableMocksAction);
        action11.add(disableMocksAction);

        ActionToolbarImpl actionToolbar = new ActionToolbarImpl(
                "Declared Mock Toolbar", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);

        controlPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);


        loadDefinitions(true, true);


    }

    private void loadDefinitions(boolean showAddNewIfEmpty, boolean resizePanel) {
        declaredMockList = insidiousService.getDeclaredMocksOf(methodUnderTest);

        List<DeclaredMock> allMocks = new ArrayList<>(declaredMockList);
        allMocks.addAll(unsavedMocks);
        int savedCandidateCount = allMocks.size();


        JPanel itemListPanel = new JPanel();
        itemListPanel.setLayout(new GridLayout(0, 1));
        GridBagLayout mgr = new GridBagLayout();
        itemListPanel.setLayout(mgr);
        itemListPanel.setAlignmentY(0);
        itemListPanel.setAlignmentX(0);

        savedItemScrollPanel.setViewportView(itemListPanel);
        itemListPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String mockCountLabelText;
        if (savedCandidateCount == 1) {
            mockCountLabelText = savedCandidateCount + " declared mock";
        } else {
            mockCountLabelText = savedCandidateCount + " declared mocks";
        }
        mockCountLabel.setText("<html><small>" + mockCountLabelText + "</html></small>");
        final int PANEL_HEIGHT = 135;
        if (savedCandidateCount == 0 && showAddNewIfEmpty) {
            savedMocksListParent.setVisible(false);
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression,
                        declaredMock -> loadDefinitions(false, true));
            });
        } else {
            savedMocksListParent.setVisible(true);

            for (int i = 0; i < savedCandidateCount; i++) {
                DeclaredMock declaredMock = allMocks.get(i);

                DeclaredMockItemPanel declaredMockItemPanel = new DeclaredMockItemPanel(declaredMock,
                        itemLifeCycleListener, insidiousService);

                if (selectedMocks.contains(declaredMock)) {
                    declaredMockItemPanel.setSelected(true);
                }
                if (unsavedMocks.contains(declaredMock)) {
                    declaredMockItemPanel.setUnsaved(true);
                }

                Component component = declaredMockItemPanel.getComponent();
                itemListPanel.add(component, createGBCForLeftMainComponent(itemListPanel.getComponentCount()));
            }
            itemListPanel.add(new JPanel(), createGBCForFakeComponent(itemListPanel.getComponentCount()));

            savedItemScrollPanel.setBorder(BorderFactory.createEmptyBorder());

            int containerHeight = Math.min(500, itemListPanel.getComponentCount() * PANEL_HEIGHT);
            if (resizePanel) {
                Dimension currentSize = savedItemScrollPanel.getSize();
                if (currentSize.getHeight() <  containerHeight) {
                    savedItemScrollPanel.setPreferredSize(new Dimension(-1, containerHeight));
                    savedItemScrollPanel.setSize(new Dimension(-1, containerHeight));
                }
            }


            itemListPanel.revalidate();
            itemListPanel.repaint();
            savedItemScrollPanel.revalidate();
            savedItemScrollPanel.repaint();
            mainPanel.repaint();
            mainPanel.revalidate();
            if (componentPopUp != null) {
                Dimension currentSize = componentPopUp.getSize();
                if (currentSize != null && resizePanel && currentSize.getHeight() < (containerHeight + 140)) {
                    componentPopUp.setSize(new Dimension((int) currentSize.getWidth(), containerHeight + 140));
                }
            }
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void setPopupHandle(JBPopup componentPopUp) {
        this.componentPopUp = componentPopUp;
    }

    @Override
    public void onSaveDeclaredMock(DeclaredMock declaredMock) {
        insidiousService.saveMockDefinition(declaredMock);
        insidiousService.enableMock(declaredMock);
    }

    private GridBagConstraints createGBCForFakeComponent(int yIndex) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = yIndex;
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

    private GridBagConstraints createGBCForLeftMainComponent(int yIndex) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = yIndex;
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
