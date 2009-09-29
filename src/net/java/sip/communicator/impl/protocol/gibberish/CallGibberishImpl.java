/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A Gibberish implementation of the <tt>Call</tt> interface.
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class CallGibberishImpl
    extends Call
    implements CallPeerListener
{
    private static final Logger logger
        = Logger.getLogger(CallGibberishImpl.class);

    /**
     * A list containing all <tt>CallPeer</tt>s of this call.
     */
    private Vector<CallPeer> callPeers = new Vector<CallPeer>();

    /**
     * Creates a <tt>CallGibberishImpl</tt> by specifying the
     * <tt>sourceProvider</tt>.
     * @param sourceProvider the source provider
     */
    public CallGibberishImpl(
        ProtocolProviderServiceGibberishImpl sourceProvider)
    {
        super(sourceProvider);
    }

    /**
     * Returns an iterator over all call peers.
     *
     * @return an Iterator over all peers currently involved in the call.
     */
    public Iterator<CallPeer> getCallPeers()
    {
        return callPeers.iterator();
    }

    /**
     * Returns the number of peers currently associated with this call.
     *
     * @return an <code>int</code> indicating the number of peers
     *   currently associated with this call.
     */
    public int getCallPeerCount()
    {
        return callPeers.size();
    }

    /**
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    public void addCallPeer(CallPeerGibberishImpl callPeer)
    {
        if(callPeers.contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        this.callPeers.add(callPeer);

        logger.info("Will fire peer added");

        fireCallPeerEvent(
            callPeer, CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    public void removeCallPeer(CallPeerGibberishImpl callPeer)
    {
        if(!callPeers.contains(callPeer))
            return;

        this.callPeers.remove(callPeer);
        callPeer.removeCallPeerListener(this);

        fireCallPeerEvent(
            callPeer, CallPeerEvent.CALL_PEER_REMOVED);

        if(callPeers.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        if ( ( (CallPeerState) evt.getNewValue())
            == CallPeerState.DISCONNECTED
            || ( (CallPeerState) evt.getNewValue())
            == CallPeerState.FAILED)
        {
            removeCallPeer(
                (CallPeerGibberishImpl) evt.getSourceCallPeer());
        }
        else if ( ( (CallPeerState) evt.getNewValue())
                 == CallPeerState.CONNECTED
                 && getCallState().equals(CallState.CALL_INITIALIZATION))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {}

    public void peerAddressChanged(CallPeerChangeEvent evt)
    {}

    public void peerImageChanged(CallPeerChangeEvent evt)
    {}

    public void peerTransportAddressChanged(CallPeerChangeEvent evt)
    {}
}
