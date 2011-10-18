/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.*;

/**
 * @author Yana Stamcheva
 */
public class TransparentPanel
    extends JPanel
{
    private static final long serialVersionUID = 0L;

    public TransparentPanel()
    {
        this.setOpaque(false);
    }

    public TransparentPanel(LayoutManager layout)
    {
        super(layout);

        this.setOpaque(false);
    }
}
