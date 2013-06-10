/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import java.beans.*;
import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetTelephonyConferencing</tt> in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @param <ProtocolProviderServiceT>
 * @param <OperationSetBasicTelephonyT>
 * @param <MediaAwareCallT>
 * @param <MediaAwareCallPeerT>
 * @param <CalleeAddressT>
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractOperationSetTelephonyConferencing<
        ProtocolProviderServiceT extends ProtocolProviderService,
        OperationSetBasicTelephonyT
                extends OperationSetBasicTelephony<ProtocolProviderServiceT>,
        MediaAwareCallT
                extends MediaAwareCall<
                        MediaAwareCallPeerT,
                        OperationSetBasicTelephonyT,
                        ProtocolProviderServiceT>,
        MediaAwareCallPeerT
                extends MediaAwareCallPeer<
                        MediaAwareCallT,
                        ?,
                        ProtocolProviderServiceT>,
        CalleeAddressT extends Object>
    implements OperationSetTelephonyConferencing,
               RegistrationStateChangeListener,
               PropertyChangeListener,
               CallListener,
               CallChangeListener
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetTelephonyConferencing</tt> class and its
     * instances.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetTelephonyConferencing.class);

    /**
     * The name of the conference-info XML element <tt>display-text</tt>.
     */
    protected static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * The name of the conference-info XML element <tt>endpoint</tt>.
     */
    protected static final String ELEMENT_ENDPOINT = "endpoint";

    /**
     * The name of the conference-info XML element <tt>media</tt>.
     */
    protected static final String ELEMENT_MEDIA = "media";

    /**
     * The name of the conference-info XML element <tt>src-id</tt>.
     */
    protected static final String ELEMENT_SRC_ID = "src-id";

    /**
     * The name of the conference-info XML element <tt>status</tt>.
     */
    protected static final String ELEMENT_STATUS = "status";

    /**
     * The name of the conference-info XML element <tt>type</tt>.
     */
    protected static final String ELEMENT_TYPE = "type";

    /**
     * The name of the conference-info XML element <tt>user</tt>.
     */
    protected static final String ELEMENT_USER = "user";

    /**
     * The name of the conference-info XML element <tt>users</tt>.
     */
    protected static final String ELEMENT_USERS = "users";

    /**
     * The <tt>OperationSetBasicTelephony</tt> implementation which this
     * instance uses to carry out tasks such as establishing <tt>Call</tt>s.
     */
    private OperationSetBasicTelephonyT basicTelephony;

    /**
     * The <tt>CallPeerListener</tt> which listens to modifications in the
     * properties/state of <tt>CallPeer</tt> so that NOTIFY requests can be sent
     * from a conference focus to its conference members to update them with
     * the latest information about the <tt>CallPeer</tt>.
     */
    private final CallPeerListener callPeerListener = new CallPeerAdapter()
    {
        /**
         * Indicates that a change has occurred in the status of the source
         * <tt>CallPeer</tt>.
         *
         * @param evt the <tt>CallPeerChangeEvent</tt> instance containing the
         * source event as well as its previous and its new status
         */
        @Override
        public void peerStateChanged(CallPeerChangeEvent evt)
        {
            CallPeer peer = evt.getSourceCallPeer();

            if (peer != null)
            {
                Call call = peer.getCall();

                if (call != null)
                {
                    CallPeerState state = peer.getState();

                    if ((state != null)
                            && !state.equals(CallPeerState.DISCONNECTED)
                            && !state.equals(CallPeerState.FAILED))
                    {
                        AbstractOperationSetTelephonyConferencing.this
                                .notifyAll(call);
                    }
                }
            }
        }
    };

    /**
     * The <tt>ProtocolProviderService</tt> implementation which created this
     * instance and for which telephony conferencing services are being provided
     * by this instance.
     */
    protected final ProtocolProviderServiceT parentProvider;

    /**
     * Initializes a new <tt>AbstractOperationSetTelephonyConferencing</tt>
     * instance which is to provide telephony conferencing services for the
     * specified <tt>ProtocolProviderService</tt> implementation.
     *
     * @param parentProvider the <tt>ProtocolProviderService</tt> implementation
     * which has requested the creation of the new instance and for which the
     * new instance is to provide telephony conferencing services
     */
    protected AbstractOperationSetTelephonyConferencing(
            ProtocolProviderServiceT parentProvider)
    {
        this.parentProvider = parentProvider;
        this.parentProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Notifies this <tt>OperationSetTelephonyConferencing</tt> that its
     * <tt>basicTelephony</tt> property has changed its value from a specific
     * <tt>oldValue</tt> to a specific <tt>newValue</tt>
     *
     * @param oldValue the old value of the <tt>basicTelephony</tt> property
     * @param newValue the new value of the <tt>basicTelephony</tt> property
     */
    protected void basicTelephonyChanged(
            OperationSetBasicTelephonyT oldValue,
            OperationSetBasicTelephonyT newValue)
    {
        if (oldValue != null)
            oldValue.removeCallListener(this);
        if (newValue != null)
            newValue.addCallListener(this);
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific <tt>Call</tt> has
     * been established.
     *
     * @param event a <tt>CallEvent</tt> which specified the newly-established
     * <tt>Call</tt>
     */
    protected void callBegun(CallEvent event)
    {
        Call call = event.getSourceCall();

        call.addCallChangeListener(this);

        /*
         * If there were any CallPeers in the Call prior to our realization that
         * it has begun, pretend that they are added afterwards.
         */
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
            callPeerAdded(
                new CallPeerEvent(
                        callPeerIter.next(),
                        call,
                        CallPeerEvent.CALL_PEER_ADDED));
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific <tt>Call</tt> has
     * ended.
     *
     * @param event a <tt>CallEvent</tt> which specified the <tt>Call</tt> which
     * has just ended
     */
    public void callEnded(CallEvent event)
    {
        Call call = event.getSourceCall();

        /*
         * If there are still CallPeers after our realization that it has ended,
         * pretend that they are removed before that.
         */
        Iterator<? extends CallPeer> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
            callPeerRemoved(
                new CallPeerEvent(
                        callPeerIter.next(),
                        call,
                        CallPeerEvent.CALL_PEER_REMOVED));

        call.removeCallChangeListener(this);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific
     * <tt>CallPeer</tt> has been added to a specific <tt>Call</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been added to a <tt>Call</tt>
     */
    public void callPeerAdded(CallPeerEvent event)
    {
        MediaAwareCallPeer<?,?,?> callPeer =
            (MediaAwareCallPeer<?,?,?>)event.getSourceCallPeer();

        callPeer.addCallPeerListener(callPeerListener);
        callPeer.getMediaHandler().addPropertyChangeListener(this);
        callPeersChanged(event);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific
     * <tt>CallPeer</tt> has been remove from a specific <tt>Call</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been removed from a <tt>Call</tt>
     */
    public void callPeerRemoved(CallPeerEvent event)
    {
        @SuppressWarnings("unchecked")
        MediaAwareCallPeerT callPeer =
            (MediaAwareCallPeerT) event.getSourceCallPeer();

        callPeer.removeCallPeerListener(callPeerListener);
        callPeer.getMediaHandler().removePropertyChangeListener(this);
        callPeersChanged(event);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that the <tt>CallPeer</tt> list
     * of a specific <tt>Call</tt> has been modified by adding or removing a
     * specific <tt>CallPeer</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been added to or removed from a <tt>Call</tt>
     */
    private void callPeersChanged(CallPeerEvent event)
    {
        notifyAll(event.getSourceCall());
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific <tt>Call</tt>
     * has changed its state. Does nothing.
     *
     * @param event a <tt>CallChangeEvent</tt> which specifies the <tt>Call</tt>
     * which has changed its state, the very state which has been changed and
     * the values of the state before and after the change
     */
    public void callStateChanged(CallChangeEvent event)
    {
    }

    /**
     * Creates a conference call with the specified callees as call peers.
     *
     * @param callees the list of addresses that we should call
     * @return the newly created conference call containing all CallPeers
     * @throws OperationFailedException if establishing the conference call
     * fails
     * @see OperationSetTelephonyConferencing#createConfCall(String[])
     */
    public Call createConfCall(String[] callees)
        throws OperationFailedException
    {
        return createConfCall(callees, null);
    }

    /**
     * Creates a conference <tt>Call</tt> with the specified callees as
     * <tt>CallPeers</tt>.
     *
     * @param callees the list of addresses that we should call
     * @param conference the <tt>CallConference</tt> which represents the state
     * of the telephony conference into which the specified callees are to be
     * invited
     * @return the newly-created conference call containing all
     * <tt>CallPeer</tt>s
     * @throws OperationFailedException if establishing the conference
     * <tt>Call</tt> fails
     */
    public Call createConfCall(String[] callees, CallConference conference)
        throws OperationFailedException
    {
        List<CalleeAddressT> calleeAddresses
            = new ArrayList<CalleeAddressT>(callees.length);

        for (String callee : callees)
            calleeAddresses.add(parseAddressString(callee));

        MediaAwareCallT call = createOutgoingCall();

        if (conference == null)
            conference = call.getConference();
        else
            call.setConference(conference);
        conference.setConferenceFocus(true);

        for (CalleeAddressT calleeAddress : calleeAddresses)
            doInviteCalleeToCall(calleeAddress, call);

        return call;
    }

    /**
     * Creates a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>.
     *
     * @return a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>
     * @throws OperationFailedException if anything goes wrong
     */
    protected abstract MediaAwareCallT createOutgoingCall()
        throws OperationFailedException;

    /**
     * Invites a callee with a specific address to join a specific <tt>Call</tt>
     * for the purposes of telephony conferencing.
     *
     * @param calleeAddress the address of the callee to be invited to the
     * specified existing <tt>Call</tt>
     * @param call the existing <tt>Call</tt> to invite the callee with the
     * specified address to
     * @return a new <tt>CallPeer</tt> instance which describes the signaling
     * and the media streaming of the newly-invited callee within the specified
     * <tt>Call</tt>
     * @throws OperationFailedException if inviting the specified callee to the
     * specified <tt>Call</tt> fails
     */
    protected abstract CallPeer doInviteCalleeToCall(
            CalleeAddressT calleeAddress,
            MediaAwareCallT call)
        throws OperationFailedException;

    /**
     * Gets the <tt>OperationSetBasicTelephony</tt> implementation which this
     * instance uses to carry out tasks such as establishing <tt>Call</tt>s.
     *
     * @return the <tt>OperationSetBasicTelephony</tt> implementation which this
     * instance uses to carry out tasks such as establishing <tt>Call</tt>s
     */
    public OperationSetBasicTelephonyT getBasicTelephony()
    {
        return basicTelephony;
    }

    private void getEndpointMediaProperties(
            Node endpoint,
            Map<String, Object> properties)
    {
        NodeList endpointChildList = endpoint.getChildNodes();
        int endpoingChildCount = endpointChildList.getLength();

        for (int endpointChildIndex = 0;
                endpointChildIndex < endpoingChildCount;
                endpointChildIndex++)
        {
            Node endpointChild = endpointChildList.item(endpointChildIndex);

            if (ELEMENT_MEDIA.equals(endpointChild.getNodeName()))
            {
                NodeList mediaChildList = endpointChild.getChildNodes();
                int mediaChildCount = mediaChildList.getLength();
                String srcId = null;
                String status = null;
                String type = null;

                for (int mediaChildIndex = 0;
                        mediaChildIndex < mediaChildCount;
                        mediaChildIndex++)
                {
                    Node mediaChild = mediaChildList.item(mediaChildIndex);
                    String mediaChildName = mediaChild.getNodeName();

                    if (ELEMENT_SRC_ID.equals(mediaChildName))
                        srcId = mediaChild.getTextContent();
                    else if (ELEMENT_STATUS.equals(mediaChildName))
                        status = mediaChild.getTextContent();
                    else if (ELEMENT_TYPE.equals(mediaChildName))
                        type = mediaChild.getTextContent();
                }

                if (MediaType.AUDIO.toString().equalsIgnoreCase(type))
                {
                    properties.put(
                            ConferenceMember.AUDIO_SSRC_PROPERTY_NAME,
                            srcId);
                    properties.put(
                            ConferenceMember.AUDIO_STATUS_PROPERTY_NAME,
                            status);
                }
                else if (MediaType.VIDEO.toString().equalsIgnoreCase(type))
                {
                    properties.put(
                            ConferenceMember.VIDEO_SSRC_PROPERTY_NAME,
                            srcId);
                    properties.put(
                            ConferenceMember.VIDEO_STATUS_PROPERTY_NAME,
                            status);
                }
            }
        }
    }

    /**
     * Reads the text content of the <tt>status</tt> XML element of a specific
     * <tt>endpoint</tt> XML element.
     *
     * @param endpoint an XML <tt>Node</tt> which represents the
     * <tt>endpoint</tt> XML element from which to get the text content of its
     * <tt>status</tt> XML element
     * @return the text content of the <tt>status</tt> XML element of the
     * specified <tt>endpoint</tt> XML element if any; otherwise, <tt>null</tt>
     */
    private String getEndpointStatus(Node endpoint)
    {
        NodeList childNodes = endpoint.getChildNodes();
        int childCount = childNodes.getLength();

        for (int i = 0; i < childCount; i++)
        {
            Node child = childNodes.item(i);

            if (ELEMENT_STATUS.equals(child.getNodeName()))
                return child.getTextContent();
        }
        return null;
    }

    /**
     * Gets the <tt>MediaDirection</tt> of the media RTP stream of a specific
     * <tt>CallPeer</tt> with a specific <tt>MediaType</tt> from the point of
     * view of the remote peer.
     *
     * @param callPeer
     * @param mediaType
     * @return the <tt>MediaDirection</tt> of the media RTP stream of a specific
     * <tt>CallPeer</tt> with a specific <tt>MediaType</tt> from the point of
     * view of the remote peer
     */
    protected MediaDirection getRemoteDirection(
            MediaAwareCallPeer<?,?,?> callPeer,
            MediaType mediaType)
    {
        MediaStream stream = callPeer.getMediaHandler().getStream(mediaType);
        MediaDirection remoteDirection;

        if (stream != null)
        {
            remoteDirection = stream.getDirection();
            if (remoteDirection != null)
                remoteDirection = remoteDirection.getReverseDirection();
        }
        else
            remoteDirection = null;
        return remoteDirection;
    }

    /**
     * Gets the remote SSRC to be reported in the conference-info XML for a
     * specific <tt>CallPeer</tt>'s media of a specific <tt>MediaType</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> whose remote SSRC for the media of
     * the specified <tt>mediaType</tt> is to be returned
     * @param mediaType the <tt>MediaType</tt> of the specified
     * <tt>callPeer</tt>'s media whose remote SSRC is to be returned
     * @return the remote SSRC to be reported in the conference-info XML for the
     * specified <tt>callPeer</tt>'s media of the specified <tt>mediaType</tt>
     */
    protected long getRemoteSourceID(
            MediaAwareCallPeer<?,?,?> callPeer,
            MediaType mediaType)
    {
        long remoteSourceID
            = callPeer.getMediaHandler().getRemoteSSRC(mediaType);

        if (remoteSourceID != -1)
        {
            /*
             * TODO Technically, we are detecting conflicts within a Call
             * while we should be detecting them within the whole
             * CallConference.
             */
            MediaAwareCall<?,?,?> call = callPeer.getCall();

            if (call != null)
            {
                for (MediaAwareCallPeer<?,?,?> aCallPeer
                        : call.getCallPeerList())
                {
                    if (aCallPeer != callPeer)
                    {
                        long aRemoteSourceID
                            = aCallPeer.getMediaHandler().getRemoteSSRC(
                                    mediaType);

                        if (aRemoteSourceID == remoteSourceID)
                        {
                            remoteSourceID = -1;
                            break;
                        }
                    }
                }
            }
        }
        return remoteSourceID;
    }

    /**
     * Notifies this <tt>CallListener</tt> that a specific incoming
     * <tt>Call</tt> has been received.
     *
     * @param event a <tt>CallEvent</tt> which specifies the newly-received
     * incoming <tt>Call</tt>
     */
    public void incomingCallReceived(CallEvent event)
    {
        callBegun(event);
    }

    /**
     * Invites the callee represented by the specified uri to an already
     * existing call. The difference between this method and createConfCall is
     * that inviteCalleeToCall allows a user to transform an existing 1 to 1
     * call into a conference call, or add new peers to an already established
     * conference.
     *
     * @param uri the callee to invite to an existing conf call.
     * @param call the call that we should invite the callee to.
     * @return the CallPeer object corresponding to the callee represented by
     * the specified uri.
     * @throws OperationFailedException if inviting the specified callee to the
     * specified call fails
     */
    public CallPeer inviteCalleeToCall(String uri, Call call)
        throws OperationFailedException
    {
        CalleeAddressT calleeAddress = parseAddressString(uri);
        @SuppressWarnings("unchecked")
        MediaAwareCallT mediaAwareCallT = (MediaAwareCallT) call;

        mediaAwareCallT.getConference().setConferenceFocus(true);
        return doInviteCalleeToCall(calleeAddress, mediaAwareCallT);
    }

    /**
     * Notifies all <tt>CallPeer</tt>s associated with the telephony conference
     * in which a specific <tt>Call</tt> is participating about changes in the
     * telephony conference-related information.
     *
     * @param call the <tt>Call</tt> which specifies the telephony conference
     * the associated <tt>CallPeer</tt>s of which are to be notified about
     * changes in the telephony conference-related information
     */
    @SuppressWarnings("rawtypes")
    protected void notifyAll(Call call)
    {
        CallConference conference = call.getConference();

        if (conference == null)
            notifyCallPeers(call);
        else
        {
            /*
             * Make each Call notify its CallPeers through its
             * OperationSetTelephonyConferencing (i.e. its protocol).
             */
            for (Call conferenceCall : conference.getCalls())
            {
                OperationSetTelephonyConferencing opSet
                    = conferenceCall.getProtocolProvider().getOperationSet(
                            OperationSetTelephonyConferencing.class);

                if (opSet instanceof AbstractOperationSetTelephonyConferencing)
                {
                    ((AbstractOperationSetTelephonyConferencing) opSet)
                        .notifyCallPeers(conferenceCall);
                }
            }
        }
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
    protected abstract void notifyCallPeers(Call call);

    /**
     * Notifies this <tt>CallListener</tt> that a specific outgoing
     * <tt>Call</tt> has been created.
     *
     * @param event a <tt>CallEvent</tt> which specifies the newly-created
     * outgoing <tt>Call</tt>
     */
    public void outgoingCallCreated(CallEvent event)
    {
        callBegun(event);
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
    protected abstract CalleeAddressT parseAddressString(
            String calleeAddressString)
        throws OperationFailedException;

    /**
     * Notifies this <tt>PropertyChangeListener</tt> that the value of a
     * specific property of the notifier it is registered with has changed.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which describes the source of
     * the event, the name of the property which has changed its value and the
     * old and new values of the property
     * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        String propertyName = ev.getPropertyName();

        if (CallPeerMediaHandler.AUDIO_LOCAL_SSRC.equals(propertyName)
                || CallPeerMediaHandler.AUDIO_REMOTE_SSRC.equals(propertyName)
                || CallPeerMediaHandler.VIDEO_LOCAL_SSRC.equals(propertyName)
                || CallPeerMediaHandler.VIDEO_REMOTE_SSRC.equals(propertyName))
        {
            @SuppressWarnings("unchecked")
            CallPeerMediaHandler<MediaAwareCallPeerT> mediaHandler
                = (CallPeerMediaHandler<MediaAwareCallPeerT>) ev.getSource();
            Call call = mediaHandler.getPeer().getCall();

            if (call != null)
                notifyAll(call);
        }
    }

    /**
     * Notifies this <tt>RegistrationStateChangeListener</tt> that the
     * <tt>ProtocolProviderSerivce</tt> it is registered with has changed its
     * registration state.
     *
     * @param event a <tt>RegistrationStateChangeEvent</tt> which specifies the
     * old and the new value of the registration state of the
     * <tt>ProtocolProviderService</tt> this
     * <tt>RegistrationStateChangeListener</tt> listens to
     */
    public void registrationStateChanged(RegistrationStateChangeEvent event)
    {
        RegistrationState newState = event.getNewState();

        if (RegistrationState.REGISTERED.equals(newState))
        {
            @SuppressWarnings("unchecked")
            OperationSetBasicTelephonyT basicTelephony
                = (OperationSetBasicTelephonyT)
                    parentProvider.getOperationSet(
                            OperationSetBasicTelephony.class);

            if (this.basicTelephony != basicTelephony)
            {
                OperationSetBasicTelephonyT oldValue = this.basicTelephony;

                this.basicTelephony = basicTelephony;
                basicTelephonyChanged(oldValue, this.basicTelephony);
            }
        }
        else if (RegistrationState.UNREGISTERED.equals(newState))
        {
            if (basicTelephony != null)
            {
                OperationSetBasicTelephonyT oldValue = basicTelephony;

                basicTelephony = null;
                basicTelephonyChanged(oldValue, null);
            }
        }
    }

    /**
     * Updates the conference-related properties of a specific <tt>CallPeer</tt>
     * such as <tt>conferenceFocus</tt> and <tt>conferenceMembers</tt> with
     * information received from it as a conference focus in the form of a
     * conference-info XML document.
     *
     * @param callPeer the <tt>CallPeer</tt> which is a conference focus and has
     * sent the specified conference-info XML document
     * @param conferenceInfoDocument the conference-info XML document sent by
     * <tt>callPeer</tt> in order to update the conference-related information
     * of the local peer represented by the associated <tt>Call</tt>
     */
    private void setConferenceInfoDocument(
            MediaAwareCallPeerT callPeer,
            Document conferenceInfoDocument)
    {
        NodeList usersList
            = conferenceInfoDocument.getElementsByTagName(ELEMENT_USERS);
        ConferenceMember[] toRemove
            = callPeer.getConferenceMembers().toArray(
                    AbstractCallPeer.NO_CONFERENCE_MEMBERS);
        int toRemoveCount = toRemove.length;
        boolean changed = false;

        if (usersList.getLength() > 0)
        {
            NodeList userList = usersList.item(0).getChildNodes();
            int userCount = userList.getLength();
            Map<String, Object> conferenceMemberProperties
                = new HashMap<String, Object>();

            for (int userIndex = 0; userIndex < userCount; userIndex++)
            {
                Node user = userList.item(userIndex);

                if (!ELEMENT_USER.equals(user.getNodeName()))
                    continue;

                String address
                    = stripParametersFromAddress(
                            ((Element) user).getAttribute("entity"));

                if ((address == null) || (address.length() < 1))
                    continue;

                /*
                 * Determine the ConferenceMembers who are no longer in the list
                 * i.e. are to be removed.
                 */
                AbstractConferenceMember conferenceMember = null;

                for (int i = 0; i < toRemoveCount; i++)
                {
                    ConferenceMember aConferenceMember
                        = toRemove[i];

                    if ((aConferenceMember != null)
                            && address.equalsIgnoreCase(
                                    aConferenceMember.getAddress()))
                    {
                        toRemove[i] = null;
                        conferenceMember
                            = (AbstractConferenceMember) aConferenceMember;
                        break;
                    }
                }

                // Create the new ones.
                boolean addConferenceMember;

                if (conferenceMember == null)
                {
                    conferenceMember
                        = new AbstractConferenceMember(callPeer, address);
                    addConferenceMember = true;
                }
                else
                    addConferenceMember = false;

                // Update the existing ones.
                if (conferenceMember != null)
                {
                    NodeList userChildList = user.getChildNodes();
                    int userChildCount = userChildList.getLength();
                    String displayName = null;
                    String endpointStatus = null;

                    conferenceMemberProperties.put(
                            ConferenceMember.AUDIO_SSRC_PROPERTY_NAME,
                            null);
                    conferenceMemberProperties.put(
                            ConferenceMember.AUDIO_STATUS_PROPERTY_NAME,
                            null);
                    conferenceMemberProperties.put(
                            ConferenceMember.VIDEO_SSRC_PROPERTY_NAME,
                            null);
                    conferenceMemberProperties.put(
                            ConferenceMember.VIDEO_STATUS_PROPERTY_NAME,
                            null);
                    for (int userChildIndex = 0;
                            userChildIndex < userChildCount;
                            userChildIndex++)
                    {
                        Node userChild = userChildList.item(userChildIndex);
                        String userChildName = userChild.getNodeName();

                        if (ELEMENT_DISPLAY_TEXT.equals(userChildName))
                            displayName = userChild.getTextContent();
                        else if (ELEMENT_ENDPOINT.equals(userChildName))
                        {
                            endpointStatus = getEndpointStatus(userChild);
                            getEndpointMediaProperties(
                                    userChild,
                                    conferenceMemberProperties);
                        }
                    }
                    conferenceMember.setDisplayName(displayName);
                    conferenceMember.setEndpointStatus(endpointStatus);

                    changed
                        = conferenceMember.setProperties(
                                conferenceMemberProperties);

                    if (addConferenceMember)
                        callPeer.addConferenceMember(conferenceMember);
                }
            }
        }

        /*
         * Remove the ConferenceMember instances which are no longer present in
         * the conference-info XML document.
         */
        for (int i = 0; i < toRemoveCount; i++)
        {
            ConferenceMember conferenceMemberToRemove = toRemove[i];

            if (conferenceMemberToRemove != null)
                callPeer.removeConferenceMember(conferenceMemberToRemove);
        }

        if (changed)
            notifyAll(callPeer.getCall());
    }

    /**
     * Updates the conference-related properties of a specific <tt>CallPeer</tt>
     * such as <tt>conferenceFocus</tt> and <tt>conferenceMembers</tt> with
     * information received from it as a conference focus in the form of a
     * conference-info XML document.
     *
     * @param callPeer the <tt>CallPeer</tt> which is a conference focus and has
     * sent the specified conference-info XML document
     * @param version the value of the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> XML element currently represented in the
     * specified <tt>callPeer</tt>
     * @param conferenceInfoXML the conference-info XML document sent by
     * <tt>callPeer</tt> in order to update the conference-related information
     * of the local peer represented by the associated <tt>Call</tt>
     * @return the value of the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> XML element of the specified
     * <tt>conferenceInfoXML</tt> if it was successfully parsed and represented
     * in the specified <tt>callPeer</tt>
     */
    protected int setConferenceInfoXML(
            MediaAwareCallPeerT callPeer,
            int version,
            String conferenceInfoXML)
    {
        byte[] bytes;

        try
        {
            bytes = conferenceInfoXML.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            logger
                .warn(
                    "Failed to gets bytes from String for the UTF-8 charset",
                    uee);
            bytes = conferenceInfoXML.getBytes();
        }

        Document doc = null;
        Throwable exception = null;

        try
        {
            doc
                = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(bytes));
        }
        catch (IOException ioe)
        {
            exception = ioe;
        }
        catch (ParserConfigurationException pce)
        {
            exception = pce;
        }
        catch (SAXException saxe)
        {
            exception = saxe;
        }
        if (exception != null)
            logger.error("Failed to parse conference-info XML", exception);
        else
        {
            /*
             * The CallPeer sent conference-info XML so we're sure it's a
             * conference focus.
             */
            callPeer.setConferenceFocus(true);

            int documentVersion
                = Integer.parseInt(
                        doc.getDocumentElement().getAttribute("version"));

            if ((version == -1) || (documentVersion >= version))
            {
                setConferenceInfoDocument(callPeer, doc);
                return documentVersion;
            }
        }
        return -1;
    }

    /**
     * Removes the parameters (specified after a semicolon) from a specific
     * address <tt>String</tt> if any are present in it.
     *
     * @param address the <tt>String</tt> value representing an address from
     * which any parameters are to be removed
     * @return a <tt>String</tt> representing the specified <tt>address</tt>
     * without any parameters
     */
    protected static String stripParametersFromAddress(String address)
    {
        if (address != null)
        {
            int parametersBeginIndex = address.indexOf(';');

            if (parametersBeginIndex > -1)
                address = address.substring(0, parametersBeginIndex);
        }
        return address;
    }
}
