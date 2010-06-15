/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.portaudio;

import java.io.*;

import javax.media.*;
import javax.media.control.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;

/**
 * Implements <tt>DataSource</tt> and <tt>CaptureDevice</tt> for PortAudio.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class DataSource
    extends AbstractPullBufferCaptureDevice
{

    /**
     * Creates a new <tt>PullBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * in the list of streams of this <tt>PullBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PullBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PullBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPullBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPullBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new PortAudioStream(formatControl);
    }

    /**
     * Opens a connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @throws IOException if anything goes wrong while opening the connection
     * to the media source specified by the <tt>MediaLocator</tt> of this
     * <tt>DataSource</tt>
     * @see AbstractPullBufferCaptureDevice#doConnect()
     */
    @Override
    protected void doConnect()
        throws IOException
    {
        super.doConnect();

        int deviceIndex = getDeviceIndex();

        synchronized (this)
        {
            for (Object stream : getStreams())
                ((PortAudioStream) stream).setDeviceIndex(deviceIndex);
        }
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>. Allows extenders to
     * override and be sure that there will be no request to close a connection
     * if the connection has not been opened yet.
     */
    protected synchronized void doDisconnect()
    {
        try
        {
            synchronized (this)
            {
                if (streams != null)
                {
                    for (Object stream : streams)
                    {
                        try
                        {
                            ((PortAudioStream) stream).setDeviceIndex(
                                    PortAudio.paNoDevice);
                        }
                        catch (IOException ioex)
                        {
                        }
                    }
                }
            }
        }
        finally
        {
            super.doDisconnect();
        }
    }

    /**
     * Gets the device index of the PortAudio device identified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     *
     * @return the device index of a PortAudio device identified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>
     */
    private int getDeviceIndex()
    {
        MediaLocator locator = getLocator();

        if (locator == null)
            throw new IllegalStateException("locator");
        return getDeviceIndex(locator);
    }

    /**
     * Gets the device index of a PortAudio device from a specific
     * <tt>MediaLocator</tt> identifying it.
     *
     * @param locator the <tt>MediaLocator</tt> identifying the device index of
     * a PortAudio device to get
     * @return the device index of a PortAudio device identified by
     * <tt>locator</tt>
     */
    public static int getDeviceIndex(MediaLocator locator)
    {
        if (PortAudioAuto.LOCATOR_PROTOCOL.equalsIgnoreCase(
                locator.getProtocol()))
        {
            return Integer.parseInt(locator.getRemainder().replace("#", ""));
        }
        else
        {
            throw new IllegalArgumentException("locator.protocol");
        }
    }
}
