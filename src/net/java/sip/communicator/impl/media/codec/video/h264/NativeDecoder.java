/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec.video.h264;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.media.codec.*;
import net.sf.fmj.media.*;
import net.sf.ffmpeg_java.*;
import net.sf.ffmpeg_java.AVCodecLibrary.*;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

/**
 * Decodes incoming rtp data of type h264 and returns the result
 * frames in RGB format.
 *
 * @author Damian Minkov
 */
public class NativeDecoder
    extends AbstractCodec
    implements Codec
{
    private final Logger logger = Logger.getLogger(NativeDecoder.class);

    private final static String PLUGIN_NAME = "H.264 Decoder";

    private VideoFormat[] outputFormats = null;
    private final VideoFormat[] defaultOutputFormats =
    new VideoFormat[]
    {
        new RGBFormat()
    };

    // Instances for the ffmpeg lib
    private AVFormatLibrary AVFORMAT;
    private AVCodecLibrary AVCODEC;
    private AVUtilLibrary AVUTIL;

    // The codec we will use
    private AVCodec avcodec;
    private AVCodecContext avcontext;
    // The decoded data is stored in avpicture in native ffmpeg format (YUV)
    private AVFrame avpicture;
    // Used to convert decoded data to RGB
    private AVFrame frameRGB;

    // The parser used to parse rtp content
    private H264Parser parser = new H264Parser();

    // supported sizes by the codec
    private Dimension[] supportedSizes = new Dimension[]
    {
            //P720
            new Dimension(720, 480),
            //CIF4
            new Dimension(704, 576),
            //CIF
            new Dimension(352, 288),
            new Dimension(320, 240),
            //QCIF
            new Dimension(176, 144),
            //SQCIF
            new Dimension(128, 96)
    };

    // index of default size (output format)
    private static int defaultSizeIx = 2;

    // current width of video, so we can detect changes in video size
    private double currentVideoWidth;

    // keep track of last received sequence in order to avoid inconsistent data
    private long lastReceivedSeq = -1;
    // in case of inconsistent data drop all data till a marker is received
    private boolean waitingForMarker = false;

    /**
     * Constructs new h264 decoder
     */
    public NativeDecoder()
    {
        inputFormats = new VideoFormat[]
        {
            new VideoFormat(Constants.H264_RTP)
        };

        outputFormats = new VideoFormat[supportedSizes.length];

        Dimension targetVideoSize =
            new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);

        for (int i = 0; i < supportedSizes.length; i++)
        {
            Dimension size = supportedSizes[i];

            if(size.equals(targetVideoSize))
                defaultSizeIx = i;

            outputFormats[i] =
              //PIX_FMT_RGB32
                new RGBFormat(
                    size,
                    -1,
                    Format.intArray,
                    Format.NOT_SPECIFIED,
                    32,
                    0xFF0000,
                    0xFF00,
                    0xFF,
                    1,
                    size.width,
                    Format.FALSE,
                    Format.NOT_SPECIFIED);
        }

        currentVideoWidth =
            outputFormats[defaultSizeIx].getSize().getWidth();

        AVFORMAT = AVFormatLibrary.INSTANCE;
        AVCODEC = AVCodecLibrary.INSTANCE;
        AVUTIL = AVUtilLibrary.INSTANCE;

        AVFORMAT.av_register_all();

        AVCODEC.avcodec_init();
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat ivf = (VideoFormat) in;

        // return the default size/currently decoder and encoder
        //set to transmit/receive at this size
        if(ivf.getSize() == null)
            return new Format[]{outputFormats[defaultSizeIx]};

        for (int i = 0; i < outputFormats.length; i++)
        {
            RGBFormat f = (RGBFormat)outputFormats[i];

            if(f.getSize().equals(ivf.getSize()))
                return new Format[]{f};
        }

        return null;
    }

    /**
     * Set the data input format.
     *
     * @return false if the format is not supported.
     */
    @Override
    public Format setInputFormat(Format format)
    {
        if (super.setInputFormat(format) != null)
        {
            reset();
            return format;
        }
        else
            return null;
    }

    @Override
    public Format setOutputFormat(Format format)
    {
        return super.setOutputFormat(format);
    }

    /**
     * Init the codec instances.
     */
    public void open() throws ResourceUnavailableException
    {
        avcodec = AVCODEC.avcodec_find_decoder(AVCodecLibrary.CODEC_ID_H264);

        avcontext = AVCODEC.avcodec_alloc_context();

        avpicture = AVCODEC.avcodec_alloc_frame();

        avcontext.workaround_bugs = 1;

//        AVUTIL.av_log_set_callback(new AVUtilLibrary.LogCallback()
//        {
//
//            public void callback(Pointer p, int l, String fmtS, Pointer va_list)
//            {
//                logger.info("decoder: " + fmtS);
//
//            }
//        });

        if (AVCODEC.avcodec_open(avcontext, avcodec) < 0)
            throw new RuntimeException("Could not open codec ");

        frameRGB = AVCODEC.avcodec_alloc_frame();

        opened = true;
    }

    @Override
    public void close()
    {
        if (opened)
        {
            opened = false;
            synchronized (this)
            {
                super.close();

                AVCODEC.avcodec_close(avcontext);

                AVUTIL.av_free(avpicture.getPointer());
                AVUTIL.av_free(avcontext.getPointer());
            }
        }
    }

    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer) || !opened)
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        if (inputBuffer.isDiscard())
        {
            inputBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        if(waitingForMarker)
        {
            lastReceivedSeq = inputBuffer.getSequenceNumber();
            if((inputBuffer.getFlags() & Buffer.FLAG_RTP_MARKER) != 0)
            {
                waitingForMarker = false;
                outputBuffer.setDiscard(true);
                return BUFFER_PROCESSED_OK;
            }
            else
                return OUTPUT_BUFFER_NOT_FILLED;
        }

        if(lastReceivedSeq != -1 && inputBuffer.getSequenceNumber() - lastReceivedSeq > 1)
        {
            long oldRecv = lastReceivedSeq;
            lastReceivedSeq = inputBuffer.getSequenceNumber();
            waitingForMarker = true;
            logger.trace("DROP rtp data! " + oldRecv + "/" + lastReceivedSeq);
            parser.reset();
            reset();
            return OUTPUT_BUFFER_NOT_FILLED;
        }
        else if(!parser.pushRTPInput(inputBuffer))
        {
            lastReceivedSeq = inputBuffer.getSequenceNumber();
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        lastReceivedSeq = inputBuffer.getSequenceNumber();

        // fills the incoming data
        IntByReference got_picture = new IntByReference();
        Pointer encBuf = AVUTIL.av_malloc(parser.getEncodedFrameLen());
        arraycopy(parser.getEncodedFrame(), 0, encBuf, 0, parser.getEncodedFrameLen());

        synchronized (this)
        {
            // decodes the data
            AVCODEC.avcodec_decode_video(
                avcontext, avpicture, got_picture, encBuf, parser.getEncodedFrameLen());

            if(avcontext.width != 0 && currentVideoWidth != avcontext.width)
            {
                currentVideoWidth = avcontext.width;
                VideoFormat format = getVideoFormat(currentVideoWidth);
                if(format != null)
                {
                    outputFormat = format;
                }
            }
            outputBuffer.setFormat(outputFormat);

            if(got_picture.getValue() == 0)
            {
                outputBuffer.setDiscard(true);
                AVUTIL.av_free(encBuf);

                return BUFFER_PROCESSED_OK;
            }

            // convert the picture in RGB Format
            int numBytes = AVCODEC.avpicture_get_size(
                AVCodecLibrary.PIX_FMT_RGB32, avcontext.width, avcontext.height);
            Pointer buffer = AVUTIL.av_malloc(numBytes);

            AVCODEC.avpicture_fill(
                frameRGB, buffer, AVCodecLibrary.PIX_FMT_RGB32, avcontext.width, avcontext.height);

            // Convert the image from its native format to RGB
            AVCODEC.img_convert(frameRGB, AVCodecLibrary.PIX_FMT_RGB32,
                avpicture, avcontext.pix_fmt, avcontext.width,
                avcontext.height);

            int[] data =
                frameRGB.data0.getIntArray(0, numBytes/4);

            int[] outData;

            Object outDataO = outputBuffer.getData();
            if(outDataO instanceof int[] &&
                ((int[])outDataO).length >= data.length)
            {
                outData = (int[])outDataO;
            }
            else
            {
                outData = new int[data.length];
            }

            System.arraycopy(data, 0, outData, 0, data.length);

            outputBuffer.setOffset(0);
            outputBuffer.setLength(data.length);
            outputBuffer.setData(outData);

            AVUTIL.av_free(encBuf);
            AVUTIL.av_free(buffer);

            return BUFFER_PROCESSED_OK;
        }
    }

    /**
     * Fills the given src array to the pointer
     * @param src the source
     * @param srcPos source offset
     * @param dest destination
     * @param destPos destination offset
     * @param length data length to be copied
     */
    private void arraycopy(byte[] src, int srcPos, Pointer dest, int destPos,
        int length)
    {
        int count = 0;
        while (count < length)
        {
            dest.setByte(destPos++, src[srcPos++]);
            count++;
        }
    }

    public boolean checkFormat(Format format)
    {
        if ((format.getEncoding()).equals(Constants.H264_RTP))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private VideoFormat getVideoFormat(double width)
    {
        for (int i = 0; i < outputFormats.length; i++)
        {
            VideoFormat vf = outputFormats[i];
            if(vf.getSize().getWidth() == width)
                return vf;
        }
        return null;
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public Format[] getSupportedOutputFormats(Format in)
    {
         // null input format
        if (in == null) {
            return defaultOutputFormats;
        }

        // mismatch input format
        if ( !(in instanceof VideoFormat ) ||
             (matches(in, inputFormats) == null) ) {
                return new Format[0];

        }

        // match input format
        return getMatchingOutputFormats(in);
    }

    /**
     * Utility to perform format matching.
     */
    public static Format matches(Format in, Format outs[])
    {
       for (int i = 0; i < outs.length; i++)
       {
          if (in.matches(outs[i]))
              return outs[i];
       }

       return null;
    }
}
