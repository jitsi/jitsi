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

import net.sf.fmj.media.*;

/**
 * Codec that use libswscale to scale images from one size to 
 * another and change format (typically RGB to YUV).
 *
 * @author Sebastien Vincent
 */
public class SwScaler
    extends AbstractCodec
    implements Codec
{
    
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
     * Set output size.
     *
     * @param size size to set
     */
    public void setOutputSize(Dimension size)
    {
        if(size == null)
        {
            size = new Dimension(640, 480);
        }

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
    }

    /**
     * Get the supported input formats.
     *
     * @return array of supported input format
     */
    @Override
    public Format[] getSupportedInputFormats()
    {
        return supportedInputFormats;
    }

    /**
     * Get the supported output formats for an input ones.
     *
     * @param input input format to convert
     * @return array of supported output format
     */
    @Override
    public Format[] getSupportedOutputFormats(Format input)
    {
        Dimension size = null;

        if(input == null)
        {
            return supportedOutputFormats;
        }

        /* if size is set for element 0 (YUVFormat), it is also set 
         * for element 1 (RGBFormat) and so on...
         */
        size = ((VideoFormat)supportedOutputFormats[0]).getSize();
    
        //System.out.println("input: " + ((VideoFormat)input).getSize());
        if(size != null)
        {
            return supportedOutputFormats;
        }

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
     * Set the input format.
     *
     * @param format format to set
     * @return format
     */
    @Override
    public Format setInputFormat(Format format)
    {
        //System.out.println("setInput: " + ((VideoFormat)format).getSize());
        final VideoFormat videoFormat = (VideoFormat) format;

        if (videoFormat.getSize() == null)
            return null;    // must set a size.

        format = super.setInputFormat(format);
        return format;
    }

    /**
     * Set the output format.
     *
     * @param format format to set
     * @return format
     */
    @Override
    public Format setOutputFormat(Format format)
    {
        //System.out.println("setOutput: " + ((VideoFormat)format).getSize());
        format = super.setOutputFormat(format);
        return format;
    }

    /**
     * Process (format conversion, rescale) a buffer.
     *
     * @param input input buffer
     * @param output output buffer
     * @return BUFFER_PROCESSED_OK if buffer successfully processed
     */
    @Override
    public int process(Buffer input, Buffer output) 
    {
        VideoFormat vinput = (VideoFormat)input.getFormat();
        VideoFormat voutput = (VideoFormat)output.getFormat();
        int inputWidth = (int)vinput.getSize().getWidth();
        int inputHeight = (int)vinput.getSize().getHeight();
        /* input's data type can be byte[], int[] or short[]
         * so we used Object type to store it
         */
        Object src = input.getData();
        Object dst = output.getData();
        int outputSize = 0;
        int outputWidth = 0;
        int outputHeight = 0;
        int infmt = 0;
        int outfmt = 0;

        /* first buffer has no output format set */
        if(voutput == null)
        {
            voutput = (VideoFormat)outputFormat;
            return BUFFER_PROCESSED_FAILED;
        }

        outputWidth = (int)voutput.getSize().getWidth();
        outputHeight = (int)voutput.getSize().getHeight();

        if (!checkInputBuffer(input))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(input))
        {
            propagateEOM(output);   // TODO: what about data? can there be any?
            return BUFFER_PROCESSED_OK;
        }
        
        /* determine output format and output size needed */
        if(voutput instanceof YUVFormat)
        {
            /* YUV420P is 12 bpp (bit per pixel) => 1,5 bytes */
            outputSize = (int)(outputWidth * outputHeight * 1.5);
            outfmt = FFMPEG.PIX_FMT_YUV420P;
        }
        else /* RGB format */
        {
            outputSize = (outputWidth * outputHeight * 4);
            if(((RGBFormat)voutput).getBitsPerPixel() == 32)
            {
                outfmt = FFMPEG.PIX_FMT_RGBA;
            }
            else
            {
                outfmt = FFMPEG.PIX_FMT_RGB24;
           }
        }
        
        /* determine input format */
        if(vinput instanceof YUVFormat)
        {
            infmt = FFMPEG.PIX_FMT_YUV420P;
        }
        else /* RGBFormat */
        {
            if(((RGBFormat)vinput).getBitsPerPixel() == 32)
            {
                infmt = FFMPEG.PIX_FMT_RGBA;
            }
            else
            {
                infmt = FFMPEG.PIX_FMT_RGB24;
            }
        }

        if(voutput.getDataType() == Format.byteArray)
        {
            if(dst == null || ((byte[])dst).length < outputSize)
            {
                dst = new byte[outputSize];
            }
        }
        else if(voutput.getDataType() == Format.intArray)
        {
            /* Java int is always 4 bytes */
            outputSize = (outputSize % 4) + outputSize / 4;
            if(dst == null || ((int[])dst).length < outputSize)
            {
                dst = new int[outputSize];
            }
        }
        else if(voutput.getDataType() == Format.shortArray)
        {
            /* Java short is always 2 bytes */
            outputSize = (outputSize % 2) + outputSize / 2;
            if(dst == null || ((short[])dst).length < outputSize)
            {
                dst = new short[outputSize];
            }
        }
        else
        {
            System.out.println("Unknown data type!");
            return BUFFER_PROCESSED_FAILED;
        }

        synchronized(src)
        {
            /* conversion! */
            FFMPEG.img_convert(dst, outfmt, src, infmt, inputWidth, inputHeight,
                    outputWidth, outputHeight);
        }

        //System.out.println("Converted: " + inputWidth + "x" + inputHeight + 
        //" to " + outputWidth + "x" + outputHeight);

        output.setData(dst);
        output.setLength(outputSize);
        output.setOffset(0);

        return BUFFER_PROCESSED_OK;   
    }
}

