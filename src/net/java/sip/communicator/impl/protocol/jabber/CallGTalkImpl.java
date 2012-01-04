/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * A Google Talk implementation of the <tt>Call</tt> abstract class
 * encapsulating Google Talk sessions.
 *
 * @author Sebastien Vincent
 */
public class CallGTalkImpl
    extends MediaAwareCall<
        CallPeerGTalkImpl,
        OperationSetBasicTelephonyJabberImpl,
        ProtocolProviderServiceJabberImpl>
{
    /**
     * If the first callPeer is a Google Voice (without resource) ones.
     */
    private boolean firstCallPeerIsGV = false; 
    
    /**
     * Initializes a new <tt>CallGTalkImpl</tt> instance belonging to
     * <tt>sourceProvider</tt> and associated with the jingle session with the
     * specified <tt>sessionID</tt>. If the new instance corresponds to an
     * incoming Google Talk session, then the sessionID would come from there.
     * Otherwise, one could generate one using {@link SessionIQ#generateSID()}.
     *
     * @param parentOpSet the {@link OperationSetBasicTelephonyJabberImpl}
     * instance in the context of which this call has been created.
     */
    protected CallGTalkImpl(
                        OperationSetBasicTelephonyJabberImpl parentOpSet)
    {
        super(parentOpSet);

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getGTalkActiveCallsRepository().addCall(this);
    }

    /**
     * Determines if this call contains a peer whose corresponding session has
     * the specified <tt>sid</tt>.
     *
     * @param sid the ID of the session whose peer we are looking for.
     *
     * @return <tt>true</tt> if this call contains a peer with the specified
     * Google Talk <tt>sid</tt> and false otherwise.
     */
    public boolean containsSessionID(String sid)
    {
        return (getPeer(sid) != null);
    }

    /**
     * Returns the peer whose corresponding session has the specified
     * <tt>sid</tt>.
     *
     * @param sid the ID of the session whose peer we are looking for.
     *
     * @return the {@link CallPeerGTalkImpl} with the specified Google Talk
     * <tt>sid</tt> and <tt>null</tt> if no such peer exists in this call.
     */
    public CallPeerGTalkImpl getPeer(String sid)
    {
        for(CallPeerGTalkImpl peer : getCallPeersVector())
        {
            if (peer.getSessionID().equals(sid))
                return peer;
        }
        return null;
    }

    /**
     * Returns the peer whose corresponding session-init ID has the specified
     * <tt>id</tt>.
     *
     * @param id the ID of the session-init IQ whose peer we are looking for.
     *
     * @return the {@link CallPeerGTalkImpl} with the specified IQ
     * <tt>id</tt> and <tt>null</tt> if no such peer exists in this call.
     */
    public CallPeerGTalkImpl getPeerBySessInitPacketID(String id)
    {
        for(CallPeerGTalkImpl peer : getCallPeersVector())
        {
            if (peer.getSessInitID().equals(id))
                return peer;
        }
        return null;
    }

    /**
     * Creates a new Google Talk call peer and sends a RINGING response.
     *
     * @param sessionIQ the {@link SessionIQ} that created the session.
     *
     * @return the newly created {@link CallPeerGTalkImpl} (the one that sent
     * the INVITE).
     */
    public CallPeerGTalkImpl processGTalkInitiate(SessionIQ sessionIQ)
    {
        String remoteParty = sessionIQ.getInitiator();

        //according to the Jingle spec initiator may be null.
        if (remoteParty == null)
            remoteParty = sessionIQ.getFrom();

        CallPeerGTalkImpl callPeer = new CallPeerGTalkImpl(remoteParty, this);

        addCallPeer(callPeer);

        //before notifying about this call, make sure that it looks alright
        callPeer.processSessionInitiate(sessionIQ);

        if( callPeer.getState() == CallPeerState.FAILED)
            return null;

        callPeer.setState( CallPeerState.INCOMING_CALL );

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(this.getCallPeerCount() == 1)
            parentOpSet.fireCallEvent( CallEvent.CALL_RECEIVED, this);

        return callPeer;
    }

    /**
     * Creates a <tt>CallPeerGTalkImpl</tt> from <tt>calleeJID</tt> and sends
     * them <tt>initiate</tt> IQ request.
     *
     * @param calleeJID the party that we would like to invite to this call.
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>initiate</tt>
     * {@link SessionIQ} which is to init this <tt>CallJabberImpl</tt>
     *
     * @return the newly created <tt>Call</tt> corresponding to
     * <tt>calleeJID</tt>. All following state change events will be
     * delivered through this call peer.
     *
     * @throws OperationFailedException  with the corresponding code if we fail
     *  to create the call.
     */
    public CallPeerGTalkImpl initiateGTalkSession(
            String calleeJID,
            Iterable<PacketExtension> sessionInitiateExtensions)
        throws OperationFailedException
    {
        // create the session-initiate IQ
        CallPeerGTalkImpl callPeer = new CallPeerGTalkImpl(calleeJID, this);

        if(!firstCallPeerIsGV)
            firstCallPeerIsGV = calleeJID.endsWith(
                ProtocolProviderServiceJabberImpl.GOOGLE_VOICE_DOMAIN);
        
        addCallPeer(callPeer);

        callPeer.setState(CallPeerState.INITIATING_CALL);

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(getCallPeerCount() == 1 && !calleeJID.endsWith(
            ProtocolProviderServiceJabberImpl.GOOGLE_VOICE_DOMAIN) ||
            getCallPeerCount() == 2 && firstCallPeerIsGV)
        {
            if(firstCallPeerIsGV)
            {
                // now all is setup, considered that there is no GV call
                firstCallPeerIsGV = false;
                Iterator<CallPeerGTalkImpl> it =
                    getCallPeersVector().iterator();
                String sub = calleeJID.substring(0, calleeJID.indexOf("/"));
                
                // remove Google Voice first call from CallPeer vector otherwise
                // we will display a conference call window
                while(it.hasNext())
                {
                    CallPeer p = it.next();
                    
                    if(p.getAddress().equals(sub))
                    {
                        it.remove();
                        break;
                    }   
                }
            }
            
            parentOpSet.fireCallEvent(CallEvent.CALL_INITIATED, this);
        }

        CallPeerMediaHandlerGTalkImpl mediaHandler
            = callPeer.getMediaHandler();

        /* enable video if it is a videocall */
        mediaHandler.setLocalVideoTransmissionEnabled(localVideoAllowed);

        //set call state to connecting so that the user interface would start
        //playing the tones. we do that here because we may be harvesting
        //STUN/TURN addresses in initiateSession() which would take a while.
        callPeer.setState(CallPeerState.CONNECTING);

        callPeer.initiateSession(sessionInitiateExtensions);

        return callPeer;
    }
}
