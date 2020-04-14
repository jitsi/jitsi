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
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.*;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.sip.message.Message;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;

/**
 * Implements <tt>OperationSetTelephonyConferencing</tt> for SIP.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
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
     * The minimum interval in milliseconds between conference-info NOTIFYs
     * sent to a single <tt>CallPeer</tt>.
     */
    private static final int MIN_NOTIFY_INTERVAL = 200;

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
     * Listener to CallPeer state.
     */
    private final CallPeerListener callPeerStateListener =
        new CallPeerAdapter()
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
                    if(peer.getState() == CallPeerState.CONNECTED)
                    {
                        if (peer.isConferenceFocus())
                        {
                            ConferenceSubscriberSubscription subscription
                                = new ConferenceSubscriberSubscription(
                                    (CallPeerSipImpl)peer);

                            try
                            {
                                subscriber.subscribe(subscription);
                            }
                            catch (OperationFailedException ofe)
                            {
                                logger
                                    .error(
                                        "Failed to create or send a " +
                                        "conference subscription to " + peer,
                                        ofe);
                            }
                            peer.removeCallPeerListener(this);
                        }
                    }
                }
            }
        };

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
                = new ConferenceEventPackageNotifier(this.parentProvider,
                    this.timer);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific
     * <tt>CallPeer</tt> has been added to a specific <tt>Call</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been added to a <tt>Call</tt>
     */
    @Override
    public void callPeerAdded(CallPeerEvent event)
    {
        super.callPeerAdded(event);

        CallPeer callPeer = event.getSourceCallPeer();

        if (callPeer instanceof CallPeerSipImpl)
            ((CallPeerSipImpl) callPeer).addMethodProcessorListener(this);
    }

    /**
     * Notifies this <tt>CallChangeListener</tt> that a specific
     * <tt>CallPeer</tt> has been remove from a specific <tt>Call</tt>.
     *
     * @param event a <tt>CallPeerEvent</tt> which specifies the
     * <tt>CallPeer</tt> which has been removed from a <tt>Call</tt>
     */
    @Override
    public void callPeerRemoved(CallPeerEvent event)
    {
        CallPeer callPeer = event.getSourceCallPeer();

        if (callPeer instanceof CallPeerSipImpl)
            ((CallPeerSipImpl) callPeer).removeMethodProcessorListener(this);

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
    @Override
    protected CallSipImpl createOutgoingCall()
        throws OperationFailedException
    {
        return getBasicTelephony().createOutgoingCall();
    }

    /**
     *
     * {@inheritDoc}
     *
     * Implements the protocol-dependent part of the logic of inviting a callee
     * to a <tt>Call</tt>. The protocol-independent part of that logic is
     * implemented by
     * {@link AbstractOperationSetTelephonyConferencing#inviteCalleeToCall(String,Call)}.
     */
    @Override
    protected CallPeerSipImpl doInviteCalleeToCall(
            Address calleeAddress,
            CallSipImpl call)
        throws OperationFailedException
    {
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

        sourceCallPeer.addCallPeerListener(callPeerStateListener);
        sourceCallPeer.setConferenceFocus(conferenceFocus);

        if (sourceCallPeer.isConferenceFocus() && sourceCallPeer.getState() ==
            CallPeerState.CONNECTED)
        {
            ConferenceSubscriberSubscription subscription
                = new ConferenceSubscriberSubscription(
                    sourceCallPeer);

            try
            {
                subscriber.subscribe(subscription);
            }
            catch (OperationFailedException ofe)
            {
                logger
                    .error(
                        "Failed to create or send a conference subscription to "
                        + sourceCallPeer, ofe);
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
    @Override
    protected void notifyCallPeers(Call call)
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
    @Override
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
     * {@inheritDoc}
     */
    @Override
    protected String getLocalEntity(CallPeer callPeer)
    {
        if (callPeer instanceof CallPeerSipImpl)
        {
            Dialog dialog = ((CallPeerSipImpl)callPeer).getDialog();

            if (dialog != null)
            {
                Address localPartyAddress = dialog.getLocalParty();

                if (localPartyAddress != null)
                    return stripParametersFromAddress(
                            localPartyAddress.getURI().toString());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getLocalDisplayName()
    {
        return parentProvider.getOurDisplayName();
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
        @Override
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

            ConferenceInfoDocument currentConfInfo
                    = getCurrentConferenceInfo(callPeer);
            ConferenceInfoDocument lastSentConfInfo
                    = callPeer.getLastConferenceInfoSent();

            //Uncomment this when the rest of the code can handle a return value
            //of null in case no NOTIFY needs to be sent.
            /*
            ConferenceInfoDocument diff
                    = lastSentConfInfo == null
                      ? currentConfInfo
                      :getConferenceInfoDiff(lastSentConfInfo, currentConfInfo);
            */
            ConferenceInfoDocument diff = currentConfInfo;

            if (diff == null)
                return null;
            else
            {
                int newVersion
                        = lastSentConfInfo == null
                        ? 1
                        : lastSentConfInfo.getVersion() + 1;
                diff.setVersion(newVersion);
                currentConfInfo.setVersion(newVersion);

                // We save currentConfInfo, because it is of state "full", while
                // diff could be a partial
                callPeer.setLastConferenceInfoSent(currentConfInfo);
                callPeer.setLastConferenceInfoSentTimestamp(
                        System.currentTimeMillis());

                String xml = diff.toXml();
                byte[] notifyContent;
                try
                {
                    notifyContent = xml.getBytes("UTF-8");
                }
                catch (UnsupportedEncodingException uee)
                {
                    logger.warn("Failed to gets bytes from String for the "
                            + "UTF-8 charset", uee);
                    notifyContent = xml.getBytes();
                }
                return notifyContent;
            }
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
        @Override
        protected void processActiveRequest(
                RequestEvent requestEvent,
                byte[] rawContent)
        {
            if (rawContent != null)
            {
                try
                {
                    setConferenceInfoXML(
                        callPeer,
                        SdpUtils.getContentAsString(requestEvent.getRequest()));
                }
                catch (XMLException e)
                {
                    logger.error("Could not handle conference-info NOTIFY sent"
                            + " to us by " + callPeer);
                }
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
        @Override
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
        @Override
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
        @Override
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

    /**
     * An implementation of <tt>EventPackageNotifier</tt> which sends RFC4575
     * NOTIFYs
     */
    private class ConferenceEventPackageNotifier
            extends EventPackageNotifier
    {
        ConferenceEventPackageNotifier(
                ProtocolProviderServiceSipImpl protocolProvider,
                TimerScheduler timer)
        {
            super(protocolProvider,
                    EVENT_PACKAGE,
                    SUBSCRIPTION_DURATION,
                    CONTENT_SUB_TYPE,
                    timer);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected EventPackageNotifier.Subscription createSubscription(
            Address fromAddress,
            String eventId)
        {
            return
                    new ConferenceNotifierSubscription(
                            fromAddress,
                            eventId);
        }

        /**
         * {@inheritDoc}
         *
         * Overrides the default implementation in order to send RFC4575
         * (conference information) NOTIFYs
         */
        @Override
        public void notify( final Subscription subscription,
                            final String subscriptionState,
                            final String reason)
                throws OperationFailedException
        {
            ConferenceNotifierSubscription conferenceSubscription
                    = (ConferenceNotifierSubscription) subscription;

            Dialog dialog = conferenceSubscription.getDialog();
            CallPeerSipImpl callPeer = conferenceSubscription.getCallPeer();
            if (callPeer == null)
            {
                throw new OperationFailedException("Failed to find the CallPeer"
                            + " of the conference subscription "
                            + conferenceSubscription,
                        OperationFailedException.INTERNAL_ERROR);
            }

            final long timeSinceLastNotify = System.currentTimeMillis()
                    - callPeer.getLastConferenceInfoSentTimestamp();
            if (timeSinceLastNotify < MIN_NOTIFY_INTERVAL)
            {
                if (callPeer.isConfInfoScheduled())
                    return;

                logger.info("Scheduling to send a conference-info NOTIFY to "
                        + callPeer);
                callPeer.setConfInfoScheduled(true);
                new Thread(new Runnable(){
                    @Override
                    public void run()
                    {
                        try
                        {
                            Thread.sleep(1 + MIN_NOTIFY_INTERVAL
                                         - timeSinceLastNotify);
                        }
                        catch (InterruptedException ie) {}

                        try
                        {
                            ConferenceEventPackageNotifier.this.notify(
                                    subscription, subscriptionState, reason);
                        }
                        catch (OperationFailedException e)
                        {
                            logger.error("Failed to send NOTIFY request");
                        }
                    }
                }).start();

                return;
            }

            ConferenceInfoDocument currentConfInfo
                    = getCurrentConferenceInfo(callPeer);
            ConferenceInfoDocument lastSentConfInfo
                    = callPeer.getLastConferenceInfoSent();

            ConferenceInfoDocument diff
                    = lastSentConfInfo == null
                      ? currentConfInfo
                      :getConferenceInfoDiff(lastSentConfInfo, currentConfInfo);

            if (diff == null)
            {
                callPeer.setConfInfoScheduled(false);
                return; //no change -- no need to send NOTIFY
            }

            int newVersion
                    = lastSentConfInfo == null
                    ? 1
                    : lastSentConfInfo.getVersion() + 1;
            diff.setVersion(newVersion);

            String xml = diff.toXml();
            byte[] notifyContent;
            try
            {
                notifyContent = xml.getBytes("UTF-8");
            }
            catch (UnsupportedEncodingException uee)
            {
                logger.warn("Failed to gets bytes from String for the "
                        + "UTF-8 charset", uee);
                notifyContent = xml.getBytes();
            }

            String callId;

            /*
             * If sending a notify is requested too quickly (and in different
             * threads, of course), a second notify may be created prior to
             * sending a previous one and the Dialog implementation may mess up
             * the CSeq which will lead to "500 Request out of order".
             */
            synchronized (dialog)
            {
                ClientTransaction transac = createNotify(dialog, notifyContent,
                                subscriptionState, reason);

                callId = dialog.getCallId().getCallId();

                try
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Sending conference-info NOTIFY (version "
                                + newVersion +") to " + callPeer);
                    }
                    dialog.sendRequest(transac);

                    // We save currentConfInfo, because it is of state "full",
                    // while diff could be a partial
                    currentConfInfo.setVersion(newVersion);
                    callPeer.setLastConferenceInfoSent(currentConfInfo);
                    callPeer.setLastConferenceInfoSentTimestamp(
                            System.currentTimeMillis());
                }
                catch (SipException sex)
                {
                    logger.error("Failed to send NOTIFY request.", sex);
                    throw
                            new OperationFailedException(
                                    "Failed to send NOTIFY request.",
                                    OperationFailedException.NETWORK_FAILURE,
                                    sex);
                }
            }

            if (SubscriptionState.TERMINATED.equals(subscriptionState))
                removeSubscription(callId, subscription);

            callPeer.setConfInfoScheduled(false);
        }
    }
}
