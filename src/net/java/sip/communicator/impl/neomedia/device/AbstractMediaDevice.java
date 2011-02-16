/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.protocol.*;

import net.java.sip.communicator.service.neomedia.*;
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
     * The <tt>MediaDeviceSession</tt> used.
     */
    private MediaDeviceSession session = null;

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
            session = new VideoMediaDeviceSession(this);
            return session;
        default:
            session = new AudioMediaDeviceSession(this);
            return session;
        }
    }

    /**
     * Get the last used <tt>MediaDeviceSession</tt>.
     *
     * @return <tt>MediaDeviceSession</tt>
     */
    public MediaDeviceSession getSession()
    {
        return session;
    }

    /**
     * Connects to a specific <tt>CaptureDevice</tt> given in the form of a
     * <tt>DataSource</tt>. Explicitly defined in order to allow extenders to
     * customize the connect procedure.
     *
     * @param captureDevice the <tt>CaptureDevice</tt> to be connected to
     * @throws IOException if anything wrong happens while connecting to the
     * specified <tt>captureDevice</tt>
     */
    public void connect(DataSource captureDevice)
        throws IOException
    {
        if (captureDevice == null)
            throw new NullPointerException("captureDevice");
        try
        {
            captureDevice.connect();
        }
        catch (NullPointerException npe)
        {
            /*
             * The old media says it happens when the operating system does not
             * support the operation.
             */
            IOException ioe = new IOException();

            ioe.initCause(npe);
            throw ioe;
        }
    }

    /**
     * Returns a <tt>List</tt> containing (at the time of writing) a single
     * extension descriptor indicating <tt>RECVONLY</tt> support for
     * mixer-to-client audio levels.
     *
     * @return a <tt>List</tt> containing the <tt>CSRC_AUDIO_LEVEL_URN</tt>
     * extension descriptor.
     */
    public List<RTPExtension> getSupportedExtensions()
    {
        return null;
    }
}
