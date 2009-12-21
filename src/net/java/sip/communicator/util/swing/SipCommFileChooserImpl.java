/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * SipCommFileChooser implementation for Swing's JFileChooser.
 * 
 * @author Valentin Martinet
 */
public class SipCommFileChooserImpl 
extends JFileChooser
implements SipCommFileChooser
{
	private static final long serialVersionUID = 6858528563334885869L;

	/**
	 * Parent component of this dialog (JFrame, Frame, etc)
	 */
	private Component parent;
	
	/**
	 * Constructor
	 */
	public SipCommFileChooserImpl()
	{
		super();
	}
	
	/**
	 * Constructor
	 * 
	 * @param path
	 */
	public SipCommFileChooserImpl(Component pparent, String path)
	{
		this();
		this.setStartPath(path);
		this.parent = pparent;
	}
	
	/**
	 * Constructor
	 * 
	 * @param path
	 * @param fileOperation
	 */
	public SipCommFileChooserImpl(Component parent, String path, int fileOperation)
	{
		this(parent, path);
		
		if(fileOperation == SipCommFileChooser.LOAD_FILE_OPERATION)
		{
			this.setDialogType(JFileChooser.OPEN_DIALOG);
		}
		else if(fileOperation == SipCommFileChooser.SAVE_FILE_OPERATION)
		{
			this.setDialogType(JFileChooser.SAVE_DIALOG);
		}
		else
		{
			try 
			{
				throw new Exception("UnknownFileOperation");
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the selected file by the user from the dialog.
	 * 
	 * @return File the selected file from the dialog
 	 */
	public File getApprovedFile() 
	{
		return this.getSelectedFile();
	}

	/**
	 * Sets the default path to be considered for browsing among files.
	 * 
	 * @param path the default start path for this dialog
	 */
	public void setStartPath(String path) 
	{
		if(path == null)
		{
			// passing null makes file chooser points to user's default dir
			this.setCurrentDirectory(null);	
		}
		else
		{
			this.setCurrentDirectory(new File(path));
		}
	}
	
	/**
	 * Shows the dialog and returns the selected file.
	 * 
	 * @return File the selected file in this dialog
	 */
	public File getFileFromDialog()
	{
		int choice = -1;
		
		if(this.getDialogType() == JFileChooser.OPEN_DIALOG)
		{
			choice = this.showOpenDialog(this.getParentComponent());
		}
		else
		{
			choice = this.showSaveDialog(this.getParentComponent());
		}
		
		if(choice == JFileChooser.APPROVE_OPTION)
		{
			return this.getSelectedFile();
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Returns the parent component of this dialog
	 * 
	 * @return Component dialog's parent component
	 */
	public Component getParentComponent()
	{
		return this.parent;
	}

	/**
	 * Adds a file filter to this dialog.
	 * 
	 * @param filter the filter to add
	 */
	public void addFilter(SipCommFileFilter filter) 
	{
		this.addChoosableFileFilter(filter);
	}
}
