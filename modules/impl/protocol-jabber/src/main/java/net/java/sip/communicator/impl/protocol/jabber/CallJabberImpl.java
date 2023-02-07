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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jitsi.xmpp.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.JingleUtils;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.utils.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.Jid;

/**
 * A Jabber implementation of the <tt>Call</tt> abstract class encapsulating
 * Jabber jingle sessions.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class CallJabberImpl
    extends MediaAwareCall<
        CallPeerJabberImpl,
        OperationSetBasicTelephonyJabberImpl,
        ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CallJabberImpl.class);

    /**
     * Indicates if the <tt>CallPeer</tt> will support <tt>inputevt</tt>
     * extension (i.e. will be able to be remote-controlled).
     */
    private boolean localInputEvtAware = false;

    /**
     * Initializes a new <tt>CallJabberImpl</tt> instance.
     *
     * @param parentOpSet the {@link OperationSetBasicTelephonyJabberImpl}
     * instance in the context of which this call has been created.
     */
    protected CallJabberImpl(
            OperationSetBasicTelephonyJabberImpl parentOpSet)
    {
        super(parentOpSet);

        //let's add ourselves to the calls repo. we are doing it ourselves just
        //to make sure that no one ever forgets.
        parentOpSet.getActiveCallsRepository().addCall(this);
    }

    /**
     * {@inheritDoc}
     *
     * Sends a <tt>content</tt> message to each of the <tt>CallPeer</tt>s
     * associated with this <tt>CallJabberImpl</tt> in order to include/exclude
     * the &quot;isfocus&quot; attribute.
     */
    @Override
    protected void conferenceFocusChanged(boolean oldValue, boolean newValue)
    {
        try
        {
            Iterator<CallPeerJabberImpl> peers = getCallPeers();

            while (peers.hasNext())
            {
                CallPeerJabberImpl callPeer = peers.next();

                if (callPeer.getState() == CallPeerState.CONNECTED)
                    callPeer.sendCoinSessionInfo();
            }
        }
        catch (SmackException.NotConnectedException | InterruptedException e)
        {
            //FIXME
        }
        finally
        {
            super.conferenceFocusChanged(oldValue, newValue);
        }
    }

    /**
     * Creates a <tt>CallPeerJabberImpl</tt> from <tt>calleeJID</tt> and sends
     * them <tt>session-initiate</tt> IQ request.
     *
     * @param calleeJID the party that we would like to invite to this call.
     * @param discoverInfo any discovery information that we have for the jid
     * we are trying to reach and that we are passing in order to avoid having
     * to ask for it again.
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>ExtensionElement</tt>s to be added to the <tt>session-initiate</tt>
     * {@link JingleIQ} which is to init this <tt>CallJabberImpl</tt>
     * @param supportedTransports the XML namespaces of the jingle transports
     * to use.
     *
     * @return the newly created <tt>CallPeerJabberImpl</tt> corresponding to
     * <tt>calleeJID</tt>. All following state change events will be
     * delivered through this call peer.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     *  to create the call.
     */
    public CallPeerJabberImpl initiateSession(
            Jid calleeJID,
            DiscoverInfo discoverInfo,
            Iterable<ExtensionElement> sessionInitiateExtensions,
            Collection<String> supportedTransports)
        throws OperationFailedException
    {
        // create the session-initiate IQ
        CallPeerJabberImpl callPeer = new CallPeerJabberImpl(calleeJID, this);

        callPeer.setDiscoveryInfo(discoverInfo);

        addCallPeer(callPeer);

        callPeer.setState(CallPeerState.INITIATING_CALL);

        // If this was the first peer we added in this call, then the call is
        // new and we need to notify everyone of its creation.
        if (getCallPeerCount() == 1)
            parentOpSet.fireCallEvent(CallEvent.CALL_INITIATED, this);

        CallPeerMediaHandlerJabberImpl mediaHandler
            = callPeer.getMediaHandler();

        //set the supported transports before the transport manager is created
        mediaHandler.setSupportedTransports(supportedTransports);

        /* enable video if it is a video call */
        mediaHandler.setLocalVideoTransmissionEnabled(localVideoAllowed);
        /* enable remote-control if it is a desktop sharing session */
        mediaHandler.setLocalInputEvtAware(getLocalInputEvtAware());

        /*
         * Set call state to connecting so that the user interface would start
         * playing the tones. We do that here because we may be harvesting
         * STUN/TURN addresses in initiateSession() which would take a while.
         */
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
            if (!sessionInitiated)
                callPeer.setState(CallPeerState.FAILED);
        }
        return callPeer;
    }

    /**
     * Updates the Jingle sessions for the <tt>CallPeer</tt>s of this
     * <tt>Call</tt>, to reflect the current state of the the video contents of
     * this <tt>Call</tt>. Sends a <tt>content-modify</tt>, <tt>content-add</tt>
     * or <tt>content-remove</tt> message to each of the current
     * <tt>CallPeer</tt>s.
     *
     * @throws OperationFailedException if a problem occurred during message
     * generation or there was a network problem
     */
    public void modifyVideoContent()
        throws OperationFailedException
    {
        if (logger.isDebugEnabled())
            logger.debug("Updating video content for " + this);

        boolean change = false;
        for (CallPeerJabberImpl peer : getCallPeerList())
        {
            try
            {
                change |= peer.sendModifyVideoContent();
            }
            catch (SmackException.NotConnectedException | InterruptedException e)
            {
                throw new OperationFailedException("Could send modify video content to " + peer.getAddress(), 0, e);
            }
        }

        if (change)
            fireCallChangeEvent(
                    CallChangeEvent.CALL_PARTICIPANTS_CHANGE, null, null);
    }

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
        // Use the IQs 'from', instead of the jingle 'initiator' field,
        // because we want to make sure that following IQs are sent with the
        // correct 'to'.
        Jid remoteParty = jingleIQ.getFrom();

        boolean autoAnswer = false;
        CallPeerJabberImpl attendant = null;
        OperationSetBasicTelephonyJabberImpl basicTelephony = null;

        CallPeerJabberImpl callPeer
            = new CallPeerJabberImpl(remoteParty, this, jingleIQ);

        addCallPeer(callPeer);

        /*
         * We've already sent ack to the specified session-initiate so if it has
         * been sent as part of an attended transfer, we have to hang up on the
         * attendant.
         */
        try
        {
            TransferPacketExtension transfer
                = jingleIQ.getExtension(TransferPacketExtension.class);

            if (transfer != null)
            {
                String sid = transfer.getSID();

                if (sid != null)
                {
                    ProtocolProviderServiceJabberImpl protocolProvider
                        = getProtocolProvider();
                    basicTelephony
                        = (OperationSetBasicTelephonyJabberImpl)
                            protocolProvider.getOperationSet(
                                    OperationSetBasicTelephony.class);
                    CallJabberImpl attendantCall
                        = basicTelephony
                            .getActiveCallsRepository()
                                .findSID(sid);

                    if (attendantCall != null)
                    {
                        attendant = attendantCall.getPeer(sid);
                        if ((attendant != null)
                                && basicTelephony
                                    .getFullCalleeURI(attendant.getAddress())
                                        .equals(transfer.getFrom())
                                && protocolProvider.getOurJID().equals(
                                        transfer.getTo()))
                        {
                            //basicTelephony.hangupCallPeer(attendant);
                            autoAnswer = true;
                        }
                    }
                }
            }
        }
        catch (Throwable t)
        {
            logger.error(
                    "Failed to hang up on attendant"
                        + " as part of session transfer",
                    t);

            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }

        CoinPacketExtension coin
            = jingleIQ.getExtension(CoinPacketExtension.class);

        if (coin != null)
        {
            boolean b
                = Boolean.parseBoolean(
                        (String)
                            coin.getAttribute(
                                    CoinPacketExtension.ISFOCUS_ATTR_NAME));

            callPeer.setConferenceFocus(b);
        }

        //before notifying about this call, make sure that it looks alright
        try
        {
            callPeer.processSessionInitiate(jingleIQ);
        }
        catch (SmackException.NotConnectedException | InterruptedException e)
        {
            callPeer.setState( CallPeerState.INCOMING_CALL );
            return null;
        }

        // if paranoia is set, to accept the call we need to know that
        // the other party has support for media encryption
        if (getProtocolProvider().getAccountID().getAccountPropertyBoolean(
                ProtocolProviderFactory.MODE_PARANOIA, false)
            && callPeer.getMediaHandler().getAdvertisedEncryptionMethods()
                    .length
                == 0)
        {
            //send an error response;
            String reasonText
                = JabberActivator.getResources().getI18NString(
                        "service.gui.security.encryption.required");
            JingleIQ errResp
                = JinglePacketFactory.createSessionTerminate(
                        jingleIQ.getTo(),
                        jingleIQ.getFrom(),
                        jingleIQ.getSID(),
                        Reason.SECURITY_ERROR,
                        reasonText);

            callPeer.setState(CallPeerState.FAILED, reasonText);
            try
            {
                getProtocolProvider().getConnection().sendStanza(errResp);
            }
            catch (SmackException.NotConnectedException | InterruptedException e)
            {
                logger.error("Could not send session terminate", e);
                return null;
            }

            return null;
        }

        if (callPeer.getState() == CallPeerState.FAILED)
            return null;

        callPeer.setState( CallPeerState.INCOMING_CALL );

        // in case of attended transfer, auto answer the call
        if (autoAnswer)
        {
            /* answer directly */
            try
            {
                callPeer.answer();
            }
            catch(Exception e)
            {
                logger.info(
                        "Exception occurred while answer transferred call",
                        e);
                callPeer = null;
            }

            // hang up now
            try
            {
                basicTelephony.hangupCallPeer(attendant);
            }
            catch(OperationFailedException e)
            {
                logger.error(
                        "Failed to hang up on attendant as part of session"
                            + " transfer",
                        e);
            }

            return callPeer;
        }

        /* see if offer contains audio and video so that we can propose
         * option to the user (i.e. answer with video if it is a video call...)
         */
        List<ContentPacketExtension> offer
            = callPeer.getSessionIQ().getContentList();
        Map<MediaType, MediaDirection> directions
            = new HashMap<>();

        directions.put(MediaType.AUDIO, MediaDirection.INACTIVE);
        directions.put(MediaType.VIDEO, MediaDirection.INACTIVE);

        for (ContentPacketExtension c : offer)
        {
            String contentName = c.getName();
            MediaDirection remoteDirection
                = JingleUtils.getDirection(c, callPeer.isInitiator());

            if (MediaType.AUDIO.toString().equals(contentName))
                directions.put(MediaType.AUDIO, remoteDirection);
            else if (MediaType.VIDEO.toString().equals(contentName))
                directions.put(MediaType.VIDEO, remoteDirection);
        }

        // If this was the first peer we added in this call, then the call is
        // new and we need to notify everyone of its creation.
        if (getCallPeerCount() == 1)
        {
            parentOpSet.fireCallEvent(
                    CallEvent.CALL_RECEIVED,
                    this,
                    directions);
        }

        // Manages auto answer with "audio only", or "audio/video" answer.
        OperationSetAutoAnswerJabberImpl autoAnswerOpSet
            = (OperationSetAutoAnswerJabberImpl)
                getProtocolProvider().getOperationSet(
                        OperationSetBasicAutoAnswer.class);

        if (autoAnswerOpSet != null)
            autoAnswerOpSet.autoAnswer(this, directions);

        return callPeer;
    }

    /**
     * Sets the properties (i.e. fingerprint and hash function) of a specific
     * <tt>DtlsControl</tt> on the specific
     * <tt>IceUdpTransportPacketExtension</tt>.
     *
     * @param dtlsControl the <tt>DtlsControl</tt> the properties of which are
     * to be set on the specified <tt>localTransport</tt>
     * @param localTransport the <tt>IceUdpTransportPacketExtension</tt> on
     * which the properties of the specified <tt>dtlsControl</tt> are to be set
     */
    static void setDtlsEncryptionOnTransport(
            DtlsControl dtlsControl,
            IceUdpTransportPacketExtension localTransport)
    {
        String fingerprint = dtlsControl.getLocalFingerprint();
        String hash = dtlsControl.getLocalFingerprintHashFunction();

        DtlsFingerprintPacketExtension fingerprintPE
            = localTransport.getFirstChildOfType(
                    DtlsFingerprintPacketExtension.class);

        if (fingerprintPE == null)
        {
            fingerprintPE = new DtlsFingerprintPacketExtension();
            localTransport.addChildExtension(fingerprintPE);
        }
        fingerprintPE.setFingerprint(fingerprint);
        fingerprintPE.setHash(hash);
        fingerprintPE.setSetup(dtlsControl.getSetup().toString());
    }

    /**
     * {@inheritDoc}
     *
     * Implements
     * {@link net.java.sip.communicator.service.protocol.event.DTMFListener#toneReceived(net.java.sip.communicator.service.protocol.event.DTMFReceivedEvent)}
     *
     * Forwards DTMF events to the <tt>IncomingDTMF</tt> operation set, setting
     * this <tt>Call</tt> as the source.
     */
    @Override
    public void toneReceived(DTMFReceivedEvent evt)
    {
        OperationSetIncomingDTMF opSet
            = getProtocolProvider()
                .getOperationSet(OperationSetIncomingDTMF.class);

        if (opSet != null && opSet instanceof OperationSetIncomingDTMFJabberImpl)
        {
            // Re-fire the event using this Call as the source.
            ((OperationSetIncomingDTMFJabberImpl) opSet).toneReceived(
                    new DTMFReceivedEvent(
                            this,
                            evt.getValue(),
                            evt.getDuration(),
                            evt.getStart()));
        }
    }

    /**
     * Enable or disable <tt>inputevt</tt> support (remote control).
     *
     * @param enable new state of inputevt support
     */
    public void setLocalInputEvtAware(boolean enable)
    {
        localInputEvtAware = enable;
    }

    /**
     * Returns if the call support <tt>inputevt</tt> (remote control).
     *
     * @return true if the call support <tt>inputevt</tt>, false otherwise
     */
    public boolean getLocalInputEvtAware()
    {
        return localInputEvtAware;
    }

    /**
     * Returns the peer whose corresponding session has the specified
     * <tt>sid</tt>.
     *
     * @param sid the ID of the session whose peer we are looking for.
     *
     * @return the {@link CallPeerJabberImpl} with the specified jingle
     * <tt>sid</tt> and <tt>null</tt> if no such peer exists in this call.
     */
    public CallPeerJabberImpl getPeer(String sid)
    {
        if (sid == null)
            return null;

        for(CallPeerJabberImpl peer : getCallPeerList())
        {
            if (sid.equals(peer.getSID()))
                return peer;
        }
        return null;
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
    public boolean containsSID(String sid)
    {
        return (getPeer(sid) != null);
    }

    /**
     * Returns the peer whose corresponding session-init ID has the specified
     * <tt>id</tt>.
     *
     * @param id the ID of the session-init IQ whose peer we are looking for.
     *
     * @return the {@link CallPeerJabberImpl} with the specified IQ
     * <tt>id</tt> and <tt>null</tt> if no such peer exists in this call.
     */
    public CallPeerJabberImpl getPeerBySessInitPacketID(String id)
    {
        if (id == null)
            return null;

        for(CallPeerJabberImpl peer : getCallPeerList())
        {
            if (id.equals(peer.getSessInitID()))
                return peer;
        }
        return null;
    }
}
