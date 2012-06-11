/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import java.awt.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.control.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Implements a H.264 encoder.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class JNIEncoder
    extends AbstractCodec
    implements RTCPFeedbackListener
{
    /**
     * The logger used by the <tt>JNIEncoder</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger = Logger.getLogger(JNIEncoder.class);

    /**
     * The name of the baseline H.264 (encoding) profile.
     */
    public static final String BASELINE_PROFILE = "baseline";

    /**
     * The frame rate to be assumed by <tt>JNIEncoder</tt> instance in the
     * absence of any other frame rate indication.
     */
    static final int DEFAULT_FRAME_RATE = 15;

    /**
     * The name of the <tt>ConfigurationService</tt> property which specifies
     * the H.264 (encoding) profile to be used in the absence of negotiation.
     * Though it seems that RFC 3984 "RTP Payload Format for H.264 Video"
     * specifies the baseline profile as the default, we have till the time of
     * this writing defaulted to the main profile and we do not currently want
     * to change from the main to the base profile unless we really have to.
     */
    public static final String DEFAULT_PROFILE_PNAME
        = "net.java.sip.communicator.impl.neomedia.codec.video.h264."
            + "defaultProfile";

    /**
     * Key frame every 150 frames.
     */
    static final int IFRAME_INTERVAL = 150;

    /**
     * The name of the main H.264 (encoding) profile.
     */
    public static final String MAIN_PROFILE = "main";

    /**
     * The default value of the {@link #DEFAULT_PROFILE_PNAME}
     * <tt>ConfigurationService</tt> property.
     */
    public static final String DEFAULT_DEFAULT_PROFILE = MAIN_PROFILE;

    /**
     * The name of the format parameter which specifies the packetization mode
     * of H.264 RTP payload.
     */
    public static final String PACKETIZATION_MODE_FMTP = "packetization-mode";

    /**
     * Minimum interval between two PLI request processing (in milliseconds).
     */
    private static final long PLI_INTERVAL = 3000;

    /**
     * Name of the code.
     */
    private static final String PLUGIN_NAME = "H.264 Encoder";

    /**
     * The list of <tt>Formats</tt> supported by <tt>JNIEncoder</tt> instances
     * as output.
     */
    static final Format[] SUPPORTED_OUTPUT_FORMATS
        = {
            new ParameterizedVideoFormat(
                    Constants.H264,
                    PACKETIZATION_MODE_FMTP, "0"),
            new ParameterizedVideoFormat(
                    Constants.H264,
                    PACKETIZATION_MODE_FMTP, "1")
        };

    /**
     * The codec we will use.
     */
    private long avctx;

    /**
     * The encoded data is stored in avpicture.
     */
    private long avframe;

    /**
     * We use this buffer to supply data to encoder.
     */
    private byte[] encFrameBuffer;

    /**
     * The supplied data length.
     */
    private int encFrameLen;

    /**
     * Force encoder to send a key frame.
     * First frame have to be a keyframe.
     */
    private boolean forceKeyFrame = true;

    /**
     * Next interval for an automatic keyframe.
     */
    private int framesSinceLastIFrame = IFRAME_INTERVAL + 1;

    /**
     * The <tt>KeyFrameControl</tt> used by this <tt>JNIEncoder</tt> to
     * control its key frame-related logic.
     */
    private KeyFrameControl keyFrameControl;

    private KeyFrameControl.KeyFrameRequestee keyFrameRequestee;

    /**
     * The time in milliseconds of the last request for a key frame from the
     * remote peer to this local peer.
     */
    private long lastKeyFrameRequestTime = System.currentTimeMillis();

    /**
     * The packetization mode to be used for the H.264 RTP payload output by
     * this <tt>JNIEncoder</tt> and the associated packetizer. RFC 3984 "RTP
     * Payload Format for H.264 Video" says that "[w]hen the value of
     * packetization-mode is equal to 0 or packetization-mode is not present,
     * the single NAL mode, as defined in section 6.2 of RFC 3984, MUST be
     * used."
     */
    private String packetizationMode;

    /**
     * The raw frame buffer.
     */
    private long rawFrameBuffer;

    /**
     * Peer that receive stream from latest ffmpeg/x264 aware peer does not
     * manage to decode the first keyframe and must wait for the next periodic
     * intra refresh to display the video to the user.
     *
     * Temporary solution for this problem: send the two first frames as
     * keyframes to display video stream.
     */
    private boolean secondKeyFrame = true;

    /**
     * Additional codec settings.
     */
    private Map<String, String> additionalSettings = null;

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        inputFormats
            = new Format[]
            {
                new YUVFormat(
                        null,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        DEFAULT_FRAME_RATE,
                        YUVFormat.YUV_420,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                        0, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED)
            };

        inputFormat = null;
        outputFormat = null;
    }

    /**
     * Closes this <tt>Codec</tt>.
     */
    @Override
    public synchronized void close()
    {
        if (opened)
        {
            opened = false;
            super.close();

            FFmpeg.avcodec_close(avctx);
            FFmpeg.av_free(avctx);
            avctx = 0;

            FFmpeg.av_free(avframe);
            avframe = 0;
            FFmpeg.av_free(rawFrameBuffer);
            rawFrameBuffer = 0;

            encFrameBuffer = null;

            if (keyFrameRequestee != null)
            {
                if (keyFrameControl != null)
                    keyFrameControl.removeKeyFrameRequestee(keyFrameRequestee);
                keyFrameRequestee = null;
            }
        }
    }

    /**
     * Event fired when RTCP feedback message is received.
     *
     * @param event <tt>RTCPFeedbackEvent</tt>
     */
    public void feedbackReceived(RTCPFeedbackEvent event)
    {
        /*
         * If RTCP message is a Picture Loss Indication (PLI) or a
         * Full Intra-frame Request (FIR) the encoder will force the next frame
         * to be a keyframe.
         */
        if (event.getPayloadType() == RTCPFeedbackEvent.PT_PS)
        {
            switch (event.getFeedbackMessageType())
            {
                case RTCPFeedbackEvent.FMT_PLI:
                case RTCPFeedbackEvent.FMT_FIR:
                    keyFrameRequest();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Gets the matching output formats for a specific format.
     *
     * @param in input format
     * @return array for formats matching input format
     */
    private Format[] getMatchingOutputFormats(Format in)
    {
        VideoFormat videoIn = (VideoFormat) in;

        String[] packetizationModes
            = (this.packetizationMode == null)
                ? new String[] { "0", "1" }
                : new String[] { this.packetizationMode };
        Format[] matchingOutputFormats = new Format[packetizationModes.length];
        Dimension size = videoIn.getSize();
        float frameRate = videoIn.getFrameRate();

        for (int index = packetizationModes.length - 1; index >= 0; index--)
        {
            matchingOutputFormats[index]
                = new ParameterizedVideoFormat(
                        Constants.H264,
                        size,
                        Format.NOT_SPECIFIED,
                        Format.byteArray,
                        frameRate,
                        ParameterizedVideoFormat.toMap(
                                PACKETIZATION_MODE_FMTP,
                                packetizationModes[index]));
        }
        return matchingOutputFormats;
    }

    /**
     * Gets the name of this <tt>Codec</tt>.
     *
     * @return codec name
     */
    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Returns the list of formats supported at the output.
     *
     * @param in input <tt>Format</tt> to determine corresponding output
     * <tt>Format</tt>s
     * @return array of formats supported at output
     */
    public Format[] getSupportedOutputFormats(Format in)
    {
        Format[] supportedOutputFormats;

        // null input format
        if (in == null)
            supportedOutputFormats = SUPPORTED_OUTPUT_FORMATS;
        // mismatch input format
        else if (!(in instanceof VideoFormat)
                || (null == AbstractCodecExt.matches(in, inputFormats)))
            supportedOutputFormats = new Format[0];
        else
            supportedOutputFormats = getMatchingOutputFormats(in);
        return supportedOutputFormats;
    }

    /**
     * Notifies this <tt>JNIEncoder</tt> that the remote peer has requested a
     * key frame from this local peer.
     *
     * @return <tt>true</tt> if this <tt>JNIEncoder</tt> has honored the request
     * for a key frame; otherwise, <tt>false</tt>
     */
    private boolean keyFrameRequest()
    {
        if (System.currentTimeMillis()
                > (lastKeyFrameRequestTime + PLI_INTERVAL))
        {
            lastKeyFrameRequestTime = System.currentTimeMillis();
            forceKeyFrame = true;
        }
        return true;
    }

    /**
     * Opens this <tt>Codec</tt>.
     */
    @Override
    public synchronized void open()
        throws ResourceUnavailableException
    {
        if (opened)
            return;

        if (inputFormat == null)
            throw new ResourceUnavailableException("No input format selected");
        if (outputFormat == null)
            throw new ResourceUnavailableException("No output format selected");

        VideoFormat outputVideoFormat = (VideoFormat) outputFormat;
        Dimension size = outputVideoFormat.getSize();
        int width = size.width;
        int height = size.height;
        boolean useIntraRefresh = true;
        boolean useCustomProfile = false;
        String customProfile = null;

        if(additionalSettings != null)
        {
            for(Map.Entry<String, String> entry : additionalSettings.entrySet())
            {
                String key = entry.getKey();
                String value = entry.getValue();

                if(key.equals("h264.intrarefresh") && value.equals("false"))
                {
                    useIntraRefresh = false;
                }
                else if(key.equals("h264.profile"))
                {
                    if(value.equals(MAIN_PROFILE) ||
                        value.equals(BASELINE_PROFILE))
                    {
                        useCustomProfile = true;
                        customProfile = value;
                    }
                }
            }
        }

        long avcodec = FFmpeg.avcodec_find_encoder(FFmpeg.CODEC_ID_H264);

        avctx = FFmpeg.avcodec_alloc_context();

        FFmpeg.avcodeccontext_set_pix_fmt(avctx, FFmpeg.PIX_FMT_YUV420P);
        FFmpeg.avcodeccontext_set_size(avctx, width, height);

        FFmpeg.avcodeccontext_set_qcompress(avctx, 0.6f);

        int bitRate = 128000;
        int frameRate = (int) outputVideoFormat.getFrameRate();

        if (frameRate == Format.NOT_SPECIFIED)
            frameRate = DEFAULT_FRAME_RATE;

        // average bit rate
        FFmpeg.avcodeccontext_set_bit_rate(avctx, bitRate);
        // so to be 1 in x264
        FFmpeg.avcodeccontext_set_bit_rate_tolerance(avctx, (bitRate /
                frameRate));
        FFmpeg.avcodeccontext_set_rc_max_rate(avctx, bitRate);
        FFmpeg.avcodeccontext_set_sample_aspect_ratio(avctx, 0, 0);
        FFmpeg.avcodeccontext_set_thread_count(avctx, 1);

        // time_base should be 1 / frame rate
        FFmpeg.avcodeccontext_set_time_base(avctx, 1, frameRate);
        FFmpeg.avcodeccontext_set_ticks_per_frame(avctx, 2);
        FFmpeg.avcodeccontext_set_quantizer(avctx, 30, 31, 4);

        // avctx.chromaoffset = -2;

        FFmpeg.avcodeccontext_add_partitions(avctx, 0x111);
        // X264_PART_I4X4 0x001
        // X264_PART_P8X8 0x010
        // X264_PART_B8X8 0x100

        FFmpeg.avcodeccontext_set_mb_decision(avctx,
            FFmpeg.FF_MB_DECISION_SIMPLE);

        FFmpeg.avcodeccontext_set_rc_eq(avctx, "blurCplx^(1-qComp)");

        FFmpeg.avcodeccontext_add_flags(avctx,
                FFmpeg.CODEC_FLAG_LOOP_FILTER);
        if(useIntraRefresh)
            FFmpeg.avcodeccontext_add_flags2(avctx,
                FFmpeg.CODEC_FLAG2_INTRA_REFRESH);
        FFmpeg.avcodeccontext_set_me_method(avctx, 7);
        FFmpeg.avcodeccontext_set_me_subpel_quality(avctx, 2);
        FFmpeg.avcodeccontext_set_me_range(avctx, 16);
        FFmpeg.avcodeccontext_set_me_cmp(avctx, FFmpeg.FF_CMP_CHROMA);
        FFmpeg.avcodeccontext_set_scenechange_threshold(avctx, 40);
        // Constant quality mode (also known as constant ratefactor)
        FFmpeg.avcodeccontext_set_crf(avctx, 0);
        FFmpeg.avcodeccontext_set_rc_buffer_size(avctx, 10);
        FFmpeg.avcodeccontext_set_gop_size(avctx, IFRAME_INTERVAL);
        FFmpeg.avcodeccontext_set_i_quant_factor(avctx, 1f / 1.4f);

        FFmpeg.avcodeccontext_set_refs(avctx, 1);
        //FFmpeg.avcodeccontext_set_trellis(avctx, 2);

        FFmpeg.avcodeccontext_set_keyint_min(avctx, 0);

        if ((null == packetizationMode) || "0".equals(packetizationMode))
        {
            FFmpeg.avcodeccontext_set_rtp_payload_size(avctx,
                    Packetizer.MAX_PAYLOAD_SIZE);
        }

        /*
         * XXX We do not currently negotiate the profile so, regardless of the
         * many AVCodecContext properties we have set above, force the default
         * profile configuration.
         */
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        String profile
            = (cfg == null)
                ? null
                : cfg.getString(DEFAULT_PROFILE_PNAME, DEFAULT_DEFAULT_PROFILE);

        if(useCustomProfile)
        {
            profile = customProfile;
        }

        try
        {
            FFmpeg.avcodeccontext_set_profile(avctx,
                    BASELINE_PROFILE.equalsIgnoreCase(profile)
                        ? FFmpeg.FF_PROFILE_H264_BASELINE
                        : FFmpeg.FF_PROFILE_H264_MAIN);
        }
        catch (UnsatisfiedLinkError ule)
        {
            logger.warn("The FFmpeg JNI library is out-of-date.");
        }

        if (FFmpeg.avcodec_open(avctx, avcodec) < 0)
        {
            throw
                new ResourceUnavailableException(
                        "Could not open codec. (size= "
                            + width + "x" + height
                            + ")");
        }

        encFrameLen = (width * height * 3) / 2;

        rawFrameBuffer = FFmpeg.av_malloc(encFrameLen);
        avframe = FFmpeg.avcodec_alloc_frame();

        int sizeInBytes = width * height;

        FFmpeg.avframe_set_data(
                avframe,
                rawFrameBuffer,
                sizeInBytes,
                sizeInBytes / 4);
        FFmpeg.avframe_set_linesize(avframe, width, width / 2, width / 2);

        encFrameBuffer = new byte[encFrameLen];

        /*
         * Implement the ability to have the remote peer request key frames from
         * this local peer.
         */
        if (keyFrameRequestee == null)
        {
            keyFrameRequestee = new KeyFrameControl.KeyFrameRequestee()
            {
                public boolean keyFrameRequest()
                {
                    return JNIEncoder.this.keyFrameRequest();
                }
            };
        }
        if (keyFrameControl != null)
            keyFrameControl.addKeyFrameRequestee(-1, keyFrameRequestee);

        opened = true;
        super.open();
    }

    /**
     * Processes/encodes a buffer.
     *
     * @param inBuffer input buffer
     * @param outBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
     */
    public synchronized int process(Buffer inBuffer, Buffer outBuffer)
    {
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

        if ((inFormat != inputFormat) && !inFormat.matches(inputFormat))
            setInputFormat(inFormat);

        if (inBuffer.getLength() < 10)
        {
            outBuffer.setDiscard(true);
            reset();
            return BUFFER_PROCESSED_OK;
        }

        // copy data to avframe
        FFmpeg.memcpy(
                rawFrameBuffer,
                (byte[]) inBuffer.getData(), inBuffer.getOffset(),
                encFrameLen);

        if (/* framesSinceLastIFrame >= IFRAME_INTERVAL || */ forceKeyFrame)
        {
            FFmpeg.avframe_set_key_frame(avframe, true);
            framesSinceLastIFrame = 0;

            /* send keyframe for the first two frames */
            if(secondKeyFrame)
            {
                secondKeyFrame = false;
                forceKeyFrame = true;
            }
            else
            {
                forceKeyFrame = false;
            }
        }
        else
        {
            framesSinceLastIFrame++;
            FFmpeg.avframe_set_key_frame(avframe, false);
        }

        // encode data
        int encLen
            = FFmpeg.avcodec_encode_video(
                    avctx,
                    encFrameBuffer, encFrameLen,
                    avframe);

        /*
         * Do not always allocate a new data array for outBuffer, try to reuse
         * the existing one if it is suitable.
         */
        Object outData = outBuffer.getData();
        byte[] out;

        if (outData instanceof byte[])
        {
            out = (byte[]) outData;
            if (out.length < encLen)
                out = null;
        }
        else
            out = null;
        if (out == null)
            out = new byte[encLen];

        System.arraycopy(encFrameBuffer, 0, out, 0, encLen);

        outBuffer.setData(out);
        outBuffer.setLength(encLen);
        outBuffer.setOffset(0);
        outBuffer.setTimeStamp(inBuffer.getTimeStamp());
        return BUFFER_PROCESSED_OK;
    }

    /**
     * Sets the input format.
     *
     * @param in format to set
     * @return format
     */
    @Override
    public Format setInputFormat(Format in)
    {
        // mismatch input format
        if (!(in instanceof VideoFormat)
                || (null == AbstractCodecExt.matches(in, inputFormats)))
            return null;

        YUVFormat yuv = (YUVFormat) in;

        if (yuv.getOffsetU() > yuv.getOffsetV())
            return null;

        Dimension size = yuv.getSize();

        if (size == null)
            size = new Dimension(Constants.VIDEO_WIDTH, Constants.VIDEO_HEIGHT);

        int strideY = size.width;
        int strideUV = strideY / 2;
        int offsetU = strideY * size.height;
        int offsetV = offsetU + strideUV * size.height / 2;

        int yuvMaxDataLength = (strideY + strideUV) * size.height;

        inputFormat
            = new YUVFormat(
                    size,
                    yuvMaxDataLength + FFmpeg.FF_INPUT_BUFFER_PADDING_SIZE,
                    Format.byteArray,
                    yuv.getFrameRate(),
                    YUVFormat.YUV_420,
                    strideY, strideUV,
                    0, offsetU, offsetV);

        // Return the selected inputFormat
        return inputFormat;
    }

    /**
     * Sets the <tt>KeyFrameControl</tt> to be used by this
     * <tt>JNIEncoder</tt> as a means of control over its key frame-related
     * logic.
     *
     * @param keyFrameControl the <tt>KeyFrameControl</tt> to be used by this
     * <tt>JNIEncoder</tt> as a means of control over its key frame-related
     * logic
     */
    public void setKeyFrameControl(KeyFrameControl keyFrameControl)
    {
        if (this.keyFrameControl != keyFrameControl)
        {
            if ((this.keyFrameControl != null) && (keyFrameRequestee != null))
                this.keyFrameControl.removeKeyFrameRequestee(keyFrameRequestee);

            this.keyFrameControl = keyFrameControl;

            if ((this.keyFrameControl != null) && (keyFrameRequestee != null))
                this.keyFrameControl.addKeyFrameRequestee(-1, keyFrameRequestee);
        }
    }

    /**
     * Sets the <tt>Format</tt> in which this <tt>Codec</tt> is to output media
     * data.
     *
     * @param out the <tt>Format</tt> in which this <tt>Codec</tt> is to
     * output media data
     * @return the <tt>Format</tt> in which this <tt>Codec</tt> is currently
     * configured to output media data or <tt>null</tt> if <tt>format</tt> was
     * found to be incompatible with this <tt>Codec</tt>
     */
    @Override
    public Format setOutputFormat(Format out)
    {
        // mismatch output format
        if (!(out instanceof VideoFormat)
                || (null
                        == AbstractCodecExt.matches(
                                out,
                                getMatchingOutputFormats(inputFormat))))
            return null;

        VideoFormat videoOut = (VideoFormat) out;
        Dimension outSize = videoOut.getSize();

        if (outSize == null)
        {
            Dimension inSize = ((VideoFormat) inputFormat).getSize();

            outSize
                = (inSize == null)
                    ? new Dimension(
                            Constants.VIDEO_WIDTH,
                            Constants.VIDEO_HEIGHT)
                    : inSize;
        }

        Map<String, String> fmtps = null;

        if (out instanceof ParameterizedVideoFormat)
            fmtps = ((ParameterizedVideoFormat) out).getFormatParameters();
        if (fmtps == null)
            fmtps = new HashMap<String, String>();
        if (packetizationMode != null)
            fmtps.put(PACKETIZATION_MODE_FMTP, packetizationMode);

        outputFormat
            = new ParameterizedVideoFormat(
                    videoOut.getEncoding(),
                    outSize,
                    Format.NOT_SPECIFIED,
                    Format.byteArray,
                    videoOut.getFrameRate(),
                    fmtps);

        // Return the selected outputFormat
        return outputFormat;
    }

    /**
     * Sets the packetization mode to be used for the H.264 RTP payload output
     * by this <tt>JNIEncoder</tt> and the associated packetizer.
     *
     * @param packetizationMode the packetization mode to be used for the H.264
     * RTP payload output by this <tt>JNIEncoder</tt> and the associated
     * packetizer
     */
    public void setPacketizationMode(String packetizationMode)
    {
        /*
         * RFC 3984 "RTP Payload Format for H.264 Video" says that "[w]hen the
         * value of packetization-mode is equal to 0 or packetization-mode is
         * not present, the single NAL mode, as defined in section 6.2 of RFC
         * 3984, MUST be used."
         */
        if ((packetizationMode == null) || "0".equals(packetizationMode))
            this.packetizationMode = "0";
        else if ("1".equals(packetizationMode))
            this.packetizationMode = "1";
        else
            throw new IllegalArgumentException("packetizationMode");
    }

    /**
     * Sets additional settings for the codec.
     *
     * @param settings additional settings
     */
    public void setAdditionalCodecSettings(Map<String, String> settings)
    {
        additionalSettings = settings;
    }
}
