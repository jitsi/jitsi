/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.JSeparator;
import javax.swing.JToolBar;

import net.java.sip.communicator.impl.gui.main.ui.SIPCommToolBarSeparatorUI;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

public class SIPCommToolBar extends JToolBar {

	public SIPCommToolBar(){
		
		this.add(Box.createRigidArea(new Dimension(4, 4)));
	}
	
	public void addSeparator()
	{		
	    JToolBar.Separator s = new JToolBar.Separator(new Dimension(8, 22));
	    s.setUI(new SIPCommToolBarSeparatorUI());
		
	    if (getOrientation() == VERTICAL) {
		    s.setOrientation(JSeparator.HORIZONTAL);
		} else {
		    s.setOrientation(JSeparator.VERTICAL);
		}
		
		add(s);
	}
	
	protected void paintBorder(Graphics g){
		
		Graphics2D g2 = (Graphics2D)g;
		
		Image dragImage = ImageLoader.getImage(ImageLoader.TOOLBAR_DRAG_ICON);
		
		g2.drawImage(dragImage, 
					 0, (this.getHeight() - dragImage.getHeight(null))/2 - 2, null);
	}
}
