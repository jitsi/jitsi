/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.video4linux2;

import java.io.*;

import javax.media.*;
import javax.media.control.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;

/**
 * Implements a <tt>PullBufferDataSource</tt> and <tt>CaptureDevice</tt> using
 * the Video for Linux Two API Specification.
 *
 * @author Lubomir Marinov
 */
public class DataSource
    extends AbstractPullBufferCaptureDevice
{

    /**
     * The default height to request from Video for Linux Two API Specification
     * devices.
     */
    public static final int DEFAULT_HEIGHT = 480;

    /**
     * The default width to request from Video for Linux Two API Specification
     * devices.
     */
    public static final int DEFAULT_WIDTH = 640;

    /**
     * The file descriptor of the opened Video for Linux Two API Specification
     * device represented by this <tt>DataSource</tt>.
     */
    private int fd = -1;

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance from a specific
     * <tt>MediaLocator</tt>.
     *
     * @param locator the <tt>MediaLocator</tt> to create the new instance from
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);
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
     */
    protected Video4Linux2Stream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        return new Video4Linux2Stream(formatControl);
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

        String deviceName = getDeviceName();
        int fd = Video4Linux2.open(deviceName, Video4Linux2.O_RDWR);

        if (-1 == fd)
            throw new IOException("Failed to open " + deviceName);
        else
        {
            boolean close = true;

            try
            {
                synchronized (this)
                {
                    for (Object stream : getStreams())
                        ((Video4Linux2Stream) stream).setFd(fd);
                }
                close = false;
            }
            finally
            {
                if (close)
                {
                    Video4Linux2.close(fd);
                    fd = -1;
                }
            }
            this.fd = fd;
        }
    }

    /**
     * Closes the connection to the media source specified by the
     * <tt>MediaLocator</tt> of this <tt>DataSource</tt>.
     */
    protected void doDisconnect()
    {
        try
        {
            /*
             * Letting the Video4Linux2Stream know that the fd is going to be
             * closed is necessary at least because
             * AbstractPullBufferStream#close() is not guaranteed.
             */
            synchronized (this)
            {
                if (streams != null)
                {
                    for (AbstractPullBufferStream stream : streams)
                    {
                        try
                        {
                            ((Video4Linux2Stream) stream).setFd(-1);
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
            try
            {
                super.doDisconnect();
            }
            finally
            {
                Video4Linux2.close(fd);
            }
        }
    }

    /**
     * Gets the name of the Video for Linux Two API Specification device which
     * represents the media source of this <tt>DataSource</tt>.
     *
     * @return the name of the Video for Linux Two API Specification device
     * which represents the media source of this <tt>DataSource</tt>
     */
    private String getDeviceName()
    {
        MediaLocator locator = getLocator();

        return
            ((locator != null)
                    && Video4Linux2Auto.LOCATOR_PROTOCOL
                        .equalsIgnoreCase(locator.getProtocol()))
                ? locator.getRemainder()
                : null;
    }
}
