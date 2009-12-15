/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Provides the base for implementations of RFC 3265
 * "Session Initiation Protocol (SIP)-Specific Event Notification" and thus
 * eases the creation of event package-specific implementations.
 * 
 * @author Lubomir Marinov
 */
public class EventPackageSupport
    extends MethodProcessorAdapter
{

    /**
     * The <tt>Logger</tt> used by the <tt>EventPackageSupport</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(EventPackageSupport.class);

    /**
     * The sub-type of the content type of the response bodies announced,
     * expected and supported by the subscriptions/notifications managed by this
     * instance.
     */
    protected final String contentSubType;

    /**
     * The name of the event package this instance implements and carried in the
     * Event and Allow-Events headers.
     */
    protected final String eventPackage;

    /**
     * The SIP <code>ProtocolProviderService</code> implementation for which
     * this instance provides support for a specific event package.
     */
    protected final ProtocolProviderServiceSipImpl protocolProvider;

    /**
     * The duration of each subscription managed by this instance and carried in
     * the Expires header. Subscribers will interpret it as the value to be
     * announced in the SIP signaling related to SUBSCRIBE requests originating
     * from them, notifiers will use it as the default value for SUBSCRIBE
     * requests coming to them which do not specify an explicit value in the
     * Expires headers.
     */
    protected final int subscriptionDuration;

    /**
     * The <code>Timer</code> support which executes the time-based tasks of
     * this instance.
     */
    protected final TimerScheduler timer;

    /**
     * Initializes a new <code>EventPackageSupport</code> instance which is to
     * provide support according to RFC 3265 to a specific SIP
     * <code>ProtocolProviderService</code> implementation for a specific event
     * package.
     * 
     * @param protocolProvider
     *            the SIP <code>ProtocolProviderService</code> implementation
     *            for which the new instance is to provide support for a
     *            specific event package
     * @param eventPackage
     *            the name of the event package the new instance is to implement
     *            and carry in the Event and Allow-Events headers
     * @param subscriptionDuration
     *            the duration of each subscription to be managed by the new
     *            instance and to be carried in the Expires headers. Subscribers
     *            will interpret it as the value to be announced in the SIP
     *            signaling related to SUBSCRIBE requests originating from them,
     *            notifiers will use it as the default value for SUBSCRIBE
     *            requests coming to them which do not specify an explicit value
     *            in the Expires headers.
     * @param contentSubType
     *            the sub-type of the content type of the response bodies to be
     *            announced, expected and supported by the
     *            subscriptions/notifications to be managed by the new instance
     * @param timer
     *            the <code>Timer</code> support which is to execute the
     *            time-based tasks of the new instance
     */
    public EventPackageSupport(
        ProtocolProviderServiceSipImpl protocolProvider,
        String eventPackage,
        int subscriptionDuration,
        String contentSubType,
        TimerScheduler timer)
    {
        this.protocolProvider = protocolProvider;
        this.eventPackage = eventPackage;
        this.subscriptionDuration = subscriptionDuration;
        this.contentSubType = contentSubType;
        this.timer = (timer != null) ? timer : new TimerScheduler();

        this.protocolProvider.registerEvent(this.eventPackage);

        /*
         * XXX Registering this instance as a MethodProcessor this early is a
         * bit against the general rules because its class is meant to be
         * extended and the runtime instance may not be completely ready to
         * operate at this point. However, going against the general rules here
         * is unlikely to be a problem because this instance is fully created
         * before its put into use by the rest of the code. Anyway, it removes
         * duplication from extending classes.
         */
        this.protocolProvider.registerMethodProcessor(Request.SUBSCRIBE, this);
        this.protocolProvider.registerMethodProcessor(Request.NOTIFY, this);
    }

    /**
     * Safely returns the <code>ServerTransaction</code> associated with a
     * specific <code>RequestEvent</code> or creates a new one if the specified
     * <code>RequestEvent</code> is not associated with one. Does not throw
     * <code>TransactionAlreadyExistsException</code> and
     * <code>TransactionUnavailableException</code> but rather logs these
     * exceptions if they occur and returns <tt>null</tt>.
     * 
     * @param requestEvent
     *            the <code>RequestEvent</code> to get the associated
     *            <code>ServerTransaction</code> of
     * @return the <code>ServerTransaction</code> carried by the specified
     *         <code>RequestEvent</code> if the carried value in question is not
     *         <tt>null</tt>; a new <code>ServerTransaction</code> initializes
     *         with the <code>Request</code> carried by the specified
     *         <code>RequestEvent</code> if the event in question carries a
     *         <tt>null</tt> <code>ServerTransaction</code>; <tt>null</tt> in
     *         case of an exception
     */
    static ServerTransaction getOrCreateServerTransaction(
        RequestEvent requestEvent)
    {
        ServerTransaction serverTransaction = null;

        try
        {
            serverTransaction
                = SipStackSharing.getOrCreateServerTransaction(requestEvent);
        }
        catch (TransactionAlreadyExistsException ex)
        {
            //let's not scare the user and only log a message
            logger.error("Failed to create a new server"
                + "transaction for an incoming request\n"
                + "(Next message contains the request)"
                , ex);
        }
        catch (TransactionUnavailableException ex)
        {
            //let's not scare the user and only log a message
            logger.error("Failed to create a new server"
                + "transaction for an incoming request\n"
                + "(Next message contains the request)"
                , ex);
        }
        return serverTransaction;
    }

    /**
     * Attempts to re-generate a <code>Request</code> within a specific
     * <code>ClientTransaction</code> with the proper authorization headers.
     * 
     * @param clientTransaction
     *            the <code>ClientTransaction</code> which was challenged to
     *            authenticate
     * @param response
     *            the challenging <code>Response</code>
     * @param jainSipProvider
     *            the provider which received the authentication challenge
     * @throws OperationFailedException
     *             if processing the authentication challenge failed
     */
    protected void processAuthenticationChallenge(
            ClientTransaction clientTransaction,
            Response response,
            SipProvider jainSipProvider)
        throws OperationFailedException
    {
        processAuthenticationChallenge(
            protocolProvider,
            clientTransaction,
            response,
            jainSipProvider);
    }

    /**
     * Attempts to re-generate a <code>Request</code> within a specific
     * <code>ClientTransaction</code> with the proper authorization headers.
     * 
     * @param protocolProvider
     *            the SIP <code>ProtocolProviderService</code> implementation
     *            which received the authentication challenge and which is to
     *            re-generate and send the respective <code>Request</code>
     * @param clientTransaction
     *            the <code>ClientTransaction</code> which was challenged to
     *            authenticate
     * @param response
     *            the challenging <code>Response</code>
     * @param jainSipProvider
     *            the provider which received the authentication challenge
     * @throws OperationFailedException
     *             if processing the authentication challenge failed
     */
    static void processAuthenticationChallenge(
            ProtocolProviderServiceSipImpl protocolProvider,
            ClientTransaction clientTransaction,
            Response response,
            SipProvider jainSipProvider)
        throws OperationFailedException
    {
        try
        {
            logger.debug("Authenticating a message request.");

            ClientTransaction retryTran
                = protocolProvider
                    .getSipSecurityManager()
                        .handleChallenge(
                            response,
                            clientTransaction,
                            jainSipProvider);

            if(retryTran == null)
            {
                logger.trace("No password supplied or error occured!");
                return;
            }

            retryTran.sendRequest();
        }
        catch (Exception exc)
        {
            logger.error("We failed to authenticate a message request.",
                         exc);
            throw
                new OperationFailedException(
                        "Failed to authenticate a message request",
                        OperationFailedException.INTERNAL_ERROR,
                        exc);
        }
    }

    /**
     * Represents a general event package subscription in the sense of RFC 3265
     * "Session Initiation Protocol (SIP)-Specific Event Notification" and its
     * signaling characteristics such as Request URI, id tag value of its Event
     * header.
     * 
     * @author Lubomir Marinov
     */
    protected static class Subscription
    {

        /**
         * The subscription <code>Address</code>/Request URI associated with
         * this instance and the target of the SUBSCRIBE requests being created
         * as descriptions of this instance or of the NOTIFY requests being
         * sent.
         */
        protected final Address address;

        /**
         * The <code>Dialog</code> which was created by the SUBSCRIBE request
         * associated with this <code>Subscription</code> or which was used to
         * send that request in.
         */
        private Dialog dialog;

        /**
         * The id tag to be present in Event headers in order to have this
         * <code>Subscription</code> associated with a specific
         * <code>Request</code> or <code>Response</code>.
         */
        protected final String eventId;

        /**
         * The <code>TimerTask</code> associated with this
         * <code>Subscription</code>.
         */
        private TimerTask timerTask;

        /**
         * Initializes a new <code>Subscription</code> instance with a specific
         * subscription <code>Address</code>/Request URI and a specific id tag
         * of the associated Event headers.
         * 
         * @param address
         *            the subscription <code>Address</code>/Request URI which is
         *            to be the target of the SUBSCRIBE requests associated with
         *            the new instance or of the NOTIFY requests to be sent
         * @param eventId
         *            the value of the id tag to be placed in the Event headers
         *            of the SUBSCRIBE and/or NOTIFY requests created for the
         *            new instance and to be present in the received Event
         *            headers in order to have the new instance associated with
         *            them
         */
        public Subscription(Address address, String eventId)
        {
            if (address == null)
                throw new NullPointerException("address");

            this.address = address;
            this.eventId = eventId;
        }

        /**
         * Determines whether the <tt>Address</tt>/Request URI of this
         * <tt>Subscription</tt> is equal to a specific <tt>Address</tt> in the
         * sense of identifying one and the same resource.
         * 
         * @param address the <tt>Address</tt> to be checked for value equality
         * to the <tt>Address</tt>/Request URI of this <tt>Subscription</tt>
         * @return <tt>true</tt> if the <tt>Address</tt>/Request URI of this
         * <tt>Subscription</tt> is equal to the specified <tt>Address</tt> in
         * the sense of identifying one and the same resource
         */
        protected boolean addressEquals(Address address)
        {
            return getAddress().equals(address);
        }

        /**
         * Determines whether this <tt>Subscription</tt> is identified by a
         * specific subscription <tt>Address</tt>/Request URI and a specific id
         * tag of Event headers.
         * 
         * @param address the subscription <tt>Address</tt>/Request URI to be
         * compared to the respective property of this <tt>Subscription</tt>
         * @param eventId the id tag of Event headers to be compared to the
         * respective property of this <tt>Subscription</tt>
         * @return <tt>true</tt> if this <tt>Subscription</tt> has the specified
         * subscription <tt>Address</t>/Request URI and the specified id tag of
         * Event headers; otherwise, <tt>false</tt>
         */
        protected boolean equals(Address address, String eventId)
        {
            if (addressEquals(address))
            {
                String thisEventId = getEventId();

                if (((thisEventId == null) && (eventId == null))
                        || ((thisEventId != null)
                                && thisEventId.equals(eventId)))
                    return true;
            }
            return false;
        }

        /**
         * Gets the subscription <code>Address</code>/Request URI associated
         * with this instance and the target of the SUBSCRIBE requests being
         * created as descriptions of this instance or of the NOTIFY requests
         * being sent.
         * 
         * @return the subscription <code>Address</code>/Request URI associated
         *         with this instance and the target of the SUBSCRIBE requests
         *         being created as descriptions of this instance or of the
         *         NOTIFY requests being sent
         */
        public final Address getAddress()
        {
            return address;
        }

        /**
         * Gets the <code>Dialog</code> which was created by the SUBSCRIBE
         * request associated with this <code>Subscription</code> or which was
         * used to send that request in.
         * 
         * @return the <code>Dialog</code> which was created by the SUBSCRIBE
         *         request associated with this <code>Subscription</code> or
         *         which was used to send that request in; <tt>null</tt> if the
         *         success of the SUBSCRIBE request has not been confirmed yet
         *         or this <code>Subscription</code> was removed from the list
         *         of the <code>EventPackageSupport</code> it used to be in
         */
        protected Dialog getDialog()
        {
            return dialog;
        }

        /**
         * Gets the id tag to be present in Event headers in order to have this
         * <code>Subscription</code> associated with a specific
         * <code>Request</code> or <code>Response</code>. It is also being added
         * to the Event headers when they are created during the generation of
         * <code>Request</code>s or <code>Response</code>s describing this
         * <code>Subscription</code> instance.
         * 
         * @return the id tag to be present in Event headers in order to have
         *         this <code>Subscription</code> associated with a specific
         *         <code>Request</code> or <code>Response</code>
         */
        public final String getEventId()
        {
            return eventId;
        }

        /**
         * Notifies this <code>Subscription</code> that it has been removed from
         * the list of subscriptions of the <code>EventPackageSupport</code>
         * which used to contain it.
         */
        protected void removed()
        {
            setDialog(null);
            setTimerTask(null);
        }

        /**
         * Sets the <code>Dialog</code> which was created by the SUBSCRIBE
         * request associated with this <code>Subscription</code> or which was
         * used to send that request in.
         * 
         * @param dialog
         *            the <code>Dialog</code> which was created by the SUBSCRIBE
         *            request associated with this <code>Subscription</code> or
         *            which was used to send that request in
         */
        protected void setDialog(Dialog dialog)
        {
            this.dialog = dialog;
        }

        /**
         * Sets the <code>TimerTask</code> associated this
         * <code>Subscription</code>. If this <code>Subscription</code> already
         * knows of a different <code>TimerTask</code>, that different one is
         * first canceled before remembering the specified new one.
         * 
         * @param timerTask
         *            a <code>TimerTask</code> to be associated with this
         *            <code>Subscription</code>
         */
        protected void setTimerTask(TimerTask timerTask)
        {
            if (this.timerTask != timerTask)
            {
                if (this.timerTask != null)
                    this.timerTask.cancel();

                this.timerTask = timerTask;
            }
        }
    }
}
