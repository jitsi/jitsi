/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>MsgToolbarButton</tt> is a <tt>SIPCommButton</tt> that has
 * specific background and rollover images. It is used in the chat window
 * toolbar.
 *
 * @author Yana Stamcheva
 */
public class ChatToolbarButton extends SIPCommButton
{
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of <tt>MsgToolbarButton</tt>.
     * @param iconImage The icon to display on this button.
     */
    public ChatToolbarButton(Image iconImage)
    {
        super(null, iconImage);

        this.setPreferredSize(new Dimension(25, 25));
    }
}
