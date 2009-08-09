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
 * <code>CallParticipant</code> methods with the purpose of only leaving custom
 * protocol development to clients using the PhoneUI service.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public abstract class AbstractCallParticipant
    extends PropertyChangeNotifier
    implements CallPeer
{
    private static final Logger logger
        = Logger.getLogger(AbstractCallParticipant.class);

    /**
     * The constant which describes an empty set of
     * <code>ConferenceMember</code>s (and which can be used to reduce
     * allocations).
     */
    protected static final ConferenceMember[] NO_CONFERENCE_MEMBERS
        = new ConferenceMember[0];

    /**
     * All the CallParticipant listeners registered with this CallParticipant.
     */
    protected final List<CallPeerListener> callParticipantListeners
                            = new ArrayList<CallPeerListener>();

    /**
     * All the CallParticipantSecurityListener-s registered with this
     * CallParticipant.
     */
    protected final List<CallParticipantSecurityListener>
        callParticipantSecurityListeners
            = new ArrayList<CallParticipantSecurityListener>();

    /**
     * The indicator which determines whether this participant is acting as a
     * conference focus and thus may provide information about
     * <code>ConferenceMember</code> such as {@link #getConferenceMembers()} and
     * {@link #getConferenceMemberCount()}.
     */
    private boolean conferenceFocus;

    /**
     * The list of <code>ConferenceMember</code>s currently known to and managed
     * in a conference by this participant.
     */
    private final List<ConferenceMember> conferenceMembers
        = new ArrayList<ConferenceMember>();

    /**
     * The list of <code>CallParticipantConferenceListener</code>s interested in
     * and to be notified about changes in conference-related information such
     * as this participant acting or not acting as a conference focus and
     * conference membership details.
     */
    protected final List<CallParticipantConferenceListener>
        callParticipantConferenceListeners
            = new ArrayList<CallParticipantConferenceListener>();

    /**
     * The state of the call participant.
     */
    private CallParticipantState state = CallParticipantState.UNKNOWN;

    private long callDurationStartTime = CALL_DURATION_START_TIME_UNKNOWN;

    private boolean isMute;

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallParticipantEvents
     * @param listener a listener instance to register with this participant.
     */
    public void addCallParticipantListener(CallPeerListener listener)
    {
        if (listener == null)
            return;
        synchronized(callParticipantListeners)
        {
            if (!callParticipantListeners.contains(listener))
                callParticipantListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     * @param listener the listener to unregister.
     */
    public void removeCallParticipantListener(CallPeerListener listener)
    {
        if (listener == null)
            return;
        synchronized(callParticipantListeners)
        {
            callParticipantListeners.remove(listener);
        }
    }

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallParticipantSecurityEvents
     * 
     * @param listener a listener instance to register with this participant.
     */
    public void addCallParticipantSecurityListener(
        CallParticipantSecurityListener listener)
    {
        if (listener == null)
            return;
        synchronized(callParticipantSecurityListeners)
        {
            if (!callParticipantSecurityListeners.contains(listener))
                callParticipantSecurityListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     * 
     * @param listener the listener to unregister.
     */
    public void removeCallParticipantSecurityListener(
        CallParticipantSecurityListener listener)
    {
        if (listener == null)
            return;
        synchronized(callParticipantSecurityListeners)
        {
            callParticipantSecurityListeners.remove(listener);
        }
    }

    /**
     * Constructs a <tt>CallParticipantChangeEvent</tt> using this call
     * participant as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     */
    protected void fireCallParticipantChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue)
    {
        this.fireCallParticipantChangeEvent(
            eventType, oldValue, newValue, null);
    }


    /**
     * Constructs a <tt>CallParticipantChangeEvent</tt> using this call
     * participant as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    protected void fireCallParticipantChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue,
                                                  String reason)
    {
        CallPeerChangeEvent evt = new CallPeerChangeEvent(
            this, eventType, oldValue, newValue, reason);

        logger.debug("Dispatching a CallParticipantChangeEvent event to "
                     + callParticipantListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallPeerListener> listeners = null;
        synchronized (callParticipantListeners)
        {
            listeners = new ArrayList<CallPeerListener>(
                                callParticipantListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerListener listener
                = (CallPeerListener) listeners.next();

            if(eventType.equals(CallPeerChangeEvent
                                .CALL_PARTICIPANT_ADDRESS_CHANGE))
            {
                listener.participantAddressChanged(evt);
            } else if(eventType.equals(CallPeerChangeEvent
                                .CALL_PARTICIPANT_DISPLAY_NAME_CHANGE))
            {
                listener.participantDisplayNameChanged(evt);
            } else if(eventType.equals(CallPeerChangeEvent
                                .CALL_PARTICIPANT_IMAGE_CHANGE))
            {
                listener.participantImageChanged(evt);
            } else if(eventType.equals(CallPeerChangeEvent
                                .CALL_PARTICIPANT_STATE_CHANGE))
            {
                listener.participantStateChanged(evt);
            }
        }
    }

    /**
     * Constructs a <tt>CallParticipantSecurityStatusEvent</tt> using this call
     * participant as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param sessionType the type of the session - audio or video
     * @param eventID the identifier of the event
     */
    protected void fireCallParticipantSecurityOnEvent(
        int sessionType,
        String cipher,
        String securityString,
        boolean isVerified)
    {
        CallParticipantSecurityOnEvent evt
            = new CallParticipantSecurityOnEvent(   this,
                                                    sessionType,
                                                    cipher,
                                                    securityString,
                                                    isVerified);

        logger.debug("Dispatching a CallParticipantSecurityStatusEvent event to "
                     + callParticipantSecurityListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallParticipantSecurityListener> listeners = null;
        synchronized (callParticipantSecurityListeners)
        {
            listeners = new ArrayList<CallParticipantSecurityListener>(
                                callParticipantSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallParticipantSecurityListener listener
                = (CallParticipantSecurityListener) listeners.next();

            listener.securityOn(evt);
        }
    }

    /**
     * Constructs a <tt>CallParticipantSecurityStatusEvent</tt> using this call
     * participant as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param sessionType the type of the session - audio or video
     * @param eventID the identifier of the event
     */
    protected void fireCallParticipantSecurityOffEvent(int sessionType)
    {
        CallParticipantSecurityOffEvent event
            = new CallParticipantSecurityOffEvent(   this,
                                                     sessionType);

        logger.debug(
            "Dispatching a CallParticipantSecurityAuthenticationEvent event to "
                     + callParticipantSecurityListeners.size()
                     +" listeners. event is: " + event.toString());

        Iterator<CallParticipantSecurityListener> listeners = null;
        synchronized (callParticipantSecurityListeners)
        {
            listeners = new ArrayList<CallParticipantSecurityListener>(
                                callParticipantSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallParticipantSecurityListener listener
                = (CallParticipantSecurityListener) listeners.next();

            listener.securityOff(event);
        }
    }

    /**
     * Constructs a <tt>CallParticipantSecurityStatusEvent</tt> using this call
     * participant as source, setting it to be of type <tt>eventType</tt> and
     * the corresponding <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param sessionType the type of the session - audio or video
     * @param eventID the identifier of the event
     */
    protected void fireCallParticipantSecurityMessageEvent(
        String messageType,
        String i18nMessage,
        int severity)
    {
        CallParticipantSecurityMessageEvent evt
            = new CallParticipantSecurityMessageEvent(   this,
                                                        messageType,
                                                        i18nMessage,
                                                        severity);

        logger.debug("Dispatching a CallParticipantSecurityFailedEvent event to "
                     + callParticipantSecurityListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallParticipantSecurityListener> listeners = null;
        synchronized (callParticipantSecurityListeners)
        {
            listeners = new ArrayList<CallParticipantSecurityListener>(
                                callParticipantSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallParticipantSecurityListener listener
                = (CallParticipantSecurityListener) listeners.next();

            listener.securityMessageRecieved(evt);
        }
    }

    /**
     * Returns a string representation of the participant in the form of
     * <br/>
     * Display Name &lt;address&gt;;status=CallParticipantStatus
     * @return a string representation of the participant and its state.
     */
    public String toString()
    {
        return getDisplayName() + " <" + getAddress()
            + ">;status=" + getState().getStateString();
    }

    /**
     * Returns a URL pointing ta a location with call control information for
     * this participant or <tt>null</tt> if no such URL is available for this
     * call participant.
     *
     * @return a URL link to a location with call information or a call control
     * web interface related to this participant or <tt>null</tt> if no such URL
     * is available.
     */
    public URL getCallInfoURL()
    {
        //if signaling protocols (such as SIP) know where to get this URL from
        //they should override this method
        return null;
    }

    /**
     * Returns an object representing the current state of that participant.
     *
     * @return a CallParticipantState instance representin the participant's
     *   state.
     */
    public CallParticipantState getState()
    {
        return state;
    }

    /**
     * Causes this CallParticipant to enter the specified state. The method also
     * sets the currentStateStartDate field and fires a
     * CallParticipantChangeEvent.
     *
     * @param newState the state this call participant should enter.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    public void setState(CallParticipantState newState, String reason)
    {
        CallParticipantState oldState = getState();

        if(oldState == newState)
            return;

        this.state = newState;

        if (CallParticipantState.CONNECTED.equals(newState)
            && !CallParticipantState.isOnHold(oldState))
        {
            callDurationStartTime = System.currentTimeMillis();
        }

        fireCallParticipantChangeEvent(
                CallPeerChangeEvent.CALL_PARTICIPANT_STATE_CHANGE,
                oldState,
                newState);
    }

    /**
     * Causes this CallParticipant to enter the specified state. The method also
     * sets the currentStateStartDate field and fires a
     * CallParticipantChangeEvent.
     *
     * @param newState the state this call participant should enter.
     */
    public void setState(CallParticipantState newState)
    {
        setState(newState, null);
    }

    /**
     * Gets the time at which this <code>CallParticipant</code> transitioned
     * into a state (likely {@link CallParticipantState#CONNECTED}) marking the
     * start of the duration of the participation in a <code>Call</code>.
     *
     * @return the time at which this <code>CallParticipant</code> transitioned
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
     * participant is mute.
     * <p>
     * The default implementation returns <tt>false</tt>.
     * </p>
     *
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         participant and it is currently mute; <tt>false</tt>, otherwise
     */
    public boolean isMute()
    {
        return false;
    }

    /**
     * Sets the mute property for this call participant.
     * 
     * @param mute the new value of the mute property for this call participant
     */
    public void setMute(boolean newMuteValue)
    {
        firePropertyChange(MUTE_PROPERTY_NAME, isMute, newMuteValue);

        this.isMute = newMuteValue;
    }

    /*
     * Implements CallParticipant#isConferenceFocus().
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

            fireCallParticipantConferenceEvent(
                new CallPeerConferenceEvent(
                        this,
                        CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED));
        }
    }

    /*
     * Implements CallParticipant#getConferenceMembers(). In order to reduce
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
     * Implements CallParticipant#getConferenceMemberCount().
     */
    public int getConferenceMemberCount()
    {
        return conferenceMembers.size();
    }

    /**
     * Adds a specific <code>ConferenceMember</code> to the list of
     * <code>ConferenceMember</code>s reported by this participant through
     * {@link #getConferenceMembers()} and {@link #getConferenceMemberCount()}
     * and fires
     * <code>CallParticipantConferenceEvent#CONFERENCE_MEMBER_ADDED</code> to
     * the currently registered <code>CallParticipantConferenceListener</code>s.
     * 
     * @param conferenceMember
     *            a <code>ConferenceMember</code> to be added to the list of
     *            <code>ConferenceMember</code> reported by this participant. If
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
        fireCallParticipantConferenceEvent(
            new CallPeerConferenceEvent(
                    this,
                    CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED,
                    conferenceMember));
    }

    /**
     * Removes a specific <code>ConferenceMember</code> from the list of
     * <code>ConferenceMember</code>s reported by this participant through
     * {@link #getConferenceMembers()} and {@link #getConferenceMemberCount()}
     * if it is contained and fires
     * <code>CallParticipantConferenceEvent#CONFERENCE_MEMBER_REMOVED</code> to
     * the currently registered <code>CallParticipantConferenceListener</code>s.
     * 
     * @param conferenceMember
     *            a <code>ConferenceMember</code> to be removed from the list of
     *            <code>ConferenceMember</code> reported by this participant. If
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
        fireCallParticipantConferenceEvent(
            new CallPeerConferenceEvent(
                    this,
                    CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED,
                    conferenceMember));
    }

    /*
     * ImplementsCallParticipant#addCallParticipantConferenceListener(
     * CallParticipantConferenceListener). In the fashion of the addition of the
     * other listeners, does not throw an exception on attempting to add a null
     * listeners and just ignores the call.
     */
    public void addCallParticipantConferenceListener(
        CallParticipantConferenceListener listener)
    {
        if (listener != null)
            synchronized (callParticipantConferenceListeners)
            {
                if (!callParticipantConferenceListeners.contains(listener))
                    callParticipantConferenceListeners.add(listener);
            }
    }

    /*
     * Implements CallParticipant#removeCallParticipantConferenceListener(
     * CallParticipantConferenceListener).
     */
    public void removeCallParticipantConferenceListener(
        CallParticipantConferenceListener listener)
    {
        if (listener != null)
            synchronized (callParticipantConferenceListeners)
            {
                callParticipantConferenceListeners.remove(listener);
            }
    }

    /**
     * Fires a specific <code>CallParticipantConferenceEvent</code> to the
     * <code>CallParticipantConferenceListener</code>s interested in changes in
     * the conference-related information provided by this participant.
     * 
     * @param conferenceEvent
     *            a <code>CallParticipantConferenceEvent</code> to be fired and
     *            carrying the event data
     */
    protected void fireCallParticipantConferenceEvent(
        CallPeerConferenceEvent conferenceEvent)
    {
        CallParticipantConferenceListener[] listeners;

        synchronized (callParticipantConferenceListeners)
        {
            listeners
                = callParticipantConferenceListeners
                    .toArray(
                        new CallParticipantConferenceListener[
                                callParticipantConferenceListeners.size()]);
        }

        int eventID = conferenceEvent.getEventID();

        for (CallParticipantConferenceListener listener : listeners)
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
