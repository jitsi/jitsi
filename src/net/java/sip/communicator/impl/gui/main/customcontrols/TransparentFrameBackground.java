package net.java.sip.communicator.impl.gui.main.customcontrols;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

import java.awt.*;

import java.awt.event.*;

import java.awt.image.*;



public class TransparentFrameBackground extends JComponent

		implements ComponentListener, WindowFocusListener, Runnable {

	
	private Window frame;

	private BufferedImage background;

	private long lastUpdate = 0;

	private boolean refreshRequested = true;

	private Robot robot;

	private Rectangle screenRect;
	
	// constructor -------------------------------------------------------------

	public TransparentFrameBackground(Window frame) {

		this.frame = frame;

		this.updateBackground();

		this.frame.addComponentListener(this);

		//this.frame.addWindowFocusListener(this);

		//new Thread(this).start();

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
			        new Rectangle(0,0,(int)dim.getWidth( ),
			                          (int)dim.getHeight( )));
	}

	protected void refresh() {

		if (this.frame.isVisible() && this.isVisible()) {

			repaint();

			this.refreshRequested = true;

			this.lastUpdate = System.currentTimeMillis();

		}

	}
	

	// JComponent --------------------------------------------------------------

	protected void paintComponent(Graphics g) {
		
		AntialiasingManager.activateAntialiasing(g);
		
		Graphics2D g2 = (Graphics2D) g;

		Point pos = this.getLocationOnScreen();

		Point offset = new Point(-pos.x,-pos.y);
		
		BufferedImage buf = new BufferedImage(	this.getWidth(), 
												this.getHeight(), 
												BufferedImage.TYPE_INT_RGB);

		buf.getGraphics().drawImage(this.background, offset.x, offset.y, null);
		
		g2.drawImage(buf, 0, 0, null);

		g2.setColor(new Color(255, 255, 255, 180));

		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
		
		g2.setColor(Constants.TOOLBAR_SEPARATOR_COLOR);
		
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

	}

	// ComponentListener -------------------------------------------------------

	public void componentHidden(ComponentEvent e) {
	}



	public void componentMoved(ComponentEvent e) {
		
		repaint();
	}

	public void componentResized(ComponentEvent e) {
		
		repaint();
	}

	public void componentShown(ComponentEvent e) {
		
		repaint();
	}

	// WindowFocusListener -----------------------------------------------------

	public void windowGainedFocus(WindowEvent e) {
		System.out.println("focus gained");
		refresh();		
	}

	public void windowLostFocus(WindowEvent e) {
		System.out.println("focuslost");
		refresh();
	}

	// Runnable ----------------------------------------------------------------

	public void run() {

		try {

			while (true) {

				Thread.sleep(100);

				long now = System.currentTimeMillis();

				if (this.refreshRequested && ((now - this.lastUpdate) > 1000)) {

					if (this.frame.isVisible()) {

						Point location = this.frame.getLocation();

						this.frame.setLocation(-this.frame.getWidth(), -this.frame.getHeight());

						//this.frame.hide();
						
						updateBackground();
						
						//this.frame.show();

						this.frame.setLocation(location);

						refresh();

					}

					this.lastUpdate = now;

					this.refreshRequested = false;

				}

			}

		} catch (InterruptedException e) {

			e.printStackTrace();

		}

	}


	public static void main(String[] args) {

		//JFrame frame = new JFrame("Transparent Window");

		JWindow frame = new JWindow();
		
		TransparentFrameBackground bg = new TransparentFrameBackground(frame);
		
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add(bg);

		frame.pack();

		frame.setSize(200, 200);

		frame.setLocation(300, 300);

		frame.setVisible(true);

	}

}