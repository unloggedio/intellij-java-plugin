package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.library.DeclaredMockItemPanel;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
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
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionListPanel implements DeclaredMockLifecycleListener, OnSaveListener {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionListPanel.class);
    private final InsidiousService insidiousService;
    private final MethodUnderTest methodUnderTest;
    private final PsiMethodCallExpression methodCallExpression;
    //    private final OnOffButton fieldMockSwitch;
    private final PsiMethod targetMethod;
    private final ItemLifeCycleListener<DeclaredMock> itemLifeCycleListener = new ItemLifeCycleListener<>() {
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
            loadDefinitions(false);
        }

        @Override
        public void onEdit(DeclaredMock item) {
            insidiousService.showMockEditor(item);
        }
    };
    private JLabel mockedMethodText;
    //    private JButton addNewMockButton;
    private JPanel savedMocksListParent;
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
    private final Set<DeclaredMock> selectedMocks = new HashSet<>();

    public MockDefinitionListPanel(PsiMethodCallExpression methodCallExpression) {
        this.methodCallExpression = methodCallExpression;
        infoPanelTitleLabel.setIcon(AllIcons.General.Information);
        enabledMockInfoLabel.setIcon(AllIcons.General.Information);
        infoItemLine1.setIcon(AllIcons.General.Modified);
        infoItemLine2.setIcon(AllIcons.General.Modified);
        infoItemLine3.setIcon(AllIcons.General.Modified);



        PsiClass parentOfType = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass.class);
        insidiousService = methodCallExpression.getProject().getService(InsidiousService.class);
        String parentClassName = parentOfType.getQualifiedName();
        PsiExpression fieldExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        String fieldName = fieldExpression.getText();
        targetMethod = methodCallExpression.resolveMethod();
        assert targetMethod != null;


        methodUnderTest = MethodUnderTest.fromPsiCallExpression(methodCallExpression);


        targetMethod.getParameterList();
        int argumentCount = targetMethod.getParameterList().getParametersCount();
        String argumentCountText = "<small>" + (argumentCount == 1 ? "1 Argument" : (argumentCount + " Arguments")) +
                "</small>";
        String text = "<html>" + methodCallExpression.getMethodExpression().getText()
                + "( " + argumentCountText + " )" + "</html>";
        mockedMethodText.setText(
                text
        );


        AnAction addAction = new AnAction(() -> "Add", AllIcons.General.Add) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };

        AnAction refreshAction = new AnAction(AllIcons.Actions.Refresh) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                loadDefinitions(false);
            }
        };

        AnAction enableMocksAction = new AnAction(() -> "Inject", UIUtils.LINK_ICON) {

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
                insidiousService.injectMocksInRunningProcess(selectedMocks);
            }



            @Override
            public boolean displayTextInToolbar() {
                return true;
            }

        };


        AnAction disableMocksAction = new AnAction(() -> "Remove", UIUtils.UNLINK_ICON) {

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

                insidiousService.removeMocksInRunningProcess(selectedMocks);
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


//        addNewMockButton.setIcon(AllIcons.General.Add);
//        addNewMockButton.addActionListener(e -> {
//            insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
//        });
        loadDefinitions(true);


    }

    private void loadDefinitions(boolean showAddNewIfEmpty) {
        declaredMockList = insidiousService.getDeclaredMocksOf(methodUnderTest);

        int savedCandidateCount = declaredMockList.size();


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
        final int PANEL_HEIGHT = 108;
        if (savedCandidateCount == 0 && showAddNewIfEmpty) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
            });
        } else {


            for (int i = 0; i < savedCandidateCount; i++) {
                DeclaredMock declaredMock = declaredMockList.get(i);

                DeclaredMockItemPanel declaredMockItemPanel = new DeclaredMockItemPanel(declaredMock,
                        itemLifeCycleListener, insidiousService);


                GridConstraints constraints = new GridConstraints(
                        i, 0, 1, 1, ANCHOR_NORTH,
                        GridConstraints.FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK,
                        SIZEPOLICY_FIXED,
                        new Dimension(-1, PANEL_HEIGHT),
                        new Dimension(-1, PANEL_HEIGHT),
                        new Dimension(-1, PANEL_HEIGHT)
                );
                Component component = declaredMockItemPanel.getComponent();
                itemListPanel.add(component, createGBCForLeftMainComponent(itemListPanel.getComponentCount()));
            }
            itemListPanel.add(new JPanel(), createGBCForFakeComponent(itemListPanel.getComponentCount()));

            savedItemScrollPanel.setBorder(BorderFactory.createEmptyBorder());
            int containerHeight = Math.min(300, savedCandidateCount * PANEL_HEIGHT);

            savedItemScrollPanel.setPreferredSize(new Dimension(-1, containerHeight));
            savedItemScrollPanel.setSize(new Dimension(-1, containerHeight));


            itemListPanel.revalidate();
            itemListPanel.repaint();
            savedItemScrollPanel.revalidate();
            savedItemScrollPanel.repaint();
            mainPanel.repaint();
            mainPanel.revalidate();
            if (componentPopUp != null) {
                Dimension currentSize = componentPopUp.getSize();
                if (currentSize != null) {
                    componentPopUp.setSize(new Dimension((int) currentSize.getWidth(), containerHeight + 140));
                }
            }
        }
    }

