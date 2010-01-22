/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.quicktime;

import java.awt.*;
import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

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
     * The <tt>QTCaptureOutput</tt> represented by this <tt>SourceStream</tt>.
     */
    final QTCaptureDecompressedVideoOutput captureOutput
        = new QTCaptureDecompressedVideoOutput();

    /**
     * The captured media data to be returned in {@link #read(Buffer)}.
     */
    private byte[] data;

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

        captureOutput.setAutomaticallyDropsLateVideoFrames(true);
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
            data = pixelBuffer.getBytes();
            dataTimeStamp = System.nanoTime();
            transferData = (data != null);

            if (dataFormat == null)
                dataFormat = getVideoFrameFormat(pixelBuffer);
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
                                        new Dimension(640, 480),
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
                return
                    new RGBFormat(
                            ((width == 0) && (height == 0)
                                ? null
                                : new Dimension(width, height)),
                            Format.NOT_SPECIFIED,
                            Format.byteArray,
                            Format.NOT_SPECIFIED,
                            32,
                            2,
                            3,
                            4);
            case CVPixelFormatType.kCVPixelFormatType_420YpCbCr8Planar:
                if ((width == 0) && (height == 0))
                    return new YUVFormat(YUVFormat.YUV_420);
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
                                strideY,
                                strideUV,
                                offsetY,
                                offsetU,
                                offsetV);
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
                buffer.setLength(0);
            else
            {
                buffer.setData(data);
                buffer
                    .setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_SYSTEM_TIME);
                if (dataFormat != null)
                    buffer.setFormat(dataFormat);
                buffer.setLength(data.length);
                buffer.setOffset(0);
                buffer.setTimeStamp(dataTimeStamp);

                data = null;
            }
        }
    }

    /**
     * Set the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt> to {@link #captureOutput}.
     *
     * @param format the <tt>Format</tt> of the media data made available by
     * this <tt>PushBufferStream</tt> to be set to {@link #captureOutput}
     */
    private void setCaptureOutputFormat(Format format)
    {
        VideoFormat videoFormat = (VideoFormat) format;
        Dimension size = videoFormat.getSize();

        /*
         * FIXME Mac OS X Leopard does not seem to report the size of the
         * QTCaptureDevice in its formatDescriptions early in its creation.
         * The workaround presented here is to just force a specific size.
         */
        if (size == null)
            size
                = new Dimension(
                        DataSource.DEFAULT_WIDTH,
                        DataSource.DEFAULT_HEIGHT);

        NSMutableDictionary pixelBufferAttributes = null;

        if (size != null)
        {
            if (pixelBufferAttributes == null)
                pixelBufferAttributes = new NSMutableDictionary();
            pixelBufferAttributes
                .setIntForKey(
                    size.width,
                    CVPixelBufferAttributeKey.kCVPixelBufferWidthKey);
            pixelBufferAttributes
                .setIntForKey(
                    size.height,
                    CVPixelBufferAttributeKey.kCVPixelBufferHeightKey);
        }

        if (format.isSameEncoding(VideoFormat.RGB))
        {
            if (pixelBufferAttributes == null)
                pixelBufferAttributes = new NSMutableDictionary();
            pixelBufferAttributes
                .setIntForKey(
                    CVPixelFormatType.kCVPixelFormatType_32ARGB,
                    CVPixelBufferAttributeKey.kCVPixelBufferPixelFormatTypeKey);
        }
        else if (format.isSameEncoding(VideoFormat.YUV))
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
            captureOutput.setPixelBufferAttributes(pixelBufferAttributes);
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
        synchronized (dataSyncRoot)
        {
            data = null;
            dataFormat = null;
        }
    }
}
