/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
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
    extends AbstractCall<CallPeerJabberImpl, ProtocolProviderServiceJabberImpl>
    implements CallPeerListener
{
    /**
     * Logger of this class
     */
    private static final Logger logger = Logger.getLogger(CallJabberImpl.class);

    /**
     * The operation set that created us.
     */
    private final OperationSetBasicTelephonyJabberImpl parentOpSet;

    /**
     * Crates a CallJabberImpl instance belonging to <tt>sourceProvider</tt> and
     * associated with the jingle session with the specified <tt>jingleSID</tt>.
     * If this call corresponds to an incoming jingle session then the jingleSID
     * would come from there. Otherwise one could generate one using {@link
     * JingleIQ#generateSID()}
     *
     * @param parentOpSet the {@link OperationSetBasicTelephonyJabberImpl}
     * instance in the context of which this call has been created.
     */
    protected CallJabberImpl(
                        OperationSetBasicTelephonyJabberImpl parentOpSet)
    {
        super(parentOpSet.getProtocolProvider());
        this.parentOpSet = parentOpSet;

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);
    }


    ////////// legacy methods ///////////////
    /**
     * Adds <tt>callPeer</tt> to the list of peers in this call.
     * If the call peer is already included in the call, the method has
     * no effect.
     *
     * @param callPeer the new <tt>CallPeer</tt>
     */
    public void addCallPeer(CallPeerJabberImpl callPeer)
    {
        if(getCallPeersVector().contains(callPeer))
            return;

        callPeer.addCallPeerListener(this);

        getCallPeersVector().add(callPeer);
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
        if(!getCallPeersVector().contains(callPeer))
            return;

        getCallPeersVector().remove(callPeer);
        callPeer.setCall(null);
        callPeer.removeCallPeerListener(this);

        fireCallPeerEvent(
            callPeer, CallPeerEvent.CALL_PEER_REMOVED);

        if(getCallPeersVector().size() == 0)
            setCallState(CallState.CALL_ENDED);
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

    ////////////////////////////////////// NEW METHODS ///////////////////////////////////////////////////////

    /**
     * Creates a new call peer and sends a RINGING response.
     *
     * @param jingleIQ the {@link JingleIQ} that created the session.
     *
     * @return the newly created {@link CallPeerJabberImpl} (the one that sent
     * the INVITE).
     */
    public CallPeerJabberImpl processSessionInitiate(JingleIQ jingleIQ)
    {
        String remoteParty = jingleIQ.getInitiator();

        //according to the Jingle spec initiator may be null.
        if (remoteParty == null)
            remoteParty = jingleIQ.getFrom();


        CallPeerJabberImpl peer = createCallPeerFor(
                            remoteParty, true, jingleIQ.getSID());

        //send a ringing response
        try
        {
            if (logger.isTraceEnabled())
                logger.trace("will send ringing response: ");

            JingleIQ response = JinglePacketFactory.createRinging(jingleIQ);

            parentOpSet.getProtocolProvider().getConnection()
                .sendPacket(response);
        }
        catch (Exception ex)
        {
            logger.error("Error while trying to send a request", ex);
            peer.setState(CallPeerState.FAILED,
                "Internal Error: " + ex.getMessage());
            return peer;
        }

        return peer;
    }

    /**
     * Creates a new call peer associated with <tt>jingleIQ</tt>
     *
     * @param remoteParty the full jid of the remote party that the new peer
     * will be representing.
     * @param isIncoming indicates whether this is an incoming call (as opposed
     * to a call that we've initiated locally).
     * @param jingleSID the ID of the session that the new peer belongs to.
     *
     * @return a new instance of a <tt>CallPeerJabberImpl</tt>.
     */
    private CallPeerJabberImpl createCallPeerFor(String  remoteParty,
                                                 boolean isIncoming,
                                                 String  jingleSID)
    {
        CallPeerJabberImpl callPeer = new CallPeerJabberImpl(
                            remoteParty, this, jingleSID);
        addCallPeer(callPeer);

        callPeer.setState( isIncoming
                        ? CallPeerState.INCOMING_CALL
                        : CallPeerState.INITIATING_CALL);

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(this.getCallPeerCount() == 1)
        {
            parentOpSet.fireCallEvent( (isIncoming
                                        ? CallEvent.CALL_RECEIVED
                                        : CallEvent.CALL_INITIATED),
                                        this);
        }

        return callPeer;
    }

    /**
     * Determines if this call contains a peer whose corresponding session has
     * the specified <tt>sid</tt>.
     *
     * @param sid the ID of the session whose peer we are looking for.
     *
     * @return <tt>true</tt> if this call contains a peer with the specified
     * jingle <tt>sid</tt> and false otherwise.
     */
    public boolean containsJingleSID(String sid)
    {
        for(CallPeerJabberImpl peer : getCallPeersVector())
        {
            if (peer.getJingleSID().equals(sid))
                return true;
        }

        return false;
    }
}
