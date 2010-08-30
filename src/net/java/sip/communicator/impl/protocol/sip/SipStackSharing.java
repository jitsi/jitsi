/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.*;
import gov.nist.javax.sip.stack.*;

import java.io.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * This class is the <tt>SipListener</tt> for all JAIN-SIP
 * <tt>SipProvider</tt>s. It is in charge of dispatching the received messages
 * to the suitable <tt>ProtocolProviderServiceSipImpl</tt>s registered with
 * <tt>addSipListener</tt>. It also contains the JAIN-SIP pieces which are
 * common between all <tt>ProtocolProviderServiceSipImpl</tt>s (namely 1
 * <tt>SipStack</tt>, 2 <tt>SipProvider</tt>s, 3 <tt>ListeningPoint</tt>s).
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 * @author Alan Kelly
 * @author Sebastien Mazy
 */
public class SipStackSharing
    implements SipListener
{
    /**
     * We set a custom parameter in the contact address for registrar accounts,
     * so as to ease dispatching of incoming requests in case several accounts
     * have the same username in their contact address, eg:
     * sip:username@192.168.0.1:5060;transport=udp;registering_acc=example_com
     */
    public static final String CONTACT_ADDRESS_CUSTOM_PARAM_NAME
        = "registering_acc";

    /**
     * Logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(SipStackSharing.class);

    /**
     * Our SIP stack (provided by JAIN-SIP).
     */
    private final SipStack stack;

    /**
     * The JAIN-SIP provider that we use for clear UDP/TCP.
     */
    private SipProvider clearJainSipProvider = null;
    /**
     *
     * The JAIN-SIP provider that we use for TLS.
     */
    private SipProvider secureJainSipProvider = null;

    /**
     * The candidate recipients to choose from when dispatching messages
     * received from one the JAIN-SIP <tt>SipProvider</tt>-s. for thread safety
     * issues reasons, better iterate on a copy of that set using
     * <tt>getSipListeners()</tt>.
     */
    private final Set<ProtocolProviderServiceSipImpl> listeners
        = new HashSet<ProtocolProviderServiceSipImpl>();

    /**
     * The property indicating the preferred UDP and TCP
     * port to bind to for clear communications.
     */
    private static final String PREFERRED_CLEAR_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_CLEAR_PORT";

    /**
     * The property indicating the preferred TLS (TCP)
     * port to bind to for secure communications.
     */
    private static final String PREFERRED_SECURE_PORT_PROPERTY_NAME
        = "net.java.sip.communicator.SIP_PREFERRED_SECURE_PORT";

    /**
     * Constructor for this class. Creates the JAIN-SIP stack.
     *
     * @throws OperationFailedException if creating the stack fails.
     */
    SipStackSharing()
        throws OperationFailedException
    {
        // init of the stack
        try
        {
            SipFactory sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");

            Properties sipStackProperties = new SipStackProperties();

            // Create SipStack object
            this.stack = sipFactory.createSipStack(sipStackProperties);
            if (logger.isTraceEnabled())
                logger.trace("Created stack: " + this.stack);

            // set our custom address resolver managing SRV records
            AddressResolverImpl addressResolver =
                new AddressResolverImpl();
            ((SIPTransactionStack) this.stack)
                .setAddressResolver(addressResolver);
        }
        catch(Exception ex)
        {
            logger.fatal("Failed to get SIP Factory.", ex);
            throw new OperationFailedException("Failed to get SIP Factory"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
        }
    }

    /**
     * Adds this <tt>listener</tt> as a candidate recipient for the dispatching
     * of new messages received from the JAIN-SIP <tt>SipProvider</tt>s.
     *
     * @param listener a new possible target for the dispatching process.
     *
     * @throws OperationFailedException if creating one of the underlying
     * <tt>SipProvider</tt>s fails for whatever reason.
     */
    public void addSipListener(ProtocolProviderServiceSipImpl listener)
        throws OperationFailedException
    {
        synchronized(this.listeners)
        {
            if(this.listeners.size() == 0)
                startListening();
            this.listeners.add(listener);
            if (logger.isTraceEnabled())
                logger.trace(this.listeners.size() + " listeners now");
        }
    }

    /**
     * This <tt>listener</tt> will no longer be a candidate recipient for the
     * dispatching of new messages received from the JAIN-SIP
     * <tt>SipProvider</tt>s.
     *
     * @param listener possible target to remove for the dispatching process.
     */
    public void removeSipListener(ProtocolProviderServiceSipImpl listener)
    {
        synchronized(this.listeners)
        {
            this.listeners.remove(listener);

            int listenerCount = listeners.size();
            if (logger.isTraceEnabled())
                logger.trace(listenerCount + " listeners left");
            if(listenerCount == 0)
                stopListening();
        }
    }

    /**
     * Returns a copy of the <tt>listeners</tt> (= candidate recipients) set.
     *
     * @return a copy of the <tt>listeners</tt> set.
     */
    private Set<ProtocolProviderServiceSipImpl> getSipListeners()
    {
        synchronized(this.listeners)
        {
            return new HashSet<ProtocolProviderServiceSipImpl>(this.listeners);
        }
    }

    /**
     * Returns the JAIN-SIP <tt>ListeningPoint</tt> associated to the given
     * transport string.
     *
     * @param transport a string like "UDP", "TCP" or "TLS".
     * @return the LP associated to the given transport.
     */
    @SuppressWarnings("unchecked") //jain-sip legacy code
    public ListeningPoint getLP(String transport)
    {
        ListeningPoint lp;
        Iterator<ListeningPoint> it = this.stack.getListeningPoints();

        while(it.hasNext())
        {
            lp = it.next();
            // FIXME: JAIN-SIP stack is not consistent with case
            // (reported upstream)
            if(lp.getTransport().toLowerCase().equals(transport.toLowerCase()))
                return lp;
        }

        throw new IllegalArgumentException("Invalid transport: " + transport);
    }

    /**
     * Put the stack in a state where it can receive data on three UDP/TCP ports
     * (2 for clear communication, 1 for TLS). That is to say create the related
     * JAIN-SIP <tt>ListeningPoint</tt>s and <tt>SipProvider</tt>s.
     *
     * @throws OperationFailedException if creating one of the underlying
     * <tt>SipProvider</tt>s fails for whatever reason.
     */
    private void startListening()
        throws OperationFailedException
    {
        try
        {
            int bindRetriesValue = getBindRetriesValue();

            this.createProvider(this.getPreferredClearPort(),
                            bindRetriesValue, false);
            this.createProvider(this.getPreferredSecurePort(),
                            bindRetriesValue, true);
            this.stack.start();
            if (logger.isTraceEnabled())
                logger.trace("started listening");
        }
        catch(Exception ex)
        {
            logger.error("An unexpected error happened while creating the"
                    + "SipProviders and ListeningPoints.");
            throw new OperationFailedException("An unexpected error hapenned"
                    + "while initializing the SIP stack"
                    , OperationFailedException.INTERNAL_ERROR
                    , ex);
        }
    }

    /**
     * Attach JAIN-SIP <tt>SipProvider</tt> and <tt>ListeningPoint</tt> to the
     * stack either for clear communications or TLS. Clear UDP and TCP
     * <tt>ListeningPoint</tt>s are not handled separately as the former is a
     * fallback for the latter (depending on the size of the data transmitted).
     * Both <tt>ListeningPoint</tt>s must be bound to the same address and port
     * in order for the related <tt>SipProvider</tt> to be created. If a UDP or
     * TCP <tt>ListeningPoint</tt> cannot bind, retry for both on another port.
     *
     * @param preferredPort which port to try first to bind.
     * @param retries how many times should we try to find a free port to bind
     * @param secure whether to create the TLS SipProvider.
     * or the clear UDP/TCP one.
     *
     * @throws TransportNotSupportedException in case we try to create a
     * provider for a transport not currently supported by jain-sip
     * @throws InvalidArgumentException if we try binding to an illegal port
     * (which we won't)
     * @throws ObjectInUseException if another <tt>SipProvider</tt> is already
     * associated with this <tt>ListeningPoint</tt>.
     * @throws TransportAlreadySupportedException if there is already a
     * ListeningPoint associated to this <tt>SipProvider</tt> with the same
     * transport of the <tt>ListeningPoint</tt>.
     * @throws TooManyListenersException if we try to add a new
     * <tt>SipListener</tt> with a <tt>SipProvider</tt> when one was already
     * registered.
     *
     */
    private void createProvider(int preferredPort, int retries, boolean secure)
        throws TransportNotSupportedException,
        InvalidArgumentException,
        ObjectInUseException,
        TransportAlreadySupportedException,
        TooManyListenersException
    {
        String context = (secure ? "TLS: " : "clear UDP/TCP: ");

        if(retries < 0)
        {
            // very unlikely to happen with the default 50 retries
            logger.error(context + "couldn't find free ports to listen on.");
            return;
        }

        ListeningPoint tlsLP = null;
        ListeningPoint udpLP = null;
        ListeningPoint tcpLP = null;

        try
        {
            if(secure)
            {
                tlsLP = this.stack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , preferredPort
                        , ListeningPoint.TLS);
                if (logger.isTraceEnabled())
                    logger.trace("TLS secure ListeningPoint has been created.");

                this.secureJainSipProvider =
                    this.stack.createSipProvider(tlsLP);
                this.secureJainSipProvider.addSipListener(this);
            }
            else
            {
                udpLP = this.stack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , preferredPort
                        , ListeningPoint.UDP);
                tcpLP = this.stack.createListeningPoint(
                        NetworkUtils.IN_ADDR_ANY
                        , preferredPort
                        , ListeningPoint.TCP);
                if (logger.isTraceEnabled())
                    logger.trace("UDP and TCP clear ListeningPoints have "
                            + "been created.");

                this.clearJainSipProvider =
                    this.stack.createSipProvider(udpLP);
                this.clearJainSipProvider.
                    addListeningPoint(tcpLP);
                this.clearJainSipProvider.addSipListener(this);
            }

            if (logger.isTraceEnabled())
                logger.trace(context + "SipProvider has been created.");
        }
        catch(InvalidArgumentException ex)
        {
            // makes sure we didn't leave an open listener
            // as both UDP and TCP listener have to bind to the same port
            if(tlsLP != null)
                this.stack.deleteListeningPoint(tlsLP);
            if(udpLP != null)
                this.stack.deleteListeningPoint(udpLP);
            if(tcpLP != null)
                this.stack.deleteListeningPoint(tcpLP);

            // FIXME: "Address already in use" is not working
            // as ex.getMessage() displays in the locale language in SC
            // (getMessage() is always supposed to be English though)
            // this should be a temporary workaround
            //if (ex.getMessage().indexOf("Address already in use") != -1)
            // another software is probably using the port
            if(ex.getCause() instanceof java.io.IOException)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Port " + preferredPort
                            + " seems in use for either TCP or UDP.");

                // tries again on a new random port
                int currentlyTriedPort = NetworkUtils.getRandomPortNumber();
                if (logger.isDebugEnabled())
                    logger.debug("Retrying bind on port " + currentlyTriedPort);
                this.createProvider(currentlyTriedPort, retries-1, secure);
            }
            else
                throw ex;
        }
    }

    /**
     * Put the JAIN-SIP stack in a state where it cannot receive any data and
     * frees the network ports used. That is to say remove JAIN-SIP
     * <tt>ListeningPoint</tt>s and <tt>SipProvider</tt>s.
     */
    @SuppressWarnings("unchecked") //jain-sip legacy code
    private void stopListening()
    {
        try
        {
            this.secureJainSipProvider.removeSipListener(this);
            this.stack.deleteSipProvider(this.secureJainSipProvider);
            this.secureJainSipProvider = null;
            this.clearJainSipProvider.removeSipListener(this);
            this.stack.deleteSipProvider(this.clearJainSipProvider);
            this.clearJainSipProvider = null;

            Iterator<ListeningPoint> it = this.stack.getListeningPoints();
            Vector<ListeningPoint> lpointsToRemove = new Vector<ListeningPoint>();
            while(it.hasNext())
            {
                lpointsToRemove.add(it.next());
            }

            it = lpointsToRemove.iterator();
            while (it.hasNext())
            {
                this.stack.deleteListeningPoint(it.next());
            }

            this.stack.stop();
            if (logger.isTraceEnabled())
                logger.trace("stopped listening");
        }
        catch(ObjectInUseException ex)
        {
            logger.fatal("Failed to stop listening", ex);
        }
    }

    /**
     * Returns the JAIN-SIP <tt>SipProvider</tt> in charge of this
     * <tt>transport</tt>.
     *
     * @param transport a <tt>String</tt> like "TCP", "UDP" or "TLS"
     * @return the corresponding <tt>SipProvider</tt>
     */
    public SipProvider getJainSipProvider(String transport)
    {
        SipProvider sp = null;
        if(transport.equalsIgnoreCase(ListeningPoint.UDP)
                || transport.equalsIgnoreCase(ListeningPoint.TCP))
            sp = this.clearJainSipProvider;
        else if(transport.equalsIgnoreCase(ListeningPoint.TLS))
            sp = this.secureJainSipProvider;

        if(sp == null)
            throw new IllegalArgumentException("invalid transport");
        return sp;
    }

    /**
     * Fetches the preferred UDP and TCP port for clear communications in the
     * user preferences or fallback on a default value.
     *
     * @return the preferred network port for clear communications.
     */
    private int getPreferredClearPort()
    {
        return SipActivator.getConfigurationService().getInt(
            PREFERRED_CLEAR_PORT_PROPERTY_NAME, ListeningPoint.PORT_5060);
    }

    /**
     * Fetches the preferred TLS (TCP) port for secure communications in the
     * user preferences or fallback on a default value.
     *
     * @return the preferred network port for secure communications.
     */
    private int getPreferredSecurePort()
    {
        return SipActivator.getConfigurationService().getInt(
            PREFERRED_SECURE_PORT_PROPERTY_NAME, ListeningPoint.PORT_5061);
    }

    /**
     * Fetches the number of times to retry when the binding of a JAIN-SIP
     * <tt>ListeningPoint</tt> fails. Looks in the user preferences or
     * fallbacks on a default value.
     *
     * @return the number of times to retry a failed bind.
     */
    private int getBindRetriesValue()
    {
        return SipActivator.getConfigurationService().getInt(
            ProtocolProviderService.BIND_RETRIES_PROPERTY_NAME,
            ProtocolProviderService.BIND_RETRIES_DEFAULT_VALUE);
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a
     * <tt>SipProvider</tt>.
     */
    public void processDialogTerminated(DialogTerminatedEvent event)
    {
        try
        {
            ProtocolProviderServiceSipImpl recipient
                = (ProtocolProviderServiceSipImpl) SipApplicationData
                    .getApplicationData(event.getDialog(),
                                        SipApplicationData.KEY_SERVICE);
            if(recipient == null)
            {
                logger.error("Dialog wasn't marked, please report this to "
                                + "dev@sip-communicator.dev.java.net");
            }
            else
            {
                if (logger.isTraceEnabled())
                    logger.trace("service was found with dialog data");
                recipient.processDialogTerminated(event);
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            this.logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processIOException(IOExceptionEvent event)
    {
        try
        {
            if (logger.isTraceEnabled())
                logger.trace(event);

            // impossible to dispatch, log here
            if (logger.isDebugEnabled())
                logger.debug("@todo implement processIOException()");
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            this.logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processRequest(RequestEvent event)
    {
        try
        {
            Request request = event.getRequest();
            if (logger.isTraceEnabled())
                logger.trace("received request: " + request.getMethod());

            /*
             * Create the transaction if it doesn't exist yet. If it is a
             * dialog-creating request, the dialog will also be automatically
             * created by the stack.
             */
            if (event.getServerTransaction() == null)
            {
                try
                {

                    /*
                     * Max-Forwards is required, yet there are UAs which do not
                     * place it. SipProvider#getNewServerTransaction(Request)
                     * will throw an exception in the case of a missing
                     * Max-Forwards header and this method will eventually just
                     * log it thus ignoring the whole event. Since these
                     * requests come often in some cases (e.g. ippi.fr),
                     * performance-wise it makes sense to just prevent the
                     * exceptions and ignore the event early.
                     */
                    if (request.getHeader(MaxForwardsHeader.NAME) == null)
                    {
                        //it appears that some buggy providers do send requests
                        //with no Max-Forwards headers, so let's at least try
                        //to save calls.
                        if(Request.INVITE.equals(request.getMethod()))
                        {
                            MaxForwardsHeader maxForwards = SipFactory
                                .getInstance().createHeaderFactory()
                                    .createMaxForwardsHeader(70);
                            request.setHeader(maxForwards);
                        }
                        else
                        {
                            if (logger.isTraceEnabled())
                                logger.trace(
                                        "Ignoring request without Max-Forwards header: "
                                        + event);
                            return;
                        }
                    }

                    SipProvider source = (SipProvider) event.getSource();
                    ServerTransaction transaction
                        = source.getNewServerTransaction(request);

                    /*
                     * Update the event, otherwise getServerTransaction() and
                     * getDialog() will still return their previous value.
                     */
                    event
                        = new RequestEvent(
                                source,
                                transaction,
                                transaction.getDialog(),
                                request);
                }
                catch (SipException ex)
                {
                    logger.error(
                        "couldn't create transaction, please report "
                            + "this to dev@sip-communicator.dev.java.net",
                        ex);
                }
            }

            ProtocolProviderServiceSipImpl service
                = getServiceData(event.getServerTransaction());
            if (service != null)
            {
                service.processRequest(event);
            }
            else
            {
                service = findTargetFor(request);
                if (service == null)
                {
                    logger.error(
                        "couldn't find a ProtocolProviderServiceSipImpl "
                            + "to dispatch to");
                }
                else
                {

                    /*
                     * Mark the dialog for the dispatching of later in-dialog
                     * requests. If there is no dialog, we need to mark the
                     * request to dispatch a possible timeout when sending the
                     * response.
                     */
                    Object container = event.getDialog();
                    if (container == null)
                        container = request;
                    SipApplicationData.setApplicationData(
                        container,
                        SipApplicationData.KEY_SERVICE,
                        service);

                    service.processRequest(event);
                }
            }
        }
        catch(Throwable exc)
        {

            /*
             * Any exception thrown within our code should be caught here so
             * that we could log it rather than interrupt stack activity with
             * it.
             */
            this.logApplicationException(DialogTerminatedEvent.class, exc);

            // Unfortunately, death can hardly be ignored.
            if (exc instanceof ThreadDeath)
                throw (ThreadDeath) exc;
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processResponse(ResponseEvent event)
    {
        try
        {
            // we don't have to accept the transaction since we
            //created the request
            ClientTransaction transaction = event.getClientTransaction();
            if (logger.isTraceEnabled())
                logger.trace("received response: "
                        + event.getResponse().getStatusCode()
                        + " " + event.getResponse().getReasonPhrase());

            if(transaction == null)
            {
                logger.warn("Transaction is null, probably already expired!");
                return;
            }

            ProtocolProviderServiceSipImpl service
                = getServiceData(transaction);
            if (service != null)
            {
                // Mark the dialog for the dispatching of later in-dialog
                // responses. If there is no dialog then the initial request
                // sure is marked otherwise we won't have found the service with
                // getServiceData(). The request has to be marked in case we
                // receive one more response in an out-of-dialog transaction.
                if (event.getDialog() != null)
                {
                    SipApplicationData.setApplicationData(event.getDialog(),
                                    SipApplicationData.KEY_SERVICE, service);
                }
                service.processResponse(event);
            }
            else
            {
                logger.error("We received a response which "
                                + "wasn't marked, please report this to "
                                + "dev@sip-communicator.dev.java.net");
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            this.logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a <tt>SipProvider</tt>.
     */
    public void processTimeout(TimeoutEvent event)
    {
        try
        {
            Transaction transaction;
            if (event.isServerTransaction())
            {
                transaction = event.getServerTransaction();
            }
            else
            {
                transaction = event.getClientTransaction();
            }

            ProtocolProviderServiceSipImpl recipient
                = getServiceData(transaction);
            if (recipient == null)
            {
                logger.error("We received a timeout which wasn't "
                                + "marked, please report this to "
                                + "dev@sip-communicator.dev.java.net");
            }
            else
            {
                recipient.processTimeout(event);
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            this.logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param event the event received for a
     * <tt>SipProvider</tt>.
     */
    public void processTransactionTerminated(TransactionTerminatedEvent event)
    {
        try
        {
            Transaction transaction;
            if (event.isServerTransaction())
                transaction = event.getServerTransaction();
            else
                transaction = event.getClientTransaction();

            ProtocolProviderServiceSipImpl recipient
                = getServiceData(transaction);

            if (recipient == null)
            {
                logger.error("We received a transaction terminated which wasn't"
                                + " marked, please report this to"
                                + " dev@sip-communicator.dev.java.net");
            }
            else
            {
                recipient.processTransactionTerminated(event);
            }
        }
        catch(Throwable exc)
        {
            //any exception thrown within our code should be caught here
            //so that we could log it rather than interrupt stack activity with
            //it.
            this.logApplicationException(DialogTerminatedEvent.class, exc);
        }
    }

    /**
     * Find the <tt>ProtocolProviderServiceSipImpl</tt> (one of our
     * "candidate recipient" listeners) which this <tt>request</tt> should be
     * dispatched to. The strategy is to look first at the request URI, and
     * then at the To field to find a matching candidate for dispatching.
     * Note that this method takes a <tt>Request</tt> as param, and not a
     * <tt>ServerTransaction</tt>, because sometimes <tt>RequestEvent</tt>s
     * have no associated <tt>ServerTransaction</tt>.
     *
     * @param request the <tt>Request</tt> to find a recipient for.
     * @return a suitable <tt>ProtocolProviderServiceSipImpl</tt>.
     */
    private ProtocolProviderServiceSipImpl findTargetFor(Request request)
    {
        if(request == null)
        {
            logger.error("request shouldn't be null.");
            return null;
        }

        Set<ProtocolProviderServiceSipImpl> currentListeners
            = this.getSipListeners();

        if(currentListeners.size() == 0)
        {
            logger.error("no listeners");
            return null;
        }

        URI requestURI = request.getRequestURI();

        if(requestURI.isSipURI())
        {
            String requestUser = ((SipURI) requestURI).getUser();

            List<ProtocolProviderServiceSipImpl> candidates =
                new ArrayList<ProtocolProviderServiceSipImpl>();

            // check if the Request-URI username is
            // one of ours usernames
            for(ProtocolProviderServiceSipImpl listener : currentListeners)
            {
                String ourUserID = listener.getAccountID().getUserID();
                //logger.trace(ourUserID + " *** " + requestUser);
                if(ourUserID.equals(requestUser))
                {
                    if (logger.isTraceEnabled())
                        logger.trace("suitable candidate found: "
                                + listener.getAccountID());
                    candidates.add(listener);
                }
            }

            // the perfect match
            // every other case is approximation
            if(candidates.size() == 1)
            {
                ProtocolProviderServiceSipImpl perfectMatch = candidates.get(0);

                if (logger.isTraceEnabled())
                    logger.trace("Will dispatch to \""
                            + perfectMatch.getAccountID() + "\"");
                return perfectMatch;
            }

            // more than one account match
            if(candidates.size() > 1)
            {
                // check if a custom param exists in the contact
                // address (set for registrar accounts)
                for (ProtocolProviderServiceSipImpl candidate : candidates)
                {
                    String hostValue = ((SipURI) requestURI).getParameter(
                            SipStackSharing.CONTACT_ADDRESS_CUSTOM_PARAM_NAME);
                    if (hostValue == null)
                        continue;
                    if (hostValue.equals(candidate
                                .getContactAddressCustomParamValue()))
                    {
                        if (logger.isTraceEnabled())
                            logger.trace("Will dispatch to \""
                                    + candidate.getAccountID() + "\" because "
                                    + "\" the custom param was set");
                        return candidate;
                    }
                }

                // Past this point, our guess is not reliable. We try to find
                // the "least worst" match based on parameters like the To field

                // check if the To header field host part
                // matches any of our SIP hosts
                for(ProtocolProviderServiceSipImpl candidate : candidates)
                {
                    URI fromURI = ((FromHeader) request
                            .getHeader(FromHeader.NAME)).getAddress().getURI();
                    if(fromURI.isSipURI() == false)
                        continue;
                    SipURI ourURI = (SipURI) candidate
                        .getOurSipAddress((SipURI) fromURI).getURI();
                    String ourHost = ourURI.getHost();

                    URI toURI = ((ToHeader) request
                            .getHeader(ToHeader.NAME)).getAddress().getURI();
                    if(toURI.isSipURI() == false)
                        continue;
                    String toHost = ((SipURI) toURI).getHost();

                    //logger.trace(toHost + "***" + ourHost);
                    if(toHost.equals(ourHost))
                    {
                        if (logger.isTraceEnabled())
                            logger.trace("Will dispatch to \""
                                    + candidate.getAccountID() + "\" because "
                                    + "host in the To: is the same as in our AOR");
                        return candidate;
                    }
                }

                // fallback on the first candidate
                ProtocolProviderServiceSipImpl target =
                    candidates.iterator().next();
                logger.warn("Will randomly dispatch to \""
                        + target.getAccountID()
                        + "\" because there is ambiguity on the username from"
                        + " the Request-URI");
                if (logger.isTraceEnabled())
                    logger.trace("\n" + request);
                return target;
            }

            // fallback on any account
            ProtocolProviderServiceSipImpl target =
                currentListeners.iterator().next();
            if (logger.isInfoEnabled())
                logger.info("Will randomly dispatch to \"" + target.getAccountID()
                        + "\" because the username in the Request-URI "
                        + "is unknown or empty");
            if (logger.isTraceEnabled())
                logger.trace("\n" + request);
            return target;
        }
        else
        {
            logger.error("Request-URI is not a SIP URI, dropping");
        }
        return null;
    }

    /**
     * Retrieves and returns that ProtocolProviderService that this transaction
     * belongs to, or <tt>null</tt> if we couldn't associate it with a provider
     * based on neither the request nor the transaction itself.
     *
     * @param transaction the transaction that we'd like to determine a provider
     * for.
     *
     * @return a reference to the <tt>ProtocolProviderServiceSipImpl</tt> that
     * <tt>transaction</tt> was associated with or <tt>null</tt> if we couldn't
     * determine which one it is.
     */
    private ProtocolProviderServiceSipImpl
        getServiceData(Transaction transaction)
    {
        ProtocolProviderServiceSipImpl service
            = (ProtocolProviderServiceSipImpl) SipApplicationData
            .getApplicationData(transaction.getRequest(),
                    SipApplicationData.KEY_SERVICE);

        if (service != null)
        {
            if (logger.isTraceEnabled())
                logger.trace("service was found in request data");
            return service;
        }

        service = (ProtocolProviderServiceSipImpl) SipApplicationData
            .getApplicationData(transaction.getDialog(),
                    SipApplicationData.KEY_SERVICE);
        if (service != null)
        {
            if (logger.isTraceEnabled())
                logger.trace("service was found in dialog data");
        }

        return service;
    }

    /**
     * Logs exceptions that have occurred in the application while processing
     * events originating from the stack.
     *
     * @param eventClass the class of the jain-sip event that we were handling
     * when the exception was thrown.
     * @param exc the exception that we need to log.
     */
    private void logApplicationException(
        Class<DialogTerminatedEvent> eventClass,
        Throwable exc)
    {
        String message
            = "An error occurred while processing event of type: "
                + eventClass.getName();

        logger.error(message, exc);
        if (logger.isDebugEnabled())
            logger.debug(message, exc);
    }

    /**
     * Safely returns the transaction from the event if already exists.
     * If not a new transaction is created.
     *
     * @param event the request event
     * @return the server transaction
     * @throws javax.sip.TransactionAlreadyExistsException if transaction exists
     * @throws javax.sip.TransactionUnavailableException if unavailable
     */
    public static ServerTransaction getOrCreateServerTransaction(
                                                            RequestEvent event)
        throws TransactionAlreadyExistsException,
               TransactionUnavailableException
    {
        ServerTransaction serverTransaction = event.getServerTransaction();

        if(serverTransaction == null)
        {
            SipProvider jainSipProvider = (SipProvider) event.getSource();

            serverTransaction
                = jainSipProvider
                    .getNewServerTransaction(event.getRequest());
        }
        return serverTransaction;
    }

    /**
     * Returns a local address to use with the specified TCP destination.
     * The method forces the JAIN-SIP stack to creates and binds (if necessary)
     * and return a socket connected to the specified destination address and
     * port and then return its local address.
     *
     * @param dst the destination address that the socket would need to connect
     *            to.
     * @param dstPort the port number that the connection would be established
     * with.
     * @param localAddress the address that we would like to bind on
     * (null for the "any" address).
     * @param transport the transport that will be used TCP ot TLS
     *
     * @return the SocketAddress that this handler would use when connecting to
     * the specified destination address and port.
     *
     * @throws IOException !!!!!!!!!!!!!!!!!!!!!!! FILL IN !!!!!!!!!!!!!!
     */
    public java.net.InetSocketAddress getLocalAddressForDestination(
                    java.net.InetAddress dst,
                    int                  dstPort,
                    java.net.InetAddress localAddress,
                    String transport)
        throws IOException
    {
//        if(ListeningPoint.TLS.equalsIgnoreCase(transport))
//            return (java.net.InetSocketAddress)(((SipStackImpl)this.stack)
//                .getLocalTLSAddressForDestination(dst, dstPort, localAddress));
//        else
            return (java.net.InetSocketAddress)(((SipStackImpl)this.stack)
            .getLocalAddressForDestination(dst, dstPort, localAddress, 0));
    }
}
