/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>DataSource</tt> and <tt>CaptureDevice</tt> for PortAudio.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class DataSource
    extends AbstractPullBufferCaptureDevice
{
    /**
     * The <tt>Logger</tt> used by the <tt>DataSource</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DataSource.class);

    /**
     * The indicator which determines whether this <tt>DataSource</tt> will
     * use audio quality improvement in accord with the preferences of the user.
     */
    private final boolean audioQualityImprovement;

    /**
     * The list of <tt>Format</tt>s in which this <tt>DataSource</tt> is
     * capable of capturing audio data.
     */
    private final Format[] supportedFormats;

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
        this.supportedFormats = null;
        this.audioQualityImprovement = true;
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance from a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    public DataSource(MediaLocator locator)
    {
        this(locator, null, true);
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance from a specific
     * <tt>MediaLocator</tt> and which has a specific list of <tt>Format</tt>
     * in which it is capable of capturing audio data overriding its
     * registration with JMF and optionally uses audio quality improvement in
     * accord with the preferences of the user.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     * @param supportedFormats the list of <tt>Format</tt>s in which the new
     * instance is to be capable of capturing audio data
     * @param audioQualityImprovement <tt>true</tt> if audio quality improvement
     * is to be enabled in accord with the preferences of the user or
     * <tt>false</tt> to completely disable audio quality improvement
     */
    public DataSource(
            MediaLocator locator,
            Format[] supportedFormats,
            boolean audioQualityImprovement)
    {
        super(locator);

        this.supportedFormats
            = (supportedFormats == null)
                ? null
                : supportedFormats.clone();
        this.audioQualityImprovement = audioQualityImprovement;
    }

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
        return
            new PortAudioStream(this, formatControl, audioQualityImprovement);
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

        synchronized (getStreamSyncRoot())
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
    @Override
    protected void doDisconnect()
    {
        try
        {
            synchronized (getStreamSyncRoot())
            {
                Object[] streams = streams();

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
                            logger.error(
                                    "Failed to close "
                                        + stream.getClass().getSimpleName(),
                                    ioex);
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
            throw new NullPointerException("locator");
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
        if (AudioSystem.LOCATOR_PROTOCOL_PORTAUDIO.equalsIgnoreCase(
                locator.getProtocol()))
        {
            return Integer.parseInt(locator.getRemainder().replace("#", ""));
        }
        else
        {
            throw new IllegalArgumentException("locator.protocol");
        }
    }

    /**
     * Gets the <tt>Format</tt>s which are to be reported by a
     * <tt>FormatControl</tt> as supported formats for a
     * <tt>PullBufferStream</tt> at a specific zero-based index in the list of
     * streams of this <tt>PullBufferDataSource</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * for which the specified <tt>FormatControl</tt> is to report the list of
     * supported <tt>Format</tt>s
     * @return an array of <tt>Format</tt>s to be reported by a
     * <tt>FormatControl</tt> as the supported formats for the
     * <tt>PullBufferStream</tt> at the specified <tt>streamIndex</tt> in the
     * list of streams of this <tt>PullBufferDataSource</tt>
     * @see AbstractPullBufferCaptureDevice#getSupportedFormats(int)
     */
    @Override
    protected Format[] getSupportedFormats(int streamIndex)
    {
        return
            (supportedFormats == null)
                ? super.getSupportedFormats(streamIndex)
                : supportedFormats;
    }
}
