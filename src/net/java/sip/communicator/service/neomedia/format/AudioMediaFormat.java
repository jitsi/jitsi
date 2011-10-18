/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.format;

/**
 * The interface represents an audio format. Audio formats characterize audio
 * streams and the <tt>AudioMediaFormat</tt> interface gives access to some of its
 * properties such as encoding, clock rate, and number of channels.
 *
 * @author Emil Ivov
 */
public interface AudioMediaFormat
    extends MediaFormat
{
    /**
     * Returns the number of audio channels associated with this
     * <tt>AudioMediaFormat</tt>.
     *
     * @return the number of audio channels associated with this
     * <tt>AudioMediaFormat</tt>.
     */
    public int getChannels();
}
