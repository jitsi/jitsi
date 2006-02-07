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
						
		this.updateBackground();
	}


	// protected ---------------------------------------------------------------

	protected void updateBackground() {
		
		try {

			robot = new Robot();

		} catch (AWTException e) {

			e.printStackTrace();

			return;

		}
				
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		
		background = robot.createScreenCapture(
			        new Rectangle(0, 0, (int)dim.getWidth( ),
			                           (int)dim.getHeight( )));
			                          
	}

	// JComponent --------------------------------------------------------------

	protected void paintComponent(Graphics g) {
		
		AntialiasingManager.activateAntialiasing(g);
		
		Graphics2D g2 = (Graphics2D) g;

		Point pos = this.getLocationOnScreen();
		
		Point offset = new Point(-pos.x,-pos.y);
				
		g2.drawImage(this.background, offset.x, offset.y, null);

		g2.setColor(new Color(255, 255, 255, 180));

		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
		
		g2.setColor(Constants.TOOLBAR_SEPARATOR_COLOR);
		
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

	}
	
}