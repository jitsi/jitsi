/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Provides a base implementation of <tt>PullBufferStream</tt> for video in
 * order to facilitate implementers by taking care of boilerplate in the most
 * common cases.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractVideoPullBufferStream
    extends AbstractPullBufferStream
{

    /**
     * The output frame rate of this <tt>AbstractVideoPullBufferStream</tt>
     * which has been specified by {@link #frameRateControl} and depending on
     * which {@link #minimumVideoFrameInterval} has been calculated.
     */
    private float frameRate;

    /**
     * The <tt>FrameRateControl</tt> which gets and sets the output frame rate
     * of this <tt>AbstractVideoPullBufferStream</tt>.
     */
    private FrameRateControl frameRateControl;

    /**
     * The minimum interval in milliseconds between consecutive video frames
     * i.e. the reverse of {@link #frameRate}.
     */
    private long minimumVideoFrameInterval;

    /**
     * Initializes a new <tt>AbstractVideoPullBufferStream</tt> instance which
     * is to have its <tt>Format</tt>-related information abstracted by a
     * specific <tt>FormatControl</tt>.
     *
     * @param dataSource the <tt>PullBufferDataSource</tt> which is creating the
     * new instance so that it becomes one of its <tt>streams</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     */
    protected AbstractVideoPullBufferStream(
            PullBufferDataSource dataSource,
            FormatControl formatControl)
    {
        super(dataSource, formatControl);
    }

    /**
     * Blocks and reads into a <tt>Buffer</tt> from this
     * <tt>PullBufferStream</tt>.
     *
     * @param buffer the <tt>Buffer</tt> this <tt>PullBufferStream</tt> is to
     * read into
     * @throws IOException if an I/O error occurs while this
     * <tt>PullBufferStream</tt> reads into the specified <tt>Buffer</tt>
     */
    protected abstract void doRead(Buffer buffer)
        throws IOException;

    /**
     * Blocks and reads into a <tt>Buffer</tt> from this
     * <tt>PullBufferStream</tt>.
     *
     * @param buffer the <tt>Buffer</tt> this <tt>PullBufferStream</tt> is to
     * read into
     * @throws IOException if an I/O error occurs while this
     * <tt>PullBufferStream</tt> reads into the specified <tt>Buffer</tt>
     */
    public void read(Buffer buffer)
        throws IOException
    {
        FrameRateControl frameRateControl = this.frameRateControl;

        if (frameRateControl != null)
        {
            float frameRate = frameRateControl.getFrameRate();

            if (frameRate > 0)
            {
                if (this.frameRate != frameRate)
                {
                    minimumVideoFrameInterval = (long) (1000 / frameRate);
                    this.frameRate = frameRate;
                }
                if (minimumVideoFrameInterval > 0)
                {
                    long startTime = System.currentTimeMillis();

                    doRead(buffer);

                    if (!buffer.isDiscard())
                    {
                        boolean interrupted = false;

                        while (true)
                        {
                            // Sleep to respect the frame rate as much as possible.
                            long sleep
                                = minimumVideoFrameInterval
                                    - (System.currentTimeMillis() - startTime);

                            if (sleep > 0)
                            {
                                try
                                {
                                    Thread.sleep(sleep);
                                }
                                catch (InterruptedException ie)
                                {
                                    interrupted = true;
                                }
                            }
                            else
                            {
                                // Yield a little bit to not use all the whole CPU.
                                Thread.yield();
                                break;
                            }
                        }
                        if (interrupted)
                            Thread.currentThread().interrupt();
                    }

                    // We've executed #doRead(Buffer).
                    return;
                }
            }
        }

        // If there is no frame rate to be respected, just #doRead(Buffer).
        doRead(buffer);
    }

    /**
     * Starts the transfer of media data from this
     * <tt>AbstractBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>AbstractBufferStream</tt>
     * @see AbstractBufferStream#start()
     */
    @Override
    public void start()
        throws IOException
    {
        super.start();

        frameRateControl
            = (FrameRateControl)
                dataSource.getControl(FrameRateControl.class.getName());
    }

    /**
     * Stops the transfer of media data from this <tt>AbstractBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>AbstractBufferStream</tt>
     * @see AbstractBufferStream#stop()
     */
    @Override
    public void stop()
        throws IOException
    {
        super.stop();

        frameRateControl = null;
    }
}
