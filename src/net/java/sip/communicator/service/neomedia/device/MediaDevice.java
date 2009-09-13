/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.device;

import java.util.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * The <tt>MediaDevice</tt> class represents capture and playback devices that
 * can be used to grab or render media. Sound cards, USB phones and webcams are
 * examples of such media devices.
 *
 * @author Emil Ivov
 */
public interface MediaDevice
{
    /**
     * Direction values
     */
    public static enum Direction{ IN, OUT, INOUT};
    /**
     * Returns a list of <tt>MediaFormat</tt> instances representing the media
     * formats supported by this <tt>MediaDevice</tt>.
     *
     * @return the list of <tt>MediaFormat</tt>s supported by this device.
     */
    public List<MediaFormat> getSupportedFormats();

    /**
     * Returns the <tt>MediaType</tt> that this device supports.
     *
     * @return <tt>MediaType.AUDIO</tt> if this is an audio device or
     * <tt>MediaType.VIDEO</tt> in case of a video device.
     */
    public MediaType getMediaType();

    /**
     * Specifies the <tt>MediaFormat</tt> that this device should use when
     * capturing data.
     *
     * @param format the <tt>MediaFormat</tt> that this device should use when
     * capturing media.
     *
     * @throws IllegalArgumentException if <tt>format</tt> is not among
     * <tt>MediaFormet</tt>s supported by this <tt>MediaDevice</tt>.
     */
    public void setFormat(MediaFormat format) throws IllegalArgumentException;

    /**
     * Returns the <tt>MediaFormat</tt> that this device is currently set to use
     * when capturing data.
     *
     * @return the <tt>MediaFormat</tt> that this device is currently set to
     * provide media in.
     */
    public MediaFormat getFormat();
}
