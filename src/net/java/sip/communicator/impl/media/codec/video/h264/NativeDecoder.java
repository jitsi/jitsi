package net.java.sip.communicator.impl.media.codec.video.h264;

import java.awt.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.impl.media.codec.*;
import net.sf.ffmpeg_java.*;
import net.sf.ffmpeg_java.AVCodecLibrary.*;

import com.ibm.media.codec.video.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;

/**
 * Decodes incoming rtp data of type h264 and returns the result 
 * frames in RGB format.
 * 
 * @author Damian Minkov
 */
public class NativeDecoder
    extends VideoCodec
{
    private final Logger logger = Logger.getLogger(NativeDecoder.class);
    
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
            //QCIF
            new Dimension(176, 144),
            //SQCIF
            new Dimension(128, 96)
    };
    
    /**
     * Constructs new h264 decoder
     */
    public NativeDecoder()
    {
        supportedInputFormats =
            new VideoFormat[]
            { 
                new VideoFormat(Constants.H264_RTP)
            };
        
        defaultOutputFormats = new VideoFormat[]
        { 
            new RGBFormat() 
        };
        PLUGIN_NAME = "H.264 Decoder";

        AVFORMAT = AVFormatLibrary.INSTANCE;
        AVCODEC = AVCodecLibrary.INSTANCE;
        AVUTIL = AVUtilLibrary.INSTANCE;

        AVFORMAT.av_register_all();

        AVCODEC.avcodec_init();
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat ivf = (VideoFormat) in;

        supportedOutputFormats = new VideoFormat[supportedSizes.length];
        
        for (int i = 0; i < supportedSizes.length; i++)
        {
            Dimension size = supportedSizes[i];
            supportedOutputFormats[i] = 
              //PIX_FMT_RGB32
                new RGBFormat(
                    size, 
                    -1,
                    Format.intArray, 
                    ivf.getFrameRate(), 
                    32, 
                    0xFF0000, 
                    0xFF00, 
                    0xFF, 
                    1, 
                    size.width, 
                    Format.FALSE, 
                    Format.NOT_SPECIFIED);
        }

        return supportedOutputFormats;
    }

    /**
     * Set the data input format.
     * 
     * @return false if the format is not supported.
     */
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

    /**
     * Init the codec instances.
     */
    public void open() throws ResourceUnavailableException
    {
        avcodec = AVCODEC.avcodec_find_decoder(AVCodecLibrary.CODEC_ID_H264);
        
        avcontext = AVCODEC.avcodec_alloc_context();
        
        if (AVCODEC.avcodec_open(avcontext, avcodec) < 0)
            throw new RuntimeException("Could not open codec "); 
        
        avpicture = AVCODEC.avcodec_alloc_frame();
        
        avcontext.lowres = 1;
        
        avcontext.workaround_bugs = 1;

        AVUTIL.av_log_set_callback(new AVUtilLibrary.LogCallback()
        {

            public void callback(Pointer p, int l, String fmtS, Pointer va_list)
            {
                logger.info("decoder: " + fmtS);

            }
        });

        frameRGB = AVCODEC.avcodec_alloc_frame();

        opened = true;
    }

    public void close()
    {
        if (opened)
        {
            opened = false;
            super.close();
            
            AVCODEC.avcodec_close(avcontext);
            
            AVUTIL.av_free(avpicture.getPointer());
            AVUTIL.av_free(avcontext.getPointer());
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
        
        if(!parser.pushRTPInput(inputBuffer))
        {
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        // fills the incoming data
        IntByReference got_picture = new IntByReference();
        Pointer encBuf = AVUTIL.av_malloc(parser.getEncodedFrameLen());
        arraycopy(parser.getEncodedFrame(), 0, encBuf, 0, parser.getEncodedFrameLen());
  
        // decodes the data
        AVCODEC.avcodec_decode_video(
            avcontext, avpicture, got_picture, encBuf, parser.getEncodedFrameLen());

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
            frameRGB.data0.getIntArray(0, avcontext.height * avcontext.width);
        int[] outData = validateIntArraySize(outputBuffer, data.length);
        System.arraycopy(data, 0, outData, 0, data.length);
        
        outputBuffer.setOffset(0);
        outputBuffer.setLength(outData.length);
        outputBuffer.setData(outData);
    
        AVUTIL.av_free(encBuf);
        AVUTIL.av_free(buffer);
    
        return BUFFER_PROCESSED_OK;
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
            return super.checkFormat(format);
        }
    }
}
