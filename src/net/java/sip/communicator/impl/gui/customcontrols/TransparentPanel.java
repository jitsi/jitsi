/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import javax.swing.*;

/**
 * @author Yana Stamcheva
 */
public class TransparentPanel
    extends JPanel
{
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
