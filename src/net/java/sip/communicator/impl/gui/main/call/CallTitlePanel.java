/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The panel, containing the title name of the call peer or member. It defines
 * its background color depending on the specified in initialization background
 * color.
 * 
 * @author Yana Stamcheva
 */
public class CallTitlePanel
    extends TransparentPanel
{
    private static final long serialVersionUID = 0L;

    private Color backgroundColor;

    /**
     * Creates a <tt>CallTitlePanel</tt> by specifying the <tt>layout</tt>
     * manager to use when layout out components.
     *
     * @param layout the layout manager to use for layout
     */
    public CallTitlePanel(LayoutManager layout)
    {
        super(layout);
    }

    /**
     * Sets the background color of this panel.
     * @param bgColor the background color of this panel
     */
    public void setBackground(Color bgColor)
    {
        this.backgroundColor = bgColor;
    }

    /**
     * Customizes the background of this panel, by painting a round rectangle in
     * the background color previously set.
     * @param g the <tt>Graphics</tt> object to use for painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(backgroundColor);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
        }
        finally
        {
            g.dispose();
        }
    }
}
