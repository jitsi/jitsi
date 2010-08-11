/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import javax.media.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.impl.neomedia.format.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.format.*;

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
    public MediaDevice getDefaultDevice(MediaType mediaType,
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
                        && captureDeviceInfo.equals(((MediaDeviceImpl) device)
                                        .getCaptureDeviceInfo()))
                {
                    defaultDevice = device;
                    break;
                }
            }
        }
        if (defaultDevice == null)
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

        //Don't use the device in case the user has disabled all codecs for that
        //kind of media.
        if (defaultDevice != null
            && defaultDevice.getSupportedFormats().size() == 0)
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
             * MediaService does not understad MediaTypes other than AUDIO and
             * VIDEO.
             */
            return EMPTY_DEVICES;
        }

        List<MediaDevice> publicDevices = new ArrayList<MediaDevice>();

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
            for (ScreenDevice screen : screens)
                screenList.add(screen);
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
}
