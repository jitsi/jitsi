/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>TitlePanel</tt> is a decorated panel, that could be used for a header
 * or a title area. This panel is used for example in the
 * <tt>ConfigurationFrame</tt>.
 * 
 * @author Yana Stamcheva
 */
public class TitlePanel
    extends TransparentPanel
{
    private JLabel titleLabel = new JLabel();

    private Color gradientStartColor = new Color(255, 255, 255, 200);

    private Color gradientEndColor = new Color(255, 255, 255, 50);

    /**
     * Creates an instance of <tt>TitlePanel</tt>.
     */
    public TitlePanel()
    {

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
    public TitlePanel(String title)
    {
        super(new FlowLayout(FlowLayout.CENTER));

        this.titleLabel.setFont(this.getFont().deriveFont(Font.BOLD, 14));

        this.titleLabel.setText(title);

        this.add(titleLabel);
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JPanel</tt> to
     * paint a gradient background of this panel.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        GradientPaint p =
            new GradientPaint(this.getWidth() / 2, 0,
                                gradientStartColor,
                                this.getWidth() / 2,
                                getHeight(),
                                gradientEndColor);

        g2.setPaint(p);
        g2.fillRoundRect(0, 0, this.getWidth(), getHeight(), 10, 10);
    }

    /**
     * Sets the title String.
     * 
     * @param title The title String.
     */
    public void setTitleText(String title)
    {
        this.removeAll();

        this.titleLabel.setText(title);

        this.add(titleLabel);
    }
}
