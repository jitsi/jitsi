/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.codec.video.h264.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.impl.neomedia.transform.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.control.KeyFrameControl; // disambiguation
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaDeviceSession</tt> to add video-specific functionality.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class VideoMediaDeviceSession
    extends MediaDeviceSession
{

    /**
     * The <tt>Logger</tt> used by the <tt>VideoMediaDeviceSession</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(VideoMediaDeviceSession.class);

    /**
     * The image ID of the icon which is to be displayed as the local visual
     * <tt>Component</tt> depicting the streaming of the desktop of the local
     * peer to the remote peer.
     */
    private static final String DESKTOP_STREAMING_ICON
        = "impl.media.DESKTOP_STREAMING_ICON";

    /**
     * The <tt>KeyFrameControl</tt> used by this<tt>VideoMediaDeviceSession</tt>
     * as a means to control its key frame-related logic.
     */
    private KeyFrameControl keyFrameControl;

    /**
     * The <tt>KeyFrameRequester</tt> implemented by this
     * <tt>VideoMediaDeviceSession</tt> and provided to
     * {@link #keyFrameControl}.
     */
    private KeyFrameControl.KeyFrameRequester keyFrameRequester;

    /**
     * Local <tt>Player</tt> for the local video.
     */
    private Processor localPlayer = null;

    /**
     * Use or not RTCP feedback Picture Loss Indication.
     */
    private boolean usePLI = false;

    /**
     * Output size of the stream.
     *
     * It is used to specify a different size (generally lesser ones)
     * than the capture device provides. Typically one usage can be
     * in desktop streaming/sharing session when sender desktop is bigger
     * than remote ones.
     */
    private Dimension outputSize;

    /**
     * The <tt>RTPConnector</tt>.
     */
    private RTPTransformConnector rtpConnector = null;

    /**
     * Local SSRC.
     */
    private long localSSRC = -1;

    /**
     * Remote SSRC.
     */
    private long remoteSSRC = -1;

    /**
     * The <tt>SwScaler</tt> inserted into the codec chain of the
     * <tt>Player</tt> rendering the media received from the remote peer and
     * enabling the explicit setting of the video size.
     */
    private SwScaler playerScaler;

    /**
     * The facility which aids this instance in managing a list of
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this);

    /**
     * Initializes a new <tt>VideoMediaDeviceSession</tt> instance which is to
     * represent the work of a <tt>MediaStream</tt> with a specific video
     * <tt>MediaDevice</tt>.
     *
     * @param device the video <tt>MediaDevice</tt> the use of which by a
     * <tt>MediaStream</tt> is to be represented by the new instance
     */
    public VideoMediaDeviceSession(AbstractMediaDevice device)
    {
        super(device);
    }

    /**
     * Initializes a new <tt>VideoMediaDeviceSession</tt> instance which is to
     * represent the work of a <tt>MediaStream</tt> with a specific video
     * <tt>MediaDevice</tt>.
     *
     * @param session previous <tt>MediaDeviceSession</tt>
     * @param device the video <tt>MediaDevice</tt> the use of which by a
     * <tt>MediaStream</tt> is to be represented by the new instance
     */
    public VideoMediaDeviceSession(AbstractMediaDevice device,
            MediaDeviceSession session)
    {
        this(device);
        transferRenderingSession(session);
    }

    /**
     * Adds a specific <tt>VideoListener</tt> to this instance in order to
     * receive notifications when visual/video <tt>Component</tt>s are being
     * added and removed.
     * <p>
     * Adding a listener which has already been added does nothing i.e. it is
     * not added more than once and thus does not receive one and the same
     * <tt>VideoEvent</tt> multiple times.
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * instance
     */
    public void addVideoListener(VideoListener listener)
    {
        videoNotifierSupport.addVideoListener(listener);
    }

    /**
     * Creates the <tt>DataSource</tt> that this instance is to read captured
     * media from.
     *
     * @return the <tt>DataSource</tt> that this instance is to read captured
     * media from
     */
    @Override
    protected DataSource createCaptureDevice()
    {
        /*
         * Create our DataSource as SourceCloneable so we can use it to both
         * display local video and stream to remote peer.
         */
        DataSource captureDevice = super.createCaptureDevice();

        if (captureDevice != null)
        {
            MediaLocator locator = captureDevice.getLocator();
            String protocol = (locator == null) ? null : locator.getProtocol();
            float frameRate;
            DeviceConfiguration deviceConfig
                = NeomediaActivator
                    .getMediaServiceImpl()
                        .getDeviceConfiguration();

            // Apply the video size and the frame rate configured by the user.
            if (ImageStreamingAuto.LOCATOR_PROTOCOL.equals(protocol))
            {
                /*
                 * It is not clear at this time what the default frame rate for
                 * desktop streaming should be.
                 */
                frameRate = 10;
            }
            else
            {
                Dimension videoSize = deviceConfig.getVideoSize();

                // if we have an output size that is smaller than our current
                // settings, respect that size
                if(outputSize != null
                   && videoSize.height > outputSize.height
                   && videoSize.width > outputSize.width)
                    videoSize = outputSize;

                VideoMediaStreamImpl.selectVideoSize(
                        captureDevice,
                        videoSize.width, videoSize.height);

                frameRate = deviceConfig.getFrameRate();
            }

            FrameRateControl frameRateControl
                = (FrameRateControl)
                    captureDevice.getControl(FrameRateControl.class.getName());

            if (frameRateControl != null)
            {
                float maxSupportedFrameRate
                    = frameRateControl.getMaxSupportedFrameRate();

                if ((maxSupportedFrameRate > 0)
                        && (frameRate > maxSupportedFrameRate))
                    frameRate = maxSupportedFrameRate;
                if(frameRate > 0)
                    frameRateControl.setFrameRate(frameRate);
            }

            /*
             * FIXME PullBufferDataSource does not seem to be correctly cloned
             * by JMF.
             */
            if (!(captureDevice instanceof PullBufferDataSource))
            {
                DataSource cloneableDataSource
                    = Manager.createCloneableDataSource(captureDevice);

                if (cloneableDataSource != null)
                    captureDevice = cloneableDataSource;
            }
        }
        return captureDevice;
    }

    /**
     * Asserts that a specific <tt>MediaDevice</tt> is acceptable to be set as
     * the <tt>MediaDevice</tt> of this instance. Makes sure that its
     * <tt>MediaType</tt> is {@link MediaType#VIDEO}.
     *
     * @param device the <tt>MediaDevice</tt> to be checked for suitability to
     * become the <tt>MediaDevice</tt> of this instance
     * @see MediaDeviceSession#checkDevice(AbstractMediaDevice)
     */
    @Override
    protected void checkDevice(AbstractMediaDevice device)
    {
        if (!MediaType.VIDEO.equals(device.getMediaType()))
            throw new IllegalArgumentException("device");
    }

    /**
     * Releases the resources allocated by a specific <tt>Player</tt> in the
     * course of its execution and prepares it to be garbage collected. If the
     * specified <tt>Player</tt> is rendering video, notifies the
     * <tt>VideoListener</tt>s of this instance that its visual
     * <tt>Component</tt> is to no longer be used by firing a
     * {@link VideoEvent#VIDEO_REMOVED} <tt>VideoEvent</tt>.
     *
     * @param player the <tt>Player</tt> to dispose of
     * @see MediaDeviceSession#disposePlayer(Player)
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
        {
            fireVideoEvent(
                VideoEvent.VIDEO_REMOVED,
                visualComponent,
                VideoEvent.REMOTE);
        }
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this instance about
     * a specific type of change in the availability of a specific visual
     * <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this instance
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
        {
            logger.trace(
                    "Firing VideoEvent with type "
                        + VideoEvent.typeToString(type)
                        + " and origin "
                        + VideoEvent.originToString(origin));
        }

        return
            videoNotifierSupport.fireVideoEvent(type, visualComponent, origin);
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this instance about
     * a specific <tt>VideoEvent</tt>.
     *
     * @param videoEvent the <tt>VideoEvent</tt> to be fired to the
     * <tt>VideoListener</tt>s registered with this instance
     */
    protected void fireVideoEvent(VideoEvent videoEvent)
    {
        videoNotifierSupport.fireVideoEvent(videoEvent);
    }

    /**
     * Gets the JMF <tt>Format</tt> of the <tt>captureDevice</tt> of this
     * <tt>MediaDeviceSession</tt>.
     *
     * @return the JMF <tt>Format</tt> of the <tt>captureDevice</tt> of this
     * <tt>MediaDeviceSession</tt>
     */
    private Format getCaptureDeviceFormat()
    {
        DataSource captureDevice = getCaptureDevice();

        if (captureDevice != null)
        {
            FormatControl[] formatControls = null;

            if (captureDevice instanceof CaptureDevice)
            {
                formatControls
                    = ((CaptureDevice) captureDevice).getFormatControls();
            }
            if ((formatControls == null) || (formatControls.length == 0))
            {
                FormatControl formatControl
                    = (FormatControl)
                        captureDevice.getControl(FormatControl.class.getName());

                if (formatControl != null)
                    formatControls = new FormatControl[] { formatControl };
            }
            if (formatControls != null)
            {
                for (FormatControl formatControl : formatControls)
                {
                    Format format = formatControl.getFormat();

                    if (format != null)
                        return format;
                }
            }
        }
        return null;
    }

    /**
     * Get the local <tt>Player</tt> if it exists,
     * create it otherwise
     * @return local <tt>Player</tt>
     */
    private Player getLocalPlayer()
    {
        DataSource captureDevice = getCaptureDevice();
        DataSource dataSource
            = (captureDevice instanceof SourceCloneable)
                ? ((SourceCloneable) captureDevice).createClone()
                : null;

        /* create local player */
        if (localPlayer == null && dataSource != null)
        {
            Exception excpt = null;
            try
            {
                localPlayer = Manager.createProcessor(dataSource);
            }
            catch (Exception ex)
            {
                excpt = ex;
            }

            if(excpt == null)
            {
                localPlayer.addControllerListener(new ControllerListener()
                {
                    public void controllerUpdate(ControllerEvent event)
                    {
                        controllerUpdateForCreateLocalVisualComponent(event);
                    }
                });
                localPlayer.configure();
            }
            else
            {
                logger.error("Failed to connect to "
                        + MediaStreamImpl.toString(dataSource),
                    excpt);
            }
        }

        return localPlayer;
    }

    /**
     * Gets notified about <tt>ControllerEvent</tt>s generated by
     * {@link #localPlayer}.
     *
     * @param controllerEvent the <tt>ControllerEvent</tt> specifying the
     * <tt>Controller</tt> which is the source of the event and the very type of
     * the event
     */
    private void controllerUpdateForCreateLocalVisualComponent(
            ControllerEvent controllerEvent)
    {
        if (controllerEvent instanceof ConfigureCompleteEvent)
        {
            Processor player = (Processor)controllerEvent.getSourceController();

            /*
             * Use SwScaler for the scaling since it produces an image with
             * better quality and add the "flip" effect to the video.
             */
            TrackControl[] trackControls = player.getTrackControls();

            if ((trackControls != null) && (trackControls.length != 0))
                try
                {
                    for (TrackControl trackControl : trackControls)
                    {
                        trackControl.setCodecChain(
                                new Codec[] { new HFlip(), new SwScaler() });
                        break;
                    }
                }
                catch (UnsupportedPlugInException upiex)
                {
                    logger.warn(
                            "Failed to add SwScaler/VideoFlipEffect to " +
                            "codec chain", upiex);
                }

            // Turn the Processor into a Player.
            try
            {
                player.setContentDescriptor(null);
            }
            catch (NotConfiguredError nce)
            {
                logger.error(
                    "Failed to set ContentDescriptor of Processor",
                    nce);
            }

            player.realize();
        }
        else if (controllerEvent instanceof RealizeCompleteEvent)
        {
            Player player = (Player) controllerEvent.getSourceController();
            Component visualComponent = player.getVisualComponent();

            if (visualComponent != null)
            {
                if (fireVideoEvent(
                        VideoEvent.VIDEO_ADDED,
                        visualComponent,
                        VideoEvent.LOCAL))
                {
                    localVisualComponentConsumed(visualComponent, player);
                }
                else
                {
                    // No listener interested in our event so free resources.
                    if(localPlayer == player)
                        localPlayer = null;

                    player.stop();
                    player.deallocate();
                    player.close();
                }
            }
            player.start();
        }
    }

    /**
     * Creates the visual <tt>Component</tt> depicting the video being streamed
     * from the local peer to the remote peer.
     *
     * @return the visual <tt>Component</tt> depicting the video being streamed
     * from the local peer to the remote peer if it was immediately created or
     * <tt>null</tt> if it was not immediately created and it is to be delivered
     * to the currently registered <tt>VideoListener</tt>s in a
     * <tt>VideoEvent</tt> with type {@link VideoEvent#VIDEO_ADDED} and origin
     * {@link VideoEvent#LOCAL}
     */
    public Component createLocalVisualComponent()
    {
        /*
         * Displaying the currently streamed desktop is perceived as unnecessary
         * because the user sees the whole desktop anyway. Instead, a static
         * image will be presented.
         */
        DataSource captureDevice = getCaptureDevice();

        if (captureDevice != null)
        {
            MediaLocator locator = captureDevice.getLocator();

            if ((locator != null)
                    && ImageStreamingAuto.LOCATOR_PROTOCOL
                            .equals(locator.getProtocol()))
                return createLocalVisualComponentForDesktopStreaming();
        }

        /*
         * The visual Component to depict the video being streamed from the
         * local peer to the remote peer is created by JMF and its Player so it
         * is likely to take noticeably long time. Consequently, we will deliver
         * it to the currently registered VideoListeners in a VideoEvent after
         * returning from the call.
         */
        getLocalPlayer();
        return null;
    }

    /**
     * Creates the visual <tt>Component</tt> to depict the streaming of the
     * desktop of the local peer to the remote peer.
     *
     * @return the visual <tt>Component</tt> to depict the streaming of the
     * desktop of the local peer to the remote peer
     */
    private Component createLocalVisualComponentForDesktopStreaming()
    {
        ResourceManagementService resources = NeomediaActivator.getResources();
        ImageIcon icon = resources.getImage(DESKTOP_STREAMING_ICON);
        Canvas canvas;

        if (icon == null)
            canvas = null;
        else
        {
            final Image img = icon.getImage();

            canvas = new Canvas()
            {
                public static final long serialVersionUID = 0L;

                @Override
                public void paint(Graphics g)
                {
                    int width = getWidth();
                    int height = getHeight();

                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, width, height);

                    int imgWidth = img.getWidth(this);
                    int imgHeight = img.getHeight(this);

                    if ((imgWidth < 1) || (imgHeight < 1))
                        return;

                    boolean scale = false;
                    float scaleFactor = 1;

                    if (imgWidth > width)
                    {
                        scale = true;
                        scaleFactor = width / (float) imgWidth;
                    }
                    if (imgHeight > height)
                    {
                        scale = true;
                        scaleFactor
                            = Math.min(scaleFactor, height / (float) imgHeight);
                    }

                    int dstWidth;
                    int dstHeight;

                    if (scale)
                    {
                        dstWidth = Math.round(imgWidth * scaleFactor);
                        dstHeight = Math.round(imgHeight * scaleFactor);
                    }
                    else
                    {
                        dstWidth = imgWidth;
                        dstHeight = imgHeight;
                    }

                    int dstX = (width - dstWidth) / 2;
                    int dstY = (height - dstWidth) / 2;

                    g.drawImage(
                            img,
                            dstX, dstY, dstX + dstWidth, dstY + dstHeight,
                            0, 0, imgWidth, imgHeight,
                            this);
                }
            };

            Dimension iconSize
                = new Dimension(icon.getIconWidth(), icon.getIconHeight());

            canvas.setMaximumSize(iconSize);
            canvas.setPreferredSize(iconSize);

            /*
             * Set a clue so that we can recognize it if it gets received as an
             * argument to #disposeLocalVisualComponent().
             */
            canvas.setName(DESKTOP_STREAMING_ICON);

            fireVideoEvent(VideoEvent.VIDEO_ADDED, canvas, VideoEvent.LOCAL);
        }
        return canvas;
    }

    /**
     * Disposes the local visual <tt>Component</tt> of the local peer.
     *
     * @param component the local visual <tt>Component</tt> of the local peer to
     * dispose of
     */
    public void disposeLocalVisualComponent(Component component)
    {
        /*
         * Desktop streaming does not use a Player but a Canvas with its name
         * equals to the value of DESKTOP_STREAMING_ICON.
         */
        if ((component != null)
                && DESKTOP_STREAMING_ICON.equals(component.getName()))
        {
            fireVideoEvent(
                    VideoEvent.VIDEO_REMOVED,
                    component,
                    VideoEvent.LOCAL);
            return;
        }

        Player localPlayer = this.localPlayer;

        if (localPlayer != null)
            disposeLocalPlayer(localPlayer);
    }

    /**
     * Releases the resources allocated by a specific local <tt>Player</tt> in
     * the course of its execution and prepares it to be garbage collected. If
     * the specified <tt>Player</tt> is rendering video, notifies the
     * <tt>VideoListener</tt>s of this instance that its visual
     * <tt>Component</tt> is to no longer be used by firing a
     * {@link VideoEvent#VIDEO_REMOVED} <tt>VideoEvent</tt>.
     *
     * @param player the <tt>Player</tt> to dispose of
     * @see MediaDeviceSession#disposePlayer(Player)
     */
    protected void disposeLocalPlayer(Player player)
    {
        /*
         * The player is being disposed so let the (interested) listeners know
         * its Player#getVisualComponent() (if any) should be released.
         */
        Component visualComponent = getVisualComponent(player);

        if(localPlayer == player)
            localPlayer = null;

        player.stop();
        player.deallocate();
        player.close();

        if (visualComponent != null)
            fireVideoEvent(
                VideoEvent.VIDEO_REMOVED,
                visualComponent,
                VideoEvent.LOCAL);
    }

    /**
     * Returns the visual <tt>Component</tt> where video from the remote peer
     * is being rendered or <tt>null</tt> if no video is currently rendered.
     *
     * @return the visual <tt>Component</tt> where video from the remote peer
     * is being rendered or <tt>null</tt> if no video is currently rendered
     */
    public Component getVisualComponent()
    {
        Component visualComponent = null;

        /*
         * When we know (through means such as SDP) that we don't want to
         * receive, it doesn't make sense to wait for the remote peer to
         * acknowledge our desire. So we'll just stop depicting the video of the
         * remote peer regarldess of whether it stops or continues its sending.
         */
        if (getStartedDirection().allowsReceiving())
        {
            Player player = getPlayer();

            if (player != null)
                visualComponent = getVisualComponent(player);
        }
        return visualComponent;
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
    private static Component getVisualComponent(Player player)
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
     * Notifies this <tt>VideoMediaDeviceSession</tt> that a specific visual
     * <tt>Component</tt> which depicts video streaming from the local peer to
     * the remote peer and which has been created by a specific <tt>Player</tt>
     * has been delivered to the registered <tt>VideoListener</tt>s and at least
     * one of them has consumed it.
     *
     * @param visualComponent the visual <tt>Component</tt> depicting local
     * video which has been consumed by the registered <tt>VideoListener</tt>s
     * @param player the local <tt>Player</tt> which has created the specified
     * visual <tt>Component</tt>
     */
    private void localVisualComponentConsumed(
            Component visualComponent,
            Player player)
    {
    }

    /**
     * Notifies this instance that a specific <tt>Player</tt> of remote content
     * has generated a <tt>ConfigureCompleteEvent</tt>.
     *
     * @param player the <tt>Player</tt> which is the source of a
     * <tt>ConfigureCompleteEvent</tt>
     * @see MediaDeviceSession#playerConfigureComplete(Processor)
     */
    @Override
    protected void playerConfigureComplete(final Processor player)
    {
        super.playerConfigureComplete(player);

        TrackControl[] trackControls = player.getTrackControls();
        SwScaler playerScaler = null;

        if ((trackControls != null) && (trackControls.length != 0))
        {
            try
            {
                for (TrackControl trackControl : trackControls)
                {
                    /*
                     * Since SwScaler will scale any input size into the
                     * configured output size, we may never get SizeChangeEvent
                     * from the player. We'll generate it ourselves then.
                     */
                    playerScaler = new PlayerScaler(player);

                    /*
                     * For H.264, we will use RTCP feedback. For example, to
                     * tell the sender that we've missed a frame.
                     */
                    if ("h264/rtp".equalsIgnoreCase(
                            getFormat().getJMFEncoding()))
                    {
                        DePacketizer depacketizer = new DePacketizer();

                        if (keyFrameControl != null)
                            depacketizer.setKeyFrameControl(keyFrameControl);

                        trackControl.setCodecChain(
                                new Codec[] { depacketizer, playerScaler });
                    }
                    else
                    {
                        trackControl.setCodecChain(
                                new Codec[] { playerScaler });
                    }
                    break;
                }
            }
            catch (UnsupportedPlugInException upiex)
            {
                logger.error(
                        "Failed to add SwScaler or H.264 DePacketizer to codec"
                            + " chain",
                        upiex);
                playerScaler = null;
            }
        }
        this.playerScaler = playerScaler;
    }

    /**
     * Gets notified about <tt>ControllerEvent</tt>s generated by a specific
     * <tt>Player</tt> of remote content.
     *
     * @param event the <tt>ControllerEvent</tt> specifying the
     * <tt>Controller</tt> which is the source of the event and the very type of
     * the event
     * @see MediaDeviceSession#playerControllerUpdate(ControllerEvent)
     */
    @Override
    protected void playerControllerUpdate(ControllerEvent event)
    {
        super.playerControllerUpdate(event);

        /*
         * If SwScaler is in the chain and it forces a specific size of the
         * output, the SizeChangeEvents of the Player do not really notify about
         * changes in the size of the input. Besides, playerScaler will take
         * care of the events in such a case.
         */
        if ((event instanceof SizeChangeEvent)
                && ((playerScaler == null)
                        || (playerScaler.getOutputSize() == null)))
        {
            SizeChangeEvent sizeChangeEvent = (SizeChangeEvent) event;

            playerSizeChange(
                sizeChangeEvent.getSourceController(),
                sizeChangeEvent.getWidth(),
                sizeChangeEvent.getHeight());
        }
    }

    /**
     * Notifies this instance that a specific <tt>Player</tt> of remote content
     * has generated a <tt>RealizeCompleteEvent</tt>.
     *
     * @param player the <tt>Player</tt> which is the source of a
     * <tt>RealizeCompleteEvent</tt>.
     * @see MediaDeviceSession#playerRealizeComplete(Processor)
     */
    @Override
    protected void playerRealizeComplete(final Processor player)
    {
        super.playerRealizeComplete(player);

        Component visualComponent = getVisualComponent(player);

        if (visualComponent != null)
        {
            /*
             * SwScaler seems to be very good at scaling with respect to image
             * quality so use it for the scaling in the player replacing the
             * scaling it does upon rendering.
             */
            visualComponent.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    playerVisualComponentResized(player, e);
                }
            });

            fireVideoEvent(
                VideoEvent.VIDEO_ADDED,
                visualComponent,
                VideoEvent.REMOTE);
        }
    }

    /**
     * Notifies this instance that a specific <tt>Player</tt> of remote content
     * has generated a <tt>SizeChangeEvent</tt>.
     *
     * @param sourceController the <tt>Player</tt> which is the source of the
     * event
     * @param width the width reported in the event
     * @param height the height reported in the event
     * @see SizeChangeEvent
     */
    protected void playerSizeChange(
            final Controller sourceController,
            final int width,
            final int height)
    {
        /*
         * Invoking anything that is likely to change the UI in the Player
         * thread seems like a performance hit so bring it into the event
         * thread.
         */
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    playerSizeChange(sourceController, width, height);
                }
            });
            return;
        }

        Player player = (Player) sourceController;
        Component visualComponent = getVisualComponent(player);

        if (visualComponent != null)
        {
            fireVideoEvent(
                new SizeChangeVideoEvent(
                        this,
                        visualComponent,
                        SizeChangeVideoEvent.REMOTE,
                        width,
                        height));
        }
    }

    /**
     * Notifies this instance that the visual <tt>Component</tt> of a
     * <tt>Player</tt> rendering remote content has been resized.
     *
     * @param player the <tt>Player</tt> rendering remote content the visual
     * <tt>Component</tt> of which has been resized
     * @param e a <tt>ComponentEvent</tt> which specifies the resized
     * <tt>Component</tt>
     */
    private void playerVisualComponentResized(
            Processor player,
            ComponentEvent e)
    {
        if (playerScaler == null)
            return;

        Component visualComponent = e.getComponent();

        /*
         * When the visualComponent is not in an UI hierarchy, its size isn't
         * expected to be representative of what the user is seeing.
         */
        if (visualComponent.getParent() == null)
            return;

        Dimension outputSize = visualComponent.getSize();
        float outputWidth = outputSize.width;
        float outputHeight = outputSize.height;

        if ((outputWidth < 1) || (outputHeight < 1))
            return;

        /*
         * The size of the output video will be calculated so that it fits into
         * the visualComponent and the video aspect ratio is preserved. The
         * presumption here is that the inputFormat holds the video size with
         * the correct aspect ratio.
         */
        Format inputFormat = playerScaler.getInputFormat();

        if (inputFormat == null)
            return;

        Dimension inputSize = ((VideoFormat) inputFormat).getSize();

        if (inputSize == null)
            return;

        int inputWidth = inputSize.width;
        int inputHeight = inputSize.height;

        if ((inputWidth < 1) || (inputHeight < 1))
            return;

        // Preserve the aspect ratio.
        outputHeight = outputWidth * inputHeight / (float) inputWidth;

        // Fit the output video into the visualComponent.
        boolean scale = false;
        float widthRatio;
        float heightRatio;

        if (Math.abs(outputWidth - inputWidth) < 1)
        {
            scale = true;
            widthRatio = outputWidth / (float) inputWidth;
        }
        else
            widthRatio = 1;
        if (Math.abs(outputHeight - inputHeight) < 1)
        {
            scale = true;
            heightRatio = outputHeight / (float) inputHeight;
        }
        else
            heightRatio = 1;
        if (scale)
        {
            float scaleFactor = Math.min(widthRatio, heightRatio);

            outputWidth = inputWidth * scaleFactor;
            outputHeight = inputHeight * scaleFactor;
        }

        outputSize.width = (int) outputWidth;
        outputSize.height = (int) outputHeight;

        Dimension playerScalerOutputSize = playerScaler.getOutputSize();

        if (playerScalerOutputSize == null)
            playerScaler.setOutputSize(outputSize);
        else
        {
            /*
             * If we are not going to make much of a change, do not even bother
             * because any scaling in the Renderer will not be noticeable
             * anyway.
             */
            int outputWidthDelta
                = outputSize.width - playerScalerOutputSize.width;
            int outputHeightDelta
                = outputSize.height - playerScalerOutputSize.height;

            if ((outputWidthDelta < -1)
                    || (outputWidthDelta > 1)
                    || (outputHeightDelta < -1)
                    || (outputHeightDelta > 1))
            {
                playerScaler.setOutputSize(outputSize);
            }
        }
    }

    /**
     * Removes a specific <tt>VideoListener</tt> from this instance in order to
     * have to no longer receive notifications when visual/video
     * <tt>Component</tt>s are being added and removed.
     *
     * @param listener the <tt>VideoListener</tt> to no longer be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * instance
     */
    public void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
    }

    /**
     * Use or not RTCP feedback Picture Loss Indication.
     *
     * @param usePLI <tt>true</tt> to use PLI; otherwise, <tt>false</tt>
     */
    public void setRtcpFeedbackPLI(boolean usePLI)
    {
        if (this.usePLI != usePLI)
        {
            this.usePLI = usePLI;

            if (this.usePLI)
            {
                if (keyFrameRequester == null)
                {
                    keyFrameRequester
                        = new KeyFrameControl.KeyFrameRequester()
                        {
                            public boolean requestKeyFrame()
                            {
                                boolean requested = false;

                                if (VideoMediaDeviceSession.this.usePLI)
                                {
                                    try
                                    {
                                        new RTCPFeedbackPacket(
                                                    1,
                                                    206,
                                                    localSSRC,
                                                    remoteSSRC)
                                                .writeTo(
                                                        rtpConnector
                                                            .getControlOutputStream());
                                        requested = true;
                                    }
                                    catch (IOException ioe)
                                    {
                                        /*
                                         * Apart from logging the ioe, there are
                                         * not a lot of ways to handle it.
                                         */
                                    }
                                }
                                return requested;
                            }
                        };
                }
                if (keyFrameControl != null)
                    keyFrameControl.addKeyFrameRequester(-1, keyFrameRequester);
            }
            else if (keyFrameRequester != null)
            {
                if (keyFrameControl != null)
                    keyFrameControl.removeKeyFrameRequester(keyFrameRequester);
                keyFrameRequester = null;
            }
        }
    }

    /**
     * Sets the size of the output video.
     *
     * @param size the size of the output video
     */
    public void setOutputSize(Dimension size)
    {
        if((size != null && outputSize == null)
            || (size == null && outputSize != null)
            || (size != null && outputSize != null && !outputSize.equals(size)))
            outputsizeChanged = true;

        outputSize = size;
    }

    /**
     * Sets the <tt>RTPConnector</tt> that will be used to
     * initialize some codec for RTCP feedback.
     *
     * @param rtpConnector the RTP connector
     */
    public void setConnector(RTPTransformConnector rtpConnector)
    {
        this.rtpConnector = rtpConnector;
    }

    /**
     * Sets the <tt>KeyFrameControl</tt> to be used by this
     * <tt>VideoMediaDeviceSession</tt> as a means of control over its
     * key frame-related logic.
     *
     * @param keyFrameControl the <tt>KeyFrameControl</tt> to be used by this
     * <tt>VideoMediaDeviceSession</tt> as a means of control over its
     * key frame-related logic
     */
    public void setKeyFrameControl(KeyFrameControl keyFrameControl)
    {
        if (this.keyFrameControl != keyFrameControl)
        {
            if ((this.keyFrameControl != null) && (keyFrameRequester != null))
                this.keyFrameControl.removeKeyFrameRequester(keyFrameRequester);

            this.keyFrameControl = keyFrameControl;

            if ((this.keyFrameControl != null) && (keyFrameRequester != null))
                this.keyFrameControl.addKeyFrameRequester(-1, keyFrameRequester);
        }
    }

    /**
     * Set the local SSRC.
     *
     * @param localSSRC local SSRC
     */
    public void setLocalSSRC(long localSSRC)
    {
        this.localSSRC = localSSRC;
    }

    /**
     * Set the remote SSRC.
     *
     * @param remoteSSRC remote SSRC
     */
    public void setRemoteSSRC(long remoteSSRC)
    {
        this.remoteSSRC = remoteSSRC;
    }

    /**
     * Sets the <tt>MediaFormatImpl</tt> in which a specific <tt>Processor</tt>
     * producing media to be streamed to the remote peer is to output.
     *
     * @param processor the <tt>Processor</tt> to set the output
     * <tt>MediaFormatImpl</tt> of
     * @param mediaFormat the <tt>MediaFormatImpl</tt> to set on
     * <tt>processor</tt>
     * @see MediaDeviceSession#setProcessorFormat(Processor, MediaFormatImpl)
     */
    @Override
    protected void setProcessorFormat(
            Processor processor,
            MediaFormatImpl<? extends Format> mediaFormat)
    {
        Format format = mediaFormat.getFormat();

        if ("h263-1998/rtp".equalsIgnoreCase(format.getEncoding()))
        {
            /*
             * If no output size has been defined, then no SDP fmtp has been
             * found with QCIF, CIF, VGA or CUSTOM parameters. Let's default to
             * QCIF (176x144).
             */
            if (outputSize == null)
                outputSize = new Dimension(176, 144);
        }

        /*
         * Add a size in the output format. As VideoFormat has no setter, we
         * recreate the object. Also check whether capture device can output
         * such a size.
         */
        if ((outputSize != null)
                && (outputSize.width > 0)
                && (outputSize.height > 0))
        {
            Dimension deviceSize
                = ((VideoFormat) getCaptureDeviceFormat()).getSize();

            if ((deviceSize != null)
                    && ((deviceSize.width > outputSize.width)
                        || (deviceSize.height > outputSize.height)))
            {
                VideoFormat videoFormat = (VideoFormat) format;

                format
                    = new VideoFormat(
                            videoFormat.getEncoding(),
                            outputSize,
                            videoFormat.getMaxDataLength(),
                            videoFormat.getDataType(),
                            videoFormat.getFrameRate());
            }
            else
                outputSize = null;
        }
        else
            outputSize = null;

        super.setProcessorFormat(processor, mediaFormat);
    }

    /**
     * Sets the <tt>MediaFormatImpl</tt> of a specific <tt>TrackControl</tt> of
     * the <tt>Processor</tt> which produces the media to be streamed by this
     * <tt>MediaDeviceSession</tt> to the remote peer. Allows extenders to
     * override the set procedure and to detect when the JMF <tt>Format</tt> of
     * the specified <tt>TrackControl</tt> changes.
     *
     * @param trackControl the <tt>TrackControl</tt> to set the JMF
     * <tt>Format</tt> of
     * @param mediaFormat the <tt>MediaFormatImpl</tt> to be set on the
     * specified <tt>TrackControl</tt>. Though <tt>mediaFormat</tt> encapsulates
     * a JMF <tt>Format</tt>, <tt>format</tt> is to be set on the specified
     * <tt>trackControl</tt> because it may be more specific. In any case, the
     * two JMF <tt>Format</tt>s match. The <tt>MediaFormatImpl</tt> is provided
     * anyway because it carries additional information such as format
     * parameters.
     * @param format the JMF <tt>Format</tt> to be set on the specified
     * <tt>TrackControl</tt>. Though <tt>mediaFormat</tt> encapsulates a JMF
     * <tt>Format</tt>, the specified <tt>format</tt> is to be set on the
     * specified <tt>trackControl</tt> because it may be more specific than the
     * JMF <tt>Format</tt> of the <tt>mediaFormat</tt>
     * @return the JMF <tt>Format</tt> set on <tt>TrackControl</tt> after the
     * attempt to set the specified <tt>mediaFormat</tt> or <tt>null</tt> if the
     * specified <tt>format</tt> was found to be incompatible with
     * <tt>trackControl</tt>
     * @see MediaDeviceSession#setProcessorFormat(TrackControl, MediaFormatImpl,
     * Format)
     */
    @Override
    protected Format setProcessorFormat(
            TrackControl trackControl,
            MediaFormatImpl<? extends Format> mediaFormat,
            Format format)
    {
        JNIEncoder encoder = null;
        SwScaler scaler = null;
        int codecCount = 0;

        /*
         * For H.264 we will monitor RTCP feedback. For example, if we receive a
         * PLI/FIR message, we will send a keyframe.
         */
        if ("h264/rtp".equalsIgnoreCase(format.getEncoding()))
        {
            encoder = new JNIEncoder();

            // packetization-mode
            {
                Map<String, String> formatParameters
                    = mediaFormat.getFormatParameters();
                String packetizationMode
                    = (formatParameters == null)
                        ? null
                        : formatParameters.get(
                                JNIEncoder.PACKETIZATION_MODE_FMTP);

                encoder.setPacketizationMode(packetizationMode);
            }

            if (usePLI)
            {
                /*
                 * The H.264 encoder needs to be notified of RTCP feedback
                 * messages.
                 */
                try
                {
                    ((ControlTransformInputStream)
                            rtpConnector.getControlInputStream())
                        .addRTCPFeedbackListener(encoder);
                }
                catch (IOException ioe)
                {
                    logger.error("Error cannot get RTCP input stream", ioe);
                }
            }
            if (keyFrameControl != null)
                encoder.setKeyFrameControl(keyFrameControl);

            codecCount++;
        }

        if (outputSize != null)
        {
            /* We have been explicitly told to use a specified output size so
             * create a custom SwScaler that will scale and convert color spaces
             * in one call.
             */
            scaler = new SwScaler();
            scaler.setOutputSize(outputSize);
            codecCount++;
        }

        Codec[] codecs = new Codec[codecCount];

        codecCount = 0;
        if(scaler != null)
            codecs[codecCount++] = scaler;
        if(encoder != null)
            codecs[codecCount++] = encoder;

        if (codecCount != 0)
        {
            /* Add our custom SwScaler and possibly RTCP aware codec to the
             * codec chain so that it will be used instead of default.
             */
            try
            {
                trackControl.setCodecChain(codecs);
            }
            catch(UnsupportedPlugInException upiex)
            {
                logger.error(
                        "Failed to add SwScaler/JNIEncoder to codec chain",
                        upiex);
            }
        }

        return super.setProcessorFormat(trackControl, mediaFormat, format);
    }

    /**
     * Sets the <tt>MediaFormat</tt> in which this <tt>MediaDeviceSession</tt>
     * outputs the media captured by its <tt>MediaDevice</tt>.
     *
     * @param format the <tt>MediaFormat</tt> in which this
     * <tt>MediaDeviceSession</tt> is to output the media captured by its
     * <tt>MediaDevice</tt>
     */
    public void setFormat(MediaFormat format)
    {
        if(format instanceof VideoMediaFormat &&
            ((VideoMediaFormat)format).getFrameRate() != -1)
        {
            FrameRateControl frameRateControl
                = (FrameRateControl)
                    getCaptureDevice().getControl(FrameRateControl.class.getName());

            if (frameRateControl != null)
            {
                float frameRate = ((VideoMediaFormat)format).getFrameRate();

                float maxSupportedFrameRate
                    = frameRateControl.getMaxSupportedFrameRate();

                if ((maxSupportedFrameRate > 0)
                        && (frameRate > maxSupportedFrameRate))
                    frameRate = maxSupportedFrameRate;
                if(frameRate > 0)
                {
                    frameRateControl.setFrameRate(frameRate);
                }
            }
        }

        super.setFormat(format);
    }

    /**
     * Notifies this instance that the value of its <tt>startedDirection</tt>
     * property has changed from a specific <tt>oldValue</tt> to a specific
     * <tt>newValue</tt>.
     *
     * @param oldValue the <tt>MediaDirection</tt> which used to be the value of
     * the <tt>startedDirection</tt> property of this instance
     * @param newValue the <tt>MediaDirection</tt> which is the value of the
     * <tt>startedDirection</tt> property of this instance
     */
    @Override
    protected void startedDirectionChanged(
            MediaDirection oldValue,
            MediaDirection newValue)
    {
        super.startedDirectionChanged(oldValue, newValue);

        Player player = getPlayer();

        if (player == null)
            return;

        int state = player.getState();

        /*
         * The visual Component of a Player is safe to access and, respectively,
         * report through a VideoEvent only when the Player is Realized.
         */
        if (state < Player.Realized)
            return;

        if (newValue.allowsReceiving())
        {
            if (state != Player.Started)
            {
                player.start();

                Component visualComponent = getVisualComponent(player);

                if (visualComponent != null)
                {
                    fireVideoEvent(
                        VideoEvent.VIDEO_ADDED,
                        visualComponent,
                        VideoEvent.REMOTE);
                }
            }
        }
        else if (state > Processor.Configured)
        {
            Component visualComponent = getVisualComponent(player);

            player.stop();

            if (visualComponent != null)
            {
                fireVideoEvent(
                    VideoEvent.VIDEO_REMOVED,
                    visualComponent,
                    VideoEvent.REMOTE);
            }
        }
    }

    /**
     * Extends <tt>SwScaler</tt> in order to provide scaling with high quality
     * to a specific <tt>Player</tt> of remote video.
     */
    private class PlayerScaler
        extends SwScaler
    {
        /**
         * The last size reported in the form of a <tt>SizeChangeEvent</tt>.
         */
        private Dimension lastSize;

        /**
         * The <tt>Player</tt> into the codec chain of which this
         * <tt>SwScaler</tt> is set.
         */
        private final Player player;

        /**
         * Initializes a new <tt>PlayerScaler</tt> instance which is to provide
         * scaling with high quality to a specific <tt>Player</tt> of remote
         * video.
         *
         * @param player the <tt>Player</tt> of remote video into the codec
         * chain of which the new instance is to be set
         */
        public PlayerScaler(Player player)
        {
            super(true);

            this.player = player;
        }

        /**
         * Determines when the input video sizes changes and reports it as a
         * <tt>SizeChangeVideoEvent</tt> because <tt>Player</tt> is unable to
         * do it when this <tt>SwScaler</tt> is scaling to a specific
         * <tt>outputSize</tt>.
         *
         * @param input input buffer
         * @param output output buffer
         * @return the native <tt>PaSampleFormat</tt>
         * @see SwScaler#process(Buffer, Buffer)
         */
        @Override
        public int process(Buffer input, Buffer output)
        {
            int result = super.process(input, output);

            if (result == BUFFER_PROCESSED_OK)
            {
                Format inputFormat = getInputFormat();

                if (inputFormat != null)
                {
                    Dimension size = ((VideoFormat) inputFormat).getSize();

                    if ((size != null)
                            && ((lastSize == null) || !lastSize.equals(size)))
                    {
                        lastSize = size;
                        playerSizeChange(
                            player,
                            lastSize.width, lastSize.height);
                    }
                }
            }
            return result;
        }

        /**
         * Ensures that this <tt>SwScaler</tt> preserves the aspect ratio of its
         * input video when scaling.
         *
         * @param inputFormat format to set
         * @return format
         * @see SwScaler#setInputFormat(Format)
         */
        @Override
        public Format setInputFormat(Format inputFormat)
        {
            inputFormat = super.setInputFormat(inputFormat);
            if (inputFormat instanceof VideoFormat)
            {
                Dimension inputSize = ((VideoFormat) inputFormat).getSize();

                if ((inputSize != null) && (inputSize.width > 0))
                {
                    Dimension outputSize = getOutputSize();

                    if ((outputSize != null) && (outputSize.width > 0))
                    {
                        int outputHeight
                            = (int)
                                (outputSize.width
                                    * inputSize.height
                                    / (float) inputSize.width);
                        int outputHeightDelta
                            = outputHeight - outputSize.height;

                        if ((outputHeightDelta < -1) || (outputHeightDelta > 1))
                        {
                             outputSize.height = outputHeight;
                             setOutputSize(outputSize);
                        }
                    }
                }
            }
            return inputFormat;
        }
    }
}
