/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A representation of a Call. The Call class must only be created by users (i.e.
 * telephony protocols) of the PhoneUIService such as a SIP protocol
 * implementation. Extensions of this class might have names like CallSipImpl
 * or CallJabberImpl or CallAnyOtherTelephonyProtocolImpl
 *
 * @author Emil Ivov
 * @author Emanuel Onica
 */
public abstract class Call
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(Call.class);

    /**
     * An identifier uniquely representing the call.
     */
    private final String callID;

    /**
     * A list of all listeners currently registered for
     * <tt>CallChangeEvent</tt>s
     */
    private final List<CallChangeListener> callListeners
                                            = new Vector<CallChangeListener>();

    /**
     * A reference to the ProtocolProviderService instance that created us.
     */
    private final ProtocolProviderService protocolProvider;

    /**
     * If this flag is set to true according to the account properties
     * related with the sourceProvider the associated CallSession will start
     * encrypted by default (where applicable)
     */
    private final boolean defaultEncryption;

    /**
     * If this flag is set to true according to the account properties
     * related with the sourceProvider the associated CallSession will set
     * the SIP/SDP attribute (where applicable)
     */
    private final boolean sipZrtpAttribute;

    /**
     * The state that this call is currently in.
     */
    private CallState callState = CallState.CALL_INITIALIZATION;

    /**
     * Creates a new Call instance.
     *
     * @param sourceProvider the proto provider that created us.
     */
    protected Call(ProtocolProviderService sourceProvider)
    {
        //create the uid
        this.callID = String.valueOf(System.currentTimeMillis())
                    + String.valueOf(super.hashCode());

        this.protocolProvider = sourceProvider;

        defaultEncryption =
            protocolProvider.getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_ENCRYPTION, true);
        sipZrtpAttribute =
            protocolProvider.getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE, true);
    }

    /**
     * Returns the id of the specified Call.
     * @return a String uniquely identifying the call.
     */
    public String getCallID()
    {
        return callID;
    }

    /**
     * Compares the specified object with this call and returns true if it the
     * specified object is an instance of a Call object and if the
     * extending telephony protocol considers the calls represented by both
     * objects to be the same.
     *
     * @param obj the call to compare this one with.
     * @return true in case both objects are pertaining to the same call and
     * false otherwise.
     */
    public boolean equals(Object obj)
    {
        if(obj == null
           || !(obj instanceof Call))
            return false;
        return (obj == this)
           || ((Call)obj).getCallID().equals(getCallID());
    }

    /**
     * Returns a hash code value for this call.
     *
     * @return  a hash code value for this call.
     */
    public int hashCode()
    {
        return getCallID().hashCode();
    }

    /**
     * Adds a call change listener to this call so that it could receive events
     * on new call peers, theme changes and others.
     *
     * @param listener the listener to register
     */
    public void addCallChangeListener(CallChangeListener listener)
    {
        synchronized(callListeners)
        {
            if(!callListeners.contains(listener))
                this.callListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> to this call so that it won't receive further
     * <tt>CallChangeEvent</tt>s.
     * @param listener the listener to register
     */
    public void removeCallChangeListener(CallChangeListener listener)
    {
        synchronized(callListeners)
        {
            this.callListeners.remove(listener);
        }
    }

    /**
     * Returns a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this call.
     * @return a reference to the <tt>ProtocolProviderService</tt> instance that
     * created this call.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.protocolProvider;
    }

    /**
     * Creates a <tt>CallPeerEvent</tt> with
     * <tt>sourceCallPeer</tt> and <tt>eventID</tt> and dispatches it on
     * all currently registered listeners.
     *
     * @param sourceCallPeer the source <tt>CallPeer</tt> for the
     * newly created event.
     * @param eventID the ID of the event to create (see CPE member ints)
     */
    protected void fireCallPeerEvent(CallPeer sourceCallPeer,
                                     int      eventID)
    {
        CallPeerEvent cpEvent = new CallPeerEvent(
            sourceCallPeer, this, eventID);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a CallPeer event to "
                     + callListeners.size()
                     +" listeners. event is: " + cpEvent.toString());

        Iterator<CallChangeListener> listeners;
        synchronized(callListeners)
        {
            listeners
                = new ArrayList<CallChangeListener>(callListeners).iterator();
        }

        while(listeners.hasNext())
        {
            CallChangeListener listener = listeners.next();

            if(eventID == CallPeerEvent.CALL_PEER_ADDED)
                listener.callPeerAdded(cpEvent);
            else if (eventID == CallPeerEvent.CALL_PEER_REMOVED)
                listener.callPeerRemoved(cpEvent);

        }
    }

    /**
     * Returns a string textually representing this Call.
     *
     * @return  a string representation of the object.
     */
    public String toString()
    {
        return "Call: id=" + getCallID() + " peers="
                           + getCallPeerCount();
    }

    /**
     * Creates a <tt>CallChangeEvent</tt> with this class as
     * <tt>sourceCall</tt>,  and the specified <tt>eventID</tt> and old and new
     * values and  dispatches it on all currently registered listeners.
     *
     * @param type the type of the event to create (see CallChangeEvent member
     * ints)
     * @param oldValue the value of the call property that changed, before the
     * event had occurred.
     * @param newValue the value of the call property that changed, after the
     * event has occurred.
     */
    protected void fireCallChangeEvent( String type,
                                        Object oldValue,
                                        Object newValue)
    {
        this.fireCallChangeEvent(type, oldValue, newValue, null);
    }

    /**
     * Creates a <tt>CallChangeEvent</tt> with this class as
     * <tt>sourceCall</tt>,  and the specified <tt>eventID</tt> and old and new
     * values and  dispatches it on all currently registered listeners.
     *
     * @param type the type of the event to create (see CallChangeEvent member
     * ints)
     * @param oldValue the value of the call property that changed, before the
     * event had occurred.
     * @param newValue the value of the call property that changed, after the
     * event has occurred.
     * @param cause the event that is the initial cause of the current one.
     */
    protected void fireCallChangeEvent( String type,
                                        Object oldValue,
                                        Object newValue,
                                        CallPeerChangeEvent cause)
    {
        CallChangeEvent ccEvent = new CallChangeEvent(
            this, type, oldValue, newValue, cause);

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a CallChange event to "
                     + callListeners.size()
                     +" listeners. event is: " + ccEvent.toString());

        Iterator<CallChangeListener> listeners;
        synchronized(callListeners)
        {
            listeners
                = new ArrayList<CallChangeListener>(callListeners).iterator();
        }

        while(listeners.hasNext())
        {
            CallChangeListener listener = listeners.next();

            if(type.equals(CallChangeEvent.CALL_STATE_CHANGE))
                listener.callStateChanged(ccEvent);
        }
    }

    /**
     * Returns the state that this call is currently in.
     *
     * @return a reference to the <tt>CallState</tt> instance that the call is
     *         currently in.
     */
    public CallState getCallState()
    {
        return callState;
    }

    /**
     * Sets the state of this call and fires a call change event notifying
     * registered listeners for the change.
     *
     * @param newState a reference to the <tt>CallState</tt> instance that the
     *            call is to enter.
     */
    protected void setCallState(CallState newState)
    {
        this.setCallState(newState, null);
    }

    /**
     * Sets the state of this call and fires a call change event notifying
     * registered listeners for the change.
     *
     * @param newState a reference to the <tt>CallState</tt> instance that the
     *            call is to enter.
     * @param cause the event that is the cause of the current change of state. 
     */
    protected void setCallState(CallState newState, CallPeerChangeEvent cause)
    {
        CallState oldState = getCallState();

        if (oldState != newState)
        {
            this.callState = newState;

            fireCallChangeEvent(
                CallChangeEvent.CALL_STATE_CHANGE, oldState, newState, cause);
        }
    }

    /**
     * Returns the default call encryption flag
     *
     * @return the default call encryption flag
     */
    public boolean isDefaultEncrypted()
    {
        return defaultEncryption;
    }

    /**
     * Check if to include the ZRTP attribute to SIP/SDP
     *
     * @return include the ZRTP attribute to SIP/SDP
     */
    public boolean isSipZrtpAttribute()
    {
        return sipZrtpAttribute;
    }

    /**
     * Returns an iterator over all call peers.
     *
     * @return an Iterator over all peers currently involved in the call.
     */
    public abstract Iterator<? extends CallPeer> getCallPeers();

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of peers currently
     *         associated with this call.
     */
    public abstract int getCallPeerCount();

    /**
     * Gets the indicator which determines whether the local peer represented by
     * this <tt>Call</tt> is acting as a conference focus and thus should send
     * the &quot;isfocus&quot; parameter in the Contact headers of its outgoing
     * SIP signaling.
     *
     * @return <tt>true</tt> if the local peer represented by this <tt>Call</tt>
     * is acting as a conference focus; otherwise, <tt>false</tt>
     */
    public abstract boolean isConferenceFocus();

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    public abstract void addLocalUserSoundLevelListener(SoundLevelListener l);

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    public abstract void removeLocalUserSoundLevelListener(SoundLevelListener l);
}
