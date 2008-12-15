/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.stack.*;

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
     */
    public void addSipListener(ProtocolProviderServiceSipImpl listener)
        throws OperationFailedException
    {
        synchronized(this.listeners)
        {
            if(this.listeners.size() == 0)
                startListening();
            this.listeners.add(listener);
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
     */
    private void startListening()
        throws OperationFailedException
    {
        try
        {
            int bindRetriesValue = getBindRetriesValue();

            this.createProvider(this.getPreferredClearPort()
                    , bindRetriesValue, false);
            this.createProvider(this.getPreferredSecurePort()
                    , bindRetriesValue, true);
            this.stack.start();
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
     */
    private void createProvider(int preferredPort, int retries, boolean secure)
        throws TransportNotSupportedException
        , InvalidArgumentException
        , ObjectInUseException
        , TransportAlreadySupportedException
        , TooManyListenersException
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
                logger.trace("UDP and TCP clear ListeningPoints have "
                        + "been created.");

                this.clearJainSipProvider =
                    this.stack.createSipProvider(udpLP);
                this.clearJainSipProvider.
                    addListeningPoint(tcpLP);
                this.clearJainSipProvider.addSipListener(this);
            }

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
                logger.debug("Port " + preferredPort
                        + " seems in use for either TCP or UDP.");

                // tries again on a new random port
                int currentlyTriedPort = NetworkUtils.getRandomPortNumber();
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
            while(it.hasNext())
            {
                this.stack.deleteListeningPoint(it.next());
                it = this.stack.getListeningPoints();
            }

            this.stack.stop();
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
     * @param dialogTerminatedEvent the event received for a
     * <tt>SipProvider</tt>.
     */
    public void processDialogTerminated(
                                DialogTerminatedEvent dialogTerminatedEvent)
    {
        logger.trace(dialogTerminatedEvent);

        Dialog dialog = dialogTerminatedEvent.getDialog();
        Address address = dialog.getLocalParty();
        if(dialog.getRemoteTarget().getURI() instanceof SipURI)
        {
            SipURI remoteURI = (SipURI) dialog.getRemoteTarget().getURI();
            ProtocolProviderServiceSipImpl pp
                                        = getListenerFor(address, remoteURI);
            if(pp != null)
                pp.processDialogTerminated(dialogTerminatedEvent);
        }


        logger.debug("Dialog terminated for req="
                + dialogTerminatedEvent.getDialog());
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param exceptionEvent the event received for a <tt>SipProvider</tt>.
     */
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
        logger.trace(exceptionEvent);

        // impossible to dispatch, log here
        logger.debug("@todo implement processIOException()");
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param requestEvent the event received for a <tt>SipProvider</tt>.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        logger.trace(requestEvent);

        ProtocolProviderServiceSipImpl recipient =
            findTargetFor(requestEvent.getRequest());

        if(recipient != null)
            recipient.processRequest(requestEvent);
        else
            logger.error("couldn't find a ProtocolProviderServiceSipImpl "
                            +"to dispatch to");
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param responseEvent the event received for a <tt>SipProvider</tt>.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        logger.trace(responseEvent);

        ProtocolProviderServiceSipImpl recipient =
            findTargetFor(responseEvent.getClientTransaction());

        if(recipient != null)
            recipient.processResponse(responseEvent);
        else
            logger.error("couldn't find a ProtocolProviderServiceSipImpl "
                            +"to dispatch to");
    }

    /**
     * Dispatches the event received from a JAIN-SIP <tt>SipProvider</tt> to one
     * of our "candidate recipient" listeners.
     *
     * @param timeoutEvent the event received for a <tt>SipProvider</tt>.
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        logger.trace(timeoutEvent);

        ProtocolProviderServiceSipImpl recipient;
        if(timeoutEvent.isServerTransaction())
        {
            recipient = findTargetFor(
                            timeoutEvent.getServerTransaction().getRequest());
            if(recipient != null)
                recipient.processTimeout(timeoutEvent);
        }
        else
        {
            recipient = findTargetFor(timeoutEvent.getClientTransaction());
            if(recipient != null)
                recipient.processTimeout(timeoutEvent);
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
        ProtocolProviderServiceSipImpl recipient;
        if(event.isServerTransaction())
        {
            logger.trace("server transaction terminated");
            recipient = findTargetFor(
                    event.getServerTransaction().getRequest());
            if(recipient != null)
                recipient.processTransactionTerminated(event);
        }
        else
        {
            logger.trace("client transaction terminated");
            recipient = findTargetFor(event.getClientTransaction());
            if(recipient != null)
                recipient.processTransactionTerminated(event);
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
                String ourUserID = (String) listener.getAccountID().getUserID();
                logger.trace(ourUserID + " *** " + requestUser);
                if(ourUserID.equals(requestUser))
                {
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

                logger.trace("(0) will dispatch to: "
                    + perfectMatch.getAccountID());
                return perfectMatch;
            }

            // past this point, our guess is not reliable
            // we try to find the "least worst" match based on parameters
            // like the To field
            logger.warn("impossible to guess reliably which account this "
                    + "request is addressed to but we'll still try");

            // more than one account match
            if(candidates.size() > 1)
            {
                // check if the To header field SIP URI
                // matches any of our SIP URIs
                // (same user and host)
                for(ProtocolProviderServiceSipImpl candidate : candidates)
                {
                    URI fromURI = ((FromHeader) request
                            .getHeader(FromHeader.NAME)).getAddress().getURI();
                    if(fromURI.isSipURI() == false)
                        continue;
                    SipURI ourURI = (SipURI) candidate
                        .getOurSipAddress((SipURI) fromURI).getURI();
                    String ourUser = ourURI.getUser();
                    String ourHost = ourURI.getHost();

                    URI toURI = ((ToHeader) request
                            .getHeader(ToHeader.NAME)).getAddress().getURI();
                    if(toURI.isSipURI() == false)
                        continue;
                    String toUser = ((SipURI) toURI).getUser();
                    String toHost = ((SipURI) toURI).getHost();

                    //logger.trace(toUser + "@" + toHost + "***"
                    //        + ourUser + "@" + ourHost);
                    if(toUser.equals(ourUser) && toHost.equals(ourHost))
                    {
                        logger.trace("(1) will dispatch to: " +
                                candidate.getAccountID());
                        return candidate;
                    }
                }

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
                        logger.trace("(2) will dispatch to: " +
                                candidate.getAccountID());
                        return candidate;
                    }
                }

                // check if the To header field username part
                // matches any of our SIP usernames
                for(ProtocolProviderServiceSipImpl candidate : candidates)
                {
                    URI fromURI = ((FromHeader) request
                            .getHeader(FromHeader.NAME)).getAddress().getURI();
                    if(fromURI.isSipURI() == false)
                        continue;
                    SipURI ourURI = (SipURI) candidate
                        .getOurSipAddress((SipURI) fromURI).getURI();
                    String ourUser = ourURI.getUser();

                    URI toURI = ((ToHeader) request
                            .getHeader(ToHeader.NAME)).getAddress().getURI();
                    if(toURI.isSipURI() == false)
                        continue;
                    String toUser = ((SipURI) toURI).getUser();

                    //logger.trace(toUser + "***" + ourUser);
                    if(toUser.equals(ourUser))
                    {
                        logger.trace("(3) will dispatch to: " +
                                candidate.getAccountID());
                        return candidate;
                    }
                }

                // fallback on the first candidate
                ProtocolProviderServiceSipImpl target =
                    candidates.iterator().next();
                logger.trace("(4) will dispatch to: " + target.getAccountID());
                return target;
            }

            // check if the To header field SIP URI
            // matches any of our SIP URIs
            for(ProtocolProviderServiceSipImpl listener : currentListeners)
            {
                URI fromURI = ((FromHeader) request
                        .getHeader(FromHeader.NAME)).getAddress().getURI();
                if(fromURI.isSipURI() == false)
                    continue;
                SipURI ourURI = (SipURI) listener
                    .getOurSipAddress((SipURI) fromURI).getURI();
                String ourUser = ourURI.getUser();
                String ourHost = ourURI.getHost();

                URI toURI = ((ToHeader) request
                        .getHeader(ToHeader.NAME)).getAddress().getURI();
                if(toURI.isSipURI() == false)
                    continue;
                String toUser = ((SipURI) toURI).getUser();
                String toHost = ((SipURI) toURI).getHost();

                //logger.trace(toUser + "@" + toHost + "***"
                //        + ourUser + "@" + ourHost);
                if(toUser.equals(ourUser) && toHost.equals(ourHost))
                {
                    logger.trace("(5) will dispatch to: " +
                            listener.getAccountID());
                    return listener;
                }
            }

            // check if the To header field host part
            // matches any of our account hosts
            for(ProtocolProviderServiceSipImpl listener : currentListeners)
            {
                URI fromURI = ((FromHeader) request
                        .getHeader(FromHeader.NAME)).getAddress().getURI();
                if(fromURI.isSipURI() == false)
                    continue;
                SipURI ourURI = (SipURI) listener
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
                    logger.trace("(6) will dispatch to: " +
                            listener.getAccountID());
                    return listener;
                }
            }

            // check if the To header field username part
            // matches any of our account usernames
            for(ProtocolProviderServiceSipImpl listener : currentListeners)
            {
                URI fromURI = ((FromHeader) request
                        .getHeader(FromHeader.NAME)).getAddress().getURI();
                if(fromURI.isSipURI() == false)
                    continue;
                SipURI ourURI = (SipURI) listener
                    .getOurSipAddress((SipURI) fromURI).getURI();
                String ourUser = ourURI.getUser();

                URI toURI = ((ToHeader) request
                        .getHeader(ToHeader.NAME)).getAddress().getURI();
                if(toURI.isSipURI() == false)
                    continue;
                String toUser = ((SipURI) toURI).getUser();

                //logger.trace(toUser + "***" + ourUser);
                if(toUser.equals(ourUser))
                {
                    logger.trace("(7) will dispatch to: " +
                            listener.getAccountID());
                    return listener;
                }
            }

            // fallback on any account
            ProtocolProviderServiceSipImpl target =
                currentListeners.iterator().next();
            logger.trace("(8) will dispatch to: " + target.getAccountID());
            return target;

        }
        else
        {
            logger.error("Request-URI is not a SIP URI, dropping");
        }
        return null;
    }

    /**
     * Finds the <tt>ProtocolProviderServiceSipImpl</tt> which this
     * <tt>ClientTransaction</tt> should be dispatched to. The JAIN-SIP stack
     * stores the initial request we made to the UAS. Using the From field we
     * set ourselves there when generating the request, it is possible to find
     * the <tt>ProtocolProviderServiceSipImpl</tt> originator of the request.
     *
     * @param transaction the <tt>ClientTransaction</tt> to find a target for.
     * @return a matching <tt>ProtocolProviderServiceSipImpl</tt>.
     */
    private ProtocolProviderServiceSipImpl findTargetFor(
                                                ClientTransaction transaction)
    {
        if(transaction == null)
        {
            logger.error("transaction shouldn't be null.");
            return null;
        }

        Request request = transaction.getRequest();

        if(request.getRequestURI().isSipURI() == false)
        {
            logger.error("requested URI wasn't a SIP URI.");
            return null;
        }

        Address address = ((FromHeader) request.getHeader(FromHeader.NAME)).
            getAddress();
        SipURI remoteURI = (SipURI) request.getRequestURI();
        return getListenerFor(address, remoteURI);
    }

    /**
     * Find the <tt>ProtocolProviderServiceSipImpl</tt> which would have
     * <tt>localParty</tt> as SIP address when contacting the remote end at
     * <tt>remoteURI</tt>.
     *
     * @param localParty the local address used to contact a remote end.
     * @param remoteURI the remote URI used to generate <tt>localParty</tt>.
     * @return a <tt>ProtocolProviderServiceSipImpl</tt> which would use
     * <tt>localParty</tt> as local address to contact <tt>remoteURI</tt>.
     */
    private ProtocolProviderServiceSipImpl getListenerFor(Address localParty,
                                                          SipURI remoteURI)
    {
        Set<ProtocolProviderServiceSipImpl> currentListeners
            = this.getSipListeners();

        for(ProtocolProviderServiceSipImpl listener : currentListeners)
            if(listener.getOurSipAddress(remoteURI).equals(localParty))
            {
                logger.trace("found listener for local party: " + localParty);
                return listener;
            }

        logger.error("no listener found for local party: " + localParty);
        return null;
    }
}
