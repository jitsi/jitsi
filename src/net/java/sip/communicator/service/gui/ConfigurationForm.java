/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.awt.Component;

import javax.swing.Icon;

/**
 * @author Yana Stamcheva
 *
 */
public interface ConfigurationForm {

	/**
	 * 
	 * @return the title of this configuration form.
	 */
	public String getTitle();
	
	/**
	 * 
	 * @return the icon to be showed near the title of this configuration form.
	 */
	public Icon getIcon();
	
	/**
	 * 
	 * @return the configuration form itself.
	 */
	public Component getForm();
}
