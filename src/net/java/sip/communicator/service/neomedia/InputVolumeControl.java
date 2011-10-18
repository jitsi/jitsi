/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Controls the capture volume in media service.
 *
 * @author Damian Minkov
 */
public interface InputVolumeControl
    extends VolumeControl
{
    /**
     * Property for storing level into configuration.
     */
    public final static String CAPTURE_VOLUME_LEVEL_PROPERTY_NAME
            = "net.java.sip.communicator.service.media.CAPTURE_VOLUME_LEVEL";
}
