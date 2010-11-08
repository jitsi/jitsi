/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.videoflip;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.util.*;

/**
 * JMF Effect to flip local video horizontally.
 *
 * @author Sebastien Vincent
 */
public class VideoFlipEffect
    extends ControlsAdapter
    implements Effect
{
    /**
     * The <tt>Logger</tt> used by the <tt>SwScaler</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(VideoFlipEffect.class);

    /**
     * List of supported input formats.
     */
    private final static Format DEFAULT_INPUT_FORMATS[]
        = new Format[]
                     {
                         new AVFrameFormat(),
                     };

    /**
     * List of supported output formats.
     */
    private final static Format DEFAULT_OUTPUT_FORMATS[]
        = new Format[]
                     {
                        new AVFrameFormat(),
                     };

    /**
     * Name of the effect.
     */
    private static final String PLUGIN_NAME = "Local video flip Effect";

    /**
     * Input Format
     */
    private VideoFormat inputFormat = null;

    /**
     * Output Format
     */
    private VideoFormat outputFormat = null;

    /**
     * AVFilterGraph reference.
     */
    private long graph = 0;

    /**
     * AVInputStream reference.
     */
    private long inputstream = 0;

    /**
     * AVFrame reference.
     */
    private long avframe = 0;

    /**
     * If the filters are configured.
     */
    private boolean configured = false;

    /**
     * Constructor.
     */
    public VideoFlipEffect()
    {
    }

    /**
     * Lists all of the input formats that this codec accepts.
     *
     * @return An array that contains the supported input <tt>Formats</tt>.
     */
    public Format[] getSupportedInputFormats()
    {
        return DEFAULT_INPUT_FORMATS;
    }

    /**
     * Lists the output formats that this codec can generate.
     *
     * @param input The <tt>Format</tt> of the data to be used as input to the
     * plug-in.
     * @return An array that contains the supported output <tt>Formats</tt>.
     */
    public Format[] getSupportedOutputFormats(Format input)
    {
        if(input == null)
        {
            return DEFAULT_OUTPUT_FORMATS;
        }

        return new Format[]
        {
                (Format)input.clone(),
        };
    }

    /**
     * Sets the format of the data to be input to this codec.
     *
     * @param format The <tt>Format</tt> to be set.
     * @return The <tt>Format</tt> that was set.
     */
    public Format setInputFormat(Format format)
    {
        inputFormat = (VideoFormat)format;
        return inputFormat;
    }

    /**
     * Sets the format for the data this codec outputs.
     *
     * @param format The <tt>Format</tt> to be set.
     * @return The <tt>Format</tt> that was set.
     */
    public Format setOutputFormat(Format format)
    {
        outputFormat = (VideoFormat)format;
        return outputFormat;
    }

    /**
     * Opens this effect.
     *
     * @throws ResourceUnavailableException If all of the required resources
     * cannot be acquired.
     */
    public void open() throws ResourceUnavailableException
    {
        graph = FFmpeg.avfilter_alloc_filtergraph();
        inputstream = FFmpeg.avfilter_alloc_inputstream();
        avframe = FFmpeg.avcodec_alloc_frame();
    }

    /**
     * Closes this effect.
     */
    public void close()
    {
        if(graph != 0)
        {
            FFmpeg.avfilter_free_filtergraph(graph);
        }

        if(inputstream != 0)
        {
            FFmpeg.avfilter_free_inputstream(inputstream);
        }

        if(avframe != 0)
        {
            FFmpeg.av_free(avframe);
            avframe = 0;
        }

        configured = false;
    }

    /**
     * Gets the name of this plug-in as a human-readable string.
     *
     * @return A <tt>String</tt> that contains the descriptive name of the
     * plug-in.
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * Performs the media processing defined by this codec.
     *
     * @param inputBuffer The <tt>Buffer</tt> that contains the media data to be
     * processed.
     * @param outputBuffer The <tt>Buffer</tt> in which to store the processed
     * media data.
     * @return <tt>BUFFER_PROCESSED_OK</tt> if the processing is successful.
     * @see PlugIn
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        int ret = 0;

        if(inputBuffer.getFormat() instanceof AVFrameFormat)
        {
            if(!configured)
            {
                AVFrameFormat format = (AVFrameFormat)inputBuffer.getFormat();
                if(FFmpeg.avfilter_configure_filters("hflip", inputstream,
                        format.getPixFmt(), graph) != 0)
                {
                    if(logger.isDebugEnabled())
                    {
                        logger.debug(
                                "Failed to configure libavfilter's filters");
                    }
                    return BUFFER_PROCESSED_FAILED;
                }
                configured = true;
            }

            AVFrame frame = (AVFrame)inputBuffer.getData();
            FFmpeg.av_vsrc_buffer_add_frame(inputstream, frame.getPtr());

            if(FFmpeg.av_get_filtered_video_frame(inputstream, avframe) != -1)
            {
                Object out = outputBuffer.getData();

                if (!(out instanceof AVFrame) ||
                        (((AVFrame) out).getPtr() != avframe))
                {
                    outputBuffer.setData(new AVFrame(avframe));
                }
            }
        }

        if(ret == BUFFER_PROCESSED_OK)
        {
            outputBuffer.setLength(inputBuffer.getLength());
            outputBuffer.setFormat(inputBuffer.getFormat());
            outputBuffer.setHeader(inputBuffer.getHeader());
            outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
            outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
            outputBuffer.setFlags(inputBuffer.getFlags());
            outputBuffer.setDiscard(inputBuffer.isDiscard());
            outputBuffer.setEOM(inputBuffer.isEOM());
            outputBuffer.setDuration(inputBuffer.getDuration());
        }

        return ret;
    }

    /**
     * Resets effect state.
     */
    public void reset()
    {
    }
}
