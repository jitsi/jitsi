/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.utils.Constants;

/**
 * The <tt>ListCellPanel</tt> is a <tt>JPanel</tt>, which is repainted to have
 * a specific look when selected and when a mouse is over it. It represents the
 * cells in the <tt>SIPCommList</tt>, which is used in the Configuration form.
 * 
 * @author Yana Stamcheva
 */
public class ListCellPanel extends JPanel implements MouseListener {

    private boolean isMouseOver = false;

    private boolean isSelected = false;

    /**
     * Creates an instance of <tt>ListCellPanel</tt>.
     */
    public ListCellPanel() {

        super(new BorderLayout());

        this.setBackground(Color.WHITE);

        this.setOpaque(true);
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to provide a custom look for this panel. A gradient background is
     * painted when the panel is selected and when the mouse is over it.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        if (this.isSelected()) {
            GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
                    Constants.SELECTED_START_COLOR, this
                            .getWidth() / 2,
                    Constants.SELECTED_GRADIENT_SIZE,
                    Constants.SELECTED_END_COLOR);

            GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
                    .getHeight()
                    - Constants.SELECTED_GRADIENT_SIZE,
                    Constants.SELECTED_END_COLOR,
                    this.getWidth() / 2, this.getHeight() - 1,
                    Constants.SELECTED_START_COLOR);

            g2.setPaint(p);
            g2.fillRect(0, 0, this.getWidth(),
                    Constants.SELECTED_GRADIENT_SIZE);

            g2.setColor(Constants.SELECTED_END_COLOR);
            g2.fillRect(0, Constants.SELECTED_GRADIENT_SIZE, this
                    .getWidth(), this.getHeight()
                    - Constants.SELECTED_GRADIENT_SIZE);

            g2.setPaint(p1);
            g2.fillRect(0, this.getHeight()
                    - Constants.SELECTED_GRADIENT_SIZE, this
                    .getWidth(), this.getHeight() - 1);

        } else if (this.isMouseOver()) {
            GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
                    Constants.MOVER_START_COLOR,
                    this.getWidth() / 2, Constants.GRADIENT_SIZE,
                    Constants.MOVER_END_COLOR);

            GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
                    .getHeight()
                    - Constants.GRADIENT_SIZE,
                    Constants.MOVER_END_COLOR,
                    this.getWidth() / 2, this.getHeight(),
                    Constants.MOVER_START_COLOR);

            g2.setPaint(p);
            g2.fillRect(0, 0, this.getWidth(),
                    Constants.GRADIENT_SIZE);

            g2.setColor(Constants.MOVER_END_COLOR);
            g2.fillRect(0, Constants.GRADIENT_SIZE, this
                    .getWidth(), this.getHeight()
                    - Constants.GRADIENT_SIZE);

            g2.setPaint(p1);
            g2.fillRect(0, this.getHeight()
                    - Constants.GRADIENT_SIZE - 1,
                    this.getWidth(), this.getHeight() - 1);

        }

        this.addMouseListener(this);
    }

    /**
     * Returns <code>true</code> if the mouse is over the panel,
     * <code>false</code> otherwise.
     * @return <code>true</code> if the mouse is over the panel,
     * <code>false</code> otherwise.
     */
    public boolean isMouseOver() {
        return isMouseOver;
    }

    /**
     * Sets the "mouse over" status for this panel.
     * @param isMouseOver <code>true</code> to indicate that the mouse is over
     * the panel, <code>false</code> otherwise.
     */
    public void setMouseOver(boolean isMouseOver) {
        this.isMouseOver = isMouseOver;
    }

    /**
     * Returns <code>true</code> if the panel is selected,
     * <code>false</code> otherwise.
     * @return <code>true</code> if the panel is selected,
     * <code>false</code> otherwise.
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Sets the selection status for this panel.
     * @param isSelected <code>true</code> to indicate that the panel is
     * selected, <code>false</code> otherwise.
     */
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

    /**
     * When the mouse is clicked over this panel sets the selection to
     * <code>true</code>.
     */
    public void mouseClicked(MouseEvent e) {
        this.setSelected(true);
    }

    /**
     * When the mouse enters the panel area sets the "mouse over" status to
     * <code>true</code> and repaints the panel.
     */
    public void mouseEntered(MouseEvent e) {

        this.setMouseOver(true);
        this.repaint();
    }

    /**
     * When the mouse exits the panel area sets the "mouse over" status to 
     * <code>true</code> and repaints the panel.
     */
    public void mouseExited(MouseEvent e) {
        this.setMouseOver(false);
        this.repaint();
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
