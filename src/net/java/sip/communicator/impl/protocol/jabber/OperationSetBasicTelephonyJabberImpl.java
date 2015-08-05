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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing <tt>OperationSetBasicTelephony</tt>.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 * @author Boris Grozev
 */
public class OperationSetBasicTelephonyJabberImpl
   extends AbstractOperationSetBasicTelephony<ProtocolProviderServiceJabberImpl>
   implements RegistrationStateChangeListener,
              PacketListener,
              PacketFilter,
              OperationSetSecureSDesTelephony,
              OperationSetSecureZrtpTelephony,
              OperationSetAdvancedTelephony<ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetBasicTelephonyJabberImpl</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetBasicTelephonyJabberImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceJabberImpl</tt> instance
     * that created us.
     */
    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private ActiveCallsRepositoryJabberGTalkImpl
        <CallJabberImpl, CallPeerJabberImpl> activeCallsRepository
            = new ActiveCallsRepositoryJabberGTalkImpl
                <CallJabberImpl, CallPeerJabberImpl>(this);

    /**
     * Google Voice domain.
     */
    private static final String GOOGLE_VOICE_DOMAIN = "voice.google.com";

    /**
     * Creates a new instance.
     *
     * @param protocolProvider a reference to the
     * <tt>ProtocolProviderServiceJabberImpl</tt> instance that created us.
     */
    public OperationSetBasicTelephonyJabberImpl(
            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
        this.protocolProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>JingleManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState registrationState = evt.getNewState();

        if (registrationState == RegistrationState.REGISTERING)
        {
            ProviderManager.getInstance().addIQProvider(
                    JingleIQ.ELEMENT_NAME,
                    JingleIQ.NAMESPACE,
                    new JingleIQProvider());

            subscribeForJinglePackets();

            if (logger.isInfoEnabled())
                logger.info("Jingle : ON ");
        }
        else if (registrationState == RegistrationState.UNREGISTERED)
        {
            unsubscribeForJinglePackets();

            if (logger.isInfoEnabled())
                logger.info("Jingle : OFF ");
        }
    }

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param callee the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @see OperationSetBasicTelephony#createCall(String)
     */
    public Call createCall(String callee, CallConference conference)
        throws OperationFailedException
    {
        CallJabberImpl call = new CallJabberImpl(this);

        if (conference != null)
            call.setConference(conference);

        CallPeer callPeer = createOutgoingCall(call, callee);

        if (callPeer == null)
        {
            throw new OperationFailedException(
                    "Failed to create outgoing call"
                        + " because no peer was created",
                    OperationFailedException.INTERNAL_ERROR);
        }

        Call callOfCallPeer = callPeer.getCall();

        // We may have a Google Talk call here.
        if ((callOfCallPeer != call) && (conference != null))
            callOfCallPeer.setConference(conference);

        return callOfCallPeer;
    }

    /**
     * {@inheritDoc}
     *
     * Creates a new <tt>CallJabberImpl</tt> and initiates a jingle session
     * to the JID obtained from the <tt>uri</tt> of <tt>cd</tt>.
     *
     * If <tt>cd</tt> contains a <tt>callid</tt>, adds the "callid" element as
     * an extension to the session-initiate IQ.
     *
     * Uses the supported transports of <tt>cd</tt>
     */
    @Override public CallJabberImpl
        createCall(ConferenceDescription cd, final ChatRoom chatRoom)
        throws OperationFailedException
    {
        final CallJabberImpl call = new CallJabberImpl(this);
        
        ((ChatRoomJabberImpl) chatRoom).addConferenceCall(call);
        
        call.addCallChangeListener(
                new CallChangeListener()
                {
                    @Override
                    public void callPeerAdded(CallPeerEvent ev) {}

                    @Override
                    public void callPeerRemoved(CallPeerEvent ev) {}

                    @Override
                    public void callStateChanged(CallChangeEvent ev)
                    {
                        if (CallState.CALL_ENDED.equals(ev.getNewValue()))
                        {
                            ((ChatRoomJabberImpl) chatRoom)
                                .removeConferenceCall(call);
                        }
                    }
                });

        String remoteJid = cd.getUri();
        if (remoteJid.startsWith("xmpp:"))
            remoteJid = remoteJid.substring(5, remoteJid.length());

        List<PacketExtension> sessionInitiateExtensions
                = new ArrayList<PacketExtension>(2);

        String callid = cd.getCallId();
        if (callid != null)
        {
            sessionInitiateExtensions.add(new CallIdPacketExtension(callid));
        }

        //String password = cd.getPassword();
        //if (password != null)
        //   extensions.add(new PasswordPacketExtension(password));

        call.initiateSession(
                remoteJid,
                null,
                sessionInitiateExtensions,
                cd.getSupportedTransports());
        return call;
    }

    /**
     * Init and establish the specified call.
     *
     * @param call the <tt>CallJabberImpl</tt> that will be used
     * to initiate the call
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     *
     * @return the <tt>CallPeer</tt> that represented by the specified uri. All
     * following state change events will be delivered through that call peer.
     * The <tt>Call</tt> that this peer is a member of could be retrieved from
     * the <tt>CallPeer</tt> instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    AbstractCallPeer<?, ?> createOutgoingCall(
            CallJabberImpl call,
            String calleeAddress)
        throws OperationFailedException
    {
        return createOutgoingCall(call, calleeAddress, null);
    }

    /**
     * Init and establish the specified call.
     *
     * @param call the <tt>CallJabberImpl</tt> that will be used
     * to initiate the call
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>session-initiate</tt>
     * {@link JingleIQ} which is to init the specified <tt>call</tt>
     *
     * @return the <tt>CallPeer</tt> that represented by the specified uri. All
     * following state change events will be delivered through that call peer.
     * The <tt>Call</tt> that this peer is a member of could be retrieved from
     * the <tt>CallPeer</tt> instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    AbstractCallPeer<?, ?> createOutgoingCall(
            CallJabberImpl call,
            String calleeAddress,
            Iterable<PacketExtension> sessionInitiateExtensions)
        throws OperationFailedException
    {
        return createOutgoingCall(call, calleeAddress, null, null);
    }

    /**
     * Init and establish the specified call.
     *
     * @param call the <tt>CallJabberImpl</tt> that will be used
     * to initiate the call
     * @param calleeAddress the address of the callee that we'd like to connect
     * with.
     * @param fullCalleeURI the full Jid address, which if specified would
     * explicitly initiate a call to this full address
     * @param sessionInitiateExtensions a collection of additional and optional
     * <tt>PacketExtension</tt>s to be added to the <tt>session-initiate</tt>
     * {@link JingleIQ} which is to init the specified <tt>call</tt>
     *
     * @return the <tt>CallPeer</tt> that represented by the specified uri. All
     * following state change events will be delivered through that call peer.
     * The <tt>Call</tt> that this peer is a member of could be retrieved from
     * the <tt>CallPeer</tt> instance with the use of the corresponding method.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    AbstractCallPeer<?, ?> createOutgoingCall(
            CallJabberImpl call,
            String calleeAddress,
            String fullCalleeURI,
            Iterable<PacketExtension> sessionInitiateExtensions)
        throws OperationFailedException
    {
        if (logger.isInfoEnabled())
            logger.info("Creating outgoing call to " + calleeAddress);
        if (protocolProvider.getConnection() == null || call == null)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession."
                        + " We don't have a valid XMPPConnection.",
                    OperationFailedException.INTERNAL_ERROR);
        }

        boolean isGoogle = protocolProvider.isGmailOrGoogleAppsAccount();
        boolean isGoogleVoice = false;

        if (isGoogle)
        {
            if (!calleeAddress.contains("@"))
            {
                calleeAddress += "@" + GOOGLE_VOICE_DOMAIN;
                isGoogleVoice = true;
            }
            else if(calleeAddress.endsWith(GOOGLE_VOICE_DOMAIN))
            {
                isGoogleVoice = true;
            }
        }

        // if address is not suffixed by @domain, add the default domain
        // corresponding to account domain or via the OVERRIDE_PHONE_SUFFIX
        // property if defined
        AccountID accountID = getProtocolProvider().getAccountID();

        if (calleeAddress.indexOf('@') == -1)
        {
            String phoneSuffix
                = accountID.getAccountPropertyString("OVERRIDE_PHONE_SUFFIX");
            String serviceName = null;

            if ((phoneSuffix == null) || (phoneSuffix.length() == 0))
                serviceName = StringUtils.parseServer(accountID.getUserID());
            else
                serviceName = phoneSuffix;
            calleeAddress = calleeAddress + "@" + serviceName;
        }

        String bypassDomain = accountID.getAccountPropertyString(
                JabberAccountID.TELEPHONY_BYPASS_GTALK_CAPS);

        boolean alwaysCallGtalk
            = ((bypassDomain != null)
                    && bypassDomain.equals(
                            calleeAddress.substring(
                                    calleeAddress.indexOf('@') + 1)))
                || isGoogleVoice;

        boolean isPrivateMessagingContact = false;
        OperationSetMultiUserChat mucOpSet = getProtocolProvider()
            .getOperationSet(OperationSetMultiUserChat.class);
        if(mucOpSet != null)
            isPrivateMessagingContact
                = mucOpSet.isPrivateMessagingContact(calleeAddress);

        if((!getProtocolProvider().getConnection().getRoster().contains(
            StringUtils.parseBareAddress(calleeAddress)) &&
            !isPrivateMessagingContact) && !alwaysCallGtalk)
        {
            throw new OperationFailedException(
                calleeAddress + " does not belong to our contact list",
                OperationFailedException.NOT_FOUND);
        }

        // If there's no fullCalleeURI specified we'll discover the most
        // connected one with highest priority.
        if (fullCalleeURI == null)
            fullCalleeURI = 
                discoverFullJid(calleeAddress);

        if (fullCalleeURI == null)
            throw new OperationFailedException(
                    "Failed to create outgoing call to " + calleeAddress
                            + ". Could not find a resource which supports " +
                            "Jingle",
                    OperationFailedException.INTERNAL_ERROR);

        DiscoverInfo di = null;

        try
        {
            // check if the remote client supports telephony.
            di = protocolProvider.getDiscoveryManager().discoverInfo(
                    fullCalleeURI);
        }
        catch (XMPPException ex)
        {
            logger.warn("could not retrieve info for " + fullCalleeURI, ex);
        }

        if(di != null)
        {
            if (logger.isInfoEnabled())
                logger.info(fullCalleeURI + ": jingle supported ");
        }
        else
        {
            if (logger.isInfoEnabled())
                logger.info(fullCalleeURI + ": jingle not supported?");

            throw new OperationFailedException(
                    "Failed to create an outgoing call.\n"
                    + fullCalleeURI + " does not support jingle",
                    OperationFailedException.INTERNAL_ERROR);
        }

        /* in case we figure that calling people without a resource id is
           impossible, we'll have to uncomment the following lines. keep in mind
           that this would mean - no calls to pstn though
        if (fullCalleeURI.indexOf('/') < 0)
        {
            throw new OperationFailedException(
                    "Failed to create OutgoingJingleSession.\n"
                    + "User " + calleeAddress + " is unknown to us."
                    , OperationFailedException.INTERNAL_ERROR);
        }
        */

        AbstractCallPeer<?, ?> peer = null;

        // initiate call
        try
        {
            if (di != null)
            {
                peer
                    = call.initiateSession(
                            fullCalleeURI,
                            di,
                            sessionInitiateExtensions,
                            null);
            }
        }
        catch (Throwable t)
        {
            /*
             * The Javadoc on ThreadDeath says: If ThreadDeath is caught by a
             * method, it is important that it be rethrown so that the thread
             * actually dies.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                ProtocolProviderServiceJabberImpl.throwOperationFailedException(
                        "Failed to create a call to " + fullCalleeURI,
                        OperationFailedException.INTERNAL_ERROR,
                        t,
                        logger);
            }
        }

        return peer;
    }

    /**
     * Discovers the resource for <tt>calleeAddress</tt> with the highest
     * priority which supports either Jingle or Gtalk. Returns the full JID.
     *
     * @param calleeAddress the address of the callee
     *
     * @return the full callee URI
     */
    private String discoverFullJid(String calleeAddress)
    {
        String fullCalleeURI = null;
        DiscoverInfo discoverInfo = null;
        int bestPriority = -1;
        PresenceStatus jabberStatus = null;
        String calleeURI = null;

        Iterator<Presence> it
            = getProtocolProvider().getConnection().getRoster().getPresences(
                    calleeAddress);

        while(it.hasNext())
        {
            Presence presence = it.next();
            int priority
                = (presence.getPriority() == Integer.MIN_VALUE)
                    ? 0
                    : presence.getPriority();
            calleeURI = presence.getFrom();

            try
            {
                // check if the remote client supports telephony.
                discoverInfo
                    = protocolProvider.getDiscoveryManager().discoverInfo(
                            calleeURI);
            }
            catch (XMPPException ex)
            {
                logger.warn("could not retrieve info for " + fullCalleeURI, ex);
            }

            if (discoverInfo != null && discoverInfo.containsFeature(
                ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE))
            {
                if(priority > bestPriority)
                {
                    bestPriority = priority;
                    fullCalleeURI = calleeURI;
                    jabberStatus = OperationSetPersistentPresenceJabberImpl
                        .jabberStatusToPresenceStatus(
                                presence, protocolProvider);
                }
                else if(priority == bestPriority && jabberStatus != null)
                {
                    PresenceStatus tempStatus =
                        OperationSetPersistentPresenceJabberImpl
                           .jabberStatusToPresenceStatus(
                               presence, protocolProvider);
                    if(tempStatus.compareTo(jabberStatus) > 0)
                    {
                        fullCalleeURI = calleeURI;
                        jabberStatus = tempStatus;
                    }
                }
            }
        }

        if(logger.isInfoEnabled())
            logger.info("Full JID for outgoing call: " + fullCalleeURI
                            + ", priority " + bestPriority);

        return fullCalleeURI;
    }

    /**
     * Gets the full callee URI for a specific callee address.
     *
     * @param calleeAddress the callee address to get the full callee URI for
     * @return the full callee URI for the specified <tt>calleeAddress</tt>
     */
    String getFullCalleeURI(String calleeAddress)
    {
        return
            (calleeAddress.indexOf('/') > 0)
                ? calleeAddress
                : protocolProvider
                    .getConnection()
                        .getRoster()
                            .getPresence(calleeAddress)
                                .getFrom();
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return an iterator over all currently active calls.
     */
    public Iterator<CallJabberImpl> getActiveCalls()
    {
        return activeCallsRepository.getActiveCalls();
    }

    /**
     * Returns the active call peer corresponding to the given sid.
     *
     * @param sid the Jingle session ID of the active <tt>Call</tt> between the
     * local peer and the callee in the case of attended transfer; <tt>null</tt>
     * in the case of unattended transfer
     *
     * @return The active call peer corresponding to the given sid. "null" if
     * there is no such call.
     */
    public CallPeerJabberImpl getActiveCallPeer(String sid)
    {
        return activeCallsRepository.findCallPeer(sid);
    }

    /**
     * Resumes communication with a call peer previously put on hold.
     *
     * @param peer the call peer to put on hold.
     *
     * @throws OperationFailedException if we fail to send the "hold" message.
     */
    public synchronized void putOffHold(CallPeer peer)
        throws OperationFailedException
    {
        putOnHold(peer, false);
    }

    /**
     * Puts the specified CallPeer "on hold".
     *
     * @param peer the peer that we'd like to put on hold.
     *
     * @throws OperationFailedException if we fail to send the "hold" message.
     */
    public synchronized void putOnHold(CallPeer peer)
        throws OperationFailedException
    {
        putOnHold(peer, true);
    }

    /**
     * Puts the specified <tt>CallPeer</tt> on or off hold.
     *
     * @param peer the <tt>CallPeer</tt> to be put on or off hold
     * @param on <tt>true</tt> to have the specified <tt>CallPeer</tt>
     * put on hold; <tt>false</tt>, otherwise
     *
     * @throws OperationFailedException if we fail to send the "hold" message.
     */
    private void putOnHold(CallPeer peer, boolean on)
        throws OperationFailedException
    {
        if(peer instanceof CallPeerJabberImpl)
            ((CallPeerJabberImpl) peer).putOnHold(on);
    }

    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     *
     * @throws ClassCastException if peer is not an instance of this
     * CallPeerSipImpl.
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public synchronized void hangupCallPeer(CallPeer peer)
        throws ClassCastException,
               OperationFailedException
    {
        hangupCallPeer(peer, HANGUP_REASON_NORMAL_CLEARING, null);
    }

    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     * @param reasonCode indicates if the hangup is following to a call failure
     * or simply a disconnect indicate by the reason.
     * @param reasonText the reason of the hangup. If the hangup is due to a
     * call failure, then this string could indicate the reason of the failure
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    public void hangupCallPeer(CallPeer peer,
                               int reasonCode,
                               String reasonText)
    {
        boolean failed = (reasonCode != HANGUP_REASON_NORMAL_CLEARING);

        // if we are failing a peer and have a reason, add the reason packet
        // extension
        ReasonPacketExtension reasonPacketExt = null;

        if (failed && (reasonText != null))
        {
            Reason reason = convertReasonCodeToSIPCode(reasonCode);

            if (reason != null)
            {
                reasonPacketExt
                    = new ReasonPacketExtension(reason, reasonText, null);
            }
        }

        // XXX maybe add answer/hangup abstract method to MediaAwareCallPeer
        if(peer instanceof CallPeerJabberImpl)
        {
            ((CallPeerJabberImpl) peer).hangup(
                    failed,
                    reasonText,
                    reasonPacketExt);
        }
    }

    /**
     * Converts the codes for hangup from OperationSetBasicTelephony one
     * to the jabber reasons.
     * @param reasonCode the reason code.
     * @return the jabber Response.
     */
    private static Reason convertReasonCodeToSIPCode(int reasonCode)
    {
        switch(reasonCode)
        {
            case HANGUP_REASON_NORMAL_CLEARING :
                return Reason.SUCCESS;
            case HANGUP_REASON_ENCRYPTION_REQUIRED :
                return Reason.SECURITY_ERROR;
            case HANGUP_REASON_TIMEOUT :
                return Reason.TIMEOUT;
            case HANGUP_REASON_BUSY_HERE :
                return Reason.BUSY;
            default : return null;
        }
    }

    /**
     * Implements method <tt>answerCallPeer</tt>
     * from <tt>OperationSetBasicTelephony</tt>.
     *
     * @param peer the call peer that we want to answer
     * @throws OperationFailedException if we fails to answer
     */
    public void answerCallPeer(CallPeer peer)
        throws OperationFailedException
    {
        // XXX maybe add answer/hangup abstract method to MediaAwareCallPeer
        if(peer instanceof CallPeerJabberImpl)
            ((CallPeerJabberImpl) peer).answer();
    }

    /**
     * Closes all active calls. And releases resources.
     */
    public void shutdown()
    {
        if (logger.isTraceEnabled())
            logger.trace("Ending all active calls. ");
        Iterator<CallJabberImpl> activeCalls
            = this.activeCallsRepository.getActiveCalls();

        // this is fast, but events aren't triggered ...
        //jingleManager.disconnectAllSessions();

        //go through all active calls.
        while(activeCalls.hasNext())
        {
            CallJabberImpl call = activeCalls.next();
            Iterator<CallPeerJabberImpl> callPeers = call.getCallPeers();

            //go through all call peers and say bye to every one.
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                try
                {
                    hangupCallPeer(peer);
                }
                catch (Exception ex)
                {
                    logger.warn("Failed to properly hangup peer " + peer, ex);
                }
            }
        }
    }

    /**
     * Subscribes us to notifications about incoming jingle packets.
     */
    private void subscribeForJinglePackets()
    {
        protocolProvider.getConnection().addPacketListener(this, this);
    }

    /**
     * Unsubscribes us from notifications about incoming jingle packets.
     */
    private void unsubscribeForJinglePackets()
    {
        XMPPConnection connection = protocolProvider.getConnection();

        if(connection != null)
            connection.removePacketListener(this);
    }

    /**
     * Tests whether or not the specified packet should be handled by this
     * operation set. This method is called by smack prior to packet delivery
     * and it would only accept <tt>JingleIQ</tt>s that are either session
     * initiations with RTP content or belong to sessions that are already
     * handled by this operation set.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet)
    {
        // We handle JingleIQ and SessionIQ.
        if(!(packet instanceof JingleIQ))
        {
            String packetID = packet.getPacketID();
            AbstractCallPeer<?, ?> callPeer
                = activeCallsRepository.findCallPeerBySessInitPacketID(
                        packetID);

            if(callPeer != null)
            {
                /* packet is a response to a Jingle call but is not a JingleIQ
                 * so it is for sure an error (peer does not support Jingle or
                 * does not belong to our roster)
                 */
                XMPPError error = packet.getError();

                if (error != null)
                {
                    String errorMessage = error.getMessage();

                    logger.error(
                            "Received an error: code=" + error.getCode()
                                + " message=" + errorMessage);

                    String message;

                    if (errorMessage == null)
                    {
                        Roster roster
                            = getProtocolProvider().getConnection().getRoster();
                        String packetFrom = packet.getFrom();

                        message = "Service unavailable";
                        if(!roster.contains(packetFrom))
                        {
                            message
                                += ": try adding the contact " + packetFrom
                                    + " to your contact list first.";
                        }
                    }
                    else
                        message = errorMessage;

                    callPeer.setState(CallPeerState.FAILED, message);
                }
            }
            return false;
        }

        if(packet instanceof JingleIQ)
        {
            JingleIQ jingleIQ = (JingleIQ)packet;

            if( jingleIQ.getAction() == JingleAction.SESSION_INITIATE)
            {
                //we only accept session-initiate-s dealing RTP
                return
                    jingleIQ.containsContentChildOfType(
                            RtpDescriptionPacketExtension.class);
            }

            String sid = jingleIQ.getSID();

            //if this is not a session-initiate we'll only take it if we've
            //already seen its session ID.
            return (activeCallsRepository.findSID(sid) != null);
        }
        return false;
    }

    /**
     * Handles incoming jingle packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        IQ iq = (IQ) packet;

        /*
         * To prevent hijacking sessions from other Jingle-based features such
         * as file transfer, we should send the ack only if this is a
         * session-initiate with RTP content or if we are the owners of the
         * packet's SID.
         */

        //first ack all "set" requests.
        if(iq.getType() == IQ.Type.SET)
        {
            IQ ack = IQ.createResultIQ(iq);

            protocolProvider.getConnection().sendPacket(ack);
        }

        try
        {
            if (iq instanceof JingleIQ)
                processJingleIQ((JingleIQ) iq);
        }
        catch(Throwable t)
        {
            if (logger.isInfoEnabled())
            {
                String packetClass;

                if (iq instanceof JingleIQ)
                    packetClass = "Jingle";
                else
                    packetClass = packet.getClass().getSimpleName();

                logger.info(
                        "Error while handling incoming " + packetClass
                            + " packet: ",
                        t);
            }

            /*
             * The Javadoc on ThreadDeath says: If ThreadDeath is caught by
             * a method, it is important that it be rethrown so that the
             * thread actually dies.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
    }

    /**
     * Analyzes the <tt>jingleIQ</tt>'s action and passes it to the
     * corresponding handler.
     *
     * @param jingleIQ the {@link JingleIQ} packet we need to be analyzing.
     */
    private void processJingleIQ(final JingleIQ jingleIQ)
    {
        //let's first see whether we have a peer that's concerned by this IQ
        CallPeerJabberImpl callPeer
            = activeCallsRepository.findCallPeer(jingleIQ.getSID());
        IQ.Type type = jingleIQ.getType();

        if (type == Type.ERROR)
        {
            logger.error("Received error");

            XMPPError error = jingleIQ.getError();
            String message = "Remote party returned an error!";

            if(error != null)
            {
                String errorStr
                    = "code=" + error.getCode()
                        + " message=" + error.getMessage();

                message += "\n" + errorStr;
                logger.error(" " + errorStr);
            }

            if (callPeer != null)
                callPeer.setState(CallPeerState.FAILED, message);

            return;
        }

        JingleAction action = jingleIQ.getAction();

        if(action == JingleAction.SESSION_INITIATE)
        {
            TransferPacketExtension transfer
                = (TransferPacketExtension)
                    jingleIQ.getExtension(
                            TransferPacketExtension.ELEMENT_NAME,
                            TransferPacketExtension.NAMESPACE);
            CallIdPacketExtension callidExt
                = (CallIdPacketExtension)
                    jingleIQ.getExtension(
                        ConferenceDescriptionPacketExtension.CALLID_ELEM_NAME,
                        ConferenceDescriptionPacketExtension.NAMESPACE);
            CallJabberImpl call = null;

            if (transfer != null)
            {
                String sid = transfer.getSID();

                if (sid != null)
                {
                    CallJabberImpl attendantCall
                        =  getActiveCallsRepository().findSID(sid);

                    if (attendantCall != null)
                    {
                        CallPeerJabberImpl attendant
                            = attendantCall.getPeer(sid);

                        if ((attendant != null)
                                && getFullCalleeURI(attendant.getAddress())
                                        .equals(transfer.getFrom())
                                && protocolProvider.getOurJID().equals(
                                        transfer.getTo()))
                        {
                            // OK, we are legally involved in the transfer.
                            call = attendantCall;
                        }
                    }
                }
            }

            if (callidExt != null)
            {
                String callid = callidExt.getText();

                if (callid != null)
                    call = getActiveCallsRepository().findCallId(callid);
            }

            if (transfer != null && callidExt != null)
                logger.warn("Received a session-initiate with both 'transfer'" +
                        " and 'callid' extensions. Ignored 'transfer' and" +
                        " used 'callid'.");

            if(call == null)
                call = new CallJabberImpl(this);

            final CallJabberImpl finalCall = call;

            new Thread()
            {
                @Override
                public void run()
                {
                    finalCall.processSessionInitiate(jingleIQ);
                }
            }.start();

            return;
        }
        else if (callPeer == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a stray trying response.");
            return;
        }

        //the rest of these cases deal with existing peers
        else if(action == JingleAction.SESSION_TERMINATE)
        {
            callPeer.processSessionTerminate(jingleIQ);
        }
        else if(action == JingleAction.SESSION_ACCEPT)
        {
            callPeer.processSessionAccept(jingleIQ);
        }
        else if (action == JingleAction.SESSION_INFO)
        {
            SessionInfoPacketExtension info = jingleIQ.getSessionInfo();

            if(info != null)
            {
                // change status.
                callPeer.processSessionInfo(info);
            }
            else
            {
                PacketExtension packetExtension
                    = jingleIQ.getExtension(
                            TransferPacketExtension.ELEMENT_NAME,
                            TransferPacketExtension.NAMESPACE);

                if (packetExtension instanceof TransferPacketExtension)
                {
                    TransferPacketExtension transfer
                        = (TransferPacketExtension) packetExtension;

                    if (transfer.getFrom() == null)
                        transfer.setFrom(jingleIQ.getFrom());

                    try
                    {
                        callPeer.processTransfer(transfer);
                    }
                    catch (OperationFailedException ofe)
                    {
                        logger.error(
                                "Failed to transfer to " + transfer.getTo(),
                                ofe);
                    }
                }

                packetExtension
                    = jingleIQ.getExtension(
                        CoinPacketExtension.ELEMENT_NAME,
                        CoinPacketExtension.NAMESPACE);

                if (packetExtension instanceof CoinPacketExtension)
                {
                    CoinPacketExtension coinExt
                        = (CoinPacketExtension)packetExtension;

                    callPeer.setConferenceFocus(
                            Boolean.parseBoolean(
                                    coinExt.getAttributeAsString(
                                            CoinPacketExtension
                                                .ISFOCUS_ATTR_NAME)));
                }
            }
        }
        else if (action == JingleAction.CONTENT_ACCEPT)
        {
            callPeer.processContentAccept(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_ADD)
        {
            callPeer.processContentAdd(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_MODIFY)
        {
            callPeer.processContentModify(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_REJECT)
        {
            callPeer.processContentReject(jingleIQ);
        }
        else if (action == JingleAction.CONTENT_REMOVE)
        {
            callPeer.processContentRemove(jingleIQ);
        }
        else if (action == JingleAction.TRANSPORT_INFO)
        {
            callPeer.processTransportInfo(jingleIQ);
        }
    }

    /**
     * Returns a reference to the {@link ActiveCallsRepositoryJabberGTalkImpl}
     * that we are currently using.
     *
     * @return a reference to the {@link ActiveCallsRepositoryJabberGTalkImpl}
     * that we are currently using.
     */
    protected ActiveCallsRepositoryJabberGTalkImpl
        <CallJabberImpl, CallPeerJabberImpl>
            getActiveCallsRepository()
    {
        return activeCallsRepository;
    }

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    public ProtocolProviderServiceJabberImpl getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Gets the secure state of the call session in which a specific peer
     * is involved
     *
     * @param peer the peer for who the call state is required
     * @return the call state
     */
    public boolean isSecure(CallPeer peer)
    {
        return ((MediaAwareCallPeer<?, ?, ?>) peer).getMediaHandler().
            isSecure();
    }

    /**
     * Transfers (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which already participates in an active
     * <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of attended call
     * transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param target the address in the form of <tt>CallPeer</tt> of the callee
     * to transfer <tt>peer</tt> to
     * @throws OperationFailedException if something goes wrong
     * @see OperationSetAdvancedTelephony#transfer(CallPeer, CallPeer)
     */
    public void transfer(CallPeer peer, CallPeer target)
        throws OperationFailedException
    {
        AbstractCallPeerJabberGTalkImpl<?,?,?> targetJabberGTalkImpl
            = (AbstractCallPeerJabberGTalkImpl<?,?,?>) target;
        String to = getFullCalleeURI(targetJabberGTalkImpl.getAddress());

        /*
         * XEP-0251: Jingle Session Transfer says: Before doing
         * [attended transfer], the attendant SHOULD verify that the callee
         * supports Jingle session transfer.
         */
        try
        {
            DiscoverInfo discoverInfo
                = protocolProvider.getDiscoveryManager().discoverInfo(to);

            if (!discoverInfo.containsFeature(
                    ProtocolProviderServiceJabberImpl
                        .URN_XMPP_JINGLE_TRANSFER_0))
            {
                throw new OperationFailedException(
                        "Callee "
                            + to
                            + " does not support"
                            + " XEP-0251: Jingle Session Transfer",
                        OperationFailedException.INTERNAL_ERROR);
            }
        }
        catch (XMPPException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        transfer(
            peer,
            to, targetJabberGTalkImpl.getSID());
    }

    /**
     * Transfers (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which may or may not already be participating
     * in an active <tt>Call</tt>.
     * <p>
     * The method is suitable for providing the implementation of unattended
     * call transfer (though no such requirement is imposed).
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param target the address of the callee to transfer <tt>peer</tt> to
     * @throws OperationFailedException if something goes wrong
     * @see OperationSetAdvancedTelephony#transfer(CallPeer, String)
     */
    public void transfer(CallPeer peer, String target)
        throws OperationFailedException
    {
        transfer(peer, target, null);
    }

    /**
     * Transfer (in the sense of call transfer) a specific <tt>CallPeer</tt> to
     * a specific callee address which may optionally be participating in an
     * active <tt>Call</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to be transfered to the specified
     * callee address
     * @param to the address of the callee to transfer <tt>peer</tt> to
     * @param sid the Jingle session ID of the active <tt>Call</tt> between the
     * local peer and the callee in the case of attended transfer; <tt>null</tt>
     * in the case of unattended transfer
     * @throws OperationFailedException if something goes wrong
     */
    private void transfer(CallPeer peer, String to, String sid)
        throws OperationFailedException
    {
        String caller = getFullCalleeURI(peer.getAddress());

        try
        {
            DiscoverInfo discoverInfo
                = protocolProvider.getDiscoveryManager().discoverInfo(caller);

            if (!discoverInfo.containsFeature(
                    ProtocolProviderServiceJabberImpl
                        .URN_XMPP_JINGLE_TRANSFER_0))
            {
                throw new OperationFailedException(
                        "Caller "
                            + caller
                            + " does not support"
                            + " XEP-0251: Jingle Session Transfer",
                        OperationFailedException.INTERNAL_ERROR);
            }
        }
        catch (XMPPException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        ((CallPeerJabberImpl) peer).transfer(getFullCalleeURI(to), sid);
    }

    /**
     * Transfer authority used for interacting with user for unknown calls
     *  and the requests for transfer.
     * @param authority transfer authority.
     */
    public void setTransferAuthority(TransferAuthority authority)
    {
    }
}
