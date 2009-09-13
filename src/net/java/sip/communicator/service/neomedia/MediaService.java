/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.service.neomedia.device.*;

/**
 * The <tt>MediaService</tt> service is meant to be a wrapper of media libraries
 * such as JMF, FMJ, FFMPEG, and others. It takes care of all media play and
 * capture as well as media transport (e.g. over RTP).
 *
 * @author Emil Ivov
 */
public interface MediaService
{
    /**
     * Returns an array of <tt>MediaType</tt> instances indicating the types
     * that an implementation supports. A <tt>MediaType</tt> is considered
     * supported if an implementation supports either rendering or capturing
     * that type of media.
     *
     * @return an array of <tt>MediaType</tt> instances indicating the types of
     * media that this service supports.
     */
    public MediaType[] getSupportedMediaTypes();

    /**
     * Returns the default <tt>MediaDevice</tt> for the specified media
     * <tt>type</tt>. In most cases users
     *
     * @param type a <tt>MediaType</tt> instance indicating the kind of device
     * that we are trying to obtain.
     *
     * @return the currently default <tt>MediaDevice</tt> for the specified
     * <tt>MediaType</tt>, or <tt>null</tt> if no such device exists.
     */
    public MediaDevice getDefaultDevice(MediaType type);
}
