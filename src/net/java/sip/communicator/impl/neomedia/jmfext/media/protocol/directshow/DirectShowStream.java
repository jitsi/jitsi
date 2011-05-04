/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.directshow;

import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.directshow.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;

/**
 * Implements a <tt>PushBufferStream</tt> using DirectShow.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class DirectShowStream extends AbstractPushBufferStream
{
    /**
     * The pool of <tt>ByteBuffer</tt>s this instances is using to transfer the
     * media data captured by {@link #grabber} out of this instance
     * through the <tt>Buffer</tt>s specified in its {@link #read(Buffer)}.
     */
    private final ByteBufferPool bufferPool = new ByteBufferPool();

    /**
     * The captured media data to be returned in {@link #read(Buffer)}.
     */
    private ByteBuffer data;

    /**
     * The <tt>Object</tt> which synchronizes the access to the
     * {@link #data}-related fields of this instance.
     */
    private final Object dataSyncRoot = new Object();

    /**
     * The time stamp in nanoseconds of {@link #data}.
     */
    private long dataTimeStamp;

    /**
     * The last-known <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt>.
     */
    private final Format format;

    /**
     * The captured media data to become the value of {@link #data} as soon as
     * the latter becomes is consumed. Thus prepares this
     * <tt>DirectShowStream</tt> to provide the latest available frame and not
     * wait for DirectShow to capture a new one.
     */
    private ByteBuffer nextData = null;

    /**
     * The time stamp in nanoseconds of {@link #nextData}.
     */
    private long nextDataTimeStamp;

    /**
     * The <tt>Thread</tt> which is to call
     * {@link BufferTransferHandler#transferData(PushBufferStream)} for this
     * <tt>DirectShowStream</tt> so that the call is not made in DirectShow
     * and we can drop late frames when
     * {@link #automaticallyDropsLateVideoFrames} is <tt>false</tt>.
     */
    private Thread transferDataThread;

    /**
     * The indicator which determines whether {@link #grabber}
     * automatically drops late frames. If <tt>false</tt>, we have to drop them
     * ourselves because DirectShow will buffer them all and the video will
     * be late.
     */
    private boolean automaticallyDropsLateVideoFrames = false;

    /**
     * Delegate class to handle video data.
     */
    final DSCaptureDevice.GrabberDelegate grabber
        = new DSCaptureDevice.GrabberDelegate()
        {
            @Override
            public void frameReceived(long ptr, int length)
            {
                processFrame(ptr, length);
            }
        };

    /**
     * Initializes a new <tt>DirectShowStream</tt> instance which is to have its
     * <tt>Format</tt>-related information abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     */
    DirectShowStream(FormatControl formatControl)
    {
        super(formatControl);

        format = (VideoFormat) formatControl.getFormat();
    }

    /**
     * Process received frames from DirectShow capture device
     *
     * @param ptr native pointer to data
     * @param length length of data
     */
    private void processFrame(long ptr, int length)
    {
        boolean transferData = false;

        synchronized (dataSyncRoot)
        {
            if(!automaticallyDropsLateVideoFrames && (data != null))
            {
                if(nextData != null)
                {
                    bufferPool.returnFreeBuffer(nextData);
                    nextData = null;
                }

                nextData
                    = bufferPool.getFreeBuffer(length);
                if(nextData != null)
                {
                    nextData.setLength(
                            DSCaptureDevice.getBytes(ptr,
                                    nextData.ptr,
                                    nextData.capacity));
                    nextDataTimeStamp = System.nanoTime();
                }

                return;
            }

            if(data != null)
            {
                bufferPool.returnFreeBuffer(data);
                data = null;
            }

            data = bufferPool.getFreeBuffer(length);
            if(data != null)
            {
                data.setLength(DSCaptureDevice.getBytes(ptr,
                        data.ptr, data.capacity));
                dataTimeStamp = System.nanoTime();
            }

            if(nextData != null)
            {
                bufferPool.returnFreeBuffer(nextData);
                nextData = null;
            }

            if(automaticallyDropsLateVideoFrames)
                transferData = (data != null);
            else
            {
                transferData = false;
                dataSyncRoot.notifyAll();
            }
        }

        if(transferData)
        {
            BufferTransferHandler transferHandler = this.transferHandler;

            if(transferHandler != null)
                transferHandler.transferData(this);
        }
    }

    /**
     * Gets the <tt>Format</tt> of this <tt>PushBufferStream</tt> as directly
     * known by it.
     *
     * @return the <tt>Format</tt> of this <tt>PushBufferStream</tt> as directly
     * known by it or <tt>null</tt> if this <tt>PushBufferStream</tt> does not
     * directly know its <tt>Format</tt> and it relies on the
     * <tt>PushBufferDataSource</tt> which created it to report its
     * <tt>Format</tt>
     */
    @Override
    protected Format doGetFormat()
    {
        return (this.format == null) ? super.doGetFormat() : this.format;
    }

    /**
     * Releases the resources used by this instance throughout its existence and
     * makes it available for garbage collection. This instance is considered
     * unusable after closing.
     *
     * @see AbstractPushBufferStream#close()
     */
    @Override
    public void close()
    {
        bufferPool.close();
    }

    /**
     * Calls {@link BufferTransferHandler#transferData(PushBufferStream)} from
     * inside {@link #transferDataThread} so that the call is not made in
     * DirectShow and we can drop late frames in the meantime.
     */
    private void runInTransferDataThread()
    {
        boolean transferData = false;

        while(Thread.currentThread().equals(transferDataThread))
        {
            if(transferData)
            {
                BufferTransferHandler transferHandler = this.transferHandler;

                if(transferHandler != null)
                    transferHandler.transferData(this);

                synchronized (dataSyncRoot)
                {
                    if(data != null)
                    {
                        bufferPool.returnFreeBuffer(data);
                        data = null;
                    }

                    data = nextData;
                    dataTimeStamp = nextDataTimeStamp;
                    nextData = null;
                }
            }

            synchronized (dataSyncRoot)
            {
                if(data == null)
                {
                    data = nextData;
                    dataTimeStamp = nextDataTimeStamp;
                    nextData = null;
                }
                if(data == null)
                {
                    boolean interrupted = false;

                    try
                    {
                        dataSyncRoot.wait();
                    }
                    catch (InterruptedException iex)
                    {
                        interrupted = true;
                    }
                    if(interrupted)
                        Thread.currentThread().interrupt();

                    transferData = (data != null);
                }
                else
                {
                    transferData = true;
                }
            }
        }
    }

    /**
     * Starts the transfer of media data from this <tt>PushBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>PushBufferStream</tt>
     */
    @Override
    public void start() throws IOException
    {
        if(!automaticallyDropsLateVideoFrames)
        {
            transferDataThread
                = new Thread(getClass().getSimpleName())
                {
                    @Override
                    public void run()
                    {
                        runInTransferDataThread();
                    }
                };

            transferDataThread.start();
        }
    }

    /**
     * Stops the transfer of media data from this <tt>PushBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>PushBufferStream</tt>
     */
    @Override
    public void stop() throws IOException
    {
        transferDataThread = null;

        synchronized (dataSyncRoot)
        {
            if(data != null)
            {
                bufferPool.returnFreeBuffer(data);
                data = null;
            }

            if(nextData != null)
            {
                bufferPool.returnFreeBuffer(nextData);
                nextData = null;
            }

            if(!automaticallyDropsLateVideoFrames)
                dataSyncRoot.notifyAll();
        }
    }

    /**
     * Reads media data from this <tt>PushBufferStream</tt> into a specific
     * <tt>Buffer</tt> without blocking.
     *
     * @param buffer the <tt>Buffer</tt> in which media data is to be read from
     * this <tt>PushBufferStream</tt>
     * @throws IOException if anything goes wrong while reading media data from
     * this <tt>PushBufferStream</tt> into the specified <tt>buffer</tt>
     */
    public void read(Buffer buffer) throws IOException
    {
        synchronized (dataSyncRoot)
        {
            if(data == null)
            {
                buffer.setLength(0);
                return;
            }

            Format bufferFormat = buffer.getFormat();

            if(bufferFormat == null)
            {
                bufferFormat = getFormat();
                if(bufferFormat != null)
                    buffer.setFormat(bufferFormat);
            }
            if(bufferFormat instanceof AVFrameFormat)
            {
                FinalizableAVFrame.read(
                        buffer,
                        bufferFormat,
                        data,
                        bufferPool);
            }
            else
            {
                Object bufferData = buffer.getData();
                byte[] bufferByteData;
                int dataLength = data.getLength();

                if(bufferData instanceof byte[])
                {
                    bufferByteData = (byte[]) bufferData;
                    if(bufferByteData.length < dataLength)
                        bufferByteData = null;
                }
                else
                    bufferByteData = null;
                if(bufferByteData == null)
                {
                    bufferByteData = new byte[dataLength];
                    buffer.setData(bufferByteData);
                }

                /* XXX */
                //DSCaptureDevice.getBytes(bufferByteData, 0, dataLength,
                //        data.ptr);
                //CVPixelBuffer.memcpy(bufferByteData, 0, dataLength, data.ptr);

                buffer.setLength(dataLength);
                buffer.setOffset(0);

                bufferPool.returnFreeBuffer(data);
            }

            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            buffer.setTimeStamp(dataTimeStamp);

            data = null;
            if(!automaticallyDropsLateVideoFrames)
                dataSyncRoot.notifyAll();
        }
    }
}
