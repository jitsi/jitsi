package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;

import net.sf.fmj.media.AbstractCodec;
import net.sf.fmj.media.util.BufferToImage;
import net.sf.fmj.media.util.ImageToBuffer;

/**
 * Codec that scales images from one size to another.
 * Interestingly, cross-platform JMF does not appear to have a corresponding codec.
 * @author Ken Larson
 * 
 * Original from fmj project, changed only output format sizes.
 * The sizes are those supported in h263 and h264.
 * @author Damian Minkov
 */
public class ImageScaler extends AbstractCodec implements Codec
{
    // TODO: all formats supported by BufferToImage
    private final Format[] supportedInputFormats = new Format[] {
            new RGBFormat(null, -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        };
    private final Format[] supportedOutputFormats = new Format[] {
      //P720
        new RGBFormat(new Dimension(720, 480), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        //CIF4
        new RGBFormat(new Dimension(704, 576), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        //CIF
        new RGBFormat(new Dimension(352, 288), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(new Dimension(320, 240), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        //QCIF
        new RGBFormat(new Dimension(176, 144), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        //SQCIF
        new RGBFormat(new Dimension(128, 96), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        };
    
    private boolean passthrough;

    @Override
    public Format[] getSupportedInputFormats()
    {
        return supportedInputFormats;
    }

    @Override
    public Format[] getSupportedOutputFormats(Format input)
    {
//        if (input == null)
            return supportedOutputFormats;
//        VideoFormat inputCast = (VideoFormat) input;
//        final Format[] result = new Format[] {
//                new RGBFormat(DIMENSION, -1, Format.intArray, -1.0f, 32, -1, -1, -1)};
//        // TODO: we have to specify the RGB, etc. in the output format.
//        return result;
        
    }

    private BufferToImage bufferToImage;
    
    @Override
    public Format setInputFormat(Format format)
    {
        final VideoFormat videoFormat = (VideoFormat) format;
        if (videoFormat.getSize() == null)
            return null;    // must set a size.
        
        // TODO: check VideoFormat and compatibility
        bufferToImage = new BufferToImage(videoFormat);

        format = super.setInputFormat(format);
        updatePassthrough();
        return format;
    }

    @Override
    public Format setOutputFormat(Format format)
    {
        format = super.setOutputFormat(format);
        updatePassthrough();
        return format;
    }

    private void updatePassthrough()
    {
        Format outputFormat = getOutputFormat();
        if (outputFormat != null)
        {
            Dimension outputSize = ((VideoFormat) outputFormat).getSize();
            if (outputSize != null)
            {
                Format inputFormat = getInputFormat();
                passthrough =
                    (inputFormat != null)
                        && outputSize.equals(((VideoFormat) inputFormat)
                            .getSize());
                return;
            }
        }
        passthrough = false;
    }

    @Override
    public int process(Buffer input, Buffer output) 
    {
        if (!checkInputBuffer(input))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(input))
        {
            propagateEOM(output);   // TODO: what about data? can there be any?
            return BUFFER_PROCESSED_OK;
        }
        
        // sometimes format sizes are the same but some other field is different
        // and jmf use the scaler (in my case length field was not sent in 
        // one of the formats) the check for sizes is made in method
        // setInputFormat
        if(passthrough)
        {
            output.setData(input.getData());
            output.setLength(input.getLength());
            output.setOffset(input.getOffset());
            return BUFFER_PROCESSED_OK;
        }
        
        final BufferedImage image = (BufferedImage) bufferToImage.createImage(input);
        
        final Dimension inputSize = ((VideoFormat) inputFormat).getSize();
        final Dimension outputSize = ((VideoFormat) outputFormat).getSize();
        final double scaleX = ((double) outputSize.width) / ((double) inputSize.width);
        final double scaleY = ((double) outputSize.height) / ((double) inputSize.height);
        
        final BufferedImage scaled = scale(image, scaleX, scaleY);  // TODO: is the size exact?  what about rounding errors?
        
//        System.out.println("scaled: " + scaled.getWidth() + "x" + scaled.getHeight());
        final Buffer b = ImageToBuffer.createBuffer(scaled, ((VideoFormat) outputFormat).getFrameRate());
        output.setData(b.getData());
        output.setLength(b.getLength());
        output.setOffset(b.getOffset());
        output.setFormat(b.getFormat());
        // TODO: what about format?
    
        return BUFFER_PROCESSED_OK;

        
    }
    private BufferedImage scale(BufferedImage bi, double scaleX, double scaleY)
    {
        AffineTransform tx = new AffineTransform();
        tx.scale(scaleX, scaleY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        return op.filter(bi, null);
    }
}
