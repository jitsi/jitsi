/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.coin.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.packet.*;

/**
 * Implements <tt>OperationSetTelephonyConferencing</tt> for Jabber.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public class OperationSetTelephonyConferencingJabberImpl
    extends AbstractOperationSetTelephonyConferencing<
            ProtocolProviderServiceJabberImpl,
            OperationSetBasicTelephonyJabberImpl,
            CallJabberImpl,
            CallPeerJabberImpl,
            String>
    implements RegistrationStateChangeListener,
               PacketListener,
               PacketFilter

{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetTelephonyConferencingJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetTelephonyConferencingJabberImpl.class);

    /**
     * Synchronization object.
     */
    private final Object lock = new Object();

    /**
     * The value of the <tt>version</tt> attribute to be specified in the
     * outgoing <tt>conference-info</tt> root XML elements.
     */
    private int version = 1;

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
        if (call.isConferenceFocus())
        {
            synchronized (lock)
            {
                // send conference-info to all CallPeers of the specified call.
                for (Iterator<? extends CallPeer> i = call.getCallPeers();
                        i.hasNext();)
                {
                    notify(i.next());
                }

                version++;
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

        // check that callPeer supports COIN before sending him a
        // conference-info
        String to = getBasicTelephony().getFullCalleeURI(callPeer.getAddress());

        try
        {
            DiscoverInfo discoverInfo
                = parentProvider.getDiscoveryManager().discoverInfo(to);

            if (!discoverInfo.containsFeature(
                    ProtocolProviderServiceJabberImpl.URN_XMPP_JINGLE_COIN))
            {
                logger.info(callPeer.getAddress() + " does not support COIN");
                return;
            }
        }
        catch (XMPPException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        IQ iq = getConferenceInfo((CallPeerJabberImpl) callPeer, version);

        if (iq != null)
            parentProvider.getConnection().sendPacket(iq);
    }

    /**
     * Get media packet extension for the specified <tt>CallPeerJabberImpl</tt>.
     *
     * @param callPeer <tt>CallPeer</tt>
     * @param remote if the callPeer is remote or local
     * @return list of media packet extension
     */
    private List<MediaPacketExtension> getMedia(
            MediaAwareCallPeer<?,?,?> callPeer,
            boolean remote)
    {
        CallPeerMediaHandler<?> mediaHandler = callPeer.getMediaHandler();
        List<MediaPacketExtension> ret = new ArrayList<MediaPacketExtension>();
        long i = 1;

        for(MediaType mediaType : MediaType.values())
        {
            MediaStream stream = mediaHandler.getStream(mediaType);

            if (stream != null)
            {
                MediaPacketExtension ext
                    = new MediaPacketExtension(Long.toString(i));
                long srcId
                    = remote
                        ? getRemoteSourceID(callPeer, mediaType)
                        : stream.getLocalSourceID();

                if (srcId != -1)
                    ext.setSrcID(Long.toString(srcId));

                ext.setType(mediaType.toString());

                MediaDirection direction
                    = remote
                        ? getRemoteDirection(callPeer, mediaType)
                        : stream.getDirection();

                if (direction == null)
                    direction = MediaDirection.INACTIVE;

                ext.setStatus(direction.toString());
                ret.add(ext);
                i++;
            }
        }

        return ret;
    }

    /**
     * Get user packet extension for the specified <tt>CallPeerJabberImpl</tt>.
     *
     * @param callPeer <tt>CallPeer</tt>
     * @return user packet extension
     */
    private UserPacketExtension getUser(CallPeer callPeer)
    {
        UserPacketExtension ext
            = new UserPacketExtension(callPeer.getAddress());

        ext.setDisplayText(callPeer.getDisplayName());

        EndpointPacketExtension endpoint
            = new EndpointPacketExtension(callPeer.getURI());

        endpoint.setStatus(getEndpointStatus(callPeer));

        if (callPeer instanceof MediaAwareCallPeer<?,?,?>)
        {
            List<MediaPacketExtension> medias
                = getMedia((MediaAwareCallPeer<?,?,?>) callPeer, true);

            if(medias != null)
            {
                for(MediaPacketExtension media : medias)
                    endpoint.addChildExtension(media);
            }
        }

        ext.addChildExtension(endpoint);

        return ext;
    }

    /**
     * Generates the text content to be put in the <tt>status</tt> XML element
     * of an <tt>endpoint</tt> XML element and which describes the state of a
     * specific <tt>CallPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> which is to get its state described
     * in a <tt>status</tt> XML element of an <tt>endpoint</tt> XML element
     * @return the text content to be put in the <tt>status</tt> XML element of
     * an <tt>endpoint</tt> XML element and which describes the state of the
     * specified <tt>callPeer</tt>
     */
    private EndpointStatusType getEndpointStatus(CallPeer callPeer)
    {
        CallPeerState callPeerState = callPeer.getState();

        if (CallPeerState.ALERTING_REMOTE_SIDE.equals(callPeerState))
            return EndpointStatusType.alerting;
        if (CallPeerState.CONNECTING.equals(callPeerState)
                || CallPeerState
                    .CONNECTING_WITH_EARLY_MEDIA.equals(callPeerState))
            return EndpointStatusType.pending;
        if (CallPeerState.DISCONNECTED.equals(callPeerState))
            return EndpointStatusType.disconnected;
        if (CallPeerState.INCOMING_CALL.equals(callPeerState))
            return EndpointStatusType.dialing_in;
        if (CallPeerState.INITIATING_CALL.equals(callPeerState))
            return EndpointStatusType.dialing_out;

        /*
         * he/she is neither "hearing" the conference mix nor is his/her media
         * being mixed in the conference
         */
        if (CallPeerState.ON_HOLD_LOCALLY.equals(callPeerState)
                || CallPeerState.ON_HOLD_MUTUALLY.equals(callPeerState))
            return EndpointStatusType.on_hold;
        if (CallPeerState.CONNECTED.equals(callPeerState))
            return EndpointStatusType.connected;
        return null;
    }

    /**
     * Generates the conference-info IQ to be sent to a specific
     * <tt>CallPeer</tt> in order to notify it of the current state of the
     * conference managed by the local peer.
     *
     * @param callPeer the <tt>CallPeer</tt> to generate conference-info XML for
     * @param version the value of the version attribute of the
     * <tt>conference-info</tt> root element of the conference-info XML to be
     * generated
     * @return the conference-info IQ to be sent to the specified
     * <tt>callPeer</tt> in order to notify it of the current state of the
     * conference managed by the local peer
     */
    private IQ getConferenceInfo(CallPeerJabberImpl callPeer, int version)
    {
        String callPeerSID = callPeer.getSID();

        if (callPeerSID == null)
            return null;

        CoinIQ iq = new CoinIQ();
        CallJabberImpl call = callPeer.getCall();

        iq.setFrom(call.getProtocolProvider().getOurJID());
        iq.setTo(callPeer.getAddress());
        iq.setType(Type.SET);
        iq.setEntity(getBasicTelephony().getProtocolProvider().getOurJID());
        iq.setVersion(version);
        iq.setState(StateType.full);
        iq.setSID(callPeerSID);

        // conference-description
        iq.addExtension(new DescriptionPacketExtension());

        // conference-state
        StatePacketExtension state = new StatePacketExtension();
        List<CallPeer> conferenceCallPeers = CallConference.getCallPeers(call);

        state.setUserCount(
                1 /* the local peer/user */ + conferenceCallPeers.size());
        iq.addExtension(state);

        // users
        UsersPacketExtension users = new UsersPacketExtension();

        // user
        UserPacketExtension user
            = new UserPacketExtension("xmpp:" + parentProvider.getOurJID());

        // endpoint
        EndpointPacketExtension endpoint = new EndpointPacketExtension(
            "xmpp:" + parentProvider.getOurJID());
        endpoint.setStatus(EndpointStatusType.connected);

        // media
        List<MediaPacketExtension> medias = getMedia(callPeer, false);

        for(MediaPacketExtension media : medias)
            endpoint.addChildExtension(media);
        user.addChildExtension(endpoint);
        users.addChildExtension(user);

        // other users
        for (CallPeer conferenceCallPeer : conferenceCallPeers)
            users.addChildExtension(getUser(conferenceCallPeer));

        iq.addExtension(users);
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
                            new PacketExtension[]
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
     * @throws OperationFailedException if parsing the specified
     * <tt>calleeAddressString</tt> fails
     */
    @Override
    protected String parseAddressString(String calleeAddressString)
        throws OperationFailedException
    {
        return getBasicTelephony().getFullCalleeURI(calleeAddressString);
    }

    /**
     * Subscribes us to notifications about incoming Coin packets.
     */
    private void subscribeForCoinPackets()
    {
        parentProvider.getConnection().addPacketListener(this, this);
    }

    /**
     * Unsubscribes us from notifications about incoming Coin packets.
     */
    private void unsubscribeForCoinPackets()
    {
        XMPPConnection connection = parentProvider.getConnection();

        if (connection != null)
            connection.removePacketListener(this);
    }

    /**
     * Tests whether or not the specified packet should be handled by this
     * operation set. This method is called by smack prior to packet delivery
     * and it would only accept <tt>CoinIQ</tt>s.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet)
    {
        return (packet instanceof CoinIQ);
    }

    /**
     * Handles incoming jingle packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        CoinIQ coinIQ = (CoinIQ) packet;

        //first ack all "set" requests.
        if (coinIQ.getType() == IQ.Type.SET)
        {
            IQ ack = IQ.createResultIQ(coinIQ);

            parentProvider.getConnection().sendPacket(ack);
        }

        String sid = coinIQ.getSID();

        if (sid != null)
        {
            CallPeerJabberImpl callPeer
                = getBasicTelephony().getActiveCallsRepository().findCallPeer(
                        sid);

            if (callPeer != null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Processing COIN from" + coinIQ.getFrom()
                                    + "(version=" + coinIQ.getVersion() + ")");
                handleCoin(callPeer, coinIQ);
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
        setConferenceInfoXML(callPeer, -1, coinIQ.getChildElementXML());
    }
}
