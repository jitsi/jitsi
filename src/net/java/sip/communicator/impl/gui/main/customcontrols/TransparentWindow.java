package net.java.sip.communicator.impl.gui.main.customcontrols;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

import java.awt.*;

import java.awt.event.*;

import java.awt.image.*;



public class TransparentWindow extends JPopupMenu

		implements ComponentListener, Runnable {
	
	private BufferedImage background;

	private long lastUpdate = 0;

	private boolean refreshRequested = true;

	private Robot robot;

	private Rectangle screenRect;
	
	// constructor -------------------------------------------------------------

	public TransparentWindow() {
		
		this.updateBackground();

		this.addComponentListener(this);		

		new Thread(this).start();
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

		if (this.isVisible()) {

			repaint();

			this.refreshRequested = true;

			this.lastUpdate = System.currentTimeMillis();

		}

	}
	

	// JComponent --------------------------------------------------------------

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		AntialiasingManager.activateAntialiasing(g);
		
		Graphics2D g2 = (Graphics2D) g;

		Point pos = this.getLocationOnScreen();

		Point offset = new Point(-pos.x,-pos.y);
		
		BufferedImage buf = new BufferedImage(	this.getWidth(), 
												this.getHeight(), 
												BufferedImage.TYPE_INT_RGB);

		buf.getGraphics().drawImage(this.background, offset.x, offset.y, null);
		
		g2.drawImage(buf, 0, 0, null);

		g2.setColor(new Color(255, 255, 255, 150));

		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
		
		g2.setColor(Color.GRAY);
		
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

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

	// Runnable ----------------------------------------------------------------

	public void run() {

		try {

			while (true) {

				Thread.sleep(100);

				long now = System.currentTimeMillis();

				if (this.refreshRequested && ((now - this.lastUpdate) > 1000)) {

					if (this.isVisible()) {

						Point location = this.getLocation();

						this.setLocation(-this.getWidth(), -this.getHeight());

						//this.frame.hide();
						
						updateBackground();
						
						//this.frame.show();

						this.setLocation(location);

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

/*
	public static void main(String[] args) {

		//JFrame frame = new JFrame("Transparent Window");

		JWindow frame = new JWindow();
		
		TransparentBackground bg = new TransparentBackground(frame);
		
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add(bg);

		frame.pack();

		frame.setSize(200, 200);

		frame.setLocation(300, 300);

		frame.setVisible(true);

	}
*/
}