//    public void showMockEditor(DeclaredMock declaredMock) {
//        JBPopup editorPopup = null;
//
//        MockDefinitionEditor mockDefinitionEditor;
//        if (declaredMock == null) {
//            mockDefinitionEditor = ApplicationManager.getApplication().runReadAction(
//                    (Computable<MockDefinitionEditor>) () -> new MockDefinitionEditor(methodUnderTest,
//                            methodCallExpression, methodCallExpression.getProject(), this,
//                            component -> {
//
//                            }));
//        } else {
//            mockDefinitionEditor = ApplicationManager.getApplication().runReadAction(
//                    (Computable<MockDefinitionEditor>) () -> new MockDefinitionEditor(methodUnderTest,
//                            new DeclaredMock(declaredMock), methodCallExpression.getProject(), this));
//        }
//
//        JComponent gutterMethodComponent = mockDefinitionEditor.getComponent();
//
//        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(gutterMethodComponent, null);
//
//        editorPopup = gutterMethodComponentPopup
//                .setProject(methodCallExpression.getProject())
//                .setShowBorder(true)
//                .setShowShadow(true)
//                .setFocusable(true)
//                .setRequestFocus(true)
//                .setResizable(true)
//                .setCancelOnClickOutside(true)
//                .setCancelOnOtherWindowOpen(true)
//                .setCancelKeyEnabled(true)
//                .setBelongsToGlobalPopupStack(false)
//                .setTitle("Mock Editor")
//                .addListener(new JBPopupListener() {
//                    @Override
//                    public void onClosed(LightweightWindowEvent event) {
//                        JBPopupListener.super.onClosed(event);
//                        ApplicationManager.getApplication().invokeLater(() -> {
//                            loadDefinitions(false);
//                        });
//                    }
//                })
//                .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
//                .createPopup();
//        JBPopup finalEditorPopup = editorPopup;
//        ApplicationManager.getApplication().invokeLater(() -> {
//            finalEditorPopup.showUnderneathOf(addNewMockButton);
//        });
//
//    }

    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void onUpdateRequest(DeclaredMock declaredMock) {
        insidiousService.showMockCreator(new JavaMethodAdapter(targetMethod), methodCallExpression);
    }

    @Override
    public void onDeleteRequest(DeclaredMock declaredMock) {
        insidiousService.deleteMockDefinition(declaredMock);
        ApplicationManager.getApplication().invokeLater(() -> {
            loadDefinitions(false);
        });
    }

    @Override
    public void onEnable(DeclaredMock declaredMock) {
        insidiousService.enableMock(declaredMock);
//        if (!fieldMockSwitch.isSelected()) {
//            fieldMockSwitch.setSelected(true);
//        }
    }

    @Override
    public void onDisable(DeclaredMock declaredMock) {
        insidiousService.disableMock(declaredMock);
    }

    public void setPopupHandle(JBPopup componentPopUp) {
        this.componentPopUp = componentPopUp;
    }

    @Override
    public void onSaveDeclaredMock(DeclaredMock declaredMock) {
        insidiousService.saveMockDefinition(declaredMock);
        insidiousService.enableMock(declaredMock);
//        insidiousService.enableFieldMock(parentClassName, fieldName);
//        fieldMockSwitch.setSelected(true);
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
