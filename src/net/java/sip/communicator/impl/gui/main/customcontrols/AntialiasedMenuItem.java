package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JMenuItem;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class AntialiasedMenuItem extends JMenuItem{

	public AntialiasedMenuItem(String text){
		super(text);
	}
	
	public AntialiasedMenuItem(String text, Icon icon){
		super(text, icon);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
