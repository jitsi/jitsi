/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;



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