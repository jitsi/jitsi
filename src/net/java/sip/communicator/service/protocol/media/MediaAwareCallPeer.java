/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A utility class implementing media control code shared between current
 * telephony implementations. This class is only meant for use by protocol
 * implementations and should/could not be accessed by bundles that are simply
 * using the telephony functionalities.
 *
 * @param <T> the peer extension class like for example <tt>CallSipImpl</tt>
 * or <tt>CallJabberImpl</tt>
 * @param <U> the media handler extension class like for example
 * <tt>CallPeerMediaHandlerSipImpl</tt> or
 * <tt>CallPeerMediaHandlerJabberImpl</tt>
 * @param <V> the provider extension class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt> or
 * <tt>ProtocolProviderServiceJabberImpl</tt>
 *
 * @author Emil Ivov
 */
public abstract class MediaAwareCallPeer
                          <T extends MediaAwareCall<?, ?, V>,
                           U extends CallPeerMediaHandler<?>,
                           V extends ProtocolProviderService>
    extends AbstractCallPeer<T, V>
    implements SrtpListener,
               CallPeerConferenceListener,
               CsrcAudioLevelListener,
               SimpleAudioLevelListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>MediaAwareCallPeer</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaAwareCallPeer.class);

    /**
     * A byte array containing the image/photo representing the call peer.
     */
    private byte[] image;

    /**
     * A string uniquely identifying the peer.
     */
    private String peerID;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to
     * {@link CallPeerMediaHandler} for changes in the values of its properties.
     */
    private PropertyChangeListener mediaHandlerPropertyChangeListener;

    /**
     * The <tt>List</tt> of <tt>PropertyChangeListener</tt>s listening to this
     * <tt>CallPeer</tt> for changes in the values of its properties related to
     * video.
     */
    private final List<PropertyChangeListener> videoPropertyChangeListeners
        = new LinkedList<PropertyChangeListener>();

    /**
     * The list of <tt>SoundLevelListener</tt>s interested in level changes in
     * the audio we are getting from the remote peer.
     * <p>
     * It is implemented as a copy-on-write storage because the number of
     * additions and removals of <tt>SoundLevelListener</tt>s is expected to be
     * far smaller than the number of audio level changes. The access to it is
     * to be synchronized using {@link #streamAudioLevelListenersSyncRoot}.
     * </p>
     */
    private List<SoundLevelListener> streamAudioLevelListeners;

    /**
     * The <tt>Object</tt> to synchronize the access to
     * {@link #streamAudioLevelListeners}.
     */
    private final Object streamAudioLevelListenersSyncRoot = new Object();

    /**
     * Holds listeners registered for level changes in the audio of participants
     * that this peer might be mixing and that we are not directly communicating
     * with.
     */
    private final List<ConferenceMembersSoundLevelListener>
        conferenceMemberAudioLevelListeners
            = new ArrayList<ConferenceMembersSoundLevelListener>();

    /**
     * The call this peer belongs to.
     */
    private T call;

    /**
     * The protocol provider that this peer belongs to.
     */
    private final V protocolProvider;

    /**
     * The media handler class handles all media management for a single
     * <tt>CallPeer</tt>. This includes initializing and configuring streams,
     * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt>always
     * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
     * both classes are only separated for reasons of readability.
     */
    private U mediaHandler;

    /**
     * Creates a new call peer with address <tt>peerAddress</tt>.
     *
     * @param owningCall the call that contains this call peer.
     */
    public MediaAwareCallPeer(T owningCall)
    {
        this.call = owningCall;
        this.protocolProvider = owningCall.getProtocolProvider();

        // create the uid
        this.peerID
            = String.valueOf(System.currentTimeMillis())
                + String.valueOf(hashCode());

        // we listen for events when the call will become focus or not
        // of a conference so we will add or remove our sound level listeners
        super.addCallPeerConferenceListener(this);
    }

    /**
     * The method returns an image representation of the call peer if one is
     * available.
     *
     * @return byte[] a byte array containing the image or null if no image is
     * available.
     */
    public byte[] getImage()
    {
        return image;
    }

    /**
     * Sets the byte array containing an image representation (photo or picture)
     * of the call peer.
     *
     * @param image a byte array containing the image
     */
    public void setImage(byte[] image)
    {
        byte[] oldImage = getImage();
        this.image = image;

        //Fire the Event
        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_IMAGE_CHANGE,
                oldImage,
                image);
    }

    /**
     * Returns a unique identifier representing this peer.
     *
     * @return an identifier representing this call peer.
     */
    public String getPeerID()
    {
        return peerID;
    }

    /**
     * Sets the String that serves as a unique identifier of this
     * CallPeer.
     * @param peerID the ID of this call peer.
     */
    public void setPeerID(String peerID)
    {
        this.peerID = peerID;
    }


    /**
     * Returns a reference to the call that this peer belongs to. Calls
     * are created by underlying telephony protocol implementations.
     *
     * @return a reference to the call containing this peer.
     */
    public T getCall()
    {
        return call;
    }

    /**
     * Sets the call containing this peer.
     *
     * @param call the call that this call peer is participating in.
     */
    public void setCall(T call)
    {
        this.call = call;
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that this
     * peer belongs to.
     */
    public V getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Determines whether the audio stream (if any) being sent to this
     * peer is mute.
     *
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         peer and it is currently mute; <tt>false</tt>, otherwise
     */
    @Override
    public boolean isMute()
    {
        return getMediaHandler().isMute();
    }

    /**
     * Updates the state of this <tt>CallPeer</tt> to match the remotely-on-hold
     * status of our media handler.
     */
    public void reevalRemoteHoldStatus()
    {
        boolean remotelyOnHold = getMediaHandler().isRemotelyOnHold();

        CallPeerState state = getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (remotelyOnHold)
                setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!remotelyOnHold)
                setState(CallPeerState.ON_HOLD_LOCALLY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (!remotelyOnHold)
                setState(CallPeerState.CONNECTED);
        }
        else if (remotelyOnHold)
        {
            setState(CallPeerState.ON_HOLD_REMOTELY);
        }
    }

    /**
     * Updates the state of this <tt>CallPeer</tt> to match the locally-on-hold
     * status of our media handler.
     */
    public void reevalLocalHoldStatus()
    {
        boolean locallyOnHold = getMediaHandler().isLocallyOnHold();

        CallPeerState state = getState();
        if (CallPeerState.ON_HOLD_LOCALLY.equals(state))
        {
            if (!locallyOnHold)
                setState(CallPeerState.CONNECTED);
        }
        else if (CallPeerState.ON_HOLD_MUTUALLY.equals(state))
        {
            if (!locallyOnHold)
                setState(CallPeerState.ON_HOLD_REMOTELY);
        }
        else if (CallPeerState.ON_HOLD_REMOTELY.equals(state))
        {
            if (locallyOnHold)
                setState(CallPeerState.ON_HOLD_MUTUALLY);
        }
        else if (locallyOnHold)
        {
            setState(CallPeerState.ON_HOLD_LOCALLY);
        }
    }

    /**
     * Sets the mute property for this call peer.
     *
     * @param newMuteValue the new value of the mute property for this call peer
     */
    @Override
    public void setMute(boolean newMuteValue)
    {
        getMediaHandler().setMute(newMuteValue);
        super.setMute(newMuteValue);
    }

    /**
     * Logs <tt>message</tt> and <tt>cause</tt> and sets this <tt>peer</tt>'s
     * state to <tt>CallPeerState.FAILED</tt>
     *
     * @param message a message to log and display to the user.
     * @param throwable the exception that cause the error we are logging
     */
    public void logAndFail(String message, Throwable throwable)
    {
        logger.error(message, throwable);
        setState(CallPeerState.FAILED, message);
    }

    /**
     * Modifies the local media setup to reflect the requested setting for the
     * streaming of the local video and then re-invites the peer represented by
     * this class using a corresponding SDP description..
     *
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     *  @throws OperationFailedException if video initialization fails.
     */
    public void setLocalVideoAllowed(boolean allowed)
        throws OperationFailedException
    {
        CallPeerMediaHandler<?> mediaHandler = getMediaHandler();

        if(mediaHandler.isLocalVideoTransmissionEnabled() != allowed)
        {
            // Modify the local media setup to reflect the requested setting for
            // the streaming of the local video.
            mediaHandler.setLocalVideoTransmissionEnabled(allowed);
        }
    }

    /**
     * Determines whether we are currently streaming video toward whoever this
     * <tt>CallPeerSipImpl</tt> represents.
     *
     * @return <tt>true</tt> if we are currently streaming video toward this
     *  <tt>CallPeer</tt> and  <tt>false</tt> otherwise.
     */
    public boolean isLocalVideoStreaming()
    {
        return getMediaHandler().isLocalVideoTransmissionEnabled();
    }

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners which get notified when the properties (e.g.
     * LOCAL_VIDEO_STREAMING) associated with this <tt>CallPeer</tt> change
     * their values.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to be notified
     * when the properties associated with the specified <tt>Call</tt> change
     * their values
     */
    public void addVideoPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (videoPropertyChangeListeners)
        {
            /*
             * The video is part of the media-related functionality and thus it
             * is the responsibility of mediaHandler. So listen to mediaHandler
             * for video-related property changes and re-fire them as
             * originating from this instance.
             */
            if (!videoPropertyChangeListeners.contains(listener)
                    && videoPropertyChangeListeners.add(listener)
                    && (mediaHandlerPropertyChangeListener == null))
            {
                mediaHandlerPropertyChangeListener
                    = new PropertyChangeListener()
                    {
                        public void propertyChange(PropertyChangeEvent event)
                        {
                            Iterable<PropertyChangeListener> listeners;

                            synchronized (videoPropertyChangeListeners)
                            {
                                listeners
                                    = new LinkedList<PropertyChangeListener>(
                                            videoPropertyChangeListeners);
                            }

                            PropertyChangeEvent thisEvent
                                = new PropertyChangeEvent(
                                        this,
                                        event.getPropertyName(),
                                        event.getOldValue(),
                                        event.getNewValue());

                            for (PropertyChangeListener listener : listeners)
                                listener.propertyChange(thisEvent);
                        }
                    };
                getMediaHandler()
                    .addPropertyChangeListener(
                        mediaHandlerPropertyChangeListener);
            }
        }
    }

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners which get notified when the properties (e.g.
     * LOCAL_VIDEO_STREAMING) associated with this <tt>CallPeer</tt> change
     * their values.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to no longer be
     * notified when the properties associated with the specified <tt>Call</tt>
     * change their values
     */
    public void removeVideoPropertyChangeListener(
                                               PropertyChangeListener listener)
    {
        if (listener != null)
            synchronized (videoPropertyChangeListeners)
            {
                /*
                 * The video is part of the media-related functionality and thus
                 * it is the responsibility of mediaHandler. So we're listening
                 * to mediaHandler for video-related property changes and w're
                 * re-firing them as originating from this instance. Make sure
                 * that we're not listening to mediaHandler if noone is
                 * interested in video-related property changes originating from
                 * this instance.
                 */
                if (videoPropertyChangeListeners.remove(listener)
                        && videoPropertyChangeListeners.isEmpty()
                        && (mediaHandlerPropertyChangeListener != null))
                {
//                    getMediaHandler()
//                        .removePropertyChangeListener(
//                            mediaHandlerPropertyChangeListener);
                    mediaHandlerPropertyChangeListener = null;
                }
            }
    }

    /**
     * Overrides the parent set state method in order to make sure that we
     * close our media handler whenever we enter a disconnected state.
     *
     * @param newState the <tt>CallPeerState</tt> that we are about to enter and
     * that we pass to our predecessor.
     * @param reason a reason phrase explaining the state (e.g. if newState
     * indicates a failure) and that we pass to our predecessor.
     * @param reasonCode the code for the reason of the state change.
     */
    @Override
    public void setState(CallPeerState newState, String reason, int reasonCode)
    {
        // synchronized to mediaHandler if there are currently jobs of
        // initializing, configuring and starting streams (method processAnswer
        // of CallPeerMediaHandler) we won't set and fire the current state
        // to Disconnected. Before closing the mediaHandler is setting the state
        // in order to deliver states as quick as possible.
        CallPeerMediaHandler<?> mediaHandler = getMediaHandler();

        synchronized(mediaHandler)
        {
            try
            {
                super.setState(newState, reason, reasonCode);
            }
            finally
            {
                // make sure whatever happens to close the media
                if (CallPeerState.DISCONNECTED.equals(newState)
                    || CallPeerState.FAILED.equals(newState))
                {
                    mediaHandler.close();
                }
            }
        }
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of listeners
     * interested in and notified about changes in the sound level of the audio
     * sent by the remote party. When the first listener is being registered
     * the method also registers its single listener with the media handler so
     * that it would receive level change events and delegate them to the
     * listeners that have registered with us.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addStreamSoundLevelListener(SoundLevelListener listener)
    {
        synchronized (streamAudioLevelListenersSyncRoot)
        {
            if ((streamAudioLevelListeners == null)
                    || streamAudioLevelListeners.isEmpty())
            {
                // if this is the first listener that's being registered with
                // us, we also need to register ourselves as an audio level
                // listener with the media handler. we do this so that audio
                // levels would only be calculated if anyone is interested in
                // receiving them.
                getMediaHandler().setStreamAudioLevelListener(this);
            }

            /*
             * Implement streamAudioLevelListeners as a copy-on-write storage so
             * that iterators over it can iterate without
             * ConcurrentModificationExceptions.
             */
            streamAudioLevelListeners
                = (streamAudioLevelListeners == null)
                    ? new ArrayList<SoundLevelListener>()
                    : new ArrayList<SoundLevelListener>(
                            streamAudioLevelListeners);
            streamAudioLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to remove
     */
    public void removeStreamSoundLevelListener(SoundLevelListener listener)
    {
        synchronized (streamAudioLevelListenersSyncRoot)
        {
            /*
             * Implement streamAudioLevelListeners as a copy-on-write storage so
             * that iterators over it can iterate over it without
             * ConcurrentModificationExceptions.
             */
            if (streamAudioLevelListeners != null)
            {
                streamAudioLevelListeners
                    = new ArrayList<SoundLevelListener>(
                            streamAudioLevelListeners);
                if (streamAudioLevelListeners.remove(listener)
                        && streamAudioLevelListeners.isEmpty())
                    streamAudioLevelListeners = null;
            }

            if ((streamAudioLevelListeners == null)
                    || streamAudioLevelListeners.isEmpty())
            {
                // if this was the last listener then we also need to remove
                // ourselves as an audio level so that audio levels would only
                // be calculated if anyone is interested in receiving them.
                getMediaHandler().setStreamAudioLevelListener(null);
            }
        }
    }

    /**
     * Adds a specific <tt>ConferenceMembersSoundLevelListener</tt> to the list
     * of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>ConferenceMembersSoundLevelListener</tt> to add
     */
    public void addConferenceMembersSoundLevelListener(
                                ConferenceMembersSoundLevelListener listener)
    {
        synchronized (conferenceMemberAudioLevelListeners)
        {
            if (conferenceMemberAudioLevelListeners.size() == 0)
            {
                // if this is the first listener that's being registered with
                // us, we also need to register ourselves as a CSRC audio level
                // listener with the media handler.
                getMediaHandler().setCsrcAudioLevelListener(this);
            }

            conferenceMemberAudioLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>ConferenceMembersSoundLevelListener</tt> of the
     * list of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>ConferenceMembersSoundLevelListener</tt> to
     * remove
     */
    public void removeConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {
        synchronized (conferenceMemberAudioLevelListeners)
        {
            conferenceMemberAudioLevelListeners.remove(listener);

            if (conferenceMemberAudioLevelListeners.size() == 0)
            {
                // if this was the last listener then we also remove ourselves
                // as a CSRC audio level listener from the handler so that we
                // don't have to create new events and maps for something no one
                // is interested in.
                getMediaHandler().setCsrcAudioLevelListener(null);
            }
        }
    }

    /**
     * Implements {@link CsrcAudioLevelListener#audioLevelsReceived(long[])}
     * so that we could deliver to {@link ConferenceMembersSoundLevelListener}s
     * the events corresponding to the audio level changes that are being
     * reported here.
     *
     * @param audioLevels the levels that we need to dispatch to all registered
     * <tt>ConferenceMemberSoundLevelListeners</tt>.
     */
    public void audioLevelsReceived(long[] audioLevels)
    {
        if (getConferenceMemberCount() == 0)
            return;

        Map<ConferenceMember, Integer> levelsMap
            = new HashMap<ConferenceMember, Integer>();

        for (int i = 0; i < audioLevels.length; i += 2)
        {
            ConferenceMember mmbr = findConferenceMember(audioLevels[i]);

            if (mmbr == null)
                continue;
            else
                levelsMap.put(mmbr, (int)audioLevels[i + 1]);
        }

        ConferenceMembersSoundLevelEvent evt
            = new ConferenceMembersSoundLevelEvent(this, levelsMap);

        synchronized( conferenceMemberAudioLevelListeners)
        {
            int conferenceMemberAudioLevelListenerCount
                = conferenceMemberAudioLevelListeners.size();

            for (int i = 0; i < conferenceMemberAudioLevelListenerCount; i++)
                conferenceMemberAudioLevelListeners
                    .get(i).soundLevelChanged(evt);
        }
    }

    /**
     * Does nothing.
     * @param evt the event.
     */
    public void callPeerAdded(CallPeerEvent evt)
    {}

    /**
     * Does nothing.
     * @param evt the event.
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {}

    /**
     * Sets the security status to ON for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     * @param cipher the cipher
     * @param sender the security controller that caused the event
     */
    public void securityTurnedOn(int sessionType, String cipher,
        SrtpControl sender)
    {
        getMediaHandler().startSrtpMultistream(sender);
        CallPeerSecurityOnEvent evt =
            new CallPeerSecurityOnEvent(this, sessionType, cipher, sender);
        fireCallPeerSecurityOnEvent(evt);
    }

    /**
     * Sets the security status to OFF for this call peer.
     *
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityTurnedOff(int sessionType)
    {
        // If this event has been triggered because of a call end event and the
        // call is already ended we don't need to alert the user for
        // security off.
        if((call != null) && !call.getCallState().equals(CallState.CALL_ENDED))
        {
            CallPeerSecurityOffEvent event
                = new CallPeerSecurityOffEvent( this, sessionType);
            fireCallPeerSecurityOffEvent(event);
        }
    }

    /**
     * Sets the security message associated with a failure/warning or
     * information coming from the encryption protocol.
     *
     * @param messageType the type of the message.
     * @param i18nMessage the message
     * @param severity severity level
     */
    public void securityMessageReceived(
        String messageType, String i18nMessage, int severity)
    {
        fireCallPeerSecurityMessageEvent(messageType,
                                         i18nMessage,
                                         severity);
    }

    /**
     * Dummy implementation of {@link CallPeerConferenceListener
     * #conferenceFocusChanged(CallPeerConferenceEvent)}.
     *
     * @param evt ignored
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent evt)
    {
    }

    /**
     * Called when this peer becomes a mixer. The method add removes this class
     * as the stream audio level listener for the media coming from this peer
     * because the levels it delivers no longer represent the level of a
     * particular member. The method also adds this class as a member (CSRC)
     * audio level listener.
     *
     * @param conferenceEvent the event containing information (that we don't
     * really use) on the newly add member.
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        if (getConferenceMemberCount() > 2)
        {
            // this peer is now a conference focus with more than three
            // participants. This means that the this peer is mixing and sending
            // us audio for at least two separate participants. We therefore
            // need to remove the stream level listeners and switch to CSRC
            // level listening
            CallPeerMediaHandler<?> mediaHandler = getMediaHandler();

            mediaHandler.setStreamAudioLevelListener(null);
            mediaHandler.setCsrcAudioLevelListener(this);
        }
    }

    /**
     * Called when this peer stops being a mixer. The method add removes this
     * class as the stream audio level listener for the media coming from this
     * peer because the levels it delivers no longer represent the level of a
     * particular member. The method also adds this class as a member (CSRC)
     * audio level listener.
     *
     * @param conferenceEvent the event containing information (that we don't
     * really use) on the freshly removed member.
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent)
    {
        if (getConferenceMemberCount() < 3)
        {
            // this call peer is no longer mixing audio from multiple sources
            // since there's only us and her in the call. Lets stop being a CSRC
            // listener and move back to listening the audio level of the
            // stream itself.
            CallPeerMediaHandler<?> mediaHandler = getMediaHandler();

            mediaHandler.setStreamAudioLevelListener(this);
            mediaHandler.setCsrcAudioLevelListener(null);
        }
    }

    /**
     * Notified by its very majesty the media service about changes in the
     * audio level of the stream coming from this peer, this method generates
     * the corresponding events and delivers them to the listeners that have
     * registered here.
     *
     * @param newLevel the new audio level of the local user.
     */
    public void audioLevelChanged(int newLevel)
    {
        /*
         * If we're in a conference in which this CallPeer is the focus and
         * we're the only member in it besides the focus, we will not receive
         * audio levels in the RTP and our media will instead measure the audio
         * levels of the received media. In order to make the UI oblivious of
         * the difference, we have to translate the event to the appropriate
         * type of listener.
         *
         * We may end up in a conference call with 0 members if the server
         * for some reason doesn't support sip conference (our subscribes
         * doesn't go to the focus of the conference) and so we must
         * pass the sound levels measured on the stream so we can see
         * the stream activity of the call.
         */
        int conferenceMemberCount;

        if (isConferenceFocus()
                && ((conferenceMemberCount = getConferenceMemberCount()) > 0)
                && (conferenceMemberCount < 3))
        {
            long audioRemoteSSRC = getMediaHandler().getAudioRemoteSSRC();

            if (audioRemoteSSRC != CallPeerMediaHandler.SSRC_UNKNOWN)
            {
                audioLevelsReceived(new long[] { audioRemoteSSRC, newLevel });
                return;
            }
        }

        List<SoundLevelListener> streamAudioLevelListeners;

        synchronized (streamAudioLevelListenersSyncRoot)
        {
            /*
             * Since the streamAudioLevelListeners field of this
             * MediaAwareCallPeer is implemented as a copy-on-write storage,
             * just get a reference to it and it should be safe to iterate over it
             * without ConcurrentModificationExceptions.
             */
            streamAudioLevelListeners = this.streamAudioLevelListeners;
        }

        if (streamAudioLevelListeners != null)
        {
            /*
             * Iterate over streamAudioLevelListeners using an index rather than
             * an Iterator in order to try to reduce the number of allocations
             * (as the number of audio level changes is expected to be very
             * large).
             */
            int streamAudioLevelListenerCount
                = streamAudioLevelListeners.size();

            for(int i = 0; i < streamAudioLevelListenerCount; i++)
                streamAudioLevelListeners.get(i).soundLevelChanged(
                        this,
                        newLevel);
        }
    }

    /**
     * Returns a reference to the <tt>CallPeerMediaHandler</tt> used by this
     * peer. The media handler class handles all media management for a single
     * <tt>CallPeer</tt>. This includes initializing and configuring streams,
     * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt>
     * always corresponds to exactly one instance of
     * <tt>CallPeerMediaHandler</tt> and both classes are only separated for
     * reasons of readability.
     *
     * @return a reference to the <tt>CallPeerMediaHandler</tt> instance that
     * this peer uses for media related tips and tricks.
     */
    public U getMediaHandler()
    {
        return mediaHandler;
    }

    /**
     * Sets a reference to the <tt>CallPeerMediaHandler</tt> used by this
     * peer. The media handler class handles all media management for a single
     * <tt>CallPeer</tt>. This includes initializing and configuring streams,
     * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt>
     * always corresponds to exactly one instance of
     * <tt>CallPeerMediaHandler</tt> and both classes are only separated for
     * reasons of readability.
     *
     * @param mediaHandler a reference to the <tt>CallPeerMediaHandler</tt>
     * instance that this peer uses for media related tips and tricks.
     */
    protected void setMediaHandler(U mediaHandler)
    {
        this.mediaHandler = mediaHandler;
    }
}
