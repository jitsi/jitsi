/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.util.*;

import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.neomedia.format.*;

/**
 * The <tt>MediaService</tt> service is meant to be a wrapper of media libraries
 * such as JMF, FMJ, FFMPEG, and/or others. It takes care of all media play and
 * capture as well as media transport (e.g. over RTP).
 *
 * @author Emil Ivov
 */
public interface MediaService
{
    /**
     * Returns the default <tt>MediaDevice</tt> for the specified media
     * <tt>type</tt>.
     *
     * @param mediaType a <tt>MediaType</tt> value indicating the kind of device
     * that we are trying to obtain.
     *
     * @return the currently default <tt>MediaDevice</tt> for the specified
     * <tt>MediaType</tt>, or <tt>null</tt> if no such device exists.
     */
    public MediaDevice getDefaultDevice(MediaType mediaType);

    /**
     * Returns a list containing all devices known to this service
     * implementation and handling the specified <tt>MediaType</tt>.
     *
     * @param mediaType the media type (i.e. AUDIO or VIDEO) that we'd like
     * to obtain the device list for.
     *
     * @return the list of <tt>MediaDevice</tt>s currently known to handle the
     * specified <tt>mediaType</tt>.
     */
    public List<MediaDevice> getDevices(MediaType mediaType);

    /**
     * Creates a <tt>MediaStream</tt> that will be using the specified
     * <tt>MediaDevice</tt> for both capture and playback of media exchanged
     * via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the connector that the stream should use for sending and
     * receiving media.
     * @param device the device to be used for both capture and playback of
     * media exchanged via the specified <tt>StreamConnector</tt>
     *
     * @return the newly created <tt>MediaStream</tt>.
     */
    public MediaStream createMediaStream(StreamConnector connector,
                                         MediaDevice     device);

    /**
     * Creates a new <tt>MediaDevice</tt> which uses a specific
     * <tt>MediaDevice</tt> to capture and play back media and performs mixing
     * of the captured media and the media played back by any other users of the
     * returned <tt>MediaDevice</tt>. For the <tt>AUDIO</tt> <tt>MediaType</tt>,
     * the returned device is commonly referred to as an audio mixer. The
     * <tt>MediaType</tt> of the returned <tt>MediaDevice</tt> is the same as
     * the <tt>MediaType</tt> of the specified <tt>device</tt>.
     *
     * @param device the <tt>MediaDevice</tt> which is to be used by the
     * returned <tt>MediaDevice</tt> to actually capture and play back media
     * @return a new <tt>MediaDevice</tt> instance which uses <tt>device</tt> to
     * capture and play back media and performs mixing of the captured media and
     * the media played back by any other users of the returned
     * <tt>MediaDevice</tt> instance
     */
    public MediaDevice createMixer(MediaDevice device);

    /**
     * Gets the <tt>MediaFormatFactory</tt> through which <tt>MediaFormat</tt>
     * instances may be created for the purposes of working with the
     * <tt>MediaStream</tt>s created by this <tt>MediaService</tt>.
     *
     * @return the <tt>MediaFormatFactory</tt> through which
     * <tt>MediaFormat</tt> instances may be created for the purposes of working
     * with the <tt>MediaStream</tt>s created by this <tt>MediaService</tt>
     */
    public MediaFormatFactory getFormatFactory();
}
