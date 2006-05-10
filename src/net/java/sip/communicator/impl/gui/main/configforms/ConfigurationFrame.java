/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommList;
import net.java.sip.communicator.impl.gui.main.customcontrols.TitlePanel;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;

/**
 * @author Yana Stamcheva
 * 
 */
public class ConfigurationFrame extends JFrame 
    implements MouseListener {

	private Vector configContainer = new Vector();

	private JScrollPane formScrollPane = new JScrollPane();

	private SIPCommList configList = new SIPCommList();
	
	private TitlePanel titlePanel = new TitlePanel();
	
	private JPanel centerPanel = new JPanel(new BorderLayout());

	public ConfigurationFrame() {

		this.getContentPane().setLayout(new BorderLayout());

		this.addDefaultForms();

		this.centerPanel.add(formScrollPane, BorderLayout.CENTER);
		
		this.getContentPane().add(centerPanel, BorderLayout.CENTER);

		this.getContentPane().add(configList, BorderLayout.WEST);
	}

	public void addDefaultForms() {

		this.addConfigurationForm(new GeneralConfigurationForm());
		this.addConfigurationForm(new AppearanceConfigurationForm());
		this.addConfigurationForm(new AccountsConfigurationForm());
	}

	public void addConfigurationForm(ConfigurationForm configForm) {

		this.configContainer.add(configForm);

		ConfigMenuItemPanel configItem = new ConfigMenuItemPanel(configForm
				.getTitle(), configForm.getIcon());

		configItem.addMouseListener(this);

		this.configList.addCell(configItem);
	}

	public void removeConfigurationForm(ConfigurationForm configForm) {

		this.configContainer.remove(configForm);
	}

	/**
	 * Calculates the size of the frame depending on the size of the largest
	 * contained form.
	 */
	public void setCalculatedSize() {

		double width = 0;

		double height = 0;

		for (int i = 0; i < configContainer.size(); i++) {

			ConfigurationForm configForm = (ConfigurationForm) configContainer
					.get(i);

			if (width < configForm.getForm().getPreferredSize().getWidth())
				width = configForm.getForm().getPreferredSize().getWidth();

			if (height < configForm.getForm().getPreferredSize().getHeight())
				height = configForm.getForm().getPreferredSize().getHeight();
		}

		if (width > Constants.CONFIG_FRAME_MAX_WIDTH)
			width = Constants.CONFIG_FRAME_MAX_WIDTH;

		if (height > Constants.CONFIG_FRAME_MAX_HEIGHT)
			height = Constants.CONFIG_FRAME_MAX_HEIGHT;

		width = width	+ configList.getPreferredSize().getWidth();
		
		height = height + titlePanel.getPreferredSize().getHeight();
		
		this.setSize((int)width + 50, (int) height + 50);
	}

	
	public void mouseClicked(MouseEvent e) {
		
		ConfigMenuItemPanel configItemPanel = (ConfigMenuItemPanel) e
				.getSource();

		this.configList.refreshCellStatus(configItemPanel);

		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {

			for (int i = 0; i < this.configContainer.size(); i++) {

				ConfigurationForm configForm = 
										(ConfigurationForm) this.configContainer.get(i);

				if (configItemPanel.getText().equals(configForm.getTitle())) {

					this.formScrollPane.getViewport().removeAll();

					this.formScrollPane.getViewport().add(configForm.getForm());

					this.titlePanel.removeAll();
					
					this.titlePanel.setTitleText(configForm.getTitle());
					
					this.centerPanel.remove(titlePanel);
					
					this.centerPanel.add(titlePanel, BorderLayout.NORTH);
					
					this.validate();
				}
			}
		}
	}

	public void mouseEntered(MouseEvent e) {		
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {		
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void paint(Graphics g) {

		AntialiasingManager.activateAntialiasing(g);

		super.paint(g);
	}
}
