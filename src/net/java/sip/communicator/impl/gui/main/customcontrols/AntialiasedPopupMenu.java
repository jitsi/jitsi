/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Graphics;

import javax.swing.JPopupMenu;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

public class AntialiasedPopupMenu extends JPopupMenu {

	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
