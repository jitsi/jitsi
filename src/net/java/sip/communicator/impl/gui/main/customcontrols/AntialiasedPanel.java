package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class AntialiasedPanel extends JPanel {

	public AntialiasedPanel(LayoutManager layout){
		super(layout);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}
}
