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
 * Implements a <tt>PullBufferDataSource</tt> wrapper which provides mute
 * support for the wrapped instance.
 * <p>
 * Because the class wouldn't work for our use case without it,
 * <tt>CaptureDevice</tt> is implemented and is being delegated to the wrapped
 * <tt>DataSource</tt> (if it supports the interface in question).
 * </p>
 *
 * @author Damian Minkov
 */
public class MutePullBufferDataSource
    extends PullBufferDataSourceDelegate<PullBufferDataSource>
    implements MuteDataSource
{
    /**
     * The indicator which determines whether this <tt>DataSource</tt> is mute.
     */
    private boolean mute;

    /**
     * Initializes a new <tt>MutePullBufferDataSource</tt> instance which is to
     * provide mute support for a specific <tt>PullBufferDataSource</tt>.
     *
     * @param dataSource the <tt>PullBufferDataSource</tt> the new instance is
     *            to provide mute support for
     */
    public MutePullBufferDataSource(PullBufferDataSource dataSource)
    {
        super(dataSource);
    }

    /**
     * Sets the mute state of this <tt>DataSource</tt>.
     *
     * @param mute <tt>true</tt> to mute this <tt>DataSource</tt>; otherwise,
     *            <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
        this.mute = mute;
    }

    /**
     * Determines whether this <tt>DataSource</tt> is mute.
     *
     * @return <tt>true</tt> if this <tt>DataSource</tt> is mute; otherwise,
     *         <tt>false</tt>
     */
    public boolean isMute()
    {
        return mute;
    }

    /**
     * Implements {@link PullBufferDataSource#getStreams()}. Wraps the streams
     * of the wrapped <tt>PullBufferDataSource</tt> into
     * <tt>MutePullBufferStream</tt> instances in order to provide mute support
     * to them.
     *
     * @return an array of <tt>PullBufferStream</tt> instances with enabled mute
     * support
     */
    @Override
    public PullBufferStream[] getStreams()
    {
        PullBufferStream[] streams = dataSource.getStreams();

        if (streams != null)
            for (int streamIndex = 0; streamIndex < streams.length; streamIndex++)
                streams[streamIndex] =
                    new MutePullBufferStream(streams[streamIndex]);
        return streams;
    }

    /**
     * Implements a <tt>PullBufferStream</tt> wrapper which provides mute
     * support for the wrapped instance.
     */
    private class MutePullBufferStream
        implements PullBufferStream
    {
        /**
         * The wrapped stream this instance provides mute support for.
         */
        private final PullBufferStream stream;

        /**
         * Initializes a new <tt>MutePullBufferStream</tt> instance which is to
         * provide mute support for a specific <tt>PullBufferStream</tt>.
         *
         * @param stream the <tt>PullBufferStream</tt> the new instance is to
         *            provide mute support for
         */
        private MutePullBufferStream(PullBufferStream stream)
        {
            this.stream = stream;
        }

        /*
         * Implements PullBufferStream#willReadBlock(). Delegates to the wrapped
         * PullSourceStream.
         */
        public boolean willReadBlock()
        {
            return stream.willReadBlock();
        }

        /*
         * Implements PullBufferStream#read(Buffer). If this instance is muted
         * (through its owning MutePullBufferDataSource), overwrites the data
         * read from the wrapped PullBufferStream with silence data.
         */
        public void read(Buffer buffer)
            throws IOException
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
         * Implements SourceStream#getContentDescriptor(). Delegates to the
         * wrapped PullBufferStream.
         */
        public ContentDescriptor getContentDescriptor()
        {
            return stream.getContentDescriptor();
        }

        /*
         * Implements SourceStream#getContentLength(). Delegates to the wrapped
         * PullBufferStream.
         */
        public long getContentLength()
        {
            return stream.getContentLength();
        }

        /*
         * Implements Controls#getControl(String). Delegates to the wrapped
         * PullBufferStream.
         */
        public Object getControl(String controlType)
        {
            return stream.getControl(controlType);
        }

        /*
         * Implements Controls#getControls(). Delegates to the wrapped
         * PullBufferStream.
         */
        public Object[] getControls()
        {
            return stream.getControls();
        }

        /*
         * Implements PullBufferStream#getFormat(). Delegates to the wrapped
         * PullBufferStream.
         */
        public Format getFormat()
        {
            return stream.getFormat();
        }

        /*
         * Implements SourceStream#endOfStream(). Delegates to the wrapped
         * PullBufferStream.
         */
        public boolean endOfStream()
        {
            return stream.endOfStream();
        }
    }
}
