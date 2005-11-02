package net.java.sip.communicator.impl.gui.main;

import java.awt.Image;

import javax.swing.ImageIcon;

public class Status {
	
	private String 		text;
	private Image	 	icon;
	
	public Status(String text, Image icon){
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
