/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Controls the playback volume in media service.
 *
 * @author Damian Minkov
 */
public interface OutputVolumeControl
    extends VolumeControl
{
    /**
     * Property for storing level into configuration.
     */
    public final static String PLAYBACK_VOLUME_LEVEL_PROPERTY_NAME
            = "net.java.sip.communicator.service.media.PLAYBACK_VOLUME_LEVEL";
}
