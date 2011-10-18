/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.neomedia.*;

/**
 * Controls media service playback volume.
 * 
 * @author Damian Minkov
 */
public class OutputVolumeControlImpl
    extends AbstractVolumeControl
    implements OutputVolumeControl
{
    /**
     * Returns the property we use to store output sound level.
     *
     * @return sound level property name for storing configuration.
     */
    @Override
    String getStoreLevelPropertyName()
    {
        return PLAYBACK_VOLUME_LEVEL_PROPERTY_NAME;
    }
}
