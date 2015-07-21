/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.media;

import java.beans.*;
import java.lang.ref.*;
import java.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.util.*;
import org.jitsi.util.event.*;

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
     * The <tt>PropertyChangeListener</tt> which will listen to the
     * <tt>MediaService</tt> about <tt>PropertyChangeEvent</tt>s.
     */
    private static WeakPropertyChangeListener
        mediaServicePropertyChangeListener;

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
     * The <tt>VolumeControl</tt> implementation which is to control the volume
     * (level) of the audio played back the telephony conference represented by
     * this instance.
     */
    private final VolumeControl outputVolumeControl
        = new BasicVolumeControl(
                VolumeControl.PLAYBACK_VOLUME_LEVEL_PROPERTY_NAME);

    /**
     * The <tt>PropertyChangeListener</tt> which listens to sources of
     * <tt>PropertyChangeEvent</tt>s on behalf of this instance.
     */
    private final PropertyChangeListener propertyChangeListener
        = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent ev)
            {
                MediaAwareCallConference.this.propertyChange(ev);
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
     * optionally utilize the Jitsi Videobridge server-side telephony
     * conferencing technology.
     *
     * @param jitsiVideobridge <tt>true</tt> if the telephony conference
     * represented by the new instance is to utilize the Jitsi Videobridge
     * server-side telephony conferencing technology; otherwise, <tt>false</tt>
     */
    public MediaAwareCallConference(boolean jitsiVideobridge)
    {
        super(jitsiVideobridge);

        int mediaTypeCount = MediaType.values().length;

        devices = new MediaDevice[mediaTypeCount];
        mixers = new MediaDevice[mediaTypeCount];

        /*
         * Listen to the MediaService in order to reflect changes in the user's
         * selection with respect to the default media device.
         */
        addMediaServicePropertyChangeListener(propertyChangeListener);
    }

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to be notified about
     * <tt>PropertyChangeEvent</tt>s fired by the current <tt>MediaService</tt>
     * implementation. The implementation adds a <tt>WeakReference</tt> to the
     * specified <tt>listener</tt> because <tt>MediaAwareCallConference</tt>
     * is unable to determine when the <tt>PropertyChangeListener</tt> is to be
     * removed.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to add
     */
    private static synchronized void addMediaServicePropertyChangeListener(
            PropertyChangeListener listener)
    {
        if (mediaServicePropertyChangeListener == null)
        {
            final MediaService mediaService
                = ProtocolMediaActivator.getMediaService();

            if (mediaService != null)
            {
                mediaServicePropertyChangeListener
                    = new WeakPropertyChangeListener()
                    {
                        @Override
                        protected void addThisToNotifier()
                        {
                            mediaService.addPropertyChangeListener(this);
                        }

                        @Override
                        protected void removeThisFromNotifier()
                        {
                            mediaService.removePropertyChangeListener(this);
                        }
                    };
            }
        }
        if (mediaServicePropertyChangeListener != null)
        {
            mediaServicePropertyChangeListener.addPropertyChangeListener(
                    listener);
        }
    }

    /**
     * {@inheritDoc}
     *
     * If this telephony conference switches from being a conference focus to
     * not being such, disposes of the mixers used by this instance when it was
     * a conference focus
     */
    @Override
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

            /* Disposing the video translator is not needed when the conference
               changes as we have video and we will want to continue with
               the video
               Removed when chasing a bug where video call becomes conference
               call and then back again video call and the video from the
               conference focus side is not transmitted.
            if (videoRTPTranslator != null)
            {
                videoRTPTranslator.dispose();
                videoRTPTranslator = null;
            }
            */
        }

        super.conferenceFocusChanged(oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * Disposes of <tt>this.videoRTPTranslator</tt> if the removed <tt>Call</tt>
     * was the last <tt>Call</tt> in this <tt>CallConference</tt>.
     *
     * @param call the <tt>Call</tt> which has been removed from the list of
     * <tt>Call</tt>s participating in this telephony conference.
     */
    @Override
    protected void callRemoved(Call call)
    {
        super.callRemoved(call);

        if (getCallCount() == 0 && (videoRTPTranslator != null))
        {
            videoRTPTranslator.dispose();
            videoRTPTranslator = null;
        }
    }

    /**
     * Gets a <tt>MediaDevice</tt> which is capable of capture and/or playback
     * of media of the specified <tt>MediaType</tt> and is the default choice of
     * the user with respect to such a <tt>MediaDevice</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> in which the retrieved
     * <tt>MediaDevice</tt> is to capture and/or play back media
     * @param useCase the <tt>MediaUseCase</tt> associated with the intended
     * utilization of the <tt>MediaDevice</tt> to be retrieved
     * @return a <tt>MediaDevice</tt> which is capable of capture and/or
     * playback of media of the specified <tt>mediaType</tt> and is the default
     * choice of the user with respect to such a <tt>MediaDevice</tt>
     */
    public MediaDevice getDefaultDevice(
            MediaType mediaType,
            MediaUseCase useCase)
    {
        int mediaTypeIndex = mediaType.ordinal();
        MediaDevice device = devices[mediaTypeIndex];
        MediaService mediaService = ProtocolMediaActivator.getMediaService();

        if (device == null)
            device = mediaService.getDefaultDevice(mediaType, useCase);

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
     * Gets the <tt>VolumeControl</tt> which controls the volume (level) of the
     * audio played back in the telephony conference represented by this
     * instance.
     *
     * @return the <tt>VolumeControl</tt> which controls the volume (level) of
     * the audio played back in the telephony conference represented by this
     * instance
     */
    public VolumeControl getOutputVolumeControl()
    {
        return outputVolumeControl;
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
     * @param ev a <tt>PropertyChangeEvent</tt> which specifies the name of the
     * property which had its value changed and the old and new values of that
     * property
     */
    private void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        if (MediaService.DEFAULT_DEVICE.equals(propertyName))
        {
            Object source = ev.getSource();

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

    /**
     * Implements a <tt>PropertyChangeListener</tt> which weakly references and
     * delegates to specific <tt>PropertyChangeListener</tt>s and automatically
     * adds itself to and removes itself from a specific
     * <tt>PropertyChangeNotifier</tt> depending on whether there are
     * <tt>PropertyChangeListener</tt>s to delegate to. Thus enables listening
     * to a <tt>PropertyChangeNotifier</tt> by invoking
     * {@link PropertyChangeNotifier#addPropertyChangeListener(
     * PropertyChangeListener)} without
     * {@link PropertyChangeNotifier#removePropertyChangeListener(
     * PropertyChangeListener)}.
     */
    private static class WeakPropertyChangeListener
        implements PropertyChangeListener
    {
        /**
         * The indicator which determines whether this
         * <tt>PropertyChangeListener</tt> has been added to {@link #notifier}.
         */
        private boolean added = false;

        /**
         * The list of <tt>PropertyChangeListener</tt>s which are to be notified
         * about <tt>PropertyChangeEvent</tt>s fired by {@link #notifier}.
         */
        private final List<WeakReference<PropertyChangeListener>> listeners
            = new LinkedList<WeakReference<PropertyChangeListener>>();

        /**
         * The <tt>PropertyChangeNotifier</tt> this instance is to listen to
         * about <tt>PropertyChangeEvent</tt>s which are to be forwarded to
         * {@link #listeners}.
         */
        private final PropertyChangeNotifier notifier;

        /**
         * Initializes a new <tt>WeakPropertyChangeListener</tt> instance.
         */
        protected WeakPropertyChangeListener()
        {
            this(null);
        }

        /**
         * Initializes a new <tt>WeakPropertyChangeListener</tt> instance which
         * is to listen to a specific <tt>PropertyChangeNotifier</tt>.
         *
         * @param notifier the <tt>PropertyChangeNotifier</tt> the new instance
         * is to listen to
         */
        public WeakPropertyChangeListener(PropertyChangeNotifier notifier)
        {
            this.notifier = notifier;
        }

        /**
         * Adds a specific <tt>PropertyChangeListener</tt> to the list of
         * <tt>PropertyChangeListener</tt>s to be notified about
         * <tt>PropertyChangeEvent</tt>s fired by the
         * <tt>PropertyChangeNotifier</tt> associated with this instance.
         *
         * @param listener the <tt>PropertyChangeListener</tt> to add
         */
        public synchronized void addPropertyChangeListener(
                PropertyChangeListener listener)
        {
            Iterator<WeakReference<PropertyChangeListener>> i
                = listeners.iterator();
            boolean add = true;

            while (i.hasNext())
            {
                PropertyChangeListener l = i.next().get();

                if (l == null)
                    i.remove();
                else if (l.equals(listener))
                    add = false;
            }
            if (add
                    && listeners.add(
                            new WeakReference<PropertyChangeListener>(listener))
                    && !this.added)
            {
                addThisToNotifier();
                this.added = true;
            }
        }

        /**
         * Adds this as a <tt>PropertyChangeListener</tt> to {@link #notifier}.
         */
        protected void addThisToNotifier()
        {
            if (notifier != null)
                notifier.addPropertyChangeListener(this);
        }

        /**
         * {@inheritDoc}
         *
         * Notifies this instance about a <tt>PropertyChangeEvent</tt> fired by
         * {@link #notifier}.
         */
        @Override
        public void propertyChange(PropertyChangeEvent ev)
        {
            PropertyChangeListener[] ls;
            int n;

            synchronized (this)
            {
                Iterator<WeakReference<PropertyChangeListener>> i
                    = listeners.iterator();

                ls = new PropertyChangeListener[listeners.size()];
                n = 0;
                while (i.hasNext())
                {
                    PropertyChangeListener l = i.next().get();

                    if (l == null)
                        i.remove();
                    else
                        ls[n++] = l;
                }
                if ((n == 0) && this.added)
                {
                    removeThisFromNotifier();
                    this.added = false;
                }
            }

            if (n != 0)
            {
                for (PropertyChangeListener l : ls)
                {
                    if (l == null)
                        break;
                    else
                        l.propertyChange(ev);
                }
            }
        }

        /**
         * Removes a specific <tt>PropertyChangeListener</tt> from the list of
         * <tt>PropertyChangeListener</tt>s to be notified about
         * <tt>PropertyChangeEvent</tt>s fired by the
         * <tt>PropertyChangeNotifier</tt> associated with this instance.
         *
         * @param listener the <tt>PropertyChangeListener</tt> to remove
         */
        @SuppressWarnings("unused")
        public synchronized void removePropertyChangeListener(
                PropertyChangeListener listener)
        {
            Iterator<WeakReference<PropertyChangeListener>> i
                = listeners.iterator();

            while (i.hasNext())
            {
                PropertyChangeListener l = i.next().get();

                if ((l == null) || l.equals(listener))
                    i.remove();
            }
            if (this.added && (listeners.size() == 0))
            {
                removeThisFromNotifier();
                this.added = false;
            }
        }

        /**
         * Removes this as a <tt>PropertyChangeListener</tt> from
         * {@link #notifier}.
         */
        protected void removeThisFromNotifier()
        {
            if (notifier != null)
                notifier.removePropertyChangeListener(this);
        }
    }
}
