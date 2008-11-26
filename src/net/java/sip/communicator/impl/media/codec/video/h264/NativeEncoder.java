package net.java.sip.communicator.impl.media.codec.video.h264;

import java.awt.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.codec.*;
import net.java.sip.communicator.util.*;
import net.sf.ffmpeg_java.*;
import net.sf.ffmpeg_java.AVCodecLibrary.*;

import com.sun.media.*;
import com.sun.jna.*;

/**
 * Encodes supplied data and encapsulate it in rtp according
 * rfc RFC3984.
 * @author Damian Minkov
 */
public class NativeEncoder
    extends BasicCodec
{
    private final Logger logger = Logger.getLogger(NativeEncoder.class);
    
    private final static String PLUGIN_NAME = "H264 Encoder";

    private static int DEF_WIDTH = 352;
    private static int DEF_HEIGHT = 288;

    // without the headers
//    private final static int MAX_PAYLOAD_SIZE = 1400;
    private final static int MAX_PAYLOAD_SIZE = 512;
    
    private final static int INPUT_BUFFER_PADDING_SIZE = 8;

    private final static Format[] defOutputFormats =
    { 
        new VideoFormat(Constants.H264_RTP)
    };

    // the frame rate we will use
    private final static int TARGET_FRAME_RATE = 15;

    // Instances for the ffmpeg lib
    private AVFormatLibrary AVFORMAT;
    private AVCodecLibrary AVCODEC;
    private AVUtilLibrary AVUTIL;

    // The codec we will use
    private AVCodec avcodec;
    private AVCodecContext avcontext;

    // the encoded data is stored in avpicture
    private AVFrame avpicture;

    // we use this buffer to supply data to encoder
    private Pointer encFrameBuffer;
    // the supplied data length
    private int encFrameLen;

    private Pointer rawFrameBuffer;

    // Vector to store NALs , when packaging encoded frame to rtp packets
    private Vector<byte[]> nals = new Vector<byte[]>();
    // last timestamp we processed
    private long lastTimeStamp = 0;
    // the current rtp sequence
    private int seq = 0;

    // key frame every four seconds
    private static int IFRAME_INTERVAL = TARGET_FRAME_RATE * 4;

    private int framesSinceLastIFrame = 0;

    /**
     * Constructor
     */
    public NativeEncoder()
    {
        DEF_WIDTH = Constants.VIDEO_WIDTH;
        DEF_HEIGHT = Constants.VIDEO_HEIGHT;

        int strideY = DEF_WIDTH;
        int strideUV = strideY / 2;
        int offsetU = strideY * DEF_HEIGHT;
        int offsetV = offsetU + strideUV * DEF_HEIGHT / 2;

        int inputYuvLength = (strideY + strideUV) * DEF_HEIGHT;
        float sourceFrameRate = TARGET_FRAME_RATE;

        inputFormats = new Format[]{ 
            new YUVFormat(new Dimension(DEF_WIDTH, DEF_HEIGHT), 
                inputYuvLength + INPUT_BUFFER_PADDING_SIZE,
                Format.byteArray, sourceFrameRate, YUVFormat.YUV_420, strideY,
                strideUV, 0, offsetU, offsetV) 
            };

        inputFormat = null;
        outputFormat = null;

        AVFORMAT = AVFormatLibrary.INSTANCE;
        AVCODEC = AVCodecLibrary.INSTANCE;
        AVUTIL = AVUtilLibrary.INSTANCE;

        AVFORMAT.av_register_all();

        AVCODEC.avcodec_init();
    }

    private Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat videoIn = (VideoFormat) in;
        Dimension inSize = videoIn.getSize();

        outputFormats =
            new VideoFormat[]
            {
                new VideoFormat(Constants.H264_RTP, inSize,
                    Format.NOT_SPECIFIED, Format.byteArray, videoIn
                        .getFrameRate()) };

        return outputFormats;
    }

    /**
     * Return the list of formats supported at the output.
     */
    public Format[] getSupportedOutputFormats(Format in)
    {
        // null input format
        if (in == null)
            return defOutputFormats;

        // mismatch input format
        if (!(in instanceof VideoFormat) || null == matches(in, inputFormats))
            return new Format[0];

        return getMatchingOutputFormats(in);
    }

    public Format setInputFormat(Format in)
    {
        // mismatch input format
        if (!(in instanceof VideoFormat) || null == matches(in, inputFormats))
            return null;

        VideoFormat videoIn = (VideoFormat) in;
        Dimension inSize = videoIn.getSize();

        if (inSize == null)
            inSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);

        YUVFormat yuv = (YUVFormat) videoIn;

        if (yuv.getOffsetU() > yuv.getOffsetV())
            return null;

        int strideY = inSize.width;
        int strideUV = strideY / 2;
        int offsetU = strideY * inSize.height;
        int offsetV = offsetU + strideUV * inSize.height / 2;

        int inputYuvLength = (strideY + strideUV) * inSize.height;
        float sourceFrameRate = videoIn.getFrameRate();

        inputFormat =
            new YUVFormat(inSize, inputYuvLength + INPUT_BUFFER_PADDING_SIZE,
                Format.byteArray, sourceFrameRate, YUVFormat.YUV_420, strideY,
                strideUV, 0, offsetU, offsetV);

        // Return the selected inputFormat
        return inputFormat;
    }

    public Format setOutputFormat(Format out)
    {
        // mismatch output format
        if (!(out instanceof VideoFormat)
            || null == matches(out, getMatchingOutputFormats(inputFormat)))
            return null;

        VideoFormat videoOut = (VideoFormat) out;
        Dimension outSize = videoOut.getSize();

        if (outSize == null)
        {
            Dimension inSize = ((VideoFormat) inputFormat).getSize();
            if (inSize == null)
                outSize = new Dimension(DEF_WIDTH, DEF_HEIGHT);
            else
                outSize = inSize;
        }

        outputFormat =
            new VideoFormat(videoOut.getEncoding(), 
                outSize, outSize.width * outSize.height,
                Format.byteArray, videoOut.getFrameRate());

        // Return the selected outputFormat
        return outputFormat;
    }

    public int process(Buffer inBuffer, Buffer outBuffer)
    {
        // if there are some nals we check and send them
        if(nals.size() > 0)
        {
            byte[] buf = nals.remove(0);
            //int type = buf[0]  & 0x1f;
            // if nal is too big we must make FU packet
            if(buf.length > MAX_PAYLOAD_SIZE)
            {
                int bufOfset = 0;
                int size = buf.length;

                int nri = buf[bufOfset]  & 0x60;
                byte[] tmp = new byte[MAX_PAYLOAD_SIZE];
                tmp[0] = 28;        /* FU Indicator; Type = 28 ---> FU-A */
                tmp[0] |= nri;
                tmp[1] = buf[bufOfset];
                // set start bit
                tmp[1] |= 1 << 7;
                // remove end bit
                tmp[1] &= ~(1 << 6);

                bufOfset += 1;
                size -= 1;
                int currentSIx = 0;
                while (size + 2 > MAX_PAYLOAD_SIZE) 
                {
                    System.arraycopy(buf, bufOfset, tmp, 2, MAX_PAYLOAD_SIZE - 2);

                    nals.add(currentSIx++, tmp.clone());

                    bufOfset += MAX_PAYLOAD_SIZE - 2;
                    size -= MAX_PAYLOAD_SIZE - 2;
                    tmp[1] &= ~(1 << 7);
                }

                byte[] tmp2 = new byte[size + 2];
                tmp2[0] = tmp[0];
                tmp2[1] = tmp[1];
                tmp2[1] |= 1 << 6;

                System.arraycopy(buf, bufOfset, tmp2, 2, size);
                nals.add(currentSIx++, tmp2);

                return INPUT_BUFFER_NOT_CONSUMED | OUTPUT_BUFFER_NOT_FILLED;
            }

            // size is ok lets send it
            outBuffer.setData(buf);
            outBuffer.setLength(buf.length);
            outBuffer.setOffset(0);
            outBuffer.setTimeStamp(lastTimeStamp);
            outBuffer.setSequenceNumber(seq++);

            // if more nals exist process them
            if(nals.size() > 0)
            {
                return  BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
            }
            else
            {
                // its the last one from current frame, mark it
                outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_RTP_MARKER);
                return BUFFER_PROCESSED_OK;
            }
        }

        if (isEOM(inBuffer))
        {
            propagateEOM(outBuffer);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        if (inBuffer.isDiscard())
        {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        Format inFormat = inBuffer.getFormat();
        if (inFormat != inputFormat && !(inFormat.matches(inputFormat)))
        {
            setInputFormat(inFormat);
        }

        if (inBuffer.getLength() < 10)
        {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        // copy data to avpicture
        rawFrameBuffer.write(0, 
            (byte[])inBuffer.getData(), inBuffer.getOffset(), encFrameLen);

        if(framesSinceLastIFrame >= IFRAME_INTERVAL )
        {
            avpicture.key_frame = 1;
            framesSinceLastIFrame = 0;
        }
        else
        {
            framesSinceLastIFrame++;
            avpicture.key_frame = 0;
        }
        
        // encode data
        int encLen = 
            AVCODEC.avcodec_encode_video(
                avcontext, encFrameBuffer, encFrameLen, avpicture);
        
        byte[] r = encFrameBuffer.getByteArray(0, encLen);

        // split encoded data to nals
        // we know it starts with 00 00 01
        int ix = 3;
        int prevIx = 1;
        while((ix = ff_avc_find_startcode(r, ix, r.length)) < r.length)
        {
            int len = ix - prevIx;
            byte[] b = new byte[len -4];
            System.arraycopy(r, prevIx+ 3, b, 0, len-4);
            nals.add(b);

            prevIx = ix;
            ix = prevIx + 3;
        }

        int len = ix - prevIx;
        if(len < 0)
        {
            logger.warn("aaaa");
            outBuffer.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }
            
        byte[] b = new byte[len - 3];
        System.arraycopy(r, prevIx+ 3, b, 0, len-3);
        nals.add(b);
        
        lastTimeStamp = inBuffer.getTimeStamp();
        
        return INPUT_BUFFER_NOT_CONSUMED | OUTPUT_BUFFER_NOT_FILLED;
    }
    
    /**
     * Find a NAL in the encoded data.
     * @param buff the encoded data
     * @param startIx starting index
     * @param endIx end index
     * @return starting position of the NAL
     */
    private int ff_avc_find_startcode(byte[] buff, int startIx, int endIx)
    {
        while(startIx < (endIx - 3))
        {
            if(buff[startIx] == 0 && 
                buff[startIx + 1] == 0 &&
                buff[startIx + 2] == 1)
                return startIx;
            
            startIx++;
        }
        return endIx;
    }

    public synchronized void open() throws ResourceUnavailableException
    {
        if (!opened)
        {
            super.open();

            if (inputFormat == null)
                throw new ResourceUnavailableException(
                    "No input format selected");
            if (outputFormat == null)
                throw new ResourceUnavailableException(
                    "No output format selected");

            AVCODEC.avcodec_init();

            avcodec = AVCODEC.avcodec_find_encoder(AVCodecLibrary.CODEC_ID_H264);
            avcontext = AVCODEC.avcodec_alloc_context();
            avpicture = AVCODEC.avcodec_alloc_frame();

            avcontext.crf = 1f;

            avcontext.pix_fmt = AVFormatLibrary.PIX_FMT_YUV420P;
            avcontext.width  = DEF_WIDTH;
            avcontext.height = DEF_HEIGHT;

            avcontext.time_base = new FFMPEGLibrary.AVRational(1, TARGET_FRAME_RATE);

            avpicture.linesize[0] = DEF_WIDTH;
            avpicture.linesize[1] = DEF_WIDTH / 2;
            avpicture.linesize[2] = DEF_WIDTH / 2;
            //avpicture.quality = (int)10;

            avcontext.qcompress = 1;

            int _bitRate = 48000;
            avcontext.bit_rate = _bitRate; // average bit rate
            avcontext.bit_rate_tolerance = _bitRate;// so to be 1 in x264
            avcontext.rc_max_rate = _bitRate;
            avcontext.sample_aspect_ratio.den = 0;
            avcontext.sample_aspect_ratio.num = 0;
            avcontext.thread_count = 0;
            avcontext.time_base.den = 15;//25500; //???
            avcontext.time_base.num = 1000;
            avcontext.qmin = 10;
            avcontext.qmax = 22;
            avcontext.max_qdiff = 4;

            avcontext.partitions |= 0x111;
            //X264_PART_I4X4 0x001  
            //X264_PART_P8X8 0x010
            //X264_PART_B8X8 0x100

            avcontext.mb_decision = AVCodecContext.FF_MB_DECISION_SIMPLE;

            avcontext.rc_eq = "blurCplx^(1-qComp)";

            avcontext.flags |= AVCodecLibrary.CODEC_FLAG_LOOP_FILTER;
            avcontext.me_method = 1;
            avcontext.me_subpel_quality = 1;//5;
            avcontext.me_range = 16;
            avcontext.me_cmp |= AVCodecContext.FF_CMP_CHROMA;
            avcontext.thread_count = 1;
            avcontext.scenechange_threshold = 40;
            avcontext.crf = 0;// Constant quality mode (also known as constant ratefactor)
            //avcontext.rc_buffer_size = _bitRate * 64;
            avcontext.gop_size = IFRAME_INTERVAL;
            framesSinceLastIFrame = IFRAME_INTERVAL + 1;
            avcontext.i_quant_factor = 1f/1.4f;

//            AVUTIL.av_log_set_callback(new AVUtilLibrary.LogCallback(){
//
//                public void callback(Pointer p, int l, String fmtS,
//                    Pointer va_list)
//                {
//                    logger.info("encoder: " + fmtS);
//                }});
               
            if (AVCODEC.avcodec_open(avcontext, avcodec) < 0)
                 throw new RuntimeException("Could not open codec "); 

            opened = true;

            encFrameLen = (DEF_WIDTH * DEF_HEIGHT * 3) / 2;

            rawFrameBuffer = AVUTIL.av_malloc(encFrameLen);
            encFrameBuffer = AVUTIL.av_malloc(encFrameLen);

            int size = DEF_WIDTH * DEF_HEIGHT;
            avpicture.data0 = rawFrameBuffer;        
            avpicture.data1 = avpicture.data0.share(size);
            avpicture.data2 = avpicture.data1.share(size/4);
            
            opened = true;
        }
    }

    public synchronized void close()
    {
        if (opened)
        {
            opened = false;
            super.close();

            AVCODEC.avcodec_close(avcontext);

            AVUTIL.av_free(avpicture.getPointer());
            AVUTIL.av_free(avcontext.getPointer());

            AVUTIL.av_free(rawFrameBuffer);
            AVUTIL.av_free(encFrameBuffer);
        }
    }

    public String getName()
    {
        return PLUGIN_NAME;
    }

    public java.lang.Object[] getControls()
    {
        if (controls == null)
        {
            controls =
                new Control[0];
        }

        return (Object[]) controls;
    }
}
