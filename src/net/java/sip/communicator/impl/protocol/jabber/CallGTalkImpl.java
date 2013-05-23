/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jivesoftware.smack.packet.*;

/**
 * A Google Talk implementation of the <tt>Call</tt> abstract class
 * encapsulating Google Talk sessions.
 *
 * @author Sebastien Vincent
 */
public class CallGTalkImpl
    extends AbstractCallJabberGTalkImpl<CallPeerGTalkImpl>
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

        // if paranoia is set, to accept the call we need to know that
        // the other party has support for media encryption
        if(getProtocolProvider().getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.MODE_PARANOIA, false)
            && callPeer.getMediaHandler().getAdvertisedEncryptionMethods().length
                == 0)
        {
            //send an error response;
            String reasonText =
                JabberActivator.getResources().getI18NString(
                    "service.gui.security.encryption.required");
            SessionIQ errResp = GTalkPacketFactory.createSessionTerminate(
                sessionIQ.getTo(),
                sessionIQ.getFrom(),
                sessionIQ.getID(),
                Reason.SECURITY_ERROR,
                reasonText);

            callPeer.setState(CallPeerState.FAILED, reasonText);
            getProtocolProvider().getConnection().sendPacket(errResp);

            return null;
        }

        if( callPeer.getState() == CallPeerState.FAILED)
            return null;

        callPeer.setState( CallPeerState.INCOMING_CALL );

        // if this was the first peer we added in this call then the call is
        // new and we also need to notify everyone of its creation.
        if(this.getCallPeerCount() == 1)
            parentOpSet.fireCallEvent(CallEvent.CALL_RECEIVED, this);

        // Manages auto answer with "audio only", or "audio / video" answer.
        OperationSetAutoAnswerJabberImpl autoAnswerOpSet
            = (OperationSetAutoAnswerJabberImpl)
            this.getProtocolProvider()
            .getOperationSet(OperationSetBasicAutoAnswer.class);

        if(autoAnswerOpSet != null)
        {
            // With Gtalk, we do not actually supports the detection if the
            // incoming call is a video call (cf. the fireCallEvent above with
            // only 2 arguments). Thus, we set the auto-answer video
            // parameter to false.
            autoAnswerOpSet.autoAnswer(this, false);
        }

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
        List<CallPeerGTalkImpl> callPeers = getCallPeerList();
        int callPeerCount = callPeers.size();

        if(((callPeerCount == 1)
                    && !calleeJID.endsWith(
                            ProtocolProviderServiceJabberImpl
                                .GOOGLE_VOICE_DOMAIN))
                || ((callPeerCount == 2) && firstCallPeerIsGV))
        {
            if(firstCallPeerIsGV)
            {
                // now all is setup, considered that there is no GV call
                firstCallPeerIsGV = false;

                String sub = calleeJID.substring(0, calleeJID.indexOf("/"));

                // remove Google Voice first call from CallPeer vector otherwise
                // we will display a conference call window
                for (CallPeerGTalkImpl p : callPeers)
                {
                    if(p.getAddress().equals(sub))
                    {
                        doRemoveCallPeer(p);
                        break;
                    }
                }
            }
            // if this was the first peer we added in this call then the call is
            // new and we also need to notify everyone of its creation.
            if(getCallPeerCount() == 1)
                parentOpSet.fireCallEvent(CallEvent.CALL_INITIATED, this);
        }

        CallPeerMediaHandlerGTalkImpl mediaHandler
            = callPeer.getMediaHandler();

        /* enable video if it is a video call */
        mediaHandler.setLocalVideoTransmissionEnabled(localVideoAllowed);

        //set call state to connecting so that the user interface would start
        //playing the tones. we do that here because we may be harvesting
        //STUN/TURN addresses in initiateSession() which would take a while.
        callPeer.setState(CallPeerState.CONNECTING);

        // if initializing session fails, set peer to failed
        boolean sessionInitiated = false;

        try
        {

            callPeer.initiateSession(sessionInitiateExtensions);
            sessionInitiated = true;
        }
        finally
        {
            // if initialization throws an exception
            if(!sessionInitiated)
                callPeer.setState(CallPeerState.FAILED);
        }
        return callPeer;
    }

    /**
     * Send a <tt>content-modify</tt> message for all current <tt>CallPeer</tt>
     * to reflect possible video change in media setup.
     *
     * @throws OperationFailedException if problem occurred during message
     * generation or network problem
     */
    @Override
    public void modifyVideoContent()
        throws OperationFailedException
    {
        // GTalk is not able to use a "content-modify", thereby this function
        // does nothing.
    }
}
