package net.java.sip.communicator.impl.gui.main.customcontrols.tabbedPane;
/*
 * The following code borrowed from
 * David Bismut, davidou@mageos.com
 * Intern, SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004
 * Ecole des Mines de Nantes, France
 */


import java.awt.event.MouseEvent;
import java.util.EventListener;

public interface DoubleClickListener extends EventListener {
	public void doubleClickOperation(MouseEvent e);
}
