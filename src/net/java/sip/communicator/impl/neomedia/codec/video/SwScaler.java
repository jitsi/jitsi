/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Implements a <tt>Codec</tt> which uses libswscale to scale images and convert
 * between color spaces (typically, RGB and YUV).
 *
 * @author Sebastien Vincent
 * @author Lyubomir Marinov
 */
public class SwScaler
    extends AbstractCodec
{

    /**
     * The <tt>Logger</tt> used by the <tt>SwScaler</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger = Logger.getLogger(SwScaler.class);

    /**
     * The minimum height and/or width of the input and/or output to be passed
     * to <tt>sws_scale</tt> in order to prevent its crashing.
     */
    private static final int MIN_SWS_SCALE_HEIGHT_OR_WIDTH = 4;

    /**
     * The indicator which determines whether this scaler will attempt to keep
     * the width and height of YUV 420 output even.
     */
    private final boolean fixOddYuv420Size;

    /**
     * The <tt>FrameProcessingControl</tt> of this <tt>Codec</tt> which allows
     * JMF to instruct it to drop frames because it's behind schedule.
     */
    private final FrameProcessingControlImpl frameProcessingControl
        = new FrameProcessingControlImpl();

    /**
     * Supported output formats.
     */
    private Format[] supportedOutputFormats = new Format[]
    {
        new YUVFormat(null, -1, Format.byteArray, -1.0f, YUVFormat.YUV_420,
                -1, -1, 0, -1, -1),
        new YUVFormat(null, -1, Format.intArray, -1.0f, YUVFormat.YUV_420,
                -1, -1, 0, -1, -1),
        new YUVFormat(null, -1, Format.shortArray, -1.0f, YUVFormat.YUV_420,
                -1, -1, 0, -1, -1),
        new RGBFormat(null, -1, Format.byteArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(null, -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(null, -1, Format.shortArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(null, -1, Format.byteArray, -1.0f, 24, -1, -1, -1),
        new RGBFormat(null, -1, Format.intArray, -1.0f, 24, -1, -1, -1),
        new RGBFormat(null, -1, Format.shortArray, -1.0f, 24, -1, -1, -1),
    };

    /**
     * Libswscale context pointer.
     */
    private long swsContext = 0;

    /**
     * Initializes a new <tt>SwScaler</tt> instance which doesn't have an output
     * size and will use a default one when it becomes necessary unless an
     * explicit one is specified in the meantime.
     */
    public SwScaler()
    {
        this(false);
    }

    /**
     * Initializes a new <tt>SwScaler</tt> instance which can optionally attempt
     * to keep the width and height of YUV 420 output even.
     *
     * @param fixOddYuv420Size <tt>true</tt> to keep the width and height of YUV
     * 420 output even; otherwise, <tt>false</tt>
     */
    protected SwScaler(boolean fixOddYuv420Size)
    {
        this.fixOddYuv420Size = fixOddYuv420Size;

        inputFormats = new Format[]
        {
            new AVFrameFormat(),
            new RGBFormat(null, -1, Format.byteArray, -1.0f, 32, -1, -1, -1),
            new RGBFormat(null, -1, Format.intArray, -1.0f, 32, -1, -1, -1),
            new RGBFormat(null, -1, Format.shortArray, -1.0f, 32, -1, -1, -1),
            new RGBFormat(null, -1, Format.byteArray, -1.0f, 24, -1, -1, -1),
            new RGBFormat(null, -1, Format.intArray, -1.0f, 24, -1, -1, -1),
            new RGBFormat(null, -1, Format.shortArray, -1.0f, 24, -1, -1, -1),
            new YUVFormat(null, -1, Format.byteArray, -1.0f, YUVFormat.YUV_420,
                    -1, -1, 0, -1, -1),
            new YUVFormat(null, -1, Format.intArray, -1.0f, YUVFormat.YUV_420,
                    -1, -1, 0, -1, -1),
            new YUVFormat(null, -1, Format.shortArray, -1.0f, YUVFormat.YUV_420,
                    -1, -1, 0, -1, -1),
        };

        addControl(frameProcessingControl);
    }

    /**
     * Close codec.
     */
    @Override
    public void close()
    {
        try
        {
            if (swsContext != 0)
            {
                FFmpeg.sws_freeContext(swsContext);
                swsContext = 0;
            }
        }
        finally
        {
            super.close();
        }
    }

    /**
     * Gets the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to accept input media data.
     * <p>
     * Makes the protected super implementation public.
     * </p>
     *
     * @return the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to accept input media data
     * @see AbstractCodec#getInputFormat()
     */
    @Override
    public Format getInputFormat()
    {
        return super.getInputFormat();
    }

    /**
     * Gets native (FFmpeg) RGB format.
     *
     * @param rgb JMF <tt>RGBFormat</tt>
     * @return native RGB format
     */
    public static int getNativeRGBFormat(RGBFormat rgb)
    {
        int fmt;

        if(rgb.getBitsPerPixel() == 32)
            switch(rgb.getRedMask())
            {
            case 1:
            case 0xff:
                fmt = FFmpeg.PIX_FMT_BGR32;
                break;
            case 2:
            case (0xff << 8):
                fmt = FFmpeg.PIX_FMT_BGR32_1;
                break;
            case 3:
            case (0xff << 16):
                fmt = FFmpeg.PIX_FMT_RGB32;
                break;
            case 4:
            case (0xff << 24):
                fmt = FFmpeg.PIX_FMT_RGB32_1;
                break;
            default:
                /* assume ARGB ? */
                fmt = FFmpeg.PIX_FMT_RGB32;
                break;
            }
        else
            fmt = FFmpeg.PIX_FMT_RGB24;

        return fmt;
    }

    /**
     * Gets the output size.
     *
     * @return the output size
     */
    public Dimension getOutputSize()
    {
        Format outputFormat = getOutputFormat();

        if (outputFormat == null)
        {
            // They all have one and the same size.
            outputFormat = supportedOutputFormats[0];
        }
        return ((VideoFormat) outputFormat).getSize();
    }

    /**
     * Gets the supported output formats for an input one.
     *
     * @param input input format to get supported output ones for
     * @return array of supported output formats
     */
    @Override
    public Format[] getSupportedOutputFormats(Format input)
    {
        if(input == null)
            return supportedOutputFormats;

        /* if size is set for element 0 (YUVFormat), it is also set
         * for element 1 (RGBFormat) and so on...
         */
        Dimension size = ((VideoFormat) supportedOutputFormats[0]).getSize();

        /* no specified size set so return the same size as input
         * in output format supported
         */
        VideoFormat videoInput = (VideoFormat) input;

        if(size == null)
            size = videoInput.getSize();

        float frameRate = videoInput.getFrameRate();

        return
            new Format[]
            {
                new YUVFormat(size, -1, Format.byteArray, frameRate,
                        YUVFormat.YUV_420, -1, -1, 0, -1, -1),
                new YUVFormat(size, -1, Format.intArray, frameRate,
                        YUVFormat.YUV_420, -1, -1, 0, -1, -1),
                new YUVFormat(size, -1, Format.shortArray, frameRate,
                        YUVFormat.YUV_420, -1, -1, 0, -1, -1),
                new RGBFormat(size, -1, Format.byteArray, frameRate, 32, -1, -1,
                        -1),
                new RGBFormat(size, -1, Format.intArray, frameRate, 32, -1, -1,
                        -1),
                new RGBFormat(size, -1, Format.shortArray, frameRate, 32, -1,
                        -1, -1),
                new RGBFormat(size, -1, Format.byteArray, frameRate, 24, -1, -1,
                        -1),
                new RGBFormat(size, -1, Format.intArray, frameRate, 24, -1, -1,
                        -1),
                new RGBFormat(size, -1, Format.shortArray, frameRate, 24, -1,
                        -1, -1)
            };
    }

    /**
     * Processes (converts color space and/or scales) a buffer.
     *
     * @param input input buffer
     * @param output output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     */
    @Override
    public int process(Buffer input, Buffer output)
    {
        if (!checkInputBuffer(input))
            return BUFFER_PROCESSED_FAILED;
        if (isEOM(input))
        {
            propagateEOM(output);
            return BUFFER_PROCESSED_OK;
        }
        if (input.isDiscard() || frameProcessingControl.isMinimalProcessing())
        {
            output.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        // Determine the input Format.
        VideoFormat inputFormat = (VideoFormat) input.getFormat();
        Format thisInputFormat = getInputFormat();

        if ((inputFormat != thisInputFormat)
                && !inputFormat.equals(thisInputFormat))
            setInputFormat(inputFormat);

        // Determine the output Format and size.
        VideoFormat outputFormat = (VideoFormat) getOutputFormat();

        if (outputFormat == null)
        {
            /*
             * The format of the output Buffer is not documented to be used as
             * input to the #process method. Anyway, we're trying to use it in
             * case this Codec doesn't have an outputFormat set which is
             * unlikely to ever happen.
             */
            outputFormat = (VideoFormat) output.getFormat();
            if (outputFormat == null)
                return BUFFER_PROCESSED_FAILED;
        }

        Dimension outputSize = outputFormat.getSize();

        if (outputSize == null)
        {
            outputSize = inputFormat.getSize();
            if (outputSize == null)
                return BUFFER_PROCESSED_FAILED;
        }

        int outputWidth = outputSize.width;
        int outputHeight = outputSize.height;

        if ((outputWidth < MIN_SWS_SCALE_HEIGHT_OR_WIDTH)
                || (outputHeight <= MIN_SWS_SCALE_HEIGHT_OR_WIDTH))
            return OUTPUT_BUFFER_NOT_FILLED; // sws_scale will crash

        // Apply the outputSize to the outputFormat of the output Buffer.
        outputFormat = setSize(outputFormat, outputSize);
        if (outputFormat == null)
            return BUFFER_PROCESSED_FAILED;

        int dstFmt;
        int dstLength;

        if (outputFormat instanceof YUVFormat)
        {
            dstFmt = FFmpeg.PIX_FMT_YUV420P;
            /* YUV420P is 12 bits per pixel i.e. 1.5 bytes. */
            dstLength = (int) (outputWidth * outputHeight * 1.5);
        }
        else /* RGB format */
        {
            dstFmt
                = OSUtils.IS_ANDROID
                    ? getNativeRGBFormat((RGBFormat) outputFormat)
                    : FFmpeg.PIX_FMT_RGB32;
            dstLength = (outputWidth * outputHeight * 4);
        }

        Class<?> outputDataType = outputFormat.getDataType();
        Object dst = output.getData();

        if (Format.byteArray.equals(outputDataType))
        {
            if(dst == null || ((byte[])dst).length < dstLength)
                dst = new byte[dstLength];
        }
        else if (Format.intArray.equals(outputDataType))
        {
            /* Java int is always 4 bytes. */
            dstLength = dstLength / 4 + ((dstLength % 4 == 0) ? 0 : 1);
            if(dst == null || ((int[])dst).length < dstLength)
                dst = new int[dstLength];
        }
        else if (Format.shortArray.equals(outputDataType))
        {
            /* Java short is always 2 bytes. */
            dstLength = dstLength / 2 + ((dstLength % 2 == 0) ? 0 : 1);
            if(dst == null || ((short[])dst).length < dstLength)
                dst = new short[dstLength];
        }
        else
        {
            logger.error("Unknown data type " + outputDataType);
            return BUFFER_PROCESSED_FAILED;
        }

        Dimension inputSize = inputFormat.getSize();

        if (inputSize == null)
            return BUFFER_PROCESSED_FAILED;

        int inputWidth = inputSize.width;
        int inputHeight = inputSize.height;

        if ((inputWidth < MIN_SWS_SCALE_HEIGHT_OR_WIDTH)
                || (inputHeight < MIN_SWS_SCALE_HEIGHT_OR_WIDTH))
            return OUTPUT_BUFFER_NOT_FILLED; // sws_scale will crash

        Object src = input.getData();
        int srcFmt;
        long srcPicture;

        if (src instanceof AVFrame)
        {
            srcFmt = ((AVFrameFormat) inputFormat).getPixFmt();
            srcPicture = ((AVFrame) src).getPtr();
        }
        else
        {
            srcFmt
                = (inputFormat instanceof YUVFormat)
                    ? FFmpeg.PIX_FMT_YUV420P
                    : getNativeRGBFormat((RGBFormat) inputFormat);
            srcPicture = 0;
        }

        swsContext
            = FFmpeg.sws_getCachedContext(
                    swsContext,
                    inputWidth, inputHeight, srcFmt,
                    outputWidth, outputHeight, dstFmt,
                    FFmpeg.SWS_BICUBIC);

        if (srcPicture == 0)
        {
            FFmpeg.sws_scale(
                    swsContext,
                    src, srcFmt, inputWidth, inputHeight, 0, inputHeight,
                    dst, dstFmt, outputWidth, outputHeight);
        }
        else
        {
            FFmpeg.sws_scale(
                    swsContext,
                    srcPicture, 0, inputHeight,
                    dst, dstFmt, outputWidth, outputHeight);
        }

        output.setData(dst);
        output.setDuration(input.getDuration());
        output.setFlags(input.getFlags());
        output.setFormat(outputFormat);
        output.setLength(dstLength);
        output.setOffset(0);
        output.setSequenceNumber(input.getSequenceNumber());
        output.setTimeStamp(input.getTimeStamp());

        // flags
        int inFlags = input.getFlags();
        int outFlags = output.getFlags();

        if ((inFlags & Buffer.FLAG_LIVE_DATA) != 0)
            outFlags |= Buffer.FLAG_LIVE_DATA;
        if ((inFlags & Buffer.FLAG_NO_WAIT) != 0)
            outFlags |= Buffer.FLAG_NO_WAIT;
        if ((inFlags & Buffer.FLAG_RELATIVE_TIME) != 0)
            outFlags |= Buffer.FLAG_RELATIVE_TIME;
        if ((inFlags & Buffer.FLAG_RTP_TIME) != 0)
            outFlags |= Buffer.FLAG_RTP_TIME;
        if ((inFlags & Buffer.FLAG_SYSTEM_TIME) != 0)
            outFlags |= Buffer.FLAG_SYSTEM_TIME;
        output.setFlags(outFlags);

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Sets the input format.
     *
     * @param format format to set
     * @return format
     */
    @Override
    public Format setInputFormat(Format format)
    {
        Format inputFormat
            = (format instanceof VideoFormat)
                ? super.setInputFormat(format)
                : null /* A size is not required, the input must be video. */;

        if (inputFormat != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("SwScaler set to input in " + inputFormat);
        }
        return inputFormat;
    }

    /**
     * Sets the <tt>Format</tt> in which this <tt>Codec</tt> is to output media
     * data.
     *
     * @param format the <tt>Format</tt> in which this <tt>Codec</tt> is to
     * output media data
     * @return the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to output media data or <tt>null</tt> if <tt>format</tt> was
     * found to be incompatible with this <tt>Codec</tt>
     */
    @Override
    public Format setOutputFormat(Format format)
    {
        if (fixOddYuv420Size && (format instanceof YUVFormat))
        {
            YUVFormat yuvFormat = (YUVFormat) format;

            if (YUVFormat.YUV_420 == yuvFormat.getYuvType())
            {
                Dimension size = yuvFormat.getSize();

                if ((size != null) && (size.width > 2) && (size.height > 2))
                {
                    int width = (size.width >> 1) << 1;
                    int height = (size.height >> 1) << 1;

                    if ((width != size.width) || (height != size.height))
                    {
                        format
                            = new YUVFormat(
                                    new Dimension(width, height),
                                    Format.NOT_SPECIFIED,
                                    yuvFormat.getDataType(),
                                    yuvFormat.getFrameRate(),
                                    yuvFormat.getYuvType(),
                                    Format.NOT_SPECIFIED,
                                    Format.NOT_SPECIFIED,
                                    0,
                                    Format.NOT_SPECIFIED,
                                    Format.NOT_SPECIFIED);
                    }
                }
            }
        }

        Format outputFormat = super.setOutputFormat(format);

        if (logger.isDebugEnabled() && (outputFormat != null))
            logger.debug("SwScaler set to output in " + outputFormat);
        return outputFormat;
    }

    /**
     * Sets the size i.e. width and height of the current <tt>outputFormat</tt>
     * of this <tt>SwScaler</tt>
     * 
     * @param size the size i.e. width and height to be set on the current
     * <tt>outputFormat</tt> of this <tt>SwScaler</tt>
     */
    private void setOutputFormatSize(Dimension size)
    {
        VideoFormat outputFormat = (VideoFormat) getOutputFormat();

        if (outputFormat != null)
        {
            outputFormat = setSize(outputFormat, size);
            if (outputFormat != null)
                setOutputFormat(outputFormat);
        }
    }

    /**
     * Sets the output size.
     *
     * @param size the size to set as the output size
     */
    public void setOutputSize(Dimension size)
    {
        supportedOutputFormats[0]
            = new YUVFormat(size, -1, Format.byteArray, -1.0f,
                    YUVFormat.YUV_420, -1, -1, 0, -1, -1);
        supportedOutputFormats[1]
            = new YUVFormat(size, -1, Format.intArray, -1.0f, YUVFormat.YUV_420,
                    -1, -1, 0, -1, -1);
        supportedOutputFormats[2]
            = new YUVFormat(size, -1, Format.shortArray, -1.0f,
                    YUVFormat.YUV_420, -1, -1, 0, -1, -1);
        supportedOutputFormats[3]
            = new RGBFormat(size, -1, Format.byteArray, -1.0f, 32, -1, -1, -1);
        supportedOutputFormats[4]
            = new RGBFormat(size, -1, Format.intArray, -1.0f, 32, -1, -1, -1);
        supportedOutputFormats[5]
            = new RGBFormat(size, -1, Format.shortArray, -1.0f, 32, -1, -1, -1);
        supportedOutputFormats[6]
            = new RGBFormat(size, -1, Format.byteArray, -1.0f, 24, -1, -1, -1);
        supportedOutputFormats[7]
            = new RGBFormat(size, -1, Format.intArray, -1.0f, 24, -1, -1, -1);
        supportedOutputFormats[8]
            = new RGBFormat(size, -1, Format.shortArray, -1.0f, 24, -1, -1, -1);

        // Set the size to the outputFormat as well.
        setOutputFormatSize(size);
    }

    /**
     * Gets a <tt>VideoFormat</tt> with a specific size i.e. width and height
     * using a specific <tt>VideoFormat</tt> as a template.
     *
     * @param format the <tt>VideoFormat</tt> which is the template for the
     * <tt>VideoFormat</tt> to be returned
     * @param size the size i.e. width and height of the <tt>VideoFormat</tt> to
     * be returned
     * @return a <tt>VideoFormat</tt> with the specified <tt>size</tt> and based
     * on the specified <tt>format</tt>
     */
    private static VideoFormat setSize(VideoFormat format, Dimension size)
    {
        /*
         * Since the size of the Format has changed, its size-related properties
         * should change as well. Format#intersects doesn't seem to be cool
         * because it preserves them and thus the resulting Format is
         * inconsistent.
         */
        if (format instanceof RGBFormat)
        {
            RGBFormat rgbFormat = (RGBFormat) format;
            Class<?> dataType = format.getDataType();
            int bitsPerPixel = rgbFormat.getBitsPerPixel();
            int pixelStride = rgbFormat.getPixelStride();

            if ((pixelStride == Format.NOT_SPECIFIED)
                    && (dataType != null)
                    && (bitsPerPixel != Format.NOT_SPECIFIED))
                pixelStride
                    = dataType.equals(Format.byteArray)
                        ? (bitsPerPixel / 8)
                        : 1;
            format
                = new RGBFormat(
                        size,
                        Format.NOT_SPECIFIED,
                        dataType,
                        format.getFrameRate(),
                        bitsPerPixel,
                        rgbFormat.getRedMask(),
                        rgbFormat.getGreenMask(),
                        rgbFormat.getBlueMask(),
                        pixelStride,
                        ((pixelStride == Format.NOT_SPECIFIED)
                                || (size == null))
                            ? Format.NOT_SPECIFIED
                            : (pixelStride * size.width) /* lineStride */,
                        rgbFormat.getFlipped(),
                        rgbFormat.getEndian());
        }
        else if (format instanceof YUVFormat)
        {
            YUVFormat yuvFormat = (YUVFormat) format;

            format
                = new YUVFormat(
                        size,
                        Format.NOT_SPECIFIED,
                        format.getDataType(),
                        format.getFrameRate(),
                        yuvFormat.getYuvType(),
                        Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED,
                        0,
                        Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED);
        }
        else if (format != null)
        {
            logger.warn(
                    "SwScaler outputFormat of type "
                        + format.getClass().getName()
                        + " is not supported for optimized scaling.");
        }
        return format;
    }
}
