/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.RGBFormat;
import javax.media.protocol.*;
import javax.swing.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.impl.neomedia.videoflip.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Implements <tt>MediaService</tt> for JMF.
 *
 * @author Lubomir Marinov
 * @author Dmitri Melnikov
 */
public class MediaServiceImpl
    implements MediaService
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(MediaServiceImpl.class);

    /**
     * With this property video support can be disabled (enabled by default).
     */
    public static final String DISABLE_VIDEO_SUPPORT_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.DISABLE_VIDEO_SUPPORT";

    /**
     * The value of the <tt>devices</tt> property of <tt>MediaServiceImpl</tt>
     * when no <tt>MediaDevice</tt>s are available. Explicitly defined in order
     * to reduce unnecessary allocations.
     */
    private static final List<MediaDevice> EMPTY_DEVICES
        = Collections.emptyList();

    /**
     * The <tt>CaptureDevice</tt> user choices such as the default audio and
     * video capture devices.
     */
    private final DeviceConfiguration deviceConfiguration
        = new DeviceConfiguration();

    /**
     * The list of audio <tt>MediaDevice</tt>s reported by this instance when
     * its {@link MediaService#getDevices(MediaType, MediaUseCase)} method is
     * called with an argument {@link MediaType#AUDIO}.
     */
    private final List<MediaDeviceImpl> audioDevices
        = new ArrayList<MediaDeviceImpl>();

    /**
     * The format-related user choices such as the enabled and disabled codecs
     * and the order of their preference.
     */
    private final EncodingConfiguration encodingConfiguration
        = new EncodingConfiguration();

    /**
     * The <tt>MediaFormatFactory</tt> through which <tt>MediaFormat</tt>
     * instances may be created for the purposes of working with the
     * <tt>MediaStream</tt>s created by this <tt>MediaService</tt>.
     */
    private MediaFormatFactory formatFactory;

    /**
     * The one and only <tt>MediaDevice</tt> instance with
     * <tt>MediaDirection</tt> not allowing sending and <tt>MediaType</tt> equal
     * to <tt>AUDIO</tt>.
     */
    private MediaDevice nonSendAudioDevice;

    /**
     * The one and only <tt>MediaDevice</tt> instance with
     * <tt>MediaDirection</tt> not allowing sending and <tt>MediaType</tt> equal
     * to <tt>VIDEO</tt>.
     */
    private MediaDevice nonSendVideoDevice;

    /**
     * The list of video <tt>MediaDevice</tt>s reported by this instance when
     * its {@link MediaService#getDevices(MediaType, MediaUseCase)} method is
     * called with an argument {@link MediaType#VIDEO}.
     */
    private final List<MediaDeviceImpl> videoDevices
        = new ArrayList<MediaDeviceImpl>();

    /**
     * A {@link Map} that binds indicates whatever preferences this
     * media service implementation may have for the RTP payload type numbers
     * that get dynamically assigned to {@link MediaFormat}s with no static
     * payload type. The method is useful for formats such as "telephone-event"
     * for example that is statically assigned the 101 payload type by some
     * legacy systems. Signalling protocol implementations such as SIP and XMPP
     * should make sure that, whenever this is possible, they assign to formats
     * the dynamic payload type returned in this {@link Map}.
     */
    private static Map<MediaFormat, Byte> dynamicPayloadTypePreferences;

    /**
     * The volume control of the media service playback.
     */
    private static OutputVolumeControl outputVolumeControl;

    /**
     * The volume control of the media service capture.
     */
    private static InputVolumeControl inputVolumeControl;

    /**
     * Lock to protected reinitialization of video devices.
     */
    private final Object reinitVideoLock = new Object();

    /**
     * Create a <tt>MediaStream</tt> which will use a specific
     * <tt>MediaDevice</tt> for capture and playback of media. The new instance
     * will not have a <tt>StreamConnector</tt> at the time of its construction
     * and a <tt>StreamConnector</tt> will be specified later on in order to
     * enable the new instance to send and receive media.
     *
     * @param device the <tt>MediaDevice</tt> to be used by the new instance for
     * capture and playback of media
     * @return a newly-created <tt>MediaStream</tt> which will use the specified
     * <tt>device</tt> for capture and playback of media
     * @see MediaService#createMediaStream(MediaDevice)
     */
    public MediaStream createMediaStream(MediaDevice device)
    {
        return createMediaStream(null, device);
    }

    /**
     * Creates a new <tt>MediaStream</tt> instance which will use the specified
     * <tt>MediaDevice</tt> for both capture and playback of media exchanged
     * via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the <tt>StreamConnector</tt> that the new
     * <tt>MediaStream</tt> instance is to use for sending and receiving media
     * @param device the <tt>MediaDevice</tt> that the new <tt>MediaStream</tt>
     * instance is to use for both capture and playback of media exchanged via
     * the specified <tt>connector</tt>
     * @return a new <tt>MediaStream</tt> instance
     * @see MediaService#createMediaStream(StreamConnector, MediaDevice)
     */
    public MediaStream createMediaStream(
            StreamConnector connector,
            MediaDevice device)
    {
        return createMediaStream(connector, device, null);
    }

    /**
     * Creates a new <tt>MediaStream</tt> instance which will use the specified
     * <tt>MediaDevice</tt> for both capture and playback of media exchanged
     * via the specified <tt>StreamConnector</tt>.
     *
     * @param connector the <tt>StreamConnector</tt> that the new
     * <tt>MediaStream</tt> instance is to use for sending and receiving media
     * @param device the <tt>MediaDevice</tt> that the new <tt>MediaStream</tt>
     * instance is to use for both capture and playback of media exchanged via
     * the specified <tt>connector</tt>
     * @param zrtpControl a control which is already created, used to control
     *        the zrtp operations.
     *
     * @return a new <tt>MediaStream</tt> instance
     * @see MediaService#createMediaStream(StreamConnector, MediaDevice)
     */
    public MediaStream createMediaStream(
            StreamConnector connector,
            MediaDevice device,
            ZrtpControl zrtpControl)
    {
        switch (device.getMediaType())
        {
        case AUDIO:
            return new AudioMediaStreamImpl(connector, device,
                (ZrtpControlImpl)zrtpControl);
        case VIDEO:
            return new VideoMediaStreamImpl(connector, device,
                (ZrtpControlImpl)zrtpControl);
        default:
            return null;
        }
    }

    /**
     * Creates a new <tt>MediaDevice</tt> which uses a specific
     * <tt>MediaDevice</tt> to capture and play back media and performs mixing
     * of the captured media and the media played back by any other users of the
     * returned <tt>MediaDevice</tt>. For the <tt>AUDIO</tt> <tt>MediaType</tt>,
     * the returned device is commonly referred to as an audio mixer. The
     * <tt>MediaType</tt> of the returned <tt>MediaDevice</tt> is the same as
     * the <tt>MediaType</tt> of the specified <tt>device</tt>.
     *
     * @param device the <tt>MediaDevice</tt> which is to be used by the
     * returned <tt>MediaDevice</tt> to actually capture and play back media
     * @return a new <tt>MediaDevice</tt> instance which uses <tt>device</tt> to
     * capture and play back media and performs mixing of the captured media and
     * the media played back by any other users of the returned
     * <tt>MediaDevice</tt> instance
     * @see MediaService#createMixer(MediaDevice)
     */
    public MediaDevice createMixer(MediaDevice device)
    {
        if (MediaType.AUDIO.equals(device.getMediaType()))
            return new AudioMixerMediaDevice((AudioMediaDeviceImpl) device);
        /*
         * TODO If we do not support mixing, should we return null or rather a
         * MediaDevice with INACTIVE MediaDirection?
         */
        return null;
    }

    /**
     * Gets the default <tt>MediaDevice</tt> for the specified
     * <tt>MediaType</tt>.
     *
     * @param mediaType a <tt>MediaType</tt> value indicating the type of media
     * to be handled by the <tt>MediaDevice</tt> to be obtained
     * @param useCase the <tt>MediaUseCase</tt> to obtain the
     * <tt>MediaDevice</tt> list for
     * @return the default <tt>MediaDevice</tt> for the specified
     * <tt>mediaType</tt> if such a <tt>MediaDevice</tt> exists; otherwise,
     * <tt>null</tt>
     * @see MediaService#getDefaultDevice(MediaType, MediaUseCase)
     */
    public MediaDevice getDefaultDevice(
            MediaType mediaType,
            MediaUseCase useCase)
    {
        CaptureDeviceInfo captureDeviceInfo;

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfo
                = getDeviceConfiguration().getAudioCaptureDevice();
            break;
        case VIDEO:
            captureDeviceInfo
                    = getDeviceConfiguration().getVideoCaptureDevice(useCase);
            break;
        default:
            captureDeviceInfo = null;
            break;
        }

        MediaDevice defaultDevice = null;

        if (captureDeviceInfo != null)
        {
            for (MediaDevice device : getDevices(mediaType, useCase))
            {
                if ((device instanceof MediaDeviceImpl)
                        && captureDeviceInfo.equals(
                                ((MediaDeviceImpl) device)
                                    .getCaptureDeviceInfo()))
                {
                    defaultDevice = device;
                    break;
                }
            }
        }
        if (defaultDevice == null)
        {
            switch (mediaType)
            {
            case AUDIO:
                defaultDevice = getNonSendAudioDevice();
                break;
            case VIDEO:
                defaultDevice = getNonSendVideoDevice();
                break;
            default:
                /*
                 * There is no MediaDevice with direction which does not allow
                 * sending and mediaType other than AUDIO and VIDEO.
                 */
                break;
            }
        }

        //Don't use the device in case the user has disabled all codecs for that
        //kind of media.
        if ((defaultDevice != null)
                    && (defaultDevice.getSupportedFormats().isEmpty()))
        {
            defaultDevice = null;
        }
        return defaultDevice;
    }

    /**
     * Gets the <tt>CaptureDevice</tt> user choices such as the default audio
     * and video capture devices.
     *
     * @return the <tt>CaptureDevice</tt> user choices such as the default audio
     * and video capture devices.
     */
    public DeviceConfiguration getDeviceConfiguration()
    {
        return deviceConfiguration;
    }

    /**
     * Gets a list of the <tt>MediaDevice</tt>s known to this
     * <tt>MediaService</tt> and handling the specified <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> to obtain the
     * <tt>MediaDevice</tt> list for
     * @param useCase the <tt>MediaUseCase</tt> to obtain the
     * <tt>MediaDevice</tt> list for
     * @return a new <tt>List</tt> of <tt>MediaDevice</tt>s known to this
     * <tt>MediaService</tt> and handling the specified <tt>MediaType</tt>. The
     * returned <tt>List</tt> is a copy of the internal storage and,
     * consequently, modifications to it do not affect this instance. Despite
     * the fact that a new <tt>List</tt> instance is returned by each call to
     * this method, the <tt>MediaDevice</tt> instances are the same if they are
     * still known to this <tt>MediaService</tt> to be available.
     * @see MediaService#getDevices(MediaType, MediaUseCase)
     */
    public List<MediaDevice> getDevices(MediaType mediaType,
            MediaUseCase useCase)
    {
        CaptureDeviceInfo[] captureDeviceInfos;
        List<MediaDeviceImpl> privateDevices;

        if(mediaType == MediaType.VIDEO)
        {
            /* in case a video capture device has been removed from system
             * (i.e. webcam, monitors, ...), rescan video capture devices
             */
            synchronized(reinitVideoLock)
            {
                getDeviceConfiguration().reinitializeVideo();
            }
        }

        switch (mediaType)
        {
        case AUDIO:
            captureDeviceInfos
                = getDeviceConfiguration().getAvailableAudioCaptureDevices();
            privateDevices = audioDevices;
            break;
        case VIDEO:
            captureDeviceInfos
                = getDeviceConfiguration().getAvailableVideoCaptureDevices(
                        useCase);
            privateDevices = videoDevices;
            break;
        default:
            /*
             * MediaService does not understand MediaTypes other than AUDIO and
             * VIDEO.
             */
            return EMPTY_DEVICES;
        }

        List<MediaDevice> publicDevices;

        synchronized (privateDevices)
        {
            if ((captureDeviceInfos == null)
                    || (captureDeviceInfos.length == 0))
                privateDevices.clear();
            else
            {
                Iterator<MediaDeviceImpl> deviceIter
                    = privateDevices.iterator();

                while (deviceIter.hasNext())
                {
                    CaptureDeviceInfo captureDeviceInfo
                        = deviceIter.next().getCaptureDeviceInfo();
                    boolean deviceIsFound = false;

                    for (int i = 0; i < captureDeviceInfos.length; i++)
                        if (captureDeviceInfo.equals(captureDeviceInfos[i]))
                        {
                            deviceIsFound = true;
                            captureDeviceInfos[i] = null;
                            break;
                        }
                    if (!deviceIsFound)
                        deviceIter.remove();
                }

                for (CaptureDeviceInfo captureDeviceInfo : captureDeviceInfos)
                {
                    if (captureDeviceInfo == null)
                        continue;

                    MediaDeviceImpl device;

                    switch (mediaType)
                    {
                    case AUDIO:
                        device = new AudioMediaDeviceImpl(captureDeviceInfo);
                        break;
                    case VIDEO:
                        device
                            = new MediaDeviceImpl(captureDeviceInfo, mediaType);
                        break;
                    default:
                        device = null;
                        break;
                    }
                    if (device != null)
                        privateDevices.add(device);
                }
            }

            publicDevices = new ArrayList<MediaDevice>(privateDevices);
        }

        /*
         * If there are no MediaDevice instances of the specified mediaType,
         * make sure that there is at least one MediaDevice which does not allow
         * sending.
         */
        if (publicDevices.isEmpty())
        {
            MediaDevice nonSendDevice;

            switch (mediaType)
            {
            case AUDIO:
                nonSendDevice = getNonSendAudioDevice();
                break;
            case VIDEO:
                nonSendDevice = getNonSendVideoDevice();
                break;
            default:
                /*
                 * There is no MediaDevice with direction not allowing sending
                 * and mediaType other than AUDIO and VIDEO.
                 */
                nonSendDevice = null;
                break;
            }
            if (nonSendDevice != null)
                publicDevices.add(nonSendDevice);
        }

        return publicDevices;
    }

    /**
     * Gets the format-related user choices such as the enabled and disabled
     * codecs and the order of their preference.
     *
     * @return the format-related user choices such as the enabled and disabled
     * codecs and the order of their preference
     */
    public EncodingConfiguration getEncodingConfiguration()
    {
        return encodingConfiguration;
    }

    /**
     * Gets the <tt>MediaFormatFactory</tt> through which <tt>MediaFormat</tt>
     * instances may be created for the purposes of working with the
     * <tt>MediaStream</tt>s created by this <tt>MediaService</tt>.
     *
     * @return the <tt>MediaFormatFactory</tt> through which
     * <tt>MediaFormat</tt> instances may be created for the purposes of working
     * with the <tt>MediaStream</tt>s created by this <tt>MediaService</tt>
     * @see MediaService#getFormatFactory()
     */
    public MediaFormatFactory getFormatFactory()
    {
        if (formatFactory == null)
            formatFactory = new MediaFormatFactoryImpl();
        return formatFactory;
    }

    /**
     * Gets the one and only <tt>MediaDevice</tt> instance with
     * <tt>MediaDirection</tt> not allowing sending and <tt>MediaType</tt> equal
     * to <tt>AUDIO</tt>.
     *
     * @return the one and only <tt>MediaDevice</tt> instance with
     * <tt>MediaDirection</tt> not allowing sending and <tt>MediaType</tt> equal
     * to <tt>AUDIO</tt>
     */
    private MediaDevice getNonSendAudioDevice()
    {
        if (nonSendAudioDevice == null)
            nonSendAudioDevice = new AudioMediaDeviceImpl();
        return nonSendAudioDevice;
    }

    /**
     * Gets the one and only <tt>MediaDevice</tt> instance with
     * <tt>MediaDirection</tt> not allowing sending and <tt>MediaType</tt> equal
     * to <tt>VIDEO</tt>.
     *
     * @return the one and only <tt>MediaDevice</tt> instance with
     * <tt>MediaDirection</tt> not allowing sending and <tt>MediaType</tt> equal
     * to <tt>VIDEO</tt>
     */
    private MediaDevice getNonSendVideoDevice()
    {
        if (nonSendVideoDevice == null)
            nonSendVideoDevice = new MediaDeviceImpl(MediaType.VIDEO);
        return nonSendVideoDevice;
    }

    /**
     * Starts this <tt>MediaService</tt> implementation and thus makes it
     * operational.
     */
    void start()
    {
        deviceConfiguration.initialize();
        encodingConfiguration.initializeFormatPreferences();
        encodingConfiguration.registerCustomPackages();
        encodingConfiguration.registerCustomCodecs();
    }

    /**
     * Stops this <tt>MediaService</tt> implementation and thus signals that its
     * utilization should cease.
     */
    void stop()
    {
    }

    /**
     * Creates <tt>ZrtpControl</tt> used to control all zrtp options
     * on particular stream.
     *
     * @return ZrtpControl instance.
     */
    public ZrtpControl createZrtpControl()
    {
        return new ZrtpControlImpl();
    }

    /**
     * Returns the control that handles current playback levels.
     *
     * @return the volume playback control.
     */
    public OutputVolumeControl getOutputVolumeControl()
    {
        if(outputVolumeControl == null)
            outputVolumeControl = new OutputVolumeControlImpl();

        return outputVolumeControl;
    }

    /**
     * Returns the control that handles current capture levels.
     *
     * @return the volume capture control.
     */
    public InputVolumeControl getInputVolumeControl()
    {
        if(inputVolumeControl == null)
            inputVolumeControl = new InputVolumeControlImpl();

        return inputVolumeControl;
    }

    /**
     * Get available screens.
     *
     * @return screens
     */
    public List<ScreenDevice> getAvailableScreenDevices()
    {
        ScreenDevice screens[] = ScreenDeviceImpl.getAvailableScreenDevice();
        List<ScreenDevice> screenList;

        if (screens != null)
        {
            screenList = new ArrayList<ScreenDevice>(screens.length);
            screenList.addAll(Arrays.asList(screens));
        }
        else
            screenList = new ArrayList<ScreenDevice>();
        return screenList;
    }

    /**
     * Get default screen device.
     *
     * @return default screen device
     */
    public ScreenDevice getDefaultScreenDevice()
    {
        List<ScreenDevice> screens = getAvailableScreenDevices();
        int width = 0;
        int height = 0;
        ScreenDevice best = null;

        for (ScreenDevice sc : screens)
        {
            java.awt.Dimension res = sc.getSize();

            if ((res != null)
                    && ((width < res.width) || (height < res.height)))
            {
                width = res.width;
                height = res.height;
                best = sc;
            }
        }
        return best;
    }

    /**
     * Creates a new <tt>Recorder</tt> instance that can be used to record a
     * call which captures and plays back media using a specific
     * <tt>MediaDevice</tt>.
     *
     * @param device the <tt>MediaDevice</tt> which is used for media capture
     * and playback by the call to be recorded
     * @return a new <tt>Recorder</tt> instance that can be used to record a
     * call which captures and plays back media using the specified
     * <tt>MediaDevice</tt>
     * @see MediaService#createRecorder(MediaDevice)
     */
    public Recorder createRecorder(MediaDevice device)
    {
        if (device instanceof AudioMixerMediaDevice)
            return new RecorderImpl((AudioMixerMediaDevice) device);
        else
            return null;
    }

    /**
     * Returns a {@link Map} that binds indicates whatever preferences this
     * media service implementation may have for the RTP payload type numbers
     * that get dynamically assigned to {@link MediaFormat}s with no static
     * payload type. The method is useful for formats such as "telephone-event"
     * for example that is statically assigned the 101 payload type by some
     * legacy systems. Signalling protocol implementations such as SIP and XMPP
     * should make sure that, whenever this is possible, they assign to formats
     * the dynamic payload type returned in this {@link Map}.
     *
     * @return a {@link Map} binding some formats to a preferred dynamic RTP
     * payload type number.
     */
    public Map<MediaFormat, Byte> getDynamicPayloadTypePreferences()
    {
        if(dynamicPayloadTypePreferences == null)
        {
            dynamicPayloadTypePreferences = new HashMap<MediaFormat, Byte>();

            MediaFormat telephoneEvent
                = MediaUtils.getMediaFormat("telephone-event", 8000);

            dynamicPayloadTypePreferences.put(telephoneEvent, (byte)101);
        }
        return dynamicPayloadTypePreferences;
    }

    /**
     * Creates a preview component for the specified device(video device) used
     * to show video preview from that device.
     *
     * @param device the video device
     * @param preferredWidth the width we prefer for the component
     * @param preferredHeight the height we prefer for the component
     * @return the preview component.
     */
    public Object getVideoPreviewComponent(
            MediaDevice device,
            int preferredWidth, int preferredHeight)
    {
        JLabel noPreview =
        new JLabel(NeomediaActivator.getResources().getI18NString(
                "impl.media.configform.NO_PREVIEW"));
        noPreview.setHorizontalAlignment(SwingConstants.CENTER);
        noPreview.setVerticalAlignment(SwingConstants.CENTER);
        final JComponent videoContainer
                = new VideoContainer(noPreview);

        videoContainer.setPreferredSize(
                new Dimension(preferredWidth, preferredHeight));
        videoContainer.setMaximumSize(
                new Dimension(preferredWidth, preferredHeight));

        try
        {
            if (device == null ||
                    ((MediaDeviceImpl)device).getCaptureDeviceInfo() == null)
            {
                return videoContainer;
            }

            DataSource dataSource = Manager.createDataSource(
                ((MediaDeviceImpl)device).getCaptureDeviceInfo().getLocator());

            /*
             * Don't let the size be uselessly small just because the
             * videoContainer has too small a preferred size.
             */
            if ((preferredWidth < 128) || (preferredHeight < 96))
            {
                preferredHeight = 128;
                preferredWidth = 96;
            }
            VideoMediaStreamImpl.selectVideoSize(
                    dataSource,
                    preferredWidth, preferredHeight);

            // A Player is documented to be created on a connected DataSource.
            dataSource.connect();

            final MediaLocator locator = dataSource.getLocator();

            Processor player = Manager.createProcessor(dataSource);

            player.addControllerListener(new ControllerListener()
            {
                public void controllerUpdate(ControllerEvent event)
                {
                    controllerUpdateForPreview(event, videoContainer, locator);
                }
            });
            player.configure();

            return videoContainer;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Listens and shows the video in the video container when needed.
     * @param event the event when player has ready visual component.
     * @param videoContainer the container.
     * @param locator input DataSource locator
     */
    private static void controllerUpdateForPreview(ControllerEvent event,
        JComponent videoContainer, MediaLocator locator)
    {
        if (event instanceof ConfigureCompleteEvent)
        {
            Processor player = (Processor) event.getSourceController();

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
                        Codec codecs[] = null;
                        SwScaler scaler = new SwScaler();

                        if(locator.getProtocol().equals(
                                ImageStreamingAuto.LOCATOR_PROTOCOL))
                        {
                            // do not flip desktop
                            codecs = new Codec[1];
                            codecs[0] = scaler;
                        }
                        else
                        {
                            VideoFlipEffect flipEffect = new VideoFlipEffect();
                            codecs = new Codec[2];
                            codecs[0] = flipEffect;
                            codecs[1] = scaler;
                        }

                        trackControl.setCodecChain(codecs);
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
        else if (event instanceof RealizeCompleteEvent)
        {
            Player player = (Player) event.getSourceController();
            Component video = player.getVisualComponent();

            showPreview(videoContainer, video, player);
        }
    }

    /**
     * Shows the preview panel.
     * @param previewContainer the container
     * @param preview the preview component.
     * @param player the player.
     */
    private static void showPreview(final JComponent previewContainer,
        final Component preview, final Player player)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    showPreview(previewContainer, preview, player);
                }
            });
            return;
        }

        previewContainer.removeAll();

        if (preview != null)
        {
            HierarchyListener hierarchyListener = new HierarchyListener()
            {
                private Window window;

                private WindowListener windowListener;

                public void dispose()
                {
                    if (windowListener != null)
                    {
                        if (window != null)
                        {
                            window.removeWindowListener(windowListener);
                            window = null;
                        }
                        windowListener = null;
                    }
                    preview.removeHierarchyListener(this);

                    disposePlayer(player);

                    /*
                     * We've just disposed the player which created the preview
                     * component so the preview component is of no use
                     * regardless of whether the Media configuration form will
                     * be redisplayed or not. And since the preview component
                     * appears to be a huge object even after its player is
                     * disposed, make sure to not reference it.
                     */
                    previewContainer.remove(preview);
                }

                public void hierarchyChanged(HierarchyEvent event)
                {
                    if ((event.getChangeFlags()
                                    & HierarchyEvent.DISPLAYABILITY_CHANGED)
                                == 0)
                        return;

                    if (!preview.isDisplayable())
                    {
                        dispose();
                        return;
                    }
                    else
                    {
                        player.start();
                    }

                    if (windowListener == null)
                    {
                        window = SwingUtilities.windowForComponent(preview);
                        if (window != null)
                        {
                            windowListener = new WindowAdapter()
                            {
                                @Override
                                public void windowClosing(WindowEvent event)
                                {
                                    dispose();
                                }
                            };
                            window.addWindowListener(windowListener);
                        }
                    }
                }
            };
            preview.addHierarchyListener(hierarchyListener);

            previewContainer.add(preview);

            previewContainer.revalidate();
            previewContainer.repaint();
        }
        else
            disposePlayer(player);
    }

    /**
     * Dispose the player used for the preview.
     * @param player the player.
     */
    private static void disposePlayer(Player player)
    {
        player.stop();
        player.deallocate();
        player.close();
    }

    /**
     * Get a <tt>MediaDevice</tt> for a part of desktop streaming/sharing.
     *
     * @param width width of the part
     * @param height height of the part
     * @param x origin of the x coordinate (relative to the full desktop)
     * @param y origin of the y coordinate (relative to the full desktop)
     * @return <tt>MediaDevice</tt> representing the part of desktop or null
     * if problem
     */
    public MediaDevice getMediaDeviceForPartialDesktopStreaming(
            int width, int height, int x, int y)
    {
        MediaDevice device = null;
        String name = "Partial desktop streaming";
        Dimension size = null;
        int multiple = 0;
        Point p = new Point(x, y);
        ScreenDevice dev = getScreenForPoint(p);
        int display = -1;

        if(dev != null)
        {
            display = dev.getIndex();
        }
        else
        {
            return null;
        }

        /* on Mac OS X, width have to be a multiple of 16 */
        if(OSUtils.IS_MAC)
        {
            multiple =  Math.round(width / 16f);
            width = multiple * 16;
        }
        else
        {
            /* JMF filter graph seems to not like odd width */
            multiple = Math.round(width / 2f);
            width = multiple * 2;
        }

        /* JMF filter graph seems to not like odd height */
        multiple = Math.round(height / 2f);
        height = multiple * 2;

        size = new Dimension(width, height);

        Format formats[] = new Format[]
                            {
                                   new AVFrameFormat(
                                        size,
                                        Format.NOT_SPECIFIED,
                                        FFmpeg.PIX_FMT_ARGB,
                                        Format.NOT_SPECIFIED),
                                   new RGBFormat(
                                        size, // size
                                        Format.NOT_SPECIFIED, // maxDataLength
                                        Format.byteArray, // dataType
                                        Format.NOT_SPECIFIED, // frameRate
                                        32, // bitsPerPixel
                                        2 /* red */,
                                        3 /* green */,
                                        4 /* blue */)
                             };

        Rectangle bounds = ((ScreenDeviceImpl)dev).getBounds();
        x -= bounds.x;
        y -= bounds.y;

        CaptureDeviceInfo devInfo
            = new CaptureDeviceInfo(
                name + " " + display,
                new MediaLocator(ImageStreamingAuto.LOCATOR_PROTOCOL +
                        ":" + display + "," + x + "," + y),
                formats);

        device = new MediaDeviceImpl(devInfo, MediaType.VIDEO);

        return device;
    }

    /**
     * Move origin of a partial desktop streaming <tt>MediaDevice</tt>.
     *
     * @param mediaDevice desktop streaming <tt>MediaDevice</tt> obtained by
     * getMediaDeviceForPartialDesktopStreaming() method.
     * @param x new x coordinate origin
     * @param y new y coordinate origin
     */
    public void movePartialDesktopStreaming(MediaDevice mediaDevice, int x,
            int y)
    {
        MediaDeviceImpl dev = (MediaDeviceImpl)mediaDevice;

        if(!dev.getCaptureDeviceInfo().getLocator().getProtocol().
                equals(ImageStreamingAuto.LOCATOR_PROTOCOL))
        {
            return;
        }

        /* To move origin of the desktop capture, we need to access the
         * JMF DataSource of imgstreaming
         */
        VideoMediaDeviceSession session =
            (VideoMediaDeviceSession)dev.getSession();

        DataSource ds = session.getCaptureDevice();
        if(ds instanceof MutePullBufferDataSource)
        {
            MutePullBufferDataSource ds2 = (MutePullBufferDataSource)ds;
            ds = ds2.getWrappedDataSource();
        }

        ScreenDevice screen = getScreenForPoint(new Point(x, y));
        ScreenDevice currentScreen = screen;

        if(screen == null)
        {
            return;
        }

        Rectangle bounds = ((ScreenDeviceImpl)screen).getBounds();
        x -= bounds.x;
        y -= bounds.y;

        ((net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming.DataSource)
        ds).setOrigin(0, currentScreen.getIndex(), x,  y);
    }

    /**
     * If the <tt>MediaDevice</tt> corresponds to partial desktop streaming
     * device.
     *
     * @param mediaDevice <tt>MediaDevice</tt>
     * @return true if <tt>MediaDevice</tt> is a partial desktop streaming
     * device, false otherwise
     */
    public boolean isPartialStreaming(MediaDevice mediaDevice)
    {
        MediaDeviceImpl dev = (MediaDeviceImpl)mediaDevice;
        CaptureDeviceInfo devInfo = dev.getCaptureDeviceInfo();

        if(devInfo.getName().startsWith("Partial desktop streaming"))
        {
            return true;
        }

        return false;
    }

    /**
      * Find the screen device that contains specified point.
      *
      * @param p point coordinates
      * @return screen device that contains point
      */
    public ScreenDevice getScreenForPoint(Point p)
    {
        List<ScreenDevice> devs = this.getAvailableScreenDevices();

        for(ScreenDevice dev : devs)
        {
            if(dev.containsPoint(p))
            {
                return dev;
            }
        }

        return null;
    }

    /**
     * Get origin for desktop streaming device.
     *
     * @param mediaDevice media device
     */
    public Point getOriginForDesktopStreamingDevice(MediaDevice mediaDevice)
    {
        MediaDeviceImpl dev = (MediaDeviceImpl)mediaDevice;
        CaptureDeviceInfo devInfo = dev.getCaptureDeviceInfo();
        MediaLocator locator = devInfo.getLocator();

        if(!locator.getProtocol().
                equals(ImageStreamingAuto.LOCATOR_PROTOCOL))
        {
            return null;
        }

        String remainder = locator.getRemainder();
        String split[] = remainder.split(",");
        int index = 0;

        if(split != null && split.length > 1)
        {
            index = Integer.parseInt(split[0]);
        }
        else
        {
            index = Integer.parseInt(remainder);
        }

        ScreenDevice devs[] = ScreenDeviceImpl.getAvailableScreenDevice();
        if(devs.length - 1 >= index)
        {
            Rectangle r = ((ScreenDeviceImpl)devs[index]).getBounds();
            return new Point(r.x, r.y);
        }

        return null;
    }
}
