/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;

import javax.swing.*;

/**
 * The <tt>TitlePanel</tt> is a decorated panel, that could be used for a
 * header or a title area. This panel is used for example in the
 * <tt>ConfigurationFrame</tt>.
 * 
 * @author Yana Stamcheva
 */
public class TitlePanel extends JPanel
{
    /**
     * A color between blue and gray used to paint some borders.
     */
    public static final Color BLUE_GRAY_BORDER_COLOR = new Color(142, 160, 188);

    /**
     * The size of the gradient used for painting the background.
     */
    private static final int GRADIENT_SIZE = 10;

    /**
     * The start color used to paint a gradient mouse over background.
     */
    private static final Color MOVER_START_COLOR = new Color(230,
            230, 230);

    /**
     * The end color used to paint a gradient mouse over background.
     */
    private static final Color MOVER_END_COLOR = new Color(255,
            255, 255);

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
                MOVER_START_COLOR, this.getWidth() / 2,
                GRADIENT_SIZE,
                MOVER_END_COLOR);

        GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
                .getHeight()
                - GRADIENT_SIZE,
                MOVER_END_COLOR, this.getWidth() / 2,
                this.getHeight(), MOVER_START_COLOR);

        g2.setPaint(p);
        g2.fillRect(0, 0, this.getWidth(), GRADIENT_SIZE);

        g2.setColor(MOVER_END_COLOR);
        g2.fillRect(0, GRADIENT_SIZE, this.getWidth(),
                this.getHeight() - GRADIENT_SIZE);

        g2.setPaint(p1);
        g2.fillRect(0, this.getHeight() - GRADIENT_SIZE
                - 1, this.getWidth(), this.getHeight() - 1);

        g2.setColor(BLUE_GRAY_BORDER_COLOR);
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
