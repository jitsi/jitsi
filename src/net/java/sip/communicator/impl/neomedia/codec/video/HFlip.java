/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.video;

import java.awt.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.util.*;

/**
 * Implements a video <tt>Effect</tt> which horizontally flips
 * <tt>AVFrame</tt>s.
 *
 * @author Sebastien Vincent
 * @author Lyubomir Marinov
 */
public class HFlip
    extends AbstractCodecExt
    implements Effect
{
    /**
     * The <tt>Logger</tt> used by the <tt>HFlip</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger = Logger.getLogger(HFlip.class);

    /**
     * The list of <tt>Format</tt>s supported by <tt>HFlip</tt> instances as
     * input and output.
     */
    private static final Format[] SUPPORTED_FORMATS
        = new Format[] { new AVFrameFormat() };

    /**
     * The name of the FFmpeg ffsink video source <tt>AVFilter</tt> used by
     * <tt>HFlip</tt>.
     */
    private static final String VSINK_FFSINK_NAME = "nullsink";

    /**
     * The name of the FFmpeg buffer video source <tt>AVFilter</tt> used by
     * <tt>HFlip</tt>.
     */
    private static final String VSRC_BUFFER_NAME = "buffer";

    /**
     * The pointer to the <tt>AVFilterContext</tt> in {@link #graph} of the
     * FFmpeg video source with the name {@link #VSRC_BUFFER_NAME}.
     */
    private long buffer;

    /**
     * The pointer to the <tt>AVFilterContext</tt> in {@link #graph} of the
     * FFmpeg video sink with the name {@link #VSINK_FFSINK_NAME}.
     */
    private long ffsink;

    /**
     * The pointer to the <tt>AVFilterGraph</tt> instance which contains the
     * FFmpeg hflip filter represented by this <tt>Effect</tt>.
     */
    private long graph;

    /**
     * The height of {@link #graph}.
     */
    private int height;

    /**
     * The pointer to the <tt>AVFilterBufferRef</tt> instance represented as an
     * <tt>AVFrame</tt> by {@link #outputFrame}.
     */
    private long outputFilterBufferRef;

    /**
     * The pointer to the <tt>AVFrame</tt> instance which is the output (data)
     * of this <tt>Effect</tt>.
     */
    private long outputFrame;

    /**
     * The FFmpeg pixel format of {@link #graph}.
     */
    private int pixFmt = FFmpeg.PIX_FMT_NONE;

    /**
     * The width of {@link #graph}.
     */
    private int width;

    /**
     * Initializes a new <tt>HFlip</tt> instance.
     */
    public HFlip()
    {
        super("FFmpeg HFlip Filter", AVFrameFormat.class, SUPPORTED_FORMATS);
    }

    /**
     * Closes this <tt>Effect</tt>.
     *
     * @see AbstractCodecExt#doClose()
     */
    protected synchronized void doClose()
    {
        try
        {
            if (outputFrame != 0)
            {
                FFmpeg.av_free(outputFrame);
                outputFrame = 0;
            }
        }
        finally
        {
            reset();
        }
    }

    /**
     * Opens this <tt>Effect</tt>.
     *
     * @throws ResourceUnavailableException if any of the required resource
     * cannot be allocated
     * @see AbstractCodecExt#doOpen()
     */
    protected synchronized void doOpen()
        throws ResourceUnavailableException
    {
        outputFrame = FFmpeg.avcodec_alloc_frame();
        if (outputFrame == 0)
        {
            String reason = "avcodec_alloc_frame: " + outputFrame;

            logger.error(reason);
            throw new ResourceUnavailableException(reason);
        }
    }

    /**
     * Performs the media processing defined by this <tt>Effect</tt>.
     *
     * @param inputBuffer the <tt>Buffer</tt> that contains the media data to be
     * processed
     * @param outputBuffer the <tt>Buffer</tt> in which to store the processed
     * media data
     * @return <tt>BUFFER_PROCESSED_OK</tt> if the processing is successful
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    protected synchronized int doProcess(
            Buffer inputBuffer,
            Buffer outputBuffer)
    {
        /*
         * A new frame is about to be output so the old frame is no longer
         * necessary.
         */
        if (outputFilterBufferRef != 0)
        {
            FFmpeg.avfilter_unref_buffer(outputFilterBufferRef);
            outputFilterBufferRef = 0;
        }

        /*
         * Make sure the graph is configured with the current Format i.e. size
         * and pixFmt.
         */
        AVFrameFormat format = (AVFrameFormat) inputBuffer.getFormat();
        Dimension size = format.getSize();
        int pixFmt = format.getPixFmt();

        if ((this.width != size.width)
                || (this.height != size.height)
                || (this.pixFmt != pixFmt))
            reset();
        if (graph == 0)
        {
            String errorReason = null;
            int error = 0;
            long buffer = 0;
            long ffsink = 0;

            graph = FFmpeg.avfilter_graph_alloc();
            if (graph == 0)
                errorReason = "avfilter_graph_alloc";
            else
            {
                String filters
                    = VSRC_BUFFER_NAME + "=" + size.width + ":" + size.height
                        + ":" + pixFmt + ":1:1000000:1:1,hflip,"
                        + VSINK_FFSINK_NAME;
                long log_ctx = 0;

                error
                    = FFmpeg.avfilter_graph_parse(
                            graph,
                            filters,
                            0, 0,
                            log_ctx);
                if (error == 0)
                {
                    /*
                     * Unfortunately, the name of an AVFilterContext created by
                     * avfilter_graph_parse is not the name of the AVFilter.
                     */
                    String parsedFilterNameFormat = "Parsed filter %1$d %2$s";

                    buffer
                        = FFmpeg.avfilter_graph_get_filter(
                                graph,
                                String.format(
                                        parsedFilterNameFormat,
                                        0,
                                        VSRC_BUFFER_NAME));
                    if (buffer == 0)
                    {
                        errorReason
                            = "avfilter_graph_get_filter: " + VSRC_BUFFER_NAME;
                    }
                    else
                    {
                        ffsink
                            = FFmpeg.avfilter_graph_get_filter(
                                    graph,
                                    String.format(
                                            parsedFilterNameFormat,
                                            2,
                                            VSINK_FFSINK_NAME));
                        if (ffsink == 0)
                        {
                            errorReason
                                = "avfilter_graph_get_filter: "
                                    + VSINK_FFSINK_NAME;
                        }
                        else
                        {
                            error
                                = FFmpeg.avfilter_graph_config(graph, log_ctx);
                            if (error != 0)
                                errorReason = "avfilter_graph_config";
                        }
                    }
                }
                else
                    errorReason = "avfilter_graph_parse";
                if ((errorReason != null) || (error != 0))
                {
                    FFmpeg.avfilter_graph_free(graph);
                    graph = 0;
                }
            }
            if (graph == 0)
            {
                if (errorReason != null)
                {
                    if (error == 0)
                        logger.error(errorReason);
                    else
                        logger.error(errorReason + ": " + error);
                }
                return BUFFER_PROCESSED_FAILED;
            }
            else
            {
                this.width = size.width;
                this.height = size.height;
                this.pixFmt = pixFmt;
                this.buffer = buffer;
                this.ffsink = ffsink;
            }
        }

        /*
         * The graph is configured for the current Format, apply its filters to
         * the inputFrame.
         */
        long inputFrame = ((AVFrame) inputBuffer.getData()).getPtr();

        outputFilterBufferRef
            = FFmpeg.get_filtered_video_frame(
                    inputFrame, this.width, this.height, this.pixFmt,
                    buffer,
                    ffsink,
                    outputFrame);
        if(outputFilterBufferRef == 0)
        {
            /*
             * If get_filtered_video_frame fails, it is likely to fail for any
             * frame. Consequently, printing that it has failed will result in a
             * lot of repeating logging output. Since the failure in question
             * will be visible in the UI anyway, just debug it.
             */
            if (logger.isDebugEnabled())
                logger.debug("get_filtered_video_frame");
            return BUFFER_PROCESSED_FAILED;
        }

        Object out = outputBuffer.getData();

        if (!(out instanceof AVFrame)
                || (((AVFrame) out).getPtr() != outputFrame))
        {
            outputBuffer.setData(new AVFrame(outputFrame));
        }

        outputBuffer.setDiscard(inputBuffer.isDiscard());
        outputBuffer.setDuration(inputBuffer.getDuration());
        outputBuffer.setEOM(inputBuffer.isEOM());
        outputBuffer.setFlags(inputBuffer.getFlags());
        outputBuffer.setFormat(format);
        outputBuffer.setHeader(inputBuffer.getHeader());
        outputBuffer.setLength(inputBuffer.getLength());
        outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
        outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
        return BUFFER_PROCESSED_OK;
    }

    /**
     * Resets the state of this <tt>PlugIn</tt>.
     */
    @Override
    public synchronized void reset()
    {
        if (outputFilterBufferRef != 0)
        {
            FFmpeg.avfilter_unref_buffer(outputFilterBufferRef);
            outputFilterBufferRef = 0;
        }
        if (graph != 0)
        {
            FFmpeg.avfilter_graph_free(graph);
            graph = 0;

            width = 0;
            height = 0;
            pixFmt = FFmpeg.PIX_FMT_NONE;
            buffer = 0;
            ffsink = 0;
        }
    }
}
