/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JMenu;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

public class AntialiasedMenu extends JMenu {

	public AntialiasedMenu(String text){
	
		super(text);	
	}
	
	public AntialiasedMenu(String text, Icon icon){
		
		super(text);
		
		this.setIcon(icon);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
