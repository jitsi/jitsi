/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.rtp.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * @author Lubomir Marinov
 */
public class VideoMediaStreamImpl
    extends MediaStreamImpl
    implements VideoMediaStream
{
    private static final Logger logger
        = Logger.getLogger(VideoMediaStreamImpl.class);

    /**
     * JMF stores <tt>CUSTOM_CODEC_FORMATS</tt> statically, so they only need to
     * be registered once. FMJ does this dynamically (per instance), so it needs
     * to be done for every time we instantiate an RTP manager.
     */
    private static boolean formatsRegisteredOnce = false;

    public static Dimension selectVideoSize(
            DataSource videoDS,
            final int preferredWidth,
            final int preferredHeight)
    {
        if(videoDS == null)
            return null;

        FormatControl formatControl =
            (FormatControl) videoDS.getControl(FormatControl.class.getName());

        if (formatControl == null)
            return null;

        Format[] formats = formatControl.getSupportedFormats();
        final int count = formats.length;

        if (count < 1)
            return null;

        Format selectedFormat = null;

        if (count == 1)
            selectedFormat = formats[0];
        else
        {
            class FormatInfo
            {
                public final VideoFormat format;

                public final double difference;

                public FormatInfo(VideoFormat format)
                {
                    this.format = format;

                    Dimension size = format.getSize();

                    int width = size.width;
                    double xScale =
                        (width == preferredWidth)
                            ? 1
                            : (preferredWidth / (double) width);

                    int height = size.height;
                    double yScale =
                        (height == preferredHeight)
                            ? 1
                            : (preferredHeight / (double) height);

                    difference = Math.abs(1 - Math.min(xScale, yScale));
                }
            }

            FormatInfo[] infos = new FormatInfo[count];

            for (int i = 0; i < count; i++)
            {
                FormatInfo info =
                    infos[i] = new FormatInfo((VideoFormat) formats[i]);

                if (info.difference == 0)
                {
                    selectedFormat = info.format;
                    break;
                }
            }
            if (selectedFormat == null)
            {
                Arrays.sort(infos, new Comparator<FormatInfo>()
                {
                    public int compare(FormatInfo info0, FormatInfo info1)
                    {
                        return Double.compare(info0.difference,
                            info1.difference);
                    }
                });
                selectedFormat = infos[0].format;
            }
        }

        formatControl.setFormat(selectedFormat);
        return ((VideoFormat) selectedFormat).getSize();
    }

    public VideoMediaStreamImpl(StreamConnector connector, MediaDevice device)
    {
        super(connector, device);
    }

    /*
     * Implements VideoMediaStream#addVideoListener(VideoListener).
     */
    public void addVideoListener(VideoListener listener)
    {
        // TODO Auto-generated method stub
    }
    
    /*
     * Implements VideoMediaStream#getVisualComponent().
     */
    public Component getVisualComponent()
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected void registerCustomCodecFormats(RTPManager rtpManager)
    {
        // if we have already registered custom formats and we are running JMF
        // we bail out.
        if (!FMJConditionals.REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
                && formatsRegisteredOnce)
            return;

        Format format = new VideoFormat(Constants.H264_RTP);

        logger.debug("registering format " + format + " with RTP manager");

        /*
         * NOTE (mkoch@rowa.de): com.sun.media.rtp.RtpSessionMgr.addFormat leaks
         * memory, since it stores the Format in a static Vector. AFAIK there is
         * no easy way around it, but the memory impact should not be too bad.
         */
        rtpManager
            .addFormat(
                format,
                MediaUtils.jmfToSdpEncoding(format.getEncoding()));

        formatsRegisteredOnce = true;
    }
}
