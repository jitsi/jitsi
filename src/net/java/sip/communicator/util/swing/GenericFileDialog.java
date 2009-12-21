/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import net.java.sip.communicator.util.*;


/**
 * This class is the entry point for creating a file dialog regarding to the OS.
 * 
 *  If the current operating system is Apple Mac OS X, we create an AWT
 *  FileDialog (user interface is more practical under Mac OS than a 
 *  JFileChooser), else, a Swing JFileChooser.
 * 
 * @author Valentin Martinet
 */
public class GenericFileDialog 
{
	/**
	 * Creates a file dialog (AWT FileDialog or Swing JFileChooser) regarding to
	 * user's operating system.
	 * 
	 * @param parent the parent Frame/JFrame of this dialog
	 * @param title dialog's title
	 * @return SipCommFileChooser an implementation of SipCommFileChooser
	 */
	public static SipCommFileChooser create(Frame parent, String title)
	{
		if(OSUtils.IS_MAC)
		{
			return new SipCommFileDialogImpl(parent, title);
		}
		else
		{
			return new SipCommFileChooserImpl();
		}
	}
	
	/**
	 * Creates a file dialog (AWT FileDialog or Swing JFileChooser) regarding to
	 * user's operating system.
	 * 
	 * @param parent the parent Frame/JFrame of this dialog
	 * @param title dialog's title
	 * @param path start path of this dialog
	 * @return SipCommFileChooser an implementation of SipCommFileChooser
	 */
	public static SipCommFileChooser create(
			Frame parent, String title, String path)
	{
		SipCommFileChooser scfc = GenericFileDialog.create(parent, title);
		scfc.setStartPath(path);
		
		return scfc;
	}
	
}
