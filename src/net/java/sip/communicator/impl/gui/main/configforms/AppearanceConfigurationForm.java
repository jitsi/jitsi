/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

public class AppearanceConfigurationForm 
    extends JPanel 
    implements ConfigurationForm {

	private JCheckBox launchOnStartUpCheck = new JCheckBox(Messages
			.getString("launchOnStartUp"));

	
	private JPanel appliBehaviourPanel = new JPanel(new GridLayout(0, 1));

	public AppearanceConfigurationForm(){
		super(new BorderLayout());

		this.appliBehaviourPanel.setBorder(BorderFactory
				.createTitledBorder(Messages.getString("application")));

		this.appliBehaviourPanel.add(launchOnStartUpCheck);
		
		this.add(appliBehaviourPanel, BorderLayout.NORTH);
	}
	
	public String getTitle() {
	
		return Messages.getString("appearance");
	}

	public Icon getIcon() {
		
		return new ImageIcon(ImageLoader.getImage(ImageLoader.QUICK_MENU_SEARCH_ICON));
	}

	public Component getForm() {
		
		return this;
	}

	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
