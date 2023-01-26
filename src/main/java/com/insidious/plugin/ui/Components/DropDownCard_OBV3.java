package com.insidious.plugin.ui.Components;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DropDownCard_OBV3 {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel card_Heading;
    private JComboBox itemSelector;
    private JLabel card_Description;
    private JPanel refreshPanel;
    private JButton refreshButton;
    public JPanel getComponent() {
        return this.mainPanel;
    }
    private CardSelectionActionListener listener;
    private DropdownCardInformation content;

    public DropDownCard_OBV3(DropdownCardInformation content, CardSelectionActionListener listener)
    {
        this.content = content;
        this.listener = listener;
        this.card_Heading.setText(content.heading);
        this.card_Description.setText(content.description);
        DefaultComboBoxModel module_model = new DefaultComboBoxModel();
        module_model.addAll(content.options);
        itemSelector.setModel(module_model);
        itemSelector.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String moduleName = event.getItem().toString();
                listener.selectedOption(moduleName,content.getType());
            }
        });
        if(content.options.size()>0)
        {
            int index=0;
            if(content.defaultSelected!=null)
            {
                index=content.defaultSelected;
            }
            itemSelector.setSelectedIndex(index);
        }
        this.refreshButton.setVisible(content.isShowRefresh());
        this.refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                triggerRefresh();
            }
        });
    }

    void triggerRefresh()
    {
        if(this.refreshButton.isVisible())
        {
            if(this.content.getType().equals(OnboardingScaffold_v3.DROP_TYPES.MODULE))
            {
                listener.refreshModules();
            }
            else if(this.content.getType().equals(OnboardingScaffold_v3.DROP_TYPES.SERIALIZER))
            {
                listener.refreshSerializerSelection();
            }
        }
    }
}
