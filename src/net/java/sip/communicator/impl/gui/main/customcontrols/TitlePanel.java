/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.utils.Constants;

/**
 * The <tt>TitlePanel</tt> is a decorated panel, that could be used for a
 * header or a title area. This panel is used for example in the
 * <tt>ConfigurationFrame</tt>.
 * 
 * @author Yana Stamcheva
 */
public class TitlePanel extends JPanel {

    private JLabel titleLabel = new JLabel();

    /**
     * Creates an instance of <tt>TitlePanel</tt>.
     */
    public TitlePanel() {

        super(new FlowLayout(FlowLayout.CENTER));

        this.setPreferredSize(new Dimension(0, 30));

        this.titleLabel.setFont(this.getFont().deriveFont(Font.BOLD, 14));
    }

    /**
     * Creates an instance of <tt>TitlePanel</tt> by specifying the title
     * String.
     * 
     * @param title A String title.
     */
    public TitlePanel(String title) {

        super(new FlowLayout(FlowLayout.CENTER));

        this.titleLabel.setFont(this.getFont().deriveFont(Font.BOLD, 14));

        this.titleLabel.setText(title);

        this.add(titleLabel);
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt>
     * to paint a gradient background of this panel.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
                Constants.MOVER_START_COLOR, this.getWidth() / 2,
                Constants.GRADIENT_SIZE,
                Constants.MOVER_END_COLOR);

        GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
                .getHeight()
                - Constants.GRADIENT_SIZE,
                Constants.MOVER_END_COLOR, this.getWidth() / 2,
                this.getHeight(), Constants.MOVER_START_COLOR);

        g2.setPaint(p);
        g2
                .fillRect(0, 0, this.getWidth(),
                        Constants.GRADIENT_SIZE);

        g2.setColor(Constants.MOVER_END_COLOR);
        g2.fillRect(0, Constants.GRADIENT_SIZE, this.getWidth(),
                this.getHeight() - Constants.GRADIENT_SIZE);

        g2.setPaint(p1);
        g2.fillRect(0, this.getHeight() - Constants.GRADIENT_SIZE
                - 1, this.getWidth(), this.getHeight() - 1);

        g2.setColor(Constants.BLUE_GRAY_BORDER_COLOR);
        g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() - 1, 5, 5);
    }

    /**
     * Sets the title String.
     * @param title The title String.
     */
    public void setTitleText(String title) {
        this.titleLabel.setText(title);

        this.add(titleLabel);
    }
}
