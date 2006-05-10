/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.Image;

import javax.swing.ImageIcon;

public class SelectorBoxItem {
	
	private String 		text;
	private Image	 	icon;
	
	public SelectorBoxItem (String text, Image icon){
		this.text = text;
		this.icon = icon;
	}
	
	public Image getIcon () {
		return icon;
	}
	public void setIcon (Image icon) {
		this.icon = icon;
	}
	public String getText () {
		return text;
	}
	public void setText (String text) {
		this.text = text;
	}
}
