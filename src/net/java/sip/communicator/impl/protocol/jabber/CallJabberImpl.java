/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

//import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A Jabber implementation of the Call abstract class encapsulating Jabber
 *  jingle sessions.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 */
public class CallJabberImpl
    extends Call
    implements CallPeerListener
{
    /**
     * Logger of this class
     */
    private static final Logger logger = Logger.getLogger(CallJabberImpl.class);
    /**
     * A list containing all <tt>CallPeer</tt>s of this call.
     */
    private Vector<CallPeerJabberImpl> callPeers
                                            = new Vector<CallPeerJabberImpl>();

    /**
     * The <tt>CallSession</tt> that the media service has created for this
     * call.
     */
//    private CallSession mediaCallSession = null;

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
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    public void addCallPeer(CallPeerJabberImpl callPeer)
    {
        if(callPeers.contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        this.callPeers.add(callPeer);
        fireCallPeerEvent( callPeer, CallPeerEvent.CALL_PEER_ADDED);
    }

    /**
     * Removes <tt>callPeer</tt> from the list of peers in this
     * call. The method has no effect if there was no such peer in the
     * call.
     *
     * @param callPeer the <tt>CallPeer</tt> leaving the call;
     */
    public void removeCallPeer(CallPeerJabberImpl callPeer)
    {
        if(!callPeers.contains(callPeer))
            return;

        this.callPeers.remove(callPeer);
        callPeer.setCall(null);
        callPeer.removeCallPeerListener(this);

        fireCallPeerEvent(
            callPeer, CallPeerEvent.CALL_PEER_REMOVED);

        if(callPeers.size() == 0)
            setCallState(CallState.CALL_ENDED);
    }

    /**
     * Returns an iterator over all call peers.
     * @return an Iterator over all peers currently involved in the call.
     */
    public Iterator<CallPeer> getCallPeers()
    {
        return new LinkedList<CallPeer>(callPeers).iterator();
    }

    /**
     * Returns the number of peers currently associated with this call.
     * @return an <tt>int</tt> indicating the number of peers currently
     * associated with this call.
     */
    public int getCallPeerCount()
    {
        return callPeers.size();
    }

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerImageChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerAddressChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerTransportAddressChanged(
                                    CallPeerChangeEvent evt)
    {}


    /**
     * Dummy implementation of a method (inherited from CallPeerListener)
     * that we don't need.
     *
     * @param evt unused.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {}

    /**
     * Verifies whether the call peer has entered a state.
     *
     * @param evt The <tt>CallPeerChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        if(((CallPeerState)evt.getNewValue())
                     == CallPeerState.DISCONNECTED
            || ((CallPeerState)evt.getNewValue())
                     == CallPeerState.FAILED)
        {
            removeCallPeer(
                (CallPeerJabberImpl)evt.getSourceCallPeer());
        }
        else if (((CallPeerState)evt.getNewValue())
                     == CallPeerState.CONNECTED
                && getCallState().equals(CallState.CALL_INITIALIZATION))
        {
            setCallState(CallState.CALL_IN_PROGRESS);
        }
    }

    /**
     * Gets the indicator which determines whether the local peer represented by
     * this <tt>Call</tt> is acting as a conference focus and thus should send
     * the &quot;isfocus&quot; parameter in the Contact headers of its outgoing
     * SIP signaling.
     *
     * @return <tt>true</tt> if the local peer represented by this <tt>Call</tt>
     * is acting as a conference focus; otherwise, <tt>false</tt>
     */
    public boolean isConferenceFocus()
    {
        return false;
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to add
     */
    public void addLocalUserSoundLevelListener(
        SoundLevelListener l)
    {

    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in local sound level
     * related information.
     * @param l the <tt>SoundLevelListener</tt> to remove
     */
    public void removeLocalUserSoundLevelListener(
        SoundLevelListener l)
    {

    }
}
