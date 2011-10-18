/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.protocol.*;

/**
 * Implements a <tt>PushBufferDataSource</tt> wrapper which provides mute
 * support for the wrapped instance.
 * <p>
 * Because the class wouldn't work for our use case without it,
 * <tt>CaptureDevice</tt> is implemented and is being delegated to the wrapped
 * <tt>DataSource</tt> (if it supports the interface in question).
 * </p>
 *
 * @author Lyubomir Marinov
 */
public class MutePushBufferDataSource
    extends PushBufferDataSourceDelegate<PushBufferDataSource>
    implements MuteDataSource
{

    /**
     * The indicator which determines whether this <tt>DataSource</tt> is mute.
     */
    private boolean mute;

    /**
     * Initializes a new <tt>MutePushBufferDataSource</tt> instance which is to
     * provide mute support for a specific <tt>PushBufferDataSource</tt>.
     * 
     * @param dataSource the <tt>PushBufferDataSource</tt> the new instance is
     *            to provide mute support for
     */
    public MutePushBufferDataSource(PushBufferDataSource dataSource)
    {
        super(dataSource);
    }

    /**
     * Implements {@link PushBufferDataSource#getStreams()}. Wraps the streams
     * of the wrapped <tt>PushBufferDataSource</tt> into
     * <tt>MutePushBufferStream</tt> instances in order to provide mute support
     * to them.
     *
     * @return an array of <tt>PushBufferStream</tt> instances with enabled mute
     * support
     */
    public PushBufferStream[] getStreams()
    {
        PushBufferStream[] streams = dataSource.getStreams();

        if (streams != null)
        {
            for (int streamIndex = 0;
                    streamIndex < streams.length;
                    streamIndex++)
            {
                PushBufferStream stream = streams[streamIndex];

                if (stream != null)
                    streams[streamIndex] = new MutePushBufferStream(stream);
            }
        }
        return streams;
    }

    /**
     * Determines whether this <tt>DataSource</tt> is mute.
     * 
     * @return <tt>true</tt> if this <tt>DataSource</tt> is mute; otherwise,
     *         <tt>false</tt>
     */
    public synchronized boolean isMute()
    {
        return mute;
    }

    /**
     * Replaces the media data contained in a specific <tt>Buffer</tt> with a
     * compatible representation of silence.
     *
     * @param buffer the <tt>Buffer</tt> the data contained in which is to be
     * replaced with silence
     */
    public static void mute(Buffer buffer)
    {
        Object data = buffer.getData();

        if (data != null)
        {
            Class<?> dataClass = data.getClass();
            final int fromIndex = buffer.getOffset();
            final int toIndex = fromIndex + buffer.getLength();

            if (Format.byteArray.equals(dataClass))
                Arrays.fill((byte[]) data, fromIndex, toIndex, (byte) 0);
            else if (Format.intArray.equals(dataClass))
                Arrays.fill((int[]) data, fromIndex, toIndex, 0);
            else if (Format.shortArray.equals(dataClass))
                Arrays.fill((short[]) data, fromIndex, toIndex, (short) 0);

            buffer.setData(data);
        }
    }

    /**
     * Sets the mute state of this <tt>DataSource</tt>.
     * 
     * @param mute <tt>true</tt> to mute this <tt>DataSource</tt>; otherwise,
     *            <tt>false</tt>
     */
    public synchronized void setMute(boolean mute)
    {
        this.mute = mute;
    }

    /**
     * Implements a <tt>PushBufferStream</tt> wrapper which provides mute
     * support for the wrapped instance.
     */
    private class MutePushBufferStream
        extends SourceStreamDelegate<PushBufferStream>
        implements PushBufferStream
    {

        /**
         * Initializes a new <tt>MutePushBufferStream</tt> instance which is to
         * provide mute support to a specific <tt>PushBufferStream</tt>.
         * 
         * @param stream the <tt>PushBufferStream</tt> the new instance is to
         * provide mute support to
         */
        public MutePushBufferStream(PushBufferStream stream)
        {
            super(stream);
        }

        /**
         * Implements {@link PushBufferStream#getFormat()}. Delegates to the
         * wrapped <tt>PushBufferStream</tt>.
         *
         * @return the <tt>Format</tt> of the wrapped <tt>PushBufferStream</tt>
         */
        public Format getFormat()
        {
            return stream.getFormat();
        }

        /**
         * Implements {@link PushBufferStream#read(Buffer)}. If this instance is
         * muted (through its owning <tt>MutePushBufferDataSource</tt>),
         * overwrites the data read from the wrapped <tt>PushBufferStream</tt>
         * with silence data.
         *
         * @param buffer a <tt>Buffer</tt> in which the read data is to be
         * returned to the caller
         * @throws IOException if reading from the wrapped
         * <tt>PushBufferStream</tt> fails
         */
        public void read(Buffer buffer)
            throws IOException
        {
            stream.read(buffer);

            if (isMute())
                mute(buffer);
        }

        /**
         * Implements
         * {@link PushBufferStream#setTransferHandler(BufferTransferHandler)}.
         * Sets up the hiding of the wrapped <tt>PushBufferStream</tt> from the
         * specified <tt>transferHandler</tt> and thus gives this
         * <tt>MutePushBufferStream</tt> full control when the
         * <tt>transferHandler</tt> in question starts calling to the stream
         * given to it in
         * <tt>BufferTransferHandler#transferData(PushBufferStream)</tt>.
         *
         * @param transferHandler a <tt>BufferTransferHandler</tt> to be
         * notified by this instance when data is available for reading from it
         */
        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            stream.setTransferHandler(
                (transferHandler == null)
                    ? null
                    : new StreamSubstituteBufferTransferHandler(
                            transferHandler,
                            stream,
                            this));
        }
    }
}
