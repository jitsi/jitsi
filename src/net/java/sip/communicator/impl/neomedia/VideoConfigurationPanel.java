/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;

import net.java.sip.communicator.util.swing.*;

/**
 * The video configuration form.
 *
 * @author Yana Stamcheva
 */
public class VideoConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of the <tt>VideoConfigurationPanel</tt>.
     */
    public VideoConfigurationPanel()
    {
        super(new BorderLayout());

        add(MediaConfiguration.createVideoConfigPanel(), BorderLayout.NORTH);
    }
}
