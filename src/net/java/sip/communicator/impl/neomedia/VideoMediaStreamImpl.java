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
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaStreamImpl</tt> in order to provide an implementation of
 * <tt>VideoMediaStream</tt>.
 *
 * @author Lubomir Marinov
 */
public class VideoMediaStreamImpl
    extends MediaStreamImpl
    implements VideoMediaStream
{

    /**
     * The <tt>Logger</tt> used by the <tt>VideoMediaStreamImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(VideoMediaStreamImpl.class);

    /**
     * JMF stores <tt>CUSTOM_CODEC_FORMATS</tt> statically, so they only need to
     * be registered once. FMJ does this dynamically (per instance), so it needs
     * to be done for every time we instantiate an RTP manager.
     */
    private static boolean formatsRegisteredOnce = false;

    /**
     * Selects the <tt>VideoFormat</tt> from the list of supported formats of a
     * specific video <tt>DataSource</tt> which has a size as close as possible
     * to a specific size and sets it as the format of the specified video
     * <tt>DataSource</tt>.
     *
     * @param videoDS the video <tt>DataSource</tt> which is to have its
     * supported formats examined and its format changed to the
     * <tt>VideoFormat</tt> which is as close as possible to the specified
     * <tt>preferredWidth</tt> and <tt>preferredHeight</tt>
     * @param preferredWidth the width of the <tt>VideoFormat</tt> to be
     * selected
     * @param preferredHeight the height of the <tt>VideoFormat</tt> to be
     * selected
     * @return the size of the <tt>VideoFormat</tt> from the list of supported
     * formats of <tt>videoDS</tt> which is as close as possible to
     * <tt>preferredWidth</tt> and <tt>preferredHeight</tt> and which has been
     * set as the format of <tt>videoDS</tt>
     */
    public static Dimension selectVideoSize(
            DataSource videoDS,
            final int preferredWidth,
            final int preferredHeight)
    {
        if (videoDS == null)
            return null;

        FormatControl formatControl
            = (FormatControl) videoDS.getControl(FormatControl.class.getName());

        if (formatControl == null)
            return null;

        Format[] formats = formatControl.getSupportedFormats();
        final int count = formats.length;

        if (count < 1)
            return null;

        VideoFormat selectedFormat = null;

        if (count == 1)
            selectedFormat = (VideoFormat) formats[0];
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
                FormatInfo info
                    = infos[i] = new FormatInfo((VideoFormat) formats[i]);

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
                        return
                            Double.compare(info0.difference, info1.difference);
                    }
                });
                selectedFormat = infos[0].format;
            }
        }

        formatControl.setFormat(selectedFormat);
        return selectedFormat.getSize();
    }

    /**
     * The <tt>VideoListener</tt> which handles <tt>VideoEvent</tt>s from the
     * <tt>MediaDeviceSession</tt> of this instance and fires respective
     * <tt>VideoEvent</tt>s from this <tt>VideoMediaStream</tt> to its
     * <tt>VideoListener</tt>s.
     */
    private VideoListener deviceSessionVideoListener;

    /**
     * The facility which aids this instance in managing a list of
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this);

    /**
     * Initializes a new <tt>VideoMediaStreamImpl</tt> instance which will use
     * the specified <tt>MediaDevice</tt> for both capture and playback of video
     * exchanged via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the <tt>StreamConnector</tt> the new instance is to use
     * for sending and receiving video
     * @param device the <tt>MediaDevice</tt> the new instance is to use for
     * both capture and playback of video exchanged via the specified
     * <tt>StreamConnector</tt>
     */
    public VideoMediaStreamImpl(StreamConnector connector, MediaDevice device)
    {
        super(connector, device);
    }

    /**
     * Adds a specific <tt>VideoListener</tt> to this <tt>VideoMediaStream</tt>
     * in order to receive notifications when visual/video <tt>Component</tt>s
     * are being added and removed.
     * <p>
     * Adding a listener which has already been added does nothing i.e. it is
     * not added more than once and thus does not receive one and the same
     * <tt>VideoEvent</tt> multiple times.
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void addVideoListener(VideoListener listener)
    {
        videoNotifierSupport.addVideoListener(listener);
    }

    /**
     * Notifies this <tt>MediaStream</tt> that the <tt>MediaDevice</tt> (and
     * respectively the <tt>MediaDeviceSession</tt> with it) which this instance
     * uses for capture and playback of media has been changed. Makes sure that
     * the <tt>VideoListener</tt>s of this instance get <tt>VideoEvent</tt>s for
     * the new/current <tt>VideoMediaDeviceSession</tt> and not for the old one.
     *
     * @param oldValue the <tt>MediaDeviceSession</tt> with the
     * <tt>MediaDevice</tt> this instance used work with
     * @param newValue the <tt>MediaDeviceSession</tt> with the
     * <tt>MediaDevice</tt> this instance is to work with
     * @see MediaStreamImpl#deviceSessionChanged(MediaDeviceSession,
     * MediaDeviceSession)
     */
    @Override
    protected void deviceSessionChanged(
            MediaDeviceSession oldValue,
            MediaDeviceSession newValue)
    {
        super.deviceSessionChanged(oldValue, newValue);

        if ((oldValue instanceof VideoMediaDeviceSession)
                && (deviceSessionVideoListener != null))
            ((VideoMediaDeviceSession) oldValue)
                .removeVideoListener(deviceSessionVideoListener);
        if (newValue instanceof VideoMediaDeviceSession)
        {
            if (deviceSessionVideoListener == null)
                deviceSessionVideoListener = new VideoListener()
                {

                    /**
                     * Notifies that a visual <tt>Component</tt> representing
                     * video has been added to the provider this listener has
                     * been added to.
                     *
                     * @param e a <tt>VideoEvent</tt> describing the added
                     * visual <tt>Component</tt> representing video and the
                     * provider it was added into
                     * @see VideoListener#videoAdded(VideoEvent)
                     */
                    public void videoAdded(VideoEvent e)
                    {
                        if (fireVideoEvent(
                                e.getType(),
                                e.getVisualComponent(),
                                e.getOrigin()))
                            e.consume();
                    }

                    /**
                     * Notifies that a visual <tt>Component</tt> representing
                     * video has been removed from the provider this listener
                     * has been added to.
                     *
                     * @param e a <tt>VideoEvent</tt> describing the removed
                     * visual <tt>Component</tt> representing video and the
                     * provider it was removed from
                     * @see VideoListener#videoRemoved(VideoEvent)
                     */
                    public void videoRemoved(VideoEvent e)
                    {
                        if (fireVideoEvent(
                                e.getType(),
                                e.getVisualComponent(),
                                e.getOrigin()))
                            e.consume();
                    }
                };

            ((VideoMediaDeviceSession) newValue)
                .addVideoListener(deviceSessionVideoListener);
        }
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>VideoMediaStream</tt> about a specific type of change in the
     * availability of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this <tt>VideoMediaStream</tt>
     * @param origin {@link VideoEvent#LOCAL} if the origin of the video is
     * local (e.g. it is being locally captured); {@link VideoEvent#REMOTE} if
     * the origin of the video is remote (e.g. a remote peer is streaming it)
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    protected boolean fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        if (logger.isTraceEnabled())
            logger
                .trace(
                    "Firing VideoEvent with type "
                        + VideoEvent.typeToString(type)
                        + " and origin "
                        + VideoEvent.originToString(origin));

        return
            videoNotifierSupport.fireVideoEvent(type, visualComponent, origin);
    }

    /**
     * Returns a reference to the visual <tt>Component</tt> where video from the
     * remote peer is being rendered or <tt>null</tt> if no video is currently
     * rendered.
     *
     * @return a reference to the visual <tt>Component</tt> where video from
     * the remote peer is being rendered or <tt>null</tt> if no video is
     * currently rendered
     */
    public Component getVisualComponent()
    {
        MediaDeviceSession deviceSession = getDeviceSession();

        return
            (deviceSession instanceof VideoMediaDeviceSession)
                ? ((VideoMediaDeviceSession) deviceSession).getVisualComponent()
                : null;
    }

    /**
     * Registers {@link Constants#H264_RTP} with a specific <tt>RTPManager</tt>.
     *
     * @param rtpManager the <tt>RTPManager</tt> to register
     * {@link Constants#H264_RTP} with
     * @see MediaStreamImpl#registerCustomCodecFormats(RTPManager)
     */
    @Override
    protected void registerCustomCodecFormats(RTPManager rtpManager)
    {
        super.registerCustomCodecFormats(rtpManager);

        // if we have already registered custom formats and we are running JMF
        // we bail out.
        if (!FMJConditionals.REGISTER_FORMATS_WITH_EVERY_RTP_MANAGER
                && formatsRegisteredOnce)
            return;

        // We do not have formats to register right now.

        formatsRegisteredOnce = true;
    }

    /**
     * Removes a specific <tt>VideoListener</tt> from this
     * <tt>VideoMediaStream</tt> in order to have to no longer receive
     * notifications when visual/video <tt>Component</tt>s are being added and
     * removed.
     *
     * @param listener the <tt>VideoListener</tt> to no longer be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
    }
}
