/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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

    private final Color backgroundColor;

    /**
     * Creates a <tt>CallTitlePanel</tt> by specifying the background color to
     * be used.
     *
     * @param backgroundColor indicates the color to be used to paint the
     * background of this title panel.
     */
    public CallTitlePanel(Color backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }

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
