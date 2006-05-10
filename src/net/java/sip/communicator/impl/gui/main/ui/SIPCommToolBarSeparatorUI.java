/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;

import net.java.sip.communicator.impl.gui.utils.Constants;

public class SIPCommToolBarSeparatorUI extends BasicToolBarSeparatorUI {

	public void paint(Graphics g,
            JComponent c){
		
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(Constants.TOOLBAR_SEPARATOR_COLOR);
		g2.drawLine(c.getWidth()/2, 0, c.getWidth()/2, c.getHeight());
	}
}
