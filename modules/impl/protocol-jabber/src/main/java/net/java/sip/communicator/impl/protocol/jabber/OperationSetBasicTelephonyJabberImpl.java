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

import org.jitsi.xmpp.extensions.condesc.*;
import org.jitsi.xmpp.extensions.jingle.*;
import org.jitsi.xmpp.extensions.jitsimeet.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.service.protocol.media.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.roster.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.stringprep.*;

/**
 * Implements all call management logic and exports basic telephony support by
 * implementing <tt>OperationSetBasicTelephony</tt>.
 *
 * @author Emil Ivov
 * @author Symphorien Wanko
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 * @author Boris Grozev
 * @author Cristian Florin Ghita
 */
public class OperationSetBasicTelephonyJabberImpl
   extends AbstractOperationSetBasicTelephony<ProtocolProviderServiceJabberImpl>
   implements RegistrationStateChangeListener,
              StanzaListener,
              StanzaFilter,
              OperationSetSecureSDesTelephony,
              OperationSetSecureZrtpTelephony,
              OperationSetAdvancedTelephony<ProtocolProviderServiceJabberImpl>
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetBasicTelephonyJabberImpl</tt> class and its instances for
     * logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetBasicTelephonyJabberImpl.class);

    /**
     * A reference to the <tt>ProtocolProviderServiceJabberImpl</tt> instance
     * that created us.
     */
    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Contains references for all currently active (non ended) calls.
     */
    private final ActiveCallsRepositoryJabberImpl activeCallsRepository
            = new ActiveCallsRepositoryJabberImpl(this);

    /** Jingle IQ set stanza processor */
    private final JingleIqSetRequestHandler setRequestHandler
        = new JingleIqSetRequestHandler();

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
            ProviderManager.addIQProvider(
                    JingleIQ.ELEMENT,
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

        String remoteUri = cd.getUri();
        if (remoteUri.startsWith("xmpp:"))
            remoteUri = remoteUri.substring(5);

        Jid remoteJid;
        try
        {
            remoteJid = JidCreate.from(remoteUri);
        }
        catch (XmppStringprepException e)
        {
            throw new OperationFailedException(
                "Invalid remote JID",
                OperationFailedException.GENERAL_ERROR,
                e);
        }

        List<ExtensionElement> sessionInitiateExtensions
                = new ArrayList<>(2);

        String callid = cd.getCallId();
        if (callid != null)
        {
            sessionInitiateExtensions.add(new CallIdExtension(callid));
        }

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
     * <tt>ExtensionElement</tt>s to be added to the <tt>session-initiate</tt>
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
            Iterable<ExtensionElement> sessionInitiateExtensions)
        throws OperationFailedException
    {
        if (calleeAddress.contains("/"))
            return createOutgoingCall(call, calleeAddress, calleeAddress, sessionInitiateExtensions);
        else
            return createOutgoingCall(call, calleeAddress, null, sessionInitiateExtensions);
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
     * <tt>ExtensionElement</tt>s to be added to the <tt>session-initiate</tt>
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
            Iterable<ExtensionElement> sessionInitiateExtensions)
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
            String serviceName;

            if ((phoneSuffix == null) || (phoneSuffix.length() == 0))
            {
                try
                {
                    serviceName = JidCreate.from(
                        accountID.getUserID()).getDomain().toString();
                }
                catch (XmppStringprepException e)
                {
                    throw new OperationFailedException(
                        "UserID is not a valid JID",
                        OperationFailedException.GENERAL_ERROR,
                        e);
                }
            }
            else
            {
                serviceName = phoneSuffix;
            }

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

        Jid calleeJid;
        try
        {
            calleeJid = JidCreate.from(calleeAddress);
        }
        catch (XmppStringprepException e)
        {
            throw new OperationFailedException(
                calleeAddress + " for callee address is not a valid JID",
                OperationFailedException.GENERAL_ERROR,
                e);
        }
        Roster r = Roster.getInstanceFor(getProtocolProvider().getConnection());
        if((!r.contains(calleeJid.asBareJid()) && !isPrivateMessagingContact)
            && !alwaysCallGtalk)
        {
            throw new OperationFailedException(
                calleeAddress + " does not belong to our contact list",
                OperationFailedException.NOT_FOUND);
        }

        // If there's no fullCalleeURI specified we'll discover the most
        // connected one with highest priority.
        EntityFullJid fullCalleeJid = null;
        try
        {
            fullCalleeJid = JidCreate.entityFullFrom(fullCalleeURI);
        }
        catch (XmppStringprepException e)
        {
            // ignore, try to obtain it via calleeJid
        }

        if (fullCalleeJid == null)
            fullCalleeJid = discoverFullJid(calleeJid);

        if (fullCalleeJid == null)
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
                fullCalleeJid);
        }
        catch (XMPPException
            | InterruptedException
            | NoResponseException
            | NotConnectedException ex)
        {
            logger.warn("could not retrieve info for " + fullCalleeJid, ex);
        }

        if(di != null)
        {
            if (logger.isInfoEnabled())
                logger.info(fullCalleeJid + ": jingle supported ");
        }
        else
        {
            if (logger.isInfoEnabled())
                logger.info(fullCalleeJid + ": jingle not supported?");

            throw new OperationFailedException(
                    "Failed to create an outgoing call.\n"
                    + fullCalleeJid + " does not support jingle",
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
            peer = call.initiateSession(
                        fullCalleeJid,
                        di,
                        sessionInitiateExtensions,
                        null);
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
                        "Failed to create a call to " + fullCalleeJid,
                        OperationFailedException.INTERNAL_ERROR,
                        t,
                        logger);
            }
        }

        return peer;
    }

    /**
     * Discovers the resource for <tt>calleeAddress</tt> with the highest
     * priority which supports Jingle. Returns the full JID.
     *
     * @param calleeAddress the address of the callee
     *
     * @return the full callee URI
     */
    private EntityFullJid discoverFullJid(Jid calleeAddress)
    {
        Jid fullCalleeURI = null;
        DiscoverInfo discoverInfo = null;
        int bestPriority = -1;
        PresenceStatus jabberStatus = null;
        Jid calleeURI;

        Roster r = Roster.getInstanceFor(getProtocolProvider().getConnection());
        for (Presence presence : r.getPresences(calleeAddress.asBareJid()))
        {
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
            catch (XMPPException
                | InterruptedException
                | NoResponseException
                | NotConnectedException ex)
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

        logger.info("Full JID for outgoing call: {}, priority {}",
            fullCalleeURI, bestPriority);

        if (fullCalleeURI != null)
        {
            return fullCalleeURI.asEntityFullJidOrThrow();
        }

        return null;
    }

    /**
     * Gets the full callee URI for a specific callee address.
     *
     * @param calleeAddress the callee address to get the full callee URI for
     * @return the full callee URI for the specified <tt>calleeAddress</tt>
     */
    EntityFullJid getFullCalleeURI(String calleeAddress)
    {
        try
        {
            Jid calleeJid = JidCreate.from(calleeAddress);
            if (calleeJid.isEntityFullJid())
            {
                return calleeJid.asEntityFullJidOrThrow();
            }

            Roster r = Roster.getInstanceFor(protocolProvider.getConnection());
            return r.getPresence(calleeJid.asBareJid())
                    .getFrom()
                    .asEntityFullJidOrThrow();
        }
        catch (XmppStringprepException e)
        {
            throw new IllegalArgumentException(
                "calleeAddress is not a valid JID", e);
        }
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
        throws OperationFailedException
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
            try
            {
                ((CallPeerJabberImpl) peer).hangup(
                        failed,
                        reasonText,
                        reasonPacketExt);
            }
            catch (NotConnectedException | InterruptedException e)
            {
                throw new OperationFailedException(
                    "Could not hang up",
                    OperationFailedException.GENERAL_ERROR,
                    e);
            }
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
        XMPPConnection conn = protocolProvider.getConnection();
        conn.addAsyncStanzaListener(this, this);
        conn.registerIQRequestHandler(setRequestHandler);
    }

    /**
     * Unsubscribes us from notifications about incoming jingle packets.
     */
    private void unsubscribeForJinglePackets()
    {
        XMPPConnection connection = protocolProvider.getConnection();
        if(connection != null)
        {
            connection.removeAsyncStanzaListener(this);
            connection.unregisterIQRequestHandler(setRequestHandler);
        }
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
    @Override
    public boolean accept(Stanza packet)
    {
        // We handle JingleIQ and SessionIQ.
        if(!(packet instanceof JingleIQ))
        {
            String packetID = packet.getStanzaId();
            AbstractCallPeer<?, ?> callPeer
                = activeCallsRepository.findCallPeerBySessInitPacketID(
                        packetID);

            if(callPeer != null)
            {
                /* packet is a response to a Jingle call but is not a JingleIQ
                 * so it is for sure an error (peer does not support Jingle or
                 * does not belong to our roster)
                 */
                StanzaError error = packet.getError();

                if (error != null)
                {
                    logger.error(
                            "Received an error: condition=" + error.getCondition()
                                + " message=" + error.getConditionText());

                    String message;

                    if (error.getConditionText() == null)
                    {
                        Roster roster = Roster.getInstanceFor(
                            getProtocolProvider().getConnection());
                        Jid packetFrom = packet.getFrom();

                        // FIXME i18n
                        message = "Service unavailable";
                        if(!roster.contains(packetFrom.asBareJid()))
                        {
                            message
                                += ": try adding the contact " + packetFrom
                                    + " to your contact list first.";
                        }
                    }
                    else
                        message = error.getConditionText();

                    callPeer.setState(CallPeerState.FAILED, message);
                }
            }
            return false;
        }

        JingleIQ jingleIQ = (JingleIQ)packet;
        if (jingleIQ.getAction() == JingleAction.SESSION_INITIATE)
        {
            //we only accept session-initiate-s dealing RTP
            return jingleIQ.containsContentChildOfType(
                        RtpDescriptionPacketExtension.class);
        }

        String sid = jingleIQ.getSID();

        //if this is not a session-initiate we'll only take it if we've
        //already seen its session ID.
        return (activeCallsRepository.findSID(sid) != null);
    }

    /**
     * Handles incoming jingle packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    @Override
    public void processStanza(Stanza packet)
    {
        IQ iq = (IQ) packet;

        /*
         * To prevent hijacking sessions from other Jingle-based features such
         * as file transfer, we should send the ack only if this is a
         * session-initiate with RTP content or if we are the owners of the
         * packet's SID.
         */
        try
        {
            if (iq instanceof JingleIQ)
            {
                processJingleIQError((JingleIQ) iq);
            }
        }
        catch(Exception t)
        {
            logger.info("Error while handling incoming Jingle packet", t);
        }
    }

    private class JingleIqSetRequestHandler extends AbstractIqRequestHandler
    {
        protected JingleIqSetRequestHandler()
        {
            super(JingleIQ.ELEMENT, JingleIQ.NAMESPACE, Type.set, Mode.sync);
        }

        @Override
        public IQ handleIQRequest(IQ iq)
        {
            try
            {
                // send ack, then process request
                protocolProvider.getConnection().sendStanza(IQ.createResultIQ(iq));
                processJingleIQ((JingleIQ) iq);
            }
            catch (Exception e)
            {
                logger.error("Error while handling incoming " + iq.getClass()
                        + " packet: ", e);
            }

            return null;
        }
    }

    /**
     * Analyzes the <tt>jingleIQ</tt>'s action and passes it to the
     * corresponding handler.
     *
     * @param jingleIQ the {@link JingleIQ} packet we need to be analyzing.
     */
    private void processJingleIQ(final JingleIQ jingleIQ)
        throws NotConnectedException, InterruptedException
    {
        //let's first see whether we have a peer that's concerned by this IQ
        CallPeerJabberImpl callPeer
            = activeCallsRepository.findCallPeer(jingleIQ.getSID());

        JingleAction action = jingleIQ.getAction();

        if(action == JingleAction.SESSION_INITIATE)
        {
            StartMutedPacketExtension startMutedExt
                = jingleIQ.getExtension(StartMutedPacketExtension.class);

            if (startMutedExt != null)
            {
                ProtocolProviderServiceJabberImpl protocolProvider
                    = getProtocolProvider();

                OperationSetJitsiMeetToolsJabberImpl operationSetJitsiMeetTools
                    = (OperationSetJitsiMeetToolsJabberImpl)protocolProvider
                        .getOperationSet(OperationSetJitsiMeetToolsJabber.class);

                if (operationSetJitsiMeetTools != null)
                {
                    boolean[] startMutedFlags = {
                        startMutedExt.getAudioMuted(),
                        startMutedExt.getVideoMuted(),
                    };

                    operationSetJitsiMeetTools
                        .notifySessionStartMuted(startMutedFlags);
                }
                else
                {
                    logger.warn("StartMutedPacketExtension not handled!" +
                                    "OperationSetJitsiMeetTools not available.");
                }
            }

            TransferPacketExtension transfer
                = jingleIQ.getExtension(TransferPacketExtension.class);
            CallIdExtension callidExt
                = jingleIQ.getExtension(CallIdExtension.class);
            CallJabberImpl call = null;

            if (transfer != null)
            {
                String sid = transfer.getSID();

                if (sid != null)
                {
                    CallJabberImpl attendantCall
                        = getActiveCallsRepository().findSID(sid);

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

            new Thread(() -> finalCall.processSessionInitiate(jingleIQ)).start();
        }
        else if (callPeer == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a stray trying response.");
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
                TransferPacketExtension transfer
                    = jingleIQ.getExtension(TransferPacketExtension.class);

                if (transfer != null)
                {
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

                CoinPacketExtension coinExt
                    = jingleIQ.getExtension(CoinPacketExtension.class);

                if (coinExt != null)
                {
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
        else if (action == JingleAction.TRANSPORT_REPLACE)
        {
            callPeer.processTransportReplace(jingleIQ);
        }
        else if (action == JingleAction.SOURCEADD)
        {
            callPeer.processSourceAdd(jingleIQ);
        }
        else if (action == JingleAction.SOURCEREMOVE)
        {
            callPeer.processSourceRemove(jingleIQ);
        }
    }

    private void processJingleIQError(JingleIQ jingleIQ)
    {
        //let's first see whether we have a peer that's concerned by this IQ
        CallPeerJabberImpl callPeer
            = activeCallsRepository.findCallPeer(jingleIQ.getSID());

        StanzaError error = jingleIQ.getError();
        // FIXME get from i18n
        String message = "Remote party returned an error!";
        if(error != null)
        {
            String errorStr
                = "code=" + error.getCondition()
                    + " message=" + error.getConditionText();

            message += "\n" + errorStr;
        }

        logger.error(message);
        if (callPeer != null)
            callPeer.setState(CallPeerState.FAILED, message);
    }

    /**
     * Returns a reference to the {@link ActiveCallsRepositoryJabberImpl}
     * that we are currently using.
     *
     * @return a reference to the {@link ActiveCallsRepositoryJabberImpl}
     * that we are currently using.
     */
    protected ActiveCallsRepositoryJabberImpl getActiveCallsRepository()
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
        CallPeerJabberImpl jabberTarget = (CallPeerJabberImpl) target;
        EntityFullJid to = getFullCalleeURI(jabberTarget.getAddress());

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
        catch (XMPPException
            | InterruptedException
            | NoResponseException
            | NotConnectedException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        transfer(peer, to, jabberTarget.getSID());
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
        EntityFullJid targetJid = getFullCalleeURI(target);
        transfer(peer, targetJid, null);
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
    private void transfer(CallPeer peer, EntityFullJid to, String sid)
        throws OperationFailedException
    {
        EntityFullJid caller = getFullCalleeURI(peer.getAddress());
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
        catch (XMPPException
            | InterruptedException
            | NoResponseException
            | NotConnectedException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        ((CallPeerJabberImpl) peer).transfer(to, sid);
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
