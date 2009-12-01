/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author Lubomir Marinov
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
            for (int streamIndex = 0; streamIndex < streams.length; streamIndex++)
                streams[streamIndex] =
                    new MutePushBufferStream(streams[streamIndex]);
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
        implements PushBufferStream
    {

        /**
         * The wrapped stream this instance provides mute support for.
         */
        private final PushBufferStream stream;

        /**
         * Initializes a new <tt>MutePushBufferStream</tt> instance which is to
         * provide mute support for a specific <tt>PushBufferStream</tt>.
         * 
         * @param stream the <tt>PushBufferStream</tt> the new instance is to
         *            provide mute support for
         */
        public MutePushBufferStream(PushBufferStream stream)
        {
            this.stream = stream;
        }

        /*
         * Implements SourceStream#getContentDescriptor(). Delegates to the
         * wrapped PushBufferStream.
         */
        public ContentDescriptor getContentDescriptor()
        {
            return stream.getContentDescriptor();
        }

        /*
         * Implements SourceStream#getContentLength(). Delegates to the wrapped
         * PushBufferStream.
         */
        public long getContentLength()
        {
            return stream.getContentLength();
        }

        /*
         * Implements Controls#getControl(String). Delegates to the wrapped
         * PushBufferStream.
         */
        public Object getControl(String controlType)
        {
            return stream.getControl(controlType);
        }

        /*
         * Implements Controls#getControls(). Delegates to the wrapped
         * PushBufferStream.
         */
        public Object[] getControls()
        {
            return stream.getControls();
        }

        /*
         * Implements PushBufferStream#getFormat(). Delegates to the wrapped
         * PushBufferStream.
         */
        public Format getFormat()
        {
            return stream.getFormat();
        }

        /*
         * Implements SourceStream#endOfStream(). Delegates to the wrapped
         * PushBufferStream.
         */
        public boolean endOfStream()
        {
            return stream.endOfStream();
        }

        /*
         * Implements PushBufferStream#read(Buffer). If this instance is muted
         * (through its owning MutePushBufferDataSource), overwrites the data
         * read from the wrapped PushBufferStream with silence data.
         */
        public void read(Buffer buffer) throws IOException
        {
            stream.read(buffer);

            if (isMute())
            {
                Object data = buffer.getData();

                if (data != null)
                {
                    Class<?> dataClass = data.getClass();
                    final int fromIndex = buffer.getOffset();
                    final int toIndex = fromIndex + buffer.getLength();

                    if (Format.byteArray.equals(dataClass))
                        Arrays
                            .fill((byte[]) data, fromIndex, toIndex, (byte) 0);
                    else if (Format.intArray.equals(dataClass))
                        Arrays.fill((int[]) data, fromIndex, toIndex, 0);
                    else if (Format.shortArray.equals(dataClass))
                        Arrays.fill((short[]) data, fromIndex, toIndex,
                            (short) 0);

                    buffer.setData(data);
                }
            }
        }

        /*
         * Implements PushBufferStream#setTransferHandler(BufferTransferHandler).
         * Sets up the hiding of the wrapped PushBufferStream from the specified
         * transferHandler and thus gives this MutePushBufferStream full control
         * when the transferHandler in question starts calling to the stream
         * given to it in BufferTransferHandler#transferData(PushBufferStream). 
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
