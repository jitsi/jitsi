/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.util.*;
import java.util.List;

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
     * Selects the <tt>VideoFormat</tt> from the list of supported formatts of a
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
     * The list of <tt>VideoListener</tt>s interested in changes in the
     * availability of visual <tt>Component</tt>s depicting video.
     */
    private final List<VideoListener> videoListeners
        = new ArrayList<VideoListener>();

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
     * <tt>VideoEvent</tt> multiple times
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * <tt>VideoMediaStream</tt>
     */
    public void addVideoListener(VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (videoListeners)
        {
            if (!videoListeners.contains(listener))
                videoListeners.add(listener);
        }
    }

    /**
     * Releases the resources allocated by a specific <tt>Player</tt> in the
     * course of its execution and prepares it to be garbage collected. If the
     * specified <tt>Player</tt> is rendering video, notifies the
     * <tt>VideoListener</tt>s of this <tt>VideoMediaStream</tt> that its visual
     * <tt>Component</tt> is to no longer be used by firing a
     * {@link VideoEvent#VIDEO_REMOVED} <tt>VideoEvent</tt>.
     *
     * @param player the <tt>Player</tt> to dispose of
     */
    @Override
    protected void disposePlayer(Player player)
    {

        /*
         * The player is being disposed so let the (interested) listeners know
         * its Player#getVisualComponent() (if any) should be released.
         */
        Component visualComponent = getVisualComponent(player);

        super.disposePlayer(player);

        if (visualComponent != null)
            fireVideoEvent(
                VideoEvent.VIDEO_REMOVED,
                visualComponent,
                VideoEvent.REMOTE);
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>VideoMediaStream</tt> about a specific type of change in the
     * availability of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depciting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this <tt>VideoMediaStream</tt>
     * @param origin
     */
    protected void fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        VideoListener[] listeners;

        synchronized (videoListeners)
        {
            listeners
                = videoListeners
                    .toArray(new VideoListener[videoListeners.size()]);
        }

        if (listeners.length > 0)
        {
            VideoEvent event
                = new VideoEvent(this, type, visualComponent, origin);

            for (VideoListener listener : listeners)
                switch (type)
                {
                    case VideoEvent.VIDEO_ADDED:
                        listener.videoAdded(event);
                        break;
                    case VideoEvent.VIDEO_REMOVED:
                        listener.videoRemoved(event);
                        break;
                }
        }
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
        synchronized (players)
        {
            for (Player player : players)
            {
                Component visualComponent = getVisualComponent(player);

                if (visualComponent != null)
                    return visualComponent;
            }
        }
        return null;
    }

    /**
     * Gets the visual <tt>Component</tt> of a specific <tt>Player</tt> if it
     * has one and ignores the failure to access it if the specified
     * <tt>Player</tt> is unrealized.
     *
     * @param player the <tt>Player</tt> to get the visual <tt>Component</tt> of
     * if it has one
     * @return the visual <tt>Component</tt> of the specified <tt>Player</tt> if
     * it has one; <tt>null</tt> if the specified <tt>Player</tt> does not have
     * a visual <tt>Component</tt> or the <tt>Player</tt> is unrealized
     */
    private Component getVisualComponent(Player player)
    {
        Component visualComponent;

        try
        {
            visualComponent = player.getVisualComponent();
        }
        catch (NotRealizedError e)
        {
            visualComponent = null;

            if (logger.isDebugEnabled())
                logger
                    .debug(
                        "Called Player#getVisualComponent() "
                            + "on Unrealized player "
                            + player,
                        e);
        }
        return visualComponent;
    }

    /**
     * Notifies this <tt>MediaStream</tt> that a specific <tt>Player</tt> of
     * remote content has generated a <tt>RealizeCompleteEvent</tt>. Allows
     * extenders to carry out additional processing on the <tt>Player</tt>.
     *
     * @param player the <tt>Player</tt> which is the source of a
     * <tt>RealizeCompleteEvent</tt>
     */
    @Override
    protected void realizeComplete(Player player)
    {
        super.realizeComplete(player);

        Component visualComponent = getVisualComponent(player);

        if (visualComponent != null)
            fireVideoEvent(
                VideoEvent.VIDEO_ADDED,
                visualComponent,
                VideoEvent.REMOTE);
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
        synchronized (videoListeners)
        {
            videoListeners.remove(listener);
        }
    }
}
