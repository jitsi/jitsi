/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.chatconfig;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.chatconfig.replacement.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The chat configuration panel.
 *
 * @author Purvesh Sahoo
 */
public class ChatConfigPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates the <tt>ChatConfigPanel</tt>.
     */
    public ChatConfigPanel()
    {
        super(new BorderLayout());

        TransparentPanel mainPanel = new TransparentPanel();

        BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(boxLayout);
        this.add(mainPanel, BorderLayout.NORTH);

        mainPanel.add(new ReplacementConfigPanel());
        mainPanel.add(Box.createVerticalStrut(10));
    }
}
