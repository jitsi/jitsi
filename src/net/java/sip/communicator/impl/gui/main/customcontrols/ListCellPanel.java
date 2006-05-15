/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;

public class ListCellPanel extends JPanel implements MouseListener {

	private boolean isMouseOver = false;

	private boolean isSelected = false;

	public ListCellPanel() {

		super(new BorderLayout());

		this.setBackground(Color.WHITE);

		this.setOpaque(true);		
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;

		if (this.isSelected()) {
			GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
					Constants.CONTACTPANEL_SELECTED_START_COLOR, this
							.getWidth() / 2,
					Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE,
					Constants.CONTACTPANEL_SELECTED_END_COLOR);

			GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
					.getHeight()
					- Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE,
					Constants.CONTACTPANEL_SELECTED_END_COLOR,
					this.getWidth() / 2, this.getHeight() - 1,
					Constants.CONTACTPANEL_SELECTED_START_COLOR);

			g2.setPaint(p);
			g2.fillRect(0, 0, this.getWidth(),
					Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE);			

			g2.setColor(Constants.CONTACTPANEL_SELECTED_END_COLOR);
			g2.fillRect(0, Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE,
					this.getWidth(), 
					this.getHeight() - Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE);
			
			g2.setPaint(p1);
			g2.fillRect(0, this.getHeight() - Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE, 
					this.getWidth(), this.getHeight() - 1);
			
		
			
		} else if (this.isMouseOver()) {
			GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
					Constants.CONTACTPANEL_MOVER_START_COLOR,
					this.getWidth() / 2, Constants.CONTACTPANEL_GRADIENT_SIZE,
					Constants.CONTACTPANEL_MOVER_END_COLOR);

			GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
					.getHeight()
					- Constants.CONTACTPANEL_GRADIENT_SIZE,
					Constants.CONTACTPANEL_MOVER_END_COLOR,
					this.getWidth() / 2, this.getHeight(),
					Constants.CONTACTPANEL_MOVER_START_COLOR);

			g2.setPaint(p);
			g2.fillRect(0, 0, this.getWidth(),
					Constants.CONTACTPANEL_GRADIENT_SIZE);			

			g2.setColor(Constants.CONTACTPANEL_MOVER_END_COLOR);
			g2.fillRect(0, Constants.CONTACTPANEL_GRADIENT_SIZE, this
					.getWidth(), this.getHeight()
					- Constants.CONTACTPANEL_GRADIENT_SIZE);
			
			g2.setPaint(p1);
			g2.fillRect(0, this.getHeight()
					- Constants.CONTACTPANEL_GRADIENT_SIZE - 1,
					this.getWidth(), this.getHeight() - 1);
			
		}

		this.addMouseListener(this);
	}

	public boolean isMouseOver() {
		return isMouseOver;
	}

	public void setMouseOver(boolean isMouseOver) {
		this.isMouseOver = isMouseOver;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		if (this.isSelected != isSelected) {
			this.isSelected = isSelected;
			/*
			 * if(isSelected) { this.setSize(new Dimension(this.getWidth(),
			 * LookAndFeelConstants.CONTACTPANEL_SELECTED_HEIGHT)); } else {
			 * this.setSize(new Dimension(this.getWidth(),
			 * LookAndFeelConstants.CONTACTPANEL_HEIGHT)); }
			 */
			this.repaint();
		}
	}

	public void mouseClicked(MouseEvent e) {

		this.setSelected(true);
	}

	public void mouseEntered(MouseEvent e) {

		this.setMouseOver(true);
		this.repaint();
	}

	public void mouseExited(MouseEvent e) {

		this.setMouseOver(false);
		this.repaint();
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}
}
