/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import net.java.sip.communicator.service.neomedia.*;

/**
 * Controls media service capture volume.
 * 
 * @author Damian Minkov
 */
public class InputVolumeControlImpl
    extends AbstractVolumeControl
    implements InputVolumeControl
{
    /**
     * Returns the property we use to store input sound level.
     *
     * @return sound level property name for storing configuration.
     */
    @Override
    String getStoreLevelPropertyName()
    {
        return CAPTURE_VOLUME_LEVEL_PROPERTY_NAME;
    }
}
