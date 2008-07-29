/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;
import java.net.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;


/**
 * The DefaultCallParticipant provides a default implementation for most of the
 * CallParticpant methods with the purpose of only leaving custom protocol
 * development to clients using the PhoneUI service.
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
    protected ArrayList callParticipantListeners = new ArrayList();

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallParticipantEvents
     * @param listener a listener instance to register with this participant.
     */
    public void addCallParticipantListener(CallParticipantListener listener)
    {
        synchronized(callParticipantListeners)
        {
            if (!callParticipantListeners.contains(listener))
                this.callParticipantListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     * @param listener the listener to unregister.
     */
    public void removeCallParticipantListener(CallParticipantListener listener)
    {
        synchronized(callParticipantListeners)
        {
            if (listener == null)
                return;
            callParticipantListeners.remove(listener);
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

        Iterator listeners = null;
        synchronized (callParticipantListeners)
        {
            listeners = new ArrayList(callParticipantListeners).iterator();
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
     * Returns a string representation of the participant in the form of
     * <br>
     * Display Name <address>;status=CallParticipantStatus
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
}
