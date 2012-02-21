/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.IQ.*;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.coin.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.Logger;

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
     * The value of the <tt>version</tt> attribute to be specified in the
     * outgoing <tt>conference-info</tt> root XML elements.
     */
    private int version = 1;

    /**
     * Synchronization object.
     */
    private final Object objSync = new Object();

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
     * Notifies this <tt>OperationSetTelephonyConferencing</tt> that its
     * <tt>basicTelephony</tt> property has changed its value from a specific
     * <tt>oldValue</tt> to a specific <tt>newValue</tt>
     *
     * @param oldValue the old value of the <tt>basicTelephony</tt> property
     * @param newValue the new value of the <tt>basicTelephony</tt> property
     */
    @Override
    protected void basicTelephonyChanged(
            OperationSetBasicTelephonyJabberImpl oldValue,
            OperationSetBasicTelephonyJabberImpl newValue)
    {
        if (oldValue != null)
            oldValue.removeCallListener(this);
        if (newValue != null)
            newValue.addCallListener(this);
    }

    /**
     * Notifies all CallPeer associated with and established in a
     * specific call for conference information.
     *
     * @param call the <tt>Call</tt>
     */
    protected void notifyAll(Call call)
    {
        if(call.getCallGroup() == null && !call.isConferenceFocus())
        {
            return;
        }

        synchronized(objSync)
        {
            // send conference-info to all CallPeer of Call
            Iterator<? extends CallPeer> it = call.getCallPeers();

            while(it.hasNext())
            {
                CallPeer callPeer = it.next();
                notify(callPeer);
            }
            version++;
        }
    }

    /**
     * Notifies all CallPeer associated with and established in a
     * specific call has occurred
     *
     * @param callPeer the <tt>CallPeer</tt>
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
                    ProtocolProviderServiceJabberImpl
                        .URN_XMPP_JINGLE_COIN))
            {
                logger.info(callPeer.getAddress() + " does not support COIN");
                return;
            }
        }
        catch (XMPPException xmppe)
        {
            logger.warn("Failed to retrieve DiscoverInfo for " + to, xmppe);
        }

        IQ iq = getConferenceInfo((CallPeerJabberImpl)callPeer, version);

        if(iq != null)
        {
            parentProvider.getConnection().sendPacket(iq);
        }
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
        MediaPacketExtension ext = null;
        CallPeerMediaHandler<?> mediaHandler =
            callPeer.getMediaHandler();
        List<MediaPacketExtension> ret =
            new ArrayList<MediaPacketExtension>();
        long i = 1;

        for(MediaType mediaType : MediaType.values())
        {
            MediaStream stream = mediaHandler.getStream(mediaType);

            if (stream != null)
            {
                long srcId = remote
                            ? stream.getRemoteSourceID()
                                    : stream.getLocalSourceID();

                if (srcId != -1)
                {
                    ext = new MediaPacketExtension(Long.toString(i));
                    ext.setSrcID(Long.toString(srcId));
                    ext.setType(mediaType.toString());
                    MediaDirection direction = stream.getDirection();

                    if (direction == null)
                        direction = MediaDirection.INACTIVE;
                    ext.setStatus(direction.toString());
                    ret.add(ext);
                    i++;
                }
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
    private UserPacketExtension getUser(
            MediaAwareCallPeer<?,?,?> callPeer)
    {
        UserPacketExtension ext = new UserPacketExtension(
                callPeer.getAddress());
        EndpointPacketExtension endpoint = null;
        List<MediaPacketExtension> medias = null;

        ext.setDisplayText(callPeer.getDisplayName());
        EndpointStatusType status = getEndpointStatus(callPeer);

        endpoint = new EndpointPacketExtension(callPeer.getURI());
        endpoint.setStatus(status);

        medias = getMedia(callPeer, true);

        if(medias != null)
        {
            for(MediaPacketExtension media : medias)
            {
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
    private EndpointStatusType getEndpointStatus(
        MediaAwareCallPeer<?,?,?> callPeer)
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
        CoinIQ iq = new CoinIQ();
        CallJabberImpl call = callPeer.getCall();
        List<CallPeer> crossPeers = new ArrayList<CallPeer>();
        Iterator<CallPeer> crossProtocolCallPeerIter =
            call.getCrossProtocolCallPeers();

        while (crossProtocolCallPeerIter.hasNext())
        {
            MediaAwareCallPeer<?,?,?> crossPeer =
                (MediaAwareCallPeer<?,?,?>)crossProtocolCallPeerIter.next();
            Iterator<CallPeerJabberImpl> it = call.getCallPeers();
            boolean found = false;

            while(it.hasNext())
            {
                if(it.next().getAddress().equals(crossPeer.getAddress()))
                {
                    found = true;
                    break;
                }
            }

            if(found)
                continue;

            if(!crossPeers.contains(crossPeer))
                crossPeers.add(crossPeer);
        }

        iq.setFrom(call.getProtocolProvider().getOurJID());
        iq.setTo(callPeer.getAddress());
        iq.setType(Type.SET);
        iq.setEntity(getBasicTelephony().getProtocolProvider().getOurJID());
        iq.setVersion(version);
        iq.setState(StateType.full);

        if(callPeer.getJingleSID() == null)
            return null;
        iq.setSID(callPeer.getJingleSID());

        // conference-description
        iq.addExtension(new DescriptionPacketExtension());

        // conference-state
        StatePacketExtension state = new StatePacketExtension();
        state.setUserCount(call.getCallPeerCount() + 1 +
            crossPeers.size());
        iq.addExtension(state);

        // users
        UsersPacketExtension users = new UsersPacketExtension();

        // user
        UserPacketExtension user = new UserPacketExtension(
               "xmpp:" + parentProvider.getOurJID());

        // endpoint
        EndpointPacketExtension endpoint = new EndpointPacketExtension(
            "xmpp:" + parentProvider.getOurJID());
        endpoint.setStatus(EndpointStatusType.connected);

        // media
        List<MediaPacketExtension> medias = getMedia(callPeer, false);

        for(MediaPacketExtension media : medias)
        {
            endpoint.addChildExtension(media);
        }
        user.addChildExtension(endpoint);
        users.addChildExtension(user);

        // other users
        Iterator<CallPeerJabberImpl> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
        {
            UserPacketExtension ext = getUser(callPeerIter.next());
            users.addChildExtension(ext);
        }

        for(CallPeer cp : crossPeers)
        {
            MediaAwareCallPeer<?,?,?> crossPeer =
                (MediaAwareCallPeer<?,?,?>)cp;
            UserPacketExtension ext = getUser(crossPeer);
            users.addChildExtension(ext);
        }

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
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        super.registrationStateChanged(evt);

        RegistrationState registrationState = evt.getNewState();

        if (registrationState == RegistrationState.REGISTERED)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Subscribes to Coin packets");
            }
            subscribeForCoinPackets();
        }
        else if (registrationState == RegistrationState.UNREGISTERED)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Unsubscribes to Coin packets");
            }
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
    protected CallJabberImpl createOutgoingCall()
        throws OperationFailedException
    {
        return new CallJabberImpl(getBasicTelephony());
    }

    /**
     * Invites a callee with a specific address to be joined in a specific
     * <tt>Call</tt> in the sense of conferencing.
     *
     * @param calleeAddress the address of the callee to be invited to the
     * specified existing <tt>Call</tt>
     * @param call the existing <tt>Call</tt> to invite the callee with the
     * specified address to
     * @param wasConferenceFocus the value of the <tt>conferenceFocus</tt>
     * property of the specified <tt>call</tt> prior to the request to invite
     * the specified <tt>calleeAddress</tt>
     * @return a new <tt>CallPeer</tt> instance which describes the signaling
     * and the media streaming of the newly-invited callee within the specified
     * <tt>Call</tt>
     * @throws OperationFailedException if inviting the specified callee to the
     * specified call fails
     */
    protected CallPeer inviteCalleeToCall(
            String calleeAddress,
            CallJabberImpl call,
            boolean wasConferenceFocus)
        throws OperationFailedException
    {
        if (!wasConferenceFocus && call.isConferenceFocus())
        {
            /*
             * Re-INVITE existing CallPeers to inform them that from now
             * the specified call is a conference call.
             */
            Iterator<CallPeerJabberImpl> callPeerIter = call.getCallPeers();

            while (callPeerIter.hasNext())
            {
                CallPeerJabberImpl callPeer = callPeerIter.next();
                if(callPeer.getState() == CallPeerState.CONNECTED)
                    callPeer.sendCoinSessionInfo(true);
            }
        }

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
     * Unsubscribes us to notifications about incoming Coin packets.
     */
    private void unsubscribeForCoinPackets()
    {
        if(parentProvider.getConnection() != null)
        {
            parentProvider.getConnection().removePacketListener(this);
        }
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
        if(!(packet instanceof CoinIQ))
        {
            return false;
        }

        return true;
    }

    /**
     * Handles incoming jingle packets and passes them to the corresponding
     * method based on their action.
     *
     * @param packet the packet to process.
     */
    public void processPacket(Packet packet)
    {
        CoinIQ coinIQ = (CoinIQ)packet;

        //first ack all "set" requests.
        if(coinIQ.getType() == IQ.Type.SET)
        {
            IQ ack = IQ.createResultIQ(coinIQ);
            parentProvider.getConnection().sendPacket(ack);
        }

        String sid = coinIQ.getSID();

        if(sid == null)
        {
            return;
        }

        CallPeerJabberImpl callPeer
            = getBasicTelephony().getActiveCallsRepository().findCallPeer(sid);

        if(callPeer == null)
        {
            return;
        }

        handleCoin(coinIQ, callPeer);
    }

    /**
     * Handle Coin IQ.
     *
     * @param coinIQ Coin IQ
     * @param callPeer a <tt>CallPeer</tt>
     */
    private void handleCoin(CoinIQ coinIQ, CallPeerJabberImpl callPeer)
    {
        ConferenceMember[] conferenceMembersToRemove
            = callPeer.getConferenceMembers();
        int conferenceMembersToRemoveCount = conferenceMembersToRemove.length;
        UsersPacketExtension users = null;
        Collection<PacketExtension> usersList = coinIQ.getExtensions();
        boolean changed = false;

        for(PacketExtension ext : usersList)
        {
            if(ext.getElementName().equals(UsersPacketExtension.ELEMENT_NAME))
            {
                users = (UsersPacketExtension)ext;
                break;
            }
        }

        if(users == null)
        {
            return;
        }

        Collection<UserPacketExtension> userList =
            users.getChildExtensionsOfType(UserPacketExtension.class);

        if(userList.size() == 0)
        {
            return;
        }

        for(UserPacketExtension u : userList)
        {
            String address = null;

            if(u.getAttribute("entity") != null)
            {
                address = (String)u.getAttribute("entity");
            }

            if ((address == null) || (address.length() < 1))
                continue;

            /*
             * Determine the ConferenceMembers which are no longer in the
             * list.
             */
            ConferenceMemberJabberImpl existingConferenceMember = null;

            for (int conferenceMemberIndex = 0;
                    conferenceMemberIndex < conferenceMembersToRemoveCount;
                    conferenceMemberIndex++)
            {
                ConferenceMemberJabberImpl conferenceMember
                    = (ConferenceMemberJabberImpl)
                        conferenceMembersToRemove[conferenceMemberIndex];

                if ((conferenceMember != null)
                        && address
                                .equalsIgnoreCase(
                                    conferenceMember.getAddress()))
                {
                    conferenceMembersToRemove[conferenceMemberIndex] = null;
                    existingConferenceMember = conferenceMember;
                    break;
                }
            }

            // Create the new ones.
            boolean addConferenceMember = false;
            if (existingConferenceMember == null)
            {
                existingConferenceMember
                    = new ConferenceMemberJabberImpl(callPeer, address);
                addConferenceMember = true;
            }
            else
            {
                addConferenceMember = false;
            }

            // Update the existing ones.
            if (existingConferenceMember != null)
            {
                String displayName = u.getDisplayText();
                List<EndpointPacketExtension> endpoints =
                    u.getChildExtensionsOfType(EndpointPacketExtension.class);
                String endpointStatus = null;
                String ssrc = null;

                if(endpoints.size() > 0)
                {
                    EndpointPacketExtension endpoint = endpoints.get(0);

                    if(endpoint.getStatus() == null)
                    {
                        break;
                    }

                    endpointStatus = endpoint.getStatus().toString();

                    List<MediaPacketExtension> medias =
                        endpoint.getChildExtensionsOfType(
                                MediaPacketExtension.class);

                    for(MediaPacketExtension media : medias)
                    {
                        if(media.getType().equalsIgnoreCase(
                                MediaType.AUDIO.toString()))
                        {
                            ssrc = media.getSrcID();
                        }
                    }
                }

                existingConferenceMember.setDisplayName(displayName);
                existingConferenceMember.setEndpointStatus(endpointStatus);

                if (ssrc != null)
                {
                    long newSsrc = Long.parseLong(ssrc);
                    if(existingConferenceMember.getSSRC() != newSsrc)
                        changed = true;

                    existingConferenceMember.setSSRC(newSsrc);
                }

                if (addConferenceMember)
                {
                    callPeer.addConferenceMember(existingConferenceMember);
                }
            }
        }

        /*
         * Remove the ConferenceMember instance which are no longer present in
         * the conference-info XML document.
         */
        for (int conferenceMemberIndex = 0;
                conferenceMemberIndex < conferenceMembersToRemoveCount;
                conferenceMemberIndex++)
        {
            ConferenceMember conferenceMemberToRemove
                = conferenceMembersToRemove[conferenceMemberIndex];

            if (conferenceMemberToRemove != null)
                callPeer.removeConferenceMember(conferenceMemberToRemove);
        }

        if(changed)
            notifyAll(callPeer.getCall());
    }
}
