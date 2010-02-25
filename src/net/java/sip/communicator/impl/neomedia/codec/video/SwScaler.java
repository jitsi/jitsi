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
        new RGBFormat(null, -1, Format.byteArray, -1.0f, 32, -1, -1, -1)
    };

    /**
     * Supported output formats.
     */
    private Format[] supportedOutputFormats = new Format[] {
        new YUVFormat(null, -1, Format.byteArray, -1.0f, YUVFormat.YUV_420, -1, -1, 0, -1, -1)
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

        Format newFormat = new YUVFormat(size, -1, Format.byteArray, -1.0f, YUVFormat.YUV_420, -1, -1, 0, -1, -1);
        supportedOutputFormats[0] = newFormat;
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
        if(input == null)
        {
            return supportedOutputFormats;
        }
    
        //System.out.println("input: " + ((VideoFormat)input).getSize());
        if(((VideoFormat)supportedOutputFormats[0]).getSize() != null)
        {
            return supportedOutputFormats;
        }

        return new Format[] { new YUVFormat(((VideoFormat)input).getSize(), -1, Format.byteArray, -1.0f, YUVFormat.YUV_420, -1, -1, 0, -1, -1)};
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
        int outputWidth = (int)voutput.getSize().getWidth();
        int outputHeight = (int)voutput.getSize().getHeight();
        byte src[] = (byte[])input.getData();
        byte dst[] = (byte[])output.getData();
        int outputSize = 0;
        int infmt = 0;
        int outfmt = 0;

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
            outfmt = FFMPEG.PIX_FMT_RGBA;
        }
        
        /* determine input format */
        if(vinput instanceof YUVFormat)
        {
            infmt = FFMPEG.PIX_FMT_YUV420P;
        }
        else /* RGBFormat */
        {
            infmt = FFMPEG.PIX_FMT_RGBA;
        }

        if(dst == null || dst.length < outputSize)
        {
            dst = new byte[outputSize];
        }

        /* conversion! */
        //System.out.println("Convert: " + inputWidth + "x" + inputHeight + " to " + outputWidth + "x" + outputHeight);
        FFMPEG.img_convert(dst, outfmt, src, infmt, inputWidth, inputHeight, outputWidth, outputHeight);

        output.setData(dst);
        output.setLength(dst.length);
        output.setOffset(0);

        return BUFFER_PROCESSED_OK;   
    }
}

