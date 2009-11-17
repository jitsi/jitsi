/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.*;

import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the notifier part of RFC 3265
 * "Session Initiation Protocol (SIP)-Specific Event Notification" and thus
 * eases the creation of event package-specific implementations.
 *
 * @author Lubomir Marinov
 * @author Benoit Pradelle
 * @author Emil Ivov
 */
public abstract class EventPackageNotifier
    extends EventPackageSupport
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(EventPackageNotifier.class);

    /**
     * The minimal Expires value for a SUBSCRIBE request.
     */
    private static final int SUBSCRIBE_MIN_EXPIRE = 120;

    /**
     * The list of subscriptions managed by this instance.
     */
    private final List<Subscription> subscriptions
        = new LinkedList<Subscription>();

    /**
     * A reference to the <tt>SipMessageFactory</tt> instance that we should
     * use when creating requests.
     */
    private final SipMessageFactory messageFactory;

    /**
     * Initializes a new <tt>EventPackageNotifier</tt> instance which is to
     * provide notifier support according to RFC 3265 to a specific SIP
     * <tt>ProtocolProviderService</tt> implementation for a specific event
     * package.
     *
     * @param protocolProvider the SIP <tt>ProtocolProviderService</tt>
     * implementation for which the new instance is to provide notifier support
     * for a specific event package
     * @param eventPackage the name of the event package the new instance is to
     * implement and carry in the Event and Allow-Events headers
     * @param subscriptionDuration the duration of each subscription to be
     * managed by the new instance and to be carried in the Expires headers
     * @param contentSubType the sub-type of the content type of the NOTIFY
     * bodies to be announced, expected and supported by the subscriptions to be
     * managed by the new instance
     * @param timer the <tt>Timer</tt> support which is to time out the
     * subscriptions to be managed by the new instance
     */
    public EventPackageNotifier(
        ProtocolProviderServiceSipImpl protocolProvider,
        String eventPackage,
        int subscriptionDuration,
        String contentSubType,
        TimerScheduler timer)
    {
        super( protocolProvider, eventPackage, subscriptionDuration,
                        contentSubType, timer);

        this.messageFactory = protocolProvider.getMessageFactory();
    }

    /**
     * Adds a specific <tt>Subscription</tt> to this list of subscriptions
     * managed by this instance. If a <tt>Subscription</tt> with matching
     * <tt>Address</tt>/Request URI and EventId tag exists in the list of
     * subscriptions managed by this instance already, that matching
     * <tt>Subscription</tt> is removed before the specified
     * <tt>Subscription</tt> is added.
     *
     * @param subscription the <tt>Subscription</tt> to be added to the list of
     * subscriptions managed by this instance
     */
    private void addSubscription(Subscription subscription)
    {
        synchronized (subscriptions)
        {
            Address address = subscription.getAddress();
            String eventId = subscription.getEventId();

            for (Subscription s : subscriptions)
                if (s.equals(address, eventId))
                {
                    removeSubscription(s);
                    break;
                }

            subscriptions.add(subscription);
        }
    }

    /**
     * Creates a NOTIFY request which is to notify about a specific subscription
     * state and carry a specific content. This request MUST be sent using
     * <tt>Dialog#sendRequest()</tt>.
     *
     * @param dialog the <tt>Dialog</tt> to create the NOTIFY request in
     * @param content the content to be carried by the NOTIFY request to be
     * created
     * @param subscriptionState the subscription state
     * @param reason the reason for the specified subscription state;
     * <tt>null</tt> for no reason
     *
     * @return a valid <tt>ClientTransaction</tt> ready to send the request
     *
     * @throws OperationFailedException if something goes wrong during the
     * creation of the request
     */
    private ClientTransaction createNotify( Dialog dialog,
                                            byte[] content,
                                            String subscriptionState,
                                            String reason)
        throws OperationFailedException
    {
        Request req = messageFactory.createRequest(dialog, Request.NOTIFY);

        // Address
        Address toAddress = dialog.getRemoteTarget();
        // no Contact field
        if (toAddress == null)
            toAddress = dialog.getRemoteParty();

        ArrayList<ViaHeader> viaHeaders;
        MaxForwardsHeader maxForwards;
        try
        {
            //ViaHeaders
            viaHeaders = protocolProvider.getLocalViaHeaders(toAddress);

            //MaxForwards
            maxForwards = protocolProvider.getMaxForwardsHeader();
        }
        catch (OperationFailedException e)
        {
            logger.error(
                "Can't retrive the via headers or the max forwards header",
                e);
            throw
                new OperationFailedException(
                    "Can't retrive the via headers or the max forwards header",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }

        EventHeader evHeader;
        try
        {
            evHeader
                = protocolProvider
                    .getHeaderFactory()
                        .createEventHeader(eventPackage);
        }
        catch (ParseException e)
        {
            //these two should never happen.
            logger.error("Can't create the Event header", e);
            throw
                new OperationFailedException(
                        "Can't create the Event header",
                        OperationFailedException.INTERNAL_ERROR,
                        e);
        }

        // Subscription-State
        SubscriptionStateHeader sStateHeader;
        try
        {
            sStateHeader = protocolProvider.getHeaderFactory()
                .createSubscriptionStateHeader(subscriptionState);

            if ((reason != null) && (reason.trim().length() != 0))
                sStateHeader.setReasonCode(reason);
        }
        catch (ParseException e)
        {
            // should never happen
            logger.error("can't create the Subscription-State header", e);
            throw
                new OperationFailedException(
                        "Can't create the Subscription-State header",
                        OperationFailedException.INTERNAL_ERROR,
                        e);
        }

        // Content-type
        ContentTypeHeader cTypeHeader;
        try
        {
            cTypeHeader = protocolProvider.getHeaderFactory()
                        .createContentTypeHeader("application", contentSubType);
        }
        catch (ParseException e)
        {
            // should never happen
            logger.error("can't create the Content-Type header", e);
            throw
                new OperationFailedException(
                        "Can't create the Content-Type header",
                        OperationFailedException.INTERNAL_ERROR,
                        e);
        }

        req.setHeader(maxForwards);
        req.setHeader(evHeader);
        req.setHeader(sStateHeader);

        /*
         * Create the transaction (then add the via header as recommended by the
         * jain-sip documentation at
         * http://snad.ncsl.nist.gov/proj/iptel/jain-sip
         * -1.2/javadoc/javax/sip/Dialog.html#createRequest(java.lang.String)).
         */
        ClientTransaction transac;
        try
        {
            transac = protocolProvider.getDefaultJainSipProvider()
                .getNewClientTransaction(req);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error( "Failed to create subscriptionTransaction."
                    + " This is most probably a network connection error.", ex);
            throw new OperationFailedException(
                        "Failed to create subscriptionTransaction.",
                        OperationFailedException.NETWORK_FAILURE,
                        ex);
        }

        req.setHeader(viaHeaders.get(0));

        // add the content
        try
        {
            req.setContent(content, cTypeHeader);
        }
        catch (ParseException e)
        {
            logger.error("Failed to add the presence document", e);
            throw new OperationFailedException(
                        "Can't add the presence document to the request",
                        OperationFailedException.INTERNAL_ERROR,
                        e);
        }

        return transac;
    }

    /**
     * Creates a NOTIFY request within a specific <tt>Dialog</tt> which is to
     * notify about a specific subscription state and carry a specific content.
     * This request MUST be sent using <tt>Dialog#sendRequest()</tt>.
     *
     * @param dialog the <tt>Dialog</tt> to create the NOTIFY request in
     * @param subscription the <tt>Subscription</tt> associated with the NOTIFY
     * request to be created
     * @param subscriptionState the subscription state
     * @param reason the reason for the specified subscription state;
     * <tt>null</tt> for no reason
     *
     * @return a valid <tt>ClientTransaction</tt> ready to send the request
     *
     * @throws OperationFailedException if something goes wrong during the
     * creation of the request
     */
    private ClientTransaction createNotify( Dialog dialog,
                                            Subscription subscription,
                                            String subscriptionState,
                                            String reason)
        throws OperationFailedException
    {
        if (dialog == null)
        {
            dialog = subscription.getDialog();
            if (dialog == null)
                throw new OperationFailedException(
                            "the dialog of the subscription is null",
                            OperationFailedException.INTERNAL_ERROR);
        }

        return
            createNotify( dialog,
                subscription.createNotifyContent(subscriptionState, reason),
                subscriptionState, reason);
    }

    /**
     * Creates a new <tt>Subscription</tt> instance which is to represent the
     * subscription signaling from and to a specific target identified by its
     * <tt>Address</tt>/Request URI and a specific EventId tag.
     *
     * @param fromAddress the <tt>Address</tt>/Request URI of the subscription
     * target
     * @param eventId the EventId tag of the subscription signaling associated
     * with the specified target
     *
     * @return a new <tt>Subscription</tt> instance which represents the
     * specified subscription target
     */
    protected abstract Subscription createSubscription(
        Address fromAddress,
        String eventId);

    /**
     * Gets the <tt>Subscription</tt> from the list of subscriptions managed by
     * this instance which is associated with a specific subscription
     * <tt>Address</tt>/Request URI and has a specific id tag in its Event
     * header.
     *
     * @param fromAddress the subscription <tt>Address</tt>/Request URI of the
     * <tt>Subscription</tt> to be retrieved
     * @param eventId the id tag placed in the Event header of the
     * <tt>Subscription</tt> to be retrieved if there is one or <tt>null</tt> if
     * the <tt>Subscription</tt> should have no id tag in its Event header
     *
     * @return an existing <tt>Subscription</tt> from the list of subscriptions
     * managed by this instance with the specified subscription <tt>Address</tt>
     * /Request URI and the specified id tag in its Event header; <tt>null</tt>
     * if no such <tt>Subscription</tt> exists in the list of subscriptions
     * managed by this instance
     */
    private Subscription getSubscription(Address fromAddress, String eventId)
    {
        synchronized (subscriptions)
        {
            for (Subscription subscription : subscriptions)
                if (subscription.equals(fromAddress, eventId))
                    return subscription;
        }
        return null;
    }

    /**
     * Gets a new copy of the list of <tt>Subscription</tt>s managed by this
     * instance.
     *
     * @return a new copy of the list of <tt>Subscription</tt>s managed by this
     * instance; if this instance currently manages no <tt>Subscription</tt>s,
     * an empty array of <tt>Subscription</tt> element type
     */
    private Subscription[] getSubscriptions()
    {
        synchronized (subscriptions)
        {
            return
                subscriptions.toArray(new Subscription[subscriptions.size()]);
        }
    }

    /**
     * Notifies a specific target identified by its <tt>Subscription</tt> about
     * a specific subscription state and a specific reason for that subscription
     * state via a NOTIFY request.
     *
     * @param subscription the target identified by its <tt>Subscription</tt> to
     * be notified about the specified subscription state and the specified
     * reason for that subscription state via a NOTIFY request
     * @param subscriptionState the subscription state to notify the target
     * represented by its <tt>Subscription</tt> about
     * @param reason the reason for that subscription state
     *
     * @throws OperationFailedException
     */
    public void notify( Subscription subscription,
                        String subscriptionState,
                        String reason)
        throws OperationFailedException
    {
        Dialog dialog = subscription.getDialog();
        ClientTransaction transac
            = createNotify(dialog, subscription, subscriptionState, reason);

        try
        {
            dialog.sendRequest(transac);
        }
        catch (SipException sex)
        {
            logger.error("Failed to send NOTIFY request.", sex);
            throw new OperationFailedException(
                        "Failed to send NOTIFY request.",
                        OperationFailedException.NETWORK_FAILURE,
                        sex);
        }

        if (SubscriptionState.TERMINATED.equals(subscriptionState))
            removeSubscription(subscription);
    }

    /**
     * Notifies all targets represented by the <tt>Subscription</tt>s managed by
     * this instance about a specific subscription state and a specific reason
     * for that subscription state via NOTIFY requests.
     *
     * @param subscriptionState the subscription state to be sent to all targets
     * represented by the <tt>Subscription</tt>s managed by this instance via
     * NOTIFY requests
     * @param reason the reason for the specified subscription state
     *
     * @throws OperationFailedException if anything goes wrong while sending out
     * the notifications.
     */
    public void notifyAll(String subscriptionState, String reason)
        throws OperationFailedException
    {
        notifyAll(subscriptionState, reason, null);
    }

    /**
     * Notifies all targets represented by the <tt>Subscription</tt>s managed by
     * this instance which are accepted by a specific
     * <tt>SubscriptionFilter</tt> about a specific subscription state and a
     * specific reason for that subscription state via NOTIFY requests.
     *
     * @param subscriptionState the subscription state to be sent to all targets
     * represented by the <tt>Subscription</tt>s managed by this instance which
     * are accepted by <tt>filter</tt> via NOTIFY requests
     * @param reason the reason for the specified subscription state
     * @param filter the <tt>SubscriptionFilter</tt> to pick up the
     * <tt>Subscription</tt>s managed by this instance to be notified about
     * <tt>subscriptionState</tt>
     * @throws OperationFailedException if anything goes wrong while sending out
     * the notifications
     */
    public void notifyAll(
            String subscriptionState,
            String reason,
            SubscriptionFilter filter)
        throws OperationFailedException
    {
        for (Subscription subscription : getSubscriptions())
            if ((filter == null) || filter.accept(subscription))
                notify(subscription, subscriptionState, reason);
    }

    /**
     * Processes incoming subscribe requests.
     *
     * @param requestEvent the event containing the request we need to handle
     * @return <tt>true</tt> if we have handled and thus consumed the request;
     * <tt>false</tt>, otherwise
     * @see MethodProcessor#processRequest(RequestEvent)
     */
    @Override
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

        if (!Request.SUBSCRIBE.equals(request.getMethod()))
            return false;

        ServerTransaction serverTransaction
            = getOrCreateServerTransaction(requestEvent);
        if (serverTransaction == null)
            return false;

        ExpiresHeader expHeader = request.getExpires();
        int expires
            = (expHeader == null)
                ? subscriptionDuration
                : expHeader.getExpires();

        // interval too brief
        if ((expires < SUBSCRIBE_MIN_EXPIRE) && (expires > 0))
        {
            // send him a 423
            Response response;
            try
            {
                response = protocolProvider.getMessageFactory()
                    .createResponse( Response.INTERVAL_TOO_BRIEF, request);
            }
            catch (Exception e)
            {
                logger.error("Error while creating the response 423", e);
                return false;
            }

            MinExpiresHeader min;
            try
            {
                min = protocolProvider .getHeaderFactory()
                            .createMinExpiresHeader(SUBSCRIBE_MIN_EXPIRE);
            }
            catch (InvalidArgumentException e)
            {
                // should not happen
                logger.error("can't create the min expires header", e);
                return false;
            }
            response.setHeader(min);

            try
            {
                serverTransaction.sendResponse(response);
            }
            catch (Exception e)
            {
                logger.error("Error while sending the response 423", e);
                return false;
            }

            return true;
        }

        FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
        Address fromAddress = fromHeader.getAddress();
        String eventId = eventHeader.getEventId();
        Subscription subscription = getSubscription(fromAddress, eventId);

        /*
         * Is it a subscription refresh? (No need to synchronize the access to
         * subscriptions: read only operation.)
         */
        if ((subscription != null) && (expires != 0))
        {
            Dialog dialog = subscription.getDialog();

            if (dialog.equals(serverTransaction.getDialog()))
            {
                // add the new timeout task
                SubscriptionTimeoutTask timeout
                    = new SubscriptionTimeoutTask(subscription);
                subscription.setTimerTask(timeout);
                timer.schedule(timeout, expires * 1000);

                // send a OK
                Response response;
                try
                {
                    response = protocolProvider.getMessageFactory()
                                .createResponse(Response.OK, request);
                }
                catch (Exception e)
                {
                    logger.error("Error while creating the response 200", e);
                    return false;
                }

                // add the expire header
                try
                {
                    expHeader = protocolProvider.getHeaderFactory()
                                .createExpiresHeader(expires);
                }
                catch (InvalidArgumentException e)
                {
                    logger.error("Can't create the expires header");
                    return false;
                }
                response.setHeader(expHeader);

                try
                {
                    serverTransaction.sendResponse(response);
                }
                catch (Exception e)
                {
                    logger.error("Error while sending the response 200", e);
                    return false;
                }

                return true;
            }
            else
            {
                /*
                 * If the contact was already subscribed, we close the last
                 * subscription before accepting the new one.
                 */
                logger.debug("refreshing subscription "
                    + subscription+ ", we will remove the first subscription");

                // terminate the subscription with a closing NOTIFY
                ClientTransaction transac = null;
                try
                {
                    transac = createNotify( dialog, subscription,
                            SubscriptionStateHeader.TERMINATED,
                            SubscriptionStateHeader.REJECTED);
                }
                catch (OperationFailedException e)
                {
                    logger.error("failed to create the new notify", e);
                    return false;
                }

                removeSubscription(subscription);

                try
                {
                    dialog.sendRequest(transac);
                }
                catch (Exception e)
                {
                    logger.error("Can't send the request", e);
                    return false;
                }
            }
        }

        if (subscription == null)
            subscription = createSubscription(fromAddress, eventId);

        // Remember the dialog we will use to send the NOTIFYs.
        Dialog dialog;
        synchronized (subscription)
        {
            subscription.setDialog(serverTransaction.getDialog());
            dialog = subscription.getDialog();
        }

        // is it a subscription end ?
        if (expires == 0)
        {
            // remove the subscription
            removeSubscription(subscription);

            // send him OK
            Response response;
            try
            {
                response = protocolProvider.getMessageFactory()
                            .createResponse(Response.OK, request);
            }
            catch (Exception e)
            {
                logger.error("Error while creating the response 200", e);
                return false;
            }

            // add the expire header
            try
            {
                expHeader = protocolProvider
                        .getHeaderFactory().createExpiresHeader(0);
            }
            catch (InvalidArgumentException e)
            {
                logger.error("Can't create the expires header", e);
                return false;
            }
            response.setHeader(expHeader);

            try
            {
                serverTransaction.sendResponse(response);
            }
            catch (Exception e)
            {
                logger.error("Error while sending the response 200", e);
                return false;
            }

            // then terminate the subscription with an ultimate NOTIFY
            ClientTransaction transac = null;
            try
            {
                transac = createNotify( dialog, subscription,
                        SubscriptionStateHeader.TERMINATED,
                        SubscriptionStateHeader.TIMEOUT);
            }
            catch (OperationFailedException e)
            {
                logger.error("failed to create the new notify", e);
                return false;
            }

            try
            {
                dialog.sendRequest(transac);
            }
            catch (Exception e)
            {
                logger.error("Can't send the request", e);
                return false;
            }

            return true;
        }

        // immediately send a 200 / OK
        Response response;
        try
        {
            response = protocolProvider
                    .getMessageFactory().createResponse(Response.OK, request);
        }
        catch (Exception e)
        {
            logger.error("Error while creating the response 200", e);
            return false;
        }

        // add the expire header
        try
        {
            expHeader = protocolProvider
                    .getHeaderFactory().createExpiresHeader(expires);
        }
        catch (InvalidArgumentException e)
        {
            logger.error("Can't create the expires header", e);
            return false;
        }
        response.setHeader(expHeader);

        try
        {
            serverTransaction.sendResponse(response);
        }
        catch (Exception e)
        {
            logger.error("Error while sending the response 200", e);
            return false;
        }

        addSubscription(subscription);

        // send a NOTIFY
        ClientTransaction transac;
        try
        {
            transac = createNotify( dialog, subscription,
                    SubscriptionStateHeader.ACTIVE, null);
        }
        catch (OperationFailedException e)
        {
            logger.error("failed to create the new notify", e);
            return false;
        }

        try
        {
            dialog.sendRequest(transac);
        }
        catch (Exception e)
        {
            logger.error("Can't send the request", e);
            return false;
        }

        // add the timeout task
        SubscriptionTimeoutTask timeout
            = new SubscriptionTimeoutTask(subscription);
        subscription.setTimerTask(timeout);
        timer.schedule(timeout, expires * 1000);
        return true;
    }

    /**
     * Handles an incoming response to a request we'vre previously sent.
     *
     * @param responseEvent the event we need to handle
     * @return <tt>true</tt> in case we've handles and thus consumed the event;
     * <tt>false</tt>, otherwise
     * @see MethodProcessor#processResponse(ResponseEvent)
     */
    @Override
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
        if (!Request.NOTIFY.equals(cseqHeader.getMethod()))
            return false;

        ClientTransaction clientTransaction
            = responseEvent.getClientTransaction();

        /*
         * Make sure we're working only on responses to NOTIFY requests
         * we're interested in.
         */
        Request notifyRequest = clientTransaction.getRequest();
        String eventId = null;
        if (notifyRequest != null)
        {
            EventHeader eventHeader =
                (EventHeader) notifyRequest.getHeader(EventHeader.NAME);

            if ((eventHeader == null)
                    || !eventPackage
                            .equalsIgnoreCase(eventHeader.getEventType()))
                return false;

            eventId = eventHeader.getEventId();
        }

        switch (response.getStatusCode())
        {
        case Response.OK:
            /*
             * Simply nothing to do here because the target received our NOTIFY
             * i.e. everything is OK.
             */
            break;

        case Response.UNAUTHORIZED:
        case Response.PROXY_AUTHENTICATION_REQUIRED:
            try
            {
                processAuthenticationChallenge(
                    clientTransaction,
                    response,
                    (SipProvider) responseEvent.getSource());
            }
            catch (OperationFailedException e)
            {
                logger.error("can't handle the challenge", e);

                // don't try to tell him anything more
                removeSubscription(response, eventId, clientTransaction);
            }
            break;

        default:

            /*
             * Any error causes the subscription to be removed as
             * recommended in rfc3265.
             */
            logger.debug("error received from the network" + response);

            removeSubscription(response, eventId, clientTransaction);
            break;
        }

        return true;
    }

    /**
     * Removes a <tt>Subscription</tt> from the list of subscriptions managed by
     * this instance identified by the <tt>Response</tt> to our NOTIFY request.
     * If the specified <tt>Response</tt> cannot identify such a
     * <tt>Subscription</tt>, does nothing.
     *
     * @param response a <tt>Response</tt> identifying the <tt>Subscription</tt>
     * to be removed from the list of subscriptions managed by this instance
     * @param eventId
     * @param clientTransaction the <tt>ClientTransaction</tt> through which the
     * specified <tt>Response</tt> came
     */
    private void removeSubscription(
        Response response,
        String eventId,
        ClientTransaction clientTransaction)
    {
        FromHeader fromHeader
            = (FromHeader) response.getHeader(FromHeader.NAME);
        Address fromAddress = fromHeader.getAddress();
        Subscription subscription = getSubscription(fromAddress, eventId);

        if (subscription != null)
        {
            /*
             * Avoid the case where we receive an error after having closed an
             * old subscription before accepting a new one from the same
             * contact.
             */
            synchronized (subscription)
            {
                if (subscription.getDialog()
                        .equals(clientTransaction.getDialog()))
                    removeSubscription(subscription);
            }
        }
    }

    /**
     * Removes a specific <tt>Subscription</tt> from the list of subscriptions
     * managed by this instance. If the specified <tt>Subscription</tt> is
     * contained in the list of subscriptions managed by this instance, it is
     * removed and then its <tt>Subscription#removed()</tt> method is called to
     * notify it that it has been removed from the list of subscriptions of its
     * containing <tt>EventPackageNotifier</tt>. If the specified
     * <tt>Subscription</tt> is not contained in the list of subscriptions
     * managed by this instance, does nothing.
     *
     * @param subscription the <tt>Subscription</tt> to be removed from the list
     * of subscriptions managed by this instance
     */
    private void removeSubscription(Subscription subscription)
    {
        synchronized (subscriptions)
        {
            if (subscriptions.remove(subscription))
                subscription.removed();
        }
    }

    /**
     * Represents a general event package subscription in the sense of RFC 3265
     * "Session Initiation Protocol (SIP)-Specific Event Notification" from the
     * point of view of the notifier and its signaling characteristics such as
     * Request URI, id tag value of its Event header, the <tt>Dialog</tt>
     * which has been created by the associated SUBSCRIBE request or through
     * which NOTIFY requests are to be sent. Additionally, represents the
     * subscription-specific processing of the related <tt>Request</tt>s and
     * <tt>Response</tt> s thus allowing implementers to tap into the
     * general event package subscription operations and provide the event
     * package-specific processing.
     *
     * @author Lubomir Marinov
     */
    public static abstract class Subscription
        extends EventPackageSupport.Subscription
    {

        /**
         * Initializes a new <tt>Subscription</tt> instance with a specific
         * subscription <tt>Address</tt>/Request URI and a specific id tag of
         * the associated Event headers.
         *
         * @param fromAddress the subscription <tt>Address</tt>/Request URI
         * which is to be the target of the NOTIFY requests associated with the
         * new instance
         * @param eventId the value of the id tag to be placed in the Event
         * headers of the NOTIFY requests created for the new instance and to be
         * present in the received Event headers in order to have the new
         * instance associated with them
         */
        public Subscription(Address fromAddress, String eventId)
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
         */
        protected abstract byte[] createNotifyContent(
            String subscriptionState,
            String reason);
    }

    /**
     * Represents a filter for <tt>Subscription</tt>s i.e. it determines whether
     * a specific <tt>Subscription</tt> is to be selected or deselected by the
     * caller of the filter.
     */
    public interface SubscriptionFilter
    {

        /**
         * Determines whether the specified <tt>Subscription</tt> is accepted by
         * this <tt>SubscriptionFilter</tt> i.e. whether the caller of this
         * instance is to include or exclude the specified <tt>Subscription</tt>
         * from their list of processed <tt>Subscription</tt>s.
         *
         * @param subscription the <tt>Subscription</tt> to be checked
         * @return <tt>true</tt> if this <tt>SubscriptionFilter</tt> accepts
         * the specified <tt>subscription</tt> i.e. the caller of this instance
         * should include <tt>subscription</tt> in their list of processed
         * <tt>Subscription</tt>s; otherwise, <tt>false</tt>
         */
        public boolean accept(Subscription subscription);
    }

    /**
     * Represents a <tt>TimerTask</tt> which times out a specific
     * <tt>Subscription</tt> when its subscription duration expires.
     */
    private class SubscriptionTimeoutTask
        extends TimerTask
    {

        /**
         * The <tt>Subscription</tt> to be timed out by this
         * <tt>TimerTask</tt>.
         */
        private final Subscription subscription;

        /**
         * Initializes a new <tt>SubscriptionTimeoutTask</tt> instance which is
         * to time out a specific <tt>Subscription</tt>.
         *
         * @param subscription the <tt>Subscription</tt> to be timed out by the
         * new instance
         */
        public SubscriptionTimeoutTask(Subscription subscription)
        {
            this.subscription = subscription;
        }

        /**
         * Sends a closing NOTIFY to the target represented by the associated
         * <tt>Subscription</tt> and removes the associated
         * <tt>Subscription</tt>.
         */
        public void run()
        {
            Dialog dialog = subscription.getDialog();

            if (dialog == null)
            {
                logger.warn(
                    "null dialog associated with "
                        + subscription
                        + ", can't send the closing NOTIFY");
                return;
            }

            try
            {
                EventPackageNotifier.this.notify(
                    subscription,
                    SubscriptionStateHeader.TERMINATED,
                    SubscriptionStateHeader.TIMEOUT);
            }
            catch (OperationFailedException ofex)
            {
                logger.error(
                    "Failed to timeout subscription " + subscription,
                    ofex);
            }
        }
    }
}
