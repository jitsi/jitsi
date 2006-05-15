/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.Graphics;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

public class MessageWindowMenuBar extends JMenuBar {

	private FileMenu fileMenu;
	
	private EditMenu editMenu; 
	
	private JMenu settingsMenu = new JMenu(Messages.getString("settings"));
	
	private JMenu helpMenu = new JMenu(Messages.getString("help"));		
	
	private ChatWindow parentWindow;
		
	public MessageWindowMenuBar(ChatWindow parentWindow){
		
		this.parentWindow = parentWindow;
		
		fileMenu = new FileMenu(this.parentWindow);
		
		editMenu = new EditMenu(this.parentWindow);
		
		this.init();		
	}
	
	public void init(){		
	
		this.add(fileMenu);
		
		this.add(editMenu);
		
		this.add(settingsMenu);
		
		this.add(helpMenu);
		
		//Disable all menus that are not yet implemented.
		this.settingsMenu.setEnabled(false);
		this.helpMenu.setEnabled(false);
	}	
}
