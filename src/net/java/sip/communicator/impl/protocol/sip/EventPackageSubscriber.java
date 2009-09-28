/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the subscriber part of RFC 3265
 * "Session Initiation Protocol (SIP)-Specific Event Notification" and thus
 * eases the creation of event package-specific implementations.
 *
 * @author Lubomir Marinov
 * @author Benoit Pradelle
 * @author Emil Ivov
 */
public class EventPackageSubscriber
    extends EventPackageSupport
{
    private static final Logger logger
        = Logger.getLogger(EventPackageSubscriber.class);

    /**
     * The number of seconds before a subscription managed by this instance
     * expires that this subscriber should attempt to refresh it.
     */
    private final int refreshMargin;

    /**
     * The list of subscriptions managed by this instance and indexed by their
     * CallId.
     */
    private final Map<String, Subscription> subscriptions
        = new HashMap<String, Subscription>();

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * Initializes a new <code>EventPackageSubscriber</code> instance which is
     * to provide subscriber support according to RFC 3265 to a specific SIP
     * <code>ProtocolProviderService</code> implementation for a specific event
     * package.
     *
     * @param protocolProvider
     *            the SIP <code>ProtocolProviderService</code> implementation
     *            for which the new instance is to provide subscriber support
     *            for a specific event package
     * @param eventPackage
     *            the name of the event package the new instance is to implement
     *            and carry in the Event and Allow-Events headers
     * @param subscriptionDuration
     *            the duration of each subscription to be managed by the new
     *            instance and to be carried in the Expires headers
     * @param contentSubType
     *            the sub-type of the content type of the NOTIFY bodies to be
     *            announced, expected and supported by the subscriptions to be
     *            managed by the new instance
     * @param timer
     *            the <code>Timer</code> support which is to refresh the
     *            subscriptions to be managed by the new instance
     * @param refreshMargin
     *            the number of seconds before a subscription to be managed by
     *            the new instance expires that the new instance should attempt
     *            to refresh it
     */
    public EventPackageSubscriber(
        ProtocolProviderServiceSipImpl protocolProvider,
        String eventPackage,
        int subscriptionDuration,
        String contentSubType,
        TimerScheduler timer,
        int refreshMargin)
    {
        super(
            protocolProvider,
            eventPackage,
            subscriptionDuration,
            contentSubType,
            timer);

        this.refreshMargin = refreshMargin;
        this.messageFactory = protocolProvider.getMessageFactory();
    }

    /**
     * Adds a specific <code>Subscription</code> associated with a specific
     * CallId to the list of subscriptions managed by this instance.
     *
     * @param callId
     *            the CallId associated with the <code>Subscription</code> to be
     *            added
     * @param subscription
     *            the <code>Subscription</code> to be added to the list of
     *            subscriptions managed by this instance
     */
    private void addSubscription(String callId, Subscription subscription)
    {
        synchronized (subscriptions)
        {
            subscriptions.put(callId, subscription);
        }
    }

    /**
     * Creates a new SUBSCRIBE request in the form of a
     * <code>ClientTransaction</code> with the parameters of a specific
     * <code>Subscription</code>.
     *
     * @param subscription
     *            the <code>Subscription</code> to be described in a SUBSCRIBE
     *            request
     * @param dialog
     *            the <code>Dialog</code> with which this request should be
     *            associated
     * @param expires
     *            the subscription duration of the SUBSCRIBE request to be
     *            created
     * @return a new <code>ClientTransaction</code> initialized with a new
     *         SUBSCRIBE request which matches the parameters of the specified
     *         <code>Subscription</code> and is associated with the specified
     *         <code>Dialog</code>
     * @throws OperationFailedException
     *             if the message could not be generated
     */
    private ClientTransaction createSubscription(
            Subscription subscription,
            Dialog dialog,
            int expires)
        throws OperationFailedException
    {
        Request req = messageFactory.createRequest(dialog, Request.SUBSCRIBE);

        // Address
        Address toAddress = dialog.getRemoteTarget();
        // no Contact field
        if (toAddress == null)
            toAddress = dialog.getRemoteParty();

        //MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider.getMaxForwardsHeader();
        req.setHeader(maxForwards);

        /*
         * Create the transaction and then add the via header as recommended by
         * the jain-sip documentation at
         * http://snad.ncsl.nist.gov/proj/iptel/jain-sip-1.2/javadoc
         * /javax/sip/Dialog.html#createRequest(String).
         */
        ClientTransaction transac = null;
        try
        {
            transac = protocolProvider
                    .getDefaultJainSipProvider().getNewClientTransaction(req);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create subscriptionTransaction.\n"
                + "This is most probably a network connection error."
                , ex);
            throw new OperationFailedException(
                    "Failed to create the subscription transaction",
                    OperationFailedException.NETWORK_FAILURE);
        }

        populateSubscribeRequest(req, subscription, expires);

        return transac;
    }

    /**
     * Creates a new SUBSCRIBE request in the form of a
     * <code>ClientTransaction</code> with the parameters of a specific
     * <code>Subscription</code>.
     *
     * @param subscription
     *            the <code>Subscription</code> to be described in a SUBSCRIBE
     *            request
     * @param expires
     *            the subscription duration of the SUBSCRIBE request to be
     *            created
     * @return a new <code>ClientTransaction</code> initialized with a new
     *         SUBSCRIBE request which matches the parameters of the specified
     *         <code>Subscription</code>
     * @throws OperationFailedException
     *             if the request could not be generated
     */
    private ClientTransaction createSubscription(
            Subscription subscription,
            int expires)
        throws OperationFailedException
    {
        Address toAddress = subscription.getAddress();
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Call ID
        CallIdHeader callIdHeader
            = protocolProvider.getDefaultJainSipProvider().getNewCallId();

        //CSeq
        CSeqHeader cSeqHeader;
        try
        {
            cSeqHeader
                = headerFactory.createCSeqHeader(1l, Request.SUBSCRIBE);
        }
        catch (InvalidArgumentException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //FromHeader and ToHeader
        String localTag = ProtocolProviderServiceSipImpl.generateLocalTag();
        FromHeader fromHeader;
        ToHeader toHeader;
        try
        {
            //FromHeader
            fromHeader
                = headerFactory
                    .createFromHeader(
                        protocolProvider.getOurSipAddress(toAddress),
                        localTag);

            //ToHeader
            toHeader = headerFactory.createToHeader(toAddress, null);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //ViaHeaders
        ArrayList<ViaHeader> viaHeaders
            = protocolProvider.getLocalViaHeaders(toAddress);

        //MaxForwards
        MaxForwardsHeader maxForwards = protocolProvider.getMaxForwardsHeader();

        Request req;
        try
        {
            req
                = protocolProvider
                    .getMessageFactory()
                        .createRequest(
                            toHeader.getAddress().getURI(),
                            Request.SUBSCRIBE,
                            callIdHeader,
                            cSeqHeader,
                            fromHeader,
                            toHeader,
                            viaHeaders,
                            maxForwards);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "Failed to create message Request!", ex);
            throw new OperationFailedException(
                "Failed to create message Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        populateSubscribeRequest(req, subscription, expires);

        //Transaction
        ClientTransaction subscribeTransaction;
        try
        {
            subscribeTransaction
                = protocolProvider
                    .getDefaultJainSipProvider()
                        .getNewClientTransaction(req);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create subscribe transaction.\n"
                + "This is most probably a network connection error.",
                ex);
            throw new OperationFailedException(
                    "Failed to create the subscription transaction",
                    OperationFailedException.NETWORK_FAILURE);
        }
        return subscribeTransaction;
    }

    /**
     * Gets the <code>Subscription</code> from the list of subscriptions managed
     * by this instance which is associated with a specific subscription
     * <code>Address</code>/Request URI and has a specific id tag in its Event
     * header.
     *
     * @param toAddress
     *            the subscription <code>Address</code>/Request URI of the
     *            <code>Subscription</code> to be retrieved
     * @param eventId
     *            the id tag placed in the Event header of the
     *            <code>Subscription</code> to be retrieved if there is one or
     *            <tt>null</tt> if the <code>Subscription</code> should have no
     *            id tag in its Event header
     * @return an existing <code>Subscription</code> from the list of
     *         subscriptions managed by this instance with the specified
     *         subscription <code>Address</code>/Request URI and the specified
     *         id tag in its Event header; <tt>null</tt> if no such
     *         <code>Subscription</code> exists in the list of subscriptions
     *         managed by this instance
     */
    private Subscription getSubscription(Address toAddress, String eventId)
    {
        synchronized (subscriptions)
        {
            for (Subscription subscription : subscriptions.values())
                if (subscription.equals(toAddress, eventId))
                    return subscription;
        }
        return null;
    }

    /**
     * Gets the <code>Subscription</code> from the list of subscriptions managed
     * by this instance which is associated with a specific CallId.
     *
     * @param callId
     *            the CallId associated with the <code>Subscription</code> to be
     *            retrieved
     * @return an existing <code>Subscription</code> from the list of
     *         subscriptions managed by this instance which is associated with
     *         the specified CallId; <tt>null</tt> if no such
     *         <code>Subscription</code> exists in the list of subscriptions
     *         managed by this instance
     */
    private Subscription getSubscription(String callId)
    {
        synchronized (subscriptions)
        {
            return subscriptions.get(callId);
        }
    }

    /**
     * Adds a specific <code>Subscription</code> to the list of subscriptions
     * managed by this instance only if another <code>Subscription</code> with
     * the same subscription <code>Address</code>/Request URI and id tag of its
     * associated Event header does not exist in the list.
     *
     * @param subscription
     *            the new <code>Subscription</code> to be added to the list of
     *            subscriptions managed by this instance if there is no other
     *            <code>Subscription</code> in the list which has the same
     *            subscription <code>Address</code>/Request URI and id tag of
     *            its Event header
     * @throws OperationFailedException
     */
    public void poll(Subscription subscription)
        throws OperationFailedException
    {
        if (getSubscription(
                    subscription.getAddress(),
                    subscription.getEventId())
                == null)
            subscribe(subscription);
    }

    /**
     * Populates a specific <code>Request</code> instance with the headers
     * common to dialog-creating <code>Request</code>s and ones sent inside
     * existing dialogs and specific to the general event package subscription
     * functionality that this instance and a specific <code>Subscription</code>
     * represent.
     *
     * @param req
     *            the <code>Request</code> instance to be populated with common
     *            headers and ones specific to the event package of a specific
     *            <code>Subscription</code>
     * @param subscription
     *            the <code>Subscription</code> which is to be described in the
     *            specified <code>Request</code> i.e. its properties are to be
     *            used to populate the specified <code>Request</code>
     * @param expires
     *            the subscription duration to be set into the Expires header of
     *            the specified SUBSCRIBE <code>Request</code>
     * @throws OperationFailedException
     */
    private void populateSubscribeRequest(
            Request req,
            Subscription subscription,
            int expires)
        throws OperationFailedException
    {
        HeaderFactory headerFactory = protocolProvider.getHeaderFactory();

        // Event
        EventHeader evHeader;
        try
        {
            evHeader = headerFactory.createEventHeader(eventPackage);

            String eventId = subscription.getEventId();
            if (eventId != null)
                evHeader.setEventId(eventId);
        }
        catch (ParseException e)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the EventHeader", e);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the EventHeader"
                , OperationFailedException.INTERNAL_ERROR
                , e);
        }
        req.setHeader(evHeader);

        // Accept
        AcceptHeader accept;
        try
        {
            accept
                = headerFactory
                    .createAcceptHeader("application", contentSubType);
        }
        catch (ParseException e)
        {
            logger.error("wrong accept header", e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the AcceptHeader",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }
        req.setHeader(accept);

        // Expires
        ExpiresHeader expHeader;
        try
        {
            expHeader = headerFactory.createExpiresHeader(expires);
        }
        catch (InvalidArgumentException e)
        {
            logger.error("Invalid expires value: " + expires, e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the ExpiresHeader",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }
        req.setHeader(expHeader);
    }

    /*
     * Implements MethodProcessor#processRequest(RequestEvent). Handles only
     * NOTIFY requests because they are the only requests concerning event
     * package subscribers and if the processing of a given request requires
     * event package-specific handling, delivers the request to the matching
     * Subscription instance. Examples of such event package-specific handling
     * include handling the termination of an existing Subscription and
     * processing the bodies of the NOTIFY requests for active Subscriptions.
     */
    public boolean processRequest(RequestEvent requestEvent)
    {
        Request request = requestEvent.getRequest();

        EventHeader eventHeader
            = (EventHeader) request.getHeader(EventHeader.NAME);
        if ((eventHeader == null)
                || !eventPackage.equalsIgnoreCase(eventHeader.getEventType()))
        {
            /*
             * We are not concerned by this request, perhaps another listener
             * is. So don't send a 489 / Bad event answer here.
             */
            return false;
        }

        if (!Request.NOTIFY.equals(request.getMethod()))
            return false;

        logger.debug("notify received");

        SubscriptionStateHeader sstateHeader
            = (SubscriptionStateHeader)
                request.getHeader(SubscriptionStateHeader.NAME);
        // notify must contain one (rfc3265)
        if (sstateHeader == null)
        {
            logger.error("no subscription state in this request");
            return false;
        }
        String sstate = sstateHeader.getState();

        ServerTransaction serverTransaction
            = getOrCreateServerTransaction(requestEvent);

        // first handle the case of a contact still pending
        // it's possible if the NOTIFY arrives before the OK
        CallIdHeader callIdHeader
            = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        Subscription subscription = getSubscription(callId);

        // see if the notify correspond to an existing subscription
        if ((subscription == null)
                && !SubscriptionStateHeader.TERMINATED.equalsIgnoreCase(sstate))
        {
            logger.debug("subscription not found for callId " + callId);

            // send a 481 response (rfc3625)
            Response response;
            try
            {
                response
                    = protocolProvider
                        .getMessageFactory()
                            .createResponse(
                                Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST,
                                request);
            }
            catch (ParseException e)
            {
                logger.error("failed to create the 481 response", e);
                return false;
            }

            try
            {
                serverTransaction.sendResponse(response);
            }
            catch (SipException e)
            {
                logger.error("failed to send the response", e);
            }
            catch (InvalidArgumentException e)
            {
                // should not happen
                logger.error(
                    "invalid argument provided while trying to send the response",
                    e);
            }
            return true;
        }

        // if we don't understand the content
        ContentTypeHeader ctheader
            = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
        if ((ctheader != null)
                && !ctheader.getContentSubType().equalsIgnoreCase(contentSubType))
        {
            // send a 415 response (rfc3261)
            Response response;
            try
            {
                response
                    = protocolProvider
                        .getMessageFactory()
                            .createResponse(
                                Response.UNSUPPORTED_MEDIA_TYPE,
                                request);
            }
            catch (ParseException e)
            {
                logger.error("failed to create the OK response", e);
                return false;
            }

            // we want PIDF
            AcceptHeader acceptHeader;
            try
            {
                acceptHeader
                    = protocolProvider
                        .getHeaderFactory()
                            .createAcceptHeader("application", contentSubType);
            }
            catch (ParseException e)
            {
                // should not happen
                logger.error("failed to create the accept header", e);
                return false;
            }
            response.setHeader(acceptHeader);

            try
            {
                serverTransaction.sendResponse(response);
            }
            catch (SipException e)
            {
                logger.error("failed to send the response", e);
            }
            catch (InvalidArgumentException e)
            {
                // should not happen
                logger.error("invalid argument provided while trying" +
                        " to send the response", e);
            }
        }

        // if the presentity doesn't want of us anymore
        if (SubscriptionStateHeader.TERMINATED.equalsIgnoreCase(sstate))
        {
            // if we requested this end of subscription, subscription == null
            if (subscription != null)
            {
                removeSubscription(callId, subscription);
                subscription
                    .processTerminatedRequest(
                        requestEvent,
                        sstateHeader.getReasonCode());
            }
        }

        // send an OK response
        Response response;
        try
        {
            response
                = protocolProvider
                    .getMessageFactory().createResponse(Response.OK, request);
        }
        catch (ParseException e)
        {
            logger.error("failed to create the OK response", e);
            return false;
        }

        try
        {
            serverTransaction.sendResponse(response);
        }
        catch (SipException e)
        {
            logger.error("failed to send the response", e);
        }
        catch (InvalidArgumentException e)
        {
            // should not happen
            logger.error(
                "invalid argument provided while trying to send the response",
                e);
        }

        // transform the presence document in new presence status
        if (subscription != null)
            subscription
                .processActiveRequest(requestEvent, request.getRawContent());

        return true;
    }

    /*
     * Implements MethodProcessor#processResponse(ResponseEvent). Handles only
     * responses to SUBSCRIBE requests because they are the only requests
     * concerning event package subscribers (and the only requests sent by them,
     * for that matter) and if the processing of a given response requires event
     * package-specific handling, delivers the response to the matching
     * Subscription instance. Examples of such event package-specific handling
     * include letting the respective Subscription handle the success or failure
     * in the establishment of a subscription.
     */
    public boolean processResponse(ResponseEvent responseEvent)
    {
        Response response = responseEvent.getResponse();

        CSeqHeader cseqHeader
            = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        if (cseqHeader == null)
        {
            logger.error("An incoming response did not contain a CSeq header");
            return false;
        }
        if (!Request.SUBSCRIBE.equals(cseqHeader.getMethod()))
            return false;

        // Find the subscription.
        CallIdHeader callIdHeader
            = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        Subscription subscription = getSubscription(callId);

        // if it's the response to an unsubscribe message, we just ignore it
        // whatever the response is however if we need to handle a
        // challenge, we do it
        ExpiresHeader expHeader = response.getExpires();
        int statusCode = response.getStatusCode();
        ClientTransaction clientTransaction
            = responseEvent.getClientTransaction();
        SipProvider sourceProvider = (SipProvider) responseEvent.getSource();
        if (((expHeader != null) && (expHeader.getExpires() == 0))
                || (subscription == null)) // this handle the unsubscription
                                           // case where we removed the contact
                                           // from subscribedContacts
        {
            boolean processed = false;

            if ((statusCode == Response.UNAUTHORIZED)
                    || (statusCode == Response.PROXY_AUTHENTICATION_REQUIRED))
            {
                try
                {
                    processAuthenticationChallenge(
                        clientTransaction,
                        response,
                        sourceProvider);
                    processed = true;
                }
                catch (OperationFailedException e)
                {
                    logger.error("can't handle the challenge", e);
                }
            }
            else if ((statusCode != Response.OK)
                    && (statusCode != Response.ACCEPTED))
                processed = true;

            // any other cases (200/202) will imply a NOTIFY, so we will
            // handle the end of a subscription there
            return processed;
        }

        if ((statusCode >= Response.OK)
                && (statusCode < Response.MULTIPLE_CHOICES))
        {
             // OK (200/202)
             if ((statusCode == Response.OK)
                     || (statusCode == Response.ACCEPTED))
             {
                 if (expHeader == null)
                 {
                     // not conform to rfc3265
                     logger.error("no Expires header in this response");
                     return false;
                 }

                 SubscriptionRefreshTask refreshTask
                     = new SubscriptionRefreshTask(subscription);
                 subscription.setTimerTask(refreshTask);

                 int refreshDelay = expHeader.getExpires();
                 // try to keep a margin
                 if (refreshDelay >= refreshMargin)
                     refreshDelay -= refreshMargin;
                 timer.schedule(refreshTask, refreshDelay * 1000);

                 // do it to remember the dialog in case of a polling
                 // subscription (which means no call to finalizeSubscription)
                 subscription.setDialog(clientTransaction.getDialog());

                 subscription.processSuccessResponse(responseEvent, statusCode);
             }
         }
         else if((statusCode >= Response.MULTIPLE_CHOICES)
                 && (statusCode < Response.BAD_REQUEST))
         {
             logger.info(
                 "Response to subscribe to " + subscription.getAddress()
                     + ": " + response.getReasonPhrase());
         }
         else if(statusCode >= Response.BAD_REQUEST)
         {
             // if the response is a 423 response, just re-send the request
             // with a valid expires value
             if (statusCode == Response.INTERVAL_TOO_BRIEF)
             {
                 MinExpiresHeader min = (MinExpiresHeader)
                     response.getHeader(MinExpiresHeader.NAME);

                 if (min == null)
                 {
                     logger.error("no minimal expires value in this 423 " +
                             "response");
                     return false;
                 }

                 Request request = responseEvent.getClientTransaction()
                     .getRequest();

                 ExpiresHeader exp = request.getExpires();

                 try
                 {
                     exp.setExpires(min.getExpires());
                 }
                 catch (InvalidArgumentException e)
                 {
                     logger.error("can't set the new expires value", e);
                     return false;
                 }

                 ClientTransaction transac = null;
                 try
                 {
                     transac
                         = protocolProvider
                             .getDefaultJainSipProvider()
                                 .getNewClientTransaction(request);
                 }
                 catch (TransactionUnavailableException e)
                 {
                     logger.error("can't create the client transaction", e);
                     return false;
                 }

                 try
                 {
                     transac.sendRequest();
                 }
                 catch (SipException e)
                 {
                     logger.error("can't send the new request", e);
                     return false;
                 }

                 return true;
             // UNAUTHORIZED (401/407)
             }
             else if ((statusCode == Response.UNAUTHORIZED)
                     || (statusCode == Response.PROXY_AUTHENTICATION_REQUIRED))
             {
                 try
                 {
                     processAuthenticationChallenge(
                         clientTransaction,
                         response,
                         sourceProvider);
                 }
                 catch (OperationFailedException e)
                 {
                     logger.error("can't handle the challenge", e);

                     removeSubscription(callId, subscription);
                     subscription
                         .processFailureResponse(responseEvent, statusCode);
                 }
             // 408 480 486 600 603 : non definitive reject
             // others: definitive reject (or not implemented)
             }
             else
             {
                 logger.debug("error received from the network:\n" + response);

                 removeSubscription(callId, subscription);
                 subscription.processFailureResponse(responseEvent, statusCode);
             }
        }

        return true;
    }

    /**
     * Removes a <code>Subscription</code> from the list of subscriptions
     * managed by this instance which is associated with a specific subscription
     * <code>Address</code>/Request URI and has an id tag in its Event header of
     * <tt>null</tt>. If such an instance is not found, does nothing.
     *
     * @param toAddress
     *            the subscription <code>Address</code>/Request URI of the
     *            <code>Subscription</code> to be removed
     */
    public void removeSubscription(Address toAddress)
    {
        removeSubscription(toAddress, null);
    }

    /**
     * Removes a <code>Subscription</code> from the list of subscriptions
     * managed by this instance which is associated with a specific subscription
     * <code>Address</code>/Request URI and has a specific id tag in its Event
     * header. If such an instance is not found, does nothing.
     *
     * @param toAddress
     *            the subscription <code>Address</code>/Request URI of the
     *            <code>Subscription</code> to be removed
     * @param eventId
     *            the id tag in the Event header of the
     *            <code>Subscription</code> to be removed; <tt>null</tt> if the
     *            <code>Subscription</code> should have no id tag in its Event
     *            header
     */
    public void removeSubscription(Address toAddress, String eventId)
    {
        synchronized (subscriptions)
        {
            for (Map.Entry<String, Subscription> entry
                    : subscriptions.entrySet())
            {
                Subscription subscription = entry.getValue();

                if (subscription.equals(toAddress, eventId))
                    removeSubscription(entry.getKey(), subscription);
            }
        }
    }

    /**
     * Removes a specific <code>Subscription</code> from the list of
     * subscriptions managed by this instance if it is associated with a
     * specific CallId. If the specified <code>Subscription</code> is not
     * associated with the specified CallId (including the case of no known
     * association for the specified CallId), does nothing.
     *
     * @param callId
     *            the CallId which is expected to be associated with the
     *            specified <code>Subscription</code>
     * @param subscription
     *            the <code>Subscription</code> to be removed from the list of
     *            subscriptions managed by this instance if it is associated
     *            with the specified CallId
     */
    private void removeSubscription(String callId, Subscription subscription)
    {
        synchronized (subscriptions)
        {
            Subscription subscriptionToRemove = subscriptions.get(callId);

            if ((subscriptionToRemove != null)
                    && subscriptionToRemove.equals(subscription))
            {
                subscription = subscriptions.remove(callId);
                subscription.removed();
            }
        }
    }

    /**
     * Creates and sends a SUBSCRIBE request to the subscription
     * <code>Address</code>/Request URI of a specific <code>Subscription</code>
     * in order to request receiving event notifications and adds the specified
     * <code>Subscription</code> to the list of subscriptions managed by this
     * instance. The added <code>Subscription</code> may later receive
     * notifications to process the <code>Request</code>s and/or
     * <code>Response</code>s which constitute the signaling session associated
     * with it. If the attempt to create and send the SUBSCRIBE request fails,
     * the specified <code>Subscription</code> is not added to the list of
     * subscriptions managed by this instance.
     *
     * @param subscription
     *            a <code>Subscription</code> which specifies the properties of
     *            the SUBSCRIBE request to be created and sent, to be added to
     *            the list of subscriptions managed by this instance
     * @throws OperationFailedException
     */
    public void subscribe(Subscription subscription)
        throws OperationFailedException
    {
        //create the subscription
        ClientTransaction subscribeTransaction;
        try
        {
            subscribeTransaction
                = createSubscription(subscription, subscriptionDuration);
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to create the subcription", ex);
            throw
                new OperationFailedException(
                        "Failed to create the subscription",
                        OperationFailedException.INTERNAL_ERROR);
        }

        // we register the contact to find him when the OK will arrive
        CallIdHeader callIdHeader
            = (CallIdHeader)
                subscribeTransaction.getRequest().getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        addSubscription(callId, subscription);

        // send the message
        try
        {
            subscribeTransaction.sendRequest();
        }
        catch (SipException ex)
        {
            // this contact will never been accepted or rejected
            removeSubscription(callId, subscription);

            logger.error("Failed to send the subscription.", ex);
            throw
                new OperationFailedException(
                        "Failed to send the subscription",
                        OperationFailedException.NETWORK_FAILURE);
        }
    }

    /**
     * Creates and sends a SUBSCRIBE request to a specific subscription
     * <code>Address</code>/Request URI if it matches a
     * <code>Subscription</code> with an id tag of its Event header of
     * <tt>null</tt> in the list of subscriptions managed by this instance with
     * an Expires header value of zero in order to terminate receiving event
     * notifications and removes the specified <code>Subscription</code> from
     * the list of subscriptions managed by this instance. The removed
     * <code>Subscription</code> may receive notifications to process the
     * <code>Request</code>s and/or <code>Response</code>s which constitute the
     * signaling session associated with it. If the attempt to create the
     * SUBSCRIBE request fails, the associated <code>Subscription</code> is not
     * removed from the list of subscriptions managed by this instance. If the
     * specified <code>Address</code> does not identify an existing
     * <code>Subscription</code> in the list of subscriptions managed by this
     * instance, an assertion may optionally be performed or no reaction can be
     * taken.
     *
     * @param toAddress
     *            a subscription <code>Address</code>/Request URI which
     *            identifies a <code>Subscription</code> to be removed from the
     *            list of subscriptions managed by this instance
     * @param assertSubscribed
     *            <tt>true</tt> to assert if the specified subscription
     *            <code>Address</code>/Request URI does not identify an existing
     *            <code>Subscription</code> in the list of subscriptions managed
     *            by this instance; <tt>false</tt> to not assert if the
     *            mentioned condition is met
     * @throws IllegalArgumentException
     *             if <code>assertSubscribed</code> is <tt>true</tt> and
     *             <code>toAddress</code> does not identify an existing
     *             <code>Subscription</code> in the list of subscriptions
     *             managed by this instance
     * @throws OperationFailedException
     */
    public void unsubscribe(Address toAddress, boolean assertSubscribed)
        throws IllegalArgumentException,
               OperationFailedException
    {
        unsubscribe(toAddress, null, assertSubscribed);
    }

    /**
     * Creates and sends a SUBSCRIBE request to a specific subscription
     * <code>Address</code>/Request URI if it matches a
     * <code>Subscription</code> with an id tag of its Event header of a
     * specific value in the list of subscriptions managed by this instance with
     * an Expires header value of zero in order to terminate receiving event
     * notifications and removes the specified <code>Subscription</code> from
     * the list of subscriptions managed by this instance. The removed
     * <code>Subscription</code> may receive notifications to process the
     * <code>Request</code>s and/or <code>Response</code>s which constitute the
     * signaling session associated with it. If the attempt to create the
     * SUBSCRIBE request fails, the associated <code>Subscription</code> is not
     * removed from the list of subscriptions managed by this instance. If the
     * specified <code>Address</code> does not identify an existing
     * <code>Subscription</code> in the list of subscriptions managed by this
     * instance, an assertion may optionally be performed or no reaction can be
     * taken.
     *
     * @param toAddress
     *            a subscription <code>Address</code>/Request URI which
     *            identifies a <code>Subscription</code> to be removed from the
     *            list of subscriptions managed by this instance
     * @param eventId
     *            the id tag placed in the Event header of the
     *            <code>Subscription</code> to be matched if there is one or
     *            <tt>null</tt> if the <code>Subscription</code> should have no
     *            id tag in its Event header
     * @param assertSubscribed
     *            <tt>true</tt> to assert if the specified subscription
     *            <code>Address</code>/Request URI does not identify an existing
     *            <code>Subscription</code> in the list of subscriptions managed
     *            by this instance; <tt>false</tt> to not assert if the
     *            mentioned condition is met
     * @throws IllegalArgumentException
     *             if <code>assertSubscribed</code> is <tt>true</tt> and
     *             <code>toAddress</code> and <code>eventId</code> do not
     *             identify an existing <code>Subscription</code> in the list of
     *             subscriptions managed by this instance
     * @throws OperationFailedException
     */
    public void unsubscribe(
            Address toAddress,
            String eventId,
            boolean assertSubscribed)
        throws IllegalArgumentException,
               OperationFailedException
    {
        Subscription subscription = getSubscription(toAddress, eventId);
        if (subscription == null)
            if (assertSubscribed)
                throw
                    new IllegalArgumentException(
                            "trying to unregister a not registered contact");
            else
                return;

        Dialog dialog = subscription.getDialog();

        // we stop the subscription if we're subscribed to this contact
        if (dialog != null)
        {
            String callId = dialog.getCallId().getCallId();

            ClientTransaction subscribeTransaction;
            try
            {
                subscribeTransaction
                    = createSubscription(subscription, dialog, 0);
            }
            catch (OperationFailedException e)
            {
                logger.debug("failed to create the unsubscription", e);
                throw e;
            }

            // we are not anymore subscribed to this contact
            // this ensure that the response of this request will be
            // handled as an unsubscription response
            removeSubscription(callId, subscription);

            try
            {
                dialog.sendRequest(subscribeTransaction);
            }
            catch (SipException e)
            {
                logger.debug("Can't send the request", e);
                throw
                    new OperationFailedException(
                            "Failed to send the subscription message",
                            OperationFailedException.NETWORK_FAILURE,
                            e);
            }
        }
    }

    /**
     * Represents a general event package subscription in the sense of RFC 3265
     * "Session Initiation Protocol (SIP)-Specific Event Notification" from the
     * point of view of the subscriber and its signaling characteristics such as
     * Request URI, id tag value of its Event header, the <code>Dialog</code>
     * which has been created by the associated SUBSCRIBE request or through
     * which it was sent. Additionally, represents the subscription-specific
     * processing of the related <code>Request</code>s and <code>Response</code>
     * s thus allowing implementers to tap into the general event package
     * subscription operations and provide the event package-specific
     * processing.
     *
     * @author Lubomir Marinov
     */
    public static abstract class Subscription
        extends EventPackageSupport.Subscription
    {

        /**
         * Initializes a new <code>Subscription</code> instance with a specific
         * subscription <code>Address</code>/Request URI and an id tag of the
         * associated Event headers of value <tt>null</tt>.
         *
         * @param toAddress
         *            the subscription <code>Address</code>/Request URI which is
         *            to be the target of the SUBSCRIBE requests associated with
         *            the new instance
         */
        public Subscription(Address toAddress)
        {
            this(toAddress, null);
        }

        /**
         * Initializes a new <code>Subscription</code> instance with a specific
         * subscription <code>Address</code>/Request URI and a specific id tag
         * of the associated Event headers.
         *
         * @param toAddress
         *            the subscription <code>Address</code>/Request URI which is
         *            to be the target of the SUBSCRIBE requests associated with
         *            the new instance
         * @param eventId
         *            the value of the id tag to be placed in the Event headers
         *            of the SUBSCRIBE requests created for the new instance and
         *            to be present in the received Event headers in order to
         *            have the new instance associated with them
         */
        public Subscription(Address toAddress, String eventId)
        {
            super(toAddress, eventId);
        }

        /**
         * Notifies this <code>Subscription</code> that an active NOTIFY
         * <code>Request</code> has been received and it may process the
         * specified raw content carried in it.
         *
         * @param requestEvent
         *            the <code>RequestEvent</code> carrying the full details of
         *            the received NOTIFY <code>Request</code> including the raw
         *            content which may be processed by this
         *            <code>Subscription</code>
         * @param rawContent
         *            an array of bytes which represents the raw content carried
         *            in the body of the received NOTIFY <code>Request</code>
         *            and extracted from the specified <code>RequestEvent</code>
         *            for the convenience of the implementers
         */
        protected abstract void processActiveRequest(
            RequestEvent requestEvent,
            byte[] rawContent);

        /**
         * Notifies this <code>Subscription</code> that a <code>Response</code>
         * to a previous SUBSCRIBE <code>Request</code> has been received with a
         * status code in the failure range and it may process the status code
         * carried in it.
         *
         * @param responseEvent
         *            the <code>ResponseEvent</code> carrying the full details
         *            of the received <code>Response</code> including the status
         *            code which may be processed by this
         *            <code>Subscription</code>
         * @param statusCode
         *            the status code carried in the <code>Response</code> and
         *            extracted from the specified <code>ResponseEvent</code>
         *            for the convenience of the implementers
         */
        protected abstract void processFailureResponse(
            ResponseEvent responseEvent,
            int statusCode);

        /**
         * Notifies this <code>Subscription</code> that a <code>Response</code>
         * to a previous SUBSCRIBE <code>Request</code> has been received with a
         * status code in the success range and it may process the status code
         * carried in it.
         *
         * @param responseEvent
         *            the <code>ResponseEvent</code> carrying the full details
         *            of the received <code>Response</code> including the status
         *            code which may be processed by this
         *            <code>Subscription</code>
         * @param statusCode
         *            the status code carried in the <code>Response</code> and
         *            extracted from the specified <code>ResponseEvent</code>
         *            for the convenience of the implementers
         */
        protected abstract void processSuccessResponse(
            ResponseEvent responseEvent,
            int statusCode);

        /**
         * Notifies this <code>Subscription</code> that a terminating NOTIFY
         * <code>Request</code> has been received and it may process the reason
         * code carried in it.
         *
         * @param requestEvent
         *            the <code>RequestEvent</code> carrying the full details of
         *            the received NOTIFY <code>Request</code> including the
         *            reason code which may be processed by this
         *            <code>Subscription</code>
         * @param reasonCode
         *            the code of the reason for the termination carried in the
         *            NOTIFY <code>Request</code> and extracted from the
         *            specified <code>RequestEvent</code> for the convenience of
         *            the implementers
         */
        protected abstract void processTerminatedRequest(
            RequestEvent requestEvent,
            String reasonCode);
    }

    /**
     * Represents a <code>TimerTask</code> which refreshes a specific
     * <code>Subscription</code>.
     */
    private class SubscriptionRefreshTask
        extends TimerTask
    {

        /**
         * The <code>Subscription</code> to be refreshed by this
         * <code>TimerTask</code>.
         */
        private final Subscription subscription;

        /**
         * Initializes a new <code>SubscriptionRefreshTask</code> which is to
         * refresh a specific <code>Subscription</code>.
         *
         * @param subscription
         *            the <code>Subscription</code> to be refreshed by the new
         *            instance
         */
        public SubscriptionRefreshTask(Subscription subscription)
        {
            this.subscription = subscription;
        }

        /**
         * Refreshes the <code>Subscription</code> associated with this
         * <code>TimerTask</code>.
         */
        public void run()
        {
            Dialog dialog = subscription.getDialog();

            if (dialog == null)
            {
                logger.warn(
                    "null dialog associated with "
                        + subscription
                        + ", can't refresh the subscription");
                return;
            }

            ClientTransaction transac = null;
            try
            {
                transac
                    = createSubscription(
                            subscription,
                            dialog,
                            subscriptionDuration);
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to create subscriptionTransaction.", e);
                return;
            }

            try
            {
                dialog.sendRequest(transac);
            }
            catch (SipException e)
            {
                logger.error("Can't send the request", e);
            }
        }
    }
}
