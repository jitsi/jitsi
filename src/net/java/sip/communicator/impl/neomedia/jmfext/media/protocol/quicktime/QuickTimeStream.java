/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.quicktime;

import java.awt.Dimension; // disambiguation
import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.impl.neomedia.quicktime.*;

/**
 * Implements a <tt>PushBufferStream</tt> using QuickTime/QTKit.
 *
 * @author Lubomir Marinov
 */
public class QuickTimeStream
    extends AbstractPushBufferStream
{

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
     * through the <tt>Buffer</tt>s specified in its {@link #read(Buffer)}.
     */
    private final ByteBufferPool byteBufferPool = new ByteBufferPool();

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
                    byteBufferPool.returnFreeBuffer(nextData);
                    nextData = null;
                }

                nextData
                    = byteBufferPool.getFreeBuffer(pixelBuffer.getByteCount());
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
                byteBufferPool.returnFreeBuffer(data);
                data = null;
            }

            data = byteBufferPool.getFreeBuffer(pixelBuffer.getByteCount());
            if (data != null)
            {
                data.setLength(pixelBuffer.getBytes(data.ptr, data.capacity));
                dataTimeStamp = System.nanoTime();
                if (dataFormat == null)
                    dataFormat = getVideoFrameFormat(pixelBuffer);
            }
            if (nextData != null)
            {
                byteBufferPool.returnFreeBuffer(nextData);
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

        byteBufferPool.close();
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
                FinalizableAVFrame.read(
                        buffer,
                        bufferFormat,
                        data,
                        byteBufferPool);
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

                byteBufferPool.returnFreeBuffer(data);
            }

            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
            buffer.setTimeStamp(dataTimeStamp);

            data = null;
            if (!automaticallyDropsLateVideoFrames)
                dataSyncRoot.notifyAll();
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
                        byteBufferPool.returnFreeBuffer(data);
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
                byteBufferPool.returnFreeBuffer(data);
                data = null;
            }
            dataFormat = null;
            if (nextData != null)
            {
                byteBufferPool.returnFreeBuffer(nextData);
                nextData = null;
            }
            nextDataFormat = null;

            if (!automaticallyDropsLateVideoFrames)
                dataSyncRoot.notifyAll();
        }
    }
}
