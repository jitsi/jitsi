/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.customcontrols.tabbedPane;
/*
 * The following code is borrowed from
 * David Bismut, davidou@mageos.com
 * Intern, SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004
 * Ecole des Mines de Nantes, France
 */

import java.awt.Event;
import java.awt.event.MouseEvent;

/**
 * @author David_211245
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CloseTabbedPaneEvent extends Event {
	
	private String description;
	private MouseEvent e;
	private int overTabIndex;
	

	public CloseTabbedPaneEvent(MouseEvent e, String description, int overTabIndex){
		super(null, 0, null);
		this.e = e;
		this.description = description;
		this.overTabIndex = overTabIndex;
	}
	
	public String getDescription(){
		return description;
	}

	public MouseEvent getMouseEvent(){
		return e;
	}
	
	public int getOverTabIndex(){
		return overTabIndex;
	}
}
