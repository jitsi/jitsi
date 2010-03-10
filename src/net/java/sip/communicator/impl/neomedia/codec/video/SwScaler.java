/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Implements a <tt>Codec</tt> which uses libswscale to scale images and convert
 * between color spaces (typically, RGB and YUV).
 *
 * @author Sebastien Vincent
 * @author Lubomir Marinov
 */
public class SwScaler
    extends AbstractCodec
{
    private static final Logger logger = Logger.getLogger(SwScaler.class);
    
    /**
     * Supported input formats.
     */
    private final Format[] supportedInputFormats = new Format[] {
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

    /**
     * Supported output formats.
     */
    private Format[] supportedOutputFormats = new Format[] {
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
        Format outputFormat = super.setOutputFormat(format);

        if (logger.isDebugEnabled() && (outputFormat != null))
                logger.debug(
                        "SwScaler set to output with size "
                            + ((VideoFormat) outputFormat).getSize());
        return outputFormat;
    }

    /**
     * Sets output size.
     *
     * @param size size to set
     */
    public void setOutputSize(Dimension size)
    {
        if(size == null)
            size = new Dimension(640, 480);

        supportedOutputFormats[0] = new YUVFormat(size, -1, Format.byteArray,
                -1.0f, YUVFormat.YUV_420, -1, -1, 0, -1, -1);
        supportedOutputFormats[1] = new YUVFormat(size, -1, Format.intArray,
                -1.0f, YUVFormat.YUV_420, -1, -1, 0, -1, -1);
        supportedOutputFormats[2] = new YUVFormat(size, -1, Format.shortArray,
                -1.0f, YUVFormat.YUV_420, -1, -1, 0, -1, -1);
        supportedOutputFormats[3] = new RGBFormat(size, -1, Format.byteArray,
                -1.0f, 32, -1, -1, -1);
        supportedOutputFormats[4] = new RGBFormat(size, -1, Format.intArray,
                -1.0f, 32, -1, -1, -1);
        supportedOutputFormats[5] = new RGBFormat(size, -1, Format.shortArray,
                -1.0f, 32, -1, -1, -1);
        supportedOutputFormats[6] = new RGBFormat(size, -1, Format.byteArray,
                -1.0f, 24, -1, -1, -1);
        supportedOutputFormats[7] = new RGBFormat(size, -1, Format.intArray,
                -1.0f, 24, -1, -1, -1);
        supportedOutputFormats[8] = new RGBFormat(size, -1, Format.shortArray,
                -1.0f, 24, -1, -1, -1);

        // Set the size to the outputFormat as well.
        VideoFormat outputFormat = (VideoFormat) this.outputFormat;

        if (outputFormat != null)
            setOutputFormat(
                new VideoFormat(
                        outputFormat.getEncoding(),
                        size,
                        outputFormat.getMaxDataLength(),
                        outputFormat.getDataType(),
                        outputFormat.getFrameRate())
                    .intersects(outputFormat));
    }

    /**
     * Gets the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to accept input media data.
     * <p>
     * Make the protected super implementation public.
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
     * Gets the supported input formats.
     *
     * @return array of supported input format
     */
    @Override
    public Format[] getSupportedInputFormats()
    {
        return supportedInputFormats;
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
        Dimension size = ((VideoFormat)supportedOutputFormats[0]).getSize();

        if(size != null)
            return supportedOutputFormats;

        /* no specified size set so return the same size as input
         * in output format supported
         */
        size = ((VideoFormat)input).getSize();

        return new Format[] { 
                              new YUVFormat(size, -1, Format.byteArray, -1.0f,
                                      YUVFormat.YUV_420, -1, -1, 0, -1, -1),
                              new YUVFormat(size, -1, Format.intArray, -1.0f,
                                      YUVFormat.YUV_420, -1, -1, 0, -1, -1),
                              new YUVFormat(size, -1, Format.shortArray, -1.0f,
                                      YUVFormat.YUV_420, -1, -1, 0, -1, -1),
                              new RGBFormat(size, -1, Format.byteArray, -1.0f,
                                      32, -1, -1, -1),
                              new RGBFormat(size, -1, Format.intArray, -1.0f,
                                      32, -1, -1, -1),
                              new RGBFormat(size, -1, Format.shortArray, -1.0f,
                                      32, -1, -1, -1),
                              new RGBFormat(size, -1, Format.byteArray, -1.0f,
                                      24, -1, -1, -1),
                              new RGBFormat(size, -1, Format.intArray, -1.0f,
                                      24, -1, -1, -1),
                              new RGBFormat(size, -1, Format.shortArray, -1.0f,
                                      24, -1, -1, -1),
                            };
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
        VideoFormat videoFormat = (VideoFormat) format;

        if (videoFormat.getSize() == null)
            return null;    // must set a size.

        return super.setInputFormat(format);
    }

    /**
     * Gets native (FFMPEG) RGB format.
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
                fmt = FFMPEG.PIX_FMT_BGR32;
                break;
            case 2:
            case (0xff << 8):
                fmt = FFMPEG.PIX_FMT_BGR32_1;
                break;
            case 3:
            case (0xff << 16):
                fmt = FFMPEG.PIX_FMT_RGB32;
                break;
            case 4:
            case (0xff << 24):
                fmt = FFMPEG.PIX_FMT_RGB32_1;
                break;
            default:
                /* assume ARGB ? */
                fmt = FFMPEG.PIX_FMT_RGB32;
                break;
            }
        else
            fmt = FFMPEG.PIX_FMT_RGB24;
        
        return fmt;
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
            propagateEOM(output);   // TODO: what about data? can there be any?
            return BUFFER_PROCESSED_OK;
        }

        VideoFormat outputFormat = (VideoFormat)output.getFormat();

        if(outputFormat == null)
        {
            outputFormat = (VideoFormat)this.outputFormat;
            if (outputFormat == null) // first buffer has no output format set
                return BUFFER_PROCESSED_FAILED;
        }

        int dstFmt;
        int dstLength;
        Dimension outputSize = outputFormat.getSize();
        int outputWidth = outputSize.width;
        int outputHeight = outputSize.height;

        /* determine output format and output size needed */
        if(outputFormat instanceof YUVFormat)
        {
            dstFmt = FFMPEG.PIX_FMT_YUV420P;
            /* YUV420P is 12 bpp (bit per pixel) => 1,5 bytes */
            dstLength = (int)(outputWidth * outputHeight * 1.5);
        }
        else /* RGB format */
        {
            dstFmt = FFMPEG.PIX_FMT_RGB32;
            dstLength = (outputWidth * outputHeight * 4);
        }
        
        /* determine input format */
        VideoFormat inputFormat = (VideoFormat)input.getFormat();
        int srcFmt;

        if(inputFormat instanceof YUVFormat)
            srcFmt = FFMPEG.PIX_FMT_YUV420P;
        else // RGBFormat
            srcFmt = getNativeRGBFormat((RGBFormat)inputFormat);

        Class<?> outputDataType = outputFormat.getDataType();
        Object dst = output.getData();

        if(Format.byteArray.equals(outputDataType))
        {
            if(dst == null || ((byte[])dst).length < dstLength)
                dst = new byte[dstLength];
        }
        else if(Format.intArray.equals(outputDataType))
        {
            /* Java int is always 4 bytes */
            dstLength = (dstLength % 4) + dstLength / 4;
            if(dst == null || ((int[])dst).length < dstLength)
                dst = new int[dstLength];
        }
        else if(Format.shortArray.equals(outputDataType))
        {
            /* Java short is always 2 bytes */
            dstLength = (dstLength % 2) + dstLength / 2;
            if(dst == null || ((short[])dst).length < dstLength)
                dst = new short[dstLength];
        }
        else
        {
            logger.error("Unknown data type " + outputDataType);
            return BUFFER_PROCESSED_FAILED;
        }

        Object src = input.getData();

        synchronized(src)
        {
            /* conversion! */
            Dimension inputSize = inputFormat.getSize();

            FFMPEG.img_convert(
                    dst, dstFmt,
                    src, srcFmt,
                    inputSize.width, inputSize.height,
                    outputWidth, outputHeight);
        }

        output.setData(dst);
        output.setLength(dstLength);
        output.setOffset(0);

        return BUFFER_PROCESSED_OK;   
    }
}
