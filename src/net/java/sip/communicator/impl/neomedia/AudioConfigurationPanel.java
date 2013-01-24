/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

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

        add(
                NeomediaActivator.getMediaConfiguration()
                        .createAudioConfigPanel(),
                BorderLayout.NORTH);
    }
}
