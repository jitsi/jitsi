/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.*;
import java.awt.image.*;

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
 * @author Sebastien Vincent
 */
public class ImageScaler
    extends AbstractCodec
    implements Codec
{
    // TODO: all formats supported by BufferToImage
    private final Format[] supportedInputFormats = new Format[] {
            new RGBFormat(null, -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        };
    private final Format[] supportedOutputFormats = new Format[] {
        //new RGBFormat(new Dimension(1280, 1024), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        //new RGBFormat(new Dimension(1280, 800), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(new Dimension(1024, 768), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(new Dimension(800, 600), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(new Dimension(640, 480), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
        new RGBFormat(new Dimension(720, 576), -1, Format.intArray, -1.0f, 32, -1, -1, -1),
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

    private BufferToImage bufferToImage;

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
                        && outputSize.equals(
                                ((VideoFormat) inputFormat).getSize());
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
/*
        final Dimension inputSize = ((VideoFormat) inputFormat).getSize();
        final Dimension outputSize = ((VideoFormat) outputFormat).getSize();
        final double scaleX = ((double) outputSize.width) / ((double) inputSize.width);
        final double scaleY = ((double) outputSize.height) / ((double) inputSize.height);

        final BufferedImage scaled = scale(image, scaleX, scaleY);  // TODO: is the size exact?  what about rounding errors?
*/

        final Dimension outputSize = ((VideoFormat) outputFormat).getSize();
        /* rescale by preserving ratio */
        final BufferedImage scaled = scalePreserveRatio(image, outputSize.width, outputSize.height);  // TODO: is the size exact?  what about rounding errors?

        final Buffer b = ImageToBuffer.createBuffer(scaled, ((VideoFormat) outputFormat).getFrameRate());
        output.setData(b.getData());
        output.setLength(b.getLength());
        output.setOffset(b.getOffset());
        output.setFormat(b.getFormat());
        // TODO: what about format?

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Get a scaled <tt>BufferedImage</tt> that preserves ratio and puts black
     * borders if the ratio of the source image is different than the ratio of
     * the scaled image.
     * <p>
     * Mainly inspired by
     * http://java.developpez.com/faq/gui/?page=graphique_general_images
     * #GRAPHIQUE_IMAGE_redimensionner
     * </p>
     *
     * @param src the <tt>BufferedImage<tt> to scale
     * @param width width of the scaled image
     * @param height height of scaled image
     * @return the <tt>BufferedImage</tt> which represents <tt>src</tt> scaled
     * to <tt>width</tt> and <tt>height</tt>
     */
    public static BufferedImage scalePreserveRatio(BufferedImage src,
                                                   int width,
                                                   int height)
    {
        BufferedImage buf = new BufferedImage(width, height, src.getType());
        double ratioSrc = (double)src.getWidth() / (double)src.getHeight();
        double ratioDst = width / height;
        int startWidth = 0;
        int startHeight = 0;

        if(Double.compare(ratioSrc, ratioDst) != 0)
        {
            /* adjust ratio */
            int newWidth = 0;
            int newHeight = ((src.getHeight() * width) / src.getWidth());

            /* first try to adjust height */
            startHeight = (height - newHeight) / 2;

            if(startHeight < 0)
            {
                /* rescale width */
                newWidth = ((src.getWidth() * height) / src.getHeight());
                startWidth = (width - newWidth) / 2;

                startHeight = 0;
                width = newWidth;
            }
            else
            {
                height = newHeight;
            }
        }

        Graphics2D g2d = buf.createGraphics();
        g2d.setBackground(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
       g2d.drawImage(src, startWidth, startHeight, width, height, null);
       g2d.dispose();
       return buf;
   }
}
