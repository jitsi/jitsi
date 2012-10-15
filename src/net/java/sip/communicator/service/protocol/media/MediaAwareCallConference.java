/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.beans.*;
import java.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Extends <tt>CallConference</tt> to represent the media-specific information
 * associated with the telephony conference-related state of a
 * <tt>MediaAwareCall</tt>.
 *
 * @author Lyubomir Marinov
 */
public class MediaAwareCallConference
    extends CallConference
{
    /**
     * The <tt>MediaDevice</tt>s indexed by <tt>MediaType</tt> ordinal which are
     * to be used by this telephony conference for media capture and/or
     * playback. If the <tt>MediaDevice</tt> for a specific <tt>MediaType</tt>
     * is <tt>null</tt>,
     * {@link MediaService#getDefaultDevice(MediaType, MediaUseCase)} is called.
     */
    private final MediaDevice[] devices;

    /**
     * The <tt>MediaDevice</tt>s which implement media mixing on the respective
     * <tt>MediaDevice</tt> in {@link #devices} for the purposes of this
     * telephony conference.
     */
    private final MediaDevice[] mixers;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to sources of
     * <tt>PropertyChangeEvent</tt>s on behalf of this instance.
     */
    private final PropertyChangeListener propertyChangeListener
        = new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent event)
                    {
                        MediaAwareCallConference.this.propertyChange(event);
                    }
                };

    /**
     * The <tt>RTPTranslator</tt> which forwards video RTP and RTCP traffic
     * between the <tt>CallPeer</tt>s of the <tt>Call</tt>s participating in
     * this telephony conference when the local peer is acting as a conference
     * focus.
     */
    private RTPTranslator videoRTPTranslator;

    /**
     * Initializes a new <tt>MediaAwareCallConference</tt> instance.
     */
    public MediaAwareCallConference()
    {
        this(false);
    }

    /**
     * Initializes a new <tt>MediaAwareCallConference</tt> instance which is to
     * optionally utilize the Jitsi VideoBridge server-side telephony
     * conferencing technology.
     *
     * @param jitsiVideoBridge <tt>true</tt> if the telephony conference
     * represented by the new instance is to utilize the Jitsi VideoBridge
     * server-side telephony conferencing technology; otherwise, <tt>false</tt>
     */
    public MediaAwareCallConference(boolean jitsiVideoBridge)
    {
        super(jitsiVideoBridge);

        int mediaTypeCount = MediaType.values().length;

        devices = new MediaDevice[mediaTypeCount];
        mixers = new MediaDevice[mediaTypeCount];

        /*
         * Listen to the MediaService in order to reflect changes in the user's
         * selection with respect to the default media device.
         */
        ProtocolMediaActivator.getMediaService().addPropertyChangeListener(
                propertyChangeListener);
    }

    /**
     * {@inheritDoc}
     *
     * If this telephony conference switches from being a conference focus to
     * not being such, disposes of the mixers used by this instance when it was
     * a conference focus 
     */
    protected void conferenceFocusChanged(boolean oldValue, boolean newValue)
    {
        /*
         * If this telephony conference switches from being a conference
         * focus to not being one, dispose of the mixers used when it was a
         * conference focus.
         */
        if (oldValue && !newValue)
        {
            Arrays.fill(mixers, null);
            if (videoRTPTranslator != null)
            {
                videoRTPTranslator.dispose();
                videoRTPTranslator = null;
            }
        }

        super.conferenceFocusChanged(oldValue, newValue);
    }

    /**
     * Gets a <tt>MediaDevice</tt> which is capable of capture and/or playback
     * of media of the specified <tt>MediaType</tt> and is the default choice of
     * the user with respect to such a <tt>MediaDevice</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> in which the retrieved
     * <tt>MediaDevice</tt> is to capture and/or play back media
     * @param mediaUseCase the <tt>MediaUseCase</tt> associated with the
     * intended utilization of the <tt>MediaDevice</tt> to be retrieved
     * @return a <tt>MediaDevice</tt> which is capable of capture and/or
     * playback of media of the specified <tt>mediaType</tt> and is the default
     * choice of the user with respect to such a <tt>MediaDevice</tt>
     */
    public MediaDevice getDefaultDevice(
            MediaType mediaType,
            MediaUseCase mediaUseCase)
    {
        int mediaTypeIndex = mediaType.ordinal();
        MediaDevice device = devices[mediaTypeIndex];
        MediaService mediaService = ProtocolMediaActivator.getMediaService();

        if (device == null)
            device = mediaService.getDefaultDevice(mediaType, mediaUseCase);

        /*
         * Make sure that the device is capable of mixing in order to support
         * conferencing and call recording.
         */
        if (device != null)
        {
            MediaDevice mixer = mixers[mediaTypeIndex];

            if (mixer == null)
            {
                switch (mediaType)
                {
                case AUDIO:
                    /*
                     * TODO AudioMixer leads to very poor audio quality on
                     * Android so do not use it unless it is really really
                     * necessary.
                     */
                    if ((!OSUtils.IS_ANDROID || isConferenceFocus())
                            /*
                             * We can use the AudioMixer only if the device is
                             * able to capture (because the AudioMixer will push
                             * when the capture device pushes).
                             */
                            && device.getDirection().allowsSending())
                    {
                        mixer = mediaService.createMixer(device);
                    }
                    break;

                case VIDEO:
                    if (isConferenceFocus())
                        mixer = mediaService.createMixer(device);
                    break;
                }

                mixers[mediaTypeIndex] = mixer;
            }

            if (mixer != null)
                device = mixer;
        }

        return device;
    }

    /**
     * Gets the <tt>RTPTranslator</tt> which forwards RTP and RTCP traffic
     * between the <tt>CallPeer</tt>s of the <tt>Call</tt>s participating in
     * this telephony conference when the local peer is acting as a conference
     * focus.
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> which
     * RTP and RTCP traffic is to be forwarded between
     * @return the <tt>RTPTranslator</tt> which forwards RTP and RTCP traffic
     * between the <tt>CallPeer</tt>s of the <tt>Call</tt>s participating in
     * this telephony conference when the local peer is acting as a conference
     * focus
     */
    public RTPTranslator getRTPTranslator(MediaType mediaType)
    {
        RTPTranslator rtpTranslator = null;

        /*
         * XXX A mixer is created for audio even when the local peer is not a
         * conference focus in order to enable additional functionality.
         * Similarly, the videoRTPTranslator is created even when the local peer
         * is not a conference focus in order to enable the local peer to turn
         * into a conference focus at a later time. More specifically,
         * MediaStreamImpl is unable to accommodate an RTPTranslator after it
         * has created its RTPManager. Yet again like the audio mixer, we'd
         * better not try to use it on Android at this time because of
         * performance issues that might arise.
         */
        if (MediaType.VIDEO.equals(mediaType)
                && (!OSUtils.IS_ANDROID || isConferenceFocus()))
        {
            if (videoRTPTranslator == null)
            {
                videoRTPTranslator
                    = ProtocolMediaActivator
                        .getMediaService()
                            .createRTPTranslator();
            }
            rtpTranslator = videoRTPTranslator;
        }
        return rtpTranslator;
    }

    /**
     * Notifies this <tt>MediaAwareCallConference</tt> about changes in the
     * values of the properties of sources of <tt>PropertyChangeEvent</tt>s. For
     * example, this instance listens to changes of the value of
     * {@link MediaService#DEFAULT_DEVICE} which represents the user's choice
     * with respect to the default audio device.
     * 
     * @param event a <tt>PropertyChangeEvent</tt> which specifies the name of
     * the property which had its value changed and the old and new values of
     * that property
     */
    private void propertyChange(PropertyChangeEvent event)
    {
        String propertyName = event.getPropertyName();

        if (MediaService.DEFAULT_DEVICE.equals(propertyName))
        {
            Object source = event.getSource();

            if (source instanceof MediaService)
            {
                /*
                 * XXX We only support changing the default audio device at the
                 * time of this writing.
                 */
                int mediaTypeIndex = MediaType.AUDIO.ordinal();
                MediaDevice mixer = mixers[mediaTypeIndex];
                MediaDevice oldValue
                    = (mixer instanceof MediaDeviceWrapper)
                        ? ((MediaDeviceWrapper) mixer).getWrappedDevice()
                        : null;
                MediaDevice newValue = devices[mediaTypeIndex];

                if (newValue == null)
                {
                    newValue
                        = ProtocolMediaActivator
                            .getMediaService()
                                .getDefaultDevice(
                                        MediaType.AUDIO,
                                        MediaUseCase.ANY);
                }

                /*
                 * XXX If MediaService#getDefaultDevice(MediaType, MediaUseCase)
                 * above returns null and its earlier return value was not null,
                 * we will not notify of an actual change in the value of the
                 * user's choice with respect to the default audio device.
                 */
                if (oldValue != newValue)
                {
                    mixers[mediaTypeIndex] = null;
                    firePropertyChange(
                            MediaAwareCall.DEFAULT_DEVICE,
                            oldValue, newValue);
                }
            }
        }
    }

    /**
     * Sets the <tt>MediaDevice</tt> to be used by this telephony conference for
     * capture and/or playback of media of a specific <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> of the media which is to be
     * captured and/or played back by the specified <tt>device</tt>
     * @param device the <tt>MediaDevice</tt> to be used by this telephony
     * conference for capture and/or playback of media of the specified
     * <tt>mediaType</tt>
     */
    void setDevice(MediaType mediaType, MediaDevice device)
    {
        int mediaTypeIndex = mediaType.ordinal();
        MediaDevice oldValue = devices[mediaTypeIndex];

        /*
         * XXX While we know the old and the new master/wrapped devices, we
         * are not sure whether the mixer has been used. Anyway, we have to
         * report different values in order to have PropertyChangeSupport
         * really fire an event.
         */
        MediaDevice mixer = mixers[mediaTypeIndex];

        if (mixer instanceof MediaDeviceWrapper)
            oldValue = ((MediaDeviceWrapper) mixer).getWrappedDevice();

        MediaDevice newValue = devices[mediaTypeIndex] = device;

        if (oldValue != newValue)
        {
            mixers[mediaTypeIndex] = null;
            firePropertyChange(
                    MediaAwareCall.DEFAULT_DEVICE,
                    oldValue, newValue);
        }
    }
}
