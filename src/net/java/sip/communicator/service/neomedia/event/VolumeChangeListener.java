/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

/**
 * <tt>VolumeChangeListener</tt> used to notify for changes in the
 * playback volume.
 *  
 * @author Damian Minkov
 */
public interface VolumeChangeListener
{
    /**
     * Event fired when volume has changed.
     * @param volumeChangeEvent the volume change event.
     */
    public void volumeChange(VolumeChangeEvent volumeChangeEvent);
}
