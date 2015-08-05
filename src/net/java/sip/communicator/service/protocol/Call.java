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
package net.java.sip.communicator.service.protocol;

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A representation of a call. <tt>Call</tt> instances must only be created by
 * users (i.e. telephony protocols) of the PhoneUIService such as a SIP protocol
 * implementation. Extensions of this class might have names like
 * <tt>CallSipImpl</tt>, <tt>CallJabberImpl</tt>, or
 * <tt>CallAnyOtherTelephonyProtocolImpl</tt>.
 * Call is DataObject, this way it will be able to store custom data and carry
 * it to various parts of the project.
 *
 * @author Emil Ivov
 * @author Emanuel Onica
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public abstract class Call
    extends DataObject
{
    /**
     * Our class logger.
     */
    private static final Logger logger = Logger.getLogger(Call.class);

    /**
     * The name of the <tt>Call</tt> property which represents its telephony
     * conference-related state.
     */
    public static final String CONFERENCE = "conference";

    /**
     * The name of the <tt>Call</tt> property which indicates whether the local
     * peer/user represented by the respective <tt>Call</tt> is acting as a
     * conference focus.
     */
    public static final String CONFERENCE_FOCUS
        = "conferenceFocus";

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
     * The telephony conference-related state of this <tt>Call</tt>. Since a
     * non-conference <tt>Call</tt> may be converted into a conference
     * <tt>Call</tt> at any time, every <tt>Call</tt> instance maintains a
     * <tt>CallConference</tt> instance regardless of whether the <tt>Call</tt>
     * in question is participating in a telephony conference.
     */
    private CallConference conference;

    /**
     * The flag that specifies whether incoming calls into this <tt>Call</tt>
     * should be auto-answered.
     */
    private boolean isAutoAnswer = false;

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

        AccountID accountID = protocolProvider.getAccountID();

        defaultEncryption
            = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                    true);
        sipZrtpAttribute
            = accountID.getAccountPropertyBoolean(
                    ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                    true);
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
    @Override
    public boolean equals(Object obj)
    {
        if ((obj == null) || !(obj instanceof Call))
            return false;
        return (obj == this) || ((Call)obj).getCallID().equals(getCallID());
    }

    /**
     * Returns a hash code value for this call.
     *
     * @return  a hash code value for this call.
     */
    @Override
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
                callListeners.add(listener);
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
            callListeners.remove(listener);
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
    * @param eventID the ID of the event to create (see constants defined in
    * <tt>CallPeerEvent</tt>)
    */
    protected void fireCallPeerEvent(CallPeer sourceCallPeer, int eventID)
    {
        fireCallPeerEvent(sourceCallPeer, eventID, false);
    }

    /**
     * Creates a <tt>CallPeerEvent</tt> with
     * <tt>sourceCallPeer</tt> and <tt>eventID</tt> and dispatches it on
     * all currently registered listeners.
     *
     * @param sourceCallPeer the source <tt>CallPeer</tt> for the
     * newly created event.
     * @param eventID the ID of the event to create (see constants defined in
     * <tt>CallPeerEvent</tt>)
     * @param delayed <tt>true</tt> if the adding/removing of the peer from the
     * GUI should be delayed and <tt>false</tt> if not.
     */
    protected void fireCallPeerEvent(CallPeer sourceCallPeer,
                                     int eventID,
                                     boolean delayed)
    {
        CallPeerEvent event
            = new CallPeerEvent(sourceCallPeer, this, eventID, delayed);

        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Dispatching a CallPeer event to "
                     + callListeners.size()
                     +" listeners. The event is: "
                     + event);
        }

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
                listener.callPeerAdded(event);
            else if (eventID == CallPeerEvent.CALL_PEER_REMOVED)
                listener.callPeerRemoved(event);

        }
    }

    /**
     * Returns a string textually representing this Call.
     *
     * @return  a string representation of the object.
     */
    @Override
    public String toString()
    {
        return "Call: id=" + getCallID() + " peers=" + getCallPeerCount();
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
        fireCallChangeEvent(type, oldValue, newValue, null);
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
        CallChangeEvent event
            = new CallChangeEvent(
                    this,
                    type,
                    oldValue, newValue,
                    cause);

        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Dispatching a CallChange event to "
                        + callListeners.size()
                        + " listeners. The event is: "
                        + event);
        }

        CallChangeListener[] listeners;

        synchronized(callListeners)
        {
            listeners
                = callListeners.toArray(
                        new CallChangeListener[callListeners.size()]);
        }
        for (CallChangeListener listener : listeners)
            listener.callStateChanged(event);
    }

    /**
     * Returns the state that this call is currently in.
     *
     * @return a reference to the <tt>CallState</tt> instance that the call is
     * currently in.
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
     * call is to enter.
     */
    protected void setCallState(CallState newState)
    {
        setCallState(newState, null);
    }

    /**
     * Sets the state of this <tt>Call</tt> and fires a new
     * <tt>CallChangeEvent</tt> notifying the registered
     * <tt>CallChangeListener</tt>s about the change of the state.
     *
     * @param newState the <tt>CallState</tt> into which this <tt>Call</tt> is
     * to enter
     * @param cause the <tt>CallPeerChangeEvent</tt> which is the cause for the
     * request to have this <tt>Call</tt> enter the specified <tt>CallState</tt>
     */
    protected void setCallState(CallState newState, CallPeerChangeEvent cause)
    {
        CallState oldState = getCallState();

        if (oldState != newState)
        {
            this.callState = newState;

            try
            {
                fireCallChangeEvent(
                        CallChangeEvent.CALL_STATE_CHANGE,
                        oldState, this.callState,
                        cause);
            }
            finally
            {
                if (CallState.CALL_ENDED.equals(getCallState()))
                    setConference(null);
            }
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
     * associated with this call.
     */
    public abstract int getCallPeerCount();

    /**
     * Gets the indicator which determines whether the local peer represented by
     * this <tt>Call</tt> is acting as a conference focus. In the case of SIP,
     * for example, it determines whether the local peer should send the
     * &quot;isfocus&quot; parameter in the Contact headers of its outgoing SIP
     * signaling.
     *
     * @return <tt>true</tt> if the local peer represented by this <tt>Call</tt>
     * is acting as a conference focus; otherwise, <tt>false</tt>
     */
    public abstract boolean isConferenceFocus();

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * information.
     *
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    public abstract void addLocalUserSoundLevelListener(SoundLevelListener l);

    /**
     * Removes a specific <tt>SoundLevelListener</tt> from the list of
     * listeners interested in and notified about changes in local sound level
     * information.
     *
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    public abstract void removeLocalUserSoundLevelListener(
            SoundLevelListener l);

    /**
     * Creates a new <tt>CallConference</tt> instance which is to represent the
     * telephony conference-related state of this <tt>Call</tt>.
     * Allows extenders to override and customize the runtime type of the
     * <tt>CallConference</tt> to used by this <tt>Call</tt>.
     *
     * @return a new <tt>CallConference</tt> instance which is to represent the
     * telephony conference-related state of this <tt>Call</tt>
     */
    protected CallConference createConference()
    {
        return new CallConference();
    }

    /**
     * Gets the telephony conference-related state of this <tt>Call</tt>. Since
     * a non-conference <tt>Call</tt> may be converted into a conference
     * <tt>Call</tt> at any time, every <tt>Call</tt> instance maintains a
     * <tt>CallConference</tt> instance regardless of whether the <tt>Call</tt>
     * in question is participating in a telephony conference.
     *
     * @return a <tt>CallConference</tt> instance which represents the
     * telephony conference-related state of this <tt>Call</tt>.
     */
    public CallConference getConference()
    {
        if (conference == null)
        {
            CallConference newValue = createConference();

            if (newValue == null)
            {
                /*
                 * Call is documented to always have a telephony
                 * conference-related state because there is an expectation that
                 * a 1-to-1 Call can always be turned into a conference Call.
                 */
                throw new IllegalStateException("conference");
            }
            else
            {
                setConference(newValue);
            }
        }
        return conference;
    }

    /**
     * Sets the telephony conference-related state of this <tt>Call</tt>. If the
     * invocation modifies this instance, it adds this <tt>Call</tt> to the
     * newly set <tt>CallConference</tt> and fires a
     * <tt>PropertyChangeEvent</tt> for the <tt>CONFERENCE</tt> property to its
     * listeners.
     *
     * @param conference the <tt>CallConference</tt> instance to represent the
     * telephony conference-related state of this <tt>Call</tt>
     */
    public void setConference(CallConference conference)
    {
        if (this.conference != conference)
        {
            CallConference oldValue = this.conference;

            this.conference = conference;

            CallConference newValue = this.conference;

            if (oldValue != null)
                oldValue.removeCall(this);
            if (newValue != null)
                newValue.addCall(this);

            firePropertyChange(CONFERENCE, oldValue, newValue);
        }
    }

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of listeners
     * interested in and notified about changes in the values of the properties
     * of this <tt>Call</tt>.
     *
     * @param listener a <tt>PropertyChangeListener</tt> to be notified about
     * changes in the values of the properties of this <tt>Call</tt>. If the
     * specified listener is already in the list of interested listeners (i.e.
     * it has been previously added), it is not added again.
     */
    public abstract void addPropertyChangeListener(
            PropertyChangeListener listener);

    /**
     * Fires a new <tt>PropertyChangeEvent</tt> to the
     * <tt>PropertyChangeListener</tt>s registered with this <tt>Call</tt> in
     * order to notify about a change in the value of a specific property which
     * had its old value modified to a specific new value.
     *
     * @param property the name of the property of this <tt>Call</tt> which had
     * its value changed
     * @param oldValue the value of the property with the specified name before
     * the change
     * @param newValue the value of the property with the specified name after
     * the change
     */
    protected abstract void firePropertyChange(
            String property,
            Object oldValue, Object newValue);

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <tt>Call</tt>.
     *
     * @param listener a <tt>PropertyChangeListener</tt> to no longer be
     * notified about changes in the values of the properties of this <tt>Call</tt>
     */
    public abstract void removePropertyChangeListener(
            PropertyChangeListener listener);

    /**
     * Returns <tt>true</tt> iff incoming calls into this <tt>Call</tt> should
     * be auto-answered.
     *
     * @return <tt>true</tt> iff incoming calls into this <tt>Call</tt> should
     * be auto-answered.
     */
    public boolean isAutoAnswer()
    {
        return isAutoAnswer;
    }

    /**
     * Sets the flag that specifies whether incoming calls into this
     * <tt>Call</tt> should be auto-answered.
     * @param autoAnswer whether incoming calls into this <tt>Call</tt> should
     * be auto-answered.
     */
    public void setAutoAnswer(boolean autoAnswer)
    {
        isAutoAnswer = autoAnswer;
    }
}
