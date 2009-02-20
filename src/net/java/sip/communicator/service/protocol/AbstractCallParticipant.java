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
 */
public abstract class AbstractCallParticipant
    implements CallParticipant
{
    private static final Logger logger
        = Logger.getLogger(AbstractCallParticipant.class);

    /**
     * All the CallParticipant listeners registered with this CallParticipant.
     */
    protected final List<CallParticipantListener> callParticipantListeners
                            = new ArrayList<CallParticipantListener>();

    /**
     * All the CallParticipantSecurityListener-s registered with this
     * CallParticipant.
     */
    protected final List<CallParticipantSecurityListener>
        callParticipantSecurityListeners
            = new ArrayList<CallParticipantSecurityListener>();

    /**
     * All the PropertyChangeListener-s registered with this CallParticipant.
     */
    protected final List<PropertyChangeListener> propertyChangeListeners
        = new ArrayList<PropertyChangeListener>();

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
    public void addCallParticipantListener(CallParticipantListener listener)
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
    public void removeCallParticipantListener(CallParticipantListener listener)
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
     * Allows the user interface to register a listener interested in property
     * changes.
     * @param listener a property change listener instance to register with this
     * participant.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            return;
        synchronized(propertyChangeListeners)
        {
            if (!propertyChangeListeners.contains(listener))
                propertyChangeListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified property change listener.
     * 
     * @param listener the property change listener to unregister.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener == null)
            return;
        synchronized(propertyChangeListeners)
        {
            propertyChangeListeners.remove(listener);
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
        CallParticipantChangeEvent evt = new CallParticipantChangeEvent(
            this, eventType, oldValue, newValue, reason);

        logger.debug("Dispatching a CallParticipantChangeEvent event to "
                     + callParticipantListeners.size()
                     +" listeners. event is: " + evt.toString());

        Iterator<CallParticipantListener> listeners = null;
        synchronized (callParticipantListeners)
        {
            listeners = new ArrayList<CallParticipantListener>(
                                callParticipantListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallParticipantListener listener
                = (CallParticipantListener) listeners.next();

            if(eventType.equals(CallParticipantChangeEvent
                                .CALL_PARTICIPANT_ADDRESS_CHANGE))
            {
                listener.participantAddressChanged(evt);
            } else if(eventType.equals(CallParticipantChangeEvent
                                .CALL_PARTICIPANT_DISPLAY_NAME_CHANGE))
            {
                listener.participantDisplayNameChanged(evt);
            } else if(eventType.equals(CallParticipantChangeEvent
                                .CALL_PARTICIPANT_IMAGE_CHANGE))
            {
                listener.participantImageChanged(evt);
            } else if(eventType.equals(CallParticipantChangeEvent
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
        String message,
        String i18nMessage)
    {
        CallParticipantSecurityMessageEvent evt
            = new CallParticipantSecurityMessageEvent(   this,
                                                        messageType,
                                                        message,
                                                        i18nMessage);

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
     * Constructs a <tt>PropertyChangeEvent</tt> using this call
     * participant as source, setting the corresponding <tt>oldValue</tt> and
     * <tt>newValue</tt>.
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     */
    protected void firePropertyChangeEvent( String propertyName,
                                            Object oldValue,
                                            Object newValue)
    {
        PropertyChangeEvent event
            = new PropertyChangeEvent(this, propertyName, oldValue, newValue);

        logger.debug("Dispatching a PropertyChangeEvent event to "
                     + propertyChangeListeners.size()
                     + " listeners. event is: " + event.toString());

        Iterator<PropertyChangeListener> listeners = null;
        synchronized (propertyChangeListeners)
        {
            listeners = new ArrayList<PropertyChangeListener>(
                                propertyChangeListeners).iterator();
        }

        while (listeners.hasNext())
        {
            PropertyChangeListener listener
                = (PropertyChangeListener) listeners.next();

            listener.propertyChange(event);
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
                CallParticipantChangeEvent.CALL_PARTICIPANT_STATE_CHANGE,
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
     *         {@link CallParticipant#CALL_DURATION_START_TIME_UNKNOWN} if such
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
        firePropertyChangeEvent(MUTE_PROPERTY_NAME, isMute, newMuteValue);

        this.isMute = newMuteValue;
    }

    /**
     * Sets the security status for this call participant.
     * 
     * @param isSecurityOn <code>true</code> to indicate that the security is
     * turned on and <code>false</code> - otherwise.
     * @param sessionType the type of the call session - audio or video.
     */
    public void setSecurityOn(  boolean isSecurityOn,
                                int sessionType,
                                String cipher,
                                String securityString,
                                boolean isVerified)
    {
        if (isSecurityOn)
            fireCallParticipantSecurityOnEvent(
                sessionType,
                cipher,
                securityString,
                isVerified);
    }

    public void setSecurityOff(int sessionType)
    {
        fireCallParticipantSecurityOffEvent(sessionType);
    }

    /**
     * Sets the security message associated with a failure/warning or
     * information coming from the encryption protocol.
     * 
     * @param messageType the type of the message.
     * @param message the message
     */
    public void setSecurityMessage( String messageType,
                                    String message,
                                    String i18nMessage)
    {
        fireCallParticipantSecurityMessageEvent(messageType,
                                                message,
                                                i18nMessage);
    }
}
