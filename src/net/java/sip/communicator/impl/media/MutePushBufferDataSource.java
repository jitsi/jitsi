/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.io.*;
import java.util.*;
import javax.media.*;
import javax.media.control.*;
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
    extends PushBufferDataSource
    implements CaptureDevice
{

    /**
     * The wrapped <tt>DataSource</tt> this instance provides mute support for.
     */
    private final PushBufferDataSource dataSource;

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
        this.dataSource = dataSource;
    }

    public void connect() throws IOException
    {
        dataSource.connect();
    }

    public void disconnect()
    {
        dataSource.disconnect();
    }

    public CaptureDeviceInfo getCaptureDeviceInfo()
    {
        CaptureDeviceInfo captureDeviceInfo;

        if (dataSource instanceof CaptureDevice)
            captureDeviceInfo =
                ((CaptureDevice) dataSource).getCaptureDeviceInfo();
        else
            captureDeviceInfo = null;
        return captureDeviceInfo;
    }

    public String getContentType()
    {
        return dataSource.getContentType();
    }

    public Object getControl(String controlType)
    {
        return dataSource.getControl(controlType);
    }

    public Object[] getControls()
    {
        return dataSource.getControls();
    }

    public Time getDuration()
    {
        return dataSource.getDuration();
    }

    public FormatControl[] getFormatControls()
    {
        FormatControl[] formatControls;

        if (dataSource instanceof CaptureDevice)
            formatControls = ((CaptureDevice) dataSource).getFormatControls();
        else
            formatControls = new FormatControl[0];
        return formatControls;
    }

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

    public void start() throws IOException
    {
        dataSource.start();
    }

    public void stop() throws IOException
    {
        dataSource.stop();
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

        public ContentDescriptor getContentDescriptor()
        {
            return stream.getContentDescriptor();
        }

        public long getContentLength()
        {
            return stream.getContentLength();
        }

        public Object getControl(String controlType)
        {
            return stream.getControl(controlType);
        }

        public Object[] getControls()
        {
            return stream.getControls();
        }

        public Format getFormat()
        {
            return stream.getFormat();
        }

        public boolean endOfStream()
        {
            return stream.endOfStream();
        }

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

        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            stream.setTransferHandler((transferHandler == null) ? null
                : new MuteBufferTransferHandler(transferHandler));
        }

        /**
         * Implements a <tt>BufferTransferHandler</tt> wrapper which doesn't
         * expose a wrapped <tt>PushBufferStream</tt> but rather its wrapper in
         * order to give full control to the
         * {@link PushBufferStream#read(Buffer)} method of the wrapper.
         */
        public class MuteBufferTransferHandler
            implements BufferTransferHandler
        {

            /**
             * The wrapped <tt>BufferTransferHandler</tt> which receives the
             * actual events from the wrapped <tt>PushBufferStream</tt>.
             */
            private final BufferTransferHandler transferHandler;

            /**
             * Initializes a new <tt>MuteBufferTransferHandler</tt> instance
             * which is to overwrite the source <tt>PushBufferStream</tt> of a
             * specific <tt>BufferTransferHandler</tt>.
             * 
             * @param transferHandler the <tt>BufferTransferHandler</tt> the new
             *            instance is to overwrite the source
             *            <tt>PushBufferStream</tt> of
             */
            public MuteBufferTransferHandler(
                BufferTransferHandler transferHandler)
            {
                this.transferHandler = transferHandler;
            }

            public void transferData(PushBufferStream stream)
            {
                transferHandler.transferData(MutePushBufferStream.this);
            }
        }
    }
}
