/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.JFileChooser;

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
	 * Creates a file dialog (AWT's FileDialog or Swing's JFileChooser) regarding to
	 * user's operating system.
	 * 
	 * @param parent the parent Frame/JFrame of this dialog
	 * @param title dialog's title
	 * @return SipCommFileChooser an implementation of SipCommFileChooser
	 */
	public static SipCommFileChooser create(
			Frame parent, String title, int fileOperation)
	{
		int operation = -1;
	
		if(OSUtils.IS_MAC)
		{
			if(fileOperation == SipCommFileChooser.LOAD_FILE_OPERATION)
				operation = FileDialog.LOAD;
			else if(fileOperation == SipCommFileChooser.SAVE_FILE_OPERATION)
				operation = FileDialog.SAVE;
			else
				try 
				{
					throw new Exception("UnknownFileOperation");
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
	
			return new SipCommFileDialogImpl(parent, title, operation);
		}
		else
		{
			if(fileOperation == SipCommFileChooser.LOAD_FILE_OPERATION)
				operation = JFileChooser.OPEN_DIALOG;
			else if(fileOperation == SipCommFileChooser.SAVE_FILE_OPERATION)
				operation = JFileChooser.SAVE_DIALOG;
			else
				try 
				{
					throw new Exception("UnknownFileOperation");
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				
			return new SipCommFileChooserImpl(title, operation);
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
			Frame parent, String title, int fileOperation, String path)
	{
		SipCommFileChooser scfc = 
			GenericFileDialog.create(parent, title, fileOperation);
		
		if(path != null)
			scfc.setStartPath(path);
		
		return scfc;
	}
	
}
