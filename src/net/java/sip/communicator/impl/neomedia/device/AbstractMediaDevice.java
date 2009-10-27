/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import javax.media.protocol.*;

import net.java.sip.communicator.service.neomedia.device.*;

/**
 * Defines the interface for <tt>MediaDevice</tt> required by the
 * <tt>net.java.sip.communicator.impl.neomedia</tt> implementation of
 * <tt>net.java.sip.communicator.service.neomedia</tt>.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractMediaDevice
    implements MediaDevice
{

    /**
     * Creates a <tt>DataSource</tt> instance for this <tt>MediaDevice</tt>
     * which gives access to the captured media.
     *
     * @return a <tt>DataSource</tt> instance which gives access to the media
     * captured by this <tt>MediaDevice</tt>
     */
    abstract DataSource createOutputDataSource();

    /**
     * Creates a new <tt>MediaDeviceSession</tt> instance which is to represent
     * the use of this <tt>MediaDevice</tt> by a <tt>MediaStream</tt>.
     *
     * @return a new <tt>MediaDeviceSession</tt> instance which is to represent
     * the use of this <tt>MediaDevice</tt> by a <tt>MediaStream</tt>
     */
    public MediaDeviceSession createSession()
    {
        switch (getMediaType())
        {
        case VIDEO:
            return new VideoMediaDeviceSession(this);
        default:
            return new MediaDeviceSession(this);
        }
    }
}
