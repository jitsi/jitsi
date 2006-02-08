package net.java.sip.communicator.impl.gui.main.customcontrols;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

import java.awt.*;

import java.awt.event.*;

import java.awt.image.*;



public class TransparentBackground extends JComponent {
	
	private BufferedImage background;

	private Robot robot;
	
	private Window window;
	
	// constructor -------------------------------------------------------------

	public TransparentBackground(Window window) {
		
		this.window = window;
		
		try {

			robot = new Robot();

		} catch (AWTException e) {

			e.printStackTrace();

			return;

		}
	}


	// protected ---------------------------------------------------------------

	public void updateBackground(int x, int y ) {
		
		this.background = robot.createScreenCapture(
			        new Rectangle(x, y, x + this.window.getWidth(),
			                           y + this.window.getHeight()));
			                          
	}

	// JComponent --------------------------------------------------------------

	protected void paintComponent(Graphics g) {
		
		AntialiasingManager.activateAntialiasing(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		g2.drawImage(this.background, 0, 0, null);
		
		g2.setColor(new Color(255, 255, 255, 180));

		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
		
		g2.setColor(Constants.TOOLBAR_SEPARATOR_COLOR);
		
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

	}
	
}