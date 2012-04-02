/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List; // disambiguation

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.swing.*;

import org.json.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.video.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.impl.neomedia.portaudio.*;
import net.java.sip.communicator.impl.neomedia.transform.sdes.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Implements <tt>MediaService</tt> for JMF.
 *
 * @author Lyubomir Marinov
 * @author Dmitri Melnikov
 */
public class MediaServiceImpl
    extends PropertyChangeNotifier
    implements MediaService,
               PortAudioDeviceChangedCallback
{
    /**
     * The <tt>Logger</tt> used by the <tt>MediaServiceImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaServiceImpl.class);

    /**
     * The name of the <tt>boolean</tt> <tt>ConfigurationService</tt> property
     * which indicates whether the detection of audio <tt>CaptureDevice</tt>s is
     * to be disabled. The default value is <tt>false</tt> i.e. the audio
     * <tt>CaptureDevice</tt>s are detected.
     */
    public static final String DISABLE_AUDIO_SUPPORT_PNAME
        = "net.java.sip.communicator.service.media.DISABLE_AUDIO_SUPPORT";

    /**
     * The name of the <tt>boolean</tt> <tt>ConfigurationService</tt> property
     * which indicates whether the detection of video <tt>CaptureDevice</tt>s is
     * to be disabled. The default value is <tt>false</tt> i.e. the video
     * <tt>CaptureDevice</tt>s are detected.
     */
    public static final String DISABLE_VIDEO_SUPPORT_PNAME
        = "net.java.sip.communicator.service.media.DISABLE_VIDEO_SUPPORT";

    /**
     * The prefix of the property names the values of which specify the dynamic
     * payload type preferences.
     */
    private static final String DYNAMIC_PAYLOAD_TYPE_PREFERENCES_PNAME_PREFIX
        = "net.java.sip.communicator.impl.neomedia.dynamicPayloadTypePreferences";

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
     * The <tt>PropertyChangeListener</tt> which listens to
     * {@link #deviceConfiguration}.
     */
    private final PropertyChangeListener
        deviceConfigurationPropertyChangeListener
            = new PropertyChangeListener()
                    {
                        public void propertyChange(PropertyChangeEvent event)
                        {
                            deviceConfigurationPropertyChange(event);
                        }
                    };

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
    private static VolumeControl outputVolumeControl;

    /**
     * The volume control of the media service capture.
     */
    private static VolumeControl inputVolumeControl;

    /**
     * Lock to protected reinitialization of video devices.
     */
    private final Object reinitVideoLock = new Object();

    /**
     * Listeners interested in Recorder events without the need to
     * have access to their instances.
     */
    private final List<Recorder.Listener> recorderListeners =
            new ArrayList<Recorder.Listener>();

    /**
     * Audio configuration panel.
     */
    private SIPCommDialog audioConfigDialog = null;

    /**
     * Initializes a new <tt>MediaServiceImpl</tt> instance.
     */
    public MediaServiceImpl()
    {
    }

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
     * @param srtpControl a control which is already created, used to control
     *        the srtp operations.
     *
     * @return a new <tt>MediaStream</tt> instance
     * @see MediaService#createMediaStream(StreamConnector, MediaDevice)
     */
    public MediaStream createMediaStream(
            StreamConnector connector,
            MediaDevice device,
            SrtpControl srtpControl)
    {
        switch (device.getMediaType())
        {
        case AUDIO:
            return new AudioMediaStreamImpl(connector, device, srtpControl);
        case VIDEO:
            return new VideoMediaStreamImpl(connector, device, srtpControl);
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
        switch (device.getMediaType())
        {
        case AUDIO:
            return new AudioMixerMediaDevice((AudioMediaDeviceImpl) device);
        case VIDEO:
            return new VideoTranslatorMediaDevice((MediaDeviceImpl) device);
        default:
            /*
             * TODO If we do not support mixing, should we return null or rather
             * a MediaDevice with INACTIVE MediaDirection?
             */
            return null;
        }
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

        /*
         * Don't use the device in case the user has disabled all codecs for
         * that kind of media.
         */
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
        deviceConfiguration.addPropertyChangeListener(
                deviceConfigurationPropertyChangeListener);

        encodingConfiguration.initializeFormatPreferences();
        encodingConfiguration.registerCustomPackages();
        encodingConfiguration.registerCustomCodecs();

        /*
         * The neomedia bundle is generic enough to be used in a headless
         * GraphicsEnvironment so prevent a HeadlessException.
         */
        if(!OSUtils.IS_ANDROID && !GraphicsEnvironment.isHeadless())
        {
            try
            {
                final Component panel
                    = MediaConfiguration.createBasicControls(
                            DeviceConfigurationComboBoxModel.AUDIO,
                            false);

                audioConfigDialog
                    = new SIPCommDialog()
                    {
                        /** Serial version UID. */
                        private static final long serialVersionUID = 0L;

                        /** {@inheritDoc} */
                        @Override
                        protected void close(boolean escaped)
                        {
                            setVisible(false);
                        }
                    };

                TransparentPanel mainPanel
                    = new TransparentPanel(new BorderLayout(20, 5));
                TransparentPanel fieldsPanel
                    = new TransparentPanel(new BorderLayout(10, 5));

                mainPanel.setBorder(
                        BorderFactory.createEmptyBorder(20, 20, 20, 20));

                TransparentPanel btnPanel
                    = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
                ResourceManagementService resources
                    = NeomediaActivator.getResources();
                JButton btn
                    = new JButton(resources.getI18NString("service.gui.CLOSE"));

                btn.addActionListener(
                        new ActionListener()
                        {
                            public void actionPerformed(ActionEvent evt)
                            {
                                audioConfigDialog.setVisible(false);
                            }
                        });
                btnPanel.add(btn);

                JTextArea infoTextArea = new JTextArea();

                infoTextArea.setOpaque(false);
                infoTextArea.setEditable(false);
                infoTextArea.setWrapStyleWord(true);
                infoTextArea.setLineWrap(true);
                infoTextArea.setText(
                        resources.getI18NString(
                                "impl.media.configform"
                                    + ".AUDIO_DEVICE_CONNECTED_REMOVED"));

                fieldsPanel.add(infoTextArea, BorderLayout.NORTH);
                fieldsPanel.add(panel, BorderLayout.CENTER);
                fieldsPanel.add(btnPanel, BorderLayout.SOUTH);

                TransparentPanel iconPanel
                    = new TransparentPanel(new BorderLayout());

                iconPanel.add(
                        new JLabel(
                                resources.getImage(
                                        "plugin.mediaconfig.AUDIO_ICON_64x64")),
                        BorderLayout.NORTH);

                mainPanel.add(iconPanel,BorderLayout.WEST);
                mainPanel.add(fieldsPanel, BorderLayout.CENTER);

                audioConfigDialog.setTitle(
                        resources.getI18NString(
                                "impl.media.configform.AUDIO_DEVICE_CONFIG"));
                audioConfigDialog.add(mainPanel);
                audioConfigDialog.validate();
                audioConfigDialog.pack();

                PortAudioDeviceChangedCallbacks.addDeviceChangedCallback(this);
            }
            catch(Throwable t)
            {
                logger.info("Failed to create audio configuration panel", t);
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
            }
        }
    }

    /**
     * Stops this <tt>MediaService</tt> implementation and thus signals that its
     * utilization should cease.
     */
    void stop()
    {
        deviceConfiguration.removePropertyChangeListener(
                deviceConfigurationPropertyChangeListener);
        PortAudioDeviceChangedCallbacks.removeDeviceChangedCallback(this);
    }

    /**
     * Initializes a new <tt>ZrtpControl</tt> instance which is to control all
     * ZRTP options.
     *
     * @return a new <tt>ZrtpControl</tt> instance which is to control all ZRTP
     * options
     */
    public ZrtpControl createZrtpControl()
    {
        return new ZrtpControlImpl();
    }

    /**
     * Initializes a new <tt>SDesControl</tt> instance which is to control all
     * SDes options.
     *
     * @return a new <tt>SDesControl</tt> instance which is to control all SDes
     * options
     */
    public SDesControl createSDesControl()
    {
        return new SDesControlImpl();
    }

    /**
     * Gets the <tt>VolumeControl</tt> which controls the volume level of audio
     * output/playback.
     *
     * @return the <tt>VolumeControl</tt> which controls the volume level of
     * audio output/playback
     * @see MediaService#getOutputVolumeControl()
     */
    public VolumeControl getOutputVolumeControl()
    {
        if (outputVolumeControl == null)
        {
            outputVolumeControl
                = new AbstractVolumeControl(
                        VolumeControl.PLAYBACK_VOLUME_LEVEL_PROPERTY_NAME);
        }
        return outputVolumeControl;
    }

    /**
     * Gets the <tt>VolumeControl</tt> which controls the volume level of audio
     * input/capture.
     *
     * @return the <tt>VolumeControl</tt> which controls the volume level of
     * audio input/capture
     * @see MediaService#getInputVolumeControl()
     */
    public VolumeControl getInputVolumeControl()
    {
        if (inputVolumeControl == null)
        {
            inputVolumeControl
                = new AbstractVolumeControl(
                        VolumeControl.CAPTURE_VOLUME_LEVEL_PROPERTY_NAME);
        }
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

        if ((screens != null) && (screens.length != 0))
            screenList = new ArrayList<ScreenDevice>(Arrays.asList(screens));
        else
            screenList = Collections.emptyList();
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

        for (ScreenDevice screen : screens)
        {
            java.awt.Dimension res = screen.getSize();

            if ((res != null) && ((width < res.width) || (height < res.height)))
            {
                width = res.width;
                height = res.height;
                best = screen;
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
     * legacy systems. Signaling protocol implementations such as SIP and XMPP
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

            /*
             * Set the dynamicPayloadTypePreferences to their default values. If
             * the user chooses to override them through the
             * ConfigurationService, they will be overwritten later on.
             */
            MediaFormat telephoneEvent
                = MediaUtils.getMediaFormat("telephone-event", 8000);
            if (telephoneEvent != null)
                dynamicPayloadTypePreferences.put(telephoneEvent, (byte) 101);

            MediaFormat h264
                = MediaUtils.getMediaFormat(
                        "H264",
                        VideoMediaFormatImpl.DEFAULT_CLOCK_RATE);
            if (h264 != null)
                dynamicPayloadTypePreferences.put(h264, (byte) 99);

            /*
             * Try to load dynamicPayloadTypePreferences from the
             * ConfigurationService.
             */
            ConfigurationService config
                = NeomediaActivator.getConfigurationService();
            String prefix = DYNAMIC_PAYLOAD_TYPE_PREFERENCES_PNAME_PREFIX;
            List<String> propertyNames
                = config.getPropertyNamesByPrefix(prefix, true);

            for (String propertyName : propertyNames)
            {
                /*
                 * The dynamic payload type is the name of the property name and
                 * the format which prefers it is the property value.
                 */
                byte dynamicPayloadTypePreference = 0;
                Throwable exception = null;

                try
                {
                    dynamicPayloadTypePreference
                        = Byte.parseByte(
                                propertyName.substring(prefix.length() + 1));
                }
                catch (IndexOutOfBoundsException ioobe)
                {
                    exception = ioobe;
                }
                catch (NumberFormatException nfe)
                {
                    exception = nfe;
                }
                if (exception != null)
                {
                    logger.warn(
                            "Ignoring dynamic payload type preference"
                                + " which could not be parsed: "
                                + propertyName,
                            exception);
                    continue;
                }

                String source = config.getString(propertyName);

                if ((source != null) && (source.length() != 0))
                {
                    try
                    {
                        JSONObject json = new JSONObject(source);
                        String encoding
                            = json.getString(MediaFormatImpl.ENCODING_PNAME);
                        int clockRate
                            = json.getInt(MediaFormatImpl.CLOCK_RATE_PNAME);
                        Map<String, String> fmtps
                            = new HashMap<String, String>();

                        if (json.has(MediaFormatImpl.FORMAT_PARAMETERS_PNAME))
                        {
                            JSONObject jsonFmtps
                                = json.getJSONObject(
                                        MediaFormatImpl
                                            .FORMAT_PARAMETERS_PNAME);
                            Iterator<?> jsonFmtpsIter = jsonFmtps.keys();

                            while (jsonFmtpsIter.hasNext())
                            {
                                String key = jsonFmtpsIter.next().toString();
                                String value = jsonFmtps.getString(key);

                                fmtps.put(key, value);
                            }
                        }

                        MediaFormat mediaFormat
                            = MediaUtils.getMediaFormat(
                                    encoding, clockRate,
                                    fmtps);

                        if (mediaFormat != null)
                            dynamicPayloadTypePreferences.put(
                                    mediaFormat,
                                    dynamicPayloadTypePreference);
                    }
                    catch (JSONException jsone)
                    {
                        logger.warn(
                                "Ignoring dynamic payload type preference"
                                    + " which could not be parsed: "
                                    + source,
                                jsone);
                    }
                }
            }
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
            CaptureDeviceInfo captureDeviceInfo;

            if ((device == null) ||
                    ((captureDeviceInfo
                                = ((MediaDeviceImpl)device)
                                    .getCaptureDeviceInfo())
                            == null))
                return videoContainer;

            DataSource dataSource
                = Manager.createDataSource(captureDeviceInfo.getLocator());

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

            Processor player = Manager.createProcessor(dataSource);
            final MediaLocator locator = dataSource.getLocator();

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
            logger.error("Failed to create video preview component", e);
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

                        // do not flip desktop
                        if (ImageStreamingAuto.LOCATOR_PROTOCOL.equals(
                                locator.getProtocol()))
                            codecs = new Codec[] { scaler };
                        else
                            codecs = new Codec[] { new HFlip(), scaler };

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
                        player.start();

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
            display = dev.getIndex();
        else
            return null;

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

        return
            (devInfo != null)
                && devInfo.getName().startsWith("Partial desktop streaming");
    }

    /**
      * Find the screen device that contains specified point.
      *
      * @param p point coordinates
      * @return screen device that contains point
      */
    public ScreenDevice getScreenForPoint(Point p)
    {
        for(ScreenDevice dev : getAvailableScreenDevices())
            if(dev.containsPoint(p))
                return dev;
        return null;
    }

    /**
     * Gets the origin of a specific desktop streaming device.
     *
     * @param mediaDevice the desktop streaming device to get the origin on
     * @return the origin of the specified desktop streaming device
     */
    public Point getOriginForDesktopStreamingDevice(MediaDevice mediaDevice)
    {
        MediaDeviceImpl dev = (MediaDeviceImpl)mediaDevice;
        CaptureDeviceInfo devInfo = dev.getCaptureDeviceInfo();
        if(devInfo == null)
            return null;
        MediaLocator locator = devInfo.getLocator();

        if(!ImageStreamingAuto.LOCATOR_PROTOCOL.equals(locator.getProtocol()))
            return null;

        String remainder = locator.getRemainder();
        String split[] = remainder.split(",");
        int index
            = Integer.parseInt(
                    ((split != null) && (split.length > 1))
                        ? split[0]
                        : remainder);

        ScreenDevice devs[] = ScreenDeviceImpl.getAvailableScreenDevice();

        if (devs.length - 1 >= index)
        {
            Rectangle r = ((ScreenDeviceImpl)devs[index]).getBounds();

            return new Point(r.x, r.y);
        }

        return null;
    }

    /**
     * Those interested in Recorder events add listener through MediaService.
     * This way they don't need to have access to the Recorder instance.
     * Adds a new <tt>Recorder.Listener</tt> to the list of listeners
     * interested in notifications from a <tt>Recorder</tt>.
     *
     * @param listener the new <tt>Recorder.Listener</tt> to be added to the
     * list of listeners interested in notifications from <tt>Recorder</tt>s.
     */
    public void addRecorderListener(Recorder.Listener listener)
    {
        synchronized(recorderListeners)
        {
            if(!recorderListeners.contains(listener))
                recorderListeners.add(listener);
        }
    }

    /**
     * Removes an existing <tt>Recorder.Listener</tt> from the list of listeners
     * interested in notifications from <tt>Recorder</tt>s.
     *
     * @param listener the existing <tt>Listener</tt> to be removed from the
     * list of listeners interested in notifications from <tt>Recorder</tt>s
     */
    public void removeRecorderListener(Recorder.Listener listener)
    {
        synchronized(recorderListeners)
        {
            recorderListeners.remove(listener);
        }
    }

    /**
     * Gives access to currently registered <tt>Recorder.Listener</tt>s.
     * @return currently registered <tt>Recorder.Listener</tt>s.
     */
    public Iterator<Recorder.Listener> getRecorderListeners()
    {
        return recorderListeners.iterator();
    }

    /**
     * Callback called from native PortAudio side that notify device changed.
     */
    public void deviceChanged()
    {
        showAudioConfiguration();
    }

    /**
     * Notifies this instance that the value of a property of
     * {@link #deviceConfiguration} has changed.
     *
     * @param event a <tt>PropertyChangeEvent</tt> which specifies the name of
     * the property which had its value changed and the old and the new values
     * of that property
     */
    private void deviceConfigurationPropertyChange(PropertyChangeEvent event)
    {
        String propertyName = event.getPropertyName();

        if (DeviceConfiguration.AUDIO_CAPTURE_DEVICE.equals(propertyName)
                || DeviceConfiguration.VIDEO_CAPTURE_DEVICE.equals(propertyName))
        {
            /*
             * We do not know the old value of the property at the time of this
             * writing. We cannot report the new value either because we do not
             * know the MediaType and the MediaUseCase.
             */
            firePropertyChange(DEFAULT_DEVICE, null, null);
        }
    }

    /**
     * Show audio configuration panel when media devices change.
     */
    private void showAudioConfiguration()
    {
        if (audioConfigDialog != null)
        {
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                                {
                                    public void run()
                                    {
                                        showAudioConfiguration();
                                    }
                                });
                return;
            }

            SwingUtilities.updateComponentTreeUI(
                audioConfigDialog.getComponent(0));
            audioConfigDialog.pack();
            audioConfigDialog.repaint();

            if (!audioConfigDialog.isVisible())
                audioConfigDialog.setVisible(true);
        }
    }

    /**
     * Initializes a new <tt>RTPTranslator</tt> which is to forward RTP and RTCP
     * traffic between multiple <tt>MediaStream</tt>s.
     *
     * @return a new <tt>RTPTranslator</tt> which is to forward RTP and RTCP
     * traffic between multiple <tt>MediaStream</tt>s
     * @see MediaService#createRTPTranslator()
     */
    public RTPTranslator createRTPTranslator()
    {
        return new RTPTranslatorImpl();
    }
}
