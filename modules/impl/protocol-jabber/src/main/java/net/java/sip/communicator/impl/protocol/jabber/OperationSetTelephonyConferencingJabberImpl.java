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

import org.jitsi.xmpp.extensions.coin.*;
import org.jitsi.xmpp.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;

import org.jitsi.util.xml.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.*;

/**
 * Implements <tt>OperationSetTelephonyConferencing</tt> for Jabber.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 * @author Boris Grozev
 * @author Pawel Domas
 */
public class OperationSetTelephonyConferencingJabberImpl
    extends AbstractOperationSetTelephonyConferencing<
            ProtocolProviderServiceJabberImpl,
            OperationSetBasicTelephonyJabberImpl,
            CallJabberImpl,
            CallPeerJabberImpl,
            String>
    implements RegistrationStateChangeListener,
               StanzaListener,
               StanzaFilter,
               IQRequestHandler
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetTelephonyConferencingJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetTelephonyConferencingJabberImpl.class);

    /**
     * The minimum interval in milliseconds between COINs sent to a single
     * <tt>CallPeer</tt>.
     */
    private static final int COIN_MIN_INTERVAL = 200;

    /**
     * Property used to disable COIN notifications.
     */
    public static final String DISABLE_COIN_PROP_NAME
        = "net.java.sip.communicator.impl.protocol.jabber.DISABLE_COIN";

    /**
     * Synchronization object.
     */
    private final Object lock = new Object();

    /**
     * Field indicates whether COIN notification are disabled or not.
     */
    private final boolean isCoinDisabled;

    /**
     * Initializes a new <tt>OperationSetTelephonyConferencingJabberImpl</tt>
     * instance which is to provide telephony conferencing services for the
     * specified Jabber <tt>ProtocolProviderService</tt> implementation.
     *
     * @param parentProvider the Jabber <tt>ProtocolProviderService</tt>
     * implementation which has requested the creation of the new instance and
     * for which the new instance is to provide telephony conferencing services
     */
    public OperationSetTelephonyConferencingJabberImpl(
        ProtocolProviderServiceJabberImpl parentProvider)
    {
        super(parentProvider);

        this.isCoinDisabled
            = JabberActivator.getConfigurationService()
                    .getBoolean(DISABLE_COIN_PROP_NAME, false);
    }

    /**
     * Notifies all <tt>CallPeer</tt>s associated with a specific <tt>Call</tt>
     * about changes in the telephony conference-related information. In
     * contrast, {@link #notifyAll()} notifies all <tt>CallPeer</tt>s associated
     * with the telephony conference in which a specific <tt>Call</tt> is
     * participating.
     *
     * @param call the <tt>Call</tt> whose <tt>CallPeer</tt>s are to be notified
     * about changes in the telephony conference-related information
     */
    @Override
    protected void notifyCallPeers(Call call)
    {
        if (!isCoinDisabled && call.isConferenceFocus())
        {
            synchronized (lock)
            {
                // send conference-info to all CallPeers of the specified call.
                for (Iterator<? extends CallPeer> i = call.getCallPeers();
                        i.hasNext();)
                {
                    notify(i.next());
                }
            }
        }
    }

    /**
     * Notifies a specific <tt>CallPeer</tt> about changes in the telephony
     * conference-related information.
     *
     * @param callPeer the <tt>CallPeer</tt> to notify.
     */
    private void notify(CallPeer callPeer)
    {
        if(!(callPeer instanceof CallPeerJabberImpl))
            return;

        //Don't send COINs to peers with might not be ready to accept COINs yet
        CallPeerState peerState = callPeer.getState();
        if (peerState == CallPeerState.CONNECTING
                || peerState == CallPeerState.UNKNOWN
                || peerState == CallPeerState.INITIATING_CALL
                || peerState == CallPeerState.DISCONNECTED
                || peerState == CallPeerState.FAILED)
            return;

        final CallPeerJabberImpl callPeerJabber = (CallPeerJabberImpl)callPeer;

        final long timeSinceLastCoin = System.currentTimeMillis()
                - callPeerJabber.getLastConferenceInfoSentTimestamp();
        if (timeSinceLastCoin < COIN_MIN_INTERVAL)
        {
            if (callPeerJabber.isConfInfoScheduled())
                return;

            logger.info("Scheduling to send a COIN to " + callPeerJabber);
            callPeerJabber.setConfInfoScheduled(true);
            new Thread(() ->
            {
                try
                {
                    Thread.sleep(1 + COIN_MIN_INTERVAL - timeSinceLastCoin);
                }
                catch (InterruptedException ie)
                {
                    // ignore
                    Thread.currentThread().interrupt();
                }

                OperationSetTelephonyConferencingJabberImpl.this
                        .notify(callPeerJabber);
            }).start();

            return;
        }

        // check that callPeer supports COIN before sending him a
        // conference-info
        EntityFullJid to = getBasicTelephony()
            .getFullCalleeURI(callPeer.getAddress());

        // XXX if this generates actual disco#info requests we might want to
        // cache it.
        try
        {
            DiscoverInfo discoverInfo
                = parentProvider.getDiscoveryManager().discoverInfo(to);

            if (!discoverInfo.containsFeature(
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_COIN))
            {
                logger.info(callPeer.getAddress() + " does not support COIN");
                callPeerJabber.setConfInfoScheduled(false);
                return;
            }
        }
        catch (XMPPException
            | InterruptedException
            | NotConnectedException
            | NoResponseException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        ConferenceInfoDocument currentConfInfo
                = getCurrentConferenceInfo(callPeerJabber);
        ConferenceInfoDocument lastSentConfInfo
                = callPeerJabber.getLastConferenceInfoSent();

        ConferenceInfoDocument diff;

        if (lastSentConfInfo == null)
            diff = currentConfInfo;
        else
            diff = getConferenceInfoDiff(lastSentConfInfo, currentConfInfo);

        if (diff != null)
        {
            int newVersion
                    = lastSentConfInfo == null
                    ? 1
                    : lastSentConfInfo.getVersion() + 1;
            diff.setVersion(newVersion);

            IQ iq = getConferenceInfo(callPeerJabber, diff);

            if (iq != null)
            {
                try
                {
                    parentProvider.getConnection().sendStanza(iq);
                }
                catch (NotConnectedException | InterruptedException e)
                {
                    logger.error("Could not send conference IQ", e);
                    return;
                }

                // We save currentConfInfo, because it is of state "full", while
                // diff could be a partial
                currentConfInfo.setVersion(newVersion);
                callPeerJabber.setLastConferenceInfoSent(currentConfInfo);
                callPeerJabber.setLastConferenceInfoSentTimestamp(
                        System.currentTimeMillis());
            }
        }
        callPeerJabber.setConfInfoScheduled(false);
    }

    /**
     * Generates the conference-info IQ to be sent to a specific
     * <tt>CallPeer</tt> in order to notify it of the current state of the
     * conference managed by the local peer.
     *
     * @param callPeer the <tt>CallPeer</tt> to generate conference-info XML for
     * @param confInfo the <tt>ConferenceInformationDocument</tt> which is to be
     * included in the IQ
     * @return the conference-info IQ to be sent to the specified
     * <tt>callPeer</tt> in order to notify it of the current state of the
     * conference managed by the local peer
     */
    private IQ getConferenceInfo(CallPeerJabberImpl callPeer,
                                 final ConferenceInfoDocument confInfo)
    {
        String callPeerSID = callPeer.getSID();

        if (callPeerSID == null)
            return null;

        IQ iq = new IQ(ConferenceInfoDocument.CONFERENCE_INFO_ELEMENT_NAME,
            ConferenceInfoDocument.NAMESPACE){
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
                buf.rightAngleBracket();
                buf.append(confInfo.toXml());
                return buf;
            }
        };

        CallJabberImpl call = callPeer.getCall();

        iq.setFrom(call.getProtocolProvider().getOurJID());
        iq.setTo(callPeer.getAddressAsJid());
        iq.setType(IQ.Type.set);

        return iq;
    }

    /**
     * Implementation of method <tt>registrationStateChange</tt> from
     * interface RegistrationStateChangeListener for setting up (or down)
     * our <tt>JingleManager</tt> when an <tt>XMPPConnection</tt> is available
     *
     * @param evt the event received
     */
    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        super.registrationStateChanged(evt);

        RegistrationState registrationState = evt.getNewState();

        if (RegistrationState.REGISTERED.equals(registrationState))
        {
            if(logger.isDebugEnabled())
                logger.debug("Subscribes to Coin packets");
            subscribeForCoinPackets();
        }
        else if (RegistrationState.UNREGISTERED.equals(registrationState))
        {
            if(logger.isDebugEnabled())
                logger.debug("Unsubscribes to Coin packets");
            unsubscribeForCoinPackets();
        }
    }

    /**
     * Creates a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>.
     *
     * @return a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>
     * @throws OperationFailedException if anything goes wrong
     */
    @Override
    protected CallJabberImpl createOutgoingCall()
        throws OperationFailedException
    {
        return new CallJabberImpl(getBasicTelephony());
    }

    /**
     * {@inheritDoc}
     *
     * Implements the protocol-dependent part of the logic of inviting a callee
     * to a <tt>Call</tt>. The protocol-independent part of that logic is
     * implemented by
     * {@link AbstractOperationSetTelephonyConferencing#inviteCalleeToCall(String,Call)}.
     */
    @Override
    protected CallPeer doInviteCalleeToCall(
            String calleeAddress,
            CallJabberImpl call)
        throws OperationFailedException
    {
        return
            getBasicTelephony().createOutgoingCall(
                    call,
                    calleeAddress,
                    Arrays.asList(
                            new ExtensionElement[]
                                    {
                                        new CoinPacketExtension(true)
                                    }));
    }

    /**
     * Parses a <tt>String</tt> value which represents a callee address
     * specified by the user into an object which is to actually represent the
     * callee during the invitation to a conference <tt>Call</tt>.
     *
     * @param calleeAddressString a <tt>String</tt> value which represents a
     * callee address to be parsed into an object which is to actually represent
     * the callee during the invitation to a conference <tt>Call</tt>
     * @return an object which is to actually represent the specified
     * <tt>calleeAddressString</tt> during the invitation to a conference
     * <tt>Call</tt>
     */
    @Override
    protected String parseAddressString(String calleeAddressString)
    {
        return getBasicTelephony()
            .getFullCalleeURI(calleeAddressString)
            .toString();
    }

    /**
     * Subscribes us to notifications about incoming Coin packets.
     */
    private void subscribeForCoinPackets()
    {
        parentProvider.getConnection().addAsyncStanzaListener(this, this);
        parentProvider.getConnection().registerIQRequestHandler(this);
    }

    /**
     * Unsubscribes us from notifications about incoming Coin packets.
     */
    private void unsubscribeForCoinPackets()
    {
        XMPPConnection connection = parentProvider.getConnection();

        if (connection != null)
        {
            connection.removeAsyncStanzaListener(this);
            connection.unregisterIQRequestHandler(this);
        }
    }

    /**
     * Tests whether the specified packet should be handled by this
     * operation set. This method is called by smack prior to packet delivery,
     * and it would only accept <tt>CoinIQ</tt>s.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    @Override
    public boolean accept(Stanza packet)
    {
        return packet instanceof CoinIQ;
    }

    /**
     * Handles incoming jingle packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    @Override
    public void processStanza(Stanza packet)
        throws NotConnectedException, InterruptedException
    {
        CoinIQ coinIQ = (CoinIQ) packet;
        String errorMessage;

        if(coinIQ.getType() != IQ.Type.error)
        {
            return;
        }

        StanzaError error = coinIQ.getError();
        if(error != null)
        {
            errorMessage = error.getConditionText();
        }
        else
        {
            errorMessage = "Unknown error.";
        }

        logger.error("Received error in COIN packet. " + errorMessage);

        String sid = coinIQ.getSID();
        if (sid != null)
        {
            CallPeerJabberImpl callPeer = getBasicTelephony()
                .getActiveCallsRepository()
                .findCallPeer(sid);

            if (callPeer != null)
            {
                callPeer.fireConferenceMemberErrorEvent(errorMessage);
            }
        }
    }

    /**
     * Handles a specific <tt>CoinIQ</tt> sent from a specific
     * <tt>CallPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> from which the specified
     * <tt>CoinIQ</tt> was sent
     * @param coinIQ the <tt>CoinIQ</tt> which was sent from the specified
     * <tt>callPeer</tt>
     */
    private void handleCoin(CallPeerJabberImpl callPeer, CoinIQ coinIQ)
    {
        try
        {
            setConferenceInfoXML(callPeer, coinIQ.getChildElementXML().toString());
        }
        catch (XMLException e)
        {
            logger.error("Could not handle received COIN from " + callPeer
                + ": " + coinIQ);
        }
    }

    /**
     * {@inheritDoc}
     *
     * For COINs (XEP-0298), we use the attributes of the
     * <tt>conference-info</tt> element to piggyback a Jingle SID. This is
     * temporary and should be removed once we choose a better way to pass the
     * SID.
     */
    @Override
    protected ConferenceInfoDocument getCurrentConferenceInfo(
            MediaAwareCallPeer<?,?,?> callPeer)
    {
        ConferenceInfoDocument confInfo
                = super.getCurrentConferenceInfo(callPeer);

        if (callPeer instanceof CallPeerJabberImpl
                && confInfo != null)
        {
            confInfo.setSid(((CallPeerJabberImpl)callPeer).getSID());
        }
        return confInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getLocalEntity(CallPeer callPeer)
    {
        JingleIQ sessionIQ = ((CallPeerJabberImpl)callPeer).getSessionIQ();
        Jid from = sessionIQ.getFrom();
        BareJid chatRoomName = from.asBareJid();
        OperationSetMultiUserChatJabberImpl opSetMUC
            = (OperationSetMultiUserChatJabberImpl)
                parentProvider.getOperationSet(OperationSetMultiUserChat.class);
        ChatRoom room = null;
        if(opSetMUC != null)
            room = opSetMUC.getChatRoom(chatRoomName);

        if(room != null)
            return "xmpp:" + chatRoomName + "/" + room.getUserNickname();

        return "xmpp:" + parentProvider.getOurJID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getLocalDisplayName()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * The URI of the returned <tt>ConferenceDescription</tt> is the occupant
     * JID with which we have joined the room.
     */
    @Override
    public ConferenceDescription setupConference(final ChatRoom chatRoom)
    {
        CallJabberImpl call = new CallJabberImpl(getBasicTelephony());
        call.setAutoAnswer(true);

        String uri = "xmpp:" + chatRoom.getIdentifier() +
                "/" + chatRoom.getUserNickname();

        ConferenceDescription cd
                = new ConferenceDescription(uri, call.getCallID());

        call.addCallChangeListener(new CallChangeListener()
        {
            @Override
            public void callStateChanged(CallChangeEvent ev)
            {
                if(CallState.CALL_ENDED.equals(ev.getNewValue()))
                    chatRoom.publishConference(null, null);
            }

            @Override
            public void callPeerRemoved(CallPeerEvent ev)
            {
            }

            @Override
            public void callPeerAdded(CallPeerEvent ev)
            {
            }
        });

        if (logger.isInfoEnabled())
        {
            logger.info("Setup a conference with uri=" + uri + " and callid=" + call.getCallID());
        }

        return cd;
    }

    @Override
    public IQ handleIQRequest(IQ iqRequest)
    {
        CoinIQ coinIQ = (CoinIQ)iqRequest;
        String sid = coinIQ.getSID();
        if (sid != null)
        {
            CallPeerJabberImpl callPeer
                = getBasicTelephony().getActiveCallsRepository().findCallPeer(
                        sid);

            if (callPeer != null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Processing COIN from " + coinIQ.getFrom()
                            + " (version=" + coinIQ.getVersion() + ")");

                handleCoin(callPeer, coinIQ);
            }
        }

        return IQ.createResultIQ(iqRequest);
    }

    @Override
    public Mode getMode()
    {
        return Mode.async;
    }

    @Override
    public Type getType()
    {
        return Type.set;
    }

    @Override
    public String getElement()
    {
        return CoinIQ.ELEMENT;
    }

    @Override
    public String getNamespace()
    {
        return CoinIQ.NAMESPACE;
    }
}
