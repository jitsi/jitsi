/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Provides a default implementation for most of the
 * <code>CallPeer</code> methods with the purpose of only leaving custom
 * protocol development to clients using the PhoneUI service.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public abstract class AbstractCallPeer
    extends PropertyChangeNotifier
    implements CallPeer
{
    private static final Logger logger
        = Logger.getLogger(AbstractCallPeer.class);

    /**
     * The constant which describes an empty set of
     * <code>ConferenceMember</code>s (and which can be used to reduce
     * allocations).
     */
    protected static final ConferenceMember[] NO_CONFERENCE_MEMBERS
        = new ConferenceMember[0];

    /**
     * All the CallPeer listeners registered with this CallPeer.
     */
    protected final List<CallPeerListener> callPeerListeners
                            = new ArrayList<CallPeerListener>();

    /**
     * All the CallPeerSecurityListener-s registered with this
     * CallPeer.
     */
    protected final List<CallPeerSecurityListener>
        callPeerSecurityListeners
            = new ArrayList<CallPeerSecurityListener>();

    /**
     * The indicator which determines whether this peer is acting as a
     * conference focus and thus may provide information about
     * <code>ConferenceMember</code> such as {@link #getConferenceMembers()} and
     * {@link #getConferenceMemberCount()}.
     */
    private boolean conferenceFocus;

    /**
     * The list of <code>ConferenceMember</code>s currently known to and managed
     * in a conference by this peer.
     */
    private final List<ConferenceMember> conferenceMembers
        = new ArrayList<ConferenceMember>();

    /**
     * The list of <code>CallPeerConferenceListener</code>s interested in
     * and to be notified about changes in conference-related information such
     * as this peer acting or not acting as a conference focus and
     * conference membership details.
     */
    protected final List<CallPeerConferenceListener>
        callPeerConferenceListeners
            = new ArrayList<CallPeerConferenceListener>();

    /**
     * The state of the call peer.
     */
    private CallPeerState state = CallPeerState.UNKNOWN;

    private long callDurationStartTime = CALL_DURATION_START_TIME_UNKNOWN;

    private boolean isMute;

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallPeerEvents
     * @param listener a listener instance to register with this peer.
     */
    public void addCallPeerListener(CallPeerListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerListeners)
        {
            if (!callPeerListeners.contains(listener))
                callPeerListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     * @param listener the listener to unregister.
     */
    public void removeCallPeerListener(CallPeerListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerListeners)
        {
            callPeerListeners.remove(listener);
        }
    }

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallPeerSecurityEvents
     *
     * @param listener a listener instance to register with this peer.
     */
    public void addCallPeerSecurityListener(
        CallPeerSecurityListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerSecurityListeners)
        {
            if (!callPeerSecurityListeners.contains(listener))
                callPeerSecurityListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     *
     * @param listener the listener to unregister.
     */
    public void removeCallPeerSecurityListener(
        CallPeerSecurityListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerSecurityListeners)
        {
            callPeerSecurityListeners.remove(listener);
        }
    }

    /**
     * Constructs a <tt>CallPeerChangeEvent</tt> using this call
     * peer as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     */
    protected void fireCallPeerChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue)
    {
        this.fireCallPeerChangeEvent( eventType, oldValue, newValue, null);
    }


    /**
     * Constructs a <tt>CallPeerChangeEvent</tt> using this call
     * peer as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    protected void fireCallPeerChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue,
                                                  String reason)
    {
        CallPeerChangeEvent evt = new CallPeerChangeEvent(
            this, eventType, oldValue, newValue, reason);

        logger.debug("Dispatching a CallPeerChangeEvent event to "
                     + callPeerListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallPeerListener> listeners = null;
        synchronized (callPeerListeners)
        {
            listeners = new ArrayList<CallPeerListener>(
                                callPeerListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerListener listener = listeners.next();

            if(eventType.equals(CallPeerChangeEvent
                                .CALL_PEER_ADDRESS_CHANGE))
            {
                listener.peerAddressChanged(evt);
            } else if(eventType.equals(CallPeerChangeEvent
                                .CALL_PEER_DISPLAY_NAME_CHANGE))
            {
                listener.peerDisplayNameChanged(evt);
            } else if(eventType.equals(CallPeerChangeEvent
                                .CALL_PEER_IMAGE_CHANGE))
            {
                listener.peerImageChanged(evt);
            } else if(eventType.equals(CallPeerChangeEvent
                                .CALL_PEER_STATE_CHANGE))
            {
                listener.peerStateChanged(evt);
            }
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call
     * peer as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param sessionType the type of the session - audio or video
     */
    protected void fireCallPeerSecurityOnEvent(
        int sessionType,
        String cipher,
        String securityString,
        boolean isVerified)
    {
        CallPeerSecurityOnEvent evt
            = new CallPeerSecurityOnEvent(   this,
                                                    sessionType,
                                                    cipher,
                                                    securityString,
                                                    isVerified);

        logger.debug("Dispatching a CallPeerSecurityStatusEvent event to "
                     + callPeerSecurityListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<CallPeerSecurityListener>(
                                callPeerSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerSecurityListener listener = listeners.next();

            listener.securityOn(evt);
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call
     * peer as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param sessionType the type of the session - audio or video
     */
    protected void fireCallPeerSecurityOffEvent(int sessionType)
    {
        CallPeerSecurityOffEvent event
            = new CallPeerSecurityOffEvent( this, sessionType);

        logger.debug(
            "Dispatching a CallPeerSecurityAuthenticationEvent event to "
                     + callPeerSecurityListeners.size()
                     +" listeners. event is: " + event.toString());

        Iterator<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<CallPeerSecurityListener>(
                                callPeerSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerSecurityListener listener = listeners.next();

            listener.securityOff(event);
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call
     * peer as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param messageType the type of the message
     * @param i18nMessage message
     * @param severity severity level
     */
    protected void fireCallPeerSecurityMessageEvent(
        String messageType,
        String i18nMessage,
        int severity)
    {
        CallPeerSecurityMessageEvent evt
            = new CallPeerSecurityMessageEvent(   this,
                                                        messageType,
                                                        i18nMessage,
                                                        severity);

        logger.debug("Dispatching a CallPeerSecurityFailedEvent event to "
                     + callPeerSecurityListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<CallPeerSecurityListener>(
                                callPeerSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerSecurityListener listener = listeners.next();

            listener.securityMessageRecieved(evt);
        }
    }

    /**
     * Returns a string representation of the peer in the form of
     * <br/>
     * Display Name &lt;address&gt;;status=CallPeerStatus
     * @return a string representation of the peer and its state.
     */
    public String toString()
    {
        return getDisplayName() + " <" + getAddress()
            + ">;status=" + getState().getStateString();
    }

    /**
     * Returns a URL pointing ta a location with call control information for
     * this peer or <tt>null</tt> if no such URL is available for this
     * call peer.
     *
     * @return a URL link to a location with call information or a call control
     * web interface related to this peer or <tt>null</tt> if no such URL
     * is available.
     */
    public URL getCallInfoURL()
    {
        //if signaling protocols (such as SIP) know where to get this URL from
        //they should override this method
        return null;
    }

    /**
     * Returns an object representing the current state of that peer.
     *
     * @return a CallPeerState instance representing the peer's state.
     */
    public CallPeerState getState()
    {
        return state;
    }

    /**
     * Causes this CallPeer to enter the specified state. The method also
     * sets the currentStateStartDate field and fires a
     * CallPeerChangeEvent.
     *
     * @param newState the state this call peer should enter.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    public void setState(CallPeerState newState, String reason)
    {
        CallPeerState oldState = getState();

        if(oldState == newState)
            return;

        this.state = newState;

        if (CallPeerState.CONNECTED.equals(newState)
            && !CallPeerState.isOnHold(oldState))
        {
            callDurationStartTime = System.currentTimeMillis();
        }

        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_STATE_CHANGE,
                oldState,
                newState);
    }

    /**
     * Causes this CallPeer to enter the specified state. The method also
     * sets the currentStateStartDate field and fires a
     * CallPeerChangeEvent.
     *
     * @param newState the state this call peer should enter.
     */
    public void setState(CallPeerState newState)
    {
        setState(newState, null);
    }

    /**
     * Gets the time at which this <code>CallPeer</code> transitioned
     * into a state (likely {@link CallPeerState#CONNECTED}) marking the
     * start of the duration of the participation in a <code>Call</code>.
     *
     * @return the time at which this <code>CallPeer</code> transitioned
     *         into a state marking the start of the duration of the
     *         participation in a <code>Call</code> or
     *         {@link CallPeer#CALL_DURATION_START_TIME_UNKNOWN} if such
     *         a transition has not been performed
     */
    public long getCallDurationStartTime()
    {
        return callDurationStartTime;
    }

    /**
     * Determines whether the audio stream (if any) being sent to this
     * peer is mute.
     * <p>
     * The default implementation returns <tt>false</tt>.
     * </p>
     *
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         peer and it is currently mute; <tt>false</tt>, otherwise
     */
    public boolean isMute()
    {
        return false;
    }

    /**
     * Sets the mute property for this call peer.
     *
     * @param newMuteValue the new value of the mute property for this call peer
     */
    public void setMute(boolean newMuteValue)
    {
        firePropertyChange(MUTE_PROPERTY_NAME, isMute, newMuteValue);

        this.isMute = newMuteValue;
    }

    /*
     * Implements CallPeer#isConferenceFocus().
     */
    public boolean isConferenceFocus()
    {
        return conferenceFocus;
    }

    public void setConferenceFocus(boolean conferenceFocus)
    {
        if (this.conferenceFocus != conferenceFocus)
        {
            this.conferenceFocus = conferenceFocus;

            fireCallPeerConferenceEvent(
                new CallPeerConferenceEvent(
                        this,
                        CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED));
        }
    }

    /*
     * Implements CallPeer#getConferenceMembers(). In order to reduce
     * allocations, returns #NO_CONFERENCE_MEMBERS if #conferenceMembers
     * contains no ConferenceMember instances.
     */
    public ConferenceMember[] getConferenceMembers()
    {
        ConferenceMember[] conferenceMembers;

        synchronized (this.conferenceMembers)
        {
            int conferenceMemberCount = this.conferenceMembers.size();

            if (conferenceMemberCount <= 0)
                conferenceMembers = NO_CONFERENCE_MEMBERS;
            else
                conferenceMembers
                    = this.conferenceMembers
                            .toArray(
                                new ConferenceMember[conferenceMemberCount]);
        }
        return conferenceMembers;
    }

    /*
     * Implements CallPeer#getConferenceMemberCount().
     */
    public int getConferenceMemberCount()
    {
        return conferenceMembers.size();
    }

    /**
     * Adds a specific <code>ConferenceMember</code> to the list of
     * <code>ConferenceMember</code>s reported by this peer through
     * {@link #getConferenceMembers()} and {@link #getConferenceMemberCount()}
     * and fires
     * <code>CallPeerConferenceEvent#CONFERENCE_MEMBER_ADDED</code> to
     * the currently registered <code>CallPeerConferenceListener</code>s.
     *
     * @param conferenceMember
     *            a <code>ConferenceMember</code> to be added to the list of
     *            <code>ConferenceMember</code> reported by this peer. If
     *            the specified <code>ConferenceMember</code> is already
     *            contained in the list, it is not added again and no event is
     *            fired.
     */
    public void addConferenceMember(ConferenceMember conferenceMember)
    {
        if (conferenceMember == null)
            throw new NullPointerException("conferenceMember");
        synchronized (conferenceMembers)
        {
            if (conferenceMembers.contains(conferenceMember))
                return;
            conferenceMembers.add(conferenceMember);
        }
        fireCallPeerConferenceEvent(
            new CallPeerConferenceEvent(
                    this,
                    CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED,
                    conferenceMember));
    }

    /**
     * Removes a specific <code>ConferenceMember</code> from the list of
     * <code>ConferenceMember</code>s reported by this peer through
     * {@link #getConferenceMembers()} and {@link #getConferenceMemberCount()}
     * if it is contained and fires
     * <code>CallPeerConferenceEvent#CONFERENCE_MEMBER_REMOVED</code> to
     * the currently registered <code>CallPeerConferenceListener</code>s.
     *
     * @param conferenceMember
     *            a <code>ConferenceMember</code> to be removed from the list of
     *            <code>ConferenceMember</code> reported by this peer. If
     *            the specified <code>ConferenceMember</code> is no contained in
     *            the list, no event is fired.
     */
    public void removeConferenceMember(ConferenceMember conferenceMember)
    {
        if (conferenceMember == null)
            throw new NullPointerException("conferenceMember");
        synchronized (conferenceMembers)
        {
            if (!conferenceMembers.remove(conferenceMember))
                return;
        }
        fireCallPeerConferenceEvent(
            new CallPeerConferenceEvent(
                    this,
                    CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED,
                    conferenceMember));
    }

    /*
     * ImplementsCallPeer#addCallPeerConferenceListener(
     * CallPeerConferenceListener). In the fashion of the addition of the
     * other listeners, does not throw an exception on attempting to add a null
     * listeners and just ignores the call.
     */
    public void addCallPeerConferenceListener(
        CallPeerConferenceListener listener)
    {
        if (listener != null)
            synchronized (callPeerConferenceListeners)
            {
                if (!callPeerConferenceListeners.contains(listener))
                    callPeerConferenceListeners.add(listener);
            }
    }

    /*
     * Implements CallPeer#removeCallPeerConferenceListener(
     * CallPeerConferenceListener).
     */
    public void removeCallPeerConferenceListener(
        CallPeerConferenceListener listener)
    {
        if (listener != null)
            synchronized (callPeerConferenceListeners)
            {
                callPeerConferenceListeners.remove(listener);
            }
    }

    /**
     * Fires a specific <code>CallPeerConferenceEvent</code> to the
     * <code>CallPeerConferenceListener</code>s interested in changes in
     * the conference-related information provided by this peer.
     *
     * @param conferenceEvent
     *            a <code>CallPeerConferenceEvent</code> to be fired and
     *            carrying the event data
     */
    protected void fireCallPeerConferenceEvent(
        CallPeerConferenceEvent conferenceEvent)
    {
        CallPeerConferenceListener[] listeners;

        synchronized (callPeerConferenceListeners)
        {
            listeners
                = callPeerConferenceListeners
                    .toArray(
                        new CallPeerConferenceListener[
                                callPeerConferenceListeners.size()]);
        }

        int eventID = conferenceEvent.getEventID();

        for (CallPeerConferenceListener listener : listeners)
            switch (eventID)
            {
            case CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED:
                listener.conferenceFocusChanged(conferenceEvent);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
                listener.conferenceMemberAdded(conferenceEvent);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
                listener.conferenceMemberRemoved(conferenceEvent);
                break;
            }
    }
}
