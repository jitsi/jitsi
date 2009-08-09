/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class MockCall
    extends Call
    implements CallPeerListener
{
    private static final Logger logger = Logger.getLogger(MockCall.class);

    /**
     * A list containing all <tt>CallParticipant</tt>s of this call.
     */
    private Vector callParticipants = new Vector();


    public MockCall(MockProvider sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Returns an iterator over all call participants.
     *
     * @return an Iterator over all participants currently involved in the
     *   call.
     */
    public Iterator getCallPeers()
    {
        return callParticipants.iterator();
    }

    /**
     * Returns the number of participants currently associated with this call.
     *
     * @return an <tt>int</tt> indicating the number of participants
     *   currently associated with this call.
     */
    public int getCallPeerCount()
    {
        return callParticipants.size();
    }

    /**
     * Adds <tt>callParticipant</tt> to the list of participants in this call.
     * If the call participant is already included in the call, the method has
     * no effect.
     *
     * @param callParticipant the new <tt>CallParticipant</tt>
     */
    public void addCallParticipant(MockCallParticipant callParticipant)
    {
        if(callParticipants.contains(callParticipant))
            return;

        callParticipant.addCallPeerListener(this);

        this.callParticipants.add(callParticipant);

        logger.info("Will fire participant added");

        fireCallParticipantEvent(
            callParticipant, CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callParticipant</tt> from the list of participants in this
     * call. The method has no effect if there was no such participant in the
     * call.
     *
     * @param callParticipant the <tt>CallParticipant</tt> leaving the call;
     */
    public void removeCallParticipant(MockCallParticipant callParticipant)
    {
        if(!callParticipants.contains(callParticipant))
            return;

        this.callParticipants.remove(callParticipant);
        callParticipant.removeCallPeerListener(this);

        fireCallParticipantEvent(
            callParticipant, CallPeerEvent.CALL_PEER_REMVOVED);

        if(callParticipants.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        if ( ( (CallPeerState) evt.getNewValue())
            == CallPeerState.DISCONNECTED
            || ( (CallPeerState) evt.getNewValue())
            == CallPeerState.FAILED)
        {
            removeCallParticipant(
                (MockCallParticipant) evt.getSourceCallPeer());
        }
        else if ( ( (CallPeerState) evt.getNewValue())
                 == CallPeerState.CONNECTED
                 && getCallState().equals(CallState.CALL_INITIALIZATION))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
    }

    public void peerAddressChanged(CallPeerChangeEvent evt)
    {
    }

    public void peerImageChanged(CallPeerChangeEvent evt)
    {
    }

    public void participantTransportAddressChanged(CallPeerChangeEvent
        evt)
    {
    }

}
