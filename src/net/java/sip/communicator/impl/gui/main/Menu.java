/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.Graphics;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

/**
 * @author Yana Stamcheva
 *
 * The main menu. 
 */
public class Menu extends JMenuBar {
	private JMenu userMenu = new JMenu();
	private JMenu toolsMenu = new JMenu();
	private JMenu viewMenu = new JMenu();
	private JMenu helpMenu = new JMenu();
	
	public Menu(){
		this.init();
	}
	
	private void init(){
		userMenu.setText(Messages.getString("file"));
		userMenu.setMnemonic(Messages.getString("mnemonic.file").charAt(0));
		userMenu.setToolTipText(Messages.getString("file"));
		
		toolsMenu.setText(Messages.getString("tools"));
		toolsMenu.setMnemonic(Messages.getString("mnemonic.tools").charAt(0));
		toolsMenu.setToolTipText(Messages.getString("tools"));
		
		viewMenu.setText(Messages.getString("view"));
		viewMenu.setMnemonic(Messages.getString("mnemonic.view").charAt(0));
		viewMenu.setToolTipText(Messages.getString("view"));
		
		helpMenu.setText(Messages.getString("help"));
		helpMenu.setMnemonic(Messages.getString("mnemonic.help").charAt(0));
		helpMenu.setToolTipText(Messages.getString("help"));
		
		this.add(userMenu);
		this.add(toolsMenu);
		this.add(viewMenu);
		this.add(helpMenu);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
