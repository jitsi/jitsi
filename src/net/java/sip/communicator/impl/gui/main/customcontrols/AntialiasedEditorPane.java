/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Graphics;

import javax.swing.JEditorPane;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class AntialiasedEditorPane extends JEditorPane {

	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
