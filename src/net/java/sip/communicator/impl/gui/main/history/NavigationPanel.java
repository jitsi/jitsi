/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.history;

import java.awt.FlowLayout;
import java.awt.Graphics;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;

public class NavigationPanel extends JPanel {

	private JButton nextPageButton = new JButton(Messages.getString("next"));
	
	private JButton previousPageButton = new JButton(Messages.getString("previous"));
	
	private JButton lastPageButton = new JButton(Messages.getString("last"));
	
	private JButton firstPageButton = new JButton(Messages.getString("first"));
	
	public NavigationPanel(){
		super (new FlowLayout(FlowLayout.CENTER));
		
		this.add(firstPageButton);
		this.add(previousPageButton);
		this.add(nextPageButton);
		this.add(lastPageButton);
	}
}
