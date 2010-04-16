/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.quicktime;

import java.awt.Dimension; // disambiguation
import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.impl.neomedia.quicktime.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a <tt>PushBufferStream</tt> using QuickTime/QTKit.
 *
 * @author Lubomir Marinov
 */
public class QuickTimeStream
    extends AbstractPushBufferStream
{

    /**
     * The <tt>Logger</tt> used by the <tt>QuickTimeStream</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(QuickTimeStream.class);

    /**
     * The indicator which determines whether {@link #captureOutput}
     * automatically drops late frames. If <tt>false</tt>, we have to drop them
     * ourselves because QuickTime/QTKit will buffer them all and the video will
     * be late.
     */
    private final boolean automaticallyDropsLateVideoFrames;

    /**
     * The pool of <tt>ByteBuffer</tt>s this instances is using to transfer the
     * media data captured by {@link #captureOutput} out of this instance
     * through the <tt>Buffer</tt>s specified in its {@link #process(Buffer)}.
     */
    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();

    /**
     * The <tt>QTCaptureOutput</tt> represented by this <tt>SourceStream</tt>.
     */
    final QTCaptureDecompressedVideoOutput captureOutput
        = new QTCaptureDecompressedVideoOutput();

    /**
     * The <tt>VideoFormat</tt> which has been successfully set on
     * {@link #captureOutput}.
     */
    private VideoFormat captureOutputFormat;

    /**
     * The indicator which determines whether this <tt>QuickTimeStream</tt> has
     * been closed. Introduced to determine when <tt>ByteBuffer</tt>s are to be
     * disposed of and no longer be pooled in {@link #buffers}.
     */
    private boolean closed = false;

    /**
     * The captured media data to be returned in {@link #read(Buffer)}.
     */
    private ByteBuffer data;

    /**
     * The <tt>Format</tt> of {@link #data} if known. If possible, determined by
     * the <tt>CVPixelBuffer</tt> video frame from which <tt>data</tt> is
     * acquired.
     */
    private Format dataFormat;

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
    private Format format;

    /**
     * The captured media data to become the value of {@link #data} as soon as
     * the latter becomes is consumed. Thus prepares this
     * <tt>QuickTimeStream</tt> to provide the latest available frame and not
     * wait for QuickTime/QTKit to capture a new one.
     */
    private ByteBuffer nextData;

    /**
     * The <tt>Format</tt> of {@link #nextData} if known.
     */
    private Format nextDataFormat;

    /**
     * The time stamp in nanoseconds of {@link #nextData}.
     */
    private long nextDataTimeStamp;

    /**
     * The <tt>Thread</tt> which is to call
     * {@link BufferTransferHandler#transferData(PushBufferStream)} for this
     * <tt>QuickTimeStream</tt> so that the call is not made in QuickTime/QTKit
     * and we can drop late frames when {@link #automaticallyDropsLateFrames} is
     * <tt>false</tt>.
     */
    private Thread transferDataThread;

    /**
     * Initializes a new <tt>QuickTimeStream</tt> instance which is to have its
     * <tt>Format</tt>-related information abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     */
    QuickTimeStream(FormatControl formatControl)
    {
        super(formatControl);

        if (formatControl != null)
        {
            Format format = formatControl.getFormat();

            if (format != null)
                setCaptureOutputFormat(format);
        }

        automaticallyDropsLateVideoFrames
            = false;//captureOutput.setAutomaticallyDropsLateVideoFrames(true);

        captureOutput
            .setDelegate(
                new QTCaptureDecompressedVideoOutput.Delegate()
                {

                    /**
                     * Notifies this <tt>Delegate</tt> that the
                     * <tt>QTCaptureOutput</tt> to which it is set has output a
                     * specific <tt>CVImageBuffer</tt> representing a video
                     * frame with a specific <tt>QTSampleBuffer</tt>.
                     *
                     * @param videoFrame the <tt>CVImageBuffer</tt> which
                     * represents the output video frame
                     * @param sampleBuffer the <tt>QTSampleBuffer</tt> which
                     * represents additional details about the output video
                     * samples
                     */
                    public void outputVideoFrameWithSampleBuffer(
                            CVImageBuffer videoFrame,
                            QTSampleBuffer sampleBuffer)
                    {
                        captureOutputDidOutputVideoFrameWithSampleBuffer(
                            captureOutput,
                            videoFrame,
                            sampleBuffer);
                    }
                });
    }

    /**
     * Notifies this instance that its <tt>QTCaptureOutput</tt> has output a
     * specific <tt>CVImageBuffer</tt> representing a video frame with a
     * specific <tt>QTSampleBuffer</tt>.
     *
     * @param captureOutput the <tt>QTCaptureOutput</tt> which has output a
     * video frame
     * @param videoFrame the <tt>CVImageBuffer</tt> which represents the output
     * video frame
     * @param sampleBuffer the <tt>QTSampleBuffer</tt> which represents
     * additional details about the output video samples
     */
    private void captureOutputDidOutputVideoFrameWithSampleBuffer(
            QTCaptureOutput captureOutput,
            CVImageBuffer videoFrame,
            QTSampleBuffer sampleBuffer)
    {
        CVPixelBuffer pixelBuffer = (CVPixelBuffer) videoFrame;
        boolean transferData;

        synchronized (dataSyncRoot)
        {
            if (!automaticallyDropsLateVideoFrames && (data != null))
            {
                if (nextData != null)
                {
                    returnFreeBuffer(nextData);
                    nextData = null;
                }

                nextData = getFreeBuffer(pixelBuffer.getByteCount());
                if (nextData != null)
                {
                    nextData.setLength(
                            pixelBuffer.getBytes(
                                    nextData.ptr,
                                    nextData.capacity));
                    nextDataTimeStamp = System.nanoTime();
                    if (nextDataFormat == null)
                        nextDataFormat = getVideoFrameFormat(pixelBuffer);
                }
                return;
            }

            if (data != null)
            {
                returnFreeBuffer(data);
                data = null;
            }

            data = getFreeBuffer(pixelBuffer.getByteCount());
            if (data != null)
            {
                data.setLength(pixelBuffer.getBytes(data.ptr, data.capacity));
                dataTimeStamp = System.nanoTime();
                if (dataFormat == null)
                    dataFormat = getVideoFrameFormat(pixelBuffer);
            }
            if (nextData != null)
            {
                returnFreeBuffer(nextData);
                nextData = null;
            }

            if (automaticallyDropsLateVideoFrames)
                transferData = (data != null);
            else
            {
                transferData = false;
                dataSyncRoot.notifyAll();
            }
        }

        if (transferData)
        {
            BufferTransferHandler transferHandler = this.transferHandler;

            if (transferHandler != null)
                transferHandler.transferData(this);
        }
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
        super.close();

        captureOutput.setDelegate(null);

        synchronized (buffers)
        {
            closed = true;

            Iterator<ByteBuffer> bufferIter = buffers.iterator();
            boolean loggerIsTraceEnabled = logger.isTraceEnabled();
            int leakedCount = 0;

            while (bufferIter.hasNext())
            {
                ByteBuffer buffer = bufferIter.next();

                if (buffer.isFree())
                {
                    bufferIter.remove();
                    FFmpeg.av_free(buffer.ptr);
                } else if (loggerIsTraceEnabled)
                    leakedCount++;
            }
            if (loggerIsTraceEnabled)
            {
                logger.trace(
                        "Leaking " + leakedCount + " ByteBuffer instances.");
            }
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
        Format format;

        if (this.format == null)
        {
            format = getCaptureOutputFormat();
            if (format == null)
                format = super.doGetFormat();
            else
            {
                VideoFormat videoFormat = (VideoFormat) format;

                if (videoFormat.getSize() != null)
                    this.format = format;
                else
                    format
                        = videoFormat
                            .intersects(
                                new VideoFormat(
                                        null,
                                        new Dimension(
                                                DataSource.DEFAULT_WIDTH,
                                                DataSource.DEFAULT_HEIGHT),
                                        Format.NOT_SPECIFIED,
                                        Format.byteArray,
                                        Format.NOT_SPECIFIED));
            }
        }
        else
            format = this.format;
        return format;
    }

    /**
     * Gets the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt> as indicated by {@link #captureOutput}.
     *
     * @return the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt> as indicated by {@link #captureOutput}
     */
    private Format getCaptureOutputFormat()
    {
        NSDictionary pixelBufferAttributes
            = captureOutput.pixelBufferAttributes();

        if (pixelBufferAttributes != null)
        {
            int pixelFormatType
                = pixelBufferAttributes
                    .intForKey(
                        CVPixelBufferAttributeKey
                            .kCVPixelBufferPixelFormatTypeKey);
            int width
                = pixelBufferAttributes
                    .intForKey(
                        CVPixelBufferAttributeKey.kCVPixelBufferWidthKey);
            int height
                = pixelBufferAttributes
                    .intForKey(
                        CVPixelBufferAttributeKey.kCVPixelBufferHeightKey);

            switch (pixelFormatType)
            {
            case CVPixelFormatType.kCVPixelFormatType_32ARGB:
                if (captureOutputFormat instanceof AVFrameFormat)
                    return
                        new AVFrameFormat(
                                ((width == 0) && (height == 0)
                                    ? null
                                    : new Dimension(width, height)),
                                Format.NOT_SPECIFIED,
                                FFmpeg.PIX_FMT_ARGB);
                else
                    return
                        new RGBFormat(
                                ((width == 0) && (height == 0)
                                    ? null
                                    : new Dimension(width, height)),
                                Format.NOT_SPECIFIED,
                                Format.byteArray,
                                Format.NOT_SPECIFIED,
                                32,
                                2, 3, 4);
            case CVPixelFormatType.kCVPixelFormatType_420YpCbCr8Planar:
                if ((width == 0) && (height == 0))
                {
                    if (captureOutputFormat instanceof AVFrameFormat)
                        return new AVFrameFormat(FFmpeg.PIX_FMT_YUV420P);
                    else
                        return new YUVFormat(YUVFormat.YUV_420);
                }
                else if (captureOutputFormat instanceof AVFrameFormat)
                {
                    return
                        new AVFrameFormat(
                                new Dimension(width, height),
                                Format.NOT_SPECIFIED,
                                FFmpeg.PIX_FMT_YUV420P);
                }
                else
                {
                    int strideY = width;
                    int strideUV = strideY / 2;
                    int offsetY = 0;
                    int offsetU = strideY * height;
                    int offsetV = offsetU + strideUV * height / 2;

                    return
                        new YUVFormat(
                                new Dimension(width, height),
                                Format.NOT_SPECIFIED,
                                Format.byteArray,
                                Format.NOT_SPECIFIED,
                                YUVFormat.YUV_420,
                                strideY, strideUV,
                                offsetY, offsetU, offsetV);
                }
            }
        }
        return null;
    }

    /**
     * Gets a <tt>ByteBuffer</tt> out of the pool of free <tt>ByteBuffer</tt>s
     * (i.e. <tt>ByteBuffer</tt>s ready for writing captured media data into
     * them) which is capable to receiving at least <tt>capacity</tt> number of
     * bytes.
     *
     * @param capacity the minimal number of bytes that the returned
     * <tt>ByteBuffer</tt> is to be capable of receiving
     * @return a <tt>ByteBuffer</tt> which is ready for writing captured media
     * data into and which is capable of receiving at least <tt>capacity</tt>
     * number of bytes
     */
    private ByteBuffer getFreeBuffer(int capacity)
    {
        synchronized (buffers)
        {
            if (closed)
                return null;

            int bufferCount = buffers.size();
            ByteBuffer freeBuffer = null;

            /*
             * XXX Pad with FF_INPUT_BUFFER_PADDING_SIZE or hell will break
             * loose.
             */
            capacity += FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE;

            for (int bufferIndex = 0; bufferIndex < bufferCount; bufferIndex++)
            {
                ByteBuffer buffer = buffers.get(bufferIndex);

                if (buffer.isFree() && (buffer.capacity >= capacity))
                {
                    freeBuffer = buffer;
                    break;
                }
            }
            if (freeBuffer == null)
            {
                freeBuffer = new ByteBuffer(capacity);
                buffers.add(freeBuffer);
            }
            freeBuffer.setFree(false);
            return freeBuffer;
        }
    }

    /**
     * Gets the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt> as indicated by a specific
     * <tt>CVPixelBuffer</tt>.
     *
     * @param videoFrame the <tt>CVPixelBuffer</tt> which provides details about
     * the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt>
     * @return the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt> as indicated by the specified
     * <tt>CVPixelBuffer</tt>
     */
    private Format getVideoFrameFormat(CVPixelBuffer videoFrame)
    {
        Format format = getFormat();
        Dimension size = ((VideoFormat) format).getSize();

        if ((size == null) || ((size.width == 0) && (size.height == 0)))
            format
                = format
                    .intersects(
                        new VideoFormat(
                                null,
                                new Dimension(
                                        videoFrame.getWidth(),
                                        videoFrame.getHeight()),
                                Format.NOT_SPECIFIED,
                                Format.byteArray,
                                Format.NOT_SPECIFIED));
        return format;
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
    public void read(Buffer buffer)
        throws IOException
    {
        synchronized (dataSyncRoot)
        {
            if (data == null)
            {
                buffer.setLength(0);
                return;
            }

            if (dataFormat != null)
                buffer.setFormat(dataFormat);

            Format bufferFormat = buffer.getFormat();

            if (bufferFormat == null)
            {
                bufferFormat = getFormat();
                if (bufferFormat != null)
                    buffer.setFormat(bufferFormat);
            }
            if (bufferFormat instanceof AVFrameFormat)
            {
                Object bufferData = buffer.getData();
                AVFrame bufferFrame;
                long bufferFramePtr;
                long bufferPtrToReturnFree;

                if (bufferData instanceof AVFrame)
                {
                    bufferFrame = (AVFrame) bufferData;
                    bufferFramePtr = bufferFrame.getPtr();
                    bufferPtrToReturnFree
                        = FFmpeg.avpicture_get_data0(bufferFramePtr);
                }
                else
                {
                    bufferFrame = new FinalizableAVFrame();
                    buffer.setData(bufferFrame);
                    bufferFramePtr = bufferFrame.getPtr();
                    bufferPtrToReturnFree = 0;
                }

                AVFrameFormat bufferFrameFormat = (AVFrameFormat) bufferFormat;
                Dimension bufferFrameSize = bufferFrameFormat.getSize();

                FFmpeg.avpicture_fill(
                        bufferFramePtr,
                        data.ptr,
                        bufferFrameFormat.getPixFmt(),
                        bufferFrameSize.width, bufferFrameSize.height);
//System.err.println(
//        "QuickTimeStream.read: bufferFramePtr= 0x"
//            + Long.toHexString(bufferFramePtr)
//            + ", data.ptr= 0x"
//            + Long.toHexString(data.ptr));
                if (bufferPtrToReturnFree != 0)
                    returnFreeBuffer(bufferPtrToReturnFree);
            }
            else
            {
                Object bufferData = buffer.getData();
                byte[] bufferByteData;
                int dataLength = data.getLength();

                if (bufferData instanceof byte[])
                {
                    bufferByteData = (byte[]) bufferData;
                    if (bufferByteData.length < dataLength)
                        bufferByteData = null;
                }
                else
                    bufferByteData = null;
                if (bufferByteData == null)
                {
                    bufferByteData = new byte[dataLength];
                    buffer.setData(bufferByteData);
                }
                CVPixelBuffer.memcpy(bufferByteData, 0, dataLength, data.ptr);

                buffer.setLength(dataLength);
                buffer.setOffset(0);

                returnFreeBuffer(data);
            }

            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            buffer.setTimeStamp(dataTimeStamp);

            data = null;
            if (!automaticallyDropsLateVideoFrames)
                dataSyncRoot.notifyAll();
        }
    }

    /**
     * Returns a specific <tt>ByteBuffer</tt> into the pool of free
     * <tt>ByteBuffer</tt>s (i.e. <tt>ByteBuffer</tt>s ready for writing
     * captured media data into them).
     *
     * @param buffer the <tt>ByteBuffer</tt> to be returned into the pool of
     * free <tt>ByteBuffer</tt>s
     */
    private void returnFreeBuffer(ByteBuffer buffer)
    {
        synchronized (buffers)
        {
            buffer.setFree(true);
            if (closed && buffers.remove(buffer))
                FFmpeg.av_free(buffer.ptr);
        }
    }

    /**
     * Returns a specific <tt>ByteBuffer</tt> given by the pointer to the native
     * memory that it represents into the pool of free <tt>ByteBuffer</tt>s
     * (i.e. <tt>ByteBuffer</tt>s ready for writing captured media data into
     * them).
     *
     * @param bufferPtr the pointer to the native memory represented by the
     * <tt>ByteBuffer</tt> to be returned into the pool of free
     * <tt>ByteBuffer</tt>s
     */
    private void returnFreeBuffer(long bufferPtr)
    {
        synchronized (buffers)
        {
            for (ByteBuffer buffer : buffers)
                if (buffer.ptr == bufferPtr)
                {
                    returnFreeBuffer(buffer);
                    break;
                }
        }
    }

    /**
     * Calls {@link BufferTransferHandler#transferData(PushBufferStream)} from
     * inside {@link #transferDataThread} so that the call is not made in
     * QuickTime/QTKit and we can drop late frames in the meantime.
     */
    private void runInTransferDataThread()
    {
        boolean transferData = false;

        while (Thread.currentThread().equals(transferDataThread))
        {
            if (transferData)
            {
                BufferTransferHandler transferHandler = this.transferHandler;

                if (transferHandler != null)
                    transferHandler.transferData(this);

                synchronized (dataSyncRoot)
                {
                    if (data != null)
                    {
                        returnFreeBuffer(data);
                        data = null;
                    }

                    data = nextData;
                    dataTimeStamp = nextDataTimeStamp;
                    if (dataFormat == null)
                        dataFormat = nextDataFormat;
                    nextData = null;
                }
            }
            
            synchronized (dataSyncRoot)
            {
                if (data == null)
                {
                    data = nextData;
                    dataTimeStamp = nextDataTimeStamp;
                    if (dataFormat == null)
                        dataFormat = nextDataFormat;
                    nextData = null;
                }
                if (data == null)
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
                    if (interrupted)
                        Thread.currentThread().interrupt();

                    transferData = (data != null);
                }
                else
                    transferData = true;
            }
        }
    }

    /**
     * Sets the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt> to {@link #captureOutput}.
     *
     * @param format the <tt>Format</tt> of the media data made available by
     * this <tt>PushBufferStream</tt> to be set to {@link #captureOutput}
     */
    private void setCaptureOutputFormat(Format format)
    {
        VideoFormat videoFormat = (VideoFormat) format;
        Dimension size = videoFormat.getSize();
        int width;
        int height;

        /*
         * FIXME Mac OS X Leopard does not seem to report the size of the
         * QTCaptureDevice in its formatDescriptions early in its creation.
         * The workaround presented here is to just force a specific size.
         */
        if (size == null)
        {
            width = DataSource.DEFAULT_WIDTH;
            height = DataSource.DEFAULT_HEIGHT;
        }
        else
        {
            width = size.width;
            height = size.height;
        }

        NSMutableDictionary pixelBufferAttributes = null;

        if ((width > 0) && (height > 0))
        {
            if (pixelBufferAttributes == null)
                pixelBufferAttributes = new NSMutableDictionary();
            pixelBufferAttributes
                .setIntForKey(
                    width,
                    CVPixelBufferAttributeKey.kCVPixelBufferWidthKey);
            pixelBufferAttributes
                .setIntForKey(
                    height,
                    CVPixelBufferAttributeKey.kCVPixelBufferHeightKey);
        }

        String encoding;

        if (format instanceof AVFrameFormat)
        {
            int pixFmt = ((AVFrameFormat) format).getPixFmt();

            if (pixFmt == FFmpeg.PIX_FMT_YUV420P)
                encoding = VideoFormat.YUV;
            else if (pixFmt == FFmpeg.PIX_FMT_ARGB)
                encoding = VideoFormat.RGB;
            else
                encoding = null;
        }
        else if (format.isSameEncoding(VideoFormat.RGB))
            encoding = VideoFormat.RGB;
        else if (format.isSameEncoding(VideoFormat.YUV))
            encoding = VideoFormat.YUV;
        else
            encoding = null;

        if (VideoFormat.RGB.equalsIgnoreCase(encoding))
        {
            if (pixelBufferAttributes == null)
                pixelBufferAttributes = new NSMutableDictionary();
            pixelBufferAttributes
                .setIntForKey(
                    CVPixelFormatType.kCVPixelFormatType_32ARGB,
                    CVPixelBufferAttributeKey.kCVPixelBufferPixelFormatTypeKey);
        }
        else if (VideoFormat.YUV.equalsIgnoreCase(encoding))
        {
            if (pixelBufferAttributes == null)
                pixelBufferAttributes = new NSMutableDictionary();
            pixelBufferAttributes
                .setIntForKey(
                    CVPixelFormatType.kCVPixelFormatType_420YpCbCr8Planar,
                    CVPixelBufferAttributeKey.kCVPixelBufferPixelFormatTypeKey);
        }
        else
            throw new IllegalArgumentException("format");

        if (pixelBufferAttributes != null)
        {
            captureOutput.setPixelBufferAttributes(pixelBufferAttributes);
            captureOutputFormat = videoFormat;
        }
    }

    /**
     * Starts the transfer of media data from this <tt>PushBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>PushBufferStream</tt>
     */
    @Override
    public void start()
        throws IOException
    {
        if (!automaticallyDropsLateVideoFrames)
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
    public void stop()
        throws IOException
    {
        transferDataThread = null;

        synchronized (dataSyncRoot)
        {
            if (data != null)
            {
                returnFreeBuffer(data);
                data = null;
            }
            dataFormat = null;
            if (nextData != null)
            {
                returnFreeBuffer(nextData);
                nextData = null;
            }
            nextDataFormat = null;

            if (!automaticallyDropsLateVideoFrames)
                dataSyncRoot.notifyAll();
        }
    }

    /**
     * Represents a buffer of native memory with a specific size/capacity which
     * either contains a specific number of bytes of valid data or is free for
     * consumption.
     */
    private static class ByteBuffer
    {

        /**
         * The maximum number of bytes which can be written into the native
         * memory represented by this instance.
         */
        public final int capacity;

        /**
         * The indicator which determines whether this instance is free to be
         * written bytes into.
         */
        private boolean free;

        /**
         * The number of bytes of valid data that the native memory represented
         * by this instance contains.
         */
        private int length;

        /**
         * The pointer to the native memory represented by this instance.
         */
        public final long ptr;

        /**
         * Initializes a new <tt>ByteBuffer</tt> instance with a specific
         * <tt>capacity</tt>.
         *
         * @param capacity the maximum number of bytes which can be written into
         * the native memory represented by the new instance
         */
        public ByteBuffer(int capacity)
        {
            this.capacity = capacity;
            this.ptr = FFmpeg.av_malloc(this.capacity);

            this.free = true;
            this.length = 0;

            if (this.ptr == 0)
            {
                throw
                    new OutOfMemoryError(
                            getClass().getSimpleName()
                                + " with capacity "
                                + this.capacity);
            }
        }

        /**
         * Gets the number of bytes of valid data that the native memory
         * represented by this instance contains.
         *
         * @return the number of bytes of valid data that the native memory
         * represented by this instance contains
         */
        public int getLength()
        {
            return length;
        }

        /**
         * Determines whether this instance is free to be written bytes into.
         *
         * @return <tt>true</tt> if this instance is free to be written bytes
         * into or <tt>false</tt> is the native memory represented by this
         * instance is already is use
         */
        public boolean isFree()
        {
            return free;
        }

        /**
         * Sets the indicator which determines whether this instance is free to
         * be written bytes into.
         *
         * @param free <tt>true</tt> if this instance is to be made available
         * for writing bytes into; otherwise, <tt>false</tt>
         */
        public void setFree(boolean free)
        {
            this.free = free;
            if (this.free)
                setLength(0);
        }

        /**
         * Sets the number of bytes of valid data that the native memory
         * represented by this instance contains.
         *
         * @param length the number of bytes of valid data that the native
         * memory represented by this instance contains
         */
        public void setLength(int length)
        {
            this.length = length;
        }
    }

    /**
     * Represents an <tt>AVFrame</tt> used by this instance to provide captured
     * media data in native format without representing the very frame data in
     * the Java heap. Since this instance cannot know when the <tt>AVFrame</tt>
     * instances are really safe for deallocation, <tt>FinalizableAVFrame</tt>
     * relies on the Java finialization mechanism to reclaim the represented
     * native memory.
     */
    private class FinalizableAVFrame
        extends AVFrame
    {

        /**
         * The indicator which determines whether the native memory represented
         * by this instance has already been freed/deallocated.
         */
        private boolean freed = false;

        /**
         * Initializes a new <tt>FinalizableAVFrame</tt> instance which is to
         * allocate a new native FFmpeg <tt>AVFrame</tt> and represent it.
         */
        public FinalizableAVFrame()
        {
            super(FFmpeg.avcodec_alloc_frame());
        }

        /**
         * Deallocates the native memory represented by this instance.
         *
         * @see Object#finalize()
         */
        @Override
        protected void finalize()
            throws Throwable
        {
            try
            {
                if (!freed)
                {
                    long ptr = getPtr();
                    long bufferPtr = FFmpeg.avpicture_get_data0(ptr);

                    if (bufferPtr != 0)
                        returnFreeBuffer(bufferPtr);
                    FFmpeg.av_free(ptr);
                    freed = true;
                }
            }
            finally
            {
                super.finalize();
            }
        }
    }
}
