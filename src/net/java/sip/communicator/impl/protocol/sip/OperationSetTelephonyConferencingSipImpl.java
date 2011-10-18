/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sip.message.Message; // disambiguation
import javax.xml.parsers.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.MediaType; // disambiguation
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Implements <tt>OperationSetTelephonyConferencing</tt> for SIP.
 *
 * @author Lyubomir Marinov
 */
public class OperationSetTelephonyConferencingSipImpl
    extends AbstractOperationSetTelephonyConferencing<
            ProtocolProviderServiceSipImpl,
            OperationSetBasicTelephonySipImpl,
            CallSipImpl,
            CallPeerSipImpl,
            Address>
    implements MethodProcessorListener
{

    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetTelephonyConferencingSipImpl</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetTelephonyConferencingSipImpl.class);

    /**
     * The content sub-type of the content supported in NOTIFY requests handled
     * by <tt>OperationSetTelephonyConferencingSipImpl</tt>.
     */
    private static final String CONTENT_SUB_TYPE = "conference-info+xml";

    /**
     * The name of the conference-info XML element
     * <tt>conference-description</tt>.
     */
    private static final String ELEMENT_CONFERENCE_DESCRIPTION
        = "conference-description";

    /**
     * The name of the conference-info XML element <tt>conference-info</tt>.
     */
    private static final String ELEMENT_CONFERENCE_INFO = "conference-info";

    /**
     * The name of the conference-info XML element <tt>conference-state</tt>.
     */
    private static final String ELEMENT_CONFERENCE_STATE = "conference-state";

    /**
     * The name of the conference-info XML element <tt>display-text</tt>.
     */
    private static final String ELEMENT_DISPLAY_TEXT = "display-text";

    /**
     * The name of the conference-info XML element <tt>endpoint</tt>.
     */
    private static final String ELEMENT_ENDPOINT = "endpoint";

    /**
     * The name of the conference-info XML element <tt>media</tt>.
     */
    private static final String ELEMENT_MEDIA = "media";

    /**
     * The name of the conference-info XML element <tt>src-id</tt>.
     */
    private static final String ELEMENT_SRC_ID = "src-id";

    /**
     * The name of the conference-info XML element <tt>status</tt>.
     */
    private static final String ELEMENT_STATUS = "status";

    /**
     * The name of the conference-info XML element <tt>type</tt>.
     */
    private static final String ELEMENT_TYPE = "type";

    /**
     * The name of the conference-info XML element <tt>user</tt>.
     */
    private static final String ELEMENT_USER = "user";

    /**
     * The name of the conference-info XML element <tt>user-count</tt>.
     */
    private static final String ELEMENT_USER_COUNT = "user-count";

    /**
     * The name of the conference-info XML element <tt>users</tt>.
     */
    private static final String ELEMENT_USERS = "users";

    /**
     * The name of the event package supported by
     * <tt>OperationSetTelephonyConferencingSipImpl</tt> in SUBSCRIBE and NOTIFY
     * requests.
     */
    private static final String EVENT_PACKAGE = "conference";

    /**
     * The time in seconds before the expiration of a <tt>Subscription</tt> at
     * which the <tt>OperationSetTelephonyConferencingSipImpl</tt> instance
     * managing it should refresh it.
     */
    private static final int REFRESH_MARGIN = 60;

    /**
     * The time in seconds after which a <tt>Subscription</tt> should be expired
     * by the <tt>OperationSetTelephonyConferencingSipImpl</tt> instance which
     * manages it.
     */
    private static final int SUBSCRIPTION_DURATION = 3600;

    /**
     * The utility which encodes text so that it's acceptable as the text of an
     * XML element or attribute.
     */
    private DOMElementWriter domElementWriter = new DOMElementWriter();

    /**
     * The <tt>EventPackageNotifier</tt> which implements conference
     * event-package notifier support on behalf of this
     * <tt>OperationSetTelephonyConferencing</tt> instance.
     */
    private final EventPackageNotifier notifier;

    /**
     * The <tt>EventPackageNotifier</tt> which implements conference
     * event-package subscriber support on behalf of this
     * <tt>OperationSetTelephonyConferencing</tt> instance.
     */
    private final EventPackageSubscriber subscriber;

    /**
     * The <tt>Timer</tt> which executes delayed tasks scheduled by
     * {@link #notifier} and {@link #subscriber}.
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * Initializes a new <tt>OperationSetTelephonyConferencingSipImpl</tt>
     * instance which is to provide telephony conferencing services for the
     * specified SIP <tt>ProtocolProviderService</tt> implementation.
     *
     * @param parentProvider the SIP <tt>ProtocolProviderService</tt>
     * implementation which has requested the creation of the new instance and
     * for which the new instance is to provide telephony conferencing services
     */
    public OperationSetTelephonyConferencingSipImpl(
        ProtocolProviderServiceSipImpl parentProvider)
    {
        super(parentProvider);

        this.subscriber
                = new EventPackageSubscriber(
                        this.parentProvider,
                        EVENT_PACKAGE,
                        SUBSCRIPTION_DURATION,
                        CONTENT_SUB_TYPE,
                        this.timer,
                        REFRESH_MARGIN);
        this.notifier
                = new EventPackageNotifier(
                        this.parentProvider,
                        EVENT_PACKAGE,
                        SUBSCRIPTION_DURATION,
                        CONTENT_SUB_TYPE,
                        this.timer)
                {
                    protected Subscription createSubscription(
                            Address fromAddress,
                            String eventId)
                    {
                        return
                            new ConferenceNotifierSubscription(
                                    fromAddress,
                                    eventId);
                    }
                };
    }

    /**
     * Appends a specific array of <tt>String</tt>s to a specific
     * <tt>StringBuffer</tt>.
     *
     * @param stringBuffer the <tt>StringBuffer</tt> to append the specified
     * <tt>strings</tt> to
     * @param strings the <tt>String</tt> values to be appended to the specified
     * <tt>stringBuffer</tt>
     */
    private static void append(StringBuffer stringBuffer, String... strings)
    {
        for (String str : strings)
            stringBuffer.append(str);
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
            OperationSetBasicTelephonySipImpl oldValue,
            OperationSetBasicTelephonySipImpl newValue)
    {
        if (oldValue != null)
            oldValue.removeCallListener(this);
        if (newValue != null)
            newValue.addCallListener(this);
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
        CallPeerSipImpl callPeer = (CallPeerSipImpl) event.getSourceCallPeer();
        callPeer.addMethodProcessorListener(this);
        super.callPeerAdded(event);
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
        CallPeerSipImpl callPeer = (CallPeerSipImpl) event.getSourceCallPeer();
        callPeer.removeMethodProcessorListener(this);
        super.callPeerRemoved(event);
    }

    /**
     * Creates a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>.
     *
     * @return a new outgoing <tt>Call</tt> into which conference callees are to
     * be invited by this <tt>OperationSetTelephonyConferencing</tt>
     * @throws OperationFailedException if anything goes wrong
     */
    protected CallSipImpl createOutgoingCall()
        throws OperationFailedException
    {
        return getBasicTelephony().createOutgoingCall();
    }

    /**
     * Generates the conference-info XML to be sent to a specific
     * <tt>CallPeer</tt> in order to notify it of the current state of the
     * conference managed by the local peer.
     *
     * @param callPeer the <tt>CallPeer</tt> to generate conference-info XML for
     * @param version the value of the version attribute of the
     * <tt>conference-info</tt> root element of the conference-info XML to be
     * generated
     * @return the conference-info XML to be sent to the specified
     * <tt>callPeer</tt> in order to notify it of the current state of the
     * conference managed by the local peer
     */
    private String getConferenceInfoXML(CallPeerSipImpl callPeer, int version)
    {
        Dialog dialog = callPeer.getDialog();
        String localParty = null;

        if (dialog != null)
        {
            Address localPartyAddress = dialog.getLocalParty();

            if (localPartyAddress != null)
                localParty
                    = stripParametersFromAddress(
                        localPartyAddress.getURI().toString());
        }

        StringBuffer xml = new StringBuffer();
        CallSipImpl call = callPeer.getCall();

        xml.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        // <conference-info>
        append(xml, "<", ELEMENT_CONFERENCE_INFO);
        // entity
        append(xml, " entity=\"", domElementWriter.encode(localParty), "\"");
        // state
        xml.append(" state=\"full\"");
        // version
        append(xml, " version=\"", Integer.toString(version), "\">");
        // <conference-description/>
        append(xml, "<", ELEMENT_CONFERENCE_DESCRIPTION, "/>");
        // <conference-state>
        append(xml, "<", ELEMENT_CONFERENCE_STATE, ">");
        // <user-count>
        append(xml, "<", ELEMENT_USER_COUNT, ">");
        xml.append(call.getCallPeerCount() + 1);
        // </user-count>
        append(xml, "</", ELEMENT_USER_COUNT, ">");
        // </conference-state>
        append(xml, "</", ELEMENT_CONFERENCE_STATE, ">");
        // <users>
        append(xml, "<", ELEMENT_USERS, ">");

        // <user>
        append(xml, "<", ELEMENT_USER);
        // entity
        append(xml, " entity=\"", domElementWriter.encode(localParty), "\"");
        // state
        xml.append(" state=\"full\">");

        String ourDisplayName = parentProvider.getOurDisplayName();

        if (ourDisplayName != null)
        {
            // <display-text>
            append(xml, "<", ELEMENT_DISPLAY_TEXT, ">");
            xml.append(domElementWriter.encode(ourDisplayName));
            // </display-text>
            append(xml, "</", ELEMENT_DISPLAY_TEXT, ">");
        }
        // <endpoint>
        append(xml, "<", ELEMENT_ENDPOINT, ">");
        // <status>
        append(xml, "<", ELEMENT_STATUS, ">");
        // We are the conference focus so we're connected to the conference.
        xml.append(ConferenceMemberSipImpl.CONNECTED);
        // </status>
        append(xml, "</", ELEMENT_STATUS, ">");
        getMediaXML(callPeer, false, xml);
        // </endpoint>
        append(xml, "</", ELEMENT_ENDPOINT, ">");
        // </user>
        append(xml, "</", ELEMENT_USER, ">");

        Iterator<CallPeerSipImpl> callPeerIter = call.getCallPeers();

        while (callPeerIter.hasNext())
            getUserXML(callPeerIter.next(), xml);

        // </users>
        append(xml, "</", ELEMENT_USERS, ">");
        // </conference-info>
        append(xml, "</", ELEMENT_CONFERENCE_INFO, ">");
        return xml.toString();
    }

    /**
     * Reads the text content of the <tt>src-id</tt> XML element of a
     * <tt>media</tt> XML element of a specific <tt>endpoint</tt> XML element.
     *
     * @param endpoint an XML <tt>Node</tt> which represents the
     * <tt>endpoint</tt> XML element from which to get the text content of a
     * <tt>src-id</tt> XML element of a <tt>media</tt> XML element
     * @param mediaType the type of the media to get the <tt>src-id</tt> of
     * @return the text content of the <tt>src-id</tt> XML element of the
     * <tt>media</tt> XML element of the specified <tt>endpoint</tt> XML element
     * if any; otherwise, <tt>null</tt>
     */
    private String getEndpointMediaSrcId(Node endpoint, MediaType mediaType)
    {
        NodeList endpointChildList = endpoint.getChildNodes();
        int endpoingChildCount = endpointChildList.getLength();
        String mediaTypeStr = mediaType.toString();

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
                String type = null;

                for (int mediaChildIndex = 0;
                        mediaChildIndex < mediaChildCount;
                        mediaChildIndex++)
                {
                    Node mediaChild = mediaChildList.item(mediaChildIndex);
                    String mediaChildName = mediaChild.getNodeName();

                    if (ELEMENT_SRC_ID.equals(mediaChildName))
                    {
                        srcId = mediaChild.getTextContent();
                        if (mediaTypeStr.equalsIgnoreCase(type))
                            return srcId;
                    }
                    else if (ELEMENT_TYPE.equals(mediaChildName))
                    {
                        type = mediaChild.getTextContent();
                        if ((srcId != null) && mediaTypeStr.equalsIgnoreCase(
                                type))
                            return srcId;
                    }
                }
            }
        }
        return null;
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
        NodeList endpointChildList = endpoint.getChildNodes();
        int endpoingChildCount = endpointChildList.getLength();

        for (int endpointChildIndex = 0;
                endpointChildIndex < endpoingChildCount;
                endpointChildIndex++)
        {
            Node endpointChild = endpointChildList.item(endpointChildIndex);

            if (ELEMENT_STATUS.equals(endpointChild.getNodeName()))
                return endpointChild.getTextContent();
        }
        return null;
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
    private String getEndpointStatusXML(CallPeerSipImpl callPeer)
    {
        CallPeerState callPeerState = callPeer.getState();

        if (CallPeerState.ALERTING_REMOTE_SIDE.equals(callPeerState))
            return ConferenceMemberSipImpl.ALERTING;
        if (CallPeerState.CONNECTING.equals(callPeerState)
                || CallPeerState
                    .CONNECTING_WITH_EARLY_MEDIA.equals(callPeerState))
            return ConferenceMemberSipImpl.PENDING;
        if (CallPeerState.DISCONNECTED.equals(callPeerState))
            return ConferenceMemberSipImpl.DISCONNECTED;
        if (CallPeerState.INCOMING_CALL.equals(callPeerState))
            return ConferenceMemberSipImpl.DIALING_IN;
        if (CallPeerState.INITIATING_CALL.equals(callPeerState))
            return ConferenceMemberSipImpl.DIALING_OUT;

        /*
         * he/she is neither "hearing" the conference mix nor is his/her media
         * being mixed in the conference
         */
        if (CallPeerState.ON_HOLD_LOCALLY.equals(callPeerState)
                || CallPeerState.ON_HOLD_MUTUALLY.equals(callPeerState))
            return ConferenceMemberSipImpl.ON_HOLD;
        if (CallPeerState.CONNECTED.equals(callPeerState))
            return ConferenceMemberSipImpl.CONNECTED;
        return null;
    }

    /**
     * Appends to a specific <tt>StringBuffer</tt> <tt>media</tt> XML element
     * trees which describe the state of the media streaming between a specific
     * <tt>CallPeer</tt> and its local peer represented by an associated
     * <tt>Call</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> which is to get its media streaming
     * state described in <tt>media</tt> XML element trees appended to the
     * specified <tt>StringBuffer</tt>
     * @param remote <tt>true</tt> if the streaming from the <tt>callPeer</tt>
     * to the local peer is to be described or <tt>false</tt> if the streaming
     * from the local peer to the remote <tt>callPeer</tt> is to be described
     * @param xml the <tt>StringBuffer</tt> to append the <tt>media</tt> XML
     * trees describing the media streaming state of the specified
     * <tt>callPeer</tt>
     */
    private void getMediaXML(
            CallPeerSipImpl callPeer,
            boolean remote,
            StringBuffer xml)
    {
        CallPeerMediaHandlerSipImpl mediaHandler = callPeer.getMediaHandler();

        for (MediaType mediaType : MediaType.values())
        {
            MediaStream stream = mediaHandler.getStream(mediaType);

            if (stream != null)
            {
                // <media>
                append(xml, "<", ELEMENT_MEDIA, ">");
                // <type>
                append(xml, "<", ELEMENT_TYPE, ">");
                xml.append(mediaType.toString());
                // </type>
                append(xml, "</", ELEMENT_TYPE, ">");

                long srcId
                    = remote
                        ? stream.getRemoteSourceID()
                        : stream.getLocalSourceID();

                if (srcId != -1)
                {
                    // <src-id>
                    append(xml, "<", ELEMENT_SRC_ID, ">");
                    xml.append(srcId);
                    // </src-id>
                    append(xml, "</", ELEMENT_SRC_ID, ">");
                }

                MediaDirection direction = stream.getDirection();

                if (direction == null)
                    direction = MediaDirection.INACTIVE;
                // <status>
                append(xml, "<", ELEMENT_STATUS, ">");
                xml.append(direction.toString());
                // </status>
                append(xml, "</", ELEMENT_STATUS, ">");
                // </media>
                append(xml, "</", ELEMENT_MEDIA, ">");
            }
        }
    }

    /**
     * Appends to a specific <tt>StringBuffer</tt> a <tt>user</tt> XML element
     * tree which describes the participation of a specific <tt>CallPeer</tt> in
     * a conference managed by the local peer represented by its associated
     * <tt>Call</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt> which is to get its conference
     * participation describes in a <tt>user</tt> XML element tree appended to
     * the specified <tt>StringBuffer</tt>
     * @param xml the <tt>StringBuffer</tt> to append the <tt>user</tt> XML
     * tree describing the conference participation of the specified
     * <tt>callPeer</tt> to
     */
    private void getUserXML(CallPeerSipImpl callPeer, StringBuffer xml)
    {
        // <user>
        append(xml, "<", ELEMENT_USER);
        // entity
        append(
            xml,
            " entity=\"",
            domElementWriter
                .encode(stripParametersFromAddress(callPeer.getAddress())),
            "\"");
        // state
        xml.append(" state=\"full\">");

        String displayName = callPeer.getDisplayName();

        if (displayName != null)
        {
            // <display-text>
            append(xml, "<", ELEMENT_DISPLAY_TEXT, ">");
            xml.append(domElementWriter.encode(displayName));
            // </display-text>
            append(xml, "</", ELEMENT_DISPLAY_TEXT, ">");
        }
        // <endpoint>
        append(xml, "<", ELEMENT_ENDPOINT, ">");

        String status = getEndpointStatusXML(callPeer);

        if (status != null)
        {
            // <status>
            append(xml, "<", ELEMENT_STATUS, ">");
            xml.append(status);
            // </status>
            append(xml, "</", ELEMENT_STATUS, ">");
        }
        getMediaXML(callPeer, true, xml);
        // </endpoint>
        append(xml, "</", ELEMENT_ENDPOINT, ">");
        // </user>
        append(xml, "</", ELEMENT_USER, ">");
    }

    /**
     * Invites a callee with a specific SIP <tt>Address</tt> to be joined in a
     * specific <tt>Call</tt> in the sense of SIP conferencing.
     *
     * @param calleeAddress the SIP <tt>Address</tt> of the callee to be invited
     * to the specified existing <tt>Call</tt>
     * @param call the existing <tt>Call</tt> to invite the callee with the
     * specified SIP <tt>calleeAddress</tt> to
     * @param wasConferenceFocus the value of the <tt>conferenceFocus</tt>
     * property of the specified <tt>call</tt> prior to the request to invite
     * the specified <tt>calleeAddress</tt>
     * @return a new SIP <tt>CallPeer</tt> instance which describes the SIP
     * signaling and the media streaming of the newly-invited callee within the
     * specified <tt>Call</tt>
     * @throws OperationFailedException if inviting the specified callee to the
     * specified call fails
     */
    protected CallPeerSipImpl inviteCalleeToCall(
            Address calleeAddress,
            CallSipImpl call,
            boolean wasConferenceFocus)
        throws OperationFailedException
    {
        if (!wasConferenceFocus && call.isConferenceFocus())
        {
            Iterator<CallPeerSipImpl> callPeerIter = call.getCallPeers();

            while (callPeerIter.hasNext())
                callPeerIter.next().sendReInvite();
        }

        return call.invite(calleeAddress, null);
    }

    /**
     * Notifies this <tt>MethodProcessorListener</tt> that the procedure for
     * handling an INVITE or reINVITE SIP <tt>Request</tt> has completed and it
     * is appropriate to determine whether the remote <tt>CallPeer</tt> is a
     * conference focus.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> with which the procedure for
     * handling an INVITE or reINVITE SIP <tt>Request</tt> has finished
     * negotiating
     * @param remoteMessage the remote SIP <tt>Message</tt> which was received,
     * processed and which prompted sending a specific local SIP
     * <tt>Message</tt>
     * @param localMessage the local SIP <tt>Message</tt> which was sent to the
     * <tt>CallPeer</tt> as part of the processing of its remote SIP
     * <tt>Message</tt>
     */
    private void inviteCompleted(
            CallPeerSipImpl sourceCallPeer,
            Message remoteMessage,
            Message localMessage)
    {
        ContactHeader contactHeader
            = (ContactHeader) remoteMessage.getHeader(ContactHeader.NAME);
        boolean conferenceFocus = false;

        if (contactHeader != null)
        {
            /*
             * The javadoc says that ContactHeader#getParameter(String) will
             * return an empty string for a flag but it does not and returns
             * null.
             */
            Iterator<?> parameterNameIter
                = contactHeader.getParameterNames();

            while (parameterNameIter.hasNext())
                if ("isfocus"
                        .equalsIgnoreCase(parameterNameIter.next().toString()))
                {
                    conferenceFocus = true;
                    break;
                }
        }

        sourceCallPeer.setConferenceFocus(conferenceFocus);
        if (conferenceFocus)
        {
            ConferenceSubscriberSubscription subscription
                = new ConferenceSubscriberSubscription(sourceCallPeer);

            try
            {
                subscriber.subscribe(subscription);
            }
            catch (OperationFailedException ofe)
            {
                logger
                    .error(
                        "Failed to create or send a conference subscription to "
                            + sourceCallPeer,
                        ofe);
            }
        }
    }

    /**
     * Notifies all <tt>Subscription</tt>s associated with and established in a
     * specific <tt>Call</tt> about an <tt>ACTIVE</tt> subscription state
     * without a reason for that subscription state.
     *
     * @param call the <tt>Call</tt> in which the <tt>Subscription</tt>s to be
     * notified have been established
     */
    protected void notifyAll(Call call)
    {
        notifyAll(SubscriptionStateHeader.ACTIVE, null, call);
    }

    /**
     * Notifies all <tt>Subscription</tt>s associated with and established in a
     * specific <tt>Call</tt> about a specific subscription state and the reason
     * for that subscription state.
     *
     * @param subscriptionState the subscription state to notify about
     * @param reason the reason for entering the specified
     * <tt>subscriptionState</tt>
     * @param call the <tt>Call</tt> in which the <tt>Subscription</tt>s to be
     * notified have been established
     */
    private void notifyAll(
            String subscriptionState,
            String reason,
            final Call call)
    {
        EventPackageNotifier.SubscriptionFilter subscriptionFilter
            = new EventPackageNotifier.SubscriptionFilter()
            {
                public boolean accept(
                        EventPackageNotifier.Subscription subscription)
                {
                    return
                        (subscription instanceof ConferenceNotifierSubscription)
                            && call
                                    .equals(
                                        ((ConferenceNotifierSubscription)
                                                subscription)
                                            .getCall());
                }
            };

        try
        {
            notifier.notifyAll(subscriptionState, reason, subscriptionFilter);
        }
        catch (OperationFailedException ofe)
        {
            logger
                .error(
                    "Failed to notify the conference subscriptions of " + call,
                    ofe);
        }
    }

    /**
     * Parses a <tt>String</tt> value which represents a SIP address into a SIP
     * <tt>Address</tt> value.
     *
     * @param calleeAddressString a <tt>String</tt> value which represents a SIP
     * address to be parsed into a SIP <tt>Address</tt> value
     * @return a SIP <tt>Address</tt> value which represents the specified
     * <tt>calleeAddressString</tt>
     * @throws OperationFailedException if parsing the specified
     * <tt>calleeAddressString</tt> fails
     */
    protected Address parseAddressString(String calleeAddressString)
        throws OperationFailedException
    {
        try
        {
            return parentProvider.parseAddressString(calleeAddressString);
        }
        catch (ParseException pe)
        {
            ProtocolProviderServiceSipImpl
                .throwOperationFailedException(
                    "Failed to parse callee address " + calleeAddressString,
                    OperationFailedException.ILLEGAL_ARGUMENT,
                    pe,
                    logger);
            return null;
        }
    }

    /**
     * Notifies this <tt>MethodProcessorListener</tt> that a specific
     * <tt>CallPeer</tt> has processed a specific SIP <tt>Request</tt> and has
     * replied to it with a specific SIP <tt>Response</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which has processed the
     * specified SIP <tt>Request</tt>
     * @param request the SIP <tt>Request</tt> which has been processed by
     * <tt>sourceCallPeer</tt>
     * @param response the SIP <tt>Response</tt> sent by <tt>sourceCallPeer</tt>
     * as a reply to the specified SIP <tt>request</tt>
     * @see MethodProcessorListener#requestProcessed(CallPeerSipImpl, Request,
     * Response)
     */
    public void requestProcessed(
            CallPeerSipImpl sourceCallPeer,
            Request request,
            Response response)
    {
        if (Request.INVITE.equalsIgnoreCase(request.getMethod())
                && (response != null)
                && (Response.OK == response.getStatusCode()))
            inviteCompleted(sourceCallPeer, request, response);
    }

    /**
     * Notifies this <tt>MethodProcessorListener</tt> that a specific
     * <tt>CallPeer</tt> has processed a specific SIP <tt>Response</tt> and has
     * replied to it with a specific SIP <tt>Request</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which has processed the
     * specified SIP <tt>Response</tt>
     * @param response the SIP <tt>Response</tt> which has been processed by
     * <tt>sourceCallPeer</tt>
     * @param request the SIP <tt>Request</tt> sent by <tt>sourceCallPeer</tt>
     * as a reply to the specified SIP <tt>response</tt>
     * @see MethodProcessorListener#responseProcessed(CallPeerSipImpl, Response,
     * Request)
     */
    public void responseProcessed(
            CallPeerSipImpl sourceCallPeer,
            Response response,
            Request request)
    {
        if (Response.OK == response.getStatusCode())
        {
            CSeqHeader cseqHeader
                = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

            if ((cseqHeader != null)
                    && Request.INVITE.equalsIgnoreCase(cseqHeader.getMethod()))
                inviteCompleted(sourceCallPeer, response, request);
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
            CallPeerSipImpl callPeer,
            Document conferenceInfoDocument)
    {
        NodeList usersList
            = conferenceInfoDocument.getElementsByTagName(ELEMENT_USERS);
        ConferenceMember[] conferenceMembersToRemove
            = callPeer.getConferenceMembers();
        int conferenceMembersToRemoveCount = conferenceMembersToRemove.length;

        if (usersList.getLength() > 0)
        {
            NodeList userList = usersList.item(0).getChildNodes();
            int userCount = userList.getLength();

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
                 * Determine the ConferenceMembers which are no longer in the
                 * list.
                 */
                ConferenceMemberSipImpl existingConferenceMember = null;

                for (int conferenceMemberIndex = 0;
                        conferenceMemberIndex < conferenceMembersToRemoveCount;
                        conferenceMemberIndex++)
                {
                    ConferenceMemberSipImpl conferenceMember
                        = (ConferenceMemberSipImpl)
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
                boolean addConferenceMember;
                if (existingConferenceMember == null)
                {
                    existingConferenceMember
                        = new ConferenceMemberSipImpl(callPeer, address);
                    addConferenceMember = true;
                }
                else
                    addConferenceMember = false;

                // Update the existing ones.
                if (existingConferenceMember != null)
                {
                    NodeList userChildList = user.getChildNodes();
                    int userChildCount = userChildList.getLength();
                    String displayName = null;
                    String endpointStatus = null;
                    String ssrc = null;

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
                            ssrc = getEndpointMediaSrcId(
                                    userChild,
                                    MediaType.AUDIO);
                        }
                    }
                    existingConferenceMember.setDisplayName(displayName);
                    existingConferenceMember.setEndpointStatus(endpointStatus);

                    if (ssrc != null)
                        existingConferenceMember.setSSRC(Long.parseLong(ssrc));

                    if (addConferenceMember)
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
    private int setConferenceInfoXML(
            CallPeerSipImpl callPeer,
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

        Document document = null;
        Throwable exception = null;

        try
        {
            document
                = DocumentBuilderFactory
                    .newInstance()
                        .newDocumentBuilder()
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
                = Integer
                    .parseInt(
                        document.getDocumentElement().getAttribute("version"));

            if (documentVersion >= version)
            {
                setConferenceInfoDocument(callPeer, document);
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
    private static String stripParametersFromAddress(String address)
    {
        if (address != null)
        {
            int parametersBeginIndex = address.indexOf(';');

            if (parametersBeginIndex > -1)
                address = address.substring(0, parametersBeginIndex);
        }
        return address;
    }

    /**
     * Implements <tt>EventPackageNotifier.Subscription</tt> in order to
     * represent a conference subscription created by a remote <tt>CallPeer</tt>
     * to the conference event package of a local <tt>Call</tt>.
     */
    private class ConferenceNotifierSubscription
        extends EventPackageNotifier.Subscription
    {

        /**
         * The value of the <tt>version</tt> attribute to be specified in the
         * outgoing <tt>conference-info</tt> root XML elements.
         */
        private int version = 1;

        /**
         * Initializes a new <tt>ConferenceNotifierSubscription</tt> instance
         * with a specific subscription <tt>Address</tt>/Request URI and a
         * specific id tag of the associated Event headers.
         *
         * @param fromAddress the subscription <tt>Address</tt>/Request URI
         * which is to be the target of the NOTIFY requests associated with the
         * new instance
         * @param eventId the value of the id tag to be placed in the Event
         * headers of the NOTIFY requests created for the new instance and to be
         * present in the received Event headers in order to have the new
         * instance associated with them
         */
        public ConferenceNotifierSubscription(
                Address fromAddress,
                String eventId)
        {
            super(fromAddress, eventId);
        }

        /**
         * Creates the content of the NOTIFY request to be sent to the target
         * represented by this <tt>Subscription</tt> and having a specific
         * subscription state and a specific reason for that subscription state.
         *
         * @param subscriptionState the subscription state to be notified about
         * in the NOTIFY request which is to carry the returned content
         * @param reason the reason for the subscription state to be notified
         * about in the NOTIFY request which is to carry the returned content
         *
         * @return an array of <tt>byte</tt>s representing the content of the
         * NOTIFY request to be sent to the target represented by this
         * <tt>Subscription</tt>
         * @see EventPackageNotifier.Subscription#createNotifyContent(String,
         * String)
         */
        protected byte[] createNotifyContent(
                String subscriptionState,
                String reason)
        {
            CallPeerSipImpl callPeer = getCallPeer();

            if (callPeer == null)
            {
                logger
                    .error(
                        "Failed to find the CallPeer of the conference " +
                        "subscription " + this);
                return null;
            }

            String conferenceInfoXML = getConferenceInfoXML(callPeer, version);
            byte[] notifyContent;

            if (conferenceInfoXML == null)
                notifyContent = null;
            else
            {
                try
                {
                    notifyContent = conferenceInfoXML.getBytes("UTF-8");
                }
                catch (UnsupportedEncodingException uee)
                {
                    logger
                        .warn(
                            "Failed to gets bytes from String for the UTF-8 " +
                            "charset",
                            uee);
                    notifyContent = conferenceInfoXML.getBytes();
                }
                ++ version;
            }
            return notifyContent;
        }

        /**
         * Gets the <tt>Call</tt> of the <tt>CallPeerSipImpl</tt> subscribed to
         * the <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>.
         *
         * @return the <tt>Call</tt> of the <tt>CallPeerSipImpl</tt> subscribed
         * to the <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>
         */
        public CallSipImpl getCall()
        {
            CallPeerSipImpl callPeer = getCallPeer();

            return (callPeer == null) ? null : callPeer.getCall();
        }

        /**
         * Gets the <tt>CallPeerSipImpl</tt> subscribed to the
         * <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>.
         *
         * @return the <tt>CallPeerSipImpl</tt> subscribed to the
         * <tt>EventPackageNotifier</tt> and represented by this
         * <tt>Subscription</tt>
         */
        private CallPeerSipImpl getCallPeer()
        {
            Dialog dialog = getDialog();

            if (dialog != null)
            {
                OperationSetBasicTelephonySipImpl basicTelephony
                    = getBasicTelephony();

                if (basicTelephony != null)
                    return
                        basicTelephony
                            .getActiveCallsRepository().findCallPeer(dialog);
            }
            return null;
        }
    }

    /**
     * Implements <tt>EventPackageSubscriber.Subscription</tt> in order to
     * represent the conference subscription of the local peer to the conference
     * event package of a specific remote <tt>CallPeer</tt> acting as a
     * conference focus.
     */
    private class ConferenceSubscriberSubscription
        extends EventPackageSubscriber.Subscription
    {

        /**
         * The <tt>CallPeer</tt> which is acting as a conference focus in its
         * <tt>Call</tt> with the local peer.
         */
        private final CallPeerSipImpl callPeer;

        /**
         * The value of the <tt>version</tt> attribute specified in the incoming
         * <tt>conference-info</tt> root XML element that is currently
         * represented in {@link #callPeer}.
         */
        private int version = 0;

        /**
         * Initializes a new <tt>ConferenceSubscriberSubscription</tt> instance
         * which is to represent the conference subscription of the local peer
         * to the conference event package of a specific <tt>CallPeer</tt>
         * acting as a conference focus.
         *
         * @param callPeer the <tt>CallPeer</tt> acting as a conference focus
         * which the new instance is to subscribe to
         */
        public ConferenceSubscriberSubscription(CallPeerSipImpl callPeer)
        {
            super(callPeer.getPeerAddress());

            this.callPeer = callPeer;
        }

        /**
         * Gets the <tt>Dialog</tt> which was created by the SUBSCRIBE request
         * associated with this <tt>Subscription</tt> or which was used to send
         * that request in.
         *
         * @return the <tt>Dialog</tt> which was created by the SUBSCRIBE
         * request associated with this <tt>Subscription</tt> or which was used
         * to send that request in; <tt>null</tt> if the success of the
         * SUBSCRIBE request has not been confirmed yet or this
         * <tt>Subscription</tt> was removed from the list of the
         * <tt>EventPackageSupport</tt> it used to be in
         * @see EventPackageSubscriber.Subscription#getDialog()
         */
        @Override
        protected Dialog getDialog()
        {
            Dialog dialog = super.getDialog();

            if ((dialog == null)
                    || DialogState.TERMINATED.equals(dialog.getState()))
                dialog = callPeer.getDialog();
            return dialog;
        }

        /**
         * Notifies this <tt>Subscription</tt> that an active NOTIFY
         * <tt>Request</tt> has been received and it may process the specified
         * raw content carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full
         * details of the received NOTIFY <tt>Request</tt> including the raw
         * content which may be processed by this <tt>Subscription</tt>
         * @param rawContent an array of bytes which represents the raw content
         * carried in the body of the received NOTIFY <tt>Request</tt> and
         * extracted from the specified <tt>RequestEvent</tt> for the
         * convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processActiveRequest(
         * RequestEvent, byte[])
         */
        protected void processActiveRequest(
                RequestEvent requestEvent,
                byte[] rawContent)
        {
            if (rawContent != null)
            {
                int contentVersion
                    = setConferenceInfoXML(
                        callPeer,
                        version,
                        SdpUtils.getContentAsString(requestEvent.getRequest()));

                if (contentVersion >= version)
                    version = contentVersion;
            }
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt> to a
         * previous SUBSCRIBE <tt>Request</tt> has been received with a status
         * code in the failure range and it may process the status code carried
         * in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the full
         * details of the received <tt>Response</tt> including the status code
         * which may be processed by this <tt>Subscription</tt>
         * @param statusCode the status code carried in the <tt>Response</tt>
         * and extracted from the specified <tt>ResponseEvent</tt> for the
         * convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processFailureResponse(
         * ResponseEvent, int)
         */
        protected void processFailureResponse(
                ResponseEvent responseEvent ,
                int statusCode)
        {

            /*
             * We've failed to subscribe to the conference event package of the
             * CallPeer so, regardless of it announcing "isfocus", it's as good
             * as not being a conference focus because we'll not be able to
             * retrieve its ConferenceMembers.
             */
            callPeer.setConferenceFocus(false);
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt> to a
         * previous SUBSCRIBE <tt>Request</tt> has been received with a status
         * code in the success range and it may process the status code carried
         * in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the full
         * details of the received <tt>Response</tt> including the status code
         * which may be processed by this <tt>Subscription</tt>
         * @param statusCode the status code carried in the <tt>Response</tt>
         * and extracted from the specified <tt>ResponseEvent</tt> for the
         * convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processSuccessResponse(
         * ResponseEvent, int)
         */
        protected void processSuccessResponse(
                ResponseEvent responseEvent,
                int statusCode)
        {
            switch (statusCode)
            {
            case Response.OK:
            case Response.ACCEPTED:

                /*
                 * We've managed to subscribe to the conference event package of
                 * the CallPeer so, regardless of it not announcing "isfocus",
                 * we know it's a conference focus.
                 */
                callPeer.setConferenceFocus(true);
                break;
            }
        }

        /**
         * Notifies this <tt>Subscription</tt> that a terminating NOTIFY
         * <tt>Request</tt> has been received and it may process the reason code
         * carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full
         * details of the received NOTIFY <tt>Request</tt> including the reason
         * code which may be processed by this <tt>Subscription</tt>
         * @param reasonCode the code of the reason for the termination carried
         * in the NOTIFY <tt>Request</tt> and extracted from the specified
         * <tt>RequestEvent</tt> for the convenience of the implementers
         * @see EventPackageSubscriber.Subscription#processTerminatedRequest(
         * RequestEvent, String)
         */
        protected void processTerminatedRequest(
                RequestEvent requestEvent,
                String reasonCode)
        {
            if (SubscriptionStateHeader.DEACTIVATED.equals(reasonCode))
                try
                {
                    subscriber.poll(this);
                }
                catch (OperationFailedException ofe)
                {
                    logger
                        .error(
                            "Failed to renew the conference subscription "
                                + this,
                            ofe);
                }
        }
    }
}
