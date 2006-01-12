package net.java.sip.communicator.impl.gui.main.utils;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class AntialiasingManager {

		
	public static void activateAntialiasing (Graphics g){
		
		Graphics2D g2d = (Graphics2D)g;
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                 RenderingHints.VALUE_ANTIALIAS_ON);
	}
}
