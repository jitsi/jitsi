/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.media.*;
import org.jivesoftware.smackx.jingle.*;

/**
 * A Jabber implementation of the Call abstract class encapsulating Jabber
 *  jingle sessions.
 *
 * @author Emil Ivov
 */
public class CallJabberImpl
    extends Call
    implements CallParticipantListener
{
    /**
     * Logger of this class
     */
    private static final Logger logger = Logger.getLogger(CallJabberImpl.class);
    /**
     * A list containing all <tt>CallParticipant</tt>s of this call.
     */
    private Vector callParticipants = new Vector();

    /**
     * The state that this call is currently in.
     */
    private CallState callState = CallState.CALL_INITIALIZATION;

    /**
     * The <tt>CallSession</tt> that the media service has created for this
     * call.
     */
    private CallSession mediaCallSession = null;

    /**
     * Crates a CallJabberImpl instance belonging to <tt>sourceProvider</tt> and
     * initiated by <tt>CallCreator</tt>.
     *
     * @param sourceProvider the ProtocolProviderServiceJabberImpl instance in the
     * context of which this call has been created.
     */
    protected CallJabberImpl(ProtocolProviderServiceJabberImpl sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Adds <tt>callParticipant</tt> to the list of participants in this call.
     * If the call participant is already included in the call, the method has
     * no effect.
     *
     * @param callParticipant the new <tt>CallParticipant</tt>
     */
    public void addCallParticipant(CallParticipantJabberImpl callParticipant)
    {
        if(callParticipants.contains(callParticipant))
            return;

        callParticipant.addCallParticipantListener(this);

        this.callParticipants.add(callParticipant);
        fireCallParticipantEvent(
            callParticipant, CallParticipantEvent.CALL_PARTICIPANT_ADDED);
    }

    /**
     * Removes <tt>callParticipant</tt> from the list of participants in this
     * call. The method has no effect if there was no such participant in the
     * call.
     *
     * @param callParticipant the <tt>CallParticipant</tt> leaving the call;
     */
    public void removeCallParticipant(CallParticipantJabberImpl callParticipant)
    {
        if(!callParticipants.contains(callParticipant))
            return;

        this.callParticipants.remove(callParticipant);
        callParticipant.setCall(null);
        callParticipant.removeCallParticipantListener(this);

        fireCallParticipantEvent(
            callParticipant, CallParticipantEvent.CALL_PARTICIPANT_REMVOVED);

        if(callParticipants.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Sets the state of this call and fires a call change event notifying
     * registered listenres for the change.
     *
     * @param newState a reference to the <tt>CallState</tt> instance that
     * the call is to enter.
     */
    public void setCallState(CallState newState)
    {
        CallState oldState = getCallState();

        if(oldState == newState)
            return;

        this.callState = newState;

        fireCallChangeEvent(
            CallChangeEvent.CALL_STATE_CHANGE, oldState, newState);
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
     * Returns an iterator over all call participants.
     * @return an Iterator over all participants currently involved in the call.
     */
    public Iterator getCallParticipants()
    {
        return new LinkedList(callParticipants).iterator();
    }

    /**
     * Returns the number of participants currently associated with this call.
     * @return an <tt>int</tt> indicating the number of participants currently
     * associated with this call.
     */
    public int getCallParticipantsCount()
    {
        return callParticipants.size();
    }

    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantImageChanged(CallParticipantChangeEvent evt)
    {}

    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {}

    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantTransportAddressChanged(
                                    CallParticipantChangeEvent evt)
    {}


    /**
     * Dummy implementation of a method (inherited from CallParticipantListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {}

    /**
     * Verifies whether the call participant has entered a state.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
        if(((CallParticipantState)evt.getNewValue())
                     == CallParticipantState.DISCONNECTED
            || ((CallParticipantState)evt.getNewValue())
                     == CallParticipantState.FAILED)
        {
            removeCallParticipant(
                (CallParticipantJabberImpl)evt.getSourceCallParticipant());
        }
        else if (((CallParticipantState)evt.getNewValue())
                     == CallParticipantState.CONNECTED
                && getCallState().equals(CallState.CALL_INITIALIZATION))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    /**
     * Returns <tt>true</tt> if <tt>session</tt> matches the jingle session
     * established with one of the participants in this call.
     * 
     * @param session the session whose corresponding participant we're looking
     * for.
     * @return true if this call contains a call participant whose jingleSession
     * session is the same as the specified and false otherwise.
     */
    public boolean contains(JingleSession session)
    {
        return findCallParticipant(session) != null;
    }

    /**
     * Returns the call participant whose associated jingle session matches
     * <tt>session</tt>.
     *
     * @param session the jingle session whose corresponding participant we're
     * looking for.
     * @return the call participant whose jingle session is the same as the
     * specified or null if no such call participant was found.
     */
    public CallParticipantJabberImpl findCallParticipant(JingleSession session)
    {
        Iterator callParticipants = this.getCallParticipants();

        if(logger.isTraceEnabled())
        {
            logger.trace("Looking for participant with session: " + session
                         + "among " + this.callParticipants.size() + " calls");
        }


        while (callParticipants.hasNext())
        {
            CallParticipantJabberImpl cp
                = (CallParticipantJabberImpl)callParticipants.next();

            if( cp.getJingleSession() == session)
            {
                logger.trace("Returing cp="+cp);
                return cp;
            }
            else
            {
                logger.trace("Ignoring cp="+cp
                             + " because cp.jingleSession="+cp.getJingleSession()
                             + " while session="+session);
            }
        }

        return null;
    }

    /**
     * Sets the <tt>CallSession</tt> that the media service has created for this
     * call.
     *
     * @param callSession the <tt>CallSession</tt> that the media service has
     * created for this call.
     */
    public void setMediaCallSession(CallSession callSession)
    {
        this.mediaCallSession = callSession;
    }

    /**
     * Sets the <tt>CallSession</tt> that the media service has created for this
     * call.
     *
     * @return the <tt>CallSession</tt> that the media service has
     * created for this call or null if no call session has been created so
     * far.
     */
    public CallSession getMediaCallSession()
    {
        return this.mediaCallSession;
    }
}
