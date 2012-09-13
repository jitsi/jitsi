/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;

import net.java.sip.communicator.util.swing.*;
import org.jitsi.service.neomedia.*;

/**
 * The audio configuration form.
 *
 * @author Yana Stamcheva
 */
public class AudioConfigurationPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates an instance of the <tt>AudioConfigurationPanel</tt>.
     */
    public AudioConfigurationPanel()
    {
        super(new BorderLayout());
        MediaConfigurationService mediaConfiguration
                = NeomediaActivator.getMediaConfiguration();
        add(mediaConfiguration.createAudioConfigPanel(), BorderLayout.NORTH);
    }
}
