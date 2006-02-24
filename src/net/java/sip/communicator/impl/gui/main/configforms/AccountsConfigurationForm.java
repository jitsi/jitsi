/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.gui.ConfigurationForm;

public class AccountsConfigurationForm extends JPanel implements
		ConfigurationForm {

	public AccountsConfigurationForm(){
		super(new BorderLayout());
	}
	
	public String getTitle() {
		
		return Messages.getString("accounts");
	}

	public Icon getIcon() {
		
		return new ImageIcon(ImageLoader.getImage(ImageLoader.QUICK_MENU_ADD_ICON));
	}

	public Component getForm() {
		
		return this;
	}

}
