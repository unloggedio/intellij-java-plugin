package com.insidious.plugin.ui;

import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UnloggedOnboardingScreenV2 {

	private JPanel seperatorPanel(int height) {

		// left panel
		JPanel leftPanel = new JPanel();
		leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, JBColor.BLACK));
	
		// right panel
		JPanel rightPanel = new JPanel();
		rightPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, JBColor.BLACK));
	
		// configure
		JPanel seperatorPanel = new JPanel();
		seperatorPanel.setLayout(new BoxLayout(seperatorPanel, BoxLayout.X_AXIS));
		seperatorPanel.add(leftPanel);
		seperatorPanel.add(rightPanel);
		seperatorPanel.setMinimumSize(new Dimension(100,height));
		seperatorPanel.setMaximumSize(new Dimension(100,height));
		return seperatorPanel;
	}

    private JPanel panelMain;

    public UnloggedOnboardingScreenV2() {
		// panelMain properties
        panelMain = new JPanel();
        panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.Y_AXIS));
		int screenWidth = 1000;

        // panel1
		// text label
		JLabel panel1text = new JLabel();
		panel1text.setText("<html>Congratulations you're all set!</html>");
		panel1text.setBorder(new EmptyBorder(0,0,0,0));
		panel1text.setForeground(new Color(75, 164, 21));
		panel1text.setFont(new Font("SF Pro Text", Font.BOLD, 18));

		// configure
		JPanel panel1 = new JPanel();
		panel1.add(panel1text);
		panel1.setMinimumSize(new Dimension(screenWidth, 40));
		panel1.setMaximumSize(new Dimension(screenWidth, 40));
		panelMain.add(panel1);


		// panel2
		JPanel panel2 = this.seperatorPanel(43);
		panelMain.add(panel2);


		// panel3
		// postman panel
		JPanel postmanPanel = new JPanel();
        postmanPanel.setLayout(new BorderLayout());
		JLabel postmanLabel = new JLabel();
		postmanLabel.setIcon(UIUtils.POSTMAN);
		postmanPanel.add(postmanLabel, BorderLayout.NORTH);
		postmanPanel.setSize(new Dimension(27,53));

		// mid panel
		JPanel panel3mid = new JPanel();
		JLabel panel3midText = new JLabel();
		panel3midText.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
		panel3midText.setText("<html>You can now start making API calls<br>from <font color=\"#5AED00\"> Swagger </font>, <font color=\"#FF6C37\">Postman</font>, etc!</html>");
		panel3mid.add(panel3midText);

		// swagger panel
		JPanel swaggerPanel = new JPanel();
        swaggerPanel.setLayout(new BorderLayout());
		JLabel swaggerLabel = new JLabel();
		swaggerLabel.setIcon(UIUtils.SWAGGER);
		swaggerPanel.add(swaggerLabel, BorderLayout.EAST);
		swaggerPanel.setSize(new Dimension(27,53));


		// configure
		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));
		panel3.add(postmanPanel);
		panel3.add(panel3mid);
		panel3.add(swaggerPanel);
		panel3.setMaximumSize(new Dimension(screenWidth, 60));
		panel3.setMinimumSize(new Dimension(screenWidth, 60));
		panelMain.add(panel3);


		// panel4
		JPanel panel4 = this.seperatorPanel(30);
		panelMain.add(panel4);


		// panel5
		// main icon 
		JPanel mainIconPanel = new JPanel();
		JLabel mainIconLabel = new JLabel();
		mainIconLabel.setIcon(UIUtils.UNLOGGED_ONBOARDING);
		mainIconPanel.add(mainIconLabel);
		mainIconPanel.setMaximumSize(new Dimension(screenWidth, 50));
		mainIconPanel.setMinimumSize(new Dimension(screenWidth, 50));

		// text panel
		JPanel mainIconText = new JPanel();
		JLabel mainIconTextLabel = new JLabel();
		mainIconTextLabel.setText("<html>But before you do....</html>");
		mainIconTextLabel.setFont(new Font("SF Pro Text", Font.PLAIN, 14));
		mainIconText.add(mainIconTextLabel);
		mainIconText.setMaximumSize(new Dimension(screenWidth, 30));
		mainIconText.setMinimumSize(new Dimension(screenWidth, 30));

		// configure
		JPanel panel5 = new JPanel();
        panel5.setLayout(new BoxLayout(panel5, BoxLayout.Y_AXIS));
		panel5.add(mainIconPanel);
		panel5.add(mainIconText);
		panelMain.add(panel5);

		
		// panel6
		JPanel panel6 = this.seperatorPanel(50);
		panelMain.add(panel6);


		// panel7
		// upper text
		JPanel panel7upper = new JPanel();
		JLabel panel7upperLabel = new JLabel();
		panel7upperLabel.setIcon(UIUtils.PLAY_ARROW);
		panel7upperLabel.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
		panel7upperLabel.setText("<html><b>Here's some starter content for you</b></html>");
		panel7upper.add(panel7upperLabel);
		panel7upper.setMaximumSize(new Dimension(screenWidth, 30));
		panel7upper.setMinimumSize(new Dimension(screenWidth, 30));

		// lower text
		JPanel panel7lower = new JPanel();
		JLabel panel7lowerText = new JLabel();
		panel7lowerText.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
		panel7lowerText.setText("<html>Checkout out our Youtube channel where we<br>talk about the tool and it’s features.</html>");
		panel7lower.add(panel7lowerText);
		panel7lower.setMaximumSize(new Dimension(screenWidth, 45));
		panel7lower.setMinimumSize(new Dimension(screenWidth, 45));

		// video panel
		JPanel videoPanel = new JPanel();
		JLabel videoLabel = new JLabel();
		videoLabel.setIcon(UIUtils.VIDEO_BANNER);
		videoLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("https://www.youtube.com/@unloggedio/featured"));
				}
				catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
			
		});
		videoPanel.add(videoLabel);
		videoPanel.setMaximumSize(new Dimension(screenWidth, 220));
		videoPanel.setMinimumSize(new Dimension(screenWidth, 220));

		// configure
		JPanel panel7 = new JPanel();
        panel7.setLayout(new BoxLayout(panel7, BoxLayout.Y_AXIS));
		panel7.add(panel7upper);
		panel7.add(panel7lower);
		panel7.add(videoPanel);
		panelMain.add(panel7);


		// panel8
		JPanel panel8 = this.seperatorPanel(80);
		panelMain.add(panel8);


		// panel9
		JPanel panel9 = new JPanel();
		JLabel panel9label = new JLabel();
		panel9label.setIcon(UIUtils.BELL_ICON);
		panel9label.setText("<html><b>Stay updated!</b></html>");
		panel9label.setFont(new Font("SF Pro Text", Font.PLAIN, 15));
		panel9.add(panel9label);
		panel9.setMaximumSize(new Dimension(screenWidth, 30));
		panel9.setMinimumSize(new Dimension(screenWidth, 30));
		panelMain.add(panel9);


		// panel10
		// panel10left
		JPanel panel10left = new JPanel();
		JLabel panel10leftText = new JLabel();
		panel10leftText.setHorizontalTextPosition(SwingConstants.LEFT);
		panel10leftText.setIcon(UIUtils.LINK_ARROW);
		panel10leftText.setText("<html><font color=\"#589DF6\">Discord</font></html>");
		panel10leftText.setFont(new Font("SF Pro Text", Font.PLAIN, 15));
		panel10leftText.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("https://discord.gg/Hhwvay8uTa"));
				}
				catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
			
		});
		panel10left.add(panel10leftText);

		// panel10right
		JPanel panel10right = new JPanel();
		JLabel panel10rightText = new JLabel();
		panel10rightText.setHorizontalTextPosition(SwingConstants.LEFT);
		panel10rightText.setIcon(UIUtils.LINK_ARROW);
		panel10rightText.setText("<html><font color=\"#589DF6\">Website</font></html>");
		panel10rightText.setFont(new Font("SF Pro Text", Font.PLAIN, 15));
		panel10rightText.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("https://www.unlogged.io/"));
				}
				catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}
			
		});
		panel10right.add(panel10rightText);

		// configure
		JPanel panel10 = new JPanel();
        panel10.setLayout(new BoxLayout(panel10, BoxLayout.X_AXIS));
		panel10.add(panel10left);
		panel10.add(panel10right);
		panel10.setMaximumSize(new Dimension(screenWidth, 30));
		panel10.setMinimumSize(new Dimension(screenWidth, 30));
		panelMain.add(panel10);
    }

    public JPanel getComponent() {
        return panelMain;
    }
}
